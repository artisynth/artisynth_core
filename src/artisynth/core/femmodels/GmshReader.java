/**
 * Copyright (c) 2024, by the Authors: John E Lloyd (UBC), Isaac McKay (USASK).
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;

import maspack.util.ReaderTokenizer;

/**
 * Reads FEM mesh geometry from a Gmsh {@code .msh} file.
 */
public class GmshReader extends FemReaderBase {

   static ElementNodeNumbering myNodeNumbering;
   static {
      ElementNodeNumbering numbering = new ElementNodeNumbering();

      // numberings are the same as ArtiSynth except for the following:
      numbering.set (
         ElemType.HEX, new int[] { 0, 3, 2, 1, 4, 7, 6, 5 });
      numbering.set (
         ElemType.QUADTET, new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 9, 8 });
      numbering.set (
         ElemType.QUADHEX, new int[] {
            0, 3, 2, 1, 4, 7, 6, 5, 11, 8, 16, 10, 19, 9, 18, 17, 15, 12, 14,13});
      numbering.set (
         ElemType.QUADWEDGE, new int[] {
            0, 1, 2, 3, 4, 5, 6, 8, 12, 7, 13, 14, 9, 11, 10 });
      numbering.set (
         ElemType.QUADPYRAMID, new int[] {
            0, 1, 2, 3, 4, 5, 8, 9, 6, 10, 7, 11, 12 });

      myNodeNumbering = numbering;
   }
   
   /**
    * Creates a GmshReader to read from a specified file. The actual reading of
    * the FEM geometry is done using {@link #readFem}.
    * 
    * @param file file to read from
    * @throws IOException
    */
   public GmshReader (File file) throws IOException {
      super (file);
   }
   
   /**
    * Creates a GmshReader to read from a specified file. The actual reading of
    * the FEM geometry is done using {@link #readFem}.
    * 
    * @param filePath path name of the file to read from
    * @throws IOException
    */
   public GmshReader (String filePath) throws IOException {
      super (new File(filePath));
   }
   
   /**
    * Creates a GmshReader to read from a specified Reader. The actual reading
    * of the FEM geometry is done using {@link #readFem}.
    * 
    * @param reader reader to read from
    * @throws IOException
    */
   public GmshReader (Reader reader) throws IOException {
      super (reader);
   }
   
   /**
    * Creates an FEM and reads its geometry from a specifed file.
    *
    * @param filePath path name of the file to read from
    * @return created FEM model
    * @throws IOException
    */
   public static FemModel3d read (String filePath) throws IOException {
      return read (new File(filePath));
   }
  
   /**
    * Creates an FEM and reads its geometry from a specifed file.
    * 
    * @param file file to read from
    * @return created FEM model
    * @throws IOException
    */
   public static FemModel3d read (File file) throws IOException {
      return read (null, file, 0);
   }
  
   /**
    * Reads the geometry for an FEM model from a specified file.
    *
    * @param fem FEM model to be populated by geometry data. If
    * <code>null</code>, a new model is created
    * @param file file to read from
    * @param options flags controlling how the input is read.
    * Current flags include {@link #PRSERVE_NUMBERING},
    * {@link #PRSERVE_NUMBERING_BASE0}, and
    * {@link #SUPPRESS_WARNINGS}.
    * @return created FEM model
    * @throws IOException
    */
   public static FemModel3d read (FemModel3d fem, File file, int options)
      throws IOException {
      if (fem == null) {
         fem = new FemModel3d();
      }
      GmshReader femReader = new GmshReader (file);
      femReader.setOptionsFromFlags (options);
      try {
         femReader.readFem (fem);
         return fem;
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         femReader.close();
      }
   }
  
   /**
    * Reads an FEM from a specifed file.
    * 
    * @param fem FEM whose geometry should be read from the file. If {@code
    * null}, then an FEM is created.
    * @param file file to read from
    * @param shellThickness default thickness to use for shell or membrane
    * elements, if not specified in the file
    * @param shellType default shell type to use, if not specified in the file
    * @param options option flags. Flags include {@link #PRESERVE_NUMBERING}
    * and {@link #PRESERVE_NUMBERING_BASE0}.
    * @return FEM that was read
    * @throws IOException
    */
   public static FemModel3d read (
      FemModel3d fem, File file,
      double shellThickness, ShellType shellType, int options) 
      throws IOException {
      if (fem == null) {
         fem = new FemModel3d();
      }
      GmshReader femReader = new GmshReader (file);
      femReader.setShellThickness (shellThickness);
      femReader.setShellType (shellType);
      femReader.setOptionsFromFlags (options);
      try {
         femReader.readFem (fem);
         return fem;
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         femReader.close();
      }
   }
  
   /**
    * Reads an FEM from the file or reader associated with this GmshReader.
    * 
    * @param fem FEM whose geometry should be read. If {@code
    * null}, then an FEM is created.
    * @return FEM that was read
    * @throws IOException
    */
   public FemModel3d readFem (FemModel3d fem)
      throws IOException {

      if (fem == null) {
         fem = new FemModel3d();
      }
      if (myNumbering == Numbering.PRESERVE) {
         fem.setOneBasedNodeElementNumbering(true);
      }
      ReaderTokenizer rtok = new ReaderTokenizer (myReader);
      String thisLine = rtok.readLine();

      if (!startsWith (thisLine, "$MeshFormat")) {
         throw new IOException ("Expecting $MeshFormat as first token");
      }

      // read the version and check that it is 4.X
      double version = rtok.scanNumber();
      if ((int)version != 4 && !mySuppressWarnings) {
         System.out.println (
            "WARNING: Possibly unsupported Gmsh file version "+version);
      }
      // make sure format is not binary
      if (rtok.scanInteger() == 1) {
         throw new IOException ("Binary file format for gmsh "
         + ".msh files not supported");
      }
      
      rtok.scanInteger(); // scan size
      
      // scan lines until we find the nodes
      while (!startsWith (rtok.readLine(), "$Nodes")) {
      }
      
      // scan nodes information
      int numNodeEntityBlocks = rtok.scanInteger();
      int numNodes = rtok.scanInteger ();
      
      if (numNodeEntityBlocks < 0 || numNodes < 0) {
         throw new IOException("Number of entity blocks or"
         + " nodes cannot be less than zero");
      }
      
      int minNodeTag = rtok.scanInteger ();
      int maxNodeTag = rtok.scanInteger ();

      // Since we know the maxNodeTag, we can use an array to map tags to nodes
      FemNode3d[] nodeMap = new FemNode3d[maxNodeTag+1];

      // collect all read nodes into a list of nodes
      ArrayList<NodeDesc> nodeDescs = new ArrayList<>(numNodes);
      
      // read the nodes in each node entity block. Ignore if the entity block
      // dimension is not 2 (surfaces) or 3 (volumes)
      for (int i = 0; i < numNodeEntityBlocks; i++) {                  
         int dim = rtok.scanInteger ();
         int entityTag = rtok.scanInteger();
         //NodeEntityBlockX e = new NodeEntityBlockX(entityTag);
         boolean parametric = rtok.scanInteger() == 1 ? true: false;
         int numNodesInBlock= rtok.scanInteger ();
         if (numNodesInBlock < 0) {
            throw new IOException(
               "Node entity block " + entityTag +
               ": cannot have less than zero nodes");
         }
         if (dim != 2 && dim != 3) {
            // read the tags and node coordinates and discard them
            for (int j = 0; j < numNodesInBlock; j++) {
               rtok.scanInteger();
            }
            for (int j = 0; j < numNodesInBlock; j++) {
               rtok.scanNumber();
               rtok.scanNumber();
               rtok.scanNumber();
            }
         }
         else {
            // read and create the nodes
            int[] tags = new int[numNodesInBlock];
            for (int j = 0; j < numNodesInBlock; j++) {
               tags[j] = rtok.scanInteger();
            }
            for (int j = 0; j < numNodesInBlock; j++) {
               double x = myScaling.x*rtok.scanNumber();
               double y = myScaling.y*rtok.scanNumber();
               double z = myScaling.z*rtok.scanNumber();
               FemNode3d node = new FemNode3d (x, y, z);
               int nodeId = tags[j];
               nodeDescs.add (new NodeDesc (node, nodeId));
               nodeMap[nodeId] = node;
            }
         }
      }
      rtok.readLine ();
      String line = rtok.readLine ();
      if (!startsWith (line, "$EndNodes")) {
         throw new IOException("Unexpected end to nodes section; was expecting"
         + "\"$EndNodes\", got \"" + line +"\" on line " + rtok.lineno ());
      }

      // Sort the nodes by tag number and then add them to the model. This will
      // help create a more ordered node list, and will also ensure that if the
      // Gmsh file was created by ArtiSynth (using GmshWriter), then the nodes
      // will be reconstructed in the correct sequence.
      Collections.sort (
         nodeDescs,
         (n0, n1) -> (n0.myTag<n1.myTag ? -1 : (n0.myTag==n1.myTag ? 0 : 1)));
      for (NodeDesc nd : nodeDescs) {
         addNode (fem, nd.myNode, nd.myTag);
      }
      
      // skip until the elements section
      while (!startsWith (rtok.readLine(), "$Elements")) {
      }
      
      int numElemEntityBlocks = rtok.scanInteger ();
      int numElements = rtok.scanInteger ();
      if (numElemEntityBlocks < 0 || numElements < 0) {
         throw new IOException("Num entity blocks or elements cannot be less"
         + "than zero!"); 
      }
      
      int minElementTag = rtok.scanInteger ();
      int maxElementTag = rtok.scanInteger ();
      ArrayList<ElemDesc> elemDescs =
         new ArrayList<>(maxElementTag-minElementTag+1);

      int[] nodeTags = new int[20]; // node numbers according to Gmsh
      // read over all the elements in all the entity blocks
      for (int i = 0; i < numElemEntityBlocks; i++) {
         int dim = rtok.scanInteger ();
         int tag = rtok.scanInteger ();
         int typeNum = rtok.scanInteger ();
         int numElementsInBlock = rtok.scanInteger ();

         ElemType type = null;
         if (dim == 3) {
            switch (typeNum) {
               case 4:  type = ElemType.TET; break;
               case 5:  type = ElemType.HEX; break;
               case 6:  type = ElemType.WEDGE; break;
               case 7:  type = ElemType.PYRAMID; break;
               case 11: type = ElemType.QUADTET; break;
               case 17: type = ElemType.QUADHEX; break;
               case 18: type = ElemType.QUADWEDGE; break;
               case 19: type = ElemType.QUADPYRAMID; break;
               default: type = null;
            }
         }
         else if (dim == 2) {
            switch (typeNum) {
               case 2:  type = ElemType.SHELL_TRI; break;
               case 3:  type = ElemType.SHELL_QUAD; break;
               default: type = null;
            }
         }
         if (type == null) {
            if (!mySuppressWarnings) {
               System.out.println (
                  "WARNING: Element code "+typeNum+
                  " not known by ArtiSynth - skipping");
            }
            rtok.readLine ();
            for (int j=0; j<numElementsInBlock; j++) {
               rtok.readLine ();
            }
         }
         else {
            for (int j=0; j<numElementsInBlock; j++) {
               int elemTag = rtok.scanInteger();
               for (int k=0; k<type.numNodes(); k++) {
                  nodeTags[k] = rtok.scanInteger();
               }
               rtok.readLine();
               FemNode3d[] nodes = new FemNode3d[type.numNodes()];
               myNodeNumbering.findNodes (nodes, type, nodeTags, nodeMap);
               elemDescs.add (new ElemDesc (type, nodes, elemTag));
            }
         }
      }
      line = rtok.readLine ();
      if (!startsWith (line, "$EndElements")) {
         throw new IOException("Unexpected end to elements section; was expecting"
         + "\"$EndElements\", got \"" + line +"\" on line " + rtok.lineno ());
      }
      // Sort the elements by tag number and then add them to the model. This
      // will ensure that if the Gmsh file was created by ArtiSynth (using
      // GmshWriter), then the elements will be reconstructed in the correct
      // sequence.
      Collections.sort (
         elemDescs,
         (e0, e1) -> (e0.myTag<e1.myTag ? -1 : (e0.myTag==e1.myTag ? 0 : 1)));
      for (ElemDesc ed : elemDescs) {
         createAndAddElement (fem, ed);
      }
      myLineCnt = rtok.lineno();
      return fem;
   }
}
