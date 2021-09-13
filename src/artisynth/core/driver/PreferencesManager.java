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

import artisynth.core.util.*;

public class PreferencesManager implements Scannable {

   PrefNode myPrefTree;
   File myFile;
   int myNumScanWarnings;

   public class PrefNode {
      String myName;
      String myInternalName;
      Preferences myPrefs;
      PrefNode myParent;
      ArrayList<PrefNode> myChildren = null;

      PrefNode (String name, String internalName) {
         myName = name;
         myInternalName = internalName;
      }

      void setPrefs (Preferences props) {
         myPrefs = props;
      }

      public Preferences getPrefs() {
         return myPrefs;
      }

      public int numChildren() {
         return myChildren == null ? 0 : myChildren.size();
      }

      public ArrayList<PrefNode> getChildren() {
         return myChildren == null ? new ArrayList<>() : myChildren;
      }

      void addChild (PrefNode node) {
         if (myChildren == null) {
            myChildren = new ArrayList<>();
         }
         node.myParent = this;
         myChildren.add (node);
      }

      public PrefNode getParent() {
         return myParent;
      }

      public PrefNode getChild (String name) {
         if (myChildren != null) {
            for (PrefNode node : myChildren) {
               if (node.myName.equals (name)) {
                  return node;
               }
            }
         }
         return null;
      }

      public PrefNode getChild (int idx) {
         if (myChildren != null) {
            return myChildren.get(idx);
         }
         return null;
      }

      public String toString() {
         return myName;
      }
   }

   public PreferencesManager (File file) {
      myFile = file;
      myPrefTree = new PrefNode(null, null);
   }

   public PrefNode getTree() {
      return myPrefTree;
   }

   public PrefNode getFirstProps () {
      if (myPrefTree.numChildren() > 0) {
         PrefNode node = myPrefTree.getChild(0);
         while (node.getPrefs() == null && node.numChildren() > 0) {
            node = node.getChild(0);
         }
         return node;
      }
      else {
         return null;
      }
   }

   public void addProps (
      String pathname, String internalName, Preferences props) {
      String[] subnames = pathname.split ("\\.");
      PrefNode node = myPrefTree;
      for (int i=0; i<subnames.length; i++) {
         String name = subnames[i];
         PrefNode child = node.getChild (name);
         if (child == null) {
            child = new PrefNode (name, internalName);
            node.addChild (child);
         }
         if (i==subnames.length-1) {
            child.setPrefs (props);
         }
         else {
            node = child;
         }
      }      
   }

   protected PrefNode getByInternalName (PrefNode node, String name) {
      if (name.equals(node.myInternalName)) {
         return node;
      }
      for (PrefNode child : node.getChildren()) {
         PrefNode foundNode = getByInternalName (child, name);
         if (foundNode != null) {
            return foundNode;
         }
      }
      return null;      
   }

   private void reloadEditPanelProps (PrefNode node) {
      if (node.getPrefs() != null) {
         node.getPrefs().reloadEditPanelProps();
      }
      for (PrefNode child : node.getChildren()) {
         reloadEditPanelProps (child);
      }
   }

   public void reloadEditPanelProperties() {
      reloadEditPanelProps (myPrefTree);
   }

   private void restoreEditPanelProps (PrefNode node) {
      if (node.getPrefs() != null) {
         node.getPrefs().restoreEditPanelProps();
      }
      for (PrefNode child : node.getChildren()) {
         restoreEditPanelProps (child);
      }
   }

   public void restoreEditPanelProperties() {
      restoreEditPanelProps (myPrefTree);
   }

   void recursivelyApply (PrefNode node) {
      if (node.myPrefs != null) {
         node.myPrefs.applyToCurrent();
      }
      for (PrefNode child : node.getChildren()) {
         recursivelyApply (child);
      }
   }

   public void resetAllDefaults() {
      recursivelyResetDefaults (myPrefTree);
   }

   void recursivelyResetDefaults (PrefNode node) {
      if (node.myPrefs != null) {
         node.myPrefs.setDefaults();
         node.myPrefs.updateEditingPanelWidgets();
      }
      for (PrefNode child : node.getChildren()) {
         recursivelyResetDefaults (child);
      }
   }

   /**
    * Load the preferences file if it is there, or create it if it is not.
    */ 
   public boolean loadOrCreate() {
      if (ArtisynthIO.loadOrCreate (this, myFile, "preferences")) {
         if (myNumScanWarnings > 0) {
            if (save()) {
               System.out.println (
                  "Reinitialized preferences file "+myFile);
            }
         }
         return true;
      }
      myFile = null;
      return false;
   }

   private boolean load() {
      return ArtisynthIO.load (this, myFile, "preferences");
   }

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      myNumScanWarnings = 0;
      rtok.scanToken ('[');
      while (rtok.nextToken() != ']') {
         if (!rtok.tokenIsWord()) {
            throw new IOException ("Preference name expected, got "+rtok);
         }
         PrefNode node = getByInternalName (myPrefTree, rtok.sval);
         if (node == null) {
            System.out.println (
               "WARNING: preferences '"+rtok.sval+"' not found");
            myNumScanWarnings++;
         }
         else {
            node.myPrefs.scan (rtok, ref);
            myNumScanWarnings += node.myPrefs.numScanWarnings();
         }
      }
   }

   private void writeRecursively (
      PrefNode node, PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {

      if (node.myPrefs != null) {
         pw.print (node.myInternalName + " ");
         node.myPrefs.write (pw, fmt, ref);
      }
      for (PrefNode child : node.getChildren()) {
         writeRecursively (child, pw, fmt, ref);
      }
   }

   public boolean save() {
      if (!ArtisynthIO.save (this, myFile, "preferences")) {
         myFile = null;
         return false;
      }
      else {
         return true;
      }
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      pw.println ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      writeRecursively (myPrefTree, pw, fmt, ref);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   public boolean isWritable() {
      return true;
   }
}
