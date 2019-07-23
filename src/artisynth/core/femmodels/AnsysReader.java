/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;
import maspack.widgets.WidgetDialog;

/**
 * A class to read an FEM described in the Abaqus file format.
 */
public class AnsysReader implements FemReader {

   /** 
    * Tells the reader to subdivide each hexahedral element into five
    * tetrahedra.
    */
   public static int TETRAHEDRALIZE_HEXES = 0x1;

   /** 
    * Tells the reader to number the nodes and elements starting from one.
    * This is the same convention as used by ANSYS.
    */   
   public static int ONE_BASED_NUMBERING = 0x2;

   static boolean cwHexWarningGiven;
   static boolean cwWedgeWarningGiven;
   static boolean cwPyramidWarningGiven;
   static boolean cwTetWarningGiven; 
   static boolean nodeIdWarningGiven;

   private File myNodeFile;
   private File myElemFile;
   
   public AnsysReader(File nodes, File elems) {
      myNodeFile = nodes;
      myElemFile = elems;
   }
   
   public AnsysReader(String nodeFile, String elemFile) {
      myNodeFile = new File(nodeFile);
      myElemFile = new File(elemFile);
   }
   
   @Override
   public FemModel3d readFem(FemModel3d fem) throws IOException {
      return read(fem, myNodeFile.getAbsolutePath(), myElemFile.getAbsolutePath());
   }
   
   /**
    * Creates an FemModel with uniform density based on ANSYS data contained in
    * a specified file. The node coordinate data can be scaled non-uniformly
    * using an optional parameter giving scale values about the x, y, and z
    * axes.
    * 
    * @param model
    * FEM model to be populated by ANSYS data. If <code>null</code>, a
    * new model is created
    * @param nodeFileName
    * path name of the ANSYS node file
    * @param elemFileName
    * path name of the ANSYS element file
    * @return created model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, String nodeFileName, String elemFileName)
      throws IOException {

      return read(model, nodeFileName, elemFileName, -1, null, 0);
   }
   
   /**
    * Creates an FemModel with uniform density based on ANSYS data contained in
    * a specified file. The node coordinate data can be scaled non-uniformly
    * using an optional parameter giving scale values about the x, y, and z
    * axes.
    * 
    * @param model
    * FEM model to be populated by ANSYS data. If <code>null</code>, a
    * new model is created 
    * @param nodeFileName
    * path name of the ANSYS node file
    * @param elemFileName
    * path name of the ANSYS element file
    * @param density
    * density of the model
    * @param scale
    * if non-null, gives scaling about the x, y, and z axes
    * @param options
    * option flags. Should be an or-ed combination of
    * {@link #TETRAHEDRALIZE_HEXES} and {@link #ONE_BASED_NUMBERING}.
    * @return created model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, String nodeFileName, String elemFileName,
      double density, Vector3d scale, int options)
      throws IOException {

      Reader nodeReader = null;
      Reader elemReader = null;

      try {
         nodeReader = new FileReader (nodeFileName);
         elemReader = new FileReader (elemFileName);
         model = read (model, nodeReader, elemReader, density, scale, options);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (nodeReader != null) {
            nodeReader.close ();
         }
         if (elemReader != null) {
            elemReader.close ();
         }
      }
      
      return model;
   }
   
   /**
    * Creates an FemModel with uniform density based on ANSYS data contained in
    * a specified file. The node coordinate data can be scaled non-uniformly
    * using an optional parameter giving scale values about the x, y, and z
    * axes.
    * 
    * @param model
    * FEM model to be populated by ANSYS data. If <code>null</code>, a
    * new model is created
    * @param nodeReader
    * reader supplying node data in the ANSYS format
    * @param elemReader
    * reader supplying element data in the ANSYS format
    * @param density
    * density of the model
    * @param scale
    * if non-null, gives scaling about the x, y, and z axes
    * @param options
    * option flags. Should be an or-ed combination of
    * {@link #TETRAHEDRALIZE_HEXES} and {@link #ONE_BASED_NUMBERING}.
    * @return created model
    * @throws IOException
    * if this is a problem reading the file
    */
   public static FemModel3d read (
      FemModel3d model, Reader nodeReader, Reader elemReader,
      double density, Vector3d scale, int options) throws IOException {

      boolean tetrahedralize = (options & TETRAHEDRALIZE_HEXES) != 0;
      boolean useAnsysNum = (options & ONE_BASED_NUMBERING) != 0;

      if (model == null) {
         model = new FemModel3d();
      } else {
         model.clear ();
      }
      if (density >= 0) {
         model.setDensity (density);
      }
      
      if (useAnsysNum) {
         model.useAnsysNumbering ();
      }
      
      LinkedHashMap<Integer, Point3d> nodeMap = readNodeFile (nodeReader, useAnsysNum);
      LinkedHashMap<Integer, Integer> nodeIdMap = 
         new LinkedHashMap<Integer, Integer> ();
      
      for (int nodeId : nodeMap.keySet ()) {
         Point3d pos = nodeMap.get (nodeId);
         if (scale != null) {
            pos.x *= scale.x;
            pos.y *= scale.y;
            pos.z *= scale.z;
         }
         
         FemNode3d node = new FemNode3d (pos);
         model.addNumberedNode (node, nodeId);
         
         // Store new node ID to match with element node IDs
         nodeIdMap.put (nodeId, node.getNumber ());
      }
      
      LinkedHashMap<Integer, ArrayList<Integer>> elemMap =
         readElemFile (elemReader, useAnsysNum);
      ArrayList<HexElement> hexElems = new ArrayList<HexElement> ();
      
      boolean flip = false;	
      if (scale != null) {
      double scaleProduct = scale.x*scale.y*scale.z;
      flip = (scaleProduct < 0);         	    
      }
      
      
      for (int elemId : elemMap.keySet ()) {
         ArrayList<Integer> elemNumList = elemMap.get (elemId);
         int[] attrList = new int[5];
         
         for (int i = attrList.length - 1 ; i >= 0 ; i--) {
            attrList[i] = elemNumList.remove (0);
         }
         
         ArrayList<Integer> nodeList = 
            getValidElemNodes (elemNumList, nodeIdMap, flip);
         
         switch (nodeList.size ()) {
            case 4:
               createTet (model, nodeList, elemId, attrList);
               break;
            case 5:
               createPyramid (model, nodeList, elemId, attrList);
               break;
            case 6:
               createWedge (model, nodeList, elemId, attrList);
               break;
            case 8:
               hexElems.add (createHex (model, nodeList, elemId, attrList));
               break;
            case 10:
               createQuadTet (model, nodeList, elemId, attrList);
               break;
            case 20:
               createQuadHex (model, nodeList, elemId, attrList);
               break;
            default:
               System.out.println ("Element "+elemId+": unknown type with " +
                  nodeList.size() + " nodes; ignoring");
         }
      }
      
      // TODO implement for quadhex elements
      HexElement.setParities (hexElems);

      if (tetrahedralize) { // replace all hex elements with tets
         for (HexElement hex : hexElems) {
            FemNode3d[] n = hex.getNodes ();
            TetElement[] tets =
               TetElement.createCubeTesselation (
                  n[0], n[1], n[2], n[3], n[4], n[5], n[6], n[7], hex
                     .getParity () == 1);
            model.removeElement (hex);
            for (TetElement tet : tets) {
               model.addElement (tet);
            }
         }
      }
      
      return model;
   }
   
   protected static int computeWidths (List<Integer> widths, String line) 
      throws IOException {
      
      int max = line.length();
      int off = 0;
      int width = 0;
      
      // skip leading white space
      while (off < max) {
         width = 0;
         
         // leading white space
         while (off < max && Character.isWhitespace((line.charAt(off)))) {
            off++;
            width++;
         }
         // digits
         while (off < max && Character.isDigit((line.charAt(off)))) {
            off++;
            width++;
         }
         
         widths.add(width);
      }
      
      return widths.size();
   }

   protected static int parseNumber (
      ArrayList<Integer> numbers, String line, int off, int maxWidth, int lineno) 
      throws IOException {
      
      int max = line.length();
      // limit maximum digits for the first 13 numbers
      if (numbers.size() < 13 && maxWidth > 0) {
         max = Math.min (max, off+maxWidth);  // maximum number width
      }
      int c = 0;
      // skip leading white space
      while (off < max && Character.isWhitespace((c=line.charAt(off)))) {
         off++;
      }
      if (off == max) {
         return -1;
      }
      if (!Character.isDigit(c)) {
         throw new IOException (
            "Error: non-digit '"+c+"' in file at line "+lineno);
      }
      int num = 0;
      while (off < max && Character.isDigit((c=line.charAt(off)))) {
         num = 10*num + (c-'0');
         off++;
      }
      numbers.add (num);
      return off;
   }

   public static LinkedHashMap<Integer, ArrayList<Integer>> readElemFile ( 
      Reader elemReader, boolean useAnsysNum) throws IOException {
      
      LinkedHashMap<Integer, ArrayList<Integer>> elemPositions = 
         new LinkedHashMap<Integer, ArrayList<Integer>> ();
      // ReaderTokenizer rtok =
      //    new ReaderTokenizer (new BufferedReader (elemReader));
      //rtok.eolIsSignificant (true);
      
      int offset = useAnsysNum ? 0 : -1;
      
      int elemId = 0;
      String line;
      int lineno = 0;
      BufferedReader reader = new BufferedReader (elemReader);
      line = reader.readLine();
      
      // compute widths
      ArrayList<Integer> widths = new ArrayList<Integer>(14);
      computeWidths(widths, line);
      widths.add(0); // terminal width to prevent crashes if no space at end of line
      
      while (line != null) {
         
         ArrayList<Integer> numbers = new ArrayList<Integer> ();
         ArrayList<Integer> elemNumList;
         lineno++;

         int off = 0;
         int nidx = 0;
         while ( (off = parseNumber (numbers, line, off, widths.get(nidx), lineno)) != -1) {
            nidx++;
            // break if at the end of the line
            if(off >= line.length()) {
               break;
            }
         }
         
         if (numbers.size() == 14) {
            elemNumList = new ArrayList<Integer> ();
            
            for (int i = 0; i < 8; i++) {
               elemNumList.add (numbers.get (i) + offset);
            }
            for (int i = 8; i < 13; i++) {
               elemNumList.add (0, numbers.get (i));
            }
            
            elemId = numbers.get (13);
         }
         else {
            elemNumList = elemPositions.get (elemId + offset);
            
            for (int i = 0; i < numbers.size(); i++) {
               elemNumList.add (numbers.get (i) + offset);
            }
         }
         elemPositions.put (elemId + offset, elemNumList);
         
         // queue-up next line
         line = reader.readLine();
      }
	   
      return elemPositions;
   }
   
   protected static LinkedHashMap<Integer, ArrayList<Integer>> readElemFileOld ( 
      Reader elemReader, boolean useAnsysNum) throws IOException {
      
      LinkedHashMap<Integer, ArrayList<Integer>> elemPositions = 
         new LinkedHashMap<Integer, ArrayList<Integer>> ();
      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (elemReader));
      rtok.eolIsSignificant (true);
      
      int offset = useAnsysNum ? 0 : -1;
      
      int elemId = 0;
      while (rtok.nextToken () != ReaderTokenizer.TT_EOF) {
         rtok.pushBack ();
         
         ArrayList<Integer> curLine = new ArrayList<Integer> ();
         ArrayList<Integer> elemNumList;
         
         int nextToken = rtok.nextToken ();
         while (nextToken != ReaderTokenizer.TT_EOL &&
                nextToken != ReaderTokenizer.TT_EOF) {
            curLine.add ((int) rtok.nval);
            nextToken = rtok.nextToken ();
         }
         
         if (curLine.size() == 14) {
            elemNumList = new ArrayList<Integer> ();
            
            for (int i = 0; i < 8; i++) {
               elemNumList.add (curLine.get (i) + offset);
            }
            for (int i = 8; i < 13; i++) {
               elemNumList.add (0, curLine.get (i));
            }
            
            elemId = curLine.get (13);
         }
         else {
            elemNumList = elemPositions.get (elemId + offset);
            
            for (int i = 0; i < curLine.size(); i++) {
               elemNumList.add (curLine.get (i) + offset);
            }
         }
         
         elemPositions.put (elemId + offset, elemNumList);
      }
	   
      return elemPositions;
   }
   
   public static LinkedHashMap<Integer, Point3d> readNodeFile (
      Reader nodeReader, boolean useAnsysNum) throws IOException {

      LinkedHashMap<Integer, Point3d> nodePositions = 
         new LinkedHashMap<Integer, Point3d> ();
      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (nodeReader));
      rtok.eolIsSignificant (true);
      
      int offset = useAnsysNum ? 0 : -1;

      while (rtok.nextToken () != ReaderTokenizer.TT_EOF) {
         rtok.pushBack ();
         int nodeId = rtok.scanInteger ();

         Point3d pos = new Point3d ();
         
         double[] posArray = new double[3];
         
         for (int i = 0; i < posArray.length; i++) {
            int nextToken = rtok.nextToken();
            if (nextToken == ReaderTokenizer.TT_EOL) {
               throw new IOException ("Unexpected EOL, " + rtok);
            }
            else if (nextToken == ReaderTokenizer.TT_NUMBER) {
               posArray[i] = rtok.nval;
            }
            else {
               i--;
            }
         }

         pos.x = posArray[0];
         pos.y = posArray[1];
         pos.z = posArray[2];
         nodePositions.put (nodeId + offset, pos);
         
         int nextToken;
         do {
            nextToken = rtok.nextToken ();
         } while (nextToken != ReaderTokenizer.TT_EOL &&
                  nextToken != ReaderTokenizer.TT_EOF);
      }

      return nodePositions;
   }
   
   private static ArrayList<Integer> getValidElemNodes (
      ArrayList<Integer> nodeList, HashMap<Integer, Integer> nodeIdMap,
      boolean reverse) {

      int nNodes = nodeList.size();
      ArrayList<Integer> validNodeIds = new ArrayList<Integer> (nNodes);
      
      for (int i = 0; i < nNodes; i++) {
         int nodeId;
         nodeId = nodeList.get (i);
         
         if (nodeId >= 0) {
            Integer validId = nodeIdMap.get (nodeId);
            if (validId != null && !validNodeIds.contains (validId)) {
               validNodeIds.add (validId);
            }
         }
      }
      
      return validNodeIds;
   }

   private static void createTet (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId, int[] attrList) {
      
      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      
      TetElement e;

      if (TetElement.computeVolume (n0, n1, n2, n3) >= 0) {
         e = new TetElement (n0, n1, n2, n3);
      }
      else {
         e = new TetElement (n0, n2, n1, n3);

         if (cwTetWarningGiven) {
            System.out.println ("found cw tet");
            cwTetWarningGiven = true;
         }
      }
      
      model.addNumberedElement (e, elemId);
      model.ansysElemProps.put (e, attrList);
   }
   
   private static void createQuadTet (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId, int[] attrList) {
      
      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      FemNode3d n4 = model.getByNumber (nodeIds.get (4));
      FemNode3d n5 = model.getByNumber (nodeIds.get (5));
      FemNode3d n6 = model.getByNumber (nodeIds.get (6));
      FemNode3d n7 = model.getByNumber (nodeIds.get (7));
      FemNode3d n8 = model.getByNumber (nodeIds.get (8));
      FemNode3d n9 = model.getByNumber (nodeIds.get (9));
      
      QuadtetElement e;
      
      if (TetElement.computeVolume (n0, n1, n2, n3) >= 0) {
         e = new QuadtetElement (n0, n1, n2, n3, n4, n5, n6,
               n7, n8, n9);
      }
      else {
         e = new QuadtetElement (n0, n2, n1, n3, n6, n5, n4,
               n7, n9, n8);
         
         if (cwTetWarningGiven) {
            System.out.println ("found ccw quadtet");
            cwTetWarningGiven = true;
         }
      }
      
      model.addNumberedElement (e, elemId);
      model.ansysElemProps.put (e, attrList);
   }
   
   private static HexElement createHex (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId, int[] attrList) {

      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      FemNode3d n4 = model.getByNumber (nodeIds.get (4));
      FemNode3d n5 = model.getByNumber (nodeIds.get (5));
      FemNode3d n6 = model.getByNumber (nodeIds.get (6));
      FemNode3d n7 = model.getByNumber (nodeIds.get (7));
      
      HexElement e;
      
      if (HexElement.computeVolume (n3, n2, n1, n0, n7, n6, n5, n4) >= 0) {
         e = new HexElement (n3, n2, n1, n0, n7, n6, n5, n4);
      }
      else {
         e = new HexElement (n0, n1, n2, n3, n4, n5, n6, n7);

         if (!cwHexWarningGiven) {
            System.out.println ("found cw hex");
            cwHexWarningGiven = true;
         }
         
         if (e.computeVolumes () < 0) {
            System.out.println ("found neg volume hex, # " + e.getNumber ());
         }
      }
      
      model.addNumberedElement (e, elemId);
      model.ansysElemProps.put (e, attrList);
      
      return e;
   }
   
   private static void createQuadHex (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId, int[] attrList) {

      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      FemNode3d n4 = model.getByNumber (nodeIds.get (4));
      FemNode3d n5 = model.getByNumber (nodeIds.get (5));
      FemNode3d n6 = model.getByNumber (nodeIds.get (6));
      FemNode3d n7 = model.getByNumber (nodeIds.get (7));
      FemNode3d n8 = model.getByNumber (nodeIds.get (8));
      FemNode3d n9 = model.getByNumber (nodeIds.get (9));
      FemNode3d n10 = model.getByNumber (nodeIds.get (10));
      FemNode3d n11 = model.getByNumber (nodeIds.get (11));
      FemNode3d n12 = model.getByNumber (nodeIds.get (12));
      FemNode3d n13 = model.getByNumber (nodeIds.get (13));
      FemNode3d n14 = model.getByNumber (nodeIds.get (14));
      FemNode3d n15 = model.getByNumber (nodeIds.get (15));
      FemNode3d n16 = model.getByNumber (nodeIds.get (16));
      FemNode3d n17 = model.getByNumber (nodeIds.get (17));
      FemNode3d n18 = model.getByNumber (nodeIds.get (18));
      FemNode3d n19 = model.getByNumber (nodeIds.get (19));
      
      QuadhexElement e;
      
      if (HexElement.computeVolume (n3, n2, n1, n0, n7, n6, n5, n4) >= 0) {
         e = new QuadhexElement (
            new FemNode3d[] {n3, n2, n1, n0, n7, n6, n5, n4, n10, n9, n8, n11,
                             n14, n13, n12, n15, n19, n18, n17, n16});
      }
      else {
         e = new QuadhexElement (
            new FemNode3d[] {n0, n1, n2, n3, n4, n5, n6, n7, n8, n9, n10, n11,
                             n12, n13, n14, n15, n16, n17, n18, n19});

         if (!cwHexWarningGiven) {
            System.out.println ("found cw quadhex");
            cwHexWarningGiven = true;
         }
         
         if (e.computeVolumes () < 0) {
            System.out.println ("found neg volume quadhex, # " + e.getNumber ());
         }
      }  
      
      model.addNumberedElement (e, elemId);
      model.ansysElemProps.put (e, attrList);
   }

   private static void createPyramid (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId, int[] attrList) {
      
      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      FemNode3d n4 = model.getByNumber (nodeIds.get (4));

      PyramidElement e = new PyramidElement (n0, n1, n2, n3, n4);

      if (e.computeVolumes () < 0) {
         e = new PyramidElement (n0, n3, n2, n1, n4);
         
         if (!cwPyramidWarningGiven) {
            System.out.println ("found ccw pyramid");
            cwPyramidWarningGiven = true;
         }
         
         if (e.computeVolumes () < 0) {
            System.out.println ("found inverted pyramid, # " + e.getNumber ());
         }
      }
      
      model.addNumberedElement (e, elemId);
      model.ansysElemProps.put (e, attrList);
   }

   private static void createWedge (FemModel3d model, 
      ArrayList<Integer> nodeIds, int elemId, int[] attrList) {
      
      FemNode3d n0 = model.getByNumber (nodeIds.get (0));
      FemNode3d n1 = model.getByNumber (nodeIds.get (1));
      FemNode3d n2 = model.getByNumber (nodeIds.get (2));
      FemNode3d n3 = model.getByNumber (nodeIds.get (3));
      FemNode3d n4 = model.getByNumber (nodeIds.get (4));
      FemNode3d n5 = model.getByNumber (nodeIds.get (5));

      WedgeElement e = new WedgeElement (n0, n1, n2, n3, n4, n5);

      if (e.computeVolumes () < 0) {
         e = new WedgeElement (n2, n1, n0, n5, n4, n3);
         
         if (!cwWedgeWarningGiven) {
            System.out.println ("found ccw wedge");
            cwWedgeWarningGiven = true;
         }
         
         if (e.computeVolumes () < 0) {
            System.out.println ("found inverted wedge, # " + e.getNumber ());
         }
      }
      
      model.addNumberedElement (e, elemId);
      model.ansysElemProps.put (e, attrList);
   }

   // private static void createTetsFromHex (FemModel3d model, int[] idxs) {

   //    TetElement[] elems =
   //       TetElement.createCubeTesselation (model.getNode (idxs[0]), model
   //          .getNode (idxs[1]), model.getNode (idxs[2]), model
   //          .getNode (idxs[3]), model.getNode (idxs[4]), model
   //          .getNode (idxs[5]), model.getNode (idxs[6]), model
   //          .getNode (idxs[7]), true);
   //    for (TetElement tet : elems)
   //       model.addElement (tet);
   // }

   // private static void createTetsFromWedge (FemModel3d model, int[] idxs) {
   //    TetElement[] elems =
   //       TetElement.createWedgeTesselation (model.getNode (idxs[0]), model
   //          .getNode (idxs[2]), model.getNode (idxs[1]), model
   //          .getNode (idxs[3]), model.getNode (idxs[5]), model
   //          .getNode (idxs[4]), true);
   //    for (TetElement tet : elems)
   //       model.addElement (tet);
   // }

   public static Integer[] readNodeIdxs (Reader nodeReader) throws IOException {

      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (nodeReader));
      Point3d coords = new Point3d ();
      
      ArrayList<Integer> nodeIds = new ArrayList<Integer> ();
      while (rtok.nextToken () != ReaderTokenizer.TT_EOF) {
         rtok.pushBack ();
         int nodeId = rtok.scanInteger ();

         nodeIds.add (nodeId);
         coords.x = rtok.scanNumber ();
         coords.y = rtok.scanNumber ();
         coords.z = rtok.scanNumber ();
      }
      return nodeIds.toArray (new Integer[0]);
   }
}
