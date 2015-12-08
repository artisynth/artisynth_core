/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.driver;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.LinkedList;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import maspack.util.InternalErrorException;
import maspack.widgets.ButtonCreator;
import maspack.widgets.ButtonMasks;
import maspack.widgets.GuiUtils;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import artisynth.core.gui.SelectableComponentPanel;
import artisynth.core.gui.selectionManager.ClassFilter;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.gui.widgets.ClassField;
import artisynth.core.gui.widgets.ComponentField;
import artisynth.core.modelbase.ModelComponent;

/**
 * Create a class that handles selecting components through a panel in the main
 * frame. The panel always specifies the last selected component and allows for
 * component selection.
 * 
 */
public class SelectComponentPanelHandler {
   private Main myMain;
   private MainFrame myFrame;
   private JPanel myPanel;
   // private StringField componentName;
   private ComponentField myComponentField;
   private ClassField componentFilter;
   // private JComboBox componentFilter;
   private JButton mySelectFilterButton;
   private ClassFilter myClassFilter;
   private SelectionManager mySelectionManager;
   private LinkedList<String> myFilterHistory;
   private static final int myMaxFilterHistory = 10;

   // the last successfully selected component
   private ModelComponent lastSelectedComponent = null;

   public SelectComponentPanelHandler (Main parent, MainFrame theFrame) {
      myMain = parent;
      mySelectionManager = myMain.getSelectionManager();
      // mySelectionManager.addSelectionListener (this);
      myFilterHistory = new LinkedList<String>();
      myFrame = theFrame;
   }

   private class FilterPopupListener implements ActionListener {
      public void actionPerformed (ActionEvent e) {
         String className = e.getActionCommand();
         Class cls = ClassField.All;
         if (!className.equals ("*")) {
            try {
               cls = Class.forName (className);
            }
            catch (Exception exc) {
               throw new InternalErrorException ("No class found for '"
               + className + "'");
            }
         }
         componentFilter.setValue (cls);
      }
   }

   public void setComponentFilter (Class cls) {
      if (cls == null) {
         cls = ClassField.All;
      }
      componentFilter.setValue (cls);
   }

   private void doSetComponentFilter (Class cls) {
      String newName;
      if (myClassFilter != null) {
         mySelectionManager.removeFilter (myClassFilter);
         myClassFilter = null;
      }
      if (cls != null && cls != ClassField.All) {
         myClassFilter = new ClassFilter (cls);
         mySelectionManager.addFilter (myClassFilter);
         mySelectionManager.filterSelections (myClassFilter);
         newName = cls.getName();
         if (!myFilterHistory.contains (newName)) {
            myFilterHistory.add (0, newName);
            if (myFilterHistory.size() > myMaxFilterHistory) {
               myFilterHistory.removeLast();
            }
         }
      }
   }

   private void showFilterPopup() {
      JPopupMenu popup = new JPopupMenu();
      FilterPopupListener listener = new FilterPopupListener();
      String lastSelectedName = null;
      if (lastSelectedComponent != null) {
         lastSelectedName = lastSelectedComponent.getClass().getName();
      }
      JMenuItem menu = new JMenuItem ("*");
      menu.addActionListener (listener);
      popup.add (menu);

      if (lastSelectedName != null) {
         menu = new JMenuItem (lastSelectedName);
         menu.addActionListener (listener);
         popup.add (menu);
      }

      for (String name : myFilterHistory) {
         if (name != lastSelectedName) {
            menu = new JMenuItem (name);
            menu.addActionListener (listener);
            popup.add (menu);
         }
      }

      Component ref = mySelectFilterButton;
      Dimension size = popup.getPreferredSize();
      popup.show (ref, ref.getWidth() - size.width, ref.getHeight());
   }

   public void clear() {
      myComponentField.setValue (null);
   }
   
   public void createPanel() {
      myPanel = new JPanel();
      myPanel.setLayout (new BoxLayout (myPanel, BoxLayout.X_AXIS));

      ImageIcon comboSelectIcon =
         GuiUtils.loadIcon (
            SelectableComponentPanel.class, "icon/downArrow.png");

      mySelectFilterButton =
         ButtonCreator.createIconicButton (
            comboSelectIcon, "Set Filter", "Set selection filter", true, false,
            null);

      componentFilter = new ClassField ("", ClassField.All, 15);
      componentFilter.getTextField().setHorizontalAlignment (JTextField.RIGHT);
      componentFilter.setSpacing (0);
      componentFilter.getTextField().addMouseListener (new MouseAdapter() {
         public void mousePressed (MouseEvent e) {
            if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
               showFilterPopup();
            }
         }
      });

      componentFilter.addValueChangeListener (new ValueChangeListener() {
         public void valueChange (ValueChangeEvent e) {
            doSetComponentFilter ((Class)e.getValue());
         }
      });

      GuiUtils.setFixedSize (mySelectFilterButton, 18, 18);

      mySelectFilterButton.addMouseListener (new MouseAdapter() {
         public void mousePressed (MouseEvent e) {
            if (e.getButton() == MouseEvent.BUTTON1) {
               showFilterPopup();
            }
         }
      });

      myComponentField = new ComponentField ("", 30, myMain);

      // GuiUtils.setFixedSize (parentBtn, 24, 24);

      myPanel.add (Box.createRigidArea (new Dimension (36, 20)));
      myPanel.add (mySelectFilterButton);
      myPanel.add (componentFilter);

      myPanel.add (myComponentField);

      // myPanel.add(componentName);
      // //myPanel.add(selectParent);
      // myPanel.add(parentBtn);
      myFrame.add (myPanel, BorderLayout.SOUTH);
   }

}
