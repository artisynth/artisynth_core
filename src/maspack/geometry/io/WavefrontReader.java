package maspack.geometry.io;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;

import maspack.geometry.MeshBase;
import maspack.geometry.PointMesh;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.PolylineMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.render.BumpMapProps;
import maspack.render.ColorMapProps;
import maspack.render.NormalMapProps;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.Shading;
import maspack.util.ArraySupport;
import maspack.util.ReaderTokenizer;
import maspack.util.TestSupport;

/**
 * Interprets a subset of the Alias-Wavefront OBJ file format. Groups are
 * implemented, but only one group may be specified at a time and grouping
 * applies only to faces, lines, curves, and surfaces.
 */
/*
 * Modified: April 30, 2012 (Sanchez) Eliminated need to have "myFile", which is
 * preventing materials from being loaded when manually constructing meshes
 */
public class WavefrontReader extends MeshReaderBase {
   
   public static final String DEFAULT_GROUP = null;
   
   public static final int BEZIER = 1;
   public static final int BMATRIX = 2;
   public static final int BSPLINE = 3;
   public static final int CARDINAL = 4;
   public static final int TAYLOR = 5;

   protected ReaderTokenizer myRtok;
   protected boolean myInputHasBeenParsed = false;
   
   private boolean verbose = false;

   boolean myZeroIndexed = false;

   public boolean getZeroIndexed() {
      return myZeroIndexed;
   }

   public void setZeroIndexed (boolean enable) {
      myZeroIndexed = enable;
   }

   ArrayList<Vector4d> vertexList = new ArrayList<Vector4d>();
   ArrayList<Vector3d> textureVertexList = new ArrayList<Vector3d>();
   ArrayList<Vector3d> normalList = new ArrayList<Vector3d>();
   // source file for data being read by this reader.
   String currPath; // path to search for included file

   private class Group {
      String name;
      RenderProps props = null;
      ArrayList<Face> faceList = new ArrayList<Face>();
      ArrayList<Line> lineList = new ArrayList<Line>();
      ArrayList<Curve> curveList = new ArrayList<Curve>();
      ArrayList<Surface> surfaceList = new ArrayList<Surface>();

      Group (String name) {
         this.name = name;
      }
   }

   protected LinkedHashMap<String,Group> myGroupMap;
   protected LinkedHashMap<String,RenderProps> myMaterialMap;
   protected Group myCurrentGroup;

   protected void init (File file) {
      myGroupMap = new LinkedHashMap<String,Group>();
      myMaterialMap = new LinkedHashMap<String,RenderProps>();
      if (file != null) {
         currPath = file.getParent();
      }
      setGroup(DEFAULT_GROUP);
   }

   // public WavefrontReader () {
   //    super ((InputStream)null);
   //    init (null);
   // }

   public WavefrontReader (ReaderTokenizer rtok) {
      super ((InputStream)null);
      myRtok = rtok;
      init (null);
   }

   public WavefrontReader (Reader reader) {
      this (new ReaderTokenizer (new BufferedReader (reader)));
   }

   public WavefrontReader (File file) throws IOException {
      this (new FileReader (file));
      myFile = file;
      currPath = myFile.getParent();
   }

   public WavefrontReader (String fileName) throws IOException {
      this (new File (fileName));
   }

   public boolean hasGroup(String name) {
      return myGroupMap.get(name) != null;
   }

   public void setGroup(String name) {
      Group group = myGroupMap.get(name);
      if (group == null) {
         group = new Group(name);
         myGroupMap.put(name, group);
      }
      myCurrentGroup = group;
   }

   public String[] getPolyhedralGroupNames() {
      Group[] groups = getGroups();
      int numGroups = 0;
      for (int i = 0; i < groups.length; i++) {
         if (groups[i].faceList.size() > 0) {
            numGroups++;
         }
      }
      String[] names = new String[numGroups];
      int k = 0;
      for (int i = 0; i < groups.length; i++) {
         if (groups[i].faceList.size() > 0) {
            names[k++] = groups[i].name;
         }
      }
      return names;
   }

   public String[] getPolylineGroupNames() {
      Group[] groups = getGroups();
      int numGroups = 0;

      for (int i = 0; i < groups.length; i++) {
         if (groups[i].lineList.size() > 0) {
            numGroups++;
         }
      }
      String[] names = new String[numGroups];
      int k = 0;
      for (int i = 0; i < groups.length; i++) {
         if (groups[i].lineList.size() > 0) {
            names[k++] = groups[i].name;
         }
      }
      return names;
   }

   public String[] getGroupNames() {
      Group[] groups = getGroups();
      String[] names = new String[groups.length];
      for (int i = 0; i < groups.length; i++) {
         names[i] = groups[i].name;
      }
      return names;
   }

   // records input numbers
   ArrayList<Number> numberList = new ArrayList<Number>(100);
   // // records input texture vertices
   // ArrayList<Integer> textureList = new ArrayList<Integer> (100);
   // // records input normal vertices
   // ArrayList<Integer> normalList = new ArrayList<Integer> (100);

   int myVertexOffset = 0;
   int myVertexTextureOffset = 0;
   int myVertexNormalOffset = 0;

   Curve curve = null;
   Surface surface = null;

   /**
    * A simple container class to hold all the info for a Wavefront curve
    * specification. At this time, only rational BSPLINE types are supported.
    */
   public static class Curve {
      public int degree;
      public int type;

      public boolean isRational;
      public boolean isClosed;

      public double[] knots;
      public int[] indices;
      public double u0;
      public double u1;
      public int lineNum;

      public Curve () {
         knots = new double[0];
         indices = new int[0];
      }

      /**
       * For testing ...
       */
      public boolean equals(Object obj) {
         if (!(obj instanceof Curve)) {
            return false;
         }
         Curve c = (Curve)obj;
         return (degree == c.degree && type == c.type &&
            isRational == c.isRational && isClosed == c.isClosed &&
            TestSupport.equals(knots, c.knots) &&
            TestSupport.equals(indices, c.indices) &&
            u0 == c.u0 && u1 == c.u1 && lineNum == c.lineNum);
      }

      /**
       * For testing ...
       */
      public String toString() {
         return ("type=" + type + " rational=" + isRational + "\n" + " degree="
            +
            degree + " isClosed=" + isClosed + " u0=" + u0 + " u1=" + u1 +
            " knots=" + TestSupport.toString(knots) + "\n" + " indices=" +
            TestSupport.toString(indices) + " lineNum=" + lineNum);
      }

      /**
       * For testing ...
       */
      public void set(
         int type, boolean rat, int deg, boolean closed, double[] knots,
         int[] idxs, double u0, double u1, int line) {
         this.degree = deg;
         this.type = type;
         this.isClosed = closed;
         this.isRational = rat;
         this.knots = knots;
         this.indices = idxs;
         this.u0 = u0;
         this.u1 = u1;
         this.lineNum = line;
      }
   }

   /**
    * A simple container class to hold all the info for a Wavefront curve
    * specification. At this time, only rational BSPLINE types are supported.
    */
   public static class Surface {
      public int type;
      public boolean isRational;

      public int udegree;
      public boolean uIsClosed;
      public double[] uknots;
      public double u0;
      public double u1;

      public int vdegree;
      public boolean vIsClosed;
      public double[] vknots;
      public double v0;
      public double v1;

      public int[] indices;
      public int[] textureIndices;
      public int[] normalIndices;
      public int lineNum;

      public Surface () {
         vknots = new double[0];
         uknots = new double[0];
         indices = new int[0];
      }

      /**
       * For testing ...
       */
      public boolean equals(Object obj) {
         if (!(obj instanceof Surface)) {
            return false;
         }
         Surface s = (Surface)obj;
         return (type == s.type && isRational == s.isRational &&
            udegree == s.udegree && uIsClosed == s.uIsClosed &&
            TestSupport.equals(uknots, s.uknots) &&
            u0 == s.u0 && u1 == s.u1 &&
            vdegree == s.vdegree && vIsClosed == s.vIsClosed &&
            TestSupport.equals(vknots, s.vknots) &&
            v0 == s.v0 && v1 == s.v1 &&
            TestSupport.equals(indices, s.indices) &&
            TestSupport.equals(textureIndices, s.textureIndices) &&
            TestSupport.equals(normalIndices, s.normalIndices) && lineNum == s.lineNum);
      }

      /**
       * For testing ...
       */
      public String toString() {
         return ("type=" + type + " rational=" + isRational + "\n" +
            " udegree=" + udegree + " uIsClosed=" + uIsClosed + " u0=" + u0 +
            " u1=" + u1 + " uknots=" + TestSupport.toString(uknots) + "\n" +
            " vdegree=" + vdegree + " vIsClosed=" + vIsClosed + " v0=" + v0 +
            " v1=" + v1 + " vknots=" + TestSupport.toString(vknots) + "\n" +
            " indices=" + TestSupport.toString(indices) + " textureIndices=" +
            TestSupport.toString(textureIndices) + " normalIndices=" +
            TestSupport.toString(normalIndices) + " lineNum=" + lineNum);
      }

      /**
       * For testing ...
       */
      public void setGen(int type, boolean rat, int[] idxs, int line) {
         this.type = type;
         this.isRational = rat;
         this.indices = idxs;
         this.lineNum = line;
      }

      public void setu(
         int deg, boolean closed, double[] knots, double u0, double u1) {
         this.udegree = deg;
         this.uIsClosed = closed;
         this.uknots = knots;
         this.u0 = u0;
         this.u1 = u1;
      }

      public void setv(
         int deg, boolean closed, double[] knots, double v0, double v1) {
         this.vdegree = deg;
         this.vIsClosed = closed;
         this.vknots = knots;
         this.v0 = v0;
         this.v1 = v1;
      }

   }

   /**
    * Gives simple information about a face
    */
   static class Face {
      public int[] indices;
      public int[] textureIndices;
      public int[] normalIndices;
      public int lineNum;

      public Face () {
      }

      /**
       * For testing ...
       */
      public boolean equals(Object obj) {
         if (!(obj instanceof Face)) {
            return false;
         }
         Face f = (Face)obj;
         return (TestSupport.equals(indices, f.indices) &&
            TestSupport.equals(textureIndices, f.textureIndices) &&
            TestSupport.equals(normalIndices, f.normalIndices) && lineNum == f.lineNum);
      }

      /**
       * For testing ...
       */
      Face (int[] vidxs, int[] tidxs, int[] nidxs, int l) {
         indices = vidxs;
         textureIndices = tidxs;
         normalIndices = nidxs;
         lineNum = l;
      }

      /**
       * For testing ...
       */
      public String toString() {
         return (TestSupport.toString(indices) + " " +
            TestSupport.toString(textureIndices) + " " +
            TestSupport.toString(normalIndices) + " " + "(" + lineNum + ")");
      }
   }

   /**
    * Gives simple information about a line
    */
   static class Line {
      public int[] indices;
      public int lineNum;

      public Line () {
      }

      /**
       * For testing ...
       */
      public boolean equals(Object obj) {
         if (!(obj instanceof Line)) {
            return false;
         }
         Line l = (Line)obj;
         return (TestSupport.equals(indices, l.indices) && lineNum == l.lineNum);
      }

      /**
       * For testing ...
       */
      Line (int[] vidxs, int l) {
         indices = vidxs;
         lineNum = l;
      }

      /**
       * For testing ...
       */
      public String toString() {
         return (TestSupport.toString(indices) + " " + "(" + lineNum + ")");
      }
   }

   int degreeu = -1;
   int degreev = -1;

   protected int nextToken(ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      while (rtok.ttype == '\\') {
         rtok.nextToken();
         if (rtok.ttype != ReaderTokenizer.TT_EOL) {
            throw new IOException(
               "Line continuation token '\\' not at end of line, line " +
                  rtok.lineno());
         }
         rtok.nextToken();
      }
      return rtok.ttype;
   }

   protected double scanDouble(ReaderTokenizer rtok) throws IOException {
      nextToken(rtok);
      if (rtok.tokenIsNumber()) {
         return rtok.nval;
      }
      else {
         return Double.NaN;
      }
   }

   protected double scanOptionalNumber(
      ReaderTokenizer rtok, String desc, double defaultValue)
      throws IOException {
      if (rtok.ttype == ReaderTokenizer.TT_EOL) {
         return defaultValue;
      }
      nextToken(rtok);
      if (rtok.ttype != ReaderTokenizer.TT_EOL) {
         if (!rtok.tokenIsNumber()) {
            throw new IOException(desc + " expected, line " + rtok.lineno());
         }
         return rtok.nval;
      }
      else {
         return defaultValue;
      }
   }

   protected double[] scanDoubleList(ReaderTokenizer rtok, String desc)
      throws IOException {
      scanNumberList(rtok, desc, /* integer= */false);
      double[] list = new double[numberList.size()];
      int i = 0;
      for (Iterator<Number> it = numberList.iterator(); it.hasNext();) {
         list[i++] = ((Double)it.next()).doubleValue();
      }
      return list;
   }

   protected int[] scanIntegerList(ReaderTokenizer rtok, String desc)
      throws IOException {
      scanNumberList(rtok, desc, /* integer= */true);
      int[] list = new int[numberList.size()];
      int i = 0;
      for (Iterator<Number> it = numberList.iterator(); it.hasNext();) {
         list[i++] = ((Integer)it.next()).intValue();
      }
      return list;
   }

   protected void scanNumberList(
      ReaderTokenizer rtok, String desc, boolean integer) throws IOException {
      numberList.clear();
      nextToken(rtok);
      while (rtok.ttype != ReaderTokenizer.TT_EOL) {
         if (!rtok.tokenIsNumber()) {
            throw new IOException(desc + " expected, line " + rtok.lineno());
         }
         else {
            double num = rtok.nval;
            if (integer) {
               if ((int)num != num) {
                  throw new IOException(desc + " expected, line " +
                     rtok.lineno());
               }
               numberList.add(new Integer((int)num));
            }
            else {
               numberList.add(new Double(num));
            }
            nextToken(rtok);
         }
      }
   }

   protected int[] scanIndexList(ReaderTokenizer rtok, String desc)
      throws IOException {
      numberList.clear();
      nextToken(rtok);
      while (rtok.ttype != ReaderTokenizer.TT_EOL) {
         if (!rtok.tokenIsNumber()) {
            throw new IOException(desc + " expected, line " + rtok.lineno());
         }
         else {
            numberList.add(new Integer(getIndex(rtok, vertexList, desc)));
            nextToken(rtok);
         }
      }
      int[] list = new int[numberList.size()];
      int i = 0;
      for (Iterator<Number> it = numberList.iterator(); it.hasNext();) {
         list[i++] = ((Integer)it.next()).intValue();
      }
      return list;
   }

   protected int getIndex(
      ReaderTokenizer rtok, ArrayList<?> vertexList, String desc)
      throws IOException {
      double num = rtok.nval;
      if (num != (int)num) {
         throw new IOException(desc + " expected, line " + rtok.lineno());
      }
      int idx = (int)num;
      if (!myZeroIndexed && idx == 0) {
         throw new IOException("index with value of zero, line " +
            rtok.lineno());
      }
      if (!myZeroIndexed && idx > 0) {
         idx -= 1;
      }
      else if (idx < 0) {
         idx += vertexList.size();
         if (idx < 0) {
            throw new IOException("relative index out of range, line " +
               rtok.lineno());
         }
      }
      return idx;
   }

   protected void scanFaceIndices(Face face, ReaderTokenizer rtok)
      throws IOException {
      int textureDefined = -1;
      int normalsDefined = -1;
      int numVertexTypes = 1;

      numberList.clear();
      face.lineNum = rtok.lineno();
      nextToken(rtok);
      while (rtok.ttype != ReaderTokenizer.TT_EOL && rtok.ttype != ReaderTokenizer.TT_EOF) {
         if (!rtok.tokenIsNumber()) {
            throw new IOException("vertex index expected, line " +
               face.lineNum);
         }
         int idx = getIndex(rtok, vertexList, "vertex index");
         numberList.add(new Integer(idx - myVertexOffset));
         nextToken(rtok);
         if (rtok.ttype == '/') {
            if (textureDefined == 0 && normalsDefined == 0) {
               throw new IOException("unexpected '/', line " + face.lineNum);
            }
            nextToken(rtok);
            if (rtok.tokenIsNumber()) {
               if (textureDefined == 0) {
                  throw new IOException("unexpected texture index, line " +
                     face.lineNum);
               }
               else if (textureDefined == -1) {
                  textureDefined = 1;
                  numVertexTypes++;
               }
               idx = getIndex(rtok, textureVertexList, "texture index");
               numberList.add(new Integer(idx - myVertexTextureOffset));
               nextToken(rtok);
            }

            if (rtok.ttype == '/') {
               if (normalsDefined == 0) {
                  throw new IOException("unexpected '/', line " + face.lineNum);
               }
               else if (normalsDefined == -1) {
                  normalsDefined = 1;
                  numVertexTypes++;
               }
               nextToken(rtok);
               if (!rtok.tokenIsNumber()) {
                  throw new IOException("normal index expected, line " +
                     face.lineNum);
               }
               idx = getIndex(rtok, normalList, "normal index");
               numberList.add(new Integer(idx - myVertexNormalOffset));
               nextToken(rtok);
            }
         }
         if (textureDefined == -1) {
            textureDefined = 0;
         }
         if (normalsDefined == -1) {
            normalsDefined = 0;
         }
      }
      int numVerts = numberList.size() / numVertexTypes;
      face.indices = new int[numVerts];
      if (textureDefined == 1) {
         face.textureIndices = new int[numVerts];
      }
      if (normalsDefined == 1) {
         face.normalIndices = new int[numVerts];
      }
      int i = 0;
      for (Iterator<Number> it = numberList.iterator(); it.hasNext();) {
         face.indices[i] = ((Integer)it.next()).intValue();
         if (textureDefined == 1) {
            face.textureIndices[i] = ((Integer)it.next()).intValue();
         }
         if (normalsDefined == 1) {
            face.normalIndices[i] = ((Integer)it.next()).intValue();
         }
         i++;
      }
   }

   protected void scanLineIndices(Line line, ReaderTokenizer rtok)
      throws IOException {

      numberList.clear();
      line.lineNum = rtok.lineno();
      nextToken(rtok);
      while (rtok.ttype != ReaderTokenizer.TT_EOL) {
         if (!rtok.tokenIsNumber()) {
            throw new IOException("vertex index expected, line " + line.lineNum);
         }
         int idx = getIndex(rtok, vertexList, "vertex index");
         numberList.add(new Integer(idx - myVertexOffset));
         nextToken(rtok);
      }
      int numVerts = numberList.size();
      line.indices = new int[numVerts];
      int i = 0;
      for (Iterator<Number> it = numberList.iterator(); it.hasNext();) {
         line.indices[i] = ((Integer)it.next()).intValue();
         i++;
      }
   }

   protected boolean processLine(ReaderTokenizer rtok) throws IOException {

      if (curve != null || surface != null) {
         if (!rtok.sval.equals("parm") && !rtok.sval.equals("end")) {
            throw new IOException(
               "unexpected keyword '" + rtok.sval +
                  "' between curv/surf and end, line " + rtok.lineno());
         }
      }
      int lineno = rtok.lineno();
      if (rtok.sval.equals("v")) {
         Vector4d pnt = new Vector4d();
         if ((pnt.x = scanDouble(rtok)) != pnt.x ||
            (pnt.y = scanDouble(rtok)) != pnt.y ||
            (pnt.z = scanDouble(rtok)) != pnt.z) {
            throw new IOException("vertex coordinate expected, line " + lineno);
         }
         pnt.w = scanOptionalNumber(rtok, "vertex w coordinate", 1);
         vertexList.add(pnt);
         // John Lloyd, Oct 16, 2012
         // Meshlab sometimes provides 6 numbers for the vertex instead of
         // 3 or 4, so flush any extra numbers
         if (toEOL(rtok)) {
            // extra numbers, so number 4 probably wasn't w
            pnt.w = 1;
         }
      }
      else if (rtok.sval.equals("vn")) {
         Vector3d nrm = new Vector3d();
         if ((nrm.x = scanDouble(rtok)) != nrm.x ||
            (nrm.y = scanDouble(rtok)) != nrm.y ||
            (nrm.z = scanDouble(rtok)) != nrm.z) {
            throw new IOException("normal coordinate expected, line " + lineno);
         }
         normalList.add(nrm);
      }
      else if (rtok.sval.equals("vt")) {
         Vector3d txt = new Vector3d();
         if ((txt.x = scanDouble(rtok)) != txt.x) {
            throw new IOException(
               "texture vertex u coordinate expected, line " + lineno);
         }
         txt.y = scanOptionalNumber(rtok, "texture vertex v coordinate", 0);
         txt.z = scanOptionalNumber(rtok, "texture vertex w coordinate", 0);
         textureVertexList.add(txt);
      }
      else if (rtok.sval.equals("voff")) {
         // non-standard - used when we are reading a snippet from an obj file
         myVertexOffset = rtok.scanInteger();
      }
      else if (rtok.sval.equals("vtoff")) {
         // non-standard - used when we are reading a snippet from an obj file
         myVertexTextureOffset = rtok.scanInteger();
      }
      else if (rtok.sval.equals("vnoff")) {
         // non-standard - used when we are reading a snippet from an obj file
         myVertexNormalOffset = rtok.scanInteger();
      }
      else if (rtok.sval.equals("f")) {
         Face face = new Face();
         scanFaceIndices(face, rtok);
         myCurrentGroup.faceList.add(face);
      }
      else if (rtok.sval.equals("l")) {
         Line line = new Line();
         scanLineIndices(line, rtok);
         myCurrentGroup.lineList.add(line);
      }
      else if (rtok.sval.equals("deg")) {
         double num = scanDouble(rtok);
         if (num != num || num != (int)num) {
            throw new IOException("u curve degree expected, line " + lineno);
         }
         degreeu = (int)num;
         num = scanOptionalNumber(rtok, "v curve degree", -1);
         if (num != (int)num) {
            throw new IOException("v curve degree expected, line " + lineno);
         }
         degreev = (int)num;
      }
      else if (rtok.sval.equals("curv")) {
         curve = new Curve();
         curve.lineNum = lineno;
         curve.isRational = true;
         curve.type = BSPLINE;
         if ((curve.u0 = scanDouble(rtok)) != curve.u0 ||
            (curve.u1 = scanDouble(rtok)) != curve.u1) {
            throw new IOException(
               "u start and end values expected, line " +
                  lineno);
         }
         curve.indices = scanIndexList(rtok, "control point index");
         if (degreeu == -1) {
            throw new IOException(
               "degree not specified for curv, line " + lineno);
         }
         curve.degree = degreeu;
      }
      else if (rtok.sval.equals("surf")) {
         surface = new Surface();
         surface.lineNum = lineno;
         surface.isRational = true;
         surface.type = BSPLINE;
         if ((surface.u0 = scanDouble(rtok)) != surface.u0 ||
            (surface.u1 = scanDouble(rtok)) != surface.u1 ||
            (surface.v0 = scanDouble(rtok)) != surface.v0 ||
            (surface.v1 = scanDouble(rtok)) != surface.v1) {
            throw new IOException(
               "u and v start and end values expected, line " + lineno);
         }
         Face face = new Face();
         scanFaceIndices(face, rtok);
         surface.indices = face.indices;
         surface.textureIndices = face.textureIndices;
         surface.normalIndices = face.normalIndices;
         if (degreeu == -1) {
            throw new IOException(
               "u degree not specified for surf, line " + lineno);
         }
         if (degreev == -1) {
            throw new IOException(
               "v degree not specified for surf, line " + lineno);
         }
         surface.udegree = degreeu;
         surface.vdegree = degreev;
      }
      else if (rtok.sval.equals("parm")) {
         if (curve != null || surface != null) {
            boolean closed = false;
            boolean isu = true;
            double[] knots = null;

            nextToken(rtok);
            if (rtok.ttype != ReaderTokenizer.TT_WORD ||
               (!rtok.sval.equals("u") && !rtok.sval.equals("v"))) {
               if (curve != null) {
                  throw new IOException("u keyword expected, line " + lineno);
               }
               else {
                  throw new IOException(
                     "u or v keyword expected, line " + lineno);
               }
            }
            if (rtok.sval.equals("v")) {
               if (curve != null) {
                  throw new IOException(
                     "v keyword inappropriate for curve construct, line " +
                        lineno);
               }
               isu = false;
            }
            nextToken(rtok);
            if (rtok.ttype == ReaderTokenizer.TT_WORD &&
               rtok.sval.equals("closed")) {
               closed = true;
               nextToken(rtok);
            }
            rtok.pushBack();
            knots = scanDoubleList(rtok, "knot point");

            if (isu) {
               if (curve != null) {
                  curve.isClosed = closed;
                  curve.knots = knots;
               }
               else {
                  surface.uIsClosed = closed;
                  surface.uknots = knots;
               }
            }
            else {
               surface.vIsClosed = closed;
               surface.vknots = knots;
            }
         }
      }
      else if (rtok.sval.equals("end")) {
         if (surface != null) {
            myCurrentGroup.surfaceList.add(surface);
            surface = null;
         }
         else if (curve != null) {
            myCurrentGroup.curveList.add(curve);
            curve = null;
         }
      }
      else if (rtok.sval.equals("g") ||
               rtok.sval.equals("sg") ||
               rtok.sval.equals("mg") ||
               rtok.sval.equals("o")) {
         rtok.parseNumbers(false);
         String groupName = scanName(rtok);
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            setGroup(groupName);
         }
         while (rtok.ttype != ReaderTokenizer.TT_EOL) { // ignore secondary
                                                        // groups?
            nextToken(rtok);
         }
         rtok.pushBack();
         rtok.parseNumbers(true);
      }
      else if (rtok.sval.equals("mtllib")) {
         String matFileName = scanFileName(rtok);
         try {
            parseMaterialFile(matFileName);
         } catch (Exception e) {
            if (verbose) {
               System.out.println(
                  "WavefrontReader warning: can't read mtllib '" + matFileName
                     + "'; ignoring");
            }
         }
      }
      else if (rtok.sval.equals("usemtl")) {
         String matName = scanName(rtok);
         RenderProps props = myMaterialMap.get(matName);
         if (props != null) {
            // System.out.println ("usemtl " + matName);
            myCurrentGroup.props = props;
         }
         else {
            if (verbose) {
               System.out.println(
                  "WavefrontReader warning: material '" + matName
                     + "' not found; ignoring");
            }
         }
      }
      else if (rtok.sval.equals("s")) {
         // process smoothing group
         // either a number, or "off"
         nextToken(rtok);
         if (rtok.ttype == ReaderTokenizer.TT_NUMBER) {
            if (verbose) {
               System.out.println("Wavefront smoothing group: "
                  + (int)rtok.lval);
            }
         } else if ((rtok.ttype != ReaderTokenizer.TT_WORD)
            || !("off".equals(rtok.sval))) {
            System.out
               .println("Wavefront: unrecognized smoothing group" + rtok);
         }
         toEOL(rtok);
      }

      else {
         return false;
      }
      return true;
   }

   RenderProps myCurrentProps;

   // allow publicly change path to search for sub-files
   public void setParentPath(String pathname) {
      currPath = pathname;
   }

   // Scans tokens to the end of the line and returns true
   // if any non-EOL/EOF tokens were encounterd.
   private boolean toEOL(ReaderTokenizer rtok) throws IOException {
      int cnt = 0;
      while ((rtok.ttype != ReaderTokenizer.TT_EOL) &&
             (rtok.ttype != ReaderTokenizer.TT_EOF)) {
         nextToken(rtok);
      }
      return cnt > 1; // non-EOL/EOF tokens if more than one pass through loop
   }

   private void toNextLine(ReaderTokenizer rtok) throws IOException {
      toEOL(rtok);
      if (rtok.ttype == ReaderTokenizer.TT_EOL) {
         nextToken(rtok); // advance one more if not end-of-file
      }
   }

   private void parseMaterialFile(String matFileName) throws IOException {

      File parentFile = new File(currPath);
      File matFile = new File(parentFile, matFileName);
      if (!matFile.canRead() && verbose) {
         System.out.println(
            "WavefrontReader warning: can't read material file '" + matFile
               + "'; ignoring");
      }
      ReaderTokenizer rtok =
         new ReaderTokenizer(new BufferedReader(new FileReader(matFile)));
      myCurrentProps = null;
      rtok.commentChar('#');
      rtok.eolIsSignificant(true);

      // fast-forward to first definition
      nextToken(rtok);
      RenderProps newProps = null;
      String matName = null;

      while (rtok.ttype != ReaderTokenizer.TT_EOF) {
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equals("newmtl")) {
               break;
            }
         }
         toNextLine(rtok);
      }

      while (rtok.ttype != ReaderTokenizer.TT_EOF) {

         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equals("newmtl")) {
               // save old
               if (newProps != null) {
                  myMaterialMap.put(matName, newProps); // this doesn't work
               }
               matName = scanName(rtok);
               newProps = new RenderProps();
               // System.out.println ("found new material '"+matName+"'");
            } else {
               processMaterialLine(rtok, newProps);
            }
            toNextLine(rtok);
         } else {
            if (rtok.ttype != ReaderTokenizer.TT_EOL) {
               throw new IOException(
                  "unexpected token " + rtok.ttype + " in material file "
                     + matFile +
                     ", line " + rtok.lineno());
            } else {
               nextToken(rtok); // move to next token
            }
         }
      }

      if (newProps != null) {
         myMaterialMap.put(matName, newProps); // add final render props
      }

      rtok.close();

   }

   String scanName(ReaderTokenizer rtok) throws IOException {
      int saveDash = rtok.getCharSetting('-');
      int saveLParen = rtok.getCharSetting('(');
      int saveRParen = rtok.getCharSetting(')');
      int savePeriod = rtok.getCharSetting('.');
      int saveColon = rtok.getCharSetting(':');
      int saveSlash = rtok.getCharSetting ('/');
      int saveBackslash = rtok.getCharSetting ('\\');
      rtok.parseNumbers(false);
      rtok.wordChar('-');
      rtok.wordChar('(');
      rtok.wordChar(')');
      rtok.wordChar('.');
      rtok.wordChar(':');
      rtok.wordChar ('/');
      rtok.wordChar ('\\');
      String name = "";

      // catch default material
      try {
         name = rtok.scanWord();
      } catch (IOException e) {
         if (rtok.ttype == ReaderTokenizer.TT_EOL) {
            // System.out.println(e.getMessage()+"...using empty material name ''");
            name = "";
         } else {
            throw (e);
         }
      }
      rtok.parseNumbers(true);
      rtok.setCharSetting('-', saveDash);
      rtok.setCharSetting('(', saveLParen);
      rtok.setCharSetting(')', saveRParen);
      rtok.setCharSetting('.', savePeriod);
      rtok.setCharSetting(':', saveColon);
      rtok.setCharSetting ('/', saveSlash);
      rtok.setCharSetting ('\\', saveBackslash);
      return name;
   }

   String scanFileName(ReaderTokenizer rtok) throws IOException {
      rtok.parseNumbers(false);
      int saveDot = rtok.getCharSetting('.');
      int saveSlash = rtok.getCharSetting('/');
      rtok.wordChar('.');
      rtok.wordChar('/');
      String fileName = rtok.scanWord();
      rtok.setCharSetting('/', saveSlash);
      rtok.setCharSetting('.', saveDot);
      rtok.parseNumbers(true);
      return fileName;
   }

   protected boolean processMaterialLine(
      ReaderTokenizer rtok, RenderProps props) throws IOException {

      // int lineno = rtok.lineno();
      if (rtok.sval.equals("Ka")) {
         // ambient colour
         double r = rtok.scanNumber();
         double g = rtok.scanNumber();
         double b = rtok.scanNumber();
         if (props != null) {
            props.setFaceColor(new Color((float)r, (float)g, (float)b));
            // System.out.println( props.getFaceColor().toString() );
         }
      }
      else if (rtok.sval.equals("Kd")) {
         // diffuse colour
         double r = rtok.scanNumber();
         double g = rtok.scanNumber();
         double b = rtok.scanNumber();
         if (props != null) {
            props.setFaceColor(new Color((float)r, (float)g, (float)b));
            // System.out.println( props.getFaceColor().toString() );
         }
      }
      else if (rtok.sval.equals("Ks")) {
         // specular colour
         double r = rtok.scanNumber();
         double g = rtok.scanNumber();
         double b = rtok.scanNumber();
         props.setSpecular (new Color((float)r, (float)g, (float)b));
      }
      else if (rtok.sval.equals("d") || rtok.sval.equals("Tr")) {
         String a = rtok.sval;
         double alpha = rtok.scanNumber();
         if (props != null) {
            if (a.equals("d")) {
               props.setAlpha(alpha);
            } else {
               props.setAlpha(1-alpha);
            }
         }
      }
      else if (rtok.sval.equals("Ns")) {
         double shininess = rtok.scanNumber();
         if (props != null) {
            props.setShininess((float)shininess);
         }
      }

      // XXX: only basic reading of material file, no options
      // eg map_Kd lenna.tga # the diffuse texture map
      else if (rtok.sval.equals("map_Kd") || rtok.sval.equals("map_Ka")) {

         // we need period
         int savePeriod = rtok.getCharSetting('.');
         rtok.wordChar('.');
         int saveDash = rtok.getCharSetting ('-');
         rtok.wordChar ('-');

         String map = rtok.scanWord();
         if (map != null) {
            // set texture properties
            props.setFaceStyle(Renderer.FaceStyle.FRONT_AND_BACK);
            props.setShading(Shading.SMOOTH);
            ColorMapProps tprops = props.getColorMap ();
            if (tprops == null) {
               tprops = new ColorMapProps();
            }
            tprops.setFileName(currPath + "/" + map);
            tprops.setEnabled(true);
            tprops.setColorMixing(ColorMixing.MODULATE);
            props.setColorMap(tprops);
         }

         // restore period state
         rtok.setCharSetting('.', savePeriod);
         rtok.setCharSetting ('-', saveDash);
      }
      // eg map_Kd lenna.tga # the diffuse texture map
      else if (rtok.sval.equals("bump") || rtok.sval.equalsIgnoreCase("map_bump")) {

         // we need period
         int savePeriod = rtok.getCharSetting('.');
         rtok.wordChar('.');
         int saveDash = rtok.getCharSetting ('-');
         rtok.wordChar ('-');
         
         String map = rtok.scanWord();
         if (map != null) {
            // set texture properties
            props.setFaceStyle(Renderer.FaceStyle.FRONT_AND_BACK);
            props.setShading(Shading.SMOOTH);
            BumpMapProps tprops = props.getBumpMap ();
            if (tprops == null) {
               tprops = new BumpMapProps();
            }
            tprops.setFileName(currPath + "/" + map);
            tprops.setEnabled(true);
            props.setBumpMap(tprops);
         }

         // restore period state
         rtok.setCharSetting('.', savePeriod);
         rtok.setCharSetting ('-', saveDash);
      }
      else if (rtok.sval.equals("norm") || rtok.sval.equalsIgnoreCase("map_norm")) {

         // we need period
         int savePeriod = rtok.getCharSetting('.');
         rtok.wordChar('.');
         int saveDash = rtok.getCharSetting ('-');
         rtok.wordChar ('-');
         
         String map = rtok.scanWord();
         if (map != null) {
            // set texture properties
            props.setFaceStyle(Renderer.FaceStyle.FRONT_AND_BACK);
            props.setShading(Shading.SMOOTH);
            NormalMapProps tprops = props.getNormalMap ();
            if (tprops == null) {
               tprops = new NormalMapProps();
            }
            tprops.setFileName(currPath + "/" + map);
            tprops.setEnabled(true);
            props.setNormalMap(tprops);
         }

         // restore period state
         rtok.setCharSetting('.', savePeriod);
         rtok.setCharSetting ('-', saveDash);
      }
      else {
         return false;
      }
      return true;
   }

   public void clear() {
      vertexList.clear();
      textureVertexList.clear();
      normalList.clear();
      myGroupMap.clear();
      setGroup(DEFAULT_GROUP);
      degreeu = -1;
      degreev = -1;
   }

   public RenderProps getMaterial(String matName) {
      return myMaterialMap.get(matName);
   }

   public String[] getMaterialNames() {
      return myMaterialMap.keySet().toArray(new String[myMaterialMap.size()]);
   }

   public void parse () throws IOException {
      
      ReaderTokenizer rtok = myRtok;
      
      boolean savedEol = rtok.getEolIsSignificant();
      int savePound = rtok.getCharSetting('#');
      int saveSlash = rtok.getCharSetting('/');

      rtok.commentChar('#');
      rtok.ordinaryChar('/');
      rtok.eolIsSignificant(true);

      while (nextToken(rtok) != ReaderTokenizer.TT_EOF) {
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equals("EOF")) {
               break;
            }
            if (!processLine(rtok)) { // flush to EOL
               toEOL(rtok);
            }
         }
         else if (rtok.ttype != ReaderTokenizer.TT_EOL) {
            throw new IOException(
               "unexpected token " + rtok.ttype + " , line " + rtok.lineno());
         }
      }
      rtok.eolIsSignificant(savedEol);
      rtok.setCharSetting('#', savePound);
      rtok.setCharSetting('/', saveSlash);
      myInputHasBeenParsed = true;
   }

   protected Group[] getGroups() {
      return myGroupMap.values().toArray(new Group[0]);
   }

   protected Group getGroup(String name) {
      return myGroupMap.get(name);
   }

   /**
    * For testing ...
    */
   public boolean equals(Object obj) {
      if (!(obj instanceof WavefrontReader)) {
         return false;
      }
      WavefrontReader wfr = (WavefrontReader)obj;

      if (!TestSupport.equals(vertexList, wfr.vertexList)) {
         return false;
      }
      if (!TestSupport.equals(textureVertexList, wfr.textureVertexList)) {
         return false;
      }
      if (!TestSupport.equals(normalList, wfr.normalList)) {
         return false;
      }
      Group[] groups = getGroups();
      if (groups.length != wfr.getGroups().length) {
         return false;
      }
      for (Group grp : groups) {
         Group wfrGrp = wfr.getGroup(grp.name);
         if (wfrGrp == null) {
            return false;
         }
         if (!TestSupport.equals(grp.faceList, wfrGrp.faceList)) {
            return false;
         }
         if (!TestSupport.equals(grp.lineList, wfrGrp.lineList)) {
            return false;
         }
         if (!TestSupport.equals(grp.curveList, wfrGrp.curveList)) {
            return false;
         }
         if (!TestSupport.equals(grp.surfaceList, wfrGrp.surfaceList)) {
            return false;
         }
      }
      return true;
   }

   <T> void fromArray(ArrayList<T> v, T[] objs) {
      v.clear();
      if (objs != null) {
         for (int i = 0; i < objs.length; i++) {
            v.add(objs[i]);
         }
      }
   }

   /**
    * For testing ...
    */
   void set(
      Vector4d[] vtxs, Vector3d[] txts, Vector3d[] nrms, Face[] faces,
      Curve[] curves, Surface[] surfaces) {
      fromArray(vertexList, vtxs);
      fromArray(textureVertexList, txts);
      fromArray(normalList, nrms);
      fromArray(myCurrentGroup.faceList, faces);
      fromArray(myCurrentGroup.curveList, curves);
      fromArray(myCurrentGroup.surfaceList, surfaces);
   }

   /**
    * For testing ...
    */
   public String toString() {
      String s = "";
      for (Iterator<?> it = vertexList.iterator(); it.hasNext();) {
         s += ("v " + ((Vector4d)it.next()).toString() + "\n");
      }
      for (Iterator<?> it = textureVertexList.iterator(); it.hasNext();) {
         s += ("vt " + ((Vector3d)it.next()).toString() + "\n");
      }
      for (Iterator<?> it = normalList.iterator(); it.hasNext();) {
         s += ("vn " + ((Vector3d)it.next()).toString() + "\n");
      }
      Group[] groups = getGroups();
      for (Group grp : groups) {
         s += "g " + grp.name + "\n";
         for (Iterator<?> it = grp.faceList.iterator(); it.hasNext();) {
            s += ("f " + ((Face)it.next()).toString() + "\n");
         }
         for (Iterator<?> it = grp.lineList.iterator(); it.hasNext();) {
            s += ("l " + ((Line)it.next()).toString() + "\n");
         }
         for (Iterator<?> it = grp.curveList.iterator(); it.hasNext();) {
            s += ("curv " + ((Curve)it.next()).toString() + "\n");
         }
         for (Iterator<?> it = grp.surfaceList.iterator(); it.hasNext();) {
            s += ("surf " + ((Surface)it.next()).toString() + "\n");
         }
      }
      return s;
   }

   private int[] reindex (int[] indices, int[] indexMap) {
      if (indices == null) {
         return null;
      }
      int[] newIndices = new int[indices.length];
      for (int k = 0; k < indices.length; k++) {
         newIndices[k] = indexMap[indices[k]];
      }
      return newIndices;
   }

   private void markIndexMap(int[] indices, int[] indexMap, int lineNum)
      throws IOException {
      if (indices != null) {
         for (int k = 0; k < indices.length; k++) {
            int idx = indices[k];
            if (idx < 0 || idx >= indexMap.length) {
               throw new IOException(
                  "Index " + idx + " out of bounds, line " + lineNum);
            }
            indexMap[indices[k]] = -1;
         }
      }
   }

   public RenderProps getRenderProps() {
      return myCurrentGroup.props;
   }

   public RenderProps getGroupRenderProps(String groupName) {
      Group grp = myGroupMap.get(groupName);
      if (grp == null) {
         return null;
      }

      return grp.props;
   }

   public int[][] getLocalFaceIndicesAndVertices(
      ArrayList<Point3d> vtxList) throws IOException {

      int[] indexMap = new int[vertexList.size()];
      for (Face face : myCurrentGroup.faceList) {
         markIndexMap(face.indices, indexMap, face.lineNum);
      }
      int idx = 0;
      for (int i = 0; i < indexMap.length; i++) {
         if (indexMap[i] == -1) {
            indexMap[i] = idx++;
            Point3d pnt = new Point3d();
            pnt.setFromHomogeneous(vertexList.get(i));
            vtxList.add(pnt);
         }
      }
      if (idx == 0) {
         return null;
      }
      int[][] indexList = new int[myCurrentGroup.faceList.size()][];
      int i = 0;
      for (Face face : myCurrentGroup.faceList) {
         indexList[i++] = reindex(face.indices, indexMap);
      }
      return indexList;
   }

   public int[][] getLocalLineIndicesAndVertices(
      ArrayList<Point3d> vtxList) throws IOException {

      int[] indexMap = new int[vertexList.size()];
      for (Line line : myCurrentGroup.lineList) {
         markIndexMap(line.indices, indexMap, line.lineNum);
      }
      int idx = 0;
      for (int i = 0; i < indexMap.length; i++) {
         if (indexMap[i] == -1) {
            indexMap[i] = idx++;
            Point3d pnt = new Point3d();
            pnt.setFromHomogeneous(vertexList.get(i));
            vtxList.add(pnt);
         }
      }
      if (idx == 0) {
         return null;
      }
      int[][] indexList = new int[myCurrentGroup.lineList.size()][];
      int i = 0;
      for (Line line : myCurrentGroup.lineList) {
         indexList[i++] = reindex(line.indices, indexMap);
      }
      return indexList;
   }

   public int[] getLocalNormalIndicesAndVertices(
      ArrayList<Vector3d> nrmList) throws IOException {

      if (normalList.size() == 0) {
         return null;
      }
      int[] indexMap = new int[normalList.size()];
      for (Face face : myCurrentGroup.faceList) {
         markIndexMap(face.normalIndices, indexMap, face.lineNum);
      }
      int idx = 0;
      for (int i = 0; i < indexMap.length; i++) {
         if (indexMap[i] == -1) {
            indexMap[i] = idx++;
            nrmList.add(new Vector3d(normalList.get(i)));
         }
      }
      if (idx == 0) {
         return null;
      }
      ArrayList<Integer> indexList = new ArrayList<Integer>();
      for (Face face : myCurrentGroup.faceList) {
         int[] idxs = reindex(face.normalIndices, indexMap);
         if (idxs != null) {
            for (int j=0; j<idxs.length; j++) {
               indexList.add (idxs[j]);
            }
         }
         else {
            for (int j=0; j<face.indices.length; j++) {
               indexList.add (-1);
            }
         }
      }
      return ArraySupport.toIntArray (indexList);
   }

   public int[] getLocalTextureIndicesAndVertices(
      ArrayList<Vector3d> vtxList) throws IOException {

      if (textureVertexList.size() == 0) {
         return null;
      }
      int[] indexMap = new int[textureVertexList.size()];
      for (Face face : myCurrentGroup.faceList) {
         markIndexMap(face.textureIndices, indexMap, face.lineNum);
      }
      int idx = 0;
      for (int i = 0; i < indexMap.length; i++) {
         if (indexMap[i] == -1) {
            indexMap[i] = idx++;
            vtxList.add(new Vector3d(textureVertexList.get(i)));
         }
      }
      if (idx == 0) {
         return null;
      }
      ArrayList<Integer> indexList = new ArrayList<Integer>();
      for (Face face : myCurrentGroup.faceList) {
         int[] idxs = reindex(face.textureIndices, indexMap);
         if (idxs != null) {
            for (int j=0; j<idxs.length; j++) {
               indexList.add (idxs[j]);
            }     
         }
         else {
            for (int j=0; j<face.indices.length; j++) {
               indexList.add (-1);
            }     
         }
      }
      return ArraySupport.toIntArray (indexList);
   }

   public int[][] getGlobalFaceIndicesAndVertices(
      ArrayList<Point3d> vtxList) throws IOException {

      for (Vector4d v4 : vertexList) {
         Point3d pnt = new Point3d();
         pnt.setFromHomogeneous (v4);
         vtxList.add (pnt);
      }
      
      ArrayList<int[]> indexArray = new ArrayList<>();
      for (Entry<String,Group> entry : myGroupMap.entrySet ()) {
         Group group = entry.getValue ();
         for (Face face : group.faceList) {
            indexArray.add (face.indices);
         }
      }
      
      return indexArray.toArray (new int[indexArray.size ()][]);
   }

   public int[][] getGlobalLineIndicesAndVertices(
      ArrayList<Point3d> vtxList) throws IOException {

      for (Vector4d v4 : vertexList) {
         Point3d pnt = new Point3d();
         pnt.setFromHomogeneous (v4);
         vtxList.add (pnt);
      }
      
      ArrayList<int[]> indexArray = new ArrayList<>();
      for (Entry<String,Group> entry : myGroupMap.entrySet ()) {
         Group group = entry.getValue ();
         for (Line line : group.lineList) {
            indexArray.add (line.indices);
         }
      }
      
      return indexArray.toArray (new int[indexArray.size ()][]);
   }

   public int[] getGlobalNormalIndicesAndVertices(
      ArrayList<Vector3d> nrmList) throws IOException {

      if (normalList.size() == 0) {
         return null;
      }
      
      nrmList.addAll (normalList);
      
      ArrayList<Integer> indexList = new ArrayList<Integer>();
      
      for (Entry<String,Group> group : myGroupMap.entrySet ()) {
         for (Face face : group.getValue ().faceList) {
            int[] idxs = face.normalIndices;
            if (idxs != null) {
               for (int j=0; j<idxs.length; j++) {
                  indexList.add (idxs[j]);
               }
            }
            else {
               for (int j=0; j<face.indices.length; j++) {
                  indexList.add (-1);
               }
            }
         }
      }
      return ArraySupport.toIntArray (indexList);
   }

   public int[] getGlobalTextureIndicesAndVertices(
      ArrayList<Vector3d> vtxList) throws IOException {

      if (textureVertexList.size() == 0) {
         return null;
      }
      
      vtxList.addAll (textureVertexList);
      
      ArrayList<Integer> indexList = new ArrayList<Integer>();
      
      for (Entry<String,Group> group : myGroupMap.entrySet ()) {
         for (Face face : group.getValue ().faceList) {
            int[] idxs = face.textureIndices;
            if (idxs != null) {
               for (int j=0; j<idxs.length; j++) {
                  indexList.add (idxs[j]);
               }
            }
            else {
               for (int j=0; j<face.indices.length; j++) {
                  indexList.add (-1);
               }
            }
         }
      }
      return ArraySupport.toIntArray (indexList);
   }
   
   public Point3d[] getVertexPoints() {
      Point3d[] pnts = new Point3d[vertexList.size()];
      int i = 0;
      for (Iterator<?> it = vertexList.iterator(); it.hasNext();) {
         pnts[i] = new Point3d();
         pnts[i].setFromHomogeneous((Vector4d)it.next());
         i++;
      }
      return pnts;
   }
   
   public Vector3d[] getVertexNormals() {
      if (normalList == null || normalList.size() == 0) { 
         return null;
      }
      return normalList.toArray(new Vector3d[normalList.size()]);
   }

   public Vector4d[] getHomogeneousPoints() {
      return (Vector4d[])vertexList.toArray(new Vector4d[0]);
   }

   public int[][] getFaceIndices() {
      int[][] indices = new int[myCurrentGroup.faceList.size()][];
      int i = 0;
      for (Face face : myCurrentGroup.faceList) {
         indices[i] = face.indices;
         i++;
      }
      return indices;
   }

   public int[][] getLineIndices() {
      int[][] indices = new int[myCurrentGroup.lineList.size()][];
      int i = 0;
      for (Line line : myCurrentGroup.lineList) {
         indices[i] = line.indices;
         i++;
      }
      return indices;
   }

   public int[][] getTextureIndices() {
      int[][] texIndices = new int[myCurrentGroup.faceList.size()][];
      int i = 0;
      boolean hasIndices = false;
      for (Face face : myCurrentGroup.faceList) {
         texIndices[i] = face.textureIndices;
         if (texIndices[i] != null) {
            hasIndices = true;
         }
         i++;
      }
      return hasIndices ? texIndices : null;
   }

   public int[][] getVertexNormalIndices() {
      int[][] vnIndices = new int[myCurrentGroup.faceList.size()][];
      int i = 0;
      boolean hasIndices = false;
      for (Face face : myCurrentGroup.faceList) {
         vnIndices[i] = face.normalIndices;
         if (vnIndices[i] != null) {
            hasIndices = true;
         }
         i++;
      }
      return hasIndices ? vnIndices : null;
   }

   public ArrayList<Vector4d> getVertexList() {
      return vertexList;
   }

   public ArrayList<Vector3d> getTextureVertexList() {
      return textureVertexList;
   }

   public ArrayList<Vector3d> getNormalList() {
      return normalList;
   }

   public ArrayList<Face> getFaceList() {
      return myCurrentGroup.faceList;
   }

   public ArrayList<Line> getLineList() {
      return myCurrentGroup.lineList;
   }

   public ArrayList<Curve> getCurveList() {
      return myCurrentGroup.curveList;
   }

   public ArrayList<Surface> getSurfaceList() {
      return myCurrentGroup.surfaceList;
   }

   public boolean isVerbose() {
      return verbose;
   }

   public void setVerbose(boolean verb) {
      verbose = verb;
   }

   protected String setGroupName (String name) {
      //      if (name == null) {
      //         String[] nameList = getPolyhedralGroupNames();
      //         if (nameList.length > 0) {
      //            name = nameList[0];
      //         }
      //         else {
      //            // will result in a null mesh since 'default' not a polyhedral group
      //            name = DEFAULT_GROUP;
      //         }
      //      }
      if (!hasGroup (name)) {
         throw new IllegalArgumentException ("Group '"+name+"' unknown");
      }
      setGroup (name);      
      return name;
   }

   protected void setNameAndRenderProps (MeshBase mesh, String name) {
      mesh.setName (name);
      if (getRenderProps() != null) {
         mesh.setRenderProps (getRenderProps());
      }
   }

   public void setMesh (PolygonalMesh mesh)
      throws IOException {
      setMesh (mesh, /*groupName=*/null);
   }

   public void setMesh (PolygonalMesh mesh, String groupName)
      throws IOException {      

      mesh.clear();
            
      groupName = setGroupName (groupName);

      ArrayList<Point3d> vtxList = new ArrayList<Point3d>();
      int[][] indices;
      if (groupName != null) {
         indices = getLocalFaceIndicesAndVertices (vtxList);
      } else {
         indices = getGlobalFaceIndicesAndVertices(vtxList);
      }

      for (int i=0; i<vtxList.size(); i++) {
         // add by reference since points have already been copied 
         mesh.addVertex (vtxList.get(i), /* byReference= */true);
      }
      if (indices != null) {
         for (int k=0; k<indices.length; k++) {
            mesh.addFace (indices[k]);
         }
      }
      ArrayList<Vector3d> textureCoords = new ArrayList<Vector3d>();
      int[] tindices;
      if (groupName != null) {
         tindices = getLocalTextureIndicesAndVertices (textureCoords);
      } else {
         tindices = getGlobalTextureIndicesAndVertices (textureCoords);
      }
      if (tindices != null) {
         // for now, make sure we don't have partial texture coordinates
         boolean incompleteTexture = false;
         for (int i=0; i<tindices.length; i++) {
            if (tindices[i] == -1) {
               incompleteTexture = true;
               break;
            }
         }
         if (!incompleteTexture) {
            mesh.setTextureCoords (textureCoords, tindices);
         }
      }
      ArrayList<Vector3d> normals = new ArrayList<Vector3d>();
      int[] nindices;
      if (groupName != null ) {
         nindices = getLocalNormalIndicesAndVertices (normals);
      } else {
         nindices = getGlobalNormalIndicesAndVertices (normals);
      }
      if (nindices != null) {
         mesh.setNormals (normals, nindices);
         mesh.setHardEdgesFromNormals();
      }      
      setNameAndRenderProps (mesh, groupName);
   }

   public MeshBase readMesh (MeshBase mesh) 
      throws IOException {
      
      if (!myInputHasBeenParsed) {
         parse ();
      }

      if (mesh == null) {
         if (myCurrentGroup.faceList.size() == 0) {
            if (myCurrentGroup.lineList.size () == 0) {
               mesh = new PointMesh();
            } else {
               mesh = new PolylineMesh();
            }
         }
         else {
            mesh = new PolygonalMesh();
         }
      }
      if (mesh instanceof PolygonalMesh) {
         setMesh ((PolygonalMesh)mesh);
      }
      else if (mesh instanceof PolylineMesh) {
         setMesh ((PolylineMesh)mesh);
      }
      else if (mesh instanceof PointMesh) {
         setMesh ((PointMesh)mesh);
      }
      else {
         throw new IllegalArgumentException (
            "Mesh type "+mesh.getClass()+" not supported for '.obj' files");
      }
      return mesh;
   }
   
   @Override
   public PolygonalMesh readMesh() throws IOException {
      return (PolygonalMesh)readMesh (new PolygonalMesh());
   }

   public void setMesh (PointMesh mesh)
      throws IOException {
      setMesh (mesh, /*groupName=*/null);
   }

   public void setMesh (PointMesh mesh, String groupName)
      throws IOException {

      mesh.clear();
      groupName = setGroupName (groupName);

      List<Point3d> vtxList = Arrays.asList(getVertexPoints());
      for (int i=0; i<vtxList.size(); i++) {
         // add by reference since points have already been copied 
         mesh.addVertex (vtxList.get(i), /* byReference= */true);
      }
      Vector3d[] nrms = getVertexNormals ();
      if (nrms != null) {
         List<Vector3d> normalList = Arrays.asList(getVertexNormals());
         mesh.setNormals (new ArrayList<Vector3d>(normalList), null);
      } else {
         mesh.clearNormals ();
      }

      setNameAndRenderProps (mesh, groupName);
   }

   public void setMesh (PolylineMesh mesh)
      throws IOException {
      setMesh (mesh, /*groupName=*/null);
   }

   public void setMesh (PolylineMesh mesh, String groupName)
      throws IOException {

      mesh.clear();
      groupName = setGroupName (groupName);


      ArrayList<Point3d> vtxList = new ArrayList<Point3d>();
      int[][] indices;
      if (groupName != null) {
         indices = getLocalLineIndicesAndVertices (vtxList);
      } else {
         indices = getGlobalLineIndicesAndVertices (vtxList);
      }

      for (int i=0; i<vtxList.size(); i++) {
         // add by reference since points have already been copied 
         mesh.addVertex (vtxList.get(i), /* byReference= */true);
      }
      if (indices != null) {
         for (int k=0; k<indices.length; k++) {
            mesh.addLine (indices[k]);
         }
      }

      setNameAndRenderProps (mesh, groupName);
   }

   public static MeshBase read (File file) throws IOException {
      WavefrontReader reader = new WavefrontReader (file);
      try {
         return reader.readMesh (null);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         reader.close();
      }
    }

   public static MeshBase read (String fileName) throws IOException {
      return read (new File(fileName));
    }

   public static PolygonalMesh readFromString (
      String input, boolean zeroIndexed) {
      WavefrontReader reader = new WavefrontReader (new StringReader (input));
      reader.setZeroIndexed (zeroIndexed);
      try {
         return (PolygonalMesh)reader.readMesh (null);
      }
      catch (Exception e) {
         throw new IllegalArgumentException (
            "Illegal mesh input string: " + e.getMessage());
      }
      finally {
         reader.close();
      }
   }

   public static PolygonalMesh readFromString (String input) {
      return readFromString (input, false);
   }

}
