package artisynth.core.driver;

import java.io.*;
import java.util.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Field;

import artisynth.core.workspace.RootModel;
import artisynth.core.util.*;
import maspack.util.*;

/**
 * Manages the set of known root models. This class exists to address the
 * problem that it can take significant time (many seconds) to locate all the
 * root models within a large package, like "artisynth.models". This in turn
 * means that it can take a long time to initialize the ArtiSynth model menu,
 * resulting in long application start times.
 *
 * <p> The RootModelManager mitigates this problem by maintaing a file-based
 * cache of known root models that is read in quickly and used to initialize
 * the model menu. Once the menu is created, a background thread is started to
 * update the root model set. When the thread completes, the model menu is
 * updated to reflect any changes.
 */
public class RootModelManager {

   // flags for package nodes:

   private static int PACKAGE_QUERIED = 0x04; // package was queried by a find request
   private static int PACKAGE_DIFFERS = 0x08; // package differs from one it
                                              // was compared to
   // flags for the findModels() method:

   public static final int RECURSIVE = 0x01; // find classes recursively
   public static final int USE_CACHE = 0x02; // used cached results if available
   public static final int INCLUDE_HIDDEN = 0x04; // include hidden classes

   // top node of the main model set
   PackageNode myMainRoot = new PackageNode ("");
   boolean myMainModified = false;

   // top node of the cached model set. Will be null if no cache has been read   
   PackageNode myCacheRoot = null;
   
   File myCacheFile;

   // list of known package names for which root models might be selected
   LinkedHashSet<String> myKnownPackageNames;
   int myKnownPackageCnt = -1;

   boolean debug = false;

   /**
    * Returns the trailing part of a full package or class name that follows the
    * last '.'. For example, for 'foo.bar.bash', returns 'bash'.
    */
   public static String getLeafName (String pathName) {
      int dotIdx = pathName.lastIndexOf ('.');
      if (dotIdx != -1) {
         return pathName.substring (dotIdx+1, pathName.length());
      }
      else {
         return pathName;
      }
   }

   /**
    * Returns the "parent" part of a full package or class name that preceeds
    * the last '.'. For example, for 'foo.bar.bash', returns 'foo.bar'.  If the
    * name does not contain a '.', returns an empty string.
    */
   public static String getParentName (String pathName) {
      int dotIdx = pathName.lastIndexOf ('.');
      if (dotIdx != -1) {
         return pathName.substring (0, dotIdx);
      }
      else {
         return "";
      }
   }

   static boolean hideFromMenu (Class<?> clazz) {
      // omit from menu if the class contains a static public field named
      // omitFromMenu whose value con be converted to 'true'
      try {
         Field field = clazz.getField ("omitFromMenu");
         return field.getBoolean (null);         
      }
      catch (Exception e) {
         return false;
      }
   }

   static abstract class Node {

      String myName;
      PackageNode myParent;

      String getName() {
         return myName;
      }

      void setName (String name) {
         myName = name;
      }

      PackageNode getParent() {
         return myParent;
      }

      void setParent (PackageNode parent) {
         myParent = parent;
      }

      int compareNames (Node node) {
         return myName.compareTo (node.myName);
      }

      /**
       * Returns true if the name and hidden flag of this node
       * equals that of another node.
       */
      boolean attributesEqual (Node node) {
         return myName.equals (node.myName);
      }

      abstract void write (PrintWriter pw) throws IOException;
   }

   static int getInsertIndex (
      ArrayList<? extends Node> list, String name) {
      int idx = list.size();
      if (idx > 0) {
         // Assume we might be inserting close to the end
         int cmp = list.get(idx-1).myName.compareTo(name);
         if (cmp < 0) {
            return idx;
         }
         else if (cmp == 0) {
            return idx-1;
         }
         // binary search
         int lo = 0;
         int hi = idx-1;
         while (hi != lo) {
            idx = (hi+lo)/2;
            cmp = list.get(idx).myName.compareTo (name);
            if (cmp == 0) {
               return idx;
            }
            else if (cmp < 0) {
               lo = (idx == lo ? hi : idx);
            }
            else {
               hi = idx;
            }
         }
         idx = lo;
      }
      return idx;
   }

   static int getIndexOf (
      ArrayList<? extends Node> list, String name) {
      // binary search
      int lo = 0;
      int hi = list.size()-1;
      if (hi == -1) {
         return -1;
      }
      while (hi != lo) {
         int idx = (hi+lo)/2;
         int cmp = list.get(idx).myName.compareTo (name);
         if (cmp == 0) {
            return idx;
         }
         else if (cmp < 0) {
            lo = (idx == lo ? hi : idx);
         }
         else {
            hi = idx;
         }
      }
      int cmp = list.get(lo).myName.compareTo (name);
      if (cmp == 0) {
         return lo;
      }
      else {
         return -1;
      }
   }

   static class PackageNode extends Node {
      ArrayList<PackageNode> myPackages = new ArrayList<>();
      ArrayList<ModelNode> myModels = new ArrayList<>();
      int myFlags = 0;

      PackageNode (String name) {
         myName = name;
      }

      int numPackages() {
         return myPackages.size();
      }

      ArrayList<PackageNode> getPackages() {
         return myPackages;
      }

      int indexOfPackage (PackageNode node) {
         return myPackages.indexOf (node);
      }

      PackageNode getPackage (int idx) {
         return myPackages.get(idx);
      }

      void addPackage (PackageNode node) {
         int idx = getInsertIndex (myPackages, node.getName());
         node.setParent (this);
         myPackages.add (idx, node);
      }

      void addPackage (int idx, PackageNode node) {
         node.setParent (this);
         myPackages.add (idx, node);
      }

      PackageNode addPackage (int idx, String name) {
         PackageNode packNode = new PackageNode (name);
         packNode.setParent (this);
         myPackages.add (idx, packNode);
         return packNode;
      }

      int comparePackageAt (int idx, String name) {
         return myPackages.get(idx).myName.compareTo(name);
      }

      PackageNode findOrAddPackage (String pname) {
         
         if (myPackages.size() == 0) {
            return addPackage (0, pname);
         }
         else {
            int idx = getInsertIndex (myPackages, pname);
            if (idx < myPackages.size() &&
                myPackages.get(idx).myName.equals(pname)) {
               return myPackages.get(idx);
            }
            else {
               return addPackage (idx, pname);
            }
         }
      }

      PackageNode findPackage (String pname) {
         int idx = getIndexOf (myPackages, pname);
         if (idx != -1) {
            return myPackages.get(idx);
         }
         else {
            return null;
         }
      }

      PackageNode removePackage (String name) {
         int idx = getIndexOf (myPackages, name);
         if (idx != -1) {
            return myPackages.remove (idx);
         }
         else {
            return null;
         }
      }

      PackageNode removePackage (int idx) {
         if (idx >= 0 && idx < myPackages.size()) {
            PackageNode node = myPackages.remove (idx);
            node.setParent (null);
            return node;
         }
         else {
            return null;
         }
      }

      boolean removePackage (PackageNode node) {
         if (myPackages.remove (node)) {
            node.setParent (null);
            return true;
         }
         else {
            return false;
         }
      }

      boolean replacePackage (PackageNode oldNode, PackageNode newNode) {
         int idx = myPackages.indexOf (oldNode);
         if (idx != -1) {
            myPackages.set (idx, newNode);
            newNode.setParent (this);
            return true;
         }
         else {
            return false;
         }
      }

      void removeAllPackages() {
         for (PackageNode node : myPackages) {
            node.setParent (null);
         }
         myPackages.clear();
      }

      int numModels() {
         return myModels.size();
      }

      ArrayList<ModelNode> getModels() {
         return myModels;
      }

      int indexOfModel (ModelNode node) {
         return myModels.indexOf (node);
      }

      ModelNode getModel (int idx) {
         return myModels.get(idx);
      }

      void addModel (ModelNode node) {
         int idx = getInsertIndex (myModels, node.getName());
         node.setParent (this);
         myModels.add (idx, node);
      }

      void addModel (int idx, ModelNode node) {
         node.setParent (this);
         myModels.add (idx, node);
      }

      int compareModelAt (int idx, String name) {
         return myModels.get(idx).myName.compareTo(name);
      }

      ModelNode findModel (String pname) {
         int idx = getIndexOf (myModels, pname);
         if (idx != -1) {
            return myModels.get(idx);
         }
         else {
            return null;
         }
      }

      ModelNode removeModel (String name) {
         int idx = getIndexOf (myModels, name);
         if (idx != -1) {
            return myModels.remove (idx);
         }
         else {
            return null;
         }
      }

      ModelNode removeModel (int idx) {
         if (idx >= 0 && idx < myModels.size()) {
            ModelNode node = myModels.remove (idx);
            node.setParent (null);
            return node;           
         }
         else {
            return null;
         }
      }


      boolean removeModel (ModelNode node) {
         if (myModels.remove (node)) {
            node.setParent (null);
            return true;
         }
         else {
            return false;
         }
      }

      void removeAllModels() {
         for (ModelNode node : myModels) {
            node.setParent (null);
         }
         myModels.clear();
      }

      boolean getDiffers() {
         return (myFlags & PACKAGE_DIFFERS) != 0;
      }

      void setDiffers (boolean differs) {
         if (differs) {
            myFlags |= PACKAGE_DIFFERS;
         }
         else {
            myFlags &= ~PACKAGE_DIFFERS;
         }
      }

      boolean wasQueried() {
         return (myFlags & PACKAGE_QUERIED) != 0;
      }

      void setQueried (boolean queried) {
         if (queried) {
            myFlags |= PACKAGE_QUERIED;
         }
         else {
            myFlags &= ~PACKAGE_QUERIED;
         }
      }

      void write (PrintWriter pw) throws IOException {
         if (myName.equals("")) {
            pw.println ("[");
         }
         else {
            pw.println (myName + " [");
         }
         IndentingPrintWriter.addIndentation (pw, 2);
         for (PackageNode pnode : myPackages) {
            pnode.write (pw);
         }
         for (ModelNode mnode : myModels) {
            mnode.write (pw);
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }

      void scan (ReaderTokenizer rtok) throws IOException {
         clear();
         rtok.scanToken ('[');
         while (rtok.nextToken() != ']') {
            if (!rtok.tokenIsWord()) {
               throw new IOException (
                  "Word describing node name expected, "+rtok);
            }
            String name = rtok.sval;
            if (rtok.nextToken() == '[') {
               // package node 
               PackageNode packNode = new PackageNode (name);
               rtok.pushBack();
               packNode.scan (rtok);
               addPackage (packNode);
            }
            else if (rtok.ttype == '*') {
               // hidden model node
               addModel (new ModelNode (name, /*hidden=*/true));
            }
            else if (rtok.ttype == ReaderTokenizer.TT_WORD ||
                     rtok.ttype == ']') {
               // regular model node
               addModel (new ModelNode (name, /*hidden=*/false));
               rtok.pushBack();
            }
            else {
               throw new IOException (
                  "Unexpected token, "+rtok);
            }
         }
      }

      void clear() {
         removeAllPackages();
         removeAllModels();
      }

      int numChildren() {
         return numModels() + numPackages();
      }
         
   }

   static class ModelNode extends Node {

      boolean myHidden;

      ModelNode (String name, boolean hidden) {
         myName = name;
         myHidden = hidden;
      }

      boolean isHidden() {
         return myHidden;
      }

      void setHidden (boolean hidden) {
         myHidden = hidden;
      }

      void write (PrintWriter pw) throws IOException {
         pw.println (myName + (isHidden() ? "*" : ""));
      }

      boolean attributesEqual (Node node) {
         if (node instanceof ModelNode) {
            ModelNode modelNode = (ModelNode)node;
            return (myName.equals (modelNode.myName) &&
                    myHidden == modelNode.myHidden);
         }
         else {
            return false;
         }
      }
   }

   String getPathName (Node node) {
      StringBuilder sb = new StringBuilder();
      sb.append (node.getName());
      PackageNode pnode = node.getParent();
      while (pnode != null && !pnode.myName.equals("")) {
         sb.insert (0, pnode.getName() + ".");
         pnode = pnode.getParent();
      }
      return sb.toString();
   }
   /*
    Things to do with RootModelManager:

    read in models from a file:

       gives a set of *unvalidated* entries

    save model to a file

       write out all current known models



    find all rootModels in a package 

    recursively find all rootModels in a package 

    return a list of all known packages that *might* contain a root model
   */

   void loadPackage (String pkgName) {
      PackageNode  packNode = findPackageNode (myMainRoot, pkgName);
      if (packNode == null) {
         packNode = addPackageModels (pkgName);
      }      
   }

   PackageNode addPackageModels (String pkgName) {
      PackageNode newNode = new PackageNode (getLeafName (pkgName));
      findValidPackageClasses (newNode, pkgName);
      if (newNode.numChildren() > 0) {
         // insert new package if it actually contains models
         PackageNode parentNode =
            findOrCreatePackageNode (myMainRoot, getParentName (pkgName));
         parentNode.addPackage (newNode);
         myMainModified = true;
      }
      return newNode;
   }      

   void recursivelyUpdateModelSet (PackageNode packNode) {
      if (packNode.numModels() > 0 || packNode.wasQueried()) {
         String pkgName = getPathName(packNode);
         if (findPackageNode (myMainRoot, pkgName) == null) {
            addPackageModels (pkgName);
         }
      }
      else {
         for (PackageNode pnode : packNode.getPackages()) {
            recursivelyUpdateModelSet (pnode);
         }
      }
   }

   public void updateModelSet () {
      // recuse through the cache and update the model set based on for
      // whichever packages were (a) queried, or (b) contain models
      recursivelyUpdateModelSet (myCacheRoot);
   }

   public boolean compareCacheToMain() {
      return recursivelyComparePackages (myCacheRoot, myMainRoot);
   }

   public boolean cacheDiffersFromMain () {
      return myCacheRoot.getDiffers();
   }

   public boolean cacheDiffersFromMain (String pkgName) {
      if (myCacheRoot == null) {
         throw new IllegalStateException (
            "Root model manager does not have cache");
      }
      PackageNode packNode = findPackageNode (myCacheRoot, pkgName);
      if (packNode == null) {
         // differs if main *does* have a corresponding package
         System.out.println ("No cache package");
         return findPackageNode (myMainRoot, pkgName) != null;
      }
      else {
         return packNode.getDiffers();
      }
   }

   void findValidPackageClasses (PackageNode packNode, String pkgName) {

      // find all classes in this package
      ArrayList<String> classNames =
         ClassFinder.findClassNames (pkgName, RootModel.class);

      Collections.sort (classNames);
      ArrayList<ModelNode> modelNodes = new ArrayList<>(classNames.size());

      String[] cursecs = new String[1];
      packNode.clear();

      String pkg = pkgName;
      if (!pkg.equals ("") && !pkg.endsWith (".")) {
         pkg = pkg + "."; // add a dot to the prefix
      }      

      // pnode will be the immediate parent node for each model class
      PackageNode pnode = packNode;
      for (String className : classNames) {
         boolean hidden = false;
         try {
            Class<?> clazz = ClassFinder.forName (className, false);
            if (Modifier.isAbstract (clazz.getModifiers())) {
               continue;
            }
            else if (hideFromMenu (clazz)) {
               hidden = true;
            }
         }
         catch (Error | Exception e) {
            // shouldn't happen - remove class if it does
            e.printStackTrace();
            continue;
         }   
         String localName = className;
         if (localName.startsWith (pkg)) {
            localName = localName.substring (pkg.length());
         }
         // new package sections - split at periods
         String[] newsecs = localName.split ("\\."); 
         int clsIdx = newsecs.length-1; // index of the class section
         // compute the index at which the *package* components of cursecs
         // and newsecs differ
         int idx = 0;
         while (idx<Math.min (cursecs.length-1, newsecs.length-1)) {
            if (!cursecs[idx].equals (newsecs[idx])) {
               break;
            }
            idx++;
         }        
         if (cursecs.length != newsecs.length || idx != clsIdx) {
            // Need to adjust the package node. First go up the hierarchy to
            // reach the idx point ...
            for (int j=0; j<cursecs.length-1-idx; j++) {
               pnode = pnode.getParent();
            }
            // and then chain on extra nodes as needed:
            for (int j=idx; j<clsIdx; j++) {
               PackageNode newNode = new PackageNode (newsecs[j]);
               pnode.addPackage (newNode);
               pnode = newNode;
            }
         }         
         ModelNode modelNode = new ModelNode (newsecs[clsIdx], hidden);
         pnode.addModel (modelNode);
         cursecs = newsecs;
      }
   }

   PackageNode findOrCreatePackageNode (PackageNode rootNode, String pkgName) {
      if (pkgName.equals ("")) {
         return rootNode;
      }
      // package sections - split at periods
      String[] pkgsecs = pkgName.split ("\\.");
      PackageNode packNode = rootNode;
      for (int i=0; i<pkgsecs.length; i++) {
         packNode = packNode.findOrAddPackage (pkgsecs[i]);
      }
      return packNode;
   }

   PackageNode findPackageNode (PackageNode rootNode, String pkgName) {
      if (pkgName.equals ("")) {
         return rootNode;
      }
      // package sections - split at periods
      String[] subPkgNames = pkgName.split ("\\.");
      PackageNode packNode = rootNode;
      for (int i=0; i<subPkgNames.length && packNode != null; i++) {
         packNode=packNode.findPackage (subPkgNames[i]);
      }
      return packNode;
   }

   void recursivelyCollectClasses (
      ArrayList<String> classNames, PackageNode packNode,
      String pathName, int flags) {

      for (ModelNode node : packNode.getModels()) {
         if (!node.isHidden() || (flags & INCLUDE_HIDDEN) != 0) {
            classNames.add (pathName + "." + node.getName());
         }
      }
      if ((flags & RECURSIVE) != 0) {
         for (PackageNode pnode : packNode.getPackages()) {
            recursivelyCollectClasses (
               classNames, pnode, pathName+"."+pnode.getName(), flags);
         }
      }
   }

   private String recursivelyFindFromSimpleName (
      PackageNode packNode, String simpName) {
      
      ModelNode modelNode = packNode.findModel (simpName);
      if (modelNode != null) {
         return getPathName(modelNode);
      }
      for (PackageNode pnode : packNode.getPackages()) {
         String modelName = recursivelyFindFromSimpleName (pnode, simpName);
         if (modelName != null) {
            return modelName;
         }
      }
      return null;
   }

   /**
    * Try to find the class name of a model given its simple name.
    */
   public String findModelFromSimpleName (String simpName) {
      if (myCacheRoot != null) {
         return recursivelyFindFromSimpleName (myCacheRoot, simpName);
      }
      else {
         return recursivelyFindFromSimpleName (myMainRoot, simpName);
      }
   }

   public ArrayList<String> findModels (String pkgName, int flags) {
      PackageNode packNode;
      if ((flags & USE_CACHE) != 0 && myCacheRoot != null) {
         packNode = findOrCreatePackageNode (myCacheRoot, pkgName);
         packNode.setQueried (true);
      }
      else {
         packNode = findPackageNode (myMainRoot, pkgName);
         if (packNode == null) {
            packNode = addPackageModels (pkgName);
         }      
      }
      ArrayList<String> classNames = new ArrayList<>();
      if (packNode.numChildren() > 0) {
         String pathName = getPathName (packNode);
         recursivelyCollectClasses (classNames, packNode, pathName, flags);
      }
      return classNames;
   }

   /**
    * Checks that two node lists are equal in terms of size and having
    * the attributes of all their nodes being equal.
    */
   boolean attributesEqual (
      List<? extends Node> list0, List<? extends Node> list1) {
      if (list0.size() != list1.size()) {
         return false;
      }
      for (int i=0; i<list0.size(); i++) {
         if (!list0.get(i).attributesEqual (list1.get(i))) {
            return false;
         }
      }
      return true;
   }

   /**
    * Recursively check if two package nodes are equal. Used for testing
    * and debugging only.
    */
   boolean recursiveEquals (
      PackageNode packNode0, PackageNode packNode1) {

      if (!packNode0.myName.equals (packNode1.myName)) {
         return false;
      }
      if (!attributesEqual (packNode0.myModels, packNode1.myModels)) {
         return false;
      }
      if (packNode0.numPackages() != packNode1.numPackages()) {
         return false;
      }
      else {
         for (int i=0; i<packNode0.numPackages(); i++) {
            if (!recursiveEquals (
                   packNode0.getPackage(i), packNode1.getPackage(i))) {
               return false;
            }
         }
      }
      return true;
   }

   /**
    * Recursively compares packages described by two package nodes {@code
    * packNode0} and {@code packNode1}, and returns {@code true} if they are
    * equal and {@code false} if they are not.  Each package under {@code
    * packNode0} is compared to its counterpart under {@code packNode1}, and
    * its {@code PACKAGE_DIFFERS} flag is set to {@code true} if it differs
    * from its counterpart (or its counterpart does not exist), and {@code
    * false} otherwise.
    */
   boolean recursivelyComparePackages (
      PackageNode packNode0,PackageNode packNode1) {

      boolean differs = false;
      if (!attributesEqual (packNode0.myModels, packNode1.myModels)) {
         differs = true;
      }
      if (!attributesEqual (packNode0.myPackages, packNode1.myPackages)) {
         differs = true;
      }
      
      // recursively call this method for all matching packages
      int i = 0; // index for packNode0 subpackages
      int j = 0; // index for packNode1 subpackages
      PackageNode pnode1 =
         packNode1.numPackages() > 0 ? packNode1.getPackage (0) : null;
      for (i=0; i<packNode0.numPackages(); i++) {
         PackageNode pnode0 = packNode0.getPackage (i);

         // advance pnode1 until its name is >= that of pnode0
         int cmp = 0;
         while (pnode1 != null && (cmp=pnode0.compareNames(pnode1)) > 0) {
            if (j < packNode1.numPackages()-1) {
               pnode1 = packNode1.getPackage (++j);
            }
            else {
               pnode1 = null;
            }
         }
         if (pnode1 != null && cmp == 0) {
            // match found; recurse
            if (!recursivelyComparePackages (pnode0, pnode1)) {
               differs = true;
            }
         }
         else {
            // no matching subpackage exists
            pnode0.setDiffers (true);
         }
      }
      packNode0.setDiffers (differs);
      if (debug) {
         System.out.println (
            "compare "+getPathName (packNode0)+ " " + !differs);
      }
      return !differs;
   }

   public void setCacheFile (File file) {
      myCacheFile = file;
   }

   public File getCacheFile() {
      return myCacheFile;
   }

   public void readCacheFile () {
      if (myCacheFile != null) {
         ReaderTokenizer rtok = null;
         try {
            rtok = ArtisynthIO.newReaderTokenizer (myCacheFile);
            myCacheRoot = new PackageNode ("");
            myCacheRoot.scan (rtok);
         }
         catch (IOException e) {
            System.out.println (
               "WARNING: can't read root model cache file " +
               myCacheFile+": " + e);
            myCacheFile = null;
            myCacheRoot = null;
         }
         finally {
            if (rtok != null) {
               rtok.close();
            }
         }
         
      }
   }

   public boolean hasCache() {
      return myCacheRoot != null;
   }

   public boolean saveCacheIfModified() {
      if (myMainModified) {
         writeCacheFile();
         System.out.println ("RootModelManager: saving cache file");
         myMainModified = false;
         return true;
      }
      else {
         return false;
      }
   }

   public void writeCacheFile() {
      if (myCacheFile != null) {
         IndentingPrintWriter pw = null;
         try {
            pw = ArtisynthIO.newIndentingPrintWriter (myCacheFile);
            myMainRoot.write (pw);
         }
         catch (IOException e) {
            System.out.println (
               "WARNING: can't write root model cache file " +
               myCacheFile+": " + e);
            myCacheFile = null;
         }
         finally {
            if (pw != null) {
               pw.close();
            }
         }
      }
   }

   /**
    * Combine a set of strings into a single string separated by '.'.
    */
   private String merge (String[] strs, int num) {
      StringBuilder sb = new StringBuilder();
      for (int i=0; i<num; i++) {
         if (i > 0) {
            sb.append ('.');
         }
         sb.append (strs[i]);
      }
      return sb.toString();
   }

   /**
    * Update the set of known packages. This can change as more classes are
    * loaded by the class loader. The set of known packages is used only to
    * facilitate auto-completion in "package" widgets.
    */
   private void updateKnownPackages() {

      // get packages currently known to the class loader, and update if their
      // number is different from the previous update
      Package[] packages = Package.getPackages(); 
      if (myKnownPackageCnt != packages.length) {
         myKnownPackageCnt = packages.length;

         // remove packages that will not contain artsynth classes
         ArrayList<String> packnames = new ArrayList<>();
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
            packnames.add (name);
         }
         // sort the names and add to the hash set
         Collections.sort(packnames);
         myKnownPackageNames = new LinkedHashSet<>();
         myKnownPackageNames.addAll (packnames);

         // The set of known packages only includes packages that directly
         // contain classes. We also want to add super packages.
         for (String name : packnames) {
            String[] split = name.split ("\\.");
            if (split.length > 1) {
               myKnownPackageNames.add (split[0]);
               for (int n=2; n<split.length; n++) {
                  myKnownPackageNames.add (merge (split, n));
               }
            }
         }
         myKnownPackageNames.remove ("artisynth");
      }
   }

   public Collection<String> getKnownPackageNames() {
      updateKnownPackages();
      return myKnownPackageNames;
   }

}

/**

 Functionality:

 1. Initialize with a set of packages to check 

 2. Initialize the data set with by reading in a cache of all known root
    models. Mark each package as being cached.

 3. Client asks for the root models in a specific package, with an
    option to allow for caching.

    If caching is allowed, and the package is present in the data set, return
    the contents, cached or otherwise.

    If caching is not allowed, and the package is present in the data set,
    return its contents only if it is not cached.

    Otherwse, search the package and if it contains packages, add
    it to the data set.

 4. Run a thread to validate the cached packages.

    Which packages do we search?

    * all specifically requested packages
    
    * all cached packages containing classes

    * all cached packages which have been queried

    Mark all packages for which the true values differ
    from the cached results, and let the clients know.

 Do we keep track of *empty* packages? Probably not - we can assume that
 packages the manager is asked to check probably *do* contain root models.

 */
