package artisynth.core.driver;

import java.io.*;
import java.util.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import maspack.util.*;
import maspack.widgets.*;
import maspack.properties.*;

public abstract class Preferences
   implements HasProperties, Scannable, ActionListener {

   HostList myHostList;
   PropTreeCell myTreeCell;
   PropertyPanel myEditingPanel;
   int myNumScanWarnings;

   // flags for addLoadApplyButtons():
   protected int ADD_RESTART_ANNOTATION = 0x01;

   @Override
   public abstract PropertyList getAllPropertyInfo();

   @Override
   public Property getProperty (String name) {
      return PropertyList.getProperty (name, this);
   }

   protected boolean scanItem (ReaderTokenizer rtok) 
      throws IOException {
      // if keyword is a property name, try scanning that
      if (getAllPropertyInfo().scanProp (this, rtok)) {
         return true;
      }
      else if (rtok.nextToken() == ReaderTokenizer.TT_WORD) {
         // if next token sequence is an unknown property, just
         // ignore it
         String name = rtok.sval;
         if (rtok.nextToken() == '=' &&
             rtok.nextToken() != ReaderTokenizer.TT_EOF) {
            System.out.println (
               "WARNING: unknown property "+name+" in preferences file");
            myNumScanWarnings++;
            return true;
         }
      }
      return false;
   }

   @Override
   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myNumScanWarnings = 0;
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         rtok.pushBack();
         if (!scanItem (rtok)) {
            throw new IOException (
               "Error scanning " + getClass().getName() +
               ": unexpected token: " + rtok);
         }
      }
   }

   public void writeItems (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      getAllPropertyInfo().writeProps (this, pw, fmt, ref);
   }

   /**
    * Number of warnings issued during the most recent scan operartion.
    */
   public int numScanWarnings() {
      return myNumScanWarnings;
   }

   @Override
   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      IndentingPrintWriter.printOpening (pw, "[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      writeItems (pw, fmt, ref);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   @Override
   public boolean isWritable () {
      return true;
   }

   public void setDefaults() {
      getAllPropertyInfo().setDefaultValues (this);
   }

   public abstract void applyToCurrent();

   public abstract void setFromCurrent();

   protected PropertyPanel createDefaultEditingPanel() {
      myHostList = new HostList(1);
      myHostList.addHost (this);
      myTreeCell = myHostList.commonProperties (null, /*allowReadonly=*/false);
      return PropertyDialog.createPanelFromHostList (myTreeCell, myHostList);
   }

   void reloadEditPanelProps() {
      if (myEditingPanel != null) {
         myHostList.saveBackupValues (myTreeCell);
         // shouldn't need get common values since there is only one host
         //myHostList.getCommonValues (myTreeCell, /* live= */true); 
         updateEditingPanelWidgets();
      }
   }

   void restoreEditPanelProps() {
      if (myEditingPanel != null) {
         myHostList.restoreBackupValues();
      }
   }

   protected JButton createStandardButton (String text, String toolTip) {
      JButton button = new JButton (text);
      if (toolTip != null) {
         button.setToolTipText (toolTip);
      }
      button.setActionCommand (text);
      button.addActionListener (this);
      button.setAlignmentX (Component.LEFT_ALIGNMENT);
      button.setMargin (new Insets (2, 4, 2, 4));
      return button;
   }

   // protected JButton addStandardButton (
   //    JPanel panel, String text, String toolTip) {
   //    JButton button = createStandardButton (text, toolTip);
   //    panel.add (button);
   //    panel.add (Box.createRigidArea (new Dimension(0, 2)));
   //    return button;
   // }
      
   protected JPanel createVerticalButtonPanel (ArrayList<JButton> buttons) {
      
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout (new BoxLayout (buttonPanel, BoxLayout.PAGE_AXIS));
      Dimension maxPrefSize = GuiUtils.getMaxPreferredSize (buttons);
      Dimension maxsize = new Dimension (Short.MAX_VALUE, maxPrefSize.height);
      for (int i=0; i<buttons.size(); i++) {
         JButton b = buttons.get(i);
         b.setHorizontalAlignment (SwingConstants.LEFT);      
         b.setPreferredSize (maxPrefSize);
         b.setMaximumSize (maxsize);
         if (i > 0) {
            buttonPanel.add (Box.createRigidArea (new Dimension(0, 3)));
         }
         buttonPanel.add (b);
      }
      buttonPanel.setBorder (BorderFactory.createEmptyBorder (3, 3, 3, 3));
      //buttonPanel.setBorder (BorderFactory.createLineBorder (Color.RED));
      Dimension panelSize = new Dimension (
         6+maxPrefSize.width, (maxPrefSize.height+3)*buttons.size() + 3);
      buttonPanel.setPreferredSize (panelSize);
      buttonPanel.setMaximumSize (panelSize);
      return buttonPanel;
   }

   protected JPanel createHorizontalButtonPanel (ArrayList<JButton> buttons) {
      
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout (new BoxLayout (buttonPanel, BoxLayout.LINE_AXIS));
      Dimension maxPrefSize = GuiUtils.getMaxPreferredSize (buttons);
      for (int i=0; i<buttons.size(); i++) {
         JButton b = buttons.get(i);
         GuiUtils.setFixedSize (b, maxPrefSize);
         buttonPanel.add (Box.createRigidArea (new Dimension(5, 0)));
         buttonPanel.add (b);
      }
      buttonPanel.add (Box.createHorizontalGlue());
      buttonPanel.setBorder (BorderFactory.createEmptyBorder (3, 3, 3, 3));
      return buttonPanel;
   }

   protected void addLoadApplyButtons (PropertyPanel panel) {
      addLoadApplyButtons (panel, null);
   }

   protected void addLoadApplyButtons (
      PropertyPanel panel, String[] annotations) {

      ArrayList<JButton> buttons = new ArrayList<>();
      buttons.add (
         createStandardButton (
            "Set from current",
            "Set values from the current application settings"));
      buttons.add (
         createStandardButton (
            "Apply to current",
            "Apply values to the current application settings"));
      buttons.add (
         createStandardButton (
            "Reset defaults",
            "Retore values to their default settings"));
      JPanel buttonPanel = createHorizontalButtonPanel (buttons);
      panel.add (Box.createVerticalGlue());

      if (annotations != null) {
         for (String str : annotations) {
            JLabel annotation = new JLabel (str);
            GuiUtils.setItalicFont (annotation);
            panel.add (annotation);
         }
      }
      panel.add (buttonPanel);
      //panel.add (new JSeparator());
   }     

   // protected void addLoadApplyButtonsNew (PropertyPanel panel) {
   //    panel.add (new JSeparator());
   //    panel.add (Box.createRigidArea (new Dimension(0, 3)));
   //    addUpdateButton (panel);
   //    addApplyButton (panel);
   //    panel.add (Box.createVerticalGlue());
   // }  
   

   // protected JButton addUpdateButton (JPanel panel) {
   //    return addStandardButton (
   //       panel, "Load from current", "load values from the current settings");
   // }

   // protected JButton addApplyButton (JPanel panel) {
   //    return addStandardButton (
   //       panel, "Apply to current", "apply values to the current settings");
   // }

   protected PropertyPanel createEditingPanel() {
      return createDefaultEditingPanel();
   }

   public PropertyPanel getEditingPanel() {
      if (myEditingPanel == null) {
         myEditingPanel = createEditingPanel();
      }
      return myEditingPanel;
   }
   
   protected void updateEditingPanelWidgets () {
      if (myEditingPanel != null) {
         myEditingPanel.updateWidgetValues();      
      }
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
      if (cmd.equals ("Set from current")) {
         setFromCurrent();
         updateEditingPanelWidgets();
      }
      else if (cmd.equals ("Apply to current")) {
         applyToCurrent();
         Main.getMain().rerender();
      }
      else if (cmd.equals ("Reset defaults")) {
         setDefaults();
         updateEditingPanelWidgets();
      }
   }
}
