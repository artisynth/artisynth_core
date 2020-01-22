package artisynth.demos.test;

import java.awt.Color;
import java.util.*;

import artisynth.core.mechmodels.*;
import artisynth.core.workspace.*;
import artisynth.core.gui.*;

import maspack.matrix.*;
import maspack.util.QuadraticSolver;
import maspack.util.RandomGenerator;
import maspack.geometry.*;
import maspack.geometry.DistanceGrid.TetID;
import maspack.geometry.DistanceGrid.TetDesc;
import maspack.geometry.DistanceGridSurfCalc.TetPlaneIntersection;
import maspack.geometry.DistanceGridSurfCalc.PlaneType;
import maspack.render.*;
import maspack.render.Renderer.*;

public class QuadGridTangent extends RootModel {

   Particle myP0;
   Particle myPa;
   Particle myPt;
   Vector3d myNrm;
   RigidBody myBody;
   FixedMeshBody myPlane;   
   DistanceGrid myGrid;   
   boolean myPlaneFixed = false;

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      RandomGenerator.setSeed (0x1234);

      DistanceGridSurfCalc.TangentProblem tanProb = null;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-tanprob")) {
            if (++i >= args.length) {
               System.out.println (
                  "Warning: tanprob requires a file name argument");
            }
            tanProb = DistanceGridSurfCalc.TangentProblem.scan (args[i]);
         }
         else {
            System.out.println ("Warning: unrecognized argument " + args[i]);
         }
      }

      myPa = new Particle (0, -4, -0.01, 1);
      myPa.setDynamic (false);
      myP0 = new Particle (0, -0.01, -0.01, 1.95);
      //myP0 = new Particle (0, -0.1, 0, 0);
      myP0.setDynamic (false);
      myPt = new Particle (0, 0, 0, 0);
      myPt.setDynamic (false);
      double planeSize = 5.0;
      myNrm = new Vector3d();

      RigidTransform3d TPW = new RigidTransform3d (0, 0, 0, 0, 0, Math.PI/2);

      if (tanProb == null) {
         PolygonalMesh sphere = MeshFactory.createIcosahedralSphere (2.0, 4);
         myBody = new RigidBody ("sphere", sphere, null, 1000.0, 1.0);
         myBody.setDynamic (false);
         
         myBody.setGridSurfaceRendering (true);
         DistanceGridComp gcomp = myBody.getDistanceGridComp();
         gcomp.setSurfaceType (DistanceGridComp.SurfaceType.TRILNEAR);
         gcomp.setResolution (new Vector3i (10, 10, 10));
         RenderProps.setSphericalPoints (myBody, 0.02, Color.WHITE);
         mech.addRigidBody (myBody);
         createControlPanel (myBody);
         myGrid = gcomp.getGrid();

         myPa.setPosition (-4, -0.01, 1);
         myP0.setPosition (-0.01, -0.01, 1.95);

         Vector3d nrm = new Vector3d(
            -0.00832938947213402, -0.005480424451343519, -0.9999502918739784);
         nrm.set (0, 1, 0);
         TPW.R.setZDirection (nrm);
         TPW.p.set (0, -0.01, 0);
      }
      else {
         TPW.R.setZDirection (tanProb.myNrm);
         TPW.p.set (tanProb.myP0);         

         myPa.setPosition (tanProb.myPa);
         myP0.setPosition (tanProb.myP0);
         myNrm.set (tanProb.myNrm);

         myGrid = tanProb.myGrid;
         PolygonalMesh mesh = myGrid.createQuadDistanceSurface (0, 3);
         FixedMeshBody meshBody = new FixedMeshBody (mesh);
         meshBody.setPose (myGrid.getLocalToWorld());
         mech.addMeshBody (meshBody);
         planeSize = 2.5*RenderableUtils.getRadius (meshBody);
         myGrid.setDebug (1);
         myPlaneFixed = true;

         System.out.println ("setting pa " + myPa.getPosition());
         System.out.println ("setting p0 " + myP0.getPosition());
         System.out.println ("setting nrm " + myNrm);
      }
      
      PolygonalMesh rect = MeshFactory.createPlane (planeSize, planeSize);
      myPlane = new FixedMeshBody (rect);
      myPlane.setPose (TPW);
      RenderProps.setAlpha (myPlane, 0.7);
      RenderProps.setFaceStyle (myPlane, Renderer.FaceStyle.FRONT_AND_BACK);
      mech.addMeshBody (myPlane);

      RenderProps.setSphericalPoints (mech, 0.001*planeSize, Color.WHITE);
      RenderProps.setSphericalPoints (myPt, 0.0015*planeSize, Color.CYAN);

      AxialSpring spr = new AxialSpring (10, 0, 0);
      RenderProps.setCylindricalLines (spr, 0.0004*planeSize, Color.RED);

      mech.addParticle (myPa);
      mech.addParticle (myP0);
      mech.addParticle (myPt);
      mech.attachAxialSpring (myP0, myPa, spr);

      // myGrid.getLocalVertexCoords (vcoords, vxyz);
      // ArrayList<TetPlaneIntersection> isects =
      //    new ArrayList<TetPlaneIntersection>();

      // for (TetID tetId : TetID.values()) {
      //    TetDesc tdesc = myGrid.getTetDesc (vxyz.x, vxyz.y, vxyz.z, tetId);
      //    for (int j=0; j<10; j++) {
      //       nrm.setRandom();
      //       nrm.normalize();
      //       Plane plane = new Plane (nrm, nrm.dot(myPlane.getPosition()));
      //       int numa = myGrid.getFeatureAdjacentTets (
      //          isects, tdesc, new TetNode(0), plane);
      //       System.out.println ("num adjacent=" + numa);
      //    }
      // }
      // Point3d qp0 = new Point3d (-0.0101, -0.01, 2.01206509);
      // Point3d qp1 = new Point3d (-0.0099, -0.01, 2.01206509);

      // Vector3d grad = new Vector3d();
      // myGrid.getQuadDistanceAndGradient (grad, null, qp0);
      // System.out.println ("grad0= " + grad.toString ("%10.7f"));
      // myGrid.getQuadDistanceAndGradient (grad, null, qp1);
      // System.out.println ("grad1= " + grad.toString ("%10.7f"));
   }

   private void updateTangentPoint() {

      Point3d pa = new Point3d(myPa.getPosition());
      Point3d p0 = new Point3d(myP0.getPosition());
      Vector3d nrm = new Vector3d();
      if (myPlaneFixed) {
         nrm.set (myNrm);
         //Plane plane = new Plane (nrm, p0);
         //plane.project (pa, pa);
         //myPa.setPosition (pa);
      }
      else {
         Vector3d da0 = new Vector3d();
         da0.sub (p0, pa);
         nrm.cross (da0, Vector3d.Z_UNIT);
      }

      Point3d pt = new Point3d();
      if (myGrid.findQuadSurfaceTangent (pt, p0, pa, nrm)) {
         myPt.setPosition (pt);
         System.out.println ("found at " + pt);
      }
      else {
         System.out.println ("not found");
         myPt.setPosition (pt);
      }
   }

   ArrayList<Point3d> gridPlaneIntersectionPnts = null;
   ArrayList<Point3d> gridPlaneSurfacePnts = null;

   Point3d intersectEdge (double d0, Point3d p0, double d1, Point3d p1) {
      Point3d pi = new Point3d();
      double s = d0/(d0-d1);
      pi.combine (1-s, p0, s, p1);
      return pi;
   }

   private int intersectSurfaceAndEdge (
      double[] roots, double[] b, double px, double dx, double py, double dy) {

      double aa =
         b[0]*dx*dx + b[1]*dy*dy + b[2]*dx*dy;
      double bb =
         2*b[0]*px*dx + 2*b[1]*py*dy + b[2]*(px*dy + dx*py) + b[3]*dx + b[4]*dy;
      double cc =
         b[0]*px*px + b[1]*py*py + b[2]*px*py + b[3]*px + b[4]*py + b[5];

      return QuadraticSolver.getRoots (roots, aa, bb, cc, 0.0, 1.0);
   }

   private void intersectTetAndPlane2 (
      ArrayList<Point3d> spnts, DistanceGrid grid, TetDesc tdesc,
      TetPlaneIntersection isect, Plane plane) {

      double off = plane.offset;
      Vector3d nrm = new Vector3d(plane.normal);
      DistanceGridSurfCalc calc = new DistanceGridSurfCalc(grid);      
      Plane planeCell = new Plane();
      calc.transformToQuadCell (planeCell, plane, tdesc);
      
      Point3d pg = new Point3d();
      Point3d pgcell = new Point3d();
      pg.scale (off, nrm);  // grid point in local coords
      //grid.getLocalCoordinates (pgcell, pg);
      calc.transformToQuadCell (pgcell, pg, tdesc);

      Vector3d nrmCell = new Vector3d();
      //grid.getLocalNormal (nrmCell, nrm);
      grid.getGridToLocalTransformer().inverseTransformCovec (nrmCell, nrm);
      nrmCell.normalize();

      Vector3d r = new Vector3d();
      double[] c = new double[10];
      double[] b = new double[6];
      grid.computeQuadCoefs (c, tdesc);
      PlaneType planeType = calc.computeBCoefs (b, r, c, planeCell);

      Point3d[] ip = new Point3d[] {
         isect.myP0, isect.myP1, isect.myP2, isect.myP3 };

      Point3d pc0 = new Point3d();
      Point3d pc1 = new Point3d();
      Point3d pl0 = ip[0];
      Point3d pl1 = null;
      //grid.getLocalCoordinates (pc0, pl0);
      calc.transformToQuadCell (pc0, pl0, tdesc);

      double[] svals = new double[2];

      for (int i=1; i<=isect.myNumSides; i++) {
         pl1 = i < isect.myNumSides ? ip[i] : ip[0];
         //grid.getLocalCoordinates (pc1, pl1);
         calc.transformToQuadCell (pc1, pl1, tdesc);

         int nr = 0;
         switch (planeType) {
            case XY: {
               nr = intersectSurfaceAndEdge (
                  svals, b, pc0.x, pc1.x-pc0.x, pc0.y, pc1.y-pc0.y);
               break;
            }
            case YZ: {
               nr = intersectSurfaceAndEdge (
                  svals, b, pc0.y, pc1.y-pc0.y, pc0.z, pc1.z-pc0.z);
               break;
            }
            case ZX: {
               nr = intersectSurfaceAndEdge (
                  svals, b, pc0.z, pc1.z-pc0.z, pc0.x, pc1.x-pc0.x);
               break;
            }
         }
         if (nr > 0) {
            Point3d pi = new Point3d();
            Point3d pw = new Point3d();
            for (int j=0; j<nr; j++) {
               pi.combine (1-svals[j], pl0, svals[j], pl1);
               grid.getLocalToWorldTransformer().transformPnt (pw, pi);
               spnts.add (pw);
            }
         }
         pl0 = pl1;
         pc0.set (pc1);               
      }
      // System.out.println (
      //    "num points=" + (spnts.size()-npnts0) + " edges=" + nedges);
 
   }

   private void updateGridPlaneIntersection() {
      Vector3i res = myGrid.getResolution();

      Point3d[] vpnts = new Point3d[4];
      for (int i=0; i<vpnts.length; i++) {
         vpnts[i] = new Point3d();
      }
      RigidTransform3d XPW = myPlane.getPose();
      ArrayList<Point3d> ipnts = new ArrayList<Point3d>();
      ArrayList<Point3d> spnts = new ArrayList<Point3d>();

      Vector3d nrm = new Vector3d();
      XPW.R.getColumn (2, nrm);
      Plane plane = new Plane (nrm, XPW.p);
      // convert plane to grid local coords
      if (myGrid.hasLocalToWorld()) {
         plane.inverseTransform (myGrid.getLocalToWorld());
      }

      int mini = 0;
      int minj = 0;
      int mink = 0;
      int maxi = res.x/2;
      int maxj = res.y/2;
      int maxk = res.z/2;

      TetPlaneIntersection isect = new TetPlaneIntersection();
      DistanceGridSurfCalc calc = new DistanceGridSurfCalc(myGrid);
      // mini = 0;
      // minj = 2;
      // mink = 2;
      // maxi = 2;
      // maxj = 4;
      // maxk = 4;
      for (int i=mini; i<maxi; i++) {
         for (int j=minj; j<maxj; j++) {
            for (int k=mink; k<maxk; k++) {
               for (TetID tetId : TetID.values()) {
                  TetDesc tdesc = new TetDesc (2*i, 2*j, 2*k, tetId);
                  calc.getVertexCoords (vpnts, tdesc);
                  if (calc.intersectTetAndPlane (isect, tdesc, vpnts, plane)) {
                     intersectTetAndPlane2 (
                        spnts, myGrid, tdesc, isect, plane);
                     Point3d[] ip = new Point3d[4];
                     ip[0] = new Point3d(isect.myP0);
                     ip[1] = new Point3d(isect.myP1);
                     ip[2] = new Point3d(isect.myP2);
                     ip[3] = new Point3d(isect.myP3);
                     
                     for (int ii=0; ii<isect.myNumSides; ii++) {
                        myGrid.getLocalToWorldTransformer().transformPnt (
                           ip[ii], ip[ii]);
                     }
                     ipnts.add (ip[0]);
                     for (int ii=1; ii<isect.myNumSides; ii++) {
                        ipnts.add (ip[ii]);
                        ipnts.add (ip[ii]);
                     }
                     ipnts.add (ip[0]);         
                  }
               }
            }
         }
      }
      gridPlaneIntersectionPnts = ipnts;
      gridPlaneSurfacePnts = spnts;
   }

   public void prerender (RenderList list) {
      super.prerender (list);

      updateTangentPoint();
      //updateIntersectionPoint();
      updateGridPlaneIntersection();
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);

      ArrayList<Point3d> ipnts = gridPlaneIntersectionPnts;
      if (ipnts != null && ipnts.size() > 0) {
         renderer.setShading (Shading.NONE);
         renderer.setColor (Color.BLUE);
         renderer.beginDraw (DrawMode.LINES);
         for (int i=0; i<ipnts.size()-1; i += 2) {
            renderer.addVertex (ipnts.get(i));
            renderer.addVertex (ipnts.get(i+1));
         }
         renderer.endDraw();
      }

      ArrayList<Point3d> spnts = gridPlaneSurfacePnts;
      if (spnts != null && spnts.size() > 0) {
         renderer.setPointSize (4);
         renderer.setColor (Color.RED);
         renderer.beginDraw (DrawMode.POINTS);
         for (int i=0; i<spnts.size(); i++) {
            renderer.addVertex (spnts.get(i));
         }
         renderer.endDraw();
      }

   }

   private void createControlPanel (RigidBody body) {
      ControlPanel panel = new ControlPanel ("options", "");
      DistanceGridComp gcomp = body.getDistanceGridComp();
      panel.addWidget (body, "gridSurfaceRendering");
      panel.addWidget (gcomp, "resolution");
      panel.addWidget (gcomp, "renderGrid");
      addControlPanel (panel);
   }




}
