package artisynth.demos.fem;

import java.awt.Color;
import java.util.*;
import java.io.*;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.NeoHookeanMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.MechSystemBase;
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
public class LeafDemo extends RootModel {
   protected FemModel3d myFem = null;
   protected MechModel myMech = null;

   // Width and length of square patch of shell elements
   public int myMeshX = 13;                
   public int myMeshY = 13;

   public double mySizeX = 10.0;
   public double mySizeY = 10.0;
   public int myNumSegs = 8;
   
   // Number of shell elements per row
   public int myMeshXDiv = 13;
   
   // Number of shell elements per column
   public int myMeshYDiv = 13;
   
   // Not part of shell elements but used to generate a square lattice of node
   // positions.
   protected PolygonalMesh myMesh = null;
   
   protected FemNode3d[] myNodes = null;
   
   // Overall density of shell patch
   protected double myDensity = 100;
   
   // Generic particle velocity damping
   protected static double myParticleDamping = 0;
   
   // Element stiffness. 0 for water-like, 100 for aluminium-like
   protected static double myStiffnessDamping = 0.05;              
   
   // Affects bending strength
   protected static double myShellThickness = 0.1;
   
   // Affects bending and shear strength
   protected double myNeoHookYoungsModulus = 2000000;
   protected double myNeoHookPoissonsRatio = 0.33;
   
   // Rendering radius of nodes
   protected double myNodeRadius = 0.1;
   
   // Dynamic nodes will be given this color.
   protected Color myNodeDynamicColor = Color.CYAN;
   
   // Non-dynamic (i.e. frozen) nodes will be given this color.
   protected Color myNodeNonDynamicColor = Color.RED;
   
   protected Vector3d mGravity = new Vector3d(0, 0, -9.81);

   PolygonalMesh createLeafMesh (
      double[] coefs, double sizex, double sizey, int nsegs) {
      PolygonalMesh mesh = new PolygonalMesh();
      Point3d p = new Point3d();
      for (int i=0; i<nsegs; i++) {
         double s = i/(double)nsegs;
         double x = sizex*s;
         double y = sizey*(((coefs[3]*s + coefs[2])*s + coefs[1])*s + coefs[0]);
         System.out.printf ("%g %g\n", x, y);
         mesh.addVertex (new Vertex3d (new Point3d (x, y, 0)));
         mesh.addVertex (new Vertex3d (new Point3d (x, 0, 0)));
         mesh.addVertex (new Vertex3d (new Point3d (x, -y, 0)));
         if (i > 0) {
            int vbase = mesh.numVertices()-6;
            mesh.addFace (new int[] {vbase+0, vbase+1, vbase+3});
            mesh.addFace (new int[] {vbase+1, vbase+4, vbase+3});
            mesh.addFace (new int[] {vbase+1, vbase+2, vbase+5});
            mesh.addFace (new int[] {vbase+1, vbase+5, vbase+4});
         }
      }
      mesh.addVertex (sizex, 0, 0);
      int vbase = mesh.numVertices()-4;
      mesh.addFace (new int[] {vbase+0, vbase+1, vbase+3});
      mesh.addFace (new int[] {vbase+1, vbase+2, vbase+3});
      return mesh;      
   }
   
   public void build (String[] args) {
      myMech = new MechModel ("mech");
      myFem = new FemModel3d();

      // Create a square lattice of node positions, represented as mesh 
      // vertices.
      myMesh = createLeafMesh (
         new double[] {0.1, 1.0, -1.8, 0.7}, mySizeX, mySizeY, myNumSegs);
      
      myNodes = new FemNode3d[myMesh.numVertices()];
      double DTOR = Math.PI/180.0;

      ArrayList<RigidTransform3d> leafTrans = new ArrayList<RigidTransform3d>();
      double the0 = 0;
      double z = 0;
      for (int i=0; i<3; i++) {
         for (int j=0; j<3; j++) {
            leafTrans.add (
               new RigidTransform3d (
                  0, 0, z, DTOR*j*120+the0, -DTOR*20*(i+1), 0));
         }
         the0 += 60;
         z += 0.5;
      }

      ReferenceList<FemNode3d> fixedNodes = new ReferenceList<>("fixedNodes");
      
      // for each leaf transform ...
      int nodeBaseIdx = 0;      
      for (RigidTransform3d TLW : leafTrans) {
         PolygonalMesh leafMesh = myMesh.copy();
         leafMesh.transform (TLW);
         // For each 3-vertex mesh face...
         for (int i=0; i<leafMesh.numVertices(); i++) {
            Vertex3d vtx = leafMesh.getVertex (i);
            Vertex3d origVtx = myMesh.getVertex (i);

            FemNode3d node = new FemNode3d(vtx.getPosition());
            myFem.addNode (node);
            if (shouldBeFrozen (origVtx.getPosition(), mySizeX, 2)) {
               node.setDynamic (false);
               RenderProps.setPointColor (node, new Color (0, 0.5f, 0));
               fixedNodes.addReference (node);
            }
         }
         ComponentList<FemNode3d> nodes = myFem.getNodes();
         for (Face face : leafMesh.getFaces()) {
            // Create a fem node for each corresponding mesh vertex of the mesh
            // face...
            Vertex3d[] triVtx = face.getTriVertices();

            FemNode3d n0 = nodes.getByNumber(nodeBaseIdx+triVtx[0].getIndex());
            FemNode3d n1 = nodes.getByNumber(nodeBaseIdx+triVtx[1].getIndex());
            FemNode3d n2 = nodes.getByNumber(nodeBaseIdx+triVtx[2].getIndex());

            // Create a shell fem element for these 3 fem nodes
            ShellTriElement ele =
               new ShellTriElement(n0, n1, n2, myShellThickness);
            myFem.addShellElement(ele);
         }
         nodeBaseIdx += myMesh.numVertices();
      }

      myFem.setMaterial (
         new NeoHookeanMaterial(myNeoHookYoungsModulus, myNeoHookPoissonsRatio));
      myFem.setStiffnessDamping (myStiffnessDamping);
      myFem.setGravity (mGravity);
      myFem.setDensity (myDensity);
      myFem.setParticleDamping (myParticleDamping);

      myMech.addModel (myFem);
      myMech.add (fixedNodes);

      addModel (myMech);
      
      // for (FemNode3d node : myNodes) {
      //    node.setRenderProps( node.createRenderProps() );
      //    if (shouldBeFrozen(node, mySizeX, 2)) {
      //       node.getRenderProps ().setPointColor (myNodeNonDynamicColor);
      //       node.setDynamic (false);
      //    }
      // }
      
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
      
      // Add control panel
      ControlPanel panel = new ControlPanel("shellPatchControlPanel");
      panel.addWidget (this, "shellThickness", myShellThickness, 10);
      panel.addWidget (myFem, "particleDamping");
      panel.addWidget (myFem, "stiffnessDamping", 0, 2000);
      panel.addWidget (myFem, "density", 10, 1000);
      panel.addWidget (myFem, "gravity");
      panel.addWidget (myFem.getMaterial (), "YoungsModulus");
      panel.addWidget (myFem.getMaterial (), "PoissonsRatio");
      addControlPanel(panel);

      System.out.println ("Number of elements: " +
                              myFem.numElements());
   }    
   
   public static PropertyList myProps =
      new PropertyList (LeafDemo.class, RootModel.class);
   
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
      return fem != null ? fem.getShellElement(0).getDefaultThickness() : 0;
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

   /**
    * Should this node be non-dynamic?
    */
   public boolean shouldBeFrozen (Point3d pnt, double xsize, int nsegs) {
      double eps = 1e-8;
      if (Math.abs(pnt.x) < eps) {
         return true;
      }
      else if (Math.abs(pnt.y) < eps && (pnt.x-nsegs) < eps) {
         return true;
      }
      else {
         return false;
      }
   }
}
