package maspack.render.GL.GL3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import maspack.render.GL.GLGarbageSource;
import maspack.render.GL.GL3.GL3PrimitiveFactory.GL3ObjectInfo;

/**
 * Manages the lifetime of primitive GLObjects, keeping track of usage (incremented when grabbing)
 * so that unused objects can be detected and cleared. 
 */
public class GL3SharedPrimitiveManager implements GLGarbageSource {

   public static class AxesKey {
      boolean x;
      boolean y;
      boolean z;
      public AxesKey(boolean x, boolean y, boolean z) {
         this.x = x;
         this.y = y;
         this.z = z;
      }
      public boolean hasX() {
         return x;
      }
      public boolean hasY() {
         return y;
      }
      public boolean hasZ() {
         return z;
      }
      public int numAxes() {
         return ( (x ? 1 : 0) + (y ? 1 : 0) + (z ? 1 : 0) );
      }
      @Override
      public int hashCode() {
         return ( (x ? 1 : 0) + (y ? 2 : 0) + (z ? 4 : 0) );
      }
      
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) || (getClass() != obj.getClass()) ) {
            return false;
         }
         AxesKey other = (AxesKey)obj;
         return equals(other.x, other.y, other.z);
      }
      
      public boolean equals(boolean x, boolean y, boolean z) {
         if ( (this.x != x) || (this.y != y) || (this.z != z) ) {
            return false;
         }
         return true;
      }
   }
   
   public static class SphereKey {
      private int slices;
      private int levels;
      public SphereKey(int slices, int levels) {
         this.slices = slices;
         this.levels = levels;
      }
      public int getSlices() {
         return slices;
      }
      public int getLevels() {
         return levels;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + levels;
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) || (getClass() != obj.getClass())) {
            return false;
         }
         SphereKey other = (SphereKey)obj;
         return equals(other.slices, other.levels);
      }
      
      public boolean equals(int slices, int levels) {
         if ((this.levels != levels) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }
   
   public static class SpindleKey {
      private int slices;
      private int levels;
      public SpindleKey(int slices, int levels) {
         this.slices = slices;
         this.levels = levels;
      }
      public int getSlices() {
         return slices;
      }
      public int getLevels() {
         return levels;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + levels;
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) || (getClass() != obj.getClass())) {
            return false;
         }
         SpindleKey other = (SpindleKey)obj;
         return equals(other.slices, other.levels);
      }
      
      public boolean equals(int slices, int levels) {
         if ((this.levels != levels) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }
   
   public static class CylinderKey {
      private int slices;
      boolean capped;
      public CylinderKey(int slices, boolean capped) {
         this.slices = slices;
         this.capped = capped;
      }
      public int getSlices() {
         return slices;
      }
      public boolean isCapped() {
         return capped;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + (capped ? 1231 : 1237);
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) ||  (getClass() != obj.getClass())) {
            return false;
         }
         CylinderKey other = (CylinderKey)obj;
         return equals(other.slices, other.capped);
      }
      
      public boolean equals(int slices, boolean capped) {
         if ((this.capped != capped) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }
   
   public static class ConeKey {
      private int slices;
      boolean capped;
      public ConeKey(int slices, boolean capped) {
         this.slices = slices;
         this.capped = capped;
      }
      public int getSlices() {
         return slices;
      }
      public boolean isCapped() {
         return capped;
      }
      @Override
      public int hashCode() {
         final int prime = 31;
         int result = 1;
         result = prime * result + (capped ? 1231 : 1237);
         result = prime * result + slices;
         return result;
      }
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if ( (obj == null) ||  (getClass() != obj.getClass())) {
            return false;
         }
         ConeKey other = (ConeKey)obj;
         return equals(other.slices, other.capped);
      }
      
      public boolean equals(int slices, boolean capped) {
         if ((this.capped != capped) || (this.slices != slices)) {
            return false;
         }
         return true;
      }
   }
   
   private static class GL3SharedPrimitive {
      GL3SharedObject glo;
      Object key;

      public GL3SharedPrimitive(Object key, GL3SharedObject glo) {
         this.glo = glo;
         this.key = key;
      }
      
      public GL3SharedObject getPrimitive() {
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

   private final GL3PrimitiveFactory factory;
   private HashMap<Object,GL3SharedPrimitive> primitiveMap;
   GL3SharedPrimitive lastAxes;
   GL3SharedPrimitive lastCone;
   GL3SharedPrimitive lastCylinder;
   GL3SharedPrimitive lastSpindle;
   GL3SharedPrimitive lastSphere;

   public GL3SharedPrimitiveManager(GL3PrimitiveFactory factory) {
      this.factory = factory;
      primitiveMap = new HashMap<>();
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
   public GL3SharedObject getSphere(GL3 gl, int nSlices, int nLevels) {
      SphereKey key = new SphereKey(nSlices, nLevels);
      return getSphere(gl, key);
   }
      
   public GL3SharedObject getSphere(GL3 gl, SphereKey key) {
      
      if (lastSphere != null && lastSphere.isValid() &&
         lastSphere.key.equals(key)) {
         return lastSphere.getPrimitive();
      }
      
      GL3SharedPrimitive out = null;
      
      synchronized(primitiveMap) {
         out = primitiveMap.get (key);
         
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3ObjectInfo primitive = factory.createSphere (gl, key.getSlices (), key.getLevels ());
            out = new GL3SharedPrimitive (key, primitive.generate ());
            primitiveMap.put (key, out);
         }
      }
      
      lastSphere = out;
      return out.getPrimitive ();
   }

   public GL3SharedObject getSpindle(GL3 gl, int nSlices, int nLevels) {

      SpindleKey key = new SpindleKey(nSlices, nLevels);
      return getSpindle(gl, key);
   }
   
   public GL3SharedObject getSpindle(GL3 gl, SpindleKey key) {
      
      if (lastSpindle != null && lastSpindle.isValid() &&
         lastSpindle.key.equals(key)) {
         return lastSpindle.getPrimitive();
      }

      GL3SharedPrimitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);
         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3ObjectInfo primitive = factory.createSpindle (gl, key.getSlices (), key.getLevels ());
            out = new GL3SharedPrimitive (key, primitive.generate ());
            primitiveMap.put (key, out);
         }
      }

      lastSpindle = out;
      return out.getPrimitive();
   }

   public GL3SharedObject getCylinder(GL3 gl, int nSlices, boolean capped) {
      
      CylinderKey key = new CylinderKey(nSlices, capped);
      return getCylinder(gl, key);
   }

   public GL3SharedObject getCylinder(GL3 gl, CylinderKey key) {
      if (lastCylinder != null && lastCylinder.isValid() &&
         lastCylinder.key.equals(key)) {
         return lastCylinder.getPrimitive();
      }

      GL3SharedPrimitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3ObjectInfo primitive = factory.createCylinder (gl, key.getSlices (), key.isCapped ());
            out = new GL3SharedPrimitive (key, primitive.generate ());
            primitiveMap.put (key, out);
         }
      }

      lastCylinder = out;
      return out.getPrimitive();
   }

   public GL3SharedObject getCone(GL3 gl, int nSlices, boolean capped) {
      
      ConeKey key = new ConeKey(nSlices, capped);
      return getCone(gl, key);
   }
   
   public GL3SharedObject getCone(GL3 gl, ConeKey key) {
      if (lastCone != null && lastCone.isValid() &&
         lastCone.key.equals(key)) {
         return lastCone.getPrimitive();
      }

      GL3SharedPrimitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3ObjectInfo primitive = factory.createCone (gl, key.getSlices (), key.isCapped ());
            out = new GL3SharedPrimitive (key, primitive.generate ());
            primitiveMap.put (key, out);
         }
      }

      lastCone = out;
      return out.getPrimitive();
   }

   public GL3SharedObject getAxes(GL3 gl, boolean x, boolean y, boolean z) {
      
      AxesKey key = new AxesKey(x, y, z);
      return getAxes(gl, key);
   }
   
   public GL3SharedObject getAxes(GL3 gl, AxesKey key) {
      if (lastAxes != null && lastAxes.isValid() &&
      lastAxes.key.equals(key)) {
         return lastAxes.getPrimitive();
      }

      GL3SharedPrimitive out = null;

      synchronized(primitiveMap) {
         out = primitiveMap.get (key);

         // if doesn't exist, create
         if (out == null || out.disposeInvalid(gl)) {
            GL3ObjectInfo primitive = factory.createAxes (gl, key.hasX (), key.hasY (), key.hasZ ());
            out = new GL3SharedPrimitive (key, primitive.generate ());
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
         Iterator<Entry<Object,GL3SharedPrimitive>> it = primitiveMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<Object,GL3SharedPrimitive> entry = it.next ();
            GL3SharedObject primitive = entry.getValue ().glo;
            if (!primitive.isValid () || primitive.releaseDispose ((GL3)gl)) {
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
         for (Entry<Object,GL3SharedPrimitive> entry : primitiveMap.entrySet ()) {
            entry.getValue ().glo.releaseDispose ((GL3)gl);
         }
         primitiveMap.clear ();
      }
   }

}
