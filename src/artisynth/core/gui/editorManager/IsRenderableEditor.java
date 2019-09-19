/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package artisynth.core.gui.editorManager;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;

import javax.swing.JOptionPane;

import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.render.IsRenderable;
import maspack.widgets.GuiUtils;
import maspack.widgets.PropertyDialog;
import artisynth.core.driver.Main;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.renderables.IsRenderableHolder;

public class IsRenderableEditor extends EditorBase {

   private static String glEditCmd = "Edit IsRenderable's properties ...";
   
   public IsRenderableEditor(Main main, EditorManager editManager) {
      super(main, editManager);
   }

   public void addActions (EditActionMap actions, SelectionManager selManager) {
      LinkedList<ModelComponent> selection = selManager.getCurrentSelection();

      if (containsMultipleSelection (selection, IsRenderableHolder.class)) {
         actions.add (this, glEditCmd);
      }
   }

   public void applyAction (
      String actionCommand, LinkedList<ModelComponent> selection,
      Rectangle popupBounds) {
      
      if (glEditCmd.equals(actionCommand)) {
         ArrayList<HasProperties> contained = new ArrayList<HasProperties>(selection.size());
         for (ModelComponent mc : selection) {
            if (mc instanceof IsRenderableHolder) {
               IsRenderableHolder holder = (IsRenderableHolder)mc;
               IsRenderable glr = holder.getRenderable();
               if (glr instanceof HasProperties) {
                  contained.add((HasProperties)glr);
               }
            }
         }
         
         if (contained.size() > 0) {
            createPropertyDialog(contained, true, popupBounds);
         } else {
            GuiUtils.showNotice (null, "No properties for selected components");
         }
      }
   }
   
   public void createPropertyDialog (
      Collection<? extends HasProperties> selectedItems, boolean locateClose,
      Rectangle popupBounds) {

      HostList hostList = new HostList (selectedItems);
      PropTreeCell tree =
         hostList.commonProperties (null, /* allowReadonly= */true);
      if (tree.numChildren() == 0) {
         GuiUtils.showNotice (
            null, "No common properties for selected components");
      }
      else {
         PropertyDialog propDialog =
            new PropertyDialog (
               "Edit properties", tree, hostList, "OK Cancel LiveUpdate");
         propDialog.setScrollable (true);
         
         if (locateClose) {
            GuiUtils.locateRelative (
               propDialog, popupBounds, 0.5, 0.5, 0, 0.5);
         }
         else {
            propDialog.locateRight (myMain.getFrame());
         }
         //propDialog.setSynchronizeObject (myMain.getRootModel());
         myMain.registerWindow (propDialog);
         propDialog.setTitle (
            "Properties for selected IsRenderables");
         propDialog.setVisible (true);
      }
   }

}
