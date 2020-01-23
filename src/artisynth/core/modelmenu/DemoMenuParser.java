/**

 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.awt.Font;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;
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
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import artisynth.core.util.AliasTable;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;
import maspack.graph.Node;
import maspack.graph.Tree;
import maspack.util.ClassFinder;

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

   public static enum MenuType {
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
   
   private static final String DIVIDER_TITLE = "<divider>";

   public static final String ROOT_TAG = "ModelMenu";
   public static final String ROOT_TAG_SCROLLING = "scrolling";
   public static final String ROOT_TAG_MAX_ROWS = "maxRows";
   public static final String MENU_TAG = "menu";
   public static final String MENU_TAG_TEXT = "text";
   public static final String MENU_TAG_ICON = "icon";
   public static final String MENU_TAG_SCROLLING = "scrolling";
   public static final String MENU_TAG_MAX_ROWS = "maxRows";
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
   public static final String PACKAGE_TAG_SCROLLING = "scrolling";
   public static final String PACKAGE_TAG_MAX_ROWS = "maxRows";
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

   public static void writeXML(String filename, Tree<MenuNode> menu) {
      writeXML(new File(filename), menu);
   }
   
   public static void writeXML(File file, Tree<MenuNode> menu) {
      FileOutputStream fout = null;
      try {
         File parent = file.getParentFile();
         // try to make directory structure if not already exists
         if (parent != null && !parent.exists()) {
            try {
               parent.mkdirs();
            } catch (Exception e) {}
         }
         fout = new FileOutputStream(file);
         writeXML(fout, menu);
      } catch (FileNotFoundException e) {
         System.err.println("File not found: " + file.getAbsolutePath());
      } finally {
         try {
            if (fout != null) {
               fout.close();
            }
         } catch (IOException e) {
         }
      }
   }
   
   public static void writeXML(OutputStream out, Tree<MenuNode> menu) {

      SchemaFactory schemaFactory =
         SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
      String schemaLoc =
         ArtisynthPath.getSrcRelativePath(DemoMenuParser.class, "modelmenu.xsd");
      File schemaLocation = new File(schemaLoc);
      Schema schema;
      try {
         schema = schemaFactory.newSchema(schemaLocation);
      } catch (SAXException e) {
         e.printStackTrace();
         return;
      }

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      factory.setSchema(schema);

      DocumentBuilder builder;
      try {
         builder = factory.newDocumentBuilder();
      } catch (ParserConfigurationException e) {
         e.printStackTrace();
         return;
      }
      builder.setErrorHandler(new SimpleErrorHandler());
      
      Document dom = builder.newDocument();
      buildDocument(dom, menu);
      
      // Use a Transformer for output
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transformer;
      try {
         transformer = tFactory.newTransformer();
         transformer.setOutputProperty(OutputKeys.INDENT, "yes");
         transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
      } catch (TransformerConfigurationException e) {
         e.printStackTrace();
         return;
      }

      DOMSource source = new DOMSource(dom);
      StreamResult result = new StreamResult(out);
      try {
         transformer.transform(source, result);
      } catch (TransformerException e) {
         e.printStackTrace();
         return;
      }
   }
   
   static void buildDocument(Document dom, Tree<MenuNode> menu) {
      
      Node<MenuNode> root = menu.getRootElement();
      MenuEntry data = (MenuEntry)root.getData();
      
      Element modelMenu = dom.createElement(ROOT_TAG);
      modelMenu.setAttribute("xmlns", "http://www.artisynth.org");
      modelMenu.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
      modelMenu.setAttribute("xsi:schemaLocation", "http://www.artisynth.org src/artisynth/core/modelmenu/modelmenu.xsd");
      
      if (data.isScrolling ()) {
         modelMenu.setAttribute (ROOT_TAG_SCROLLING, Boolean.toString (data.isScrolling ()));
      }
      modelMenu.setAttribute (ROOT_TAG_MAX_ROWS, Integer.toString (data.getMaxRows ()));
      
      dom.appendChild(modelMenu);
      for (Node<MenuNode> child : root.getChildren()) {
         buildElement(dom, modelMenu, child);
      }
   }
   
   static void buildElement(Document dom, Element parent, Node<MenuNode> node) {
      
      MenuNode data = node.getData();
      MenuType type = data.getType();
      
      // recursively add menu
      Element el = null;
      switch(type) {
         case DIVIDER:
            el = dom.createElement(DIVIDER_TAG);
            break;
         case HISTORY:
            el = buildHistory(dom, (HistoryEntry)data);
            break;
         case LABEL:
            el = buildLabel(dom, (LabelEntry)data);
            break;
         case MENU:
            el = buildMenu(dom, (MenuEntry)data);
            for (Node<MenuNode> child : node.getChildren()) {
               buildElement(dom, el, child);
            }
            break;
         case MODEL:
            el = buildDemo(dom, (DemoEntry)data);
            break;
         case ROOT:
            break;
         default:
            break;
       
      }  
      
      if (parent != null && el != null) {
         parent.appendChild(el);
      }
   }
   
   public static Tree<MenuNode> parseXML(String filename) throws IOException,
      ParserConfigurationException, SAXException {

      // get path of filename
      File file = new File(filename);
      filename = file.getAbsolutePath();
      String localPath =
         filename.substring(0, filename.lastIndexOf(File.separator));

      SchemaFactory schemaFactory =
         SchemaFactory.newInstance("http://www.w3.org/2001/XMLSchema");
      String schemaLoc =
         ArtisynthPath.getSrcRelativePath(DemoMenuParser.class, "modelmenu.xsd");

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating(false);
      factory.setNamespaceAware(true);
      
      if (schemaLoc != null) {
         File schemaLocation = new File(schemaLoc);
         Schema schema = schemaFactory.newSchema(schemaLocation);
         factory.setSchema(schema);
      }

      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setErrorHandler(new SimpleErrorHandler());
      Document dom = builder.parse(filename);
      return parseDocument(dom, localPath);
   }

   private static Tree<MenuNode> parseDocument(Document dom, String localPath) {

      Tree<MenuNode> menu = new Tree<MenuNode>(new MenuEntry("Models"));
      Element docEle = dom.getDocumentElement();

      NodeList nl = docEle.getChildNodes();
      Node<MenuNode> root = menu.getRootElement();
      MenuEntry rootEntry = (MenuEntry)root.getData ();
      String scrolling = docEle.getAttribute (ROOT_TAG_SCROLLING);
      rootEntry.setScrolling (parseBoolean (scrolling));
      String maxRows = docEle.getAttribute (ROOT_TAG_MAX_ROWS);
      rootEntry.setMaxRows (Integer.parseInt (maxRows));
      
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
   public static void compactMenu(Tree<MenuNode> menu) {
      // compact menu
      Node<MenuNode> root = menu.getRootElement();

      compactMenuNode(root, new MergeTitleFunction() {
         public String merge(String title1, String title2) {
            return title1 + "  (" + title2 + ")";
         }
      });
   }

   private static void parseElement(Node<MenuNode> node, Element el,
      String localPath) {

      // recursively add menu
      if (el.getNodeName().equals(MENU_TAG)) {
         MenuNode newEntry = parseMenu(el, localPath);
         if (newEntry != null) {

            Node<MenuNode> newNode = new Node<MenuNode>(newEntry);
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
         DividerEntry div = new DividerEntry(DIVIDER_TITLE);
         node.addChild(new Node<MenuNode>(div));

         // add a text label
      } else if (el.getNodeName().equals(LABEL_TAG)) {
         LabelEntry label = parseLabel(el, localPath);
         node.addChild(new Node<MenuNode>(label));

         // add a demo
      } else if (el.getNodeName().equals(MODEL_TAG)) {
         DemoEntry demo = parseDemo(el, localPath);
         if (demo != null) {
            node.addChild(new Node<MenuNode>(demo));
            // add to demo list
            // demos.add(demo.getModel());
         }

         // parse an entire demo file
      } else if (el.getNodeName().equals(DEMOFILE_TAG)) {
         ArrayList<DemoEntry> newEntries = parseDemoFile(el, localPath);

         for (int j = 0; j < newEntries.size(); j++) {
            node.addChild(new Node<MenuNode>(newEntries.get(j)));
            // demos.add(newEntries.get(j).getModel());
         }

         // parse an entire package
      } else if (el.getNodeName().equals(PACKAGE_TAG)) {
         Tree<MenuNode> newEntries = parsePackage(el);

         if (newEntries != null) {
            Node<MenuNode> root = newEntries.getRootElement();

            // ignore root, and add all children
            for (Node<MenuNode> daughter : root.getChildren()) {
               node.addChild(daughter);
            }
            // demos.addAll(models);
         }

      // parse history tag
      } else if (el.getNodeName().equals(HISTORY_TAG)) {
         HistoryEntry hist = parseHistory(el, localPath);
         if (hist != null) {
            node.addChild(new Node<MenuNode>(hist));
         }
         
      } else if (el.getNodeName().equals(XMLINCLUDE_TAG)) {

         Tree<MenuNode> newEntries = parseXML(el, localPath);

         if (newEntries != null) {
            Node<MenuNode> root = newEntries.getRootElement();
            for (Node<MenuNode> daughter : root.getChildren()) {
               node.addChild(daughter);
            }
         }

      } else if (el.getNodeName().equals(HIDDEN_TAG)) {
         // skip entry entirely
      } else {
         System.out.println("Warning: unknown tag <" + el.getNodeName() + ">");
      } // end checking type

   }
   
   private static void addFontAttributes(Element el, Font font) {
      if (font != null) {
         el.setAttribute(ALL_TAG_FONTNAME, font.getFontName());
         el.setAttribute(ALL_TAG_FONTSIZE, Integer.toString(font.getSize()));
         String style = "";
         if (font.isBold()) {
            style += ALL_TAG_FONTSTYLE_BOLD;
         }
         if (font.isItalic()) {
            style += ALL_TAG_FONTSTYLE_ITALIC;
         }
         el.setAttribute(ALL_TAG_FONTSTYLE, style);
      }
   }
   
   private static boolean parseBoolean(String str) {
      boolean scrolling = false;
      if (str != null && !"".equals (str)) {
         if ("true".equalsIgnoreCase(str) || "1".equals(str)) {
            scrolling = true;
         }
      }
      return scrolling;
   }

   // get font information from element
   private static Font parseFont(Element el) {

      boolean specified = el.hasAttribute(ALL_TAG_FONTNAME) || el.hasAttribute(ALL_TAG_FONTSIZE) 
         || el.hasAttribute(ALL_TAG_FONTSTYLE);
      if (!specified) {
         return null;
      }
      
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
         name = fontName;
      }
      if (!fontSize.equals("")) {
         size = Integer.parseInt(fontSize);
      }
      if (!fontStyle.equals("")) {
         // we need to check if they actually put something
         if (fontStyle.contains(ALL_TAG_FONTSTYLE_BOLD)) {
            style |= Font.BOLD;
         }
         if (fontStyle.contains(ALL_TAG_FONTSTYLE_ITALIC)) {
            style |= Font.ITALIC;
         }
      }

      return new Font(name, style, size);
   }

   private static Tree<MenuNode> parseXML(Element el, String localPath) {

      String file = el.getAttribute(XMLINCLUDE_TAG_FILE);

      // find included file
      String incfile = findFile(file, localPath);

      Tree<MenuNode> menu = null;

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

   private static Element buildLabel(Document dom, LabelEntry label) {
      Element el = dom.createElement(LABEL_TAG);
      String icon = label.getIcon();
      if (icon != null) {
         el.setAttribute(LABEL_TAG_ICON, icon);
      }
      String text = label.getTitle();
      if (text != null) {
         el.setAttribute(LABEL_TAG_TEXT, text);
      }
      
      Font font = label.getFont();
      if (font != null) {
         addFontAttributes(el, font);
      }
      return el;
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

   private static boolean omitFromMenu (Class<?> clazz) {
      // omit from menu if the class contains a static public field named
      // omitFromMenu whose value con be converted to 'true'
      try {
         Field field = clazz.getField ("omitFromMenu");
         return field.getBoolean (null);         
      }
      catch (Exception e) {
         return false;
      }
   }

   private static ArrayList<DemoEntry> parseDemoFile(Element el,
      String localPath) {
      
      ArrayList<DemoEntry> demos = new ArrayList<DemoEntry>();
      String file = el.getAttribute(DEMOFILE_TAG_FILENAME);
      String argsStr = el.getAttribute(DEMOFILE_TAG_ARGS);
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
                Modifier.isAbstract (clazz.getModifiers()) ||
                omitFromMenu (clazz)) {
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
         for (MenuNode demo : demos) {
            demo.setFont(myFont);
         }
      }

      return demos;
   }
   
   private static String mergeArgs(String[] args) {
      StringBuilder argBuilder = new StringBuilder();
      
      for (String arg : args) {
         argBuilder.append(' ');
         if (!arg.contains(" ")) {
            argBuilder.append(arg);
         } else if (!arg.contains("\"")) {
            argBuilder.append('"');
            argBuilder.append(arg);
            argBuilder.append('"');
         } else if (!arg.contains("\'")) {
            argBuilder.append('\'');
            argBuilder.append(arg);
            argBuilder.append('\'');
         } else {
            System.err.println("Error: cannot append argument: " + arg + " because it has spaces and both types of quotes");
         }
      }
      
      return argBuilder.toString().trim();
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

   private static Element buildDemo(Document dom, DemoEntry demo) {
      String name, file, icon, argsStr;
      Font font;

      file = demo.getModel().getClassNameOrFile();
      name = demo.getTitle();
      icon = demo.getIcon();
      String[] args = demo.getModel().getArgs();
      font = demo.getFont();
      
      Element el = dom.createElement(MODEL_TAG);
      el.setAttribute(MODEL_TAG_CLASS, file);
      el.setAttribute(MODEL_TAG_TEXT, name);
      if (icon != null) {
         el.setAttribute(MODEL_TAG_ICON, icon);
      }
      if (args != null) {
         // combine into single string
         argsStr = mergeArgs(args);
         el.setAttribute(MODEL_TAG_ARGS, argsStr);
      }
      
      if (font != null) {
         addFontAttributes(el, font);
      }
      
      return el;
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
   
   private static Element buildHistory(Document dom, HistoryEntry entry) {

      Element el = dom.createElement(HISTORY_TAG);
      int size = entry.getSize();
      int compact = entry.getCompact();
      
      el.setAttribute(HISTORY_TAG_SIZE, Integer.toString(size));
      el.setAttribute(HISTORY_TAG_COMPACT, Integer.toString(compact));
      addFontAttributes(el, entry.getFont());

      return el;
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

   private static Element buildMenu(Document dom, MenuEntry entry) {
      Element el = dom.createElement(MENU_TAG);
      el.setAttribute(MENU_TAG_TEXT, entry.getTitle());
      String icon = entry.getIcon();
      if (icon != null) {
         el.setAttribute(MENU_TAG_ICON, icon);
      }
      boolean scrolling = entry.isScrolling ();
      el.setAttribute (MENU_TAG_SCROLLING, Boolean.toString (scrolling));
      int maxRows = entry.getMaxRows ();
      el.setAttribute (MENU_TAG_MAX_ROWS, Integer.toString (maxRows));
      addFontAttributes(el, entry.getFont());
      return el;
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
      
      String scrollingStr = el.getAttribute (MENU_TAG_SCROLLING);
      boolean scrolling = parseBoolean(scrollingStr);
      m.setScrolling (scrolling);
      
      String maxRowsStr =el.getAttribute (MENU_TAG_MAX_ROWS);
      int maxRows = Integer.parseInt (maxRowsStr);
      m.setMaxRows (maxRows);
      
      Font myFont = parseFont(el);
      if (myFont != null) {
         m.setFont(myFont);
      }

      return m;
   }

   private static Tree<MenuNode> parsePackage(Element el) {

      MenuEntry rootEntry = new MenuEntry("root");
      Tree<MenuNode> menu = new Tree<MenuNode>(rootEntry);
      Node<MenuNode> root = menu.getRootElement();

      String view = el.getAttribute(PACKAGE_TAG_VIEW);
      String pkg = el.getAttribute(PACKAGE_TAG_SRC);
      String baseClass = el.getAttribute(PACKAGE_TAG_BASECLASS);
      String compactStr = el.getAttribute(PACKAGE_TAG_COMPACT);
      String regex = el.getAttribute(PACKAGE_TAG_REGEX);
      String scrollingStr = el.getAttribute (MENU_TAG_SCROLLING);
      String maxRowsStr = el.getAttribute (MENU_TAG_MAX_ROWS);
      
      boolean scrolling = parseBoolean(scrollingStr);
      rootEntry.setScrolling (scrolling);
      
      int maxRows = Integer.parseInt (maxRowsStr);
      rootEntry.setMaxRows (maxRows);
      
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
            Class<?> clazz = Class.forName (li.next());
            if (Modifier.isAbstract (clazz.getModifiers())) {
               li.remove();
            }
            else if (omitFromMenu (clazz)) {
               li.remove();
            }
         } catch (Error | Exception e) {
            // shouldn't happen - remove class if it does
            e.printStackTrace ();
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
            root.addChild(new Node<MenuNode>(demo));
         }
         sortMenu(root, new MenuCompareByNameButDemosLast());

         // default to hierarchical
      } else {
         menu = getPackageMenuTree(clsList, pkg, compact, args, scrolling, maxRows);
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
   public static void setFont(Node<MenuNode> node, Font font) {
      node.getData().setFont(font);
      for (Node<MenuNode> child : node.getChildren()) {
         setFont(child, font);
      }
   }

   // sorts a menu, respecting dividers
   public static void sortMenu(Node<MenuNode> root,
      Comparator<Node<MenuNode>> comparer) {

      // sort children, respecting dividers
      int start = 0;
      int end = 0;

      // create array of children
      List<Node<MenuNode>> children = root.getChildren();

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
         for (Node<MenuNode> child : root.getChildren()) {
            sortMenu(child, comparer);
         }
      }
   }

   public static void sortMenu(Node<MenuNode> root) {
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
   private static void insertDividersInPackageMenu(Node<MenuNode> root) {

      List<Node<MenuNode>> children = root.getChildren();
      MenuType typeA, typeB;

      int i = 1;
      while (i < children.size()) {

         // if different data type, insert a divider
         typeA = children.get(i).getData().getType();
         typeB = children.get(i - 1).getData().getType();
         if ((typeA != typeB) && (typeA != MenuType.DIVIDER)
            && (typeB != MenuType.DIVIDER)) {
            root.insertChildAt(i, new Node<MenuNode>(new DividerEntry(
              DIVIDER_TITLE)));
            i = i++; // skip divider
         }
         i++;
      }

      // recursively add dividers
      for (Node<MenuNode> child : children) {
         if (child.getNumberOfChildren() > 0) {
            insertDividersInPackageMenu(child);
         }
      }

   }

   private static Tree<MenuNode> getPackageMenuTree(ArrayList<String> clsList,
      String pkg, int compact, String[] args, boolean scrolling, int maxRows) {
      Tree<MenuNode> menu = new Tree<MenuNode>(new MenuEntry("root"));
      Node<MenuNode> root = menu.getRootElement();
      Node<MenuNode> base;

      // first, create naive ragged-array structure
      for (int i = 0; i < clsList.size(); i++) {
         String cls = clsList.get(i);
         String title = cls;
         if (title.startsWith(pkg)) {
            title = title.substring(pkg.length());
         }
         String[] sections = title.split("\\."); // split at periods
         MenuNode newEntry;
         base = root;

         // tack on to tree
         for (int j = 0; j < sections.length; j++) {
            if (j < sections.length - 1) {
               MenuEntry newMenu = new MenuEntry(sections[j]);
               newMenu.setScrolling(scrolling);
               newMenu.setMaxRows (maxRows);
               newEntry = newMenu;
            } else {
               newEntry = new DemoEntry(cls, sections[j], args);
            }
            Node<MenuNode> newNode = new Node<MenuNode>(newEntry);
            base.addChild(newNode);
            base = newNode;
         }
      }

      // consolidate branches
      menu.consolidate();

      // merge items with single children to prevent excessive menu levels
      if (compact == 1) {

         List<Node<MenuNode>> packageRoot = root.getChildren();
         MergeTitleFunction mergeTitle = new MergeTitleFunction() {
            public String merge(String title1, String title2) {
               return title1 + "." + title2;
            }
         };

         for (Node<MenuNode> node : packageRoot) {
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

   private static void compactMenuNode(Node<MenuNode> node,
      MergeTitleFunction mergeStrings) {

      if (node.getNumberOfChildren() == 1) {
         // merge the nodes
         String thisTitle = node.getData().getTitle();
         Node<MenuNode> child = node.getChild(0); // get first child
         String thatTitle = child.getData().getTitle();

         // copy data and children
         node.setData(child.getData());
         node.getData().setTitle(mergeStrings.merge(thisTitle, thatTitle));
         for (Node<MenuNode> grandChild : child.getChildren()) {
            node.addChild(grandChild);
         }
         child.clear(); // kill the single child
         node.removeChild(child); // remove from family tree
         compactMenuNode(node, mergeStrings); // repeat now that we are
         // effectively the new child node

         // otherwise, try compacting the children
      } else {
         for (Node<MenuNode> child : node.getChildren()) {

            if (child.getNumberOfChildren() > 0) {
               compactMenuNode(child, mergeStrings);
            }
         }
      }
   }

}
