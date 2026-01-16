/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

import artisynth.core.materials.AxialMaterial;
import artisynth.core.materials.LinearAxialMaterial;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.mechmodels.HasSlaveObjects;
import artisynth.core.modelbase.HasNumericStateComponents;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import artisynth.core.driver.Main;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.LineSegment;
import maspack.geometry.OBB;
import maspack.geometry.DistanceGrid;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.properties.PropertyUtils;
import maspack.render.PointRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.render.Renderer.PointStyle;
import maspack.util.Clonable;
import maspack.util.DataBuffer;
import maspack.util.DoubleHolder;
import maspack.util.DynamicIntArray;
import maspack.util.FunctionTimer;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.ListRemove;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * Multi-segment point-based spring that supports supports wrapping of selected
 * segments.
 *
 * <p>A multipoint spring is divided into one or more <i>segments</i>, each
 * separated by a single point and represented by a {@link Segment}.
 *
 * <p>Segments may optionally by made <i>wrappable</i>, allowing them to wrap
 * around any of the {@link Wrappable} objects known the spring. Wrappable
 * segments are represented by the {@link WrapSegment} subclass of {@link
 * Segment}. Each wrappable segment is implemented using a prescribed number of
 * knot points (typically 50-100), which are drawn tightly around the wrappable
 * objects using fictitious <i>wrap</i> forces which pull them together and
 * <i>contact</i> forces that repel them from the wrappables. The process of
 * determining the wrap path is done using a Newton-based iteration inside the
 * {@link WrapSegment#updateWrapStrand} method of {@link WrapSegment}.
 *
 * <p>When a WrapSegment makes contact with one or more wrappables, is
 * dynamically partitioned into two or more subsegments separating the
 * contacting wrappables and the spring start and end points. More
 * specifically, assume that the segment is in contact with n
 * wrappables, identified by W_j for {@code 0 <= j < n}. Then
 * the segment will be divided into n + 1 subsegments, where 
 * 
 * <ul>
 * <li>The first subsegment describes the line segment between the
 * spring starting point and the first contact point on W_0;</li>
 * 
 * <li>The intermediate subsegment<i>i</i> describes the line segment between
 * the last contact point on W_{i-1} and the first contact point on W_i;</li>
 *
 * <li>The last subsegment describes the line segment between the
 * last contact point on W_{n-1} and the spring end point.</li>
 * </ul>
 *
 * <p>During simulation, forces are imparted from the spring to each contacting
 * wrappable via A/B points, which represent the locations at which the spring
 * first makes contact (A point) and finally breaks contact (B point) with the
 * wrappable. The A/B points are associated with the subsegments of each
 * wrappable segment, and are computed dynamically when the subsegments
 * are computed, which happens inside the {@link WrapSegment#updateSubSegments}
 * method of {@link WrapSegment}.
 *
 * <p>The computation of the wrap path and the updating of the subsegments for
 * all wrappable segments occurs inside {@link #updateWrapSegments(int)},
 * which is initially called at time 0 (before any simulation steps)
 * inside {@link #preadvance}, and subsequently <i>after</i> each
 * simulation step inside {@link #postadvance}.
 */
public class MultiPointSpring extends PointSpringBase
   implements ScalableUnits, TransformableGeometry,
              CopyableComponent, RequiresPrePostAdvance,
              HasSlaveObjects {

   private final static double INF = Double.POSITIVE_INFINITY;

   public boolean myDebug = false;

   public static boolean myDrawWrapPoints = true;
   protected boolean myUpdateContactsP = true;
   protected int myStateVersion = 0;
   
   /* === segment, wrappable and solve block information === */

   protected ArrayList<SegmentSpec> mySegmentSpecs;
   protected ArrayList<Segment> mySegments;
   protected ArrayList<WrappableSpec> myWrappables;
   protected int myNumBlks; // set to numPoints()
   protected int[] mySolveBlkNums;
   protected int myHasWrappableSegs = -1; // -1 means we don't know
   protected int myHasConditionalPoints = -1; // -1 means we don't know
   protected int myHasMovingMarkers = -1; // -1 means we don't know
   protected ArrayList<Point3d[]> myWrapPaths; // used only for scanning

   /* === property attributes === */

   protected static double DEFAULT_WRAP_STIFFNESS = 1;
   protected double myWrapStiffness = DEFAULT_WRAP_STIFFNESS;

   protected static double DEFAULT_WRAP_DAMPING = -1;
   protected double myWrapDamping = DEFAULT_WRAP_DAMPING;

   protected static double DEFAULT_CONTACT_STIFFNESS = 10;
   protected double myContactStiffness = DEFAULT_CONTACT_STIFFNESS;

   protected static double DEFAULT_CONTACT_DAMPING = 0;
   protected double myContactDamping = DEFAULT_CONTACT_DAMPING;

   protected static int DEFAULT_MAX_WRAP_ITERATIONS = 10;
   protected int myMaxWrapIterations = DEFAULT_MAX_WRAP_ITERATIONS;

   protected static int DEFAULT_MAX_WRAP_DISPLACEMENT = -1;
   protected double myMaxWrapDisplacement = DEFAULT_MAX_WRAP_DISPLACEMENT;
   protected boolean myMaxWrapDisplacementExplicitP = false;

   protected static double DEFAULT_CONV_TOL = 1e-5;
   protected double myConvTol = DEFAULT_CONV_TOL;

   protected static boolean DEFAULT_LINE_SEARCH = true;
   protected boolean myLineSearchP = DEFAULT_LINE_SEARCH;

   protected static boolean DEFAULT_DRAW_KNOTS = false;
   protected boolean myDrawKnotsP = DEFAULT_DRAW_KNOTS;

   protected static boolean DEFAULT_DRAW_AB_POINTS = false;
   protected boolean myDrawABPointsP = DEFAULT_DRAW_AB_POINTS;

   protected static final Color DEFAULT_AB_POINT_COLOR = Color.CYAN;
   protected Color myABPointColor =  DEFAULT_AB_POINT_COLOR;

   protected static final Color DEFAULT_CONTACTING_KNOTS_COLOR = null;
   protected Color myContactingKnotsColor = DEFAULT_CONTACTING_KNOTS_COLOR;

   // successive overrelaxation parameter (nominally 1.0)
   protected static double DEFAULT_SOR = 1.0;
   protected double mySor = DEFAULT_SOR;

   protected static int DEFAULT_DEBUG_LEVEL = 0;
   protected int myDebugLevel = DEFAULT_DEBUG_LEVEL;
   
   protected static boolean DEFAULT_PROFILING = false;
   protected boolean myProfilingP = DEFAULT_PROFILING;

   protected static final double DEFAULT_WRAP_KNOT_DENSITY = 50; 
   protected double myWrapKnotDensity = DEFAULT_WRAP_KNOT_DENSITY;
   protected PropertyMode myWrapKnotDensityMode = PropertyMode.Inherited;

   protected double myDrawDisplacements = 0;

   /* === profiling attributes === */

   protected FunctionTimer myProfileTimer = new FunctionTimer();
   protected int myProfileCnt = 0;
   protected int myUpdateContactsCnt = 0;
   protected int myIterationCnt = 0;
   protected int myConvergedCnt = 0;
   protected int myContactCnt = 0;

   public int totalStuck;
   public int totalCalls;
   public int totalFails;
   public int totalFalseStuck;
   public double maxForceNorm;
   public double sumForceNorm;
   public double maxLengthErr;
   public double sumLengthErr;

   protected boolean myPrintProfilingP = false;

   /* === rendering attributes === */

   protected RenderProps myABRenderProps;
   protected RenderObject myRenderObj; // used to render the strands
   protected boolean myRenderObjValidP = false;

   /* === Property declarations and accessors === */

   public static PropertyList myProps =
      new PropertyList (MultiPointSpring.class, PointSpringBase.class);

   static {
      myProps.add (
         "wrapStiffness", "stiffness for wrapping strands",
         DEFAULT_WRAP_STIFFNESS);
      myProps.add (
         "wrapDamping", "damping for wrapping strands",
         DEFAULT_WRAP_DAMPING);
      myProps.add (
         "contactStiffness", "contact stiffness for wrapping strands",
         DEFAULT_CONTACT_STIFFNESS);
      myProps.add (
         "contactDamping", "contact damping for wrapping strands",
         DEFAULT_CONTACT_DAMPING);
      myProps.addInheritable (
         "wrapKnotDensity:Inherited",
         "default number of wrap knots per unit distance",
         DEFAULT_WRAP_KNOT_DENSITY);
      myProps.add (
         "maxWrapIterations", "max number of wrap iterations per step",
         DEFAULT_MAX_WRAP_ITERATIONS);
      myProps.add (
         "maxWrapDisplacement", "max knot displacement per step",
         DEFAULT_MAX_WRAP_DISPLACEMENT, "NW");
      myProps.add (
         "convergenceTol", "convergence tolerance", DEFAULT_CONV_TOL);
      myProps.add (
         "lineSearch", "enable or disable line search",
         DEFAULT_LINE_SEARCH);
      myProps.add (
         "drawKnots", "draw wrap strand knots",
         DEFAULT_DRAW_KNOTS);
      myProps.add (
         "drawABPoints", "draw A/B points on wrapping obstacles",
         DEFAULT_DRAW_AB_POINTS);
      myProps.add (
         "ABPointColor", "color to use when drawing A/B points",
         DEFAULT_AB_POINT_COLOR);
      myProps.add (
         "contactingKnotsColor", "draw contacting knots with this color",
         DEFAULT_CONTACTING_KNOTS_COLOR);
      myProps.add (
         "sor", "successive overrelaxation parameter", DEFAULT_SOR);
      myProps.add (
         "debugLevel", "turns on debug prints if > 0", DEFAULT_DEBUG_LEVEL);
      myProps.add (
         "profiling", "enables timing of the wrapping code", DEFAULT_PROFILING);
      myProps.add (
         "drawDisplacements", 
         "enables drawing of the most recent knot displacements", 0);
   }

   /**
    * {@inheritDoc}
    */
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * Queries the wrap stiffness for this spring.
    *
    * @return wrap stiffness
    */
   public double getWrapStiffness () {
      return myWrapStiffness;
   }

   /**
    * Sets the wrap stiffness for this spring. There is usually no need to set
    * this, since without loss of generality, the wrap stiffness can be set to
    * 1.
    *
    * @param stiffness wrap stiffness
    */
   public void setWrapStiffness (double stiffness) {
      myWrapStiffness = stiffness;
   }

   /**
    * Queries the wrap damping for this spring.
    *
    * @return wrap damping
    */
   public double getWrapDamping () {
      if (myWrapDamping < 0) {
         return getDefaultWrapDamping();
      }
      else {
         return myWrapDamping;
      }
   }

   /**
    * Sets the wrap damping for this spring. If the wrap damping is K, then
    * values between 10 K and 100 K generally give good results.
    *
    * @param damping wrap damping
    */
   public void setWrapDamping (double damping) {
      myWrapDamping = damping;
   }

   /**
    * Determine a default wrap damping for this spring.
    */
   private double getDefaultWrapDamping() {
      // for now, just set default wrap damping to 10 X wrap stiffness, so that
      // the stiffness/damping ratio will be 0.1 (and 1 for contact
      // damping). This which should imply relatively fast convergence.
      return 10*myWrapStiffness;
   }

   /**
    * Queries the contact stiffness for this spring.
    *
    * @return contact stiffness
    */
   public double getContactStiffness () {
      return myContactStiffness;
   }

   /**
    * Sets the contact stiffness for this spring. If K is the wrap damping,
    * then a value of 10 K generally gives good results.
    *
    * @param stiffness contact stiffness
    */
   public void setContactStiffness (double stiffness) {
      myContactStiffness = stiffness;
   }

   /**
    * Queries the the contact damping for this spring.
    *
    * @return contact damping
    */
   public double getContactDamping () {
      return myContactDamping;
   }

   /**
    * Sets the contact damping for this spring. By default, the contact damping
    * is 0.
    *
    * @param damping contact damping
    */
   public void setContactDamping (double damping) {
      myContactDamping = damping;
   }

   /**
    * Sets the default knot density for wrapping strands.  This is the number
    * of knots used per unit distance. When a wrapping strand is initialized,
    * and the number of knots is not explicitly specified, this number is
    * multiplied by the strand's initial length to determine the number of
    * knots.
    *
    * @param p desired knot density
    */
   public void setWrapKnotDensity (double p) {
      if (p <= 0) {
         throw new IllegalArgumentException (
            "knot density must be positive");
      }
      myWrapKnotDensity = p;
      myWrapKnotDensityMode =
         PropertyUtils.propagateValue (
            this, "wrapKnotDensity", p, myWrapKnotDensityMode);
   }

   /**
    * Returns the default knot density for wrapping strands.  See {@link
    * #setWrapKnotDensity} for details.
    *
    * @return current knot density
    */
   public double getWrapKnotDensity () {
      return myWrapKnotDensity;
   }

   public PropertyMode getWrapKnotDensityMode() {
      return myWrapKnotDensityMode;
   }

   public void setWrapKnotDensityMode (PropertyMode mode) {
      myWrapKnotDensityMode =
         PropertyUtils.setModeAndUpdate (
            this, "wrapKnotDensity", myWrapKnotDensityMode, mode);
   }

   /**
    * Queries the maximum number of wrap iterations for this spring.
    *
    * @return maximum number of wrap iterations
    */
   public int getMaxWrapIterations () {
      return myMaxWrapIterations;
   }

   /**
    * Sets the maximum number of wrap iterations for this spring.
    *
    * @param num maximum number of wrap iterations
    */
   public void setMaxWrapIterations (int num) {
      myMaxWrapIterations = num;
   }

   /**
    * Queries the maximum wrap displacement for this spring.
    *
    * @return maximum wrap displacement 
    */
   public double getMaxWrapDisplacement () {
      if (myMaxWrapDisplacement == -1) {
         myMaxWrapDisplacement = computeDefaultMaxWrapDisplacement();
      }
      return myMaxWrapDisplacement;
   }

   /**
    * Sets the maximum wrap displacement for this spring. Setting a value of -1
    * causes the displacement to be computed automatically.
    *
    * @param d maximum wrap displacement 
    */
   public void setMaxWrapDisplacement (double d) {
      if (d < 0) {
         // displacement to be computed automatically. Update the value if
         // necessary
         if (myMaxWrapDisplacementExplicitP || myMaxWrapDisplacement == -1) {
            myMaxWrapDisplacement = computeDefaultMaxWrapDisplacement();
         }
         myMaxWrapDisplacementExplicitP = false;
      }
      else {
         myMaxWrapDisplacement = d;
         myMaxWrapDisplacementExplicitP = true;
      }
   }

   protected void maybeInvalidateMaxWrapDisplacement() {
      if (!myMaxWrapDisplacementExplicitP) {
         myMaxWrapDisplacement = -1;
      }
   }

   /**
    * Computes a default maximum wrap displacement for this spring.
    */
   private double computeDefaultMaxWrapDisplacement() {
      double mind = Double.POSITIVE_INFINITY;

      CompositeComponent comp =
         ComponentUtils.nearestEncapsulatingAncestor (this);
      if (comp instanceof Renderable) {
         mind = RenderableUtils.getRadius ((Renderable)comp)/10;
      }
      for (WrappableSpec wspec : myWrappables) {
         Wrappable w = wspec.myWrappable;
         if (w instanceof HasSurfaceMesh) {
            PolygonalMesh mesh = ((HasSurfaceMesh)w).getSurfaceMesh();
            if (mesh != null) {
               OBB obb = new OBB();
               if (mesh.numVertices() > 10000) {
                  // hull can take too long with too many points
                  obb.set (mesh, 0, OBB.Method.Points);
               }
               else {
                  obb.set (mesh, 0, OBB.Method.ConvexHull);
               }
               Vector3d widths = new Vector3d();
               obb.getWidths (widths);
               double hw = widths.minElement()/2;
               if (hw < mind) {
                  mind = hw/2;
               }
            }
         }
      }
      if (mind == Double.POSITIVE_INFINITY) {
         mind = 1.0;
      }
      return mind;
   }

   /**
    * Queries the convergence tolerance for this spring. See {@link 
    * #setConvergenceTol} for a description of the tolerance.
    *
    * @return convergence tolerance
    */
   public double getConvergenceTol () {
      return myConvTol;
   }

   /**
    * Sets the convergence tolerance {@code tol} for this spring. The default
    * value is {@code 1.0e-5}. The tolerance specifies the maximum residual
    * force at each knot as a fraction of the nominal wrapping tension F.
    *
    * <p>Since F is given by
    * <pre>
    *     K l
    * F = ---
    *      m
    * </pre>
    * where {@code K} is the wrap stiffness, {@code l} is the strand
    * length, and {@code m} is the number of knots, a maximum residual
    * of {@code F * tol} at each knot implies a bound on the norm
    * of the total force vector given by
    * <pre>
    *            K l tol
    * || f || &lt;  -------
    *            sqrt(m)
    * </pre>
    * 
    * @param tol convergence tolerance
    */
   public void setConvergenceTol (double tol) {
      myConvTol = tol;
   }

   /**
    * Queries whether line search is enabled for this spring.
    *
    * @return {@code true} if line search is enabled
    */
   public boolean getLineSearch () {
      return myLineSearchP;
   }

   /**
    * Enables or disables line search for this spring. Line search is enabled
    * by default.
    *
    * @param enable if {@code true}, enables line search
    */
   public void setLineSearch (boolean enable) {
      myLineSearchP = enable;
   }

   /**
    * Queries whether knot rendering is enabled for this spring.
    *
    * @return {@code true} if knot rendering is enabled
    */
   public boolean getDrawKnots () {
      return myDrawKnotsP;
   }

   /**
    * Enables or disables knot rendering for this spring.
    *
    * @param enable if {@code true}, enables knot rendering
    */
   public void setDrawKnots (boolean enable) {
      myDrawKnotsP = enable;
   }

   /**
    * Queries whether drawing A/B points is enabled for this spring.
    *
    * @return {@code true} if drawing A/B points is enabled
    */
   public boolean getDrawABPoints () {
      return myDrawABPointsP;
   }

   /**
    * Enables or disables drawing A/B points for this spring.
    *
    * @param enable if {@code true}, drawing A/B points is enabled
    */
   public void setDrawABPoints (boolean enable) {
      myDrawABPointsP = enable;
   }

   /**
    * Queries the color to be used for rendering A/B points.
    * 
    * @return A/B point color
    */
   public Color getABPointColor() {
      return myABPointColor;
   }

   /**
    * Sets the color to be used for rendering A/B points.
    * 
    * @param color A/B point color
    */
   public void setABPointColor (Color color) {
      myABPointColor = color;
   }

   /**
    * Queries the color, if any, used for rendering knots that are in contact,
    * or {@code null} if no such color is specified.
    *
    * @return color for rendering contacting knots, or {@code null}
    */
   public Color getContactingKnotsColor() {
      return myContactingKnotsColor;
   }

   /**
    * Sets a special color that should be used for rendering knots that are in
    * contact. Specifying {@code null} removes any special color.
    *
    * @param color color for rendering contacting knots, or {@code null}
    */
   public void setContactingKnotsColor (Color color) {
      myContactingKnotsColor = color;
   }

   /**
    * Gets the successive overrelaxation parameter for this spring.
    *
    * @return successive overrelaxation parameter
    */
   public double getSor() {
      return mySor;
   }

   /**
    * Sets the successive overrelaxation parameter for this spring.
    * The default value is 1, meaning that SOR is not currently used.
    *
    * @param sor successive overrelaxation parameter
    */   
   public void setSor (double sor) {
      mySor = sor;
   }

   /**
    * Gets the debug level for this spring.
    *
    * @return debug level
    */
   public int getDebugLevel () {
      return myDebugLevel;
   }

   /**
    * Sets the debug level for this spring. Values {@code > 0} causes different
    * amount of debugging information to be printed.
    *
    * @param level debug level
    */
   public void setDebugLevel (int level) {
      myDebugLevel = level;
      for (Segment seg : getSegments()) {
         if (seg instanceof WrapSegment) {
            ((WrapSegment)seg).debugLevel = level;
         }
      }
   }

   /**
    * Queries whether the collection of profiling information is enabled for
    * this spring.
    *
    * @return {@code true} if profiling is enabled
    */
   public boolean getProfiling () {
      return myProfilingP;
   }
 
   /**
    * Enables or disables the collection of profiling information for this
    * spring. Profiling information includes computation time, and cummulative
    * counts of iterations and contact detection.
    *
    * @param enabled if {@code true}, enabled profiling
    */
   public void setProfiling (boolean enabled) {

      myUpdateContactsCnt = 0;
      myIterationCnt = 0;
      myProfilingP = enabled;
      myProfileCnt = 0;
      myContactCnt = 0;
      myConvergedCnt = 0;
      myProfileTimer.reset();

      myProfilingP = enabled;
   }

   public double getDrawDisplacements() {
      return myDrawDisplacements;
   }

   public void setDrawDisplacements (double scale) {
      myDrawDisplacements = scale;
   }

   /* === profiling methods === */

   /**
    * Queries whether printing profile timing information is enabled for this
    * spring.
    *
    * @return {@code true} if printing profile timing information is enabled
    */
   public boolean getPrintProfiling () {
      return myPrintProfilingP;
   }

   /**
    * Enables or disables the printing of profile timing information for this
    * spring.
    *
    * @param enabled if {@code true}, printing profile timing information is
    * enabled
    */
   public void setPrintProfiling (boolean enabled) {
      myPrintProfilingP = enabled;
   }

   /**
    * When profiling is enabled, returns the averge compute time required
    * to update each wrap path.
    *
    * @return average wrap path compute time (in usec)
    */
   public double getProfileTimeUsec() {
      return myProfileTimer.getTimeUsec()/myProfileCnt;
   }

   /**
    * When profiling is enabled, returns the cummulative number of contact
    * detection operations for all wrappable segment updates.
    *
    * @return cummulative number of contact detection operations
    */
   public int getUpdateContactsCount() {
      return myUpdateContactsCnt;
   }

   /**
    * When profiling is enabled, returns the cummulative number of iterations
    * for updating all wrap paths.
    *
    * @return cummulative number of wrap path iterations
    */
   public int getIterationCount() {
      return myIterationCnt;
   }

   /**
    * When profiling is enabled, returns the total number of times that the
    * wrap path solution converged.
    *
    * @return total number of times the path solution converged
    */
   public int getConvergedCount() {
      return myConvergedCnt;
   }

   /**
    * When profiling is enabled, returns the total number of times that the
    * wrap path was in contact.
    *
    * @return total number of times the wrap path was in contact
    */
   public int getContactCount() {
      return myContactCnt;
   }

   /**
    * When profiling is enabled, returns the total number of times a wrap
    * path has been updated.
    *
    * @return total number of wrap path updates
    */
   public int getProfileCount() {
      return myProfileCnt;
   }

   /* === constructors === */

   /**
    * Constructs an empty multipoint spring.
    */
   public MultiPointSpring() {
      this (null);
   }

   /**
    * Constructs an empty multipoint spring with a name.
    *
    * @param name name for the spring
    */
   public MultiPointSpring (String name) {
      super (name);
      mySegmentSpecs = new ArrayList<SegmentSpec>();
      mySegments = null;
      myWrappables = new ArrayList<WrappableSpec>();
      mySolveBlkNums = new int[0];
      myNumBlks = 0;
   }

   /**
    * Constructs an empty multipoint spring with a name and initialized with a
    * linear axial material.
    *
    * @param name name for the spring
    * @param k stiffness for the axial material
    * @param d damping for the axial material
    * @param l0 rest length for the axial material
    */
   public MultiPointSpring (String name, double k, double d, double l0) {
      this (name);
      setRestLength (l0);
      setMaterial (new LinearAxialMaterial (k, d));
   }

   /**
    * Constructs an empty multipoint spring initialized with a linear axial
    * material.
    *
    * @param k stiffness for the axial material
    * @param d damping for the axial material
    * @param l0 rest length for the axial material
    */
   public MultiPointSpring (double k, double d, double l0) {
      this (null);
      setRestLength (l0);
      setMaterial (new LinearAxialMaterial (k, d));
   }


   /* === methods for managing points === */

   /**
    * Adds a point to this spring at a specified index. This will also add a
    * segment to the spring. The index value {@code idx} must not exceed the
    * current number of points.
    * 
    * @param idx index location for the point
    * @param pnt point to add
    */
   public void addPoint (int idx, Point pnt) {
      if (idx > mySegmentSpecs.size()) {
         throw new ArrayIndexOutOfBoundsException (
            "specified index "+idx+
            " exceeds number of points "+mySegmentSpecs.size());
      }
      SegmentSpec spec = new SegmentSpec();
      spec.setPntB (pnt);
      mySegmentSpecs.add (idx, spec);
      if (invalidateSegments()) {
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
   }

   /**
    * Adds a point to this spring. The will also begin the definition of a new
    * segment.
    * 
    * @param pnt point to add
    */
   public void addPoint (Point pnt) {
      addPoint (numPoints(), pnt);
   }

   /**
    * {@inheritDoc}
    */
   public Point getPoint (int idx) {
      if (idx < 0 || idx >= numPoints()) {
         throw new IndexOutOfBoundsException (
            "idx=" + idx + ", number of points=" + numPoints());
      }
      return mySegmentSpecs.get(idx).myPntB;
   }

   /**
    * {@inheritDoc}
    */
   public int numPoints() {
      return mySegmentSpecs.size();
   }

   /**
    * {@inheritDoc}
    */
   public int indexOfPoint (Point pnt) {
      for (int i=0; i<mySegmentSpecs.size(); i++) {
         SegmentSpec spec = mySegmentSpecs.get(i);
         if (spec.myPntB == pnt) {
            return i;
         }
      }
      return -1;
   }

   /**
    * Removes a point (and the corresponding segment, if the point is not the
    * first point) from this spring.
    * 
    * @param pnt point to remove
    * @return {@code true} if the point was present in the spring and removed
    */
   public boolean removePoint (Point pnt) {
      int idx = indexOfPoint (pnt);
      if (idx != -1) {      
         mySegmentSpecs.remove (idx);
         if (invalidateSegments()) {
            notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
         }
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * {@inheritDoc}
    *
    * All aspects of the segment defined by this point are preserved.
    */
   public void setPoint (int idx, Point pnt) {
      int nump = mySegmentSpecs.size();
      if (idx >= nump) {
         throw new ArrayIndexOutOfBoundsException (
            "specified index "+idx+" exceeds number of points "+nump);
      }
      mySegmentSpecs.get(idx).setPntB (pnt);
      if (invalidateSegments()) {
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
   }

   /**
    * Removes all points (and segments) from this spring.
    */
   public void clearPoints() {
      mySegmentSpecs.clear();
      if (invalidateSegments()) {
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
   }

   /* === wrappable methods === */

   /**
    * Adds a wrappable to this spring which is applied to all wrap segments.
    * 
    * @param wrappable wrappable to add
    */
   public void addWrappable (Wrappable wrappable) {
      addWrappable (wrappable, -1, -1);
   }

   /**
    * Adds a wrappable to this spring which is applied to the segments between
    * the points indexed by {@code pntIdx0} and {@code pntIdx1}.  {@code
    * pntIdx0} and {@code pntIdx1} are assumed to be non-negative with {@code
    * pntIdx0 <= pntIdx1}; otherwise, the wrappable will be applied to all
    * segments.
    *
    * <p>The range indicated by {@code pntIdx0} and {@code pntIdx1} may lie
    * partly or completely outside the range of actual points. The range will
    * also remain fixed even if points are subsequently added or removed from
    * the spring.
    * 
    * @param wrappable wrappable to add
    * @param pntIdx0 index of the first point in the wrapping range
    * @param pntIdx1 index of the second point in the wrapping range
    */
   public void addWrappable (Wrappable wrappable, int pntIdx0, int pntIdx1) {
      if (pntIdx0 < 0 || pntIdx1 < 0 || pntIdx0 > pntIdx1) {
         pntIdx0 = -1;
         pntIdx1 = -1;
      }
      if (!containsWrappable (wrappable, pntIdx0, pntIdx1)) {
         myWrappables.add (new WrappableSpec(wrappable, pntIdx0, pntIdx1));
         invalidateSegments();
         maybeInvalidateMaxWrapDisplacement();
      }        
   }

   /**
    * Queries whether or not this spring contains a wrappable 
    * for which wrapping is applied to all segments.
    * 
    * @param wrappable wrappable to query
    * @return {@code true} if the wrappable is present in the spring
    */
   public boolean containsWrappable (Wrappable wrappable) {
      return indexOfWrappable (wrappable) != -1;
   }

   /**
    * Queries whether or not this spring contains a wrappable which is applied
    * to the segments between the points indexed by {@code pntIdx0} and {@code
    * pntIdx1}.  See {@link #addWrappable(Wrappable,int,int)} for a description
    * of the constraints on {@code pntIdx0} and {@code pntIdx1}.
    * 
    * @param wrappable wrappable to query
    * @param pntIdx0 index of the first point in the wrapping range
    * @param pntIdx1 index of the second point in the wrapping range
    * @return {@code true} if the wrappable is present in the spring
    */
   public boolean containsWrappable (
      Wrappable wrappable, int pntIdx0, int pntIdx1) {
      return indexOfWrappable (wrappable, pntIdx0, pntIdx1) != -1;
   }

   /**
    * Queries the number of wrappables specified in this spring.  Note that the
    * same wrappable may be specified multiple times if it has different
    * segment ranges.
    * 
    * @return number of wrappables in this spring
    */
   public int numWrappables() {
      return myWrappables.size();
   }

   /**
    * Returns a list of the wrappables specified in this spring.
    * 
    * @return wrappables in this spring
    */
   public ArrayList<Wrappable> getWrappables() {
      ArrayList<Wrappable> list = new ArrayList<>(numWrappables());
      for (WrappableSpec ws : myWrappables) {
         list.add (ws.myWrappable);
      }
      return list;
   }

   /**
    * Returns the index of a wrappable in this spring which is applied to
    * all segments, or -1 if such a wrappable is not present.
    *
    * @param wrappable wrappable to locate
    * @return index of the wrappable, or -1 if not present
    */
   public int indexOfWrappable (Wrappable wrappable) {
      return indexOfWrappable (wrappable, -1, -1);
   }

   /**
    * Returns the index of a wrappable in this spring which is applied to the
    * segments between the points indexed by {@code pntIdx0} and {@code
    * pntIdx1}.  See {@link #addWrappable(Wrappable,int,int)} for a description
    * of the constraints on {@code pntIdx0} and {@code pntIdx1}. If no such
    * wrappable is present, the method returns -1.
    *
    * @param wrappable wrappable to locate
    * @param pntIdx0 index of the first point in the wrapping range
    * @param pntIdx1 index of the second point in the wrapping range
    * @return index of the wrappable, or -1 if not present
    */
   public int indexOfWrappable (Wrappable wrappable, int pntIdx0, int pntIdx1) {
      if (pntIdx0 < 0 || pntIdx1 < 0 || pntIdx0 > pntIdx1) {
         pntIdx0 = -1;
         pntIdx1 = -1;
      }
      for (int i=0; i<myWrappables.size(); i++) {
         WrappableSpec wspec = myWrappables.get(i);
         if (wspec.myWrappable == wrappable &&
             wspec.myPntIdx0 == pntIdx0 && wspec.myPntIdx1 == pntIdx1) {
            return i;
         }
      }
      return -1;
   }

   /**
    * Returns the idx-th wrappable in this spring.
    *
    * @param idx index of the wrappable
    * @return {@code idx}-th wrappable in this spring
    */
   public Wrappable getWrappable (int idx) {
      if (idx < 0 || idx >= myWrappables.size()) {
         throw new IndexOutOfBoundsException (
            "idx=" + idx + ", num wrappables=" + numWrappables());
      }
      return myWrappables.get(idx).myWrappable;
   }
   
   /**
    * Returns the point indices defining the wrappable range for the idx-th
    * wrappable in this spring. Values of -1 indicate that the wrappable is
    * applied to all segments.
    *
    * @param idx index of the wrappable
    * @return point indices defining the wrappable range.
    */
   public int[] getWrappableRange (int idx) {
      if (idx < 0 || idx >= myWrappables.size()) {
         throw new IndexOutOfBoundsException (
            "idx=" + idx + ", num wrappables=" + numWrappables());
      }
      WrappableSpec wspec = myWrappables.get(idx);
      return new int[] { wspec.myPntIdx0, wspec.myPntIdx1 };
   }
   
   protected void removeWrappableFromContacts (int widx) {
      // XXX check this
      System.out.println ("removeWrappableFromContacts " + widx);
      if (mySegments != null) {
         for (Segment seg : mySegments) {
            if (seg instanceof WrapSegment) {
               ((WrapSegment)seg).removeWrappableFromContacts (widx);
            }
         }
      }     
   }

   protected ArrayList<int[]> reindexContactInfo (int[] wrappableReindexMap) {
      ArrayList<int[]> savedContactInfo = new ArrayList<>();
      if (mySegments != null) {
         boolean contactChanged = false;
         for (Segment seg : mySegments) {
            if (seg instanceof WrapSegment) {
               int[] cinfo = ((WrapSegment)seg).reindexContactInfo (
                  wrappableReindexMap);
               if (cinfo != null) {
                  contactChanged = true;
               }
               savedContactInfo.add (cinfo);
            }
         }
         if (contactChanged) {
            myStateVersion++;
         }
      }     
      return savedContactInfo;
   }

   protected void restoreContactInfo (ArrayList<int[]> savedContactInfo) {
      if (mySegments != null) {
         boolean contactChanged = false;
         int wsegi = 0;
         for (Segment seg : mySegments) {
            if (seg instanceof WrapSegment) {
               int[] cinfo = savedContactInfo.get(wsegi++);
               if (cinfo != null) {
                  contactChanged = true;
               }
               ((WrapSegment)seg).restoreContactInfo (cinfo);
            }
         }
         if (contactChanged) {
            myStateVersion++;
         }
      }     
   }
   
   protected ArrayList<int[]> reindexWrappableSegmentRanges (
      int[] removedSegmentIdxs) {
      ArrayList<int[]> savedRanges = new ArrayList<>();
      if (myWrappables != null) {
         for (WrappableSpec spec : myWrappables) {
            savedRanges.add (
               spec.reindexSegmentRanges (removedSegmentIdxs));
          }
      }         
      return savedRanges;
   }

   protected void restoreWrappableSegmentRanges (ArrayList<int[]> savedRanges) {
      if (myWrappables != null) {
         int widx = 0;
         for (int[] rng : savedRanges) {
            if (rng != null) {
               myWrappables.get(widx).setPointIndices (rng[0], rng[1]);
            }
            widx++;
         }
      }
   }     

   /**
    * Remove from this spring a wrappable which is applied to all segments.
    * 
    * @param wrappable wrappable to remove
    * @return {@code true} if the wrappable was present
    */
   public boolean removeWrappable (Wrappable wrappable) {
      return removeWrappable (wrappable, -1, -1);
   }

   /**
    * Remove from this spring a wrappable which is applied to the segments
    * between the points indexed by {@code pntIdx0} and {@code pntIdx1}.  See
    * {@link #addWrappable(Wrappable,int,int)} for a description of the
    * constraints on {@code pntIdx0} and {@code pntIdx1}.
    * 
    * @param wrappable wrappable to remove
    * @param pntIdx0 index of the first point in the wrapping range
    * @param pntIdx1 index of the second point in the wrapping range
    * @return {@code true} if the wrappable was present
    */
   public boolean removeWrappable (
      Wrappable wrappable, int pntIdx0, int pntIdx1) {
      int widx = indexOfWrappable (wrappable, pntIdx0, pntIdx1);
      if (widx != -1) {
         myWrappables.remove (widx);
         removeWrappableFromContacts (widx);
         invalidateSegments();
         maybeInvalidateMaxWrapDisplacement();
         return true;
      }
      else {
         return false;
      }
   }

   protected void removeAllWrappablesFromContacts() {
      // XXX check this
      if (mySegments != null) {
         for (Segment seg : mySegments) {
            if (seg instanceof WrapSegment) {
               ((WrapSegment)seg).removeAllWrappablesFromContacts();
            }
         }
      }     
   }

   /**
    * Removes all wrappables from this spring
    */
   public void clearWrappables() {
      myWrappables.clear();
      removeAllWrappablesFromContacts();
      invalidateSegments();
      maybeInvalidateMaxWrapDisplacement();
      //notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
   }

   /* === segment query and control === */

   private static Point3d[] copyPoints (Vector3d[] pnts) {
      if (pnts == null) {
         return null;
      }
      else {
         Point3d[] copy = new Point3d[pnts.length];
         for (int k=0; k<copy.length; k++) {
            copy[k] = new Point3d(pnts[k]);
         }        
         return copy;
      }
   }

   // /**
   //  * Returns a list of the segment specifications in this spring. These
   //  * are for reference only and should not be modified.
   //  * 
   //  * @return list of segment specifications
   //  */
   // public ArrayList<SegmentSpec> getSegmentSpecs() {
   //    ArrayList<SegmentSpec> list = new ArrayList<>();
   //    list.addAll (mySegmentSpecs);
   //    return list;
   // }
   
   /**
    * Returns the segment specification indexed by {@code segIdx} in this 
    * spring. This is the segment that lies between points {@code idx} and 
    * {@code idx}+1. The return specification is for reference only and
    * should not be modified.
    * 
    * @param segIdx index of the segment specification
    * @return the {@code idx}-th segment spec
    */
   public SegmentSpec getSegmentSpec(int segIdx) {
      return mySegmentSpecs.get(segIdx);
   }

   /**
    * Returns the curren segment indexed by {@code segIdx} in this spring.
    * 
    * @param segIdx index of the segment
    * @return the {@code idx}-th segment
    */
   public Segment getSegment(int segIdx) {
      return getSegments().get(segIdx);
   }

   /**
    * Sets the current spring segment (the one following the most recently
    * added point) to be wrappable, with a specified number of knots {@code
    * numk}.  If {@code numk <= 0}, then the number of knots is computed from
    * the initial length of the path times the spring's knot point density, as
    * returned by {@link #getWrapKnotDensity}. The initial path for the
    * segment is taken to be a straight line between its end points.
    * 
    * @param numk if {@code > 0}, number of knots to be used in the segment
    */
   public void setSegmentWrappable (int numk) {
      setSegmentWrappable (numk, null);
   }

   /**
    * Sets the current spring segment (the one following the most recently
    * added point) to be wrappable, with a specified number of knots {@code
    * numk} and initializing points {@code initialPnts}. If {@code numk <= 0},
    * then the number of knots is computed from the initial length of the path
    * times the spring's knot point density, as returned by {@link
    * #getWrapKnotDensity}. If {@code initialPnts} is {@code null}, then no
    * initializing points are specified.
    *
    * <p>The initializing points are used to specify the wrap path for the
    * segment: the knots are distributed along the piecewise-linear path
    * defined by the segment end points on each end and the initializing points
    * in between. The path is then ``pulled taught'' while wrapping around any
    * intermediate obstacles. Initializing points help define how a path should
    * wrap around obstacles that would otherwise intersect a straight line
    * path.
    * 
    * @param numk if {@code > 0}, the number of knots to be used in the segment
    * @param initialPnts if not {@code null}, the initializing points
    */
   public void setSegmentWrappable (int numk, Point3d[] initialPnts) {
      if (mySegmentSpecs.size() == 0) {
         throw new ImproperStateException (
            "setSegmentWrappable() called before first call to addPoint()");
      }
      doSetSegmentWrappable (mySegmentSpecs.size()-1, numk, initialPnts);
      invalidateSegments();
   }

   /**
    * Sets the segment indexed by {@code segIdx} to be wrappable, with a
    * specified number of knots and set of initializing points. See {@link
    * #setSegmentWrappable(int,Point3d[])} for a description of how the
    * arguments work.
    * 
    * @param segIdx index of the segment
    * @param numk if {@code > 0}, number of knots to be used in the segment
    * @param initialPnts if not {@code null}, the initializing points
    */
   public void setSegmentWrappable (
      int segIdx, int numk, Point3d[] initialPnts) {
      if (segIdx >= mySegmentSpecs.size()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" does not exist");
      }
      doSetSegmentWrappable (segIdx, numk, initialPnts);
      invalidateSegments();
   }

   /**
    * Sets all segments in this spring to be wrappable with a specified number
    * of knots {@code numk}. If {@code numk <= 0}, then the number of knots is
    * computed from the initial length of the path times the spring's knot
    * point density, as returned by {@link #getWrapKnotDensity}.
    *
    * The initial path for each segment is taken to be a straight
    * line between its end points.
    * 
    * @param numk if {@code > 0}, the number of knots to be used for each
    * segment
    */
   public void setAllSegmentsWrappable (int numk) {
      if (mySegmentSpecs.size() == 0) {
         throw new ImproperStateException (
            "setAllSegmentsWrappable() called before first call to addPoint()");
      }
      for (int i=0; i<mySegmentSpecs.size(); i++) {
         mySegmentSpecs.get(i).setWrappable (true);
      }
      invalidateSegments();
   }   

   /**
    * Queries whether the segment indexed by {@code segIdx} is wrappable.
    *
    * @return {@code true} if the segment is wrappable.
    */
   public boolean isSegmentWrappable (int segIdx) {
      if (segIdx >= mySegmentSpecs.size()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" does not exist");
      }
      return mySegmentSpecs.get(segIdx).isWrappable();
   }

   /**
    * Queries the specified number of knots for the wrap segment indexed by
    * {@code segIdx}. A returned value {@code <= 0} indicates that the segment
    * is either not wrappable, or the number of knots will be computed from the
    * initial length of the path times the spring's knot point density, as
    * returned by {@link #getWrapKnotDensity}.
    *
    * <p>The <i>actual</i> number of knots assigned to a segment can be queried
    * from {@link #numKnots(int)}.
    * 
    * @param segIdx index of the segment
    * @return specified number of knots
    * @throws IndexOutOfBoundsException if the segment is not defined
    */
   public int getNumKnots (int segIdx) {
      if (segIdx >= mySegmentSpecs.size()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" does not exist");
      }
      if (mySegmentSpecs.get(segIdx).isWrappable()) {
         return mySegmentSpecs.get(segIdx).getNumKnots();
      }
      else {
         return 0;
      }
   }

   /**
    * Sets the number of knots in the wrap segment indexed by {@code segIdx}.
    * 
    * @param segIdx index of the segment
    * @param numk number of knots in the segment
    * @throws IndexOutOfBoundsException if the segment is not defined
    * @throws IllegalArgumentException if the segment is not wrappable
    */
   public void setNumKnots (int segIdx, int numk) {
      if (segIdx >= mySegmentSpecs.size()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" does not exist");
      }
      if (!mySegmentSpecs.get(segIdx).isWrappable()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" is not wrappable");
      }
      mySegmentSpecs.get(segIdx).setNumKnots (numk);
      invalidateSegments();
   }

   /**
    * Queries the points, if any, used to initialize the path for the wrap
    * segment indexed by {@code segIdx}. See {@link
    * #setSegmentWrappable(int,Point3d[])} for a description of how the
    * initializing points are used. Returns {@code null} if there are no
    * initializing points or if the segment is not wrappable.
    * 
    * @param segIdx index of the segment
    * @return path initializing points, or {@code null}
    * @throws IndexOutOfBoundsException if the segment is not defined
    */
   public Point3d[] getInitializingPoints (int segIdx) {
      if (segIdx >= mySegmentSpecs.size()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" does not exist");
      }
      if (mySegmentSpecs.get(segIdx).isWrappable()) {
         return mySegmentSpecs.get(segIdx).getInitialPoints();
      }
      else {
         return null;
      }
   }

   /**
    * Sets the points used to initialize the wrap path for the wrap segment
    * indexed by {@code segIdx}. See {@link
    * #setSegmentWrappable(int,Point3d[])} for a description of how the
    * initializing points are used. A value of {@code null} will remove any
    * initializing points.
    *
    * @param segIdx index of the segment
    * @param initialPnts new initializing points for the segment
    * @throws IndexOutOfBoundsException if the segment is not defined
    * @throws IllegalArgumentException if the segment is not wrappable
    */
   public void setInitializingPoints (int segIdx, Point3d[] initialPnts) {
      if (segIdx >= mySegmentSpecs.size()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" does not exist");
      }
      if (!mySegmentSpecs.get(segIdx).isWrappable()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" is not wrappable");
      }
      mySegmentSpecs.get(segIdx).setInitialPoints (initialPnts);
      invalidateSegments();
   }

   protected void doSetSegmentWrappable (
      int segIdx, int numk, Point3d[] initialPnts) {

      SegmentSpec spec = mySegmentSpecs.get (segIdx);
      spec.setWrappable (true);
      spec.myNumKnots = numk;
//      if (numk > 0) {
//         spec.setNumKnotsExplicit (true);
//      }
      spec.setInitialPoints (initialPnts);
   }

//   public boolean clearAllWrapableSegments () {
//      if (mySegmentSpecs.size() == 0) {
//         throw new ImproperStateException (
//            "clearAllWrappableSegments() called before first call to addPoint()");
//      }
//      boolean cleared = false;
//      for (SegmentSpec spec : mySegmentSpecs) {
//         if (spec.isWrappable()) {
//            spec.setWrappable (false);
//            cleared = true;
//         }
//      }
//      if (cleared) {
//         invalidateSegments();
//      }
//      return cleared;
//   }   
         
   public boolean clearWrappableSegment (int segIdx) {
      if (segIdx >= mySegmentSpecs.size()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" does not exist");
      }
      SegmentSpec spec = mySegmentSpecs.get(segIdx);
      if (spec.isWrappable()) {
         spec.setWrappable (false);
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
         return true;
      }
      else {
         return false;
      }
   }

   /**
    * @deprecated Use {@link #setInitializingPoints} instead.
    */
   public void initializeSegment (int segIdx, Point3d[] initialPnts) {
      // XXX do something with this
      if (segIdx >= mySegmentSpecs.size()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" does not exist");
      }
      SegmentSpec spec = mySegmentSpecs.get (segIdx);
      if (spec.isWrappable()) {
         if (mySegments == null) {
            spec.setInitialPoints (initialPnts);
         }
         else {
            // XXX finish
         }
      }
   }

   /**
    * Returns the number of specified segment in this spring.
    * 
    * @return number of specified segments
    */
   public int numSegmentSpecs() {
      // we ignore the last segment because that is used to simply store the
      // terminating point
      return Math.max (0, mySegmentSpecs.size()-1);
   }

   /**
    * Returns the current number of segments in this spring. For internal
    * use only.
    * 
    * @return current number of segments
    */
   public int numSegments() {
      return getSegments().size();
   }

   /**
    * Queries whether the segment indexed by {@code segIdx} is passive.
    *
    * @return {@code true} if the segment is passive
    */
   public boolean isSegmentPassive (int segIdx) {
      if (segIdx >= mySegmentSpecs.size()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" is not defined");
      }
      return mySegmentSpecs.get(segIdx).isPassive();
   }

   /**
    * Sets the current spring segment (the one following the most recently
    * added point) to be <i>passive</i>, meaning that its length is not
    * included as part of the spring's <i>active</i> length.
    */
   public void setSegmentPassive () {
      if (mySegmentSpecs.size() == 0) {
         throw new ImproperStateException (
            "setSegmentPassive() called before first call to addPoint()");
      }
      setSegmentPassive (mySegmentSpecs.size()-1, true);
   }

   /**
    * Sets whether or not the segment indexed by {@code segIdx} is
    * <i>passive</i>, meaning that its length is not included as part of the
    * spring's <i>active</i> length.
    * 
    * @param segIdx index of the segment
    * @param passive if {@code true}, makes the segment passive
    */
   public void setSegmentPassive (int segIdx, boolean passive) {
      if (segIdx >= mySegmentSpecs.size()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" is not defined");
      }
      SegmentSpec spec = mySegmentSpecs.get(segIdx);
      if (spec.isPassive() != passive) {
         spec.setPassive (passive);
         // XXX notify spec change
         notifyParentOfChange (DynamicActivityChangeEvent.defaultEvent);
      }
   }

   /**
    * Queries the number of passive segments in this spring.
    * 
    * @return number of passive segments
    */
   public int numPassiveSegments() {
      int num = 0;
      for (int i=0; i<numSegmentSpecs(); i++) {
         if (mySegmentSpecs.get(i).isPassive()) {
            num++;
         }
      }
      return num;
   }

   /**
    * Queries the number of knots in the segment indexed by {@code
    * segIdx}. Returns 0 if the segment is not wrappable.
    * 
    * @param segIdx index of the segment
    * @return number of knots in the segment
    * @throws IndexOutOfBoundsException if the segment is not defined
    */
   public int numKnots (int segIdx) {
      ArrayList<Segment> segs = getSegments();
      if (segIdx >= segs.size()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" is not defined");
      }
      Segment seg = segs.get(segIdx);
      if (seg instanceof WrapSegment) {
         return ((WrapSegment)seg).numKnots();
      }
      else {
         return 0;
      }
   }

   /**
    * Returns the {@code k}-th knot in the current segment indexed by {@code
    * segIdx}.
    * 
    * @param segIdx index of the current segment
    * @param k index of the knot within the segment
    * @return {@code k}-th knot in segment
    */
   public WrapKnot getKnot (int segIdx, int k) {
      ArrayList<Segment> segs = getSegments();
      if (segIdx >= segs.size()) {
         throw new IndexOutOfBoundsException (
            "Segment "+segIdx+" is not defined");
      }
      Segment seg = segs.get(segIdx);
      if (seg instanceof WrapSegment) {
         WrapSegment wseg = (WrapSegment)seg;
         if (k >= 0 && k < wseg.myNumKnots) {
            return wseg.myKnots[k];
         }
      }
      return null;
   }
   
   /**
    * Invalidates all segments in this spring. Called when the spring
    * structure is changed. Returns {@code true} if the segments were
    * actually invalidated.
    */
   protected boolean invalidateSegments() {
      myRenderObjValidP = false;
      myHasWrappableSegs = -1; // set to "don't know"
      myHasConditionalPoints = -1; // set to "don't know"
      myHasMovingMarkers = -1; // set to "don't know"
      myStateVersion++;
      if (mySegments != null) {
         mySegments = null;
         return true;
      }
      else {
         return false;
      }
   }

   private WrapSegment createWrapSegment (int numk) {
      WrapSegment wseg = new WrapSegment(numk, null);
      wseg.debugLevel = myDebugLevel;
      wseg.setWrappable (true); // set the WRAPPABLE flag
      return wseg;
   }      

   private ArrayList<Point3d> getInitialSegmentPoints (int i0, int i1) {
      ArrayList<Point3d> pnts = new ArrayList<>();
      
      pnts.add (mySegmentSpecs.get(i0).myPntB.getPosition());
      for (int i=i0; i<i1; i++) {
         SegmentSpec spec = mySegmentSpecs.get(i);
         if (i > i0) {
            pnts.add (spec.myPntB.getPosition());
         }
         if (spec.myInitialPnts != null) {
            for (Point3d p : spec.myInitialPnts) {
               pnts.add (p);
            }
         }
      }
      pnts.add (mySegmentSpecs.get(i1).myPntB.getPosition());
      return pnts;
   }

   /**
    * Create initial segments from the specs.
    */
   private void initializeSegments() {
      // make sure the that the number of solve block corresponds to 
      // the number of points
      int numBlks = numPoints()+numWrappables();
      if (numBlks != myNumBlks) {
         mySolveBlkNums = new int[numBlks*numBlks];
         myNumBlks = numBlks;
      }
      ArrayList<Segment> segs = new ArrayList<>(numSegmentSpecs());
      if (numPoints() > 1) {
         if (mySegmentSpecs.get(0).isPointConditional()) {
            throw new IllegalStateException (
               "First point in MultiPointSpring must not be conditional");
         }
         if (mySegmentSpecs.get(numSegmentSpecs()).isPointConditional()) {
            throw new IllegalStateException (
               "Last point in MultiPointSpring must not be conditional");
         }
         int iprev = 0;
         for (int i=1; i<mySegmentSpecs.size(); i++) {
            SegmentSpec spec = mySegmentSpecs.get(i);
            if (spec.isPointActive()) {
               int numk = findNumSegmentKnots (iprev, i);
               Segment seg;
               if (numk > 0) {
                  seg = createWrapSegment (numk);
               }
               else {
                  seg = new Segment();
               }
               seg.setPassive (isSegmentPassive (iprev, i));
               seg.mySpecIdx = iprev;
               seg.setPntB (mySegmentSpecs.get(iprev).myPntB);
               seg.myBlockIdxB = iprev;
               seg.myPntA = mySegmentSpecs.get(i).myPntB;
               seg.myBlockIdxA = i;
               if (numk > 0) {
                  WrapSegment wseg = (WrapSegment)seg;
                  ArrayList<Point3d> ipnts =
                     getInitialSegmentPoints (iprev, i);
                  wseg.initializeStrand (ipnts);
               }
               segs.add (seg);
               iprev = i;
            }
         }
      }
      mySegments = segs;
      //mySegsValidP = true;
   }

   private void setSegmentPointsFromSpecs (ArrayList<Segment> seglist) {
      for (int k=0; k<seglist.size(); k++) {
         Segment seg = seglist.get(k);
         int sidx = seg.mySpecIdx;
         if (sidx >= mySegmentSpecs.size()) {
            throw new InternalErrorException (
               "specIdx "+sidx+" is out of bounds");
         }
         seg.setPntB (mySegmentSpecs.get(sidx).myPntB);
         seg.myBlockIdxB = sidx;
         int nidx;
         if (k+1 < seglist.size()) {
            nidx = seglist.get(k+1).mySpecIdx;
            if (sidx >= mySegmentSpecs.size()) {
               throw new InternalErrorException (
                  "next specIdx "+nidx+" is out of bounds");
            }
         }
         else {
            nidx = mySegmentSpecs.size()-1;
         }
         seg.myPntA = mySegmentSpecs.get(nidx).myPntB;
         seg.myBlockIdxA = nidx;
      }
   }

   /**
    * Returns the segment list, initializing it on demand.
    */
   public ArrayList<Segment> getSegments() {
      if (mySegments == null) {
         initializeSegments();
      }
      return mySegments;
   }
   
   private void allocateSolveBlocks() {
      int numBlks = numPoints()+numWrappables();
      if (numBlks != myNumBlks) {
         mySolveBlkNums = new int[numBlks*numBlks];
         myNumBlks = numBlks;
      }
   }
   
   /**
    * Updates the segments of this spring so that their end points correspond
    * to the points of this spring. Other initializations that depend on
    * structure are performed. If {@code updateWrapSegs} is {@code true}, then
    * any wrap paths will also be updated.
    */
   protected void updateSegs (boolean updateWrapSegs) {
      // XXX finish
      int numBlks = numPoints()+numWrappables();
      myNumBlks = numBlks;
      mySolveBlkNums = new int[myNumBlks*myNumBlks];
//      updateSegs (getSegments(), updateWrapSegs);
   }
   
   /**
    * Determines whether a spanning segment is wrappable and if it is, returns
    * the number of knots that should be used for that segment. Otherwise, the
    * method returns 0. The spanning segment spans the segment specs starting
    * at index {@code sidx} and ending at index {@code eidx} (exclusive). It is
    * wrappable if any sub-segment is wrappable.
    */
   private int findNumSegmentKnots (int sidx, int eidx) {
      int numk = 0;
      boolean wrappable = false;
      for (int i=sidx; i<eidx; i++) {
         SegmentSpec spec = mySegmentSpecs.get(i);
         wrappable |= spec.isWrappable();
         if (spec.isWrappable() && spec.getNumKnots() > 0) {
            numk += spec.getNumKnots();
         }
         else {
            double dist = spec.myPntB.distance (mySegmentSpecs.get(i+1).myPntB);
            numk += Math.max (1, dist*myWrapKnotDensity);
         }
      }
      return wrappable ? numk : 0;
   }

   /**
    * Determines whether a spanning segment is passive. The spanning segment
    * spans the segment specs starting at index {@code sidx} and ending at
    * index {@code eidx} (exclusive). It is passive only if all sub-segments
    * are passive.
    */
   private boolean isSegmentPassive (int sidx, int eidx) {
      for (int i=sidx; i<eidx; i++) {
         SegmentSpec spec = mySegmentSpecs.get(i);
         if (!spec.isPassive()) {
            return false;
         }
      }
      return true;
   }

   private void initializeStrandFromPrevious (
      WrapSegment newseg, int nidx0, int nidx1, int j0, int j1) {

      ArrayList<Point3d> pnts = new ArrayList<>();

      for (int j=j0; j<=j1; j++) {
         Point3d p0 = null;
         Point3d p1 = null;
         Segment seg = null;
         if (j < mySegments.size()) {
            seg = mySegments.get(j);
         }
         if (seg != null && seg.mySpecIdx == nidx1) {
            seg = null;
         }
         if (j == j0) {
            // start point is the first point of newseg
            p0 = mySegmentSpecs.get(nidx0).myPntB.getPosition();
         }
         else if (seg != null) {
            p0 = seg.myPntB.getPosition();
         }
         if (j == j1) {
            // stop point is the last point of newseg
            p1 = mySegmentSpecs.get(nidx1).myPntB.getPosition();
         }
         if (seg != null) {
            pnts.add (p0);
            if (seg instanceof WrapSegment) {
               WrapSegment wseg = (WrapSegment)seg;
               int numk = wseg.numKnots();
               // need to fill in with knot points on wseg.
               int k0 = 0;
               int k1 = numk-1;
               if (j == j0 && seg.mySpecIdx < nidx0) {
                  k0 = Math.max (0, wseg.findNearestKnot (p0));
               }
               if (j == j1 && seg.mySpecIdx < nidx1) {
                  k1 = Math.min (numk-1, wseg.findNearestKnot (p1));
               }
               if (k0 >= k1) {
                  for (int k=k0; k<=k1; k++) {
                     pnts.add (wseg.getKnotPos (k));
                  }
               }
               else {
                  // unlikely to happen; ignore
               }
            }
         }
         if (p1 != null) {
            pnts.add (p1);            
         }
      }
      newseg.initializeStrand (pnts);
   }

   private void initializeStrandFromPreviousInterp (
      WrapSegment newseg, int nidx0, int nidx1, int j0, int j1) {

      int pidx0 = mySegments.get(j0).mySpecIdx;
      int pidx1 = mySegments.get(j1).mySpecIdx;

      ArrayList<Point3d> pnts = new ArrayList<>();
      if (j0 == j1) {
         if (mySegments.get(j0) instanceof WrapSegment) {
            WrapSegment wseg = (WrapSegment)mySegments.get(j0);
            Point3d p0 = mySegmentSpecs.get(nidx0).myPntB.getPosition();
            int k0 = wseg.findNearestKnot (p0); // will be -1 if pidx0 == nidx0
            Point3d p1 = mySegmentSpecs.get(nidx1).myPntB.getPosition();
            int k1 = wseg.findNearestKnot (p1);
            pnts.add (p0);
            if (k1 - k0 > 1) {
               Point3d ps = new Point3d();
               Vector3d disp0 = new Vector3d();
               disp0.sub (p0, wseg.getKnotPos (k0));
               Vector3d disp1 = new Vector3d();
               disp1.sub (p1, wseg.getKnotPos (k1));
               for (int k=k0+1; k<k1; k++) {
                  double s = (k-k0)/(double)(k1-k0);
                  ps.combine (1-s, disp0, s, disp1);
                  ps.add (wseg.getKnotPos (k));
               }
            }
            pnts.add (p1);            
         }
      }
      else {
         if (pidx0 < nidx0) {
            // previous extends back before current segment
            if (mySegments.get(j0) instanceof WrapSegment) {
               WrapSegment wseg = (WrapSegment)mySegments.get(j0);
               int numk = wseg.numKnots();
               Point3d p0 = mySegmentSpecs.get(nidx0).myPntB.getPosition();
               int k0 = wseg.findNearestKnot (p0);
               pnts.add (p0);
               if (k0 < numk-1) {
                  Vector3d disp = new Vector3d();
                  disp.sub (p0, wseg.getKnotPos (k0));
                  for (int k=k0+1; k<numk; k++) {
                     Point3d px = new Point3d();
                     px.scaledAdd (
                        (numk-k)/(double)(numk-k0), disp, wseg.getKnotPos(k));
                     pnts.add (px);
                  }
               }
            }
         }
         for (int j=j0; j<=j1; j++) {
            Segment seg = mySegments.get(j);
            pnts.add (seg.myPntB.getPosition());
            if (seg instanceof WrapSegment) {
               WrapSegment wseg = (WrapSegment)seg;
               for (int i=0; i<wseg.numKnots(); i++) {
                  pnts.add (wseg.getKnot(i).myPos);
               }
            }
         }
         if (pidx1 < nidx1) {
            // previous extends back before current segment
            if (mySegments.get(j1) instanceof WrapSegment) {
               WrapSegment wseg = (WrapSegment)mySegments.get(j1);
               int numk = wseg.numKnots();
               Point3d p1 = mySegmentSpecs.get(nidx1).myPntB.getPosition();
               int k1 = wseg.findNearestKnot (p1);
               if (k1 > 0) {
                  Vector3d disp = new Vector3d();
                  disp.sub (p1, wseg.getKnotPos (k1));
                  for (int k=0; k<k1; k++) {
                     Point3d px = new Point3d();
                     px.scaledAdd (
                        (k+1)/(double)(k1+1), disp, wseg.getKnotPos(k));
                     pnts.add (px);
                  }
               }
               pnts.add (p1);
            }
         }
      }
      newseg.initializeStrand (pnts);
   }

   /**
    * Update the segments to reflect a change in the activity of
    * conditional points.
    */
   private void updateForConditionals (DynamicIntArray newActivePnts) {
      ArrayList<Segment> segs = new ArrayList<>();

      // create a list of the old active points
      DynamicIntArray oldActivePnts = new DynamicIntArray();
      for (int j=0; j<mySegments.size(); j++) {
         oldActivePnts.add (mySegments.get(j).mySpecIdx);
      }
      oldActivePnts.add (numSegmentSpecs());

      // System.out.println ("conditionals: " + getName());
      // System.out.println ("  " + new VectorNi(oldActivePnts.getArray()));
      // System.out.println ("  " + new VectorNi(newActivePnts.getArray()));

      // Iterate through all the new segments, based on the point (spec)
      // indices in newActivePoints. Each new segment is defined by the spec
      // indices (nidx0, nidx1). As we iterative, keep track of the nearest
      // trailing old segment. j0 is the nearest old segment whose spec index
      // is <= nid0, while j1 is the nearest old segment whose spec index is <=
      // nidx1. Note that j0 and j1 may be the same.
      int nidx0 = 0;

      int j0 = 0; // index into old segments
      for (int i=1; i<newActivePnts.size(); i++) {
         int nidx1 = newActivePnts.get(i); // new segment index
         int j1 = j0;
         while (j1+1 < oldActivePnts.size() && oldActivePnts.get(j1+1) <= nidx1) {
            j1++;
         }
         Segment seg;
         int numk = 0;
         if (oldActivePnts.get(j1) == nidx1) {
            // new and old points at segment end coincide
            int pidx0 = oldActivePnts.get(j0);
            if (pidx0 == nidx0 && j1-j0 == 1) {
               // segment unchanged; use the old one
               seg = mySegments.get(pidx0);
            }
            else {
               // new segment consolidates several segments
               numk = findNumSegmentKnots (nidx0, nidx1);
               if (numk > 0) {
                  // new segment is wrappable
                  seg = createWrapSegment (numk);
               }
               else {
                  // new segment is not wrappable
                  seg = new Segment();
               }
               seg.mySpecIdx = nidx0;
               seg.setPntB (mySegmentSpecs.get(nidx0).myPntB);
               seg.myBlockIdxB = nidx0;
               seg.myPntA = mySegmentSpecs.get(nidx1).myPntB;
               seg.myBlockIdxA = nidx1;
               seg.setPassive (isSegmentPassive (nidx0, nidx1));
            }
         }
         else {
            // new segment includes a fraction of a previous segment
            numk = findNumSegmentKnots (nidx0, nidx1);
            if (numk > 0) {
               // new segment is wrappable
               seg = createWrapSegment (numk);
            }
            else {
               // new segment is not wrappable
               seg = new Segment();
            }
            seg.mySpecIdx = nidx0;
            seg.setPntB (mySegmentSpecs.get(nidx0).myPntB);
            seg.myBlockIdxB = nidx0;
            seg.myPntA = mySegmentSpecs.get(nidx1).myPntB;
            seg.myBlockIdxA = nidx1;
            seg.setPassive (isSegmentPassive (nidx0, nidx1));
         } 
         if (numk > 0) {
            // initialize wrap segment from old segmnet
            initializeStrandFromPrevious (
               (WrapSegment)seg, nidx0, nidx1, j0, j1);
         }
         segs.add (seg);
         nidx0 = nidx1;
      }
      mySegments = segs;
   }

   /**
    * Checks to see if conditional point activity has changed, and
    * if it has, updates the segment structure and returns {@code true}.
    */
   boolean maybeUpdateForConditionals() {
      ArrayList<Segment> segs = getSegments();

      // get currently active segments
      DynamicIntArray newActivePnts = new DynamicIntArray();
      for (int k=0; k<mySegmentSpecs.size(); k++) { 
         if (mySegmentSpecs.get(k).isPointActive()) {
            newActivePnts.add (k);
         }
      }
      // see if there is a change
      if (newActivePnts.size() != segs.size()+1) {
         updateForConditionals(newActivePnts);
         return true;
      }
      for (int i=0; i<segs.size(); i++) {
         if (newActivePnts.get(i) != segs.get(i).mySpecIdx) {
            updateForConditionals(newActivePnts);
            return true;
         }
      }
      return false;
   }

   /**
    * Updates a list of segments so that their end points correspond to the
    * points of this spring.  If {@code updateWrapSegs} is {@code true}, then
    * the wrap paths of any wrappable segments will also be updated.
    */
   protected void updateSegs (
      ArrayList<Segment> segments, boolean updateWrapSegs) {
      // XXX finish
      // int nump = numPoints();
      // // make sure segment pointers are correct
      // for (int i=0; i<nump-1; i++) {
      //    Segment seg = segments.get(i);
      //    Segment segNext = segments.get(i+1);
      //    if (seg.myPntA != segNext.myPntB) {
      //       // then this segment was changed
      //       seg.myPntA = segNext.myPntB;
      //       if (updateWrapSegs && seg instanceof WrapSegment) {
      //          WrapSegment wrapSeg = (WrapSegment)seg;
      //          wrapSeg.initializeStrand (/*initialPnts=*/null);
      //          wrapSeg.updateWrapStrand(myMaxWrapIterations);
      //          // A/B points are computed here:
      //          wrapSeg.updateSubSegments();
      //       }
      //    }
      // }
      // segments.get(nump-1).myPntA = null;     
   }

   /**
    * Updates the wrap path for all wrappable segments in this spring, using a
    * secified maximum number of iterations.
    *
    * @param maxIter maximum number of iterations
    */
   public void updateWrapSegments (int maxIter) {
      if (myProfilingP) {
         myProfileTimer.restart();
      }
      for (Segment seg : getSegments()) {
         if (seg instanceof WrapSegment) {
            WrapSegment wrapSeg = (WrapSegment)seg;
            wrapSeg.updateWrapStrand(maxIter);
            // A/B points are computed here:
            wrapSeg.updateSubSegments();
         }
      }
      if (myProfilingP) {
         myProfileTimer.stop();
         myProfileCnt++;
         if (myPrintProfilingP && myProfileCnt > 0 && (myProfileCnt % 100) == 0) {
            System.out.println (
               "MultiPointSpring: updateWrapSegments time=" + 
               myProfileTimer.result(myProfileCnt));
            myProfileTimer.reset();
         }
      }
   }      

   public boolean hasWrappableSegments() {
      if (myHasWrappableSegs == -1) {
         myHasWrappableSegs = 0;
         for (int k=0; k<numSegmentSpecs(); k++) {
            if (mySegmentSpecs.get(k).isWrappable()) {
               myHasWrappableSegs = 1;
               break;
            }
         }
      }
      return myHasWrappableSegs == 1;
   }

   public boolean hasConditionalPoints() {
      if (myHasConditionalPoints == -1) {
         myHasConditionalPoints = 0;
         for (int k=0; k<numSegmentSpecs(); k++) {
            if (mySegmentSpecs.get(k).isPointConditional()) {
               myHasConditionalPoints = 1;
               break;
            }
         }
      }
      return myHasConditionalPoints == 1;
   }

   public boolean hasMovingMarkers() {
      if (myHasMovingMarkers == -1) {
         myHasMovingMarkers = 0;
         for (int k=0; k<numSegmentSpecs(); k++) {
            if (mySegmentSpecs.get(k).isPointMoving()) {
               myHasMovingMarkers = 1;
               break;
            }
         }
      }
      return myHasMovingMarkers == 1;
   }

   /* === rendering methods === */

   /**
    * Updates the render properties used to render A/B points.
    */
   private void updateABRenderProps() {
      if (myABRenderProps == null) {
         myABRenderProps = new PointRenderProps();
      }
      myABRenderProps.setPointColor (myABPointColor);
      myABRenderProps.setPointStyle (myRenderProps.getPointStyle());
      myABRenderProps.setPointRadius (1.2*myRenderProps.getPointRadius());
   }

   /**
    * {@inheritDoc}
    */
   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      // just update bounds for the points, since the wrap segments will
      // hug the wrappables, and bounds are updated elsewhere to account for
      // wrappables.
      for (int i=0; i<numPoints(); i++) {
         getPoint(i).updateBounds (pmin, pmax);
      }
   }

   /**
    * Converts the position of a point into an array of three floats.
    */
   private float[] getRenderCoords (Point pnt) {
      Point3d pos = pnt.getPosition();
      return new float[] { (float)pos.x, (float)pos.y, (float)pos.z };
   }

   private static int FREE_KNOTS = 0;
   private static int CONTACTING_KNOTS = 1;

   private static int STRAIGHT_SEGS = 0;
   private static int WRAPPED_SEGS = 1;
   private static int DISPLACEMENTS = 2;

   /**
    * Adds the information to a render object needed to render a specific knot.
    */
   private void addRenderPos (
      RenderObject robj, float[] pos, WrapKnot knot) {
      int vidx = robj.vertex (pos);
      if (knot != null) {
         if (knot.inContact()) {
            robj.pointGroup (CONTACTING_KNOTS);
         }
         else {
            robj.pointGroup (FREE_KNOTS);
         }
         robj.addPoint (vidx);
      }
      if (vidx > 0) {
         robj.addLine (vidx-1, vidx);
      }
   }

   /**
    * Builds a render object for this spring.
    */
   protected RenderObject buildRenderObject() {
      RenderObject robj = new RenderObject();
      robj.createPointGroup();
      robj.createPointGroup();
      // explicitly create a line group, in case the state of the spring
      // causes lines to not be added - in which case, drawLines() would fail
      robj.createLineGroup();
      robj.createLineGroup();
      robj.createLineGroup();
      ArrayList<Segment> segs = getSegments();
      for (int i=0; i<segs.size(); i++) {
         Segment seg = segs.get(i);
         if (seg instanceof WrapSegment) {
            robj.lineGroup (WRAPPED_SEGS);
            addRenderPos (robj, seg.myPntB.getRenderCoords(), null);
            WrapSegment wrapSeg = (WrapSegment)seg;
            for (int k=0; k<wrapSeg.myNumKnots; k++) {
               WrapKnot knot = wrapSeg.myKnots[k];
               addRenderPos (robj, knot.updateRenderPos(), knot);
               
            }
         }
         else {
            robj.lineGroup (STRAIGHT_SEGS);
            addRenderPos (robj, seg.myPntB.getRenderCoords(), null);
         }
         if (i == segs.size()-1) {
            addRenderPos (robj, seg.myPntA.getRenderCoords(), null);
         }
      }
      if (myDrawDisplacements > 0) {
         robj.lineGroup (DISPLACEMENTS);
         int vidxBase = 0;
         for (int i=0; i<segs.size(); i++) {
            Segment seg = segs.get(i);
            if (seg instanceof WrapSegment) {
               WrapSegment wrapSeg = (WrapSegment)seg;
               Vector3d disp = new Vector3d();
               Vector3d tip = new Vector3d();
               for (int k=0; k<wrapSeg.myNumKnots; k++) {
                  WrapKnot knot = wrapSeg.myKnots[k];
                  wrapSeg.myDvec.getSubVector (3*k, disp);
                  tip.scaledAdd (myDrawDisplacements, disp, knot.myPos);
                  int vidx0 = vidxBase + k + 1;
                  int vidx1 = robj.vertex (tip);
                  robj.addLine (vidx0, vidx1);
               }
               vidxBase += 2+wrapSeg.myNumKnots;
            }
         }        
      }
      return robj;
   }

   /**
    * Updates the render object for this spring. Not currently used because
    * render objects are always rebuilt from scratch.
    */
   protected void updateRenderObject (RenderObject robj) {
      // updating the render object involves updating the knot render positions
      ArrayList<Segment> segs = getSegments();
      for (int i=0; i<segs.size(); i++) {
         Segment seg = segs.get(i);
         if (seg instanceof WrapSegment) {
            WrapSegment wrapSeg = (WrapSegment)seg;
            for (int k=0; k<wrapSeg.myNumKnots; k++) {
               wrapSeg.myKnots[k].updateRenderPos();
            }
         }
      }
      robj.notifyPositionsModified();
   }

   /**
    * {@inheritDoc}
    */
   public void prerender (RenderList list) {
      // A render object is used to render the strands and the knots.  AB
      // points are rendered using basic point primitives on a per-segment list
      // of current AB points.

      // Ideally, we want to rebuilt the object when the strand structure
      // changes *or* the knot contact configuration changes. But since we
      // can't currently tell the latter, just rebuild every time:
      myRenderObjValidP = false;
      if (!myRenderObjValidP) {
         RenderObject robj = buildRenderObject();
         myRenderObj = robj;
         myRenderObjValidP = true;
      }
      else {
         updateRenderObject(myRenderObj);
      }

      if (myDrawABPointsP) {
         // for each wrappable segment, update the current list of AB points to
         // be rendered:
         updateABRenderProps();
         ArrayList<Segment> segs = getSegments();
         for (int i=0; i<segs.size(); i++) {
            Segment seg = segs.get(i);
            if (seg instanceof WrapSegment) {
               WrapSegment wrapSeg = (WrapSegment)seg;
               ArrayList<float[]> renderPoints = null;
               SubSegment ss = wrapSeg.firstSubSegment();
               if (ss != null) {
                  renderPoints = new ArrayList<float[]>(10);
                  while (ss!=null) {
                     if (ss.myAttachmentB != null) {
                        renderPoints.add (getRenderCoords (ss.myPntB));
                     }
                     if (ss.myAttachmentA != null) {
                        renderPoints.add (getRenderCoords (ss.myPntA));
                     }
                     ss = ss.myNext;
                  }
               }
               wrapSeg.myRenderABPoints = renderPoints;
            }
         }
      }
   }

   /**
    * Internal method for rendering this spring, using a renderer and a
    * specified set of render properties.
    */
   protected void dorender (Renderer renderer, RenderProps props) {
      RenderObject robj = myRenderObj;

      if (myDrawABPointsP) {
         // draw AB points
         ArrayList<Segment> segs = getSegments();
         for (int i=0; i<segs.size(); i++) {
            Segment seg = segs.get(i);
            if (seg instanceof WrapSegment) {
               WrapSegment wrapSeg = (WrapSegment)seg;
               ArrayList<float[]> renderPoints = wrapSeg.myRenderABPoints;
               if (renderPoints != null) {
                  for (int k=0; k<renderPoints.size(); k++) {
                     renderer.drawPoint (
                        myABRenderProps, renderPoints.get(k), isSelected());
                  }
               }
            }
         }
      }
      
      if (robj != null) {
         double size;

         // draw the strands
         LineStyle lineStyle = props.getLineStyle();
         if (lineStyle == LineStyle.LINE) {
            size = props.getLineWidth();
         }
         else {
            size = props.getLineRadius();
         }
         if (getRenderColor() != null) {
            renderer.setColor (getRenderColor(), isSelected());
         }
         else {
            renderer.setLineColoring (props, isSelected());
         }
         robj.lineGroup (STRAIGHT_SEGS);
         renderer.drawLines (robj, lineStyle, size);
         robj.lineGroup (WRAPPED_SEGS);
         if (lineStyle == LineStyle.SPINDLE) {
            lineStyle = LineStyle.CYLINDER;
         }
         renderer.drawLines (robj, lineStyle, size);

         if (myDrawKnotsP) {
            // draw the knots, if any
            PointStyle pointStyle = props.getPointStyle();
            if (pointStyle == PointStyle.POINT) {
               size = props.getPointSize();
            }
            else {
               size = props.getPointRadius();
            }
            renderer.setPointColoring (props, isSelected());
            robj.pointGroup (FREE_KNOTS);
            renderer.drawPoints (robj, pointStyle, size);
            robj.pointGroup (CONTACTING_KNOTS);
            if (myContactingKnotsColor != null) {
               renderer.setColor (myContactingKnotsColor);
            }
            renderer.drawPoints (robj, pointStyle, size);
         }
         if (myDrawDisplacements > 0) {
            robj.lineGroup (DISPLACEMENTS);
            renderer.setColor (Color.GREEN);
            renderer.drawLines (robj, LineStyle.LINE, 1);
         }
      }
   }     

   /**
    * {@inheritDoc}
    */
   public void render (Renderer renderer, int flags) {
      dorender (renderer, myRenderProps);
   }

   /* === scanning and writing methods === */

   /**
    * Helper method to scan the wrappables of this spring.
    */
   protected void scanWrappables (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) 
      throws IOException {
      tokens.offer (new StringToken ("wrappables", rtok.lineno()));
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN);
      myWrappables = new ArrayList<>();
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         WrappableSpec wspec = new WrappableSpec(null);
         if (!ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
            throw new IOException ("Expected wrappble path, got: "+rtok);
         }
         wspec.myPntIdx0 = rtok.scanInteger();
         wspec.myPntIdx1 = rtok.scanInteger();
         myWrappables.add (wspec);
      }
      tokens.offer (ScanToken.END);
   }

   /**
    * Helper method to scan the segment specs of this spring.
    */
   protected void scanSegmentSpecs (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) 
      throws IOException {
      tokens.offer (new StringToken ("segmentSpecs", rtok.lineno()));
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN);
      mySegmentSpecs = new ArrayList<>();
      while (rtok.nextToken() != ']') {
         SegmentSpec spec = new SegmentSpec();
         if (rtok.ttype != '[') {
            throw new IOException ("Expecting token '[', got "+rtok);
         }
         tokens.offer (ScanToken.BEGIN);
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            if (!spec.scanItem (rtok, tokens)) {
               throw new IOException ("Unexpected token: "+rtok);
            }
         }
         tokens.offer (ScanToken.END); // terminator token
         mySegmentSpecs.add (spec);
      }
      tokens.offer (ScanToken.END);
   }

   /**
    * Helper method to postscan the segment specs of this spring.
    */
   protected void postscanSegmentSpecs (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      ScanToken tok = tokens.poll();
      if (tok != ScanToken.BEGIN) {
         throw new IOException (
            "BEGIN token expected for segment list, got " + tok);
      }
      for (int i=0; i<mySegmentSpecs.size(); i++) {
         SegmentSpec spec = mySegmentSpecs.get(i);
         tok = tokens.poll();
         if (tok != ScanToken.BEGIN) {
            throw new IOException (
               "BEGIN token expected for segment "+i+", got " + tok);
         }
         while (tokens.peek() != ScanToken.END) {
            if (!spec.postscanItem (tokens, ancestor)) {
               throw new IOException (
                  "Unexpected token for segment "+i+": " + tokens.poll());
            }
         }
         tokens.poll(); // eat END token      
      }
      tok = tokens.poll();
      if (tok != ScanToken.END) {
         throw new IOException (
            "END token expected for segment list, got " + tok);
      }
   }

   /**
    * Helper method to postscan the wrappables of this spring.
    */
   protected void postscanWrappables (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      ScanToken tok = tokens.poll();
      if (tok != ScanToken.BEGIN) {
         throw new IOException (
            "BEGIN token expected for segment list, got " + tok);
      }
      for (int i=0; i<myWrappables.size(); i++) {
         WrappableSpec wspec = myWrappables.get(i);
         wspec.myWrappable = ScanWriteUtils.postscanReference (
            tokens, Wrappable.class, ancestor);
      }
      tok = tokens.poll();
      if (tok != ScanToken.END) {
         throw new IOException (
            "END token expected for segment list, got " + tok);
      }
   }

   /**
    * Helper method to scan the segments of this spring.
    */
   protected void scanSegments (ReaderTokenizer rtok, Deque<ScanToken> tokens) 
      throws IOException {
      rtok.scanToken ('[');
      mySegments = new ArrayList<>();
      while (rtok.nextToken() != ']') {
         Segment seg;
         if (rtok.tokenIsWord ("Segment")) {
            seg = new Segment();
         }
         else if (rtok.tokenIsWord ("WrapSegment")) {
            seg = new WrapSegment();            
            seg.setWrappable (true);
         }
         else {
            throw new IOException (
               "Expecting word token, Segment or WrapSegment, " + rtok); 
         }
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            rtok.pushBack();
            if (!seg.scanItem (rtok, tokens)) {
               throw new IOException ("Unexpected token: "+rtok);
            }
         }
         mySegments.add (seg);
      }
   }

   /**
    * {@inheritDoc}
    */
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "segmentSpecs")) {
         scanSegmentSpecs (rtok, tokens);
         return true;
      }
      else if (scanAttributeName (rtok, "segments")) {
         scanSegments (rtok, tokens);
         return true;
      }
      else if (scanAttributeName (rtok, "wrappables")) {
         scanWrappables (rtok, tokens);
         return true;
      }
      else if (scanAttributeName (rtok, "maxWrapDisplacement")) {
         myMaxWrapDisplacement = rtok.scanNumber();
         myMaxWrapDisplacementExplicitP = true;
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }  

   /**
    * {@inheritDoc}
    */
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      clearPoints();
      clearWrappables();
      super.scan (rtok, ref);
   }

   /**
    * {@inheritDoc}
    */
   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (postscanAttributeName (tokens, "segmentSpecs")) {
         postscanSegmentSpecs (tokens, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "wrappables")) {
         postscanWrappables (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /**
    * {@inheritDoc}
    */
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      super.postscan (tokens, ancestor);
      if (mySegments != null) {
         setSegmentPointsFromSpecs (mySegments);
         for (Segment seg : mySegments) {
            if (seg instanceof WrapSegment) {
               WrapSegment wrapSeg = (WrapSegment)seg;
               if (wrapSeg.inContact()) {
                  wrapSeg.updateContacts (/*getNormalDeriv=*/true);
               }
               wrapSeg.updateSubSegments();
               wrapSeg.myLength = wrapSeg.computeLength();
            }
         }
      }
      allocateSolveBlocks();
   }
   
   /**
    * Helper method to write information about the wrappables
    */
   protected void writeWrappables (
      PrintWriter pw, CompositeComponent ancestor) throws IOException {
      int numw = 0;
      for (WrappableSpec wspec : myWrappables) {
         if (wspec.myWrappable.isWritable()) {
            numw++;
         }
      }
      if (numw == 0) {
         pw.println ("wrappables=[ ]");
      }
      else {
         pw.println ("wrappables=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (WrappableSpec wspec : myWrappables) {
            Wrappable w = wspec.myWrappable;
            if (w.isWritable()) {
               pw.print (ComponentUtils.getWritePathName (ancestor, w));
               pw.println (" " + wspec.myPntIdx0 + " " + wspec.myPntIdx1);
            }
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }
   }

   /**
    * Helper method to write the segment specs of this spring.
    */
   protected void writeSegmentSpecs (
      PrintWriter pw, ArrayList<SegmentSpec> specs, 
      NumberFormat fmt, CompositeComponent ancestor) 
      throws IOException {
      
      pw.println ("segmentSpecs=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int i=0; i<specs.size(); i++) {
         SegmentSpec spec = specs.get(i);
         pw.print ("[ ");
         IndentingPrintWriter.addIndentation (pw, 2);
         spec.writeItems (pw, fmt, ancestor);
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");      
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");      
   }

   /**
    * Helper method to write the segments of this spring.
    */
   protected void writeSegments (
      PrintWriter pw, ArrayList<Segment> segments, 
      NumberFormat fmt, CompositeComponent ancestor) 
      throws IOException {
      
      pw.println ("segments=[");
      IndentingPrintWriter.addIndentation (pw, 2);
      for (int i=0; i<segments.size(); i++) {
         Segment seg = segments.get(i);
         if (seg instanceof WrapSegment) {
            pw.println ("WrapSegment [");
         }
         else {
            pw.println ("Segment [");
         }
         IndentingPrintWriter.addIndentation (pw, 2);
         seg.writeItems (pw, fmt, ancestor);
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");      
      }
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");      
   }

   /**
    * Returns {@code true} if all segments in this spring are writable.
    */
   private boolean allSegmentsWritable() {
      for (int i=1; i<mySegmentSpecs.size(); i++) {
         if (!mySegmentSpecs.get(i).myPntB.isWritable()) {
            return false;
         }
      }
      return true;
   }

   /**
    * Creates a cloned list of all writable segment specs.
    */
   private ArrayList<SegmentSpec> cloneWritableSegmentSpecs() {
      ArrayList<SegmentSpec> specs = new ArrayList<>();

      for (int i=0; i<mySegmentSpecs.size(); i++) {
         if (i==0 || mySegmentSpecs.get(i).myPntB.isWritable()) {
            specs.add (mySegmentSpecs.get(i).clone());
         }
      }
      // XXX finish
      // updateSegs (segments, /*updateWrapSegs=*/true);
      return specs;
   }
      
   /**
    * Creates a cloned list of all writable segments.
    */
   private ArrayList<Segment> cloneWritableSegments() {
      ArrayList<Segment> segments = new ArrayList<>();

      ArrayList<Segment> segs = getSegments();
      for (int i=0; i<segs.size(); i++) {
         if (i==0 || segs.get(i).myPntB.isWritable()) {
            segments.add (segs.get(i).clone());
         }
      }
      updateSegs (segments, /*updateWrapSegs=*/true);
      return segments;
   }
      
   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      // write wrappable specs
      writeWrappables (pw, ancestor);
      // write segment specs
      if (allSegmentsWritable()) {
         writeSegmentSpecs (pw, mySegmentSpecs, fmt, ancestor);
         writeSegments (pw, getSegments(), fmt, ancestor);
      }
      else {
         writeSegmentSpecs (pw, cloneWritableSegmentSpecs(), fmt, ancestor);
         writeSegments (pw, cloneWritableSegments(), fmt, ancestor);
      }
      if (myMaxWrapDisplacementExplicitP) {
         pw.print ("maxWrapDisplacement=" + fmt.format(myMaxWrapDisplacement));
      }
      super.writeItems (pw, fmt, ancestor);
   }

   /* === scaling and transformation === */

   /**
    * {@inheritDoc}
    */
   public void scaleDistance (double s) {
      super.scaleDistance (s);
      if (myMaterial != null) {
         myMaterial.scaleDistance (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
      if (mySegments != null) {
         for (int i=0; i<mySegments.size(); i++) {
            mySegments.get(i).scaleDistance (s);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void scaleMass (double s) {
      super.scaleMass (s);
      if (myMaterial != null) {
         myMaterial.scaleMass (s);
      }
   }

   /**
    * {@inheritDoc}
    */
   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   /**
    * {@inheritDoc}
    */
   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      // just transform the segments. Points and wrappables will be
      // transformed elsewhere 
      for (SegmentSpec spec : mySegmentSpecs) {
         spec.transformGeometry (gtr);
      }
      if (mySegments != null) {
         for (Segment seg : mySegments) {
            seg.transformGeometry (gtr);
         }
      }
   }
   
   /**
    * {@inheritDoc}
    */
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   /* === copying and structure management === */

   /**
    * Returns {@code false}, since copying of MultiPointSpring is not currently
    * implemented.
    */
   public boolean isDuplicatable() {
      return false;
   }

   /**
    * Does nothing and returns {@code false}, since copying of MultiPointSpring
    * is not currently implemented.
    */
   public boolean getCopyReferences (
      List<ModelComponent> refs, ModelComponent ancestor) {
      // copying not currently supported
      return false;
   }

   /**
    * Makes a copy of this spring. Not currently implemented.
    */
   public ModelComponent copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      MultiPointSpring comp = (MultiPointSpring)super.copy (flags, copyMap);

      // copying not currently supported. This method must be completed
      // if that changes.
      return comp;
   }

   /**
    * {@inheritDoc}
    *
    * Hard references for MultiPointSpring include the two end points.
    */
   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      int nump = numPoints();
      if (nump > 0) {
         refs.add (getPoint(0));
      }
      if (nump > 1) {
         refs.add (getPoint(nump-1));
      }
   }

   /**
    * {@inheritDoc}
    *
    * Soft references for MultiPointSpring include intermediate points between
    * the two end points, and the wrappables.
    */
   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      int nump = numPoints();
      if (nump > 2) {
         for (int i=1; i<nump-1; i++) {
            refs.add (getPoint(i));
         }
      }
      for (WrappableSpec wspec : myWrappables) {
         refs.add (wspec.myWrappable);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);
      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            ((ListRemove<WrappableSpec>)obj).undo();
            maybeInvalidateMaxWrapDisplacement();
         }
         obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            restoreContactInfo ((ArrayList<int[]>)obj);
         }
         obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            ((ListRemove<SegmentSpec>)obj).undo();
            myStateVersion++;
            allocateSolveBlocks();
         }
         obj = undoInfo.removeFirst();
         if (obj != NULL_OBJ) {
            restoreWrappableSegmentRanges ((ArrayList<int[]>)obj);
         }
      }
      else {
         // remove soft references which aren't in the hierarchy any more:
         //DynamicIntArray removedWrappableIndices = new DynamicIntArray();
         ListRemove<WrappableSpec> wrappableRemove = null;
         for (int i=0; i<myWrappables.size(); i++) {
            if (!ComponentUtils.areConnected (
                   this, myWrappables.get(i).myWrappable)) {
               if (wrappableRemove == null) {
                  wrappableRemove = new ListRemove<WrappableSpec>(myWrappables);
               }
               //removedWrappableIndices.add (i);
               wrappableRemove.requestRemove(i);
            }
         }
         int[] wrappableReindexMap = null;
         if (wrappableRemove != null) {
            wrappableRemove.remove();
            wrappableReindexMap = wrappableRemove.getReindexMap();
            undoInfo.addLast (wrappableRemove);
            maybeInvalidateMaxWrapDisplacement();
         }
         else {
            undoInfo.addLast (NULL_OBJ);
         }
         ListRemove<SegmentSpec> specRemove = null;
         for (int i=1; i<mySegmentSpecs.size()-1; i++) {
            if (!ComponentUtils.areConnected (
                   this, mySegmentSpecs.get(i).myPntB)) {
               if (specRemove == null) {
                  specRemove = new ListRemove<SegmentSpec>(mySegmentSpecs);
               }
               specRemove.requestRemove(i);
            }
         }
         if (specRemove != null) {
            // one or more points were removed
            int[] removedSpecIdxs = specRemove.remove();
            undoInfo.addLast (NULL_OBJ); // saved contact info not needed 
            undoInfo.addLast (specRemove);
            undoInfo.addLast (reindexWrappableSegmentRanges(removedSpecIdxs));
            myRenderObjValidP = false;
            mySegments = null;
            myStateVersion++;
            initializeSegments();
            allocateSolveBlocks();
         }
         else {
            if (wrappableReindexMap != null) {
               undoInfo.addLast (reindexContactInfo (wrappableReindexMap));
            }
            else {
               undoInfo.addLast (NULL_OBJ); // saved contact info not needed
            }
            undoInfo.addLast (NULL_OBJ); // spec remove info not needed
            undoInfo.addLast (NULL_OBJ); // saved wrap segment ranges not needed
         }         
         if (specRemove != null || wrappableRemove != null) {
            updateWrapSegments(myMaxWrapIterations);
         }
      }
   }
   /* === length methods === */

   /**
    * Sets the rest length of the spring from the current point locations
    * @return the new rest length
    */
   public double setRestLengthFromPoints() {
      double l = getActiveLength();
      setRestLength(l);
      return l;
   }

   /**
    * Computes the length (or active length, if {@code
    * activeOnly} is {@code true}) of this spring.
    */
   private double computeLength (boolean activeOnly) {
      double len = 0;
      ArrayList<Segment> segs = getSegments();
      for (int i=0; i<segs.size(); i++) {
         Segment seg = segs.get(i);
         
         if (seg.hasSubSegments()) {
            // Segment is a WrapSegment in contact. myLength has already
            // been computed and will not be changed.
            for (SubSegment ss=seg.firstSubSegment(); ss!=null; ss=ss.myNext) {
               ss.updateU();
            }
         }
         else {
            // Segment is not a WrapSegment or is not in contact. myLength
            // will be computed inside update().
            seg.updateU();
         }
         if (activeOnly && seg.isPassive()) {
            continue;
         }
         len += seg.myLength;
      }
      return len;
   }

   /**
    * Computes the derivative of the active length of this spring. The active
    * length is the length of all the <i>active</i> segments.
    *
    * @return active length derivative
    */
   public double getActiveLengthDot() {
      return computeLengthDot (/*activeOnly=*/true);
   }

   /**
    * Computes the derivative of the length of this spring.
    *
    * @return length derivative
    */
   public double getLengthDot() {
      return computeLengthDot (/*activeOnly=*/false);
   }

   /**
    * Computes the derivative of the length (or active length, if {@code
    * activeOnly} is {@code true}) of this spring.
    */
   private double computeLengthDot (boolean activeOnly) {
      double lenDot = 0;
      ArrayList<Segment> segs = getSegments();
      for (int i=0; i<segs.size(); i++) {
         Segment seg = segs.get(i);
         if (activeOnly && seg.isPassive()) {
            continue;
         }
         // TODO: need to make sure uvec is up to for the segments
         if (seg.hasSubSegments()) {
            for (SubSegment ss=seg.firstSubSegment(); ss!=null; ss=ss.myNext) {
               lenDot += ss.getLengthDot();
            }
         }
         else {
            lenDot += seg.getLengthDot();         
         }
      }
      return lenDot;
   }
   
   /**
    * Computes the active length of this spring, corresponding to
    * the length of all the <i>active</i> segments.
    * 
    * @return active length of this spring
    */
   public double getActiveLength() {
      return computeLength (/*activeOnly=*/true);
   }         

   /**
    * Computes the length of this spring.
    *
    * @return length of this spring
    */
   public double getLength() {
      return computeLength (/*activeOnly=*/false);
   }         

   /* === segment updating === */

   /**
    * Update derivative terms for all non-wrappable segments, and subsegments
    * of wrappable segments, in this spring.
    *
    * @param dFdl derivative of tension F with respect to length
    * @param dFdldot derivative of tension F with respect to length dot
    */
   private void updateDfdx (double dFdl, double dFdldot) {
      // assume that PointData v vectors and segment P and u is up to date
      ArrayList<Segment> segs = getSegments();
      for (int i=0; i<segs.size(); i++) {
         Segment seg = segs.get(i);
         if (seg.hasSubSegments()) {
            for (SubSegment ss=seg.firstSubSegment(); ss!=null; ss=ss.myNext) {
               ss.updateDfdx (dFdl, dFdldot);
            }
         }
         else {
            seg.updateDfdx (dFdl, dFdldot);
         }
      }
   }

   /**
    * Updates the P matrix for all non-wrappable segments, and subsegments of
    * wrappable segments, in this spring.
    */
   private void updateP () {
      ArrayList<Segment> segs = getSegments();
      for (int i=0; i<segs.size(); i++) {
         Segment seg = segs.get(i);
         if (seg.hasSubSegments()) {
            for (SubSegment ss=seg.firstSubSegment(); ss!=null; ss=ss.myNext) {
               ss.updateP();
            }
         }
         else {
            seg.updateP();
         }
      }
   }   

   /**
    * {@inheritDoc}
    *
    * <p>For MultiPointSpring, this method updates the spring's structure and wrap
    * segments when {@code t0} is 0. Otherwise, the method does nothing.
    */
   public void preadvance (double t0, double t1, int flags) {
      if (t0 == 0) {
         updateStructure();
         updateWrapSegments(10*myMaxWrapIterations);
      }
   }
   
   /**
    * {@inheritDoc}
    *
    * <p>For MultiPointSpring, this method updates the spring's structure and wrap
    * segments. 
    */
   public void postadvance (double t0, double t1, int flags) {
      updateStructure();
      updateWrapSegments(myMaxWrapIterations);
   }

   public String pathWrapString() {
      ArrayList<Segment> segs = getSegments();
      String str = "";
      for (Segment seg : segs) {
         if (seg.isWrappable()) {
            str += "W";
         }
         else {
            str += "r";
         }
      }
      return str;
   }

   /** 
    * Hook method to allow sub-classes to update their structure by adding
    * or removing points.
    */
   public void updateStructure() {
      if (hasConditionalPoints()) {
         if (maybeUpdateForConditionals()) {
            // update the warp segments, with additional iterations since
            // the knots may be somewhat perturbed.
            updateWrapSegments(5*myMaxWrapIterations);
         }
      }
   }

   // HasSlaveObjects interface. This interface is implemented to enable a call
   // to updateStructure() upon initialization.

   /**
    * {@inheritDoc}
    */
   public void updateSlavePos() {
      // For now, update marker positions here, so that they are updated
      // whenever state is restored. This is current inefficient since
      // updateSlavePos can be called several times at the start of a
      // simulation step, but that's a separate issue.
      if (hasMovingMarkers()) {
         for (Segment seg : getSegments()) {
            if (seg.isPointMoving()) {
               ((MovingMarker)seg.myPntB).updateMarkerLocation();
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void updateSlaveVel() {
   }
   
   public boolean hasState() {
      return hasWrappableSegments() || hasConditionalPoints() || super.hasState();
   }
   
   /**
    * {@inheritDoc}
    */
   public int getStateVersion() {
      int version = myStateVersion;
      if (myStateMat != null) {
         version += myStateMat.getStateVersion();
      }
      return version;
   }

   /**
    * {@inheritDoc}
    */
   public void getState (DataBuffer data) {
      super.getState (data);
      ArrayList<Segment> segs = getSegments();
      data.zput (segs.size());
      for (Segment seg : segs) {
         data.zput (seg.mySpecIdx);
      }
      for (Segment seg : segs) {
         if (seg instanceof WrapSegment) {
            ((WrapSegment)seg).getState (data);
         }
      }
   }         

   private ArrayList<Segment> rebuildSegments (int[] specIdxs) {
      int nsegs = specIdxs.length;
      ArrayList<Segment> segs = new ArrayList<>(nsegs);
      for (int i=0; i<nsegs; i++) {
         int sidx = specIdxs[i];
         int eidx = (i+1 < nsegs ? specIdxs[i+1] : numSegmentSpecs());
         boolean passive = isSegmentPassive (sidx, eidx);
         boolean wrappable = (findNumSegmentKnots (sidx, eidx) > 0);
         Segment seg;
         if (wrappable) {
            seg = createWrapSegment (0);
         }
         else {
            seg = new Segment();
         }
         seg.setPassive (passive);
         seg.mySpecIdx = sidx;
         seg.setPntB (mySegmentSpecs.get(sidx).myPntB);
         seg.myBlockIdxB = sidx;
         seg.myPntA = mySegmentSpecs.get(eidx).myPntB;
         seg.myBlockIdxA = eidx;
         segs.add (seg);
      }
      mySegments = segs;
      return segs;
   }

   private ArrayList<Segment> rebuildSegmentsIfNecessary (int[] specIdxs) {
      ArrayList<Segment> segs = getSegments();
      int nsegs = specIdxs.length;
      if (segs.size() != nsegs) {
         return rebuildSegments (specIdxs);
      }
      for (int i=0; i<segs.size(); i++) {
         if (segs.get(i).mySpecIdx != specIdxs[i]) {
            return rebuildSegments (specIdxs);
         }
      }
      return segs;
   }

   /**
    * {@inheritDoc}
    */
   public void setState (DataBuffer data) {
      super.setState (data);
      int nsegs = data.zget();
      int[] specIdxs = new int[nsegs];
      for (int i=0; i<nsegs; i++) {
         specIdxs[i] = data.zget();
      }
      ArrayList<Segment> segs = rebuildSegmentsIfNecessary (specIdxs);
      for (Segment seg : segs) {
         if (seg instanceof WrapSegment) {
            ((WrapSegment)seg).setState (data);
         }
      }
      if (hasConditionalPoints()) {
         // XXX hack here: check activity of conditional points to
         // update their visibility
         for (SegmentSpec spec : mySegmentSpecs) {
            spec.isPointActive(); // visibility may be updated in this method
         }
      }
   }

   // public void notifyStateVersionChanged() {
   //    myStateVersion++;
   // }

   /**
    * Sets the knot positions for a wrappable current segment
    * indxed by {@code segIdx}. Used for debugging and testing.
    * 
    * @param segIdx index of the current segment
    * @param plist array of knot positions
    */
   public void setKnotPositions (int segIdx, Point3d[] plist) {
      ArrayList<Segment> segs = getSegments();
      if (segIdx >= segs.size()) {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" does not exist");
      }
      Segment seg = segs.get (segIdx);
      if (seg instanceof WrapSegment) {
         ((WrapSegment)seg).setKnotPositions (plist);
      }
      else {
         throw new IllegalArgumentException (
            "Segment "+segIdx+" is not a wrappable segment");
      }
   }

   /**
    * Update the wrapping paths for all wrap segments in this spring.  Since
    * this may be called outside the normal simulation flow, and from an
    * arbitrary starting position, the maximum number of iterations
    * allowed for solving each wrap path is 10 times the default
    * number returned by {@link #getMaxWrapIterations}.
    */
   int cnt = 0;
   public void updateWrapSegments() {
//      if (cnt++ < 3) {
//         System.out.println ("HERE");
//         updateWrapSegments(10*myMaxWrapIterations);
//      }
      updateWrapSegments(10*myMaxWrapIterations);
   }

   /**
    * Returns all the AB points which are currently active on the
    * segments. This should be called in sync with the simulation, since the
    * set of AB points varies across time steps.
    *
    * @param pnts returns the AB points. Will be cleared at the start
    * of the method.
    * @return number of AB points found
    */
   public int getAllABPoints (ArrayList<Point> pnts) {
      pnts.clear();
      ArrayList<Segment> segs = getSegments();
      for (int i = 0; i < segs.size(); i++) {
         Segment seg = segs.get (i);
         if (seg instanceof WrapSegment) {
            WrapSegment wrapSeg = (WrapSegment)seg;
            SubSegment ss = wrapSeg.firstSubSegment ();
            while (ss != null) {
               if (ss.myAttachmentB != null) {
                  pnts.add (ss.myPntB);
               }
               if (ss.myAttachmentA != null) {
                  pnts.add (ss.myPntA);
               }
               ss = ss.myNext;
            }
         }
      }
      return pnts.size();
   }

   /* === force effector methods === */

   /**
    * Applies forces to the points of this spring, as well as any wrappables
    * that are in contact with wrappable segments, resulting from the tension
    * in the spring.
    * 
    * @param t
    * time (seconds)
    */
   public void applyForces (double t) {
      double len = getActiveLength();
      double dldt = getActiveLengthDot();

      double F = computeF (len, dldt);
      // if (F != 0) {
      //    System.out.printf ("  %s: %g\n", getName(), F);
      // }
      ArrayList<Segment> segs = getSegments();
      for (int i=0; i<segs.size(); i++) {
         Segment seg = segs.get(i);
         if (seg.hasSubSegments()) {
            for (SubSegment ss=seg.firstSubSegment(); ss!=null; ss=ss.myNext) {
               ss.applyForce (F);
            }
         }
         else {
            seg.applyForce (F);
         }
      }
   }

   /**
    * Returns the solve index for a Wrappable. This is obtained by assuming
    * that the wrappable is an instance of a RigidBody, and returning the
    * body's solve index. If the wrappabel is not a RigidBody, -1 is returned,
    * indicating no solve index.
    */
   private int getWrappableSolveIndex (int widx) {
      Wrappable wrapobj = getWrappable(widx);
      if (wrapobj instanceof RigidBody) {
         return ((RigidBody)wrapobj).getSolveIndex();
      }
      else {
         return -1;
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      // TODO: FINISH - currently adds solve blocks only for the points
      int nump = numPoints();
      int numw = numWrappables();
      int numBlks = nump+numw;
      if (numBlks != myNumBlks) {
         allocateSolveBlocks();
      }
      int rowSize, colSize, bi, bj;
      if (numBlks > 1) {
         for (int i=0; i<numBlks; i++) {
            if (i < nump) {
               bi = getPoint(i).getSolveIndex();
               rowSize = 3;
            }
            else {
               bi = getWrappableSolveIndex(i-nump);
               rowSize = 6;
            }
            for (int j=0; j<numBlks; j++) {
               if (j < nump) {
                  bj = getPoint(j).getSolveIndex();
                  colSize = 3;
               }
               else {
                  bj = getWrappableSolveIndex(j-nump);
                  colSize = 6;
               }
               MatrixBlock blk = null;
               if (bi != -1 && bj != -1) {
                  blk = M.getBlock (bi, bj);
                  if (blk == null) {
                     blk = MatrixBlockBase.alloc (rowSize, colSize);
                     M.addBlock (bi, bj, blk);
                  }
               }
               if (blk != null) {
                  mySolveBlkNums[i*numBlks+j] = blk.getBlockNumber();
               }
               else {
                  mySolveBlkNums[i*numBlks+j] = -1;
               }
            }
         }
      }
   }

   private void applyBlock (
      SparseNumberedBlockMatrix M, int bi, int bj, Matrix3dBase X, double s) {
      applyBlock (M, bi, bj, null, null, X, s);
   }

   /**
    * Applys {@code s * X} to the block of {@code M} indexed by {@code bi} and
    * {@code bj}. {@code Ji} and {@code Jj} are used to map {@code s X} onto
    * the block in cases where the block is not 3 x 3. Letting {@code B}
    * denote the block, this is done as follows:
    * <pre>
    * B = s X                // B is 3 x 3
    * B = s Ji^T X           // B is 6 x 3
    * B = s X Jj             // B is 3 x 6
    * B = s Ji^T X Jj        // B is 6 x 6
    * </pre>
    */
   private void applyBlock (
      SparseNumberedBlockMatrix M, int bi, int bj, 
      Matrix3x6 Ji, Matrix3x6 Jj, Matrix3dBase X, double s) {

      boolean Jenabled = true;

      int blkNum = mySolveBlkNums[bi*myNumBlks+bj];
      int nump = numPoints();
      MatrixBlock blk = M.getBlockByNumber (blkNum);

      if (blkNum != -1) {
         if (Ji == null) {
            if (Jj == null) {
               // blk is 3 x 3
               blk.scaledAdd (s, X);
            }
            else {
               // blk is 3 x 6
               Matrix3d Xs = new Matrix3d(X);
               Xs.scale (s);
               if (Jenabled) blk.mulAdd (Xs, Jj);
            }
         }
         else {
            if (Jj == null) {
               // blk is 6 x 3
               Matrix3d Xs = new Matrix3d(X);
               Xs.scale (s);
               if (Jenabled) blk.mulTransposeLeftAdd (Ji, Xs);
            }
            else {
               // blk is 6 x 6
               Matrix3x6 XsJj = new Matrix3x6();
               MatrixMulAdd.mulAdd3x6 (XsJj, X, Jj);
               XsJj.scale (s);
               if (Jenabled) blk.mulTransposeLeftAdd (Ji, XsJj);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addPosJacobianOld (SparseNumberedBlockMatrix M, double s) {
      // TODO: FINISH. currently computes Jacobian only for the points

      Matrix3d X = new Matrix3d();
      ArrayList<Segment> segs = getSegments();
      int numSegs = segs.size();
      if (numSegs > 0) {
         double l = getActiveLength();
         double ldot = getActiveLengthDot();
         double F = computeF (l, ldot);
         double dFdl = computeDFdl (l, ldot);
         double dFdldot = computeDFdldot (l, ldot);
         updateP();
         updateDfdx (dFdl, dFdldot);

         for (int i=0; i<numSegs; i++) {
            Segment seg_i = segs.get(i);
            Vector3d uvecB_i, uvecA_i;
            int iA = seg_i.myBlockIdxA;
            int iB = seg_i.myBlockIdxB;
            // if (iA != indexOfPoint(seg_i.myPntA)) {
            //    System.out.printf (
            //       "ERROR: iA=%d, expected %d\n", iA, indexOfPoint(seg_i.myPntA));
            // }
            // if (iB != indexOfPoint(seg_i.myPntB)) {
            //    System.out.printf (
            //       "ERROR: iB=%d, expected %d\n", iB, indexOfPoint(seg_i.myPntB));
            // }
            // if (iA != i+1) {
            //    System.out.printf ("iA=%d, i+1=%d numSegs=%d\n", iA, i+1, numSegs);
            // }
            // if (iB != i) {
            //    System.out.printf ("iB=%d, i=%d\n", iB, i);
            // }
            if (seg_i.hasSubSegments()) {
               SubSegment sub0 = seg_i.firstSubSegment();
               SubSegment subL = seg_i.lastSubSegment();
               for (SubSegment sub = sub0; sub != null; sub = sub.getNext()) {
                  if (F != 0 && sub.myLength != 0) { // avoid NaN
                     X.scale (F/sub.myLength, sub.myP);
                     if (sub == sub0) {
                        applyBlock (M, iB, iB, X, -s);
                     }
                     if (sub == subL) {
                        applyBlock (M, iA, iA, X, -s);
                     }
                  }
               }
               uvecB_i = sub0.myUvec;
               uvecA_i = subL.myUvec;
            }
            else {
               if (F != 0 && seg_i.myLength != 0) { // avoid NaN
                  X.scale (F/seg_i.myLength, seg_i.myP);
                  applyBlock (M, iB, iB, X, -s);
                  applyBlock (M, iA, iB, X, s);
                  applyBlock (M, iB, iA, X, s);
                  applyBlock (M, iA, iA, X, -s);
               }
               uvecB_i = seg_i.myUvec;
               uvecA_i = seg_i.myUvec;
            }
            for (int j=0; j<numSegs; j++) {
               Segment seg_j = segs.get(j);
               Vector3d dFdxB_j, dFdxA_j; 
               int jA = seg_j.myBlockIdxA;
               int jB = seg_j.myBlockIdxB;

               if (seg_j.hasSubSegments()) {
                  dFdxB_j = new Vector3d(seg_j.firstSubSegment().mydFdxA);
                  dFdxB_j.negate();
                  dFdxA_j = seg_j.lastSubSegment().mydFdxA;
               }
               else {
                  dFdxB_j = new Vector3d(seg_j.mydFdxA);
                  dFdxB_j.negate();
                  dFdxA_j = seg_j.mydFdxA;
               }
               X.outerProduct (uvecB_i, dFdxB_j);
               applyBlock (M, iB, jB, X, s);
               applyBlock (M, iA, jB, X, -s);
               X.outerProduct (uvecA_i, dFdxA_j);
               applyBlock (M, iB, jA, X, s);
               applyBlock (M, iA, jA, X, -s);
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      // TODO: FINISH. currently computes Jacobian only for the points
      Matrix3d X = new Matrix3d();
      ArrayList<Segment> segs = getSegments();
      int numSegs = segs.size();
      if (numSegs > 0) {
         double l = getActiveLength();
         double ldot = getActiveLengthDot();
         double F = computeF (l, ldot);
         double dFdl = computeDFdl (l, ldot);
         double dFdldot = computeDFdldot (l, ldot);
         updateP();
         updateDfdx (dFdl, dFdldot);

         //F = 0;
         for (int i=0; i<numSegs; i++) {
            Segment seg_i = segs.get(i);
            if (seg_i.hasSubSegments()) {
               SubSegment sub0 = seg_i.firstSubSegment();
               for (SubSegment subseg=sub0; subseg!=null; subseg=subseg.myNext) {
                  addSegmentPosBlocks (M, subseg, /*F*/F, s);
               }
            }
            else {
               addSegmentPosBlocks (M, seg_i, /*F*/F, s);
            }
         }
      }
      //checkMatrix (M);
   }

   /**
    * For testing: check properties of Jacobian matrices.
    */
   void checkMatrix(SparseNumberedBlockMatrix S) {
      int nump = numPoints();
      int numw = numWrappables();
      int numb = nump+numw;
      int size = 3*nump + 6*numw;
      MatrixNd M = new MatrixNd (size, size);
      int i = 0;
      for (int bi=0; bi<numb; bi++) {
         int j = 0;
         for (int bj=0; bj<numb; bj++) {
            MatrixBlock blk = S.getBlockByNumber (mySolveBlkNums[bi*numb+bj]);
            M.setSubMatrix (i, j, blk);
            j += (bj < nump ? 3 : 6);
         }
         i += (bi < nump ? 3 : 6);
      }
      EigenDecomposition eig = 
         new EigenDecomposition (M, EigenDecomposition.SYMMETRIC);
      double minEig = eig.getEigReal().minElement();
      double maxEig = eig.getEigReal().maxElement();
      if (minEig < -1e-8) {
         int num = 0;
         for (i=0; i<M.rowSize(); i++) {
            if (eig.getEigReal().get(i) < -1e-8) {
               num++;
            }
         }
         System.out.printf (
            " %s matrix BAD minEig %g maxEig %g, num=%d, size %d\n",
            getName(), minEig, maxEig, num, M.rowSize());
         if ("ADM".equals (getName())) {
            System.out.println ("M=\n" + M.toString("%13.8f"));
         }
      }
      
      // if (M.isSymmetric(1e-10)) {
      //    System.out.println ("symmetric OK");
      // }
      // else {
      //    System.out.println ("NOT symmetric");
      // }
   }

   public void addSegmentPosBlocks (
      SparseNumberedBlockMatrix M, Segment seg_i, double F, double s) {

      ArrayList<Segment> segs = getSegments();
      int numSegs = segs.size();
      
      Matrix3d X = new Matrix3d();
      int idxA_i = seg_i.myBlockIdxA;
      int idxB_i = seg_i.myBlockIdxB;
      Matrix3x6 JA_i = seg_i.getJA();
      Matrix3x6 JB_i = seg_i.getJB();
      Vector3d nrmA = seg_i.getNrmlA();
      Vector3d nrmB = seg_i.getNrmlB();

      if (F != 0 && seg_i.myLength != 0) { // avoid NaN
         if (nrmA != null) {
            if (nrmB == null) {
               X.outerProduct (nrmA, nrmA);
            }
            else {
               // Both normals present. Average and scale by the 
               // cosine between them
               Vector3d nrmAvg = new Vector3d();
               nrmAvg.add (nrmA, nrmB);
               nrmAvg.normalize();
               X.outerProduct (nrmAvg, nrmAvg);
               X.scale (nrmA.dot(nrmB)); // cosine between normals
            }
         }
         else if (nrmB != null) {
            X.outerProduct (nrmB, nrmB);
         }
         else {
            X.set (seg_i.myP);
         }
         X.scale (F/seg_i.myLength);
         applyBlock (M, idxB_i, idxB_i, JB_i, JB_i, X, -s);
         applyBlock (M, idxA_i, idxB_i, JA_i, JB_i, X, s);
         applyBlock (M, idxB_i, idxA_i, JB_i, JA_i, X, s);
         applyBlock (M, idxA_i, idxA_i, JA_i, JA_i, X, -s);
      }
      Vector3d uvec = seg_i.myUvec;

      // add DFdx blocks
      for (int j=0; j<numSegs; j++) {
         Segment seg_j = segs.get(j);
         if (seg_j.isPassive()) {
            continue;
         }
         if (seg_j.hasSubSegments()) {
            SubSegment sub0 = seg_j.firstSubSegment();
            for (SubSegment subseg=sub0; subseg!=null; subseg=subseg.myNext) {
               int idxA_j = subseg.myBlockIdxA;
               int idxB_j = subseg.myBlockIdxB;
               Matrix3x6 JA_j = subseg.getJA();
               Matrix3x6 JB_j = subseg.getJB();
               X.outerProduct (uvec, subseg.mydFdxA);
               applyBlock (M, idxB_i, idxB_j, JB_i, JB_j, X, -s);
               applyBlock (M, idxA_i, idxB_j, JA_i, JB_j, X, s);
               X.outerProduct (uvec, subseg.mydFdxA);
               applyBlock (M, idxB_i, idxA_j, JB_i, JA_j, X, s);
               applyBlock (M, idxA_i, idxA_j, JA_i, JA_j, X, -s);
            }
         }
         else {
            int idxA_j = seg_j.myBlockIdxA;
            int idxB_j = seg_j.myBlockIdxB;
            Matrix3x6 JA_j = seg_j.getJA();
            Matrix3x6 JB_j = seg_j.getJB();
            X.outerProduct (uvec, seg_j.mydFdxA);
            applyBlock (M, idxB_i, idxB_j, JB_i, JB_j, X, -s);
            applyBlock (M, idxA_i, idxB_j, JA_i, JB_j, X, s);
            X.outerProduct (uvec, seg_j.mydFdxA);
            applyBlock (M, idxB_i, idxA_j, JB_i, JA_j, X, s);
            applyBlock (M, idxA_i, idxA_j, JA_i, JA_j, X, -s);
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addVelJacobianOld (SparseNumberedBlockMatrix M, double s) {
      // TODO: FINISH. currently computes Jacobian only for the points
      Matrix3d X = new Matrix3d();
      ArrayList<Segment> segs = getSegments();
      int numSegs = segs.size();
      if (numSegs > 0) {
         double l = getActiveLength();
         double ldot = getActiveLengthDot();
         double dFdldot = computeDFdldot (l, ldot);
         for (int i=0; i<numSegs; i++) {
            Segment seg_i = segs.get(i);
            int idxA_i = seg_i.myBlockIdxA;
            int idxB_i = seg_i.myBlockIdxB;
            Vector3d uvecB_i, uvecA_i;
            if (seg_i.hasSubSegments()) {
               uvecB_i = seg_i.firstSubSegment().myUvec;
               uvecA_i = seg_i.lastSubSegment().myUvec;
            }
            else {
               uvecB_i = seg_i.myUvec;
               uvecA_i = seg_i.myUvec;
            }
            for (int j=0; j<numSegs; j++) {
               Segment seg_j = segs.get(j);
               int idxA_j = seg_j.myBlockIdxA;
               int idxB_j = seg_j.myBlockIdxB;
               Vector3d uvecB_j, uvecA_j;
               if (!seg_j.isPassive()) {
                  if (seg_j.hasSubSegments()) {
                     uvecB_j = seg_j.firstSubSegment().myUvec;
                     uvecA_j = seg_j.lastSubSegment().myUvec;
                  }
                  else {
                     uvecB_j = seg_j.myUvec;
                     uvecA_j = seg_j.myUvec;
                  }
                  X.outerProduct (uvecB_i, uvecB_j);
                  applyBlock (M, idxB_i, idxB_j, X, -s*dFdldot);
                  if (uvecB_j != uvecA_j) {
                     X.outerProduct (uvecB_i, uvecA_j);
                  }
                  applyBlock (M, idxB_i, idxA_j, X, s*dFdldot);                  
                  X.outerProduct (uvecA_i, uvecB_j);
                  applyBlock (M, idxA_i, idxB_j, X, s*dFdldot);
                  if (uvecB_j != uvecA_j) {
                     X.outerProduct (uvecA_i, uvecA_j);
                  }
                  applyBlock (M, idxA_i, idxA_j, X, -s*dFdldot);
               }
            }
         }
      }
   }

   /**
    * {@inheritDoc}
    */
   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      // TODO: FINISH. currently computes Jacobian only for the points
      Matrix3d X = new Matrix3d();
      ArrayList<Segment> segs = getSegments();
      int numSegs = segs.size();
      if (numSegs > 0) {
         double l = getActiveLength();
         double ldot = getActiveLengthDot();
         double dFdldot = computeDFdldot (l, ldot);
         for (int i=0; i<numSegs; i++) {
            Segment seg_i = segs.get(i);
            if (seg_i.hasSubSegments()) {
               SubSegment sub0 = seg_i.firstSubSegment();
               for (SubSegment subseg=sub0; subseg!=null; subseg=subseg.myNext) {
                  addSegmentVelBlocks (M, subseg, dFdldot, s);
               }
            }
            else {
               addSegmentVelBlocks (M, seg_i, dFdldot, s);
            }
         }
      }
   }

   public void addSegmentVelBlocks (
      SparseNumberedBlockMatrix M, Segment seg_i, double dFdldot, double s) {

      ArrayList<Segment> segs = getSegments();
      int numSegs = segs.size();

      Matrix3d X = new Matrix3d();
      int idxA_i = seg_i.myBlockIdxA;
      int idxB_i = seg_i.myBlockIdxB;
      Matrix3x6 JA_i = seg_i.getJA();
      Matrix3x6 JB_i = seg_i.getJB();

      Vector3d uvec = seg_i.myUvec;

      // add DFdx blocks
      for (int j=0; j<numSegs; j++) {
         Segment seg_j = segs.get(j);
         if (seg_j.isPassive()) {
            continue;
         }
         if (seg_j.hasSubSegments()) {
            SubSegment sub0 = seg_j.firstSubSegment();
            for (SubSegment subseg=sub0; subseg!=null; subseg=subseg.myNext) {
               int idxA_j = subseg.myBlockIdxA;
               int idxB_j = subseg.myBlockIdxB;
               Matrix3x6 JA_j = subseg.getJA();
               Matrix3x6 JB_j = subseg.getJB();
               X.outerProduct (uvec, subseg.myUvec);
               applyBlock (M, idxB_i, idxB_j, JB_i, JB_j, X, -s*dFdldot);
               applyBlock (M, idxA_i, idxB_j, JA_i, JB_j, X, s*dFdldot);
               X.outerProduct (uvec, subseg.myUvec);
               applyBlock (M, idxB_i, idxA_j, JB_i, JA_j, X, s*dFdldot);
               applyBlock (M, idxA_i, idxA_j, JA_i, JA_j, X, -s*dFdldot);
            }
         }
         else {
            int idxA_j = seg_j.myBlockIdxA;
            int idxB_j = seg_j.myBlockIdxB;
            Matrix3x6 JA_j = seg_j.getJA();
            Matrix3x6 JB_j = seg_j.getJB();
            X.outerProduct (uvec, seg_j.myUvec);
            applyBlock (M, idxB_i, idxB_j, JB_i, JB_j, X, -s*dFdldot);
            applyBlock (M, idxA_i, idxB_j, JA_i, JB_j, X, s*dFdldot);
            X.outerProduct (uvec, seg_j.myUvec);
            applyBlock (M, idxB_i, idxA_j, JB_i, JA_j, X, s*dFdldot);
            applyBlock (M, idxA_i, idxA_j, JA_i, JA_j, X, -s*dFdldot);
         }
      }
   }

   // /**
   //  * {@inheritDoc}
   //  */
   // public int getJacobianType() {
   //    AxialMaterial mat = getEffectiveMaterial();
   //    if (numPassiveSegments() == 0 &&
   //        (myIgnoreCoriolisInJacobian || mat.isDFdldotZero())) {
   //       return Matrix.SYMMETRIC;
   //    }
   //    else {
   //       return 0;
   //    }
   // }
   
   /* === inner classes for knot and segments === */

   /**
    * Stores information for a single knot point in a wrappable segment.
    */
   public class WrapKnot implements Clonable {
      
      public Point3d myPos;      // knot position 
      public Point3d myLocPos;   // local position wrt wrappble if in contact 
      public Vector3d myForce;   // wrapping force on the knot
 
      // attributes used if the knot is is contact with a wrappable:
      Vector3d myNrml;           // contact normal
      Matrix3d myDnrm;           // derivative of normal wrt knot position
      double myDist;             // distance to surface
      double myPrevDist;         // previous distance to surface
      int myWrappableIdx;        // index of contacting wrappable
      int myPrevWrappableIdx;    // index of previous contacting wrappable
      int myContactGid;          // group ID for consecutive contacting knots

      // attributes used to store knot's components in the block triadiagonal
      // stiffness/force system
      Matrix3d myBmat;
      Matrix3d myBinv;
      Matrix3d myCinv;
      Vector3d myDvec;
      
      float[] myRenderPos;          // knot position used for rendering
      
      /**
       * Sets the index of the wrappable this knot is contacting, or -1 to
       * indicate no contact.
       */
      public void setWrappableIdx (int idx) {
         myWrappableIdx = idx;
      }
      
      /**
       * Gets the index of the wrappable this knot is contacting, or -1 if
       * there is no contact.
       */
      public int getWrappableIdx () {
         return myWrappableIdx;
      }

      /**
       * Returns true if this knot is in contact.
       */
      public boolean inContact() {
         return myWrappableIdx != -1;
      }

      /**
       * Returns the contact distance if this knot is in contact. Contact
       * distances are negative.
       */
      public double getContactDistance() {
         return myDist;
      }

      /**
       * Returns the contact normal for this knot. Only valid when
       * the knot is in contact.
       */
      public Vector3d getContactNormal() {
         return myNrml;
      }

      /**
       * Returns the wrappable this knot is in contact with, or null.
       */
      public Wrappable getWrappable() {
         if (myWrappableIdx < 0) {
            return null;
         }
         else {
            return myWrappables.get(myWrappableIdx).myWrappable;
         }
      }

      /**
       * Returns the wrappable this knot was previously in contact with, or
       * null.
       */
      public Wrappable getPrevWrappable() {
         if (myPrevWrappableIdx < 0) {
            return null;
         }
         else {
            return myWrappables.get(myPrevWrappableIdx).myWrappable;
         }
      }

      /**
       * Constructs a new knot
       */
      WrapKnot () {
         myPos = new Point3d();
         myLocPos = new Point3d();
         myForce = new Vector3d();
         myNrml = new Vector3d();
         myDnrm = new Matrix3d();
         myBmat = new Matrix3d();
         myBinv = new Matrix3d();
         myCinv = new Matrix3d();
         myDvec = new Vector3d();
         myRenderPos = new float[3];
         myWrappableIdx = -1;
         myPrevWrappableIdx = -1;
         myDist = Wrappable.OUTSIDE;
         myPrevDist = Wrappable.OUTSIDE;
      }

      /**
       * Updates the render position for this knot.
       */
      float[] updateRenderPos() {
         myRenderPos[0] = (float)myPos.x;
         myRenderPos[1] = (float)myPos.y;
         myRenderPos[2] = (float)myPos.z;
         return myRenderPos;
      }

      /**
       * Writes this knot to a PrintWriter. Used when writing the spring to a
       * file.
       */
      void write (PrintWriter pw, NumberFormat fmt) throws IOException {
         pw.print ("[ ");
         pw.print ("pos=");
         myPos.write (pw, fmt, true);
         IndentingPrintWriter.addIndentation (pw, 2);
         pw.println ("");
         if (!myLocPos.equals (Vector3d.ZERO)) {
            pw.print ("loc=");
            myLocPos.write (pw, fmt, true);
            pw.println ("");
         }
         if (myWrappableIdx != -1) {
            pw.println ("dist=" + fmt.format(myDist));
            pw.println ("wrapIdx=" + myWrappableIdx);
         }
         if (myPrevWrappableIdx != -1) {
            pw.println ("prevDist=" + fmt.format(myPrevDist));
            pw.println ("prevWrapIdx=" + myPrevWrappableIdx);
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }

      /**
       * Scan this knot from a ReaderTokenizer. Used when reading the spring
       * from a file.
       */
      void scan (ReaderTokenizer rtok) throws IOException {
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            if (ScanWriteUtils.scanAttributeName (rtok, "pos")) {
               myPos.scan (rtok);
            }
            else if (ScanWriteUtils.scanAttributeName (rtok, "loc")) {
               myLocPos.scan (rtok);
            }
            else if (ScanWriteUtils.scanAttributeName (rtok, "prevDist")) {
               myPrevDist = rtok.scanNumber();
            }
            else if (ScanWriteUtils.scanAttributeName (rtok, "prevWrapIdx")) {
               myPrevWrappableIdx = rtok.scanInteger();
            }
            else if (ScanWriteUtils.scanAttributeName (rtok, "dist")) {
               myDist = rtok.scanNumber();
            }
            else if (ScanWriteUtils.scanAttributeName (rtok, "wrapIdx")) {
               myWrappableIdx = rtok.scanInteger();
            }
            else {
               throw new IOException ("Unrecognized token " + rtok);
            }
         }
      }

      /**
       * Clones a copy of this knot.
       */
      public WrapKnot clone() {
         WrapKnot knot;
         try {
            knot = (WrapKnot)super.clone();
         }
         catch (Exception e) {
            throw new InternalErrorException ("Can't clone WrapKnot");
         }
         knot.myPos = new Point3d(myPos);
         knot.myLocPos = new Point3d(myLocPos);
         knot.myForce = new Vector3d(myForce);

         knot.myNrml = new Vector3d(myNrml);
         knot.myDnrm = new Matrix3d(myDnrm);
         knot.myBmat = new Matrix3d(myBmat);
         knot.myBinv = new Matrix3d(myBinv);
         knot.myCinv = new Matrix3d(myCinv);
         knot.myDvec = new Vector3d(myDvec);
         knot.myRenderPos = new float[3];
         return knot;
      }

      String cellInfo() {
         if (inContact()) {
            Wrappable wrappable = getWrappable();
            if (wrappable instanceof RigidBody) {
               DistanceGrid grid = ((RigidBody)wrappable).getDistanceGrid();
               return grid.getQuadCellInfo (myPos);
            }
         }
         return "";
      }

   }

   /**
    * Gives information about a specific wrappable, including points delimiting
    * the range of spring segments for which the wrappable should be applied.
    */
   static class WrappableSpec {
      Wrappable myWrappable;
      int myPntIdx0; // index of first point (inclusive)
      int myPntIdx1; // index of last point (exclusive)
 
      WrappableSpec (Wrappable w) {
         myWrappable = w;
         myPntIdx0 = -1;
         myPntIdx1 = -1;
      }

      WrappableSpec (Wrappable w, int pntIdx0, int pntIdx1) {
         myWrappable = w;
         setPointIndices (pntIdx0, pntIdx1);
      }
      
      void setPointIndices (int pntIdx0, int pntIdx1) {
         myPntIdx0 = pntIdx0;
         myPntIdx1 = pntIdx1;
      }
      
      int[] reindexSegmentRanges (int[] removedSegmentIdxs) {
         if (myPntIdx0 == -1 && myPntIdx1 == -1) {
            return null;
         }
         else {
            int idx0 = myPntIdx0;
            int idx1 = myPntIdx1;
            int[] saved = new int[] { idx0, idx1 };
            for (int ridx : removedSegmentIdxs) {
               if (ridx < myPntIdx0) {
                  idx0--;
                  idx1--;
               }
               else if (ridx < myPntIdx1) {
                  idx1--;
               }
            }
            myPntIdx0 = idx0;
            myPntIdx1 = idx1;
            return saved;
         }
      }
  }

   /**
    * Base class for both SegmentSpec and Segment. Contains flag information.
    */
   static class SegmentBase {

      int myFlags;

      // flags describing characteristic of specific segments
      static final int SEG_POINT_CONDITIONAL = 0x0002;
      static final int SEG_POINT_MOVING = 0x0004;
      static final int SEG_WRAPPABLE = 0x0008;
      static final int SEG_PASSIVE = 0x0010;
   
      void setFlag (int flag, boolean enable) {
         if (enable) {
            myFlags |= flag;
         }
         else {
            myFlags &= ~flag;
         }
      }

      boolean isPointConditional() {
         return (myFlags & SEG_POINT_CONDITIONAL) != 0;
      }

      boolean isPointMoving() {
         return (myFlags & SEG_POINT_MOVING) != 0;
      }

      boolean isPassive() {
         return (myFlags & SEG_PASSIVE) != 0;
      }

      void setPassive (boolean enable) {
         setFlag (SEG_PASSIVE, enable);
      }

      boolean isWrappable() {
         return (myFlags & SEG_WRAPPABLE) != 0;
      }

      void setWrappable (boolean enable) {
         setFlag (SEG_WRAPPABLE, enable);
      }
   }

   /**
    * Stores specification information for the segments of this spring.
    */
   static public class SegmentSpec extends SegmentBase implements Clonable {
      Point myPntB; 
      ConditionalPoint myCondB;

      int myNumKnots;

      // Optional list of points that are used to help provide an initial
      // "path" for the segment
      Point3d[] myInitialPnts;

//      boolean isNumKnotsExplicit() {
//         return (myFlags & NUM_KNOTS_EXPLICIT) != 0;
//      }
//
//      void setNumKnotsExplicit (boolean enable) {
//         setFlag (NUM_KNOTS_EXPLICIT, enable);
//      }
      public Point3d[] getInitialPoints() {
         return myInitialPnts;
      }

      void setPntB (Point p) {
         if (p instanceof ConditionalPoint) {
            myCondB = (ConditionalPoint)p;
            setFlag (SEG_POINT_CONDITIONAL, true);
         }
         else {
            myCondB = null;
            setFlag (SEG_POINT_CONDITIONAL, false);
         }
         setFlag (SEG_POINT_MOVING, p instanceof MovingMarker);
         myPntB = p;
      }         

      boolean isPointActive() {
         if (myCondB != null) {
            return myCondB.isPointActive();
         }
         else {
            return true;
         }
      }

      public int getNumKnots() {
         return myNumKnots;
      }

      void setNumKnots (int numk) {
         myNumKnots = numk;
      }

      /**
       * Scan attributes of this segment spec from a ReaderTokenizer. Used when
       * reading the spring from a file.
       */
      boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
         throws IOException {

         rtok.nextToken();
         if (ScanWriteUtils.scanAttributeName (rtok, "flags")) {
            myFlags = rtok.scanInteger();
            return true;
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "numKnots")) {
            myNumKnots = rtok.scanInteger();
            return true;
         }
         else if (ScanWriteUtils.scanAndStoreReference (rtok, "pntB", tokens)) {
            return true;
         }
         else if (ScanWriteUtils.scanAndStoreReference (rtok, "pntA", tokens)) {
            return true;
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "initialPoints")) {
            Vector3d[] list = ScanWriteUtils.scanVector3dList (rtok);
            setInitialPoints (list);
            return true;
         }
         rtok.pushBack();
         return false;
      }

      /**
       * Postscans end-point information for this segment spec. Used to when
       * reading the spring from a file.
       */
      boolean postscanItem (
         Deque<ScanToken> tokens, CompositeComponent ancestor)
         throws IOException {
         if (ScanWriteUtils.postscanAttributeName (tokens, "pntB")) {
            setPntB (ScanWriteUtils.postscanReference (
                        tokens, Point.class, ancestor));
            return true;
         }
         return false;         
      }

      /**
       * Writes attributes of this segment spec to a PrintWriter. Used when
       * writing the spring to a file.
       */
      void writeItems (
         PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {
         
         pw.println (
            "pntB=" + ComponentUtils.getWritePathName (ancestor, myPntB));
         pw.println ("flags=0x" + Integer.toHexString(myFlags));
         if (myNumKnots > 0) {
            pw.println ("numKnots=" + myNumKnots);
         }
         if (myInitialPnts != null) {
            pw.print ("initialPoints=");
            ScanWriteUtils.writeVector3dList (pw, fmt, myInitialPnts);
         }
      }

      /**
       * Transforms the geometry of this segment spec
       */      
      void transformGeometry (GeometryTransformer gtr) {
         if (myInitialPnts != null) {
            for (Point3d pnt : myInitialPnts) {
               gtr.transformPnt (pnt);
            }
         }
      }

      public void setInitialPoints (Vector3d[] pnts) {
         myInitialPnts = copyPoints (pnts);
      }
      
      public SegmentSpec clone() {
         SegmentSpec spec;
         try {
            spec = (SegmentSpec)super.clone();
         }
         catch (Exception e) {
            throw new InternalErrorException (
               "cannot clone MultiPointSpring.Segment");
         }
         if (myInitialPnts != null) {
            spec.setInitialPoints (myInitialPnts);
         }
         return spec;
      }
   }


   /**
    * Stores information for the segments of this spring. There is a segment
    * between each of this spring's point. Wrappable segments are described in
    * further detail by the subclass {@link WrapSegment}.
    *
    * <p>This class is also used as the baseclass for the {@link SubSegment},
    * which described the subsegments within a wrappable segment.
    */
   public class Segment extends SegmentBase implements Clonable {

      // myPntB and myPntA are the first and last segment end points
      // as we traverse the segments in point order
      public Point myPntB; 
      public Point myPntA; 
      // myBlockIdxA and myBlockIdxB are the indices into the solve block
      // number table mySolveBlkNums for points A and B. If the point is a via
      // point, then the index will be the index into the via point list.
      // Otherwise, if the point is a wrappable contact point, the index will
      // be the index of the wrappable, plus the number of points.
      int myBlockIdxA;     // index into SolveBlkNums for point A
      int myBlockIdxB;     // index into SolveBlkNums for point B
      int mySpecIdx;       // index into the segment specs

      Vector3d mydFdxA;    // derivative of tension force F wrt point A

      Vector3d myUvec;     // unit vector in direction of segment
      Matrix3d myP;        // (I - uvec uvec^T)
      double myLength;     // length of the segment
      
      protected Segment() {
         myP = new Matrix3d();
         myUvec = new Vector3d();
         mydFdxA = new Vector3d();
      }

      void setPntB (Point p) {
         setFlag (SEG_POINT_MOVING, p instanceof MovingMarker);
         myPntB = p;
      }         

      public Point getPntB() {
         return myPntB;
      }

      Matrix3x6 getJA() {
         return null;
      }

      Matrix3x6 getJB() {
         return null;
      }

      Vector3d getNrmlA() {
         return null;
      }

      Vector3d getNrmlB() {
         return null;
      }

      /**
       * Applies the physical tension F to the forces of the segment
       * end-points. Assumes that {@link #updateU} has been called.
       */
      void applyForce (double F) {
         Vector3d f = new Vector3d();
         f.scale (F, myUvec);
         myPntB.addForce (f);
         myPntA.subForce (f);
      }

      /**
       * Updates the unit vector and length of this segment.
       */
      double updateU () {
         myUvec.sub (myPntA.getPosition(), myPntB.getPosition());
         myLength = myUvec.norm();
         if (myLength != 0) {
            myUvec.scale (1 / myLength);
         }
         return myLength;
      }

      /**
       * Computes the derivative of the length of this segment.
       * Assumes that {@link #updateU} has been called.
       */
      double getLengthDot () {
         Vector3d velA = myPntA.getVelocity();
         Vector3d velB = myPntB.getVelocity();
         double dvx = velA.x-velB.x;
         double dvy = velA.y-velB.y;
         double dvz = velA.z-velB.z;
         return myUvec.x*dvx + myUvec.y*dvy + myUvec.z*dvz;
      } 

      /**
       * Updates the P matrix of this segment, defined as
       * <pre>
       * I - u u^T
       * </pre>
       * where u is the segment unit vector.
       * Assumes that {@link #updateU} has been called.
       */
      void updateP () {
         myP.outerProduct (myUvec, myUvec);
         myP.negate();
         myP.m00 += 1;
         myP.m11 += 1;
         myP.m22 += 1;
      }

      /**
       * Updates the derivatives of the tension force F with respect to changes
       * in end-points A and B. Assumes that uvec and P are both up to date.
       */
      void updateDfdx (double dFdl, double dFdldot) {
         if (!isPassive()) {
            mydFdxA.scale (dFdl, myUvec);
            if (!myIgnoreCoriolisInJacobian) {
               Vector3d y = new Vector3d();
               y.sub (myPntA.getVelocity(), myPntB.getVelocity());
               myP.mulTranspose (y, y);
               y.scale (dFdldot/myLength);
               mydFdxA.add (y);  
            }
         }
         else {
            mydFdxA.setZero();
         }
      }

      /**
       * If this segment has subsegments, return the first subsegment.
       * Otherwise, return <code>null</code>.
       */
      public SubSegment firstSubSegment() {
         return null;
      }

      /**
       * If this segment has subsegments, return the last subsegment.
       * Otherwise, return <code>null</code>.
       */
      public SubSegment lastSubSegment() {
         return null;
      }

      /**
       * Queries whether this segment has subsegments.
       * @return <code>true</code> if this segment has subsegments.
       */
      public boolean hasSubSegments() {
         return false;
      }

      /**
       * Returns the current length of this segment.
       *
       * @return current segment length
       */
      public double getLength() {
         return myLength;
      }

      /**
       * Scan attributes of this segment from a ReaderTokenizer. Used when
       * reading the spring from a file.
       */
      boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
         throws IOException {

         rtok.nextToken();
         if (ScanWriteUtils.scanAttributeName (rtok, "specIdx")) {
            mySpecIdx = rtok.scanInteger();
            return true;
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "flags")) {
            myFlags = rtok.scanInteger();
            return true;
         }
         rtok.pushBack();
         return false;
      }

      /**
       * Writes attributes of this segment to a PrintWriter. Used when writing
       * the spring to a file.
       */
      void writeItems (
         PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {

         pw.println ("specIdx=" + mySpecIdx);
         if (myFlags != 0) {
            pw.println ("flags=0x" + Integer.toHexString(myFlags));
         }
      }

      /**
       * Applies distance scaling to this segment.
       */
      void scaleDistance (double s) {
         // no need to do anything for the basic segment
      }

      /**
       * Transforms the geometry of this segment.
       */
      void transformGeometry (GeometryTransformer gtr) {
         // no need to do anything for the basic segment
      }

      /**
       * Creates a clone of this segment.
       */
      public Segment clone() {
         Segment seg;
         try {
            seg = (Segment)super.clone();
         }
         catch (Exception e) {
            throw new InternalErrorException (
               "cannot clone MultiPointSpring.Segment");
         }
         seg.mydFdxA = new Vector3d(mydFdxA);
         seg.myUvec = new Vector3d(myUvec);
         seg.myP = new Matrix3d(myP);
         return seg;
      }
   }

   /**
    * Contains subsegment information. If a wrappable segment is in contact
    * with one or more wrappables, it is divided into subsegments, which
    * connect the A/B points of different wrappables to either each other, or
    * to the segment end points.
    */   
   public class SubSegment extends Segment {

      // The end points A and B of a subsegment are either points of this
      // MultiPointSpring, or the A/B points of a wrappable. In the latter
      // case, an attachment is assigned to transmit the sub-segment forces to
      // the wrappable.
      int myWrappableIdxB; // index of possible wrappable associated with B   
      int myWrappableIdxA; // index of possible wrappable associated with A
      PointAttachment myAttachmentB; // possible attachment for B
      PointAttachment myAttachmentA; // possible attachment for A
      // The JB and JA matrices project wrappable velocities onto the
      // direction of the contact normal for points B and A
      // parallel to the contact normal      
      Matrix3x6 myJB; // project wrappable velocity to contact normal for B
      Matrix3x6 myJA; // project wrappable velocity to contact normal for A
      Vector3d myNrmB; // normal at point B, if in contact with wrappble
      Vector3d myNrmA; // normal at point A, if in contact with wrappble


      // index of first contacting knot on next wrappable A, or numKnots if the
      // subsegment ends at the WrapSegment's final point
      int myKa;
      // index of first contacting knot on previous wrappable B, or -1 if the
      // subsegment starts at the WrapSegment's initial point
      int myKb; 

      // Link to the next subsegment
      SubSegment myNext;

      SubSegment() {
         super();
         myWrappableIdxB = -1;
         myWrappableIdxA = -1;
      }

      Matrix3x6 getJB() {
         return myWrappableIdxB != -1 ? myJB : null;
      }

      Matrix3x6 getJA() {
         return myWrappableIdxA != -1 ? myJA : null;
      }
      
      Vector3d getNrmlB() {
         return myWrappableIdxB != -1 ? myNrmB : null;
      }

      Vector3d getNrmlA() {
         return myWrappableIdxA != -1 ? myNrmA : null;
      }

      /**
       * Returns the next subsegment, or null if there is none.
       */
      public SubSegment getNext() {
         return myNext;
      }

      /**
       * Applies the tension F in this subsegment to the points or
       * wrappables associated with its end-points.
       */
      void applyForce (double F) {
         if (myAttachmentB != null) {
            myPntB.zeroForces();
         }
         if (myAttachmentA != null) {
            myPntA.zeroForces();
         }
         super.applyForce (F);
         if (myAttachmentB != null) {
            myAttachmentB.applyForces();
         }
         if (myAttachmentA != null) {
            myAttachmentA.applyForces();
         }
      }

      /**
       * Computes the derivative of the length of this subsegment.
       * Assumes that uvec is up to date.
       */
      double getLengthDot () {
         if (myAttachmentB != null) {
            myAttachmentB.updateVelStates();
         }
         if (myAttachmentA != null) {
            myAttachmentA.updateVelStates();
         }
         Vector3d velA = myPntA.getVelocity();
         Vector3d velB = myPntB.getVelocity();
         double dvx = velA.x-velB.x;
         double dvy = velA.y-velB.y;
         double dvz = velA.z-velB.z;
         return myUvec.x*dvx + myUvec.y*dvy + myUvec.z*dvz;
      } 

      /**
       * Clones this subsegment.
       */
      public SubSegment clone() {
         SubSegment seg = (SubSegment)super.clone();
         if (myWrappableIdxB != -1 && myAttachmentB != null) {
            seg.myPntB = new Point (myPntB.getPosition());
            seg.myAttachmentB =
               getWrappable(myWrappableIdxB).createPointAttachment (myPntB);
         }
         if (myWrappableIdxA != -1 && myAttachmentA != null) {
            seg.myPntA = new Point (myPntA.getPosition());
            seg.myAttachmentA =
               getWrappable(myWrappableIdxA).createPointAttachment (myPntA);
         }
         return seg;
      }

      /**
       * Returns the index of the first knot contacting the wrappable that
       * terminates this subsegment, or {@code numk} if the subsegment
       * terminates at the wrappable segment's end point, where {@code numk} is
       * the number of knots in the wrappable segment.
       */
      public int getKa() {
         return myKa;
      }
      
      /**
       * Returns the index of the last knot contacting the wrappable the begins
       * this subsegment, or -1 if the subsegment begins at the wrappable
       * segment's start point.
       */
      public int getKb() {
         return myKb;
      }
      
      /**
       * Compute a J matrix for this subsegment. This projects the velocity
       * of a wrappable onto the normal dirction for either the A or B
       * contact.
       */
      void computeJMatrix (
         Matrix3x6 J, Point pnt, Vector3d nrm, Wrappable wrappable) {
         
//         Matrix3d N = new Matrix3d(); // normal project matrix
//         N.outerProduct (nrm, nrm);
//         J.setSubMatrix00 (N);
//         // r is the displacement from pnt to the wrappable origin. We use this
//         // direction so that the second block of J becomes N [ r ].
//         Vector3d r = new Vector3d(); 
//         r.sub (wrappable.getPose().p, pnt.getPosition());
//         N.crossProduct (N, r);
//         J.setSubMatrix03 (N);
         
         // set first block to identity
         J.m00 = 1;
         J.m11 = 1;
         J.m22 = 1;
         // r is the displacement from pnt to the wrappable origin. We use this
         // direction so that the second block of J becomes [ r ].
         Vector3d r = new Vector3d(); 
         r.sub (wrappable.getPose().p, pnt.getPosition());
         Matrix3d X = new Matrix3d();
         X.setSkewSymmetric (r);
         J.setSubMatrix03 (X);
      }
   }

   private interface DifferentiableFunction1x1 {
         
      public double eval (DoubleHolder dfds, double s);
   }

   /**
    * Implements a wrappable segment. This is done by dividing the segment into
    * a fixed number of "knots", which are attracted to each other and out of
    * the interior of "wrappable" objects using linear elastic forces. Once per
    * time step, these forces are used to iteratively update the knot positions
    * so as to "shrink wrap" the segment around whatever wrappables it is
    * associated with. The physics used to do this is first-order, and
    * independent of the second order physics of the overall simulation.
    */
   public class WrapSegment extends Segment implements HasNumericState {
      int myNumKnots;                       // number of knot points
      WrapKnot[] myKnots;                   // list of knot points
      int debugLevel = myDebugLevel;

      // previous values of myPntA and myPntB so we can adjust knots for
      // displacments in updateContactingKnotPositions()
      Point3d myLastPntA = new Point3d();      
      Point3d myLastPntB = new Point3d();

      ArrayList<float[]> myRenderABPoints;// rendering positions for A/B points

      // if this segment is in contact with one or more wrappables,
      // it keeps list of subsegments describing the free line segments
      // between wrappables.
      SubSegment mySubSegHead; // first subsegment
      SubSegment mySubSegTail; // last subsegment

      VectorNd myDvec;

      /**
       * Base class for a function used to perform line searches when solving
       * for wrapping force equilibrium. The line is the line of
       * knot positions {@code x(s)} formed by
       * <pre>
       * x(s) = pvec + s*dvec
       * </pre>
       * where {@code pvec} is the current knot positions, {@code s} is a
       * scalar, and {@code dvec} is a vector of knot displacements.
       *
       * <p>Each subclass LineSearchFunc implements an {@code eval()} method
       * which evaluates a particular scalar function and its derivative for
       * different values of {@code s}.
       */
      private abstract class LineSearchFunc implements DifferentiableFunction1x1 {

         VectorNd myPvec;
         VectorNd myDvec;
         
         LineSearchFunc (VectorNd pvec, VectorNd dvec) {
            myPvec = pvec;
            myDvec = dvec;
         }

         protected void update (double s) {
            advanceKnotPositions (myPvec, s, myDvec);
            updateContacts (/*getNormalDeriv=*/false);
            updateForces();
         }

         /**
          * Evaluates a scalar function at a given location {@code s} along the
          * line. This is done by adding {@code s * dvec} to the current knot
          * postions, updating contact and force information, and then
          * computing and returning the function value. If {@code dfds} is
          * non-{@code null}, the method also computes the function derivative
          * (with respect to {@code s}) and returns it in {@code dfds}.
          */
         @Override
         public abstract double eval (DoubleHolder dfds, double s);
      }

      /***
       * Line search function that evaluates the wrapping energy (and
       * optionally, its derivative) for different knot displacements {@code s
       * * dvec}. The wrapping energy is formed from the wrap and contact
       * forces acting on all the knots.
       */
      private class EnergyFunc extends LineSearchFunc {

         EnergyFunc (VectorNd pvec, VectorNd dvec) {
            super (pvec, dvec);
         }

         @Override
         public double eval (DoubleHolder dfds, double s) {
            update (s);
            if (dfds != null) {
               dfds.value = -forceDotDisp(myDvec);
            }
            return computeEnergy();
         }
      }
      
      /***
       * Line search function that evaluates the "force squared sum" (and
       * optionally, its derivative) for different knot displacements {@code s
       * * dvec}. The "force squared sum" is
       * <pre>
       * sum_k f_k^T f_k
       * </pre>
       * where {@code f_k} is the combined wrap and contact force acting on
       * knot {@code k}.
       */
      private class ForceSqrFunc extends LineSearchFunc {

         ForceSqrFunc (VectorNd pvec, VectorNd dvec) {
            super (pvec, dvec);
         }

         @Override
         public double eval (DoubleHolder dfds, double s) {
            update (s);
            if (dfds != null) {
               dfds.value = computeForceSqrDeriv(myDvec);
            }
            return computeForceSqr();
         }
      }
     
      /** 
       * Constructs an empty wrap segment; used mainly when the segment
       * information is to be read from a file.
       */
      WrapSegment () {
         this (0, null);
      }

      /**
       * Constructs a new WrapSegment with a specified number of knots
       * and an (optional) set of initial points given by {@code initialPnts}.
       * See {@link #setSegmentWrappable(int,Point3d[])} for a description of 
       * how the initializing points are used.
       */
      protected WrapSegment (int numk, Point3d[] initialPnts) {
         super();
         setNumKnots (numk);
         myRenderABPoints = null; // will be created in prerender()
      }

      private void setNumKnots (int numk) {
         myNumKnots = numk;
         myKnots = new WrapKnot[numk];
         for (int i=0; i<numk; i++) {
            myKnots[i] = new WrapKnot();
         }
      }
                                   

      /**
       * Returns the index of knot in this wrappable segment, or -1 if
       * the knot is not present.
       */
      int indexOf (WrapKnot knot) {
         for (int k=0; k<myNumKnots; k++) {
            if (myKnots[k] == knot) {
               return k;
            }
         }
         return -1;
      }

      /**
       * Clear all subsegments in this wrappable segment.
       */
      void clearSubSegs () {
         mySubSegHead = null;
         mySubSegTail = null;
      }

      /**
       * Return the A/B in this wrappable segment indexed by {@code idx},
       * (which should be less than the number of A/B points returned
       * by {@link #numABPoints}.
       */
      public Point getABPoint (int idx) {
         SubSegment sg = firstSubSegment();
         int i = 0;
         if (sg != null) {
            while (sg!=null) {
               if (sg.myAttachmentB != null) {
                  if (i++ == idx) {
                     return sg.myPntB;
                  }
               }
               if (sg.myAttachmentA != null) {
                  if (i++ == idx) {
                     return sg.myPntA;
                  }
               }
               sg = sg.myNext;
            }
         }
         return null;
      }

      /**
       * Return the number of A/B points currently active in this 
       * wrappable segment.
       */
      public int numABPoints () {
         int num = 0;
         SubSegment sg = firstSubSegment();
         if (sg != null) {
            while (sg!=null) {
               if (sg.myAttachmentB != null) {
                  num++;
               }
               if (sg.myAttachmentA != null) {
                  num++;
               }
               sg = sg.myNext;
            }
         }
         return num;
      }

      /**
       * Remove seg and all SubSegments following it.
       */
      private void removeSubSegs (SubSegment seg) {
         SubSegment sg = mySubSegHead;
         if (sg == seg) {
            clearSubSegs();
         }
         else {
            while (sg != null) {
               if (sg.myNext == seg) {
                  sg.myNext = null;
                  mySubSegTail = sg;
                  break;
               }
               sg = sg.myNext;
            }
         }
      }

      /**
       * Append subsegment seg to the list of SubSegments
       */
      private void addSubSeg (SubSegment seg) {
         if (mySubSegHead == null) {
            mySubSegHead = seg;
         }
         else {
            mySubSegTail.myNext = seg;         
         }
         mySubSegTail = seg;
         seg.myNext = null;
      }

      /**
       * Initialize the knots in the strand so that they are distributed evenly
       * along the piecewise-linear path specified by the start and end points
       * and any initializing points that may have been specified.
       */      
      public void initializeStrand (Point3d[] initialPnts) {
         
         // create the piece-wise path
         ArrayList<Point3d> pnts = new ArrayList<Point3d>();
         pnts.add (myPntB.getPosition());
         if (initialPnts != null) {
            for (int i=0; i<initialPnts.length; i++) {
               pnts.add (initialPnts[i]);
            }
         }
         pnts.add (myPntA.getPosition());
         initializeStrand (pnts);
      }

      /**
       * Initialize the knots in the strand so that they are distributed evenly
       * along a specified list of points. This list is assumed to begin and
       * end with the strand's start point (myPntB) and end point (myPntA).
       */      
      public void initializeStrand (ArrayList<Point3d> pnts) {
         // compute the length of this path, saving the inter-point distances
         // in 'lens'.
         double length = 0;
         double[] lens = new double[pnts.size()-1];
         for (int i=0; i<pnts.size()-1; i++) {
            lens[i] = pnts.get(i+1).distance (pnts.get(i));
            length += lens[i];
         }
         if (myNumKnots <= 0) {
            // compute the knots from the knot density
            setNumKnots((int)Math.max (1, myWrapKnotDensity*length));            
         }
         
         // distribute knots along the path according to distance
         double seglen;  // length of each segment
         double dist0;   // path distance at the beginning of each segment
         int pidx;       // index of last point on each segment
         pidx = 1;
         dist0 = 0;
         seglen = lens[pidx-1];
         for (int k=0; k<myNumKnots; k++) {
            // interpolate knot position along current segment
            double dist = (k+1)*length/(myNumKnots+1);
            double s = (dist-dist0)/seglen;
            while (s > 1 && pidx < pnts.size()-1) {
               pidx++;
               dist0 += seglen;
               seglen = lens[pidx-1];
               s = (dist-dist0)/seglen;
            }
            if (s > 1) {               
               s = 1; // paranoid
            }
            myKnots[k].myPos.combine (
                1-s, pnts.get(pidx-1), s, pnts.get(pidx));
         }
         myLength = computeLength();
      }

      /**
       * Returns the index of the nearest knot to a given point. An index value
       * of {@code -1} indicates that the nearest point is the segment's start
       * point ({@code myPntB}), while a value of {@code numKnots()} indicates
       * that the nearest point is the segment's end point ({@code myPntA}).
       */
      int findNearestKnot (Point3d p) {
         int neark = -1;
         double nearDist = myPntB.distance(p);
         for (int k=0; k<myNumKnots; k++) {
            double d = myKnots[k].myPos.distance(p);
            if (d < nearDist) {
               nearDist = d;
               neark = k;
            }
         }
         double d = myPntA.distance(p);
         if (d < nearDist) {
            neark = myNumKnots;
         }
         return neark;
      }

      /**
       * Returns the nearest point on the wrap strand to a given point.
       */
      Point3d nearestPoint (Point3d p) {
         // This method works by checking the distance between p and each of
         // the inter-knot segments.
         double nearDist = Double.POSITIVE_INFINITY;
         Point3d nearPnt = new Point3d();
         Vector3d r = new Vector3d(); // displacement from knot segment start to p
         Vector3d u = new Vector3d(); // direction along the knot segment
         Point3d qs = new Point3d();  // computed nearest point on knot segment

         Point3d q0, q1; // first and second points on each knot segment
         q0 = myKnots[0].myPos;
         for (int i=1; i<myNumKnots; i++) {
            q1 = myKnots[i].myPos;
            r.sub (p, q0);
            u.sub (q1, q0);
            u.normalize(); // ok if u == 0 since then s will be 0
            Point3d qd;    // nearest point on this segment
            double s = r.dot(u);
            if (s <= 0) {
               qd = q0;
            }
            else if (s < 1) {
               qs.scaledAdd (s, u, q0);
               qd = qs;
            }
            else {
               qd = q1;
            }
            double d = qd.distance (p);
            if (d < nearDist) {
               nearDist = d;
               nearPnt.set (qd);
            }
            q0 = q1;
         }
         return nearPnt;
      }

      /***
       * Adjust the knot positions between k0 and k1, exclusive, by adding
       * displacements interpolated between disp0 and disp1, corresponding to
       * knots k0 and k1.
       */
      private void adjustKnotPositions (
         int k0, Vector3d disp0, int k1, Vector3d disp1) {

         Vector3d disp = new Vector3d();
         double numk = k1-k0;
         for (int k=k0+1; k<k1; k++) {
            double lam = (k-k0)/numk;
            disp.combine (1-lam, disp0, lam, disp1);
            myKnots[k].myPos.add (disp);
         }
      }

      /***
       * Adjust a set of knot displacements between k0 and k1, exclusive, by
       * adding displacements interpolated between disp0 and disp1,
       * corresponding to knots k0 and k1.
       */
      private void adjustKnotDisplacements (
         VectorNd dvec, int k0, Vector3d disp0, int k1, Vector3d disp1) {

         double[] dbuf = dvec.getBuffer();
         Vector3d disp = new Vector3d();
         double numk = k1-k0;
         for (int k=k0+1; k<k1; k++) {
            double lam = (k-k0)/numk;
            disp.combine (1-lam, disp0, lam, disp1);
            dbuf[3*k  ] += disp.x;
            dbuf[3*k+1] += disp.y;
            dbuf[3*k+2] += disp.z;
         }
      }

      /**
       * Adjust the knot positions to account for the motions of the
       * segment end-points, and any wrappables the segment is in
       * contact with. This will ensure that moving a wrappable does
       * not break contacts, and the next wrap path update will start
       * from an equivalent (though shifted) contact configuration.
       */
      private void updateContactingKnotPositions() {
         // keep track of the indices and displacements of knots that bracket
         // free sections, so displacements can be interpolated over free
         // sections.
         int k0 = -1;
         Vector3d disp0 = new Vector3d();
         disp0.sub (myPntA.getPosition(), myLastPntA);
         Vector3d disp1 = new Vector3d();
         // XXX do we need to interpolate displacements in end point positions
         // as well?
         boolean contacting = false;
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            Wrappable wrappable = knot.getWrappable();
            if (wrappable != null) {
               // knot is in contact with wrappable.
               // transform knot position to accommodate rigid body
               // displacement. Compute displacement in disp1.
               disp1.set (knot.myPos);
               knot.myPos.transform (wrappable.getPose(), knot.myLocPos);
               disp1.sub (knot.myPos, disp1);
               if (!contacting) {
                  adjustKnotPositions (k0, disp0, k, disp1);
                  contacting = true;
               }
            }
            else {
               if (contacting) {
                  disp0.set (disp1);
                  k0 = k-1;
                  contacting = false;
               }
            }
         }
         if (!contacting && k0 != -1) {
            disp1.sub (myPntB.getPosition(), myLastPntB);
            adjustKnotPositions (k0, disp0, myNumKnots, disp1);
         }
      }
      
      /**
       * For knots that are in contact, save their positions with respect
       * to the *local* coordinates of the wrappable they are in contact with.
       * This will allow their positions to be shifted later in
       * updateContactingKnotPositions() to accommodate subsequent motion
       * of the wrappable.
       */
      private void saveContactingKnotPositions() {
         myLastPntA.set (myPntA.getPosition());
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            Wrappable wrappable = knot.getWrappable();
            if (wrappable != null) {
               knot.myLocPos.inverseTransform (wrappable.getPose(), knot.myPos);
            }
         }
         myLastPntB.set (myPntB.getPosition());
      }
      
      /**
       * For each knot, saves the contact information {@code myDist} and {@code
       * WrappableIdx} in the fields {@code myPrevDist} and {@code
       * myPrevWrappableIdx}.
       */
      private void saveKnotContactData() {
         for (WrapKnot knot : myKnots) {
            knot.myPrevDist = knot.myDist;
            knot.myPrevWrappableIdx = knot.myWrappableIdx;
         }
      }

      /**
       * Checks each knot in this segment to see if it is intersecting any
       * wrappables, and if so, computes the contact normal and distance. If a
       * knot intersects multiple wrappables, then the one with the deepest
       * penetration is used. If {@code getNormalDeriv} is {@code true}, then
       * for each contacting knot, the contact normal derivative is also
       * computed and stored in the knot's {@code myDnrm} field. {@code
       * getNormalDeriv} should be set {@code true} if the wrapping stiffness
       * matrix will need to be computed later.
       */
      private boolean updateContacts (boolean getNormalDeriv) {
         boolean changed = false;
         Vector3d nrml = new Vector3d();
         Matrix3d dnrm = getNormalDeriv ? new Matrix3d() : null;
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            Wrappable lastWrappable = knot.getWrappable();
            knot.setWrappableIdx (-1);
            knot.myDist = Wrappable.OUTSIDE;
            for (int i=0; i<myWrappables.size(); i++) {
               WrappableSpec wspec = myWrappables.get(i);
               if (wspec.myPntIdx0 == -1 ||
                   (wspec.myPntIdx0 <= mySpecIdx &&
                    mySpecIdx < wspec.myPntIdx1)) {
                  double d = wspec.myWrappable.penetrationDistance (
                     nrml, dnrm, knot.myPos);
                  if (d < knot.myDist) {
                     knot.myDist = d;
                     if (d < 0) {
                        knot.setWrappableIdx (i);
                        knot.myNrml.set (nrml);
                        if (dnrm != null) {
                           knot.myDnrm.set (dnrm);
                        }
                     }
                  }
               }
            }
            if (knot.getWrappable() != lastWrappable) {
               changed = true;
            }
         }
         myUpdateContactsCnt++;
         return changed;
      }

      /**
       * Updates the forces on each knot point. These forces are the sum of the
       * tension between adjacent knots, plus the repulsion forces pushing
       * knots out of any wrappable which they are penetrating. 
       * 
       * <p>This method does <i>not</i> invoke contact detection. Instead,
       * it is assumed that contact detection has already been called, and
       * the penetration distances and contact normals for each contacting
       * knot are already known.
       */
      private void updateForces() {

         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            if (knot.myDist < 0) {
               knot.myForce.scale (
                  -knot.myDist*myContactStiffness, knot.myNrml);
            }
            else {
               knot.myForce.setZero();
            }
         }
         Vector3d dprev = new Vector3d();
         Vector3d dnext = new Vector3d();
         dprev.sub (myKnots[0].myPos, myPntB.getPosition());
         for (int k=0; k<myNumKnots; k++) {
            if (k<myNumKnots-1) {
               dnext.sub (myKnots[k+1].myPos, myKnots[k].myPos);
            }
            else {
               dnext.sub (myPntA.getPosition(), myKnots[k].myPos);
            }
            WrapKnot knot = myKnots[k];
            knot.myForce.scaledAdd (-myWrapStiffness, dprev);
            knot.myForce.scaledAdd ( myWrapStiffness, dnext);
            dprev.set (dnext);
         }
      }        

      /**
       * Returns the force tolerance for this segment that corresponds to the
       * current convergence tolerance. See {@link #setConvergenceTol} for
       * details.
       */
      public double getForceTol () {
         return myWrapStiffness*myLength*myConvTol*Math.sqrt(1.0/numKnots());
      }

      /**
       * Computes the elastic energy for the strand, given by the tension
       * between adjacent knots, plus the repulsion forces pushing knots out of
       * any wrappable which they are penetrating.
       */
      public double computeEnergy() {

         double E = 0;
         Vector3d dnext = new Vector3d();
         dnext.sub (myKnots[0].myPos, myPntB.getPosition());
         E += 0.5*myWrapStiffness*dnext.normSquared();
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            if (k<myNumKnots-1) {
               dnext.sub (myKnots[k+1].myPos, knot.myPos);
            }
            else {
               dnext.sub (myPntA.getPosition(), knot.myPos);
            }
            E += 0.5*myWrapStiffness*dnext.normSquared();
            if (knot.myDist < 0) {
               E += 0.5*myContactStiffness*knot.myDist*knot.myDist;
            }
         }
         return E;
      }

      /**
       * Updates the stiffness matrix terms associated with each knot point.
       * These give the force derivatives with respect to changes in knot
       * position. The stiffness matrix structure is block-tridiagonal, where
       * the diagonal blocks account for self-motion and changes wrappable
       * repulsion forces, while the off-diagonal blocks account for the
       * coupling between adjacent blocks.
       */
      protected void updateStiffness () {
         double stiffness = myWrapStiffness;
         double cstiffness = myContactStiffness;
         double cd = myContactDamping/(myNumKnots*myNumKnots);
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            double s = 2*stiffness; // + d
            knot.myBmat.setDiagonal (s, s, s);            
            if (knot.myDist < 0) {
               knot.myBmat.addScaledOuterProduct (
                  cd+cstiffness, knot.myNrml, knot.myNrml);
               knot.myBmat.scaledAdd (
                  knot.myDist*cstiffness, knot.myDnrm);
            }
         }
      }

      /**
       * Modify a vector of knot displacements {@code dvec} by adding {@code
       * lam * force} to it, where {@code lam} is a scalar and {@code force} is
       * the vector of wrapping forces on all the knots.
       *
       * <p>The purpose of this is to modify the displacement vector from that
       * produced by a Newton's method approach, to one which combines Newton's
       * method with gradient descent (since forces represent the gradient) and
       * is hence potentially more stable.
       */
      private void modifyDvec (double lam, VectorNd dvec) {
         double[] dbuf = dvec.getBuffer();
         for (int k=0; k<myNumKnots; k++) {         
            WrapKnot knot = myKnots[k];
            dbuf[k*3  ] += lam*knot.myForce.x;
            dbuf[k*3+1] += lam*knot.myForce.y;
            dbuf[k*3+2] += lam*knot.myForce.z;
         }        
      }

      /**
       * Sets the knot positions {@code pos} to
       * <pre>
       *   pos = pvec + s * dvec
       * </pre>
       * where {@code pvec} are the previous knot positions, {@code
       * s} is a scale factor, and {@code dvec} is a vector of knot
       * displacements.
       */
      private void advanceKnotPositions (VectorNd pvec, double s, VectorNd dvec) {
         double[] dbuf = dvec.getBuffer();
         double[] pbuf = pvec.getBuffer();
         for (int k=0; k<myNumKnots; k++) {         
            WrapKnot knot = myKnots[k];
            int idx = k*3;
            knot.myPos.x = s*dbuf[idx] + pbuf[idx];
            idx++;
            knot.myPos.y = s*dbuf[idx] + pbuf[idx];
            idx++;
            knot.myPos.z = s*dbuf[idx] + pbuf[idx];
         }        
         myLength = computeLength();
      }

      /**
       * Uses numerical differentiation to estimate the stiffness
       * matrix for all the knots. Used for debugging.
       */
      protected Matrix3d[] computeNumericStiffness() {

         Matrix3d[] S = new Matrix3d[myNumKnots];
        
         Matrix3d Kbase = new Matrix3d();
         Kbase.setIdentity();
         Kbase.scale (-2*myWrapStiffness);

         Point3d q = new Point3d();
         Vector3d f = new Vector3d();
         Vector3d f0 = new Vector3d();
         Vector3d nrm = new Vector3d();

         double h = 1e-8;

         for (int k=0; k<myNumKnots; k++) {
            Wrappable wrappable = myKnots[k].getWrappable();
            Point3d q0 = myKnots[k].myPos;
            Matrix3d Kknot = new Matrix3d();
            
            boolean invalid = false;

            if (wrappable != null) {
               double d = wrappable.penetrationDistance (nrm, null, q0);
               f0.scale (-d*myContactStiffness, nrm);
               for (int i=0; i<3; i++) {
                  q.set (q0);
                  q.set (i, q.get(i)+h);
                  d = wrappable.penetrationDistance (nrm, null, q);
                  if (d > 0) {
                     invalid = true;
                     d = 0;
                  }
                  f.scale (-d*myContactStiffness, nrm);
                  f.sub (f0);
                  f.scale (1/h);
                  Kknot.setColumn (i, f);
               }
            }
            if (!invalid) {
               Kknot.add (Kbase);
               S[k] = Kknot;
            }
         }
         return S;
      }

      /**
       * Sets MT to M + d I, where I is the 3x3 identity matrix.
       */
      private void addToDiagonal (Matrix3d MR, Matrix3d M, double d) {
         MR.m00 = M.m00 + d;
         MR.m01 = M.m01;
         MR.m02 = M.m02;
         
         MR.m10 = M.m10;
         MR.m11 = M.m11 + d;
         MR.m12 = M.m12;
         
         MR.m20 = M.m20;
         MR.m21 = M.m21;
         MR.m22 = M.m22 + d;
      }
      
      //
      /**
       * Computes a knot displacement vector {@code dvec} by solving
       * <pre>
       * K x = f
       * </pre>
       * where {@code K} is the block tridiagonal stiffness matrix, {@code x}
       * is the unknown later stored into {@code dvec}, and {@code f} is the
       * vector of knot wrapping forces.
       */
      double solveForKnotDisplacements (VectorNd dvec) {
         // The block tridiagonal system can be written as
         //
         // [ B_0 C_0                 0    ] [   x_0   ]   [   f_0   ]
         // [                              ] [         ]   [         ]
         // [ A_1 B_1 C_1                  ] [   x_1   ]   [   f_1   ]
         // [                              ] [         ]   [         ]
         // [     A_2 B_2   ...            ] [   x_2   ] = [   f_2   ]
         // [                              ] [         ]   [         ]
         // [         ...   ...     C_{n-2}] [         ]   [         ]
         // [                              ] [         ]   [         ]
         // [  0           A_{n-1}  B_{n-1}] [ x_{n-1} ]   [ f_{n-1} ]
         //
         // and we can the block tridiagonal matrix algorithm:
         //
         //         { inv(B_i) C_i                                  i = 0
         //  C'_i = {
         //         { inv(B_i - A_i*C'_{i-1}) C_i                   i = 1, 2, ...
         //
         //         { inv(B_i) f_i                                  i = 0
         //  d_i =  {
         //         { inv(B_i - A_i*C'_{i-1}) (f_i - A_i d_{i-1})   i > 0
         //
         //  x_n  = d_n
         //
         //  x_i  = d_i - C'_i x_{i+1}    i = n-2, ... 0
         //
         // We also exploit the fact that all C_i and A_i matrices are given by
         //
         // C_i = A_i = -wrapStiffness I,
         //
         // where I is the 3x3 identity matrix, so their multiplications can be
         // done by simple scaling.
         //
         // For each knot, B_i, inv(B_i), C'_i, f_i are stored in the myBmat,
         // myBinv, myCinv, and myForce, while d_i and x_i are stored in the
         // myDvec field.

         double c = -myWrapStiffness;
         double d = getWrapDamping()/(myNumKnots*myNumKnots);        
         WrapKnot knot = myKnots[0];
         addToDiagonal (knot.myBinv, knot.myBmat, d);
         knot.myBinv.invert ();
         knot.myCinv.scale (c, knot.myBinv);
         for (int i=1; i<myNumKnots; i++) {
            knot = myKnots[i];
            Matrix3d Binv = knot.myBinv;
            addToDiagonal (Binv, knot.myBmat, d);
            Binv.scaledAdd (-c, myKnots[i-1].myCinv);
            Binv.invert();
            if (i<myNumKnots-1) {
               knot.myCinv.scale (c, Binv);
            }
         }
         Vector3d vec = new Vector3d();
         Vector3d tmp = new Vector3d();
         knot = myKnots[0];
         knot.myBinv.mul (knot.myDvec, knot.myForce);
         for (int i=1; i<myNumKnots; i++) {
            knot = myKnots[i];
            vec.set (knot.myForce);
            vec.scaledAdd (-c, myKnots[i-1].myDvec);
            knot.myBinv.mul (knot.myDvec, vec);
         }

         int k = myNumKnots-1;
         vec.set (myKnots[k].myDvec);
         double maxd = 0; //maximum displacement
         while (--k >= 0) {
            knot = myKnots[k];
            knot.myCinv.mul (tmp, vec);
            vec.sub (knot.myDvec, tmp);
            knot.myDvec.set (vec);
            double m = vec.infinityNorm();
            if (m > maxd) {
               maxd = m;
            }
         }
         double s = 1.0;
         if (maxd > getMaxWrapDisplacement()) {
            // scale solution to maximum displacement
            s = getMaxWrapDisplacement()/maxd;
            for (int i=0; i<myNumKnots; i++) {
               myKnots[i].myDvec.scale (s);
            }
         }
         double[] dbuf = dvec.getBuffer();
         for (int i=0; i<myNumKnots; i++) {
            Vector3d dv = myKnots[i].myDvec;
            dbuf[i*3  ] = dv.x;
            dbuf[i*3+1] = dv.y;
            dbuf[i*3+2] = dv.z;
         }
         if (s < 1.0) {
            if (myDebugLevel > 1) {
               System.out.println ("  clipped " + s);
            }
         }
         return s;
      }
      
      /**
       * Computes the norm of the composite wrapping force vector for all knots
       */
      double forceNorm() {
         double sumSqr = 0;
         for (int i=0; i<myNumKnots; i++) {
            sumSqr += myKnots[i].myForce.normSquared();
         }
         return Math.sqrt(sumSqr);
      }

      /**
       * Finds the maximum wrapping force over all the knots
       */
      double maxForce() {
         double maxSqr = 0;
         for (int i=0; i<myNumKnots; i++) {
            double sqr = myKnots[i].myForce.normSquared();
            if (sqr > maxSqr) {
               maxSqr = sqr;
            }
         }
         return Math.sqrt(maxSqr);
      }

      /**
       * Computes the dot product of the wrapping forces with a set
       * of knot displacements {@code dvec}. 
       */
      double forceDotDisp(VectorNd dvec) {
         double[] dbuf = dvec.getBuffer();
         double dot = 0;
         for (int i=0; i<myNumKnots; i++) {
            Vector3d f = myKnots[i].myForce;
            dot += (f.x*dbuf[3*i] + f.y*dbuf[3*i+1] + f.z*dbuf[3*i+2]);
         }
         return dot;
      }

      /**
       * Computes the sum of the squared magnitude of the wrapping
       * forces on all the knots.
       */
      double computeForceSqr() {
         double fsqr = 0;
         for (int i=0; i<myNumKnots; i++) {
            WrapKnot knot = myKnots[i];
            fsqr += knot.myForce.normSquared();
         }
         return fsqr;
      }

      /**
       * Computes the derivative of the sum of the squared magnitude of the 
       * wrapping forces on all the knots, with respect to a line parameter
       * {@code s} which controls the knot positions {@code pos} according to
       * <pre>
       * pos = prevPos + s * dvec
       * </pre>
       */
      double computeForceSqrDeriv(VectorNd dvec) {
         Vector3d tmp = new Vector3d();
         Vector3d dv = new Vector3d();
         double c = -myWrapStiffness;
         double deriv = 0;
         for (int i=0; i<myNumKnots; i++) {
            dv.set (dvec, i*3);
            myKnots[i].myBmat.mul (tmp, dv);
            if (i > 0) {
               dv.set (dvec, (i-1)*3);
               tmp.scaledAdd (c, dv);
            }
            if (i < myNumKnots-1) {
               dv.set (dvec, (i+1)*3);
               tmp.scaledAdd (c, dv);
            }
            deriv += myKnots[i].myForce.dot(tmp);
         }        
         return -2*deriv;
      }
      
      /**
       * Computes the length of this wrappable segment by summing all
       * the distances between knots, including the first and last 
       * segment points.
       */
      double computeLength() {
         double len = 0;
         Point3d pos0 = myPntB.getPosition();
         for (int k=0; k<myNumKnots; k++) {
            Point3d pos1 = myKnots[k].myPos;
            len += pos1.distance(pos0);
            pos0 = pos1;
         }
         len += myPntA.getPosition().distance(pos0);
         return len;
      }

      protected int totalIterations;

      /**
       * Checks the value of the stiffness matrix against a numerically
       * computed value. Used for testing and debugging only.
       */
      void checkStiffness() {
         updateContacts(/*getNormalDeriv=*/true);
         updateForces();
         updateStiffness();
         
         Matrix3d[] Kcheck = computeNumericStiffness();
         Matrix3d Kerr = new Matrix3d();

         double maxErr = 0;
         int kmax = -1;

         for (int k=0; k<myNumKnots; k++) {
            // add because Bmat is the negative of the stiffness
            if (Kcheck[k] != null) {
               Kerr.add (Kcheck[k], myKnots[k].myBmat);
               double err = Kerr.frobeniusNorm()/Kcheck[k].frobeniusNorm();
               if (err > maxErr) {
                  maxErr = err;
                  kmax = k;
               }
            }
         }
         if (maxErr > 1e-6) {
            System.out.println ("err=" + maxErr + ", knot=" + kmax);
            Matrix3d Kknot = new Matrix3d();
            Kknot.negate (myKnots[kmax].myBmat);
            System.out.println ("Kknot=\n" + Kknot.toString ("%12.8f"));
            System.out.println ("Kcheck=\n" + Kcheck[kmax].toString ("%12.8f"));
            Kerr.sub (Kcheck[kmax], Kknot);
            System.out.println ("Kerror=\n" + Kerr.toString ("%12.8f"));
         }
      }         

      /**
       * Find the contact groups for all the knots. A contact group is a
       * continguous sequence of knots that are in contact with the same
       * wrappable. Assign each contact group a unique "contact group id"
       * and store this in each knot. Knots which are not in contact will
       * have a contact group id of -1.
       */
      void updateContactGroups() {
         int contactGid = -1;
         WrapKnot prev = null;
         for (WrapKnot knot : myKnots) {
            if (knot.inContact()) {
               if (prev == null ||
                   prev.getWrappable() != knot.getWrappable()) {
                  contactGid++;
               }
               knot.myContactGid = contactGid;
            }
            else {
               knot.myContactGid = -1;
            }
            prev = knot;
         }
      }
      
      /**
       * Called when a wrappable, indexed by {@code widx}, is removed from this
       * spring. Sets the contract group id and wrappable index for any knots
       * that were in contact with the wrappable to be -1.
       */
      void removeWrappableFromContacts (int widx) {
         for (WrapKnot knot : myKnots) {
            if (knot.getWrappableIdx() == widx) {
               knot.myContactGid = -1;
               knot.setWrappableIdx (-1);
            }
         }
      }
      
      private boolean containsIndex (int[] indices, int idx) {
         for (int i=0; i<indices.length; i++) {
            if (indices[i] == idx) {
               return true;
            }
         }
         return false;
      }

      /**
       * Called when one or more wrappables are removed to reindex the
       * associated contact info.
       */
      int[] reindexContactInfo (int[] wrappableReindexMap) {
         int[] savedInfo = new int[numKnots()];
         int k = 0;
         boolean wasReindexed = false;
         for (WrapKnot knot : myKnots) {
            int widx = knot.getWrappableIdx();
            savedInfo[k] = widx;
            if (widx != -1 && wrappableReindexMap[widx] != widx) {
               knot.setWrappableIdx (wrappableReindexMap[widx]);
               wasReindexed = true;
            }
            k++;
         }
         if (wasReindexed) {
            updateContactGroups();
            return savedInfo;
         }
         else {
            return null;
         }
      }
      
      /**
       * Called to restore contact info when wrappables are un-removed.
       */
      void restoreContactInfo (int[] savedInfo) {
         if (savedInfo != null) {
            int k = 0;
            for (WrapKnot knot : myKnots) {
               knot.setWrappableIdx (savedInfo[k++]);
            }
            updateContactGroups();
         }
      }
      
      /**
       * Called when all wrappables are removed from this spring. Sets the 
       * contract group id and wrappable index for all knots to be -1.
       */     
      void removeAllWrappablesFromContacts() {
         for (WrapKnot knot : myKnots) {
            knot.myContactGid = -1;
            knot.setWrappableIdx (-1);
         }        
      }
      
      /**
       * Returns true if this wrappable segment is in contact with one or
       * more wrappables.
       */
      public boolean inContact() {
         for (WrapKnot knot : myKnots) {
            if (knot.inContact()) {
               return true;
            }
         }
         return false;
      }

      /**
       * Returns the number of knots in this wrappable segment which are
       * in contact.
       */
      int numContacts() {
         int numc = 0;
         for (int k=0; k<myNumKnots; k++) {
            if (myKnots[k].inContact()) {
               numc++;
            }
         }
         return numc;
      }

      /**
       * Returns {@code true} if the strand has broken contact with one or more
       * wrappables in one or more places.
       */
      boolean checkForBrokenContact() {

         int contactGid = -1;
         int contactCnt = 0;
         for (int k=0; k<numKnots(); k++) {
            WrapKnot knot = myKnots[k];
            if (knot.myContactGid != -1) {
               // knot was in contact
               if (knot.myContactGid != contactGid) {
                  // starting a new contact group
                  contactGid = knot.myContactGid;
                  contactCnt = 0;
               }
               if (knot.inContact()) {
                  // bump contact count for current group
                  contactCnt++;
               }
            }
            else {
               // knot not in contact
               if (contactGid != -1) {
                  // leaving contact group; return true if there
                  // was no contact
                  if (contactCnt == 0) {
                     return true;
                  }
                  contactGid = -1;
               }
            }
         }
         if (contactGid != -1) {
            // leaving contact group; return true if there was no contact
            return contactCnt == 0;
         }
         else {
            return false;
         }
      }

      /**
       * Contains information about a "pullback" knot
       */
      private class ContactPullback {
         int knotIdx = -1;  // index of the knot
         double scale;      // displacement scale factor required
      }

      /**
       * Updates pullback information to account for a previous contact group
       * that is no longer in contact. The contact group is delimited by the
       * knot indices {@code k} such that {@code start <= k < end}. Every knot
       * in this group is inspected to see if it may try to repenetrate the
       * wrappable when the next set of knot displacements {@code dnew} are
       * applied. When such a knot is found, we estimate the amount that the
       * original knot displacements should be scaled by in order to prevent
       * the knot from leaving contact in the first place. The knot with the
       * *largest* such scale is the "pullback" knot. If the scale for this
       * knot is larger than any previous pullback that was passed in through
       * {@code pullback}, or if there was no previous pullback, then the
       * {@code pullback} is set to this knot.
       */
      void updatePullbackForGroup (
         ContactPullback pullback, VectorNd dnew, int start, int end) {
         if (debugLevel > 1) {
            System.out.println ("  broken contact from "+start+" to "+end);
         }
         int groupKnotIdx = -1;
         Vector3d dv = new Vector3d();
         double groupScale = 0;
         for (int k=start; k<end; k++) {
            WrapKnot knot = myKnots[k];
            dv.set (dnew, 3*k);
            if (dv.dot (knot.myNrml) < 0) {
               // knot is trying to renter object, so find scale factor
               double dist = knot.myDist;
               double prev = knot.myPrevDist; 
               if (dist != Wrappable.OUTSIDE && dist > 0 && prev < 0) {
                  // Note that dist > 0 and prev < 0 since we were contacting
                  // but are not anymore.
                  double scale = 1 - dist/(dist-prev);
                  if (debugLevel > 1) {
                     System.out.println ("  "+k+" scale=" + scale);
                  }
                  if (scale > groupScale) {
                     groupScale = scale;
                     groupKnotIdx = k;
                  }
               }
            }
         }
         if (groupKnotIdx != -1) {
            if (pullback.knotIdx == -1 || groupScale > pullback.scale) {
               pullback.scale = groupScale;
               pullback.knotIdx = groupKnotIdx; 
            }
         }
      }

      /**
       * Examines all the knots in the segment, and compares their previous and
       * current contact states. If any previous contact group (set of
       * consecutive knots in contact) is now completely separated (none of its
       * knots are in contact), then the free knots are examined to see if any
       * of them would try to repenetrate the wrappable when the new knot
       * displacements {@code dnew} are applied. For each of these, we estimate
       * the amount that the original knot displacements would need to be
       * scaled by in order to prevent the contact from breaking. The knot
       * with the maximum such scale factor is the "pullback" knot and is
       * returned in {@code pullback}.
       */
      void computePullback (ContactPullback pullback, VectorNd dnew) {
         
         pullback.scale = 0.0;
         pullback.knotIdx = -1;
         int contactGid = -1;
         int contactGstart = -1;
         int contactCnt = 0;
         for (int k=0; k<numKnots(); k++) {
            WrapKnot knot = myKnots[k];
            if (knot.myContactGid != -1) {
               // knot was in contact
               if (knot.myContactGid != contactGid) {
                  // starting a new contact group
                  contactGstart = k;
                  contactGid = knot.myContactGid;
                  contactCnt = 0;
               }
               if (knot.inContact()) {
                  // bump contact count for current group
                  contactCnt++;
               }
            }
            else {
               // knot not in contact
               if (contactGid != -1) {
                  // leaving contact group; if contact was broken, update
                  // pullback
                  if (contactCnt == 0) {
                     updatePullbackForGroup (
                        pullback, dnew, contactGstart, k);
                  }
                  contactGid = -1;
               }
            }
         }
         if (contactGid != -1) {
            // leaving contact group; if contact was broken, update
            // pullback
            if (contactCnt == 0) {
               updatePullbackForGroup (
                  pullback, dnew, contactGstart, numKnots());
            }
         }
      }

      /**
       * Line search method. Attempts to find a value of s that minimizes a
       * function f(s) over an interval [0, maxs], starting with an initial
       * value of s = s1. The variables f0, f1, df0, and df1 give the function
       * value and its derivative (with respect to s) at s = 0 and s = s1.
       *
       * <p>For more info on lines search, see Nocedal and Wright, "Numerical
       * Optimization".
       */
      private double lineSearch (
         double f0, double df0, double f1, double df1, LineSearchFunc func,
         double s1, double maxs) {

         DoubleHolder deriv = new DoubleHolder();
         double flo = f0;
         double slo = 0;
         double shi = 1;
         
         double f = f1;
         double df = df1;
         double s = s1;

         if (f < flo && df <= 0) {
            slo = s;
            flo = f;
            while (slo < maxs) {
               s += 1.0;
               f = func.eval (deriv, s);
               df = deriv.value;              
               if (f < flo && df <= 0) {
                  slo = s;
               }
               else {
                  break;
               }
            }
            return slo;
         }
         else {
            shi = s;
            int maxIter = 3;
            for (int i=0; i<maxIter; i++) {
               s = (slo+shi)/2;
               f = func.eval (deriv, s);
               df = deriv.value; 
               if (f < flo || df <= 0) {
                  return s;
               }
               else {
                  shi = s;
               }
            }
            return s;
         }
      }

      /**
       * Compute a rescale factor s for a pullback knot. That is a maximal
       * value s such that if the current knot positions are computed from
       * <pre>
       * pvec + s * dvec
       * </pre>
       * instead of 
       * <pre>
       * pvec + dvec
       * </pre>
       * the pullback knot will remain in contact.  The method works by
       * iterating through different values of s, and is likely to find valid s
       * but is not guaranteed to do so. If it does not, the last s that was
       * tried is returned.
       */
      private double computeRescale (
         ContactPullback pullback, VectorNd pvec, VectorNd dvec) {
         
         WrapKnot knot = myKnots[pullback.knotIdx];
         Vector3d pv = new Vector3d();
         Vector3d dv = new Vector3d();
         pv.set (pvec, 3*pullback.knotIdx);
         dv.set (dvec, 3*pullback.knotIdx);
         
         double s = 0.99*pullback.scale;
         Point3d pos = new Point3d();
         Wrappable wrappable = knot.getPrevWrappable(); 
         double dist = knot.myDist;
         double prev = knot.myPrevDist;
         
         int cnt = 0;
         while (dist > 0 && cnt < 10) {
            pos.scaledAdd (s, dv, pv);
            dist = wrappable.penetrationDistance (null, null, pos);
            if (dist > 0) {
               s *= 0.99*(-prev/(dist-prev));
            }
            cnt++;
         }
         return s;                     
      }

      /**
       * Used by adjustDvecForGroup(), which is not currently used.
       */
      boolean projectKnotToSurface (
         Vector3d disp, WrapKnot knot, double pen, Wrappable wrappable) {
         Point3d pos = new Point3d();
         Vector3d nrm = new Vector3d();
         int iter = 1;
         int maxIter = 3;
         nrm.set (knot.myNrml);
         pos.set (knot.myPos);
         double dist = knot.myDist;
         do {
            pos.scaledAdd (-dist-pen, nrm, pos);
            dist = wrappable.penetrationDistance (nrm, null, pos);
            iter++;
         }
         while (dist >= 0 && dist != Wrappable.OUTSIDE && iter <= maxIter);
         disp.sub (pos, knot.myPos);
         if (myDebugLevel > 1) {
            System.out.println ("      dist=" + dist + " pen=" + pen);
         }
         return dist < 0;
      }

      /**
       * Used by adjustDvecForGroup(), which is not currently used.
       */
      void addDisplacement (VectorNd dvec, int idx, Vector3d disp) {
         double[] dbuf = dvec.getBuffer();
         dbuf[idx++] = disp.x;         
         dbuf[idx++] = disp.y;
         dbuf[idx++] = disp.z;
      }

      /**
       * Used by projectionSeparationControl(), which is not currently used.
       */
      int adjustDvecForGroup (
         Vector3d lastDisp, int lastk,
         VectorNd dvec, VectorNd dnew, int start, int end) {

         if (debugLevel > 1) {
            System.out.println ("  broken contact from "+start+" to "+end);
         }
         int kfirst = -1;
         Vector3d dv = new Vector3d();
         Wrappable wrappable = myKnots[start].getPrevWrappable();
         double pen = wrappable.getCharacteristicRadius()*0.01;
         Vector3d dispFirst = new Vector3d();
         int kmin = -1;
         double dmin = -1;
         for (int k=start; k<end; k++) {
            WrapKnot knot = myKnots[k];
            dv.set (dnew, 3*k);
            double retro = dv.dot (knot.myNrml);
            double dist = knot.myDist;
            if (debugLevel > 1) {
               System.out.println (
                  "    "+k+" retro=" + retro + " dist=" + dist + " len=" +
                  knot.myNrml.norm());
            }
            if (dist != Wrappable.OUTSIDE && dist > 0 && retro < 0) {
               // knot is trying to renter object, so project point back
               if (kfirst == -1) {
                  kmin = kfirst = k;
                  dmin = dist;
               }
               else if (dist < dmin) {
                  dmin = dist;
                  kmin = k;
               }
               lastk = k;
            }
         }
         if (kfirst == -1) {
            if (!lastDisp.equals (Vector3d.ZERO)) {
               adjustKnotDisplacements (
                  dvec, lastk, lastDisp, start, Vector3d.ZERO);
            }
            lastDisp.setZero();
            return -1;
         }
         if (debugLevel > 1) {
            System.out.println ("  adjusting from "+kfirst+" to "+lastk);
         }
         if (projectKnotToSurface (dispFirst, myKnots[kmin], pen, wrappable)) {
            addDisplacement (dvec, 3*kmin, dispFirst);
            if (debugLevel > 1) {
               System.out.println ("    * " + dispFirst.toString());
            }
            lastDisp.set (dispFirst);
            adjustKnotDisplacements (
               dvec, lastk, lastDisp, kfirst, dispFirst);
            lastk = kmin;
         }
         return lastk;
      }

      /**
       * Separation control strategy using projection to the contact
       * surface. Not currently used.
       */
      boolean projectionSeparationControl (VectorNd dvec, VectorNd dnew) {

         int lastk = -1; // index of last adjusted k
         int contactGid = -1;
         int contactGstart = -1;
         int contactCnt = 0;
         Vector3d lastDisp = new Vector3d(); // displacement applied to lastk
         for (int k=0; k<numKnots(); k++) {
            WrapKnot knot = myKnots[k];
            if (knot.myContactGid != -1) {
               // knot was in contact
               if (knot.myContactGid != contactGid) {
                  // starting a new contact group
                  contactGstart = k;
                  contactGid = knot.myContactGid;
                  contactCnt = 0;
               }
               if (knot.inContact()) {
                  // bump contact count for current group
                  contactCnt++;
               }
            }
            else {
               // knot not in contact
               if (contactGid != -1) {
                  // leaving contact group; if contact was broken, update
                  // pullback
                  if (contactCnt == 0) {
                     lastk = adjustDvecForGroup (
                        lastDisp, lastk, dvec, dnew, contactGstart, k);
                     if (lastk == -1) {
                        lastk = k-1;
                     }
                  }
                  else {
                     if (!lastDisp.equals (Vector3d.ZERO)) {
                        adjustKnotDisplacements (
                           dvec, lastk, lastDisp, contactGstart, Vector3d.ZERO);
                     }
                     lastDisp.setZero();
                     lastk = k-1;
                  }
                  contactGid = -1;
               }
            }
         }
         if (contactGid != -1) {
            // leaving contact group; if contact was broken, update
            // pullback
            if (contactCnt == 0) {
               lastk = adjustDvecForGroup (
                  lastDisp, lastk, dvec, dnew, contactGstart, numKnots());
            }
            else {
               if (!lastDisp.equals (Vector3d.ZERO)) {
                  adjustKnotDisplacements (
                     dvec, lastk, lastDisp, contactGstart, Vector3d.ZERO);
               }
            }
         }
         else {
            if (!lastDisp.equals (Vector3d.ZERO)) {
               adjustKnotDisplacements (
                  dvec, lastk, lastDisp, numKnots(), Vector3d.ZERO);
            }
         }
         return lastk != -1;
      }

      /**
       * Separation control tries to prevent contact separation
       * instabilty. This can happen during a wrap path solve when the updated
       * knot positions completely separate from one or more wrappable
       * sections, only to repenetrate on the next iteration. The solution is
       * to locate any knots that are likely repenetrate, and then adjust the
       * knot displacements that were used to update the knot positions so that
       * at least a few knots remain in contact.
       *
       * <p>This method takes three arguments: a vector of knot positions
       * {@code pvec} and displacements {@code dvec} such the {@code pvec +
       * dvec} equals the current knot positions, and another vector of
       * displacements {@code dnew} computed for the current position but which
       * have yet to be applied. The latter can be used to try and determine if
       * any knots which have broken contact would subsequently repenetrate. If
       * this method detects a separation instabilty, it will modify {@code
       * dvec} and return {@code true}.
       */
      boolean applySeparationControl (
         VectorNd pvec, VectorNd dvec, VectorNd dnew) {
         return rescaleSeparationControl (pvec, dvec, dnew);
         //return projectionSeparationControl (dvec, dnew);
      }         

      /**
       * Separation control based on rescaling the displacement vector {@code
       * dvec}. See {@link #applySeparationControl} for general details about
       * separation control and a description of the arguuments.
       */
      boolean rescaleSeparationControl (
         VectorNd pvec, VectorNd dvec, VectorNd dnew) {
         ContactPullback pullback = new ContactPullback();
         computePullback (pullback, dnew);
         if (pullback.knotIdx != -1) {
            // adjust the pullback if necessary
            double s = computeRescale (pullback, pvec, dvec);
            dvec.scale (s);         
            if (debugLevel > 1) {
               System.out.println (
                  "  RESCALE " + s + "  knot=" + pullback.knotIdx);
            }
            return true;
         }
         else {
            return false;
         }
      }


      protected void printContactingKnotInfo (VectorNd dvec) {
         System.out.println ("contacting knot displacements and forces");
         Vector3d disp = new Vector3d();
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            if (knot.inContact()) {
               dvec.getSubVector (3*k, disp);
               System.out.println (
                  k + "  disp=" + disp.toString ("%10.6f") + " force=" + 
                  knot.myForce.toString ("%10.7f") + " nrml=" +
                  knot.myNrml.toString ("%10.7f") + " dist=" +
                  knot.myDist + " cell=" + knot.cellInfo());
            }
         }
      }
      
      protected double computeCosine (VectorNd vec0, VectorNd vec1) {
         double[] buf0 = vec0.getBuffer();
         double[] buf1 = vec1.getBuffer();
         double magSqr0 = 0;
         double magSqr1 = 0;
         double sum = 0;
         for (int i=0; i<vec0.size(); i++) {
            double x0 = buf0[i];
            double x1 = buf1[i];
            sum += x0*x1;
            magSqr0 += x0*x0;
            magSqr1 += x1*x1;
         }
         return sum/(Math.sqrt(magSqr0*magSqr1));
      }

      /**
       * Updates the wrap path for this wrappable segment. This is done by
       * iterating until the wrapping and contact forces on the knots are close
       * enough to equilibrium. The number of iterations is limited by {@code
       * maxIter}.
       */
      protected int updateWrapStrand (int maxIter) {
         // Basic approach is a damped Newton's method, modified with step size
         // adjustment to account for premature contact breaking and a line
         // search to minimize energy.

         int icnt = 0;
         boolean converged = false;

         if (myUpdateContactsP) {
            updateContactingKnotPositions();
         }
         // assume contact info has already been updated
         updateForces(); 

         double ftol = getForceTol();
         double lastLength = 0;
         
         // knot positions and displacements, used to update the knot positions
         VectorNd pvec = new VectorNd(3*myNumKnots);
         VectorNd dvec = new VectorNd(3*myNumKnots);
         VectorNd lastDvec = new VectorNd(3*myNumKnots);
         int oscillateCnt = 0;
         boolean contactChanged = false;
         boolean localMinimumFound = false;
         do {
            // save knot positions in pvec
            getKnotPositions (pvec);
            lastLength = getLength();

            // compute knot displacements for a single Newton step.
            updateStiffness();
            updateContactGroups();
            solveForKnotDisplacements(dvec);
            if (icnt > 0 && !localMinimumFound) {
               // if last iteration did not have a contact change, and 
               // the cosine of the last direction with respect to the
               // new one is < -0.99, then bump the oscillation count
               if (computeCosine (dvec, lastDvec) < -0.95) {
                  oscillateCnt++;
                  if (oscillateCnt >= 3) {
                     localMinimumFound = true;
                     totalStuck++;
                     //break;
                  }
               }
               else {
                  oscillateCnt = 0;
               }
            }
            lastDvec.set (dvec);
            myDvec = new VectorNd(dvec);

            // updated knot positions will now be given by
            // 
            // pos = pvec + s*dvec
            //
            // the rest of this iteration will deterine s, and if necessary
            // adjust dvec, to ensure good converge

            // make sure that dvec is pointing at least a little into the
            // direction of the gradient (i.e., force). If it isn't, adjust it
            // so that it is.  In other words, make sure dvec is pointing in a
            // descent direction. See Nocedal and Wright, "Numerical
            // Optimization".
            double r0 = forceDotDisp(dvec);
            double denom = forceNorm()*dvec.norm();
            double cos = r0/denom;
            double mincos = 0.01;
            if (cos < mincos) {
               double lam = (denom*(mincos-cos)/computeForceSqr());
               modifyDvec (lam, dvec);
               r0 = forceDotDisp(dvec);
               if (debugLevel > 1) {
                  System.out.println (
                     "COS=" + cos +", new cos=" + 
                     r0/(forceNorm()*dvec.norm()));
               }
            }

            // set function and initial values of f(s) and df/ds for the
            // line search
            LineSearchFunc func = new EnergyFunc(pvec, dvec);
            double f0 = computeEnergy();
            double df0 = -forceDotDisp(dvec);
            // LineSearchFunc func = new ForceSqrFunc(pvec, dvec);
            // double f0 = computeForceSqr();
            // double df0 = computeForceSqrDeriv(dvec);

            if (debugLevel > 2) {
               printContactingKnotInfo(dvec);
            }
            
            // advance the knots by s1*dvec, where s1 is the SOR parameter
            // (which is 1.0 unless SOR is set otherwise)
            double s1 = mySor;
            advanceKnotPositions (pvec, s1, dvec);

            saveKnotContactData(); // save wrappableIdx and dist for eack knot
            contactChanged = updateContacts (/*getNormalDeriv=*/true);

            // see if contact has been broken from any contact groups
            boolean contactBroken = checkForBrokenContact();
            if (contactBroken && !contactChanged) {
               throw new InternalErrorException (
                  "broken contact detected but not contact change");
            }
            if (debugLevel > 1 && contactChanged) {
               System.out.println ("CONTACT CHANGED");
            }
            if (debugLevel > 1 && contactBroken) {
               System.out.println ("CONTACT BROKEN");
            }

            // update forces for new positions and contact 
            updateForces();

            // set values of f(s) and df/ds at s=s1 for the line search
            double f1 = computeEnergy(); // func.eval (deriv, s);
            double df1 = -forceDotDisp(dvec);// deriv.value;

            if (contactBroken) {
               // if contact was broken, evaluate the next anticipated
               // displacements from the new displacements. Use these for
               // separation control: reducing dvec if necessary to prevent
               // premature contact separation
               updateStiffness();
               VectorNd dnew = new VectorNd(myNumKnots*3);
               solveForKnotDisplacements(dnew);

               if (applySeparationControl (pvec, dvec, dnew)) {
                  // dvec was reduced. Recompute current position, contacts,
                  // forces, etc.
                  advanceKnotPositions (pvec, 1.0, dvec);
                  updateContacts (/*getNormalDeriv=*/true);
                  updateForces();
                  
                  s1 = 1.0;
                  f1 = computeEnergy();
                  df1 = -forceDotDisp(dvec);
               }
            }
            
            // check for convergence before line search, to avoid unnecessary
            // line search. At the moment, convergence is detected when force
            // drops below the threshold ftol
            if (!contactChanged) {
               // ftol works out to about 10^-6 in our examples
               if (forceNorm() < ftol) {
                  converged = true;
               }
            }
            if (!converged && myLineSearchP) {
               // apply line search if enabled and if we are not converged.  if
               // contact was not changed, we set maxs to 3, allowing the line
               // search to extend the step as well as reduce it.
               double maxs = (contactChanged ? s1 : 3.0);

               if (df0 >= 0) {
                  System.out.println ("  WARNING: df0=" + df0);
               }
               else {
                  double snew = lineSearch (f0, df0, f1, df1, func, s1, maxs);
                  if (snew != s1) {
                     // line search found value of s different from s1
                     if (debugLevel > 1) {
                        System.out.printf (
                           "    NEW line search snew=%g\n", snew);
                     }
                  }
                  // need to reset positions, contacts, forces etc. after line
                  // search
                  advanceKnotPositions (pvec, snew, dvec);
                  updateContacts (/*getNormalDeriv=*/true);
                  updateForces();
               }
            }
         }
         while (++icnt < maxIter && !converged);

         if (myUpdateContactsP) {
            saveContactingKnotPositions();
         }
         totalIterations += icnt;
         totalCalls++;
         boolean contacting = inContact();
         if (contacting) {
            myContactCnt++;
         }
         if (converged) {
            if (localMinimumFound) {
               totalFalseStuck++;
            }
            if (debugLevel > 0) {
               System.out.printf (
                  "converged, icnt=%d numc=%d forceNorm=%g\n",
                  icnt, numContacts(), forceNorm());
            }
            if (contacting) {
               myConvergedCnt++;
               maxForceNorm = Math.max(maxForceNorm, forceNorm());
               sumForceNorm += forceNorm();
               double lenErr = Math.abs(lastLength-getLength())/getLength();
               //maxLengthErr = Math.max(maxLengthErr, lenErr);
               //sumLengthErr += lenErr;
            }
         }
         else {
            if (debugLevel > 0) {
               System.out.printf (
                  "did NOT converge, icnt=%d numc=%d forceNorm=%g "+
                  "maxForce=%g ftol=%g\n",
                  icnt, numContacts(), forceNorm(), maxForce(), ftol);
            }
            totalFails++;
            maxForceNorm = Math.max(maxForceNorm, forceNorm());
            sumForceNorm += forceNorm();
            double lenErr = Math.abs(lastLength-getLength())/getLength();
            maxLengthErr = Math.max(maxLengthErr, lenErr);
            sumLengthErr += lenErr;
         }

         myIterationCnt += icnt;
         return icnt;
      }

      /**
       * Returns the position for knot k. If k == -1, returns the position of
       * the segment's initial point. If k === numKnots, returns the position
       * of the segment's final point. If k is outside the range [-1,numKnots],
       * returns null.
       */
      Point3d getKnotPos (int k) {
         if (k < 0) {
            if (k == -1) {
               return myPntB.getPosition();
            }
            else {
               return null;
            }
         }
         else if (k >= myNumKnots) {
            if (k == myNumKnots) {
               return myPntA.getPosition();
            }
            else {
               return null;
            }
         }
         else {
            return myKnots[k].myPos;
         }
      }               

      /**
       * Queries the number of knots in this wrappable segment.
       * 
       * @return number of knots
       */
      public int numKnots() {
         return myNumKnots;
      }

      /**
       * Returns the {@code idx}-th knot in this wrappable segment.
       * 
       * @param idx indexed of the desired knot
       * @return {@code idx}-th knot
       */
      public WrapKnot getKnot(int idx) {
         return myKnots[idx];
      }

      /**
       * Set the knot positions in this wrappable segment from
       * an array of points. Used for debugging and testing only.
       */
      private void setKnotPositions (Point3d[] plist) {
         if (plist.length < myNumKnots) {
            throw new IllegalArgumentException (
               "Number of positions "+plist.length+
               " less than number of knots");
         }
         for (int i=0; i<myNumKnots; i++) {
            myKnots[i].myPos.set (plist[i]);
         }
      }

      /**
       * Computes a normal for the plane containing the three points
       * <code>pa</code>, <code>p0</code>, and <code>p1</code>.  This plane can
       * be used to help compute the A/B points on the wrappable surface.
       *
       * <p>If the three points are colinear, then the normal is computed as
       * the vector that is perpendicular to both (pa, p0) and the
       * direction dir.
       */
      private void computeSideNormal (
         Vector3d sideNrm, Point3d pa, Point3d p0, Point3d p1, Vector3d dir) {

         Vector3d dela0 = new Vector3d();
         Vector3d dela1 = new Vector3d();

         dela0.sub (p0, pa);
         dela1.sub (p1, pa);
         double tol = 1e-8*dela1.norm();
         sideNrm.cross (dela1, dela0);
         double mag = sideNrm.norm();

         if (true || mag <= tol) {
            // cross product is too small to infer a normal; use dir
            sideNrm.cross (dela0, dir);
            mag = sideNrm.norm();
         }
         sideNrm.scale (1/mag);
      }

      String getCompName (String tag, String name) {
         if (name != null) {
            return " " + tag + "=" + name;
         }
         else {
            return "";
         }
      }

      /**
       * Creates a subsegment between two knots indexed by kb and ka. The
       * information is stored in <code>subseg</code>, unless
       * <code>subseg</code> is <code>null</code>, in which case a new
       * subsegment object is created and addded.
       *
       *<p> kb is the index of the last knot contacting the previous wrappable,
       * unless kb = -1, in which case the subsegment is formed between knot ka
       * and the segment's initial point. If knot kb is contacting a wrappable,
       * an exit point B is determined on that wrappable by computing the 
       * surface tangent associated with knots ka and kb-1.
       *
       * <p> ka is the index of the first knot contacting the next wrappable,
       * unless ka = numKnots, in which case the subsegment is formed between
       * knot kb and the segment's final point. If knot ka is contacting a 
       * wrappable, an exit point A is determined on that wrappable by 
       * computing the surface tangent associated with knots kb and ka+1.
       */
      private SubSegment addOrUpdateSubSegment (
         int ka, int kb, SubSegment subseg) {

         int wrappableIdxA = -1;
         Wrappable wrappableA = null;
         if (ka >= 0 && ka < myNumKnots) {
            wrappableIdxA = myKnots[ka].getWrappableIdx();
            if (wrappableIdxA != -1) {
               wrappableA = getWrappable(wrappableIdxA);
            }
            //wrappableA = myKnots[ka].getWrappable();
         }
         int wrappableIdxB = -1;
         Wrappable wrappableB = null;
         if (kb >= 0 && kb < myNumKnots) {
            wrappableIdxB = myKnots[kb].getWrappableIdx();
            if (wrappableIdxB != -1) {
               wrappableB = getWrappable(wrappableIdxB);
            }
         }

         Vector3d sideNrm = new Vector3d();
         Vector3d nrmlA = null;
         Point3d tanA = null;
         if (wrappableA != null) {
            nrmlA = new Vector3d();
            tanA = new Point3d();
            Point3d pb = getKnotPos (kb);
            Point3d pprev = getKnotPos (ka-1);
            Point3d pa = getKnotPos (ka);
            Point3d pnext = getKnotPos (ka+1);
            wrappableA.penetrationDistance (nrmlA, null, pa);
            double lam0 = LineSegment.projectionParameter (pb, pnext, pprev);
            if (lam0 <= 0.0 || lam0 >= 1.0) {
               // shouldn't happen - probably a glitch in the knot convergence
               tanA.set (pa);
            }
            else {
               computeSideNormal (sideNrm, pb, pa, pnext, nrmlA);
               wrappableA.surfaceTangent (
                  tanA, pb, pa, lam0, sideNrm);
               if (tanA.equals (pa)) {
                  System.out.printf (
                     "problem computing surfaceTanA%s%s\n",
                     getCompName("muscle", getName()),
                     getCompName("wrappable", wrappableA.getName()));
               }
            }
         }
         Vector3d nrmlB = null;
         Point3d tanB = null;
         if (wrappableB != null) {
            nrmlB = new Vector3d();
            tanB = new Point3d();
            Point3d pa = getKnotPos (ka);
            Point3d pprev = getKnotPos (kb+1);
            Point3d pb = getKnotPos (kb);
            Point3d pnext = getKnotPos (kb-1);
            wrappableB.penetrationDistance (nrmlB, null, pb);
            double lam0 = LineSegment.projectionParameter (pa, pnext, pprev);
            if (lam0 <= 0.0 || lam0 >= 1.0) {
               // shouldn't happen - probably a glitch in the knot convergence
               tanB.set (pb);
            }
            else {
               computeSideNormal (sideNrm, pa, pb, pnext, nrmlB);
               wrappableB.surfaceTangent (
                  tanB, pa, pb, lam0, sideNrm);
               if (tanB.equals (pb)) {
                  System.out.printf (
                     "problem computing surfaceTanB%s%s\n",
                     getCompName("muscle", getName()),
                     getCompName("wrappable", wrappableB.getName()));
               }
            }
         }
         
         if (subseg == null) {
            subseg = new SubSegment();
            addSubSeg (subseg);
         }
         if (wrappableB == null) {
            subseg.myPntB = myPntB;
            subseg.myBlockIdxB = myBlockIdxB;
            subseg.myAttachmentB = null;
            subseg.myWrappableIdxB = -1;
            subseg.myJB = null;
            subseg.myNrmB = null;
         }
         else { 
            if (subseg.myWrappableIdxB != wrappableIdxB) {
               subseg.myPntB = new Point(tanB);
               subseg.myAttachmentB =
               wrappableB.createPointAttachment (subseg.myPntB);
               subseg.myWrappableIdxB = wrappableIdxB;
               subseg.myBlockIdxB = wrappableIdxB + numPoints();
               subseg.myJB = new Matrix3x6();
               subseg.myNrmB = new Vector3d();
            }
            else {
               subseg.myPntB.setPosition (tanB);
               subseg.myAttachmentB.updateAttachment();
            }
            subseg.myNrmB.set (nrmlB);
            subseg.computeJMatrix (
               subseg.myJB, subseg.myPntB, nrmlB, wrappableB);
         }

         if (wrappableA == null) {
            subseg.myPntA = myPntA;
            subseg.myBlockIdxA = myBlockIdxA;
            subseg.myAttachmentA = null;
            subseg.myWrappableIdxA = -1;
            subseg.myJA = null;
            subseg.myNrmA = null;
         }
         else {
            if (subseg.myWrappableIdxA != wrappableIdxA) {
               subseg.myPntA = new Point(tanA);
               subseg.myAttachmentA =
               wrappableA.createPointAttachment (subseg.myPntA);
               subseg.myWrappableIdxA = wrappableIdxA;
               subseg.myBlockIdxA = wrappableIdxA + numPoints();
               subseg.myJA = new Matrix3x6();
               subseg.myNrmA = new Vector3d();
            }
            else {
               subseg.myPntA.setPosition (tanA);
               subseg.myAttachmentA.updateAttachment();
            }
            subseg.myNrmA.set (nrmlA);
            subseg.computeJMatrix (
               subseg.myJA, subseg.myPntA, nrmlA, wrappableA);           
         }
         subseg.myKa = ka;
         subseg.myKb = kb;

         return subseg.myNext;
      }

      /**
       * Updates the subsegments associated with this wrappable segment.
       * Assumes that updateWrapStrand() has already been called. If any knots
       * are in contact with wrappables, subsegments are created between (a)
       * the spring's initial point point and the first wrappable, (b) each 
       * distinct wrappable, and (c) the last wrappable and the spring's final
       * point.
       */
      private void updateSubSegments() {
         Wrappable wrappable = null;
         SubSegment subseg = mySubSegHead;
         int lastContactK = -1;
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            if (knot.inContact()) {
               if (knot.getWrappable() != wrappable) {
                  // transitioning to a new wrappable
                  subseg = addOrUpdateSubSegment (k, lastContactK, subseg);
                  wrappable = knot.getWrappable();
               }
               lastContactK = k;
            }
         }
         if (wrappable != null) {
            subseg = addOrUpdateSubSegment (myNumKnots, lastContactK, subseg);
         }
         if (subseg != null) {
            removeSubSegs (subseg);
         }
      }

      /**
       * If this segment has subsegments, return the first subsegment.
       * Otherwise, return <code>null</code>.
       */
      public SubSegment firstSubSegment() {
         return mySubSegHead;
      }

      /**
       * If this segment has subsegments, return the last subsegment.
       * Otherwise, return <code>null</code>.
       */
      public SubSegment lastSubSegment() {
         return mySubSegTail;
      }

       /**
       * Queries whether this segment has subsegments.
       * @return <code>true</code> if this segment has subsegments.
       */
     public boolean hasSubSegments() {
         return mySubSegHead != null;
      }

      // Begin methods to save and restore auxiliary state.
      //
      // Auxiliary state for a wrappable segment consists of the positions of
      // all the knot points.

     /**
      * {@inheritDoc}
      */
      public void getState (DataBuffer data) {
         data.dput (myLastPntA);
         data.dput (myLastPntB);
         data.zput (myNumKnots);
         int ncontact = 0;
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            data.dput (knot.myPos);
            data.dput (knot.myLocPos);
            data.dput (knot.myDist);
            data.dput (knot.myPrevDist);
            data.zput (knot.myWrappableIdx);
            if (knot.myWrappableIdx != -1) {
               ncontact++;                     
            }
            data.zput (knot.myPrevWrappableIdx);
         }
         data.dput (myLength);
      }

      /**
       * {@inheritDoc}
       */
      public void setState (DataBuffer data) {
         data.dget (myLastPntA);
         data.dget (myLastPntB);
         int numk = data.zget();
         if (numk != myNumKnots) {
            // adjust number of knots
            setNumKnots (numk);
         }
         boolean contacting = false;
         for (int k=0; k<myNumKnots; k++) {
            WrapKnot knot = myKnots[k];
            data.dget (knot.myPos);
            data.dget (knot.myLocPos);
            knot.myDist = data.dget();
            knot.myPrevDist = data.dget();
            knot.myWrappableIdx = data.zget();
            knot.myPrevWrappableIdx = data.zget();
            if (knot.myWrappableIdx != -1) {
               contacting = true;
            }

         }
         if (contacting) {
            // call updateContacts to update the myWrappable field for each knot
            updateContacts (/*getNormalDeriv=*/true); 
         }
         updateSubSegments();
         myLength = data.dget();
      }

      /**
       * {@inheritDoc}
       */
      public boolean hasState() {
         return true;
      }

      /**
       * {@inheritDoc}
       */
      public int getStateVersion() {
         return myStateVersion;
      }

      // End methods to save and restore auxiliary state.

      /**
       * Scan attributes of this wrappable segment from a ReaderTokenizer. Used
       * to read the spring from a file.
       */
      protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
         throws IOException {

         rtok.nextToken();
         if (ScanWriteUtils.scanAttributeName (rtok, "lastPntA")) {
            myLastPntA.scan (rtok);
            return true;
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "lastPntB")) {
            myLastPntB.scan (rtok);
            return true;
         }
         else if (ScanWriteUtils.scanAttributeName (rtok, "knots")) {
            rtok.scanToken ('[');
            ArrayList<WrapKnot> knots = new ArrayList<WrapKnot>();
            while (rtok.nextToken() != ']') {
               rtok.pushBack();
               WrapKnot knot = new WrapKnot();
               knot.scan (rtok);
               knots.add (knot);
            }
            myKnots = (WrapKnot[])knots.toArray(new WrapKnot[0]);
            myNumKnots = knots.size();
            return true;
         }
         rtok.pushBack();
         return super.scanItem (rtok, tokens);
      }

      /**
       * Writes attributes of this wrappable segment to a PrintWriter. Used to
       * write the spring to a file.
       */
      void writeItems (
         PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
         throws IOException {

         super.writeItems (pw, fmt, ancestor);
         pw.print ("lastPntA=");
         myLastPntA.write (pw, fmt, true);
         pw.println ("");
         pw.print ("lastPntB=");
         myLastPntB.write (pw, fmt, true);
         pw.println ("");
         if (myNumKnots > 0) {
            pw.println ("knots=[");
            IndentingPrintWriter.addIndentation (pw, 2);
            for (int i=0; i<myKnots.length; i++) {
               myKnots[i].write (pw, fmt);
            }
            IndentingPrintWriter.addIndentation (pw, -2);
            pw.println ("]");
         }
      }

      /**
       * Applies distance scaling to this wrappable segment.
       */      
      void scaleDistance (double s) {
         for (int k=0; k<myNumKnots; k++) {
            myKnots[k].myPos.scale (s);
         }
      }

      /**
       * Transforms the geometry of this wrappable segment.
       */      
      void transformGeometry (GeometryTransformer gtr) {
         gtr.transformPnt (myLastPntA);
         for (int k=0; k<myNumKnots; k++) {
            gtr.transformPnt (myKnots[k].myPos);
         }
         gtr.transformPnt (myLastPntB);
      }
      
      /**
       * Stores all knot positions for this wrappable into a vector.
       */
      void getKnotPositions (VectorNd pos) {
         pos.setSize (myNumKnots*3);
         double[] buf = pos.getBuffer();
         int idx = 0;
         for (int k=0; k<myNumKnots; k++) {
            Point3d p = myKnots[k].myPos;
            buf[idx++] = p.x;
            buf[idx++] = p.y;
            buf[idx++] = p.z;
         }
      }

      /**
       * Sets all knot positions for this wrappable from a vector
       */
      void setKnotPositions (VectorNd pos) {
         double[] buf = pos.getBuffer();
         int idx = 0;
         for (int k=0; k<myNumKnots; k++) {
            Point3d p = myKnots[k].myPos;
            p.x = buf[idx++];
            p.y = buf[idx++];
            p.z = buf[idx++];
         }
      }

      /**
       * Creates a clone of this wrappable segment.
       */
      public WrapSegment clone() {
         WrapSegment seg = (WrapSegment)super.clone();

         seg.myLastPntA = new Point3d(myLastPntA);
         seg.myLastPntB = new Point3d(myLastPntB);

         if (myKnots != null) {
            seg.myKnots = new WrapKnot[myKnots.length];
            for (int i=0; i<myKnots.length; i++) {
               seg.myKnots[i] = myKnots[i].clone();
            }
         }

         seg.myRenderABPoints = null;
         seg.mySubSegHead = null;
         seg.mySubSegTail = null;
         for (SubSegment sub=mySubSegHead; sub!=null; sub=sub.myNext) {
            SubSegment newSeg = sub.clone();
            newSeg.myNext = null;
            if (seg.mySubSegHead == null) {
               seg.mySubSegHead = newSeg;
            }
            else {
               seg.mySubSegTail.myNext = newSeg;
            }
            seg.mySubSegTail = newSeg;
         }

         return seg;
      }
   }

   /**
    * For debugging.
    */
   public void printSegments(String msg) {
      int i = 0;
      if (msg != null) {
         System.out.println (msg);
      }
      for (Segment seg : mySegments) {
         System.out.print (" seg "+i+": ");
         if (seg instanceof WrapSegment) {
            System.out.print (((WrapSegment)seg).numKnots());
         }
         System.out.println ("");
         i++;
      }
   }

   /**
    * For debugging.
    */
   public void printSegmentSpecs(String msg) {
      int i = 0;
      if (msg != null) {
         System.out.println (msg);
      }
      for (SegmentSpec spec : mySegmentSpecs) {
         System.out.print (" spec "+i+": " + spec);
         if (spec.getNumKnots() > 0) {
            System.out.print (" " + spec.getNumKnots() + " knots");
         }
         System.out.println ("");
         i++;
      }
   }

   /**
    * Returns {@code true} if this spring references wrappables which are both
    * renderable and currently visible.
    *
    * @return {@code true} if one or more of this spring's wrappables is
    * visible
    */
   public boolean hasVisibleWrappables() {
      for (WrappableSpec ws : myWrappables) {
         if (ws.myWrappable instanceof RenderableComponent) {
            if (RenderableComponentBase.isVisible(
                  (RenderableComponent)ws.myWrappable)) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Returns {@code true} if this spring references wrappables which are both
    * renderable and currently invisible.
    *
    * @return {@code true} if one or more of this spring's wrappables is
    * invisible
    */
   public boolean hasInvisibleWrappables() {
      for (WrappableSpec ws : myWrappables) {
         if (ws.myWrappable instanceof RenderableComponent) {
            if (!RenderableComponentBase.isVisible(
                  (RenderableComponent)ws.myWrappable)) {
               return true;
            }
         }
      }
      return false;
   }

   /**
    * Sets all renderable wrappables referenced by this spring to be visible or
    * invisible.
    *
    * @param visible if {@code true}, sets all wrappables to be visible.
    */
   public void setWrappablesVisible (boolean visible) {
      for (WrappableSpec ws : myWrappables) {
         if (ws.myWrappable instanceof RenderableComponent) {
            RenderableComponentBase.setVisible (
               (RenderableComponent)ws.myWrappable, visible);
         }
      }
   }

   public static void main (String[] args) {
      MultiPointSpring spr = new MultiPointSpring(null);
      Particle p0 = new Particle ("", 0,  0, 0, 0);
      Particle p1 = new Particle ("", 0,  1, 0, 0);
      Point3d ptarg = new Point3d (1, 1, 0);

      int numk = 10;
      spr.addPoint (p0);
      spr.setSegmentWrappable (numk);
      spr.addPoint (p1);
      spr.initializeSegment (0, null);
      spr.myLineSearchP = false;
      spr.setWrapDamping (100);
      WrapSegment seg = (WrapSegment)spr.getSegment(0);
      VectorNd pos = new VectorNd();
      VectorNd dst = new VectorNd(3*numk);
      int idx = 0;
      Vector3d diff = new Vector3d();
      diff.sub (ptarg, p0.getPosition());
      for (int k=0; k<numk; k++) {
         dst.set (idx++, (k+1)*diff.x/(numk+1));
         dst.set (idx++, (k+1)*diff.y/(numk+1));
         dst.set (idx++, (k+1)*diff.z/(numk+1));
      }
      VectorNd err = new VectorNd();
      seg.getKnotPositions (pos);
      err.sub (dst, pos);
      int icnt = 0;
      double tol = 0.0001;
      p1.setPosition (ptarg);
      while (err.infinityNorm() > tol) {
         icnt += seg.updateWrapStrand (100);
         seg.getKnotPositions (pos);
         System.out.println ("seg=" + pos.toString ("%8.3f"));
         System.out.println ("dst=" + dst.toString ("%8.3f"));
         err.sub (dst, pos);
      }
      System.out.println ("icnt=" + icnt);
   }

   // TODO: FINISH Jacobian stuff
}
