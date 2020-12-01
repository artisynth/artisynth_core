/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Random;

import artisynth.core.mechmodels.DynamicAttachmentComp;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ComponentListView;
import artisynth.core.modelbase.CopyableComponent;
import artisynth.core.modelbase.ModelComponent;
import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.TetgenTessellator;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderableUtils;
import maspack.util.InternalErrorException;

public class FemFactory {

   public enum FemElementType {
      Tet, Hex, Wedge, Pyramid, QuadTet, QuadHex, QuadWedge, QuadPyramid,
      ShellTri, ShellQuad
   }

   // not currently used
   public enum FemShapingType {
      Linear, Quadratic
   }

//   public enum FemElemType {
//      Tet, Hex, QuadTet, QuadHex, Wedge, QuadWedge
//   }

   /**
    * Create the grid nodes for a volumetric grid.
    */
   private static void createGridNodes(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {

      if (numX < 1 || numY < 1 || numZ < 1) {
         throw new IllegalArgumentException(
            "number of elements in each direction must be >= 1");
      }
      // create all the particles
      double dx = 1.0 / numX;
      double dy = 1.0 / numY;
      double dz = 1.0 / numZ;

      Point3d p = new Point3d();

      for (int k = 0; k <= numZ; k++) {
         for (int j = 0; j <= numY; j++) {
            for (int i = 0; i <= numX; i++) {
               p.x = widthX * (-0.5 + i * dx);
               p.y = widthY * (-0.5 + j * dy);
               p.z = widthZ * (-0.5 + k * dz);
               model.addNode(new FemNode3d(p));
            }
         }
      }
   }

   /**
    * Creates a regular grid composed of tet elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Tet}.
    */
   public static FemModel3d createTetGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {

      if (model != null) {
         model.clear();
      }
      else {
         model = new FemModel3d();
      }

      createGridNodes(model, widthX, widthY, widthZ, numX, numY, numZ);
      // create all the elements
      ComponentListView<FemNode3d> nodes = model.getNodes();
      int wk = (numX + 1) * (numY + 1);
      int wj = (numX + 1);
      for (int i = 0; i < numX; i++) {
         for (int j = 0; j < numY; j++) {
            for (int k = 0; k < numZ; k++) {
               TetElement[] elems =
                  TetElement.createCubeTesselation(
                     nodes.get((k + 1) * wk + j * wj + i),
                     nodes.get((k + 1) * wk + j * wj + i + 1),
                     nodes.get((k + 1) * wk + (j + 1) * wj + i + 1),
                     nodes.get((k + 1) * wk + (j + 1) * wj + i),
                     nodes.get(k * wk + j * wj + i),
                     nodes.get(k * wk + j * wj + i + 1),
                     nodes.get(k * wk + (j + 1) * wj + i + 1),
                     nodes.get(k * wk + (j + 1) * wj + i),
                     /* even= */(i + j + k) % 2 == 0);
               for (FemElement3d e : elems) {
                  model.addElement(e);
               }
            }
         }
      }
      setGridEdgesHard(model, widthX, widthY, widthZ);
      model.invalidateStressAndStiffness();
      return model;
   }

   private static final int[] apexNodeTable = new int[] { 2, 3, 1, 0, 6, 7, 5,
                                                         4 };

   /**
    * Creates a regular grid composed of pyramid elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Pyramid}.
    */
   public static FemModel3d createPyramidGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {

      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }
      
      createGridNodes(model, widthX, widthY, widthZ, numX, numY, numZ);
      // create all the elements
      ComponentListView<FemNode3d> nodes = model.getNodes();
      int wk = (numX + 1) * (numY + 1);
      int wj = (numX + 1);
      for (int i = 0; i < numX; i++) {
         for (int j = 0; j < numY; j++) {
            for (int k = 0; k < numZ; k++) {
               int evenCode = 0;
               if ((i % 2) == 0) {
                  evenCode |= 0x1;
               }
               if ((j % 2) == 0) {
                  evenCode |= 0x2;
               }
               if ((k % 2) == 0) {
                  evenCode |= 0x4;
               }
               PyramidElement[] elems =
                  PyramidElement.createCubeTesselation(
                     nodes.get((k + 1) * wk + j * wj + i),
                     nodes.get((k + 1) * wk + j * wj + i + 1),
                     nodes.get((k + 1) * wk + (j + 1) * wj + i + 1),
                     nodes.get((k + 1) * wk + (j + 1) * wj + i),
                     nodes.get(k * wk + j * wj + i),
                     nodes.get(k * wk + j * wj + i + 1),
                     nodes.get(k * wk + (j + 1) * wj + i + 1),
                     nodes.get(k * wk + (j + 1) * wj + i),
                     apexNodeTable[evenCode]);
               for (FemElement3d e : elems) {
                  model.addElement(e);
               }
            }
         }
      }
      setGridEdgesHard(model, widthX, widthY, widthZ);
      model.invalidateStressAndStiffness();
      return model;
   }

   /**
    * Creates a regular grid composed of hex elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Hex}.
    */
   public static FemModel3d createHexGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      // clear();

      if (model != null) {
         model.clear();
      }
      else {
         model = new FemModel3d();
      }

      createGridNodes(model, widthX, widthY, widthZ, numX, numY, numZ);
      // System.out.println("num nodes: "+myNodes.size());
      // create all the elements
      ComponentListView<FemNode3d> nodes = model.getNodes();

      int wk = (numX + 1) * (numY + 1);
      int wj = (numX + 1);
      for (int i = 0; i < numX; i++) {
         for (int j = 0; j < numY; j++) {
            for (int k = 0; k < numZ; k++) {
               // TetElement[] elems = TetElement.createCubeTesselation(
               HexElement e =
                  new HexElement(
                     nodes.get((k + 1) * wk + j * wj + i), nodes.get((k + 1)
                        * wk + j * wj + i + 1), nodes.get((k + 1) * wk
                        + (j + 1) * wj + i + 1), nodes.get((k + 1) * wk
                        + (j + 1) * wj + i), nodes.get(k * wk + j * wj + i),
                     nodes.get(k * wk + j * wj + i + 1), nodes.get(k * wk
                        + (j + 1) * wj + i + 1), nodes.get(k * wk + (j + 1)
                        * wj + i));

               // System.out.println ("node idxs");
               // for (int c = 0; c < e.getNodes().length; c++)
               // System.out.print (e.getNodes()[c].getNumber() + ", ");
               // System.out.println ("");

               e.setParity((i + j + k) % 2 == 0 ? 1 : 0);

               // /* even= */(i + j + k) % 2 == 0);

               model.addElement(e);
               // for (FemElement3d e : elems)
               // {
               // addElement(e);
               // }
            }
         }
      }
      setGridEdgesHard(model, widthX, widthY, widthZ);
      model.invalidateStressAndStiffness();
      return model;
   }


   /**
    * Creates a regular grid composed of wedge elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Wedge}.
    */
   public static FemModel3d createWedgeGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {

      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }

      createGridNodes(model, widthX, widthY, widthZ, numX, numY, numZ);
      // System.out.println("num nodes: "+myNodes.size());
      // create all the elements
      ComponentListView<FemNode3d> nodes = model.getNodes();

      int wk = (numX + 1) * (numY + 1);
      int wj = (numX + 1);
      for (int i = 0; i < numX; i++) {
         for (int j = 0; j < numY; j++) {
            for (int k = 0; k < numZ; k++) {
               // node numbers reflect their location in a hex node
               FemNode3d n0 = nodes.get((k + 1) * wk + j * wj + i);
               FemNode3d n1 = nodes.get((k + 1) * wk + j * wj + i + 1);
               FemNode3d n2 = nodes.get((k + 1) * wk + (j + 1) * wj + i + 1);
               FemNode3d n3 = nodes.get((k + 1) * wk + (j + 1) * wj + i);
               FemNode3d n4 = nodes.get(k * wk + j * wj + i);
               FemNode3d n5 = nodes.get(k * wk + j * wj + i + 1);
               FemNode3d n6 = nodes.get(k * wk + (j + 1) * wj + i + 1);
               FemNode3d n7 = nodes.get(k * wk + (j + 1) * wj + i);

               WedgeElement e1 = new WedgeElement(n0, n1, n4, n3, n2, n7);
               WedgeElement e2 = new WedgeElement(n1, n5, n4, n2, n6, n7);

               model.addElement(e1);
               model.addElement(e2);
            }
         }
      }
      setGridEdgesHard(model, widthX, widthY, widthZ);
      model.invalidateStressAndStiffness();
      return model;
   }

   /**
    * Creates a regular grid composed of quadratic tet elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadTet}.
    */
   public static FemModel3d createQuadtetGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      FemModel3d tetmod = new FemModel3d();
      createTetGrid(tetmod, widthX, widthY, widthZ, numX, numY, numZ);

      model = createQuadraticModel(model, tetmod);
      setGridEdgesHard(model, widthX, widthY, widthZ);
      return model;
   }

   /**
    * Creates a regular grid composed of quadratic hex elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadHex}.
    */
   public static FemModel3d createQuadhexGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      
      FemModel3d hexmod = new FemModel3d();
      createHexGrid(hexmod, widthX, widthY, widthZ, numX, numY, numZ);
      model = createQuadraticModel(model, hexmod);
      setGridEdgesHard(model, widthX, widthY, widthZ);
      return model;
   }

   /**
    * Creates a regular grid composed of quadratic wedge elements. Identical to
    * {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadWedge}.
    */
   public static FemModel3d createQuadwedgeGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      FemModel3d linmod = new FemModel3d();
      createWedgeGrid(linmod, widthX, widthY, widthZ, numX, numY, numZ);

      model = createQuadraticModel(model, linmod);
      setGridEdgesHard(model, widthX, widthY, widthZ);

      return model;
   }

   /**
    * Creates a regular grid composed of quadratic pyramid elements. Identical
    * to {@link
    * #createGrid(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadPyramid}.
    */
   public static FemModel3d createQuadpyramidGrid(
      FemModel3d model, double widthX, double widthY, double widthZ, int numX,
      int numY, int numZ) {
      FemModel3d linmod = new FemModel3d();
      createPyramidGrid(linmod, widthX, widthY, widthZ, numX, numY, numZ);

      model = createQuadraticModel(model, linmod);
      setGridEdgesHard(model, widthX, widthY, widthZ);

      return model;
   }

   /**
    * Creates a regular grid, composed of elements of the type specified by
    * <code>type</code>, centered on the origin, with specified widths and grid
    * resolutions along each axis.
    *
    * @param model model to which the elements be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param widthX x axis model width
    * @param widthY y axis model width
    * @param widthZ z axis model width
    * @param numX element resolution along the x axis
    * @param numY element resolution along the y axis
    * @param numZ element resolution along the z axis
    * @return created FEM model
    * @throws IllegalArgumentException if the specified element type
    * is not supported
    */
   public static FemModel3d createGrid(
      FemModel3d model, FemElementType type, double widthX, double widthY,
      double widthZ, int numX, int numY, int numZ) {
      switch (type) {
         case Tet:
            return createTetGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case Hex:
            return createHexGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case Wedge:
            return createWedgeGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case Pyramid:
            return createPyramidGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case QuadTet:
            return createQuadtetGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case QuadHex:
            return createQuadhexGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case QuadWedge:
            return createQuadwedgeGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         case QuadPyramid:
            return createQuadpyramidGrid(
               model, widthX, widthY, widthZ, numX, numY, numZ);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   /**
    * Create the grid nodes for a shell grid.
    */
   private static void createShellGridNodes(
      FemModel3d model, double widthX, double widthY, int numX, int numY) {

      if (numX < 1 || numY < 1) {
         throw new IllegalArgumentException(
            "number of elements in each direction must be >= 1");
      }
      // create all the particles
      double dx = 1.0 / numX;
      double dy = 1.0 / numY;

      Point3d p = new Point3d();

      for (int j = 0; j <= numY; j++) {
         for (int i = 0; i <= numX; i++) {
            p.x = widthX * (-0.5 + i * dx);
            p.y = widthY * (-0.5 + j * dy);
            p.z = 0;
            model.addNode(new FemNode3d(p));
         }
      }
   }

   /**
    * Creates a regular grid composed of shell triangle elements. Identical to
    * {@link
    * #createShellGrid(FemModel3d,FemElementType,double,double,int,int,double,
    * boolean)}
    * with the element type set to {@link FemElementType#ShellTri}.
    */
   public static FemModel3d createShellTriGrid (
      FemModel3d model, double widthX, double widthY,
      int numX, int numY, double thickness, boolean membrane) {

      if (model != null) {
         model.clear();
      }
      else {
         model = new FemModel3d();
      }
      createShellGridNodes(model, widthX, widthY, numX, numY);
      // create all the elements
      ComponentList<FemNode3d> nodes = model.getNodes();
      int wj = (numX + 1);
      for (int j = 0; j < numY; j++) {
         for (int i = 0; i < numX; i++) {
            FemNode3d n0 = nodes.get (j*wj + i);
            FemNode3d n1 = nodes.get (j*wj + i+1);
            FemNode3d n2 = nodes.get ((j+1)*wj + i+1);
            FemNode3d n3 = nodes.get ((j+1)*wj + i);
            if ((i+j)%2 == 0) {
               // even parity
               model.addShellElement (
                  new ShellTriElement (n0, n1, n2, thickness, membrane));
               model.addShellElement (
                  new ShellTriElement (n0, n2, n3, thickness, membrane));
            }
            else {
               // odd parity
               model.addShellElement (
                  new ShellTriElement (n0, n1, n3, thickness, membrane));
               model.addShellElement (
                  new ShellTriElement (n1, n2, n3, thickness, membrane));
            }
         }
      }
      model.invalidateStressAndStiffness();
      return model;
   }

   /**
    * Creates a shell model from an input mesh. The mesh must contain
    * either triangle or quad elements.
    */
   public static FemModel3d createShellModel (
      FemModel3d model, PolygonalMesh mesh,
      double thickness, boolean membrane) {

      if (model != null) {
         model.clear();
      }
      else {
         model = new FemModel3d();
      }
      for (Vertex3d vtx : mesh.getVertices()) {
         model.addNode (new FemNode3d (vtx.getPosition()));
      }
      ComponentList<FemNode3d> nodes = model.getNodes();
      for (Face face : mesh.getFaces()) {
         int numv = face.numVertices();
         if (numv == 3) {
            FemNode3d n0 = nodes.get (face.getVertex(0).getIndex());
            FemNode3d n1 = nodes.get (face.getVertex(1).getIndex());
            FemNode3d n2 = nodes.get (face.getVertex(2).getIndex());
            model.addShellElement (
               new ShellTriElement (n0, n1, n2, thickness, membrane));
         }
         else if (numv == 4) {
            FemNode3d n0 = nodes.get (face.getVertex(0).getIndex());
            FemNode3d n1 = nodes.get (face.getVertex(1).getIndex());
            FemNode3d n2 = nodes.get (face.getVertex(2).getIndex());
            FemNode3d n3 = nodes.get (face.getVertex(3).getIndex());
            model.addShellElement (
               new ShellQuadElement (n0, n1, n2, n3, thickness, membrane));
         }
         else {
            throw new IllegalArgumentException (
               "Input mesh must contain triangle or quad elements");
         }
      }
      model.invalidateStressAndStiffness();
      return model;
   }   

   /**
    * Creates a regular grid composed of shell quad elements. Identical to
    * {@link
    * #createShellGrid(FemModel3d,FemElementType,double,double,int,int,double,
    * boolean)}
    * with the element type set to {@link FemElementType#ShellQuad}.
    */
   public static FemModel3d createShellQuadGrid (
      FemModel3d model, double widthX, double widthY,
      int numX, int numY, double thickness, boolean membrane) {

      if (model != null) {
         model.clear();
      }
      else {
         model = new FemModel3d();
      }
      createShellGridNodes(model, widthX, widthY, numX, numY);
      // create all the elements
      ComponentList<FemNode3d> nodes = model.getNodes();
      int wj = (numX + 1);
      for (int j = 0; j < numY; j++) {
         for (int i = 0; i < numX; i++) {
            FemNode3d n0 = nodes.get (j*wj + i);
            FemNode3d n1 = nodes.get (j*wj + i+1);
            FemNode3d n2 = nodes.get ((j+1)*wj + i+1);
            FemNode3d n3 = nodes.get ((j+1)*wj + i);
            model.addShellElement (
               new ShellQuadElement (n0, n1, n2, n3, thickness, membrane));
         }
      }
      model.invalidateStressAndStiffness();
      return model;
   }

   /**
    * Creates a regular grid of shell elements of the type specified by
    * <code>type</code>, centered on the origin, with specified widths and grid
    * resolutions along each axis.
    *
    * @param model model to which the elements be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param widthX x axis model width
    * @param widthY y axis model width
    * @param numX element resolution along the x axis
    * @param numY element resolution along the y axis
    * @param thickness element rest thickness
    * @param membrane if {@code true}, defined the elements to
    * to be membrane elements
    * @return created FEM model
    * @throws IllegalArgumentException if the specified element type
    * is not supported or is not an shell element
    */
   public static FemModel3d createShellGrid (
      FemModel3d model, FemElementType type,
      double widthX, double widthY, int numX, int numY,
      double thickness, boolean membrane) {
      switch (type) {
         case ShellTri:
            return createShellTriGrid (
               model, widthX, widthY, numX, numY, thickness, membrane);
         case ShellQuad:
            return createShellQuadGrid (
               model, widthX, widthY, numX, numY, thickness, membrane);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   public static FemModel3d mergeCollapsedNodes(
      FemModel3d model, FemModel3d orig, double epsilon) {

      HashMap<FemNode3d,FemNode3d> pointMap =
         new HashMap<FemNode3d,FemNode3d>();
      HashMap<FemNode3d,ArrayList<FemNode3d>> invPointMap =
         new HashMap<FemNode3d,ArrayList<FemNode3d>>();

      // group nodes based on position
      for (FemNode3d node : orig.getNodes()) {

         boolean found = false;
         for (FemNode3d pos : invPointMap.keySet()) {
            if (node.getRestPosition().distance (
                   pos.getRestPosition()) < epsilon) {
               found = true;
               pointMap.put(node, pos);
               invPointMap.get(pos).add(node);
               break;
            }
         }

         if (!found) {
            FemNode3d pos = new FemNode3d(new Point3d(node.getRestPosition()));
            ArrayList<FemNode3d> nodeList = new ArrayList<FemNode3d>();
            nodeList.add(node);

            pointMap.put(node, pos);
            invPointMap.put(pos, nodeList);
         }

      }

      // clear output model
      model.clear();
      for (FemNode3d node : invPointMap.keySet()) {
         model.addNode(node);
      }

      FemNode3d[] elemNodes = null;
      FemElement3d newElem = null;

      // now we should have a new set of nodes, time to build elements
      for (FemElement3d elem : orig.getElements()) {

         // unwind existing element, replace nodes with potentially
         // reduced ones then build a new element
         elemNodes = get8Nodes(elem);
         for (int i = 0; i < elemNodes.length; i++) {
            elemNodes[i] = pointMap.get(elemNodes[i]); // replace
         }

         newElem = createElem(elemNodes);
         model.addElement(newElem);

      }

      return model;
   }

   private static FemNode3d[] get8Nodes(FemElement3d elem) {

      FemNode3d[] nodeList = elem.getNodes();
      FemNode3d[] node8List = new FemNode3d[8];

      if (elem instanceof HexElement) {
         node8List[0] = nodeList[4];
         node8List[1] = nodeList[5];
         node8List[2] = nodeList[6];
         node8List[3] = nodeList[7];
         node8List[4] = nodeList[0];
         node8List[5] = nodeList[1];
         node8List[6] = nodeList[2];
         node8List[7] = nodeList[3];
      } else if (elem instanceof WedgeElement) {
         node8List[0] = nodeList[0];
         node8List[1] = nodeList[0];
         node8List[2] = nodeList[1];
         node8List[3] = nodeList[2];
         node8List[4] = nodeList[3];
         node8List[5] = nodeList[3];
         node8List[6] = nodeList[4];
         node8List[7] = nodeList[5];
      } else if (elem instanceof PyramidElement) {
         node8List[0] = nodeList[0];
         node8List[1] = nodeList[1];
         node8List[2] = nodeList[2];
         node8List[3] = nodeList[3];
         node8List[4] = nodeList[4];
         node8List[5] = nodeList[4];
         node8List[6] = nodeList[4];
         node8List[7] = nodeList[4];
      } else if (elem instanceof TetElement) {
         node8List[0] = nodeList[0];
         node8List[1] = nodeList[0];
         node8List[2] = nodeList[1];
         node8List[3] = nodeList[2];
         node8List[4] = nodeList[3];
         node8List[5] = nodeList[3];
         node8List[6] = nodeList[3];
         node8List[7] = nodeList[3];
      } else {
         throw new IllegalArgumentException("Invalid element type");
      }

      return node8List;
   }

   private static int[][] node8Faces = { { 0, 1, 2, 3 }, { 4, 5, 6, 7 },
                                        { 0, 4, 5, 1 }, { 3, 7, 6, 2 },
                                        { 0, 3, 7, 4 }, { 1, 2, 6, 5 } };

   private static FemElement3d createElem(FemNode3d[] node8List) {

      // determine element type, 8=hex,6=wedge,5=pyramid,4=tet
      ArrayList<FemNode3d> unique = new ArrayList<FemNode3d>();
      for (FemNode3d node : node8List) {
         if (!unique.contains(node)) {
            unique.add(node);
         }
      }

      int nFaceNodes = 4;
      if (unique.size() == 6 || unique.size() == 4) {
         nFaceNodes = 3;
      }

      // find first face
      ArrayList<FemNode3d> faceNodes = new ArrayList<FemNode3d>(4);
      int faceIdx = -1;
      for (int i = 0; i < 6; i++) {
         faceNodes.clear();
         for (int j = 0; j < 4; j++) {
            int idx = node8Faces[i][j];
            if (!faceNodes.contains(node8List[idx])) {
               faceNodes.add(node8List[idx]);
            }
         }

         if (faceNodes.size() == nFaceNodes) {
            faceIdx = i;
            break;
         }
      }

      if (unique.size() == 8 || unique.size() == 6) {
         // add nodes from opposite face
         for (int i = 0; i < 4; i++) {
            FemNode3d nextNode = node8List[node8Faces[faceIdx + 1][i]];
            if (!faceNodes.contains(nextNode)) {
               faceNodes.add(nextNode);
            }
         }
      } else {
         // check if mirrored
         if (faceIdx % 2 == 1) {
            // swap nodes 2/(3,4)
            FemNode3d tmp = faceNodes.get(1);
            faceNodes.set(1, faceNodes.get(nFaceNodes - 1));
            faceNodes.set(nFaceNodes - 1, tmp);
         }

         // fill in final node
         for (FemNode3d node : unique) {
            if (!faceNodes.contains(node)) {
               faceNodes.add(node);
               break;
            }
         }
      }

      // we should now have complete oriented set of nodes
      // create the actual element

      switch (faceNodes.size()) {
         case 8:
            return new HexElement(
               faceNodes.get(4), faceNodes.get(5), faceNodes.get(6),
               faceNodes.get(7), faceNodes.get(0), faceNodes.get(1),
               faceNodes.get(2), faceNodes.get(3));
         case 6:
            return new WedgeElement(
               faceNodes.get(0), faceNodes.get(1), faceNodes.get(2),
               faceNodes.get(3), faceNodes.get(4), faceNodes.get(5));
         case 5:
            return new PyramidElement(
               faceNodes.get(0), faceNodes.get(1), faceNodes.get(2),
               faceNodes.get(3), faceNodes.get(4));
         case 4:
            return new TetElement(
               faceNodes.get(0), faceNodes.get(1), faceNodes.get(2),
               faceNodes.get(3));
         default:
      }

      throw new IllegalArgumentException(
         "Invalid number or ordering of unique nodes");

   }


   /**
    * Creates a tet-based spherical model based on a icosahedron. The method
    * works by creating an icosahedral surface mesh and then using tetgen.
    * 
    * @param model empty FEM model to which elements are added; if
    * <code>null</code> then a new model is allocated
    * @param r radius
    * @param ndivisions number of divisions used in creating the surface mesh.
    * Typical values are 1 or 2.
    * @param quality quality parameter passed to tetgen. See
    * {@link #createFromMesh} for a full description.
    * @return the FEM model (which will be <code>model</code> if
    * <code>model</code> is not <code>null</code>).
    */
   public static FemModel3d createIcosahedralSphere (
      FemModel3d model, double r, int ndivisions, double quality) {

      PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (
         r, Point3d.ZERO, ndivisions);
      return createFromMesh (model, mesh, quality);
   }
   
   /**
    * Creates a fem model with tet, pyramid and hex elements by breaking
    * the domain into spherical (3D polar) coordinates.  The radius is adjusted
    * geometrically so that elements maintain equal proportions
    * 
    * @param model model to populate, null to create one
    * @param r radius of sphere
    * @param nlat number of latitude divisions (not including 0)
    * @param nlong number of longitude divisions
    * @param nr number of radial divisions (not including 0)
    * @return populated or created model
    */
   public static FemModel3d createPolarSphere(FemModel3d model,
      double r, int nlat, int nlong, int nr) {
      
      if (nr < 1) {
         nr = 1;
      }
      if (nlat < 4) {
         nlat = 4;
      }
      nlat += (nlat % 2);
      
      if (nlong < 4) {
         nlong = 4;
      }
      nlong += (4-(nlong % 4)) % 4;
      
      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      
      FemNode3d[][][] nodes = new FemNode3d[nr+1][nlat+1][nlong];
      
      nodes[0][0][0] = new FemNode3d(0,0,0);
      model.addNode (nodes[0][0][0]);
      
      double dt = Math.PI/nlat;
      double ds = Math.PI*2/nlong;
      
      // geometric growth of r
      double cr = 2.78;
      double rr = r/Math.pow (cr, nr);
      
      for (int k = 1; k <= nr; ++k) {
         rr = rr*cr; // increase radius
         
         // top
         nodes[k][0][0] = new FemNode3d(0, 0, rr);
         model.addNode (nodes[k][0][0]);
         
         // arc
         for (int t = 1; t < nlat; ++t) {
            double theta = t*dt;
            double sint = Math.sin (theta);
            double cost = Math.cos (theta);
            
            for (int s = 0; s < nlong; ++s) {
               double phi = s*ds;
               
               double x = rr*sint*Math.cos (phi);
               double y = rr*sint*Math.sin (phi);
               double z = rr*cost;
               nodes[k][t][s] = new FemNode3d(x, y, z);
               model.addNode (nodes[k][t][s]);
            }
         }
         // bottom
         nodes[k][nlat][0] = new FemNode3d(0, 0, -rr);
         model.addNode (nodes[k][nlat][0]);
      }
      
      // add elements
      
      // k == 0
      // tets, t=0
      for (int s=0; s<nlong; ++s) {
         FemNode3d n0 = nodes[1][1][s];
         FemNode3d n1 = nodes[1][0][0];
         FemNode3d n2 = nodes[1][1][(s+1)%nlong];
         FemNode3d n3 = nodes[0][0][0];
         model.addElement (new TetElement(n0, n1, n2, n3));
      }
      // pyramids
      for (int t=1; t<nlat-1; ++t) {
         for (int s=0; s<nlong; ++s) {
            int snext = (s+1) % nlong;
            FemNode3d n0 = nodes[1][t+1][s];
            FemNode3d n1 = nodes[1][t][s];
            FemNode3d n2 = nodes[1][t][snext];
            FemNode3d n3 = nodes[1][t+1][snext];
            FemNode3d n4 = nodes[0][0][0];
            model.addElement (new PyramidElement(n0, n1, n2, n3, n4));
         }
      }
      // tet t=nlat-1
      for (int s=0; s<nlong; ++s) {
         
         FemNode3d n0 = nodes[1][nlat][0];
         FemNode3d n1 = nodes[1][nlat-1][s];
         FemNode3d n2 = nodes[1][nlat-1][(s+1) % nlong];
         FemNode3d n3 = nodes[0][0][0];
         model.addElement (new TetElement(n0, n1, n2, n3));
      }
      
      for (int k=1; k<nr; ++k) {
         
         // wedges t=0;
         for (int s=0; s<nlong; ++s) {
            int snext = (s+1) % nlong;
            FemNode3d n0 = nodes[k+1][1][s];
            FemNode3d n1 = nodes[k+1][0][0];
            FemNode3d n2 = nodes[k+1][1][snext];
            FemNode3d n3 = nodes[k][1][s];
            FemNode3d n4 = nodes[k][0][0];
            FemNode3d n5 = nodes[k][1][snext];
            model.addElement (new WedgeElement (n0, n1, n2, n3, n4, n5));
         }
         // hexes
         for (int t=1; t<nlat-1; ++t) {
            for (int s = 0; s<nlong; ++s) {
               int snext = (s+1) % nlong;
               FemNode3d n0 = nodes[k+1][t+1][s];
               FemNode3d n1 = nodes[k+1][t][s];
               FemNode3d n2 = nodes[k+1][t][snext];
               FemNode3d n3 = nodes[k+1][t+1][snext];
               FemNode3d n4 = nodes[k][t+1][s];
               FemNode3d n5 = nodes[k][t][s];
               FemNode3d n6 = nodes[k][t][snext];
               FemNode3d n7 = nodes[k][t+1][snext];
               // hex has backwards definition
               model.addElement (
                  new HexElement(n0, n3, n2, n1, n4, n7, n6, n5));
            }
         }
         // wedges t=nlat-1;
         for (int s=0; s<nlong; ++s) {
            int snext = (s+1) % nlong;
            FemNode3d n0 = nodes[k+1][nlat][0];
            FemNode3d n1 = nodes[k+1][nlat-1][s];
            FemNode3d n2 = nodes[k+1][nlat-1][snext];
            
            FemNode3d n3 = nodes[k][nlat][0];
            FemNode3d n4 = nodes[k][nlat-1][s];
            FemNode3d n5 = nodes[k][nlat-1][snext];
            
            model.addElement (new WedgeElement (n0, n1, n2, n3, n4, n5));
         }
      }
      
      return model;
   }

   /**
    * Convenience method to create a symmetric hex/wedge dominant sphere
    * using {@link #createEllipsoid}.
    * 
    * @param model empty FEM model to which elements are added; if
    * <code>null</code> then a new model is allocated
    * @param r radius
    * @param nt number of nodes in each ring parallel to the equator
    * @param nl number of nodes in each quarter ring perpendicular to the
    * equator (including end nodes)
    * @param ns number of nodes in each radial line extending out from
    * the polar axis (including end nodes)
    * @return the FEM model (which will be <code>model</code> if
    * <code>model</code> is not <code>null</code>).
    */
   public static FemModel3d createSphere (
       FemModel3d model, double r, int nt, int nl, int ns) {
      return createEllipsoid (model, r, r, r, nt, nl, ns);
   }
   
   /**
    * Tessellate a pyramid.  Even splits have a rising edge from n0 to n2
    * @param fem model to add elements to
    * @param n0 first node on quad face
    * @param n1 second node on quad face
    * @param n2 third node on quad face
    * @param n3 fourth node on quad face
    * @param n4 opposite node
    * @param even split control
    */
   private static void addPyramidTesselation (
      FemModel3d fem, FemNode3d n0, FemNode3d n1, FemNode3d n2,
      FemNode3d n3, FemNode3d n4, boolean even) {
      if (even) {
         fem.addElement (new TetElement(n0, n1, n2, n4));
         fem.addElement (new TetElement(n0, n2, n3, n4));
      } else {
         fem.addElement (new TetElement(n0, n1, n3, n4));
         fem.addElement (new TetElement(n1, n2, n3, n4));
      }
   }
   
   /**
    * Tessellate a pyramid with quadratic tets. Even splits have a rising edge
    * from n0 to n2
    * 
    * @param fem model to add elements to
    * @param n0 first node on quad face
    * @param n1 second node on quad face
    * @param n2 third node on quad face
    * @param n3 fourth node on quad face
    * @param n4 opposite node
    * @param n01 node half-way between 0 and 1
    * @param n12 node half-way between 1 and 2
    * @param n23 node half-way between 2 and 3
    * @param n30 node half-way between 3 and 0
    * @param n04 node half-way between 0 and 4
    * @param n14 node half-way between 1 and 4
    * @param n24 node half-way between 2 and 4
    * @param n34 node half-way between 3 and 4
    * @param n0123 node in center of face
    
    * 
    * @param even split control
    */
   private static void addPyramidQuadTesselation(FemModel3d fem, 
      FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3, FemNode3d n4, 
      FemNode3d n01, FemNode3d n12, FemNode3d n23, FemNode3d n30, 
      FemNode3d n04, FemNode3d n14, FemNode3d n24, FemNode3d n34,
      FemNode3d n0123,
      boolean even) {
      if (even) {
         fem.addElement (
            new QuadtetElement(n0, n1, n2, n4, n01, n12, n0123, n04, n14, n24));
         fem.addElement (
            new QuadtetElement(n0, n2, n3, n4, n0123, n23, n30, n04, n24, n34));
      } else {
         fem.addElement (
            new QuadtetElement(n0, n1, n3, n4, n01, n0123, n30, n04, n14, n34));
         fem.addElement (
            new QuadtetElement(n1, n2, n3, n4, n12, n23, n0123, n14, n24, n34));
      }
   }
   
   /**
    * Adds tests to fill a space between two triangles facing opposite
    * directions.  The resulting shape has a total of 8 triangular faces.  The
    * volume is tessellated symmetrically with 4 tets, with the first face
    * connected to node 5.  Nodes are ordered such that n0-n2 describe the
    * first triangle CW, n3-n5 the second CCW, and n0 is connected to edge
    * n3-n4
    * 
    * @param fem model to add elements to
    * @param n0 first node of triangle 1
    * @param n1 second node of triangle 1
    * @param n2 third node of triangle 1
    * @param n3 first node of triangle 2
    * @param n4 second node of triangle 2
    * @param n5 third node of triangle 2
    */
   private static void addFlippedTriTessellation (
      FemModel3d fem, FemNode3d n0, FemNode3d n1, FemNode3d n2,
      FemNode3d n3, FemNode3d n4, FemNode3d n5) {
      
      fem.addElement (new TetElement(n0, n1, n2, n5));
      fem.addElement (new TetElement(n5, n4, n3, n0));
      fem.addElement (new TetElement(n1, n0, n4, n5));
      fem.addElement (new TetElement(n2, n0, n5, n3));
      
   }
   
   /**
    * Adds tests to fill a space between two triangles facing opposite
    * directions.  The resulting shape has a total of 8 triangular faces.  The
    * volume is tessellated symmetrically with 4 tets, with the first face
    * connected to node 5.  Nodes are ordered such that n0-n2 describe the
    * first triangle CW, n3-n5 the second CCW, and n0 is connected to edge
    * n3-n4
    * 
    * @param fem model to add elements to
    * @param n0 first node of triangle 1
    * @param n1 second node of triangle 1
    * @param n2 third node of triangle 1
    * @param n3 first node of triangle 2
    * @param n4 second node of triangle 2
    * @param n5 third node of triangle 2
    * @param n01 node half-way between 0 and 1
    * @param n12 node half-way between 1 and 2
    * @param n20 node half-way between 2 and 0
    * @param n34 node half-way between 3 and 4
    * @param n45 node half-way between 4 and 5
    * @param n53 node half-way between 5 and 3
    * @param n03 node half-way between 0 and 3
    * @param n04 node half-way between 0 and 4
    * @param n14 node half-way between 1 and 4
    * @param n15 node half-way between 1 and 5
    * @param n25 node half-way between 2 and 5
    * @param n23 node half-way between 2 and 3
    * @param n05 node half-way between 0 and 5 (center)
    */
   private static void addFlippedTriQuadTessellation(FemModel3d fem, 
      FemNode3d n0, FemNode3d n1, FemNode3d n2, 
      FemNode3d n3, FemNode3d n4, FemNode3d n5,
      FemNode3d n01, FemNode3d n12, FemNode3d n20,
      FemNode3d n34, FemNode3d n45, FemNode3d n53,
      FemNode3d n03, FemNode3d n04,
      FemNode3d n14, FemNode3d n15, 
      FemNode3d n25, FemNode3d n23,
      FemNode3d n05) {
      
      fem.addElement (
         new QuadtetElement(n0, n1, n2, n5, n01, n12, n20, n05, n15, n25));
      fem.addElement (
         new QuadtetElement(n5, n4, n3, n0, n45, n34, n53, n05, n04, n03));
      fem.addElement (
         new QuadtetElement(n1, n0, n4, n5, n01, n04, n14, n15, n05, n45));
      fem.addElement (
         new QuadtetElement(n2, n0, n5, n3, n20, n05, n25, n23, n03, n53));
      
   }
   
   /**
    * Creates a sphere out of approximately uniform tets.  This is accomplished
    * by separating the sphere into a hexagonal prism with hexagonal pyramid
    * caps, dividing each edge into k segments for the kth radius, and
    * connecting the layers with patterns of tets.
    * 
    * @param model model to populate, created if null
    * @param r radius of sphere
    * @param nr number of layers
    * @return populated or created model
    */
   public static FemModel3d createTetSphere (
      FemModel3d model, double r, int nr) {
      
      if (model == null) {
         model = new FemModel3d();
      }
      
      int ngs = 6;
      int ngt = 3;
      
      if (nr < 1) {
         r = 1;
      }
      
      FemNode3d[][] layer = new FemNode3d[1][2];
      FemNode3d[][] lastLayer;
      {
         // center
         FemNode3d node = new FemNode3d(0,0,0);
         model.addNode (node);
         layer[0][0] = node;
         layer[0][1] = node;
      }
      
      
      // distribute nodes radially
      double dr = r/nr;
      
      // radial layers
      for (int k = 1; k<=nr; ++k) {

         int nt = ngt*k;
         double rr = k*dr;
         double dphi = Math.PI/nt;
         
         lastLayer = layer;
         layer = new FemNode3d[nt+1][];
      
         // distribute nodes
         
         // north pole
         {
            layer[0] = new FemNode3d[2];
            FemNode3d node = new FemNode3d(0,0,rr);
            model.addNode (node);
            layer[0][0] = node;
            layer[0][1] = node;
         }
         
         // first latitude group grows
         {
            int gt = 0;
            for (int i=1; i<k; ++i) {
               int t = gt*k+i;
               int ns = ngs*i;
               layer[t] = new FemNode3d[ns+1];
               
               double phi = t*dphi;
               double z = rr*Math.cos (phi);
               double xy = rr*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<i; ++j) {
                     int s = gs*i+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     FemNode3d node = new FemNode3d(x,y,z);
                     layer[t][s] = node;
                     model.addNode (node);
                  }
               }
               layer[t][ns] = layer[t][0]; // wrap around
            }
         }
         
         // middle groups constant width
         for (int gt=1; gt<ngt-1; ++gt) {
            for (int i=0; i<k; ++i) {
               int t = gt*k+i;
               int ns = ngs*k;
               layer[t] = new FemNode3d[ns+1];
               
               double phi = t*dphi;
               double z = rr*Math.cos (phi);
               double xy = rr*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<k; ++j) {
                     int s = gs*k+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     FemNode3d node = new FemNode3d(x,y,z);
                     layer[t][s] = node;
                     model.addNode (node);
                  }
               }
               layer[t][ns] = layer[t][0]; // wrap around
            }
         }
         
         // last group shrinks
         {
            int gt = ngt-1;
            for (int i=0; i<k; ++i) {
               int t = gt*k+i;
               int ns = ngs*(k-i);
               layer[t] = new FemNode3d[ns+1];
               
               double phi = t*dphi;
               double z = rr*Math.cos (phi);
               double xy = rr*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<k-i; ++j) {
                     int s = gs*(k-i)+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     FemNode3d node = new FemNode3d(x,y,z);
                     layer[t][s] = node;
                     model.addNode (node);
                  }
               }
               layer[t][ns] = layer[t][0]; // wrap around
            }
         }
         
         // south pole
         {
            layer[nt] = new FemNode3d[2];
            FemNode3d node = new FemNode3d(0,0,-rr);
            model.addNode (node);
            layer[nt][0] = node;
            layer[nt][1] = node;
         }
         
         // generate elements, move in groups
         
         // first latitude group
         {
            int gt = 0;
            for (int i=0; i<k; ++i) {
               int t = gt*k+i;
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<i; ++j) {
                     int s = gs*i+j;
                     int sdown = s+gs;
                     int sup = s-gs;
                     // downward tet 
                     model.addElement (
                        new TetElement(
                           layer[t][s], layer[t+1][sdown+1],
                           layer[t+1][sdown], lastLayer[t][s]));
                     
                     // flipped 4-tet
                     addFlippedTriTessellation (
                        model, layer[t+1][sdown+1], layer[t][s],
                        layer[t][s+1], lastLayer[t][s+1],
                        lastLayer[t][s], lastLayer[t-1][sup]);
                  }
                  
                  // trailing tet
                  int s = gs*i + i;  // current s
                  int sdown = s+gs;
                  model.addElement (
                     new TetElement(
                        layer[t][s], layer[t+1][sdown+1],
                        layer[t+1][sdown], lastLayer[t][s]));
               }
            }
            
            // upward tets connect to interior group nodes
            for (int i=2; i<k; ++i) {
               int t = gt*k+i;
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=1; j<i; ++j) {
                     int s = gs*i+j;
                     int sup = s-gs;
                     model.addElement (
                        new TetElement (
                           lastLayer[t][s], lastLayer[t-1][sup],
                           lastLayer[t-1][sup-1], layer[t][s]));
                  }
               }
            }
         }
         
         // middle groups constant width
         for (int gt=1; gt<ngt-1; ++gt) {
            
            // first k-1 rows
            for (int i=0; i<k-1; ++i) {
               int t = gt*k+i;
               int tlast = t-gt;
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  
                  // first k-1 columns
                  for (int j=0; j<k-1; ++j) {
                     int s = gs*k+j;
                     int slast = s-gs;
                     
                     // parity
                     boolean even = ((j + t) % 2) == 0;
                     
                     // pyramids up and down
                     addPyramidTesselation (
                        model, layer[t][s], layer[t][s+1], layer[t+1][s+1],
                        layer[t+1][s], lastLayer[tlast][slast], even);
                     
                     addPyramidTesselation (
                        model, lastLayer[tlast][slast],
                        lastLayer[tlast+1][slast], lastLayer[tlast+1][slast+1],
                        lastLayer[tlast][slast+1], layer[t+1][s+1], !even);
                     
                     // tets below and right
                     model.addElement (
                        new TetElement(
                           layer[t+1][s], layer[t+1][s+1],
                           lastLayer[tlast+1][slast], lastLayer[tlast][slast]));
                     model.addElement (
                        new TetElement(
                           layer[t+1][s+1], layer[t][s+1],
                           lastLayer[tlast][slast+1], lastLayer[tlast][slast]));
                  }
                  // last column 
                  {
                     // pyramid down
                     int s = (gs+1)*k-1;
                     int slast = s-gs;
                     boolean even = ((t + k + 1 ) % 2) == 0;
                     addPyramidTesselation (
                        model, layer[t][s], layer[t][s+1], layer[t+1][s+1],
                        layer[t+1][s], lastLayer[tlast][slast], even);
                     
                     // tet below
                     model.addElement (
                        new TetElement(
                           layer[t+1][s], layer[t+1][s+1],
                           lastLayer[tlast+1][slast], lastLayer[tlast][slast]));
                  }
                  
               }
            }
            
            // last row
            {
               int i=k-1;
               int t = gt*k+i;
               int tlast = t-gt;
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  
                  // first k-1 columns
                  for (int j=0; j<k-1; ++j) {
                     int s = gs*k+j;
                     int slast = s-gs;
                     
                     // parity
                     boolean even = ((t + j) % 2) == 0;
                     
                     // pyramids down
                     addPyramidTesselation (
                        model, layer[t][s], layer[t][s+1], layer[t+1][s+1],
                        layer[t+1][s], lastLayer[tlast][slast], even);
                     
                     // tets right
                     model.addElement (
                        new TetElement(
                           layer[t+1][s+1], layer[t][s+1],
                           lastLayer[tlast][slast+1], lastLayer[tlast][slast]));
                  }
                  // last column 
                  {
                     int s = (gs+1)*k - 1;
                     int slast = s - gs;
                     // parity
                     boolean even = ((t + k + 1) % 2) == 0;
                     // pyramid down
                     addPyramidTesselation (
                        model, layer[t][s], layer[t][s+1], layer[t+1][s+1],
                        layer[t+1][s], lastLayer[tlast][slast], even);
                     
                  }
                  
               }
               
            }
         }
         
         // last group shrinks
         {
            int gt = ngt-1;
            for (int i=0; i<k; ++i) {
               int t = gt*k+i;
               int tlast = t-gt;
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<k-i-1; ++j) {
                     int s = gs*(k-i)+j;
               
                     int sdown = s-gs;
                     int slast = s-gs;

                     // downward tet
                     model.addElement (
                        new TetElement(
                           layer[t][s], layer[t][s+1], layer[t+1][sdown],
                           lastLayer[tlast][slast]));
                     
                     // flipped 4-tet
                     addFlippedTriTessellation (
                        model, layer[t][s+1], layer[t+1][sdown+1],
                        layer[t+1][sdown], lastLayer[tlast][slast],
                        lastLayer[tlast][slast+1],
                        lastLayer[tlast+1][slast-gs]);
                  }
                  int s = (gs+1)*(k-i)-1;
                  int sdown = s-gs;
                  int slast = s-gs;
                  
                  // downward tet
                  model.addElement (
                     new TetElement (
                        layer[t][s], layer[t][s+1], layer[t+1][sdown],
                        lastLayer[tlast][slast]));
               }
            }
            
            // upward tets connect to interior group nodes
            for (int i=1; i<k-1; ++i) {
               int t = gt*k+i;
               int tlast = t-gt;
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=1; j<k-i; ++j) {
                     int s = gs*(k-i)+j;
                     int slast = s-gs;
                     int slastup = slast+gs;
                     
                     model.addElement (
                        new TetElement (
                           lastLayer[tlast][slast], lastLayer[tlast-1][slastup],
                           lastLayer[tlast][slast-1], layer[t][s]));
                  }
               }
            }
         }
      }
      
      return model;
   }
   
   
   /**
    * Creates a sphere out of approximately uniform quadratic tets.  This is
    * accomplished by separating the sphere into a hexagonal prism with
    * hexagonal pyramid caps, dividing each edge into k segments for the kth
    * radius, and connecting the layers with patterns of tets.
    * 
    * @param model model to populate, created if null
    * @param r radius of sphere
    * @param nr number of layers
    * @return populated or created model
    */
   public static FemModel3d createQuadtetSphere (
      FemModel3d model, double r, int nr) {
      
      if (model == null) {
         model = new FemModel3d();
      }
      
      int ngs = 6;
      int ngt = 3;
      
      if (nr < 1) {
         r = 1;
      }
      
      FemNode3d[][] outerLayer = new FemNode3d[1][2];
      FemNode3d[][] lastOuterLayer;
      {
         // center
         FemNode3d node = new FemNode3d(0,0,0);
         model.addNode (node);
         outerLayer[0][0] = node;
         outerLayer[0][1] = node;
      }
      
      
      // distribute nodes radially
      double dr = r/(2*nr);
      
      // radial layers
      for (int k = 1; k<=nr; ++k) {

         // inner and outer layers
         int ik = 2*k-1;
         int ok = 2*k;
         int nit = ngt*ik;
         int not = ngt*ok;
         
         double rir = ik*dr;
         double ror = ok*dr;
         
         double diphi = Math.PI/nit;
         double dophi = Math.PI/not;
         
         lastOuterLayer = outerLayer;
         FemNode3d[][] innerLayer = new FemNode3d[nit+1][];
         outerLayer = new FemNode3d[not+1][];
      
         // distribute nodes
         
         // inner nodes
         // north pole
         {
            innerLayer[0] = new FemNode3d[2];
            FemNode3d node = new FemNode3d(0,0,rir);
            model.addNode (node);
            innerLayer[0][0] = node;
            innerLayer[0][1] = node;
         }
         
         // first latitude group grows
         {
            int gt = 0;
            for (int i=1; i<ik; ++i) {
               int t = gt*ik+i;
               int ns = ngs*i;
               innerLayer[t] = new FemNode3d[ns+1];
               
               double phi = t*diphi;
               double z = rir*Math.cos (phi);
               double xy = rir*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<i; ++j) {
                     int s = gs*i+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     FemNode3d node = new FemNode3d(x,y,z);
                     innerLayer[t][s] = node;
                     model.addNode (node);
                  }
               }
               innerLayer[t][ns] = innerLayer[t][0]; // wrap around
            }
         }
         
         // middle groups constant width
         for (int gt=1; gt<ngt-1; ++gt) {
            for (int i=0; i<ik; ++i) {
               int t = gt*ik+i;
               int ns = ngs*ik;
               innerLayer[t] = new FemNode3d[ns+1];
               
               double phi = t*diphi;
               double z = rir*Math.cos (phi);
               double xy = rir*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<ik; ++j) {
                     int s = gs*ik+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     FemNode3d node = new FemNode3d(x,y,z);
                     innerLayer[t][s] = node;
                     model.addNode (node);
                  }
               }
               innerLayer[t][ns] = innerLayer[t][0]; // wrap around
            }
         }
         
         // last group shrinks
         {
            int gt = ngt-1;
            for (int i=0; i<ik; ++i) {
               int t = gt*ik+i;
               int ns = ngs*(ik-i);
               innerLayer[t] = new FemNode3d[ns+1];
               
               double phi = t*diphi;
               double z = rir*Math.cos (phi);
               double xy = rir*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<ik-i; ++j) {
                     int s = gs*(ik-i)+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     FemNode3d node = new FemNode3d(x,y,z);
                     innerLayer[t][s] = node;
                     model.addNode (node);
                  }
               }
               innerLayer[t][ns] = innerLayer[t][0]; // wrap around
            }
         }
         
         // south pole
         {
            innerLayer[nit] = new FemNode3d[2];
            FemNode3d node = new FemNode3d(0,0,-rir);
            model.addNode (node);
            innerLayer[nit][0] = node;
            innerLayer[nit][1] = node;
         }
         
         // outer nodes
         // north pole
         {
            outerLayer[0] = new FemNode3d[2];
            FemNode3d node = new FemNode3d(0,0,ror);
            model.addNode (node);
            outerLayer[0][0] = node;
            outerLayer[0][1] = node;
         }
         
         // first latitude group grows
         {
            int gt = 0;
            for (int i=1; i<ok; ++i) {
               int t = gt*ok+i;
               int ns = ngs*i;
               outerLayer[t] = new FemNode3d[ns+1];
               
               double phi = t*dophi;
               double z = ror*Math.cos (phi);
               double xy = ror*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<i; ++j) {
                     int s = gs*i+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     FemNode3d node = new FemNode3d(x,y,z);
                     outerLayer[t][s] = node;
                     model.addNode (node);
                  }
               }
               outerLayer[t][ns] = outerLayer[t][0]; // wrap around
            }
         }
         
         // middle groups constant width
         for (int gt=1; gt<ngt-1; ++gt) {
            for (int i=0; i<ok; ++i) {
               int t = gt*ok+i;
               int ns = ngs*ok;
               outerLayer[t] = new FemNode3d[ns+1];
               
               double phi = t*dophi;
               double z = ror*Math.cos (phi);
               double xy = ror*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<ok; ++j) {
                     int s = gs*ok+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     FemNode3d node = new FemNode3d(x,y,z);
                     outerLayer[t][s] = node;
                     model.addNode (node);
                  }
               }
               outerLayer[t][ns] = outerLayer[t][0]; // wrap around
            }
         }
         
         // last group shrinks
         {
            int gt = ngt-1;
            for (int i=0; i<ok; ++i) {
               int t = gt*ok+i;
               int ns = ngs*(ok-i);
               outerLayer[t] = new FemNode3d[ns+1];
               
               double phi = t*dophi;
               double z = ror*Math.cos (phi);
               double xy = ror*Math.sin (phi);
               double dtheta = 2*Math.PI/ns;  
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<ok-i; ++j) {
                     int s = gs*(ok-i)+j;
                     double theta = s*dtheta;
                     double x = xy*Math.cos (theta);
                     double y = xy*Math.sin (theta);
                     FemNode3d node = new FemNode3d(x,y,z);
                     outerLayer[t][s] = node;
                     model.addNode (node);
                  }
               }
               outerLayer[t][ns] = outerLayer[t][0]; // wrap around
            }
         }
         
         // south pole
         {
            outerLayer[not] = new FemNode3d[2];
            FemNode3d node = new FemNode3d(0,0,-ror);
            model.addNode (node);
            outerLayer[not][0] = node;
            outerLayer[not][1] = node;
         }
         
         // generate elements, move in groups
         
         // first latitude group
         {
            int gt = 0;
            for (int i=0; i<k; ++i) {
               int t = 2*(gt*k+i);
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<i; ++j) {
                     int s = 2*(gs*i+j);
                     int sd = s+gs;
                     int sdd = s+2*gs;
                     int su = s-gs;
                     int suu = s-2*gs;
                     
                     // downward tet 
                     model.addElement (
                        new QuadtetElement (
                           outerLayer[t][s], outerLayer[t+2][sdd+2],
                           outerLayer[t+2][sdd], lastOuterLayer[t][s],
                           outerLayer[t+1][sd+1], outerLayer[t+2][sdd+1],
                           outerLayer[t+1][sd], innerLayer[t][s],
                           innerLayer[t+1][sd+1], innerLayer[t+1][sd]));
                     
                     // flipped 4-tet
                     addFlippedTriQuadTessellation (
                        model,
                        outerLayer[t+2][sdd+2], outerLayer[t][s],
                        outerLayer[t][s+2], lastOuterLayer[t][s+2],
                        lastOuterLayer[t][s], lastOuterLayer[t-2][suu],
                        outerLayer[t+1][sd+1], outerLayer[t][s+1],
                        outerLayer[t+1][sd+2], lastOuterLayer[t][s+1],
                        lastOuterLayer[t-1][su], lastOuterLayer[t-1][su+1],
                        innerLayer[t+1][sd+2], innerLayer[t+1][sd+1],
                        innerLayer[t][s], innerLayer[t-1][su],
                        innerLayer[t-1][su+1], innerLayer[t][s+2],
                        innerLayer[t][s+1]);
                  }
                  
                  // trailing tet
                  int s = 2*(gs+1)*i;  // current s
                  int sd = s+gs;
                  int sdd = s+2*gs;
                  model.addElement (
                     new QuadtetElement (
                        outerLayer[t][s], outerLayer[t+2][sdd+2],
                        outerLayer[t+2][sdd], lastOuterLayer[t][s],
                        outerLayer[t+1][sd+1], outerLayer[t+2][sdd+1],
                        outerLayer[t+1][sd], innerLayer[t][s],
                        innerLayer[t+1][sd+1], innerLayer[t+1][sd]));
               }
            }
            
            // upward tets connect to interior group nodes
            for (int i=2; i<k; ++i) {
               int t = 2*(gt*k+i);
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=1; j<i; ++j) {
                     int s = 2*(gs*i+j);
                     int su = s-gs;
                     int suu = s-2*gs;
                     model.addElement (new QuadtetElement (
                        lastOuterLayer[t][s], lastOuterLayer[t-2][suu],
                        lastOuterLayer[t-2][suu-2], outerLayer[t][s],
                        lastOuterLayer[t-1][su], lastOuterLayer[t-2][suu-1],
                        lastOuterLayer[t-1][su-1], innerLayer[t][s],
                        innerLayer[t-1][su], innerLayer[t-1][su-1]));
                  }
               }
            }
         }
         
         // middle groups constant width
         for (int gt=1; gt<ngt-1; ++gt) {
            
            // first k-1 rows
            for (int i=0; i<k-1; ++i) {
               int t = 2*(gt*k+i);
               int tl = t-gt;    // half level lower
               int tll = t-2*gt; // full level lower
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  
                  // first k-1 columns
                  for (int j=0; j<k-1; ++j) {
                     int s = 2*(gs*k+j);
                     int sl = s-gs;    // half level lower
                     int sll = s-2*gs; // full level lower
                     
                     // parity
                     boolean even = ((t/2 + j) % 2) == 0;
                     
                     // pyramids up and down
                     addPyramidQuadTesselation (
                        model, 
                        outerLayer[t][s], outerLayer[t][s+2],
                        outerLayer[t+2][s+2], outerLayer[t+2][s], 
                        lastOuterLayer[tll][sll], outerLayer[t][s+1],
                        outerLayer[t+1][s+2], outerLayer[t+2][s+1],
                        outerLayer[t+1][s], innerLayer[tl][sl],
                        innerLayer[tl][sl+1], innerLayer[tl+1][sl+1],
                        innerLayer[tl+1][sl], outerLayer[t+1][s+1],
                        even);
                     
                     addPyramidQuadTesselation (
                        model, 
                        lastOuterLayer[tll][sll], lastOuterLayer[tll+2][sll],
                        lastOuterLayer[tll+2][sll+2],
                        lastOuterLayer[tll][sll+2],
                        outerLayer[t+2][s+2], lastOuterLayer[tll+1][sll],
                        lastOuterLayer[tll+2][sll+1],
                        lastOuterLayer[tll+1][sll+2],
                        lastOuterLayer[tll][sll+1],
                        innerLayer[tl+1][sl+1], innerLayer[tl+2][sl+1],
                        innerLayer[tl+2][sl+2], innerLayer[tl+1][sl+2],
                        lastOuterLayer[tll+1][sll+1],
                        !even);
                     
                     // tets below and right
                     model.addElement (new QuadtetElement(
                        outerLayer[t+2][s], outerLayer[t+2][s+2],
                        lastOuterLayer[tll+2][sll], lastOuterLayer[tll][sll],
                        outerLayer[t+2][s+1], innerLayer[tl+2][sl+1],
                        innerLayer[tl+2][sl], innerLayer[tl+1][sl],
                        innerLayer[tl+1][sl+1], lastOuterLayer[tll+1][sll]));
                     model.addElement (new QuadtetElement(
                        outerLayer[t+2][s+2], outerLayer[t][s+2],
                        lastOuterLayer[tll][sll+2], lastOuterLayer[tll][sll],
                        outerLayer[t+1][s+2], innerLayer[tl][sl+2],
                        innerLayer[tl+1][sl+2], innerLayer[tl+1][sl+1],
                        innerLayer[tl][sl+1], lastOuterLayer[tll][sll+1]));
                  }
                  // last column 
                  {
                     // pyramid down
                     int s = 2*((gs+1)*k-1);
                     int sl = s-gs;
                     int sll = s-2*gs;
                     boolean even = ((t/2 + k + 1 ) % 2) == 0;
                     addPyramidQuadTesselation (
                        model, 
                        outerLayer[t][s], outerLayer[t][s+2],
                        outerLayer[t+2][s+2], outerLayer[t+2][s], 
                        lastOuterLayer[tll][sll], outerLayer[t][s+1],
                        outerLayer[t+1][s+2], outerLayer[t+2][s+1],
                        outerLayer[t+1][s], innerLayer[tl][sl],
                        innerLayer[tl][sl+1], innerLayer[tl+1][sl+1],
                        innerLayer[tl+1][sl], outerLayer[t+1][s+1],
                        even);
                     
                     // tet below
                     model.addElement (new QuadtetElement(
                        outerLayer[t+2][s], outerLayer[t+2][s+2],
                        lastOuterLayer[tll+2][sll], lastOuterLayer[tll][sll],
                        outerLayer[t+2][s+1], innerLayer[tl+2][sl+1],
                        innerLayer[tl+2][sl], innerLayer[tl+1][sl],
                        innerLayer[tl+1][sl+1], lastOuterLayer[tll+1][sll]));
                  }
                  
               }
            }
            
            // last row
            {
               int i=k-1;
               int t = 2*(gt*k+i);
               int tl = t-gt;
               int tll = t-2*gt;
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  
                  // first k-1 columns
                  for (int j=0; j<k-1; ++j) {
                     int s = 2*(gs*k+j);
                     int sl = s-gs;
                     int sll = s-2*gs;
                     
                     // parity
                     boolean even = ((t/2 + j) % 2) == 0;
                     
                     // pyramids down
                     addPyramidQuadTesselation (
                        model,
                        outerLayer[t][s], outerLayer[t][s+2],
                        outerLayer[t+2][s+2], outerLayer[t+2][s], 
                        lastOuterLayer[tll][sll], outerLayer[t][s+1],
                        outerLayer[t+1][s+2], outerLayer[t+2][s+1],
                        outerLayer[t+1][s], innerLayer[tl][sl],
                        innerLayer[tl][sl+1], innerLayer[tl+1][sl+1],
                        innerLayer[tl+1][sl], outerLayer[t+1][s+1],
                        even);
                     
                     // tets right
                     model.addElement (new QuadtetElement(
                        outerLayer[t+2][s+2], outerLayer[t][s+2],
                        lastOuterLayer[tll][sll+2], lastOuterLayer[tll][sll],
                        outerLayer[t+1][s+2], innerLayer[tl][sl+2],
                        innerLayer[tl+1][sl+2], innerLayer[tl+1][sl+1],
                        innerLayer[tl][sl+1], lastOuterLayer[tll][sll+1]));
                  }
                  // last column 
                  {
                     int s = 2*((gs+1)*k - 1);
                     int sl = s - gs;
                     int sll = s - 2*gs;
                     // parity
                     boolean even = ((t/2 + k + 1) % 2) == 0;
                     // pyramid down
                     addPyramidQuadTesselation (
                        model, 
                        outerLayer[t][s], outerLayer[t][s+2],
                        outerLayer[t+2][s+2], outerLayer[t+2][s], 
                        lastOuterLayer[tll][sll], outerLayer[t][s+1],
                        outerLayer[t+1][s+2], outerLayer[t+2][s+1],
                        outerLayer[t+1][s], innerLayer[tl][sl],
                        innerLayer[tl][sl+1], innerLayer[tl+1][sl+1],
                        innerLayer[tl+1][sl], outerLayer[t+1][s+1],
                        even);
                     
                  }
               }
            }
         }
         
         // last group shrinks
         {
            int gt = ngt-1;
            for (int i=0; i<k; ++i) {
               int t = 2*(gt*k+i);
               int tl = t-gt;
               int tll = t-2*gt;
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=0; j<k-i-1; ++j) {
                     int s = 2*(gs*(k-i)+j);
               
                     int sd = s-gs;
                     int sdd = s-2*gs;
                     int sl = s-gs;
                     int sll = s-2*gs;
                     int sld = sl-gs;
                     int sldd = sl-2*gs;
                     int slld = sll-gs;
                     int slldd = sll-2*gs;

                     // downward tet
                     model.addElement (
                        new QuadtetElement(
                           outerLayer[t][s], outerLayer[t][s+2],
                           outerLayer[t+2][sdd], lastOuterLayer[tll][sll],
                           outerLayer[t][s+1], outerLayer[t+1][sd+1],
                           outerLayer[t+1][sd], innerLayer[tl][sl],
                           innerLayer[tl][sl+1], innerLayer[tl+1][sld]));
                     
                     // flipped 4-tet
                     addFlippedTriQuadTessellation (
                        model, 
                        outerLayer[t][s+2], outerLayer[t+2][sdd+2],
                        outerLayer[t+2][sdd], lastOuterLayer[tll][sll],
                        lastOuterLayer[tll][sll+2],
                        lastOuterLayer[tll+2][slldd],
                        outerLayer[t+1][sd+2], outerLayer[t+2][sdd+1],
                        outerLayer[t+1][sd+1], lastOuterLayer[tll][sll+1],
                        lastOuterLayer[tll+1][slld+1],
                        lastOuterLayer[tll+1][slld],
                        innerLayer[tl][sl+1], innerLayer[tl][sl+2], 
                        innerLayer[tl+1][sld+2], innerLayer[tl+2][sldd+1],
                        innerLayer[tl+2][sldd], innerLayer[tl+1][sld],
                        innerLayer[tl+1][sld+1]
                        );
                  }
                  int s = 2*((gs+1)*(k-i)-1);
                  int sd = s-gs;
                  int sdd = s-2*gs;
                  int sl = s-gs;
                  int sld = sl-gs;
                  int sll = s-2*gs;
                  
                  // downward tet
                  model.addElement (new QuadtetElement(
                     outerLayer[t][s], outerLayer[t][s+2],
                     outerLayer[t+2][sdd], lastOuterLayer[tll][sll],
                     outerLayer[t][s+1], outerLayer[t+1][sd+1],
                     outerLayer[t+1][sd], innerLayer[tl][sl],
                     innerLayer[tl][sl+1], innerLayer[tl+1][sld]
                     ));
               }
            }
            
            // upward tets connect to interior group nodes
            for (int i=1; i<k-1; ++i) {
               int t = 2*(gt*k+i);
               int tl = t-gt;
               int tll = t-2*gt;
               
               // longitude groups
               for (int gs=0; gs<ngs; ++gs) {
                  for (int j=1; j<k-i; ++j) {
                     int s = 2*(gs*(k-i)+j);
                     int sl = s-gs;
                     int slu = sl+gs;
                     int sll = s-2*gs;
                     int sllu = sll+gs;
                     int slluu = sll+2*gs;
                     
                     model.addElement (new QuadtetElement (
                        lastOuterLayer[tll][sll], lastOuterLayer[tll-2][slluu],
                        lastOuterLayer[tll][sll-2], outerLayer[t][s],
                        lastOuterLayer[tll-1][sllu],
                        lastOuterLayer[tll-1][sllu-1],
                        lastOuterLayer[tll][sll-1],
                        innerLayer[tl][sl],
                        innerLayer[tl-1][slu], innerLayer[tl][sl-1]));
                  }
               }
            }
         }
      }
      
      return model;
   }
   
   /**
    * Creates a hex sphere by first generating a regular grid, then mapping it
    * to the sphere using a volume-preserving bi-lipschitz projection
    * 
    * @param fem model to populate
    * @param r radius
    * @param nr number of elements radially along each axis from the sphere
    * center
    * @return populated model
    */
   public static FemModel3d createHexSphere(FemModel3d fem, double r, int nr) {
      if (fem == null) {
         fem = new FemModel3d();
      }
      
      FemFactory.createHexGrid (fem, 2, 2, 2, 2*nr, 2*nr, 2*nr);
      Point3d pnt3d = new Point3d();
      
      // map nodes
      for (FemNode3d node : fem.getNodes ()) {
         Point3d pos = node.getRestPosition ();
         
         // map x-y to conformal unit circle
         pnt3d.x = pos.x;
         pnt3d.y = pos.y;
         pnt3d.z = pos.z;
         conformalMapRectangleEllipse (1, 1, 1, 1, pnt3d, pnt3d);
         
         // map disc to sphere
         bilipschitzMapCylinderSphere (pnt3d, pnt3d);
         
         // scale to appropriate radius
         pnt3d.scale (r);
         node.setRestPosition (pnt3d);
         node.setPosition (pnt3d);
         
      }
      
      return fem;
   }
   

   /**
    * Creates an ellipsoidal model using a combination of hex, wedge, and tet
    * elements. The model is created symmetrically about a central polar axis,
    * using wedge elements at the core.  <code>rl</code> should be the longest
    * radius, and corresponds to the polar axis.
    *
    * @param model empty FEM model to which elements are added; if
    * <code>null</code> then a new model is allocated
    * @param rl longest radius (also the polar radius)
    * @param rs1 first radius perpendicular to the polar axis
    * @param rs2 second radius perpendicular to the polar axis
    * @param nt number of nodes in each ring parallel to the equator
    * @param nl number of nodes in each quarter ring perpendicular to the
    * equator (including end nodes)
    * @param ns number of nodes in each radial line extending out from
    * the polar axis (including end nodes)
    * @return the FEM model (which will be <code>model</code> if
    * <code>model</code> is not <code>null</code>).
    */
   public static FemModel3d createEllipsoid(
      FemModel3d model, 
      double rl, double rs1, double rs2, int nt, int nl, int ns) {

      double dl = Math.PI / (2 * nl - 2);
      double dt = 2 * Math.PI / nt;
      double dr = 1.0 / (ns - 1);

      FemNode3d nodes[][][] = new FemNode3d[nt][2 * nl - 1][ns];
      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }

      // generate nodes
      for (int k = 0; k < ns; k++) {
         for (int j = 0; j < 2 * nl - 1; j++) {

            if (k == 0) {
               FemNode3d node =
                  new FemNode3d(new Point3d(0, 0, -rl + 2 * rl * dl * j
                     / Math.PI));

               // System.out.println(node.getPosition());

               for (int i = 0; i < nt; i++) {
                  nodes[i][j][k] = node;
               }
               model.addNode(node);
            } else {
               if (j == 0) {
                  for (int i = 0; i < nt; i++) {
                     nodes[i][j][k] = nodes[i][j][0];
                  }
               } else if (j == 2 * nl - 2) {
                  for (int i = 0; i < nt; i++) {
                     nodes[i][j][k] = nodes[i][j][0];
                  }
               } else {
                  for (int i = 0; i < nt; i++) {
                     
                     // // XXX inverted elements at poles for large r
                     // double kInterp =
                     //    Math.pow(((double)k)/(ns-1), 2)*(rs1 + rs2)/(2*rl);
                     // double l =
                     //       (-rl + 2 * rl * dl * j / Math.PI) * (1 - kInterp)
                     //       + (-rl * Math.cos(j * dl)) * (kInterp);        
                     // double rAdj = dr * k * Math.sqrt(1 - l * l / rl / rl);
                     
                     // need kInterp(0) = 0, kInterp(ns-1) = 1
                     double a = (double)k/(ns-1);
                     double kInterp = 1-Math.sqrt (1-a*a);
                     // interpolate between axis and arc
                     double l =
                     (-rl + j*2.0*rl/(2*nl-2)) * (1 - kInterp)
                     + (-rl * Math.cos(j*Math.PI/(2*nl-2))) * (kInterp);
                     // linearly interpolate radius scale factor
                     kInterp = a;
                     // dr * k * Math.sqrt(1 - l * l / rl / rl); 
                     double rAdj = kInterp*Math.sin (j*Math.PI/(2*nl-2)); 
                     
                     nodes[i][j][k] =
                        new FemNode3d(
                           new Point3d(-rs1 * rAdj * Math.sin(dt * i), rs2
                              * rAdj * Math.cos(dt * i), l));
                     model.addNode(nodes[i][j][k]);
                     // System.out.println(nodes[i][j][k].getPosition());
                  }
               }
            }
         }
      }

      FemNode3d[] node8List =
         new FemNode3d[8]; // storing 8 nodes, repeated or not

      // generate elements
      for (int k = 0; k < ns - 1; k++) {
         for (int j = 0; j < 2 * nl - 2; j++) {
            for (int i = 0; i < nt; i++) {

               node8List[0] = nodes[i][j][k];
               node8List[1] = nodes[(i + 1) % nt][j][k];
               node8List[2] = nodes[(i + 1) % nt][j + 1][k];
               node8List[3] = nodes[i][j + 1][k];
               node8List[4] = nodes[i][j][k + 1];
               node8List[5] = nodes[(i + 1) % nt][j][k + 1];
               node8List[6] = nodes[(i + 1) % nt][j + 1][k + 1];
               node8List[7] = nodes[i][j + 1][k + 1];

               FemElement3d elem = createElem(node8List);
               model.addElement(elem);
            }
         }
      }

      return model;
   }
   
   /**
    * Creates an ellipsoidal model using tet elements. The model is created
    * symmetrically about a central polar axis, using tesselated wedge elements
    * at the core.  <code>rl</code> should be the longest radius, and
    * corresponds to the polar axis.
    *
    * @param model empty FEM model to which elements are added; if
    * <code>null</code> then a new model is allocated
    * @param rl longest radius (also the polar radius)
    * @param rs1 first radius perpendicular to the polar axis
    * @param rs2 second radius perpendicular to the polar axis
    * @param nt number of nodes in each ring parallel to the equator
    * @param nl number of nodes in each quarter ring perpendicular to the
    * equator (including end nodes)
    * @param ns number of nodes in each radial line extending out from
    * the polar axis (including end nodes)
    * @return the FEM model (which will be <code>model</code> if
    * <code>model</code> is not <code>null</code>).
    */
   public static FemModel3d createTetEllipsoid(
      FemModel3d model, 
      double rl, double rs1, double rs2, int nt, int nl, int ns) {

      if (ns < 1) {
         ns = 1;
      }
      nt += (nt % 2);  // make nt even
      
      double dl = Math.PI / (2 * nl - 2);
      double dt = 2 * Math.PI / nt;
      double dr = 1.0 / (ns - 1);

      FemNode3d nodes[][][] = new FemNode3d[nt][2 * nl - 1][ns];
      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }

      // generate nodes
      for (int k = 0; k < ns; k++) {
         for (int j = 0; j < 2 * nl - 1; j++) {

            if (k == 0) {
               FemNode3d node =
                  new FemNode3d(new Point3d(0, 0, -rl + 2 * rl * dl * j
                     / Math.PI));
               
               for (int i = 0; i < nt; i++) {
                  nodes[i][j][k] = node;
               }
               model.addNode(node);
            } else {
               if (j == 0) {
                  for (int i = 0; i < nt; i++) {
                     nodes[i][j][k] = nodes[i][j][0];
                  }
               } else if (j == 2 * nl - 2) {
                  for (int i = 0; i < nt; i++) {
                     nodes[i][j][k] = nodes[i][j][0];
                  }
               } else {
                  for (int i = 0; i < nt; i++) {
                     double kInterp =
                        Math.pow(((double)k) / (ns - 1), 2) * (rs1 + rs2)
                           / (2 * rl);
                     double l =
                        (-rl + 2 * rl * dl * j / Math.PI) * (1 - kInterp)
                           + (-rl * Math.cos(j * dl)) * (kInterp);
                     double rAdj = dr * k * Math.sqrt(1 - l * l / rl / rl);
                     nodes[i][j][k] =
                        new FemNode3d(
                           new Point3d(-rs1 * rAdj * Math.sin(dt * i), rs2
                              * rAdj * Math.cos(dt * i), l));
                     model.addNode(nodes[i][j][k]);
                  }
               }
            }
         }
      }

      FemNode3d[] node8List =
         new FemNode3d[8]; // storing 8 nodes, repeated or not

      // generate elements
      for (int k = 0; k < ns - 1; k++) {
         for (int j = 0; j < 2 * nl - 2; j++) {
            for (int i = 0; i < nt; i++) {

               boolean even = ((i+j+k) % 2) == 0;
               node8List[0] = nodes[i][j][k];
               node8List[1] = nodes[(i + 1) % nt][j][k];
               node8List[2] = nodes[(i + 1) % nt][j + 1][k];
               node8List[3] = nodes[i][j + 1][k];
               node8List[4] = nodes[i][j][k + 1];
               node8List[5] = nodes[(i + 1) % nt][j][k + 1];
               node8List[6] = nodes[(i + 1) % nt][j + 1][k + 1];
               node8List[7] = nodes[i][j + 1][k + 1];
               
               TetElement elems[] =
                  TetElement.createCubeTesselation (
                     node8List[0], node8List[3], node8List[2], node8List[1], 
                     node8List[4], node8List[7], node8List[6], node8List[5],
                     even);
               
               // only add non-degenerate elements
               for (TetElement elem : elems) {
                  FemNode3d[] enodes = elem.getNodes ();
                  if (enodes[0] != enodes[1] && enodes[0] != enodes[2] &&
                      enodes[0] != enodes[3] && enodes[1] != enodes[2] &&
                      enodes[1] != enodes[3] && enodes[2]!= enodes[3]) {
                     model.addElement (elem);
                  }
               }
               
            }
         }
      }

      return model;
   }
   
   /**
    * Creates a ellipsoidal model using a hex elements by first
    * creating a hex sphere and then scaling it.
    *
    * @param model empty FEM model to which elements are added; if
    * <code>null</code> then a new model is allocated
    * @param rz longest radius (also the polar radius)
    * @param rsx first radius perpendicular to the polar axis
    * @param rsy second radius perpendicular to the polar axis
    * @param nr number of nodes in each radial line extending out from
    * the polar axis (including end nodes)
    * @return the FEM model (which will be <code>model</code> if
    * <code>model</code> is not <code>null</code>).
    */
   public static FemModel3d createHexEllipsoid(
      FemModel3d model, 
      double rz, double rsx, double rsy, int nr) {
   
      model = createHexSphere (model, 1, nr);
      for (FemNode3d node : model.getNodes ()) {
         Point3d pos = node.getRestPosition ();
         pos.scale (rsx, rsy, rz);
         node.setRestPosition (pos);
         node.setPosition (pos);
      }
      
      return model;
   }
   
   /**
    * Creates a ellipsoidal model using a tet elements by first
    * creating a tet sphere and then scaling it.
    *
    * @param model empty FEM model to which elements are added; if
    * <code>null</code> then a new model is allocated
    * @param rz longest radius (also the polar radius)
    * @param rsx first radius perpendicular to the polar axis
    * @param rsy second radius perpendicular to the polar axis
    * @param nr number of nodes in each radial line extending out from
    * the polar axis (including end nodes)
    * @return the FEM model (which will be <code>model</code> if
    * <code>model</code> is not <code>null</code>).
    */
   public static FemModel3d createTetEllipsoid(
      FemModel3d model, 
      double rz, double rsx, double rsy, int nr) {
   
      model = createTetSphere (model, 1, nr);
      for (FemNode3d node : model.getNodes ()) {
         Point3d pos = node.getRestPosition ();
         pos.scale (rsx, rsy, rz);
         node.setRestPosition (pos);
         node.setPosition (pos);
      }
      
      return model;
   }
   
   /**
    * Creates a cylinder made of mostly hex elements, with wedges in the centre
    * column.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param l length along the z axis
    * @param r radius in the x-y plane
    * @param nt element resolution around the center axis
    * @param nl element resolution along the length
    * @param nr element resolution along the radius
    * @return created FEM model
    */
   public static FemModel3d createCylinder(
      FemModel3d model, double l, double r, int nt, int nl, int nr) {

      return createHexWedgeCylinder(model, l, r, nt, nl, nr);
   }
   
   /**
    * Creates a cylinder made of mostly hex elements, with wedges in the centre
    * column. Identical to {@link 
    * #createCylinder(FemModel3d,double,double,int,int,int)}.
    */  
   public static FemModel3d createHexWedgeCylinder(
      FemModel3d model, double l, double r, int nt, int nl, int nr) {

      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }

      FemNode3d nodes[][][] = new FemNode3d[nt][nl+1][nr+1];

      double dl = l / nl;
      double dt = 2 * Math.PI / nt;
      double dr = 1.0 / nr;

      // generate nodes
      for (int k = 0; k < nr+1; k++) {
         for (int j = 0; j < nl+1; j++) {

            if (k == 0) {
               FemNode3d node =
                  new FemNode3d(new Point3d(0, 0, -l / 2 + j * dl));
               for (int i = 0; i < nt; i++) {
                  nodes[i][j][k] = node;
               }
               model.addNode(node);
            } else {
               for (int i = 0; i < nt; i++) {
                  double rr = r * Math.pow(dr * k, 0.7);
                  nodes[i][j][k] =
                     new FemNode3d(new Point3d(-rr * Math.sin(dt * i), rr
                        * Math.cos(dt * i), -l / 2 + j * dl));
                  model.addNode(nodes[i][j][k]);
               }
            }
         }
      }

      // generate elements
      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {

               if (k == 0) {
                  // wedge element
                  WedgeElement wedge =
                     new WedgeElement(
                        nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                        nodes[i][j][k],

                        nodes[i][j + 1][k + 1],
                        nodes[(i + 1) % nt][j + 1][k + 1], nodes[i][j + 1][k]);
                  model.addElement(wedge);
               } else {
                  // hex element
                  HexElement hex =
                     new HexElement(
                        nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                        nodes[(i + 1) % nt][j + 1][k + 1],
                        nodes[i][j + 1][k + 1],

                        nodes[i][j][k], nodes[(i + 1) % nt][j][k],
                        nodes[(i + 1) % nt][j + 1][k], nodes[i][j + 1][k]);
                  model.addElement(hex);
               }

            }
         }
      }

      return model;
   }
   
   /**
    * Creates a tetrahedral cylinder by tetrahedralizing a hex-wedge cylinder
    * 
    * @param fem model to populate
    * @param l length of cylinder (z-axis)
    * @param r radius of cylinder
    * @param nt number of elements around the arc
    * @param nl number of elements along the length
    * @param nr number of elements radially
    * @return populated model
    */
   public static FemModel3d createTetCylinder (
      FemModel3d fem, double l, double r, int nt, int nl, int nr) {
      if (fem == null) {
         fem = new FemModel3d();
      }

      // round nt up to even to allow proper tesselation
      if ((nt % 2) == 1) {
         nt++;
      }
      // HexModel model = new HexModel();

      FemNode3d nodes[][][] = new FemNode3d[nt][nl+1][nr+1];

      double dl = l / nl;
      double dt = 2 * Math.PI / nt;
      double dr = r / nr;

      // height
      for (int j = 0; j < nl+1; j++) {
         
         // centre
         nodes[0][j][0] = new FemNode3d(new Point3d(
            0, 0, -l / 2 + j * dl));
         fem.addNode (nodes[0][j][0]);
         
         // radius
         for (int k = 1; k < nr+1; k++) {
            // angle
            for (int i = 0; i < nt; i++) {
               nodes[i][j][k] =
                  new FemNode3d(new Point3d(
                     -(dr * k) * Math.sin(dt * i), (dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));
               fem.addNode(nodes[i][j][k]);
            }
         }
      }

      TetElement elems[][][][] = new TetElement[nt][nl][nr][];

      for (int j = 0; j < nl; j++) {
         
         // k = 0, wedges
         for (int i = 0; i < nt; i++) {
            
            boolean even = (i + j) % 2 == 0;
            
            FemNode3d p0 = nodes[i][j][1];
            FemNode3d p1 = nodes[(i + 1) % nt][j][1];
            FemNode3d p2 = nodes[(i + 1) % nt][j + 1][1];
            FemNode3d p3 = nodes[i][j + 1][1];
            FemNode3d p4 = nodes[0][j][0];
            FemNode3d p5 = nodes[0][j + 1][0]; 
            
            elems[i][j][0] = new TetElement[3];
            if (even) {
               elems[i][j][0][0] = new TetElement (p0, p1, p4, p3);
               elems[i][j][0][1] = new TetElement (p2, p3, p5, p1);
               elems[i][j][0][2] = new TetElement (p1, p5, p4, p3);
            } else {
               elems[i][j][0][0] = new TetElement (p1, p0, p2, p4);
               elems[i][j][0][1] = new TetElement (p3, p0, p5, p2);
               elems[i][j][0][2] = new TetElement (p0, p2, p4, p5);
            }
            
            fem.addElement(elems[i][j][0][0]);
            fem.addElement(elems[i][j][0][1]);
            fem.addElement(elems[i][j][0][2]);
         }
         
         // hexes
         for (int k = 1; k < nr; k++) {
            for (int i = 0; i < nt; i++) {
               elems[i][j][k] =
                  TetElement.createCubeTesselation(
                     nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                     nodes[(i + 1) % nt][j + 1][k + 1], nodes[i][j + 1][k + 1],
                     nodes[i][j][k], nodes[(i + 1) % nt][j][k], nodes[(i + 1)
                        % nt][j + 1][k], nodes[i][j + 1][k],
                     (i + j + k) % 2 == 0);
               
               fem.addElement(elems[i][j][k][0]);
               fem.addElement(elems[i][j][k][1]);
               fem.addElement(elems[i][j][k][2]);
               fem.addElement(elems[i][j][k][3]);
               fem.addElement(elems[i][j][k][4]);
            }
         }
      }
      
      return fem;
   }
   
   /**
    * Creates a quadratic tetrahedral cylinder by tetrahedralizing a hex-wedge
    * cylinder
    * 
    * @param fem model to populate
    * @param l length of cylinder (z-axis)
    * @param r radius of cylinder
    * @param nt number of elements around the arc
    * @param nl number of elements along the length
    * @param nr number of elements radially
    * @return populated model
    */
   public static FemModel3d createQuadtetCylinder (
      FemModel3d fem, double l, double r, int nt, int nl, int nr) {
      if (fem == null) {
         fem = new FemModel3d();
      }

      // round nt up to even to allow proper tesselation
      if ((nt % 2) == 1) {
         nt++;
      }
      // HexModel model = new HexModel();

      FemNode3d nodes[][][] = new FemNode3d[2*nr+1][2*nt][2*nl+1];

      double dl = l / nl / 2;
      double dt = Math.PI / nt;
      double dr = r / nr / 2;

      // height
      for (int j = 0; j < 2*nl+1; j++) {
         
         // centre
         nodes[0][0][j] = new FemNode3d(new Point3d(
            0, 0, -l / 2 + j * dl));
         fem.addNode (nodes[0][0][j]);
         
         for (int k=1; k<2*nr+1; ++k) {
            
            // angle inner ring, skip middle if 
            //   t odd && (k == 1 || j odd)
            int tinc = 1;
            if ( (k == 1) || (((k % 2) == 1) && ((j % 2) == 1)) ) {
               tinc = 2;
            }
            
            for (int i = 0; i < 2*nt; i+=tinc) {
               nodes[k][i][j] =
               new FemNode3d(new Point3d(
                  -(dr * k) * Math.sin(dt * i), (dr * k)
                  * Math.cos(dt * i), -l / 2 + j * dl));
               fem.addNode(nodes[k][i][j]);
            }
         }
      }

      QuadtetElement elems[][][][] = new QuadtetElement[nr][nt][nl][];

      for (int j = 0; j < nl; j++) {

         // k = 0, wedges
         for (int i = 0; i < nt; ++i) {

            boolean even = (((i+j) % 2) == 0);

            int ni = 2*i;
            int nin = (ni+1) % (2*nt);
            int ninn = (ni+2) % (2*nt);
            int nj = 2*j;
            
            FemNode3d[] p0 = nodes[0][0];
            
            FemNode3d[] p1 = nodes[1][ni];
            FemNode3d[] p2 = nodes[1][ninn];
            
            FemNode3d[] p3 = nodes[2][ni];
            FemNode3d[] p4 = nodes[2][nin];
            FemNode3d[] p5 = nodes[2][ninn];
                        
            elems[0][i][j] = new QuadtetElement[3]; 
            if (even) {
               elems[0][i][j][0] =
                  new QuadtetElement (
                     p0[nj], p3[nj], p5[nj], p3[nj+2], p1[nj],
                     p4[nj], p2[nj], p1[nj+1], p3[nj+1], p4[nj+1]);
               elems[0][i][j][1] =
                  new QuadtetElement (
                     p3[nj+2], p0[nj+2], p5[nj+2], p5[nj], p1[nj+2],
                     p2[nj+2], p4[nj+2], p4[nj+1], p2[nj+1], p5[nj+1]);
               elems[0][i][j][2] =
                  new QuadtetElement (
                     p0[nj], p0[nj+2], p3[nj+2], p5[nj], p0[nj+1],
                     p1[nj+2], p1[nj+1], p2[nj], p2[nj+1], p4[nj+1]);
            } else {
               elems[0][i][j][0] =
                  new QuadtetElement (
                     p0[nj], p3[nj], p5[nj], p5[nj+2], p1[nj],
                     p4[nj], p2[nj], p2[nj+1], p4[nj+1], p5[nj+1]);
               elems[0][i][j][1] =
                  new QuadtetElement (
                     p3[nj+2], p0[nj+2], p5[nj+2], p3[nj], p1[nj+2],
                     p2[nj+2], p4[nj+2], p3[nj+1], p1[nj+1], p4[nj+1]);
               elems[0][i][j][2] =
                  new QuadtetElement (
                     p0[nj], p5[nj+2], p0[nj+2], p3[nj], p2[nj+1],
                     p2[nj+2], p0[nj+1], p1[nj], p4[nj+1], p1[nj+1]);
            }

            fem.addElement(elems[0][i][j][0]);
            fem.addElement(elems[0][i][j][1]);
            fem.addElement(elems[0][i][j][2]);
            
         }

         // hexes
         for (int k = 1; k < nr; k++) {
            for (int i = 0; i < nt; i++) {
               
               boolean even = (((i+j+k) % 2) == 0);
               elems[k][i][j] = new QuadtetElement[5];
               
               int ni = 2*i;
               int nin = (ni+1) % (2*nt);
               int ninn = (ni+2) % (2*nt);
               int nj = 2*j;
               int nk = 2*k;
               
               FemNode3d[] p0 = nodes[nk][ni];
               FemNode3d[] p1 = nodes[nk][nin];
               FemNode3d[] p2 = nodes[nk][ninn];
               
               FemNode3d[] p3 = nodes[nk+1][ni];
               FemNode3d[] p4 = nodes[nk+1][nin];
               FemNode3d[] p5 = nodes[nk+1][ninn];
               
               FemNode3d[] p6 = nodes[nk+2][ni];
               FemNode3d[] p7 = nodes[nk+2][nin];
               FemNode3d[] p8 = nodes[nk+2][ninn];
               
               elems[k][i][j] = new QuadtetElement[5]; 
               if (even) {
                  elems[k][i][j][0] =
                     new QuadtetElement (
                        p0[nj], p8[nj], p2[nj], p2[nj+2], p4[nj],
                        p5[nj], p1[nj], p1[nj+1], p5[nj+1], p2[nj+1]);
                  elems[k][i][j][1] =
                     new QuadtetElement (
                        p0[nj], p6[nj], p8[nj], p6[nj+2], p3[nj],
                        p7[nj], p4[nj], p3[nj+1], p6[nj+1], p7[nj+1]);
                  elems[k][i][j][2] =
                     new QuadtetElement (
                        p0[nj+2], p2[nj+2], p6[nj+2], p0[nj], p1[nj+2],
                        p4[nj+2], p3[nj+2], p0[nj+1], p1[nj+1], p3[nj+1]);
                  elems[k][i][j][3] = 
                     new QuadtetElement (
                        p8[nj+2], p6[nj+2], p2[nj+2], p8[nj], p7[nj+2],
                        p4[nj+2], p5[nj+2], p8[nj+1], p7[nj+1], p5[nj+1]);
                  elems[k][i][j][4] = 
                     new QuadtetElement (
                        p0[nj], p6[nj+2], p8[nj], p2[nj+2], p3[nj+1],
                        p7[nj+1], p4[nj], p1[nj+1], p4[nj+2], p5[nj+1]);
               } else {
                  elems[k][i][j][0] = 
                     new QuadtetElement (
                        p0[nj], p6[nj], p2[nj], p0[nj+2], p3[nj],
                        p4[nj], p1[nj], p0[nj+1], p3[nj+1], p1[nj+1]);
                  elems[k][i][j][1] = 
                     new QuadtetElement (
                        p2[nj], p6[nj], p8[nj], p8[nj+2], p4[nj],
                        p7[nj], p5[nj], p5[nj+1], p7[nj+1], p8[nj+1]);
                  elems[k][i][j][2] = 
                     new QuadtetElement (
                        p0[nj+2], p2[nj+2], p8[nj+2], p2[nj], p1[nj+2],
                        p5[nj+2], p4[nj+2], p1[nj+1], p2[nj+1], p5[nj+1]);
                  elems[k][i][j][3] = 
                     new QuadtetElement (
                        p0[nj+2], p8[nj+2], p6[nj+2], p6[nj], p4[nj+2],
                        p7[nj+2], p3[nj+2], p3[nj+1], p7[nj+1], p6[nj+1]);
                  elems[k][i][j][4] = 
                     new QuadtetElement (
                        p2[nj], p6[nj], p8[nj+2], p0[nj+2], p4[nj],
                        p7[nj+1], p5[nj+1], p1[nj+1], p3[nj+1], p4[nj+2]);
               }
               
               fem.addElement (elems[k][i][j][0]);
               fem.addElement (elems[k][i][j][1]);
               fem.addElement (elems[k][i][j][2]);
               fem.addElement (elems[k][i][j][3]);
               fem.addElement (elems[k][i][j][4]);
            }
         }
      }
      
      return fem;
   }
   
   /**
    * Creates a cylinder made entirely of wedges, all approximately uniform in
    * size
    * @param model model to populate, created if null
    * @param h height of cylinder (z-axis)
    * @param r radius of cylinder
    * @param nh element resolution in z
    * @param nr radial element resolution
    * @return populated or created model
    */
   public static FemModel3d createWedgeCylinder (
      FemModel3d model, double h, double r, int nh, int nr) {
      if (model == null) {
         model = new FemModel3d();
      }
      
      if (nr < 1) {
         nr = 1;
      }
      double dr = r/nr;
      
      double zmin = -h/2;
      double dh = h/nh;
      FemNode3d[][] layer = new FemNode3d[nh+1][1];
      FemNode3d[][] lastLayer;
      
      // axis nodes
      for (int k=0; k<=nh; ++k) {
         FemNode3d node = new FemNode3d(0, 0, zmin+k*dh);
         layer[k][0] = node;
         model.addNode (node);
      }
      
      int ngroups = 6;  // even split around circle
      
      // circle nodes
      for (int i=1; i<=nr; ++i) {
         
         // next layer of nodes
         double rr = dr*i;
         lastLayer = layer;
         int nt = ngroups*i;
         double dt = 2*Math.PI/nt;
         layer = new FemNode3d[nh+1][nt];
         for (int k=0; k<=nh; ++k) {
            double z = zmin+k*dh;
            for (int j=0; j<nt; ++j) {
               double theta = j*dt;
               double x = rr*Math.cos (theta);
               double y = rr*Math.sin (theta);
               FemNode3d node = new FemNode3d(x, y, z);
               layer[k][j] = node;
               model.addNode (node);
            }
         }
            
         // layer of elements
         for (int k=0; k<nh; ++k) {
            for (int g=0; g<ngroups; ++g) {
               int j = (g*i);
               int pj = g*(i-1);
               for (int t=0; t<i-1; ++t) {
                  int nextpj = (pj+1) % (nt-ngroups);
                  int nextj = (j+1) % nt;
                  
                  model.addElement (
                     new WedgeElement (
                        layer[k][j], layer[k][nextj], lastLayer[k][pj],
                        layer[k+1][j], layer[k+1][nextj], lastLayer[k+1][pj]));
                  model.addElement (
                     new WedgeElement(
                        lastLayer[k][nextpj], lastLayer[k][pj], layer[k][nextj],
                        lastLayer[k+1][nextpj], lastLayer[k+1][pj],
                        layer[k+1][nextj]));
                  
                  j = nextj;
                  pj = nextpj;
               }
               int nextj = (j+1) % nt;
               
               model.addElement (
                  new WedgeElement(
                     layer[k][j], layer[k][nextj], lastLayer[k][pj],
                     layer[k+1][j], layer[k+1][nextj], lastLayer[k+1][pj]));
            }
         }
      }
         
      return model;
   }
   
   /**
    * Tessellate a wedge, adding tets to model
    * @param model model to add tets to
    * @param n0 wedge node 0
    * @param n1 wedge node 1
    * @param n2 wedge node 2
    * @param n3 wedge node 3
    * @param n4 wedge node 4
    * @param n5 wedge node 5
    * @param code code to indicate splitting of wedge (1-6), @see {@link
    * TetElement#createWedgeTesselation(FemNode3d,FemNode3d,FemNode3d,FemNode3d, FemNode3d,FemNode3d,int)}
    */
   private static void addWedgeTessellation (
      FemModel3d model, FemNode3d n0, FemNode3d n1, FemNode3d n2, 
      FemNode3d n3, FemNode3d n4, FemNode3d n5, int code) {
      // wedge is ordered opposite orientation
      TetElement[] elems =
         TetElement.createWedgeTesselation (n0, n2, n1, n3, n5, n4, code);
      for (TetElement elem : elems) {
         model.addElement (elem);
      }
   }
   
   /**
    * Tessellate a wedge with quadratic tets
    */
   private static void addQuadwedgeTessellation (
      FemModel3d model, 
      FemNode3d n0, FemNode3d n1, FemNode3d n2, 
      FemNode3d n3, FemNode3d n4, FemNode3d n5, 
      FemNode3d n01, FemNode3d n7, FemNode3d n8,     // m01, m12, m20
      FemNode3d n34, FemNode3d n45, FemNode3d n53,   // m34, m45, m53
      FemNode3d n03, FemNode3d n14, FemNode3d n15,  // m03, m14, m15
      FemNode3d n0134, FemNode3d n1245,
      FemNode3d n2053, // mid-quad: m0134, m1245, m2053 
      int code) {
      // wedge is ordered opposite orientation
      QuadtetElement[] elems = new QuadtetElement[3];
      
      // switch around for consistency with TetElement.createWedgeTessellation
      FemNode3d p0 = n0;
      FemNode3d p1 = n2;
      FemNode3d p2 = n1;
      FemNode3d p3 = n3;
      FemNode3d p4 = n5;
      FemNode3d p5 = n4;
      FemNode3d p6 = n8;
      FemNode3d p7 = n7;
      FemNode3d p8 = n01;
      FemNode3d p9 = n53;
      FemNode3d p10 = n45;
      FemNode3d p11 = n34;
      FemNode3d p12 = n03;
      FemNode3d p13 = n15;
      FemNode3d p14 = n14;
      FemNode3d p15 = n2053;
      FemNode3d p16 = n1245;
      FemNode3d p17 = n0134;
      
      switch (code) {
         case 0x1: { /* R, F, F */ 
            elems[0] = new QuadtetElement (
               p0, p2, p1, p3, p8, p7, p6, p12, p17, p15);
            elems[1] = new QuadtetElement (
               p2, p5, p1, p3, p14, p16, p7, p17, p11, p15);
            elems[2] = new QuadtetElement (
               p1, p5, p4, p3, p16, p10, p13, p15, p11, p9);
            break;
         }
         case 0x2: { /* F, R, F */ 
            elems[0] = new QuadtetElement (
               p0, p2, p1, p4, p8,  p7,  p6, p15, p16, p13);
            elems[1] = new QuadtetElement (
               p0, p3, p2, p4, p12, p17, p8, p15, p9, p16);
            elems[2] = new QuadtetElement (
               p2, p3, p5, p4, p17, p11, p14, p16, p9, p10);
            break;
         }
         case 0x3: { /* R, R, F */ 
            elems[0] = new QuadtetElement (
               p0, p2, p1, p3, p8, p7, p6, p12, p17, p15);
            elems[1] = new QuadtetElement (
               p1, p2, p4, p3, p7, p16, p13, p15, p17, p9);
            elems[2] = new QuadtetElement (
               p2, p5, p4, p3, p14, p10, p16, p17, p11, p9);
            break;
         }
         case 0x4: { /* F, F, R */ 
            elems[0] = new QuadtetElement (
               p0, p2, p1, p5, p8, p7, p6, p17, p14, p16);
            elems[1] = new QuadtetElement (
               p1, p4, p0, p5, p13, p15, p6, p16, p10, p17);
            elems[2] = new QuadtetElement (
               p0, p4, p3, p5, p15, p9, p12, p17, p10, p11);
            break;
         }
         case 0x5: { /* R, F, R */ 
            elems[0] = new QuadtetElement (
               p0, p2, p1, p5, p8, p7, p6, p17, p14, p16);
            elems[1] = new QuadtetElement (
               p0, p1, p3, p5, p6, p15, p12, p17, p16, p11);
            elems[2] = new QuadtetElement (
               p1, p4, p3, p5, p13, p9, p15, p16, p10, p11);
            break;
         }
         case 0x6: { /* F, R, R */ 
            elems[0] = new QuadtetElement (
               p0, p2, p1, p4, p8, p7, p6, p15, p16, p13);
            elems[1] = new QuadtetElement (
               p2, p0, p5, p4, p8, p17, p14, p16, p15, p10);
            elems[2] = new QuadtetElement (
               p0, p3, p5, p4, p12, p11, p17, p15, p9, p10);
            break;
         }
         default: {
            throw new IllegalArgumentException (
               "Illegal or unknown configuration type: " + code);
         }
      }
      
      for (QuadtetElement elem : elems) {
         model.addElement (elem);
      }
   }
   
   /**
    * Creates a cylinder of tets, all with approximately uniform size
    * @param model model to populate (if null, one is created)
    * @param h height of cylinder
    * @param r radius of cylinder
    * @param nh element resolution along height
    * @param nr radial element resolution
    * @return populated or created model
    */
   public static FemModel3d createTetCylinder (
      FemModel3d model, double h, double r, int nh, int nr) {
      if (model == null) {
         model = new FemModel3d();
      }
      
      if (nr < 1) {
         nr = 1;
      }
      double dr = r/nr;
      
      double zmin = -h/2;
      double dh = h/nh;
      FemNode3d[][] layer = new FemNode3d[nh+1][1];
      FemNode3d[][] lastLayer;
      
      // axis nodes
      for (int k=0; k<=nh; ++k) {
         FemNode3d node = new FemNode3d(0, 0, zmin+k*dh);
         layer[k][0] = node;
         model.addNode (node);
      }
      
      int ngroups = 6;  // even split around circle
      
      // way to split hex
      // int split = 0x02;  // options are 0b001, 0b010, 0b101, 0b110
      
      
      // circle nodes
      for (int i=1; i<=nr; ++i) {
         
         // next layer of nodes
         double rr = dr*i;
         lastLayer = layer;
         int nt = ngroups*i;
         double dt = 2*Math.PI/nt;
         layer = new FemNode3d[nh+1][nt];
         
         for (int k=0; k<=nh; ++k) {
            double z = zmin+k*dh;
            for (int j=0; j<nt; ++j) {
               double theta = j*dt;
               double x = rr*Math.cos (theta);
               double y = rr*Math.sin (theta);
               FemNode3d node = new FemNode3d(x, y, z);
               layer[k][j] = node;
               model.addNode (node);
            }
         }
            
         // layer of elements
         for (int k=0; k<nh; ++k) {
            for (int g=0; g<ngroups; ++g) {
               int j = (g*i);
               int pj = g*(i-1);
               for (int t=0; t<i-1; ++t) {
                  int nextpj = (pj+1) % (nt-ngroups);
                  int nextj = (j+1) % nt;
                  
                  int outerParity = (i + k + j) % 2;
                  int sideParity = (i + k) % 2;
                  int parity = outerParity*4 + sideParity+1;
                  
                  addWedgeTessellation (
                     model, layer[k][j], layer[k][nextj], lastLayer[k][pj],
                     layer[k+1][j], layer[k+1][nextj], lastLayer[k+1][pj],
                     parity);

                  outerParity = (i + k + pj) % 2;
                  sideParity = (i + k + 1) % 2;
                  parity = outerParity*4 + sideParity + 1;
                  addWedgeTessellation (
                     model, lastLayer[k][nextpj], lastLayer[k][pj],
                     layer[k][nextj], lastLayer[k+1][nextpj],
                     lastLayer[k+1][pj], layer[k+1][nextj], 
                     parity);
                  
                  j = nextj;
                  pj = nextpj;
               }
               int nextj = (j+1) % nt;

               int outerParity = (i + j + k) % 2;
               int sideParity = (i + k) % 2;
               int parity = outerParity*4 + sideParity+1;
               
               addWedgeTessellation (
                  model, layer[k][j], layer[k][nextj], lastLayer[k][pj],
                  layer[k+1][j], layer[k+1][nextj], lastLayer[k+1][pj],
                  parity);

            }
            // next height
         }
         // next radius
      }
         
      return model;
   }

   /**
    * Creates a cylinder of quadratic tets, all with approximately uniform size
    * @param model model to populate (if null, one is created)
    * @param h height of cylinder
    * @param r radius of cylinder
    * @param nh element resolution along height
    * @param nr radial element resolution
    * @return populated or created model
    */
   public static FemModel3d createQuadtetCylinder (
      FemModel3d model, double h, double r, int nh, int nr) {
      if (model == null) {
         model = new FemModel3d();
      }
      
      if (nh < 1) {
         nh = 1;
      }
      if (nr < 1) {
         nr = 1;
      }
      double dr = r/(2*nr);
      
      double zmin = -h/2;
      double dh = h/(2*nh);
      FemNode3d[][] outerlayer = new FemNode3d[2*nh+1][1];
      FemNode3d[][] lastOuterLayer;
      
      // axis nodes
      for (int k=0; k<=2*nh; ++k) {
         FemNode3d node = new FemNode3d(0, 0, zmin+k*dh);
         outerlayer[k][0] = node;
         model.addNode (node);
      }
      
      int ngroups = 6;  // even split around circle
      
      // circle nodes
      for (int i=1; i<=nr; ++i) {
         
         // next layer of nodes
         double ri = dr*(2*i-1);
         double ro = dr*(2*i);
         lastOuterLayer = outerlayer;
         int nit = ngroups*(2*i-1); // inner
         int not = ngroups*(2*i);   // outer
         int npt = ngroups*(2*i-2); // previous outer
         
         double dit = 2*Math.PI/nit;
         double dot = 2*Math.PI/not;
         
         outerlayer = new FemNode3d[2*nh+1][not];
         FemNode3d[][] innerlayer = new FemNode3d[2*nh+1][nit];
         
         // generate nodes
         for (int k=0; k<=2*nh; ++k) {
            double z = zmin+k*dh;
            
            for (int j=0; j<nit; ++j) {
               double theta = j*dit;
               double x = ri*Math.cos (theta);
               double y = ri*Math.sin (theta);
               FemNode3d node = new FemNode3d(x, y, z);
               innerlayer[k][j] = node;
               model.addNode (node);
            }
            
            for (int j=0; j<not; ++j) {
               double theta = j*dot;
               double x = ro*Math.cos (theta);
               double y = ro*Math.sin (theta);
               FemNode3d node = new FemNode3d(x, y, z);
               outerlayer[k][j] = node;
               model.addNode (node);
            }
         }
            
         // layer of elements
         for (int k=0; k<2*nh; k += 2) {
            int nnk = k+2;
            int nk = k+1;
            
            for (int g=0; g<ngroups; ++g) {
               int j = g*(2*i);      // outer group
               int mj = g*(2*i-1);   // inner group
               int pj = g*(2*i-2);   // last outer group
               
               for (int t=0; t<i-1; ++t) {
                  
                  int nnpj = (pj+2) % npt;
                  int npj = (pj+1) % npt;
                  
                  int nnmj = (mj+2) % nit;
                  int nmj = (mj+1) % nit;
                  
                  int nj = (j+1) % not;
                  int nnj = (j+2) % not;
                  
                  int outerParity = (i + k/2 + j/2) % 2;
                  int sideParity = (i + k/2) % 2;
                  int parity = outerParity*4 + sideParity+1;
                  
                  addQuadwedgeTessellation(
                     model, 
                     outerlayer[k][j], outerlayer[k][nnj],
                     lastOuterLayer[k][pj], outerlayer[nnk][j],
                     outerlayer[nnk][nnj], lastOuterLayer[nnk][pj], 
                     outerlayer[k][nj], innerlayer[k][nmj],
                     innerlayer[k][mj],           // bottom edges
                     outerlayer[nnk][nj], innerlayer[nnk][nmj],
                     innerlayer[nnk][mj],     // top edges
                     outerlayer[nk][j], outerlayer[nk][nnj],
                     lastOuterLayer[nk][pj],     // sides
                     outerlayer[nk][nj], innerlayer[nk][nmj],
                     innerlayer[nk][mj],        // quads
                     parity);

                  outerParity = (i + k/2 + pj/2) % 2;
                  sideParity = (i + k/2 + 1) % 2;
                  parity = outerParity*4 + sideParity + 1;
                  
                  addQuadwedgeTessellation(
                     model, 
                     lastOuterLayer[k][nnpj], lastOuterLayer[k][pj],
                     outerlayer[k][nnj], lastOuterLayer[nnk][nnpj],
                     lastOuterLayer[nnk][pj], outerlayer[nnk][nnj], 
                     lastOuterLayer[k][npj], innerlayer[k][nmj],
                     innerlayer[k][nnmj],             // bottom edges
                     lastOuterLayer[nnk][npj], innerlayer[nnk][nmj],
                     innerlayer[nnk][nnmj],       // top edges
                     lastOuterLayer[nk][nnpj], lastOuterLayer[nk][pj],
                     outerlayer[nk][nnj],       // sides
                     lastOuterLayer[nk][npj], innerlayer[nk][nmj],
                     innerlayer[nk][nnmj],          // quads
                     parity);
                  
                  j = nnj;
                  pj = nnpj;
                  mj = nnmj;
               }
               int nmj = (mj+1) % nit;
               int nj = (j+1) % not;
               int nnj = (j+2) % not;
               
               int outerParity = (i + j/2 + k/2) % 2;
               int sideParity = (i + k/2) % 2;
               int parity = outerParity*4 + sideParity+1;
               
               addQuadwedgeTessellation(
                  model, 
                  outerlayer[k][j], outerlayer[k][nnj],
                  lastOuterLayer[k][pj], outerlayer[nnk][j],
                  outerlayer[nnk][nnj], lastOuterLayer[nnk][pj], 
                  outerlayer[k][nj], innerlayer[k][nmj],
                  innerlayer[k][mj],           // bottom edges
                  outerlayer[nnk][nj], innerlayer[nnk][nmj],
                  innerlayer[nnk][mj],     // top edges
                  outerlayer[nk][j], outerlayer[nk][nnj],
                  lastOuterLayer[nk][pj],     // sides
                  outerlayer[nk][nj], innerlayer[nk][nmj],
                  innerlayer[nk][mj],        // quads
                  parity);
               
            }
            // next height
         }
         // next radius
      }
         
      return model;
   }
   
   /**
    * Conformally maps an ellipse to a rectangular grid using the method of 
    * <blockquote>
    * Daniela Rosca, Uniform and refinable grids on elliptic domains and on
    * some surfaces of revolution, Applied Mathematics and Computation,Volume
    * 217, Issue 19, 2011, Pages 7812-7817
    * </blockquote>
    * @param a ellipsoid radius along x
    * @param b ellipsoid radius along y
    * @param L1 rectangle half-width along x
    * @param L2 rectangle half-width along y
    * @param input input point (x,y)
    * @param output output point (x,y) modified, z left unchanged
    */
   public static void conformalMapEllipseRectangle (
      double a, double b, double L1, double L2, Point3d input, Point3d output) {
      double x = input.x;
      double y = input.y;
      
      double absx = Math.abs (x);
      double absy = Math.abs (y);
      
      double sx = Math.signum (x);
      double sy = Math.signum (y);
      
      if (absy*a <= absx*b) {
         if ( absx == 0) {
            output.x = 0;
            output.y = 0;
         } else {
            double c = sx*Math.sqrt (x*x + a*a/(b*b)*y*y);
            output.x = c*Math.sqrt (Math.PI)/2;
            output.y = c*2*b/a/Math.sqrt (Math.PI)*Math.atan (a*y/(b*x));
         }
      } else {
         double c = sy*Math.sqrt (b*b/(a*a)*x*x + y*y);
         output.x = c*2*a/b/Math.sqrt (Math.PI)*Math.atan (b*x/(a*y));
         output.y = c*Math.sqrt (Math.PI)/2;         
      }
   }
   
   /**
    * Conformally maps a rectangular grid to an ellipse using the method of 
    * <blockquote>
    * Daniela Rosca, Uniform and refinable grids on elliptic domains and on
    * some surfaces of revolution, Applied Mathematics and Computation,Volume
    * 217, Issue 19, 2011, Pages 7812-7817
    * </blockquote>
    * @param a ellipsoid radius along x
    * @param b ellipsoid radius along y
    * @param L1 rectangle half-width along x
    * @param L2 rectangle half-width along y
    * @param input input point using (x,y)
    * @param output output point, only x,y are modified
    */
   public static void conformalMapRectangleEllipse (
      double a, double b, double L1, double L2, Point3d input, Point3d output) {
      double x = input.x;
      double y = input.y;
      
      double absx = Math.abs (x);
      double absy = Math.abs (y);
      
      if (absy*L1 <= absx*L2) {
         if ( absx == 0) {
            output.x = 0;
            output.y = 0;
         } else {
            double theta = Math.PI*L1*y/4/L2/x;
            output.x = x*Math.sqrt (a*L2/b/L1)*Math.cos (theta);
            output.y = x*Math.sqrt (b*L2/a/L1)*Math.sin (theta);
         }
      } else {
         double theta = Math.PI*L2*x/4/L1/y;
         output.x = y*Math.sqrt (a*L1/b/L2)*Math.sin (theta);
         output.y = y*Math.sqrt (b*L1/a/L2)*Math.cos (theta);         
      }
   }
   
   /**
    * Maps a unit sphere to a cylinder with unit radius and z in [-1,1] using
    * the bilipschitz method described in
    * <blockquote>
    * A bi-Lipschitz continuous, volume preserving map from the unit ball onto
    * a cube, Griepentrog, Hoppner, Kaiser, Rehberg, 2008
    * </blockquote>
    * @param input sphere input point
    * @param output cylinder input point
    */
   public static void bilipschitzMapSphereCylinder (
      Point3d input, Point3d output) {
      double x1 = input.x;
      double x2 = input.y;
      double y = input.z;
      
      double ay = Math.abs (y);
      double xx = Math.sqrt (x1*x1+x2*x2);
      double xy = Math.cbrt (
         Math.abs (x1*x1*x1) + Math.abs (x2*x2*x2) + Math.abs (y*y*y));

      double c = Math.sqrt (5)/2;
      if (xx == 0 && ay == 0) {
         output.x = 0;
         output.y = 0;
         output.z = 0;
      } else if (c*ay <= xx) {
         output.x = x1*xy/xx;
         output.y = x2*xy/xx;
         output.z = 3*y/2;
      } else if ( c*y >= xx) {
         double d = Math.sqrt (3*xy/(xy+ay));
         output.x = x1*d;
         output.y = x2*d;
         output.z = xy;
      } else {
         double d = Math.sqrt (3*xy/(xy+ay));
         output.x = x1*d;
         output.y = x2*d;
         output.z = -xy;
      }
   }
   
   /**
    * Maps a cylinder with unit radius and z in [-1,1] to a unit sphere using
    * the bilipschitz method described in
    * <blockquote>
    * A bi-Lipschitz continuous, volume preserving map from the unit ball onto
    * a cube, Griepentrog, Hoppner, Kaiser, Rehberg, 2008
    * </blockquote>
    * @param input cylinder input point
    * @param output sphere output point
    */
   public static void bilipschitzMapCylinderSphere (
      Point3d input, Point3d output) {
      double a = input.x;
      double b = input.y;
      double z = input.z;
      double z2 = z*z;
      
      double r2 = a*a + b*b;
      if (z2 >= r2) {
         if (z2 > 0) {
            double s = Math.sqrt (2.0/3-r2/9/z2);
            output.x = a*s;
            output.y = b*s;
            output.z = z - r2/3/z;
         } else {
            output.x = 0;
            output.y = 0;
            output.z = 0;
         }
      } else {
         double s = Math.sqrt(1-4*z2/9/r2);
         output.x = a*s;
         output.y = b*s;
         output.z = 2.0/3*z;
      }
   }
   
   /**
    * Maps a 2x2x2 cube to a unit sphere
    * @param cube input point in cube coordinates
    * @param sphere output point in sphere coordinates
    */
   public static void mapCubeToSphere(Point3d cube, Point3d sphere) {
      // map x-y to conformal unit circle
      sphere.x = cube.x;
      sphere.y = cube.y;
      sphere.z = cube.z;
      conformalMapRectangleEllipse (1, 1, 1, 1, sphere, sphere);
      
      // map disc to sphere
      bilipschitzMapCylinderSphere (sphere, sphere);
   }
   
   /**
    * Maps a unit sphere to a 2x2x2 cube
    * @param sphere input point in sphere coordinates
    * @param cube output point in cube coordinates
    */
   public static void mapSphereToCube(Point3d sphere, Point3d cube) {
      cube.x = sphere.x;
      cube.y = sphere.y;
      cube.z = sphere.z;
      bilipschitzMapSphereCylinder (cube, cube);
      conformalMapEllipseRectangle (1, 1, 1, 1, cube, cube);
   }
   
   /**
    * Creates a hex cylinder by first generating a hex grid, then mapping it to
    * a cylinder using a conformal map
    * 
    * @param fem model
    * @param l length of cylinder (z-axis)
    * @param r radius of cylinder 
    * @param nl number of elements along the length
    * @param nr number of elements outward from the radius along the x,y-axes
    * @return generated model
    */
   public static FemModel3d createHexCylinder (
      FemModel3d fem, double l, double r, int nl, int nr) {
      if (fem == null) {
         fem = new FemModel3d();
      }
      
      FemFactory.createHexGrid (fem, 2*r, 2*r, l, 2*nr, 2*nr, nl);
      Point2d pnt = new Point2d();
      
      // map nodes
      for (FemNode3d node : fem.getNodes ()) {
         Point3d pos = node.getRestPosition ();
         double d2 = pos.x*pos.x + pos.y*pos.y;  // 2D squared distance 
         
         if (d2 > 0) {
            // radial scaling
            //    // radial distance of current square:
            //    double rd = Math.max (Math.abs (pos.x), Math.abs (pos.y));
            //    // scale x, y so that new distance is rd
            //    double s = Math.sqrt (rd*rd/d2);
            //    pos.x *= s;
            //    pos.y *= s;
            
            // conformal scaling
            conformalMapRectangleEllipse (r, r, r, r, pos, pos);
            
            node.setRestPosition (pos);
            node.setPosition (pos);
         }
      }
      
      return fem;
   }
   
   /**
    * Creates a partial cylinder made of mostly hex elements, with wedges in
    * the centre column
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param l length along the z axis
    * @param r radius in the x-y plane
    * @param theta size of the slice, in radians
    * @param nl element resolution along the length
    * @param nr element resolution along the radius
    * @param ntheta element resolution around the slice
    * @return created FEM model
    */
   public static FemModel3d createPartialCylinder(
      FemModel3d model, double l, double r, double theta,
      int nl, int nr, int ntheta) {

      return createPartialHexWedgeCylinder(model, l, r, theta, nl, nr, ntheta);

   }
   
   public static FemModel3d createPartialHexWedgeCylinder(
      FemModel3d model, double l, double r, double theta,
      int nl, int nr, int ntheta) {
 
      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }

     FemNode3d nodes[][][] = new FemNode3d[ntheta][nl][nr];

      double dl = l / (nl - 1);
      double dt = theta / (ntheta-1);
      double dr = 1.0 / (nr - 1);

      // generate nodes
      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {

            if (k == 0) {
               FemNode3d node =
                  new FemNode3d(new Point3d(0, 0, -l / 2 + j * dl));
               for (int i = 0; i < ntheta; i++) {
                  nodes[i][j][k] = node;
               }
               model.addNode(node);
            } else {
               for (int i = 0; i < ntheta; i++) {
                  double rr = r * Math.pow(dr * k, 0.7);
                  nodes[i][j][k] =
                     new FemNode3d(new Point3d(-rr * Math.sin(dt * i), rr
                        * Math.cos(dt * i), -l / 2 + j * dl));
                  model.addNode(nodes[i][j][k]);
               }
            }
         }
      }

      // generate elements
      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl - 1; j++) {
            for (int i = 0; i < ntheta -1; i++) {

               if (k == 0) {
                  // wedge element
                  WedgeElement wedge =
                     new WedgeElement(
                        nodes[i][j][k + 1], nodes[i + 1][j][k + 1],
                        nodes[i][j][k],

                        nodes[i][j + 1][k + 1],
                        nodes[i + 1][j + 1][k + 1], nodes[i][j + 1][k]);
                  model.addElement(wedge);
               } else {
                  // hex element
                  HexElement hex =
                     new HexElement(
                        nodes[i][j][k + 1], nodes[i + 1][j][k + 1],
                        nodes[i + 1][j + 1][k + 1],
                        nodes[i][j + 1][k + 1],

                        nodes[i][j][k], nodes[i + 1][j][k],
                        nodes[i + 1][j + 1][k], nodes[i][j + 1][k]);
                  model.addElement(hex);
               }

            }
         }
      }

      return model;
   }

   /**
    * Creates a tube made of hex elements. Identical to {@link
    * #createTube(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Hex}.
    */
   public static FemModel3d createHexTube(
      FemModel3d model, double l, double rin, double rout,
      int nt, int nl, int nr) {

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }

      FemNode3d nodes[][][] = new FemNode3d[nt][nl+1][nr+1];
      double dl = l / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / nr;
      // double dr = 0.5*r;

      for (int k = 0; k < nr+1; k++) {
         for (int j = 0; j < nl+1; j++) {
            for (int i = 0; i < nt; i++) {
               nodes[i][j][k] =
                  // new FemNode3d(new Point3d(
                  // -l/2+j*dl,
                  // (rin+dr*k)*Math.cos(dt*i),
                  // (rin+dr*k)*Math.sin(dt*i)));
                  // Changed to align tube with z axis
                  new FemNode3d(new Point3d(
                     -(rin + dr * k) * Math.sin(dt * i), (rin + dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      HexElement elems[][][] = new HexElement[nt][nl][nr];
      LinkedList<HexElement> elemList = new LinkedList<HexElement>();

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               elems[i][j][k] =
                  new HexElement(
                     nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                     nodes[(i + 1) % nt][j + 1][k + 1], nodes[i][j + 1][k + 1],

                     nodes[i][j][k], nodes[(i + 1) % nt][j][k], nodes[(i + 1)
                        % nt][j + 1][k], nodes[i][j + 1][k]

                  );

               // elems[i][j][k].setParity ((i+j)%2==0 ? 1 : 0);

               elemList.add(elems[i][j][k]);
               model.addElement(elems[i][j][k]);
            }
         }
      }
      HexElement.setParities(elemList);

      for (int i = 0; i < nt; i++) {
         for (int j = 0; j < nr; j++) {
            // nodes[i][0][j].setDynamic(false);
         }
      }
      setTubeEdgesHard(model, l, rin, rout);
      return model;
   }

   /**
    * Creates a tube made of tet elements. Identical to {@link
    * #createTube(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Tet}.
    */
   public static FemModel3d createTetTube(
      FemModel3d model, 
      double l, double rin, double rout, int nt, int nl, int nr) {

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }

      // round nt up to even to allow proper tesselation
      if ((nt % 2) == 1) {
         nt++;
      }
      // HexModel model = new HexModel();

      FemNode3d nodes[][][] = new FemNode3d[nt][nl+1][nr+1];

      double dl = l / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / nr;
      // double dr = 0.5*r;

      for (int k = 0; k < nr+1; k++) {
         for (int j = 0; j < nl+1; j++) {
            for (int i = 0; i < nt; i++) {
               nodes[i][j][k] =
                  // new FemNode3d(new Point3d(-l/2+j*dl,
                  // (rin+dr*k)*Math.cos(dt*i),
                  // (rin+dr*k)*Math.sin(dt*i)));
                  // changed to make tube align with the z axis
                  new FemNode3d(new Point3d(
                     -(rin + dr * k) * Math.sin(dt * i), (rin + dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      // for(FemNode3d n : tetMod.getNodes())
      // {
      // R.mul(pos, n.getPosition());
      // n.setPosition(new Point3d(pos));
      // }

      TetElement elems[][][][] = new TetElement[nt][nl][nr][5];

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               elems[i][j][k] =
                  TetElement.createCubeTesselation(
                     nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                     nodes[(i + 1) % nt][j + 1][k + 1], nodes[i][j + 1][k + 1],
                     nodes[i][j][k], nodes[(i + 1) % nt][j][k], nodes[(i + 1)
                        % nt][j + 1][k], nodes[i][j + 1][k], (i+j+k) % 2 == 0);

               model.addElement(elems[i][j][k][0]);
               model.addElement(elems[i][j][k][1]);
               model.addElement(elems[i][j][k][2]);
               model.addElement(elems[i][j][k][3]);
               model.addElement(elems[i][j][k][4]);
            }
         }
      }

      // model.getSurfaceMesh().setEdgeHard(model.getNode(3), model.getNode(34),
      // true);
      setTubeEdgesHard(model, l, rin, rout);
      return model;
   }
   
   /**
    * Creates a partial tube made of hex elements. Identical to
    * {@link
    * #createPartialTube(FemModel3d,FemElementType,double,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Hex}.
    */
   public static FemModel3d createPartialHexTube(
      FemModel3d model, double l, double rin, double rout, double theta, 
      int nl, int nr, int ntheta) {
      FemNode3d nodes[][][] = new FemNode3d[ntheta][nl][nr];

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      
      double dl = l / (nl - 1);
      double dt = theta / (ntheta-1);
      double dr = (rout - rin) / (nr - 1);
      // double dr = 0.5*r;

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < ntheta; i++) {
               nodes[i][j][k] =
                  // new FemNode3d(new Point3d(
                  // -l/2+j*dl,
                  // (rin+dr*k)*Math.cos(dt*i),
                  // (rin+dr*k)*Math.sin(dt*i)));
                  // Changed to align tube with z axis
                  new FemNode3d(new Point3d(
                     -(rin + dr * k) * Math.sin(dt * i), (rin + dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      HexElement elems[][][] = new HexElement[ntheta][nl - 1][nr - 1];
      LinkedList<HexElement> elemList = new LinkedList<HexElement>();

      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl - 1; j++) {
            for (int i = 0; i < ntheta-1; i++) {
               elems[i][j][k] =
                  new HexElement(
                     nodes[i][j][k + 1], nodes[i + 1][j][k + 1],
                     nodes[i + 1][j + 1][k + 1], nodes[i][j + 1][k + 1],

                     nodes[i][j][k], nodes[i + 1][j][k], nodes[i + 1][j + 1][k],
                     nodes[i][j + 1][k]
                  );

               // elems[i][j][k].setParity ((i+j)%2==0 ? 1 : 0);

               elemList.add(elems[i][j][k]);
               model.addElement(elems[i][j][k]);
            }
         }
      }
      HexElement.setParities(elemList);

      setTubeEdgesHard(model, l, rin, rout);
      return model;
   }

   /**
    * Creates a partial tube made of tet elements. Identical to
    * {@link
    * #createPartialTube(FemModel3d,FemElementType,double,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Tet}.
    */
   public static FemModel3d createPartialTetTube(
      FemModel3d model, double l, double rin, double rout, double theta,
      int nl, int nr, int ntheta) {
      // HexModel model = new HexModel();

      FemNode3d nodes[][][] = new FemNode3d[ntheta][nl][nr];

      double dl = l / (nl - 1);
      double dt = theta / (ntheta-1);
      double dr = (rout - rin) / (nr - 1);
      // double dr = 0.5*r;

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < ntheta; i++) {
               nodes[i][j][k] =
                  // new FemNode3d(new Point3d(-l/2+j*dl,
                  // (rin+dr*k)*Math.cos(dt*i),
                  // (rin+dr*k)*Math.sin(dt*i)));
                  // changed to make tube align with the z axis
                  new FemNode3d(new Point3d(
                     -(rin + dr * k) * Math.sin(dt * i), (rin + dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      // for(FemNode3d n : tetMod.getNodes())
      // {
      // R.mul(pos, n.getPosition());
      // n.setPosition(new Point3d(pos));
      // }

      TetElement elems[][][][] = new TetElement[ntheta][nl - 1][nr - 1][5];

      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl - 1; j++) {
            for (int i = 0; i < ntheta -1; i++) {
               elems[i][j][k] =
                  TetElement.createCubeTesselation(
                     nodes[i][j][k + 1], nodes[i + 1][j][k + 1],
                     nodes[i + 1][j + 1][k + 1], nodes[i][j + 1][k + 1],
                     nodes[i][j][k], nodes[i + 1][j][k], nodes[i + 1][j + 1][k],
                     nodes[i][j + 1][k], (i + j + k) % 2 == 0);

               model.addElement(elems[i][j][k][0]);
               model.addElement(elems[i][j][k][1]);
               model.addElement(elems[i][j][k][2]);
               model.addElement(elems[i][j][k][3]);
               model.addElement(elems[i][j][k][4]);
            }
         }
      }

      // model.getSurfaceMesh().setEdgeHard(model.getNode(3), model.getNode(34),
      // true);
      setTubeEdgesHard(model, l, rin, rout);
      return model;
   }

   /**
    * Creates a hollow torus made of hex elements. Identical to {@link
    * #createTorus(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Hex}.
    */
   public static FemModel3d createHexTorus(
      FemModel3d model,
      double R, double rin, double rout, int nt, int nl, int nr) {

      FemNode3d nodes[][][] = new FemNode3d[nt][nl][nr];

      double dT = 2 * Math.PI / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / (nr - 1);

      RotationMatrix3d RM = new RotationMatrix3d(1.0, 0, 0, Math.PI / 2.0);
      Vector3d pos = new Vector3d();

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               pos.set(
                  R * Math.cos(dT * j) + (rin + dr * k) * Math.cos(dt * i)
                     * Math.cos(dT * j), R * Math.sin(dT * j) + (rin + dr * k)
                     * Math.cos(dt * i) * Math.sin(dT * j), (rin + dr * k)
                     * Math.sin(dt * i));
               RM.mul(pos);

               nodes[i][j][k] = new FemNode3d(new Point3d(pos));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      HexElement elems[][][] = new HexElement[nt][nl][nr - 1];

      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               elems[i][j][k] =
                  new HexElement(
                     nodes[i][j][k], nodes[(i + 1) % nt][j][k], nodes[(i + 1)
                        % nt][(j + 1) % nl][k], nodes[i][(j + 1) % nl][k],
                     nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                     nodes[(i + 1) % nt][(j + 1) % nl][k + 1], nodes[i][(j + 1)
                        % nl][k + 1]

                  );

               model.addElement(elems[i][j][k]);
            }
         }
      }

      return model;
   }

   /**
    * Creates a hollow torus made of tet elements. Identical to {@link
    * #createTorus(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#Tet}.
    */
   public static FemModel3d createTetTorus(
      FemModel3d model,
      double R, double rin, double rout, int nt, int nl, int nr) {

      FemNode3d nodes[][][] = new FemNode3d[nt][nl][nr];
      
      // round nt and nl up to even to allow proper tesselation
      if ((nt % 2) == 1) {
         nt++;
      }
      if ((nl % 2) == 1) {
         nl++;
      }

      double dT = 2 * Math.PI / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / (nr - 1);

      RotationMatrix3d RM = new RotationMatrix3d(1.0, 0, 0, Math.PI / 2.0);
      Vector3d pos = new Vector3d();

      for (int k = 0; k < nr; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               pos.set(
                  R * Math.cos(dT * j) + (rin + dr * k) * Math.cos(dt * i)
                     * Math.cos(dT * j), R * Math.sin(dT * j) + (rin + dr * k)
                     * Math.cos(dt * i) * Math.sin(dT * j), (rin + dr * k)
                     * Math.sin(dt * i));
               RM.mul(pos);

               nodes[i][j][k] = new FemNode3d(new Point3d(pos));
               model.addNode(nodes[i][j][k]);
            }
         }
      }

      TetElement elems[][][][] = new TetElement[nt][nl][nr - 1][5];

      for (int k = 0; k < nr - 1; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               elems[i][j][k] =
                  TetElement.createCubeTesselation(
                     nodes[i][j][k], nodes[(i + 1) % nt][j][k], nodes[(i + 1)
                        % nt][(j + 1) % nl][k], nodes[i][(j + 1) % nl][k],
                     nodes[i][j][k + 1], nodes[(i + 1) % nt][j][k + 1],
                     nodes[(i + 1) % nt][(j + 1) % nl][k + 1], nodes[i][(j + 1)
                        % nl][k + 1], (i + j + k) % 2 == 0);

               model.addElement(elems[i][j][k][0]);
               model.addElement(elems[i][j][k][1]);
               model.addElement(elems[i][j][k][2]);
               model.addElement(elems[i][j][k][3]);
               model.addElement(elems[i][j][k][4]);
            }
         }
      }
      return model;
   }

   private static int X_POS = 0x001;
   private static int X_NEG = 0x002;
   private static int Y_POS = 0x004;
   private static int Y_NEG = 0x008;
   private static int Z_POS = 0x010;
   private static int Z_NEG = 0x020;
   private static int R_INNER = 0x40;
   private static int R_OUTER = 0x80;

   private static int gridBoundarySurfaces (
      Point3d pnt, double widthX, double widthY, double widthZ) {
      double tol =
         (1e-14) * (Math.abs(widthX) + Math.abs(widthY) + Math.abs(widthZ));

      int boundarySurfaces = 0;
      
      if (pnt.x >= widthX / 2 - tol) {
         boundarySurfaces |= X_POS;
      }
      else if (pnt.x <= -widthX / 2 + tol) {
         boundarySurfaces |= X_NEG;
      }
      if (pnt.y >= widthY / 2 - tol) {
         boundarySurfaces |= Y_POS;
      }
      else if (pnt.y <= -widthY / 2 + tol) {
         boundarySurfaces |= Y_NEG;
      }
      if (pnt.z >= widthZ / 2 - tol) {
         boundarySurfaces |= Z_POS;
      }
      else if (pnt.z <= -widthZ / 2 + tol) {
         boundarySurfaces |= Z_NEG;
      }
      return boundarySurfaces;
   }

   private static int bitCount (int val) {
      int cnt = 0;
      while (val != 0) {
         if ((val & 0x1) != 0) {
            cnt++;
         }
         val = (val >>> 1);
      }
      return cnt;
   }

   private static int tubeBoundarySurfaces (
      Point3d pnt, double l, double rin, double rout) {
      double tol = (1e-14) * (Math.abs(l) + Math.abs(rout));
      double radius = Math.sqrt(pnt.x * pnt.x + pnt.y * pnt.y);

      int boundarySurfaces = 0;

      if (radius >= rout - tol) {
         boundarySurfaces |= R_OUTER;
      }
      else if (radius <= rin + tol) {
         boundarySurfaces |= R_INNER;
      }
      if (pnt.z >= l / 2 - tol) {
         boundarySurfaces |= Z_POS;
      }
      else if (pnt.z <= -l / 2 + tol) {
         boundarySurfaces |= Z_NEG;
      }
      return boundarySurfaces;
   }

   private static void setTubeEdgesHard(
      FemModel3d model, double l, double rin, double rout) {
      // and now set the surface edges hard ...
      PolygonalMesh mesh = model.getSurfaceMesh();
      // iterate through all edges in the surface mesh.
      for (Face face : mesh.getFaces()) {
         Vertex3d vtx = (Vertex3d)face.getVertex(0);
         for (int i = 1; i < face.numVertices(); i++) {
            Vertex3d nextVtx = (Vertex3d)face.getVertex(i);
            // a hard edge occurs when both vertices share two
            // or more boundary surfaces.
            int mutualSurfaces = 
               (tubeBoundarySurfaces (vtx.pnt, l, rin, rout) &
                tubeBoundarySurfaces (nextVtx.pnt, l, rin, rout));
            if (bitCount (mutualSurfaces) > 1) {
               mesh.setHardEdge(vtx, nextVtx, true);
            }
            vtx = nextVtx;
         }
      }
   }

   protected static FemModel3d createQuadraticTube(
      FemModel3d model,
      double l, double rin, double rout, int nt, int nl, int nr, 
      boolean useHexes) {
      
      // round nt up to even to allow proper tesselation
      if ((nt % 2) == 1) {
         nt++;
      }
      // double nt, nl, nr to get the equivalent element res
      // for a linear element tesselation. This is what we will work with
      nt *= 2;
      nl *= 2;
      nr *= 2;      

      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }
      
      FemNode3d nodes[][][] = new FemNode3d[nt][nl+1][nr+1];

      double dl = l / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / nr;
      // double dr = 0.5*r;

      for (int k = 0; k < nr+1; k++) {
         for (int j = 0; j < nl+1; j++) {
            for (int i = 0; i < nt; i++) {
               nodes[i][j][k] =
                  // new FemNode3d(new Point3d(
                  // -l/2+j*dl,
                  // (rin+dr*k)*Math.cos(dt*i),
                  // (rin+dr*k)*Math.sin(dt*i)));
                  // Changed to align tube with z axis
                  new FemNode3d(new Point3d(
                     -(rin + dr * k) * Math.sin(dt * i), (rin + dr * k)
                        * Math.cos(dt * i), -l / 2 + j * dl));

            }
         }
      }

      for (int k = 0; k < nr - 1; k += 2) {
         for (int j = 0; j < nl - 1; j += 2) {
            for (int i = 0; i < nt; i += 2) {
               if (useHexes) {
                  FemNode3d enodes[] = new FemNode3d[] 
                    {
                    nodes[i][j][k+2],
                    nodes[(i+2)%nt][j][k+2],
                    nodes[(i+2)%nt][j+2][k+2],
                    nodes[i][j+2][k+2],
                    nodes[i][j][k],
                    nodes[(i+2)%nt][j][k],
                    nodes[(i+2)%nt][j+2][k],
                    nodes[i][j+2][k],

                    nodes[i+1][j][k+2],
                    nodes[(i+2)%nt][j+1][k+2],
                    nodes[i+1][j+2][k+2],
                    nodes[i][j+1][k+2],

                    nodes[i+1][j][k],
                    nodes[(i+2)%nt][j+1][k],
                    nodes[i+1][j+2][k],
                    nodes[i][j+1][k],

                    nodes[i][j][k+1],
                    nodes[(i+2)%nt][j][k+1],
                    nodes[(i+2)%nt][j+2][k+1],
                    nodes[i][j+2][k+1], 
                  };
                  QuadhexElement e = new QuadhexElement (enodes);
                  for (FemNode3d n : enodes) {
                     if (!model.getNodes().contains(n)) {
                        model.addNode(n);
                     }
                  }
                  model.addElement (e);
               }
               else {
                  QuadtetElement[] elems = 
                     createCubeTesselation(
                        new FemNode3d[][][]
                         {
                            {
                               { nodes[i][j][k],
                                 nodes[i][j][k + 1],
                                 nodes[i][j][k + 2] },
                               { nodes[i][j + 1][k],
                                 nodes[i][j + 1][k + 1],
                                 nodes[i][j + 1][k + 2] },
                               { nodes[i][j + 2][k],
                                 nodes[i][j + 2][k + 1],
                                 nodes[i][j + 2][k + 2] } },
                            
                            {
                               { nodes[i + 1][j][k],
                                 nodes[i + 1][j][k + 1],
                                 nodes[i + 1][j][k + 2] },
                               { nodes[i + 1][j + 1][k],
                                 nodes[i + 1][j + 1][k + 1],
                                 nodes[i + 1][j + 1][k + 2] },
                               { nodes[i + 1][j + 2][k],
                                 nodes[i + 1][j + 2][k + 1],
                                 nodes[i + 1][j + 2][k + 2] } },
                            
                            {
                               { nodes[(i + 2) % nt][j][k],
                                 nodes[(i + 2) % nt][j][k + 1],
                                 nodes[(i + 2) % nt][j][k + 2] },
                               { nodes[(i + 2) % nt][j + 1][k],
                                 nodes[(i + 2) % nt][j + 1][k + 1],
                                 nodes[(i + 2) % nt][j + 1][k + 2] },
                               { nodes[(i + 2) % nt][j + 2][k],
                                 nodes[(i + 2) % nt][j + 2][k + 1],
                                 nodes[(i + 2) % nt][j + 2][k + 2] } }
                         },
                        ((i + j + k) / 2 % 2 == 0));
                  for (QuadtetElement e : elems) {
                     FemNode3d[] enodes = e.getNodes();
                     for (FemNode3d n : enodes) {
                        if (!model.getNodes().contains(n)) {
                           model.addNode(n);
                        }
                     }
                     model.addElement (e);
                  }
               }
            }
         }
      }

      setTubeEdgesHard(model, l, rin, rout);

      return model;
   }

   protected static FemModel3d createQuadraticTorus (
      FemModel3d model, double R, double rin, double rout,
      int nt, int nl, int nr, boolean useHexes) {

      // round nt and nl up to even to allow proper tesselation
      if ((nt % 2) == 1) {
         nt++;
      }
      if ((nl % 2) == 1) {
         nl++;
      }
      // double nt, nl, nr to get the equivalent element res
      // for a linear element tesselation. This is what we will work with
      nt *= 2;
      nl *= 2;
      nr *= 2;      

      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }

      FemNode3d nodes[][][] = new FemNode3d[nt][nl][nr+1];

      double dT = 2 * Math.PI / nl;
      double dt = 2 * Math.PI / nt;
      double dr = (rout - rin) / nr;

      RotationMatrix3d RM = new RotationMatrix3d(1.0, 0, 0, Math.PI / 2.0);
      Vector3d pos = new Vector3d();

      for (int k = 0; k < nr+1; k++) {
         for (int j = 0; j < nl; j++) {
            for (int i = 0; i < nt; i++) {
               pos.set(
                  R * Math.cos(dT * j) + (rin + dr * k) * Math.cos(dt * i)
                     * Math.cos(dT * j), R * Math.sin(dT * j) + (rin + dr * k)
                     * Math.cos(dt * i) * Math.sin(dT * j), (rin + dr * k)
                     * Math.sin(dt * i));
               RM.mul(pos, pos);

               nodes[i][j][k] = new FemNode3d(new Point3d(pos));
            }
         }
      }

      for (int k = 0; k < nr - 1; k += 2) {
         for (int j = 0; j < nl; j += 2) {
            for (int i = 0; i < nt; i += 2) {
               if (useHexes) {
                  FemNode3d enodes[] = new FemNode3d[] 
                     {
                        nodes[i][j][k],
                        nodes[(i+2)%nt][j][k],
                        nodes[(i+2)%nt][(j+2)%nl][k],
                        nodes[i][(j+2)%nl][k],
                        nodes[i][j][k+2],
                        nodes[(i+2)%nt][j][k+2],
                        nodes[(i+2)%nt][(j+2)%nl][k+2],
                        nodes[i][(j+2)%nl][k+2],

                        nodes[i+1][j][k],
                        nodes[(i+2)%nt][j+1][k],
                        nodes[i+1][(j+2)%nl][k],
                        nodes[i][j+1][k],

                        nodes[i+1][j][k+2],
                        nodes[(i+2)%nt][j+1][k+2],
                        nodes[i+1][(j+2)%nl][k+2],
                        nodes[i][j+1][k+2],

                        nodes[i][j][k+1],
                        nodes[(i+2)%nt][j][k+1],
                        nodes[(i+2)%nt][(j+2)%nl][k+1],
                        nodes[i][(j+2)%nl][k+1],
                     };
                  QuadhexElement e = new QuadhexElement (enodes);
                  for (FemNode3d n : enodes) {
                     if (!model.getNodes().contains(n)) {
                        model.addNode(n);
                     }
                  }
                  model.addElement (e);
               }
               else {
                  QuadtetElement[] elems = 
                     createCubeTesselation(
                     new FemNode3d[][][]
                      {
                         {
                            { nodes[i][j][k + 2],
                              nodes[i][j][k + 1],
                              nodes[i][j][k] },
                            { nodes[i][j + 1][k + 2],
                              nodes[i][j + 1][k + 1],
                              nodes[i][j + 1][k] },
                            { nodes[i][(j + 2) % nl][k + 2],
                              nodes[i][(j + 2) % nl][k + 1],
                              nodes[i][(j + 2) % nl][k] } },
                         
                         {
                            { nodes[i + 1][j][k + 2],
                              nodes[i + 1][j][k + 1],
                              nodes[i + 1][j][k] },
                            { nodes[i + 1][j + 1][k + 2],
                              nodes[i + 1][j + 1][k + 1],
                              nodes[i + 1][j + 1][k] },
                            { nodes[i + 1][(j + 2) % nl][k + 2],
                              nodes[i + 1][(j + 2) % nl][k + 1],
                              nodes[i + 1][(j + 2) % nl][k] } },
                         
                         {
                            { nodes[(i + 2) % nt][j][k + 2],
                              nodes[(i + 2) % nt][j][k + 1],
                              nodes[(i + 2) % nt][j][k] },
                            { nodes[(i + 2) % nt][j + 1][k + 2],
                              nodes[(i + 2) % nt][j + 1][k + 1],
                              nodes[(i + 2) % nt][j + 1][k] },
                            { nodes[(i + 2) % nt][(j + 2) % nl][k + 2],
                              nodes[(i + 2) % nt][(j + 2) % nl][k + 1],
                              nodes[(i + 2) % nt][(j + 2) % nl][k] } }
                      },
                     ((i + j + k) / 2 % 2 == 0));
                  for (QuadtetElement e : elems) {
                     FemNode3d[] enodes = e.getNodes();
                     for (FemNode3d n : enodes) {
                        if (!model.getNodes().contains(n)) {
                           model.addNode(n);
                        }
                     }
                     model.addElement (e);
                  }        
               }
            }
         }
      }

      return model;
   }

   private static QuadtetElement[] createCubeTesselation(
      FemNode3d[][][] nodes27, boolean even) {
      QuadtetElement qelems[] = new QuadtetElement[5];

      if (even) {
         qelems[0] =
            new QuadtetElement(
               nodes27[0][0][0], nodes27[2][0][0], nodes27[2][2][0],
               nodes27[2][0][2], nodes27[1][0][0], nodes27[2][1][0],
               nodes27[1][1][0], nodes27[1][0][1], nodes27[2][0][1],
               nodes27[2][1][1]);

         qelems[1] =
            new QuadtetElement(
               nodes27[0][0][0], nodes27[2][0][2], nodes27[0][2][2],
               nodes27[0][0][2], nodes27[1][0][1], nodes27[1][1][2],
               nodes27[0][1][1], nodes27[0][0][1], nodes27[1][0][2],
               nodes27[0][1][2]);

         qelems[2] =
            new QuadtetElement(
               nodes27[0][2][2], nodes27[2][0][2], nodes27[2][2][0],
               nodes27[2][2][2], nodes27[1][1][2], nodes27[2][1][1],
               nodes27[1][2][1], nodes27[1][2][2], nodes27[2][1][2],
               nodes27[2][2][1]);

         qelems[3] =
            new QuadtetElement(
               nodes27[0][0][0], nodes27[2][2][0], nodes27[0][2][0],
               nodes27[0][2][2], nodes27[1][1][0], nodes27[1][2][0],
               nodes27[0][1][0], nodes27[0][1][1], nodes27[1][2][1],
               nodes27[0][2][1]);

         qelems[4] =
            new QuadtetElement(
               nodes27[0][2][2], nodes27[0][0][0], nodes27[2][2][0],
               nodes27[2][0][2], nodes27[0][1][1], nodes27[1][1][0],
               nodes27[1][2][1], nodes27[1][1][2], nodes27[1][0][1],
               nodes27[2][1][1]);
      } else {
         qelems[0] =
            new QuadtetElement(
               nodes27[0][0][0], nodes27[2][0][0], nodes27[0][2][0],
               nodes27[0][0][2], nodes27[1][0][0], nodes27[1][1][0],
               nodes27[0][1][0], nodes27[0][0][1], nodes27[1][0][1],
               nodes27[0][1][1]);

         qelems[1] =
            new QuadtetElement(
               nodes27[0][0][2], nodes27[2][0][0], nodes27[2][2][2],
               nodes27[2][0][2], nodes27[1][0][1], nodes27[2][1][1],
               nodes27[1][1][2], nodes27[1][0][2], nodes27[2][0][1],
               nodes27[2][1][2]);

         qelems[2] =
            new QuadtetElement(
               nodes27[0][2][2], nodes27[0][0][2], nodes27[0][2][0],
               nodes27[2][2][2], nodes27[0][1][2], nodes27[0][1][1],
               nodes27[0][2][1], nodes27[1][2][2], nodes27[1][1][2],
               nodes27[1][2][1]);

         qelems[3] =
            new QuadtetElement(
               nodes27[0][2][0], nodes27[2][0][0], nodes27[2][2][0],
               nodes27[2][2][2], nodes27[1][1][0], nodes27[2][1][0],
               nodes27[1][2][0], nodes27[1][2][1], nodes27[2][1][1],
               nodes27[2][2][1]);

         qelems[4] =
            new QuadtetElement(
               nodes27[0][2][0], nodes27[2][0][0], nodes27[2][2][2],
               nodes27[0][0][2], nodes27[1][1][0], nodes27[2][1][1],
               nodes27[1][2][1], nodes27[0][1][1], nodes27[1][0][1],
               nodes27[1][1][2]);
      }

      return qelems;
   }

   /**
    * Takes a FemModel3d containing linear elements, and creates a quadratic
    * model whose elements are the corresponding quadratic elements, with new
    * nodes inserted along the edges as required. The new quadratic model will
    * have straight edges in the rest position.
    * 
    * @param linMod
    * A FemModel3d previously inialized with only linear elements.
    */
   public static FemModel3d createQuadraticModel(
      FemModel3d quadMod, FemModel3d linMod) {
      
      if (quadMod == linMod) {
         throw new IllegalArgumentException(
            "quadMod and linMod must be different");
      }
      
      if (quadMod == null) {
         quadMod = new FemModel3d();
      } else {
         quadMod.clear();
      }

      ComponentListView<FemNode3d> quadNodes = quadMod.getNodes();
      HashMap<FemNode3d,FemNode3d> nodeMap =
         new HashMap<FemNode3d,FemNode3d>();

      for (FemNode3d n : linMod.getNodes()) {
         FemNode3d newn = new FemNode3d(n.getPosition());
         nodeMap.put(n, newn);
         quadMod.addNode(newn);
      }

      for (FemElement3d e : linMod.getElements()) {
         ArrayList<FemNode3d> allNodes = new ArrayList<FemNode3d>();
         FemNode3d qnodes[];

         for (FemNode3d n : e.getNodes()) {
            allNodes.add(nodeMap.get(n));
         }

         if (e instanceof TetElement) {
            qnodes = QuadtetElement.getQuadraticNodes((TetElement)e);
         } else if (e instanceof HexElement) {
            qnodes = QuadhexElement.getQuadraticNodes((HexElement)e);
         } else if (e instanceof WedgeElement) {
            qnodes = QuadwedgeElement.getQuadraticNodes((WedgeElement)e);
         } else if (e instanceof PyramidElement) {
            qnodes = QuadpyramidElement.getQuadraticNodes((PyramidElement)e);
         } else {
            throw new UnsupportedOperationException(
               "Only linear elements supported");
         }
         for (int i = 0; i < qnodes.length; i++) {
            boolean nodeExists = false;
            for (FemNode3d n : quadNodes) {
               if (qnodes[i].getPosition().equals(n.getPosition())) {
                  qnodes[i] = n;
                  nodeExists = true;
                  break;
               }
            }
            if (!nodeExists) {
               quadMod.addNode(qnodes[i]);
            }
         }
         for (FemNode3d n : qnodes) {
            allNodes.add(n);
         }
         FemNode3d[] nodes = allNodes.toArray(new FemNode3d[0]);
         FemElement3d qe = null;
         if (e instanceof TetElement) {
            qe = new QuadtetElement(nodes);
         } else if (e instanceof HexElement) {
            qe = new QuadhexElement(nodes);
         } else if (e instanceof WedgeElement) {
            qe = new QuadwedgeElement(nodes);
         } else if (e instanceof PyramidElement) {
            qe = new QuadpyramidElement(nodes);
         }
         quadMod.addElement(qe);
      }

      quadMod.setMaterial(linMod.getMaterial());
      /*
       * redistributes mass to quadratic model. ONLY works for uniform density
       */
      // double linModPerElementMass = 0;
      // for (FemElement3d e : linMod.getElements()) {
      // linModPerElementMass += e.getMass();
      // }
      // linModPerElementMass /= quadMod.getElements().size();

      for (FemNode3d n : quadNodes) {
         n.clearMass();
      }

      double density = linMod.getDensity();
      for (FemElement3d e : quadMod.getElements()) {
         double mass = e.getRestVolume() * density;
         e.setMass(mass);
         e.invalidateNodeMasses();
      }
      return quadMod;
   }

   /**
    * Creates a tube made of quadratic tet elements. Identical to {@link
    * #createTube(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadTet}.
    */
   public static FemModel3d createQuadtetTube(
      FemModel3d model,
      double l, double rin, double rout, int nt, int nl, int nr) {

      return createQuadraticTube (
         model, l, rin, rout, nt, nl, nr, /*useHexes=*/false);
   }

   /**
    * Creates a hollow torus made of quadratic tet elements. Identical to
    * {@link
    * #createTorus(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadTet}.
    */
   public static FemModel3d createQuadtetTorus(
      FemModel3d model, 
      double R, double rin, double rout, int nt, int nl, int nr) {

      return createQuadraticTorus (
         model, R, rin, rout, nt, nl, nr, /*useHexes=*/false);
   }

   /**
    * Creates a shell-based FEM model made of quadratic tet elements by
    * extruding a surface mesh along the normal direction of its faces.
    * Identical to {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#QuadTet}.
    */
   public static FemModel3d createQuadtetExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      
      FemModel3d tetmod = new FemModel3d();
      createTetExtrusion(tetmod, n, d, zOffset, surface);
      model = createQuadraticModel(model, tetmod);
      return model;
   }

   /**
    * Creates a tube made of quadratic hex elements. Identical to {@link
    * #createTube(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadHex}.
    */
   public static FemModel3d createQuadhexTube(
      FemModel3d model,
      double l, double rin, double rout, int nt, int nl, int nr) {

      return createQuadraticTube (
         model, l, rin, rout, nt, nl, nr, /*useHexes=*/true);
   }

   /**
    * Creates a hollow torus made of quadratic hex elements. Identical to
    * {@link
    * #createTorus(FemModel3d,FemElementType,double,double,double,int,int,int)}
    * with the element type set to {@link FemElementType#QuadHex}.
    */
   public static FemModel3d createQuadhexTorus(
      FemModel3d model,
      double R, double rin, double rout, int nt, int nl, int nr) {

      return createQuadraticTorus (
         model, R, rin, rout, nt, nl, nr, /*useHexes=*/true);
   }

   /**
    * Creates a shell-based FEM model made of quadratic hex elements by
    * extruding a surface mesh along the normal direction of its faces. The
    * surface mesh must be composed of quads.  Identical to {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#QuadHex}.
    */
   public static FemModel3d createQuadhexExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      
      FemModel3d hexmod = new FemModel3d();
      createHexExtrusion(hexmod, n, d, zOffset, surface);
      model = createQuadraticModel(model, hexmod);
      return model;
   }

   /**
    * Creates a shell-based FEM model made of quadratic wedge elements by
    * extruding a surface mesh along the normal direction of its faces.  The
    * surface mesh must be composed of triangles. Identical to {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#Wedge}.
    */
   public static FemModel3d createQuadwedgeExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      
      FemModel3d wedgemod = new FemModel3d();
      createWedgeExtrusion(wedgemod, n, d, zOffset, surface);
      model = createQuadraticModel(model, wedgemod);
      return model;
   }

   private static void setGridEdgesHard(
      FemModel3d model, double widthX, double widthY, double widthZ) {

      // and now set the surface edges hard ...
      PolygonalMesh mesh = model.getSurfaceMesh();
      // iterate through all edges in the surface mesh.
      for (Face face : mesh.getFaces()) {
         HalfEdge he0 = face.firstHalfEdge();
         HalfEdge he = he0;
         do {
            Vertex3d tailv = he.getTail();
            Vertex3d headv = he.getHead();
            int mutualSurfaces = 
               (gridBoundarySurfaces (tailv.pnt, widthX, widthY, widthZ) &
                gridBoundarySurfaces (headv.pnt, widthX, widthY, widthZ));
            if (bitCount (mutualSurfaces) > 1) {
               mesh.setHardEdge(tailv, headv, true);
            }
            he = he.getNext();
         }
         while (he != he0);
      }
   }

   /**
    * Creates a shell-based FEM model made of hex elements by extruding a
    * surface mesh along the normal direction of its faces.  The surface mesh
    * must be composed of quads.  Identical to {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#Hex}.
    */
   public static FemModel3d createHexExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }
      if (!surface.isQuad()) {
         throw new IllegalArgumentException (
            "Hex extrusion requires a quad mesh");
      }

      for (Vertex3d v : surface.getVertices()) {
         model.addNode(new FemNode3d(v.pnt));
      }

      Point3d newpnt = new Point3d();
      Vector3d nrm = new Vector3d();
      for (int i = 0; i < n; i++) {
         for (Vertex3d v : surface.getVertices()) {
            v.computeNormal(nrm);
            newpnt.scaledAdd((i + 1) * d + zOffset, nrm, v.pnt);
            model.addNode(new FemNode3d(newpnt));
         }

         for (Face f : surface.getFaces()) {
            FemNode3d[] nodes = new FemNode3d[8];
            int cnt = 0;

            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] =
                  model.getNode(idx + (i + 1) * surface.numVertices());
            }
            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] = model.getNode(idx + i * surface.numVertices());
            }

            HexElement e = new HexElement(nodes);
            model.addElement(e);
         }
      }
      return model;
   }

   /**
    * Creates a shell-based FEM model made of wedge elements by extruding a
    * surface mesh along the normal direction of its faces.  The surface mesh
    * must be composed of triangles. Identical to {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#Wedge}.
    */
   public static FemModel3d createWedgeExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      if (model == null) {
         model = new FemModel3d();
      }
      else {
         model.clear();
      }
      if (!surface.isTriangular()) {
         throw new IllegalArgumentException (
            "Wedge extrusion requires a triangular mesh");
      }

      for (Vertex3d v : surface.getVertices()) {
         model.addNode(new FemNode3d(v.pnt));
      }

      Point3d newpnt = new Point3d();
      Vector3d nrm = new Vector3d();
      for (int i = 0; i < n; i++) {
         for (Vertex3d v : surface.getVertices()) {
            v.computeNormal(nrm);
            newpnt.scaledAdd((i + 1) * d + zOffset, nrm, v.pnt);
            model.addNode(new FemNode3d(newpnt));
         }

         for (Face f : surface.getFaces()) {
            FemNode3d[] nodes = new FemNode3d[6];
            int cnt = 0;

            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] =
                  model.getNode(idx + (i + 1) * surface.numVertices());
            }
            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] = model.getNode(idx + i * surface.numVertices());
            }

            WedgeElement e = new WedgeElement(nodes);
            model.addElement(e);
         }
      }
      return model;
   }

   /**
    * Given a triangular face associated with an element, finds the
    * corresponding face in the element, and if that face is a quad, returns the
    * additional node completes the quad.
    * @param surfaceFem TODO
    */
   private static FemNode3d getQuadFaceNode(
      Face tri, FemElement3d elem, FemModel3d surfaceFem) {

      int[] faceNodeIdxs = elem.getFaceIndices();
      boolean[] marked = new boolean[4];
      int[] localTriIdxs = new int[3];

      for (int k = 0; k < 3; k++) {
         FemNode node = surfaceFem.getSurfaceNode (tri.getVertex(k));
         localTriIdxs[k] = elem.getLocalNodeIndex(node);
         if (localTriIdxs[k] == -1) {
            throw new InternalErrorException(
               "tri does not share all nodes with element");
         }
      }

      // Check each face in the element to see if it is a quad, and if
      // so, whether it contains tri.
      for (int i = 0; i < faceNodeIdxs.length; i += (faceNodeIdxs[i] + 1)) {
         int j, k;
         if (faceNodeIdxs[i] == 4) {
            // only consider quad faces
            for (j = 0; j < 4; j++) {
               marked[j] = false;
            }
            // see if every node in tri lies in the face
            for (k = 0; k < 3; k++) {
               int li = localTriIdxs[k];
               for (j = 0; j < 4; j++) {
                  if (li == faceNodeIdxs[j + i + 1]) {
                     marked[j] = true;
                     break;
                  }
               }
               if (j == 4) {
                  // node is not in face
                  break;
               }
            }
            if (k == 3) {
               // every node in tri does lie in the i-th face, so
               // return the remaining node
               for (j = 0; j < 4; j++) {
                  if (!marked[j]) {
                     int li = faceNodeIdxs[j + i + 1];
                     return elem.getNodes()[li];
                  }
               }
            }
         }
      }
      return null;
   }

   /**
    * Creates a shell-based FEM model by extruding a surface mesh along the
    * normal direction of its faces. The element types used depend on the
    * underlying faces: triangular faces generate wedge elements, while quad
    * faces generate hex elements. If the mesh is the surface mesh of an
    * underlying FemModel, then each triangle is examined to see if it is
    * associated with an underlying hex element, and if it is, then a hex
    * element is extruded from both the surface triangles connected to that
    * element. The shell can have multiple layers; the number of layers is
    * <code>n</code>.
    * 
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch. Note that
    * <code>model</code> must be different from <code>surfaceFem</code>
    * @param n number of layers
    * @param d layer thickness
    * @param zOffset offset from the surface
    * @param surface surface mesh to extrude
    * @param surfaceFem FEM associated with the surface mesh, or 
    * <code>null</code> if there is no associated FEM.
    * @return extruded FEM model, which will be <code>model</code> if
    * that argument is not <code>null</code>.
    */
   public static FemModel3d createHexWedgeExtrusion(
      FemModel3d model, int n, double d, double zOffset, PolygonalMesh surface, 
      FemModel3d surfaceFem) {

      if (model == null) {
         model = new FemModel3d();
      } else if (model == surfaceFem) {
         throw new IllegalArgumentException (
            "model and surfaceFem cannot be the same FEM");
      } else {
         model.clear();
      }
      if (n < 1) {
         throw new IllegalArgumentException ("n must be >= 1");
      }

      for (Vertex3d v : surface.getVertices()) {
         FemNode3d node = new FemNode3d(v.pnt);
         model.addNode(node);
      }

      Point3d newpnt = new Point3d();
      Vector3d nrm = new Vector3d();
      for (int l = 0; l < n; l++) {
         // surface.transform (new RigidTransform3d (avgNormal,
         // new RotationMatrix3d()));

         boolean[] marked = new boolean[surface.numFaces()];

         for (Vertex3d v : surface.getVertices()) {
            v.computeAngleWeightedNormal(nrm);
            newpnt.scaledAdd((l + 1) * d + zOffset, nrm, v.pnt);
            model.addNode(new FemNode3d(newpnt));
         }

         int numSurfVtxs = surface.numVertices();

         for (int i = 0; i < surface.numFaces(); i++) {
            if (!marked[i]) {
               Face f = surface.getFaces().get(i);
               int numv = f.numVertices();

               if (numv != 3 && numv != 4) {
                  throw new IllegalArgumentException(
                     "Surface mesh must consist of triangles and/or quads");
               }

               int[] vertexIndices = null;
               // For cases where the surface mesh is an an actual FEM surface
               // mesh, find the element corresponding to this face. Otherwise,
               // elem will be set to null.
               FemNode3d quadNode = null;
               if (surfaceFem != null) {
                  FemElement3dBase elem = surfaceFem.getSurfaceElement(f);
                  if (elem instanceof FemElement3d && numv == 3) {
                     // If there is an element associated with f, and f is a
                     // triangle, see if the element has a corresponding quad 
                     // face and if so, find the extra node associated with it.
                     quadNode =
                        getQuadFaceNode(f, (FemElement3d)elem, surfaceFem);
                  }
               }
               if (quadNode != null) {
                  vertexIndices = new int[4];
                  // iterate through the face edges to build up the list
                  // of vertex indices
                  HalfEdge he = f.firstHalfEdge();
                  int k = 0;
                  for (int j = 0; j < 3; j++) {
                     vertexIndices[k++] = he.tail.getIndex();
                     Vertex3d vop = he.opposite.getNext().head;
                     if (surfaceFem.getSurfaceNode(vop) == quadNode) {
                        // add the extra quad vertex if it is on the triangle
                        // opposite this half edge, and mark that triangle.
                        vertexIndices[k++] = vop.getIndex();
                        marked[he.opposite.getFace().getIndex()] = true;
                     }
                     he = he.getNext();
                  }
               } else {
                  vertexIndices = f.getVertexIndices();
               }

               FemElement3d e;
               // Note: vertexIndices gives the indices of the surface face (or
               // composed quad face) in counter-clockwise order
               if (vertexIndices.length == 3) {
                  // add wedge element, which requires the first three nodes be
                  // around clockwise around a face
                  FemNode3d[] nodes = new FemNode3d[6];
                  for (int j = 0; j < 3; j++) {
                     int idx = vertexIndices[j];
                     nodes[j] = model.getNode(idx + l * numSurfVtxs);
                     nodes[j + 3] = model.getNode(idx + (l + 1) * numSurfVtxs);
                  }
                  e = new WedgeElement(nodes);
               } else {
                  // add hex element, which requires first four nodes to
                  // be arranged counter-clockwise around a face
                  FemNode3d[] nodes = new FemNode3d[8];
                  for (int j = 0; j < 4; j++) {

                     int idx = vertexIndices[j];
                     nodes[j] = model.getNode(idx + (l + 1) * numSurfVtxs);
                     nodes[j + 4] = model.getNode(idx + l * numSurfVtxs);
                  }
                  e = new HexElement(nodes);
               }
               model.addElement(e);
               marked[f.getIndex()] = true;
            }
         }
      }
      return model;
   }

   private static void getTypeConstraints(int[] res, Face face, int[] types) {

      int mustHave = 0;
      int dontCare = 0;

      HalfEdge he = face.firstHalfEdge();
      for (int i = 0; i < 3; i++) {
         Face opface = he.opposite != null ? he.opposite.getFace() : null;
         if (opface == null || types[opface.getIndex()] == 0) {
            dontCare |= (1 << i);
         } else {
            int optype = types[opface.getIndex()];
            int k = opface.indexOfEdge(he.opposite);
            if ((optype & (1 << k)) == 0) {
               mustHave |= (1 << i);
            }
         }
         he = he.getNext();
      }
      res[0] = mustHave;
      res[1] = dontCare;
   }

   private static int[] computeTesselationTypes(PolygonalMesh surface) {

      int numFaces = surface.numFaces();

      int[] types = new int[numFaces];

      // compute a valid set of tetrahedral tesselation types for an extruded
      // triangular mesh, to ensure that all faces of the resulting tesselation
      // match up properly. This algorithm is from Erleben and Dohlmann,
      // "The Thin Shell Tetrahedral Mesh".

      Random rand = new Random(0x1234);

      int[] res = new int[2];
      int[] candidates = new int[6];

      LinkedList<Face> queue = new LinkedList<Face>();
      queue.offer(surface.getFaces().get(0));
      while (!queue.isEmpty()) {
         Face face = queue.poll();

         if (types[face.getIndex()] != 0) {
            // already visited; continue
            continue;
         }

         getTypeConstraints(res, face, types);
         int mustHave = res[0];
         int dontCare = res[1];
         int type = 0;
         if (dontCare == 0 && (mustHave == 0 || mustHave == 7)) {
            HalfEdge he = face.firstHalfEdge();
            for (int i = 0; i < 3; i++) {
               Face opface = he.opposite.getFace();
               int flippedType = (types[opface.getIndex()] ^ (1 << i));
               if (0 < flippedType && flippedType < 7) {
                  // good - fixes it
                  types[opface.getIndex()] = flippedType;
                  type = (mustHave ^ (1 << i));
                  System.out.println("flipping types");
                  break;
               }
               he = he.getNext();
            }
            if (type == 0) {
               // have to keep looking
               type = (mustHave == 0 ? 0x1 : 0x6);
               System.out.println("Warning: incompatible type " + type
                  + ", face " + face.getIndex());

            }
         } else {
            int k = 0;
            for (int code = 1; code <= 6; code++) {
               if ((code & ~dontCare) == mustHave) {
                  candidates[k++] = code;
               }
            }
            type = candidates[rand.nextInt(k)];
         }
         // System.out.println ("face "+face.getIndex()+" " + type);

         types[face.getIndex()] = type;
         HalfEdge he = face.firstHalfEdge();
         for (int i = 0; i < 3; i++) {
            Face opface = he.opposite != null ? he.opposite.getFace() : null;
            if (opface != null && types[opface.getIndex()] == 0) {
               // System.out.println ("offering " + opface.getIndex());
               queue.offer(opface);
            }
            he = he.getNext();
         }
      }
      return types;
   }

   /**
    * Creates a shell-type FEM model by extruding a surface mesh along the
    * normal direction of its faces. The element types used depend on the
    * underlying faces: triangular faces generate wedge elements, while quad
    * faces generate hex elements. The shell can have multiple layers; the
    * number of layers is <code>n</code>.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param n number of layers
    * @param d layer thickness
    * @param zOffset offset from the surface
    * @param surface surface mesh to extrude
    * @return extruded FEM model, which will be <code>model</code> if that
    * argument is not <code>null</code>
    * @throws IllegalArgumentException if the specified element type is not
    * supported, or if the surface faces are not triangles or quads.
    */
   public static FemModel3d createExtrusion(
      FemModel3d model, int n, double d, double zOffset, 
      PolygonalMesh surface) {
      
      // create model
      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      if (n < 1) {
         throw new IllegalArgumentException ("n must be >= 1");
      }

      // compute normals and add base nodes
      Vector3d[] normals = new Vector3d[surface.numVertices()];
      Point3d newpnt = new Point3d();
      for (int i=0; i<surface.numVertices(); i++) {
         normals[i] = new Vector3d();
         Vertex3d vtx = surface.getVertex(i);
         vtx.computeNormal(normals[i]);
         newpnt.scaledAdd (zOffset, normals[i], vtx.pnt);
         FemNode3d newnode = new FemNode3d(newpnt);
         model.addNode(newnode);
      }
      
      // // add vertices as nodes
      // Point3d newpnt = new Point3d();
      // for (int i=0; i<surface.numVertices(); i++) {
      //    Vertex3d v = surface.getVertex(i);
      //    FemNode3d newnode = new FemNode3d(v.pnt);
      //    model.addNode(newnode);
      // }
      
      for (int i = 0; i < n; i++) {
         for (int j=0; j < surface.numVertices(); j++) {
            Vertex3d v = surface.getVertex(j);
            newpnt.scaledAdd((i + 1) * d + zOffset, normals[j], v.pnt);
            FemNode3d newnode = new FemNode3d(newpnt);
            model.addNode(newnode);
         }

         for (Face f : surface.getFaces()) {
            int numv = f.numVertices();
            if (numv != 3 && numv != 4) {
               throw new IllegalArgumentException (
                  "Surfaces face "+f.getIndex()+" has "+numv+
                  " vertices. Only triangles and quads are supported");
            }
            FemNode3d[] nodes = new FemNode3d[2 * numv];
            int cnt = 0;

            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] =
                  model.getNode(idx + (i + 1) * surface.numVertices());
            }
            for (Integer idx : f.getVertexIndices()) {
               nodes[cnt++] = model.getNode(idx + i * surface.numVertices());
            }
            // hex and wedge have different winding order, swap around
            boolean flipNodeOrder = (numv != 4);
            // also need to flip if d < 0
            if (d < 0) {
               flipNodeOrder = !flipNodeOrder;
            }
            if (flipNodeOrder) {
               FemNode3d tmp;
               for (int k=0; k<numv; ++k) {
                  tmp = nodes[k];
                  nodes[k] = nodes[k+numv];
                  nodes[k+numv] = tmp;
               }
            }
            FemElement3d e = FemElement3d.createElement(nodes);
            model.addElement(e);
         }
      }
      return model;
   }

   /**
    * Creates a shell-type FEM model by extruding the shell elements of an
    * existing surface mesh along the direction of the associated vertex
    * normals. The element types used depend on the underlying shell elements:
    * triangular elements generate wedge elements, while quad elements generate
    * hex elements. The shell can have multiple layers; the number of layers is
    * <code>n</code>.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param n number of layers
    * @param d layer thickness
    * @param zOffset offset from the surface
    * @param shellFem FEM model suppling the shell elements
    * @return extruded FEM model, which will be <code>model</code> if that
    * argument is not <code>null</code>
    * @throws IllegalArgumentException if the specified element type is not
    * supported, or if the surface faces are not triangles or quads.
    */
   public static FemModel3d createExtrusion(
      FemModel3d model, int n, double d, double zOffset, 
      FemModel3d shellFem) {
      
      // create model
      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      if (n < 1) {
         throw new IllegalArgumentException ("n must be >= 1");
      }

      HashMap<FemNode3d,Vector3d> nodeNormals = new HashMap<>();
      int[] newBaseNodeIndices = new int[shellFem.numNodes()];
      for (int i=0; i<newBaseNodeIndices.length; i++) {
         newBaseNodeIndices[i] = -1;
      }
      ComponentList<FemNode3d> shellNodes = shellFem.getNodes();
      for (FemNode3d node : shellNodes) {
         List<ShellElement3d> shellElemDeps = node.getAdjacentShellElements();
         if (shellElemDeps.size() > 0) {
            Vector3d nrm = new Vector3d();
            for (ShellElement3d se : shellElemDeps) {
               Vector3d snrm = new Vector3d();
               double area = se.computeNodeNormal (snrm, node);
               nrm.scaledAdd (area, snrm);
            }
            nrm.normalize();
            Point3d newpnt = new Point3d (node.getPosition());
            newpnt.scaledAdd(zOffset, nrm);
            FemNode3d newnode = new FemNode3d(newpnt);
            nodeNormals.put (node, nrm);
            newBaseNodeIndices[shellNodes.indexOf(node)] = model.numNodes();
            model.addNode(newnode);
         }
      }

      int numBaseNodes = model.numNodes();
      
      for (int i = 0; i < n; i++) {
         for (int j=0; j < numBaseNodes; j++) {
            FemNode3d node = shellFem.getNode(j);
            Vector3d nrm = nodeNormals.get (node);
            Point3d newpnt = new Point3d (node.getPosition());
            newpnt.scaledAdd((i + 1) * d + zOffset, nrm);
            FemNode3d newnode = new FemNode3d(newpnt);
            model.addNode(newnode);
         }

         for (ShellElement3d shellElem : shellFem.getShellElements()) {
            FemNode3d[] snodes = shellElem.getNodes();
            FemNode3d[] enodes = new FemNode3d[2*snodes.length];
            int cnt = 0;

            for (FemNode3d node : snodes) {
               int idx = shellNodes.indexOf(node);
               enodes[cnt++] = model.getNode (
                  newBaseNodeIndices[idx] + (i+1)*numBaseNodes);
            }
            for (FemNode3d node : snodes) {
               int idx = shellNodes.indexOf(node);
               enodes[cnt++] = model.getNode (
                  newBaseNodeIndices[idx] + i*numBaseNodes);
            }
            // hex and wedge have different winding order, swap around
            boolean flipNodeOrder = (snodes.length != 4);
            // also need to flip if d < 0
            if (d < 0) {
               flipNodeOrder = !flipNodeOrder;
            }
            if (flipNodeOrder) {
               FemNode3d tmp;
               for (int k=0; k<snodes.length; ++k) {
                  tmp = enodes[k];
                  enodes[k] = enodes[k+snodes.length];
                  enodes[k+snodes.length] = tmp;
               }
            }
            FemElement3d e = FemElement3d.createElement(enodes);
            model.addElement(e);
         }
      }
      return model;
   }
   
   /**
    * Creates a shell-based FEM model made of tet elements by
    * extruding a surface mesh along the normal direction of its faces. 
    * Identical to
    * {@link
    * #createExtrusion(FemModel3d,FemElementType,int,double,double,PolygonalMesh)}
    * with the element type set to {@link FemElementType#Tet}.
    */
   public static FemModel3d createTetExtrusion(
      FemModel3d model, 
      int n, double d, double zOffset, PolygonalMesh surface) {

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      if (!surface.isTriangular()) {
         throw new IllegalArgumentException (
            "Tet extrusion requires a triangular mesh");
      }

      for (Vertex3d v : surface.getVertices()) {
         model.addNode(new FemNode3d(v.pnt));
      }

      Point3d newpnt = new Point3d();
      Vector3d nrm = new Vector3d();

      int[] tesselationTypes = null;
      tesselationTypes = computeTesselationTypes(surface);

      for (int i = 0; i < n; i++) {

         for (Vertex3d v : surface.getVertices()) {
            v.computeAngleWeightedNormal(nrm);
            newpnt.scaledAdd((i + 1) * d + zOffset, nrm, v.pnt);
            model.addNode(new FemNode3d(newpnt));
         }

         for (Face f : surface.getFaces()) {

            // HalfEdge he = f.firstHalfEdge();
            // for (int k=0; k<3; k++) {
            // System.out.print (he.head.getIndex() + " ");
            // he = he.getNext();
            // }
            // System.out.println ("");

            int numf = f.numVertices();
            FemNode3d[] nodes = new FemNode3d[2 * numf];
            // int cnt = 0;

            HalfEdge he = f.firstHalfEdge();
            for (int k = 0; k < numf; k++) {
               int idx = he.tail.getIndex();
               nodes[k] = model.getNode(idx + i * surface.numVertices());
               nodes[k + numf] =
                  model.getNode(idx + (i + 1) * surface.numVertices());
               he = he.getNext();
            }

            // for (Integer idx : f.getVertexIndices()) {
            // nodes[cnt++] =
            // model.getNode (idx + i * surface.numVertices());
            // }
            // for (Integer idx : f.getVertexIndices()) {
            // nodes[cnt++] =
            // model.getNode (idx + (i + 1) * surface.numVertices());
            // }

            TetElement[] tets;
            if (surface.isQuad()) {
               tets =
                  TetElement.createCubeTesselation(
                     nodes[4], nodes[5], nodes[6], nodes[7], nodes[0],
                     nodes[1], nodes[2], nodes[3], true);
            } else {
               tets =
                  TetElement.createWedgeTesselation(
                     nodes[3], nodes[4], nodes[5], nodes[0], nodes[1],
                     nodes[2], tesselationTypes[f.getIndex()]);
            }

            for (TetElement tet : tets) {
               model.addElement(tet);
            }
         }
      }
      return model;
   }

   /**
    * Creates a tube made of either tet, hex, quadTet, or quadHex elements, as
    * specified by <code>type</code>. Note that the element resolution 
    * <code>nt</code> around the central axis will be rounded up to 
    * an even number for tet or quadTet models. 
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param l length along the z axis
    * @param rin inner radius
    * @param rout outer radius
    * @param nt element resolution around the central axis (will be 
    * rounded up to an even number for tet or quadTet models)
    * @param nl element resolution along the length
    * @param nr element resolution along the thickness
    * @return created FEM model
    * @throws IllegalArgumentException if the specified element type
    * is not supported
    */
   public static FemModel3d createTube (
      FemModel3d model, FemElementType type,
      double l, double rin, double rout, int nt, int nl, int nr) {
      switch (type) {
         case Tet:
            return createTetTube(model, l, rin, rout, nt, nl, nr);
         case Hex:
            return createHexTube(model, l, rin, rout, nt, nl, nr);
         case QuadTet:
            return createQuadtetTube(model, l, rin, rout, nt, nl, nr);
         case QuadHex:
            return createQuadhexTube(model, l, rin, rout, nt, nl, nr);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   /**
    * Creates a partial tube made of either tet or hex elements, as specified
    * by <code>type</code>.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param l length along the z axis
    * @param rin inner radius
    * @param rout outer radius
    * @param theta size of the partial tube slice, in radians
    * @param nl element resolution along the length
    * @param nr element resolution along the thickness
    * @param ntheta element resolution along the slice
    * @return created FEM model
    * @throws IllegalArgumentException if the specified element type
    * is not supported
    */
   public static FemModel3d createPartialTube(
      FemModel3d model, FemElementType type, double l, double rin, double rout,
      double theta, int nl, int nr, int ntheta) {
      switch (type) {
         case Tet:
            return createPartialTetTube (
               model, l, rin, rout, theta, nl, nr, ntheta);
         case Hex:
            return createPartialHexTube (
               model, l, rin, rout, theta, nl, nr, ntheta);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   /**
    * Creates a hollow torus made of either tet, hex, quadTet, or quadHex
    * elements, as specified by <code>type</code>. The result is essentially
    * a tube, with inner and outer radii given by <code>rin</code>
    * and <code>rout</code>, bent around the major radius R and connected.
    * For tet or quadTet models, the element resolutions <code>nt</code>
    * and <code>nl</code> will be rounded up to an even number.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param R major radius
    * @param rin inner part of the minor radius
    * @param rout outer part of the minor radius
    * @param nt element resolution around the major radius (will be rounded
    * up to an even number for tet or quadTet models)
    * @param nl element resolution around the minor radius (will be rounded
    * up to an even number for tet or quadTet models)
    * @param nr element resolution along the inner thickness
    * @return created FEM model
    * @throws IllegalArgumentException if the specified element type
    * is not supported
    */
   public static FemModel3d createTorus(
      FemModel3d model, FemElementType type, double R, double rin, double rout,
      int nt, int nl, int nr) {
      switch (type) {
         case Tet:
            return createTetTorus(model, R, rin, rout, nt, nl, nr);
         case Hex:
            return createHexTorus(model, R, rin, rout, nt, nl, nr);
         case QuadTet:
            return createQuadtetTorus(model, R, rin, rout, nt, nl, nr);
         case QuadHex:
            return createQuadhexTorus(model, R, rin, rout, nt, nl, nr);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   /**
    * Creates a shell-based FEM model by extruding a surface mesh along the
    * normal direction of its faces. The model is composed of either tet, hex,
    * quadTet or quadHex elements, as specified by <code>type</code>. The shell
    * can have multiple layers; the number of layers is <code>n</code>.
    *
    * @param model model to which the elements should be added, or
    * <code>null</code> if the model is to be created from scratch.
    * @param type desired element type
    * @param n number of layers
    * @param d layer thickness
    * @param zOffset offset from the surface
    * @param surface surface mesh to extrude
    * @return extruded FEM model, which will be <code>model</code> if
    * that argument is not <code>null</code>
    * @throws IllegalArgumentException if the specified element type is not
    * supported, or if the surface faces are incompatible with the element
    * type.
    */
   public static FemModel3d createExtrusion(
      FemModel3d model, FemElementType type, 
      int n, double d, double zOffset, PolygonalMesh surface) {
      
      switch (type) {
         case Tet:
            return createTetExtrusion(model, n, d, zOffset, surface);
         case Hex:
            return createHexExtrusion(model, n, d, zOffset, surface);
         case Wedge:
            return createWedgeExtrusion(model, n, d, zOffset, surface);
         case QuadTet:
            return createQuadtetExtrusion(model, n, d, zOffset, surface);
         case QuadHex:
            return createQuadhexExtrusion(model, n, d, zOffset, surface);
         case QuadWedge:
            return createQuadwedgeExtrusion(model, n, d, zOffset, surface);
         default:
            throw new IllegalArgumentException (
               "Unsupported element type " + type.toString());
      }
   }

   /**
    * Creates a tetrahedral FEM model from a triangular surface mesh. The
    * tetrahedra will be added to either an existing model (supplied through
    * the argument <code>model</code>), or a newly created
    * <code>FemModel3d</code> (if <code>model</code> is <code>null</code>).
    * 
    * <p>
    * The tessellation is done using tetgen, which is called through a JNI
    * interface. The tessellation quality is controlled using the
    * <code>quality</code> variable, described below.
    *
    * @param model
    * model to which the tetrahedra should be added, or <code>null</code> if
    * the model is to be created from scratch.
    * @param surface
    * triangular surface mesh used to define the tessellation.
    * @param quality
    * If 0, then only the
    * mesh nodes will be used to form the tessellation. However, this may result
    * in highly degenerate tetrahedra. Otherwise, if &gt;
    * 0, tetgen will add additional nodes to ensure that the minimum edge-radius
    * ratio does not exceed <code>quality</code>. A good default value for
    * <code>quality</code> is 2. If set too small (such as less then 1), then
    * tetgen may not terminate.
    * @return the FEM model
    */
   public static FemModel3d createFromMesh(
      FemModel3d model, PolygonalMesh surface, double quality) {
      TetgenTessellator tetgen = new TetgenTessellator();
      tetgen.buildFromMesh(surface, quality);

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      Point3d[] points = tetgen.getPoints();
      for (int i = 0; i < points.length; i++) {
         model.addNode(new FemNode3d(points[i]));
      }
      ComponentList<FemNode3d> nodes = model.getNodes();
      int[] tets = tetgen.getTets();
      for (int i = 0; i < tets.length / 4; i++) {
         FemNode3d n0 = nodes.get(tets[i * 4 + 0]);
         FemNode3d n1 = nodes.get(tets[i * 4 + 1]);
         FemNode3d n2 = nodes.get(tets[i * 4 + 2]);
         FemNode3d n3 = nodes.get(tets[i * 4 + 3]);
         TetElement elem = new TetElement(n1, n3, n2, n0);
         model.addElement(elem);
      }
      return model;
   }

   /**
    * Constrained Delaunay, including the supplied list of points if they fall
    * inside the surface
    */
   public static FemModel3d createFromMeshAndPoints(
      FemModel3d model, PolygonalMesh surface, double quality, Point3d[] pnts) {

      TetgenTessellator tetgen = new TetgenTessellator();
      tetgen.buildFromMeshAndPoints(surface, quality, pnts);

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      Point3d[] points = tetgen.getPoints();
      for (int i = 0; i < points.length; i++) {
         model.addNode(new FemNode3d(points[i]));
      }
      ComponentList<FemNode3d> nodes = model.getNodes();

      int[] tets = tetgen.getTets();
      for (int i = 0; i < tets.length / 4; i++) {
         FemNode3d n0 = nodes.get(tets[i * 4 + 0]);
         FemNode3d n1 = nodes.get(tets[i * 4 + 1]);
         FemNode3d n2 = nodes.get(tets[i * 4 + 2]);
         FemNode3d n3 = nodes.get(tets[i * 4 + 3]);
         TetElement elem = new TetElement(n1, n3, n2, n0);
         model.addElement(elem);
      }
      return model;
   }

   /**
    * Creates a refined version of a an existing tetrahedral FEM model using
    * tetgen and a list of supplemental node locations.
    * 
    * @param model model in which the refined model be built, or
    * <code>null</code> if the model is to be created from scratch.
    * @param input original FEM model which is to be refined
    * @param quality quality factor used by tetgen to refine the model
    * @param pnts locations of the supplemental node
    * @return refined FEM model
    */
   public static FemModel3d refineFem(
      FemModel3d model, FemModel3d input, double quality, Point3d[] pnts) {

      TetgenTessellator tetgen = new TetgenTessellator();

      int[] tets = new int[4 * input.numElements()];
      double[] nodeCoords = new double[3 * input.numNodes()];
      double[] addCoords = new double[3 * pnts.length];

      int idx = 0;
      for (FemNode3d node : input.getNodes()) {
         node.setIndex(idx);
         Point3d pos = node.getPosition();
         nodeCoords[3 * idx] = pos.x;
         nodeCoords[3 * idx + 1] = pos.y;
         nodeCoords[3 * idx + 2] = pos.z;
         idx++;
      }

      idx = 0;
      int numTets = 0;
      for (FemElement3d elem : input.getElements()) {
         if (elem instanceof TetElement) {
            FemNode3d[] nodes = elem.getNodes();
            tets[idx++] = nodes[0].getIndex();
            tets[idx++] = nodes[1].getIndex();
            tets[idx++] = nodes[2].getIndex();
            tets[idx++] = nodes[3].getIndex();
            numTets++;
         }
      }

      idx = 0;
      for (Point3d pnt : pnts) {
         addCoords[idx++] = pnt.x;
         addCoords[idx++] = pnt.y;
         addCoords[idx++] = pnt.z;

      }

      //
      // tetgen.buildFromMeshAndPoints (surface, quality, pnts);
      tetgen.refineMesh(
         nodeCoords, input.numNodes(), tets, numTets, quality, addCoords,
         pnts.length);

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      Point3d[] points = tetgen.getPoints();
      for (int i = 0; i < points.length; i++) {
         model.addNode(new FemNode3d(points[i]));
      }
      ComponentList<FemNode3d> nodes = model.getNodes();

      int[] outTets = tetgen.getTets();
      for (int i = 0; i < outTets.length / 4; i++) {
         FemNode3d n0 = nodes.get(outTets[i * 4 + 0]);
         FemNode3d n1 = nodes.get(outTets[i * 4 + 1]);
         FemNode3d n2 = nodes.get(outTets[i * 4 + 2]);
         FemNode3d n3 = nodes.get(outTets[i * 4 + 3]);
         TetElement elem = new TetElement(n1, n3, n2, n0);
         model.addElement(elem);
      }
      return model;
   }

   /**
    * Creates a refined version of a an existing tetrahedral FEM model using
    * tetgen.
    * 
    * @param model model in which the refined model be built, or
    * <code>null</code> if the model is to be created from scratch.
    * @param input original FEM model which is to be refined
    * @param quality quality factor used by tetgen to refine the model
    * @return refined FEM model
    */
   public static FemModel3d refineFem(
      FemModel3d model, FemModel3d input, double quality) {

      TetgenTessellator tetgen = new TetgenTessellator();

      int[] tets = new int[4 * input.numElements()];
      double[] nodeCoords = new double[3 * input.numNodes()];

      int idx = 0;
      for (FemNode3d node : input.getNodes()) {
         node.setIndex(idx);
         Point3d pos = node.getPosition();
         nodeCoords[3 * idx] = pos.x;
         nodeCoords[3 * idx + 1] = pos.y;
         nodeCoords[3 * idx + 2] = pos.z;
         idx++;
      }

      idx = 0;
      int numTets = 0;
      for (FemElement3d elem : input.getElements()) {
         if (elem instanceof TetElement) {
            FemNode3d[] nodes = elem.getNodes();
            tets[idx++] = nodes[0].getIndex();
            tets[idx++] = nodes[1].getIndex();
            tets[idx++] = nodes[2].getIndex();
            tets[idx++] = nodes[3].getIndex();
            numTets++;
         }
      }

      //
      // tetgen.buildFromMeshAndPoints (surface, quality, pnts);
      tetgen.refineMesh(nodeCoords, input.numNodes(), tets, numTets, quality);

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear();
      }
      Point3d[] points = tetgen.getPoints();
      for (int i = 0; i < points.length; i++) {
         model.addNode(new FemNode3d(points[i]));
      }
      ComponentList<FemNode3d> nodes = model.getNodes();
      int[] outTets = tetgen.getTets();
      for (int i = 0; i < outTets.length / 4; i++) {
         FemNode3d n0 = nodes.get(outTets[i * 4 + 0]);
         FemNode3d n1 = nodes.get(outTets[i * 4 + 1]);
         FemNode3d n2 = nodes.get(outTets[i * 4 + 2]);
         FemNode3d n3 = nodes.get(outTets[i * 4 + 3]);
         TetElement elem = new TetElement(n1, n3, n2, n0);
         model.addElement(elem);
      }
      return model;
   }

   /**
    * Adds a copy of the nodes, elements, markers and attachments from
    * <code>fem1</code> to <code>fem0</code>. Nodes in fem1 are merged with
    * nodes in fem0 that are within TOL distance of each other, where TOL is
    * 1e-8 times the maximum radius of fem0 and fem1. For precise control of
    * node merging, use {@link #addFem(FemModel3d,FemModel3d,double)}.
    * 
    * @param fem0
    * FEM model to which components should be added
    * @param fem1
    * FEM model providing components
    */
   public static void addFem(FemModel3d fem0, FemModel3d fem1) {

      double tol =
         1e-8 * Math.max(
            RenderableUtils.getRadius(fem0), RenderableUtils.getRadius(fem1));
      addFem(fem0, fem1, tol);
   }

   /**
    * Adds a copy of the nodes, elements, markers and attachments from
    * <code>fem1</code> to <code>fem0</code>.
    * 
    * @param fem0
    * FEM model to which components should be added
    * @param fem1
    * FEM model providing components
    * @param nodeMergeDist
    * If &gt;= 0,
    * causes nearby nodes of <code>fem1</code> and <code>fem0</code> to
    * be merged: any node of <code>fem1</code> that is within
    * <code>nodeMergeDist</code> of a node in <code>fem0</code> is replaced by
    * the nearest node in <code>fem0</code>.
    */
   public static void addFem(
      FemModel3d fem0, FemModel3d fem1, double nodeMergeDist) {

      int flags = CopyableComponent.COPY_REFERENCES;
      HashMap<ModelComponent,ModelComponent> copyMap =
         new HashMap<ModelComponent,ModelComponent>();
      ArrayList<FemNode3d> newNodes = new ArrayList<FemNode3d>();

      // Go through all nodes in fem1 and either copy them, or find their
      // nearest counterparts in fem0 that are within a distance given by
      // nodeMergeDist.
      //
      // Note that we want to first find all the new nodes, and then add them
      // later as a group, to avoid merging nodes in fem1 (and also to avoid
      // constantly recomputing the bounding volume hierarchy in fem0).
      for (FemNode3d n : fem1.myNodes) {
         FemNode3d newn;
         if (nodeMergeDist < 0 ||
             (newn=fem0.findNearestNode (
                n.getPosition(), nodeMergeDist))==null) {
            newn = n.copy(flags, copyMap);
            newn.setName(n.getName());
            newNodes.add(newn);
         }
         copyMap.put(n, newn);
      }
      for (FemNode3d n : newNodes) {
         fem0.myNodes.add(n);
      }
      for (FemElement3d e : fem1.myElements) {
         FemElement3d newe = e.copy(flags, copyMap);
         newe.setName(e.getName());
         copyMap.put(e, newe);
         fem0.myElements.add(newe);
      }
      for (ShellElement3d e : fem1.myShellElements) {
         ShellElement3d newe = e.copy(flags, copyMap);
         newe.setName(e.getName());
         copyMap.put(e, newe);
         fem0.myShellElements.add(newe);
      }
      for (FemMarker m : fem1.myMarkers) {
         FemMarker newm = m.copy(flags, copyMap);
         newm.setName(m.getName());
         fem0.myMarkers.add(newm);
      }
      for (DynamicAttachmentComp a : fem1.myAttachments) {
         DynamicAttachmentComp newa = a.copy(flags, copyMap);
         newa.setName(a.getName());
         fem0.myAttachments.add(newa);
      }
   }

   private static class Edge {

      FemNode3d myN0;
      FemNode3d myN1;

      public Edge(FemNode3d n0, FemNode3d n1) {
         myN0 = n0;
         myN1 = n1;
      }

      public boolean equals(Object obj) {
         if (obj instanceof Edge) {
            Edge e = (Edge)obj;
            return ((e.myN0 == myN0 && e.myN1 == myN1) ||
                    (e.myN1 == myN0 && e.myN0 == myN1));
         } else {
            return false;
         }
      }

      public int hashCode() {
         return (myN0.hashCode() + myN1.hashCode());
      }
   }

   private static FemNode3d createNode(FemNode3d[] nodes) {
      Point3d pos = new Point3d();
      FemNode3d node = new FemNode3d();
      for (FemNode3d n : nodes) {
         pos.add(n.getPosition());
      }
      pos.scale(1.0 / nodes.length);
      node.setPosition(pos);
      pos.setZero();
      for (FemNode3d n : nodes) {
         pos.add(n.getRestPosition());
      }
      pos.scale(1.0 / nodes.length);
      node.setRestPosition(pos);
      return node;
   }

   private static TetElement createTet(
      FemNode3d[] nodes, int i0, int i1, int i2, int i3) {
      return new TetElement(nodes[i0], nodes[i1], nodes[i2], nodes[i3]);
   }

   private static WedgeElement createWedge(
      FemNode3d[] nodes, int i0, int i1, int i2, int i3, int i4, int i5) {
      return new WedgeElement(
         nodes[i0], nodes[i1], nodes[i2], nodes[i3], nodes[i4], nodes[i5]);
   }

   private static PyramidElement createPyramid(
      FemNode3d[] nodes, int i0, int i1, int i2, int i3, int i4) {

      return new PyramidElement(
         nodes[i0], nodes[i1], nodes[i2], nodes[i3], nodes[i4]);
   }

   private static HexElement createHex(
      FemNode3d[] nodes, int i0, int i1, int i2, int i3, int i4, int i5,
      int i6, int i7) {
      return new HexElement(
         nodes[i0], nodes[i1], nodes[i2], nodes[i3], nodes[i4], nodes[i5],
         nodes[i6], nodes[i7]);
   }

   private static FemNode3d getEdgeNode(
      FemModel3d fem, FemNode3d n0, FemNode3d n1,
      HashMap<Edge,FemNode3d> edgeNodeMap) {

      Edge edge = new Edge(n0, n1);
      FemNode3d node;
      if ((node = edgeNodeMap.get(edge)) == null) {
         node = createNode(new FemNode3d[] { n0, n1 });
         edgeNodeMap.put(edge, node);
         fem.addNode(node);
      }
      return node;
   }

   private static FemNode3d getQuadFaceNode(
      FemModel3d fem, FemNode3d n0, FemNode3d n1, FemNode3d n2, FemNode3d n3,
      HashMap<Edge,FemNode3d> edgeNodeMap) {

      Edge edge02 = new Edge(n0, n2);
      Edge edge13 = new Edge(n1, n3);
      FemNode3d node;
      if ((node = edgeNodeMap.get(edge02)) == null
         && (node = edgeNodeMap.get(edge13)) == null) {
         node = createNode(new FemNode3d[] { n0, n1, n2, n3 });
         edgeNodeMap.put(edge02, node);
         edgeNodeMap.put(edge13, node);
         fem.addNode(node);
      }
      return node;
   }

   private static void addSubdivisionNodes(
      FemNode3d[] newn, FemModel3d fem, FemElement3d e,
      HashMap<Edge,FemNode3d> edgeNodeMap,
      HashMap<ModelComponent,ModelComponent> copyMap) {

      int idx = 0;
      FemNode3d[] oldn = e.getNodes();
      for (int i = 0; i < oldn.length; i++) {
         newn[idx++] = (FemNode3d)copyMap.get(oldn[i]);
      }
      int[] edgeIdxs = e.getEdgeIndices();
      for (int i = 0; i < edgeIdxs.length;) {
         // for each edge ...
         i++;
         FemNode3d n0 = oldn[edgeIdxs[i++]];
         FemNode3d n1 = oldn[edgeIdxs[i++]];
         newn[idx++] = getEdgeNode(fem, n0, n1, edgeNodeMap);
      }
      int[] faceIdxs = e.getFaceIndices();
      for (int i = 0; i < faceIdxs.length;) {
         // for each face ...
         int nn = faceIdxs[i++];
         if (nn == 4) {
            FemNode3d n0 = oldn[faceIdxs[i++]];
            FemNode3d n1 = oldn[faceIdxs[i++]];
            FemNode3d n2 = oldn[faceIdxs[i++]];
            FemNode3d n3 = oldn[faceIdxs[i++]];
            newn[idx++] = getQuadFaceNode(fem, n0, n1, n2, n3, edgeNodeMap);
         } else {
            i += nn;
         }
      }
      if (idx < newn.length) {
         // create center node ...
         newn[idx] = createNode((FemNode3d[])e.getNodes());
         fem.addNode(newn[idx]);
      }
   }

   private static void subdivideTet(
      FemModel3d fem, TetElement e, HashMap<Edge,FemNode3d> edgeNodeMap,
      HashMap<ModelComponent,ModelComponent> copyMap) {

      FemNode3d[] newn = new FemNode3d[10];
      addSubdivisionNodes(newn, fem, e, edgeNodeMap, copyMap);

      fem.addElement(createTet(newn, 0, 4, 5, 6));
      fem.addElement(createTet(newn, 5, 7, 2, 8));
      fem.addElement(createTet(newn, 4, 1, 7, 9));
      fem.addElement(createTet(newn, 6, 9, 8, 3));
      fem.addElement(createTet(newn, 4, 9, 7, 5));
      fem.addElement(createTet(newn, 5, 7, 8, 9));
      fem.addElement(createTet(newn, 6, 4, 5, 9));
      fem.addElement(createTet(newn, 5, 9, 8, 6));
   }

   private static void subdivideHex(
      FemModel3d fem, HexElement e, HashMap<Edge,FemNode3d> edgeNodeMap,
      HashMap<ModelComponent,ModelComponent> copyMap) {

      FemNode3d[] newn = new FemNode3d[27];
      addSubdivisionNodes(newn, fem, e, edgeNodeMap, copyMap);

      fem.addElement(createHex(newn, 0, 8, 20, 11, 16, 24, 26, 23));
      fem.addElement(createHex(newn, 8, 1, 9, 20, 24, 17, 21, 26));
      fem.addElement(createHex(newn, 20, 9, 2, 10, 26, 21, 18, 25));
      fem.addElement(createHex(newn, 11, 20, 10, 3, 23, 26, 25, 19));
      fem.addElement(createHex(newn, 16, 24, 26, 23, 4, 12, 22, 15));
      fem.addElement(createHex(newn, 24, 17, 21, 26, 12, 5, 13, 22));
      fem.addElement(createHex(newn, 26, 21, 18, 25, 22, 13, 6, 14));
      fem.addElement(createHex(newn, 23, 26, 25, 19, 15, 22, 14, 7));
   }

   private static void subdivideWedge(
      FemModel3d fem, WedgeElement e, HashMap<Edge,FemNode3d> edgeNodeMap,
      HashMap<ModelComponent,ModelComponent> copyMap) {

      FemNode3d[] newn = new FemNode3d[18];
      addSubdivisionNodes(newn, fem, e, edgeNodeMap, copyMap);

      fem.addElement(createWedge(newn, 0, 6, 7, 12, 15, 17));
      fem.addElement(createWedge(newn, 7, 8, 2, 17, 16, 14));
      fem.addElement(createWedge(newn, 6, 1, 8, 15, 13, 16));
      fem.addElement(createWedge(newn, 6, 8, 7, 15, 16, 17));
      fem.addElement(createWedge(newn, 12, 15, 17, 3, 9, 10));
      fem.addElement(createWedge(newn, 17, 16, 14, 10, 11, 5));
      fem.addElement(createWedge(newn, 15, 13, 16, 9, 4, 11));
      fem.addElement(createWedge(newn, 15, 16, 17, 9, 11, 10));
   }

   private static void subdividePyramid(
      FemModel3d fem, PyramidElement e, HashMap<Edge,FemNode3d> edgeNodeMap,
      HashMap<ModelComponent,ModelComponent> copyMap) {

      FemNode3d[] newn = new FemNode3d[14];
      addSubdivisionNodes(newn, fem, e, edgeNodeMap, copyMap);

      fem.addElement(createPyramid(newn, 9, 10, 11, 12, 4));
      fem.addElement(createPyramid(newn, 12, 11, 10, 9, 13));

      fem.addElement(createPyramid(newn, 8, 0, 5, 13, 9));
      fem.addElement(createPyramid(newn, 5, 1, 6, 13, 10));
      fem.addElement(createPyramid(newn, 6, 2, 7, 13, 11));
      fem.addElement(createPyramid(newn, 7, 3, 8, 13, 12));

      fem.addElement(createTet(newn, 5, 9, 10, 13));
      fem.addElement(createTet(newn, 6, 10, 11, 13));
      fem.addElement(createTet(newn, 7, 11, 12, 13));
      fem.addElement(createTet(newn, 8, 12, 9, 13));
   }
   
   /**
    * Creates a subdvided FEM model by subdividing all the elements of an
    * existing model into eight sub-elements, adding additional nodes as
    * required. The existing model is not modified. At present, this is
    * supported only for models composed of tetrahedra, hexahedra, and wedges.
    * Markers in the original mesh are copied, but attachments (T-junction
    * connections) are not. Likewise, if the original FEM is a FemMuscleModel,
    * the muscle group information is not copied either.
    * 
    * @param femr
    * model in which refined FEM is to be constructed, or <code>null</code> if
    * the model is to be created from scratch.
    * @param fem0
    * existing FEM model to be refined.
    */
   public static FemModel3d subdivideFem (FemModel3d femr, FemModel3d fem0) {
      return subdivideFem(femr, fem0, true);
   }

   public static FemModel3d subdivideFem (
      FemModel3d femr, FemModel3d fem0, boolean addMarkers) {

      if (fem0 == null) {
         throw new IllegalArgumentException("fem0 must not be null");
      }
      if (femr == fem0) {
         throw new IllegalArgumentException("femr and fem0 must be different");
      }
      if (femr == null) {
         femr = new FemModel3d();
      } else {
         femr.clear();
      }
      for (FemElement3d e : fem0.myElements) {
         if (!(e instanceof TetElement) && !(e instanceof HexElement)
            && !(e instanceof WedgeElement) && !(e instanceof PyramidElement)) {
            throw new IllegalArgumentException(
               "fem0 must contain only test, hexs, pyramids, or wedges");
         }
      }

      int flags = CopyableComponent.COPY_REFERENCES;
      HashMap<ModelComponent,ModelComponent> copyMap =
         new HashMap<ModelComponent,ModelComponent>();
      HashMap<Edge,FemNode3d> edgeNodeMap = new HashMap<Edge,FemNode3d>();

      for (FemNode3d n : fem0.myNodes) {
         FemNode3d newn = n.copy(flags, copyMap);
         newn.setName(n.getName());
         copyMap.put(n, newn);
         femr.myNodes.add(newn);
      }

      for (FemElement3d e : fem0.myElements) {
         if (e instanceof TetElement) {
            subdivideTet(femr, (TetElement)e, edgeNodeMap, copyMap);
         } else if (e instanceof HexElement) {
            subdivideHex(femr, (HexElement)e, edgeNodeMap, copyMap);
         } else if (e instanceof WedgeElement) {
            subdivideWedge(femr, (WedgeElement)e, edgeNodeMap, copyMap);
         } else if (e instanceof PyramidElement) {
            subdividePyramid(femr, (PyramidElement)e, edgeNodeMap, copyMap);
         }
      }
      
      if (addMarkers) {
         for (FemMarker m : fem0.myMarkers) {
            FemMarker newm = new FemMarker(m.getPosition());
            newm.setName(m.getName());
            fem0.addMarker(newm);
         }
      }
      // Doesn't clone attachments yet. Should do this ...
      // for (DynamicAttachment a : fem1.myAttachments) {
      // DynamicAttachment newa = a.copy (flags, copyMap);
      // newa.setName (a.getName());
      // fem0.myAttachments.add (newa);
      // }
      return femr;
   }

   /**
    * Creates a new model by duplicating nodes and elements
    * 
    * @param out
    * model to fill
    * @param elemList
    * elements to build model from
    */
   public static void createFromElementList(
      FemModel3d out, Collection<FemElement3d> elemList) {

      HashMap<FemNode3d,FemNode3d> nodeMap = new HashMap<FemNode3d,FemNode3d>();

      for (FemElement3d elem : elemList) {
         FemNode3d[] oldNodes = elem.getNodes();
         FemNode3d[] newNodes = new FemNode3d[elem.numNodes()];
         for (int i = 0; i < newNodes.length; i++) {
            newNodes[i] = nodeMap.get(oldNodes[i]);
            if (newNodes[i] == null) {
               newNodes[i] = new FemNode3d(oldNodes[i].getPosition());
               nodeMap.put(oldNodes[i], newNodes[i]);
               out.addNode(newNodes[i]);
            }
         }

         FemElement3d newElem = FemElement3d.createElement(newNodes);
         out.addElement(newElem);
      }
   }
   
   public static void setPlanarNodesFixed (
      FemModel fem, Point3d center, Vector3d normal, boolean fixed) {

      double off = normal.dot(center);
      double tol = RenderableUtils.getRadius (fem)*1e-12;
      for (FemNode n : fem.getNodes()) {
         double d = normal.dot(n.getPosition());
         if (Math.abs (d-off) <= tol) {
            n.setDynamic (!fixed);
         }
      }
   }

}
