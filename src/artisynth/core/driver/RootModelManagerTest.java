package artisynth.core.driver;

import java.io.*;
import java.util.*;

import maspack.util.*;
import maspack.matrix.*;

import artisynth.core.driver.RootModelManager.*;
import artisynth.core.workspace.RootModel;

public class RootModelManagerTest extends UnitTest {

   int RECURSIVE = RootModelManager.RECURSIVE;
   int USE_CACHE = RootModelManager.USE_CACHE;
   int HIDDEN = RootModelManager.INCLUDE_HIDDEN;

   File srcDir = new File(PathFinder.findSourceDir (this));
   
   /**
    * Check equality of two string lists
    */
   boolean equals (ArrayList<String> list0, ArrayList<String> list1) {
      if (list0.size() != list1.size()) {
         return false;
      }
      for (int i=0; i<list0.size(); i++) {
         if (!list0.get(i).equals (list1.get(i))) {
            return false;
         }
      }
      return true;
   }

   void findAndPrintClasses (
      RootModelManager m, String pkgName, int flags) {

      ArrayList<String> classNames = 
         m.findModels (pkgName, flags);
      String flagStr = "";
      if ((flags & RECURSIVE) != 0) {
         flagStr += "RECURSIVE ";
      }
      if ((flags & USE_CACHE) != 0) {
         flagStr += "CACHED ";
      }
      if ((flags & HIDDEN) != 0) {
         flagStr += "HIDDEN ";
      }
      System.out.println (pkgName + " " + flagStr);
      for (String s : classNames) {
         System.out.println ("  "+s);
      }
   }

   private int getInsertIndex (
      ArrayList<? extends Node> list, String name) {
      return RootModelManager.getInsertIndex (list, name);
   }

   private int getIndexOf (
      ArrayList<? extends Node> list, String name) {
      return RootModelManager.getIndexOf (list, name);
   }

   private String listToString (ArrayList<? extends Node> list) {
      StringBuilder sb = new StringBuilder();
      sb.append ("'");
      for (Node node : list) {
         if (sb.length() > 1) {
            sb.append (" ");
         }
         sb.append (node.getName());
      }
      sb.append ("'");
      return sb.toString();
   }

   void testIndexing() {
      // make sure the bineary search indexing methods are working properly
      ArrayList<PackageNode> list = new ArrayList<>();

      // check with empty list
      checkEquals (
         "getInsertIndex(empty, 'A'), empty list",
         getInsertIndex (list, "A"), 0);
      checkEquals (
         "getIndexOf(empty, 'A'), empty list",
         getIndexOf (list, "A"), -1);

      // single item list
      list.add (new PackageNode ("B"));

      checkEquals (
         "getInsertIndex('B', 'A')", getInsertIndex (list, "A"), 0);
      checkEquals (
         "getInsertIndex('B', 'B')", getInsertIndex (list, "B"), 0);
      checkEquals (
         "getInsertIndex('B', 'C')", getInsertIndex (list, "C"), 1);
      checkEquals (
         "getIndexOf('B', 'A')", getIndexOf (list, "A"), -1);
      checkEquals (
         "getIndexOf('B', 'B')", getIndexOf (list, "B"), 0);
      checkEquals (
         "getIndexOf('B', 'C')", getIndexOf (list, "C"), -1);

      // N item lists
      for (int n=2; n<8; n++) {
         list.add (new PackageNode (Character.toString((char)('A'+n))));
         String listStr = listToString (list);

         for (int i=0; i<n+2; i++) {
            int res = (i == 0 ? 0 : i-1);
            String s = Character.toString ((char)('A'+i));
            checkEquals (
               "getInsertIndex("+listStr+", '"+s+"')",
               getInsertIndex (list, s), res);
            res = (i<n+1 ? i-1 : -1);
            checkEquals (
               "getIndexO("+listStr+", '"+s+"')",
               getIndexOf (list, s), res);
         }
      }
   }

   void testCacheFile () {
      RootModelManager manager = new RootModelManager();

      // use models from "artisynth.demos" as a test
      manager.loadPackage ("artisynth.demos");

      // extract the models, non-hidden and all, from the 
      // "artisynth.demos.test"
      ArrayList<String> nonhiddenModels =
         manager.findModels ("artisynth.demos.test", 0);
      ArrayList<String> allModels =
         manager.findModels ("artisynth.demos.test", HIDDEN);

      PackageNode pnode = manager.findPackageNode (
         manager.myMainRoot, "artisynth.demos.test");
      
      check (
         "number of models found with HIDDEN not equal to internal count",
         allModels.size() == pnode.numModels());

      // set a temporary cache file, write to it, and then read it in and make
      // sure the model set remains unchanged.

      File cacheFile = new File (srcDir, "_testcache.txt");
      manager.setCacheFile (cacheFile);
      manager.writeCacheFile();
      manager.readCacheFile();
      check ("can't read cache file", manager.myCacheRoot != null);

      check (
         "cache read not equal to cache written",
         manager.recursiveEquals (manager.myCacheRoot, manager.myMainRoot));

      // now check that the models found in 'artisynth.demos.test' are
      // consistent with earlier results
      check (
         "inconsistent results for findModels() with USE_CACHE",
         equals (nonhiddenModels,
                 manager.findModels ("artisynth.demos.test", USE_CACHE)));

      check (
         "inconsistent results for findModels() with USE_RECURSIVE",
         equals (nonhiddenModels,
                 manager.findModels ("artisynth.demos.test", RECURSIVE)));

      check (
         "inconsistent results for findModels() with INCLUDE_HIDDEN",
         equals (allModels,
                 manager.findModels ("artisynth.demos.test", HIDDEN)));

      // remove the cache file
      cacheFile.delete();
   }

   private void checkDiffers (
      RootModelManager manager, String pkgName, boolean check) {
      checkEquals (
         "cacheDiffersFromMain("+pkgName+")", 
         manager.cacheDiffersFromMain (pkgName), check);
   }
      

   void testUpdateScenario () {
      RootModelManager manager = new RootModelManager();

      File originalCacheFile = new File (srcDir, "_originalCache.txt");
      File modifiedCacheFile = new File (srcDir, "_modifiedCache.txt");

      // use models from "artisynth.demos" as a test
      manager.loadPackage ("artisynth.demos");

      // write two cache files: an original one unchanged from the true values,
      // and a modified one that reflects changes made to the cache.

      manager.setCacheFile (originalCacheFile);
      manager.writeCacheFile();

      // edit the cache to create a modified cache that will differ from the
      // original results

      // remove demos.fem so that it will appear:
      PackageNode demos = manager.findPackageNode (
         manager.myMainRoot, "artisynth.demos");     
      demos.removePackage ("fem");

      // add a fake package 'foo' that will disappear:
      PackageNode pnode = new PackageNode ("foo");
      pnode.addModel (new ModelNode ("model0", false));
      pnode.addModel (new ModelNode ("model1", true));
      demos.addPackage (pnode);

      // remove models from tutorial that will appear
      pnode = demos.findPackage ("tutorial");
      pnode.removeModel (2);
      pnode.removeModel (4);
      pnode.removeModel (10);

      // add fake models to 'mech' that will disappear
      pnode = demos.findPackage ("mech");
      pnode.addModel (new ModelNode ("FakeMech0", false));
      pnode.addModel (new ModelNode ("FakeMech1", true));

      manager.setCacheFile (modifiedCacheFile);
      manager.writeCacheFile();

      // now create a new manager, and test operations with the two caches

      // modified cache first
      manager = new RootModelManager();
      manager.setCacheFile (modifiedCacheFile);      
      manager.readCacheFile();

      // query some models 
      manager.findModels ("artisynth.demos.mech", USE_CACHE);
      manager.findModels ("artisynth.demos.fem", USE_CACHE);
      manager.findModels ("artisynth.demos.tutorial", USE_CACHE);
      manager.findModels ("artisynth.demos.foo", USE_CACHE);
      manager.findModels ("artisynth.demos.inverse", USE_CACHE);
      manager.findModels ("artisynth.demos.wrapping", USE_CACHE);

      manager.updateModelSet();
      manager.compareCacheToMain();

      checkEquals (
         "cacheDiffersFromMain()", manager.cacheDiffersFromMain(), true);

      checkDiffers (manager, "artisynth", true);
      checkDiffers (manager, "artisynth.demos", true);
      checkDiffers (manager, "artisynth.demos.mech", true);
      checkDiffers (manager, "artisynth.demos.fem", true);
      checkDiffers (manager, "artisynth.demos.tutorial", true);
      checkDiffers (manager, "artisynth.demos.foo", true);
      checkDiffers (manager, "artisynth.demos.inverse", false);
      checkDiffers (manager, "artisynth.demos.wrapping", false);

      // now check with original cache

      manager.setCacheFile (originalCacheFile);      
      manager.readCacheFile();

      // query some models 
      manager.findModels ("artisynth.demos.mech", USE_CACHE);
      manager.findModels ("artisynth.demos.fem", USE_CACHE);
      manager.findModels ("artisynth.demos.tutorial", USE_CACHE);
      manager.findModels ("artisynth.demos.inverse", USE_CACHE);
      manager.findModels ("artisynth.demos.wrapping", USE_CACHE);

      manager.updateModelSet();
      manager.debug = true;
      manager.compareCacheToMain();
      manager.debug = false;

      checkEquals (
         "cacheDiffersFromMain()", manager.cacheDiffersFromMain(), false);

      checkDiffers (manager, "artisynth", false);
      checkDiffers (manager, "artisynth.demos", false);
      checkDiffers (manager, "artisynth.demos.mech", false);
      checkDiffers (manager, "artisynth.demos.fem", false);
      checkDiffers (manager, "artisynth.demos.tutorial", false);
      checkDiffers (manager, "artisynth.demos.foo", false);
      checkDiffers (manager, "artisynth.demos.inverse", false);
      checkDiffers (manager, "artisynth.demos.wrapping", false);
      
      // remove the cache file
      //originalCacheFile.delete();
      //modifiedCacheFile.delete();
   }

   public void timing() {
      FunctionTimer timer = new FunctionTimer();
      timer.start();
      ClassFinder.findClassNames (
         "artisynth", RootModel.class, /*recursive=*/false);
      timer.stop();
      System.out.println ("search time artisynth=" + timer.result(1));

      timer.start();
      ClassFinder.findClassNames (
         "artisynth.models", RootModel.class, /*recursive=*/false);
      timer.stop();
      System.out.println ("search time artisynth.models=" + timer.result(1));

      timer.start();
      ClassFinder.findClassNames (
         "artisynth.models", RootModel.class, /*recursive=*/true);
      timer.stop();
      System.out.println ("search time artisynth.models.*=" + timer.result(1));
   }

   public void testSpecial() {
      List<String> names = ClassFinder.findClassNames (
         "artisynth.models.eth_shoulder", RootModel.class, /*recursive=*/true);
      for (String s : names) {
         System.out.println (s);
      }
   }

   public void test() {
      testIndexing ();
      testCacheFile ();
      testUpdateScenario ();
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      RootModelManagerTest tester = new RootModelManagerTest();
      //tester.timing();
      //tester.testSpecial();
      tester.runtest();
   }

}



