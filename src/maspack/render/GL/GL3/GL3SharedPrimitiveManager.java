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
 * Manages the lifetime of primitive GLObjects, keeping track of usage (incremented when grabbing)
 * so that unused objects can be detected and cleared. 
 */
public class GL3SharedPrimitiveManager implements GLGarbageSource {

   private final GL3PrimitiveFactory factory;
   private HashMap<Object,GL3SharedPrimitive> primitiveMap;
   GL3SharedPrimitive lastAxes;
   GL3SharedPrimitive lastCone;
   GL3SharedPrimitive lastCylinder;
   GL3SharedPrimitive lastSpindle;
   GL3SharedPrimitive lastSphere;
   GL3SharedPrimitive lastCube;

   public GL3SharedPrimitiveManager(GL3PrimitiveFactory factory) {
      this.factory = factory;
      primitiveMap = new HashMap<>();
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
   public GL3SharedPrimitive getAcquiredSphere(GL3 gl, int nSlices, int nLevels) {
      SphereKey key = new SphereKey(nSlices, nLevels);
      return getAcquiredSphere(gl, key);
   }
      
   public GL3SharedPrimitive getAcquiredSphere(GL3 gl, SphereKey key) {
      
      if (lastSphere != null) {
         if (lastSphere.isValid() && lastSphere.key.equals(key)) {
            return lastSphere.acquire();
         } else {
            lastSphere.releaseDispose (gl);
            lastSphere = null;
         }
      }
      
      GL3SharedPrimitive out = null;
      
      synchronized(primitiveMap) {
         out = primitiveMap.get (key);
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            out = factory.createSphere (gl, key.getSlices (), key.getLevels ());
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire ();
      }
      
      lastSphere = out.acquire();
      return out;
   }

   public GL3SharedPrimitive getAcquiredSpindle(GL3 gl, int nSlices, int nLevels) {

      SpindleKey key = new SpindleKey(nSlices, nLevels);
      return getAcquiredSpindle(gl, key);
   }
   
   public GL3SharedPrimitive getAcquiredSpindle(GL3 gl, SpindleKey key) {
      
      if (lastSpindle != null) {
         if (lastSpindle.isValid() && lastSpindle.key.equals(key)) {
            return lastSpindle.acquire();
         } else {
            lastSpindle.releaseDispose (gl);
            lastSpindle = null;
         }
      }

      GL3SharedPrimitive out = null;
      synchronized(primitiveMap) {
         out = primitiveMap.get (key);
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            out = factory.createSpindle (gl, key.getSlices (), key.getLevels ());
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire();
      }

      lastSpindle = out.acquire();
      return out;
   }

   public GL3SharedPrimitive getAcquiredCylinder(GL3 gl, int nSlices, boolean capped) {
      
      CylinderKey key = new CylinderKey(nSlices, capped);
      return getAcquiredCylinder(gl, key);
   }

   public GL3SharedPrimitive getAcquiredCylinder(GL3 gl, CylinderKey key) {
      if (lastCylinder != null) {
         if (lastCylinder.isValid() && lastCylinder.key.equals(key)) {
            return lastCylinder.acquire();
         } else {
            lastCylinder.releaseDispose (gl);
            lastCylinder = null;
         }
      }

      GL3SharedPrimitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            out = factory.createCylinder (gl, key.getSlices (), key.isCapped ());
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire();
      }

      lastCylinder = out.acquire();
      return out;
   }

   public GL3SharedPrimitive getAcquiredCone(GL3 gl, int nSlices, boolean capped) {
      ConeKey key = new ConeKey(nSlices, capped);
      return getAcquiredCone(gl, key);
   }
   
   public GL3SharedPrimitive getAcquiredCone(GL3 gl, ConeKey key) {
      if (lastCone != null) {
         if (lastCone.isValid() && lastCone.key.equals(key)) {
            return lastCone.acquire();
         } else {
            lastCone.releaseDispose (gl);
            lastCone = null;
         }
      }

      GL3SharedPrimitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            out = factory.createCone (gl, key.getSlices (), key.isCapped ());
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire();
      }

      lastCone = out.acquire();
      return out;
   }

   public GL3SharedPrimitive getAcquiredAxes(GL3 gl, boolean x, boolean y, boolean z) {
      AxesKey key = new AxesKey(x, y, z);
      return getAcquiredAxes(gl, key);
   }
   
   public GL3SharedPrimitive getAcquiredAxes(GL3 gl, AxesKey key) {
      if (lastAxes != null) {
         if (lastAxes.isValid() && lastAxes.key.equals(key)) {
            return lastAxes.acquire();
         } else {
            lastAxes.releaseDispose (gl);
            lastAxes = null;
         }
      }

      GL3SharedPrimitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            out = factory.createAxes (gl, key.hasX (), key.hasY (), key.hasZ ());
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire();
      }

      lastAxes = out.acquire();
      return out;
   }
   
   public GL3SharedPrimitive getAcquiredCube(GL3 gl, CubeKey key) {
      if (lastCube != null) {
         if (lastCube.isValid() && lastCube.key.equals(key)) {
            return lastCube.acquire();
         } else {
            lastCube.releaseDispose (gl);
            lastCube = null;
         }
      }

      GL3SharedPrimitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            out = factory.createCube (gl);
            primitiveMap.put (key, out.acquire ());
         }
         out.acquire ();
      }

      lastCube = out.acquire ();
      return out;
   }

   @Override
   public void garbage (GL gl) {
      // Map now holds on to all primitives
      //      // loop through resources
      //      synchronized(primitiveMap) {
      //         Iterator<Entry<Object,GL3SharedPrimitive>> it = primitiveMap.entrySet ().iterator ();
      //         while (it.hasNext ()) {
      //            Entry<Object,GL3SharedPrimitive> entry = it.next ();
      //            GL3SharedPrimitive primitive = entry.getValue ();
      //            if (!primitive.isValid () || primitive.releaseDispose ((GL3)gl)) {
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
         for (GL3SharedPrimitive val : primitiveMap.values ()) {
            val.releaseDispose ((GL3)gl);
         }
         primitiveMap.clear ();
      }
   }

}
