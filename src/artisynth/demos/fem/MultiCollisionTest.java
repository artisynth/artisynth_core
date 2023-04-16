package artisynth.demos.fem;

import java.awt.Color;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemFactory.FemElementType;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemSolver;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.renderables.TextComponentBase.HorizontalAlignment;
import artisynth.core.renderables.TextComponentBase.VerticalAlignment;
import artisynth.core.renderables.TextLabeller3d;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.core.util.ArtisynthIO;

public class MultiCollisionTest extends RootModel {
   
   boolean addRbRb = true;
   boolean addRbFem = true;
   boolean addFemRb = true;
   boolean addFemFem = true;
   
   public static boolean defaultBodyFaceContact = false;
   
   int [] femRes = {2,2,2};
   int [] rbRes = {1,1,1};
   double mu = 0.02; // friction
   
   private boolean origBodyFaceContact; // for resetting
   private static PropertyList myProps = new PropertyList(MultiCollisionTest.class, RootModel.class);
   static {
      myProps.add("bodyFaceContact", "do body->Fem face contact", defaultBodyFaceContact);
      myProps.add("friction", "friction", 0, "[0,1]");
   }
   
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   MechModel mech = new MechModel ("mech");

   public void build (String[] args) {
      
      setAdaptiveStepping (false);
      mech.setGravity (0,0,-9.8);
      
      origBodyFaceContact = getBodyFaceContact();
      setBodyFaceContact(true);
      
      double h2 = 1.25;
      double w1 = 0.5;
      double w2 = 1;
      
      double sep = 0.25;
      double x2 = w2/2+sep/2;
      double x1 = x2+w2+sep;
      
      addModel (mech);
      
      // rb,rb
      if (addRbRb) {
         RigidBody rb1 = getRbBox("RB-rb", new Vector3d(w1,w1,w1), 
            new Point3d(-x1,0,h2), rbRes);
         RenderProps.setAlpha (rb1, 0.3);
         mech.addRigidBody (rb1);
         
         RigidBody rb2 = getRbBox("rb-RB", new Vector3d(w2,w2,w2), 
            new Point3d(-x1,0,0), rbRes);
         rb2.setDynamic(false);
         RenderProps.setAlpha (rb2, 0.3);
         mech.addRigidBody (rb2);
         
         mech.setCollisionBehavior(rb1, rb2, true, mu);
      }
      
      // rb, fem
      if (addRbFem) {
         RigidBody rb1 = getRbBox("RB-fem", new Vector3d(w1,w1,w1), 
            new Point3d(-x2,0,h2), rbRes);
         RenderProps.setAlpha (rb1, 0.3);
         mech.addRigidBody (rb1);
         
         FemModel3d fem2 = getFemBox("rb-FEM", new Vector3d(w2,w2,w2), 
            new Point3d(-x2,0,0), femRes);
         RenderProps.setAlpha(fem2, 0.3);
         freezeBase(fem2);
         mech.addModel(fem2);
         
         mech.setCollisionBehavior(rb1, fem2, true, mu);
      }
      
      // fem, rb
      if (addFemRb) {
         FemModel3d fem1 = getFemBox("FEM-rb", new Vector3d(w1,w1,w1), 
            new Point3d(x2,0,h2), femRes);
         RenderProps.setAlpha(fem1, 0.3);
         mech.addModel(fem1);
         
         RigidBody rb2 = getRbBox("fem-RB", new Vector3d(w2,w2,w2), 
            new Point3d(x2,0,0), rbRes);
         rb2.setDynamic(false);
         RenderProps.setAlpha (rb2, 0.3);
         mech.addRigidBody (rb2);
         
         mech.setCollisionBehavior(fem1, rb2, true, mu);
      }
      
      // fem, fem
      if (addFemFem) {
         FemModel3d fem1 = getFemBox("FEM-fem", new Vector3d(w1,w1,w1), 
            new Point3d(x1,0,h2), femRes);
         RenderProps.setAlpha(fem1, 0.3);
         mech.addModel(fem1);
         
         FemModel3d fem2 = getFemBox("fem-FEM", new Vector3d(w2,w2,w2), 
            new Point3d(x1,0,0), femRes);
         RenderProps.setAlpha(fem2, 0.3);
         freezeBase(fem2);
         mech.addModel(fem2);
         
         TextLabeller3d labeller = new TextLabeller3d("vertices");
         for (Vertex3d vtx : fem1.getSurfaceMesh().getVertices()) {
            labeller.addItem("" + vtx.getIndex(), vtx.getPosition(), true);
         }
         labeller.setTextSize(0.1);
         labeller.setTextColor(Color.RED);
         // addRenderable(labeller);
         
         mech.setCollisionBehavior(fem1, fem2, true, mu);
         mech.updateForces(0);
       
      }

      CollisionManager collisions = mech.getCollisionManager();
      RenderProps.setVisible(collisions, true);
      RenderProps.setEdgeWidth(collisions, 2);
      RenderProps.setEdgeColor(collisions, Color.YELLOW);
      RenderProps.setLineWidth(collisions, 3);
      RenderProps.setLineColor(collisions, Color.GREEN);
      collisions.setContactNormalLen(0.1);
      collisions.setDrawContactNormals(true);
      collisions.setDrawIntersectionContours(true);

      for (double t=0; t<10; t+= 0.1) {
         addWayPoint(t);
      }
      addBreakPoint(10);
      
      createControlPanel();

      if (mech.getUseImplicitFriction()) {
         // need compliant contact if implicit friction is set
         mech.setCompliantContact();
      }
   }

   protected RigidBody getRbBox(String name, Vector3d size, Point3d c, int[] rbRes) {

      RigidBody rb = new RigidBody(name);
      PolygonalMesh blockMesh = MeshFactory.createQuadBox (size.x, size.y, size.z,
         Point3d.ZERO, rbRes[0], rbRes[1], rbRes[2]);
      blockMesh.triangulate();
      rb.setMesh (blockMesh, null);
      rb.setDensity (4000);
      rb.setPosition (c);
      
      return rb;
   }
   
   protected FemModel3d getFemBox(String name, Vector3d size, Point3d c, int[] res) {
      
      FemModel3d fem = FemFactory.createGrid (null, FemElementType.Hex, 
         size.x, size.y, size.z, res[0], res[1], res[2]);
      fem.setName (name);
      fem.transformGeometry(new RigidTransform3d(c, AxisAngle.IDENTITY));
      fem.setMaterial (new LinearMaterial(150000,0.49));
      fem.setDensity (4000);
      return fem;
      
   }
   
   protected void freezeBase(FemModel3d fem) {
      
      double minz = Double.POSITIVE_INFINITY;
      for (FemNode3d node : fem.getNodes ()) {
         if (node.getPosition ().z < minz) {
            minz = node.getPosition().z;
         }
      }
      
      for (FemNode3d node : fem.getNodes ()) {
         if (node.getPosition ().z < minz+1e-5) {
            node.setDynamic (false);
         }
      }
   }
   
   public void setBodyFaceContact(boolean set) {
      CollisionManager cm = mech.getCollisionManager();
      cm.setBodyFaceContact (set);
   }
   
   public boolean getBodyFaceContact() {
      CollisionManager cm = mech.getCollisionManager();
      return cm.getBodyFaceContact ();
   }
   
   public void setFriction(double mu) {
      this.mu = mu;
      mech.setFriction (mu);
   }
   
   public double getFriction() {
      return mu;
   }
   
   @Override
   public void attach(DriverInterface driver) {
      super.attach(driver);
      
      //addBreakPoint(1.5);
   }
   
   protected ControlPanel createControlPanel() {
      ControlPanel panel = new ControlPanel("Settings");
      panel.addWidget(this, "bodyFaceContact");
      panel.addWidget(this, "friction");
      panel.pack();
      addControlPanel (panel);
      return panel;
   }
   
   @Override
   public void detach(DriverInterface driver) {
      super.detach(driver);
      
      // reset default contact conditions
      setBodyFaceContact (origBodyFaceContact);
   }
   

}
