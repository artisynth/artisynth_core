/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

/**
 * Base class for storing information about changes within a component
 * hierarchy.
 */
public class ComponentChangeEvent {
   protected Code myCode;
   protected ModelComponent myComp;

   public enum Code {
      /**
       * Components have been added or deleted.
       */
      STRUCTURE_CHANGED,

      /**
       * Geometry has changed in one or more components.
       */
      GEOMETRY_CHANGED,

      /**
       * Properties have been changed in one or more components.
       */
      PROPERTY_CHANGED,

      /**
       * Dynamic actvity has changed in one or more components.
       */
      DYNAMIC_ACTIVITY_CHANGED,

      /**
       * A Component's name has changed
       */
      NAME_CHANGED
   }

   public ComponentChangeEvent (Code code) {
      myCode = code;
      myComp = null;
   }

   public ComponentChangeEvent (Code code, ModelComponent comp) {
      myCode = code;
      myComp = comp;
   }

   public Code getCode() {
      return myCode;
   }

   public ModelComponent getComponent() {
      return myComp;
   }

   public String toString() {
      String str = "ComponentChangeEvent: " + myCode;
      if (myComp != null) {
         str += " " + myComp.getClass();
      }
      return str;
   }
}
