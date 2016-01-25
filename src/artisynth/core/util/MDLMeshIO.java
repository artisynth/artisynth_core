/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A class to read an PolygonalMesh described from Yohan Payan MDL file format.
 */
public class MDLMeshIO {

   // public static final String DefaultTopHeader = "[Name, STRING]";
   // public static final String DefaultVerticesHeader =
   // "[Vertices, ARRAY1<POINT3D>]";
   // public static final String DefaultNormalsHeader =
   // "[Normals, ARRAY1<VECTOR3D>]";
   // public static final String DefaultFacesHeader =
   // "[Triangles, ARRAY1<STRING>]";

   public static final MDLHeader defaultTopHeader =
      new MDLHeader ("Name", "STRING");
   public static final MDLHeader defaultVerticesHeader =
      new MDLHeader ("Vertices", "ARRAY1<POINT3D>");
   public static final MDLHeader defaultNormalsHeader =
      new MDLHeader ("Normals", "ARRAY1<VECTOR3D>");
   public static final MDLHeader defaultFacesHeader =
      new MDLHeader ("Triangles", "ARRAY1<STRING>");

   /**
    * Creates a PolygonalMesh based on MDL data contained in a specified file.
    * The node coordinate data can be scaled non-uniformly using an optional
    * parameter giving scale values about the x, y, and z axes.
    * 
    * @param fileName
    * path name of the MDL file
    * @param scale
    * if non-null, gives scaling about the x, y, and z axes
    * @return created polygonal mesh
    * @throws IOException
    * if this is a problem reading the file
    */
   public static PolygonalMesh read (String fileName, Vector3d scale)
      throws IOException {

      Reader reader = new FileReader (fileName);
      PolygonalMesh mesh = read (reader, scale);
      reader.close ();
      return mesh;
   }

   /**
    * Creates a PolygonalMesh based on MDL data read from a Reader. The node
    * coordinate data can be scaled non-uniformly using an optional parameter
    * giving scale values about the x, y, and z axes.
    * 
    * @param reader
    * the Reader which references MDL data to be read
    * @param scale
    * if non-null, gives scaling about the x, y, and z axes
    * @return created polygonal mesh
    * @throws IOException
    * if this is a problem reading the file
    */
   public static PolygonalMesh read (Reader reader, Vector3d scale)
      throws IOException {

      PolygonalMesh mesh = new PolygonalMesh ();
      ReaderTokenizer rtok = new ReaderTokenizer (new BufferedReader (reader));
      rtok.wordChars ("<>.:/\\");

      // read top header
      MDLHeader header = MDLHeader.scan (rtok);
      if (!header.equals (defaultTopHeader)) {
//         throw new IOException ("MDLReader: bad top header, " + header);
      }

      // read file name
      if (rtok.nextToken () != ReaderTokenizer.TT_WORD) {
         throw new IOException ("MDLReader: expecting filename, got "
         + rtok.tokenName ());
      }

      // read vertices header information
      header = MDLHeader.scan (rtok);
      if (!header.equals (defaultVerticesHeader)) {
//         throw new IOException ("MDLReader: bad vertices header, " + header);
         System.err.println("MDLReader: bad vertices header, no vertices read...");
         return mesh;
      }

      // read vertices
      int numVertices = rtok.scanInteger ();
      Point3d coords = new Point3d ();
      for (int i = 0; i < numVertices; i++) {
         coords.x = rtok.scanNumber ();
         coords.y = rtok.scanNumber ();
         coords.z = rtok.scanNumber ();
         if (scale != null) {
            coords.x *= scale.x;
            coords.y *= scale.y;
            coords.z *= scale.z;
         }
         mesh.addVertex (coords);
      }

      // read normals header information
      try {
         header = MDLHeader.scan (rtok);
      } catch (IOException e) {
         System.out.println("MDLReader: no normals read...");
         return mesh;
      }
      if (!header.equals (defaultNormalsHeader)) {
//         throw new IOException ("MDLReader: bad normals header, " + header);
         System.out.println("MDLReader: no normals read...");
         return mesh;
      }

      // read vertices
      int numNormals = rtok.scanInteger ();
      ArrayList<Vector3d> vn = new ArrayList<Vector3d> (numNormals);
      for (int i = 0; i < numNormals; i++) {
         Vector3d vec = new Vector3d ();
         vec.x = rtok.scanNumber ();
         vec.y = rtok.scanNumber ();
         vec.z = rtok.scanNumber ();
         if (scale != null) {
            vec.x *= scale.x;
            vec.y *= scale.y;
            vec.z *= scale.z;
         }
         vn.add (vec);
      }

      // read faces header information
      try {
         header = MDLHeader.scan (rtok);
      } catch (IOException e) {
         System.out.println("MDLReader: no faces read...");
         return mesh;
      }
      if (!header.equals (defaultFacesHeader)) {
//         throw new IOException ("MDLReader: bad faces header, " + header);
         System.out.println("MDLReader: no faces read...");
         return mesh;
      }

      // read vertices
      int numFaces = rtok.scanInteger ();
      int[] vi = new int[3];
      boolean faceIdxWarningGiven = false;
      int[] normalIdxs = new int[numFaces*3];
      int k = 0;
      for (int i = 0; i < numFaces; i++) {
         int[] ni = new int[3];
         vi[0] = rtok.scanInteger ();
         vi[1] = rtok.scanInteger ();
         vi[2] = rtok.scanInteger ();
         ni[0] = rtok.scanInteger ();
         ni[1] = rtok.scanInteger ();
         ni[2] = rtok.scanInteger ();
         if (!faceIdxWarningGiven && 
             (vi[0] != ni[0] || vi[0] != ni[0] || vi[0] != ni[0])) {
            System.out.println (
               "Warning: MDL face idxs don't match normal idxs; ignoring");
            faceIdxWarningGiven = true;
         }
         mesh.addFace (vi);
         normalIdxs[k++] = ni[0];
         normalIdxs[k++] = ni[1];
         normalIdxs[k++] = ni[2];
      }
      mesh.setNormals (vn, normalIdxs);
      return mesh;

   }


   public static class MDLHeader {
      String myName;
      String myType;

      public MDLHeader (String name, String type) {
         myName = name;
         myType = type;
      }

      @Override
      public boolean equals (Object obj) {
         boolean equal = false;
         if (obj instanceof MDLHeader) {
            MDLHeader mdlh = (MDLHeader)obj;
            equal = myName.equals (mdlh.myName) && myType.equals (mdlh.myType);
         }
         return equal;
      }

      @Override
      public String toString () {
         return String.format ("[%s,%s]", myName, myType);
      }

      public static MDLHeader scan (ReaderTokenizer rtok) throws IOException {

         if (rtok.nextToken () != '[') {
            throw new IOException ("MDLReader: expecting '['");
         }
         if (rtok.nextToken () != ReaderTokenizer.TT_WORD) {
            throw new IOException ("MDLReader: expecting word, got "
            + rtok.tokenName ());
         }
         if (rtok.nextToken () != ',') {
            throw new IOException ("MDLReader: expecting ','");
         }
         String name = rtok.sval;
         if (rtok.nextToken () != ReaderTokenizer.TT_WORD) {
            throw new IOException ("MDLReader: expecting word, got "
            + rtok.tokenName ());
         }
         String type = rtok.sval;
         if (rtok.nextToken () != ']') {
            throw new IOException ("MDLReader: expecting '['");
         }
         return new MDLHeader (name, type);
      }

   }
   
   /**
    * Writes the specified mesh to a PrintWriter, using the MDL
    * file format. Vertices are printed first by x, y, and z coordinates. 
    * Normals are printed next by x, y, and z coordinates. 
    * Faces are printed next  by a list of
    * integers which gives the indices of that face's vertices in
    * counter-clockwise order followed by the indices of the vertex normals. 
    *
    * <p>
    * The format used to print vertex
    * coordinates is specified by a C <code>printf</code> style format
    * string contained in the parameter fmtStr. For a description of the
    * format string syntax, see {@link maspack.util.NumberFormat
    * NumberFormat}.
    *
    * @param mesh polygonal mesh to write to file
    * @param pw PrintWriter to write this mesh to
    * @param fmt format for writing the vertex coordinates
    */
   public static void write (PolygonalMesh mesh, String filename, PrintWriter pw, NumberFormat fmt) {
      
      pw.println (defaultTopHeader);
      pw.println (filename);
      pw.println ("\n" + defaultVerticesHeader);
      pw.println (mesh.getVertices ().size());
      for (Iterator<Vertex3d> it = mesh.getVertices ().iterator (); it.hasNext ();) {
         Point3d pnt = ((Vertex3d)it.next ()).pnt;
         pw.println (fmt.format (pnt.x) + " " + 
            fmt.format (pnt.y) + " " +
            fmt.format (pnt.z));
      }
      
      pw.println ("\n" + defaultNormalsHeader);
      ArrayList<Vector3d> normals = mesh.getNormals();
      int[] indexOffs = mesh.getFeatureIndexOffsets();
      int[] nidxs = mesh.getNormalIndices();      
      if (normals == null) {
         // no normals specified; need to compute them
         normals = new ArrayList<Vector3d>();
         nidxs = mesh.computeVertexNormals (normals, /*multi=*/false);
      }
      pw.println (normals.size());
      for (Vector3d vn : normals) {
         pw.println (fmt.format (vn.x) + " " + 
         fmt.format (vn.y) + " " +
         fmt.format (vn.z));
      }
      
      pw.println ("\n" + defaultFacesHeader);
      pw.println(mesh.getFaces ().size ());

      for (int i = 0; i < mesh.getFaces ().size (); i++) {
         int[] vi = mesh.getFaces().get (i).getVertexIndices ();
         pw.print (vi[0]+" "+vi[1]+" "+vi[2]+" ");
         int foff = indexOffs[i];
         pw.println (nidxs[foff]+" "+nidxs[foff+1]+" "+nidxs[foff+2]);
      }
      
   }

   
   public static void writePoints (
      Point3d[] pnts, String filename, PrintWriter pw, NumberFormat fmt) {
      pw.println (defaultTopHeader);
      pw.println (filename);
      pw.println ("\n" + defaultVerticesHeader);
      pw.println (pnts.length);
      for (Point3d pnt : pnts) {
         pw.println (fmt.format (pnt.x) + " " + 
            fmt.format (pnt.y) + " " +
            fmt.format (pnt.z));
      }
   }
}
