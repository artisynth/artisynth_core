/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.util.*;
import java.awt.*;
import java.awt.event.*;

import javax.swing.event.*;
import javax.swing.*;

import maspack.properties.*;
import maspack.render.*;
import maspack.render.GL.GLViewer;
import maspack.util.*;

/**
 * This class implements a number of commands that are convenient for
 * controlling a GLViewer, particularly from a pull-down menu.
 */
public class ViewerPopupManager implements ActionListener {

   protected GLViewer myViewer;
   private ArrayList<DialogHandle> myDialogHandles =
      new ArrayList<DialogHandle>();

   public ViewerPopupManager (GLViewer viewer) {
      myViewer = viewer;
   }     

   public GLViewer getViewer() {
      return myViewer;
   }

   /**
    * Should be overridden as needed by subclasses.
    */
   public void actionPerformed (ActionEvent e) {
   }

   private class DialogHandle implements RenderListener {
      
      PropertyDialog myDialog;

      DialogHandle (PropertyDialog dialog) {
         myDialog = dialog;
      }

      public void renderOccurred (RendererEvent e) {
         myDialog.updateWidgetValues();
      }
   }      

   private class RerenderListener implements ValueChangeListener {
      public void valueChange (ValueChangeEvent e) {
         myViewer.rerender(); 
      }
   }      

   public void registerDialog (PropertyDialog dialog) {
      DialogHandle handle = new DialogHandle (dialog);
      myDialogHandles.add (handle);
      myViewer.addRenderListener (handle);
      dialog.addWindowListener (new WindowAdapter() {
            public void windowClosed (WindowEvent e) {
               deregisterDialog (e.getWindow());
            }
         });     
   }

   private void deregisterDialog (Window w) {
      if (w instanceof PropertyDialog) {
         PropertyDialog dialog = (PropertyDialog)w;
         for (int i=0; i<myDialogHandles.size(); i++) {
            DialogHandle handle = myDialogHandles.get(i);
            if (handle.myDialog == dialog) {
               myViewer.removeRenderListener (handle);
               myDialogHandles.remove (handle);
               return;
            }
         }
      }
   }
   
   public PropertyDialog createPropertyDialog (String controlStr) {

      // 
      // PropertyDialog dialog =
      //    new PropertyDialog (
      //       "Viewer properties", new PropertyPanel(myViewer), controlStr);
      //
      // John Lloyd, Feb 2014: changed code to create a property dialog
      // based on a hostList, so that we can restore original values:
      HostList hostList = new HostList(1);
      hostList.addHost (myViewer);
      PropTreeCell tree =
         hostList.commonProperties (null, /* allowReadonly= */true);
      tree.removeDescendant ("renderProps");
      PropertyDialog dialog =
         new PropertyDialog (
            "Viewer properties", tree, hostList, controlStr);

      dialog.locateRight (myViewer.getCanvas().getComponent());
      dialog.addGlobalValueChangeListener (new RerenderListener());
      registerDialog (dialog);
      return dialog;
   }

   public PropertyDialog createGridPropertyDialog (String controlStr) {
      PropertyDialog dialog = new PropertyDialog (
         "Viewer grid properties", myViewer.getGrid(), "Done");
      dialog.locateRight (myViewer.getCanvas().getComponent());
      dialog.addGlobalValueChangeListener (new RerenderListener());
      registerDialog (dialog);
      return dialog;
   }

}
