/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;

import maspack.util.ClassFinder;

public class ClassAliases {
   private static LinkedHashMap<String,String> aliasesToClasses =
      new LinkedHashMap<String,String>();
   private static LinkedHashMap<String,String> classesToAliases =
      new LinkedHashMap<String,String>();

   private static HashSet<Class<?>> myValidClasses = new HashSet<>();
   
   public interface ClassFilter {
      public boolean isValid (Class<?> cls);
   }
   
   private static ClassFilter myClassFilter = null;
   
   public static void setClassFilter (ClassFilter filter) {
      myClassFilter = filter;
      myValidClasses.clear();
   }
   
   public static ClassFilter getClassFilter () {
      return myClassFilter;
   }

   public static boolean isClassValid (Class<?> cls) {

      if (myClassFilter != null) {
         if (myValidClasses.contains(cls)) {
            return true;
         }
         if (myClassFilter.isValid (cls)) {
            myValidClasses.add (cls);
            return true;
         }
         else {
            return false;
         }
      }
      else {
         return true;
      }
   }
   
   /**
    * Checks a component to see if its class is valid according to any class
    * filter which has been set. If the component is a ParameterizedClass, 
    * then its parameter type is checked as well.
    */
   public static <C> boolean isClassValid (Object comp) {
      
      if (!isClassValid (comp.getClass())) {
         return false;
      }
      if (comp instanceof ParameterizedClass) {
         ParameterizedClass pcomp = (ParameterizedClass)comp;
         if (pcomp.hasParameterizedType()) {
            Class<?> paramType = pcomp.getParameterType();
            if (!isClassValid (paramType)) {
               return false;
            }
         }
      }
      return true;
   }   

   /**
    * Searches for the subclasses of T within a specified package whose full
    * name match the specified regular expression, and adds them to the alias
    * table, using their simple names as a key.
    */
   static public void addPackageAliases (
      String pkgName, String regex, Class<?> T) {
      try {
         ArrayList<Class<?>> list = ClassFinder.findClasses (pkgName, regex, T);
         for (int i=0; i<list.size(); i++) {
            Class<?> cls = list.get(i);
            if (!cls.isInterface()) {
               addAlias (cls.getSimpleName(), cls);
            }
         }
      }
      catch (Exception e) {
         // keep calm and carry on
     }
   }   

   public static boolean addAlias (String alias, Class<?> cls) {
      String currentClass = aliasesToClasses.get (alias);
      if (currentClass == null) {
         aliasesToClasses.put (alias, cls.getName());
         classesToAliases.put (cls.getName(), alias);
         return true;
      }
      else if (!currentClass.equals(cls.getName())) {
         System.err.println (
            "Warning: class alias \""+alias+"\" already assigned to "+
            currentClass);
         return false;
      }
      else {
         return false;
      }
   }
   
   public static boolean addAlias (String alias, String className) {
      Class<?> cls;
      try {
         cls = Class.forName (className);
      }
      catch (Exception e) {
         System.err.println (
            "Error: class \"" + className + "\" cannot be located");
         return false;
      }      
      return addAlias (alias, cls);
   }
   
   private static Class<?> getClass (String alias) {
      Class<?> clazz = null;
      try {
         String classname = aliasesToClasses.get (alias);
         if (classname != null) {
            clazz = Class.forName(classname);
         }
      } catch (ClassNotFoundException e) {
      }
      return clazz;
   }

   public static String getAlias (Class<?> cls) {
      return classesToAliases.get (cls.getName());
   }

   public static String getAliasOrName (Class<?> cls) {
      String alias = classesToAliases.get (cls.getName());
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
   
   public static Object newInstance (
      String classId, Class<?> superclass, Object... args)
      throws InstantiationException, IllegalAccessException {
      
      Class<?>[] classTypes = new Class<?>[args.length];
      for (int i=0; i<args.length; i++) {
         classTypes[i] = args[i].getClass();
      }
      return newInstance(classId, superclass, classTypes, args);
   }
   
   public static Object newInstance (
      String classId, Class<?> superclass, Class<?>[] argTypes, Object[] args)
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
         throw new IllegalArgumentException (
            "Failed to find constructor: " + e.getMessage(), e);
      }
      
      return inst;
   }

   public static void main (String[] args) {
   }

}
