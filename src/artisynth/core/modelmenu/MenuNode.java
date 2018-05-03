/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelmenu;

import java.awt.Font;

import artisynth.core.modelmenu.DemoMenuParser.MenuType;

public abstract class MenuNode implements Comparable<MenuNode> {

   private String title;
   private String icon;
   private Font menuFont;

   public MenuNode() {
      menuFont = null;
   }
   public MenuNode(String title) {
      this.title = title;
   }
   public String getIcon() {
      return icon;
   }
   public void setIcon(String icon) {
      this.icon = icon;
   }
   public String getTitle() {
      return title;
   }
   public void setTitle(String title) {
      this.title = title;
   }
   public MenuType getType() {
      return MenuType.MENU;
   }

   @Override
   public int hashCode() {
      int hc = getType().hashCode();
      
      if (title != null) {
         hc = 31*hc + title.hashCode();
      }
      if (icon != null) {
         hc = 31*hc+icon.hashCode();
      }
      if (menuFont != null) {
         hc = 31*hc + menuFont.hashCode();
      }

      return hc;
   }
   
   protected static<T> boolean equalsWithNull(T o1, T o2) {
      if (o1 == null && o2 == null) {
         return true;
      } else if (o1 == null) {
         return false;
      }
      return o1.equals(o2);
   }
   
   @Override
   public boolean equals(Object obj) {
      if (obj == null) {
         return false;
      }
      if (obj == this) {
         return true;
      }
      
      if (!(obj.getClass() == getClass())) { 
         return false; 
      }

      MenuNode other = (MenuNode) obj;
      
      return equals(other);
   }
   
   public boolean equals(MenuNode other) {
      
      if (getType() != other.getType()) {
         return false;
      }
      if (!equalsWithNull(title, other.title)) {
         return false;
      }
      if (!equalsWithNull(icon, other.icon)) {
         return false;
      }
      if (!equalsWithNull(menuFont, other.menuFont)) {
         return false;
      }
      
      return true;
   }
   
   protected static<T extends Comparable<T>> int compareWithNull(T o1, T o2) {
      if (o1 == null) {
         if (o2 != null) {
            return -1;
         }
      } else if (o2 == null) {
         return 1;
      } else {
         return o1.compareTo(o2);
      }
      return 0;
   }

   @Override
   public int compareTo(MenuNode o) {
      
      int cmp = getType().compareTo(o.getType());
      if (cmp != 0) {
         return cmp;
      }
      
      // sort based on type, then titles, then icon, then font  
      cmp = compareWithNull(title, o.title);
      if (cmp != 0) {
         return cmp;
      }
      
      cmp = compareWithNull(icon, o.icon);
      if (cmp != 0) {
         return cmp;
      }
      
      cmp = compareWithNull(menuFont.toString(), o.menuFont.toString());

      return cmp;
   }
   
   public void setFont(Font font) {
      menuFont = font;
   }
   public Font getFont() {
      return menuFont;
   }
   
   @Override
   public String toString() {
      return title;
   }

}