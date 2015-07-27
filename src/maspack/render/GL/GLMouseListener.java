package maspack.render.GL;

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
   
}
