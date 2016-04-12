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

public class RenderMappingsDialog extends PropertyDialog {
   int myNumProps;

   public RenderMappingsDialog (String title,
   Iterable<? extends HasProperties> mappables) {
      super();
      setTitle (title);
      initialize ("Done Reset");
      build (mappables);
      setDefaultCloseOperation (DISPOSE_ON_CLOSE);
   }

   private void build (Iterable<? extends HasProperties> mappables) {
      myHostList = new HostList (mappables);

      Iterator<? extends HasProperties> it = mappables.iterator();
      if (!it.hasNext()) {
         throw new IllegalArgumentException ("list of mappables is empty");
      }
      HasProperties renderable = it.next();
      PropertyInfo renderInfo =
         renderable.getAllPropertyInfo().get ("renderMappings");
      if (renderInfo == null) {
         throw new IllegalArgumentException ("renderable '"
         + renderable.getClass()+"' does not contain property 'renderMappings'");
      }
      PropTreeCell tree = new PropTreeCell();
      PropTreeCell renderCell = new PropTreeCell (renderInfo, null);
      tree.addChild (renderCell);

      // need to expand the host list down one level before we
      // can get to the render mappings
      myHostList.saveBackupValues (tree);

      // null render mappings will be stored as null here
      myHostList.getCommonValues (tree, /* live= */true);

      // values for null render mappings will be expanded
      // using mappings returned from createRenderMappings
      renderCell.addChildren (myHostList.commonProperties (renderCell, false));

      // backup values will be saved, including the original
      // null values
      myHostList.saveBackupValues (renderCell);

      // null render mappings will be replaced with the render mappings
      // that were created during the the call to commonProperties
      myHostList.addSubHostsIfNecessary (renderCell);
      myHostList.getCommonValues (renderCell, /* live= */true);

      myNumProps = renderCell.numChildren();

      setPanel (new RenderMappingsPanel (EditingProperty.createProperties (
         renderCell, myHostList, /* isLive= */true)));
      myTree = renderCell;

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
