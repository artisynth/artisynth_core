package artisynth.core.modelmenu;

import java.awt.Container;
import java.awt.GridLayout;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JSeparator;
import javax.xml.parsers.ParserConfigurationException;

import maspack.graph.Node;
import maspack.graph.Tree;

import org.python.modules.synchronize;
import org.xml.sax.SAXException;

import artisynth.core.driver.ModelHistory;
import artisynth.core.driver.ModelHistory.ModelHistoryInfo;
import artisynth.core.driver.ModelInfo;
import artisynth.core.driver.VerticalGridLayout;
import artisynth.core.util.AliasTable;
import artisynth.core.util.ArtisynthPath;

public class ArtisynthModelMenu {

   public static int DEFAULT_MAX_ROWS = 15;

   Tree<MenuEntry> menuTree;
   AliasTable demoTable;
   int maxRows;

   JMenu root;
   ArrayList<HistoryMenuInfo> historyList;
   
   public ArtisynthModelMenu(File menuFile) {
      
      this.demoTable = new AliasTable();
      this.maxRows = DEFAULT_MAX_ROWS;
      historyList = null;
      
      try {
         menuTree = DemoMenuParser.parseXML(menuFile.getAbsolutePath());
      } catch (IOException | ParserConfigurationException | SAXException e) {
         return;
      }
      
   }
   
   public void buildMenu(JMenu menu, ModelActionListener actionListener, ModelHistory hist) { 
      historyList = null;
      buildJMenu(menu, actionListener, hist);
      root = menu;
   }

   private void buildJMenu(JMenu menu, ModelActionListener actionListener, ModelHistory hist) {
      if (menuTree.getRootElement().getChildren().size() > maxRows) {
         VerticalGridLayout menuGrid = new VerticalGridLayout(maxRows, 0);
         menu.getPopupMenu().setLayout(menuGrid);
      }
      // climb through tree and build menus
      for (Node<MenuEntry> node : menuTree.getRootElement().getChildren()) {
         if (!isMenuEmpty(node)) {
            buildMenu(menu, node, maxRows, actionListener, hist); // recursively builds menu
         }
      }
   }

   private boolean isMenuEmpty(Node<MenuEntry> node) {

      MenuEntry entry = node.getData();

      switch (entry.getType()) {
         case DIVIDER:
            return false;
         case LABEL:
            return false;
         case HISTORY:
            return false;
         default:
            if (hasModelEntries(node)) {
               return false;
            }
      }
      return true;

   }

   // Recursively find if there are any model entries under a node.
   // Faster than actually counting entries.
   private boolean hasModelEntries(Node<MenuEntry> node) {
      MenuEntry entry = node.getData();

      switch (entry.getType()) {
         case MENU: {
            for (Node<MenuEntry> child : node.getChildren()) {
               boolean hasModels = hasModelEntries(child);
               if (hasModels) {
                  return true;
               }
            }
            break;
         }
         case MODEL: {
            if (entry instanceof DemoEntry) {
               return true;
            }
            break;
         }
         default:
            break;
      }
      return false;
   }

   // recursively find the number of model entries under a node
   private int numModelEntries(Node<MenuEntry> node) {

      int num = 0;
      MenuEntry entry = node.getData();

      switch (entry.getType()) {
         case MENU: {
            for (Node<MenuEntry> child : node.getChildren()) {
               num += numModelEntries(child);
            }
            break;
         }
         case MODEL: {
            if (entry instanceof DemoEntry) {
               num++;
            }
            break;
         }
         default:
            break;
      }
      return num;
   }

   // recursively build menu from supplied tree
   private void buildMenu(JMenu menu, Node<MenuEntry> menuNode, 
      int maxRows, ModelActionListener actionListener, ModelHistory hist) {

      MenuEntry entry = menuNode.getData();

      switch (entry.getType()) {
         case MENU:
            JMenu newMenu = new JMenu(entry.getTitle());
            if (entry.getIcon() != null) {
               URL iconFile = ArtisynthPath.findResource(entry.getIcon());
               newMenu.setIcon(new ImageIcon(iconFile));
            }
            if (entry.getFont() != null) {
               newMenu.setFont(entry.getFont());
            }
            menu.add(newMenu);
            // adjust layout if need to
            if (menuNode.getChildren().size() > maxRows) {
               VerticalGridLayout menuGrid = new VerticalGridLayout(maxRows, 0);
               newMenu.getPopupMenu().setLayout(menuGrid);
            }

            // loop through all children
            for (Node<MenuEntry> child : menuNode.getChildren()) {
               if (!isMenuEmpty(child)) {
                  buildMenu(newMenu, child, maxRows, actionListener, hist);
               }
            }

            break;
         case DIVIDER:

            JSeparator div = new JSeparator();
            div.setLayout(new GridLayout());
            menu.add(div);
            break;

         case LABEL:
            if (entry instanceof LabelEntry) {
               LabelEntry label = (LabelEntry)entry;
               JLabel lbl = new JLabel(label.getTitle());
               if (label.getIcon() != null) {
                  URL iconFile = ArtisynthPath.findResource(entry.getIcon());
                  lbl.setIcon(new ImageIcon(iconFile));
               }
               if (entry.getFont() != null) {
                  lbl.setFont(entry.getFont());
               }

               menu.add(lbl);
            }
         case MODEL:
            if (entry instanceof DemoEntry) {
               DemoEntry demo = (DemoEntry)entry;
               ModelInfo mi = demo.getModel();

               JMenuItem newItem =
                  makeMenuItem(demo.getTitle(), "load", mi, actionListener);
               
               // automatically add entry to the hashmap
               demoTable.addEntry(mi.getShortName(), mi.getClassNameOrFile());

               if (entry.getIcon() != null) {
                  URL iconFile = ArtisynthPath.findResource(entry.getIcon());
                  newItem.setIcon(new ImageIcon(iconFile));
               }
               if (entry.getFont() != null) {
                  newItem.setFont(entry.getFont());
               }
               newItem.setToolTipText(mi.getClassNameOrFile());
               menu.add(newItem);
            }
            break;
         case HISTORY: {
            if (entry instanceof HistoryEntry) {
               HistoryEntry he = (HistoryEntry)entry;

               // add history items
               if (hist != null) {
                  ModelHistoryInfo[] mhi = hist.getRecent(he.getSize());
                  ArrayList<JMenuItem> histItems = new ArrayList<JMenuItem>();
                  
                  if (mhi== null || mhi.length == 0) {
                     JMenuItem newItem = makeMenuItem("<no recent models>", "ignore", null, null);
                     if (entry.getIcon() != null) {
                        URL iconFile = ArtisynthPath.findResource(entry.getIcon());
                        newItem.setIcon(new ImageIcon(iconFile));
                     }
                     if (entry.getFont() != null) {
                        newItem.setFont(entry.getFont());
                     }
                     histItems.add(newItem);
                     menu.add(newItem);
                  } else {
                     for (int i=0; i<mhi.length; i++) {
                        ModelInfo mi = mhi[i].getModelInfo();
      
                        //                     String dispName = mi.getLongName();
                        //                     if (demo.getCompact() > 0) {
                        String dispName = mi.getShortName();
                        //                      }
                        JMenuItem newItem =
                           makeMenuItem(dispName, "load", mi, actionListener);
                        
                        if (entry.getIcon() != null) {
                           URL iconFile = ArtisynthPath.findResource(entry.getIcon());
                           newItem.setIcon(new ImageIcon(iconFile));
                        }
                        if (entry.getFont() != null) {
                           newItem.setFont(entry.getFont());
                        }
                        newItem.setToolTipText(mi.getClassNameOrFile());
                        menu.add(newItem);
                        histItems.add(newItem);
                     }
                  }
                  if (historyList == null) {
                     historyList = new ArrayList<>();
                  }
                  historyList.add(new HistoryMenuInfo(he, histItems));
               }
            }
            break;

         }
         default:
            break;
      }

   }
   
   public AliasTable getDemoTable() {
      return demoTable;
   }
   
   protected JMenuItem makeMenuItem(String name, String cmd, ModelInfo mi, 
      ModelActionListener listener) {
      
      JMenuItem item = new JMenuItem(name);
      updateMenuItem(item, name, cmd, mi, listener);
      return item;
   }
   
   protected void updateMenuItem(JMenuItem item, String name, String cmd, ModelInfo mi, 
      ModelActionListener listener) {
      item.setText(name);
      if (listener != null) {
         item.addActionListener(
            new ModelActionForwarder(listener, cmd, mi));
      }
   }

   public void updateHistoryNodes(ModelHistory hist, ModelActionListener listener) {
      
      // no history, no update
      if (hist == null || historyList == null) {
         return;
      }
      
      for (HistoryMenuInfo hmi : historyList) {
         // replace info on nodes
         ModelHistoryInfo[] mhi = hist.getRecent(hmi.hist.getSize());
         // check if we need to resize the menu at all:
         int ms = hmi.items.size();
         JMenuItem item0 = hmi.items.get(0);
         Container parent = item0.getParent();
         
         if (ms < mhi.length) {
            // need to grow menu
            int idx = -1;
            {// lock
               Object lock = parent.getTreeLock();
               synchronized(lock) {
                  // find index of first item
                  for (int i=0; i<parent.getComponentCount(); i++) {
                     if (parent.getComponent(i) == item0) {
                        idx = i;
                        break;
                     }
                  }
               }
            }
            if (idx >= 0) {
               // currently there are #ms items starting from idx
               for (int i=0; i<mhi.length-ms; i++) {
                  JMenuItem jmi = new JMenuItem("temp");
                  parent.add(jmi, idx+1); // skip over first entry
                  hmi.items.add(jmi);
               }
            }
         }
          
         // replace actual contents of menu entries
         ms = Math.min(hmi.items.size(), mhi.length);
         for (int i=0; i<ms; i++) {
            JMenuItem jmi = hmi.items.get(i);
            
            // remove all action listeners
            ArrayList<ActionListener> als = new ArrayList<>();
            for (ActionListener al : jmi.getActionListeners()) {
               als.add(al);
            }
            for (ActionListener al : als) {
               jmi.removeActionListener(al);
            }
            
            // adjust info
            ModelInfo mi = mhi[i].getModelInfo();
            String dispName = mi.getShortName();
            //                      }
            updateMenuItem(jmi, dispName, "load", mi, listener);
         }
         
      }
   }
}
