/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import artisynth.core.modelbase.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.driver.*;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.*;
import maspack.util.Disposable;
import maspack.util.InternalErrorException;
import maspack.widgets.GuiUtils;
import maspack.widgets.OptionPanel;
import maspack.widgets.PropertyPanel;

public abstract class FrameBasedEditingAgent extends EditingAgent implements
ActionListener, Disposable, ComponentChangeListener {
   protected JFrame myDisplay;
   protected PropertyPanel myContentPane;

   protected OptionPanel myOptionPanel;
   protected JButton myAddButton;
   protected JButton myDoneButton;
   protected JButton myStopButton;

   protected RootModel myRootModel;
   protected JLabel myInstructionBox;
   protected JTextField myProgressBox;

   protected SelectionFilter mySelectionFilter = null;

   public FrameBasedEditingAgent (Main main) {
      super (main);
   }

   protected void createDisplay() {
      // this method will be overridden
      createDisplayFrame ("Add component");
   }

   protected void installSelectionFilter (SelectionFilter filter) {
      if (mySelectionFilter == null) {
         mySelectionManager.addFilter (filter);
         mySelectionFilter = filter;
      }
   }

   protected void uninstallSelectionFilter() {
      if (mySelectionFilter != null) {
         mySelectionManager.removeFilter (mySelectionFilter);
         mySelectionFilter = null;
      }
   }

   /**
    * Creates the frame associated with this agent. After this method is called,
    * subclasses can use specialized methods to add specific components to the
    * frame, e.g.:
    * 
    * <pre>
    * createFrame (&quot;Add AxialSprings&quot;);
    * createSeparator();
    * createTypeSelector (typeMap);
    * createPropertyFrame();
    * </pre>
    */
   protected void createDisplayFrame (String frameName) {
      myDisplay = new JFrame (frameName);
      myDisplay.setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
      myDisplay.addWindowListener (new WindowAdapter() {
         public void windowClosed (WindowEvent e) {
            dispose();
         }
      });
      
      myContentPane = new PropertyPanel();
      myContentPane.setWidgetsDraggable (false);
      myContentPane.setWidgetsSelectable (false);
      myContentPane.setBorder (BorderFactory.createEmptyBorder (5, 10, 5, 10));
      
      myDisplay.getContentPane().add (myContentPane);
   }

   /**
    * Called by subclasses inside {@link #createDisplay createDisplay} to create
    * an instruction box. This is a text box whose messages indicate to the user
    * the steps required to add a component.
    */
   protected void createInstructionBox (String labelText) {
      myInstructionBox = new JLabel ("X");
      myInstructionBox.setBorder (BorderFactory.createLineBorder (Color.RED));
      Dimension size = myInstructionBox.getPreferredSize();
      size.width = 1000000;
      myInstructionBox.setMaximumSize (size);
      myInstructionBox.setMinimumSize (size);

      myInstructionBox.setHorizontalAlignment (JTextField.LEFT);
      myInstructionBox.setBackground (new Color (1f, 1f, 0.5f));
      myInstructionBox.setOpaque (true);
      if (labelText != null) {
         JLabel label = new JLabel (labelText);
         GuiUtils.setItalicFont (label);
         addToContentPane (label);
      }
      addToContentPane (myInstructionBox);
   }

   protected void createInstructionBox() {
      createInstructionBox ("Instructions:");
   }

   /**
    * Called by subclasses inside {@link #createDisplay createDisplay} to create
    * a progress box. This is a text box whose messages indicate progress as the
    * user proceedes through the sequence of steps required to add a component.
    */
   protected void createProgressBox() {
      myProgressBox = new JTextField ("");
      Dimension size = myProgressBox.getPreferredSize();
      size.width = 1000000;
      myProgressBox.setMaximumSize (size);
      Font font = myProgressBox.getFont();
      myProgressBox.setFont (
         new Font (font.getName(), Font.BOLD, font.getSize()));
      addToContentPane (myProgressBox);
   }

   /**
    * Called by subclasses inside {@link #createDisplay createDisplay} to create
    * a button panel. The names of the buttons are placed inside the string
    * <code>buttonNames</code>, separated by white-space. An array of all
    * created buttons is returned. Buttons with certain known names (e.g.,
    * "Done", "Add") are also assigned to predefined variables.
    */
   protected JButton[] createOptionPanel (String buttonNames) {
      myOptionPanel = new OptionPanel (buttonNames, this);
      myAddButton = myOptionPanel.getButton ("Add");
      myDoneButton = myOptionPanel.getButton ("Done");
      myStopButton = myOptionPanel.getButton ("Stop");
      addToContentPane (myOptionPanel);
      return myOptionPanel.getButtons();
   }

   protected void addToContentPane (Component comp) {
      if (comp instanceof JComponent) {
         ((JComponent)comp).setAlignmentX (Component.LEFT_ALIGNMENT);
      }
      // Component filler = Box.createRigidArea (new Dimension (2, 4));
      // if (filler instanceof JComponent)
      // { ((JComponent)filler).setAlignmentX (Component.LEFT_ALIGNMENT);
      // System.out.println ("aligning filler");
      // ((JComponent)filler).setBorder (
      // BorderFactory.createLineBorder (Color.black));
      // }
      // myContentPane.addWidget (filler);
      myContentPane.addWidget (comp);
   }

   /**
    * Connects the agent to the ArtiSynth infrastructure and makes it visible.
    * 
    * @param popupBounds
    * TODO
    */
   public void show (Rectangle popupBounds) {
      if (myDisplay == null) {
         createDisplay();
      }
      myContentPane.addWidget (Box.createVerticalGlue());
      myDisplay.pack();
      // GuiUtils.locateRight (myDisplay, myMain.getFrame());
      GuiUtils.locateRelative (myDisplay, popupBounds, 0.5, 0.5, 0, 0.5);
      myRootModel = myMain.getRootModel();
      myRootModel.addComponentChangeListener (this);
      myMain.getWorkspace().registerDisposable (this);
      myDisplay.setVisible (true);
   }

   /**
    * Disconnects the agent from the ArtiSynth infrastructure and disposes of
    * its resources.
    */
   public void dispose() {
      if (myMain.getWorkspace().deregisterDisposable (this)) {
         if (myMain.getRootModel() == myRootModel) {
            myRootModel.removeComponentChangeListener (this);
         }
         else {
            throw new InternalErrorException (
               "Root model has changed unexpectedly from " + myRootModel + " to "
               + myMain.getRootModel());
         }
         if (myDisplay != null) {
            myDisplay.dispose();
            myDisplay = null;
         }
      }
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Done")) {
         myDisplay.setVisible (false);
         dispose();
      }
   }

   /**
    * Called by subclasses inside {@link #createDisplay createDisplay} to add a
    * separator to the display.
    */
   protected void createSeparator() {
      addToContentPane (new JSeparator());
   }

   /**
    * Called by subclasses inside {@link #createDisplay createDisplay} to add a
    * custom component to the display.
    */
   public void addWidget (Component comp) {
      addToContentPane (comp);
   }

   public void componentChanged (ComponentChangeEvent e) {
      if (!isContextValid()) {
         dispose();
         return;
      }
   }

   public boolean isVisible () {
      return myDisplay != null && myDisplay.isVisible();
   }
}
