package artisynth.core.workspace;

import java.awt.event.MouseEvent;

import javax.swing.event.MouseInputListener;

import artisynth.core.driver.ViewerManager;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.gui.selectionManager.SelectionManager;
import artisynth.core.modelbase.ModelComponent;
import maspack.render.MouseRayEvent;
import maspack.render.GL.GLViewer;

public class AddMarkerTool implements SelectionListener, MouseInputListener {

   private ModelComponent myLastComponent = null;
   private SelectionManager mySelectionManager;
   private AddMarkerHandler myHandler;
   
   /*
    * Order of events:
    *   - mousePressed
    *   - selectionChanged
    *   - mouseReleased
    *   - mouseClicked
    */
   
   public AddMarkerTool(SelectionManager selManager) {
      mySelectionManager = selManager;
      setDefaultHandler ();
   }
   
   /**
    * Resets handler to default
    */
   public void setDefaultHandler() {
      myHandler = new AddMarkerHandler ();
   }
   
   /**
    * Set custom handler for adding markers
    * @param handler handler to be added
    */
   public void setHandler(AddMarkerHandler handler) {
      myHandler = handler;
   }
   
   @Override
   public void mouseClicked (MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
         if (myLastComponent != null) {
            if (e.getClickCount() == 2) {
               
               // determine event
               GLViewer viewer = ViewerManager.getViewerFromComponent (e.getComponent());
               MouseRayEvent mark = MouseRayEvent.create (e, viewer);
               
               // add marker
               myHandler.addMarker (myLastComponent, mark.getRay ());
               
            }
         }
         
         clear ();
      }
   }

   @Override
   public void mousePressed (MouseEvent e) {
      if (e.getButton() == MouseEvent.BUTTON1) {
         mySelectionManager.clearSelections();
      }
   }

   @Override
   public void mouseReleased (MouseEvent e) {}

   @Override
   public void mouseEntered (MouseEvent e) {}

   @Override
   public void mouseExited (MouseEvent e) {}

   @Override
   public void mouseDragged (MouseEvent e) {}

   @Override
   public void mouseMoved (MouseEvent e) {}

   private void clear() {
      myLastComponent = null;
   }
   
   @Override
   public void selectionChanged (SelectionEvent e) {
      
      ModelComponent comp = e.getLastAddedComponent();
      myLastComponent = comp;

   }
   
   

}
