package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.NeoHookeanMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.RootModel;
import maspack.geometry.Face;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.widgets.DoubleFieldSlider;
import maspack.widgets.LabeledComponentBase;

/**
 * Square patch of triangular shell elements, subjected to gravity. 
 * Some nodes will be held in-place to demonstrate shell bending forces.
 */
public class ShellTriPatch extends RootModel {
   
   protected static final double EPS = 1e-15;

   protected FemModel3d myFem = null;
   protected FemNode3d[] myNodes = null;
   protected MechModel myMech = null;
   protected PolygonalMesh myMesh = null;

   // Width and length of square patch of shell elements
   public int myMeshX = 13;                
   public int myMeshY = 13;
   
   // Number of shell elements per row
   public int myMeshXDiv = 13;
   
   // Number of shell elements per column
   public int myMeshYDiv = 13;
   
   // Overall density of shell patch
   protected double myDensity = 100;
   
   // Generic particle velocity damping
   protected static double myParticleDamping = 1.0;
   
   // Element stiffness. 0 for water-like, 100 for aluminium-like
   protected static double myStiffnessDamping = 0.05;              
   
   // Affects bending strength
   protected static double myShellThickness = 0.1;
   
   // Affects bending and shear strength
   protected double myYoungsModulus = 500000;
   protected double myPoissonsRatio = 0.33;
   
   // Rendering radius of nodes
   protected double myNodeRadius = 0.1;
   
   // Dynamic nodes will be given this color.
   protected Color myNodeDynamicColor = Color.GREEN;
   
   // Non-dynamic (i.e. frozen) nodes will be given this color.
   protected Color myNodeNonDynamicColor = Color.GRAY;
   
   protected Vector3d mGravity = new Vector3d(0, 0, -9.81);


   public void build (String[] args) {

      boolean membrane = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-membrane")) {
            membrane = true;
         }
         else {
            System.out.println ("Warning: unknown model argument '"+args[i]+"'");
         }
      }

      build_pre();
      build_modelStructure(membrane);
      build_modelProperties();
      build_femRendering();
      build_meshRendering();
      build_UI();
      build_post();
   }    
   
   protected void build_pre() {
      
   }
   
   protected void build_modelStructure(boolean membrane) {
      myMech = new MechModel ("mech");
      myFem = new FemModel3d();

      // Create a square lattice of node positions, represented as mesh 
      // vertices.
      myMesh = MeshFactory.createPlane(myMeshX, myMeshY, myMeshXDiv, myMeshYDiv);
      myNodes = new FemNode3d[myMesh.numVertices()];
      
      // For each 3-vertex mesh face...
      for (Face face : myMesh.getFaces()) {
         // Create a fem node for each corresponding mesh vertex of the mesh
         // face...
         Vertex3d[] triVtx = face.getTriVertices();
         for (Vertex3d vertex : triVtx) {
            int v = vertex.getIndex();
            if (myNodes[v] == null) {
               myNodes[v] = new FemNode3d(vertex.getPosition());
               myFem.addNumberedNode(myNodes[v], v);
            }
         }
         
         FemNode3d n0 = myNodes[ triVtx[0].getIndex() ];
         FemNode3d n1 = myNodes[ triVtx[1].getIndex() ];
         FemNode3d n2 = myNodes[ triVtx[2].getIndex() ];

         // Create a shell fem element for these 3 fem nodes
         ShellTriElement ele =
            new ShellTriElement(n0, n1, n2, myShellThickness, membrane);
         ele.setIndex (face.idx);
         myFem.addNumberedShellElement(ele, face.idx);
      }
      
      myMech.addModel (myFem);
      addModel (myMech);
   }
   
   protected void build_modelProperties() {
      myFem.setMaterial (
         new LinearMaterial(myYoungsModulus, myPoissonsRatio));
      myFem.setStiffnessDamping (myStiffnessDamping);
      myFem.setGravity (mGravity);
      myFem.setDensity (myDensity);
      myFem.setParticleDamping (myParticleDamping);

      // Hold some nodes in-place
      for (FemNode3d node : myNodes) {
         node.setRenderProps( node.createRenderProps() );
         if (shouldBeFrozen(node)) {
            node.getRenderProps ().setPointColor (myNodeNonDynamicColor);
            node.setDynamic (false);
         }
      }
   }
   
   protected void build_femRendering() {
      // Setup rendering options
      myFem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (myFem, new Color (0.7f, 0.7f, 0.9f));
      RenderProps.setPointColor (myFem, Color.GREEN);
      RenderProps.setShininess (
         myFem, myFem.getRenderProps().getShininess() * 10);
      RenderProps.setVisible (myFem, true);
      RenderProps.setFaceStyle (myFem, Renderer.FaceStyle.FRONT);
      RenderProps.setPointStyle (myFem.getNodes(), 
                                 Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (myFem.getNodes(), myNodeRadius);
   }
   
   protected void build_meshRendering() {
      
   }
   
   protected void build_UI() {
      // Add control panel
      ControlPanel panel = new ControlPanel("shellPatchControlPanel");
      panel.addWidget (this, "shellThickness", myShellThickness, 10);
      panel.addWidget (myFem, "particleDamping");
      panel.addWidget (myFem, "stiffnessDamping", 0, 2000);
      panel.addWidget (myFem, "density", 10, 1000);
      panel.addWidget (myFem, "gravity");
      panel.addWidget (myFem, "directorRenderLen");
      panel.addWidget (myFem, "material");
      LabeledComponentBase comp = panel.addWidget (myFem, "material.YoungsModulus");
      ((DoubleFieldSlider)comp).setSliderRange (1, 100000);
      //panel.addWidget (m_femShellModel.getMaterial(), "YoungsModulus");
      //panel.addWidget (m_femShellModel.getMaterial(), "PoissonsRatio");
      addControlPanel(panel);
      myFem.setDirectorRenderLen (5);
   }
   
   protected void build_post() {
   }
   
   /* --- Properties --- */
   
   public static PropertyList myProps =
      new PropertyList (ShellTriPatch.class, RootModel.class);
   
   static {
      myProps.add("shellThickness", "Thickness of each shell element", 
                  myShellThickness);
   }
   
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public double getShellThickness() {
      FemModel3d fem = findFem();
      if (fem != null) {
         return fem.getShellElement(0).getDefaultThickness();
      }
      else {
         return 0;
      }
   }
   
   public void setShellThickness(double newThickness) {
      FemModel3d fem = findFem();
      if (fem != null) {
         for (ShellElement3d e : fem.getShellElements()) {
            e.setDefaultThickness (newThickness);
         }
      }
   }

   protected FemModel3d findFem() {
      ModelComponent comp = findComponent ("models/mech/models/0");
      if (comp instanceof FemModel3d) {
         return (FemModel3d)comp;
      }
      else {
         return null;
      }
   }
   
   /* --- Miscellaneous --- */
   
   /**
    * Should this node be non-dynamic?
    */
   public boolean shouldBeFrozen(FemNode3d node) {
      return (node.getPosition().x <= -myMeshX/2 + EPS);
   }
}
