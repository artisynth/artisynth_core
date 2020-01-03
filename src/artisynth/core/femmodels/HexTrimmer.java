package artisynth.core.femmodels;

import java.util.List;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.HexElement;
import artisynth.core.femmodels.IntegrationPoint3d;
import artisynth.core.femmodels.PyramidElement;
import artisynth.core.femmodels.TetElement;
import artisynth.core.femmodels.WedgeElement;
import artisynth.core.femmodels.integration.RegularFemElementSampler;
import maspack.geometry.DistanceGrid;
import maspack.matrix.Point3d;

/**
 * "Trims" hex elements by decomposing into other elements and removing parts that do not contain volume
 */
public class HexTrimmer {

   public static boolean checkNodes = false;
   
   // all 24 permutations of a valid hex
   public static final int[][] HEX_PERMUTATIONS = {
                                                   // bottom
                                                   {0, 1, 2, 3, 4, 5, 6, 7},
                                                   {1, 2, 3, 0, 5, 6, 7, 4},
                                                   {2, 3, 0, 1, 6, 7, 4, 5},
                                                   {3, 0, 1, 2, 7, 4, 5, 6},
                                                   // front
                                                   {4, 5, 1, 0, 7, 6, 2, 3},
                                                   {5, 1, 0, 4, 6, 2, 3, 7},
                                                   {1, 0, 4, 5, 2, 3, 7, 6},
                                                   {0, 4, 5, 1, 3, 7, 6, 2},
                                                   // right
                                                   {5, 6, 2, 1, 4, 7, 3, 0},
                                                   {6, 2, 1, 5, 7, 3, 0, 4},
                                                   {2, 1, 5, 6, 3, 0, 4, 7},
                                                   {1, 5, 6, 2, 0, 4, 7, 3},
                                                   // top
                                                   {7, 6, 5, 4, 3, 2, 1, 0},
                                                   {4, 7, 6, 5, 0, 3, 2, 1},
                                                   {5, 4, 7, 6, 1, 0, 3, 2},
                                                   {6, 5, 4, 7, 2, 1, 0, 3},
                                                   // back
                                                   {3, 2, 6, 7, 0, 1, 5, 4},
                                                   {7, 3, 2, 6, 4, 0, 1, 5},
                                                   {6, 7, 3, 2, 5, 4, 0, 1},
                                                   {2, 6, 7, 3, 1, 5, 4, 0},
                                                   // left
                                                   {0, 3, 7, 4, 1, 2, 6, 5},
                                                   {4, 0, 3, 7, 5, 1, 2, 6},
                                                   {7, 4, 0, 3, 6, 5, 1, 2},
                                                   {3, 7, 4, 0, 2, 6, 5, 1},
   };

   static final Pattern[] PATTERNS = {
                                      new Pattern0(),
                                      new Pattern5Base(),
                                      new Pattern5Mid(),
                                      new Pattern6Wedge(),
                                      new Pattern6PyramidPyramid(),
                                      new Pattern6PyramidTetTet()
   };

   public static int SAMPLE_POINTS = 10000;
   public static int SAMPLE_RES = 50;
   
   private static interface Pattern {
      public int numNodes();
      public boolean accept(HexElement hex, FemNode3d[] nodes, IntegrationPoint3d[] ipnts,
         int[] nodePermutation, double margin, DistanceGrid dg,
         List<FemElement3d> out); 
   }


   //   XXX Incompatible faces
   //   /**
   //    * Remove one node, forming a pyramid and wedge
   //    */
   //   private static class Pattern7 implements Pattern {
   //
   //      @Override
   //      public int numNodes() {
   //         return 7;
   //      }
   //
   //      @Override
   //      public boolean accept(
   //         HexElement hex, int[] nodePermutation,
   //         double margin, DistanceGrid dg, List<FemElement3d> out) {
   //         
   //         // use ipnts to check
   //         IntegrationPoint3d[] ipnts = hex.getIntegrationPoints();
   //         Point3d pos = new Point3d();
   //         
   //         // remove node 7
   //         ipnts[nodePermutation[7]].computePosition(pos, hex);
   //         
   //         if (dg.getDistance(pos) <= margin) {
   //            // invalid
   //            return false;
   //         }  else if (dg.getDistance(nodes[nodePermutation[7]].getPosition()) <= margin) {
   //                   return false;
   //           }
   //         
   //         // create wedge and pyramid
   //         FemNode3d[] nodes = hex.getNodes();
   //         out.add(new WedgeElement(
   //            nodes[nodePermutation[0]], nodes[nodePermutation[1]], nodes[nodePermutation[5]],
   //            nodes[nodePermutation[3]], nodes[nodePermutation[2]], nodes[nodePermutation[6]]));
   //         
   //         out.add(new PyramidElement( 
   //            nodes[nodePermutation[0]], nodes[nodePermutation[3]], nodes[nodePermutation[6]],
   //            nodes[nodePermutation[5]], nodes[nodePermutation[4]]));
   //         return true;
   //      }
   //   }

   private static class Pattern6Wedge implements Pattern {

      @Override
      public int numNodes() {
         return 6;
      }

      @Override
      public boolean accept(
         HexElement hex, FemNode3d[] nodes, IntegrationPoint3d[] ipnts, int[] nodePermutation, double margin,
         DistanceGrid dg, List<FemElement3d> out) {

         if (checkNodes) {
            if (dg.getLocalDistance(nodes[nodePermutation[7]].getPosition()) <= margin) {
               return false;
            }
            if (dg.getLocalDistance(nodes[nodePermutation[6]].getPosition()) <= margin) {
               return false;
            }
         }
         
         // use ipnts to check
         Point3d pos = new Point3d();

         // remove node 7
         ipnts[nodePermutation[7]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // remove node 6
         ipnts[nodePermutation[6]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // sample from elements to remove
         WedgeElement remove = new WedgeElement(nodes[nodePermutation[2]], nodes[nodePermutation[5]], nodes[nodePermutation[6]],
            nodes[nodePermutation[3]], nodes[nodePermutation[4]], nodes[nodePermutation[7]]);
         
         RegularFemElementSampler sampler = new RegularFemElementSampler(SAMPLE_RES);
         sampler.setElement(remove);
         int samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }

         // add wedge
         out.add(new WedgeElement(
            nodes[nodePermutation[1]], nodes[nodePermutation[5]], nodes[nodePermutation[2]],
            nodes[nodePermutation[0]], nodes[nodePermutation[4]], nodes[nodePermutation[3]]));

         return true;
      }
   }

   /**
    * Remove two nodes across a face diagonal
    */
   private static class Pattern6PyramidTetTet implements Pattern {

      @Override
      public int numNodes() {
         return 6;
      }

      @Override
      public boolean accept(
         HexElement hex, FemNode3d[] nodes, IntegrationPoint3d[] ipnts, int[] nodePermutation, double margin,
         DistanceGrid dg, List<FemElement3d> out) {

         if (checkNodes) {
            if (dg.getLocalDistance(nodes[nodePermutation[6]].getPosition()) <= margin) {
               return false;
            }
            if (dg.getLocalDistance(nodes[nodePermutation[4]].getPosition()) <= margin) {
               return false;
            }
         }
         
         // use ipnts to check
         Point3d pos = new Point3d();

         // remove node 6
         ipnts[nodePermutation[6]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // remove node 4
         ipnts[nodePermutation[4]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         } 

         // sample from elements to remove
         TetElement tet1 = new TetElement(nodes[nodePermutation[0]], nodes[nodePermutation[5]], nodes[nodePermutation[7]],
            nodes[nodePermutation[4]]);
         TetElement tet2 = new TetElement(nodes[nodePermutation[2]], nodes[nodePermutation[7]], nodes[nodePermutation[5]],
            nodes[nodePermutation[6]]);
         
         RegularFemElementSampler sampler = new RegularFemElementSampler(SAMPLE_RES);
         sampler.setElement(tet1);
         int samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }
         sampler.setElement(tet2);
         samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }

         out.add(new PyramidElement(
            nodes[nodePermutation[0]], nodes[nodePermutation[1]], nodes[nodePermutation[2]],
            nodes[nodePermutation[3]], nodes[nodePermutation[5]]));
         out.add(new TetElement (
            nodes[nodePermutation[0]], nodes[nodePermutation[3]], nodes[nodePermutation[7]],
            nodes[nodePermutation[5]]
            ));
         out.add(new TetElement (
            nodes[nodePermutation[2]], nodes[nodePermutation[7]], nodes[nodePermutation[3]],
            nodes[nodePermutation[5]]
            ));

         return true;
      }
   }

   /**
    * Remove two nodes across long diagonal
    */
   private static class Pattern6PyramidPyramid implements Pattern {

      @Override
      public int numNodes() {
         return 6;
      }

      @Override
      public boolean accept(
         HexElement hex, FemNode3d[] nodes, IntegrationPoint3d[] ipnts, int[] nodePermutation, double margin,
         DistanceGrid dg, List<FemElement3d> out) {

         if (checkNodes) {
            if (dg.getLocalDistance(nodes[nodePermutation[7]].getPosition()) <= margin) {
               return false;
            }
            if (dg.getLocalDistance(nodes[nodePermutation[1]].getPosition()) <= margin) {
               return false;
            }
         }
         
         // use ipnts to check
         Point3d pos = new Point3d();

         // remove node 7
         ipnts[nodePermutation[7]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // remove node 1
         ipnts[nodePermutation[1]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // sample from elements to remove
         TetElement tet1 = new TetElement(nodes[nodePermutation[3]], nodes[nodePermutation[4]], nodes[nodePermutation[6]],
            nodes[nodePermutation[7]]);
         TetElement tet2 = new TetElement(nodes[nodePermutation[0]], nodes[nodePermutation[2]], nodes[nodePermutation[5]],
            nodes[nodePermutation[1]]);
         
         // LagrangianFemElementSampler sampler = new LagrangianFemElementSampler();
         RegularFemElementSampler sampler = new RegularFemElementSampler(SAMPLE_RES);
         sampler.setElement(tet1);
         int samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }
         sampler.setElement(tet2);
         samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }

         out.add(new PyramidElement(
            nodes[nodePermutation[0]], nodes[nodePermutation[2]], nodes[nodePermutation[6]],
            nodes[nodePermutation[4]], nodes[nodePermutation[5]]));
         out.add(new PyramidElement (
            nodes[nodePermutation[0]], nodes[nodePermutation[4]], nodes[nodePermutation[6]],
            nodes[nodePermutation[2]], nodes[nodePermutation[3]]
            ));

         return true;
      }
   }

   /**
    * Remove three on a face
    */
   private static class Pattern5Base implements Pattern {

      @Override
      public int numNodes() {
         return 5;
      }

      @Override
      public boolean accept(
         HexElement hex, FemNode3d[] nodes, IntegrationPoint3d[] ipnts, int[] nodePermutation, double margin,
         DistanceGrid dg, List<FemElement3d> out) {

         if (checkNodes) {
            if (dg.getLocalDistance(nodes[nodePermutation[7]].getPosition()) <= margin) {
               return false;
            }
            if (dg.getLocalDistance(nodes[nodePermutation[6]].getPosition()) <= margin) {
               return false;
            }
            if (dg.getLocalDistance(nodes[nodePermutation[5]].getPosition()) <= margin) {
               return false;
            }
         }
         
         // use ipnts to check
         Point3d pos = new Point3d();

         // remove node 7
         ipnts[nodePermutation[7]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // remove node 6
         ipnts[nodePermutation[6]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // remove node 5
         ipnts[nodePermutation[5]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // sample from elements to remove
         PyramidElement pyr1 = new PyramidElement(nodes[nodePermutation[2]], nodes[nodePermutation[6]], nodes[nodePermutation[7]],
            nodes[nodePermutation[3]], nodes[nodePermutation[4]]);
         PyramidElement pyr2 = new PyramidElement(nodes[nodePermutation[1]], nodes[nodePermutation[5]], nodes[nodePermutation[6]],
            nodes[nodePermutation[2]], nodes[nodePermutation[4]]);

         
         // LagrangianFemElementSampler sampler = new LagrangianFemElementSampler();
         RegularFemElementSampler sampler = new RegularFemElementSampler(SAMPLE_RES);
         sampler.setElement(pyr1);
         int samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }
         sampler.setElement(pyr2);
         samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }
         
         out.add(new PyramidElement(
            nodes[nodePermutation[0]], nodes[nodePermutation[1]], nodes[nodePermutation[2]],
            nodes[nodePermutation[3]], nodes[nodePermutation[4]]));

         return true;
      }
   }

   /**
    * Remove an edge and diagonal
    */
   private static class Pattern5Mid implements Pattern {

      @Override
      public int numNodes() {
         return 5;
      }

      @Override
      public boolean accept(
         HexElement hex, FemNode3d[] nodes, IntegrationPoint3d[] ipnts, int[] nodePermutation, double margin,
         DistanceGrid dg, List<FemElement3d> out) {

         if (checkNodes) {
            if (dg.getLocalDistance(nodes[nodePermutation[7]].getPosition()) <= margin) {
               return false;
            }
            if (dg.getLocalDistance(nodes[nodePermutation[6]].getPosition()) <= margin) {
               return false;
            }
            if (dg.getLocalDistance(nodes[nodePermutation[1]].getPosition()) <= margin) {
               return false;
            }
         }
         
         // use ipnts to check
         Point3d pos = new Point3d();

         // remove node 7
         ipnts[nodePermutation[7]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // remove node 6
         ipnts[nodePermutation[6]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // remove node 1
         ipnts[nodePermutation[1]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // removing wedge and tet
         // sample from elements to remove
         WedgeElement wedge1 = new WedgeElement(nodes[nodePermutation[2]], nodes[nodePermutation[5]], nodes[nodePermutation[6]],
            nodes[nodePermutation[3]], nodes[nodePermutation[4]], nodes[nodePermutation[7]]);
         TetElement tet1 = new TetElement(nodes[nodePermutation[0]], nodes[nodePermutation[1]], nodes[nodePermutation[2]],
            nodes[nodePermutation[5]]);
         
         // LagrangianFemElementSampler sampler = new LagrangianFemElementSampler();
         RegularFemElementSampler sampler = new RegularFemElementSampler(SAMPLE_RES);
         
         sampler.setElement(tet1);
         int samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }
         sampler.setElement(wedge1);
         samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }

         out.add(new PyramidElement(
            nodes[nodePermutation[2]], nodes[nodePermutation[5]], nodes[nodePermutation[4]],
            nodes[nodePermutation[3]], nodes[nodePermutation[0]]));

         return true;
      }
   }

   // Pattern 4, anything except all on same face
   /**
    * Keep 0,1,3,4
    */
   private static class Pattern4Corner implements Pattern {

      @Override
      public int numNodes() {
         return 4;
      }

      @Override
      public boolean accept(
         HexElement hex, FemNode3d[] nodes, IntegrationPoint3d[] ipnts, int[] nodePermutation, double margin,
         DistanceGrid dg, List<FemElement3d> out) {

         if (checkNodes) {
            if (dg.getLocalDistance(nodes[nodePermutation[2]].getPosition()) <= margin) {
               return false;
            }
            if (dg.getLocalDistance(nodes[nodePermutation[5]].getPosition()) <= margin) {
               return false;
            }
            if (dg.getLocalDistance(nodes[nodePermutation[6]].getPosition()) <= margin) {
               return false;
            }
            if (dg.getLocalDistance(nodes[nodePermutation[7]].getPosition()) <= margin) {
               return false;
            }
         }
         
         // use ipnts to check
         Point3d pos = new Point3d();

         // ipnts
         ipnts[nodePermutation[2]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }
         ipnts[nodePermutation[5]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }
         ipnts[nodePermutation[6]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }
         ipnts[nodePermutation[7]].computePosition(pos, hex);
         if (dg.getLocalDistance(pos) <= margin) {
            return false;
         }

         // removing wedge and tet
         // sample from elements to remove
         WedgeElement wedge1 = new WedgeElement(nodes[nodePermutation[1]], nodes[nodePermutation[2]], nodes[nodePermutation[3]],
            nodes[nodePermutation[5]], nodes[nodePermutation[6]], nodes[nodePermutation[7]]);
         PyramidElement pyr1 = new PyramidElement(nodes[nodePermutation[1]], nodes[nodePermutation[5]], nodes[nodePermutation[7]],
            nodes[nodePermutation[3]], nodes[nodePermutation[4]]);
         
         // LagrangianFemElementSampler sampler = new LagrangianFemElementSampler();
         RegularFemElementSampler sampler = new RegularFemElementSampler(SAMPLE_RES);
         sampler.setElement(wedge1);
         int samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }
         sampler.setElement(pyr1);
         samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }

         out.add(new TetElement(
            nodes[nodePermutation[0]], nodes[nodePermutation[1]], nodes[nodePermutation[3]],
            nodes[nodePermutation[4]]));

         return true;
      }
   }

   private static class Pattern0 implements Pattern {

      @Override
      public int numNodes() {
         return 0;
      }

      @Override
      public boolean accept(
         HexElement hex, FemNode3d[] nodes, IntegrationPoint3d[] ipnts, int[] nodePermutation, double margin,
         DistanceGrid dg, List<FemElement3d> out) {
         
         if (checkNodes) {
            for (int i=0; i<nodes.length; ++i) {
               double d = dg.getLocalDistance(nodes[i].getPosition()); 
               if (d <= margin) {
                  return false;
               }
            }
         }
         
         // use ipnts to check
         Point3d pos = new Point3d();

         // remove all nodes
         for (int i=0; i<ipnts.length; ++i) {
            IntegrationPoint3d pnt = ipnts[i];
            pnt.computePosition(pos, hex);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }

         // LagrangianFemElementSampler sampler = new LagrangianFemElementSampler();
         RegularFemElementSampler sampler = new RegularFemElementSampler(SAMPLE_RES);
         sampler.setElement(hex);
         int samples = sampler.numUniqueSamples();
         for (int i=0; i<samples; ++i) {
            sampler.sample(pos);
            double d = dg.getLocalDistance(pos);
            if (d <= margin) {
               return false;
            }
         }

         return true;
      }

   }


   /**
    * Trims corners off a Hex Element if doing so is still outside a margin from an object.  Quad faces
    * not involved in trimming are kept as quads to help ensure compatibility
    * 
    * @param hex   hex to trim
    * @param margin  margin to leave around the distance queryable object
    * @param dg distance queryable object
    * @param toAdd populated list of elements to add (result of trimming)
    * @return true if element can be trimmed by removing the hex and adding toAdd
    */
   public static boolean trim(HexElement hex, double margin, DistanceGrid dg, List<FemElement3d> toAdd) {

      Pattern[] patterns = {
                            new Pattern0(),
                            new Pattern4Corner(),
                            new Pattern5Base(),
                            new Pattern5Mid(),
                            new Pattern6Wedge(),
                            new Pattern6PyramidPyramid(),
                            new Pattern6PyramidTetTet()
      };

      // flip order because of odd hex ordering
      FemNode3d[] nodes = new FemNode3d[8];
      FemNode3d[] onodes = hex.getNodes();
      nodes[0] = onodes[4];
      nodes[1] = onodes[5];
      nodes[2] = onodes[6];
      nodes[3] = onodes[7];
      nodes[4] = onodes[0];
      nodes[5] = onodes[1];
      nodes[6] = onodes[2];
      nodes[7] = onodes[3];

      IntegrationPoint3d[] ipnts = new IntegrationPoint3d[8];
      IntegrationPoint3d[] oipnts = hex.getIntegrationPoints();
      ipnts[0] = oipnts[4];
      ipnts[1] = oipnts[5];
      ipnts[2] = oipnts[6];
      ipnts[3] = oipnts[7];
      ipnts[4] = oipnts[0];
      ipnts[5] = oipnts[1];
      ipnts[6] = oipnts[2];
      ipnts[7] = oipnts[3];

      for (Pattern pattern : patterns) {
         for (int[] perm : HEX_PERMUTATIONS) {
            if (pattern.accept(hex, nodes, ipnts, perm, margin, dg, toAdd)) {
               return true;
            }
         }
      }

      return false;
   }

}
