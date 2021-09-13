package artisynth.core.gui;

import java.awt.BasicStroke;
import java.awt.Rectangle;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Line2D;
import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.plaf.IconUIResource;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.filechooser.*;
import javax.swing.filechooser.FileFilter;

import artisynth.core.gui.ControlPanel;
import artisynth.core.gui.editorManager.Command;
import artisynth.core.gui.editorManager.UndoManager;
import artisynth.core.util.AliasTable;
import artisynth.core.util.ArtisynthPath;
import maspack.util.PathFinder;
import maspack.util.GenericFileFilter;
import maspack.util.FolderFileFilter;
import maspack.widgets.GuiUtils;
import maspack.widgets.OptionPanel;
import maspack.widgets.StringField;
import maspack.widgets.ValueChangeEvent;
import maspack.widgets.ValueChangeListener;
import maspack.widgets.WidgetDialog;

public class ExtClassPathEditor extends JFrame
   implements ActionListener {

   static ImageIcon myUpIcon;
   static ImageIcon myDownIcon;
   static ImageIcon myDeleteIcon;

   JPanel myPanel;
   JScrollPane myListView;
   JList<String> myList;
   DefaultListModel<String> myListModel;
   UndoManager myUndoManager;

   File myFile;

   JButton myAddClassDirButton;
   JButton myAddJARFIleButton;
   JButton myEditButton;

   JButton myDeleteButton;
   JButton myUpButton;
   JButton myDownButton;
   
   JButton myDoneButton;
   JButton myCancelButton;

   File myLastJARFile;
   File myLastClassFolder;

   JFileChooser myClassFolderChooser;
   JFileChooser myJARFileChooser;

   protected void initializeIcons() {
      if (myUpIcon == null) {
         String iconDir = PathFinder.getSourceRelativePath (
            ControlPanel.class, "icon/");

         myUpIcon = GuiUtils.loadIcon (iconDir + "moveUpArrow.png");
         myDownIcon = GuiUtils.loadIcon (iconDir + "moveDownArrow.png");
         myDeleteIcon = GuiUtils.loadIcon (iconDir + "xcross.png");
      }
   }

   protected class SelectionListener implements ListSelectionListener {

      public void valueChanged (ListSelectionEvent e) {
         updateButtons();
      }
   }

   public class AddEntryCommand implements Command {

      String myEntry;
      int myIdx;
      String myCmdName;

      public AddEntryCommand (
         int idx, String entry, String cmdName) {
         myIdx = idx;
         myEntry = entry;
         myCmdName = cmdName;
      }

      public void execute() {
         myListModel.add (myIdx, myEntry);
      }
      
      public void undo() {
         myListModel.remove (myIdx);
      }
      
      public String getName() {
         return myCmdName;
      }
   }

   public class RemoveEntriesCommand implements Command {

      ArrayList<Integer> myIdxs;
      ArrayList<String> myEntries;
      String myCmdName;

      public RemoveEntriesCommand (
         Collection<Integer> idxs, String cmdName) {
         myIdxs = new ArrayList<>(idxs.size());
         myIdxs.addAll (idxs);
         Collections.sort (myIdxs);
         Collections.reverse (myIdxs); // want highest indices first
         myEntries = new ArrayList<>();
         myCmdName = cmdName;
      }

      public void execute() {
         for (int i=0; i<myIdxs.size(); i++) {
            int idx = myIdxs.get(i);
            myEntries.add (myListModel.remove (idx));
         }
      }
      
      public void undo() {
         for (int i=myIdxs.size()-1; i>=0; i--) {
            int idx = myIdxs.get(i);
            myListModel.add (idx, myEntries.get(i));
         }
      }
      
      public String getName() {
         return myCmdName;
      }
   }

   public class ShiftEntriesCommand implements Command {

      int myMinIdx;
      int myMaxIdx;
      String myCmdName;
      int myShift;

      public ShiftEntriesCommand (
         int minIdx, int maxIdx, int shift, String cmdName) {

         myMinIdx = minIdx;
         myMaxIdx = maxIdx;
         myShift = shift;
         myCmdName = cmdName;
      }

      private void shiftEntries (int minIdx, int maxIdx, int shift) {
         if (shift < -1 || shift > 1) {
            throw new IllegalArgumentException ("shift must be -1, 0, or 1");
         }
         if (shift == 1 && maxIdx+1 < myListModel.getSize()) {
            String last = myListModel.get(maxIdx+1);
            for (int i=minIdx; i<=maxIdx+1; i++) {
               String node = myListModel.get(i);
               myListModel.set (i, last);
               last = node;
            }
         }
         else if (shift == -1 && minIdx-1 >= 0) {
            String last = myListModel.get(minIdx-1);
            for (int i=maxIdx; i>=minIdx-1; i--) {
               String node = myListModel.get(i);
               myListModel.set (i, last);
               last = node;
            }
         }
      }

      public void execute() {
         shiftEntries (myMinIdx, myMaxIdx, myShift);
      }
      
      public void undo() {
         if (myShift == 1) {
            shiftEntries (myMinIdx+1, myMaxIdx+1, -1);
         }
         else if (myShift == -1) {
            shiftEntries (myMinIdx-1, myMaxIdx-1, 1);
         }
      }
      
      public String getName() {
         return myCmdName;
      }
   }

   private JFileChooser getOrCreateClassFolderChooser() {
      if (myClassFolderChooser == null) {
         JFileChooser chooser = new JFileChooser();
         chooser.setFileSelectionMode (JFileChooser.FILES_AND_DIRECTORIES);
         chooser.setAcceptAllFileFilterUsed (false);
         FileFilter filter = new FolderFileFilter("Class folders");
         chooser.addChoosableFileFilter (filter);
         chooser.setFileFilter (filter);
         File homeParent =
            new File (ArtisynthPath.getHomeDir()).getParentFile();
         if (homeParent.isDirectory()) {
            chooser.setCurrentDirectory (homeParent);
         }
         myClassFolderChooser = chooser;
      }
      return myClassFolderChooser;
   }

   private JFileChooser getOrCreateJARFileChooser() {
      if (myJARFileChooser == null) {
         JFileChooser chooser = new JFileChooser();
         FileFilter filter = new GenericFileFilter ("jar", "JAR files (*.jar)");
         chooser.addChoosableFileFilter (filter);
         chooser.setFileFilter (filter);
         File homeParent =
            new File (ArtisynthPath.getHomeDir()).getParentFile();
         if (homeParent.isDirectory()) {
            chooser.setCurrentDirectory (homeParent);
         }
         myJARFileChooser = chooser;
      }
      return myJARFileChooser;
   }

   public void actionPerformed (ActionEvent e) {
      String cmdName = e.getActionCommand();

      if (cmdName.equals ("Add class folder")) {
         JFileChooser chooser = getOrCreateClassFolderChooser();
         if (myLastClassFolder != null) {
            chooser.setCurrentDirectory (myLastClassFolder.getParentFile());
         }
         chooser.setSelectedFile (new File("")); // null doesn't do anything
         if (chooser.showDialog(this, "Add") == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.canRead()) {
               GuiUtils.showError (
                  this, file+" does not exist or is unreadable");
            }
            else {
               myLastClassFolder = file;
               addToList (file);
            }
         }
      }
      else if (cmdName.equals ("Add JAR file")) {
         JFileChooser chooser = getOrCreateJARFileChooser();
         if (myLastJARFile != null) {
            chooser.setCurrentDirectory (myLastJARFile);
         }
         chooser.setSelectedFile (new File("")); // null doesn't do anything
         if (chooser.showDialog(this, "Add") == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.canRead()) {
               GuiUtils.showError (
                  this, "File "+file+" does not exist or is unreadable");
            }
            else {
               myLastJARFile = file;
               addToList (file);
            }
         }
      }
      else if (cmdName.equals ("Edit")) {
         editSelectedEntry ();
      }
      else if (cmdName.equals ("Delete")) {
         deleteSelectedEntries ();
      }
      else if (cmdName.equals ("Up")) {
         shiftSelectedEntries (-1);
      }
      else if (cmdName.equals ("Down")) {
         shiftSelectedEntries (1);
      }
      else if (cmdName.equals ("Save")) {
         if (myFile != null) {
            try {
               save (myFile);
            }
            catch (IOException ioe) {
               System.out.println (
                  "WARNING: could not save "+myFile+": " + e);
            }
         }
         setVisible (false);
      }
      else if (cmdName.equals ("Cancel")) {
         setVisible (false);
      }
   }

   private void updateButtons() {

      // Delete: any item selected
      //
      // Up: a set of contiguous nodes lowest index > 0
      //
      // Down: a set of contiguos nodes with highest index < size()-1
      
      boolean hasDeleteSelection = false;
      boolean hasUpSelection = false;
      boolean hasDownSelection = false;
      boolean hasEditSelection = false;
    
      if (!myList.isSelectionEmpty()) {
         hasDeleteSelection = true;
         // see if selection is contiguous
         boolean contiguous = true;
         int minSelectedIdx = myList.getMinSelectionIndex();
         int maxSelectedIdx = myList.getMaxSelectionIndex();
         if (minSelectedIdx == maxSelectedIdx) {
            hasEditSelection = true;
         }
         for (int idx=minSelectedIdx; idx<=maxSelectedIdx; idx++) {
            if (!myList.isSelectedIndex (idx)) {
               contiguous = false;
               break;
            }
         }
         if (contiguous) {
            if (minSelectedIdx > 0) {
               hasUpSelection = true;
            }
            if (maxSelectedIdx < myListModel.getSize()-1) {
               hasDownSelection = true;
            }
         }
      }
      myAddClassDirButton.setEnabled (true);
      myAddJARFIleButton.setEnabled (true);

      myEditButton.setEnabled (hasEditSelection);
      myUpButton.setEnabled (hasUpSelection);
      myDownButton.setEnabled (hasDownSelection);
      myDeleteButton.setEnabled (hasDeleteSelection);
   }

   private JButton createIconButton (
      JPanel panel, ImageIcon icon, String cmd, String toolTip) {
      JButton button = new JButton (icon);
      button.setActionCommand (cmd);
      if (toolTip != null) {
         button.setToolTipText (toolTip);
      }
      button.addActionListener (this);
      button.setEnabled (false);  
      button.setHorizontalAlignment (SwingConstants.LEFT);
      button.setMargin (new Insets (5, 10, 5, 10));
      panel.add (button);
      panel.add (Box.createRigidArea (new Dimension(2, 0)));
      return button;
   }

   private JButton createVerticalButton (JPanel panel, String name) {
      return createVerticalButton (panel, name, null);
   }

   private JButton createVerticalButton (
      JPanel panel, String name, String toolTip) {
      JButton button = new JButton (name);
      button.setActionCommand (name);
      if (toolTip != null) {
         button.setToolTipText (toolTip);
      }
      button.addActionListener (this);
      button.setEnabled (false);  
      button.setAlignmentX (Component.LEFT_ALIGNMENT);
      Dimension size = button.getPreferredSize();
      button.setMaximumSize (new Dimension (Short.MAX_VALUE, size.height));
      button.setHorizontalAlignment (SwingConstants.LEFT);
      button.setMargin (new Insets (5, 10, 5, 10));
      panel.add (button);
      panel.add (Box.createRigidArea (new Dimension(0, 2)));
      return button;
   }

   private JPanel createUpDownButtons (JPanel panel) { 
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout (new BoxLayout (buttonPanel, BoxLayout.LINE_AXIS));

      myUpButton = createIconButton (
         buttonPanel, myUpIcon, "Up", "Shift the selected entries up");
      myDownButton = createIconButton (
         buttonPanel, myDownIcon, "Down", "Shift the selected entries down");
      myDeleteButton = createIconButton (
         buttonPanel, myDeleteIcon, "Delete", "Delete the selected entries");

      buttonPanel.setAlignmentX (Component.LEFT_ALIGNMENT);
      Dimension size = buttonPanel.getPreferredSize();
      buttonPanel.setMaximumSize (new Dimension (Short.MAX_VALUE, size.height));

      panel.add (buttonPanel);      
      return buttonPanel;
   }

   private JPanel createButtonPanel() {
      JPanel panel = new JPanel();
      panel.setLayout (new BoxLayout (panel, BoxLayout.PAGE_AXIS));

      myAddClassDirButton = createVerticalButton (
         panel, "Add class folder",
         "Add a class folder to the external classpath");
      myAddJARFIleButton = createVerticalButton (
         panel, "Add JAR file",
         "Add a JAR file to the external classpath");

      panel.add (Box.createRigidArea (new Dimension(0, 10)));

      myEditButton = createVerticalButton (
         panel, "Edit", "Change the selected entry");
      createUpDownButtons (panel);

      panel.add (Box.createVerticalGlue());
      panel.setBorder (BorderFactory.createEmptyBorder (2, 2, 2, 2));
      return panel;
   }

   private JButton addBottomButton (JPanel panel, String cmd, String toolTip) {
      JButton button = new JButton (cmd);
      button.setActionCommand (cmd);
      button.addActionListener (this);
      button.setAlignmentX (Component.CENTER_ALIGNMENT);
      panel.add (button);
      if (toolTip != null) {
         button.setToolTipText(toolTip);
      }
      return button;
   }

   protected class ListMouseListener extends MouseInputAdapter {

      public void mousePressed (MouseEvent e) {
         int idx = myList.locationToIndex (e.getPoint());
         Rectangle rec = myList.getCellBounds (idx, idx);
         if (rec == null || !rec.contains(e.getPoint())) {
            myList.clearSelection();
            myList.requestFocus();
         }
      }
   }

   public ExtClassPathEditor (UndoManager undoManager) {

      super ("External classpath editor");
//      setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
//      addWindowListener (new WindowAdapter() {
//         public void windowClosed (WindowEvent e) {
//            dispose();
//         }
//      });
      myUndoManager = undoManager;

      initializeIcons();

      myListModel = new DefaultListModel<String>();      
      myList = new JList<>(myListModel);
      myList.addListSelectionListener (new SelectionListener());
      myList.addMouseListener (new ListMouseListener());
      myListView = new JScrollPane (myList);
      myListView.setPreferredSize(new Dimension(480, 480));

      myPanel = new JPanel();
      myPanel.setLayout (new BorderLayout());
      myPanel.add (myListView, BorderLayout.CENTER);

      JPanel buttonPanel = createButtonPanel();
      myPanel.add (buttonPanel, BorderLayout.LINE_END);
      
      JPanel bottomPanel = new JPanel();
      bottomPanel.setBorder (BorderFactory.createEmptyBorder (5, 5, 5, 5));
      bottomPanel.setLayout (new BoxLayout(bottomPanel, BoxLayout.X_AXIS));
      bottomPanel.add (Box.createRigidArea (new Dimension (20, 10)));
      bottomPanel.add (Box.createHorizontalGlue());
      myDoneButton = addBottomButton (
         bottomPanel, "Save", "Save the external classpath and close the dialog");
      bottomPanel.add (Box.createRigidArea (new Dimension (10, 10)));
      myCancelButton = addBottomButton (
         bottomPanel, "Cancel", "Close the dialog without saving");
      bottomPanel.add (Box.createHorizontalGlue());
      bottomPanel.add (Box.createRigidArea (new Dimension (20, 10)));

      myPanel.add (bottomPanel, BorderLayout.PAGE_END);

      getContentPane().add (myPanel);
      updateButtons();

      pack();
   }

   public ExtClassPathEditor (UndoManager undoManager, File file) 
      throws IOException {
      this (undoManager);
      if (file.exists()) {
         if (!file.canRead()) {
            throw new IOException ("File "+file+" is not readable");
         }
      }
      else {
         // try writing the (empty) file
         save (file);
      }
      myFile = file;
   }

   private void addToList (File file) {
      int idx = myList.getMaxSelectionIndex();
      if (idx == -1) {
         idx = myListModel.size();
      }
      else {
         idx = idx+1;
      }
      myUndoManager.execute (
         new AddEntryCommand (
            idx, file.getAbsolutePath(), "Add entry"));
   }

   private boolean hasSuffix (String fileName, String suffix) {
      int dot = fileName.lastIndexOf ('.');
      if (dot != -1) {
         return suffix.equalsIgnoreCase (fileName.substring (dot+1));
      }
      else {
         return false;
      }
   }        

   protected void editSelectedEntry () {
      int maxSelectedIdx = myList.getMaxSelectionIndex();
      if (maxSelectedIdx != -1) {
         String pathName = myListModel.get(maxSelectedIdx);
         JFileChooser chooser;
         boolean jarfile = hasSuffix (pathName, "jar");
         if (jarfile) {
            chooser = getOrCreateJARFileChooser();
         }
         else {
            chooser = getOrCreateClassFolderChooser();
         }
         chooser.setSelectedFile (new File(pathName));
         if (chooser.showDialog(this, "Done") == JFileChooser.APPROVE_OPTION) {
            File file = chooser.getSelectedFile();
            if (!file.canRead()) {
               GuiUtils.showError (
                  this, "File "+file+" does not exist or is unreadable");
            }
            else {
               myListModel.set(maxSelectedIdx, file.getAbsolutePath());
               if (jarfile) {
                  myLastJARFile = file;
               }
               else {
                  myLastClassFolder = file;
               }
            }
         }
      }
   }

   protected void deleteSelectedEntries () {
      int minSelectedIdx = myList.getMinSelectionIndex();
      int maxSelectedIdx = myList.getMaxSelectionIndex();
      if (minSelectedIdx != -1) {
         ArrayList<Integer> removeIdxs = new ArrayList<>();
         for (int idx=minSelectedIdx; idx<=maxSelectedIdx; idx++) {
            if (myList.isSelectedIndex (idx)) {
               removeIdxs.add (idx);
            }
         }
         myUndoManager.execute (
            new RemoveEntriesCommand (removeIdxs, "Delete entries"));
      }
   }

   protected void shiftSelectedEntries (int shift) {
      int minIdx = myList.getMinSelectionIndex();
      int maxIdx = myList.getMaxSelectionIndex();
      // verify again that we have a selection
      if (minIdx == -1) {
         return;
      }
      myUndoManager.execute (
         new ShiftEntriesCommand (minIdx, maxIdx, shift, "Move entries"));
      // keep shifted nodes selected
      int[] idxs = new int[maxIdx-minIdx+1];
      int k = 0;
      for (int idx=minIdx+shift; idx<=maxIdx+shift; idx++) {
         idxs[k++] = idx;
      }
      myList.setSelectedIndices (idxs);
   }

   private void save (File file) throws IOException {
      PrintWriter pw =
         new PrintWriter (
            new BufferedWriter (new FileWriter (file)));
      for (int i=0; i<myListModel.getSize(); i++) {
         pw.println (myListModel.elementAt(i));
      }
      pw.close();
   }

   private void load (File file) throws IOException {
      BufferedReader reader =
         new BufferedReader (new FileReader (file));
      myListModel.removeAllElements();
      try {
         String line;
         while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.length() != 0) {
               myListModel.add (myListModel.getSize(), line);
            }
         }
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         reader.close();
      }
   }

   public void open() {
      if (myFile != null) {
         try {
            load (myFile);
         }
         catch (IOException e) {
            System.out.println ("WARNING: cannot load "+myFile+": "+e);
         }
      }
      setVisible (true);
   }

}
