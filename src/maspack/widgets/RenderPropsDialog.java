/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.widgets;

import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.util.Iterator;

import maspack.properties.EditingProperty;
import maspack.properties.HasProperties;
import maspack.properties.HostList;
import maspack.properties.PropTreeCell;
import maspack.properties.PropertyInfo;
import maspack.util.InternalErrorException;

public class RenderPropsDialog extends PropertyDialog {
   int myNumProps;

   public RenderPropsDialog (String title,
   Iterable<? extends HasProperties> renderables) {
      super();
      setTitle (title);
      initialize ("Done Reset");
      build (renderables);
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
   }

//   public RenderPropsDialog (Frame owner, String title,
//   Iterable<? extends HasProperties> renderables) {
//      super (owner, title);
//      initialize ("Done Reset");
//      build (renderables);
//      inheritGlobalListeners (owner);
//      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
//   }

//   public RenderPropsDialog (Dialog owner, String title,
//   Iterable<? extends HasProperties> renderables) {
//      super (owner, title);
//      initialize ("Done Reset");
//      build (renderables);
//      inheritGlobalListeners (owner);
//      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
//   }

//   public static RenderPropsDialog createDialog (
//      Window win, String title, Iterable<? extends HasProperties> renderables) {
//      RenderPropsDialog dialog;
//      if (win instanceof Dialog) {
//         dialog = new RenderPropsDialog ((Dialog)win, title, renderables);
//      }
//      else if (win instanceof Frame) {
//         dialog = new RenderPropsDialog ((Frame)win, title, renderables);
//      }
//      else if (win == null) {
//         dialog = new RenderPropsDialog (title, renderables);
//      }
//      else {
//         throw new InternalErrorException ("Unsupported window type " + win);
//      }
//      return dialog;
//   }

   private void build (Iterable<? extends HasProperties> renderables) {
      myHostList = new HostList (renderables);

      Iterator<? extends HasProperties> it = renderables.iterator();
      if (!it.hasNext()) {
         throw new IllegalArgumentException ("list of renderables is empty");
      }
      HasProperties renderable = it.next();
      PropertyInfo renderInfo =
         renderable.getAllPropertyInfo().get ("renderProps");
      if (renderInfo == null) {
         throw new IllegalArgumentException ("renderable '"
         + renderable.getClass() + "' does not contain property 'renderProps'");
      }
      PropTreeCell tree = new PropTreeCell();
      PropTreeCell renderCell = new PropTreeCell (renderInfo, null);
      tree.addChild (renderCell);

      // need to expand the host list down one level before we
      // can get to the render props
      myHostList.saveBackupValues (tree);

      // null render props will be stored as null here
      myHostList.getCommonValues (tree, /* live= */true);

      // values for null render props will be expanded
      // using props returned from createRenderProps
      renderCell.addChildren (myHostList.commonProperties (renderCell, false));

      // backup values will be saved, including the original
      // null values
      myHostList.saveBackupValues (renderCell);

      // null render props will be replaced with the render props
      // that were created during the the call to commonProperties
      myHostList.addSubHostsIfNecessary (renderCell);
      myHostList.getCommonValues (renderCell, /* live= */true);

      myNumProps = renderCell.numChildren();

      setPanel (new RenderPropsPanel (EditingProperty.createProperties (
         renderCell, myHostList, /* isLive= */true)));
      setScrollable(true);
      myTree = renderCell;

      //enableAutoRerendering (true);

//      ViewerManager driver = Main.getMain().getViewerManager();
//      driver.setSelectionHighlighting (GLViewer.SelectionHighlighting.None);
//      driver.render();

      // addWindowListener (
      // new PropertyWindowAdapter(this)
      // {
      // public void windowClosing (WindowEvent e)
      // {
      // getWindow().dispose();
      // }

      // public void windowClosed (WindowEvent e)
      // {
      // ViewerManager driver = Main.getMain().getViewerManager();
      // driver.setSelectionHighlighting (
      // GLViewer.SelectionHighlighting.Color);
      // driver.render();
      // }
      // });

//      addWindowListener (new WindowAdapter() {
//         // public void windowClosing (WindowEvent e)
//         // {
//         // e.getSource().dispose();
//         // }
//
//         public void windowClosed (WindowEvent e) {
//            ViewerManager driver = Main.getMain().getViewerManager();
//            driver.setSelectionHighlighting (
//               GLViewer.SelectionHighlighting.Color);
//            driver.render();
//         }
//      });
      // initFinish ("Done Reset");
      pack();
   }

   public void actionPerformed (ActionEvent e) {
      String actionCmd = e.getActionCommand();
      if (actionCmd.equals ("Done")) {
         myReturnValue = OptionPanel.OK_OPTION;
         setVisible (false);
         dispose();
      }
      else if (actionCmd.equals ("Reset")) {
         System.out.println ("reset");
         if (myHostList != null) {
            myHostList.restoreBackupValues();

            myHostList.replaceSubHostsIfNecessary (myTree);
            myHostList.getCommonValues (myTree, /* live= */true);

            updateWidgetValues();
         }
         fireGlobalValueChangeListeners();
      }
      else {
         throw new InternalErrorException ("Unimplemented action command "
         + actionCmd);
      }
   }

   public int numProperties() {
      return myNumProps;
   }
}
