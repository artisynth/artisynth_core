package artisynth.core.modelmenu;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Line2D;
import java.io.File;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.border.*;
import javax.swing.plaf.IconUIResource;
import javax.swing.tree.DefaultTreeCellEditor;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.filechooser.*;

import artisynth.core.gui.*;
import artisynth.core.driver.PreferencesManager;
import artisynth.core.driver.Preferences;
import artisynth.core.driver.PreferencesManager.PrefNode;
import artisynth.core.gui.editorManager.Command;
import artisynth.core.gui.editorManager.UndoManager;
import artisynth.core.util.AliasTable;
import maspack.util.*;
import maspack.widgets.*;

public class PreferencesEditor extends JFrame
   implements ActionListener {

   static ImageIcon myTreeCollapsedIcon;
   static ImageIcon myTreeExpandedIcon;
   public static Font myTreeFont;
   
   static {
      // need to create tree font this way because otherwise
      // DefaultTreeCellRenderer recognizes the UI font and prevents it from
      // being "bold"
      Font menufont = UIManager.getFont ("Menu.font");
      myTreeFont = new Font (
         menufont.getName(), menufont.getStyle(), menufont.getSize());
   }

   PreferencesManager myManager;
   JPanel myPanel;
   JScrollPane myTreeView;
   JTree myTree;
   PreferenceTreeModel myTreeModel;
   JSplitPane mySplitPane;
   JButton myResetAllButton;
   JButton myCancelButton;
   JButton myDoneButton;

   protected TreePath pathToProps (PrefNode node) {
      LinkedList<PrefNode> path = new LinkedList<>();
      while (node != null) {
         path.push (node);
         node = node.getParent();
      }
      return new TreePath (path.toArray(new PrefNode[0]));
   }

   protected class PreferenceTreeModel implements TreeModel {
      ArrayList<TreeModelListener> myListeners;
      
      PreferenceTreeModel() {
         myListeners = new ArrayList<>();
      }

      public void addTreeModelListener(TreeModelListener l) {
         myListeners.add(l);
      }

      public void removeTreeModelListener(TreeModelListener l) {
         myListeners.remove(l);
      }

      public Object getChild (Object parent, int index) {
         if (parent instanceof PrefNode) {
            PrefNode pnode = (PrefNode)parent;
            if (index >= 0 && index < pnode.numChildren()) {
               return pnode.getChild(index);
            }
         }
         return null;
      }

      public int getChildCount (Object parent) {
         if (parent instanceof PrefNode) {
            PrefNode pnode = (PrefNode)parent;
            return pnode.numChildren();
         }
         return 0;
      }

      public int getIndexOfChild (Object parent, Object child) {
         if (parent instanceof PrefNode) {
            PrefNode pnode = (PrefNode)parent;
            if (pnode.numChildren() > 0) {
               for (int idx=0; idx<pnode.numChildren(); idx++) {
                  if (pnode.getChild(idx) == child) {
                     return idx;
                  }
               }
            }
         }
         return -1;
      }

      public Object getRoot() {
         return myManager.getTree();
      }

      public boolean isLeaf (Object node) {
         if (node instanceof PrefNode) {
            return ((PrefNode)node).numChildren() == 0;
         }
         return true;
      }

      public void valueForPathChanged(TreePath path, Object newValue) {
      }
   }

   public interface Agent {
      String getName();

      PropertyPanel getPanel();
   }

   protected class TreeRenderer extends DefaultTreeCellRenderer {

      private Border myBorder = BorderFactory.createEmptyBorder (2, 0, 2, 0);

      public Component getTreeCellRendererComponent (
         JTree tree, Object value, boolean selected, boolean expanded,
         boolean leaf, int row, boolean hasFocus) {

         super.getTreeCellRendererComponent (
            tree, value, selected, expanded, leaf, row, hasFocus);

         setBorder (myBorder);
         setIcon (null);
         setFont (myTreeFont);
         return this;
      }
   }

   public void actionPerformed (ActionEvent e) {
      String cmdName = e.getActionCommand();
      if (cmdName.equals ("Save")) {
         myManager.save();
         setVisible (false);
      }
      else if (cmdName.equals ("Reset all defaults")) {
         myManager.resetAllDefaults();
      }
      else if (cmdName.equals ("Cancel")) {
         restoreValues();
         setVisible (false);
      }
   }

   public PreferencesEditor (PreferencesManager manager) {
      myManager = manager;
   }

   protected class SelectionListener implements TreeSelectionListener {

      public void valueChanged (TreeSelectionEvent e) {
         TreePath[] paths = myTree.getSelectionPaths();
         if (paths != null && paths.length == 1) {
            PrefNode node = (PrefNode)paths[0].getLastPathComponent();
            Preferences prefs = node.getPrefs();
            if (prefs != null) {
               mySplitPane.setRightComponent (prefs.getEditingPanel());
            }
         }
      }
   }

   protected void initializeIcons() {
      if (myTreeCollapsedIcon == null) {
         String iconDir = PathFinder.getSourceRelativePath (
            ControlPanel.class, "icon/");

         myTreeCollapsedIcon = GuiUtils.loadIcon(iconDir+"treeCollapsed.png");
         myTreeExpandedIcon = GuiUtils.loadIcon(iconDir+"treeExpanded.png");   
      }
   }

   public void build() {
      initializeIcons();

      UIManager.put (
         "Tree.collapsedIcon", new IconUIResource(myTreeCollapsedIcon));
      UIManager.put (
         "Tree.expandedIcon", new IconUIResource (myTreeExpandedIcon));

      myTree = new JTree();
      //myTreeView = new JScrollPane (myTree);      
      myTreeModel = new PreferenceTreeModel();
      myTree.setModel (myTreeModel);
      myTree.setCellRenderer (new TreeRenderer());

      myTree.setEditable (false);
      myTree.setRootVisible (false);
      myTree.setShowsRootHandles (true);
      myTree.putClientProperty ("JTree.lineStyle", "None");
      //myTree.setCellRenderer (new MenuTreeRenderer());
      myTree.getSelectionModel().setSelectionMode(
         TreeSelectionModel.SINGLE_TREE_SELECTION);
      PrefNode firstNode = myManager.getFirstProps();
      if (firstNode != null) {
         myTree.setSelectionPath (pathToProps (firstNode));
      }
      myTree.addTreeSelectionListener (new SelectionListener());
      Dimension psize = myTree.getPreferredSize();
      psize.setSize (psize.getWidth()+10.0, psize.getHeight());
      myTree.setPreferredSize (psize);
      myTree.setMinimumSize (psize);
      //myTree.expandRow (0);

      JPanel rightPanel;
      if (firstNode != null) {
         rightPanel = firstNode.getPrefs().getEditingPanel();
      }
      else {
         rightPanel = new JPanel(); // stub just in case
      }

      mySplitPane = new JSplitPane (
         JSplitPane.HORIZONTAL_SPLIT, myTree, rightPanel);
      mySplitPane.setContinuousLayout (true);
      mySplitPane.setDividerSize (2);
      mySplitPane.setPreferredSize (new Dimension (620, 520));
      mySplitPane.setBorder (
         BorderFactory.createEtchedBorder (EtchedBorder.LOWERED));

      JPanel bottomPanel = new JPanel();
      bottomPanel.setBorder (BorderFactory.createEmptyBorder (5, 5, 5, 5));
      bottomPanel.setLayout (new BoxLayout(bottomPanel, BoxLayout.X_AXIS));

      myResetAllButton = GuiUtils.addHorizontalButton (
         bottomPanel, "Reset all defaults", this, 
         "Reset all preference values to their defaults");
      bottomPanel.add (Box.createHorizontalGlue());
      myCancelButton = GuiUtils.addHorizontalButton (
         bottomPanel, "Cancel", this, "Close dialog without make changes");
      bottomPanel.add (Box.createRigidArea (new Dimension (10, 10)));
      myDoneButton = GuiUtils.addHorizontalButton (
         bottomPanel, "Save", this, "Save preferences and close dialog");
      bottomPanel.add (Box.createRigidArea (new Dimension (0, 10)));

      myPanel = new JPanel();
      myPanel.setLayout (new BorderLayout());
      myPanel.add (mySplitPane, BorderLayout.CENTER);
      myPanel.add (bottomPanel, BorderLayout.PAGE_END);

      getContentPane().add (myPanel);
      pack();
   }

   /**
    * Reload the values for all property panels which currently exists
    */
   public void reloadValues() {
      myManager.reloadEditPanelProperties();
   }

   /**
    * Restore values for all property panels which currently exist
    */
   public void restoreValues() {
      myManager.restoreEditPanelProperties();
   }
}
