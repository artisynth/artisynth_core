package maspack.render.GL.GL3;

import java.util.HashMap;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.GL.GLGarbageSource;
import maspack.render.GL.GL3.GL3SharedPrimitive.AxesKey;
import maspack.render.GL.GL3.GL3SharedPrimitive.ConeKey;
import maspack.render.GL.GL3.GL3SharedPrimitive.CubeKey;
import maspack.render.GL.GL3.GL3SharedPrimitive.CylinderKey;
import maspack.render.GL.GL3.GL3SharedPrimitive.SphereKey;
import maspack.render.GL.GL3.GL3SharedPrimitive.SpindleKey;

/**
 * Manager for UNSHARED primitive resources
 * @author Antonio
 *
 */
public class GL3PrimitiveManager implements GLGarbageSource {

   GL3SharedPrimitiveManager shared;
   HashMap<Object,GL3Primitive> primitiveMap;
   
   
   GL3Primitive lastAxes;
   GL3Primitive lastCone;
   GL3Primitive lastCylinder;
   GL3Primitive lastSpindle;
   GL3Primitive lastSphere;
   GL3Primitive lastCube;
   
   public GL3PrimitiveManager(GL3SharedPrimitiveManager shared) {
      this.shared = shared;
      primitiveMap = new HashMap<> ();
      
      lastAxes = null;
      lastCone = null;
      lastCylinder = null;
      lastSpindle = null;
      lastSphere = null;
      lastCube = null;
   }
   
   /**
    * Returns a sphere object
    * @return sphere object 
    */
   public GL3Primitive getAcquiredSphere(GL3 gl, int nSlices, int nLevels) {
      SphereKey key = new SphereKey(nSlices, nLevels);
      return getAcquiredSphere(gl, key);
   }
      
   public GL3Primitive getAcquiredSphere(GL3 gl, SphereKey key) {
      
      if (lastSphere != null) {
         if (lastSphere.isValid() && lastSphere.matches(key)) {
            return lastSphere.acquire ();
         } else {
            lastSphere.releaseDispose (gl);
            lastSphere = null;
         }
      }
      
      GL3Primitive out = null;
      
      synchronized(primitiveMap) {
         out = primitiveMap.get (key);         
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedPrimitive p = shared.getAcquiredSphere (gl, key);
            out = GL3Primitive.generate (gl, p);
            p.release ();  // now that it has been acquired
            primitiveMap.put (key, out.acquire ()); // hold a reference in the map
         }
         out.acquire ();
      }
      
      lastSphere = out.acquire ();
      return out;
   }

   public GL3Primitive getAcquiredSpindle(GL3 gl, int nSlices, int nLevels) {

      SpindleKey key = new SpindleKey(nSlices, nLevels);
      return getAcquiredSpindle(gl, key);
   }
   
   public GL3Primitive getAcquiredSpindle(GL3 gl, SpindleKey key) {
      
      if (lastSpindle != null) {
         if (lastSpindle.isValid() && lastSpindle.matches(key)) {
            return lastSpindle.acquire ();
         } else {
            lastSpindle.releaseDispose (gl);
            lastSpindle = null;
         }
      }
      
      GL3Primitive out = null;
      
      synchronized(primitiveMap) {
         out = primitiveMap.get (key);         
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedPrimitive p = shared.getAcquiredSpindle (gl, key);
            out = GL3Primitive.generate (gl, p);
            p.release ();  // now that it has been acquired
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire ();
      }
      
      lastSpindle = out.acquire ();
      return out;
   }

   public GL3Primitive getAcquiredCylinder(GL3 gl, int nSlices, boolean capped) {
      
      CylinderKey key = new CylinderKey(nSlices, capped);
      return getAcquiredCylinder(gl, key);
   }

   public GL3Primitive getAcquiredCylinder(GL3 gl, CylinderKey key) {
      if (lastCylinder != null) {
         if (lastCylinder.isValid() && lastCylinder.matches(key)) {
            return lastCylinder.acquire ();
         } else {
            lastCylinder.releaseDispose (gl);
            lastCylinder = null;
         }
      }
      
      GL3Primitive out = null;
      
      synchronized(primitiveMap) {
         out = primitiveMap.get (key);         
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedPrimitive p = shared.getAcquiredCylinder (gl, key);
            out = GL3Primitive.generate (gl, p);
            p.release ();  // now that it has been acquired
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire ();
      }
      
      lastCylinder = out.acquire ();
      return out;
   }

   public GL3Primitive getAcquiredCone(GL3 gl, int nSlices, boolean capped) {
      ConeKey key = new ConeKey(nSlices, capped);
      return getAcquiredCone(gl, key);
   }
   
   public GL3Primitive getAcquiredCone(GL3 gl, ConeKey key) {
      if (lastCone != null) {
         if (lastCone.isValid() && lastCone.matches(key)) {
            return lastCone.acquire ();
         } else {
            lastCone.releaseDispose (gl);
            lastCone = null;
         }
      }
      
      GL3Primitive out = null;
      
      synchronized(primitiveMap) {
         out = primitiveMap.get (key);         
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedPrimitive p = shared.getAcquiredCone (gl, key);
            out = GL3Primitive.generate (gl, p);
            p.release ();  // now that it has been acquired
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire ();
      }
      
      lastCone = out.acquire ();
      return out;
   }

   public GL3Primitive getAcquiredAxes(GL3 gl, boolean x, boolean y, boolean z) {
      
      AxesKey key = new AxesKey(x, y, z);
      return getAcquiredAxes(gl, key);
   }
   
   public GL3Primitive getAcquiredAxes(GL3 gl, AxesKey key) {
      if (lastAxes != null) {
         if (lastAxes.isValid() && lastAxes.matches(key)) {
            return lastAxes.acquire ();
         } else {
            lastAxes.releaseDispose (gl);
            lastAxes = null;
         }
      }
      
      GL3Primitive out = null;
      
      synchronized(primitiveMap) {
         out = primitiveMap.get (key);         
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedPrimitive p = shared.getAcquiredAxes (gl, key);
            out = GL3Primitive.generate (gl, p);
            p.release ();  // now that it has been acquired
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire ();
      }
      
      lastAxes = out.acquire ();
      return out;
   }
   
   public GL3Primitive getAcquiredCube(GL3 gl) {
      CubeKey key = new CubeKey();
      return getAcquiredCube(gl, key);
   }
   
   public GL3Primitive getAcquiredCube(GL3 gl, CubeKey key) {
      if (lastCube != null) {
         if (lastCube.isValid() && lastCube.matches(key)) {
            return lastCube.acquire ();
         } else {
            lastCube.releaseDispose (gl);
            lastCube = null;
         }
      }
      
      GL3Primitive out = null;
      
      synchronized(primitiveMap) {
         out = primitiveMap.get (key);         
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedPrimitive p = shared.getAcquiredCube (gl, key);
            out = GL3Primitive.generate (gl, p);
            p.release ();  // now that it has been acquired
         }
         out.acquire ();
      }
      
      lastCube = out.acquire ();
      return out;
   }

   @Override
   public void garbage (GL gl) {
      
      // XXX we now hold a reference in the map, so will never be unreferenced
      //      // loop through resources
      //      synchronized(primitiveMap) {
      //         Iterator<Entry<Object,GL3Primitive>> it = primitiveMap.entrySet ().iterator ();
      //         while (it.hasNext ()) {
      //            Entry<Object,GL3Primitive> entry = it.next ();
      //            GL3Primitive primitive = entry.getValue ();
      //            if (!primitive.isValid () || primitive.disposeUnreferenced ((GL3)gl)) {
      //               it.remove ();
      //            }
      //         }
      //      }
   }
   
   /**
    * Destroy all stored resources
    */
   @Override
   public void dispose(GL gl) {
      
      if (lastAxes != null) {
         lastAxes.releaseDispose (gl);
         lastAxes = null;
      }
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
      
      // destroy all stored primitives
      synchronized(primitiveMap) {
         for (GL3Primitive prim : primitiveMap.values ()) {
            prim.releaseDispose (gl);
         }
         primitiveMap.clear ();
      }
   }
   
}
