/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import javax.swing.*;
import javax.swing.event.*;

public class JListTest extends JFrame implements ListSelectionListener {
   JList myList;

   public JListTest() {
      super ("JlistTest");
      myList = new JList (new String[] { "foo", "bar", "bat", "hi" });
      myList.addListSelectionListener (this);
      myList.setSelectionMode (ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      ListSelectionModel selectionModel = myList.getSelectionModel();
      if (selectionModel instanceof DefaultListSelectionModel) {
         DefaultListSelectionModel defSelModel =
            (DefaultListSelectionModel)selectionModel;
         // defSelModel.setLeadAnchorNotificationEnabled (false);
      }
      getContentPane().add (myList);
      pack();
   }

   public void valueChanged (ListSelectionEvent e) {
      System.out.println ("value changed: " + e.getFirstIndex() + " "
      + e.getLastIndex());
      for (int i = e.getFirstIndex(); i <= e.getLastIndex(); i++) {
         System.out.print (" " + i);
         if (myList.isSelectedIndex (i)) {
            System.out.println (" selected");
            // myList.removeSelectionInterval (i, i);
         }
         else {
            System.out.print (" not selected");
         }
         System.out.println ("");
      }
   }

   public static void main (String[] args) {
      JListTest test = new JListTest();
      test.setVisible (true);
   }
}
