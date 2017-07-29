package maspack.render.GL;

import java.util.LinkedList;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;

public class GLGarbageCollector implements GLEventListener {

   private static boolean GARBAGE_COLLECTOR_ENABLED = true;
   
   volatile long lastCollection;
   LinkedList<GLGarbageSource> sources;
   boolean enabled;

   public GLGarbageCollector() {
      sources = new LinkedList<> ();
      enabled = GARBAGE_COLLECTOR_ENABLED;
      lastCollection = System.currentTimeMillis ();
   }

   public void addSource(GLGarbageSource source) {
      synchronized (sources) {
         sources.add (source);  
      }
   }

   public void collect(GL gl) {
      if (enabled) {
         //System.out.println ("Garbage being collected");
         synchronized(sources) {
            for (GLGarbageSource source : sources) {
               source.garbage (gl);
            }
            lastCollection = System.currentTimeMillis ();
         }
      }
   }
   
   /**
    * Will collect garbage as long as the last collection time
    * was at least the supplied elapsed period 
    * @param timeperiod in ms
    * @return true if collected
    */
   public boolean maybeCollect(GL gl, long timeperiod) {
      
      // first check unsynchronized for fast rejection
      long diff = System.currentTimeMillis ()-lastCollection;
      
      if (diff > timeperiod) {
         synchronized(sources) {
            // check again in a synchronized way to prevent
            // double collection
            diff = System.currentTimeMillis ()-lastCollection;
            if (diff > timeperiod) {
               collect (gl);
            }
            return true;
         }
      }
      
      return false;
   }
   
   public void dispose(GL gl) {
      //System.out.println ("Garbage disposing all");
      synchronized(sources) {
         for (GLGarbageSource source : sources) {
            source.dispose (gl);
         }
      }
   }

   @Override
   public void init (GLAutoDrawable drawable) {
      collect(drawable.getGL ());
   }

   @Override
   public void dispose (GLAutoDrawable drawable) {
      dispose (drawable.getGL ());
   }

   @Override
   public void display (GLAutoDrawable drawable) {
      collect(drawable.getGL ());
   }

   @Override
   public void reshape (
      GLAutoDrawable drawable, int x, int y, int width, int height) {
      collect(drawable.getGL ());
   }

}
