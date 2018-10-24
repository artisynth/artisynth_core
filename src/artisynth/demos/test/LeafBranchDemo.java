package artisynth.demos.test;

import java.awt.Color;
import java.util.*;
import java.io.*;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.*;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.RootModel;
import maspack.geometry.Face;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.*;

/**
 * Square patch of triangular shell elements, subjected to gravity. 
 * Some nodes will be held in-place to demonstrate shell bending forces.
 */
public class LeafBranchDemo extends RootModel {
   protected FemModel3d myFem = null;
   protected MechModel myMech = null;

   public double myLen = 10.0;
   public double myWidth = 10.0;
   public int myNumSegs = 8;

   double[] myLeafCoefs = new double[] {0.1, 1.0, -1.8, 0.7};
   
   protected FemNode3d[] myNodes = null;
   
   // Overall density of shell patch
   protected double myDensity = 1.0;

   protected static double myParticleDamping = 0;
   protected static double myStiffnessDamping = 0.05;  
   
   protected static double myThickness = 0.1;
   
   // Affects bending and shear strength
   protected double myNeoHookYoungsModulus = 2000;
   protected double myNeoHookPoissonsRatio = 0.33;
   
   // Rendering radius of nodes
   protected double myNodeRadius = 0.1;
   
   // Dynamic nodes will be given this color.
   protected Color myNodeDynamicColor = Color.CYAN;
   
   // Non-dynamic (i.e. frozen) nodes will be given this color.
   protected Color myNodeNonDynamicColor = Color.RED;
   
   protected Vector3d mGravity = new Vector3d(0, 0, -9.81);

   public static PropertyList myProps =
      new PropertyList (LeafBranchDemo.class, RootModel.class);
   
   static {
      myProps.add(
         "thickness", "Thickness of each shell element", myThickness);
   }
   
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   FemNode3d addNode (
      FemModel3d fem, double x, double y, double z, RigidTransform3d TLW) {
      Point3d pos = new Point3d (x, y, z);
      pos.transform (TLW);
      FemNode3d node = new FemNode3d(pos);
      fem.addNode (node);
      return node;
   }

   ShellTriElement addTri (FemModel3d fem, int i0, int i1, int i2) {
      ShellTriElement elem = new ShellTriElement (
         fem.getNode(i0), fem.getNode(i1), fem.getNode(i2), myThickness);
      fem.addShellElement (elem);
      return elem;
   }

   void addLeaf (
      MechModel mech, FemModel3d fem, EBBeamBody branch,
      double len, double width, RigidTransform3d TLW) {

      double[] c = myLeafCoefs;

      ArrayList<FemNode3d> midNodes = new ArrayList<FemNode3d>();
      for (int i=0; i<=myNumSegs; i++) {
         if (i == 0) {
            addNode (fem, 0, 0, 0, TLW);
            midNodes.add (fem.getNode(fem.numNodes()-1));
         }
         else if (i < myNumSegs) {
            double s = i/(double)myNumSegs;
            double x = len*s;
            double y = width*(((c[3]*s + c[2])*s + c[1])*s + c[0]);
            addNode (fem, x, y, 0, TLW);
            addNode (fem, x, 0, 0, TLW);
            addNode (fem, x, -y, 0, TLW);
            midNodes.add (fem.getNode(fem.numNodes()-2));
         }
         else {
            addNode (fem, len, 0, 0, TLW);
            midNodes.add (fem.getNode(fem.numNodes()-1));
         }
         if (i == 1) {
            int vbase = fem.numNodes()-4;
            addTri (fem, vbase+0, vbase+3, vbase+2);
            addTri (fem, vbase+0, vbase+2, vbase+1);
         }
         else if (i > 1 && i < myNumSegs) {
            int vbase = fem.numNodes()-6;
            addTri (fem, vbase+0, vbase+1, vbase+3);
            addTri (fem, vbase+1, vbase+4, vbase+3);
            addTri (fem, vbase+1, vbase+2, vbase+5);
            addTri (fem, vbase+1, vbase+5, vbase+4);
         }
         else if (i == myNumSegs) {
            int vbase = fem.numNodes()-4;
            addTri (fem, vbase+0, vbase+1, vbase+3);
            addTri (fem, vbase+1, vbase+2, vbase+3);
         }
      }
      
      if (branch != null) {
         // connect middle nodes to branch
         for (FemNode3d n : midNodes) {
            n.initializeDirectorIfNecessary();
            ShellNodeFrameAttachment at =
               new ShellNodeFrameAttachment (n, branch);
            mech.addAttachment (at);
         }
      }
   }

   /**
    * Create an EBBeamBody to simulate a branch or stem.
    */
   EBBeamBody createBranch (
      MechModel mech, double lenx, double rad, RigidTransform3d TBW) {

      PolygonalMesh mesh;
      mesh = MeshFactory.createCylinder (rad, lenx, 20);
      mesh = MeshFactory.createRoundedBox (
            lenx, rad, rad, 20, 4, 4, 20);
      mesh.transform (new RigidTransform3d (lenx/2, 0, 0,  0, Math.PI/2, 0));

      //EBBeamBody body = new EBBeamBody (mesh, /*density=*/1.0, lenx, 10000);
      EBBeamBody body = new EBBeamBody (lenx, rad, /*density=*/1.0, 10000);

      RenderProps.setPointRadius (mech, 0.05);
      RenderProps.setFaceColor (body, new Color (100/256f, 50/256f, 0));
      body.setPose (new RigidTransform3d (lenx/2, 0, 0,  0, 0, 0));

      body.transformGeometry (new RigidTransform3d (-lenx/2, 0, 0));
      body.transformGeometry (TBW);
      mech.addRigidBody (body);
      return body;
   }  

   /**
    * Create a solid joint to connect two branches
    */
   void connectBranches (
      MechModel mech, EBBeamBody b0, EBBeamBody b1, RigidTransform3d TSW) {

      if (b1 == null) {
         mech.addBodyConnector (new SolidJoint (b0, TSW));
      }
      else {
         mech.addBodyConnector (new SolidJoint (b0, b1, TSW));
      }
   }
   
   public void build (String[] args) {

      myMech = new MechModel ("mech");
      addModel (myMech);

      myFem = new FemModel3d ("leaves");
      myMech.addModel (myFem);
      
      boolean useTwoSegs = true;

      // first create branches

      RigidTransform3d TBW = new RigidTransform3d();

      double rad = myLen/50.0;

      TBW.setXyz (-myLen, 0, 0);
      EBBeamBody branch0;      
      if (useTwoSegs) {
         branch0 = createBranch (myMech, myLen, rad, TBW);
         TBW.setXyz (0, 0, 0);
         EBBeamBody branch1 = createBranch (myMech, myLen, rad, TBW);
         connectBranches (myMech, branch0, branch1, TBW);
         addLeaf (myMech, myFem, branch1, myLen, myWidth, TBW);         
      }
      else {
         branch0 = createBranch (myMech, 2*myLen, rad, TBW);
         TBW.setXyz (0, 0, 0);
         addLeaf (myMech, myFem, branch0, myLen, myWidth, TBW);
      }
      TBW.setXyz (-myLen, 0, 0);
      connectBranches (myMech, branch0, null, TBW);

      if (false) {
         TBW.setXyzRpyDeg (-myLen/2, 0, 0,  40.0, 0, 0);
         EBBeamBody branch2 = createBranch (myMech, myLen, rad, TBW);
         connectBranches (myMech, branch0, branch2, TBW);
         addLeaf (myMech, myFem, branch2, myLen, myWidth/2, TBW);
         
         TBW.setXyzRpyDeg (-myLen/2, 0, 0,  -40.0, 0, 0);
         EBBeamBody branch3 = createBranch (myMech, myLen, rad, TBW);
         connectBranches (myMech, branch0, branch3, TBW);
         addLeaf (myMech, myFem, branch3, myLen, myWidth/2, TBW);
      }
      
      double DTOR = Math.PI/180.0;

      ReferenceList fixedNodes = new ReferenceList("fixedNodes");
      
      myFem.setMaterial (
         new NeoHookeanMaterial(myNeoHookYoungsModulus, myNeoHookPoissonsRatio));
      myFem.setStiffnessDamping (myStiffnessDamping);
      //myFem.setGravity (mGravity);
      myFem.setDensity (myDensity);
      myFem.setParticleDamping (myParticleDamping);
      myFem.setDirectorRenderLen (10.0);

      myMech.add (fixedNodes);

      //addModel (myMech);
      
      // Setup rendering options
      myFem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (myFem, new Color (0.2f, 0.6f, 0f));
      RenderProps.setShininess (
         myFem, myFem.getRenderProps().getShininess() * 10);
      RenderProps.setVisible (myFem, true);
      RenderProps.setFaceStyle (myFem, FaceStyle.FRONT_AND_BACK);
      RenderProps.setPointStyle (myFem.getNodes(), 
                                 Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (myFem.getNodes(), myNodeRadius);
      
      // // Add control panel
      // ControlPanel panel = new ControlPanel("shellPatchControlPanel");
      // panel.addWidget (this, "shellThickness", myShellThickness, 10);
      // panel.addWidget (myFem, "particleDamping");
      // panel.addWidget (myFem, "stiffnessDamping", 0, 2000);
      // panel.addWidget (myFem, "density", 10, 1000);
      // panel.addWidget (myFem, "gravity");
      // panel.addWidget (myFem.getMaterial (), "YoungsModulus");
      // panel.addWidget (myFem.getMaterial (), "PoissonsRatio");
      // addControlPanel(panel);

      System.out.println ("Number of elements: " +
                              myFem.numElements());
   }    

   private void initMembersIfNecessary() {
      if (myMech == null) {
         myMech = (MechModel)findComponent ("models/mech");
         myFem = (FemModel3d)myMech.findComponent ("models/leaves");
      }
   }

   public double getThickness() {
      initMembersIfNecessary();
      return myFem.getShellElement(0).getDefaultThickness();
   }
   
   public void setThickness(double newThickness) {
      initMembersIfNecessary();
      for (ShellElement3d e : myFem.getShellElements()) {
         e.setDefaultThickness (newThickness);
      }
   }

}
