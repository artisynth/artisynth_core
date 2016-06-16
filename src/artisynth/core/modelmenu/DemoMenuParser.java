/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.awt.Font;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.UIManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import maspack.graph.Node;
import maspack.graph.Tree;
import maspack.util.ClassFinder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import artisynth.core.util.AliasTable;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;

/**
 * 
 * @author antonio
 * 
 * Parses an XML menu that can be used to create a menu entries for "Models".
 * Doesn't actually build a JMenu, instead all information is constructed in a
 * Tree that holds "MenuEntry" objects with icon/title/type information
 * 
 * The XML format supports submenus, icons, dividers, reading classes from a
 * .demoModels file, and reading classes in a given package that are derived
 * from a supplied base class (e.g. RootModel). Other xml menu files can also be
 * imported.
 */
public class DemoMenuParser {

   public enum MenuType {
      ROOT, DIVIDER, MENU, MODEL, LABEL, HISTORY
   }

   public static class SimpleErrorHandler implements ErrorHandler {
      public void warning(SAXParseException e) throws SAXException {
         System.out.println(e.getMessage());
      }

      public void error(SAXParseException e) throws SAXException {
         throw e;
      }

      public void fatalError(SAXParseException e) throws SAXException {
         throw e;
      }
   }

   public static final String ROOT_TAG = "ModelMenu";
   public static final String MENU_TAG = "menu";
   public static final String MENU_TAG_TEXT = "text";
   public static final String MENU_TAG_ICON = "icon";
   public static final String DIVIDER_TAG = "separator";
   public static final String HISTORY_TAG = "history";
   public static final String HISTORY_TAG_SIZE = "size";
   public static final String HISTORY_TAG_COMPACT = "compact";
   public static final String LABEL_TAG = "label";
   public static final String LABEL_TAG_ICON = "icon";
   public static final String LABEL_TAG_TEXT = "text";
   public static final String MODEL_TAG = "model";
   public static final String MODEL_TAG_TEXT = "text";
   public static final String MODEL_TAG_CLASS = "class";
   public static final String MODEL_TAG_ARGS = "args";
   public static final String MODEL_TAG_ICON = "icon";
   public static final String DEMOFILE_TAG = "demosFile";
   public static final String DEMOFILE_TAG_FILENAME = "file";
   public static final String DEMOFILE_TAG_ARGS = "args";
   public static final String PACKAGE_TAG = "package";
   public static final String PACKAGE_TAG_SRC = "source";
   public static final String PACKAGE_TAG_ARGS = "args";
   public static final String PACKAGE_TAG_VIEW = "view";
   public static final String PACKAGE_TAG_VIEW_FLAT = "flat";
   public static final String PACKAGE_TAG_VIEW_HIERARCHICAL =
      "hierarchical";
   public static final String PACKAGE_TAG_BASECLASS = "base";
   public static final String PACKAGE_TAG_REGEX = "regex";
   // 0 for not compact, 1 for compact, 2 for very compact
   public static final String PACKAGE_TAG_COMPACT = "compact"; 
   public static final String XMLINCLUDE_TAG = "include";
   public static final String XMLINCLUDE_TAG_FILE = "file";
   public static final String HIDDEN_TAG = "hidden";

   // font tags
   public static final String ALL_TAG_FONTNAME = "fontname";
   public static final String ALL_TAG_FONTSTYLE = "fontstyle";
   public static final String ALL_TAG_FONTSTYLE_BOLD = "bold";
   public static final String ALL_TAG_FONTSTYLE_ITALIC = "italic";
   // public static final String ALL_TAG_FONTSTYLE_UNDERLINE = "underline";
   // public static final String ALL_TAG_FONTSTYLE_STRIKETHROUGH =
   // "strikethrough";
   public static final String ALL_TAG_FONTSIZE = "fontsize";

   public static Tree<MenuEntry> parseXML(String filename) throws IOException,
      ParserConfigurationException, SAXException {

      // get path of filename
      File file = new File(filename);
      filename = file.getAbsolutePath();
      String localPath =
         filename.substring(0, filename.lastIndexOf(File.separator));

      SchemaFactory schemaFactory =
         SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
      String schemaLoc =
         ArtisynthPath
         .getSrcRelativePath(DemoMenuParser.class, "modelmenu.xsd");
      File schemaLocation = new File(schemaLoc);
      Schema schema = schemaFactory.newSchema(schemaLocation);

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      factory.setSchema(schema);

      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new SimpleErrorHandler());

      Document dom = builder.parse(filename);
      return parseDocument(dom, localPath);
   }

   private static Tree<MenuEntry> parseDocument(Document dom, String localPath) {

      Tree<MenuEntry> menu = new Tree<MenuEntry>(new MenuEntry("Models"));
      Element docEle = dom.getDocumentElement();

      NodeList nl = docEle.getChildNodes();
      Node<MenuEntry> root = menu.getRootElement();

      for (int i = 0; i < nl.getLength(); i++) {
         if (nl.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            Element el = (Element)nl.item(i);
            parseElement(root, el, localPath);
         } // end if element
      } // end loop

      return menu;
   }

   /**
    * Finds a file specified by "file" First checks if file is specified by an
    * absolute path. If not found, checks if it's relative to the supplied
    * localPath. As a last resort, it searches ARTISYNTH_PATH
    * 
    * @return The absolute path of the file, null if not found
    */
   public static String findFile(String filename, String localPath) {

      File file = new File(filename);
      String fullFile = null;

      if (file.canRead()) {
         fullFile = file.getAbsolutePath();
      } else {
         file = new File(localPath, filename);
         if (file.canRead()) {
            fullFile = file.getAbsolutePath();
         } else {

            // use artisynthpath to find file
            File[] found = ArtisynthPath.findFiles(filename);
            if (found != null) {
               if (found.length > 0) {
                  fullFile = found[0].getAbsolutePath();
               }
            }
         }
      }

      if (fullFile == null) {
         System.out.println("Warning: Cannot find file '" + filename + "'");
      }

      return fullFile;

   }

   /**
    * If a menu entry has only a single child, it merges it with this one and
    * the new title becomes "title1 (title2)"
    */
   public static void compactMenu(Tree<MenuEntry> menu) {
      // compact menu
      Node<MenuEntry> root = menu.getRootElement();

      compactMenuNode(root, new MergeTitleFunction() {
         public String merge(String title1, String title2) {
            return title1 + "  (" + title2 + ")";
         }
      });
   }

   private static void parseElement(Node<MenuEntry> node, Element el,
      String localPath) {

      // recursively add menu
      if (el.getNodeName().equals(MENU_TAG)) {
         MenuEntry newEntry = parseMenu(el, localPath);
         if (newEntry != null) {

            Node<MenuEntry> newNode = new Node<MenuEntry>(newEntry);
            node.addChild(newNode);

            // add all children
            NodeList nl = el.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
               if (nl.item(i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                  Element nEl = (Element)nl.item(i);
                  parseElement(newNode, nEl, localPath);
               }
            } // end looping through children

         }

         // add a divider
      } else if (el.getNodeName().equals(DIVIDER_TAG)) {
         DividerEntry div = new DividerEntry("<divider>");
         node.addChild(new Node<MenuEntry>(div));

         // add a text label
      } else if (el.getNodeName().equals(LABEL_TAG)) {
         LabelEntry label = parseLabel(el, localPath);
         node.addChild(new Node<MenuEntry>(label));

         // add a demo
      } else if (el.getNodeName().equals(MODEL_TAG)) {
         DemoEntry demo = parseDemo(el, localPath);
         if (demo != null) {
            node.addChild(new Node<MenuEntry>(demo));
            // add to demo list
            // demos.add(demo.getModel());
         }

         // parse an entire demo file
      } else if (el.getNodeName().equals(DEMOFILE_TAG)) {
         ArrayList<DemoEntry> newEntries = parseDemoFile(el, localPath);

         for (int j = 0; j < newEntries.size(); j++) {
            node.addChild(new Node<MenuEntry>(newEntries.get(j)));
            // demos.add(newEntries.get(j).getModel());
         }

         // parse an entire package
      } else if (el.getNodeName().equals(PACKAGE_TAG)) {
         Tree<MenuEntry> newEntries = parsePackage(el);

         if (newEntries != null) {
            Node<MenuEntry> root = newEntries.getRootElement();

            // ignore root, and add all children
            for (Node<MenuEntry> daughter : root.getChildren()) {
               node.addChild(daughter);
            }
            // demos.addAll(models);
         }

      // parse history tag
      } else if (el.getNodeName().equals(HISTORY_TAG)) {
         HistoryEntry hist = parseHistory(el, localPath);
         if (hist != null) {
            node.addChild(new Node<MenuEntry>(hist));
         }
         
      } else if (el.getNodeName().equals(XMLINCLUDE_TAG)) {

         Tree<MenuEntry> newEntries = parseXML(el, localPath);

         if (newEntries != null) {
            Node<MenuEntry> root = newEntries.getRootElement();
            for (Node<MenuEntry> daughter : root.getChildren()) {
               node.addChild(daughter);
            }
         }

      } else if (el.getNodeName().equals(HIDDEN_TAG)) {
         // skip entry entirely
      } else {
         System.out.println("Warning: unknown tag <" + el.getNodeName() + ">");
      } // end checking type

   }

   // get font information from element
   private static Font parseFont(Element el) {

      boolean modified = false;

      // Map<TextAttribute, Integer> fontAttributes = new HashMap<TextAttribute,
      // Integer>();
      // fontAttributes.put(TextAttribute.UNDERLINE,
      // TextAttribute.UNDERLINE_ON);
      // Font boldUnderline = new Font("Serif",Font.BOLD,
      // 12).deriveFont(fontAttributes);

      Font defaultFont = UIManager.getFont("Menu.font");
      int size = defaultFont.getSize();
      String name = defaultFont.getName();
      int style = Font.PLAIN;

      // = new Font(defaultFont.getName(), defaultFont.getStyle(),
      // defaultFont.getSize());

      String fontName = el.getAttribute(ALL_TAG_FONTNAME);
      String fontSize = el.getAttribute(ALL_TAG_FONTSIZE);
      String fontStyle = el.getAttribute(ALL_TAG_FONTSTYLE);

      if (!fontName.equals("")) {
         modified = true;
         name = fontName;
      }
      if (!fontSize.equals("")) {
         size = Integer.parseInt(fontSize);
         modified = true; 
      }
      if (!fontStyle.equals("")) {
         // we need to check if they actually put something
         modified = true;
         if (fontStyle.contains(ALL_TAG_FONTSTYLE_BOLD)) {
            style |= Font.BOLD;
         }
         if (fontStyle.contains(ALL_TAG_FONTSTYLE_ITALIC)) {
            style |= Font.ITALIC;
         }
      } else {
         // if they purposely set style to ""
         if (el.hasAttribute(ALL_TAG_FONTSTYLE)) {
            modified = true;
         }
      }

      if (!modified) {
         return null;
      }
      return new Font(name, style, size);
   }

   private static Tree<MenuEntry> parseXML(Element el, String localPath) {

      String file = el.getAttribute(XMLINCLUDE_TAG_FILE);

      // find included file
      String incfile = findFile(file, localPath);

      Tree<MenuEntry> menu = null;

      if (incfile != null) {
         try {
            menu = parseXML(incfile);
         } catch (Exception e) {
            e.printStackTrace();
            return null;
         }
      } else {
         System.out.println("DemoMenuParser: Unable to find included file '"
            + file + "', ignoring...");
      }
      return menu;
   }

   private static LabelEntry parseLabel(Element el, String localPath) {

      String text = el.getAttribute(LABEL_TAG_TEXT);
      String icon = el.getAttribute(LABEL_TAG_ICON);

      LabelEntry label = new LabelEntry(text);
      if (!icon.equals("")) {
         String fullicon = findFile(icon, localPath);
         if (fullicon != null) {
            label.setIcon(fullicon);
         } else {
            System.out.println("Unable to find icon '" + icon + "'");
         }
      }
      Font myFont = parseFont(el);
      if (myFont != null) {
         label.setFont(myFont);
      }

      return label;

   }

   private static ArrayList<DemoEntry> parseDemoFile(Element el,
      String localPath) {

      ArrayList<DemoEntry> demos = new ArrayList<DemoEntry>();
      String file = el.getAttribute(DEMOFILE_TAG_FILENAME);
      String argsStr = el.getAttribute(PACKAGE_TAG_ARGS);
      String[] args = null;
      if (argsStr != null && !"".equals(argsStr)) {
         args = splitArgs(argsStr);
      }

      AliasTable myDemoModels = null;
      String fullFile = findFile(file, localPath);

      if (fullFile == null) {
         System.out.println("Warning: demosFile: '" + file + "' not found");
      } else {
         try {
            myDemoModels = new AliasTable(new File(fullFile));
         } catch (Exception e) {
            System.out.println("Warning: error reading demosFile: '" + file
               + "'");
            System.out.println(e.getMessage());
         }
      }

      if (myDemoModels == null) {
         myDemoModels = new AliasTable(); // default: an empty alias table
      }

      // remove entries that are not valid models
      Iterator<Map.Entry<String,String>> li = myDemoModels.entrySet().iterator();
      while (li.hasNext()) {
         Map.Entry<String,String> entry = li.next();
         try {
            Class<?> clazz = Class.forName (entry.getValue());
            if (!RootModel.class.isAssignableFrom(clazz) ||
                Modifier.isAbstract (clazz.getModifiers())) {
               li.remove();
            }
         }
         catch (Exception e) {
            // if there's a problem finding the class, just remove the entry
            li.remove();
         }
      }

      String[] aliases = myDemoModels.getAliases();
      for (int i = 0; i < aliases.length; i++) {
         demos.add(new DemoEntry(myDemoModels.getName(aliases[i]), aliases[i], args));
      }

      // set fonts for all entries
      Font myFont = parseFont(el);
      if (myFont != null) {
         for (MenuEntry demo : demos) {
            demo.setFont(myFont);
         }
      }

      return demos;
   }
   
   private static String[] splitArgs(String argsStr) {
      List<String> matchList = new ArrayList<String>();
      Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
      Matcher regexMatcher = regex.matcher(argsStr);
      while (regexMatcher.find()) {
          if (regexMatcher.group(1) != null) {
              // Add double-quoted string without the quotes
              matchList.add(regexMatcher.group(1));
          } else if (regexMatcher.group(2) != null) {
              // Add single-quoted string without the quotes
              matchList.add(regexMatcher.group(2));
          } else {
              // Add unquoted word
              matchList.add(regexMatcher.group());
          }
      } 
      return matchList.toArray(new String[matchList.size()]);
   }

   private static DemoEntry parseDemo(Element el, String localPath) {
      String name, file, icon, argsStr;

      file = el.getAttribute(MODEL_TAG_CLASS);
      name = el.getAttribute(MODEL_TAG_TEXT);
      icon = el.getAttribute(MODEL_TAG_ICON);
      argsStr = el.getAttribute(MODEL_TAG_ARGS);

      // must be non-null
      if (file.equals("")) {
         return null;
      }

      // if no name supplied, use the class
      if (name.equals("")) {
         name = file;
      }
      
      String[] args = null;
      if (argsStr != null && !"".equals(argsStr)) {
         args = splitArgs(argsStr);
      }
      
     // separate model args to a list of strings

      DemoEntry demo = new DemoEntry(file, name, args);
      if (!icon.equals("")) {
         String fullicon = findFile(icon, localPath);
         if (fullicon != null) {
            demo.setIcon(fullicon);
         } else {
            System.out.println("Unable to find icon '" + icon + "'");
         }
      }

      Font myFont = parseFont(el);
      if (myFont != null) {
         demo.setFont(myFont);
      }

      return demo;
   }
   
   private static HistoryEntry parseHistory(Element el, String localPath) {
      String sizeStr, compactStr;

      sizeStr = el.getAttribute(HISTORY_TAG_SIZE);
      compactStr = el.getAttribute(HISTORY_TAG_COMPACT);

      int size = 4;
      if (sizeStr != null && ! "".equals(sizeStr)) {
         size = Integer.parseInt(sizeStr);
      }
      
      int compact = 0;
      if (compactStr != null && !"".equals(compactStr)) {
         compact = Integer.parseInt(compactStr);
      }
      
      HistoryEntry entry = new HistoryEntry(size,compact);

      Font myFont = parseFont(el);
      if (myFont != null) {
         entry.setFont(myFont);
      }

      return entry;
   }

   private static MenuEntry parseMenu(Element el, String localPath) {

      String name = el.getAttribute(MENU_TAG_TEXT);
      String icon = el.getAttribute(MENU_TAG_ICON);
      if (name.equals("")) {
         name = "<unknown>";
      }

      MenuEntry m = new MenuEntry(name);
      if (!icon.equals("")) {
         String fullicon = findFile(icon, localPath);
         if (fullicon != null) {
            m.setIcon(fullicon);
         } else {
            System.out.println("Unable to find icon '" + icon + "'");
         }
      }
      Font myFont = parseFont(el);
      if (myFont != null) {
         m.setFont(myFont);
      }

      return m;
   }

   private static Tree<MenuEntry> parsePackage(Element el) {

      Tree<MenuEntry> menu = new Tree<MenuEntry>(new MenuEntry("root"));
      Node<MenuEntry> root = menu.getRootElement();

      String view = el.getAttribute(PACKAGE_TAG_VIEW);
      String pkg = el.getAttribute(PACKAGE_TAG_SRC);
      String baseClass = el.getAttribute(PACKAGE_TAG_BASECLASS);
      String compactStr = el.getAttribute(PACKAGE_TAG_COMPACT);
      String regex = el.getAttribute(PACKAGE_TAG_REGEX);
      
      String argsStr = el.getAttribute(PACKAGE_TAG_ARGS);
      String[] args = null;
      if (argsStr != null && !"".equals(argsStr)) {
         args = splitArgs(argsStr);
      }

      int compact = 0;
      if (!compactStr.equals("")) {
         compact = Integer.parseInt(compactStr);
      }

      Class<?> base = null;

      if (baseClass.equals("")) {
         baseClass = "artisynth.core.workspace.RootModel";
      }

      try {
         // first check if base model explicitly provided
         try {
            base = Class.forName(baseClass);
         } catch (ClassNotFoundException e) {

            System.out.println("Searching for base class '" + baseClass + "'");
            // search for package, first checking supplied package directory
            ArrayList<Class<?>> searchResults = ClassFinder.findClass(pkg,
               baseClass);// Class.forName(baseClass);
            if (searchResults.size() == 0) {
               // then check root directory
               searchResults = ClassFinder.findClass("", baseClass);
            }
            if (searchResults.size() > 0) {
               base = searchResults.get(0);
            } else {
               System.out.println("Cannot find class '" + baseClass + "'");
            }
         }

      } catch (Exception e) {
         e.printStackTrace();
      }

      if (base == null) {
         base = RootModel.class;
      }

      // System.out.println("Searching for classes of type '" + base.getName()
      // + "' in package '" + pkg + "'");

      ArrayList<String> clsList;
      if (!regex.equals("")) {
         clsList = ClassFinder.findClassNames(pkg, regex, base);
      } else {
         clsList = ClassFinder.findClassNames(pkg, base);
      }
      Collections.sort(clsList);

      // John Lloyd, June, 2014
      // filter out abstract classes that we can't instantiate ...
      ListIterator<String> li = clsList.listIterator();
      while (li.hasNext()) {
         try {
            Class clazz = Class.forName (li.next());
            if (Modifier.isAbstract (clazz.getModifiers())) {
               li.remove();
            }            
         }
         catch (Exception e) {
            // shouldn't happen - remove class if it does
            li.remove();
         }
      }

      if (!pkg.equals("") && !pkg.endsWith(".")) {
         pkg = pkg + "."; // add a dot to the prefix
      }

      // if the view is flat, simply list packages
      if (view.equals(PACKAGE_TAG_VIEW_FLAT)) {

         // finds greatest common prefix of class list (used for compacting)
         String prefix = getPrefix(new ArrayList<String>(clsList));

         for (String cls : clsList) {
            String title = cls;
            if (title.startsWith(pkg)) {
               title = cls.substring(pkg.length()); // remove supplied package
               // from title
            }
            if (compact > 1) {
               String[] titleArray = title.split("\\.");
               title = titleArray[titleArray.length - 1];
            } else if (compact == 1) {
               // remove prefix
               title = cls.substring(prefix.length());
            }
            DemoEntry demo = new DemoEntry(cls, title, args);
            root.addChild(new Node<MenuEntry>(demo));
         }
         sortMenu(root, new MenuCompareByNameButDemosLast());

         // default to hierarchical
      } else {
         menu = getPackageMenuTree(clsList, pkg, compact, args);
         root = menu.getRootElement();
         sortMenu(root, new MenuCompareByNameButDemosLast()); // sort first
         insertDividersInPackageMenu(root); // then insert dividers
      }

      Font myFont = parseFont(el);
      if (myFont != null) {
         // go through and set font for all items in the menu
         setFont(menu.getRootElement(), myFont);
      }

      return menu;
   }

   // recursively set the font for all items in the tree
   public static void setFont(Node<MenuEntry> node, Font font) {
      node.getData().setFont(font);
      for (Node<MenuEntry> child : node.getChildren()) {
         setFont(child, font);
      }
   }

   // sorts a menu, respecting dividers
   public static void sortMenu(Node<MenuEntry> root,
      Comparator<Node<MenuEntry>> comparer) {

      // sort children, respecting dividers
      int start = 0;
      int end = 0;

      // create array of children
      List<Node<MenuEntry>> children = root.getChildren();

      while (start < children.size()) {

         end = start;

         // find divider or end of array
         while (children.get(end).getData().getType() != MenuType.DIVIDER) {
            end++;
            if (end == root.getNumberOfChildren()) {
               break;
            }
         }
         // sort the sublist
         Collections.sort(children.subList(start, end), comparer); // skip
         // divider or
         // end of
         // list
         start = end + 1; // move to next item
      }

      if (root.getNumberOfChildren() > 0) {
         for (Node<MenuEntry> child : root.getChildren()) {
            sortMenu(child, comparer);
         }
      }
   }

   public static void sortMenu(Node<MenuEntry> root) {
      sortMenu(root, new MenuCompareByNameButDemosLast());
   }

   /**
    * Given a list of strings, finds the greatest common prefix
    * 
    * @param array
    * input array of strings
    * @return the greatest common prefix
    */
   public static String getPrefix(ArrayList<String> array) {
      String pre = "";
      int preLength = 0;

      if (array.size() == 0) {
         return "";
      }

      int maxLength = array.get(0).length();

      char c;

      // loop through each character to see if it matches in all supplied words
      for (int i = 0; i < maxLength; i++) {
         boolean diff = false;
         c = array.get(0).charAt(i);
         for (int j = 1; j < array.size(); j++) {
            if (array.get(j).charAt(i) != c) {
               diff = true;
               break;
            }
         }
         if (diff) {
            break;
         }
         preLength++;
      }
      pre = array.get(0).substring(0, preLength);
      return pre;
   }

   // divides models from packages with a divider
   private static void insertDividersInPackageMenu(Node<MenuEntry> root) {

      List<Node<MenuEntry>> children = root.getChildren();
      MenuType typeA, typeB;

      int i = 1;
      while (i < children.size()) {

         // if different data type, insert a divider
         typeA = children.get(i).getData().getType();
         typeB = children.get(i - 1).getData().getType();
         if ((typeA != typeB) && (typeA != MenuType.DIVIDER)
            && (typeB != MenuType.DIVIDER)) {
            root.insertChildAt(i, new Node<MenuEntry>(new DividerEntry(
               "<inserted divider>")));
            i = i++; // skip divider
         }
         i++;
      }

      // recursively add dividers
      for (Node<MenuEntry> child : children) {
         if (child.getNumberOfChildren() > 0) {
            insertDividersInPackageMenu(child);
         }
      }

   }

   private static Tree<MenuEntry> getPackageMenuTree(ArrayList<String> clsList,
      String pkg, int compact, String[] args) {
      Tree<MenuEntry> menu = new Tree<MenuEntry>(new MenuEntry("root"));
      Node<MenuEntry> root = menu.getRootElement();
      Node<MenuEntry> base;

      // first, create naive ragged-array structure
      for (int i = 0; i < clsList.size(); i++) {
         String cls = clsList.get(i);
         String title = cls;
         if (title.startsWith(pkg)) {
            title = title.substring(pkg.length());
         }
         String[] sections = title.split("\\."); // split at periods
         MenuEntry newEntry;
         base = root;

         // tack on to tree
         for (int j = 0; j < sections.length; j++) {
            if (j < sections.length - 1) {
               newEntry = new MenuEntry(sections[j]);
            } else {
               newEntry = new DemoEntry(cls, sections[j], args);
            }
            Node<MenuEntry> newNode = new Node<MenuEntry>(newEntry);
            base.addChild(newNode);
            base = newNode;
         }
      }

      // consolidate branches
      menu.consolidate();

      // merge items with single children to prevent excessive menu levels
      if (compact == 1) {

         List<Node<MenuEntry>> packageRoot = root.getChildren();
         MergeTitleFunction mergeTitle = new MergeTitleFunction() {
            public String merge(String title1, String title2) {
               return title1 + "." + title2;
            }
         };

         for (Node<MenuEntry> node : packageRoot) {
            compactMenuNode(node, mergeTitle);
         }

      } else if (compact > 1) {

         // merge to root and only use demo name
         compactMenuNode(root, new MergeTitleFunction() {
            public String merge(String title1, String title2) {
               return title2;
            }
         });
      }

      return menu;
   }

   private static void compactMenuNode(Node<MenuEntry> node,
      MergeTitleFunction mergeStrings) {

      if (node.getNumberOfChildren() == 1) {
         // merge the nodes
         String thisTitle = node.getData().getTitle();
         Node<MenuEntry> child = node.getChild(0); // get first child
         String thatTitle = child.getData().getTitle();

         // copy data and children
         node.setData(child.getData());
         node.getData().setTitle(mergeStrings.merge(thisTitle, thatTitle));
         for (Node<MenuEntry> grandChild : child.getChildren()) {
            node.addChild(grandChild);
         }
         child.clear(); // kill the single child
         node.removeChild(child); // remove from family tree
         compactMenuNode(node, mergeStrings); // repeat now that we are
         // effectively the new child node

         // otherwise, try compacting the children
      } else {
         for (Node<MenuEntry> child : node.getChildren()) {

            if (child.getNumberOfChildren() > 0) {
               compactMenuNode(child, mergeStrings);
            }
         }
      }
   }

}
