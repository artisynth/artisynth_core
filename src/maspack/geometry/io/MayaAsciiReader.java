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
import java.util.List;

import maspack.geometry.Polyline;
import maspack.geometry.PolylineMesh;
import maspack.geometry.Vertex3d;
import maspack.graph.Node;
import maspack.graph.Tree;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.Point3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;

// too complex to be associated with a simple MeshReader interface
public class MayaAsciiReader {

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

      private LengthUnit (double si, String... str) {
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
      DEGREE (Math.PI / 180, "degree", "deg"),
      RADIAN (1, "radian", "rad");

      String[] myStrs;
      double mySI;

      private AngleUnit (double si, String... str) {
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
      HOUR (3600, "hour"),
      MINUTE (60, "min"),
      SECOND (1, "sec"),
      MILLISECOND (1e-3, "millisec"),
      GAME (1.0 / 15, "game"),
      FILM (1.0 / 24, "film"),
      PAL (1.0 / 25, "pal"),
      NTSC (1.0 / 30, "ntsc"),
      SHOW (1.0 / 48, "show"),
      PALF (1.0 / 50, "palf"),
      NTSCF (1.0 / 60, "ntscf");

      String[] myStrs;
      double mySI;

      private TimeUnit (double si, String... str) {
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

      public UnitInfo (LengthUnit lu, AngleUnit au, TimeUnit tu) {
         length = lu;
         angle = au;
         time = tu;
      }
   }

   public static class UnitUtility {
      public static double convertLength(double src, LengthUnit srcUnit,
         LengthUnit destUnit) {
         return src * srcUnit.getSI() / destUnit.getSI();
      }

      public static double convertAngle(double src, AngleUnit srcUnit,
         AngleUnit destUnit) {
         return src * srcUnit.getSI() / destUnit.getSI();
      }

      public static double convertTime(double src, TimeUnit srcUnit,
         TimeUnit destUnit) {
         return src * srcUnit.getSI() / destUnit.getSI();
      }
   }

   public static class MayaNode {
      public String name = null;
      public String parent = null;
      public String options = "";
      public UnitInfo units = null;
      public ArrayList<String> attributes = new ArrayList<String>();

      public void addAttribute(String attr) {
         attributes.add(attr);
      }

      public String toString() {
         return name;
      }
   }

   public static class MayaTransform extends MayaNode {
      public AffineTransform3d transform = new AffineTransform3d();
   }

   public static class MayaNurbsCurve extends MayaNode {
      public Polyline curve = new Polyline(-1);
   }

   Tree<MayaNode> tree = new Tree<MayaNode>();

   public MayaAsciiReader () {
   }

   public MayaAsciiReader (File file) throws IOException {
      BufferedReader reader = new BufferedReader(new FileReader(file));
      read(reader);
   }

   public void read(Reader reader) throws IOException {
      read(new ReaderTokenizer(reader));
   }

   public void read(ReaderTokenizer rtok) throws IOException {

      rtok.eolIsSignificant(true);
      ArrayList<MayaNode> nodes = new ArrayList<MayaNode>();
      UnitInfo currentUnits =
         new UnitInfo(LengthUnit.CENTIMETER, AngleUnit.DEGREE, TimeUnit.FILM);

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

   private void parseUnits(ReaderTokenizer rtok, UnitInfo info)
      throws IOException {
      int dashSetting = rtok.getCharSetting('-');
      rtok.wordChar('-');
      boolean eolSetting = rtok.getEolIsSignificant();
      rtok.eolIsSignificant(true);

      while (rtok.nextToken() != ReaderTokenizer.TT_EOL) {
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equals("-l")) {
               // length
               String id = rtok.scanWordOrQuotedString('"');
               // remove space/semi-colon
               id = id.replace(";", "").trim();
               info.length = LengthUnit.detect(id);
            } else if (rtok.sval.equals("-a")) {
               String id = rtok.scanWordOrQuotedString('"');
               // remove space/semi-colon
               id = id.replace(";", "").trim();
               info.angle = AngleUnit.detect(id);
            } else if (rtok.sval.equals("-t")) {
               // time
               String id = rtok.scanWordOrQuotedString('"');
               // remove space/semi-colon
               id = id.replace(";", "").trim();
               info.time = TimeUnit.detect(id);
            }
         }
      }
      rtok.setCharSetting('-', dashSetting);
      rtok.eolIsSignificant(eolSetting);
   }

   public String[] getGroupNames() {
      ArrayList<String> groupNames = new ArrayList<String>();
      addChildrenNames(tree.getRootElement(), groupNames,"", "");
      return groupNames.toArray(new String[groupNames.size()]);
   }
   
   public String[] getGroupHierarchy(String levelIndicator) {
      ArrayList<String> groupNames = new ArrayList<String>();
      addChildrenNames(tree.getRootElement(), groupNames,"", levelIndicator);
      return groupNames.toArray(new String[groupNames.size()]);
   }
   

   public PolylineMesh getPolylineMesh() {
      return getPolylineMesh(tree.getRootElement(), 
         tree.getRootElement().getData().units);
   }
   
   public PolylineMesh getPolylineMesh(UnitInfo units) {
      return getPolylineMesh(tree.getRootElement(), units);
   }

   public PolylineMesh getPolylineMesh(String group) {
      return getPolylineMesh(group, null);
   }
   
   public PolylineMesh getPolylineMesh(String group, UnitInfo units) {

      Node<MayaNode> root = tree.getRootElement();
      if (group != null) {
         root = getNode(group);
      }
      if (root == null) {
         throw new IllegalArgumentException("Group \"" + group
            + "\" not found.");
      }
      
      if (units == null) {
         units = root.getData().units;
      }
      return getPolylineMesh(root, units);
   }

   private PolylineMesh getPolylineMesh(Node<MayaNode> root, UnitInfo units) {

      PolylineMesh mesh = new PolylineMesh();
      AffineTransform3d trans = new AffineTransform3d();
      recursiveBuildParentTransform(root, trans, units);
      recursiveAddPolylines(root, mesh, trans, units);

      return mesh;

   }

   private void recursiveBuildParentTransform(Node<MayaNode> leaf,
      AffineTransform3d trans, UnitInfo units) {

      if (leaf.getNumberOfParents() > 0) {
         Node<MayaNode> parent = leaf.getParent(0);
         MayaNode data = parent.getData();
         if (data instanceof MayaTransform) {
            MayaTransform dtrans = (MayaTransform)data;
            AffineTransform3d tu = new AffineTransform3d(dtrans.transform);
            // convert units
            trans.p.scale(dtrans.units.length.getSI() / units.length.getSI());
            trans.mul(tu, trans);
         }

         recursiveBuildParentTransform(parent, trans, units);
      }
   }

   private void recursiveAddPolylines(Node<MayaNode> root, PolylineMesh mesh,
      AffineTransform3d trans, UnitInfo units) {

      trans = new AffineTransform3d(trans); // make copy so can traverse
                                            // children independently

      MayaNode data = root.getData();
      if (data instanceof MayaTransform) {
         MayaTransform dtrans = (MayaTransform)data;
         AffineTransform3d tu = new AffineTransform3d(dtrans.transform);
         // convert units
         trans.p.scale(dtrans.units.length.getSI() / units.length.getSI()); 
         trans.mul(tu, trans);
      } else if (data instanceof MayaNurbsCurve) {
         MayaNurbsCurve mnc = (MayaNurbsCurve)data;
         Polyline line = mnc.curve;

         // transform line
         for (Vertex3d vtx : line.getVertices()) {
            vtx.pnt.scale(mnc.units.length.getSI()/units.length.getSI());
            vtx.pnt.transform(trans);
         }

         if (line != null) {
            mesh.addLine(line);
         }
      }

      for (Node<MayaNode> child : root.getChildren()) {
         recursiveAddPolylines(child, mesh, trans, units);
      }

   }

   public Tree<MayaNode> getTree() {
      return tree;
   }

   private Node<MayaNode> getNode(String nodeName) {
      if (nodeName == null) {
         return tree.getRootElement();
      }

      int idx = nodeName.indexOf(':');
      if (idx < 0) {
         return recursiveGetNode(tree.getRootElement(), nodeName);
      }

      return getNode(tree.getRootElement(), nodeName);
   }

   public Node<MayaNode> getNode(Node<MayaNode> root, String nodeName) {

      int idx = nodeName.indexOf(':');
      String childName = nodeName;
      String rest = null;
      if (idx >= 0) {
         childName = nodeName.substring(0, idx);
         if (idx < nodeName.length() - 1) {
            rest = nodeName.substring(idx + 1);
         }
      }

      for (Node<MayaNode> child : root.getChildren()) {
         MayaNode data = child.getData();
         if (data != null) {
            if (data.name.equals(childName)) {
               if (rest == null) {
                  return child;
               } else {
                  return getNode(child, rest);
               }
            }
         }
      }

      return null;

   }

   private Node<MayaNode>
      recursiveGetNode(Node<MayaNode> root, String nodeName) {

      for (Node<MayaNode> child : root.getChildren()) {
         if (nodeName.equals(child.getData().name)) {
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

   private void
      addChildrenNames(Node<MayaNode> root, ArrayList<String> nameList, String prefix, String levelAppend) {
      for (Node<MayaNode> node : root.getChildren()) {
         MayaNode data = node.getData();
         if (data instanceof MayaTransform) {
            nameList.add(prefix + node.getData().name);
         }
         addChildrenNames (node, nameList, prefix + levelAppend, levelAppend);
      }
   }

   private static class TreeDepthComparator implements Comparator<MayaNode> {

      @Override
      public int compare(MayaNode o1, MayaNode o2) {

         int pCount0 = 0;
         int pCount1 = 0;

         String p0 = o1.parent;
         String p1 = o2.parent;
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

   private static void
      buildTree(Tree<MayaNode> tree, ArrayList<MayaNode> nodes) {

      Node<MayaNode> root = new Node<MayaNode>();
      tree.setRootElement(root);
      // ArrayList<Node<MayaNode>> treeNodes = new ArrayList<Node<MayaNode>>();

      ArrayDeque<Node<MayaNode>> orphan = new ArrayDeque<Node<MayaNode>>();
      ArrayList<Node<MayaNode>> treeNodes = new ArrayList<Node<MayaNode>>();

      // build tree nodes
      // first sort by depth if possible
      Collections.sort(nodes, new TreeDepthComparator());
      for (MayaNode node : nodes) {
         orphan.add(new Node<MayaNode>(node));
      }

      // build tree
      int nSkipped = 0; // for detecting case where can't place node

      // loop through all "orphans", trying to attach to tree
      while (orphan.size() > 0) {

         boolean mod = false; // whether tree was modified
         Node<MayaNode> node = orphan.poll(); // grab the first entry

         // attach parent
         String parent = node.getData().parent;
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
               for (Node<MayaNode> tnode : treeNodes) {
                  if (tnode.getData().name.equals(hnames[0])) {
                     pnode = tnode;
                     break;
                  }
               }
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
                  if (child.getData().name.equals(hnames[i])) {
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

   private static MayaNode parseNode(ReaderTokenizer rtok, UnitInfo units)
      throws IOException {

      String nodeType = rtok.scanWord();
      if (nodeType.equals("transform")) {
         return parseTransform(rtok, units);
      } else if (nodeType.equals("nurbsCurve")) {
         return parseNurbsCurve(rtok, units);
      } else if (nodeType.equals("camera")
         || nodeType.equals("lightLinker")
         || nodeType.equals("displayLayerManager")
         || nodeType.equals("displayLayer")
         || nodeType.equals("renderLayerManager")
         || nodeType.equals("renderLayer")) {
         // ignore
      } else {
         System.err.println("Unknown node type: " + rtok.sval +
            ", Line " + rtok.lineno() + ", ignoring...");
      }

      return null;
   }

   private static MayaNode parseTransform(ReaderTokenizer rtok, UnitInfo units)
      throws IOException {

      // get options and such
      MayaTransform trans = new MayaTransform();
      trans.units = units;
      parseOptions(rtok, trans);

      // parse attributes
      toEOL(rtok);
      rtok.nextToken(); // advance to next token
      while (rtok.ttype == ReaderTokenizer.TT_WORD &&
         rtok.sval.equals("setAttr")) {

         String attr = rtok.scanWordOrQuotedString('"');
         if (".t".equals(attr)) {
            // get translation
            double[] t = new double[3];
            parse3double(rtok, t);
            trans.transform.setTranslation(new Vector3d(t));
         } else if (".r".equals(attr)) {
            // get rotation
            RotationMatrix3d rpy = new RotationMatrix3d();
            double[] v = new double[3];
            parse3double(rtok, v);

            if (units.angle == AngleUnit.DEGREE) {
               rpy.setEuler(
                  Math.toRadians(v[0]), Math.toRadians(v[1]),
                  Math.toRadians(v[2]));
            } else {
               rpy.setEuler(v[0], v[1], v[2]);
            }
            trans.transform.setRotation(rpy);
         } else {
            String line = readLine(rtok);
            if (!attr.startsWith("-")) {
               attr = "\"" + attr + "\"";
            }
            trans.addAttribute(attr + " " + line);
         }
         toEOL(rtok);
         rtok.nextToken();
      }

      rtok.pushBack(); // push last item back, in case it is a createNode
      return trans;
   }

   private static void parse3double(ReaderTokenizer rtok, double[] v)
      throws IOException {
      // "-type" "double3"
      int saveDash = rtok.getCharSetting('-');
      rtok.wordChar('-');
      rtok.scanWordOrQuotedString('"'); // "-type"
      rtok.scanWordOrQuotedString('"'); // "double3"
      rtok.setCharSetting('-', saveDash);
      int read = scanNumbers(rtok, v, 3);
      if (read != 3) {
         throw new IOException("Cannot read 3 doubles on line " + rtok.lineno());
      }
   }

   private static void parseOptions(ReaderTokenizer rtok, MayaNode node)
      throws IOException {

      int saveDash = rtok.getCharSetting('-');
      rtok.wordChar('-');

      rtok.nextToken();
      while (rtok.ttype != ReaderTokenizer.TT_EOL) {
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equals("-n")) {
               node.name = rtok.scanQuotedString('"');
            } else if (rtok.sval.equals("-p")) {
               node.parent = rtok.scanQuotedString('"');
            } else if (rtok.sval.equals(";")) {
               // ignore
            } else {
               node.options += rtok.sval + " ";
            }
         }
         rtok.nextToken();
      }

      rtok.setCharSetting('-', saveDash);
   }

   private static MayaNode
      parseNurbsCurve(ReaderTokenizer rtok, UnitInfo units) throws IOException {

      MayaNurbsCurve curve = new MayaNurbsCurve();
      curve.units = units;
      parseOptions(rtok, curve);

      // parse attributes
      toEOL(rtok);
      rtok.nextToken(); // advance to next token
      while (rtok.ttype == ReaderTokenizer.TT_WORD &&
         rtok.sval.equals("setAttr")) {
         String line = readLine(rtok);
         curve.addAttribute(line);
         toEOL(rtok);
         rtok.nextToken();
      }
      
      int lastType = rtok.ttype;
      String lastString = rtok.sval;
      rtok.pushBack(); // push last item back
      
      // check for empty node
      if (lastType == ReaderTokenizer.TT_WORD &&
         lastString.equals ("createNode") ) {
         return null;
      } else if (lastType == ReaderTokenizer.TT_WORD){
         System.err.println ("Warning: not sure how to handle this inside a nurbs curve: "  + lastString);
         return null;
      }

      // throw away next line (form: 1 17 0 no 3)
      toEOL(rtok);

      // read curve
      // read indexing
      int nSkip = (int)(rtok.scanNumber());
      int skipped = 0;
      double vals[] = new double[nSkip];

      // throw away numbers
      skipped = scanNumbers(rtok, vals, nSkip);
      if (skipped != nSkip) {
         throw new IOException("Problem parsing numbers on line "
            + rtok.lineno());
      }

      // read entire curve
      int nPoints = (int)(scanNumber(rtok));
      double pnt[] = new double[3];
      Vertex3d vtxs[] = new Vertex3d[nPoints];

      for (int i = 0; i < nPoints; i++) {

         int nRead = scanNumbers(rtok, pnt, 3);
         if (nRead < 3) {
            throw new IOException("Error: cannot read coordinate on line "
               + rtok.lineno());
         }
         vtxs[i] = new Vertex3d(new Point3d(pnt));
      }
      curve.curve.set(vtxs, nPoints);

      return curve;
   }

   // read numbers, skipping over newlines
   public static double scanNumber(ReaderTokenizer rtok) throws IOException {

      while (rtok.nextToken() == ReaderTokenizer.TT_EOL) {
      }
      if (rtok.ttype != ReaderTokenizer.TT_NUMBER) {
         throw new IOException("expected a number, got " + rtok.tokenName()
            + ", line " + rtok.lineno());
      }

      return rtok.nval;
   }

   // read numbers, skipping over newlines
   public static int scanNumbers(ReaderTokenizer rtok, double val[],
      int maxCount) throws IOException {

      int readCount = 0;
      while (true) {
         rtok.nextToken();
         if (rtok.ttype == ReaderTokenizer.TT_NUMBER) {
            val[readCount] = rtok.nval;
            readCount++;

            // if anything else other than number or EOL, then break
         }
         else if (rtok.ttype != ReaderTokenizer.TT_EOL) {
            break;
         }
         if (readCount == maxCount) {
            break;
         }
      }
      return readCount;
   }

   protected static int nextToken(ReaderTokenizer rtok) throws IOException {
      rtok.nextToken();
      return rtok.ttype;
   }

   private static void toEOL(ReaderTokenizer rtok) throws IOException {
      while ((rtok.ttype != ReaderTokenizer.TT_EOL)
         && (rtok.ttype != ReaderTokenizer.TT_EOF)) {
         nextToken(rtok);
      }
   }

   protected static String readLine(ReaderTokenizer rtok) throws IOException {

      Reader rtokReader = rtok.getReader();
      String line = "";
      int c;
      while (true) {
         c = rtokReader.read();

         if (c < 0) {
            rtok.ttype = ReaderTokenizer.TT_EOF;
            return line;
         }
         else if (c == '\n') {
            rtok.setLineno(rtok.lineno() + 1); // increase line number
            rtok.ttype = ReaderTokenizer.TT_EOL;
            break;
         }
         line += (char)c;
      }

      return line;
   }

   public static void doRead(String inputFile, String outputFile) {

      PrintStream out;
      ReaderTokenizer rtok;

      // output stream
      if (outputFile == null) {
         out = System.out;
      }
      else {
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

   public static void main(String[] args) {

      String outfile = null;
      if (args.length < 1) {
         System.out.println("arguments: input_file [output_file]");
         return;
      }
      if (args.length > 1) {
         outfile = args[1];
      }

      doRead(args[0], outfile);

   }
}
