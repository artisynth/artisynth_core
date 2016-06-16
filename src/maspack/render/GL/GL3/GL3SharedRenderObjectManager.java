package maspack.render.GL.GL3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import javax.media.opengl.GL;
import javax.media.opengl.GL3;

import maspack.render.RenderObject;
import maspack.render.RenderObject.RenderObjectIdentifier;
import maspack.render.GL.GLGarbageSource;

public class GL3SharedRenderObjectManager implements GLGarbageSource {

   GL3VertexAttributeMap attributeMap;
   HashMap<RenderObjectIdentifier,GL3SharedRenderObjectPrimitives> indexedMap;
   HashMap<RenderObjectIdentifier,GL3SharedRenderObjectLines> lineMap;
   HashMap<RenderObjectIdentifier,GL3SharedRenderObjectPoints> pointMap;
   
   public GL3SharedRenderObjectManager(GL3VertexAttributeMap attributeMap) {
      this.attributeMap = attributeMap;
      indexedMap = new HashMap<> ();
      lineMap = new HashMap<> ();
      pointMap = new HashMap<> ();
   }
   
   public GL3SharedRenderObjectPrimitives getPrimitives(GL3 gl, RenderObject robj) {
   
      GL3SharedRenderObjectPrimitives gro = null;
      synchronized (indexedMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = indexedMap.get (rid);
         if (gro == null || gro.disposeInvalid (gl)) {
            gro = GL3SharedRenderObjectPrimitives.generate(gl, robj, attributeMap.getPosition (),
               attributeMap.getNormal (), attributeMap.getColor (), attributeMap.getTexcoord ());
            indexedMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, robj);
         }
      }
      
      return gro;
   }
   
   public GL3SharedRenderObjectLines getLines(GL3 gl, RenderObject robj) {
      
      GL3SharedRenderObjectLines gro = null;
      synchronized (lineMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = lineMap.get (rid);
         if (gro == null || gro.disposeInvalid (gl)) {
            gro = GL3SharedRenderObjectLines.generate(gl, robj, attributeMap.getPosition (),
               attributeMap.getNormal (), attributeMap.getColor (), attributeMap.getTexcoord (),
               attributeMap.get ("line_bottom_position"),
               attributeMap.get ("line_top_position"),
               attributeMap.get ("line_bottom_color"),
               attributeMap.get ("line_top_color"));
            lineMap.put (rid, gro);
         } else {
            gro.maybeUpdate (gl, robj);
         }
      }
      
      return gro;
   }
   
   public GL3SharedRenderObjectPoints getPoints(GL3 gl, RenderObject robj) {
      
      GL3SharedRenderObjectPoints gro = null;
      synchronized (pointMap) {
         RenderObjectIdentifier rid = robj.getIdentifier ();
         gro = pointMap.get (rid);
         if (gro == null || gro.disposeInvalid (gl)) {
            gro = GL3SharedRenderObjectPoints.generate(gl, robj, attributeMap.getPosition (),
               attributeMap.getNormal (), attributeMap.getColor (), attributeMap.getTexcoord (),
               attributeMap.get ("instance_position"),
               attributeMap.get ("instance_color"));
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
         Iterator<Entry<RenderObjectIdentifier,GL3SharedRenderObjectPrimitives>> it = indexedMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3SharedRenderObjectPrimitives> entry = it.next ();
            if (!entry.getKey ().isValid ()) {
               it.remove ();
               entry.getValue ().dispose (gl3);
            }
         }
      }
      
      synchronized(lineMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3SharedRenderObjectLines>> it = lineMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3SharedRenderObjectLines> entry = it.next ();
            if (!entry.getKey ().isValid ()) {
               it.remove ();
               entry.getValue ().dispose (gl3);
            }
         }
      }
      
      synchronized(pointMap) {
         Iterator<Entry<RenderObjectIdentifier,GL3SharedRenderObjectPoints>> it = pointMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<RenderObjectIdentifier,GL3SharedRenderObjectPoints> entry = it.next ();
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
         for (GL3SharedRenderObjectPrimitives gro : indexedMap.values ()) {
            gro.dispose (gl3);
         }
         indexedMap.clear ();
      }
      
      synchronized(lineMap) {
         for (GL3SharedRenderObjectLines gro : lineMap.values ()) {
            gro.dispose (gl3);
         }
         lineMap.clear ();
      }
      
      synchronized(pointMap) {
         for (GL3SharedRenderObjectPoints gro : pointMap.values ()) {
            gro.dispose (gl3);
         }
         pointMap.clear ();
      }
   }
   
   public GL3VertexAttributeInfo getAttribute(String str) {
      return attributeMap.get (str);
   }
   
   
}
