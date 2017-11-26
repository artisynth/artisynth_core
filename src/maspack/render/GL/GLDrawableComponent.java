package maspack.render.GL;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.awt.GLCanvas;

import maspack.render.GL.jogl.GLJPanel;


/**
 * Wrapper for either GLCanvas or GLJPanel, since
 * the two do not have a sufficient common interface
 * to act as both an AWT Component and a GLAutoDrawable interface
 * 
 * @author Antonio
 *
 */
public abstract class GLDrawableComponent {
   
   Component component;
   GLAutoDrawable drawable;

   // private constructor, use static factory method
   private GLDrawableComponent(Component component, GLAutoDrawable drawable) {
      this.component = component;
      this.drawable = drawable;
   }
   
   /**
    * Visibility of underlying Java AWT component
    * @return true if display component is visible
    * @see java.awt.Component#isVisible() 
    */
   public boolean isVisible() {
      return component.isVisible();
   }
   
   /**
    * Returns the auto-drawable component
    * @return the underlying GLAutoDrawable
    */
   public GLAutoDrawable getDrawable() {
      return drawable;
   }
   
   /**
    * Returns the AWT Component
    * @return the AWT component
    */
   public Component getComponent() {
      return component;
   }
   
   /**
    * Repaints the AWT component
    * @see Component#repaint()
    */
   public void repaint() {
      component.repaint();
   }
   
   /**
    * Causes OpenGL rendering to be performed
    * @see GLAutoDrawable#display()
    */
   public void display() {
      drawable.display();
   }
   
   /**
    * Paints the underlying AWT component
    * @see Component#paint(Graphics)
    */
   public void paint(Graphics graphics) {
      component.paint(graphics);
   }
   
   /**
    * Creates a graphics context for the underlying component
    * @return a graphics context, or null if none exists
    * @see Component#getGraphics()
    */
   public Graphics getGraphics() {
      return component.getGraphics();
   }
   
   /**
    * Sets the preferred size for the underlying AWT component
    * @param preferredSize width/height dimension
    * @see Component#setPreferredSize(Dimension)
    */
   public void setPreferredSize(Dimension preferredSize) {
      component.setPreferredSize(preferredSize);
   }
   
   /**
    * Gets the preferred size for the underlying AWT component
    * @return preferred size (width/height)
    * @see Component#getPreferredSize()
    */
   public Dimension getPreferredSize() {
      return component.getPreferredSize();
   }
   
   /**
    * Sets the minimum size for the underlying AWT component
    * @param minimumSize width/height dimension
    * @see Component#setMinimumSize(Dimension)
    */
   public void setMinimumSize(Dimension minimumSize) {
      component.setMinimumSize(minimumSize);
   }
   
   /**
    * Gets the minimum size for the underlying AWT component
    * @return minimum size (width/height)
    * @see Component#getMinimumSize()
    */
   public Dimension getMinimumSize() {
      return component.getMinimumSize();
   }
   
   /**
    * Sets the maximum size for the underlying AWT component
    * @param maximumSize width/height dimension
    * @see Component#setMaximumSize(Dimension)
    */
   public void setMaximumSize(Dimension maximumSize) {
      component.setMaximumSize(maximumSize);
   }
   
   /**
    * Gets the maximum size for the underlying AWT component
    * @return maximum size (width/height)
    * @see Component#getMaximumSize()
    */
   public Dimension getMaximumSize() {
      return component.getMaximumSize();
   }
   
   /**
    * Resizes the underlying AWT component
    * @param width pixel width
    * @param height pixel height
    * @see Component#setSize(int, int)
    */
   public void setSize(int width, int height) {
      component.setSize(width, height);
   }
   
   /**
    * Resizes the underlying AWT component
    * @param size width/height dimension
    * @see Component#setSize(Dimension)
    */
   public void setSize(Dimension size) {
      component.setSize(size);
   }
   
   /**
    * Gets the pixel width of the underlying AWT component
    * @return pixel width
    * @see Component#getWidth
    */
   public int getWidth() {
      return component.getWidth();
   }
   
   /**
    * Gets the pixel height of the underlying AWT component
    * @return pixel height
    * @see Component#getHeight
    */
   public int getHeight() {
      return component.getHeight();
   }
   
   /**
    * Gets the size of the underlying AWT component
    * @return underlying component size
    */
   public Dimension getSize() {
      return component.getSize();
   }
   
   /**
    * Gets the real pixel width of the underlying GLAutoDrawable.
    * @return pixel width
    * @see GLAutoDrawable#getSurfaceWidth
    */
   public int getSurfaceWidth() {
      return drawable.getSurfaceWidth();
   }
   
   /**
    * Gets the real pixel height of the underlying GLAutoDrawable.
    * @return pixel height
    * @see GLAutoDrawable#getSurfaceHeight
    */
   public int getSurfaceHeight() {
      return drawable.getSurfaceHeight();
   }
   
   /**
    * Gets the x component of the origin of the underlying AWT component
    * @return origin x component
    * @see Component#getY()
    */
   public int getX() {
      return component.getX();
   }
   
   /**
    * Gets the y component of the origin of the underlying AWT component
    * @return origin y component
    * @see Component#getX()
    */
   public int getY() {
      return component.getY();
   }
   
   /**
    * Gets the top-left corner location of the component
    * @return Point object representing top-left corner
    * @see Component#getLocationOnScreen()
    */
   public Point getLocationOnScreen() {
      return component.getLocationOnScreen();
   }
   
   
   /**
    * Sets the cursor image for the underlying AWT component
    * @param cursor cursor constant
    * @see Component#setCursor(Cursor)
    */
   public void setCursor(Cursor cursor) {
      component.setCursor(cursor);
   }
   
   /**
    * Adds a listener to the end of the underlying drawable's queue
    * @param listener event listener to add
    * @see GLAutoDrawable#addGLEventListener(GLEventListener)
    */
   public void addGLEventListener(GLEventListener listener) {
      drawable.addGLEventListener(listener);
   }
   
   /**
    * Removes a listener from the underlying drawable's queue
    * @param listener event listener to remove
    * @return the removed listener, or null if not present in queue
    * @see GLAutoDrawable#removeGLEventListener(GLEventListener)
    */
   public GLEventListener removeGLEventListener(GLEventListener listener) {
      return drawable.removeGLEventListener(listener);
   }
   
   /**
    * Adds a mouse listener to the end of the underlying component's queue
    * @param l mouse listener to add
    * @see Component#addMouseListener(MouseListener)
    */
   public void addMouseListener(MouseListener l) {
      component.addMouseListener(l);
   }
   
   /**
    * Removes a mouse listener from the underlying component
    * @param l mouse listener to remove
    * @see Component#removeMouseListener(MouseListener)
    */
   public void removeMouseListener(MouseListener l) {
      component.removeMouseListener(l);
   }
   
   /**
    * Adds a mouse motion listener to the underlying component
    * @param l mouse motion listener
    * @see Component#addMouseMotionListener(MouseMotionListener)
    */
   public void addMouseMotionListener(MouseMotionListener l) {
      component.addMouseMotionListener(l);
   }
   
   /**
    * Removes a mouse motion listener from the underlying component
    * @param l mouse motion listener to remove
    * @see Component#removeMouseMotionListener(MouseMotionListener)
    */
   public void removeMouseMotionListener(MouseMotionListener l) {
      component.removeMouseMotionListener(l);
   }
   
   /**
    * Adds a mouse wheel listener to the underlying component
    * @param l mouse wheel listener to add
    * @see Component#addMouseWheelListener(MouseWheelListener)
    */
   public void addMouseWheelListener(MouseWheelListener l) {
      component.addMouseWheelListener(l);
   }
   
   /**
    * Removes a mouse wheel listener from the underlying component
    * @param l mouse wheel listener to remove
    * @see Component#removeMouseWheelListener(MouseWheelListener)
    */
   public void removeMouseWheelListener(MouseWheelListener l) {
      component.removeMouseWheelListener(l);
   }
   
   /**
    * Adds a key listener to the underlying component
    * @param l key listener to add
    * @see Component#addKeyListener(KeyListener)
    */
   public void addKeyListener(KeyListener l) {
      component.addKeyListener(l);
   }
   
   /**
    * Removes a key listener from the underlying component
    * @param l key listener to remove
    * @see Component#removeKeyListener(KeyListener)
    */
   public void removeKeyListener(KeyListener l) {
      component.removeKeyListener(l);
   }
   
   /**
    * Returns an array of all key listeners registered with the underlying component
    * @return array of key listeners
    * @see Component#getKeyListeners()
    */
   public KeyListener[] getKeyListeners() {
      return component.getKeyListeners();
   }
   
   /**
    * Enables or disables automatic buffer swapping
    * @see GLAutoDrawable#setAutoSwapBufferMode(boolean)
    */
   public void setAutoSwapBufferMode(boolean enable) {
      drawable.setAutoSwapBufferMode(enable);
   }
   
   /**
    * Indicates whether automatic buffer swapping is enabled
    * @return true if enabled
    * @see GLAutoDrawable#getAutoSwapBufferMode()
    */
   public boolean getAutoSwapBufferMode() {
      return drawable.getAutoSwapBufferMode();
   }
   
   /**
    * Destroys all resources associated with the underlying drawable
    * @see GLAutoDrawable#destroy()
    */
   public void destroy() {
      drawable.destroy();
   }
   
   
   /**
    * Wrapper for a GLCanvas
    */
   public static class GLDrawableCanvas extends GLDrawableComponent {
      GLCanvas canvas;
      public GLDrawableCanvas(GLCanvas canvas) {
         super(canvas, canvas);
         this.canvas = canvas;
      }
      
      public GLCanvas getCanvas() {
         return canvas;
      }
      
      @Override
      public void setSurfaceScale(float[] scale) {
         canvas.setSurfaceScale(scale);
      }
   }
   
   /**
    * Wrapper for a GLJPanel
    */
   public static class GLDrawableJPanel extends GLDrawableComponent {
      GLJPanel panel;
      
      public GLDrawableJPanel(final GLJPanel panel) {
         super(panel, panel);
         this.panel = panel;
         
         // XXX hack to get panel to actually get focus
         // on mouse click, found in JOGL's JRefract demo
         panel.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
              panel.requestFocus();
            }
          });
      }
      
      public GLJPanel getPanel() {
         return panel;
      }
      
      @Override
      public void setSurfaceScale(float[] scale) {
         panel.setSurfaceScale(scale);
      }
      
   }
   
   /**
    * Creates a GLAutoDrawable and Component wrapper for a GLJPanel
    * @param panel panel to wrap
    * @return wrapped component
    */
   public static GLDrawableComponent create(GLJPanel panel) {
      return new GLDrawableJPanel(panel);
   }
   
   /**
    * Creates a GLAutoDrawable and Component wrapper for a GLCanvas
    * @param canvas canvas to wrap
    * @return wrapped component
    */
   public static GLDrawableComponent create(GLCanvas canvas) {
      return new GLDrawableCanvas(canvas);
   }

   public abstract void setSurfaceScale(float[] scale);
   
}
