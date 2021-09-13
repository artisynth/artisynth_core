package artisynth.core.modelmenu;

import java.awt.*;
import java.util.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;
import javax.swing.table.*;

import maspack.graph.*;
import maspack.util.*;
import maspack.widgets.*;
import maspack.properties.*;

public class AddPackageDialog extends JFrame
   implements ActionListener, ValueChangeListener {
   
   JPanel myPanel;
   JScrollPane myTableView;
   JTable myTable;
   MenuNode myMenuNode;
   ArrayList<PackageEntry> myPackageEntries = new ArrayList<>();   
   ArrayList<String> myPackageNames;

   AutoCompleteStringField myAddPackageField;
   PackageTableModel myTableModel;

   protected class PackageTableModel extends AbstractTableModel {
      ArrayList<TableModelListener> listeners = new ArrayList<>();
      
      public void addTableModelListener (TableModelListener l) {
         listeners.add (l);
      }

      public void removeTableModelListener (TableModelListener l) {
         listeners.remove (l);
      }

      public int getColumnCount() {
         return 2;
      }

      public String getColumnName (int colIdx) {
         switch (colIdx) {
            case 0: {
               return "Title";
            }
            case 1: {
               return "Package";
            }
            default: {
               throw new InternalErrorException (
                  "Unexpected column index "+colIdx);
            }
         }
      }

      public Class<?> getColumnClass (int colIdx) {
         return String.class;
      }

      public int getRowCount () {
         return myPackageEntries.size();
      }

      public Object getValueAt (int rowIdx, int colIdx) {
         switch (colIdx) {
            case 0: {
               return myPackageEntries.get(rowIdx).getTitle();
            }
            case 1: {
               return myPackageEntries.get(rowIdx).getPackageName();
            }
            default: {
               throw new InternalErrorException (
                  "Unexpected column index "+colIdx);
            }
         }
      }

      public void setValueAt (Object value, int rowIdx, int colIdx) {
         if (value instanceof String) {
            String str = (String)value;
         
            switch (colIdx) {
               case 0: {
                  myPackageEntries.get(rowIdx).setTitle(str);
                  break;
               }
               case 1: {
                  myPackageEntries.get(rowIdx).setPackageName(str);
                  break;
               }
               default: {
                  throw new InternalErrorException (
                     "Unexpected column index "+colIdx);
               }
            }
         }
      }

      public boolean isCellEditable (int rowIdx, int colIdx) {
         return true;
      }

   }

   public AddPackageDialog (MenuEntry menuNode) {
      super ("Add package menus");
      setDefaultCloseOperation (WindowConstants.DISPOSE_ON_CLOSE);
      addWindowListener (new WindowAdapter() {
         public void windowClosed (WindowEvent e) {
            dispose();
         }
      });
      myMenuNode = menuNode;

      for (MenuNode child : menuNode.getChildren()) {
         if (child instanceof PackageEntry) {
            myPackageEntries.add ((PackageEntry)child);
         }
      }

      myTable = new JTable();
      myTableModel = new PackageTableModel();
      myTable.setModel (myTableModel);
      myTable.getColumnModel().getColumn(0).setPreferredWidth (120);
      myTable.getColumnModel().getColumn(1).setPreferredWidth (240);

      myTableView = new JScrollPane (myTable);
      myTableView.setPreferredSize(new Dimension(360, 480));

      myPanel = new JPanel();
      myPanel.setLayout (new BorderLayout());
      myPanel.add (myTableView, BorderLayout.CENTER);
      //myPanel.setPreferredSize(new Dimension(320, 480));
      myPanel.setBorder (BorderFactory.createLineBorder (Color.RED));

      PropertyPanel topPanel = new PropertyPanel();
      Package[] packages = Package.getPackages();
      myPackageNames = new ArrayList<>();
      for (Package pack : Package.getPackages()) {
         String name = pack.getName();
         if (name.startsWith ("java") ||
             name.startsWith ("artisynth.core.") ||
             name.startsWith ("maspack.") ||
             name.startsWith ("com.sun.") ||
             name.startsWith ("com.jogamp.") ||
             name.startsWith ("sun.") ||
             name.startsWith ("org.xml.") ||
             name.startsWith ("org.w3c.") ||
             name.startsWith ("argparser") ||
             name.startsWith ("jdk") ||
             name.startsWith ("jogamp.")) {
            continue;
         }
         myPackageNames.add (name);
      }
      myAddPackageField =
         new AutoCompleteStringField (
            "Add package:", "", 28, myPackageNames);
      myAddPackageField.setStretchable (true);
      myAddPackageField.addValueCheckListener (
         new ValueCheckListener () {
            public Object validateValue (
               ValueChangeEvent e, StringHolder errMsg) {
               String str = (String)e.getValue();
               // null string is OK
               if (str.length() == 0) {
                  return str;
               }
               for (String packname : myPackageNames) {
                  if (packname.equals (str)) {
                     return str;
                  }
               }
               if (errMsg != null) {
                  errMsg.value = ""+str+" is not a known package name";
               }
               return Property.IllegalValue;
            }
         });
      myAddPackageField.addValueChangeListener (this);
      topPanel.addWidget (myAddPackageField);
      topPanel.setBorder (BorderFactory.createLineBorder (Color.GREEN));
      myPanel.add (topPanel, BorderLayout.PAGE_START);
         

      getContentPane().add (myPanel);
      pack();
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();
   }

   public void valueChange (ValueChangeEvent e) {
      if (e.getSource() == myAddPackageField) {
         String packname = (String)e.getValue();
         int lastDot = packname.lastIndexOf ('.');
         String title;
         if (lastDot != -1) {
            title = packname.substring (lastDot+1);
         }
         else {
            title = packname;
         }
         PackageEntry entry = new PackageEntry (title);
         entry.setPackageName (packname);
         myPackageEntries.add (entry);
         myAddPackageField.maskValueChangeListeners(true);
         myAddPackageField.setValue ("");
         myAddPackageField.maskValueChangeListeners(false);
         int nump = myPackageEntries.size();
         myTableModel.fireTableRowsInserted (nump, nump);
         myTable.revalidate();
         myTable.repaint();
      }
   }
}


