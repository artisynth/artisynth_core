package maspack.test.GL;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLProfile;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import com.jogamp.opengl.util.FPSAnimator;

import maspack.matrix.AxisAlignedRotation;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.IsRenderable;
import maspack.render.IsSelectable;
import maspack.render.RenderList;
import maspack.render.Renderer;
import maspack.render.ViewerSelectionEvent;
import maspack.render.ViewerSelectionListener;
import maspack.render.GL.GLMouseAdapter;
import maspack.render.GL.GLMouseListener;
import maspack.render.GL.GLViewer;
import maspack.render.GL.GL2.GL2SharedResources;
import maspack.render.GL.GL2.GL2Viewer;
import maspack.render.GL.GL3.GL3SharedResources;
import maspack.render.GL.GL3.GL3VertexAttributeInfo;
import maspack.render.GL.GL3.GL3VertexAttributeMap;
import maspack.render.GL.GL3.GL3Viewer;
import maspack.render.GL.GL3.GLSLGenerator;
import maspack.render.GL.GL3.GLSLGenerator.StringIntPair;

/**
 * Class for creating multiple viewers, with potentially shared contexts
 * between them in order to help debugging viewer-related issues
 * @author antonio
 *
 */

public class MultiViewer {
   
   public static boolean doFPS = false;
   private static final int FPS = 60; // animator's target frames per second
   
   public static interface SimpleSelectable extends IsSelectable {
      public void setSelected(boolean set);
      public boolean isSelected();
   }
   
   private static class RenderableWrapper implements SimpleSelectable {

      IsRenderable r;
      boolean selected;
      
      public RenderableWrapper(IsRenderable r) {
         this.selected = false;
         this.r = r;
      }
      
      @Override
      public boolean isSelectable() {
         return true;
      }

      @Override
      public int numSelectionQueriesNeeded() {
         return -1;
      }

      @Override
      public void getSelection(LinkedList<Object> list, int qid) {
      }

      @Override
      public void prerender(RenderList list) {
         r.prerender(list);
      }

      @Override
      public void render(Renderer renderer, int flags) {
         r.render(renderer, flags);
      }

      @Override
      public void updateBounds(Vector3d pmin, Vector3d pmax) {
         r.updateBounds(pmin, pmax);
      }

      @Override
      public int getRenderHints() {
         return r.getRenderHints();
      }

      @Override
      public void setSelected(boolean set) {
         selected = set;
      }

      @Override
      public boolean isSelected() {
         return selected;
      }
      
   }
   
   private static class SimpleSelectionHandler implements ViewerSelectionListener {

      List<SimpleSelectable> myRenderables = null;
      public SimpleSelectionHandler(List<SimpleSelectable> r) {
         myRenderables = r;
      }

      @Override
      public void itemsSelected(ViewerSelectionEvent e) {

         for (SimpleSelectable r : myRenderables) {
            r.setSelected(false);
         }

         for (List<?> ll : e.getSelectedObjects()) {
            Object obj = ll.get(0);
            if (obj instanceof SimpleSelectable) {
               ((SimpleSelectable)obj).setSelected(true);
            }
         }

      }

   }
   
   private static class FPSMonitor implements Runnable {
      FPSAnimator animator;
      String header;
      boolean stop;
      
      public FPSMonitor(String header, FPSAnimator animator) {
         
         this.animator = animator;
         this.header = header;
         stop = false;
         
      }
      
      public void stop() {
         stop = true;
      }
      
      @Override
      public void run() {
       
         while (!stop) {
            try {
               Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            Thread.yield();
            float FPS = animator.getTotalFPS();
            System.out.println(header + FPS);
         }
         
      }
   }
   
   public static class SimpleViewerApp {
      boolean closed;
      JFrame frame;
      ArrayList<SimpleSelectable> renderables;
      GLViewer viewer;
      
      public SimpleViewerApp(final String title, GLViewer v) {
         
         this.viewer = v;
         this.closed = false;
         renderables = new ArrayList<>();
         
         // Run the GUI codes in the event-dispatching thread for thread safety
         try {
            SwingUtilities.invokeAndWait(new Runnable() {
               
               @Override
               public void run() {
                  // Create the OpenGL rendering canvas

                  GLMouseListener mouse = viewer.getMouseHandler();
                  if (mouse instanceof GLMouseAdapter) {
                     ((GLMouseAdapter)mouse).setLaptopConfig();
                  }
                  
                  viewer.addSelectionListener(new SimpleSelectionHandler(renderables));

                  // Create a animator that drives canvas' display() at the specified FPS.
                  final FPSAnimator animator;
                  final FPSMonitor fpsMonitor;
                  
                  if (MultiViewer.doFPS) {
                     animator = new FPSAnimator(viewer.getCanvas().getDrawable(), FPS, true);
                     animator.setUpdateFPSFrames(3, null);
                     animator.start();
                     fpsMonitor = new FPSMonitor(title + " FPS: ", animator);
                     new Thread(fpsMonitor).start();
                  } else {
                     animator = null;
                     fpsMonitor = null;
                  }
                  
                  // Create the top-level container
                  frame = new JFrame(); // Swing's JFrame or AWT's Frame
                  frame.add(viewer.getCanvas().getComponent());

                  frame.addWindowListener(new WindowAdapter() {
                     @Override
                     public void windowClosing(WindowEvent e) {
                        // Use a dedicate thread to run the stop() to ensure that the
                        // animator stops before program exits.
                        new Thread() {
                           @Override
                           public void run() {
                              
                              if (animator != null && animator.isStarted()) {
                                 animator.stop();
                              }
                              if (fpsMonitor != null) { 
                                 fpsMonitor.stop();
                              }
                              
                              try {
                                 SwingUtilities.invokeAndWait (new Runnable() {
                                    @Override
                                    public void run () {
                                       viewer.dispose ();
                                    }
                                 });
                              }
                              catch (InvocationTargetException e) {
                              }
                              catch (InterruptedException e) {
                              }
                              
                              closed = true;
                           }
                        }.start();
                     }
                  });
                  frame.setTitle(title);
                  // frame.pack();
                  frame.setVisible(true);
                  // animator.start(); // start the animation loop
               }
            });
         } catch (InvocationTargetException | InterruptedException e) {
            e.printStackTrace();
         }
      }
      
      public void addRenderable(SimpleSelectable r) {
         renderables.add(r);
         viewer.addRenderable(r);
      }
      
      public void addRenderable(IsRenderable r) {
         RenderableWrapper rw = new RenderableWrapper(r);
         addRenderable(rw);
      }
      
      public boolean isClosed() {
         return closed;
      }
      
      public void setLocation(final int x, final int y, final int w, final int h) {
         try {
            SwingUtilities.invokeAndWait(new Runnable() {
               @Override
               public void run() {
                  frame.setLocation(x, y);
                  frame.setPreferredSize(new Dimension(w, h));
                  frame.setSize(w, h);
               }
            });
         } catch (InvocationTargetException | InterruptedException e) {
         }
      }
      
      public void setSize(final int w, final int h) {
         try {
            SwingUtilities.invokeAndWait(new Runnable() {
               @Override
               public void run() {
                  frame.setSize(w, h);
               }
            });
         } catch (InvocationTargetException | InterruptedException e) {
         }
         // System.out.println(frame.getTitle() + ": " + frame.getSize());
      }
      
   }   
   
   private ArrayList<SimpleViewerApp> windows;
   private Thread closeThread;
   
   private GL2SharedResources gl2resources;
   private GL3SharedResources gl3resources;
   
   public MultiViewer() {
      windows = new ArrayList<>();
      closeThread = null;
   }
   
   public void addRenderable(SimpleSelectable r) {
      for (SimpleViewerApp app : getWindows()) {
         app.addRenderable(r);
         app.viewer.repaint();
      }
   }
   
   public void addRenderable(IsRenderable r) {
      RenderableWrapper rw = new RenderableWrapper(r);
      addRenderable(rw);
   }
   
   public void addViewer(String title, GLViewer viewer, int x, int y, int w, int h) {
      
      SimpleViewerApp app = new SimpleViewerApp(title, viewer);
            
      windows.add(app);
      
      if (closeThread == null) {
         closeThread = new Thread(new Runnable() {
            @Override
            public void run() {
               boolean closed = false;
               while (!closed) {
                  try {
                     Thread.sleep(100);
                  } catch (InterruptedException e) {
                  }
                  Thread.yield();
                  
                  if (windows.size() > 0) {
                     closed = true;
                     for (SimpleViewerApp sva : getWindows()) {
                        if (!sva.isClosed()) {
                           closed = false;
                           break;
                        }
                     }
                  } else {
                     closed = false;
                  }
               }
               unsyncViews ();
               System.exit(0);
            }
         });
         
         closeThread.start();
      }
      
      app.setLocation(x, y, w, h);
   }

   public void addGL2Viewer(String title, int x, int y, int w, int h) {
      if (gl2resources == null) {
         GLProfile glp3 = GLProfile.get(GLProfile.GL2);
         GLCapabilities cap = new GLCapabilities(glp3);
         cap.setSampleBuffers (true);
         cap.setNumSamples (8);
         gl2resources = new GL2SharedResources (cap);
      }
      GL2Viewer viewer = new GL2Viewer(null, gl2resources, w, h);
      addViewer(title, viewer, x, y, w, h);
   }

   public void addGL3Viewer(String title, int x, int y, int w, int h) {
      if (gl3resources == null) {
         GLProfile glp3 = GLProfile.get(GLProfile.GL3);
         GLCapabilities cap = new GLCapabilities(glp3);
         cap.setSampleBuffers (true);
         cap.setNumSamples (8);
        
         // get attribute map from GLSL generator
         StringIntPair[] attributes = GLSLGenerator.ATTRIBUTES;
         GL3VertexAttributeMap attributeMap = new GL3VertexAttributeMap (
            new GL3VertexAttributeInfo (attributes[0].getString (), attributes[0].getInt ()), 
            new GL3VertexAttributeInfo (attributes[1].getString (), attributes[1].getInt ()),
            new GL3VertexAttributeInfo (attributes[2].getString (), attributes[2].getInt ()),
            new GL3VertexAttributeInfo (attributes[3].getString (), attributes[3].getInt ()));
         for (int i=4; i<attributes.length; ++i) {
            attributeMap.add (new GL3VertexAttributeInfo (attributes[i].getString (), attributes[i].getInt ()));
         }
         gl3resources = new GL3SharedResources (cap, attributeMap);
      }
      GLViewer viewer = new GL3Viewer(null, gl3resources, w, h);
      addViewer(title, viewer, x, y, w, h);
   }
   
   public class ViewSyncThread extends Thread {

      Point3d lastEye = new Point3d();
      Point3d lastCenter = new Point3d();
      volatile boolean terminate = false;;
      
      @Override
      public void run () {
         while(!terminate) {
            Point3d nextEye = null;
            Point3d nextCenter = null;
            for (SimpleViewerApp app : getWindows()) {
               GLViewer viewer = app.viewer;
               Point3d eye = viewer.getEye();
               Point3d center = viewer.getCenter();
               if (!eye.epsilonEquals (lastEye, eye.norm()*1e-4)) {
                  nextEye = eye;
               }
               if (!center.epsilonEquals (lastCenter, center.norm ()*1e-4)) {
                  nextCenter = center;
               }
            }
            
            if (nextEye != null) {
               for (SimpleViewerApp app : getWindows()) {
                  GLViewer viewer = app.viewer;
                  viewer.setEye(nextEye);
               }  
               lastEye = nextEye;
            }
            
            if (nextCenter != null) {
               for (SimpleViewerApp app : getWindows()) {
                  GLViewer viewer = app.viewer;
                  viewer.setCenter(nextCenter);
               }  
               lastCenter = nextCenter;
            }
            
            if (nextCenter != null || nextEye != null) {
               for (SimpleViewerApp app : getWindows()) {
                  GLViewer viewer = app.viewer;
                  viewer.rerender ();
               }  
            }
            
            try {
               Thread.sleep (50);
            }
            catch (InterruptedException e) {
            }
            Thread.yield ();
         }
      }
      
      public void terminate() {
         this.terminate = true;
      }
      
   }
   
   ViewSyncThread viewSync = null;
   
   public void syncViews() {
      if (viewSync == null) {
         viewSync = new ViewSyncThread ();
         viewSync.start ();
      }
   }
   
   public void unsyncViews() {
      if (viewSync != null) {
         viewSync.terminate();
         try {
            viewSync.join();
         } catch(Exception e){}
         viewSync = null;
      }
   }

   public ArrayList<SimpleViewerApp> getWindows() {
      return windows;
   }
   
   public void setWindowSizes(int width, int height) {
      for (SimpleViewerApp app : windows) {
         app.setSize(width,height);
      }
   }
   
   public void setAxialView(AxisAlignedRotation view) {
      for (SimpleViewerApp app : windows) {
         app.viewer.setAxialView (view);
      }
   }
   
   public void autoFitViewers() {
      for (SimpleViewerApp app : windows) {
         app.viewer.autoFit();
      }
   }

   public void rerender() {
      for (SimpleViewerApp app : windows) {
         app.viewer.rerender();
      }
      
   }

}