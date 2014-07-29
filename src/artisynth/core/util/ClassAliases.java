/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.LinkedHashMap;

import maspack.util.ClassFinder;
import maspack.util.ReaderTokenizer;
import artisynth.core.modelbase.ModelComponent;

public class ClassAliases {
   private static LinkedHashMap<String,Class<?>> aliasesToClasses =
      new LinkedHashMap<String,Class<?>>();
   private static LinkedHashMap<Class<?>,String> classesToAliases =
      new LinkedHashMap<Class<?>,String>();

   // private static boolean initFromFileP = false;
   private static boolean initialized = false;

   protected static String[] packageList =
      new String[] {
      "artisynth.core.modelbase.",
      "artisynth.core.mechmodels.",
      "artisynth.core.femmodels.",
      "artisynth.core.mfreemodels.",
      "artisynth.core.renderables.",};

   protected static String[] aliasTable =
      new String[] {
      "Tet", "artisynth.core.femmodels.TetElement",
      "Hex", "artisynth.core.femmodels.HexElement",
      "Wedge", "artisynth.core.femmodels.WedgeElement",
      "Pyramid", "artisynth.core.femmodels.PyramidElement",
      "Quadtet", "artisynth.core.femmodels.QuadtetElement",
      "Quadhex", "artisynth.core.femmodels.QuadhexElement",
      "Quadwedge", "artisynth.core.femmodels.QuadwedgeElement",
      "Quadpyramid", "artisynth.core.femmodels.QuadpyramidElement",

      "NumericInputProbe", "artisynth.core.probes.NumericInputProbe",

      "NumericOutputProbe", "artisynth.core.probes.NumericOutputProbe",

      "PointParticleAttachment",
      "artisynth.core.mechmodels.PointParticleAttachment",

      "PointFrameAttachment",
      "artisynth.core.mechmodels.PointFrameAttachment", };

   /**
    * Searches for subclasses of T within a specified package and adds
    * them to this table, using their simple names as aliases.
    */
   static public void addClasses (String pkgName, String regex, Class<?> T) {
      try {
         ArrayList<Class<?>> list = ClassFinder.findClasses (pkgName, regex, T);
         for (int i=0; i<list.size(); i++) {
            Class<?> cls = list.get(i);
            if (!cls.isInterface()) {
               String alias = cls.getSimpleName();
               if (classesToAliases.get (cls) == null &&
                   aliasesToClasses.get (alias) == null) {
                  doAddEntry (alias, cls);
               }
            }
         }
      }
      catch (Exception e) {
         // just carry on 
      }
   }

   protected static void initializeFromFile (File file) throws IOException {
      try {
         ReaderTokenizer rtok =
            new ReaderTokenizer (new BufferedReader (new FileReader (file)));
         rtok.commentChar ('#');
         rtok.wordChar ('/');
         rtok.wordChar ('.');
         rtok.wordChar ('$');
         rtok.wordChar ('_');
         rtok.wordChar ('-');
         String alias = null;
         while (rtok.nextToken() != ReaderTokenizer.TT_EOF
         && rtok.tokenIsWordOrQuotedString ('"')) {
            if (alias == null) {
               alias = rtok.sval;
            }
            else {
               Class<?> cls;
               try {
                  cls = Class.forName (rtok.sval);
               }
               catch (Exception e) {
                  System.err.println ("Error: class \"" + rtok.sval
                  + "\" in file " + file.getAbsolutePath()
                  + " cannot be located");
                  cls = null;
               }
               if (cls != null) {
                  doAddEntry (alias, cls);
               }
            }
         }
         if (rtok.ttype != ReaderTokenizer.TT_EOF) {
            throw new IOException ("Warning: unknown token "
            + rtok.tokenName() + " in file " + file.getAbsolutePath());
         }
      }
      catch (Exception e) {
         throw new IOException ("Warning: error reading file "
         + file.getAbsolutePath() + "\n" + e.getMessage());
      }
   }

   protected static void initializeFromTable (String[] table) {
      for (int i = 0; i < table.length; i += 2) {
         String alias = table[i];
         String className = table[i + 1];
         Class<?> cls;
         try {
            cls = Class.forName (className);
         }
         catch (Exception e) {
            System.err.println ("Error: class \"" + className
            + "\" in aliasTable cannot be located");
            cls = null;
         }
         if (cls != null) {
            doAddEntry (alias, cls);
         }
      }
   }

   private static void initialize() {
      initializeFromTable (aliasTable);
      addClasses ("artisynth.core", ".*", ModelComponent.class);
      initialized = true;
   }

   private static void doAddEntry (String alias, Class<?> cls) {
      aliasesToClasses.put (alias, cls);
      classesToAliases.put (cls, alias);
   }

   public static void addEntry (String alias, Class<?> cls) {
      if (!initialized) {
         initialize();
      }
      doAddEntry (alias, cls);
   }

   public static Class<?> getClass (String alias) {
      if (!initialized) {
         initialize();
      }
      return aliasesToClasses.get (alias);
   }

   public static String getAlias (Class<?> cls) {
      if (!initialized) {
         initialize();
      }
      return classesToAliases.get (cls);
   }

   public static String getAliasOrName (Class<?> cls) {
      if (!initialized) {
         initialize();
      }
      String alias = classesToAliases.get (cls);
      if (alias == null) {
         return cls.getName();
      }
      else {
         return alias;
      }
   }

   /**
    * Try to find the class associated with a specified name or alias. The input
    * string is first checked to see if it corresponds to a class alias or a
    * valid class name. If not, the method attempts to find a valid class name
    * by appending the input string to a list of predefined package names. If a
    * valid class is found by any of these means the associated class is
    * returned.
    * 
    * @param nameOrAlias
    * string used to identity class
    * @return valid class, or null if no class was found
    */
   public static Class<?> resolveClass (String nameOrAlias) {
      if (!initialized) {
         initialize();
      }
      Class<?> cls;
      if ((cls = getClass (nameOrAlias)) != null) {
         return cls;
      }
      else {
         try {
            return Class.forName (nameOrAlias);
         }
         catch (Exception e) { // just continue
         }
         for (int i = 0; i < packageList.length; i++) {
            try {
               return Class.forName (packageList[i] + nameOrAlias);
            }
            catch (Exception e) { // just continue
            }
         }
         return null;
      }
   }

   public static Object newInstance (String classId, Class<?> superclass)
      throws InstantiationException, IllegalAccessException {
      Class<?> cls;
      if (classId == null) {
         cls = superclass;
      }
      else {
         cls = ClassAliases.resolveClass (classId);
         if (cls == null) {
            throw new IllegalArgumentException ("can't resolve class: "
            + classId);
         }
         if (!superclass.isAssignableFrom (cls)) {
            throw new IllegalArgumentException ("class corresponding to "
            + classId + " not a subclass of " + superclass.getName());
         }
      }
      return cls.newInstance();
   }
   
   public static Object newInstance (String classId, Class<?> superclass, Object... args)
      throws InstantiationException, IllegalAccessException {
      
      Class<?>[] classTypes = new Class<?>[args.length];
      for (int i=0; i<args.length; i++) {
         classTypes[i] = args[i].getClass();
      }
      return newInstance(classId, superclass, classTypes, args);
   }
   
   public static Object newInstance (String classId, Class<?> superclass, Class<?>[] argTypes, Object[] args)
      throws InstantiationException, IllegalAccessException {
      Class<?> cls;
      if (classId == null) {
         cls = superclass;
      }
      else {
         cls = ClassAliases.resolveClass (classId);
         if (cls == null) {
            throw new IllegalArgumentException ("can't resolve class: "
            + classId);
         }
         if (!superclass.isAssignableFrom (cls)) {
            throw new IllegalArgumentException ("class corresponding to "
            + classId + " not a subclass of " + superclass.getName());
         }
      }
      
      Object inst = null;
      try {
         Constructor<?> construct = cls.getDeclaredConstructor(argTypes);
         inst = construct.newInstance(args);
      } catch (Exception e) {
         throw new IllegalArgumentException ("Failed to find constructor: " + e.getMessage(), e);
      }
      
      return inst;
   }

   public static void main (String[] args) {
      initialize();
   }

}
