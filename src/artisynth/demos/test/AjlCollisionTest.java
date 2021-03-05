package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.mechmodels.*;
import artisynth.core.renderables.*;
import artisynth.core.workspace.*;
import artisynth.core.gui.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.collision.*;
import maspack.collision.SurfaceMeshIntersector.CSG;
import maspack.collision.SurfaceMeshIntersectorTest.TestInfo;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.geometry.*;
import maspack.geometry.MeshFactory.FaceType;
import maspack.geometry.io.*;
import maspack.properties.*;

public class AjlCollisionTest extends RootModel {

   MechModel myMech;
   RigidBody myBody0;
   RigidBody myBody1;

   EditablePolygonalMeshComp myEditMesh0;
   EditablePolygonalMeshComp myEditMesh1;
   EditablePolygonalMeshComp myEditCSGMesh;
   IntersectionTester myTester;

   ControlPanel myControlPanel;

   public static PropertyList myProps =
      new PropertyList (AjlCollisionTest.class, RootModel.class);

   static {
      myProps.add ("mesh0Visible", "make mesh 0 visible", false);
      myProps.add ("mesh1Visible", "make mesh 1 visible", false);
      myProps.add ("editMesh0Visible", "make edit mesh 0 visible", false);
      myProps.add ("editMesh1Visible", "make edit mesh 1 visible", false);
      myProps.add ("editCSGMeshVisible", "make edit CSG mesh visible", false);
      myProps.add ("CSGOperation", "CSG operation", SurfaceMeshIntersector.CSG.NONE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public boolean getMesh0Visible() {
      return myBody0.getRenderProps().isVisible();
   }

   public void setMesh0Visible (boolean visible) {
      RenderProps.setVisible (myBody0, visible);
   }

   public boolean getMesh1Visible() {
      return myBody1.getRenderProps().isVisible();
   }

   public void setMesh1Visible (boolean visible) {
      RenderProps.setVisible (myBody1, visible);
   }

   public boolean getEditMesh0Visible() {
      return myEditMesh0.getRenderProps().isVisible();
   }

   public void setEditMesh0Visible (boolean visible) {
      RenderProps.setVisible (myEditMesh0, visible);
   }

   public boolean getEditMesh1Visible() {
      return myEditMesh1.getRenderProps().isVisible();
   }

   public void setEditMesh1Visible (boolean visible) {
      RenderProps.setVisible (myEditMesh1, visible);
   }

   public SurfaceMeshIntersector.CSG getCSGOperation() {
      return myTester.getCSGOperation();
   }

   public void setCSGOperation (SurfaceMeshIntersector.CSG op) {
      if (op != getCSGOperation()) {
         myTester.setCSGOperation(op);
         if (getEditCSGMeshVisible()) {
            setEditCSGMeshVisible (false);
            setEditCSGMeshVisible (true);
         }
      }
   }
   
   public boolean getEditCSGMeshVisible() {
      return myEditCSGMesh != null && myEditCSGMesh.getRenderProps().isVisible();
   }

   public void setEditCSGMeshVisible (boolean visible) {
      if (visible != getEditCSGMeshVisible()) {
         if (!visible) {
            RenderProps.setVisible (myEditCSGMesh, false);
         }
         else {
            if (myEditCSGMesh != null) {
               removeEditMesh (myMech, myEditCSGMesh);
               myEditCSGMesh = null;
            }
            if (myTester != null && myTester.getCSGMesh() != null) {
               myEditCSGMesh = addEditMesh (myMech, myTester.getCSGMesh());
               RenderProps.setDrawEdges (myEditCSGMesh, true);
               RenderProps.setEdgeColor (myEditCSGMesh, Color.CYAN);
               RenderProps.setVisible (myEditCSGMesh, true);
            }
         }
      }
   }

   RigidBody addBody (MechModel mech, PolygonalMesh mesh, RigidTransform3d pose) {
      RigidTransform3d TMW = (pose != null ? new RigidTransform3d (pose) : null);
      RigidBody body = RigidBody.createFromMesh (null, mesh, 1000, 1.0);
      body.setDynamic(false);

      if (TMW != null) {
         body.setPose (TMW);
         // XXX hack since body.setPose() may alter pose slightly from TMW
         mesh.setMeshToWorld (TMW); 
      }
      RenderProps.setDrawEdges (body, true);
      //RenderProps.setEdgeColor (body, Color.CYAN);
      RenderProps.setFaceStyle (body, FaceStyle.NONE);
      mech.addRigidBody (body);
      return body;
   }

   EditablePolygonalMeshComp addEditMesh (MechModel mech, PolygonalMesh mesh) {
      EditablePolygonalMeshComp editMesh =
         new EditablePolygonalMeshComp (mesh);

      RenderProps.setVisible (editMesh, false);
      RenderProps.setShading (editMesh, Shading.FLAT);

      mech.addRenderable (editMesh);
      return editMesh;
   }   

   void removeEditMesh (MechModel mech, EditablePolygonalMeshComp editMesh) {
      mech.removeRenderable (editMesh);
   }   
 
   private void setupBodies (
      MechModel mech, PolygonalMesh mesh0, PolygonalMesh mesh1) {

      myBody0 = addBody (mech, mesh0, mesh0.getMeshToWorld());
      myBody1 = addBody (mech, mesh1, mesh1.getMeshToWorld());

      myEditMesh0 = addEditMesh (mech, mesh0);
      myEditMesh1 = addEditMesh (mech, mesh1);
      double rad = RenderableUtils.getRadius (mech);
      RenderProps.setSphericalPoints (mech, 0.005*rad, Color.RED);
   }

   private void addControlPanel() {
      myControlPanel = new ControlPanel ("options", "");
      if (myBody0 != null) {
         myControlPanel.addWidget (this, "mesh0Visible");
      }
      if (myBody1 != null) {
         myControlPanel.addWidget (this, "mesh1Visible");
      }
      if (myEditMesh0 != null) {
         myControlPanel.addWidget (this, "editMesh0Visible");
      }
      if (myEditMesh1 != null) {
         myControlPanel.addWidget (this, "editMesh1Visible");
      }
      if (myTester != null) {
         myControlPanel.addWidget (myTester, "renderCSGMesh");
         myControlPanel.addWidget (this, "CSGOperation");
      }
      myControlPanel.addWidget (this, "editCSGMeshVisible");
      addControlPanel (myControlPanel);
   }

   private void readTestFile (
      String testFileName, PolygonalMesh mesh0, PolygonalMesh mesh1) {

      TestInfo tinfo = new TestInfo();
      CSG csgOp = null;
      try {
         csgOp = SurfaceMeshIntersectorTest.scanProblem (
            testFileName, mesh0, mesh1, tinfo);
      }
      catch (Exception e) {
         e.printStackTrace(); 
      }
      SurfaceMeshIntersector smi = new SurfaceMeshIntersector();

      System.out.println ("csgOp=" + csgOp);
      System.out.println (
         "mesh0 manifold="+mesh0.isManifold()+" closed="+mesh0.isClosed());
      System.out.println (
         "mesh1 manifold="+mesh1.isManifold()+" closed="+mesh1.isClosed());

      if (tinfo.TBW != null) {
         RigidTransform3d TMW0Orig =
            new RigidTransform3d (mesh0.getMeshToWorld());
         RigidTransform3d TMW0 = new RigidTransform3d();
         TMW0.mul (tinfo.TBW, TMW0Orig);
         mesh0.setMeshToWorld (TMW0);
         System.out.println ("TMW0=\n" + TMW0);
      }
      
      if (tinfo.TBW != null || tinfo.T10 != null) {
         RigidTransform3d TMW1Orig =
            new RigidTransform3d (mesh1.getMeshToWorld());
         RigidTransform3d TMW1 = new RigidTransform3d();
         RigidTransform3d T1W = new RigidTransform3d();
         if (tinfo.TBW != null) {
            T1W.set (tinfo.TBW);
         }
         if (tinfo.T10 != null) {
            T1W.mul (T1W, tinfo.T10);
         }
         TMW1.mul (T1W, TMW1Orig);
         mesh1.setMeshToWorld (TMW1);
         System.out.println ("TMW1=\n" + TMW1);
      }      
   }

   public void build (String[] args) {

      RandomGenerator.setSeed (0x1234);
      myMech = new MechModel ("mech");
      addModel (myMech);   

      SurfaceMeshIntersector.CSG csg = null;
      String crashFile = null;
      boolean contoursOnly = false;

      double perturb0 = 0;
      double perturb1 = 0;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-intersection")) {
            csg = SurfaceMeshIntersector.CSG.INTERSECTION;
         }
         else if (args[i].equals ("-union")) {
            csg = SurfaceMeshIntersector.CSG.UNION;
         }
         else if (args[i].equals ("-difference01")) {
            csg = SurfaceMeshIntersector.CSG.DIFFERENCE01;
         }
         else if (args[i].equals ("-difference10")) {
            csg = SurfaceMeshIntersector.CSG.DIFFERENCE10;
         }
         else if (args[i].equals ("-contoursOnly")) {
            contoursOnly = true;
         }
         else if (args[i].equals ("-perturb0")) {
            if (++i == args.length) {
               System.out.println (
                  "WARNING: -perturb0 needs an additional argument");
            }
            else {
               perturb0 = Double.valueOf (args[i]);
            }
         }
         else if (args[i].equals ("-perturb1")) {
            if (++i == args.length) {
               System.out.println (
                  "WARNING: -perturb1 needs an additional argument");
            }
            else {
               perturb1 = Double.valueOf (args[i]);
            }
         }
         else if (!args[i].startsWith ("-")) {
            crashFile = args[i];
         }
         else {
            System.out.println ("Unrecognized model argument "+args[i]);
         }
      }

      if (contoursOnly) {
         csg = null;
      }

      String denseOuterStr = new String(
            "v  2  3  0.5\n" + 
            "v  0  3  0.5\n" + 
            "v -2  3  0.5\n" + 
            "v -2 -1  0.5\n" + 
            "v  0 -3  0.5\n" + 
            "v  2 -1  0.5\n" + 
            "v  1  1  0.5\n" + 
            "v -1  1  0.5\n" + 
            "v  0 -1  0.5\n" + 
            "v  2  3 -0.5\n" + 
            "v  0  3 -0.5\n" + 
            "v -2  3 -0.5\n" + 
            "v -2 -1 -0.5\n" + 
            "v  0 -3 -0.5\n" + 
            "v  2 -1 -0.5\n" + 
            "v  1  1 -0.5\n" + 
            "v -1  1 -0.5\n" + 
            "v  0 -1 -0.5\n" + 

            "f 0 1 6\n" +
            "f 0 6 5\n" +
            "f 1 2 7\n" +
            "f 2 3 7\n" +
            "f 5 8 4\n" +
            "f 8 3 4\n" +
            "f 1 7 6\n" +
            "f 6 7 8\n" +
            "f 7 3 8\n" +
            "f 6 8 5\n" +

            "f 1 0 9\n" +
            "f 1 9 10\n" +
            "f 2 1 10\n" +
            "f 2 10 11\n" +
            "f 3 2 11\n" +
            "f 3 11 12\n" +
            "f 4 3 12\n" +
            "f 4 12 13\n" +
            "f 5 4 13\n" +
            "f 5 13 14\n" +
            "f 0 5 14\n" +
            "f 0 14 9\n" +
               
            "f 10 9 15\n" +
            "f 15 9 14\n" +
            "f 12 11 16\n" +
            "f 16 11 10\n" +
            "f 10 15 16\n" +
            "f 16 17 12\n" +
            "f 16 15 17\n" +
            "f 15 14 17\n" +
            "f 12 17 13\n" +
            "f 17 14 13\n");

      String sparseOuterStr = new String(
            "v  2  3  0.5\n" + 
            "v  0  3  0.5\n" + 
            "v -2  3  0.5\n" + 
            "v -2 -1  0.5\n" + 
            "v  0 -3  0.5\n" + 
            "v  2 -1  0.5\n" + 
            "v  2  3 -0.5\n" + 
            "v  0  3 -0.5\n" + 
            "v -2  3 -0.5\n" + 
            "v -2 -1 -0.5\n" + 
            "v  0 -3 -0.5\n" + 
            "v  2 -1 -0.5\n" + 

            "f 0 1 5\n" +
            "f 1 2 3\n" +
            "f 5 3 4\n" +
            "f 1 3 5\n" +

            "f 1 0 6\n" +
            "f 1 6 7\n" +
            "f 2 1 7\n" +
            "f 2 7 8\n" +
            "f 3 2 8\n" +
            "f 3 8 9\n" +
            "f 4 3 9\n" +
            "f 4 9 10\n" +
            "f 5 4 10\n" +
            "f 5 10 11\n" +
            "f 0 5 11\n" +
            "f 0 11 6\n" +
               
            "f 7 6 11\n" +
            "f 9 8 7\n" +
            "f 9 11 10\n" +
            "f 7 11 9\n");

      String denseOuterStrOld = new String (
         "v  1  1  0.5\n" +
         "v  0  3  0.5\n" +
         "v -1  1  0.5\n" +
         "v -2 -1  0.5\n" +
         "v  0 -1  0.5\n" +
         "v  2 -1  0.5\n" +
         "v  1  1 -0.5\n" +
         "v  0  3 -0.5\n" +
         "v -1  1 -0.5\n" +
         "v -2 -1 -0.5\n" +
         "v  0 -1 -0.5\n" +
         "v  2 -1 -0.5\n" +

         "f 0 2 4\n" + 
         "f 0 1 2\n" + 
         "f 2 3 4\n" + 
         "f 0 4 5\n" + 

         "f 3 10 4\n" +
         "f 3 9 10\n" +
         "f 5 4 10\n" +
         "f 5 10 11\n" +

         "f 2 1 8\n" +
         "f 1 7 8\n" +
         "f 3 2 8\n" +
         "f 3 8 9\n" +

         "f 0 5 6\n" +
         "f 5 11 6\n" +
         "f 0 6 1\n" +
         "f 1 6 7\n" +

         "f 6 8 7\n" + 
         "f 6 10 8\n" + 
         "f 8 10 9\n" + 
         "f 6 11 10\n");

      String denseInnerStr = new String (
         "v  1  1  1.5\n" + 
         "v -1  1  1.5\n" + 
         "v  0 -1  1.5\n" + 
         "v  1  1  0.5\n" + 
         "v -1  1  0.5\n" + 
         "v  0 -1  0.5\n" + 
         "v  1  1 -0.5\n" + 
         "v -1  1 -0.5\n" + 
         "v  0 -1 -0.5\n" + 
         "v  1  1 -1.5\n" + 
         "v -1  1 -1.5\n" + 
         "v  0 -1 -1.5\n" + 
         "f 0 1 2\n" + 

         "f 2 1 4\n" + 
         "f 2 4 5\n" + 
         "f 0 2 5\n" + 
         "f 0 5 3\n" + 
         "f 1 0 3\n" + 
         "f 1 3 4\n" + 

         "f 5 4 7\n" + 
         "f 5 7 8\n" + 
         "f 3 5 8\n" + 
         "f 3 8 6\n" + 
         "f 4 3 6\n" + 
         "f 4 6 7\n" + 

         "f 8 7 10\n" + 
         "f 8 10 11\n" + 
         "f 6 8 11\n" + 
         "f 6 11 9\n" + 
         "f 7 6 9\n" + 
         "f 7 9 10\n" + 

         "f 9 11 10\n");
      
      PolygonalMesh mesh0 = null;
      PolygonalMesh mesh1 = null;
      TestInfo tinfo;

      if (false) {
         PolygonalMesh tallBox65 = MeshFactory.createBox (0.6, 0.5, 2.0);
         PolygonalMesh cube = MeshFactory.createBox (1.0, 1.0, 1.0);
         tallBox65.setMeshToWorld (new RigidTransform3d (0.2, 0, 0.0));

         mesh0 = cube;
         mesh1 = tallBox65;
         setupBodies (myMech, mesh0, mesh1);
      }

      if (false) {
         String dataDir = 
            PathFinder.expand (
               "${srcdir PolygonalMesh}/sampleData/");
         
         try {
            mesh0 = new PolygonalMesh (dataDir+"molar1.2.obj");
            mesh1 = new PolygonalMesh (dataDir+"molar2.2.obj");
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
         setupBodies (myMech, mesh0, mesh1);
      }
      
      if (false) {
         String dataDir = 
            PathFinder.expand (
               "${srcdir PolygonalMesh}/sampleData/");
         
         try {
            mesh0 = new PolygonalMesh (dataDir+"molar1.2.obj");
            mesh1 = MeshFactory.createPlane (2, 2);
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
         setupBodies (myMech, mesh0, mesh1);
      }
      
      if (false) {
         String dataDir = 
            PathFinder.expand (
               "${srcdir AjlCollisionTest}/data/");
         
         try {
            mesh0 = new PolygonalMesh (dataDir+"ACLC01-R-femur-cart-072715.obj");
            mesh1 = new PolygonalMesh (dataDir+"ACLC01-R-tibia-cart-072715.obj");
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
         setupBodies (myMech, mesh0, mesh1);
      }
      
      if (true) {
         String dataDir = 
            PathFinder.expand (
               "${srcdir AjlCollisionTest}/data/");
         
         try {
            mesh1 = new PolygonalMesh (dataDir+"clipmesh.obj");
            mesh0 = new PolygonalMesh (dataDir+"PatientMesh.obj");
         }
         catch (Exception e) {
            e.printStackTrace(); 
         }
         setupBodies (myMech, mesh0, mesh1);
      }
      
      if (false) {
         mesh0 = MeshFactory.createBox (
            3.0, 3.0, 1.0, Point3d.ZERO, 1, 1, 1);
         mesh1 = MeshFactory.createBox (
            1.0, 1.0, 5.0, Point3d.ZERO, 1, 1, 5);
         mesh1.transform (new RigidTransform3d (-0.6, 0.6, 0));
         setupBodies (myMech, mesh0, mesh1);
      }

      if (false) {
         mesh0 = MeshFactory.createRectangle (2.0, 2.0, false);
         mesh1 = MeshFactory.createOpenCylinder (0.5, 1.0, 12, 4);
         PolygonalMesh inner = MeshFactory.createOpenCylinder (0.3, 1.0, 12, 4);
         mesh1.addMesh (inner);
         setupBodies (myMech, mesh0, mesh1);
      }

      if (false) {
         mesh0 = MeshFactory.createBox (
            6.0, 5.0, 1.0, Point3d.ZERO, 3, 5, 1);
         mesh1 = MeshFactory.createSkylineMesh (
            3.0, 4.0, 1.0, 3, 4,
            "111",
            "1 1",
            "1 1",
            "111");
         mesh1.transform (new RigidTransform3d (0.0, 0.0, -1.0));
         setupBodies (myMech, mesh0, mesh1);
      }

      if (false) {
         mesh0 = MeshFactory.createBox (
            5.0, 3.0, 0.5, Point3d.ZERO, 1, 1, 1);
         mesh1 = MeshFactory.createSkylineMesh (
            3.0, 1.0, 1.0, 3, 1,
            "1 1");
         mesh1.transform (new RigidTransform3d (0.0, 0.0, -0.25));
         setupBodies (myMech, mesh0, mesh1);
      }

      if (false) {
         PolygonalMesh rect10x4 = MeshFactory.createRectangle (
            10.0, 6.0, 10, 4, false);
         PolygonalMesh hollowBox = MeshFactory.createSkylineMesh (
            3.0, 4.0, 1.0, 3, 4,
            "222",
            "2 2",
            "2 2",
            "222");
         hollowBox.transform (new RigidTransform3d (0.0, 2.5, -1.0));
         //hollowBox.setMeshToWorld (new RigidTransform3d (-3.0, 0.0, 0.0));
         mesh0 = rect10x4;
         mesh1 = hollowBox;
         setupBodies (myMech, mesh0, mesh1);
      }

      if (false) {
         PolygonalMesh rect = MeshFactory.createBox (
            10.0, 5.0, 1.0, Point3d.ZERO, 1, 1, 1);
         PolygonalMesh hollowBox = MeshFactory.createSkylineMesh (
            3.0, 3.0, 1.0, 3, 3,
            "333",
            "3 3",
            "333");
         hollowBox.transform (new RigidTransform3d (0.0, 0, -1.5));
         //hollowBox.setMeshToWorld (new RigidTransform3d (-3.0, 0.0, 0.0));
         mesh0 = rect;
         mesh1 = hollowBox;
         setupBodies (myMech, mesh0, mesh1);
      }

      if (false) {
         // box and stylus
         PolygonalMesh box = MeshFactory.createBox (
            10.0, 5.0, 5.0, Point3d.ZERO, 1, 1, 1);
         PolygonalMesh stylus = MeshFactory.createRoundedCylinder (
            0.5, 4.0, 8, 2, /*flatBottom=*/false);
         //stylus.setMeshToWorld (new RigidTransform3d (-3.0, 0.0, 0.0));
         mesh0 = box;
         mesh1 = stylus;
         setupBodies (myMech, mesh0, mesh1);
         myBody1.setPose (
            new RigidTransform3d (0, 5, 2, 0, 0, Math.toRadians(-30)));
      }

      if (false) {
         //PolygonalMesh sparseOuter = MeshFactory.createPrism (
         //      new double[] { 0.0, 3.0, -2.0, -1.0, 2.0, -1.0 }, 1.0);
         PolygonalMesh sparseInner = MeshFactory.createPrism (
            new double[] { 1.0, 1.0, -1.0, 1.0, 0.0, -1.0 }, 2.0);
         //mesh1.transform (new RigidTransform3d (0.0, 0.0, -0.5));

         PolygonalMesh sparseOuter = 
            WavefrontReader.readFromString (sparseOuterStr, true);
         PolygonalMesh denseOuter =
            WavefrontReader.readFromString (denseOuterStr, true);
         PolygonalMesh denseInner = 
            WavefrontReader.readFromString (denseInnerStr, true);

         mesh1 = denseInner;
         mesh0 = denseOuter;
         setupBodies (myMech, mesh0, mesh1);
      }
      
      if (false) {
         mesh0 = MeshFactory.createRectangle (
            3.0, 3.0, 3, 3, false);
         mesh1 = MeshFactory.createSkylineMesh (
            2.0, 1.5, 1.0, 5, 3,
            "11111",
            "     ",
            "11111");
         setupBodies (myMech, mesh0, mesh1);
      }

      if (false) {
         mesh0 = MeshFactory.createBox (
            2.5, 2.5, 2.5, Point3d.ZERO, 2, 2, 2, /*addNormals=*/false);
         mesh1 = MeshFactory.createSphere (1.0, 20);
         setupBodies (myMech, mesh0, mesh1);
      }
      

      if (false) {
         mesh0 = MeshFactory.createBox (
            2.5, 2.5, 2.5, Point3d.ZERO, 1, 1, 1, /*addNormals=*/false);
         mesh1 = MeshFactory.createBox (
            1.5, 1.5, 1.5, Point3d.ZERO, 1, 1, 1, /*addNormals=*/false);
         setupBodies (myMech, mesh0, mesh1);
      }
      
      if (false) {
         // crown box
         mesh0 = MeshFactory.createBox (
            2.0, 2.0, 2.0, Point3d.ZERO, 2, 2, 2,
            /*addNormals=*/false, FaceType.ALT_TRI);

         mesh0.getVertex(7).pnt.z -= 0.5;
         mesh0.getVertex(11).pnt.z -= 0.5;
         mesh0.getVertex(5).pnt.z += 0.5;
         mesh0.getVertex(14).pnt.z += 0.5;
         mesh0.notifyVertexPositionsModified();

         mesh1 = MeshFactory.createBox (
            3.0, 3.0, 1.0, Point3d.ZERO, 2, 2, 1,
            /*addNormals=*/false, FaceType.ALT_TRI);
         setupBodies (myMech, mesh0, mesh1);
         myBody1.setPose (new RigidTransform3d (0, 0, 1.5));
      }
      
      if (false) {
         // crown cylinder
         mesh0 = MeshFactory.createCylinder (1.0, 2.0, 12);

         int[] upVtxs = new int[] {2, 7, 11, 15, 19, 23 };
         int[] downVtxs = new int[] {3, 5, 9, 13, 17, 21};

         for (int k : upVtxs) {
            mesh0.getVertex(k).pnt.z += 0.25;
         }
         for (int k : downVtxs) {
            mesh0.getVertex(k).pnt.z -= 0.25;
         }
         mesh0.notifyVertexPositionsModified();

         mesh1 = MeshFactory.createBox (
           4.0, 4.0, 1.0, Point3d.ZERO, 2, 2, 1,
           /*addNormals=*/false, FaceType.ALT_TRI);

         //mesh1 = MeshFactory.createBox (4.0, 4.0, 1.0);

         setupBodies (myMech, mesh0, mesh1);

         // centered
         myBody1.setPose (new RigidTransform3d (0, 0, 1.5));
         // offset to avoid edges
         //myBody1.setPose (new RigidTransform3d (0.8, -0.8, 1.5));
      }
      
      if (false) {
         mesh0 = MeshFactory.createBox (
            1.0, 1.0, 0.5, Point3d.ZERO, 1, 1, 1, /*addNormals=*/false);
         mesh1 = MeshFactory.createBox (
            2.0, 2.0, 1.0, Point3d.ZERO, 4, 4, 1, /*addNormals=*/false);

         mesh1.getVertex(24).pnt.z = 0;

         // mesh0 = MeshFactory.createRectangle (1.0, 1.0, 1, 1, false);
         // mesh0.transform (new RigidTransform3d (0, 0, 0.25));

         // mesh1 = MeshFactory.createRectangle (2.0, 2.0, 4, 4, false);
         // mesh1.transform (new RigidTransform3d (0, 0, 0.5));
            
         //mesh1.getVertex(12).pnt.z = 0;
         mesh1.notifyVertexPositionsModified();
         mesh1.updateFaceNormals();
         setupBodies (myMech, mesh0, mesh1);
      }

      if (false) {
         mesh0 = MeshFactory.createBox (
            1.0, 1.0, 0.5, Point3d.ZERO, 1, 1, 1, /*addNormals=*/false);

         String simpleTet = new String (
            "v  0.25  0.30 0.5\n" + 
            "v -0.25  0.00 0.5\n" + 
            "v  0.25 -0.30 0.5\n" + 
            "v  0.0   0.0  0.0\n" +
            "f 0 1 2\n" +
            "f 0 3 1\n" +
            "f 1 3 2\n" +
            "f 2 3 0\n");

         mesh1 = 
            WavefrontReader.readFromString (simpleTet, true);
         setupBodies (myMech, mesh0, mesh1);
      }
      

      if (false) {
         mesh0 = MeshFactory.createBox (2, 2, 1.0);

         mesh0.setMeshToWorld (new RigidTransform3d (0.55, -0.30, 0));

         mesh1 = MeshFactory.createSkylineMesh (
            3.25, 2.75, 1.0, 13, 11, 
            "1111111111111",
            "1111111111111",
            "1111111111111",
            "1111111111111",
            "11111   11111",
            "11111 1 11111",
            "11111   11111",
            "1111111111111",
            "1111111111111",
            "1111111111111",
            "1111111111111");
         setupBodies (myMech, mesh0, mesh1);
      }

      if (crashFile != null) {
         mesh0 = new PolygonalMesh();
         mesh1 = new PolygonalMesh();
         readTestFile (crashFile, mesh0, mesh1);
         myMech.clearRigidBodies();
         myMech.clearRenderables();
         setupBodies (myMech, mesh0, mesh1);
      }

      if (perturb0 != 0) {
         myBody0.getMesh().perturb (perturb0);
         System.out.println ("perturb0 " + perturb0);
      }
      if (perturb1 != 0) {
         myBody1.getMesh().perturb (perturb1);
         System.out.println ("perturb1 " + perturb1);
      }

      //myMech.addMeshBody (new FixedMeshBody (mesh0));
      //myBody0 = addBody (myMech, mesh0, mesh0.getMeshToWorld());
      
      myTester = new IntersectionTester (
         myBody0.getMesh(), myBody1.getMesh(), 0);
      
      //myTester.setContoursOnly (true);
      //myTester.setZPerturb0 ( 0, 0.02);
      //tester01.setZPerturb1 (-0.01, 0.02);
      
      // myTester.setRandomTransform (
      //    new IntersectionTester.RandomTransform2d (0.5));
      //addController (tester01);
      //addController (testerx1);

      if (contoursOnly) {
         myTester.setContoursOnly (true);
      }
      if (csg != null) {
         myTester.setCSGOperation (csg);
      }
      RenderProps.setSphericalPoints (myTester, 0.02, Color.CYAN);
      RenderProps.setFaceColor (myTester, new Color (0.8f, 0.8f, 1f));
      addController (myTester);
      addControlPanel ();
      
      setDefaultViewOrientation (new AxisAngle (1, 0, 0, 0));
      RenderProps.setShading (myMech, Shading.NONE);
   }  
      
}    


