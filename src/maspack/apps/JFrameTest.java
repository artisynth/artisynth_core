package maspack.apps;

import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.event.*;

import maspack.render.GL.*;

public class JFrameTest {

   private static class MyListener implements ComponentListener {
      public void componentHidden (ComponentEvent e) {
      }

      public void componentMoved (ComponentEvent e) {
         System.out.println ("moved " + e);
      }

      public void componentResized (ComponentEvent e) {
         System.out.println ("resized " + e);
      }

      public void componentShown (ComponentEvent e) {
      }
   }

   public static void main (String[] args) {
      GLViewerFrame frame = new GLViewerFrame("FrameDemo", 400, 200);
      JMenuBar menu = new JMenuBar();
      menu.add (new JMenu ("File"));
      menu.add (new JMenu ("View"));
      frame.setJMenuBar (menu);
      frame.pack();
      frame.setVisible (true);

      frame.getViewer().addMouseInputListener (
         new MouseInputAdapter () {
            public void mousePressed (MouseEvent e) {
               System.out.printf (
                  "mods=%x exmod=%x\n",
                  e.getModifiers(), 
                  e.getModifiersEx());
            }
         });

      frame.addComponentListener (new MyListener());


   }
}
