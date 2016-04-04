package maspack.render.GL.GL3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import maspack.render.GL.GLGarbageSource;
import maspack.render.GL.GL3.GL3SharedPrimitiveManager.AxesKey;
import maspack.render.GL.GL3.GL3SharedPrimitiveManager.ConeKey;
import maspack.render.GL.GL3.GL3SharedPrimitiveManager.CylinderKey;
import maspack.render.GL.GL3.GL3SharedPrimitiveManager.SphereKey;
import maspack.render.GL.GL3.GL3SharedPrimitiveManager.SpindleKey;

/**
 * Manager for UNSHARED resources
 * @author Antonio
 *
 */
public class GL3PrimitiveManager implements GLGarbageSource {

   GL3SharedPrimitiveManager shared;
   HashMap<Object,GL3Primitive> primitiveMap;
   
   private static class GL3Primitive {
      Object key;
      GL3Object glo;
      
      public GL3Primitive(Object key, GL3Object glo) {
         this.glo = glo;
         this.key = key;
      }
      
      public GL3Object getPrimitive() {
         return glo;
      }
      
      public boolean isValid() {
         return glo.isValid ();
      }
      
      public boolean disposeInvalid(GL3 gl) {
         if (!isValid()) {
            dispose(gl);
            return true;
         }
         return false;
      }
      
      public void dispose(GL3 gl) {
         if (glo != null) {
            glo.releaseDispose (gl);
            glo = null;
         }
      }
   }
   
   GL3Primitive lastAxes;
   GL3Primitive lastCone;
   GL3Primitive lastCylinder;
   GL3Primitive lastSpindle;
   GL3Primitive lastSphere;
   
   public GL3PrimitiveManager(GL3SharedPrimitiveManager shared) {
      this.shared = shared;
      primitiveMap = new HashMap<> ();
      
      lastAxes = null;
      lastCone = null;
      lastCylinder = null;
      lastSpindle = null;
      lastSphere = null;
   }
   
   /**
    * Returns a sphere object
    * @param gl
    * @param nSlices
    * @param nLevels
    * @return
    */
   public GL3Object getSphere(GL3 gl, int nSlices, int nLevels) {
      SphereKey key = new SphereKey(nSlices, nLevels);
      return getSphere(gl, key);
   }
      
   public GL3Object getSphere(GL3 gl, SphereKey key) {
      
      if (lastSphere != null && lastSphere.isValid() &&
         lastSphere.key.equals(key)) {
         return lastSphere.getPrimitive();
      }
      
      GL3Primitive out = null;
      
      synchronized(primitiveMap) {
         out = primitiveMap.get (key);
         
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedObject primitive = shared.getSphere (gl, key);
            out = new GL3Primitive (key, GL3Object.generate (gl, primitive));
            primitiveMap.put (key, out);
         }
      }
      
      lastSphere = out;
      return out.getPrimitive ();
   }

   public GL3Object getSpindle(GL3 gl, int nSlices, int nLevels) {

      SpindleKey key = new SpindleKey(nSlices, nLevels);
      return getSpindle(gl, key);
   }
   
   public GL3Object getSpindle(GL3 gl, SpindleKey key) {
      
      if (lastSpindle != null && lastSpindle.isValid() &&
         lastSpindle.key.equals(key)) {
         return lastSpindle.getPrimitive();
      }

      GL3Primitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedObject primitive = shared.getSpindle (gl, key);
            out = new GL3Primitive (key, GL3Object.generate (gl, primitive));
            primitiveMap.put (key, out);
         }
      }

      lastSpindle = out;
      return out.getPrimitive();
   }

   public GL3Object getCylinder(GL3 gl, int nSlices, boolean capped) {
      
      CylinderKey key = new CylinderKey(nSlices, capped);
      return getCylinder(gl, key);
   }

   public GL3Object getCylinder(GL3 gl, CylinderKey key) {
      if (lastCylinder != null && lastCylinder.isValid() &&
         lastCylinder.key.equals(key)) {
         return lastCylinder.getPrimitive();
      }

      GL3Primitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedObject primitive = shared.getCylinder (gl, key);
            out = new GL3Primitive (key, GL3Object.generate (gl, primitive));
            primitiveMap.put (key, out);
         }
      }

      lastCylinder = out;
      return out.getPrimitive();
   }

   public GL3Object getCone(GL3 gl, int nSlices, boolean capped) {
      ConeKey key = new ConeKey(nSlices, capped);
      return getCone(gl, key);
   }
   
   public GL3Object getCone(GL3 gl, ConeKey key) {
      if (lastCone != null && lastCone.isValid() &&
         lastCone.key.equals(key)) {
         return lastCone.getPrimitive();
      }

      GL3Primitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedObject primitive = shared.getCone (gl, key);
            out = new GL3Primitive (key, GL3Object.generate (gl, primitive));
            primitiveMap.put (key, out);
         }
      }

      lastCone = out;
      return out.getPrimitive();
   }

   public GL3Object getAxes(GL3 gl, boolean x, boolean y, boolean z) {
      
      AxesKey key = new AxesKey(x, y, z);
      return getAxes(gl, key);
   }
   
   public GL3Object getAxes(GL3 gl, AxesKey key) {
      if (lastAxes != null && lastAxes.isValid() &&
         lastAxes.key.equals(key)) {
         return lastAxes.getPrimitive();
      }

      GL3Primitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3SharedObject primitive = shared.getAxes (gl, key);
            out = new GL3Primitive (key, GL3Object.generate (gl, primitive));
            primitiveMap.put (key, out);
         }
      }

      lastAxes = out;
      return out.getPrimitive();
   }

   @Override
   public void garbage (GL gl) {
      // loop through resources
      synchronized(primitiveMap) {
         Iterator<Entry<Object,GL3Primitive>> it = primitiveMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<Object,GL3Primitive> entry = it.next ();
            GL3Object primitive = entry.getValue ().glo;
            if (!primitive.isValid () || primitive.disposeUnreferenced ((GL3)gl)) {
               it.remove ();
            }
         }
      }
   }
   
   /**
    * Destroy all stored resources
    * @param gl
    */
   @Override
   public void dispose(GL gl) {      
      // destroy all stored primitives
      synchronized(primitiveMap) {
         for (Entry<Object,GL3Primitive> entry : primitiveMap.entrySet ()) {
            entry.getValue ().glo.dispose ((GL3)gl);
         }
         primitiveMap.clear ();
      }
   }
   
}
