package artisynth.core.renderables;

import java.awt.Color;
import artisynth.core.mechmodels.MeshComponent;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.*;

import maspack.geometry.*;
import maspack.interpolation.*;
import maspack.matrix.*;
import maspack.spatialmotion.Twist;
import maspack.properties.*;
import maspack.util.*;
import maspack.render.*;
import maspack.render.Renderer.*;

import java.io.*;
import java.util.*;

import artisynth.core.util.*;

/**
 * Component for representing a curve on the surface of a mesh.
 */
public class MeshCurve extends RenderableCompositeBase {

   public static final double INF = Double.POSITIVE_INFINITY;
   
   protected MeshComponent myMeshComp = null;
   protected PointList<MeshMarker> myMarkers;
   protected boolean myCurveValid = false;
   protected ArrayList<Point3d> myPoints;
   protected ArrayList<Vector3d> myNormals;
   private RigidTransform3d myRenderFrame = new RigidTransform3d();

   private static final int CURVE_GRP = 0;
   private static final int NORMAL_GRP = 1;
   private RenderObject myRob;

   protected static final double DEFAULT_NORMAL_COMPUTE_RADIUS = -1;
   protected double myNormalComputeRadius = DEFAULT_NORMAL_COMPUTE_RADIUS;
   protected PropertyMode myNormalComputeRadiusMode = PropertyMode.Inherited;

   public enum Interpolation {
      LINEAR,
      B_SPLINE,
      NATURAL_SPLINE
   };

   public static int DEFAULT_RESOLUTION = -1;
   protected double myResolution = DEFAULT_RESOLUTION;

   public static boolean DEFAULT_CLOSED = false;
   protected boolean myClosed = DEFAULT_CLOSED;

   public static Interpolation DEFAULT_INTERPOLATION = Interpolation.LINEAR;
   protected Interpolation myInterpolation = DEFAULT_INTERPOLATION;

   public static double DEFAULT_NORMAL_LENGTH = 0;
   protected double myNormalLength = DEFAULT_NORMAL_LENGTH;

   public static boolean DEFAULT_PROJECT_TO_MESH = true;
   protected boolean myProjectToMesh = DEFAULT_PROJECT_TO_MESH;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createRenderProps (host);
      return props;
   }

   public static PropertyList myProps =
      new PropertyList (MeshCurve.class, RenderableCompositeBase.class);

   static {
      myProps.add (
         "renderProps", "render properties", defaultRenderProps(null));
      myProps.add (
         "resolution", "spacing between curve points",
         DEFAULT_RESOLUTION);
      myProps.add (
         "closed isClosed", "sets whether or not the curve is closed",
         DEFAULT_CLOSED);
      myProps.add (
         "interpolation", "interpolation for knot-based curves",
         DEFAULT_INTERPOLATION);
      myProps.addInheritable (
         "normalComputeRadius:Inherited",
         "max distance for vertices used to compute the marker surface normal",
         DEFAULT_NORMAL_COMPUTE_RADIUS);
      myProps.add (
         "normalLength", 
         "length of surface normals to draw along the curve points",
         DEFAULT_NORMAL_LENGTH);
      myProps.add (
         "projectToMesh", 
         "if true, curve points between knots are projected onto the mesh",
         DEFAULT_PROJECT_TO_MESH);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MeshCurve () {
      setRenderProps (createRenderProps());
      initializeChildComponents();
   }

   public MeshCurve (MeshComponent mcomp) {
      this();
      validateMesh (mcomp);
      myMeshComp = mcomp;
      setResolution (-1);
      setNormalComputeRadius (-1);
   }

   private void validateMesh (MeshComponent mcomp) {
      MeshBase mesh = mcomp.getMesh();
      if (!(mesh instanceof PolygonalMesh) ||
          !((PolygonalMesh)mesh).isTriangular()) {
         throw new IllegalArgumentException (
            "Mesh component does not containt a triangular mesh");
      }
   }

   protected void initializeChildComponents() {
      myPoints = new ArrayList<>();
      myNormals = new ArrayList<>();
      myMarkers = new PointList (MeshMarker.class, "markers");
      add (myMarkers);
   }

   // property accessors

   public double getResolution() {
      return myResolution;
   }

   public void setResolution (double res) {
      if (res < 0) {
         res = computeDefaultResolution();
      }
      if (res != myResolution) {
         myResolution = res;
         myCurveValid = false;
      }
   }

   private double computeDefaultResolution () {
      return RenderableUtils.getRadius (myMeshComp)/100;
   }

   public Interpolation getInterpolation() {
      return myInterpolation;
   }

   public boolean isClosed() {
      return myClosed;
   }

   public void setClosed (boolean closed) {
      if (myClosed != closed) {
         myClosed = closed;
         myCurveValid = false;
      }
   }

   public void setInterpolation (Interpolation interp) {
      if (myInterpolation != interp) {
         myInterpolation = interp;
         myCurveValid = false;
      }
   }

   public double getNormalLength() {
      return myNormalLength;
   }

   public void setNormalLength (double len) {
      if (len != myNormalLength) {
         myNormalLength = len;
         myCurveValid = false;
      }
   }

   /**
    * Sets the radius around the marker used to collect vertices for estimating
    * its surface normal. The surface normal is computed as a weighted average
    * of the normals of these vertices plus the normal of the marker's face.  A
    * value of 0 means that only the face normal is used. The weighting is
    * Gaussian, based on the distance of the vertex from the marker, with the
    * radius corresponding to two standard deviations. By default, this value
    * is inherited from an ancestor component, or otherwise set to a default
    * value based on the mesh's size. Specifying a negative value also causes
    * this default value to be computed.
    *
    * @param rad normal compute radius
    */
   public void setNormalComputeRadius (double rad) {
      if (rad < 0) {
         rad = computeDefaultNormalComputeRadius();
      }
      if (rad != myNormalComputeRadius) {
         myNormalComputeRadius = rad;
         myCurveValid = false;
      }
      myNormalComputeRadiusMode =
         PropertyUtils.propagateValue (
            this, "normalComputeRadius", rad, myNormalComputeRadiusMode);
   }

   /**
    * Queries the radius around the marker used to collect vertices for
    * estimating its surface normal. See {@link #setNormalComputeRadius} for a
    * description of how this is done.
    *
    * @return normal compute radius
    */
   public double getNormalComputeRadius () {
      return myNormalComputeRadius;
   }

   public PropertyMode getNormalComputeRadiusMode() {
      return myNormalComputeRadiusMode;
   }

   public void setNormalComputeRadiusMode (PropertyMode mode) {
      myNormalComputeRadiusMode =
         PropertyUtils.setModeAndUpdate (
            this, "normalComputeRadius", myNormalComputeRadiusMode, mode);
   }

   private double computeDefaultNormalComputeRadius () {
      return RenderableUtils.getRadius (myMeshComp)/50;
   }

   public boolean getProjectToMesh() {
      return myProjectToMesh;
   }

   public void setProjectToMesh (boolean enable) {
      if (myProjectToMesh != enable) {
         myProjectToMesh = enable;
         myCurveValid = false;
      }
   }

   // other accessors

   public PolygonalMesh getMesh() {
      return (PolygonalMesh)myMeshComp.getMesh();
   }

   public MeshComponent getMeshComp() {
      return myMeshComp;
   }

   /* --- mesh markers --- */

   public MeshMarker addMarker (Point3d pos) {
      MeshMarker mkr = new MeshMarker (myMeshComp, pos);
      myMarkers.add (mkr);
      myCurveValid = false;
      return mkr;
   }
   
   public PointList<MeshMarker> getMarkers() {
      return myMarkers;
   }

   public int numMarkers() {
      return myMarkers.size();
   }

   public boolean removeMarker (MeshMarker mkr) {
      if (myMarkers.remove (mkr)) {
         myCurveValid = false;
         return true;
      }
      else {
         return false;
      }
   }

   public void clearMarkers() {
      myMarkers.removeAll();
      myCurveValid = false;
   }

   public void updatePosition() {
      for (MeshMarker mm : myMarkers) {
         mm.updatePosition();
      }
      myCurveValid = false;
   }

   /* --- curve and curve points --- */

   /**
    * Returns the number of points (and normals) associated with this
    * MeshCurve. This number {@code n} is determined by the curve's {@code
    * resolution} property, and is computed so that in the case of linear
    * interpolation between the knot points, the distance between each point
    * will be {@code <=} resolution.
    *
    * @return number of curve points and normals
    */
   public int numPoints() {
      updateCurveIfNecessary();
      return myPoints.size();
   }

   /**
    * Returns a list of all the points on this curve. The list will have size
    * {@link numPoints}.
    *
    * @return list of all the points
    */
   public List<Point3d> getPoints() {
      updateCurveIfNecessary();
      return myPoints;
   }

   /**
    * Returns the {@code idx}-th point on this curve, where {@code idx}
    * should be in the range 0 to {@link numPoints()}-1;
    *
    * @param idx index of the desired point
    * @return {@code idx}-th point
    */
   public Vector3d getPoint (int idx) {
      return myPoints.get(idx);
   }

   /**
    * Returns a list of all the normals on this curve. The list will have size
    * {@link numPoints}.
    *
    * @return list of all the normals
    */
   public List<Vector3d> getNormals() {
      updateCurveIfNecessary();
      return myNormals;
   }

   /**
    * Returns the {@code idx}-th normal on this curve, where {@code idx}
    * should be in the range 0 to {@link numPoints()}-1;
    *
    * @param idx index of the desired normal
    * @return {@code idx}-th normal
    */
   public Vector3d getNormal (int idx) {
      return myNormals.get(idx);
   }

   /**
    * Returns the curve tangent for the {@code idx}-th point on this curve,
    * where {@code idx} should be in the range 0 to {@link numPoints()}-1; The
    * tangent is computed by numerically differencing adjacent points.  If the
    * number of curve points is less than 1, the tangent is set to 0.
    *
    * @param idx index of the point for which the tangent is desired
    * @return tangent for the {@code idx}-th point
    */
   public Vector3d getTangent (int idx) {
      Vector3d tan = new Vector3d();
      Point3d p0;
      Point3d p1;
      int nump = numPoints();
      if (nump > 1) {
         if (idx == 0) {
            if (isClosed() && nump > 2) {
               p1 = myPoints.get(1);
               p0 = myPoints.get(nump-1);
            }
            else {
               p1 = myPoints.get(1);
               p0 = myPoints.get(0);
            }       
         }
         else if (idx == nump-1) {
            if (isClosed() && nump > 2) {
               p1 = myPoints.get(0);
               p0 = myPoints.get(nump-2);
            }
            else {
               p1 = myPoints.get(nump-1);
               p0 = myPoints.get(nump-2);
            }
         }
         else {
            p1 = myPoints.get(idx+1);
            p0 = myPoints.get(idx-1);
         }
         tan.sub (p1, p0);
         tan.normalize();
      }
      return tan;      
   }

   /**
    * Finds the point on this curve that is a specified distance {@code dist}
    * from a prescribed point {@code p0}, within machine precision.  Point
    * locations on the curve are described by a non-negative parameter {@code
    * r}, which takes the form
    * <pre>
    * r = k + s
    * </pre>
    * where {@code k} is the index of a curve point (as returned by {@link
    * #getPoints()}), and {@code s} is a scalar parameter in the range [0,1]
    * that specifies the location along the interval between curve points
    * {@code k} and {@code k+1}. If the curve is open, the search for the
    * distance point begins at a location specified by {@code r0} and locations
    * before {@code r0} are ignored. If the curve is closed, {@code r0} is
    * ignored.
    *
    * <p>If found, the method will return the point's location parameter {@code
    * r}, as well as the point's value in the optional parameter {@code pr} if
    * it is not {@code null}. If the point is not found, the method will return
    * -1.
    *
    * @param pr if not {@code null}, returns the distance point, if found
    * @param p0 point with respect to which distance should be determined
    * @param dist desired distance from {@code p0}
    * @param r0 for open curves, a non-negative scalar giving the location on
    * the curve where the search should start. This should be a non-negative
    * scalar whose value is less than or equal to the number of curve points
    * (as returned by {@link #numPoints()}).
    * @return location of the distance point on the polyline, if
    * found, or -1.
    */
   public double findPointAtDistance (
      Point3d pr, Point3d p0, double dist, double r0) {

      updateCurveIfNecessary();      
      return GeometryUtils.findPointAtDistance (
         pr, myPoints, isClosed(), p0, dist, r0);
   }


   /**
    * Finds the nearest point on this curve to a prescribed point {@code
    * p0}. Locations on the curve are described by a non-negative
    * parameter {@code r}, which takes the form
    * <pre>
    * r = k + s
    * </pre>
    * 
    * where {@code k} is the index of a curve point (as returned by {@link
    * #getPoints()}), and {@code s} is a scalar parameter in the range [0,1]
    * that specifies the location along the interval between curve points
    * {@code k} and {@code k+1}. If the curve is open, the search for the
    * nearest point begins at a location specified by {@code r0} and locations
    * before {@code r0} are ignored. If the polyline is closed, {@code r0} is
    * ignored. The nearest point may not be unique.
    *
    * <p>The method will return the nearest point's location parameter {@code
    * r}, as well as the point's value in the optional parameter {@code pr} if
    * it is not {@code null}. If the curve is empty, both {@code r} and
    * {@code pr} at set to 0.
    *
    * @param pr if not {@code null}, returns the nearest point
    * @param p0 point for which the nearest point should be determined
    * @param r0 for open curves, a non-negative scalar giving the location
    * on the curve where the search should start. This should be a
    * non-negative scalar whose value is less than or equal to the number of
    * curve points (as returned by {@link #numPoints()}).
    * @return location of the nearest point on the curve
    */
   public double findNearestPoint (
      Point3d pr, Point3d p0, double r0) {

      updateCurveIfNecessary();      
      return GeometryUtils.findNearestPoint (pr, myPoints, isClosed(), p0, r0);
   }

   private void computeLinearCurve() {
      BVFeatureQuery bvq = new BVFeatureQuery();
      int numIntervals = isClosed() ? numMarkers() : numMarkers()-1;
      PolygonalMesh mesh = getMesh();
      for (int i=0; i<numIntervals; i++) {
         MeshMarker mkr0 = myMarkers.get(i);
         MeshMarker mkr1 = myMarkers.get((i+1) % numMarkers());
         Point3d pos0 = mkr0.getPosition();
         Point3d pos1 = mkr1.getPosition();
         Vector3d nrm0 = mkr0.getNormal();
         Vector3d nrm1 = mkr1.getNormal();
         int nsegs = (int)Math.ceil(pos0.distance(pos1)/getResolution());
         Point3d pos = new Point3d();
         Vector3d nrm = new Vector3d();
         mkr0.myPntIdx = myPoints.size();
         myPoints.add (new Point3d(pos0));
         myNormals.add (mkr0.getNormal());
         for (int k=1; k<nsegs; k++) {
            double s = k/(double)nsegs;
            pos.combine (1-s, pos0, s, pos1);
            nrm.combine (1-s, nrm0, s, nrm1);
            if (myProjectToMesh) {
               Face face = bvq.nearestFaceAlongLine (
                  pos, null, mesh, pos, nrm, -INF, INF);
               nrm.set (
                  mesh.estimateSurfaceNormal (pos, face, myNormalComputeRadius));
            }
            myPoints.add (new Point3d(pos));
            myNormals.add (new Vector3d (nrm));
         }
         if (!isClosed() && i == numIntervals-1) {
            myPoints.add (new Point3d(pos1));
            myNormals.add (mkr1.getNormal());
            mkr1.myPntIdx = myPoints.size()-1;
         }
      }
   }

   private void computeBSplineCurve() {
      Vector4d[] cpnts = new Vector4d[numMarkers()];
      Vector4d[] cnrms = new Vector4d[numMarkers()];
      for (int i=0; i<numMarkers(); i++) {
         Point3d pos = myMarkers.get(i).getPosition();
         Vector3d nrm = myMarkers.get(i).getNormal();
         cpnts[i] = new Vector4d(pos.x, pos.y, pos.z, 1);
         cnrms[i] = new Vector4d(nrm.x, nrm.y, nrm.z, 1);
      }
      int type = isClosed() ? NURBSCurveBase.CLOSED :  NURBSCurveBase.OPEN;
      NURBSCurve3d pcurve = new NURBSCurve3d (3, type, cpnts, null);
      NURBSCurve3d ncurve = new NURBSCurve3d (3, type, cnrms, null);
      int numIntervals = isClosed() ? numMarkers() : numMarkers()-1;
      BVFeatureQuery bvq = new BVFeatureQuery();
      PolygonalMesh mesh = getMesh();
      for (int i=0; i<numIntervals; i++) {
         MeshMarker mkr0 = myMarkers.get(i);
         MeshMarker mkr1 = myMarkers.get((i+1) % numMarkers());
         Point3d pos0 = mkr0.getPosition();
         Point3d pos1 = mkr1.getPosition();
         int nsegs = (int)Math.ceil(pos0.distance(pos1)/getResolution());
         Point3d pos = new Point3d();
         Point3d tmp = new Point3d();
         Vector3d nrm = new Vector3d();
         int npnts = nsegs;
         if (!isClosed() && i == myMarkers.size()-1) {
            npnts++;
         }
         mkr0.myPntIdx = myPoints.size();
         for (int k=0; k<npnts; k++) {
            double s = k/(double)nsegs;
            pcurve.eval (pos, i + s);
            ncurve.eval (tmp, i + s);
            nrm.set (tmp);
            nrm.normalize();
            if (myProjectToMesh) {
               Face face = bvq.nearestFaceAlongLine (
                  pos, null, mesh, pos, nrm, -INF, INF);
               nrm.set (
                  mesh.estimateSurfaceNormal (pos, face, myNormalComputeRadius));
            }
            myPoints.add (new Point3d(pos));
            myNormals.add (new Vector3d(nrm));
         }
         if (!isClosed() && i == numIntervals-1) {
            mkr1.myPntIdx = myPoints.size()-1;
         }
      }
   }

   private void computeNaturalSplineCurve() {
      ArrayList<Point3d> pnts = new ArrayList<>();
      ArrayList<Vector3d> nrms = new ArrayList<>();
      double[] svals = new double[numMarkers()];
      for (int i=0; i<numMarkers(); i++) {
         Point3d pos = myMarkers.get(i).getPosition();
         Vector3d nrm = myMarkers.get(i).getNormal();
         pnts.add (pos);
         nrms.add (nrm);
         svals[i] = i;
      }
      CubicHermiteSpline3d pcurve = new CubicHermiteSpline3d();
      CubicHermiteSpline3d ncurve = new CubicHermiteSpline3d();
      if (isClosed()) {
         pcurve.setClosed (1);
         ncurve.setClosed (1);
      }
      pcurve.setNatural (pnts, svals);
      ncurve.setNatural (nrms, svals);
      int numIntervals = isClosed() ? numMarkers() : numMarkers()-1;
      BVFeatureQuery bvq = new BVFeatureQuery();
      PolygonalMesh mesh = getMesh();
      for (int i=0; i<numIntervals; i++) {
         MeshMarker mkr0 = myMarkers.get(i);
         MeshMarker mkr1 = myMarkers.get((i+1) % numMarkers());
         Point3d pos0 = mkr0.getPosition();
         Point3d pos1 = mkr1.getPosition();
         int nsegs = (int)Math.ceil(pos0.distance(pos1)/getResolution());
         int npnts = nsegs;
         if (!isClosed() && i == myMarkers.size()-1) {
            npnts++;
         }
         mkr0.myPntIdx = myPoints.size();
         for (int k=0; k<npnts; k++) {
            double s = k/(double)nsegs;
            Point3d pos = new Point3d(pcurve.eval (i + s, new IntHolder(i)));
            Vector3d nrm = ncurve.eval (i + s, new IntHolder(i));
            nrm.normalize();
            if (myProjectToMesh) {
               Face face = bvq.nearestFaceAlongLine (
                  pos, null, mesh, pos, nrm, -INF, INF);
               nrm.set (
                  mesh.estimateSurfaceNormal (pos, face, myNormalComputeRadius));
            }
            myPoints.add (new Point3d(pos));
            myNormals.add (new Vector3d(nrm));
         }
         if (!isClosed() && i == numIntervals-1) {
            mkr1.myPntIdx = myPoints.size()-1;
         }
      }
   }

   public void updateCurveIfNecessary() {
      if (!myCurveValid) {
         myPoints.clear();
         myNormals.clear();
         if (numMarkers() > 1) {
            switch (myInterpolation) {
               case LINEAR: {
                  computeLinearCurve();
                  break;
               }
               case B_SPLINE: {
                  computeBSplineCurve();
                  break;
               }
               case NATURAL_SPLINE: {
                  computeNaturalSplineCurve();
                  break;
               }
               default: {
                  throw new UnsupportedOperationException (
                     "Unimplement interpolation mode "+myInterpolation);
               }
            }
            RigidTransform3d TMW = getMesh().getMeshToWorld();
            for (Point3d p : myPoints) {
               p.inverseTransform (TMW);
            }
         }
         myCurveValid = true;
      }
   }

   public void componentChanged (ComponentChangeEvent e) {
      super.componentChanged(e);
      if (e instanceof GeometryChangeEvent) {
         myCurveValid = false;
      }
      else if (e instanceof StructureChangeEvent) {
         myCurveValid = false;
      }
   }

   // renderable methods

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void prerender (RenderList list) {
      updateCurveIfNecessary();
      myRob = buildRenderObject (getRenderProps());
      myRenderFrame.set (getMesh().getMeshToWorld());
      list.addIfVisible (myMarkers);
   }

   protected RenderObject buildRenderObject (RenderProps props) {

      RenderObject r = new RenderObject();
      r.createLineGroup();
      for (int i=0; i<myPoints.size(); i++) {
         Point3d pos = myPoints.get(i);
         r.addPosition((float)pos.x, (float)pos.y, (float)pos.z);
         r.addVertex (i);
         if (i > 0) {
            r.addLine (i-1, i);
         }
      }
      if (isClosed()) {
         r.addLine (myPoints.size()-1, 0);
      }

      if (myNormalLength > 0) {
         int vbase = myPoints.size();
         r.createLineGroup();
         r.lineGroup (NORMAL_GRP);
         PolygonalMesh mesh = getMesh();
         Point3d pos1 = new Point3d();
         for (int i=0; i<myPoints.size(); i++) {
            Point3d pos0 = myPoints.get(i);
            Vector3d nrm = myNormals.get(i);
            pos1.scaledAdd (myNormalLength, nrm, pos0);
            r.addPosition((float)pos0.x, (float)pos0.y, (float)pos0.z);
            r.addVertex (vbase+2*i);
            r.addPosition((float)pos1.x, (float)pos1.y, (float)pos1.z);
            r.addVertex (vbase+2*i+1);
            r.addLine (vbase+2*i, vbase+2*i+1);
         }
      }
      return r;
   }

   public void render (Renderer renderer, int flags) {
      RenderObject rob = myRob;
      if (rob != null) {
         RenderProps props = getRenderProps();
         renderer.pushModelMatrix();
         renderer.mulModelMatrix (myRenderFrame);

         float savedLineWidth = renderer.getLineWidth();
         Shading savedShadeModel = renderer.getShading();
         LineStyle lineStyle = props.getLineStyle();
         Shading savedShading = renderer.getShading();

         if (lineStyle == LineStyle.LINE) {
            renderer.setShading (Shading.NONE);
         }
         else {
            renderer.setShading (props.getShading());
         }
         renderer.setLineColoring (props, isSelected());
         switch (lineStyle) {
            case LINE: {
               int width = props.getLineWidth();
               if (width > 0) {
                  renderer.drawLines (rob, CURVE_GRP, LineStyle.LINE, width);
               }
               break;
            }
            case SPINDLE:
            case SOLID_ARROW:
            case CYLINDER: {
               double rad = props.getLineRadius();
               if (rad > 0) {
                  renderer.drawLines (rob, CURVE_GRP, props.getLineStyle(), rad);
               }
               break;
            }
         }
         renderer.setShading (savedShading);
         renderer.setLineWidth (savedLineWidth);
         renderer.setShading (savedShadeModel);
         renderer.popModelMatrix();         
      }
      if (rob.numLineGroups() > 1) {
         renderer.setColor (Color.BLUE);
         renderer.drawLines (
            rob, NORMAL_GRP, LineStyle.SOLID_ARROW, myNormalLength/40);
      }
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      for (MeshMarker mm : myMarkers) {
         mm.updateBounds (pmin, pmax);
      }
   }

   // I/O methods

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReference (rtok, "meshComp", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "meshComp")) {
         myMeshComp = 
            postscanReference (tokens, MeshComponent.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println (
         "meshComp="+ComponentUtils.getWritePathName (ancestor,myMeshComp));
   }
   
   @Override
   public MeshCurve copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      MeshCurve curve = (MeshCurve)super.copy (flags, copyMap);

      curve.initializeChildComponents();
      if (copyMap != null) {
         curve.myMeshComp = (MeshComponent)copyMap.get(myMeshComp);
         copyMap.put (this, curve);
      }
      if (curve.myMeshComp == null) {
         curve.myMeshComp = myMeshComp;
      }
      for (int i=0; i<myMarkers.size(); i++) {
         curve.myMarkers.add (myMarkers.get(i).copy (flags, copyMap));
      }     
      curve.myCurveValid = false;
      return curve;
   }

}
      
