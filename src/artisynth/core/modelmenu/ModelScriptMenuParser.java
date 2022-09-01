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
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
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

import artisynth.core.driver.ModelScriptInfo;
import artisynth.core.driver.ModelScriptInfo.InfoType;
import artisynth.core.driver.RootModelManager;
import artisynth.core.util.AliasTable;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.workspace.RootModel;

import maspack.util.InternalErrorException;
import maspack.util.FileSearchPath;
import maspack.util.ClassFinder;

/**
 * 
 * @author antonio, John E. Lloyd
 * 
 * Parses an XML menu that can be used to create a menu entries for "Models".
 * Doesn't actually build a JMenu, instead all information is constructed in a
 * Tree that holds "MenuEntry" objects with title and attribute information
 */
public class ModelScriptMenuParser {

   public static enum MenuType {
      ROOT, DIVIDER, MENU, MODEL, LABEL, SCRIPT
   }

   FileSearchPath myFileSearchPath;
   
   public static class SimpleErrorHandler implements ErrorHandler {
      public void warning (SAXParseException e) throws SAXException {
         System.out.println (e.getMessage());
      }

      public void error (SAXParseException e) throws SAXException {
         throw e;
      }

      public void fatalError (SAXParseException e) throws SAXException {
         throw e;
      }
   }

   private static String getSchemaFileName (ModelScriptMenu.Type type) {
      return type.lowerCaseName() + "menu.xsd";
   }
   
   private static String getRootTag (ModelScriptMenu.Type type) {
      return type.mixedCaseName() + "Menu";
   }
   
   public static final String ROOT_TAG_SCROLLING = "scrolling";
   public static final String ROOT_TAG_MAX_ROWS = "maxRows";
   public static final String MENU_TAG = "menu";

   // tags used by all nodes
   public static final String NODE_TAG_TITLE = "title";

   // font tags
   public static final String NODE_TAG_FONTNAME = "fontname";
   public static final String NODE_TAG_FONTSTYLE = "fontstyle";
   public static final String NODE_TAG_FONTSTYLE_BOLD = "bold";
   public static final String NODE_TAG_FONTSTYLE_ITALIC = "italic";

   // flags indicating which font characteristic were specified
   public static final int FONT_STYLE = 0x01;
   public static final int FONT_NAME = 0x02;
   public static final int FONT_SIZE = 0x04;

   // public static final String NODE_TAG_FONTSTYLE_UNDERLINE = "underline";
   // public static final String NODE_TAG_FONTSTYLE_STRIKETHROUGH =
   // "strikethrough";
   public static final String NODE_TAG_FONTSIZE = "fontsize";

   public static final String MENU_TAG_SCROLLING = "scrolling";
   public static final String MENU_TAG_MAX_ROWS = "maxRows";
   public static final String DIVIDER_TAG = "separator";
   public static final String LABEL_TAG = "label";
   public static final String MODEL_TAG = "model";
   public static final String MODEL_TAG_CLASS = "class";
   public static final String MODEL_TAG_BUILD_ARGS = "buildArgs";
   public static final String DEMOFILE_TAG = "demoFile";
   public static final String DEMOFILE_TAG_FILENAME = "file";
   public static final String PACKAGE_TAG = "package";
   public static final String PACKAGE_TAG_NAME = "name";
   public static final String PACKAGE_TAG_VIEW = "view";
   public static final String PACKAGE_TAG_VIEW_FLAT = "flat";
   public static final String PACKAGE_TAG_VIEW_HIERARCHICAL =
      "hierarchical";
   public static final String PACKAGE_TAG_SCROLLING = "scrolling";
   public static final String PACKAGE_TAG_MAX_ROWS = "maxRows";
   // 0 for not compact, 1 for compact, 2 for very compact
   public static final String PACKAGE_TAG_COMPACT = "compact"; 
      public static final String HIDDEN_TAG = "hidden";

   // tags for script menus
   public static final String SCRIPT_TAG = "script";
   public static final String SCRIPT_TAG_FILE = "file";
   public static final String SCRIPT_TAG_ARGS = "args";
   public static final String SCRIPT_FOLDER_TAG = "scriptFolder";
   public static final String SCRIPT_FOLDER_TAG_FILE = "file";
   public static final String SCRIPT_FOLDER_TAG_SCROLLING = "scrolling";
   public static final String SCRIPT_FOLDER_TAG_MAX_ROWS = "maxRows";

   public static final boolean DEFAULT_SCROLLING = false;
   public static final int DEFAULT_MAX_ROWS = 20; 
   public static final String DEFAULT_BASECLASS =
      "artisynth.core.workspace.RootModel";

   public void writeXML (
      File file, ModelScriptMenu.Type type, MenuEntry menu) {
      FileOutputStream fout = null;
      
      myFileSearchPath = ArtisynthPath.createDefaultSearchPath();
      if (type == ModelScriptMenu.Type.MODEL) {
         // allow the file directory itself to be searched for demo files
         myFileSearchPath.addDirectory (0, FileSearchPath.getParentFile(file));
      }
      try {
         File parent = file.getParentFile();
         // try to make directory structure if not already exists
         if (parent != null && !parent.exists()) {
            try {
               parent.mkdirs();
            } catch (Exception e) {}
         }
         fout = new FileOutputStream (file);
         writeXML (fout, type, menu);
      }
      catch (FileNotFoundException e) {
         System.err.println ("File not found: " + file.getAbsolutePath());
      }
      finally {
         try {
            if (fout != null) {
               fout.close();
            }
         } catch (IOException e) {
         }
      }
   }
   
   public void writeXML (
      OutputStream out, ModelScriptMenu.Type type, MenuEntry menu) {

      SchemaFactory schemaFactory =
         SchemaFactory.newInstance ("http://www.w3.org/2001/XMLSchema");
      String schemaLoc =
         ArtisynthPath.getSrcRelativePath (
            ModelScriptMenuParser.class, getSchemaFileName(type));
      File schemaLocation = new File (schemaLoc);
      Schema schema;
      try {
         schema = schemaFactory.newSchema (schemaLocation);
      }
      catch (SAXException e) {
         e.printStackTrace();
         return;
      }

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating (false);
      factory.setNamespaceAware (true);
      factory.setSchema (schema);

      DocumentBuilder builder;
      try {
         builder = factory.newDocumentBuilder();
      }
      catch (ParserConfigurationException e) {
         e.printStackTrace();
         return;
      }
      builder.setErrorHandler (new SimpleErrorHandler());
      
      Document dom = builder.newDocument();
      buildDocument (dom, type, menu);
      
      // Use a Transformer for output
      TransformerFactory tFactory = TransformerFactory.newInstance();
      Transformer transformer;
      try {
         transformer = tFactory.newTransformer();
         transformer.setOutputProperty (OutputKeys.INDENT, "yes");
         transformer.setOutputProperty ("{http://xml.apache.org/xslt}indent-amount", "2");
      }
      catch (TransformerConfigurationException e) {
         e.printStackTrace();
         return;
      }

      DOMSource source = new DOMSource (dom);
      StreamResult result = new StreamResult (out);
      try {
         transformer.transform (source, result);
      }
      catch (TransformerException e) {
         e.printStackTrace();
         return;
      }
   }
   
   private void buildDocument (
      Document dom, ModelScriptMenu.Type type, MenuEntry menu) {
      
      Element modelMenu = dom.createElement (getRootTag(type));
      modelMenu.setAttribute ("xmlns", "https://www.artisynth.org");
      modelMenu.setAttribute ("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
      modelMenu.setAttribute ("xsi:schemaLocation", "https://www.artisynth.org src/artisynth/core/modelmenu/modelmenu.xsd");
      
      if (menu.isScrolling() != DEFAULT_SCROLLING) {
         modelMenu.setAttribute (
            ROOT_TAG_SCROLLING, Boolean.toString (menu.isScrolling()));
      }
      if (menu.getMaxRows() != DEFAULT_MAX_ROWS) {
         modelMenu.setAttribute (
            ROOT_TAG_MAX_ROWS, Integer.toString (menu.getMaxRows()));
      }
      dom.appendChild (modelMenu);
      for (MenuNode child : menu.getChildren()) {
         buildElement (dom, modelMenu, child);
      }
   }
   
   private void buildElement (
      Document dom, Element parent, MenuNode node) {
      
      MenuType type = node.getType();
      
      // recursively add menu
      Element el = null;
      switch (type) {
         case DIVIDER:
            el = dom.createElement (DIVIDER_TAG);
            break;
         case LABEL:
            el = buildLabel (dom, (LabelEntry)node);
            break;
         case MENU:
            if (node instanceof PackageEntry) {
               el = buildPackage (dom, (PackageEntry)node);
            }
            else if (node instanceof DemoFileEntry) {
               el = buildDemoFile (dom, (DemoFileEntry)node);
            }
            else if (node instanceof ScriptFolderEntry) {
               el = buildScriptFolder (dom, (ScriptFolderEntry)node);
            }
            else {
               MenuEntry menuEntry = (MenuEntry)node;
               el = buildMenu (dom, menuEntry);
               for (MenuNode child : menuEntry.getChildren()) {
                  buildElement (dom, el, child);
               }
            }
            break;
         case MODEL:
            el = buildModel (dom, (ModelEntry)node);
            break;
         case SCRIPT:
            el = buildScript (dom, (ScriptEntry)node);
            break;
         case ROOT:
            break;
         default:
            break;
       
      }  
      
      if (parent != null && el != null) {
         parent.appendChild (el);
      }
   }
   
//   public static MenuEntry parseXML (
//      String filename, RootModelManager rmm) throws IOException,
//   ParserConfigurationException, SAXException {
//      return parseXML (new File(filename), rmm);
//   }
//   
   public MenuEntry parseXML (
      File file, ModelScriptMenu.Type type, RootModelManager rmm) 
      throws IOException, ParserConfigurationException, SAXException {

      // get path of filename
      String filename = file.getAbsolutePath();
      
      myFileSearchPath = ArtisynthPath.createDefaultSearchPath();
      if (type == ModelScriptMenu.Type.MODEL) {
         // allow the file directory itself to be searched for demo files
         myFileSearchPath.addDirectory (0, FileSearchPath.getParentFile(file));
      }
      
      SchemaFactory schemaFactory =
         SchemaFactory.newInstance ("http://www.w3.org/2001/XMLSchema");
      String schemaLoc = ArtisynthPath.getSrcRelativePath (
         ModelScriptMenuParser.class, getSchemaFileName(type));

      DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
      factory.setValidating (false);
      factory.setNamespaceAware (true);
      
      if (schemaLoc != null) {
         File schemaLocation = new File (schemaLoc);
         Schema schema = schemaFactory.newSchema (schemaLocation);
         factory.setSchema (schema);
      }

      DocumentBuilder builder = factory.newDocumentBuilder();
      builder.setErrorHandler (new SimpleErrorHandler());
      Document dom = builder.parse (filename);
      return parseDocument (dom, type, rmm);
   }

   public MenuEntry parseSimpleFile (File file) {
      DemoFileEntry dentry = new DemoFileEntry();

      AliasTable table = dentry.readAliasTable(file);
      if (table == null) {
         return null;
      }
      MenuEntry menu = new MenuEntry ("Models");
      // just add the child nodes of dentry to the menu
      dentry.createChildNodes(table);
      for (MenuNode node : dentry.getChildren()) {
         menu.addChild (node);
      }
      return menu;
   }
   
   /**
    * Used for debugging and testing only
    */
   private static void printMenu (MenuNode node, int level) {

      String prefix = "";
      for (int i=0; i<level; i++) {
         prefix += "  ";
      }
      String name;
      if (node instanceof SeparatorEntry) {
         name = "DIVIDER " + node.hashCode();
      }
      else if (node instanceof LabelEntry) {
         name = "LABEL " + node.getTitle();
      }
      else {
         name = node.getTitle();
      }
      System.out.print (prefix + name);
      if (node instanceof MenuEntry && ( (MenuEntry)node).isExpandable()) {
         System.out.println ("*");
      }
      else {
         System.out.println ("");
      }
      if (node instanceof MenuEntry) {
         MenuEntry menuEntry = (MenuEntry)node;
         for (MenuNode child : menuEntry.getChildren()) {
            printMenu (child, level+1);
         }
      }
   }

   private MenuEntry parseDocument (
      Document dom, ModelScriptMenu.Type type,
      RootModelManager rmm) {

      MenuEntry menu = new MenuEntry (type.mixedCaseName()+"s");
      Element docEle = dom.getDocumentElement();

      NodeList nl = docEle.getChildNodes();
      String scrolling = docEle.getAttribute (ROOT_TAG_SCROLLING);
      menu.setScrolling (parseBoolean (scrolling));
      String maxRows = docEle.getAttribute (ROOT_TAG_MAX_ROWS);
      menu.setMaxRows (Integer.parseInt (maxRows));
      
      for (int i = 0; i < nl.getLength(); i++) {
         if (nl.item (i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
            Element el = (Element)nl.item (i);
            parseElement (menu, el, rmm);
         } // end if element
      } // end loop

      //printMenu (menu, 0);

      return menu;
   }

   private void parseElement (
      MenuEntry menuEntry, Element el, RootModelManager rmm) {

      // recursively add menu
      if (el.getNodeName().equals (MENU_TAG)) {
         MenuEntry newMenu = parseMenu (el);
         if (newMenu != null) {

            menuEntry.addChild (newMenu);

            // add all children
            NodeList nl = el.getChildNodes();
            for (int i = 0; i < nl.getLength(); i++) {
               if (nl.item (i).getNodeType() == org.w3c.dom.Node.ELEMENT_NODE) {
                  Element nEl = (Element)nl.item (i);
                  parseElement (newMenu, nEl, rmm);
               }
            } // end looping through children

         }

         // add a divider
      }
      else if (el.getNodeName().equals (DIVIDER_TAG)) {
         SeparatorEntry div = new SeparatorEntry();
         menuEntry.addChild (div);

         // add a text label
      }
      else if (el.getNodeName().equals (LABEL_TAG)) {
         LabelEntry label = parseLabel (el);
         menuEntry.addChild (label);

         // add a model
      }
      else if (el.getNodeName().equals (MODEL_TAG)) {
         ModelEntry model = parseModel (el);
         if (model != null) {
            menuEntry.addChild (model);
         }

         // parse an entire demo file
      }
      else if (el.getNodeName().equals (DEMOFILE_TAG)) {
         DemoFileEntry demoFileEntry = parseDemoFile (el);
         if (demoFileEntry != null) {
            menuEntry.addChild (demoFileEntry);
         }
         
         // parse an entire package
      }
      else if (el.getNodeName().equals (PACKAGE_TAG)) {
         MenuNode root = parsePackage (el, rmm);
         if (root != null) {
            menuEntry.addChild (root);
         }
      }
      else if (el.getNodeName().equals (SCRIPT_TAG)) {
         MenuNode root = parseScript (el);
         if (root != null) {
            menuEntry.addChild (root);
         }
      }
      else if (el.getNodeName().equals (SCRIPT_FOLDER_TAG)) {
         MenuNode root = parseScriptFolder (el);
         if (root != null) {
            menuEntry.addChild (root);
         }
      }
      else if (el.getNodeName().equals (HIDDEN_TAG)) {
         // skip entry entirely
      }
      else {
         System.out.println ("WARNING: unknown tag <" + el.getNodeName() + ">");
      } // end checking type

   }
   
   private static void maybeAddFontAttributes (Element el, MenuNode node) {
      int spec = node.getFontSpec();
      if (spec != 0) {
         Font font = node.getFont();         
         if ( (spec & FONT_NAME) != 0) {
            el.setAttribute (NODE_TAG_FONTNAME, font.getName());
         }
         if ( (spec & FONT_SIZE) != 0) {
            el.setAttribute (NODE_TAG_FONTSIZE, Integer.toString(font.getSize()));
         }
         if ( (spec & FONT_STYLE) != 0) {
            String style = "";
            if (font.isBold()) {
               style += NODE_TAG_FONTSTYLE_BOLD;
            }
            if (font.isItalic()) {
               style += NODE_TAG_FONTSTYLE_ITALIC;
            }
            el.setAttribute (NODE_TAG_FONTSTYLE, style);
         }
      }
   }
   
   private static boolean parseBoolean (String str) {
      boolean scrolling = false;
      if (str != null && !"".equals (str)) {
         if ("true".equalsIgnoreCase (str) || "1".equals (str)) {
            scrolling = true;
         }
      }
      return scrolling;
   }

   // get font information from element
   private Font parseFont (Element el, MenuNode node) {
      
      int fontSpec = 0;
      if (el.hasAttribute (NODE_TAG_FONTNAME)) {
         fontSpec |= FONT_NAME;
      }
      if (el.hasAttribute (NODE_TAG_FONTSIZE)) {
         fontSpec |= FONT_SIZE;
      }
      if (el.hasAttribute (NODE_TAG_FONTSTYLE)) {
         fontSpec |= FONT_STYLE;
      }
      if (fontSpec == 0) {
         return null;
      }
      
      Font defaultFont = UIManager.getFont ("Menu.font");
      int size = defaultFont.getSize();
      String name = defaultFont.getName();
      int style = Font.PLAIN;

      // = new Font (defaultFont.getName(), defaultFont.getStyle(),
      // defaultFont.getSize());

      String fontName = el.getAttribute (NODE_TAG_FONTNAME);
      String fontSize = el.getAttribute (NODE_TAG_FONTSIZE);
      String fontStyle = el.getAttribute (NODE_TAG_FONTSTYLE);
      
      if (!fontName.equals ("")) {
         name = fontName;
      }
      if (!fontSize.equals ("")) {
         size = Integer.parseInt (fontSize);
      }
      if (!fontStyle.equals ("")) {
         // we need to check if they actually put something
         if (fontStyle.contains (NODE_TAG_FONTSTYLE_BOLD)) {
            style |= Font.BOLD;
         }
         if (fontStyle.contains (NODE_TAG_FONTSTYLE_ITALIC)) {
            style |= Font.ITALIC;
         }
      }
      Font font = new Font (name, style, size);
      node.setFont (font);
      node.setFontSpec (fontSpec);
      return font;
   }

   private void buildNode (Element el, MenuNode node) {
      if (node.getTitle() != null) {
         el.setAttribute (NODE_TAG_TITLE, node.getTitle());
      }
      maybeAddFontAttributes (el, node);
   }

   private void parseNode (
      Element el, MenuNode node) {

      String text = el.getAttribute (NODE_TAG_TITLE);
      if (!text.equals ("")) {
         node.setTitle (text);
      }
      parseFont (el, node);
   }

   private Element buildMenu (Document dom, MenuEntry entry) {
      Element el = dom.createElement (MENU_TAG);
      buildNode (el, entry);
      boolean scrolling = entry.isScrolling();
      if (scrolling != DEFAULT_SCROLLING) {
         el.setAttribute (MENU_TAG_SCROLLING, Boolean.toString (scrolling));
      }
      int maxRows = entry.getMaxRows();
      if (maxRows != DEFAULT_MAX_ROWS) {
         el.setAttribute (MENU_TAG_MAX_ROWS, Integer.toString (maxRows));
      }
      return el;
   }
   
   private MenuEntry parseMenu (Element el) {

      MenuEntry menu = new MenuEntry();
      parseNode (el, menu);

      if (menu.getTitle() == null) {
         menu.setTitle ("<unknown>");
      }
      String scrollingStr = el.getAttribute (MENU_TAG_SCROLLING);
      boolean scrolling = parseBoolean (scrollingStr);
      menu.setScrolling (scrolling);
      
      String maxRowsStr =el.getAttribute (MENU_TAG_MAX_ROWS);
      int maxRows = Integer.parseInt (maxRowsStr);
      menu.setMaxRows (maxRows);

      return menu;
   }

   private Element buildModel (Document dom, ModelEntry model) {

      Element el = dom.createElement (MODEL_TAG);
      buildNode (el, model);

      String file = model.getModel().getClassNameOrFile();
      el.setAttribute (MODEL_TAG_CLASS, file);

      String[] args = model.getModel().getArgs();
      if (args != null && args.length > 0) {
         // combine into single string
         String argsStr = ModelScriptInfo.mergeArgs (args);
         el.setAttribute (MODEL_TAG_BUILD_ARGS, argsStr);
      }
      return el;
   }
   
   private ModelEntry parseModel (Element el) {

      ModelEntry model = new ModelEntry();      
      parseNode (el, model);

      String classname = el.getAttribute (MODEL_TAG_CLASS);
      String argsStr = el.getAttribute (MODEL_TAG_BUILD_ARGS);

      // must be non-null
      if (classname.equals ("")) {
         System.out.println (
            "WARNING: class not specified in model menu entry; ignoring");
         return null;
      }
      // if no name supplied, use the class
      String name = model.getTitle();
      if (name == null) {
         model.setTitle (classname);
         name = classname;
      }
      String[] args = null;
      // separate model args to a list of strings
      if (argsStr != null && !"".equals (argsStr)) {
         args = ModelScriptInfo.splitArgs (argsStr);
         model.setBuildArgs (argsStr);
      }
      if (!classIsRootModel (classname)) {
         System.out.println (
            "WARNING: class '" + classname +
            "' in model menu entry not found or not a RootModel; ignoring");
         return null;
      }
      model.setModel (new ModelScriptInfo(InfoType.CLASS, classname, name, args));
      return model;
   }


   public static boolean classIsRootModel (String classname) {
      Class<?> cls = null;
      try {
         cls = ClassFinder.forName(classname,false);
      }
      catch (Exception e) {
         // ignore if not found
      }
      if (cls == null) {
         return false;
      }      
      if (!RootModel.class.isAssignableFrom (cls)) {
         return false;
      }
      return true;
   }
   
   private Element buildPackage (Document dom, PackageEntry entry) {

      Element el = dom.createElement (PACKAGE_TAG);

      buildNode (el, entry);

      el.setAttribute (PACKAGE_TAG_NAME, entry.getPackageName());
      if (entry.getFlatView()) {
         el.setAttribute (PACKAGE_TAG_VIEW, PACKAGE_TAG_VIEW_FLAT);
      }
      int compact = entry.getCompact();
      if (compact != 0) {
         el.setAttribute (PACKAGE_TAG_COMPACT, Integer.toString (compact));
      }
      boolean scrolling = entry.isScrolling();
      if (scrolling != DEFAULT_SCROLLING) {
         el.setAttribute (PACKAGE_TAG_SCROLLING, Boolean.toString (scrolling));
      }
      int maxRows = entry.getMaxRows();
      if (maxRows != DEFAULT_MAX_ROWS) {
         el.setAttribute (PACKAGE_TAG_MAX_ROWS, Integer.toString (maxRows));
      }
      return el;
   }
   
   private PackageEntry parsePackage (
      Element el, RootModelManager rmm) {

      PackageEntry pentry = new PackageEntry();
      parseNode (el, pentry);

      String view = el.getAttribute (PACKAGE_TAG_VIEW);
      if (view.equals (PACKAGE_TAG_VIEW_FLAT)) {
         pentry.setFlatView (true);
      }

      String pkgName = el.getAttribute (PACKAGE_TAG_NAME);
      pentry.setPackageName (pkgName);

      String compactStr = el.getAttribute (PACKAGE_TAG_COMPACT);
      int compact = 0;
      if (!compactStr.equals ("")) {
         compact = Integer.parseInt (compactStr);
      }
      pentry.setCompact (compact);

      String scrollingStr = el.getAttribute (PACKAGE_TAG_SCROLLING);
      boolean scrolling = parseBoolean (scrollingStr);
      pentry.setScrolling (scrolling);

      String maxRowsStr = el.getAttribute (PACKAGE_TAG_MAX_ROWS);
      int maxRows = Integer.parseInt (maxRowsStr);
      pentry.setMaxRows (maxRows);

      pentry.createChildNodes (rmm, /*useCache=*/true);
      return pentry;
   }

   private Element buildDemoFile (Document dom, DemoFileEntry entry) {

      Element el = dom.createElement (DEMOFILE_TAG);
      buildNode (el, entry);

      if (entry.getFile() != null) {
         el.setAttribute (
            DEMOFILE_TAG_FILENAME, myFileSearchPath.findPath(entry.getFile()));
      }
      boolean scrolling = entry.isScrolling();
      if (scrolling != DEFAULT_SCROLLING) {
         el.setAttribute (MENU_TAG_SCROLLING, Boolean.toString (scrolling));
      }
      int maxRows = entry.getMaxRows();
      if (maxRows != DEFAULT_MAX_ROWS) {
         el.setAttribute (MENU_TAG_MAX_ROWS, Integer.toString (maxRows));
      }
      return el;
   }
   
   private DemoFileEntry parseDemoFile (
      Element el) {

      DemoFileEntry dentry = new DemoFileEntry();
      parseNode (el, dentry);
      
      String filename = el.getAttribute (DEMOFILE_TAG_FILENAME);
      File fullFile = myFileSearchPath.findFile (filename);
      if (fullFile == null) {
         System.out.println (
            "WARNING: demoFile: '"+filename+"' not found; ignoring");
         return null;
      }
      dentry.setFile (fullFile);

      String scrollingStr = el.getAttribute (MENU_TAG_SCROLLING);
      boolean scrolling = parseBoolean (scrollingStr);
      dentry.setScrolling (scrolling);
      
      String maxRowsStr =el.getAttribute (MENU_TAG_MAX_ROWS);
      int maxRows = Integer.parseInt (maxRowsStr);
      dentry.setMaxRows (maxRows);
      AliasTable table = dentry.readAliasTable(fullFile);
      if (table == null) {
         return null;
      }
      dentry.createChildNodes(table);
      return dentry;
   }

   private Element buildScript (Document dom, ScriptEntry script) {

      Element el = dom.createElement (SCRIPT_TAG);
      buildNode (el, script);

      String filepath = script.getScriptInfo().getClassNameOrFile();
      el.setAttribute (
         SCRIPT_TAG_FILE, myFileSearchPath.findPath (new File(filepath)));

      String[] args = script.getScriptInfo().getArgs();
      if (args != null && args.length > 0) {
         // combine into single string
         String argsStr = ModelScriptInfo.mergeArgs (args);
         el.setAttribute (SCRIPT_TAG_ARGS, argsStr);
      }
      return el;
   }
   
   private ScriptEntry parseScript (Element el) {

      ScriptEntry script = new ScriptEntry();      
      parseNode (el, script);

      String fileName = el.getAttribute (SCRIPT_TAG_FILE);
      String argsStr = el.getAttribute (SCRIPT_TAG_ARGS);

      // must be non-null
      if (fileName.equals ("")) {
         System.out.println (
            "WARNING: empty fileName in script menu entry; ignoring");
         return null;
      }
      File file = myFileSearchPath.findFile (fileName);
      if (file == null || !file.canRead()) {
         System.out.println (
            "WARNING: file '" + fileName +
            "' in script menu entry not found or not readable; ignoring");
         return null;
      }      
      // if no title supplied, use the class
      String title = script.getTitle();
      if (title == null) {
         title = ScriptEntry.getDefaultScriptTitle(file);
         script.setTitle (title);
      }
      String[] args = null;
      // separate script args to a list of strings
      if (argsStr != null && !"".equals (argsStr)) {
         args = ModelScriptInfo.splitArgs (argsStr);
         script.setArgs (argsStr);
      }
      script.setScriptInfo (
         new ModelScriptInfo(InfoType.SCRIPT, file.getAbsolutePath(), title, args));
      return script;
   }

   private Element buildScriptFolder (
      Document dom, ScriptFolderEntry entry) {

      Element el = dom.createElement (SCRIPT_FOLDER_TAG);
      buildNode (el, entry);

      File dir = entry.getFile();
      if (dir != null) {
         String path;
         if (dir.getName().equals (".")) {
            path = ".";
         }
         else {
            path = myFileSearchPath.findPath (dir);
         }
         el.setAttribute (SCRIPT_FOLDER_TAG_FILE, path);
      }
      boolean scrolling = entry.isScrolling();
      if (scrolling != DEFAULT_SCROLLING) {
         el.setAttribute (SCRIPT_FOLDER_TAG_SCROLLING, Boolean.toString (scrolling));
      }
      int maxRows = entry.getMaxRows();
      if (maxRows != DEFAULT_MAX_ROWS) {
         el.setAttribute (SCRIPT_FOLDER_TAG_MAX_ROWS, Integer.toString (maxRows));
      }
      return el;
   }
   
   private ScriptFolderEntry parseScriptFolder (
      Element el) {

      ScriptFolderEntry dentry = new ScriptFolderEntry();
      parseNode (el, dentry);

      String dirName = el.getAttribute (SCRIPT_FOLDER_TAG_FILE);
      File dir;
      if (!dirName.equals (".")) {
         dir = myFileSearchPath.findFile (dirName);
         if (dir == null) {
            System.out.println (
               "WARNING: script folder '"+dirName+"' not found; ignoring");
            return null;
         }
      }
      else {
         dir = new File (".");
      }
      if (!dir.isDirectory()) {
         System.out.println (
            "WARNING: script folder '"+dirName+"' not a folder; ignoring");
         return null;
      }
      dentry.setFile (dir);

      String scrollingStr = el.getAttribute (SCRIPT_FOLDER_TAG_SCROLLING);
      boolean scrolling = parseBoolean (scrollingStr);
      dentry.setScrolling (scrolling);

      String maxRowsStr = el.getAttribute (SCRIPT_FOLDER_TAG_MAX_ROWS);
      int maxRows = Integer.parseInt (maxRowsStr);
      dentry.setMaxRows (maxRows);

      dentry.createChildNodes();
      return dentry;
   }

   private Element buildLabel (Document dom, LabelEntry label) {
      Element el = dom.createElement (LABEL_TAG);
      buildNode (el, label);
      return el;
   }
   
   private LabelEntry parseLabel (Element el) {
      LabelEntry label = new LabelEntry();
      parseNode (el, label);
      return label;
   }

   static boolean omitFromMenu (Class<?> clazz) {
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
}
