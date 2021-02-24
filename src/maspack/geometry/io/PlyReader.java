package maspack.geometry.io;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import maspack.geometry.MeshBase;
import maspack.geometry.PointMesh;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.io.MeshWriter.DataFormat;
import maspack.geometry.io.MeshWriter.FloatType;
import maspack.geometry.io.PlyWriter.DataType;
import maspack.matrix.Point3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.util.BinaryInputStream;
import maspack.util.InternalErrorException;
import maspack.util.ReaderTokenizer;

/**
 * Reads a PolygonalMesh from an ascii or binary PLY format
 * @author John Lloyd, Jan 2014
 *
 */
public class PlyReader extends MeshReaderBase {

   // Note that UV and COLOR are currently here just for further use
   private enum PropertyType {
      VERTEX, NORMAL, UV, COLOR, VERTEX_INDICES, UNKNOWN };   

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
      String myName;
      PropertyType myPropType;
      DataType myDataType;

      Property (String name, PropertyType ptype, DataType dtype) {
         myName = name;
         myPropType = ptype;
         myDataType = dtype;
      }
      
      String getName() {
         return myName;
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
            case NORMAL: {
               Vector3d vec = new Vector3d();
               vec.x = readNumber (bis, myDataType);
               vec.y = readNumber (bis, myDataType);
               vec.z = readNumber (bis, myDataType);
               return vec;
            }
            case COLOR: {
               float[] color = new float[3];
               color[0] = (float)readNumber (bis, myDataType)/255f;
               color[1] = (float)readNumber (bis, myDataType)/255f;
               color[2] = (float)readNumber (bis, myDataType)/255f;
               return color;
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
            case NORMAL: {
               Vector3d vec = new Vector3d();
               vec.x = rtok.scanNumber();
               vec.y = rtok.scanNumber();
               vec.z = rtok.scanNumber();
               return vec;
            }
            case COLOR: {
               float[] color = new float[3];
               color[0] = (float)rtok.scanNumber()/255f;
               color[1] = (float)rtok.scanNumber()/255f;
               color[2] = (float)rtok.scanNumber()/255f;
               return color;
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
      
   private class PropertyList extends Property {
      DataType mySizeType;

      PropertyList (String name, PropertyType ptype, DataType stype,
                    DataType vtype) {
         super(name, ptype, vtype);
         mySizeType = stype;
      }
      
      DataType getSizeType() {
         return mySizeType;
      }
      
      DataType getValueType() {
         return myDataType;
      }

      Object read (BinaryInputStream bis) throws IOException {
         // read length first
         int v = readInt(bis, mySizeType);
         
         switch(myDataType) {
            case CHAR: 
            case UCHAR: {
               char[] out = new char[v];
               for (int i=0; i<v; ++i) {
                  out[i] = bis.readChar();
               }
               return out;
            }
            case DOUBLE: {
               double[] out = new double[v];
               for (int i=0; i<v; ++i) {
                  out[i] = bis.readDouble();
               }
               return out;
            }
            case FLOAT: {
               float[] out = new float[v];
               for (int i=0; i<v; ++i) {
                  out[i] = bis.readFloat();
               }
               return out;
            }
            case INT: 
            case UINT: {
               int[] out = new int[v];
               for (int i=0; i<v; ++i) {
                  out[i] = bis.readInt();
               }
               return out;
            }
            case SHORT: 
            case USHORT: {
               short[] out = new short[v];
               for (int i=0; i<v; ++i) {
                  out[i] = bis.readShort();
               }
               return out;
            }
            default:
         }
         return null;
      }

      Object read (ReaderTokenizer rtok) throws IOException {
         // read length first
         int v = rtok.scanInteger();
         
         switch(myDataType) {
            case CHAR: 
            case UCHAR: {
               char[] out = new char[v];
               for (int i=0; i<v; ++i) {
                  out[i] = (char)rtok.scanNumber();
               }
               return out;
            }
            case DOUBLE: {
               double[] out = new double[v];
               for (int i=0; i<v; ++i) {
                  out[i] = rtok.scanNumber();
               }
               return out;
            }
            case FLOAT: {
               float[] out = new float[v];
               for (int i=0; i<v; ++i) {
                  out[i] = (float)rtok.scanNumber();
               }
               return out;
            }
            case INT: 
            case UINT: {
               int[] out = new int[v];
               for (int i=0; i<v; ++i) {
                  out[i] = rtok.scanInteger();
               }
               return out;
            }
            case SHORT: 
            case USHORT: {
               short[] out = new short[v];
               for (int i=0; i<v; ++i) {
                  out[i] = rtok.scanShort();
               }
               return out;
            }
            default:
         }
         return null;
         
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
   PropertyList myFaceVertexIndices = null;
   ArrayList<Property> myFaceProps = new ArrayList<Property>();
   
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
            // avoid deprecation warning
            StringBuffer sb = new StringBuffer();
            int c = -1;
            while ((c = is.read()) >= 0) {
               if ((char)c == '\r') {
                  // discard
                  continue;
               } else if ((char)c == '\n') {
                  break;
               }
               sb.append((char)c);
            }
            myLine = sb.toString();
            myLineNum++;
            if (c < 0 || myLine == null) {
               throw new EOFException();
            }
         }
         while (myLine.startsWith ("comment") || myLine.startsWith ("obj_info"));
         // some software (like VTK) write "obj_info" header data; ignore for now
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
            return readInt (bis, type);
         case UINT:
            int val = readInt (bis, type);
            long uval = val & 0xFFFFFFFFL;
            return (double)uval;
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

   private void scanProperties (
      DataInputStream is, ArrayList<Property> propList) throws IOException {
      
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
            propList.add (
               new Property ("vertex", PropertyType.VERTEX, dataType));
         }
         else if (propStr.equals ("nx")) {
            scanHeaderProperties (is, typeStr, "ny", "nz");
            propList.add (
               new Property ("normal", PropertyType.NORMAL, dataType));
         }
         else if (propStr.equals ("red")) {
            scanHeaderProperties (is, typeStr, "green", "blue");
            propList.add (
               new Property ("color", PropertyType.COLOR, dataType));
         }
         else {
            propList.add (
               new Property (propStr, PropertyType.UNKNOWN, dataType));
         }
      }
      while (myLine.startsWith ("property"));
      pushLine();
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
      scanProperties (is, myVertProps);
   }

   private PropertyList parsePropertyList(String line) throws IOException {
      
      String[] parts = line.split ("\\s+", 5);
      if (parts.length < 5 ||
          !parts[0].equalsIgnoreCase("property") ||
          !parts[1].equalsIgnoreCase("list")) {
         throw new IOException(
            "Line '" + line + "' does not specify a property list");
      }
      DataType sizeType = parseDataType(parts[2]);
      DataType valueType = parseDataType(parts[3]);
      String name = parts[4];
      PropertyType ptype = PropertyType.UNKNOWN;
      if (name.equalsIgnoreCase("vertex_index") ||
          name.equalsIgnoreCase("vertex_indices")) {
         ptype = PropertyType.VERTEX_INDICES;
      }
      return new PropertyList(name, ptype, sizeType, valueType);
   }
   
   private void scanHeaderFaceInfo (DataInputStream is) throws IOException {
      String key = "element face ";

      readLine(is);
      if (myLine.startsWith (key)) {
         myNumFaces = Integer.parseInt (myLine.substring (key.length()));
      }
      else if (myLine.equals ("end_header")) {
         // no faces
         pushLine();
         return;
      }
      else {
         throw new IOException (
            "Unexpected face info: "+myLine+", line "+myLineNum);
      }
      readLine (is);
      String tline = myLine.trim();
      if (!tline.startsWith("property list") || 
         (!tline.endsWith ("vertex_indices") &&
          !tline.endsWith ("vertex_index"))) {
         throw new IOException (
"Expected 'property list <size type> <value type> vertex_indices' or " +
"'property list <size type> <value type> vertex_index' at line " + myLineNum);
      }
      myFaceVertexIndices = parsePropertyList(tline);
      scanProperties (is, myFaceProps);
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
      ArrayList<Point3d> verts,
      ArrayList<Vector3d> nrmls,
      ArrayList<float[]> colors) throws IOException {

      for (int i=0; i<myNumVerts; i++) {
         for (Property prop : myVertProps) {
            Object obj = prop.read (rtok);
            if (prop.myPropType == PropertyType.VERTEX) {
               verts.add ((Point3d)obj);
            }
            else if (prop.myPropType == PropertyType.NORMAL) {
               nrmls.add ((Vector3d)obj);
            }
            else if (prop.myPropType == PropertyType.COLOR) {
               colors.add ((float[])obj);
            }
         }
      }
   }

   private void readFaceInfo (
      ReaderTokenizer rtok,
      ArrayList<int[]> faces,
      ArrayList<float[]> colors) throws IOException {
      
      for (int i=0; i<myNumFaces; i++) {
         
         int[] idxs = null;
         
         // read face
         Object oidxs = myFaceVertexIndices.read(rtok);
            
         switch(myFaceVertexIndices.getValueType()) {
            case CHAR: {
               char[] cidxs = (char[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)cidxs[j];
               }
               break;
            }
            case DOUBLE: {
               double[] cidxs = (double[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)cidxs[j];
               }
               break;
            }
            case FLOAT: {
               float[] cidxs = (float[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)cidxs[j];
               }
               break;
            }
            case INT:
            case UINT:
               idxs = (int[])oidxs;
               break;
            case SHORT:  {
               short[] cidxs = (short[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)cidxs[j];
               }
               break;
            }
            case UCHAR:  {
               char[] cidxs = (char[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)(cidxs[j] & 0xFF);
               }
               break;
            }
            case USHORT: {
               short[] cidxs = (short[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)(cidxs[j] & 0xFFFF);
               }
               break;
            }
            default:
               break;
            
         }
        
         if (idxs != null) {
            faces.add (idxs);
         }

         for (Property prop : myFaceProps) {
            Object obj = prop.read (rtok);
            if (prop.myPropType == PropertyType.VERTEX) {
               // not implemented, ignore
            }
            else if (prop.myPropType == PropertyType.NORMAL) {
               // not implemented, ignore
            }
            else if (prop.myPropType == PropertyType.COLOR) {
               colors.add ((float[])obj);
            }
         }
      }
   }

   private void readVertexInfo (
      BinaryInputStream bis,
      ArrayList<Point3d> verts, 
      ArrayList<Vector3d> nrmls,
      ArrayList<float[]> colors) throws IOException {


      for (int i=0; i<myNumVerts; i++) {
         for (Property prop : myVertProps) {
            Object obj = prop.read (bis);
            if (prop.myPropType == PropertyType.VERTEX) {
               verts.add ((Point3d)obj);
            }
            else if (prop.myPropType == PropertyType.NORMAL) {
               nrmls.add ((Vector3d)obj);
            }
            else if (prop.myPropType == PropertyType.COLOR) {
               colors.add ((float[])obj);
            }
         }
      }
   }

   private void readFaceInfo (
      BinaryInputStream bis,
      ArrayList<int[]> faces,
      ArrayList<float[]> colors) throws IOException {

      for (int i=0; i<myNumFaces; i++) {
         
         int[] idxs = null;
         
         // read face
         Object oidxs = myFaceVertexIndices.read(bis);
            
         switch(myFaceVertexIndices.getValueType()) {
            case CHAR: {
               char[] cidxs = (char[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)cidxs[j];
               }
               break;
            }
            case DOUBLE: {
               double[] cidxs = (double[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)cidxs[j];
               }
               break;
            }
            case FLOAT: {
               float[] cidxs = (float[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)cidxs[j];
               }
               break;
            }
            case INT:
            case UINT:
               idxs = (int[])oidxs;
               break;
            case SHORT:  {
               short[] cidxs = (short[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)cidxs[j];
               }
               break;
            }
            case UCHAR:  {
               char[] cidxs = (char[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)(cidxs[j] & 0xFF);
               }
               break;
            }
            case USHORT: {
               short[] cidxs = (short[])oidxs;
               idxs = new int[cidxs.length];
               for (int j=0; j<idxs.length; ++j) {
                  idxs[j] = (int)(cidxs[j] & 0xFFFF);
               }
               break;
            }
            default:
               break;
            
         }
        
         if (idxs != null) {
            faces.add (idxs);
         }
         for (Property prop : myFaceProps) {
            Object obj = prop.read (bis);
            if (prop.myPropType == PropertyType.VERTEX) {
               // not implemented, ignore
            }
            else if (prop.myPropType == PropertyType.NORMAL) {
               // not implemented, ignore
            }
            else if (prop.myPropType == PropertyType.COLOR) {
               colors.add ((float[])obj);
            }
         }
      }
   }

   public DataFormat getDataFormat() {
      return myDataFormat;
   }

   public FloatType getFloatType() {
      return myFloatType;
   }

   @Override
   public PolygonalMesh readMesh() throws IOException {
      return (PolygonalMesh)readMesh (new PolygonalMesh());
   }

   public MeshBase readMesh (MeshBase mesh) throws IOException {

      DataInputStream is = new DataInputStream (myIstream);

      parseHeader (is);
      ArrayList<Point3d> verts = new ArrayList<Point3d>();
      ArrayList<Vector3d> nrmls = new ArrayList<Vector3d>();
      ArrayList<float[]> vertexColors = new ArrayList<float[]>();
      ArrayList<float[]> faceColors = new ArrayList<float[]>();
      ArrayList<int[]> faces = new ArrayList<int[]>();

      if (myDataFormat == DataFormat.ASCII) {
         ReaderTokenizer rtok = new ReaderTokenizer (
            new BufferedReader (new InputStreamReader (myIstream)));
         readVertexInfo (rtok, verts, nrmls, vertexColors);
         readFaceInfo (rtok, faces, faceColors);
      }
      else {
         BinaryInputStream bis = 
            new BinaryInputStream (new BufferedInputStream (myIstream));
         if (myDataFormat == DataFormat.BINARY_LITTLE_ENDIAN) {
            bis.setLittleEndian (true);
         }
         readVertexInfo (bis, verts, nrmls, vertexColors);
         readFaceInfo (bis, faces, faceColors);
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
         int icnt = 0;
         for (Point3d pnt : verts) {
            pmesh.addVertex (pnt);
         }
         for (int[] idxs : faces) {
            pmesh.addFace (idxs);
            icnt += idxs.length;
         }
         if (nrmls.size() > 0) {
            // we have to assume here the there is one normal per vertex,
            // and assign the normal indices accordingly]
            int k = 0;
            int[] normalIndices = new int[icnt];
            for (int i=0; i<faces.size(); i++) {
               int[] idxs = pmesh.getFaces().get(i).getVertexIndices();
               for (int j=0; j<idxs.length; j++) {
                  normalIndices[k++] = idxs[j];
               }
            }
            pmesh.setNormals (nrmls, normalIndices);
            pmesh.setHardEdgesFromNormals();
         }
         if (faceColors.size() > 0) {
            mesh.setFeatureColoringEnabled();
            for (int i=0; i<faceColors.size(); i++) {
               mesh.setColor (i, faceColors.get(i));
            }
         }
      }
      else if (mesh instanceof PointMesh) {
         PointMesh pmesh = (PointMesh)mesh;
         pmesh.set (
            verts.toArray(new Point3d[0]), nrmls.toArray(new Vector3d[0]));
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.ply' files");
      }
      if (vertexColors.size() > 0) {
         mesh.setVertexColoringEnabled();
         for (int i=0; i<vertexColors.size(); i++) {
            mesh.setColor (i, vertexColors.get(i));
         }
      }
      return mesh;   
   }
   
   public static MeshBase read (File file) throws IOException {
      PlyReader reader = null;
      try {
         reader = new PlyReader(file);
         return reader.readMesh ();
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (reader != null) {
            reader.close();
         }
      }
    }

   public static MeshBase read (String fileName) throws IOException {
      return read (new File(fileName));
    }

}
