/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.awt.Font;
import java.awt.Component;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.AbstractButton;

import java.net.URL;
import java.util.ArrayList;

import javax.swing.UIManager;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.modelmenu.ModelScriptMenuParser.MenuType;
import maspack.properties.*;

public abstract class MenuNode implements Comparable<MenuNode>, HasProperties {

   public enum FontStyle {
      PLAIN (Font.PLAIN),      
      BOLD (Font.BOLD),
      ITALIC (Font.ITALIC),
      BOLD_ITALIC (Font.BOLD|Font.ITALIC);

      public int code;

      FontStyle (int code) {
         this.code = code;
      }
   };

   public static final String DEFAULT_TITLE = null;
   String myTitle = DEFAULT_TITLE;

   public static final String DEFAULT_ICON = null;
   private String myIcon = DEFAULT_ICON;

   public static Font myMenuDefaultFont = UIManager.getFont ("Menu.font");
   public static Font myDefaultFont;
   static {
      Font menufont = UIManager.getFont ("Menu.font");
      myDefaultFont = new Font (
         menufont.getName(), menufont.getStyle(), menufont.getSize());
   }
   protected Font myFont = myDefaultFont;

   public static final String DEFAULT_FONT_NAME = myDefaultFont.getName();
   public static final FontStyle DEFAULT_FONT_STYLE =
      getFontStyle (myDefaultFont);
   public static final int DEFAULT_FONT_SIZE = myDefaultFont.getSize();

   private int myFontSpec; // flags indicating which font attributes were specified

   protected MenuEntry parent;
   // most recent index within the parent
   protected int myIndex; 
   
   public static PropertyList myProps =
      new PropertyList (MenuNode.class);

   static {
      myProps.add (
         "title", "Title for the menu item",  DEFAULT_TITLE);
      myProps.add (
         "icon", "URI for an icon", DEFAULT_ICON);
      myProps.add (
         "fontName", "Name of the text font", DEFAULT_FONT_NAME);
      myProps.add (
         "fontStyle", "Style for the text font", DEFAULT_FONT_STYLE);
      myProps.add (
         "fontSize",
         "Size of the text font", DEFAULT_FONT_SIZE, "NS [0,inf]");
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   public MenuNode() {
      myFont = myDefaultFont;
   }

   public MenuNode (String title) {
      this();
      myTitle = title;
   }

   public String getTitle() {
      return myTitle;
   }

   public boolean titleIsEmpty() {
      return myTitle == null || myTitle.length() == 0;
   }

   public void setTitle (String title) {
      myTitle = title;
   }

   public String getIcon() {
      return myIcon;
   }

   public void setIcon (String icon) {
      myIcon = icon;
   }

   public String getFontName() {
      return myFont.getName();
   }

   public void setFontName (String fontName) {
      if (fontName == null || fontName.equals("")) {
         fontName = DEFAULT_FONT_NAME;
      }
      if (!fontName.equals(getFontName())) {
         Font newFont;
         if (fontName.equals (DEFAULT_FONT_NAME) &&
             getFontStyle() == DEFAULT_FONT_STYLE &&
             getFontSize() == DEFAULT_FONT_SIZE) {
            newFont = myDefaultFont;
         }
         else {
            newFont = new Font (fontName, getFontStyle().code, getFontSize());
         }
         setFont (newFont);
      }
   }

   public FontStyle getFontStyle() {
      return getFontStyle (myFont);
   }

   public static FontStyle getFontStyle(Font font) {
      int code = font.getStyle();
      if ((code & Font.BOLD) != 0) {
         if ((code & Font.ITALIC) != 0) {
            return FontStyle.BOLD_ITALIC;
         }
         else {
            return FontStyle.BOLD;
         }
      }
      else if ((code & Font.ITALIC) != 0) {
         return FontStyle.ITALIC;
      }
      else {
         return FontStyle.PLAIN;
      }
   }

   public void setFontStyle (FontStyle style) {
      if (style != getFontStyle()) {
         Font newFont;
         if (getFontName().equals (DEFAULT_FONT_NAME) &&
             style == DEFAULT_FONT_STYLE &&
             getFontSize() == DEFAULT_FONT_SIZE) {
            newFont = myDefaultFont;
         }
         else {
            newFont = new Font (getFontName(), style.code, getFontSize());
         }
         setFont (newFont);
      }
   }

   public int getFontSize() {
      return myFont.getSize();
   }

   public void setFontSize (int size) {
      if (size <= 0) {
         size = DEFAULT_FONT_SIZE;
      }
      if (size != getFontSize()) {
         Font newFont;         
         if (getFontName().equals (DEFAULT_FONT_NAME) &&
             getFontStyle() == DEFAULT_FONT_STYLE &&
             size == DEFAULT_FONT_SIZE) {
            newFont = myDefaultFont;
         }
         else {
            newFont = new Font (getFontName(), getFontStyle().code, size);
         }
         setFont (newFont);
      }
   }

   public MenuType getType() {
      return MenuType.MENU;
   }
   
   protected static<T> boolean equalsWithNull (T o1, T o2) {
      if (o1 == null && o2 == null) {
         return true;
      }
      else if (o1 == null) {
         return false;
      }
      return o1.equals (o2);
   }

   public boolean equals (MenuNode other) {
      
      if (getType() != other.getType()) {
         return false;
      }
      if (!equalsWithNull (getTitle(), other.getTitle())) {
         return false;
      }
      if (!equalsWithNull (myIcon, other.myIcon)) {
         return false;
      }
      if (!equalsWithNull (myFont, other.myFont)) {
         return false;
      }
      
      return true;
   }
   
   protected static<T extends Comparable<T>> int compareWithNull (T o1, T o2) {
      if (o1 == null) {
         if (o2 != null) {
            return -1;
         }
      } else if (o2 == null) {
         return 1;
      } else {
         return o1.compareTo (o2);
      }
      return 0;
   }

   @Override
   public int compareTo (MenuNode o) {
      
      int cmp = getType().compareTo (o.getType());
      if (cmp != 0) {
         return cmp;
      }
      
      // sort based on type, then titles, then icon, then font  
      cmp = compareWithNull (myTitle, o.myTitle);
      if (cmp != 0) {
         return cmp;
      }
      
      cmp = compareWithNull (myIcon, o.myIcon);
      if (cmp != 0) {
         return cmp;
      }
      
      String fontStr = (myFont != null) ? myFont.toString() : null;
      String ofontStr = (o.myFont != null) ? o.myFont.toString() : null;

      cmp = compareWithNull (fontStr, ofontStr);

      return cmp;
   }
   
   protected void updateFontSpec(Font font) {
      // set the font spec to flag settings that differ from the default
      Font defaultFont = UIManager.getFont ("Menu.font");
      int defaultSize = defaultFont.getSize();
      String defaultName = defaultFont.getName();
      int defaultStyle = Font.PLAIN;

      int spec = 0;
      if (font.getStyle() != defaultStyle) {
         spec |= ModelScriptMenuParser.FONT_STYLE;
      }
      if (font.getSize() != defaultSize) {
         spec |= ModelScriptMenuParser.FONT_SIZE;
      }
      if (!font.getName().equals (defaultName)) {
         spec |= ModelScriptMenuParser.FONT_NAME;
      }
      myFontSpec = spec;      
   }
   
   public void setFont (Font font) {
      if (font != myFont) {
         updateFontSpec (font);
         myFont = font;
      }
   }

   public Font getFont() {
      return myFont;
   }
   
   public void setFontSpec (int spec) {
      myFontSpec = spec;
   }
   
   public int getFontSpec() {
      return myFontSpec;
   }
   
   @Override
   public String toString() {
      return myTitle;
   }
   
   public MenuEntry getParent() {
      return parent;
   }

   public int getIndex() {
      return myIndex;
   }

   public void set (MenuNode node) {
      setTitle (node.getTitle());
      setIcon (node.getIcon());
      setFont (node.getFont());
   }

   protected boolean stringEquals (String s0, String s1) {
      if (s0 != null) {
         if (s1 == null) {
            return false;
         }
         else {
            return s0.equals (s1);
         }
      }
      else {
         return s1 == null;
      }
   }

   protected void setLabelAttributes (AbstractButton comp) {
      comp.setText (getTitle());
      if (getIcon() != null) {
         URL iconFile = ArtisynthPath.findResource (getIcon());
         comp.setIcon (new ImageIcon (iconFile));
      }
      comp.setFont (getFont());
   }

   protected void updateLabelAttributes (AbstractButton comp) {
      if (!stringEquals (getTitle(), comp.getText())) {
         comp.setText (getTitle());
      }
      if (getFont() != comp.getFont()) {
         comp.setFont (getFont());
      }
   }

   public int numChildren() {
      return 0;
   }
   

   public abstract Component getComponent();

   public abstract Component updateComponent (ModelScriptMenu modelMenu);
}
