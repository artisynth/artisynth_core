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

public class RenderPropsPanel extends PropertyPanel {
   private static final long serialVersionUID = -8163801337166221686L;
   
   maspack.render.Material mat = new maspack.render.Material();
   PropTreeCell node = new PropTreeCell();

   // public RenderPropsPanelX (Iterable<? extends Property> propList)
   // {
   // set (propList);
   // }

   // public RenderPropsPanelX (PropTreeCell tree, HostList hostList)
   // {
   // myTree = tree;
   // myHostList = hostList;
   // set (EditingProperty.createProperties (
   // tree, hostList, /*isLive=*/true));
   // }

   private Property getProp (String name, Iterable<? extends Property> propList) {
      for (Property p : propList) {
         if (name.equals (p.getName())) {
            return p;
         }
      }
      return null;
   }

   private LabeledComponentBase maybeAddWidget (
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
         return null;
      }
   }

   private LabeledComponentBase maybeAddWidget (
      LinkedList<Component> widgets, String name, double min, double max,
      Iterable<? extends Property> propList) {
      Property p = getProp (name, propList);
      if (p != null) {
         LabeledComponentBase w = PropertyWidget.create (p);
         if (w == null) {
            throw new InternalErrorException (
               "Cannot create widget for property " + p.getName());
         }
         if (w instanceof IntegerField) {
            IntegerField field = (IntegerField)w;
            field.setRange ((int)min, (int)max);
         }
         else if (w instanceof DoubleField) {
            DoubleField field = (DoubleField)w;
            field.setRange (min, max);
         }
         PropertyWidget.addModeButtonOrSpace (w, p);
         putWidgetPropMap (w, p);
         widgets.add (w);
         return w;
      }
      else {
         return null;
      }
   }

   private static double INF = Double.POSITIVE_INFINITY;
   private static double MAX_I = Integer.MAX_VALUE;
   private static double MIN_I = Integer.MIN_VALUE;

   public RenderPropsPanel (Iterable<? extends Property> propList) {
      LinkedList<Object> itemList = new LinkedList<Object>();
      LinkedList<Object> sectionList;

      LinkedList<Component> widgets = new LinkedList<Component>();

      // for (Property p : propList)
      // { System.out.print (p.getName());
      // if (p instanceof InheritableProperty)
      // { System.out.println (" " + ((InheritableProperty)p).getMode());
      // }
      // else
      // { System.out.println ("");
      // }
      // }

      maybeAddWidget (widgets, "visible", propList);
      maybeAddWidget (widgets, "alpha", 0, 1, propList);
      maybeAddWidget (widgets, "shading", propList);
      maybeAddWidget (widgets, "shininess", 0, INF, propList);
      maybeAddWidget (widgets, "ambience", 0, 1, propList);

      int baseIdx = widgets.size();

      maybeAddWidget (widgets, "faceStyle", propList);
      maybeAddWidget (widgets, "faceColor", propList);
      maybeAddWidget (widgets, "backColor", propList);
      maybeAddWidget (widgets, "drawEdges", propList);
      maybeAddWidget (widgets, "textureProps", propList);
      if (widgets.size() - baseIdx > 0) {
         addSection (widgets, baseIdx, " Faces ...");
      }

      baseIdx = widgets.size();
      maybeAddWidget (widgets, "edgeWidth", propList);
      maybeAddWidget (widgets, "edgeColor", propList);
      if (widgets.size() - baseIdx > 0) {
         addSection (widgets, baseIdx, " Edges ...");
      }

      baseIdx = widgets.size();
      maybeAddWidget (widgets, "lineStyle", propList);
      maybeAddWidget (widgets, "lineColor", propList);
      maybeAddWidget (widgets, "lineWidth", 0, MAX_I, propList);
      maybeAddWidget (widgets, "lineRadius", 0, INF, propList);
      maybeAddWidget (widgets, "lineSlices", 3, MAX_I, propList);
      if (widgets.size() - baseIdx > 0) {
         addSection (widgets, baseIdx, " Lines ...");
      }

      baseIdx = widgets.size();
      maybeAddWidget (widgets, "pointStyle", propList);
      maybeAddWidget (widgets, "pointColor", propList);
      maybeAddWidget (widgets, "pointSize", 0, MAX_I, propList);
      maybeAddWidget (widgets, "pointRadius", 0, INF, propList);
      maybeAddWidget (widgets, "pointSlices", 4, MAX_I, propList);
      if (widgets.size() - baseIdx > 0) {
         addSection (widgets, baseIdx, " Points ...");
      }
      addWidgets (widgets);
   }

   protected boolean addSection (LinkedList<Component> widgets, int idx, String name) {
      JSeparator sep = new JSeparator();
      sep.setAlignmentX (JLabel.LEFT_ALIGNMENT);
      widgets.add (idx, sep);
      JLabel label = new JLabel (name);
      label.setHorizontalTextPosition (JLabel.LEFT);
      label.setAlignmentX (JLabel.LEFT_ALIGNMENT);
      GuiUtils.setItalicFont (label);
      widgets.add (idx + 1, label);
      return true;
   }

   public static void main (String[] args) {
      RenderProps props = new RenderProps();
      PropertyDialog dialog =
         new PropertyDialog ("render props test", new RenderPropsPanel (
            PropertyUtils.createProperties (props)), "OK Cancel");
      dialog.setVisible (true);
      try {
         dialog.wait();
      }
      catch (Exception e) {
      }
   }
}
