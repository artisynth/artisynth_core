package maspack.render.GL;

import java.awt.Point;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

public interface GLMouseListener extends MouseListener, 
   MouseMotionListener, MouseWheelListener {

   public double getMouseWheelZoomScale();
   public void setMouseWheelZoomScale(double val);


   // Currently used by timeline to ensure interface consistency
   // Ideally, would not force to have a particular mask
   public int getMultipleSelectionMask();
   
   /**
    * Configures the selection handler to only select objects visible on the screen
    * (i.e. ignore depth mask)
    * @param set if true, only selects visible components.  False, select occluded
    */
   public void setSelectVisibleOnly(boolean set);
   public boolean isSelectVisibleOnly();
   
   public Point getCurrentCursor();
}
