package maspack.render.GL;

import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.SwingUtilities;

import maspack.matrix.Point3d;
import maspack.render.RenderList;
import maspack.render.Renderer;
import maspack.render.GL.GL2.GL2Viewer;
import maspack.render.GL.GL3.GL3Viewer;

import com.jogamp.opengl.util.FPSAnimator;

public class MultiViewerTest {
   
   private static final int FPS = 60; // animator's target frames per second
   
   public static interface SimpleSelectable extends GLSelectable {
      public void setSelected(boolean set);
      public boolean isSelected();
   }
   
   private static class RenderableWrapper implements SimpleSelectable {

      GLRenderable r;
      boolean selected;
      
      public RenderableWrapper(GLRenderable r) {
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
      public void updateBounds(Point3d pmin, Point3d pmax) {
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
   
   private static class SimpleSelectionHandler implements GLSelectionListener {

      List<SimpleSelectable> myRenderables = null;
      public SimpleSelectionHandler(List<SimpleSelectable> r) {
         myRenderables = r;
      }

      @Override
      public void itemsSelected(GLSelectionEvent e) {

         for (SimpleSelectable r : myRenderables) {
            r.setSelected(false);
         }

         for (LinkedList<Object> ll : e.getSelectedObjects()) {
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
                  final FPSAnimator animator = new FPSAnimator(viewer.getCanvas(), FPS, true);
                  animator.setUpdateFPSFrames(3, null);
                  animator.start();
                  final FPSMonitor fpsMonitor = new FPSMonitor(title + " FPS: ", animator);
                  new Thread(fpsMonitor).start();

                  // Create the top-level container
                  frame = new JFrame(); // Swing's JFrame or AWT's Frame
                  frame.getContentPane().add(viewer.getCanvas());

                  frame.addWindowListener(new WindowAdapter() {
                     @Override
                     public void windowClosing(WindowEvent e) {
                        // Use a dedicate thread to run the stop() to ensure that the
                        // animator stops before program exits.
                        new Thread() {
                           @Override
                           public void run() {
                              if (animator.isStarted()) {
                                 animator.stop();
                              }
                              fpsMonitor.stop();
                              //                              
                              // remove JOGL content to prevent crash on exit
                              try {
                                 SwingUtilities.invokeAndWait(
                                    new Runnable() {
                                       @Override
                                       public void run() {
                                          frame.getContentPane().remove(viewer.getCanvas());                                 
                                       }
                                    });
                              } catch (InvocationTargetException | InterruptedException e) {
                                 e.printStackTrace();
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
      
      public void addRenderable(GLRenderable r) {
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
         System.out.println(frame.getTitle() + ": " + frame.getSize());
      }
      
   }   
   
   private ArrayList<SimpleViewerApp> windows;
   private Thread closeThread;
   
   public MultiViewerTest() {
      windows = new ArrayList<>();
      closeThread = null;
   }
   
   public void addRenderable(SimpleSelectable r) {
      for (SimpleViewerApp app : getWindows()) {
         app.addRenderable(r);
         app.viewer.repaint();
      }
   }
   
   public void addRenderable(GLRenderable r) {
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
               System.exit(0);
            }
         });
         
         closeThread.start();
      }
      
      app.setLocation(x, y, w, h);
   }

   
   
   public void addGL2Viewer(String title, int x, int y, int w, int h) {
      GL2Viewer viewer = new GL2Viewer(w, h);
      addViewer(title, viewer, x, y, w, h);
   }

   public void addGL3Viewer(String title, int x, int y, int w, int h) {
      GL3Viewer viewer = new GL3Viewer(w, h);
      addViewer(title, viewer, x, y, w, h);
   }
   
   public void syncMouseListeners() {
      ArrayList<GLMouseListener> ml = new ArrayList<GLMouseListener>();
      for (SimpleViewerApp app : getWindows()) {
         ml.add(app.viewer.getMouseHandler());
      }
      for (SimpleViewerApp app : getWindows()) {
         for (GLMouseListener glml : ml) {
            if (glml != app.viewer.getMouseHandler()) {
               app.viewer.getCanvas().addMouseListener(glml);
               app.viewer.getCanvas().addMouseMotionListener(glml);
               app.viewer.getCanvas().addMouseWheelListener(glml);
            }
         }
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
   
   public void autoFitViewers() {
      for (SimpleViewerApp app : windows) {
         app.viewer.autoFit();
      }
   }

}