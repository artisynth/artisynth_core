package maspack.geometry.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Polyline;
import maspack.geometry.PolylineMesh;
import maspack.geometry.Vertex3d;
import maspack.geometry.io.MayaAsciiReader.MayaTransformBuilder.RotationOrder;
import maspack.graph.Node;
import maspack.graph.Tree;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;

// too complex to be associated with a simple MeshReader interface
public class MayaAsciiReader {

   private static int SEMICOLON = ';';
   private static int DASH_CHAR = '-';
   private static String PERIOD_STR = ".";
   public static boolean verbose = false;

   static final UnitInfo SI_UNITS = new UnitInfo(
      LengthUnit.METER, AngleUnit.RADIAN, TimeUnit.SECOND);

   HashMap<String,MayaNodeParser> parsers;
   IgnoreParser ignoreParser;
   private UnitInfo defaultUnits = SI_UNITS;

   public enum LengthUnit {
      MILLIMETER (1e-3, "millimeter", "mm"),
      CENTIMETER (1e-2, "centimeter", "cm"),
      METER (1, "meter", "m"),
      KILOMETER (1e3, "kilometer", "km"),
      INCH (0.0254, "inch", "in"),
      FOOT (0.3048, "foot", "ft"),
      YARD (0.9144, "yard", "yd"),
      MILE (1609, "mile", "mi");

      String[] myStrs;
      double mySI;

      private LengthUnit(double si, String... str) {
         myStrs = str;
         mySI = si;
      }

      public double getSI() {
         return mySI;
      }

      public static LengthUnit detect(String str) {
         for (LengthUnit v : values()) {
            for (String s : v.myStrs) {
               if (s.equalsIgnoreCase(str)) {
                  return v;
               }
            }
         }
         return null;
      }
   }

   public enum AngleUnit {
      DEGREE (Math.PI / 180, "degree", "deg"), RADIAN (1, "radian", "rad");

      String[] myStrs;
      double mySI;

      private AngleUnit(double si, String... str) {
         myStrs = str;
         mySI = si;
      }

      public double getSI() {
         return mySI;
      }

      public static AngleUnit detect(String str) {
         for (AngleUnit v : values()) {
            for (String s : v.myStrs) {
               if (s.equalsIgnoreCase(str)) {
                  return v;
               }
            }
         }
         return null;
      }
   }

   public enum TimeUnit {
      HOUR (3600, "hour"), MINUTE (60, "min"), SECOND (1, "sec"), MILLISECOND (
         1e-3,
         "millisec"), GAME (1.0 / 15, "game"), FILM (1.0 / 24, "film"), PAL (
         1.0 / 25,
         "pal"), NTSC (1.0 / 30, "ntsc"), SHOW (1.0 / 48, "show"), PALF (
         1.0 / 50,
         "palf"), NTSCF (1.0 / 60, "ntscf");

      String[] myStrs;
      double mySI;

      private TimeUnit(double si, String... str) {
         myStrs = str;
         mySI = si;
      }

      public double getSI() {
         return mySI;
      }

      public static TimeUnit detect(String str) {
         for (TimeUnit v : values()) {
            for (String s : v.myStrs) {
               if (s.equalsIgnoreCase(str)) {
                  return v;
               }
            }
         }
         return null;
      }
   }

   public static class UnitInfo {
      LengthUnit length;
      AngleUnit angle;
      TimeUnit time;

      public UnitInfo(LengthUnit lu, AngleUnit au, TimeUnit tu) {
         length = lu;
         angle = au;
         time = tu;
      }
   }

   public static class UnitUtility {
      public static double convertLength(
         double src, LengthUnit srcUnit, LengthUnit destUnit) {
         return src * srcUnit.getSI() / destUnit.getSI();
      }

      public static double convertAngle(
         double src, AngleUnit srcUnit, AngleUnit destUnit) {
         return src * srcUnit.getSI() / destUnit.getSI();
      }

      public static double convertTime(
         double src, TimeUnit srcUnit, TimeUnit destUnit) {
         return src * srcUnit.getSI() / destUnit.getSI();
      }
   }

   private static class MayaAttribute {
      public HashMap<String,Object> options = new HashMap<String,Object>();
      public String name;
      public Object data;
   }

   private static class MayaNodeInfo {
      String type;
      String name;
      String parent;
      boolean skipSelect;
      boolean shared;
   }

   public static class MayaNode {
      MayaNodeInfo info;
      public UnitInfo units = null;
      public HashMap<String,MayaAttribute> attributes =
         new HashMap<String,MayaAttribute>();

      public MayaNode(MayaNodeInfo info) {
         this.info = info;
      }

      public void addAttribute(
         String name, HashMap<String,Object> options, Object data) {
         MayaAttribute attr = new MayaAttribute();
         attr.name = name;
         attr.options = options;
         attr.data = data;
         addAttribute(attr);
      }

      public void addAttribute(MayaAttribute attr) {
         attributes.put(attr.name, attr);
      }

      public String getName() {
         return info.name;
      }

      public String getParent() {
         return info.parent;
      }

      public String toString() {
         return info.name;
      }

      public String type() {
         return "Generic";
      }
   }

   public static class MayaTransform extends MayaNode {
      
      private AffineTransform3dBase transform = null;
      
      public MayaTransform(MayaNodeInfo info) {
         super(info);
      }

      public boolean inheritsTransform() {
         MayaAttribute ma = attributes.get("inheritsTransform");
         if (ma == null) {
            return true;
         }
         return (Boolean)(ma.data);
      }

      public AffineTransform3dBase buildTransform() {
         
         MayaTransformBuilder builder = new MayaTransformBuilder();
         
         // loop through properties, setting what I can decipher
         for (Entry<String,MayaAttribute> attrib : attributes.entrySet()) {
            String key = attrib.getKey();
            if ("translate".equals(key)) {
               builder.setT(new Vector3d((double[])(attrib.getValue().data)));
            } else if ("rotate".equals(key)) {
               
               // determine rotate order
               RotationOrder rotOrder = RotationOrder.XYZ; // default
               if (attributes.containsKey("rotateOrder")) {
                  Integer ri = (Integer)(attributes.get("rotateOrder").data);
                  rotOrder = RotationOrder.values()[ri.intValue()];
               }
               
               double[] v = (double[])(attrib.getValue().data);
               double[] va = null;
               if (units.angle == AngleUnit.DEGREE) {
                  va = new double[3];
                  for (int i=0; i<3; i++) {
                     va[i] = Math.toRadians(v[i]);
                  }
               } else {
                  va = v;
               }
               builder.setR(new Vector3d(va), rotOrder);
               
            } else if ("scale".equals(key)) {
               double[] s = (double[])(attrib.getValue().data);
               builder.setS(new Vector3d(s));
            } else if ("shear".equals(key)) {
               double[] s = (double[])(attrib.getValue().data);
               builder.setSH(new Vector3d(s));
            } else if ("rotatePivot".equals(key)) {
               double[] rp = (double[])(attrib.getValue().data);
               builder.setRP(new Vector3d(rp));
            } else if ("rotatePivotTranslate".equals(key)) {
               double[] rpt = (double[])(attrib.getValue().data);
               builder.setRT(new Vector3d(rpt));
            } else if ("scalePivot".equals(key)) {
               double[] sp = (double[])(attrib.getValue().data);
               builder.setSP(new Vector3d(sp));
            } else if ("scalePivotTranslate".equals(key)) {
               double[] spt = (double[])(attrib.getValue().data);
               builder.setST(new Vector3d(spt));
            } else if ("rotateAxis".equals(key)) {
               double[] ra = (double[])(attrib.getValue().data);
               builder.setRA(new Vector3d(ra));
            }
         } // end looping through attributes
         
         return builder.getTransform();
         
      }
      
      public void getTransform(AffineTransform3d trans) {

         if (transform == null) {
            transform = buildTransform();
         }
         trans.set(transform);
         
      }

      public String type() {
         return "Transform";
      }
   }

   private static class MayaNurbsCurve extends MayaNode {

      public static class NurbsCurve {
         int degree;
         int spans;
         int form; // 0:open, 1:closed, 2:periodic
         boolean rational;
         int dim;
         double[] knots;
         double[][] cv; // dim (+1 if rational)
      }

      NurbsCurve nurbs;
      public Polyline curve = new Polyline(-1);

      public MayaNurbsCurve(MayaNodeInfo info) {
         super(info);
      }

      public String type() {
         return "Curve";
      }
   }

   public static class MayaMesh extends MayaNode {
      PolygonalMesh mesh = null;;

      public MayaMesh(MayaNodeInfo info) {
         super(info);
      }

      public PolygonalMesh createMesh() {
         // create from attributes
         MayaAttribute vrts = attributes.get("vrts");
         if (vrts == null) {
            System.err.println("mesh has no vertices");
            return null;
         }
         double[][] vt = (double[][])(vrts.data);
         
         MayaAttribute edge = attributes.get("edge");
         if (edge == null) {
            System.err.println("mesh has no edges");
            return null;
         }
         int[][] ed = (int[][])(edge.data);
         
         MayaAttribute face = attributes.get("face");
         if (face == null) {
            System.err.println("mesh has no faces");
            return null;
         }
         int[][] fc = (int[][])(face.data);

         int faces[][] = new int[fc.length][];

         // build face array
         for (int i = 0; i < fc.length; i++) {
            faces[i] = new int[fc[i].length];

            for (int j = 0; j < fc[i].length; j++) {
               int e = fc[i][j];
               // if negative, flip direction (first vertex is 1)
               if (e < 0) {
                  faces[i][j] = ed[-e - 1][1];
               } else {
                  faces[i][j] = ed[e][0];
               }
            }
         }

         PolygonalMesh mesh = new PolygonalMesh();
         mesh.set(vt, faces);

         return mesh;
      }

      public PolygonalMesh getMesh() {
         if (mesh == null) {
            mesh = createMesh();
         }
         return mesh;
      }

      public String type() {
         return "Mesh";
      }
   }

   Tree<MayaNode> tree = new Tree<MayaNode>();

   public MayaAsciiReader() {
      parsers = new HashMap<String,MayaNodeParser>();
      parsers.put("transform", new TransformParser());
      parsers.put("nurbsCurve", new NurbsCurveParser());
      parsers.put("mesh", new MeshParser());

      ignoreParser = new IgnoreParser();
      parsers.put("camera", ignoreParser);
      parsers.put("lightLinker", ignoreParser);
      parsers.put("displayLayerManager", ignoreParser);
      parsers.put("displayLayerManager", ignoreParser);
      parsers.put("displayLayer", ignoreParser);
      parsers.put("renderLayerManager", ignoreParser);
      parsers.put("renderLayer", ignoreParser);

   }

   public MayaAsciiReader(File file) throws IOException {
      this();
      BufferedReader reader = null;
      try {
         reader = new BufferedReader(new FileReader(file));
      }
      catch (IOException e) {
         throw e;
      }
      
      read(reader);
      
      if (reader != null) {
         reader.close();
      }
   }

   public void read(Reader reader) throws IOException {
      read(new ReaderTokenizer(reader));
   }

   public void read(ReaderTokenizer rtok) throws IOException {

      rtok.eolIsSignificant(false);
      rtok.ordinaryChar(';');

      ArrayList<MayaNode> nodes = new ArrayList<MayaNode>();
      UnitInfo currentUnits =
         new UnitInfo(LengthUnit.CENTIMETER, AngleUnit.DEGREE, TimeUnit.FILM); // default
                                                                               // for
                                                                               // Maya

      while (rtok.ttype != ReaderTokenizer.TT_EOF) {
         // search for a new node
         rtok.nextToken();
         if (rtok.ttype == ReaderTokenizer.TT_WORD
            && rtok.sval.equals("createNode")) {
            MayaNode node = parseNode(rtok, currentUnits);
            if (node != null) {
               nodes.add(node);
            }
         } else if (rtok.ttype == ReaderTokenizer.TT_WORD
            && rtok.sval.equals("currentUnit")) {
            parseUnits(rtok, currentUnits);
         }
      }

      buildTree(tree, nodes);

   }

   public void setDefaultUnits(UnitInfo units) {
      defaultUnits = units;
   }

   public UnitInfo getDefaultUnits() {
      return defaultUnits;
   }

   public String[] getGroupNames() {
      ArrayList<String> groupNames = new ArrayList<String>();
      addChildrenNames(tree.getRootElement(), groupNames, "", "");
      return groupNames.toArray(new String[groupNames.size()]);
   }

   public String[] getGroupHierarchy(String levelIndicator) {
      ArrayList<String> groupNames = new ArrayList<String>();
      addChildrenNames(tree.getRootElement(), groupNames, "", levelIndicator);
      return groupNames.toArray(new String[groupNames.size()]);
   }

   public PolygonalMesh getPolygonalMesh() {
      return getPolygonalMesh(tree.getRootElement(), null);
   }

   public PolygonalMesh getPolygonalMesh(UnitInfo units) {
      return getPolygonalMesh(tree.getRootElement(), units);
   }

   public PolygonalMesh getPolygonalMesh(String group) {
      return getPolygonalMesh(group, null);
   }

   public PolygonalMesh getPolygonalMesh(String group, UnitInfo units) {

      Node<MayaNode> root = tree.getRootElement();
      if (group != null) {
         root = getNode(group);
      }
      if (root == null) {
         throw new IllegalArgumentException("Group \"" + group
            + "\" not found.");
      }
      return getPolygonalMesh(root, units, null);
   }
   
   public PolygonalMesh getPolygonalMesh(Node<MayaNode> root, UnitInfo units) {
      return getPolygonalMesh (root, units, null);
   }
   
   public PolygonalMesh getPolygonalMesh(Node<MayaNode> root, UnitInfo units, String regex) {

      if (units == null) {
         units = defaultUnits;
      }

      Pattern pregex = null;
      if (regex != null) {
         pregex = Pattern.compile(regex);
      }
      
      PolygonalMesh mesh = new PolygonalMesh();
      AffineTransform3d trans = new AffineTransform3d();
      recursiveBuildParentTransform(root, trans, units);
      recursiveAddPolygonalMeshes(root, mesh, trans, units, pregex);

      return mesh;

   }

   private void recursiveAddPolygonalMeshes(
      Node<MayaNode> root, PolygonalMesh mesh, AffineTransform3d trans,
      UnitInfo units, Pattern regex) {

      trans = new AffineTransform3d(trans); // make copy so can traverse
                                            // children independently

      MayaNode data = root.getData();
      if (data instanceof MayaTransform) {
         MayaTransform dtrans = (MayaTransform)data;
         AffineTransform3d tu = new AffineTransform3d();
         dtrans.getTransform(tu);

         // convert units
         tu.p.scale(dtrans.units.length.getSI() / units.length.getSI());
         // only multiply if inherited
         if (dtrans.inheritsTransform()) {
            trans.mul(tu);
         } else {
            trans.set(tu);
         }
      } else if (data instanceof MayaMesh) {
         MayaMesh mm = (MayaMesh)data;
         
         if (regex == null || regex.matcher(mm.getName()).matches()) {
            
            PolygonalMesh mmesh = mm.getMesh();
   
            if (mmesh != null) {
               // transform mesh
               HashMap<Vertex3d,Vertex3d> vtxMap =
                  new HashMap<Vertex3d,Vertex3d>();
               for (Vertex3d vtx : mmesh.getVertices()) {
                  Vertex3d nvtx = new Vertex3d(vtx.pnt);
                  // XXX prevent transform
                  nvtx.pnt.scale(mm.units.length.getSI() / units.length.getSI());
                  nvtx.pnt.transform(trans);
                  vtxMap.put(vtx, nvtx);
                  mesh.addVertex(nvtx);
               }
   
               for (Face face : mmesh.getFaces()) {
                  Vertex3d[] oface = face.getVertices();
                  Vertex3d[] nface = new Vertex3d[oface.length];
                  for (int i = 0; i < oface.length; i++) {
                     nface[i] = vtxMap.get(oface[i]);
                  }
                  mesh.addFace(nface);
               }
            }
         }
      }

      for (Node<MayaNode> child : root.getChildren()) {
         recursiveAddPolygonalMeshes(child, mesh, trans, units, regex);
      }

   }

   public PolylineMesh getPolylineMesh() {
      return getPolylineMesh(tree.getRootElement(), null, null);
   }

   public PolylineMesh getPolylineMesh(UnitInfo units) {
      return getPolylineMesh(tree.getRootElement(), units, null);
   }

   public PolylineMesh getPolylineMesh(String group, String regex) {
      return getPolylineMesh(group, null, regex);
   }

   public PolylineMesh getPolylineMesh(String group) {
      return getPolylineMesh(group, null, null);
   }

   public PolylineMesh getPolylineMesh(String group, UnitInfo units) {
      return getPolylineMesh(group, units, null);
   }

   public PolylineMesh getPolylineMesh(
      String group, UnitInfo units, String regex) {

      if ("".equals(group) || "/".equals(group)) {
         group = null;
      }
      Node<MayaNode> root = tree.getRootElement();
      if (group != null) {
         root = getNode(group);
      }
      if (root == null) {
         throw new IllegalArgumentException("Group \"" + group
            + "\" not found.");
      }

      return getPolylineMesh(root, units, regex);
   }

   public PolylineMesh getPolylineMesh(
      Node<MayaNode> root, UnitInfo units, String regex) {

      if (units == null) {
         units = defaultUnits;
      }
      PolylineMesh mesh = new PolylineMesh();
      AffineTransform3d trans = new AffineTransform3d();
      recursiveBuildParentTransform(root, trans, units);

      Pattern pregex = null;
      if (regex != null) {
         pregex = Pattern.compile(regex);
      }
      recursiveAddPolylines(root, mesh, trans, units, pregex);

      return mesh;

   }

   private void recursiveBuildParentTransform(
      Node<MayaNode> leaf, AffineTransform3d trans, UnitInfo units) {

      if (leaf.getNumberOfParents() > 0) {
         Node<MayaNode> parent = leaf.getParent(0);
         MayaNode data = parent.getData();
         if (data instanceof MayaTransform) {
            MayaTransform dtrans = (MayaTransform)data;
            AffineTransform3d tu = new AffineTransform3d();
            dtrans.getTransform(tu);

            // convert units
            tu.p.scale(dtrans.units.length.getSI() / units.length.getSI());
            if (dtrans.inheritsTransform()) {
               trans.mul(tu);
            } else {
               trans.set(tu);
            }
         }

         recursiveBuildParentTransform(parent, trans, units);
      }
   }

   private void recursiveAddPolylines(
      Node<MayaNode> root, PolylineMesh mesh, AffineTransform3d trans,
      UnitInfo units, Pattern pregex) {

      trans = new AffineTransform3d(trans); // make copy so can traverse
                                            // children independently

      MayaNode data = root.getData();
      if (data instanceof MayaTransform) {
         MayaTransform dtrans = (MayaTransform)data;
         AffineTransform3d tu = new AffineTransform3d();
         dtrans.getTransform(tu);

         // convert units
         tu.p.scale(dtrans.units.length.getSI() / units.length.getSI());
         if (dtrans.inheritsTransform()) {
            trans.mul(tu);
         } else {
            trans.set(tu);
         }
      } else if (data instanceof MayaNurbsCurve) {
         MayaNurbsCurve mnc = (MayaNurbsCurve)data;

         if (pregex == null || pregex.matcher(mnc.getName()).matches()) {
            Polyline line = new Polyline(mnc.curve);

            if (line != null) {
               // transform line
               for (Vertex3d vtx : line.getVertices()) {
                  vtx.pnt
                     .scale(mnc.units.length.getSI() / units.length.getSI());
                  vtx.pnt.transform(trans);
               }
               mesh.addLine(line);
            }
         }

      }

      for (Node<MayaNode> child : root.getChildren()) {
         recursiveAddPolylines(child, mesh, trans, units, pregex);
      }

   }

   public Tree<MayaNode> getTree() {
      return tree;
   }

   private Node<MayaNode> getNode(String nodeName) {
      if (nodeName == null) {
         return tree.getRootElement();
      }
      
      // System.out.print(getGroupNames());

      // first look for node with given exact name
      Node<MayaNode> node = recursiveGetNode(tree.getRootElement(), nodeName);

      if (node == null) {
         int idx = nodeName.indexOf('/');
         if (idx >= 0) {
            String[] nodePath = nodeName.split("/");
            node = getSplitNode(tree.getRootElement(), nodePath);
         }
      }
      return node;
   }

   public Node<MayaNode> getSplitNode(Node<MayaNode> root, String[] nodePath) {

      // traverse tree until we find the full path
      Node<MayaNode> node = root;
      for (int i = 0; i < nodePath.length; i++) {
         List<Node<MayaNode>> children = node.getChildren();
         node = null;
         for (Node<MayaNode> child : children) {
            MayaNode data = child.getData();
            if (data != null) {
               if (data.getName().equals(nodePath[i])) {
                  node = child;
                  continue;
               }
            }
         }

         if (node == null) {
            return null;
         }
      }

      return node;

   }

   private Node<MayaNode> recursiveGetNode(Node<MayaNode> root, String nodeName) {

      for (Node<MayaNode> child : root.getChildren()) {
         if (nodeName.equals(child.getData().getName())) {
            return child;
         }
      }

      for (Node<MayaNode> child : root.getChildren()) {
         Node<MayaNode> out = recursiveGetNode(child, nodeName);
         if (out != null) {
            return out;
         }
      }

      return null;
   }

   private void addChildrenNames(
      Node<MayaNode> root, ArrayList<String> nameList, String prefix,
      String levelAppend) {
      for (Node<MayaNode> node : root.getChildren()) {
         MayaNode data = node.getData();
         if (data instanceof MayaTransform) {
            nameList.add(prefix + node.getData().getName());
         }
         addChildrenNames(node, nameList, prefix + levelAppend, levelAppend);
      }
   }

   private static class TreeDepthComparator implements Comparator<MayaNode> {

      @Override
      public int compare(MayaNode o1, MayaNode o2) {

         int pCount0 = 0;
         int pCount1 = 0;

         String p0 = o1.getParent();
         String p1 = o2.getParent();
         if (p0 != null) {
            pCount0 = countChar(p0, '|') + 1;
         }
         if (p1 != null) {
            pCount1 = countChar(p1, '|') + 1;
         }

         if (pCount0 < pCount1) {
            return -1;
         } else if (pCount0 > pCount1) {
            return 1;
         }

         return 0;
      }

      private static int countChar(String str, char c) {
         int count = 0;
         for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) == c) {
               count++;
            }
         }
         return count;
      }

   }
   
   private static void buildTree(Tree<MayaNode> tree, ArrayList<MayaNode> nodes) {

      
      Node<MayaNode> root = new Node<MayaNode>();
      tree.setRootElement(root);
      // ArrayList<Node<MayaNode>> treeNodes = new ArrayList<Node<MayaNode>>();

      ArrayDeque<Node<MayaNode>> orphan = new ArrayDeque<Node<MayaNode>>();
      ArrayList<Node<MayaNode>> treeNodes = new ArrayList<Node<MayaNode>>();

      // sort by depth if possible
      Collections.sort(nodes, new TreeDepthComparator());
      
      // create a map of all nodes
      HashMap<String,Node<MayaNode>> nodeMap = new HashMap<String,Node<MayaNode>>(nodes.size());
      for (MayaNode node : nodes) {
         Node<MayaNode> mnode = new Node<MayaNode>(node);
         nodeMap.put(node.getName(), mnode);
         orphan.add(mnode);  
      }

      // build tree
      int nSkipped = 0; // for detecting case where can't place node

      // loop through all "orphans", trying to attach to tree
      while (orphan.size() > 0) {

         boolean mod = false; // whether tree was modified
         Node<MayaNode> node = orphan.poll(); // grab the first entry
         

         // attach parent
         String parent = node.getData().getParent();
         if (parent == null) {
            root.addChild(node);
            treeNodes.add(node);
            mod = true;
         } else {

            // try to attach to existing tree, or else
            // throw to back of queue
            String[] hnames = parent.split("\\|");
            Node<MayaNode> pnode = null;
            if (hnames[0].equals("")) {
               pnode = root;
            } else {
               pnode = nodeMap.get(hnames[0]);
            }

            // travel down tree
            for (int i = 1; i < hnames.length; i++) {
               if (pnode == null) {
                  break;
               }
               // find next node in sequence
               List<Node<MayaNode>> childs = pnode.getChildren();
               pnode = null;
               for (Node<MayaNode> child : childs) {
                  if (child.getData().getName().equals(hnames[i])) {
                     pnode = child;
                     break;
                  }
               }
            }

            if (pnode != null) {
               pnode.addChild(node);
               treeNodes.add(node);
               mod = true;
            }
         }

         if (mod == false) {
            nSkipped++;
            orphan.addLast(node); // add to end of queue
            if (nSkipped > orphan.size()) {
               throw new IllegalStateException(
                  "Can't place Maya nodes into hierarchy");
            }
         } else {
            nSkipped = 0;
         }
      }

   }

   private void parseUnits(ReaderTokenizer rtok, UnitInfo info)
      throws IOException {
      int dashSetting = rtok.getCharSetting(DASH_CHAR);
      rtok.wordChar(DASH_CHAR);

      while (rtok.nextToken() != SEMICOLON) {
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if ("-l".equals(rtok.sval) || "-linear".equals(rtok.sval)) {
               // length
               String id = rtok.scanWordOrQuotedString('"');
               info.length = LengthUnit.detect(id);
            } else if ("-a".equals(rtok.sval) || "-angle".equals(rtok.sval)) {
               // angle
               String id = rtok.scanWordOrQuotedString('"');
               info.angle = AngleUnit.detect(id);
            } else if ("-t".equals(rtok.sval) || "-time".equals(rtok.sval)) {
               // time
               String id = rtok.scanWordOrQuotedString('"');
               info.time = TimeUnit.detect(id);
            } else {
               String cmd = rtok.sval;
               String option = null;
               rtok.nextToken();
               if (rtok.ttype == ReaderTokenizer.TT_WORD) {
                  if (rtok.sval.startsWith("-") || rtok.ttype == SEMICOLON) {
                     rtok.pushBack();
                  } else {
                     option = rtok.sval;
                  }
               }
               System.err
                  .println("Unknown unit option: " + cmd + ", " + option);
            }
         } else {
            System.err.println("Invalid unit token: " + rtok.ttype);
         }
      }
      rtok.setCharSetting(DASH_CHAR, dashSetting);
   }

   private MayaNode parseNode(ReaderTokenizer rtok, UnitInfo units)
      throws IOException {

      // parse type/name/parent/shared/skipSelect
      MayaNodeInfo info = parseNodeLine(rtok);

      // find appropriate parser
      MayaNodeParser parser = parsers.get(info.type);
      if (parser == null) {
         if (verbose) {
            System.err.println("Unknown node of type '" + info.type
               + "', ignoring.");
         }
         parser = ignoreParser;
      }
      return parser.parseNode(info, units, rtok);
   }

   private static void parse3double(ReaderTokenizer rtok, double[] v)
      throws IOException {
      int read = scanNumbers(rtok, v, 3);
      if (read != 3) {
         throw new IOException("Cannot read 3 doubles on line " + rtok.lineno());
      }
   }

   private static MayaNodeInfo parseNodeLine(ReaderTokenizer rtok)
      throws IOException {

      int saveDash = rtok.getCharSetting(DASH_CHAR);
      rtok.wordChar(DASH_CHAR);
      MayaNodeInfo info = new MayaNodeInfo();

      rtok.nextToken();
      while (rtok.ttype != SEMICOLON) {
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (!rtok.sval.startsWith("-")) {
               info.type = rtok.sval;
            } else if ("-n".equals(rtok.sval) || "-name".equals(rtok.sval)) { // name
               info.name = rtok.scanQuotedString('"');
            } else if ("-p".equals(rtok.sval) || "-parent".equals(rtok.sval)) { // parent
               info.parent = rtok.scanQuotedString('"');
            } else if ("-s".equals(rtok.sval) || "-shared".equals(rtok.sval)) { // shared
               info.shared = true;
            } else if ("-ss".equals(rtok.sval)
               || "-skipSelect".equals(rtok.sval)) {
               info.skipSelect = true;
            } else {
               System.err.println("ERROR! Unknown flag: " + rtok.sval);
            }

         }
         rtok.nextToken();
      }
      rtok.setCharSetting(DASH_CHAR, saveDash);

      return info;
   }

   private static MayaMesh parseMesh(
      ReaderTokenizer rtok, UnitInfo units, MayaNodeInfo info)
      throws IOException {

      MayaMesh mesh = new MayaMesh(info);
      mesh.units = units;

      // parse attributes
      readToNextSemicolon(rtok);
      // parse bunch of attributes

      return null;
   }

   protected static boolean scanBoolean(ReaderTokenizer rtok)
      throws IOException {
      String bool = rtok.scanWordOrQuotedString('"').toLowerCase();
      if ("off".equals(bool) || "no".equals(bool) || "false".equals(bool)) {
         return false;
      } else if ("on".equals(bool) || "yes".equals(bool) || "true".equals(bool)) {
         return true;
      } else {
         throw new IOException("Unknown boolean value: " + bool);
      }
   }

   // read numbers, skipping over newlines
   protected static int scanNumbers(
      ReaderTokenizer rtok, double val[], int maxCount) throws IOException {

      int readCount = 0;
      while (true) {
         rtok.nextToken();
         if (rtok.ttype == ReaderTokenizer.TT_NUMBER) {
            val[readCount] = rtok.nval;
            readCount++;

            // if anything else other than number or EOL, then break
         } else if (rtok.ttype != ReaderTokenizer.TT_EOL) {
            break;
         }
         if (readCount == maxCount) {
            break;
         }
      }
      return readCount;
   }

   private static String getTokenString(ReaderTokenizer rtok) {
      switch (rtok.ttype) {
         case ReaderTokenizer.TT_NOTHING:
         case ReaderTokenizer.TT_EOF: {
            return "";
         }
         case ReaderTokenizer.TT_EOL: {
            return "\n";
         }
         case ReaderTokenizer.TT_WORD: {
            return rtok.sval;
         }
         case ReaderTokenizer.TT_NUMBER: {
            if (rtok.tokenIsHexInteger()) {
               return "0x" + Long.toHexString(rtok.lval);
            } else if (rtok.tokenIsInteger()) {
               return Long.toString(rtok.lval);
            } else {
               return Double.toString(rtok.nval);
            }
         }
         default: {
            if (rtok.isQuoteChar(rtok.ttype)) {
               char quote = (char)rtok.ttype;
               return quote + rtok.sval + quote;
            }
            return Character.toString((char)(rtok.ttype)); // other characters
         }
      }
   }

   protected static String readToNextSemicolon(ReaderTokenizer rtok)
      throws IOException {

      String line = "";

      rtok.nextToken();
      while (rtok.ttype != SEMICOLON) {
         if (rtok.ttype == ReaderTokenizer.TT_EOF) {
            break;
         }
         line = line + " " + getTokenString(rtok);
         rtok.nextToken();
      }
      line = line.trim();

      return line;
   }

   public static void doRead(String inputFile, String outputFile) {

      PrintStream out;
      ReaderTokenizer rtok;

      // output stream
      if (outputFile == null) {
         out = System.out;
      } else {
         try {
            out = new PrintStream(outputFile);
         } catch (IOException e) {
            e.printStackTrace();
            return;
         }
      }

      // input stream
      try {
         rtok = new ReaderTokenizer(new FileReader(inputFile));
      } catch (FileNotFoundException e) {
         e.printStackTrace();
         return;
      }

      MayaAsciiReader reader = new MayaAsciiReader();
      try {
         reader.read(rtok);
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
         rtok.close();
      }

      out.print(reader.tree.toString());

      if (out != System.out) {
         out.close();
      }

   }

   private static abstract class MayaNodeParser {
      public abstract String getAttributeName(String shortOrLongName);

      public abstract MayaNode createNode(
         MayaNodeInfo info, UnitInfo units,
         HashMap<String,MayaAttribute> attributes);

      public abstract void parseAttributeData(
         MayaAttribute attribute, int[] range, String[] subAttributes,
         ReaderTokenizer rtok) throws IOException;

      public MayaNode parseNode(
         MayaNodeInfo info, UnitInfo units, ReaderTokenizer rtok)
         throws IOException {

         int dashSetting = rtok.getCharSetting(DASH_CHAR);
         rtok.wordChar(DASH_CHAR);

         HashMap<String,MayaAttribute> attributes =
            new HashMap<String,MayaAttribute>();

         // parse attributes
         rtok.nextToken(); // advance to next token
         while (rtok.ttype == ReaderTokenizer.TT_WORD
            && !rtok.sval.equals("createNode")) {

            if (rtok.sval.equals("setAttr")) {
               try {
                  parseAttribute(rtok, attributes);
               } catch (IOException e) {
                  if (verbose) {
                     System.err.println("IOError: " + e.getMessage());
                  }
               }
            } else if (rtok.sval.equals("connectAttr")) {
               // connect two attributes
            } else {
               if (verbose) {
                  System.err.println("Unknown command \"" + rtok.sval
                     + "\" (Line " + rtok.lineno() + ")");
               }
            }

            if (rtok.ttype != SEMICOLON) {
               readToNextSemicolon(rtok); // advance to the next semicolon
            }
            rtok.nextToken();
         }

         rtok.pushBack(); // push last item back
         rtok.setCharSetting(DASH_CHAR, dashSetting);

         return createNode(info, units, attributes);
      }

      public void parseAttribute(
         ReaderTokenizer rtok, HashMap<String,MayaAttribute> attributes)
         throws IOException {

         MayaAttribute currentAttribute = null;
         String[] subAttributes = null;
         int[] range = null;

         // read to semicolon
         HashMap<String,Object> options = new HashMap<String,Object>();
         rtok.nextToken();
         while (rtok.ttype != SEMICOLON) {

            if (!parseOption(rtok, options)) {

               if (currentAttribute == null
                  && rtok.tokenIsWordOrQuotedString('"')
                  && rtok.sval.startsWith(PERIOD_STR)) {
                  // we have an attribute
                  String attrStr = rtok.sval.substring(1); // remove initial
                                                           // period

                  subAttributes = attrStr.split("\\."); // split at periods
                  if (subAttributes.length > 1) {
                     attrStr = subAttributes[0];
                  } else {
                     subAttributes = null;
                  }

                  // check for ranged
                  int rin = attrStr.lastIndexOf('[');
                  int rout = -1;
                  if (rin >= 0) {
                     range = new int[2];
                     rout = attrStr.indexOf(']', rin + 1);
                     if (rout >= rin) {
                        String rangeStr = attrStr.substring(rin + 1, rout);
                        int colonIdx = rangeStr.indexOf(':');
                        if (colonIdx >= 0) {
                           range[0] =
                              Integer.parseInt(rangeStr.substring(0, colonIdx));
                           range[1] =
                              Integer
                                 .parseInt(rangeStr.substring(colonIdx + 1));
                        } else {
                           range[0] = Integer.parseInt(rangeStr);
                           range[1] = range[0];
                        }
                     }
                     attrStr = attrStr.substring(0, rin);
                  } // done checking for range

                  String attr = getAttributeName(attrStr);
                  if (attr == null) {
                     throw new IOException("Unknown attribute '" + attrStr
                        + "' (Line " + rtok.lineno() + ")");
                  }
                  currentAttribute = attributes.get(attr);
                  if (currentAttribute == null) {
                     currentAttribute = new MayaAttribute();
                     currentAttribute.name = attr;
                     attributes.put(attr, currentAttribute);
                  } // done finding current attribute

                  // merge options
                  if (options.size() > 0) {
                     if (currentAttribute.options == null
                        || currentAttribute.options.size() == 0) {
                        currentAttribute.options = options;
                     } else {
                        options.putAll(currentAttribute.options);
                        currentAttribute.options = options;
                     }
                  }

               } // end grabbing attribute type
               else {
                  // read attribute data
                  rtok.pushBack();
                  parseAttributeData(
                     currentAttribute, range, subAttributes, rtok);
               }

            } // end parsing attribute
            if (rtok.ttype != SEMICOLON) {
               rtok.nextToken(); // move on to next token
            }
         } // end reading to semicolon
      }

      public boolean parseOption(
         ReaderTokenizer rtok, HashMap<String,Object> options)
         throws IOException {
         // check if it is an option
         if (rtok.tokenIsWordOrQuotedString('"') && rtok.sval.startsWith("-")) {
            // must be an option, scan option
            if ("-k".equals(rtok.sval) || "-keyable".equals(rtok.sval)) {
               boolean val = scanBoolean(rtok);
               options.put("keyable", val);
            } else if ("-l".equals(rtok.sval) || "-lock".equals(rtok.sval)) {
               boolean val = scanBoolean(rtok);
               options.put("lock", val);
            } else if ("-cb".equals(rtok.sval)
               || "-channelBox".equals(rtok.sval)) {
               boolean val = scanBoolean(rtok);
               options.put("channelBox", val);
            } else if ("-ca".equals(rtok.sval) || "-caching".equals(rtok.sval)) {
               boolean val = scanBoolean(rtok);
               options.put("caching", val);
            } else if ("-s".equals(rtok.sval) || "-size".equals(rtok.sval)) {
               int val = rtok.scanInteger();
               options.put("size", val);
            } else if ("-typ".equals(rtok.sval) || "-type".equals(rtok.sval)) {
               String val = rtok.scanWordOrQuotedString('"');
               options.put("type", val);
            } else if ("-av".equals(rtok.sval)
               || "-alteredValue".equals(rtok.sval)) {
               options.put("alteredValue", true);
            } else if ("-c".equals(rtok.sval) || "-clamp".equals(rtok.sval)) {
               options.put("clamp", true);
            } else if ("-ch".equals(rtok.sval)
               || "-capacityHint".equals(rtok.sval)) {
               int val = rtok.scanInteger();
               options.put("capacityHint", val);
            } else {
               throw new IOException("Unknown attribute option " + rtok.sval
                  + "(Line " + rtok.lineno() + ")");
            }
            return true;
         }
         return false;
      }

      protected static double[][] resize(double[][] array, int size) {

         if (array.length == size) {
            return array;
         }

         double[][] narray = new double[size][];
         int N = Math.min(size, array.length);

         for (int i = 0; i < N; i++) {
            narray[i] = array[i];
         }

         return narray;
      }

      protected static int[][] resize(int[][] array, int size) {

         if (array.length == size) {
            return array;
         }

         int[][] narray = new int[size][];
         int N = Math.min(size, array.length);

         for (int i = 0; i < N; i++) {
            narray[i] = array[i];
         }

         return narray;
      }
   }

   private static class TransformParser extends MayaNodeParser {

      private static HashMap<String,String> attributes =
         new HashMap<String,String>();
      static {
         attributes.put("v", "visibility");
         attributes.put("visibility", "visibility");
         attributes.put("t", "translate");
         attributes.put("translate", "translate");
         attributes.put("r", "rotate");
         attributes.put("rotate", "rotate");
         attributes.put("ro", "rotateOrder");
         attributes.put("rotateOrder", "rotateOrder");
         attributes.put("s", "scale");
         attributes.put("scale", "scale");
         attributes.put("sh", "shear");
         attributes.put("shear", "shear");
         attributes.put("rp", "rotatePivot");
         attributes.put("rotatePivot", "rotatePivot");
         attributes.put("sp", "scalePivot");
         attributes.put("scalePivot", "scalePivot");
         attributes.put("rpt", "rotatePivotTranslate");
         attributes.put("rotatePivotTranslate", "rotatePivotTranslate");
         attributes.put("spt", "scalePivotTranslate");
         attributes.put("scalePivotTranslate", "scalePivotTranslate");
         attributes.put("ra", "rotateAxis");
         attributes.put("rotateAxis", "rotateAxis");
         attributes.put("tmrp", "transMinusRotatePivot");
         attributes.put("transMinusRotatePivot", "transMinusRotatePivot");
         attributes.put("mntl", "minTransLimit");
         attributes.put("minTransLimit", "minTransLimit");
         attributes.put("mxtl", "maxTransLimit");
         attributes.put("maxTransLimit", "maxTransLimit");
         attributes.put("mtle", "minTransLimitEnable");
         attributes.put("minTransLimitEnable", "minTransLimitEnable");
         attributes.put("xtle", "maxTransLimitEnable");
         attributes.put("maxTransLimitEnable", "maxTransLimitEnable");
         attributes.put("mnrl", "minRotLimit");
         attributes.put("minRotLimit", "minRotLimit");
         attributes.put("mxrl", "maxRotLimit");
         attributes.put("maxRotLimit", "maxRotLimit");
         attributes.put("mrle", "minRotLimitEnable");
         attributes.put("minRotLimitEnable", "minRotLimitEnable");
         attributes.put("xrle", "maxRotLimitEnable");
         attributes.put("maxRotLimitEnable", "maxRotLimitEnable");
         attributes.put("mnsl", "minScaleLimit");
         attributes.put("minScaleLimit", "minScaleLimit");
         attributes.put("mxsl", "maxScaleLimit");
         attributes.put("maxScaleLimit", "maxScaleLimit");
         attributes.put("msle", "minScaleLimitEnable");
         attributes.put("minScaleLimitEnable", "minScaleLimitEnable");
         attributes.put("xsle", "maxScaleLimitEnable");
         attributes.put("maxScaleLimitEnable", "maxScaleLimitEnable");
         attributes.put("it", "inheritsTransform");
         attributes.put("inheritsTransform", "inheritsTransform");
      }

      @Override
      public String getAttributeName(String shortOrLongName) {
         return attributes.get(shortOrLongName);
      }

      @Override
      public void parseAttributeData(
         MayaAttribute attribute, int[] range, String[] subAttributes,
         ReaderTokenizer rtok) throws IOException {

         if ("visibility".equals(attribute.name)) {
            attribute.data = scanBoolean(rtok);
         } else if (
               "translate".equals(attribute.name) ||
               "rotate".equals(attribute.name) ||
               "scale".equals(attribute.name) ||
               "shear".equals(attribute.name) ||
               "rotatePivot".equals(attribute.name) ||
               "rotatePivotTranslate".equals(attribute.name) ||
               "scalePivot".equals(attribute.name) ||
               "scalePivotTranslate".equals(attribute.name) ||
               "rotateAxis".equals(attribute.name) ||
               "transMinusRotatePivot".equals(attribute.name) ||
               "minTransLimit".equals(attribute.name) ||
               "maxTransLimit".equals(attribute.name) ||
               "minRotLimit".equals(attribute.name) ||
               "maxRotLimit".equals(attribute.name) ||
               "minScaleLimit".equals(attribute.name) ||
               "maxScaleLimit".equals(attribute.name)
            ) {
            double[] v = new double[3];
            parse3double(rtok, v);
            attribute.data = v;
         } else if ("rotateOrder".equals(attribute.name)) {
            int ro = rtok.scanInteger();
            attribute.data = ro;        // xyz, yzx, zxy, xzy, yxz, zyx
         } else if ("inheritsTransform".equals(attribute.name)) {
            attribute.data = scanBoolean(rtok);
         } else if (
               "minTransLimitEnable".equals(attribute.name) ||
               "maxTransLimitEnable".equals(attribute.name) ||
               "minRotLimitEnable".equals(attribute.name) ||
               "maxRotLimitEnable".equals(attribute.name) ||  
               "minScaleLimitEnable".equals(attribute.name) ||
               "maxScaleLimitEnable".equals(attribute.name)) {
            boolean[] bv = new boolean[3];
            for (int i=0; i<3; i++) {
               bv[i] = scanBoolean(rtok);
            }
            attribute.data = bv;
         } else {
            // throw new IOException("Unhandled attribute with name '"
            // + attribute.name + "' (Line " + rtok.lineno() + ")");
            if (verbose) {
               System.err.println("Unhandled attribute with name '"
                  + attribute.name + "' (Line " + rtok.lineno() + ")");
            }
            attribute.data = readToNextSemicolon(rtok);
         }
      }

      @Override
      public MayaNode createNode(
         MayaNodeInfo info, UnitInfo units,
         HashMap<String,MayaAttribute> attributes) {

         MayaTransform transNode = new MayaTransform(info);

         // built upon creation

         transNode.attributes = attributes;
         transNode.units = units;
         return transNode;
      }

   }

   private static class NurbsCurveParser extends MayaNodeParser {

      private static HashMap<String,String> attributes =
         new HashMap<String,String>();
      static {
         attributes.put("v", "visible");
         attributes.put("visible", "visible");
         attributes.put("cc", "cached"); // curve geometry
         attributes.put("cached", "cached");
         attributes.put("tw", "tweak");
         attributes.put("tweak", "tweak");
         attributes.put("cp", "controlPoints");
         attributes.put("controlPoints", "controlPoints");
      }

      @Override
      public String getAttributeName(String shortOrLongName) {
         return attributes.get(shortOrLongName);
      }

      @Override
      public void parseAttributeData(
         MayaAttribute attribute, int[] range, String[] subAttributes,
         ReaderTokenizer rtok) throws IOException {

         if ("visibility".equals(attribute.name)) {
            attribute.data = scanBoolean(rtok);
         } else if ("cached".equals(attribute.name)) {
            MayaNurbsCurve.NurbsCurve curve = new MayaNurbsCurve.NurbsCurve();
            curve.degree = rtok.scanInteger();
            curve.spans = rtok.scanInteger();
            curve.form = rtok.scanInteger(); // open (0), closed(1), periodic(2)
            curve.rational = scanBoolean(rtok);
            curve.dim = rtok.scanInteger();
            int numKnots = rtok.scanInteger();
            curve.knots = new double[numKnots];
            int scanned = rtok.scanNumbers(curve.knots, numKnots);
            if (scanned != numKnots) {
               throw new IOException("Unable to read " + numKnots + " knots, ("
                  + scanned + ") (Line " + rtok.lineno() + ")");
            }

            int npnts = rtok.scanInteger();
            curve.cv = new double[npnts][];
            int len = curve.dim;
            if (curve.rational) {
               len += 1;
            }
            for (int i = 0; i < npnts; i++) {
               curve.cv[i] = new double[len];
               scanned = rtok.scanNumbers(curve.cv[i], len);
               if (scanned != len) {
                  throw new IOException("Unable to read point " + i + " (Line "
                     + rtok.lineno() + ")");
               }
            }

            attribute.data = curve;
         } else if ("tweak".equals(attribute.name)) {
            attribute.data = scanBoolean(rtok);
         } else if ("controlPoints".equals(attribute.name)) {
            double[][] cp = (double[][])(attribute.data);
            // resize if required
            int s = -1;
            if (attribute.options.get("size") != null) {
               s = (Integer)(attribute.options.get("size"));
            }
            if (range != null) {
               if (range[1] + 1 > s) {
                  s = range[1] + s;
               }
            }

            // create or adjust vrts array if required
            if (cp == null) {
               cp = new double[s][];
            } else if (cp.length < s) {
               // expand and copy over old values
               cp = resize(cp, s);
            }

            // read in values
            if (range == null) {
               int idx = 0;
               while (rtok.ttype != SEMICOLON) {
                  // keep reading in values
                  double[] nextpnt = new double[3];
                  int scanned = rtok.scanNumbers(nextpnt, 3);
                  if (scanned != 3) {
                     throw new IOException("Unable to scan numbers (Line "
                        + rtok.lineno() + ")");
                  }
                  if (idx + 1 > cp.length) {
                     cp = resize(cp, (int)((idx + 1) * 1.5));
                  }
                  cp[idx] = nextpnt;
                  idx++;
               }
               if (idx < cp.length) {
                  cp = resize(cp, idx);
               }

            } else {
               // fill in range
               for (int i = range[0]; i <= range[1]; i++) {
                  cp[i] = new double[3];
                  int scanned = rtok.scanNumbers(cp[i], 3);
                  if (scanned != 3) {
                     throw new IOException("Unable to scan numbers (Line "
                        + rtok.lineno() + ")");
                  }
               }
            }
            if (rtok.ttype != SEMICOLON) {
               readToNextSemicolon(rtok);
            }

            attribute.data = cp;
         }
      }

      @Override
      public MayaNode createNode(
         MayaNodeInfo info, UnitInfo units,
         HashMap<String,MayaAttribute> attributes) {

         MayaNurbsCurve curveNode = new MayaNurbsCurve(info);

         for (Entry<String,MayaAttribute> attrib : attributes.entrySet()) {
            String key = attrib.getKey();
            if ("cached".equals(key)) {

               curveNode.nurbs =
                  (MayaNurbsCurve.NurbsCurve)(attrib.getValue().data);

               // build from set of points
               double[][] pnts = curveNode.nurbs.cv;
               int nPoints = pnts.length;
               Vertex3d vtxs[] = new Vertex3d[nPoints];

               for (int i = 0; i < nPoints; i++) {
                  vtxs[i] =
                     new Vertex3d(new Point3d(
                        pnts[i][0], pnts[i][1], pnts[i][2]));
               }
               curveNode.curve.set(vtxs, nPoints);
            } else if ("controlPoints".equals(key)) {
               double[][] cp = (double[][])(attrib.getValue().data);

               // build from set of points
               int nPoints = cp.length;
               Vertex3d vtxs[] = new Vertex3d[nPoints];

               for (int i = 0; i < nPoints; i++) {
                  vtxs[i] =
                     new Vertex3d(new Point3d(cp[i][0], cp[i][1], cp[i][2]));
               }
               curveNode.curve.set(vtxs, nPoints);
            }
         }

         curveNode.attributes = attributes;
         curveNode.units = units;
         return curveNode;

      }

   }

   private static class MeshParser extends MayaNodeParser {

      private static HashMap<String,String> attributes =
         new HashMap<String,String>();
      static {
         attributes.put("v", "visible");
         attributes.put("visible", "visible");
         attributes.put("vir", "visibleInReflections");
         attributes.put("visibleInReflections", "visibleInReflections");
         attributes.put("vif", "visibleInRefractions");
         attributes.put("uvst", "uvSet");
         attributes.put("uvSet", "uvSet");
         attributes.put("cuvs", "currentUVSet");
         attributes.put("currentUVSet", "currentUVSet");
         attributes.put("dcc", "displayColorChannel");
         attributes.put("displayColorChannel", "displayColorChannel");
         attributes.put("sdt", "sdt");
         attributes.put("ugsdt", "ugsdt");
         attributes.put("cd", "creaseData");
         attributes.put("creaseData", "creaseData");
         attributes.put("cvd", "creaseVertexData");
         attributes.put("creaseVertexData", "creaseVertexData");
         attributes.put("hfd", "holeFaceData");
         attributes.put("creaseData", "creaseData");
         attributes.put("pt", "pnts");
         attributes.put("pnts", "pnts");
         attributes.put("vt", "vrts");
         attributes.put("vrts", "vrts");
         attributes.put("ed", "edge");
         attributes.put("edge", "edge");
         attributes.put("n", "normals");
         attributes.put("normals", "normals");
         attributes.put("fc", "face");
         attributes.put("face", "face");
         attributes.put("iog", "instObjGroups");
         attributes.put("instObjGroups", "instObjGroups");
      }

      @Override
      public String getAttributeName(String shortOrLongName) {
         return attributes.get(shortOrLongName);
      }

      @Override
      public void parseAttributeData(
         MayaAttribute attribute, int[] range, String[] subAttributes,
         ReaderTokenizer rtok) throws IOException {

         if ("visibility".equals(attribute.name)) {
            attribute.data = scanBoolean(rtok);
         } else if ("pnts".equals(attribute.name)) {
            double[][] pnts = (double[][])(attribute.data);
            // resize if required
            int s = -1;
            if (attribute.options.get("size") != null) {
               s = (Integer)(attribute.options.get("size"));
            }
            if (range != null) {
               if (range[1] + 1 > s) {
                  s = range[1] + 1;
               }
            }

            // create or adjust pnts array if required
            if (pnts == null) {
               pnts = new double[s][];
            } else if (pnts.length < s) {
               // expand and copy over old values
               pnts = resize(pnts, s);
            }

            // read in values
            if (range == null) {
               int idx = 0;
               while (rtok.ttype != SEMICOLON) {
                  // keep reading in values
                  double[] nextpnt = new double[3];
                  int scanned = rtok.scanNumbers(nextpnt, 3);
                  if (scanned != 3) {
                     throw new IOException("Unable to scan numbers (Line "
                        + rtok.lineno() + ")");
                  }
                  if (idx + 1 > pnts.length) {
                     pnts = resize(pnts, (int)((idx + 1) * 1.5));
                  }
                  pnts[idx] = nextpnt;

                  idx++;
               }
               if (idx < pnts.length) {
                  pnts = resize(pnts, idx);
               }

            } else {
               // fill in range
               for (int i = range[0]; i <= range[1]; i++) {
                  pnts[i] = new double[3];
                  int scanned = rtok.scanNumbers(pnts[i], 3);
                  if (scanned != 3) {
                     throw new IOException("Unable to scan numbers (Line "
                        + rtok.lineno() + ")");
                  }
               }
            }
            if (rtok.ttype != SEMICOLON) {
               readToNextSemicolon(rtok);
            }

            attribute.data = pnts;
         } else if ("vrts".equals(attribute.name)) {
            double[][] vrts = (double[][])(attribute.data);
            // resize if required
            int s = -1;
            if (attribute.options.get("size") != null) {
               s = (Integer)(attribute.options.get("size"));
            }
            if (range != null) {
               if (range[1] + 1 > s) {
                  s = range[1] + s;
               }
            }

            // create or adjust vrts array if required
            if (vrts == null) {
               vrts = new double[s][];
            } else if (vrts.length < s) {
               // expand and copy over old values
               vrts = resize(vrts, s);
            }

            // read in values
            if (range == null) {
               int idx = 0;
               while (rtok.ttype != SEMICOLON) {
                  // keep reading in values
                  double[] nextpnt = new double[3];
                  int scanned = rtok.scanNumbers(nextpnt, 3);
                  if (scanned != 3) {
                     throw new IOException("Unable to scan numbers (Line "
                        + rtok.lineno() + ")");
                  }
                  if (idx + 1 > vrts.length) {
                     vrts = resize(vrts, (int)((idx + 1) * 1.5));
                  }
                  vrts[idx] = nextpnt;
                  idx++;
               }
               if (idx < vrts.length) {
                  vrts = resize(vrts, idx);
               }

            } else {
               // fill in range
               for (int i = range[0]; i <= range[1]; i++) {
                  vrts[i] = new double[3];
                  int scanned = rtok.scanNumbers(vrts[i], 3);
                  if (scanned != 3) {
                     throw new IOException("Unable to scan numbers (Line "
                        + rtok.lineno() + ")");
                  }
               }
            }
            if (rtok.ttype != SEMICOLON) {
               readToNextSemicolon(rtok);
            }

            attribute.data = vrts;
         } else if ("edge".equals(attribute.name)) {
            int[][] ed = (int[][])(attribute.data);
            // resize if required
            int s = -1;
            if (attribute.options.get("size") != null) {
               s = (Integer)(attribute.options.get("size"));
            }
            if (range != null) {
               if (range[1] + 1 > s) {
                  s = range[1] + s;
               }
            }

            // create or adjust pnts array if required
            if (ed == null) {
               ed = new int[s][];
            } else if (ed.length < s) {
               // expand and copy over old values
               ed = resize(ed, s);
            }

            // read in values
            if (range == null) {
               int idx = 0;
               while (rtok.ttype != SEMICOLON) {
                  // keep reading in values
                  int[] nextpnt = new int[3];
                  int scanned = rtok.scanIntegers(nextpnt, 3);
                  if (scanned != 3) {
                     throw new IOException("Unable to scan integers (Line "
                        + rtok.lineno() + ")");
                  }
                  if (idx + 1 > ed.length) {
                     ed = resize(ed, (int)((idx + 1) * 1.5));
                  }
                  ed[idx] = nextpnt;

                  idx++;
               }
               if (idx < ed.length) {
                  ed = resize(ed, idx);
               }

            } else {
               // fill in range
               for (int i = range[0]; i <= range[1]; i++) {
                  ed[i] = new int[3];
                  int scanned = rtok.scanIntegers(ed[i], 3);
                  if (scanned != 3) {
                     throw new IOException("Unable to scan numbers (Line "
                        + rtok.lineno() + ")");
                  }
               }
            }
            if (rtok.ttype != SEMICOLON) {
               readToNextSemicolon(rtok);
            }

            attribute.data = ed;
         } else if ("face".equals(attribute.name)) {
            int[][] fc = (int[][])(attribute.data);
            // resize if required
            int s = -1;
            if (attribute.options.get("size") != null) {
               s = (Integer)(attribute.options.get("size"));
            }
            if (range != null) {
               if (range[1] + 1 > s) {
                  s = range[1] + s;
               }
            }

            // create or adjust pnts array if required
            if (fc == null) {
               fc = new int[s][];
            } else if (fc.length < s) {
               // expand and copy over old values
               fc = resize(fc, s);
            }

            // read in values
            if (range == null) {
               int idx = 0;
               while (rtok.ttype != SEMICOLON) {

                  String f = rtok.scanWord();
                  if (!"f".equals(f)) {
                     throw new IOException("Unknown face type '" + f
                        + "' (Line " + rtok.lineno() + ")");
                  }
                  int flen = rtok.scanInteger();

                  // keep reading in values
                  int[] nextpnt = new int[flen];
                  int scanned = rtok.scanIntegers(nextpnt, flen);
                  if (scanned != flen) {
                     throw new IOException("Unable to scan integers (Line "
                        + rtok.lineno() + ")");
                  }
                  if (idx + 1 > fc.length) {
                     fc = resize(fc, (int)((idx + 1) * 1.5));
                  }
                  fc[idx] = nextpnt;

                  idx++;
               }
               if (idx < fc.length) {
                  fc = resize(fc, idx);
               }

            } else {
               // fill in range
               int fidx = range[0];
               while (fidx <= range[1]) {
                  String f = rtok.scanWord();
                  if ("f".equals(f)) {
                     int flen = rtok.scanInteger();
                     fc[fidx] = new int[flen];
                     int scanned = rtok.scanIntegers(fc[fidx], flen);
                     if (scanned != flen) {
                        throw new IOException("Unable to scan numbers (Line "
                           + rtok.lineno() + ")");
                     }
                     ++fidx;
                  } else if ("mu".equals(f)) {
                     // uv indices
                     int uvSet = rtok.scanInteger();
                     int uvCount = rtok.scanInteger();
                     int uvIdx[] = new int[uvCount];
                     int scanned = rtok.scanIntegers(uvIdx, uvCount);
                     if (scanned != uvCount) {
                        throw new IOException("Unable to scan numbers (Line "
                           + rtok.lineno() + ")");
                     }
                  } else if ("h".equals(f)) {
                     // holes
                     int edgeCount = rtok.scanInteger();
                     int edges[] = new int[edgeCount];
                     int scanned = rtok.scanIntegers(edges, edgeCount);
                     if (scanned != edgeCount) {
                        throw new IOException("Unable to scan numbers (Line "
                           + rtok.lineno() + ")");
                     }
                  } else if ("fc".equals(f)) {
                     // face color
                     int cCount = rtok.scanInteger();
                     int colors[] = new int[cCount];
                     int scanned = rtok.scanIntegers(colors, cCount);
                     if (scanned != cCount) {
                        throw new IOException("Unable to scan numbers (Line "
                           + rtok.lineno() + ")");
                     }
                  } else {
                     throw new IOException("Unknown face type '" + f
                        + "' (Line " + rtok.lineno() + ")");
                  }
                  
               }
            }
            if (rtok.ttype != SEMICOLON) {
               readToNextSemicolon(rtok);
            }

            attribute.data = fc;
         } else if ("normals".equals(attribute.name)) {
            // ignore
            attribute.data = readToNextSemicolon(rtok);
         } else if ("visibleInReflections".equals(attribute.name)) {
            attribute.data = scanBoolean(rtok);
         } else if ("visibleInRefractions".equals(attribute.name)) {
            attribute.data = scanBoolean(rtok);
         } else if ("uvSet".equals(attribute.name)) {
            // ignore
            // XXX should parse sub-properties
            readToNextSemicolon(rtok);
         } else if ("currentUVSet".equals(attribute.name)) {
            attribute.data = readToNextSemicolon(rtok);
         } else if ("displayColorChannel".equals(attribute.name)) {
            attribute.data = readToNextSemicolon(rtok);
         } else if ("sdt".equals(attribute.name)) {
            attribute.data = readToNextSemicolon(rtok);
         } else if ("ugsdt".equals(attribute.name)) {
            attribute.data = scanBoolean(rtok);
         } else if ("creaseData".equals(attribute.name)) {
            attribute.data = readToNextSemicolon(rtok);
         } else if ("creaseVertexData".equals(attribute.name)) {
            attribute.data = readToNextSemicolon(rtok);
         } else if ("holeFaceData".equals(attribute.name)) {
            attribute.data = readToNextSemicolon(rtok);
         } else {
            if (verbose) {
               System.err.println("Unhandled attribute with name '"
                  + attribute.name + "' (Line " + rtok.lineno() + ")");
            }
            attribute.data = readToNextSemicolon(rtok);
         }
      }

      @Override
      public MayaNode createNode(
         MayaNodeInfo info, UnitInfo units,
         HashMap<String,MayaAttribute> attributes) {

         MayaMesh meshNode = new MayaMesh(info);
         meshNode.attributes = attributes;
         meshNode.units = units;
         return meshNode;

      }

   }

   private static class IgnoreParser extends MayaNodeParser {

      @Override
      public String getAttributeName(String shortOrLongName) {
         return shortOrLongName;
      }

      @Override
      public void parseAttributeData(
         MayaAttribute attribute, int[] range, String[] subAttributes,
         ReaderTokenizer rtok) throws IOException {

         String line = readToNextSemicolon(rtok);
         attribute.data = line;
      }

      @Override
      public MayaNode createNode(
         MayaNodeInfo info, UnitInfo units,
         HashMap<String,MayaAttribute> attributes) {

         MayaNode node = new MayaNode(info);
         node.attributes = attributes;
         node.units = units;
         return node;

      }

   }

   public static class MayaTransformBuilder {

      RigidTransform3d SP = null;
      AffineTransform3d S = null;
      AffineTransform3d SH = null;
      RigidTransform3d ST = null;
      RigidTransform3d RP = null;
      RigidTransform3d RA = null;
      RigidTransform3d R = null;
      RigidTransform3d RT = null;
      RigidTransform3d T = null;
      RotationOrder rotOrder = RotationOrder.XYZ; // default

      public MayaTransformBuilder() {}

      public AffineTransform3dBase getTransform() {

         RigidTransform3d rigidComponent = getRigidTrans();

         if (S == null && SH == null) {
            return rigidComponent;
         }

         AffineTransform3d trans = new AffineTransform3d();

         trans.set(rigidComponent);
         if (SP != null) {
            trans.mul(SP);
         }
         if (SH != null) {
            trans.mul(SH);
         }
         if (S != null) {
            trans.mul(S);
         }
         if (SP != null) {
            trans.mulInverse(SP);
         }
         return trans;

      }

      private RigidTransform3d getRigidTrans() {
         RigidTransform3d trans = new RigidTransform3d();
         trans.setIdentity();

         if (T != null) {
            trans.mul(T);
         }
         if (RT != null) {
            trans.mul(RT);
         }
         if (RP != null) {
            trans.mul(RP);
         }
         if (R != null) {
            trans.mul(R);
         }
         if (RA != null) {
            trans.mul(RA);
         }
         if (RP != null) {
            trans.mulInverse(RP);
         }
         if (ST != null) {
            trans.mul(ST);
         }

         return trans;
      }

      public void setT(Vector3d t) {
         if (t != null) {
            T = new RigidTransform3d(t.x, t.y, t.z);
         } else {
            T = null;
         }
      }

      public void setRT(Vector3d rt) {
         if (rt != null) {
            RT = new RigidTransform3d(rt.x, rt.y, rt.z);
         } else {
            RT = null;
         }
      }

      public void setRP(Vector3d rp) {
         if (rp != null) {
            RP = new RigidTransform3d(rp.x, rp.y, rp.z);
         } else {
            RP = null;
         }
      }

      public enum RotationOrder {
         XYZ, YZX, ZXY, XZY, YXZ, ZYX
      }

      public void setR(Vector3d r, RotationOrder order) {

         if (r == null) {
            R = null;
            return;
         }

         // multiply rotation matrices according to order
         RotationMatrix3d Rx = new RotationMatrix3d();
         RotationMatrix3d Ry = new RotationMatrix3d();
         RotationMatrix3d Rz = new RotationMatrix3d();
         Rx.setRotX(r.x);
         Ry.setRotY(r.y);
         Rz.setRotZ(r.z);

         RotationMatrix3d Rmat = new RotationMatrix3d();
         switch (order) {
            case XYZ:
               Rmat.set(Rz);
               Rmat.mul(Ry);
               Rmat.mul(Rx);
               break;
            case XZY:
               Rmat.set(Ry);
               Rmat.mul(Rz);
               Rmat.mul(Rx);
               break;
            case YXZ:
               Rmat.set(Rz);
               Rmat.mul(Rx);
               Rmat.mul(Ry);
               break;
            case YZX:
               Rmat.set(Rx);
               Rmat.mul(Rz);
               Rmat.mul(Ry);
               break;
            case ZXY:
               Rmat.set(Ry);
               Rmat.mul(Rx);
               Rmat.mul(Rz);
               break;
            case ZYX:
               Rmat.set(Rx);
               Rmat.mul(Ry);
               Rmat.mul(Rz);
               break;
         }

         R = new RigidTransform3d(Vector3d.ZERO, Rmat);

      }

      public void setRA(Vector3d ra) {

         if (ra == null) {
            RA = null;
            return;
         }
         // multiply rotation matrices according to order
         RotationMatrix3d R = new RotationMatrix3d();
         R.setRotZ(ra.z);
         R.mulRotY(ra.y);
         R.mulRotX(ra.x);

         RA = new RigidTransform3d(Vector3d.ZERO, R);
      }

      public void setST(Vector3d st) {
         if (st == null) {
            ST = null;
         } else {
            ST = new RigidTransform3d(st.x, st.y, st.z);
         }
      }

      public void setSP(Vector3d sp) {
         if (sp == null) {
            SP = null;
         } else {
            SP = new RigidTransform3d(sp.x, sp.y, sp.z);
         }
      }

      public void setSH(Vector3d sh) {
         if (sh == null) {
            SH = null;
         } else {
            SH = new AffineTransform3d();
            SH.A.set(1, sh.x, sh.y, 0, 1, sh.z, 0, 0, 1);
         }
      }

      public void setS(Vector3d s) {
         if (s == null) {
            S = null;
         } else {
            S = new AffineTransform3d();
            S.A.set(s.x, 0, 0, 0, s.y, 0, 0, 0, s.z);
         }
      }

      public void setIdentity() {
         SP = null;
         S = null;
         SH = null;
         ST = null;
         RP = null;
         RA = null;
         R = null;
         RT = null;
         T = null;
      }

      public void clear() {
         setIdentity();
      }

   }

   // public static void main(String[] args) {
   //
   // String outfile = null;
   // if (args.length < 1) {
   // System.out.println("arguments: input_file [output_file]");
   // return;
   // }
   // if (args.length > 1) {
   // outfile = args[1];
   // }
   //
   // doRead(args[0], outfile);
   //
   // }
}
