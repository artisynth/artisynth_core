package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.HexElement;
import artisynth.core.femmodels.PyramidElement;
import artisynth.core.femmodels.TetElement;
import artisynth.core.femmodels.WedgeElement;
import artisynth.core.femmodels.integration.RegularFemElementSampler;
import artisynth.core.mechmodels.FixedMeshBody;
import artisynth.core.workspace.RootModel;

import maspack.geometry.PointMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;

/**
 * Tests the RegularFemElementSampler for tet, pyramid, wedge and hex elements.
 * 
 * @author Antonio
 */
public class RegularSamplingElementTests extends RootModel {

   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      addFem("tet", createTet(), new Point3d(0,0,1), Color.ORANGE);
      addFem("pyramid", createPyramid(),  new Point3d(0,1,0), Color.MAGENTA);
      addFem("wedge", createWedge(), new Point3d(-1,0,0), Color.GREEN);
      addFem("hex", createHex(), new Point3d(1,0,0), Color.CYAN);

   }
   
   public static FemElement3d createPyramid() {
      FemNode3d[] nodes = new FemNode3d[5];
      for (int i=0; i<5; ++i) {
         //         nodes[0] = new FemNode3d(0.1, -0.3, -0.1);
         //         nodes[1] = new FemNode3d(1.2, 0.1, 0.2);
         //         nodes[2] = new FemNode3d(1.6, 1.6,-0.3);
         //         nodes[3] = new FemNode3d(0.3, 0.8, 0.1);
         //         nodes[4] = new FemNode3d(0.5, 1.2, 1.1);
         nodes[0] = new FemNode3d(-1, -1, -1);
         nodes[1] = new FemNode3d( 1, -1, -1);
         nodes[2] = new FemNode3d( 1,  1, -1);
         nodes[3] = new FemNode3d(-1,  1, -1);
         nodes[4] = new FemNode3d( 0,  0, 1);
      }
      PyramidElement pyr = new PyramidElement(nodes);
      return pyr;
   }
   
   public static FemElement3d createWedge() {
      FemNode3d[] nodes = new FemNode3d[6];
      for (int i=0; i<5; ++i) {
         nodes[0] = new FemNode3d(-1, -1, -1);
         nodes[1] = new FemNode3d( 1, -1, -1);
         nodes[2] = new FemNode3d( 1,  1, -1);
         nodes[3] = new FemNode3d(-1, -1,  1);
         nodes[4] = new FemNode3d( 1, -1,  1);
         nodes[5] = new FemNode3d( 1,  1,  1);
      }
      WedgeElement wedge = new WedgeElement(nodes);
      return wedge;
   }
   
   public static FemElement3d createHex() {
      FemNode3d[] nodes = new FemNode3d[8];
      double w = 2;
      double b = 0.5;
      for (int i=0; i<8; ++i) {
         nodes[0] = new FemNode3d(-w,-w,-w);
         nodes[1] = new FemNode3d(-w, w,-w);
         nodes[2] = new FemNode3d( w, w,-w);
         nodes[3] = new FemNode3d( w,-w,-w);
         nodes[4] = new FemNode3d(-b,-b, b);
         nodes[5] = new FemNode3d(-b, b, b);
         nodes[6] = new FemNode3d( b, b, b);
         nodes[7] = new FemNode3d( b,-b, b);
      }
      HexElement hex = new HexElement(nodes);
      return hex;
   }
   
   public static FemElement3d createTet() {
      FemNode3d[] nodes = new FemNode3d[4];
      double w = 1;
      double b = 1;

      nodes[0] = new FemNode3d(-w,-w,0);
      nodes[1] = new FemNode3d(-w, w,0);
      nodes[2] = new FemNode3d( w, w,0);
      nodes[3] = new FemNode3d( 0, 0, b);

      //      nodes[0] = new FemNode3d(0, 0, 0);
      //      nodes[1] = new FemNode3d(w, 0, 0);
      //      nodes[2] = new FemNode3d(0, w, 0);
      //      nodes[3] = new FemNode3d(0, 0, b);

      
      TetElement tet = new TetElement(nodes);
      return tet;
   }
   
   public void addFem(String name, FemElement3d elem, Point3d origin, Color color) {
      
      RigidTransform3d trans = new RigidTransform3d(origin, AxisAngle.IDENTITY);
      
      FemModel3d fem = new FemModel3d(name);
      for (FemNode3d node : elem.getNodes()) {
         fem.addNode(node);
      }
      fem.addElement(elem);
      addRenderable(fem);
      
      int res = 50;
      
      PointMesh points = new PointMesh();
      RegularFemElementSampler msampler = new RegularFemElementSampler(res);
      msampler.setElement(elem);
      int nsamples = msampler.numUniqueSamples();
      double sum = 0;
      Point3d coord = new Point3d();
      for (int i=0; i<nsamples; ++i) {
         Point3d pos = new Point3d();
         double p = msampler.isample(coord, pos);
         sum += p;
         points.addVertex(pos, true);
      }
      System.out.println("Probability sum: " + sum);
      
      // m.addMesh("samples", points);
      FixedMeshBody fmb = new FixedMeshBody(name + " s points", points);
      RenderProps.setPointColor(fmb, color);
      addRenderable(fmb);
      
      fem.transformPose(trans);
      fmb.transformGeometry(trans);
   }
   
}
