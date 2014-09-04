package maspack.render;

import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelListener;

public interface GLMouseListener extends MouseListener, 
   MouseMotionListener, MouseWheelListener {
   
   public int getSelectionButtonMask();

   public double getMouseWheelZoomScale();
   public void setMouseWheelZoomScale(double val);

   public int getMultipleSelectionMask();
   
}
