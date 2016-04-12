/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import maspack.render.*;

import java.awt.*;

import javax.swing.*;

import java.util.*;

import maspack.properties.*;
import maspack.util.InternalErrorException;

public class RenderMappingsPanel extends PropertyPanel {
   private static final long serialVersionUID = -8163801337166221686L;
   
   PropTreeCell node = new PropTreeCell();

   private Property getProp (String name, Iterable<? extends Property> propList) {
      for (Property p : propList) {
         if (name.equals (p.getName())) {
            return p;
         }
      }
      return null;
   }

   private LabeledComponentBase addWidget (
      LinkedList<Component> widgets, String name,
      Iterable<? extends Property> propList) {
      Property p = getProp (name, propList);
      if (p != null) {
         LabeledComponentBase w = PropertyWidget.create (p);
         if (w == null) {
            throw new InternalErrorException (
               "Cannot create widget for property " + p.getName());
         }
         PropertyWidget.addModeButtonOrSpace (w, p);
         putWidgetPropMap (w, p);
         widgets.add (w);
         return w;
      }
      else {
         throw new InternalErrorException (
            "Property '"+name+"' nor found");
      }
   }

   public RenderMappingsPanel (Iterable<? extends Property> propList) {
      LinkedList<Component> widgets = new LinkedList<Component>();

      addWidget (widgets, "textureMapping", propList);
      addWidget (widgets, "normalMapping", propList);
      addWidget (widgets, "bumpMapping", propList);

      addWidgets (widgets);
   }

   public static void main (String[] args) {
      RenderMappings mappings = new RenderMappings();
      PropertyDialog dialog =
         new PropertyDialog ("render mappings test", new RenderMappingsPanel (
            PropertyUtils.createProperties (mappings)), "OK Cancel");
      dialog.setVisible (true);
      try {
         dialog.wait();
      }
      catch (Exception e) {
      }
   }
}
