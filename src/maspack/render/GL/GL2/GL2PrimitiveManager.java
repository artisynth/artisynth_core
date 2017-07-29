package maspack.render.GL.GL2;

import java.util.HashMap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;

import maspack.render.GL.GLGarbageSource;
import maspack.render.GL.GL2.GL2Primitive.PrimitiveKey;
import maspack.render.GL.GL2.GL2Primitive.PrimitiveType;

public class GL2PrimitiveManager implements GLGarbageSource {
   
   HashMap<PrimitiveKey, GL2Primitive> primitiveMap;
   
   // track last obtained primitives to improve efficiency (avoiding hashmap lookup)
   GL2Primitive lastCone = null;
   GL2Primitive lastCylinder = null;
   GL2Primitive lastSphere = null;
   GL2Primitive lastSpindle = null;
   GL2Primitive lastCube = null;
   
   public GL2PrimitiveManager() {
      primitiveMap = new HashMap<>();
   }
   
   public GL2Primitive getAcquiredSphere (GL2 gl, int resolution) {
      
      // if last acquired sphere is correct resolution and is valid, use that
      if (lastSphere != null) {
         if (lastSphere.isValid () && lastSphere.getResolution () == resolution) {
            return lastSphere.acquire ();  
         } else {
            lastSphere.releaseDispose (gl);
            lastSphere = null;
         }
      }

      GL2Primitive out = null;
      PrimitiveKey key = new PrimitiveKey (PrimitiveType.SPHERE, resolution, false);
      
      // look in map to see if there
      synchronized (primitiveMap) {
         out = primitiveMap.get (key);
         // if doesn't exist, create it. 
         if (out == null || out.disposeInvalid (gl)) {
            GL2DisplayList list = GL2PrimitiveFactory.createSphere (gl, resolution, resolution);
            out = new GL2Primitive(key, list);
            primitiveMap.put (key, out.acquire ());
         } 
         out.acquire ();
      }
      
      lastSphere = out.acquire ();  // keep hold of a reference
      return out;      
   }
   
   public GL2Primitive getAcquiredSpindle (GL2 gl, int resolution) {
      
      // if last acquired Spindle is correct resolution and is valid, use that
      if (lastSpindle != null) {
         if (lastSpindle.isValid () && lastSpindle.getResolution () == resolution) { 
            return lastSpindle.acquire ();
         } else {
            lastSpindle.releaseDispose (gl);
            lastSpindle = null;
         }
      }
      

      GL2Primitive out = null;
      PrimitiveKey key = new PrimitiveKey (PrimitiveType.SPINDLE, resolution, false);
      
      // look in map to see if there
      synchronized (primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create it. 
         if (out == null || out.disposeInvalid (gl)) {
            GL2DisplayList list = GL2PrimitiveFactory.createSpindle (gl, resolution, resolution);
            out = new GL2Primitive(key, list);
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire ();
      }
      
      lastSpindle = out.acquire ();
      return out;      
   }
   
   public GL2Primitive getAcquiredCone (GL2 gl, int resolution, boolean capped) {
      
      // if last acquired Cone is correct resolution and is valid, use that
      if (lastCone != null) {
         if (lastCone.getResolution () == resolution && 
             lastCone.isCapped () == capped && lastCone.isValid ()) {
            return lastCone.acquire ();
         } else {
            lastCone.releaseDispose (gl);
            lastCone = null;
         }
      }
      
      GL2Primitive out = null;
      PrimitiveKey key = new PrimitiveKey (PrimitiveType.CONE, resolution, capped);
      
      // look in map to see if there
      synchronized (primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create it. 
         if (out == null || out.disposeInvalid (gl)) {
            GL2DisplayList list = GL2PrimitiveFactory.createCone (gl, resolution, capped);
            out = new GL2Primitive(key, list);
            primitiveMap.put (key, out.acquire ());
         } 
         out.acquire ();
      }
      
      lastCone = out.acquire ();
      return out;       
   }
   
   public GL2Primitive getAcquiredCylinder (GL2 gl, int resolution, boolean capped) {
      
      // if last acquired Cylinder is correct resolution and is valid, use that
      if (lastCylinder != null) {
         if (lastCylinder.getResolution () == resolution && 
             lastCylinder.isCapped () == capped && lastCylinder.isValid ()) {
            return lastCylinder.acquire ();
         } else {
            lastCylinder.releaseDispose (gl);
            lastCylinder = null;
         }
      }

      GL2Primitive out = null;
      PrimitiveKey key = new PrimitiveKey (PrimitiveType.CYLINDER, resolution, capped);
      
      // look in map to see if there
      synchronized (primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create it. 
         if (out == null || out.disposeInvalid (gl)) {
            GL2DisplayList list = GL2PrimitiveFactory.createCylinder (gl, resolution, capped);
            out = new GL2Primitive(key, list);
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire ();
      }
      
      lastCylinder = out.acquire ();
      return out;       
   }
   
   
   public GL2Primitive getAcquiredCube (GL2 gl) {
      
      // if last acquired Cylinder is correct resolution and is valid, use that
      if (lastCube != null) {
         if (lastCube.isValid ()) {
            return lastCube.acquire ();
         } else {
            lastCube.releaseDispose (gl);
            lastCube = null;
         }
      }

      // create a new cube
      PrimitiveKey key = new PrimitiveKey (PrimitiveType.CUBE, 0, false);
      GL2DisplayList cubeList = GL2PrimitiveFactory.createCube (gl);
      GL2Primitive cube = new GL2Primitive(key, cubeList);
      
      lastCube = cube.acquire ();
      return cube.acquire ();       
   }

   @Override
   public void garbage (GL gl) {
      // Map now holds on to primitives
      //      // loop through resources
      //      synchronized(primitiveMap) {
      //         Iterator<Entry<PrimitiveKey,GL2Primitive>> it = primitiveMap.entrySet ().iterator ();
      //         while (it.hasNext ()) {
      //            Entry<PrimitiveKey,GL2Primitive> entry = it.next ();
      //            GL2DisplayList list = entry.getValue ().displayList;
      //            if (list.disposeInvalid (gl) || list.disposeUnreferenced (gl)) {
      //               it.remove ();
      //            }
      //         }
      //      }
   }

   @Override
   public void dispose (GL gl) {
      
      if (lastCone != null) {
         lastCone.releaseDispose (gl);
         lastCone = null;
      }
      if (lastCylinder != null) {
         lastCylinder.releaseDispose (gl);
         lastCylinder = null;
      }
      if (lastSpindle != null) {
         lastSpindle.releaseDispose (gl);
         lastSpindle = null;
      }
      if (lastSphere != null) {
         lastSphere.releaseDispose (gl);
         lastSphere = null;
      }
      if (lastCube != null) {
         lastCube.releaseDispose (gl);
         lastCube = null;
      }
      
      GL2 gl2 = (GL2)gl;
      synchronized(primitiveMap) {
         for (GL2Primitive prim : primitiveMap.values ()) {
            prim.releaseDispose (gl2);
         }
         primitiveMap.clear ();
      }
      
   }

}
