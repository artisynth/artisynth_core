package maspack.render.GL.GL3;

import java.util.HashMap;
import java.util.LinkedList;

import javax.media.opengl.GL3;

public class GL3ResourceManager {

   private static class GLResourceInfo {
      GL3Resource glres;
      int useCount;
      Object key;

      public GLResourceInfo(GL3Resource glres, Object key) {
         this.glres = glres;
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
         glres.dispose(gl);
         glres = null;
         useCount = 0;
         key = null;
      }
   }

   private HashMap<Object,GLResourceInfo> glresMap;
   private HashMap<GL3Resource, Object> keyMap;

   private LinkedList<GLResourceInfo> freeList;

   public GL3ResourceManager() {
      glresMap = new HashMap<>();
      keyMap = new HashMap<>();
      freeList = new LinkedList<>();
   }

   protected GLResourceInfo getGLI(Object key) {
      GLResourceInfo gli;
      synchronized (glresMap) {
         gli = glresMap.get(key);
      }
      if (gli == null) {
         return null;
      }
      gli.use();
      return gli;
   }

   private void freeReleased(GL3 gl) {
      synchronized (freeList) {
         for (GLResourceInfo gli : freeList) {
            gli.glres.dispose(gl);
         }
         freeList.clear();
      }
   }

   private void addToFreeList(GLResourceInfo gli) {
      synchronized (freeList) {
         freeList.add(gli);
      }
   }

   private void releaseGLI(GLResourceInfo gli) {
      synchronized(glresMap) {
         glresMap.remove(gli.key);
         keyMap.remove(gli.glres);
      }
      addToFreeList(gli);
   }

   private void addGLI(GL3 gl, Object key, GLResourceInfo gli) {
      synchronized (glresMap) {
         GLResourceInfo old = glresMap.put(key, gli);
         if (old != null && old.glres != gli.glres) {
            old.dispose(gl);
         }
         keyMap.put(gli.glres, key);
      }
   }

   private void releaseGLI(GL3 gl, GLResourceInfo gli) {
      synchronized (glresMap) {
         glresMap.remove(gli.key);
         keyMap.remove(gli.glres);
      }
      gli.glres.dispose(gl);
   }

   public void release(Object key) {
      GLResourceInfo gli = null;
      synchronized (glresMap) {
         gli = glresMap.get(key);
      }
      if (gli != null) {
         releaseGLI(gli);
      }
   }

   public void release(GL3 gl, Object key) {
      GLResourceInfo gli = null;
      synchronized (glresMap) {
         gli = glresMap.get(key);
      }
      if (gli != null) {
         releaseGLI(gl, gli);
      }
   }

   public void add(GL3 gl, Object key, GL3Resource glo) {
      GLResourceInfo gli = new GLResourceInfo(glo, key);
      addGLI(gl, key, gli);
   }

   public GL3Resource get(Object key) {
      GLResourceInfo gli = null;
      synchronized(glresMap) {
         gli = glresMap.get(key);
      }
      if (gli != null) {
         return gli.glres;
      }
      return null;
   }

   public void clear(GL3 gl) {
      freeReleased(gl);
      synchronized (glresMap) {
         for (GLResourceInfo gli : glresMap.values()) {
            gli.glres.dispose(gl);
         }
         glresMap.clear();
         keyMap.clear();
      }
   }

   public void dispose(GL3 gl) {
      clear(gl);
   }

   public void release(GL3Resource glres) {
      Object key = null;
      synchronized(glresMap) {
         key = keyMap.get(glres);
      }
      if (key != null) {
         release(key);
      }
   }

   public void release(GL3 gl, GL3Resource glres) {
      Object key = null;
      synchronized(glresMap) {
         key = keyMap.get(glres);
      }
      if (key != null) {
         release(gl, key);
      }
   }

   public void resetUseCounts() {
      synchronized (glresMap) {
         for (GLResourceInfo gli : glresMap.values()) {
            gli.resetUseCount();
         }
      }
   }

   public void releaseUnunsed() {
      LinkedList<GLResourceInfo> toRemove = new LinkedList<>();
      synchronized(glresMap) {
         for (GLResourceInfo gli : glresMap.values()) {
            if (gli.getUseCount() <= 0) {
               toRemove.add(gli);
            }
         }
      }

      for (GLResourceInfo gli : toRemove) {
         releaseGLI(gli);
      }
   }

   public void releaseUnused(GL3 gl) {
      LinkedList<GLResourceInfo> toRemove = new LinkedList<>();
      synchronized(glresMap) {
         for (GLResourceInfo gli : glresMap.values()) {
            if (gli.getUseCount() <= 0) {
               toRemove.add(gli);
            }
         }
      }

      for (GLResourceInfo gli : toRemove) {
         releaseGLI(gl, gli);
      }
   }

}
