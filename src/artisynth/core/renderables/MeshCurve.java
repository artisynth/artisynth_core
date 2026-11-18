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
public class MeshCurve extends RenderableCompositeBase
   implements IsLineComponent, LineIntersectable {

   public static final double INF = Double.POSITIVE_INFINITY;

   /**
    * Defines a position at a specified location on a mesh curve.
    */
   protected class MeshCurvePosition implements HasPosition {
      private MeshCurve myCurve; // curve containing the position
      private double myR0; // point location on curve when position created
      private int myNumPoints0; // number of curve points when position created


      /**
       * Creates a MeshCurvePosition.
       *  
       * @param curve mesh curve containing the position
       * @param r location of the curve with respect to the points
       */
      public MeshCurvePosition (MeshCurve curve, double r) {
         myCurve = curve;
         myR0 = r;
         myNumPoints0 = curve.numPoints();
      }

      /**
       * {@inheritDoc}
       */
      public Point3d getPosition() {
         int nump = myCurve.numPoints();
         double r = myR0;
         if (nump != myNumPoints0) {
            // scale r to conform to new number of points
            r = (myR0*nump)/myNumPoints0;
         }
         // compute the interpolated position
         Point3d pos = new Point3d();
         myCurve.findPointAtLocation (pos, r);
         return pos;
      }
   }  
   
   
   protected MeshComponent myMeshComp = null;
   protected PointList<MeshMarker> myMarkers;
   protected boolean myCurveValid = false;
   protected ArrayList<Point3d> myLocalPoints;
   protected DynamicDoubleArray myArcLengths;
   protected double myLength; // arc-length estimate based on points
   // indices of the first point for each interval
   protected int[] myIntervalPointIndices; 
   protected ArrayList<Vector3d> myLocalNormals;
   private RigidTransform3d myRenderFrame;

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

   /**
    * Returns the mesh-to-world transform, or null if this transform is the
    * identity.
    */
   private RigidTransform3d getTMW() {
      if (!getMesh().meshToWorldIsIdentity()) {
         return getMesh().getMeshToWorld();
      }
      else {
         return null;
      }
   }

   private void transformToWorld (Point3d p) {
      if (!getMesh().meshToWorldIsIdentity()) {
         p.transform (getMesh().getMeshToWorld());
      }
   }

   private void transformToMesh (Point3d p) {
      if (!getMesh().meshToWorldIsIdentity()) {
         p.inverseTransform (getMesh().getMeshToWorld());
      }
   }

   private void transformToWorld (Vector3d v) {
      if (!getMesh().meshToWorldIsIdentity()) {
         v.transform (getMesh().getMeshToWorld());
      }
   }

   private void transformToMesh (Vector3d v) {
      if (!getMesh().meshToWorldIsIdentity()) {
         v.inverseTransform (getMesh().getMeshToWorld());
      }
   }

   protected void initializeChildComponents() {
      myLocalPoints = new ArrayList<>();
      myArcLengths = new DynamicDoubleArray();
      myLocalNormals = new ArrayList<>();
      myMarkers = new PointList<> (MeshMarker.class, "markers");
      myRenderFrame = new RigidTransform3d();
      myRob = null;
      myIntervalPointIndices = null;
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

   public void updateMarkerPositions() {
      for (MeshMarker mm : myMarkers) {
         mm.updateState();
      }
      myCurveValid = false;
   }

   /* --- curve and curve points --- */

   /**
    * Returns the number of points (and normals) associated with this
    * MeshCurve. This number {@code n} is determined by the curve's {@code
    * resolution} property. Each interval between adjacent markers is divided
    * into segments whose length, measured along the interpolating curve
    * (computed in 3D space), is {@code <=} resolution. For spline
    * interpolation, interval lengths are estimated numerically and the
    * segments are spaced approximately uniformly in arc length. If the {@code
    * projectToMesh} property is {@code true}, the points are subsequently
    * projected onto the mesh surface, which may increase their spacing
    * somewhat beyond the resolution, particularly on convex surfaces.
    * Intervals of zero length (such as between coincident markers) contribute
    * no points.
    *
    * @return number of curve points and normals
    */
   public int numPoints() {
      updateCurveIfNecessary();
      return myLocalPoints.size();
   }

   /**
    * Returns a list of all the points on this curve, in local mesh
    * coordinate. The list will have size {@link numPoints}, and should not be
    * modified.
    *
    * @return list of all the points
    */
   public List<Point3d> getLocalPoints() {
      updateCurveIfNecessary();
      return myLocalPoints;
   }

   /**
    * Returns the total length of this curve, as approximated by the sum of the
    * distances between all points.
    * 
    * @return length of the curve
    */
   public double getLength() {
      updateCurveIfNecessary();
      return myLength;
   }
   
   /**
    * Returns a list of all the points on this curve, in world coordinates. The
    * list will have size {@link numPoints}.
    *
    * @return list of all the points
    */
   public List<Point3d> getPoints() {
      updateCurveIfNecessary();
      ArrayList<Point3d> list = new ArrayList<Point3d>();
      RigidTransform3d TMW = getTMW();
      for (Point3d p : myLocalPoints) {
         Point3d pw = new Point3d(p);
         if (TMW != null) {
            pw.transform (TMW);
         }
         list.add (pw);
      }
      return list;
   }

   /**
    * Returns the {@code idx}-th point on this curve, in world coordinates,
    * where {@code idx} should be in the range 0 to {@link #numPoints()}-1;
    *
    * @param idx index of the desired point
    * @return {@code idx}-th point
    */
   public Point3d getPoint (int idx) {
      updateCurveIfNecessary();
      Point3d pnt = new Point3d(myLocalPoints.get(idx));
      transformToWorld (pnt);
      return pnt;
   }
   
   /**
    * Returns the point position on this curve at a specified arc-length 
    * {@code len}, in world coordinates. If the curve is open, {@code len}
    * is clipped to the range [0, {@link #getLength()}]. If the curve is
    * closed, {@code len} is wrapped, using the modulo function, to the range
    * [0, {@link #getLength()}), so that negative values or values exceeding
    * the length continue around the curve.
    * 
    * @param len arc-length along the curve
    * @return interpolated point at the specified arc-length, or {@code null}
    * if the curve has no points
    */
   public Point3d getPointAt (double len) {
      updateCurveIfNecessary();
      len = clipOrWrapArcLength (len);
      Point3d pnt = new Point3d();
      int npts = myLocalPoints.size();
      if (npts == 0) {
         return null;
      }
      double[] sval = new double[1];
      int k = findArcLengthInterval (len, sval);
      double s = sval[0];
      pnt.combine (
         1-s, myLocalPoints.get(k), s, myLocalPoints.get((k+1)%npts));
      transformToWorld (pnt);
      return pnt;
   }

   /**
    * Clips an arc-length to [0, myLength] for open curves, or wraps it to
    * [0, myLength) for closed curves.
    */
   private double clipOrWrapArcLength (double len) {
      if (isClosed() && myLength > 0) {
         len -= Math.floor (len/myLength)*myLength;
         if (len >= myLength) {
            // guard against roundoff for small negative len
            len = 0;
         }
      }
      else if (len < 0) {
         len = 0;
      }
      else if (len > myLength) {
         len = myLength;
      }
      return len;
   }

   /**
    * Finds the interval {@code k} of the curve containing the arc-length
    * {@code len}, along with the parameter {@code s} in {@code [0,1]} giving
    * the location within that interval. The interval lies between points
    * {@code k} and {@code (k+1)%numPoints()}. Assumes that the curve is
    * valid, contains at least one point, and that {@code len} lies within
    * {@code [0, myLength]}.
    *
    * @param len arc-length along the curve
    * @param sval returns the interval parameter {@code s} in element 0
    * @return interval index {@code k}
    */
   private int findArcLengthInterval (double len, double[] sval) {
      double[] alens = myArcLengths.getArray();
      int nint = myArcLengths.size()-1; // number of intervals
      if (nint <= 0) {
         sval[0] = 0;
         return 0;
      }
      // binary search for k such that alens[k] <= len <= alens[k+1]
      int lo = 0;
      int hi = nint;
      while (hi - lo > 1) {
         int mid = (lo+hi)/2;
         if (alens[mid] <= len) {
            lo = mid;
         }
         else {
            hi = mid;
         }
      }
      double ilen = alens[lo+1]-alens[lo];
      if (ilen > 0) {
         sval[0] = Math.max (0, Math.min (1, (len-alens[lo])/ilen));
      }
      else {
         // zero-length interval (coincident points)
         sval[0] = 0;
      }
      return lo;
   }
   
   /**
    * Returns the arc-length at the {@code idx}-th point on this curve, where
    * {@code idx} should be in the range {@code [0, maxIdx]}, where {@code
    * maxIdx} is {@link #numPoints()}-1 if the curve is open {@link
    * #numPoints()} if it is closed.
    *
    * @param idx index of the point for which arc-length is desired.
    * @return arc-length at the {@code idx}-th point
    */
   public double getArcLength (int idx) {
      updateCurveIfNecessary();
      int maxIdx = isClosed() ? numPoints() : numPoints()-1;
      if (idx < 0 || idx > maxIdx) {
         throw new IllegalArgumentException (
            "idx must be in the range [0, "+maxIdx+"]");
      }
      return myArcLengths.get(idx);
   }

   /**
    * Returns a list of all the normals on this curve, in local mesh
    * coordinates. The list will have size {@link numPoints}, and should not be
    * modified.
    *
    * @return list of all the normals
    */
   public List<Vector3d> getLocalNormals() {
      updateCurveIfNecessary();
      return myLocalNormals;
   }

   /**
    * Returns a list of all the normals on this curve, in world
    * coordinates. The list will have size {@link numPoints}.
    *
    * @return list of all the normals
    */
   public List<Vector3d> getNormals() {
      updateCurveIfNecessary();
      ArrayList<Vector3d> list = new ArrayList<Vector3d>();
      RigidTransform3d TMW = getTMW();
      for (Vector3d n : myLocalNormals) {
         Vector3d nw = new Vector3d(n);
         if (TMW != null) {
            nw.transform (TMW);
         }
         list.add (nw);
      }
      return list;
   }

   /**
    * Returns the {@code idx}-th normal on this curve, in world coordinates,
    * where {@code idx} should be in the range 0 to {@link #numPoints()}-1;
    *
    * @param idx index of the desired normal
    * @return {@code idx}-th normal
    */
   public Vector3d getNormal (int idx) {
      updateCurveIfNecessary();
      Vector3d nrm = new Vector3d(myLocalNormals.get(idx));
      transformToWorld (nrm);
      return nrm;
   }
   
   /**
    * Returns the normal on this curve at a specified arc-length 
    * {@code len}, in world coordinates. If the curve is open, {@code len}
    * is clipped to the range [0, {@link #getLength()}]. If the curve is
    * closed, {@code len} is wrapped, using the modulo function, to the range
    * [0, {@link #getLength()}), so that negative values or values exceeding
    * the length continue around the curve.
    * 
    * @param len arc-length along the curve
    * @return interpolated normal at the specified arc-length, or {@code null}
    * if the curve has no points
    */
   public Vector3d getNormalAt (double len) {
      updateCurveIfNecessary();
      len = clipOrWrapArcLength (len);
      Vector3d nrm = new Vector3d();
      int npts = myLocalNormals.size();
      if (npts == 0) {
         return null;
      }
      double[] sval = new double[1];
      int k = findArcLengthInterval (len, sval);
      double s = sval[0];
      Vector3d n0 = myLocalNormals.get(k);
      Vector3d n1 = myLocalNormals.get((k+1)%npts);
      nrm.combine (1-s, n0, s, n1);
      double mag = nrm.norm();
      if (mag < 1e-8*(n0.norm()+n1.norm())) {
         // adjacent normals are opposed; use the nearer one
         nrm.set (s < 0.5 ? n0 : n1);
         nrm.normalize();
      }
      else {
         nrm.scale (1/mag);
      }
      transformToWorld (nrm);
      return nrm;
   }

   /**
    * Returns the curve tangent for the {@code idx}-th point on this curve,
    * where {@code idx} should be in the range 0 to {@link #numPoints()}-1; The
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
               p1 = myLocalPoints.get(1);
               p0 = myLocalPoints.get(nump-1);
            }
            else {
               p1 = myLocalPoints.get(1);
               p0 = myLocalPoints.get(0);
            }       
         }
         else if (idx == nump-1) {
            if (isClosed() && nump > 2) {
               p1 = myLocalPoints.get(0);
               p0 = myLocalPoints.get(nump-2);
            }
            else {
               p1 = myLocalPoints.get(nump-1);
               p0 = myLocalPoints.get(nump-2);
            }
         }
         else {
            p1 = myLocalPoints.get(idx+1);
            p0 = myLocalPoints.get(idx-1);
         }
         tan.sub (p1, p0);
         tan.normalize();
      }
      if (!getMesh().meshToWorldIsIdentity()) {
         tan.transform (getMesh().getMeshToWorld());
      }
      return tan;      
   }

   /**
    * Finds the point on this curve that is a specified distance {@code dist}
    * from a prescribed point {@code p0}, within machine precision. Equivalent
    * to calling {@link #findPointAtDistance(Point3d,Point3d,double,double,
    * boolean)} with {@code reverse} set to {@code false}.
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
      return findPointAtDistance (pr, p0, dist, r0, false);
   }

   /**
    * Finds the point on this curve that is a specified distance {@code dist}
    * from a prescribed point {@code p0}, within machine precision.  The method
    * also returns the location of the point. Point locations on the curve are
    * described by a non-negative parameter {@code r}, which takes the form
    * <pre>
    * r = k + s
    * </pre>
    * where {@code k} is the index of a curve point (as returned by {@link
    * #getPoints()}), and {@code s} is a scalar parameter in the range [0,1]
    * that specifies the location along the interval between curve points
    * {@code k} and {@code k+1}. If the curve is open, the search for the
    * distance point begins at a location specified by {@code r0}. By default,
    * the search proceeds forward (toward larger {@code r}) and locations
    * before {@code r0} are ignored. If {@code reverse} is {@code true}, the
    * search instead proceeds backward (toward smaller {@code r}) and
    * locations after {@code r0} are ignored; if the search reaches the
    * beginning of the curve without finding a point at distance {@code dist}
    * from {@code p0}, -1 is returned. If the curve is closed, {@code r0} and
    * {@code reverse} are both ignored.
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
    * @param reverse if {@code true} and the curve is open, search backward
    * from {@code r0} instead of forward
    * @return location of the distance point on the polyline, if
    * found, or -1.
    */
   public double findPointAtDistance (
      Point3d pr, Point3d p0, double dist, double r0, boolean reverse) {

      updateCurveIfNecessary();
      Point3d p0loc = new Point3d(p0);
      transformToMesh (p0loc);
      double r = GeometryUtils.findPointAtDistance (
         pr, myLocalPoints, isClosed(), p0loc, dist, r0, reverse);
      if (pr != null) {
         transformToWorld (pr);
      }
      return r;
   }

   /**
    * Finds the nearest point on this curve to a prescribed point {@code
    * p0}.The method also returns the point's location. Locations on the curve
    * are described by a non-negative parameter {@code r}, which takes the form
    * <pre>
    * r = k + s
    * </pre>
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
      Point3d p0loc = new Point3d(p0);
      transformToMesh (p0loc);
      double r = GeometryUtils.findNearestPoint (
         pr, myLocalPoints, isClosed(), p0loc, r0);
      if (pr != null) {
         transformToWorld (pr);
      }
      return r;
   }

   /**
    * Finds the index of the nearest marker interval on this curve to a
    * prescribed point {@code p0}. This is defined as the interval containing
    * the nearest point to {@code p0}.
    *
    * <p>If the curve is open, then there are {@code numi = numMarkers()-1}
    * intervals. If the nearest point is either the first point or last point,
    * then {@code p0} is considered to lie <i>before</i> or <i>after</i> the
    * intervals and the method returns {@code -1} or {@code numi},
    * respectively.  Otherwise, the nearest interval index in the range {@code
    * 0} to {@code numi-1} is returned.
    *
    * <p>If the curve is closed, then the number of intervals is {@code numi =
    * numMarkers()}, and the method returns the nearest interval index in the
    * range {@code 0} to {@code numi-1}.
    *
    * <p>In all cases, the nearest interval may not be unique, in which
    * case the interval with lowest index will be returned.
    *
    * @param p0 point for which the nearest point should be determined
    * @return index of the nearest interval
    */
   public int findNearestMarkerInterval (Point3d p0) {
      double r = findNearestPoint (null, p0, 0);
      int iidx = -1; // interval index
      for (int k=0; k<myIntervalPointIndices.length; k++) {
         if (r > myIntervalPointIndices[k]) {
            iidx++;
         }
      }
      if (isClosed()) {
         if (iidx == -1) {
            iidx = numMarkers()-1;
         }
      }
      else {
         if (r == numPoints()-1) {
            iidx++;
         }
      }
      return iidx;
   }

   /**
    * Finds a point at a specific location on this curve, in world coordinates.
    * The location is described by a non-negative parameter {@code r}, which
    * takes the form
    * <pre>
    * r = k + s
    * </pre>
    * where {@code k} is the index of a curve point (as returned by {@link
    * #getPoints()}), and {@code s} is a scalar parameter that specifies the
    * location along the interval between curve points {@code k} and {@code
    * k+1}. If the curve is open, {then {@code r} is clamped so as to not 
    * exceed {@code npts-1}, where {@code npts} is the number of curve points.
    * If the polyline is closed, then {@code r} is reduced, using the modulo 
    * function, to the range {@code [0, npts)}.
    *
    * @param pr returns the located point, in world coordinates
    * @param r specifies the location on the curve
    * @throws IllegalArgumentException if {@code r} is negative
    */
   public void findPointAtLocation (Point3d pr, double r) {
      updateCurveIfNecessary();      
      Point3d ploc = GeometryUtils.getPointAt (r, myLocalPoints, isClosed());
      pr.set (ploc);
      transformToWorld (pr);
   }

   // number of sub-intervals used to tabulate arc length along a spline
   // interval, for estimating its length and for arc-length sampling
   private static final int NUM_LENGTH_SAMPLES = 32;

   // if true, spline intervals are sampled uniformly in arc length;
   // otherwise, they are sampled uniformly in the curve parameter
   private boolean myArcLengthSampling = true;

   /**
    * Returns the number of segments into which a curve interval of estimated
    * length {@code ilen} should be divided, based on the curve resolution.
    * Returns 0 if the interval is degenerate (zero length to within
    * roundoff), which can occur when adjacent markers coincide. No points
    * are generated for degenerate intervals, so that the curve will not
    * contain coincident adjacent points.
    *
    * @param ilen estimated length of the interval
    * @return number of segments for the interval
    */
   private int numIntervalSegments (double ilen) {
      double res = getResolution();
      if (ilen <= 1e-10*res) {
         return 0;
      }
      return Math.max (1, (int)Math.ceil (ilen/res));
   }

   /**
    * Computes the parameter values at which to sample interval {@code i} of
    * a parametric curve whose intervals are delimited by integer parameter
    * values. The number of samples is determined from the interval's length,
    * estimated by tabulating arc length at {@code NUM_LENGTH_SAMPLES}
    * uniformly spaced parameter values. If {@code myArcLengthSampling} is
    * {@code true}, the samples are spaced uniformly in arc length, by
    * inverting this table with linear interpolation; otherwise, they are
    * spaced uniformly in the curve parameter.
    *
    * <p>The returned values lie in [0,1) and give offsets from {@code i}. If
    * {@code addEnd} is {@code true}, a final value of 1 is appended. If the
    * interval is degenerate (see {@link #numIntervalSegments}), no values
    * are returned except for the optional end value.
    *
    * @param curve evaluates the curve at a given parameter value
    * @param i index of the interval
    * @param addEnd if {@code true}, append a sample at the end of the interval
    * @return parameter offsets at which to sample the interval
    */
   private double[] computeIntervalSamples (
      java.util.function.DoubleFunction<Point3d> curve, int i,
      boolean addEnd) {

      // tabulate cumulative arc length at uniformly spaced parameters
      int m = NUM_LENGTH_SAMPLES;
      double[] cumlen = new double[m+1];
      Point3d prev = curve.apply (i);
      for (int j=1; j<=m; j++) {
         Point3d pnt = curve.apply (i + j/(double)m);
         cumlen[j] = cumlen[j-1] + pnt.distance (prev);
         prev = pnt;
      }
      double len = cumlen[m];
      int nsegs = numIntervalSegments (len);
      double[] svals = new double[addEnd ? nsegs+1 : nsegs];
      int j = 0;
      for (int k=0; k<nsegs; k++) {
         if (myArcLengthSampling) {
            // find s such that the arc length at s is k*len/nsegs
            double target = k*len/nsegs;
            while (j < m-1 && cumlen[j+1] <= target) {
               j++;
            }
            double dl = cumlen[j+1]-cumlen[j];
            double t = (dl > 0 ? (target-cumlen[j])/dl : 0);
            svals[k] = (j + Math.max (0, Math.min (1, t)))/m;
         }
         else {
            svals[k] = k/(double)nsegs;
         }
      }
      if (addEnd) {
         svals[nsegs] = 1.0;
      }
      return svals;
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
         int nsegs = numIntervalSegments (pos0.distance(pos1));
         Point3d pos = new Point3d();
         Vector3d nrm = new Vector3d();
         // if the interval is degenerate, no points are added and mkr0
         // shares the first point of the next interval
         mkr0.myPntIdx = myLocalPoints.size();
         myIntervalPointIndices[i] = myLocalPoints.size();
         if (nsegs > 0) {
            myLocalPoints.add (new Point3d(pos0));
            myLocalNormals.add (nrm0);
         }
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
            myLocalPoints.add (new Point3d(pos));
            myLocalNormals.add (new Vector3d (nrm));
         }
         if (!isClosed() && i == numIntervals-1) {
            myLocalPoints.add (new Point3d(pos1));
            myLocalNormals.add (mkr1.getNormal());
            mkr1.myPntIdx = myLocalPoints.size()-1;
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
      java.util.function.DoubleFunction<Point3d> peval = (r) -> {
         Point3d p = new Point3d();
         pcurve.eval (p, r);
         return p;
      };
      for (int i=0; i<numIntervals; i++) {
         MeshMarker mkr0 = myMarkers.get(i);
         MeshMarker mkr1 = myMarkers.get((i+1) % numMarkers());
         // B-spline intervals need not have zero length when adjacent
         // markers coincide, so sampling is based on the curve itself
         boolean addEnd = (!isClosed() && i == numIntervals-1);
         double[] svals = computeIntervalSamples (peval, i, addEnd);
         Point3d pos = new Point3d();
         Point3d tmp = new Point3d();
         Vector3d nrm = new Vector3d();
         mkr0.myPntIdx = myLocalPoints.size();
         myIntervalPointIndices[i] = myLocalPoints.size();
         for (double s : svals) {
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
            myLocalPoints.add (new Point3d(pos));
            myLocalNormals.add (new Vector3d(nrm));
         }
         if (addEnd) {
            mkr1.myPntIdx = myLocalPoints.size()-1;
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
         final int ii = i;
         // the spline may loop between coincident adjacent markers, so
         // sampling is based on the curve itself
         boolean addEnd = (!isClosed() && i == numIntervals-1);
         double[] ivals = computeIntervalSamples (
            (r) -> new Point3d (pcurve.eval (r, new IntHolder(ii))), i, addEnd);
         mkr0.myPntIdx = myLocalPoints.size();
         myIntervalPointIndices[i] = myLocalPoints.size();
         for (double s : ivals) {
            Point3d pos = new Point3d(pcurve.eval (i + s, new IntHolder(i)));
            Vector3d nrm = ncurve.eval (i + s, new IntHolder(i));
            nrm.normalize();
            if (myProjectToMesh) {
               Face face = bvq.nearestFaceAlongLine (
                  pos, null, mesh, pos, nrm, -INF, INF);
               nrm.set (
                  mesh.estimateSurfaceNormal (pos, face, myNormalComputeRadius));
            }
            myLocalPoints.add (new Point3d(pos));
            myLocalNormals.add (new Vector3d(nrm));
         }
         if (addEnd) {
            mkr1.myPntIdx = myLocalPoints.size()-1;
         }
      }
   }

   public void updateCurveIfNecessary() {
      if (!myCurveValid) {
         myLocalPoints.clear();
         myArcLengths.clear();
         myLocalNormals.clear();
         myLength = 0;
         if (numMarkers() > 1) {
            int numi = isClosed() ? numMarkers() : numMarkers()-1;
            myIntervalPointIndices = new int[numi];
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
            int npts = myLocalPoints.size();
            if (npts == 0) {
               // all intervals are degenerate because all markers coincide
               MeshMarker mkr = myMarkers.get(0);
               myLocalPoints.add (new Point3d(mkr.getPosition()));
               myLocalNormals.add (mkr.getNormal());
               for (MeshMarker m : myMarkers) {
                  m.myPntIdx = 0;
               }
               Arrays.fill (myIntervalPointIndices, 0);
            }
            else if (isClosed()) {
               // markers whose trailing intervals were degenerate at the end
               // of a closed curve share the first point
               for (MeshMarker m : myMarkers) {
                  if (m.myPntIdx == npts) {
                     m.myPntIdx = 0;
                  }
               }
            }
            RigidTransform3d TMW = getTMW();
            double len = 0;
            Point3d prev = null;
            for (Point3d p : myLocalPoints) {
               if (TMW != null) {
                  p.inverseTransform (TMW);
               }
               if (prev != null) {
                  len += p.distance(prev);
               }
               myArcLengths.add (len);
               prev = p;
            }
            if (isClosed()) {
               len += prev.distance(myLocalPoints.get(0));
               myArcLengths.add (len);
            }
            myLength = len;
            if (TMW != null) {
               for (Vector3d n : myLocalNormals) {
                  n.inverseTransform (TMW);
               }
            }
         }
         else {
            myIntervalPointIndices = new int[0];
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
      for (int i=0; i<myLocalPoints.size(); i++) {
         Point3d pos = myLocalPoints.get(i);
         r.addPosition((float)pos.x, (float)pos.y, (float)pos.z);
         r.addVertex (i);
         if (i > 0) {
            r.addLine (i-1, i);
         }
      }
      if (isClosed()) {
         r.addLine (myLocalPoints.size()-1, 0);
      }

      if (myNormalLength > 0) {
         int vbase = myLocalPoints.size();
         r.createLineGroup();
         r.lineGroup (NORMAL_GRP);
         PolygonalMesh mesh = getMesh();
         Point3d pos1 = new Point3d();
         for (int i=0; i<myLocalPoints.size(); i++) {
            Point3d pos0 = myLocalPoints.get(i);
            Vector3d nrm = myLocalNormals.get(i);
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
         if (rob.numLineGroups() > 1) {
            renderer.setColor (Color.BLUE);
            renderer.drawLines (
               rob, NORMAL_GRP, LineStyle.SOLID_ARROW, myNormalLength/40);
         }
         renderer.setShading (savedShading);
         renderer.setLineWidth (savedLineWidth);
         renderer.setShading (savedShadeModel);
         renderer.popModelMatrix();         
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
   public void connectToHierarchy (CompositeComponent hcomp) {
      super.connectToHierarchy (hcomp);
      if (myMeshComp != null) {
         CompositeComponent gparent = ComponentUtils.getGrandParent(this);
         if (gparent != myMeshComp) {
            // curve is not a child of the mesh and needs to be registered as
            // an external curve
            if (ComponentUtils.areConnectedVia (this, myMeshComp, hcomp)) {
               myMeshComp.addExternalCurve (this);
            }
         }
      }
   }

   @Override
   public void disconnectFromHierarchy(CompositeComponent hcomp) {
      super.disconnectFromHierarchy(hcomp);
      if (myMeshComp != null) {
         CompositeComponent gparent = ComponentUtils.getGrandParent(this);
         if (gparent != myMeshComp) {
            // curve is not a child of the mesh; deregister external curve
            if (ComponentUtils.areConnectedVia (this, myMeshComp, hcomp)) {
               myMeshComp.removeExternalCurve (this);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void getHardReferences (List<ModelComponent> refs) {
      if (ComponentUtils.getGrandParent(this) != myMeshComp) {
         refs.add (myMeshComp);
      }
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
   
   /* ---- LineIntersectable implementation ---- */

   /**
    * {@inheritDoc}
    */
   public MeshCurvePosition nearestPointToLine (Line line) {

      updateCurveIfNecessary();
      if (numPoints() == 0) {
         return null;
      }
      if (!getMesh().meshToWorldIsIdentity()) {
         // transform line to local coordinates if necessary
         line = new Line(line);
         line.inverseTransform (getMesh().getMeshToWorld());
      }

      // find the nearest curve point to the line.
      int nearIdx0 = -1;
      double nearDist = INF;
      for (int k=0; k<numPoints(); k++) {
         Point3d p = myLocalPoints.get(k);
         double d = line.distance(p);
         if (d < nearDist) {
            nearDist = d; 
            nearIdx0 = k;
         }
      }
      // now check the segments adjacent to the nearest curve point and find
      // the one which is nearest to the line
      nearDist = INF;
      double loc = 0; // location of the nearest segment
      DoubleHolder segParam = new DoubleHolder();
      if (nearIdx0 > 0) {
         double d = line.distanceToSegment (
            segParam, myLocalPoints.get(nearIdx0-1), myLocalPoints.get(nearIdx0));
         if (d < nearDist) {
            nearDist = d;
            loc = nearIdx0-1+segParam.value;
         }
      }
      if (nearIdx0 < numPoints()-1) {
         double d = line.distanceToSegment (
            segParam, myLocalPoints.get(nearIdx0), myLocalPoints.get(nearIdx0+1));
         if (d < nearDist) {
            nearDist = d;
            loc = nearIdx0+segParam.value;
         }
      }
      return new MeshCurvePosition (this, loc);
   }      

}
      
