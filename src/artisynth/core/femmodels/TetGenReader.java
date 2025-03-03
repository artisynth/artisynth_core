/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), 
 * Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.*;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;
import artisynth.core.util.ArtisynthPath;

public class TetGenReader implements FemReader {
   
   public static String rbPath1 =
      ArtisynthPath.getHomeRelativePath (
         "src/maspack/geometry/sampleData/", ".");
   public static String rbPath2 =
      ArtisynthPath.getHomeRelativePath (
         "src/artisynth/models/mechdemos/geometry/", ".");
   public static String rbPath3 =
      ArtisynthPath.getHomeRelativePath (
         "classes/artisynth/models/mechdemos/geometry/", ".");
   public static String femPath =
      ArtisynthPath.getHomeRelativePath (
         "src/artisynth/core/femmodels/meshes/", ".");
   public static String workPath =
      ArtisynthPath.getHomeRelativePath (
         "src/maspack/geometry/tetgen/lib/", ".");

   File myNodeFile;
   File myElemFile;
   
   public TetGenReader(File nodes, File elems) {
      myNodeFile = nodes;
      myElemFile = elems;
   }
   
   public TetGenReader(String nodeFile, String elemFile) {
      myNodeFile = new File(nodeFile);
      myElemFile = new File(elemFile);
   }
   
   @Override
   public FemModel3d readFem(FemModel3d fem) throws IOException {
      return read(fem, myNodeFile.getAbsolutePath(), 
         myElemFile.getAbsolutePath());
   }
   
   /*
    * To refine an existing surface mesh with Tetgen: - create a poly file from
    * the obj file containing the surface mesh. - run Tetgen to refine as
    * desired, and create new .node, .ele files. - load a FemModel3d from the
    * new files - save the resulting surface mesh to an .obj file.
    */
   public static void main (String[] args) throws IOException {
      String meshName = "torus274";
      String qual = "";
      // writePolyFileFromSurfaceMesh(meshName, rbPath2, workPath);
      // Go to workPath and run Tetgen. ie. Tetgen -pq1.2a0.1 meshName
      writeSurfaceMeshFromVolumeMesh (workPath + meshName, femPath + meshName
      + qual);
   }

   /*
    * Load a surface mesh from a .obj and write a .poly file for input to
    * Tetgen, containing the surface mesh as a piecewise linear complex.
    */
   public static void writePolyFileFromSurfaceMesh (
      String meshName, String inPath, String outPath) throws Exception {
      PolygonalMesh pm =
         new PolygonalMesh (new File (inPath + meshName + ".obj"));
      pm.triangulate();
      pm.writePoly (outPath + meshName + ".poly");
      System.out.println ("wrote " + outPath + meshName + ".poly");
   }

   // .poly
   /*
    * Load a volumetric mesh from a pair of .node, .ele files (from Tetgen) and
    * write a .obj surface mesh file.
    */
   public static void writeSurfaceMeshFromVolumeMesh (
      String inPath, String outPath) throws IOException {
      FemModel3d fem0 =
         TetGenReader.read (
            "fem0", 5000, inPath + ".1.node", inPath + ".1.ele", new Vector3d (
               1.0, 1.0, 1.0));
      FileWriter fw = new FileWriter (outPath + ".obj");
      String fmt = ""; // "%10.7f";
      fem0.getSurfaceMesh().write (
         new PrintWriter (new BufferedWriter (fw)), fmt);
      fw.close();
      System.out.println ("wrote " + outPath + ".obj");
   }

   public static FemModel3d read (
      FemModel3d model, String nodeFileName, String elemFileName) throws IOException {
      
      read (model, 1, nodeFileName, elemFileName, null);

      return model;
   }
   
   public static FemModel3d read (
      String name, double density, String nodeFileName, String elemFileName,
      Vector3d scale) throws IOException {
      FemModel3d model = new FemModel3d (name);

      read (model, density, nodeFileName, elemFileName, scale);

      return model;
   }

   public static FemModel3d read (
      FemModel3d model, double density, String nodeFileName, String elemFileName,
      Vector3d scale) throws IOException {

      FileReader nodeFile = null;
      FileReader elemFile = null;
      try {
         nodeFile = new FileReader (nodeFileName);
         elemFile = new FileReader (elemFileName);
         model = read (model, density, scale, nodeFile, elemFile);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (nodeFile != null) {
            nodeFile.close();
         }
         if (elemFile != null) {
            elemFile.close();
         }
      }
      return model;
   }

   public static FemModel3d read (
      FemModel3d model, double density, Vector3d scale, Reader nodeReader,
      Reader elemReader) throws IOException {

      if (model == null) {
         model = new FemModel3d();
      }
      
      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (nodeReader));
      model.setDensity (density);
      int numNodes = 0;
      int numAttrs = 0;
      int numBoundaryMkrs = 0;
      rtok.nextToken();
      if (!rtok.tokenIsInteger()) {
         throw new IOException ("Expecting number of nodes, got " + rtok);
      }
      else {
         numNodes = (int)rtok.lval;
      }
      rtok.nextToken();
      if (!rtok.tokenIsInteger() || rtok.lval != 3) {
         throw new IOException ("Expecting dimension number 3, got " + rtok);
      }
      rtok.nextToken();
      if (!rtok.tokenIsInteger()) {
         throw new IOException ("Expecting number of attributes, got " + rtok);
      }
      else {
         numAttrs = (int)rtok.lval;
      }
      rtok.nextToken();
      if (!rtok.tokenIsInteger() || (rtok.lval != 0 && rtok.lval != 1)) {
         throw new IOException (
            "Expecting number of boundary markers (0 or 1), got " + rtok);
      }
      else {
         numBoundaryMkrs = (int)rtok.lval;
      }

      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         if (!rtok.tokenIsInteger()) {
            throw new IOException ("Expecting node index, got " + rtok);
         }
         // int index = (int)rtok.lval;
         Point3d coords = new Point3d();

         for (int i=0; i<3; i++) {
            coords.set (i, rtok.scanNumber());
         }

         // System.out.println(coords);
         if (scale != null) {
            coords.x *= scale.x;
            coords.y *= scale.y;
            coords.z *= scale.z;
         }
         model.addNode (new FemNode3d (coords));
         // scan attributes and boundary markers, if any; these are not used
         for (int i=0; i<numAttrs+numBoundaryMkrs; i++) {
            rtok.scanNumber();
         }
      }

      rtok = new ReaderTokenizer (new BufferedReader (elemReader));

      rtok.nextToken();
      rtok.nextToken();
      rtok.nextToken();

      int indexBase = -1;
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         if (!rtok.tokenIsInteger()) {
            throw new IOException ("Expecting element index, got " + rtok);
         }
         if (indexBase == -1) {
            indexBase = (rtok.nval == 1.0 ? 1 : 0);
         }
         // int index = (int)rtok.lval;

         int[] idxs = new int[4];
         for (int i = 0; i < 4; i++) {
            idxs[i] = rtok.scanInteger()-indexBase;
            // System.out.print(idxs[i] + " ");
         }
         // System.out.println();

         FemNode3d n0 = model.getNode(idxs[0]);
         FemNode3d n1 = model.getNode(idxs[1]);
         FemNode3d n2 = model.getNode(idxs[2]);
         FemNode3d n3 = model.getNode(idxs[3]);

         // check to make sure that the tet is defined so that the
         // first three nodes are arranged clockwise about their face
         TetElement tet;
         if (TetElement.computeVolume (n0, n1, n2, n3) >= 0) {
            tet = new TetElement (n0, n1, n2, n3);
         }
         else {
            tet = new TetElement (n0, n2, n1, n3);
         }
         model.addElement (tet);
      }
      return model;
   }

   /*
    * Read a surface mesh from .face, .node files from Tetgen. Unfortunately
    * this is not very useful as Tetgen does not sequence the vertices in such a
    * way as to have the maspack-generated face normals point outwards.
    */
   public static PolygonalMesh readFaces (
      String nodeFileName, String faceString, Vector3d scale) throws IOException {

      FileReader nodeFile = new FileReader (nodeFileName);
      FileReader faceFile = new FileReader (faceString);
      PolygonalMesh mesh = null;
      try {
         nodeFile = new FileReader (nodeFileName);
         faceFile = new FileReader (faceString);
         mesh = readFaces (scale, nodeFile, faceFile);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (nodeFile != null) {
            nodeFile.close();
         }
         if (faceFile != null) {
            faceFile.close();
         }
      }
      return mesh;
   }

   public static PolygonalMesh readFaces (
      Vector3d scale, Reader nodeReader, Reader faceReader) throws IOException {
      PolygonalMesh mesh = new PolygonalMesh();
      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (nodeReader));
      rtok.nextToken();
      rtok.nextToken();
      rtok.nextToken();
      rtok.nextToken();
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         Point3d coords = new Point3d();
         for (int i = 0; i < 3; i++) {
            coords.set (i, rtok.scanNumber());
         }
         if (scale != null) {
            coords.x *= scale.x;
            coords.y *= scale.y;
            coords.z *= scale.z;
         }
         mesh.addVertex (coords.x, coords.y, coords.z);
      }

      ReaderTokenizer faceFile =
         new ReaderTokenizer (new BufferedReader (faceReader));
      faceFile.nextToken();
      faceFile.nextToken();
      while (faceFile.nextToken() != ReaderTokenizer.TT_EOF) {
         Vertex3d[] vtxs = new Vertex3d[3];
         for (int i = 0; i < vtxs.length; i++) {
            vtxs[i] = mesh.getVertices().get (faceFile.scanInteger());
         }
         faceFile.scanInteger(); // discard
         mesh.addFace (vtxs);
      }
      return mesh;
   }
}
