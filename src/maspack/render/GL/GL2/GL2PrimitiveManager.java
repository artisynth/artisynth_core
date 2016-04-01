package maspack.render.GL.GL2;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import maspack.render.GL.GLGarbageSource;
import maspack.render.GL.GL2.GL2Primitive.PrimitiveKey;
import maspack.render.GL.GL2.GL2Primitive.PrimitiveType;

public class GL2PrimitiveManager implements GLGarbageSource {
   
   /**
    * Primitive that doesn't hold on to the display list
    * @author Antonio
    *
    */
   private static class GL2WeakPrimitive {
      PrimitiveKey key;
      GL2DisplayList displayList;
      GL2WeakPrimitive(PrimitiveKey key, GL2DisplayList list) {
         this.key = key;
         this.displayList = list;
      }
      
      public int getResolution() {
         return key.getResolution ();
      }
      
      public boolean isCapped() {
         return key.isCapped ();
      }
      
      public GL2Primitive createPrimitive() {
         return new GL2Primitive (key, displayList);
      }
      
      public boolean isValid() {
         return displayList.isValid ();
      }
      
      public boolean disposeInvalid(GL2 gl) {
         if (!isValid()) {
            dispose(gl);
            return true;
         }
         return false;
      }
      
      public void dispose(GL2 gl) {
         if (displayList != null) {
            displayList.dispose (gl);
            displayList = null;
         }
      }
   }
   
   HashMap<PrimitiveKey, GL2WeakPrimitive> primitiveMap;
   
   // track last obtained primitives to improve efficiency (avoiding hashmap lookup)
   GL2WeakPrimitive lastCone = null;
   GL2WeakPrimitive lastCylinder = null;
   GL2WeakPrimitive lastSphere = null;
   GL2WeakPrimitive lastSpindle = null;
   
   public GL2PrimitiveManager() {
      primitiveMap = new HashMap<>();
   }
   
   public GL2Primitive getSphere (GL2 gl, int resolution) {
      
      // if last acquired sphere is correct resolution and is valid, use that
      if (lastSphere != null && lastSphere.isValid () && 
         lastSphere.getResolution () == resolution) {
         return lastSphere.createPrimitive ();
      }

      GL2WeakPrimitive weak = null;
      PrimitiveKey key = new PrimitiveKey (PrimitiveType.SPHERE, resolution, false);
      
      // look in map to see if there
      synchronized (primitiveMap) {
         weak = primitiveMap.get (key);

         // if doesn't exist, create it. 
         if (weak == null || weak.disposeInvalid (gl)) {
            GL2DisplayList list = GL2PrimitiveFactory.createSphere (gl, resolution, resolution);
            weak = new GL2WeakPrimitive(key, list);
            primitiveMap.put (key, weak);
         } 
      }
      
      lastSphere = weak;
      return weak.createPrimitive ();      
   }
   
   public GL2Primitive getSpindle (GL2 gl, int resolution) {
      
      // if last acquired Spindle is correct resolution and is valid, use that
      if (lastSpindle != null && lastSpindle.getResolution () == resolution 
         && lastSpindle.isValid ()) {
         return lastSpindle.createPrimitive ();
      }

      GL2WeakPrimitive weak = null;
      PrimitiveKey key = new PrimitiveKey (PrimitiveType.SPINDLE, resolution, false);
      
      // look in map to see if there
      synchronized (primitiveMap) {
         weak = primitiveMap.get (key);

         // if doesn't exist, create it. 
         if (weak == null || weak.disposeInvalid (gl)) {
            GL2DisplayList list = GL2PrimitiveFactory.createSpindle (gl, resolution, resolution);
            weak = new GL2WeakPrimitive(key, list);
            primitiveMap.put (key, weak);
         } 
      }
      
      lastSpindle = weak;
      return weak.createPrimitive ();      
   }
   
   public GL2Primitive getCone (GL2 gl, int resolution, boolean capped) {
      
      // if last acquired Cone is correct resolution and is valid, use that
      if (lastCone != null && lastCone.getResolution () == resolution && 
          lastCone.isCapped () == capped && lastCone.isValid ()) {
         return lastCone.createPrimitive ();
      }

      GL2WeakPrimitive weak = null;
      PrimitiveKey key = new PrimitiveKey (PrimitiveType.CONE, resolution, capped);
      
      // look in map to see if there
      synchronized (primitiveMap) {
         weak = primitiveMap.get (key);

         // if doesn't exist, create it. 
         if (weak == null || weak.disposeInvalid (gl)) {
            GL2DisplayList list = GL2PrimitiveFactory.createCone (gl, resolution, capped);
            weak = new GL2WeakPrimitive(key, list);
            primitiveMap.put (key, weak);
         } 
      }
      
      lastCone = weak;
      return weak.createPrimitive ();      
   }
   
   public GL2Primitive getCylinder (GL2 gl, int resolution, boolean capped) {
      
      // if last acquired Cylinder is correct resolution and is valid, use that
      if (lastCylinder != null && lastCylinder.getResolution () == resolution && 
         lastCylinder.isCapped () == capped && lastCylinder.isValid ()) {
         return lastCylinder.createPrimitive ();
      }

      GL2WeakPrimitive weak = null;
      PrimitiveKey key = new PrimitiveKey (PrimitiveType.CYLINDER, resolution, capped);
      
      // look in map to see if there
      synchronized (primitiveMap) {
         weak = primitiveMap.get (key);

         // if doesn't exist, create it. 
         if (weak == null || weak.disposeInvalid (gl)) {
            GL2DisplayList list = GL2PrimitiveFactory.createCylinder (gl, resolution, capped);
            weak = new GL2WeakPrimitive(key, list);
            primitiveMap.put (key, weak);
         } 
      }
      
      lastCylinder = weak;
      return weak.createPrimitive ();      
   }

   @Override
   public void garbage (GL gl) {      
      // loop through resources
      synchronized(primitiveMap) {
         Iterator<Entry<PrimitiveKey,GL2WeakPrimitive>> it = primitiveMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<PrimitiveKey,GL2WeakPrimitive> entry = it.next ();
            GL2DisplayList list = entry.getValue ().displayList;
            if (list.disposeInvalid (gl) || list.disposeUnreferenced (gl)) {
               it.remove ();
            }
         }
      }
   }

   /**
    * Destroy all stored resources
    * @param gl
    */
   public void disposeResources(GL gl) {
      // destroy all stored displaylist
      synchronized(primitiveMap) {
         for (Entry<PrimitiveKey,GL2WeakPrimitive> entry : primitiveMap.entrySet ()) {
            entry.getValue ().displayList.dispose (gl);
         }
         primitiveMap.clear ();
      }
   }
  
   @Override
   public void dispose (GL gl) {
      
      GL2 gl2 = (GL2)gl;
      synchronized(primitiveMap) {
         for (GL2WeakPrimitive prim : primitiveMap.values ()) {
            prim.dispose (gl2);
         }
         primitiveMap.clear ();
      }
      
      lastCone = null;
      lastCylinder = null;
      lastSphere = null;
      lastSpindle = null;
      
   }

}
