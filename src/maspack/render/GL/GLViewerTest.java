/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JToggleButton;

import maspack.render.Dragger3d;
import maspack.render.Jack3d;
import maspack.render.Rotator3d;
import maspack.render.Translator3d;

class GLViewerTest extends GLViewerFrame implements ActionListener {

   private static final long serialVersionUID = -5575271706227591496L;

   protected JButton myQuitButton;
   protected JMenuItem myJackItem;
   protected JMenuItem myTranslatorItem;
   protected JMenuItem myRotatorItem;
   protected JMenuItem myNoneItem;
   protected JToggleButton myOrthoButton;

   protected double mySize = 30;

   Dragger3d dragger;

   public void actionPerformed (ActionEvent e) {
      if (e.getSource() == myQuitButton) {
         System.exit (0);
      }
      else if (e.getSource() == myJackItem) {
         if (dragger != null) {
            viewer.removeDragger (dragger);
         }
         dragger = new Jack3d (mySize);
         viewer.addDragger (dragger);
         viewer.repaint();
      }
      else if (e.getSource() == myTranslatorItem) {
         if (dragger != null) {
            viewer.removeDragger (dragger);
         }
         dragger = new Translator3d (viewer, mySize);
         viewer.addDragger (dragger);
         viewer.repaint();
      }
      else if (e.getSource() == myRotatorItem) {
         if (dragger != null) {
            viewer.removeDragger (dragger);
         }
         dragger = new Rotator3d (viewer, mySize);
         viewer.addDragger (dragger);
         viewer.repaint();
      }
      else if (e.getSource() == myNoneItem) {
         if (dragger != null) {
            viewer.removeDragger (dragger);
         }
         dragger = null;
         viewer.repaint();
      }
      else if (e.getSource() == myOrthoButton) {
         if (myOrthoButton.isSelected()) {
            viewer.autoFitOrtho();
            myOrthoButton.setText ("ortho");
            viewer.repaint();
         }
         else {
            viewer.autoFitPerspective();
            myOrthoButton.setText ("ortho");
            viewer.repaint();
         }
      }

   }

   protected void createMenuButtons() {
      JPopupMenu.setDefaultLightWeightPopupEnabled (false);

      JMenuBar menuBar = new JMenuBar();
      myQuitButton = new JButton ("quit");
      myQuitButton.addActionListener (this);
      menuBar.add (myQuitButton);

      JMenu draggerMenu = new JMenu ("dragger");

      myNoneItem = new JMenuItem ("none");
      myNoneItem.addActionListener (this);
      draggerMenu.add (myNoneItem);

      myJackItem = new JMenuItem ("jack");
      myJackItem.addActionListener (this);
      draggerMenu.add (myJackItem);

      myTranslatorItem = new JMenuItem ("translator");
      myTranslatorItem.addActionListener (this);
      draggerMenu.add (myTranslatorItem);

      myRotatorItem = new JMenuItem ("rotator");
      myRotatorItem.addActionListener (this);
      draggerMenu.add (myRotatorItem);

      menuBar.add (draggerMenu);

      myOrthoButton = new JToggleButton ("ortho");
      myOrthoButton.setSelected (false);
      myOrthoButton.addActionListener (this);
      menuBar.add (myOrthoButton);

      setJMenuBar (menuBar);
   }

   public GLViewerTest (String name, int width, int height) {
      super (name, width, height);

      dragger = new Jack3d (mySize);

      // viewer.addRenderable (MeshFactory.createSphere (10.0, 8));
      viewer.addDragger (dragger);
      viewer.autoFitPerspective ();
      viewer.setBackgroundColor (0.3f, 0.3f, 0.3f);
      viewer.setAxisLength (1000);
      viewer.setMouseHandler(new GLMouseAdapter(viewer));
      // viewer.addViewerListener (
      // new GLViewerListener()
      // {
      // public void rerendered (GLViewerEvent e)
      // { System.out.println ("hi");
      // }
      // });

      createMenuButtons();
      pack();
   }

   public static void main (String[] args) {
      GLViewerTest test = new GLViewerTest ("GLViewerTest", 400, 400);
      test.setVisible (true);
   }
}
