package maspack.render.GL.GL3;

import java.util.HashMap;
import java.util.LinkedList;

import javax.media.opengl.GL3;

/**
 * Manages the lifetime of primitive GLObjects, keeping track of usage (incremented when grabbing)
 * so that unused objects can be detected and cleared. 
 */
public class GL3PrimitiveManager {

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
   
   private static class GLObjectInfo {
      GL3Object glo;
      int useCount;
      Object key;

      public GLObjectInfo(GL3Object glo, Object key) {
         this.glo = glo;
         this.useCount = 0;
         this.key = key;
      }

      public void use() {
         useCount++;
      }

      public int getUseCount() {
         return useCount;
      }
      
      public void resetUseCount() {
         useCount = 0;
      }
      
      public void dispose(GL3 gl) {
         glo.release(gl);
         glo = null;
         useCount = 0;
         key = null;
      }
   }

   private HashMap<Object,GLObjectInfo> gloMap;
   private HashMap<GL3Object, Object> keyMap;
   
   private LinkedList<GLObjectInfo> freeList;

   public GL3PrimitiveManager() {
      gloMap = new HashMap<>();
      keyMap = new HashMap<>();
      freeList = new LinkedList<>();
   }

   private GLObjectInfo getGLI(Object key) {
      GLObjectInfo gli;
      synchronized (gloMap) {
         gli = gloMap.get(key);
      }
      if (gli == null) {
         return null;
      }
      gli.use();
      return gli;
   }

   private void freeReleasedGLOs(GL3 gl) {
      synchronized (freeList) {
         for (GLObjectInfo gli : freeList) {
            gli.glo.release(gl);
         }
         freeList.clear();
      }
   }

   private void addToFreeList(GLObjectInfo gli) {
      synchronized (freeList) {
         freeList.add(gli);
      }
   }

   private void releaseGLI(GLObjectInfo gli) {
      synchronized(gloMap) {
         gloMap.remove(gli.key);
         keyMap.remove(gli.glo);
      }
      addToFreeList(gli);
   }
   
   private void releaseGLI(GL3 gl, GLObjectInfo gli) {
         synchronized (gloMap) {
            gloMap.remove(gli.key);
            keyMap.remove(gli.glo);
         }
         gli.glo.release(gl);
   }
   
   public void release(Object key) {
      GLObjectInfo gli = null;
      synchronized (gloMap) {
         gli = gloMap.get(key);
      }
      if (gli != null) {
         releaseGLI(gli);
      }
   }
   
   public void release(GL3 gl, Object key) {
      GLObjectInfo gli = null;
      synchronized (gloMap) {
         gli = gloMap.get(key);
      }
      if (gli != null) {
         releaseGLI(gl, gli);
      }
   }

   public void clear(GL3 gl) {
      freeReleasedGLOs(gl);
      synchronized (gloMap) {
         for (GLObjectInfo gli : gloMap.values()) {
            gli.glo.release(gl);
         }
         gloMap.clear();
         keyMap.clear();
      }
   }

   private void addGLI(GL3 gl, Object key, GLObjectInfo gli) {
      synchronized (gloMap) {
         GLObjectInfo old = gloMap.put(key, gli);
         if (old != null && old.glo != gli.glo) {
            old.dispose(gl);
         }
         keyMap.put(gli.glo, key);
      }
   }
   
   public void dispose(GL3 gl) {
      clear(gl);
   }
   
   public void release(GL3Object glo) {
      Object key = null;
      synchronized(gloMap) {
         key = keyMap.get(glo);
      }
      if (key != null) {
         release(key);
      }
   }
   
   public void release(GL3 gl, GL3Object glo) {
      Object key = null;
      synchronized(gloMap) {
         key = keyMap.get(glo);
      }
      if (key != null) {
         release(gl, key);
      }
   }
   
   public void add(GL3 gl, Object key, GL3Object glo) {
      GLObjectInfo gli = new GLObjectInfo(glo, key);
      addGLI(gl, key, gli);
   }
   
   public GL3Object get(Object key) {
      GLObjectInfo gli = null;
      synchronized(gloMap) {
         gli = gloMap.get(key);
      }
      if (gli != null) {
         return gli.glo;
      }
      return null;
   }
   
   public void resetUseCounts() {
      synchronized (gloMap) {
         for (GLObjectInfo gli : gloMap.values()) {
            gli.resetUseCount();
         }
      }
   }
   
   public void releaseUnused() {
      LinkedList<GLObjectInfo> toRemove = new LinkedList<>();
      synchronized(gloMap) {
         for (GLObjectInfo gli : gloMap.values()) {
            if (gli.getUseCount() <= 0) {
               toRemove.add(gli);
            }
         }
      }
      
      for (GLObjectInfo gli : toRemove) {
         releaseGLI(gli);
      }
   }
   
   public void releaseUnused(GL3 gl) {
      LinkedList<GLObjectInfo> toRemove = new LinkedList<>();
      synchronized(gloMap) {
         for (GLObjectInfo gli : gloMap.values()) {
            if (gli.getUseCount() <= 0) {
               toRemove.add(gli);
            }
         }
      }
      
      for (GLObjectInfo gli : toRemove) {
         releaseGLI(gl, gli);
      }
   }

   // ==========================================================================
   // Shape-specific Objects
   // ==========================================================================

   public GL3Object getSphere(GL3 gl, int nSlices, int nLevels) {
      freeReleasedGLOs(gl);
      
      SphereKey key = new SphereKey(nSlices, nLevels);
      GLObjectInfo gli = getGLI(key);
      if (gli == null) {
         
         // create sphere
         GL3Object glo = GL3ObjectFactory.createSphere(gl, nSlices, nLevels);
         gli = new GLObjectInfo(glo, key);
         addGLI(gl, key, gli);
      }
      return gli.glo;
   }

   public GL3Object getSpindle(GL3 gl, int nSlices, int nLevels) {
      freeReleasedGLOs(gl);
      
      SpindleKey key = new SpindleKey(nSlices, nLevels);
      GLObjectInfo gli = getGLI(key);
      if (gli == null) {
         GL3Object glo = GL3ObjectFactory.createSpindle(gl, nSlices, nLevels);
         gli = new GLObjectInfo(glo, key);
         addGLI(gl, key, gli);
      }
      return gli.glo;
   }

   public GL3Object getCylinder(GL3 gl, int nSlices, boolean capped) {
      freeReleasedGLOs(gl);
      
      CylinderKey key = new CylinderKey(nSlices, capped);
      GLObjectInfo gli = getGLI(key);
      if (gli == null) {
         GL3Object glo = GL3ObjectFactory.createCylinder(gl, nSlices, capped);
         gli = new GLObjectInfo(glo, key);
         addGLI(gl, key, gli);
      }
      return gli.glo;
   }

   public GL3Object getCone(GL3 gl, int nSlices, boolean capped) {
      freeReleasedGLOs(gl);
      
      ConeKey key = new ConeKey(nSlices, capped);
      GLObjectInfo gli = getGLI(key);
      if (gli == null) {
         GL3Object glo = GL3ObjectFactory.createCone(gl, nSlices, capped);
         gli = new GLObjectInfo(glo, key);
         addGLI(gl, key, gli);
      }
      return gli.glo;
   }

   public GL3Object getAxes(GL3 gl, boolean x, boolean y, boolean z) {
      freeReleasedGLOs(gl);
      
      AxesKey key = new AxesKey(x, y, z);
      GLObjectInfo gli = getGLI(key);
      if (gli == null) {
         GL3Object glo = GL3ObjectFactory.createAxes(gl, x, y, z);
         gli = new GLObjectInfo(glo, key);
         addGLI(gl, key, gli);
      }
      return gli.glo;
   }

}
