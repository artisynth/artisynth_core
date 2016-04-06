package maspack.render.GL.GL3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import maspack.render.RenderObject;
import maspack.render.GL.GLGarbageSource;
import maspack.render.RenderObject.RenderObjectIdentifier;

public class GL3RenderObjectManager implements GLGarbageSource {

   GL3SharedRenderObjectManager shared;
   
   HashMap<RenderObjectIdentifier,GL3RenderObjectIndexed> indexedMap;
   HashMap<RenderObjectIdentifier,GL3RenderObjectLines> lineMap;
   HashMap<RenderObjectIdentifier,GL3RenderObjectPoints> pointMap;
   
   public GL3RenderObjectManager(GL3SharedRenderObjectManager sharedManager) {
      this.shared = sharedManager;
      indexedMap = new HashMap<> ();
      lineMap = new HashMap<> ();
      pointMap = new HashMap<> ();
   }
   
   public GL3RenderObjectIndexed getIndexed(GL3 gl, RenderObject robj) {
   
      GL3RenderObjectIndexed gro = null;
      synchronized (indexedMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = indexedMap.get (rid);
         if (gro == null || gro.disposeInvalid (gl)) {
            gro = GL3RenderObjectIndexed.generate (gl, shared.getIndexed (gl, robj));
            indexedMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, robj);
         }
      }
      
      return gro;
   }
   
   public GL3RenderObjectLines getLines(GL3 gl, RenderObject robj) {

      GL3RenderObjectLines gro = null;
      synchronized (lineMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = lineMap.get (rid);
         if (gro == null || gro.disposeInvalid (gl)) {
            GL3LinesVertexBuffer lineBuff = GL3LinesVertexBuffer.generate(gl, shared.getAttribute ("line_radius"),
               shared.getAttribute ("line_bottom_scale_offset"),
               shared.getAttribute ("line_top_scale_offset"));
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
               shared.getAttribute ("instance_scale"));
            gro = GL3RenderObjectPoints.generate(gl, pointBuff, shared.getPoints (gl, robj));;
            pointMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, robj);
         }
      }
      
      return gro;
   }

   @Override
   public void garbage (GL gl) {
      GL3 gl3 = (GL3)gl;
      
      // dispose dead RenderObjects
      synchronized(indexedMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3RenderObjectIndexed>> it = indexedMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3RenderObjectIndexed> entry = it.next ();
            if (!entry.getKey ().isValid ()) {
               it.remove ();
               entry.getValue ().dispose (gl3);
            }
         }
      }
      
      synchronized(lineMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3RenderObjectLines>> it = lineMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3RenderObjectLines> entry = it.next ();
            if (!entry.getKey ().isValid ()) {
               it.remove ();
               entry.getValue ().dispose (gl3);
            }
         }
      }
      
      synchronized(pointMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3RenderObjectPoints>> it = pointMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3RenderObjectPoints> entry = it.next ();
            if (!entry.getKey ().isValid ()) {
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
      synchronized(indexedMap) {
         for (GL3RenderObjectIndexed gro : indexedMap.values ()) {
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
   }
   
}
