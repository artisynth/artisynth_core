package maspack.render.GL;

import java.util.LinkedList;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLEventListener;

public class GLGarbageCollector implements GLEventListener {

   private static boolean GARBAGE_COLLECTOR_ENABLED = true;
   
   LinkedList<GLGarbageSource> sources;
   boolean enabled;

   public GLGarbageCollector() {
      sources = new LinkedList<> ();
      enabled = GARBAGE_COLLECTOR_ENABLED;
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
         }
      }
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
