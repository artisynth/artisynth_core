package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.EmbeddedFem;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.HexElement;
import artisynth.core.femmodels.HexTrimmer;
import artisynth.core.renderables.TextLabeller3d;
import artisynth.core.workspace.RootModel;
import maspack.geometry.DistanceGrid;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.AxisAngle;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.render.RenderProps;
import maspack.render.Renderer.PointStyle;

/**
 * Tests the HexTrimmer for embedded FEM models.
 * 
 * @author Antonio
 */
public class HexTrimTest extends RootModel {
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      // addWedgeTest();
      // addPyramidTetTest();
      // addTetTest(new int[]{0}, 0, 24);
      addTest(new int[]{0}, 0, 24);
   }
   
   void addTest(int[] nodes, int pstart, int nperms) {
      int i=0;
      int[][] permutations = HexTrimmer.HEX_PERMUTATIONS;
      
      for (i=pstart; i<pstart+nperms; ++i) {
         PolygonalMesh box = createNodeBlocks(nodes);
         RenderProps.setAlpha(box, 0.9);
        
         box.setName("box_" + i);
         box.translate(new Vector3d((i-pstart)*0.3, 0, 0));
         DistanceGrid sdg = box.getSignedDistanceGrid(0.1, new Vector3i(30,30,30));
         
         FemModel3d hex = createHex(permutations[i]);
         hex.transformGeometry(new RigidTransform3d(new Vector3d((i-pstart)*0.3,0,0), AxisAngle.IDENTITY));
         hex.setName("hex_" + i);
         RenderProps.setPointStyle(hex, PointStyle.SPHERE);
         RenderProps.setPointRadius(hex, 0.003);
         EmbeddedFem.trimBoundaryHexes(hex, sdg, 0);
         
         TextLabeller3d labeller = new TextLabeller3d("nodes_" + i);
         labeller.setTextColor(Color.WHITE);
         labeller.setTextSize(0.02);
         for (FemNode3d node : hex.getNodes()) {
            labeller.addItem(Integer.toString(node.getNumber()), node.getPosition(), true);
         }
         
         addRenderable(box);
         addRenderable(hex);
         addRenderable(labeller);
      }
   }
   
   void addPyramidPyramidTest() {
      int i=0;
      int[][] permutations = HexTrimmer.HEX_PERMUTATIONS;
      
      for (i=0; i<permutations.length; ++i) {
         // skip 3,6
         PolygonalMesh box = createNodeBlocks(new int[]{0, 2, 3, 4, 5, 6});
         RenderProps.setAlpha(box, 0.9);
         
         box.setName("box_" + i);
         box.translate(new Vector3d(i*0.3, 0, 0));
         DistanceGrid sdg = box.getSignedDistanceGrid(0.1, new Vector3i(30,30,30));
         
         FemModel3d hex = createHex(permutations[i]);
         hex.transformGeometry(new RigidTransform3d(new Vector3d(i*0.3,0,0), AxisAngle.IDENTITY));
         hex.setName("hex_" + i);
         RenderProps.setPointStyle(hex, PointStyle.SPHERE);
         RenderProps.setPointRadius(hex, 0.003);
         EmbeddedFem.trimBoundaryHexes(hex, sdg, 0);
         
         TextLabeller3d labeller = new TextLabeller3d("nodes_" + i);
         labeller.setTextColor(Color.WHITE);
         labeller.setTextSize(0.02);
         for (FemNode3d node :hex.getNodes()) {
            labeller.addItem(Integer.toString(node.getNumber()), node.getPosition(), true);
         }
         
         addRenderable(box);
         addRenderable(hex);
         addRenderable(labeller);
      }
   }
   
   void addPyramidTetTest() {
      int i=0;
      int[][] permutations = HexTrimmer.HEX_PERMUTATIONS;
      
      for (i=0; i<permutations.length; ++i) {
         // skip 3,6
         PolygonalMesh box = createNodeBlocks(new int[]{0, 1, 2, 3, 5, 7});
         RenderProps.setAlpha(box, 0.9);
         
         box.setName("box_" + i);
         box.translate(new Vector3d(i*0.3, 0, 0));
         DistanceGrid sdg = box.getSignedDistanceGrid(0.1, new Vector3i(30,30,30));
         
         FemModel3d hex = createHex(permutations[i]);
         hex.transformGeometry(new RigidTransform3d(new Vector3d(i*0.3,0,0), AxisAngle.IDENTITY));
         hex.setName("hex_" + i);
         RenderProps.setPointStyle(hex, PointStyle.SPHERE);
         RenderProps.setPointRadius(hex, 0.003);
         EmbeddedFem.trimBoundaryHexes(hex, sdg, 0);
         
         TextLabeller3d labeller = new TextLabeller3d("nodes_" + i);
         labeller.setTextColor(Color.WHITE);
         labeller.setTextSize(0.02);
         for (FemNode3d node :hex.getNodes()) {
            labeller.addItem(Integer.toString(node.getNumber()), node.getPosition(), true);
         }
         
         addRenderable(box);
         addRenderable(hex);
         addRenderable(labeller);
      }
   }
   
   void addWedgeTest() {
      
      int[][] permutations = HexTrimmer.HEX_PERMUTATIONS;
      for (int i=0; i<permutations.length; ++i) {
         PolygonalMesh box = MeshFactory.createBox(0.2, 0.2, 0.2);
         box.setName("box_" + i);
         box.translate(new Vector3d(i*0.3, 0, 0));
         box.translate(new Vector3d(0, 0.15, 0.15));
         DistanceGrid sdg = box.getSignedDistanceGrid(0.1, new Vector3i(30,30,30));
         
         // wedge
         FemModel3d hex = createHex(permutations[i]);
         hex.transformGeometry(new RigidTransform3d(new Vector3d(i*0.3,0,0), AxisAngle.IDENTITY));
         hex.setName("hex_" + i);
         RenderProps.setPointStyle(hex, PointStyle.SPHERE);
         RenderProps.setPointRadius(hex, 0.003);
         EmbeddedFem.trimBoundaryHexes(hex, sdg, 0);
         
         
         TextLabeller3d labeller = new TextLabeller3d("nodes_" + i);
         labeller.setTextColor(Color.WHITE);
         labeller.setTextSize(0.02);
         for (FemNode3d node :hex.getNodes()) {
            labeller.addItem(Integer.toString(node.getNumber()), node.getPosition(), true);
         }
         
         addRenderable(box);
         addRenderable(hex);
         addRenderable(labeller);
      }
   }
   
   PolygonalMesh createNodeBlocks(int[] nodeIdxs) {
      
      double a = 0.1/Math.sqrt(3);
      double[][] offsets = {
                            {-a, -a, -a},
                            { a, -a, -a},
                            { a,  a, -a},
                            {-a,  a, -a},
                            {-a, -a,  a},
                            { a, -a,  a},
                            { a,  a,  a},
                            {-a,  a,  a},

      };
      
      PolygonalMesh nodeBox = new PolygonalMesh();
      for (int i : nodeIdxs) {
         PolygonalMesh box0 = MeshFactory.createBox(0.02, 0.02, 0.02);
         box0.translate(new Vector3d(offsets[i]));
         nodeBox.addMesh(box0);
         
      }
      return nodeBox;
   }
   
   FemModel3d createHex(int[] permutation) {
      FemNode3d[] nodes = new FemNode3d[8];
      double a = 0.1;
      nodes[0] = new FemNode3d(-a, -a, -a);
      nodes[1] = new FemNode3d( a, -a, -a);
      nodes[2] = new FemNode3d( a,  a, -a);
      nodes[3] = new FemNode3d(-a,  a, -a);
      nodes[4] = new FemNode3d(-a, -a,  a);
      nodes[5] = new FemNode3d( a, -a,  a);
      nodes[6] = new FemNode3d( a,  a,  a);
      nodes[7] = new FemNode3d(-a,  a,  a);
      
      // re-arrange hex
      HexElement hex = new HexElement(
         nodes[permutation[4]], nodes[permutation[5]], nodes[permutation[6]], nodes[permutation[7]], 
         nodes[permutation[0]], nodes[permutation[1]], nodes[permutation[2]], nodes[permutation[3]]);
      
      FemModel3d fem = new FemModel3d("hex");
      for (FemNode3d node : nodes) {
         fem.addNode(node);
      }
      fem.addElement(hex);
      boolean inverted = hex.isInvertedAtRest();
      if (inverted) {
         System.out.println("inverted at rest");
      }
      
      return fem;
   }

}
