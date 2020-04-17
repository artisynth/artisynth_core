/**
 * Copyright (c) 2014, by the Authors: Chad Decker (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.gui.navpanel;

import java.awt.Component;

import javax.swing.JTree;
import javax.swing.tree.DefaultTreeCellRenderer;

import artisynth.core.modelbase.*;

public class NavPanelRenderer extends DefaultTreeCellRenderer {
   private static final long serialVersionUID = 1L;

   //static String iconPath = "/artisynth/core/gui/navpanel/";

//   static final Icon myExpandUnnamedIcon = 
//      GuiUtils.loadIcon(NavigationPanel.class, "expandUnnamed.gif");

//   Icon myCollapseUnnamedIcon = 
//      GuiUtils.loadIcon(NavigationPanel.class, "collapseUnnamed.gif");

   public NavPanelRenderer() {}

   private String getBaseName (ModelComponent c) {
      if (c.getName() != null) {
	 return c.getName();
      }
      else {
	 return c.getNumber()+" {"+c.getClass().getSimpleName()+"}";
      }
   }

   static String getReferenceName (ModelComponent c) {
      CompositeComponent ancestor =
         ComponentUtils.nearestEncapsulatingAncestor(c);
      return "-> " + ComponentUtils.getPathName (ancestor, c);
   }

   private String getComponentName (ModelComponent c) {
      if (c instanceof ReferenceComp) {
         ReferenceComp rcomp = (ReferenceComp)c;
         if (rcomp.getName() != null || rcomp.getReference() == null) {
            return getBaseName (rcomp);
         }
         else {
            return getReferenceName (rcomp.getReference());
         }
      }
      else {
         return getBaseName (c);
      }
   }

   public Component getTreeCellRendererComponent(JTree tree, Object value,
	 boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus) {

      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
	    hasFocus);

      ModelComponent comp;
      if (value instanceof ModelComponent) {
	 setText(getComponentName((ModelComponent) value));
      } else if (value instanceof NavPanelNode) {
	 setText(getComponentName(((NavPanelNode) value).myComponent));
      } else if (value instanceof UnnamedPlaceHolder) {
	 if (((UnnamedPlaceHolder) value).myParent.myUnnamedVisibleP) {
	    setIcon(null);
	    setText("<<<");
	 } else {
	    setIcon(null); // myExpandUnnamedIcon);
	    setText(">>>");
	 }
      }
      return this;
   }
}
