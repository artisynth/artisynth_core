package maspack.render.GL.GL3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.RenderInstances;
import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectIdentifier;
import maspack.render.VertexIndexArray;
import maspack.render.GL.GLGarbageSource;
import maspack.util.DisposeObserver;

public class GL3RenderObjectManager implements GLGarbageSource {

   GL3SharedResources shared;
   
   private static class ROIKey {
      RenderObjectIdentifier rid;
      DisposeObserver via;
      
      ROIKey(RenderObjectIdentifier rid, DisposeObserver via) {
         this.rid = rid;
         this.via = via;
      }
      
      public boolean isValid() {
         if (rid.isDisposed ()) {
            return false;
         }
         if (via.isDisposed ()) {
            return false;
         }
         return true;
      }

      @Override
      public int hashCode () {
         int result = 31*rid.hashCode () + via.hashCode ();
         return result;
      }

      @Override
      public boolean equals (Object obj) {
         
         if (this == obj) {
            return true;
         }
         if (obj == null || getClass () != obj.getClass ()) { 
            return false;
         }
         
         ROIKey other = (ROIKey)obj;
         if (!rid.equals (other.rid)) {
            return false;
         }
         if (!via.equals (other.via)) {
            return false;
         }
         return true;
      }
   }
   
   HashMap<ROIKey, GL3RenderObjectElements> elementsMap;
   HashMap<RenderObjectIdentifier,GL3RenderObjectPrimitives> indexedMap;
   HashMap<RenderObjectIdentifier,GL3RenderObjectLines> lineMap;
   HashMap<RenderObjectIdentifier,GL3RenderObjectPoints> pointMap;
   HashMap<DisposeObserver, GL3RenderInstances> instanceMap;
   
   public GL3RenderObjectManager(GL3SharedResources sharedManager) {
      this.shared = sharedManager;
      elementsMap = new HashMap<> ();
      indexedMap = new HashMap<> ();
      lineMap = new HashMap<> ();
      pointMap = new HashMap<> ();
      instanceMap = new HashMap<>();
   }
   
   public GL3RenderObjectElements getElements(GL3 gl, RenderObject robj, VertexIndexArray idxs) {
      
      GL3RenderObjectElements gro = null;
      ROIKey key = new ROIKey (robj.getIdentifier (), idxs.getDisposeObserver ());
      synchronized (elementsMap) {
         gro = elementsMap.get (key);
         if (gro == null || gro.disposeInvalid (gl)) {
            gro = GL3RenderObjectElements.generate (gl, shared.getPrimitives (gl, robj), 
               shared.getVertexIndexArray (gl, idxs));
            elementsMap.put (key, gro);
         } else {
            gro.maybeUpdate (gl, robj, idxs);
         }
      }
      
      return gro;
   }
   
   public GL3RenderObjectPrimitives getPrimitives(GL3 gl, RenderObject robj) {
   
      GL3RenderObjectPrimitives gro = null;
      synchronized (indexedMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = indexedMap.get (rid);
         if (gro == null || gro.disposeInvalid (gl)) {
            gro = GL3RenderObjectPrimitives.generate (gl, shared.getPrimitives (gl, robj));
            indexedMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, robj);
         }
      }
      
      return gro;
   }
   
   public GL3SharedRenderObjectPrimitives getSharedPrimitives(GL3 gl, RenderObject robj) {
      GL3SharedRenderObjectPrimitives gro = shared.getPrimitives (gl, robj);
      return gro;
   }
   
   public GL3RenderObjectLines getLines(GL3 gl, RenderObject robj) {

      GL3RenderObjectLines gro = null;
      synchronized (lineMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = lineMap.get (rid);
         if (gro == null || gro.disposeInvalid (gl)) {
            GL3LinesVertexBuffer lineBuff = GL3LinesVertexBuffer.generate(gl, shared.getVertexAttribute ("line_radius"),
               shared.getVertexAttribute ("line_bottom_scale_offset"),
               shared.getVertexAttribute ("line_top_scale_offset"));
            gro = GL3RenderObjectLines.generate(gl, lineBuff, shared.getLines (gl, robj));
            lineMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, robj);
         }
      }
      
      return gro;
   }
   
   public GL3RenderObjectPoints getPoints(GL3 gl, RenderObject robj) {
   
      GL3RenderObjectPoints gro = null;
      synchronized (pointMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = pointMap.get (rid);
         if (gro == null || gro.disposeInvalid (gl)) {
            GL3PointsVertexBuffer pointBuff = GL3PointsVertexBuffer.generate(gl, 
               shared.getVertexAttribute ("instance_scale"));
            gro = GL3RenderObjectPoints.generate(gl, pointBuff, shared.getPoints (gl, robj));;
            pointMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, robj);
         }
      }
      
      return gro;
   }

   public GL3RenderInstances getInstances(GL3 gl, RenderInstances rinst) {
               
      GL3RenderInstances grinst = null;
      synchronized (instanceMap) {
         DisposeObserver key = rinst.getDisposeObserver();
         grinst = instanceMap.get (key);
         if (grinst == null || grinst.disposeInvalid (gl)) {
            grinst = GL3RenderInstances.generate(gl, shared.getInstances(gl, rinst));
            instanceMap.put (key, grinst);
            
         } else {
            grinst.maybeUpdate (gl, rinst);
         }
      }
      
      return grinst;
   }
   
   @Override
   public void garbage (GL gl) {
      GL3 gl3 = (GL3)gl;
      
      // dispose dead RenderObjects
      synchronized(elementsMap) {
         Iterator<Entry<ROIKey,GL3RenderObjectElements>> it = elementsMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<ROIKey,GL3RenderObjectElements> entry = it.next ();
            if (!entry.getKey ().isValid ()) {
               it.remove ();
               entry.getValue ().dispose (gl3);
            }
         }
      }
      
      // dispose dead RenderObjects
      synchronized(indexedMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3RenderObjectPrimitives>> it = indexedMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3RenderObjectPrimitives> entry = it.next ();
            if (entry.getKey ().isDisposed ()) {
               it.remove ();
               entry.getValue ().dispose (gl3);
            }
         }
      }
      
      synchronized(lineMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3RenderObjectLines>> it = lineMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3RenderObjectLines> entry = it.next ();
            if (entry.getKey ().isDisposed ()) {
               it.remove ();
               entry.getValue ().dispose (gl3);
            }
         }
      }
      
      synchronized(pointMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3RenderObjectPoints>> it = pointMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3RenderObjectPoints> entry = it.next ();
            if (entry.getKey ().isDisposed ()) {
               it.remove ();
               entry.getValue ().dispose (gl3);
            }
         }
      }
      
      synchronized (instanceMap) {
         Iterator<Entry<DisposeObserver,GL3RenderInstances>> it = instanceMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<DisposeObserver,GL3RenderInstances> entry = it.next ();
            if (entry.getKey ().isDisposed()) {
               it.remove ();
               entry.getValue ().dispose (gl3);
            }
         }
      }
   }
   
   public void dispose(GL gl) {
      // dispose of all resources
      
      GL3 gl3 = (GL3)gl;
      
      // dispose dead RenderObjects
      synchronized(elementsMap) {
         for (GL3RenderObjectElements gro : elementsMap.values ()) {
            gro.dispose (gl3);
         }
         elementsMap.clear ();
      }
      
      synchronized(indexedMap) {
         for (GL3RenderObjectPrimitives gro : indexedMap.values ()) {
            gro.dispose (gl3);
         }
         indexedMap.clear ();
      }
      
      synchronized(lineMap) {
         for (GL3RenderObjectLines gro : lineMap.values ()) {
            gro.dispose (gl3);
         }
         lineMap.clear ();
      }
      
      synchronized(pointMap) {
         for (GL3RenderObjectPoints gro : pointMap.values ()) {
            gro.dispose (gl3);
         }
         pointMap.clear ();
      }
      
      synchronized(instanceMap) {
         for (GL3RenderInstances grinst : instanceMap.values ()) {
            grinst.dispose (gl3);
         }
         instanceMap.clear ();
      }
   }
   
}
