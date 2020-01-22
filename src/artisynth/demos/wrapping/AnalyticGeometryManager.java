package artisynth.demos.wrapping;

import artisynth.core.mechmodels.MultiPointSpring;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.RigidCylinder;
import artisynth.core.mechmodels.RigidEllipsoid;
import artisynth.core.mechmodels.RigidSphere;
import artisynth.core.mechmodels.RigidTorus;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.DistanceGrid;
import maspack.geometry.DistanceGrid.DistanceMethod;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.properties.PropertyList;
import maspack.util.FunctionTimer;

/**
 * Manages wrappable objects whose geometry is described
 * by analytical surfaces.
 *
 * @author Francois Roewer-Despres
 * @param <T>
 * the type of object that is managed
 */
public class AnalyticGeometryManager<T> extends Manager<T> {

   /**
    * The type of geometry.
    *
    * @author Francois Roewer-Despres
    */
   public static enum Geometry {
      CYLINDER, CYLINDER2, SPHERE, TORUS, ELLIPSOID, NONE;
   }

   /**
    * The type of {@link MultiPointSpring} wrap method.
    *
    * @author Francois Roewer-Despres
    */
   public static enum WrapMethod {
      ANALYTIC, SIGNED_DISTANCE_GRID, NONE;
   }

   // Default values for model-building properties.
   public static final double DEFAULT_SPHERE_RADIUS = 8.0;
   public static final double DEFAULT_CYLINDER_RADIUS = 4.0;
   public static final double DEFAULT_CYLINDER_LENGTH =
      DEFAULT_CYLINDER_RADIUS * 10.0;
   public static final Vector2d DEFAULT_TORUS_RADII = new Vector2d (8.0, 3.75);
   public static final Vector3d DEFAULT_ELLIPSOID_RADII =
      new Vector3d (7.0, 15.0, 5.0);
   public static final double DEFAULT_DENSITY = 1.0;
   public static final double DEFAULT_SCALE = 1.0;

   // Model-building properties.
   protected double mySphereRadius = DEFAULT_SPHERE_RADIUS;
   protected double myCylinderRadius = DEFAULT_CYLINDER_RADIUS;
   protected double myCylinderLength = DEFAULT_CYLINDER_LENGTH;
   protected Vector2d myTorusRadii = new Vector2d (DEFAULT_TORUS_RADII);
   protected Vector3d myEllipsoidRadii = new Vector3d (DEFAULT_ELLIPSOID_RADII);
   protected double myDensity = DEFAULT_DENSITY;
   protected double myScale = DEFAULT_SCALE;

   // Default values for properties.
   //public static final Geometry DEFAULT_GEOMETRY = Geometry.ELLIPSOID; 
   //public static final Geometry DEFAULT_GEOMETRY = Geometry.TORUS;
   public static final Geometry DEFAULT_GEOMETRY = Geometry.CYLINDER;
   public static final WrapMethod DEFAULT_WRAP_METHOD = WrapMethod.ANALYTIC;
   public static final int DEFAULT_RESOLUTION = 400;

   // Properties.
   private Geometry myGeometry = DEFAULT_GEOMETRY;
   private WrapMethod myWrapMethod = DEFAULT_WRAP_METHOD;
   private int myRes = DEFAULT_RESOLUTION;

   public static PropertyList myProps =
      new PropertyList (AnalyticGeometryManager.class, Manager.class);

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   static {
      myProps.add (
         "sphereRadius", "The sphere radius to use.", DEFAULT_SPHERE_RADIUS);
      myProps.add (
         "cylinderRadius", "The cylinder radius to use.",
         DEFAULT_CYLINDER_RADIUS);
      myProps.add (
         "cylinderLength", "The cylinder length to use.",
         DEFAULT_CYLINDER_LENGTH);
      myProps
         .add ("torusRadii", "The torus radii to use.", DEFAULT_TORUS_RADII);
      myProps.add (
         "ellipsoidRadii", "The ellipsoid radii to use.",
         DEFAULT_ELLIPSOID_RADII);
      myProps.add ("density", "The density to use.", DEFAULT_DENSITY);
      myProps.add ("scale", "The scale to use.", DEFAULT_SCALE);
      myProps.add ("geometry", "The Geometry to use.", DEFAULT_GEOMETRY);
      myProps.add ("wrapMethod", "The WrapMethod to use.", DEFAULT_WRAP_METHOD);
      myProps
         .add ("resolution", "The mesh resolution to use.", DEFAULT_RESOLUTION);
   }

   public double getSphereRadius () {
      return mySphereRadius;
   }

   public void setSphereRadius (double radius) {
      mySphereRadius = radius;
   }

   public double getCylinderRadius () {
      return myCylinderRadius;
   }

   public void setCylinderRadius (double radius) {
      myCylinderRadius = radius;
   }

   public double getCylinderLength () {
      return myCylinderLength;
   }

   public void setCylinderLength (double length) {
      myCylinderLength = length;
   }

   public Vector2d getTorusRadii () {
      return myTorusRadii;
   }

   public void setTorusRadii (Vector2d radii) {
      myTorusRadii = radii;
   }

   public Vector3d getEllipsoidRadii () {
      return myEllipsoidRadii;
   }

   public void setEllipsoidRadii (Vector3d radii) {
      myEllipsoidRadii = radii;
   }

   public double getDensity () {
      return myDensity;
   }

   public void setDensity (double density) {
      myDensity = density;
   }

   public double getScale () {
      return myScale;
   }

   public void setScale (double scale) {
      myScale = scale;
   }

   public Geometry getGeometry () {
      return myGeometry;
   }

   /**
    * Changes the currently "active" object.
    * <p>
    * Sets the new geometry to the given {@link Geometry}.
    *
    * @param geometry
    * the new {@code Geometry} for this {@link AnalyticGeometryManager}
    * @throws IllegalArgumentException
    * if geometry == Geometry.NONE {@code &&} !mySupportsNoneP
    */
   public void setGeometry (Geometry geometry) throws IllegalArgumentException {
      if (myGeometry != geometry) {
         if (geometry == Geometry.NONE && !mySupportsNoneP) {
            throw new IllegalArgumentException ("Geometry.NONE not supported.");
         }
         myGeometry = geometry;
         myUpdatable.update ();
      }
   }

   public WrapMethod getWrapMethod () {
      return myWrapMethod;
   }

   /**
    * Changes the currently "active" object.
    * <p>
    * Sets the new wrap method to the given {@link WrapMethod}.
    *
    * @param wrapMethod
    * the new {@code WrapMethod} for this {@link AnalyticGeometryManager}
    * @throws IllegalArgumentException
    * if wrapMethod == WrapMethod.NONE {@code &&} !mySupportsNoneP
    */
   public void setWrapMethod (WrapMethod wrapMethod)
      throws IllegalArgumentException {
      if (myWrapMethod != wrapMethod) {
         if (wrapMethod == WrapMethod.NONE && !mySupportsNoneP) {
            throw new IllegalArgumentException (
               "WrapMethod.NONE not supported.");
         }
         myWrapMethod = wrapMethod;
         myUpdatable.update ();
      }
   }

   public int getResolution () {
      return myRes;
   }

   /**
    * Changes the currently "active" object.
    * <p>
    * Sets the new resolution to the given resolution.
    *
    * @param res
    * the new resolution for this {@link AnalyticGeometryManager}
    */
   public void setResolution (int res) {
      if (myRes != res) {
         myRes = res;
         myUpdatable.update ();
      }
   }

   /**
    * {@inheritDoc}
    */
   public AnalyticGeometryManager (Creator<T> creator, Updatable updatable) {
      this (null, creator, updatable);
   }

   /**
    * {@inheritDoc}
    */
   public AnalyticGeometryManager (String name, Creator<T> creator,
   Updatable updatable) {
      super (name, creator, updatable);
   }

   @Override
   protected String getActiveName () {
      return myGeometry + "-" + myWrapMethod + "-" + myRes;
   }

   /**
    * Sets the distance grid resolution of the given {@link RigidBody} using the
    * given density.
    *
    * @param body
    * the {@code RigidBody} for which the distance grid resolution should be set
    * @param gridDensity
    * the grid density to use to set the distance grid resolution
    */
   public void setGridRes (
      RigidBody body, double gridDensity, Vector3i explicitRes) {
      // There will be gridDensity number of grid cells per unit of volume of
      // the body's bounding box.
      if (!explicitRes.equals(Vector3i.ZERO)) {
         body.setDistanceGridRes (explicitRes);
      }
      else {
         double density2 = gridDensity * 2;
         switch (myGeometry) {
            case CYLINDER:
               body.setDistanceGridRes (
                  new Vector3i (
                     (int)(density2 * myCylinderRadius),
                     (int)(density2 * myCylinderRadius),
                     (int)(gridDensity * myCylinderLength)));
               break;
            case CYLINDER2:
               body.setDistanceGridRes (
                  new Vector3i (
                     (int)(density2 * myCylinderRadius/3),
                     (int)(density2 * myCylinderRadius/3),
                     (int)(gridDensity * myCylinderLength)));
               break;
            case SPHERE:
               int res = (int)(density2 * mySphereRadius);
               body.setDistanceGridRes (new Vector3i (res, res, res));
               break;
            case TORUS:
               body.setDistanceGridRes (
                  new Vector3i (
                     (int)(density2 * myTorusRadii.x),
                     (int)(density2 * myTorusRadii.x),
                     (int)(density2 * myTorusRadii.y)));
               break;
            case ELLIPSOID:
               Vector3d scaled = new Vector3d ();
               scaled.scale (density2, myEllipsoidRadii);
               body.setDistanceGridRes (
                  new Vector3i ((int)scaled.x, (int)scaled.y, (int)scaled.z));
               break;
            case NONE:
               // Do nothing.
         }
      }
      if (myGeometry == Geometry.SPHERE) {
         //setDistanceUsingFaces (body, mySphereRadius);
         //setDistanceUsingMesh (body, mySphereRadius);
         //setSphericalDistanceGrid (body, mySphereRadius);
      }
   }

   /**
    * Returns via points that should be used when initializing a strand
    * around the object.
    */
   public Point3d[] getViaPoints() {
      switch (myGeometry) {
         case CYLINDER:
         case CYLINDER2:
            return new Point3d[] { new Point3d (0, -5, 0) };
         case SPHERE:
            return new Point3d[] { new Point3d (0, -10, 0) };
         case TORUS:
            return null;
         case ELLIPSOID:
            return new Point3d[] { new Point3d (-10, 0, 0) };
         default:
            return null;
      }
   }

   protected void printSphericalGridError (
      String msg, RigidBody body, double radius) {
      DistanceGrid grid = body.getDistanceGrid();
      double avgErr = 0;
      double maxErr = 0;
      double maxD = 0;
      Point3d maxCoords = null;
      for (int vi=0; vi<grid.numVertices(); vi++) {
         Point3d coords = new Point3d();
         grid.getLocalVertexCoords (coords, vi);
         double d = coords.norm()-radius;
         double err = Math.abs(d-grid.getVertexDistances()[vi]);
         avgErr += err;
         if (err > maxErr) {
            maxErr = err;
            maxD = grid.getVertexDistances()[vi];
            maxCoords = coords;
         }
      }
      avgErr /= grid.numVertices();
      System.out.println (
         msg + " avg=" + avgErr + " max=" + maxErr + " maxd=" + maxD + " at " + maxCoords);
   }

   protected void setDistanceUsingMesh (RigidBody body, double radius) {
      DistanceGrid grid = body.getDistanceGrid();
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      grid.setDistanceMethod (DistanceMethod.BVH);
      grid.computeDistances (body.getSurfaceMesh(), true);
      grid.setDistanceMethod (null);
      timer.stop();
      System.out.println ("bvh: " + timer.result(1));
      printSphericalGridError ("bvh:", body, radius);
   }

   protected void setDistanceUsingFaces (RigidBody body, double radius) {
      DistanceGrid grid = body.getDistanceGrid();
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      grid.setDistanceMethod (DistanceMethod.BRIDSON);
      grid.computeDistances (body.getSurfaceMesh(), true);
      grid.setDistanceMethod (null);
      timer.stop();
      System.out.println ("bridson: " + timer.result(1));
      printSphericalGridError ("bridson:", body, radius);
   }

   protected void setSphericalDistanceGrid (RigidBody body, double radius) {
      DistanceGrid grid = body.getDistanceGrid();
      double avgGridErr = 0;
      double maxGridErr = 0;
      double avgBvfqErr = 0;
      double maxBvfqErr = 0;
      double[] dists = new double[grid.numVertices()];
      for (int vi=0; vi<grid.numVertices(); vi++) {
         Point3d coords = new Point3d();
         grid.getLocalVertexCoords (coords, vi);
         double d = coords.norm()-radius;
         double err = Math.abs(d-grid.getVertexDistances()[vi]);
         avgGridErr += err;
         maxGridErr = Math.max (maxGridErr, err);
         dists[vi] = d;
         Point3d near = new Point3d();
         if (!coords.equals (Point3d.ZERO)) {
            double dq = BVFeatureQuery.distanceToMesh (
               near, body.getSurfaceMesh(), coords);
            if (dq != -1) {
               err = Math.abs(Math.abs(d)-dq);
               avgBvfqErr += err;
               if (err > maxBvfqErr) {
                  maxBvfqErr = err;
                  System.out.println (
                     "max at " + coords + "  near=" + near + "  dq=" + d);
               }
               dists[vi] = (d < 0 ? -dq : dq);
            }
         }
      }
      avgGridErr /= grid.numVertices();
      avgBvfqErr /= grid.numVertices();
      System.out.println (
         "gridErr: avg=" + avgGridErr + " max=" + maxGridErr);
      System.out.println (
         "bvfqErr: avg=" + avgBvfqErr + " max=" + maxBvfqErr);
      grid.setVertexDistances (dists, /*signed=*/true);
   }
      
   /**
    * Creates the {@link RigidBody} corresponding to the currently "active".
    *
    * @return the created {@code RigidBody}
    */
   protected RigidBody createActive () {
      String name = getActiveName ();
      int res = getResolution ();
      switch (myGeometry) {
         case CYLINDER:
            switch (myWrapMethod) {
               case ANALYTIC:
                  return new RigidCylinder (
                     name, myCylinderRadius, myCylinderLength, myDensity, res);
               case SIGNED_DISTANCE_GRID:
                  PolygonalMesh mesh =
                     MeshFactory.createCylinder (
                        myCylinderRadius, myCylinderLength, res);
                  RigidBody body =
                     new RigidBody (name, mesh, null, myDensity, myScale);
                  return body;
               case NONE:
                  return null;
            }
            break;
         case CYLINDER2:
            switch (myWrapMethod) {
               case ANALYTIC:
                  return new RigidCylinder (
                     name, myCylinderRadius/3, myCylinderLength, 9*myDensity, res);
               case SIGNED_DISTANCE_GRID:
                  PolygonalMesh mesh =
                     MeshFactory.createCylinder (
                        myCylinderRadius/3, myCylinderLength, res);
                  return new RigidBody (name, mesh, null, 9*myDensity, myScale);
               case NONE:
                  return null;
            }
            break;
         case SPHERE:
            switch (myWrapMethod) {
               case ANALYTIC:
                  return new RigidSphere (name, mySphereRadius, myDensity, res);
               case SIGNED_DISTANCE_GRID:
                  PolygonalMesh mesh =
                     MeshFactory.createSphere (mySphereRadius, res);
                  RigidBody body =
                     new RigidBody (name, mesh, null, myDensity, myScale);
                  return body;
               case NONE:
                  return null;
            }
            break;
         case TORUS:
            switch (myWrapMethod) {
               case ANALYTIC:
                  return new RigidTorus (
                     name, myTorusRadii.x, myTorusRadii.y, myDensity, res, res);
               case SIGNED_DISTANCE_GRID:
                  PolygonalMesh mesh =
                     MeshFactory
                        .createTorus (myTorusRadii.x, myTorusRadii.y, res, res);
                  return new RigidBody (name, mesh, null, myDensity, myScale);
               case NONE:
                  return null;
            }
            break;
         case ELLIPSOID:
            switch (myWrapMethod) {
               case ANALYTIC:
                  return new RigidEllipsoid (
                     name, myEllipsoidRadii.x, myEllipsoidRadii.y,
                     myEllipsoidRadii.z, myDensity, res);
               case SIGNED_DISTANCE_GRID:
                  PolygonalMesh mesh = // Necessary hack.
                     new RigidEllipsoid (
                        name, myEllipsoidRadii.x, myEllipsoidRadii.y,
                        myEllipsoidRadii.z, myDensity, res).getSurfaceMesh ();
                  return new RigidBody (name, mesh, null, myDensity, myScale);
               case NONE:
                  return null;
            }
            break;
         case NONE:
            return null;
      }
      return null;
   }
}
