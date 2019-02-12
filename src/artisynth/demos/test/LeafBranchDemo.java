package artisynth.demos.test;

import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.ShellElement3d;
import artisynth.core.femmodels.ShellNodeFrameAttachment;
import artisynth.core.femmodels.ShellTriElement;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.NeoHookeanMaterial;
import artisynth.core.mechmodels.EBBeamBody;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.SolidJoint;
import artisynth.core.workspace.RootModel;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.FaceStyle;

/**
 * Simple demo showing three branches, made of EBBeamBodys, each with an FEM
 * leaf attached. All leaves belong to the same same FEM model.
 */
public class LeafBranchDemo extends RootModel {
   FemModel3d myFem = null; // fem model containing all the leaves
   MechModel myMech = null; // mech model containing everything

   double myLen = 10.0;    // nominal length of each leaf
   double myWidth = 10.0;  // nominal width of each leaf
   int myNumSegs = 8;      // number of element segments in each leaf

   // cubic coefficients used to create leaf outline
   double[] myLeafCoefs = new double[] { 0.1, 1.0, -1.8, 0.7 };

   double myDensity = 1.0; // default density
   static double myParticleDamping = 0;     // FEM particle damping
   static double myStiffnessDamping = 0.05; // FEM stiffness damping
   
   static double DEFAULT_LEAF_THICKNESS = 0.1;

   // Leaf material properties
   static double DEFAULT_LEAF_STIFFNESS = 2000;
   double myLeafStiffness = DEFAULT_LEAF_STIFFNESS;
   static double DEFAULT_BRANCH_STIFFNESS = 400000;
   double myBranchStiffness = DEFAULT_BRANCH_STIFFNESS;

   static double DEFAULT_LEAF_MASS_DAMPING = 0.1;
   double myLeafMassDamping = DEFAULT_LEAF_MASS_DAMPING;
   static double DEFAULT_BRANCH_MASS_DAMPING = 0.1;
   double myBranchMassDamping = DEFAULT_BRANCH_MASS_DAMPING;

   double myPoissonsRatio = 0.33;
   double myNodeRadius = 0.1; // rendering radius for nodes

   public static PropertyList myProps =
      new PropertyList (LeafBranchDemo.class, RootModel.class);

   static {
      myProps.add (
         "leafThickness", "Thickness of the leaves", DEFAULT_LEAF_THICKNESS);
      myProps.add (
         "leafStiffness", "Stiffness of the leaves", DEFAULT_LEAF_STIFFNESS);
      myProps.add (
         "branchStiffness", "Stiffness of the branches",
         DEFAULT_BRANCH_STIFFNESS);
      myProps.add (
         "leafMassDamping", "Mass damping of the leaves",
         DEFAULT_LEAF_MASS_DAMPING);
      myProps.add (
         "branchMassDamping", "Mass damping of the branches",
         DEFAULT_BRANCH_MASS_DAMPING);
   }

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   /**
    * Add a node to the FEM model, at position x, y, z transformed by TLW
    */
   FemNode3d addNode (
      FemModel3d fem, double x, double y, double z, RigidTransform3d TLW) {
      Point3d pos = new Point3d (x, y, z);
      pos.transform (TLW);
      FemNode3d node = new FemNode3d (pos);
      fem.addNode (node);
      return node;
   }

   /**
    * Add a triangle to the FEM model, using nodes indexed by i0, i1, i2
    */
   ShellTriElement addTri (FemModel3d fem, int i0, int i1, int i2) {
      ShellTriElement elem =
         new ShellTriElement (
            fem.getNode (i0), fem.getNode (i1), fem.getNode (i2),
            DEFAULT_LEAF_THICKNESS);
      fem.addShellElement (elem);
      return elem;
   }

   /**
    * Creates the shell elements to form a leaf
    * 
    * @param mech mech model containing everthing
    * @param fem FEM model to which leaf elements and nodes should be added
    * @param branch if non-null, specifies branch to which middle leaf nodes
    * should be connected
    * @param len length of the leaf 
    * @param width width of the leaf
    * @param TLW transform from leaf primary coordinates to the world
    */
   void addLeaf (
      MechModel mech, FemModel3d fem, EBBeamBody branch, double len,
      double width, RigidTransform3d TLW) {

      double[] c = myLeafCoefs;

      ArrayList<FemNode3d> midNodes = new ArrayList<FemNode3d> ();
      for (int i = 0; i <= myNumSegs; i++) {
         if (i == 0) {
            addNode (fem, 0, 0, 0, TLW);
            midNodes.add (fem.getNode (fem.numNodes () - 1));
         }
         else if (i < myNumSegs) {
            double s = i / (double)myNumSegs;
            double x = len * s;
            double y = width * (((c[3] * s + c[2]) * s + c[1]) * s + c[0]);
            addNode (fem, x, y, 0, TLW);
            addNode (fem, x, 0, 0, TLW);
            addNode (fem, x, -y, 0, TLW);
            midNodes.add (fem.getNode (fem.numNodes () - 2));
         }
         else {
            addNode (fem, len, 0, 0, TLW);
            midNodes.add (fem.getNode (fem.numNodes () - 1));
         }
         if (i == 1) {
            int vbase = fem.numNodes () - 4;
            addTri (fem, vbase + 0, vbase + 3, vbase + 2);
            addTri (fem, vbase + 0, vbase + 2, vbase + 1);
         }
         else if (i > 1 && i < myNumSegs) {
            int vbase = fem.numNodes () - 6;
            addTri (fem, vbase + 0, vbase + 1, vbase + 3);
            addTri (fem, vbase + 1, vbase + 4, vbase + 3);
            addTri (fem, vbase + 1, vbase + 2, vbase + 5);
            addTri (fem, vbase + 1, vbase + 5, vbase + 4);
         }
         else if (i == myNumSegs) {
            int vbase = fem.numNodes () - 4;
            addTri (fem, vbase + 0, vbase + 1, vbase + 3);
            addTri (fem, vbase + 1, vbase + 2, vbase + 3);
         }
      }

      if (branch != null) {
         // connect middle nodes to branch
         for (FemNode3d n : midNodes) {
            ShellNodeFrameAttachment at =
               new ShellNodeFrameAttachment (n, branch);
            mech.addAttachment (at);
         }
      }
   }

   /**
    * Create an EBBeamBody to simulate a branch or stem.
    * 
    * @param mech MechModel to add the branch to
    * @param lenx length the branch along the x-axis
    * @param rad radius of the branch
    * @param final pose of the body
    */
   EBBeamBody createBranch (
      MechModel mech, double lenx, double rad, RigidTransform3d TBW) {

      PolygonalMesh mesh;
      mesh = MeshFactory.createCylinder (rad, lenx, 20);
      mesh = MeshFactory.createRoundedBox (lenx, rad, rad, 20, 4, 4, 20);
      mesh.transform (new RigidTransform3d (lenx / 2, 0, 0, 0, Math.PI / 2, 0));

      // EBBeamBody body = new EBBeamBody (mesh, /*density=*/1.0, lenx, 10000);
      EBBeamBody body = 
         new EBBeamBody (lenx, rad, /* density= */1.0, myBranchStiffness);
      body.setMassDamping (getBranchMassDamping());
      
      RenderProps.setPointRadius (mech, 0.05);
      RenderProps.setFaceColor (body, new Color (100 / 256f, 50 / 256f, 0));

      body.setPose (TBW);
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

      // create a fem model to hold the leaf elements
      myFem = new FemModel3d ("leaves");
      myMech.addModel (myFem);
      myFem.setMaterial (
         new NeoHookeanMaterial (
            myLeafStiffness, myPoissonsRatio));
      myFem.setStiffnessDamping (myStiffnessDamping);
      myFem.setDensity (myDensity);
      myFem.setParticleDamping (getLeafMassDamping());

      // create branches and attach leaves to them

      RigidTransform3d TBW = new RigidTransform3d ();

      double rad = myLen/50.0; // branch radius

      // create the main branch in two connected segments.
      // first segment:
      TBW.setXyz (-myLen, 0, 0);
      EBBeamBody branch0 = createBranch (myMech, myLen, rad, TBW);
      // anchor frame of first segment
      TBW.setXyz (-myLen, 0, 0);
      connectBranches (myMech, branch0, null, TBW);
      
      // second segment:
      TBW.setXyz (0, 0, 0);
      EBBeamBody branch1 = createBranch (myMech, myLen, rad, TBW);
      connectBranches (myMech, branch0, branch1, TBW);
      // add leaf to second segment
      addLeaf (myMech, myFem, branch1, myLen, myWidth, TBW);

      // now create two more branch segments, with attached leaves,
      // and connect each to the first segment of the main branch
      TBW.setXyzRpyDeg (-myLen / 2, 0, 0, 40.0, 0, 0);
      EBBeamBody branch2 = createBranch (myMech, myLen, rad, TBW);
      addLeaf (myMech, myFem, branch2, myLen, myWidth / 2, TBW);
      connectBranches (myMech, branch0, branch2, TBW);

      TBW.setXyzRpyDeg (-myLen / 2, 0, 0, -40.0, 0, 0);
      EBBeamBody branch3 = createBranch (myMech, myLen, rad, TBW);
      addLeaf (myMech, myFem, branch3, myLen, myWidth / 2, TBW);
      connectBranches (myMech, branch0, branch3, TBW);

      setRenderProperties (myFem);
      addControlPanel (createControlPanel()); 
   }

   ControlPanel createControlPanel () {
      ControlPanel panel = new ControlPanel("controls");
      panel.addWidget (this, "leafStiffness");
      panel.addWidget (this, "leafMassDamping");
      panel.addWidget (this, "leafThickness", 0.01, 1);
      panel.addWidget (this, "branchStiffness");
      panel.addWidget (this, "branchMassDamping");
      return panel;
   }
   
   void setRenderProperties (FemModel3d fem) {
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      fem.setDirectorRenderLen (10.0);
      RenderProps.setFaceColor (fem, new Color (0.2f, 0.6f, 0f));
      RenderProps
         .setShininess (fem, fem.getRenderProps ().getShininess () * 10);
      RenderProps.setVisible (fem, true);
      RenderProps.setFaceStyle (fem, FaceStyle.FRONT_AND_BACK);
      RenderProps.setPointStyle (fem.getNodes (), Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (fem.getNodes (), myNodeRadius);     
   }
   
   private void initMembersIfNecessary () {
      if (myMech == null) {
         myMech = (MechModel)findComponent ("models/mech");
         myFem = (FemModel3d)myMech.findComponent ("models/leaves");
      }
   }

   public double getLeafThickness () {
      initMembersIfNecessary();
      return myFem.getShellElement (0).getDefaultThickness ();
   }

   public void setLeafThickness (double newThickness) {
      initMembersIfNecessary();
      for (ShellElement3d e : myFem.getShellElements ()) {
         e.setDefaultThickness (newThickness);
      }
   }
   
   public void setLeafStiffness (double E) {
      if (E != myLeafStiffness) {
         initMembersIfNecessary();
         myLeafStiffness = E;
         myFem.setMaterial (
            new NeoHookeanMaterial (myLeafStiffness, myPoissonsRatio));
      }
   }
   
   public double getLeafStiffness () {
      return myLeafStiffness;
   }
   
   public void setBranchStiffness (double E) {
      if (E != myBranchStiffness) {
         initMembersIfNecessary();
         myBranchStiffness = E;
         for (RigidBody body : myMech.rigidBodies()) {
            if (body instanceof EBBeamBody) {
               ((EBBeamBody)body).setStiffness (E);
            }
         }
      }
   }
   
   public double getBranchStiffness() {
      return myBranchStiffness;
   }

   public void setLeafMassDamping (double d) {
      if (d != myLeafMassDamping) {
         initMembersIfNecessary();
         myLeafMassDamping = d;
         myFem.setMassDamping (d);
      }
   }
   
   public double getLeafMassDamping () {
      return myLeafMassDamping;
   }
   
   public void setBranchMassDamping (double d) {
      if (d != myBranchMassDamping) {
         initMembersIfNecessary();
         myBranchMassDamping = d;
         for (RigidBody body : myMech.rigidBodies()) {
            if (body instanceof EBBeamBody) {
               ((EBBeamBody)body).setMassDamping (d);
            }
         }
      }
   }
   
   public double getBranchMassDamping() {
      return myBranchMassDamping;
   }

}
