package maspack.render.GL.GL3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL3;

import maspack.render.VertexIndexArray;
import maspack.render.GL.GLGarbageSource;
import maspack.util.DisposeObserver;

public class GL3SharedVertexIndexArrayManager implements GLGarbageSource {
  
   HashMap<DisposeObserver, GL3SharedVertexIndexArray> elementMap;
   
   public GL3SharedVertexIndexArrayManager () {
      elementMap = new HashMap<> ();
   }
   
   public GL3SharedVertexIndexArray getElementArray(GL3 gl, VertexIndexArray idxs) {
   
      GL3SharedVertexIndexArray out = null;
      synchronized (elementMap) {
         DisposeObserver obs = idxs.getDisposeObserver ();
         out = elementMap.get (obs);
         
         if (out == null || out.disposeInvalid (gl)) {
            out = GL3SharedVertexIndexArray.generate (gl);
            out.maybeUpdate (gl, idxs);
            elementMap.put (obs, out);
         } else {
            out.maybeUpdate (gl, idxs);
         }
      }
      
      return out;
   }

   @Override
   public void garbage (GL gl) {
      GL3 gl3 = (GL3)gl;
      
      // dispose dead RenderObjects
      synchronized(elementMap) {
         Iterator<Entry<DisposeObserver,GL3SharedVertexIndexArray>> it = elementMap.entrySet ().iterator ();
         while (it.hasNext ()) {
            Entry<DisposeObserver,GL3SharedVertexIndexArray> entry = it.next ();
            if (entry.getKey ().isDisposed ()) {
               it.remove ();
               entry.getValue ().dispose (gl3);
            }
         }
      }
   }

   @Override
   public void dispose (GL gl) {
      synchronized (elementMap) {
         for (GL3SharedVertexIndexArray via : elementMap.values ()) {
            via.dispose (gl);
         }
         elementMap.clear ();
      }
   }

}
