package maspack.geometry.io;

import java.io.*;
import java.util.ArrayList;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.PointMesh;
import maspack.geometry.MeshBase;
import maspack.geometry.io.PlyWriter.DataType;
import maspack.geometry.io.MeshWriter.DataFormat;
import maspack.geometry.io.MeshWriter.FloatType;
import maspack.matrix.*;
import maspack.util.ReaderTokenizer;
import maspack.util.BinaryInputStream;
import maspack.util.InternalErrorException;

/**
 * Reads a PolygonalMesh from an ascii PLY format
 * @author John Lloyd, Jan 2014
 *
 */
public class PlyReader extends MeshReaderBase {

   // Note that UV and COLOR are currently here just for further use
   private enum PropertyType {
      VERTEX, NORMAL, UV, COLOR, UNKNOWN };   

   public PlyReader (InputStream is) throws IOException {
      super (is);
   }

   public PlyReader (File file) throws IOException {
      super (file);
   }

   public PlyReader (String fileName) throws IOException {
      this (new File (fileName));
   }

   private class Property {
      PropertyType myPropType;
      DataType myDataType;

      Property (PropertyType ptype, DataType dtype) {
         myPropType = ptype;
         myDataType = dtype;
      }

      Object read (BinaryInputStream bis) throws IOException {
         switch (myPropType) {
            case VERTEX: {
               Point3d pnt = new Point3d();
               pnt.x = readNumber (bis, myDataType);
               pnt.y = readNumber (bis, myDataType);
               pnt.z = readNumber (bis, myDataType);
               return pnt;
            }
            case NORMAL:
            case COLOR: {
               Vector3d vec = new Vector3d();
               vec.x = readNumber (bis, myDataType);
               vec.y = readNumber (bis, myDataType);
               vec.z = readNumber (bis, myDataType);
               return vec;
            }
            case UV: {
               Vector2d uv = new Vector2d();
               uv.x = readNumber (bis, myDataType);
               uv.y = readNumber (bis, myDataType);
               return uv;
            }
            case UNKNOWN: {
               readNumber (bis, myDataType);
               return null;
            } 
            default: {
               throw new InternalErrorException (
                  "Unimplemented property type " + myPropType);
            }
        }
      }

      Object read (ReaderTokenizer rtok) throws IOException {
         switch (myPropType) {
            case VERTEX: {
               Point3d pnt = new Point3d();
               pnt.x = rtok.scanNumber();
               pnt.y = rtok.scanNumber();
               pnt.z = rtok.scanNumber();
               return pnt;
            }
            case NORMAL:
            case COLOR: {
               Vector3d vec = new Vector3d();
               vec.x = rtok.scanNumber();
               vec.y = rtok.scanNumber();
               vec.z = rtok.scanNumber();
               return vec;
            }
            case UV: {
               Vector2d uv = new Vector2d();
               uv.x = rtok.scanNumber();
               uv.y = rtok.scanNumber();
               return uv;
            }
            case UNKNOWN: {
               rtok.scanNumber();
               return null;
            }
            default: {
               throw new InternalErrorException (
                  "Unimplemented property type " + myPropType);
            }
         }
      }
   }

   DataType parseDataType (String str) {
      for (DataType type : DataType.values()) {
         if (str.equals (type.toString().toLowerCase())) {
            return type;
         }
      }
      return null;
   }

   private DataFormat myDataFormat = DataFormat.ASCII;
   private FloatType myFloatType = null;

   private int myNumVerts = 0;
   ArrayList<Property> myVertProps = new ArrayList<Property>();
   private int myNumFaces = 0;

   // private double readFloat (DataInputStream in) throws IOException {
   //    // convert from little-endian
   //    int bytes = Integer.reverseBytes (in.readInt());
   //    return (double)Float.intBitsToFloat (bytes);
   // }

   // private int readInt (DataInputStream in) throws IOException {
   //    // convert from little-endian
   //    int bytes = Integer.reverseBytes (in.readInt());
   //    return bytes;
   // }

   // private int readByte (DataInputStream in) throws IOException {
   //    // convert from little-endian
   //    return in.readByte();
   // }

   private String myLine = null;
   private boolean myLinePushed = false;
   private int myLineNum = 0;

   private void readLine (DataInputStream is) throws IOException {
      if (myLinePushed) {
         // just "return" the currently stored line 
         myLinePushed = false;
      }
      else {
         do {
            myLine = is.readLine();
            myLineNum++;
            if (myLine == null) {
               throw new EOFException();
            }
         }
         while (myLine.startsWith ("comment") || myLine.startsWith ("obj_info"));
         // some softwares (like VTK) write "obj_info" header data; ignore for now. 
      }
   }

   private void pushLine() {
      myLinePushed = true;
   }

   private double readNumber (BinaryInputStream bis, DataType type)
      throws IOException {
      
      switch (type) {
         case CHAR: 
         case UCHAR:
         case SHORT:
         case USHORT:
         case INT: 
         case UINT: {
            return readInt (bis, type);
         }
         case FLOAT: {
            return bis.readFloat();
         }
         case DOUBLE: {
            return bis.readDouble();
         }
         default: {
            throw new IllegalArgumentException (
               "Unimplemented data type '"+type+"'");
         }
      }
   }

   private int readInt (BinaryInputStream bis, DataType type) throws IOException {

      switch (type) {      
         case CHAR: {
            return bis.readByte();
         }
         case UCHAR: {
            return bis.readUnsignedByte();
         }
         case SHORT: {
            return bis.readShort();
         }
         case USHORT: {
            return bis.readUnsignedShort();
         }
         case INT: 
         case UINT: {
            return bis.readInt();
         }
         default: {
            throw new IllegalArgumentException (
               "Inappropriate data type '"+type+"' for int");
         }
      }
   }

   private void readLine (DataInputStream is, String str) throws IOException {
      readLine(is);
      if (!myLine.startsWith (str)) {
         throw new IOException (
            "Unsupported header entry: "+myLine+", line "+myLineNum);
      }
   }

   private void scanHeaderFormatInfo (DataInputStream is) throws IOException {

      readLine(is);
      if (myLine.startsWith ("format ascii")) {
         myDataFormat = DataFormat.ASCII;
      }
      else if (myLine.startsWith ("format binary_little_endian")) {
         myDataFormat = DataFormat.BINARY_LITTLE_ENDIAN;
      }
      else if (myLine.startsWith ("format binary_big_endian")) {
         myDataFormat = DataFormat.BINARY_BIG_ENDIAN;
      }         
      else {
         throw new IOException (
            "Unexpected format info: "+myLine+", line "+myLineNum);
      }
   }

   private void scanHeaderProperties (
      DataInputStream is, String typeStr, String... propNames)
      throws IOException {

      for (String propName : propNames) {
         readLine (is, "property");
         String[] parts = myLine.split ("\\s+", 3);
         if (!parts[1].equals (typeStr) || !parts[2].equals (propName)) {
            throw new IOException (
               "Expecting 'property "+typeStr+" "+propName+
               "' at line "+myLineNum);
         }
      }
   }

   private void scanHeaderVertexInfo (DataInputStream is) throws IOException {
      String key = "element vertex ";

      readLine(is);
      if (myLine.startsWith (key)) {
         myNumVerts = Integer.parseInt (myLine.substring (key.length()));
      }
      else {
         throw new IOException (
            "Unexpected vertex info: "+myLine+", line "+myLineNum);
      }
      while (true) {
         readLine (is);
         if (!myLine.startsWith ("property")) {
            break;
         }
         String[] parts = myLine.split ("\\s+", 3);
         
         String typeStr = parts[1];
         String propStr = parts[2];
         DataType dataType = parseDataType (typeStr);
         if (dataType == null) {
            throw new IOException (
               "Expected data type at line "+myLineNum+", got " + typeStr);
         }
         if (myFloatType == null) {
            if (myDataFormat == DataFormat.ASCII) {
               myFloatType = FloatType.ASCII;
            }
            else if (dataType == DataType.FLOAT) {
               myFloatType = FloatType.FLOAT;
            }
            else {
               myFloatType = FloatType.DOUBLE;
            }
         }
         if (propStr.equals ("x")) {
            scanHeaderProperties (is, typeStr, "y", "z");
            myVertProps.add (new Property (PropertyType.VERTEX, dataType));
         }
         else if (propStr.equals ("nx")) {
            scanHeaderProperties (is, typeStr, "ny", "nz");
            myVertProps.add (new Property (PropertyType.NORMAL, dataType));
         }
         else {
            myVertProps.add (new Property (PropertyType.UNKNOWN, dataType));
         }
      }
      while (myLine.startsWith ("property"));
      pushLine();
   }

   private void scanHeaderFaceInfo (DataInputStream is) throws IOException {
      String key = "element face ";

      readLine(is);
      if (myLine.startsWith (key)) {
         myNumFaces = Integer.parseInt (myLine.substring (key.length()));
      }
      else if (myLine.equals ("end_header")) {
         pushLine();
         return;
      }
      else {
         throw new IOException (
            "Unexpected face info: "+myLine+", line "+myLineNum);
      }
      readLine (is);
      if (!myLine.startsWith ("property list uchar int vertex_indices") &&
          !myLine.startsWith ("property list uchar int vertex_index")) {
         throw new IOException (
            "Expected 'property list uchar int vertex_indices' or " +
            "'property list uchar int vertex_index' at line " + myLineNum);
      }
   }

   private void parseHeader (DataInputStream is) throws IOException {
      
      myFloatType = null;
      readLine (is);
      if (!myLine.equals ("ply")) {
         throw new IOException (
            "File header starts with '"+myLine+"' instead of 'ply'");
      }
      scanHeaderFormatInfo (is);
      scanHeaderVertexInfo (is);
      scanHeaderFaceInfo (is);
      readLine (is, "end_header");
   }

   private void readVertexInfo (
      ReaderTokenizer rtok,
      ArrayList<Point3d> verts, ArrayList<Vector3d> nrmls) throws IOException {

      for (int i=0; i<myNumVerts; i++) {
         for (Property prop : myVertProps) {
            Object obj = prop.read (rtok);
            if (prop.myPropType == PropertyType.VERTEX) {
               verts.add ((Point3d)obj);
            }
            else if (prop.myPropType == PropertyType.NORMAL) {
               nrmls.add ((Vector3d)obj);
            }
         }
      }
   }

   private void readFaceInfo (
      ReaderTokenizer rtok, ArrayList<int[]> faces) throws IOException {

      for (int i=0; i<myNumFaces; i++) {
         int numv = rtok.scanInteger ();
         int[] idxs = new int[numv];
         for (int k=0; k<numv; k++) {
            idxs[k] = rtok.scanInteger ();
         }
         faces.add (idxs);
      }
   }

   private void readVertexInfo (
      BinaryInputStream bis,
      ArrayList<Point3d> verts, ArrayList<Vector3d> nrmls) throws IOException {

      for (int i=0; i<myNumVerts; i++) {
         for (Property prop : myVertProps) {
            Object obj = prop.read (bis);
            if (prop.myPropType == PropertyType.VERTEX) {
               verts.add ((Point3d)obj);
            }
            else if (prop.myPropType == PropertyType.NORMAL) {
               nrmls.add ((Vector3d)obj);
            }
         }
      }
   }

   private void readFaceInfo (
      BinaryInputStream bis, ArrayList<int[]> faces) throws IOException {

      for (int i=0; i<myNumFaces; i++) {
         int numv = bis.readByte();
         int[] idxs = new int[numv];
         for (int k=0; k<numv; k++) {
            idxs[k] = bis.readInt ();
         }
         faces.add (idxs);
      }
   }

   // public PolygonalMesh readMesh (PolygonalMesh mesh) throws IOException {

   //    DataInputStream is = new DataInputStream (myIstream);

   //    parseHeader (is);
   //    if (mesh == null) {
   //       mesh = new PolygonalMesh();
   //    }
   //    ArrayList<Point3d> verts = new ArrayList<Point3d>();
   //    ArrayList<Vector3d> nrmls = new ArrayList<Vector3d>();
   //    ArrayList<int[]> faces = new ArrayList<int[]>();

   //    if (myDataFormat == DataFormat.ASCII) {
   //       ReaderTokenizer rtok = new ReaderTokenizer (
   //          new BufferedReader (new InputStreamReader (myIstream)));
   //       readVertexInfo (rtok, verts, nrmls);
   //       readFaceInfo (rtok, faces);
   //    }
   //    else {
   //       BinaryInputStream bis = 
   //          new BinaryInputStream (new BufferedInputStream (myIstream));
   //       if (myDataFormat == DataFormat.BINARY_LITTLE_ENDIAN) {
   //          bis.setLittleEndian (true);
   //       }
   //       readVertexInfo (bis, verts, nrmls);
   //       readFaceInfo (bis, faces);
   //    }

   //    for (Point3d pnt : verts) {
   //       mesh.addVertex (pnt);
   //    }
   //    for (int[] idxs : faces) {
   //       mesh.addFace (idxs);
   //    }
   //    if (nrmls.size() > 0) {
   //       mesh.setNormalList (nrmls);
   //       // we have to assume here the there is one normal per vertex,
   //       // and assign the normal indices accordingly
   //       int[][] normalIndices = new int[faces.size()][];
   //       for (int i=0; i<faces.size(); i++) {
   //          normalIndices[i] = mesh.getFaces().get(i).getVertexIndices();
   //       }
   //       mesh.setNormalIndices (normalIndices);
   //    }
      
   //    return mesh;
   // }

   public DataFormat getDataFormat() {
      return myDataFormat;
   }

   public FloatType getFloatType() {
      return myFloatType;
   }

   // public PointMesh readMesh (PointMesh mesh) throws IOException {
      
   //    DataInputStream is = new DataInputStream (myIstream);

   //    parseHeader (is);
   //    if (mesh == null) {
   //       mesh = new PointMesh();
   //    }
   //    ArrayList<Point3d> verts = new ArrayList<Point3d>();
   //    ArrayList<Vector3d> nrmls = new ArrayList<Vector3d>();

   //    if (myDataFormat == DataFormat.ASCII) {
   //       ReaderTokenizer rtok = new ReaderTokenizer (
   //          new BufferedReader (new InputStreamReader (myIstream)));
   //       readVertexInfo (rtok, verts, nrmls);
   //    }
   //    else {
   //       BinaryInputStream bis = 
   //          new BinaryInputStream (new BufferedInputStream (myIstream));
   //       if (myDataFormat == DataFormat.BINARY_LITTLE_ENDIAN) {
   //          bis.setLittleEndian (true);
   //       }
   //       readVertexInfo (bis, verts, nrmls);
   //    }
   //    mesh.set (verts.toArray(new Point3d[0]), nrmls.toArray(new Vector3d[0]));
      
   //    return mesh;
   // }

//   public MeshBase readMesh (MeshBase mesh) throws IOException {
//      return read (mesh, myIstream);
//   }

   @Override
   public PolygonalMesh readMesh() throws IOException {
      return (PolygonalMesh)readMesh (new PolygonalMesh());
   }

   public MeshBase readMesh (MeshBase mesh) throws IOException {

      DataInputStream is = new DataInputStream (myIstream);

      parseHeader (is);
      ArrayList<Point3d> verts = new ArrayList<Point3d>();
      ArrayList<Vector3d> nrmls = new ArrayList<Vector3d>();
      ArrayList<int[]> faces = new ArrayList<int[]>();

      if (myDataFormat == DataFormat.ASCII) {
         ReaderTokenizer rtok = new ReaderTokenizer (
            new BufferedReader (new InputStreamReader (myIstream)));
         readVertexInfo (rtok, verts, nrmls);
         readFaceInfo (rtok, faces);
      }
      else {
         BinaryInputStream bis = 
            new BinaryInputStream (new BufferedInputStream (myIstream));
         if (myDataFormat == DataFormat.BINARY_LITTLE_ENDIAN) {
            bis.setLittleEndian (true);
         }
         readVertexInfo (bis, verts, nrmls);
         readFaceInfo (bis, faces);
      }

      if (mesh == null) {
         if (myNumFaces == 0) {
            mesh = new PointMesh();
         }
         else {
            mesh = new PolygonalMesh();
         }
      }
      
      if (mesh instanceof PolygonalMesh) {
         PolygonalMesh pmesh = (PolygonalMesh)mesh;
         for (Point3d pnt : verts) {
            pmesh.addVertex (pnt);
         }
         for (int[] idxs : faces) {
            pmesh.addFace (idxs);
         }
         if (nrmls.size() > 0) {
            pmesh.setNormalList (nrmls);
            // we have to assume here the there is one normal per vertex,
            // and assign the normal indices accordingly
            int[][] normalIndices = new int[faces.size()][];
            for (int i=0; i<faces.size(); i++) {
               normalIndices[i] = pmesh.getFaces().get(i).getVertexIndices();
            }
            pmesh.setNormalIndices (normalIndices);
         }
      }
      else if (mesh instanceof PointMesh) {
         PointMesh pmesh = (PointMesh)mesh;
         pmesh.set (
            verts.toArray(new Point3d[0]), nrmls.toArray(new Vector3d[0]));
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported by this reader");
      }
      return mesh;   
   }
   
   public static MeshBase read (File file) throws IOException {
      PlyReader reader = new PlyReader(file);
      return reader.readMesh ();
    }

   public static MeshBase read (String fileName) throws IOException {
      return read (new File(fileName));
    }

}
