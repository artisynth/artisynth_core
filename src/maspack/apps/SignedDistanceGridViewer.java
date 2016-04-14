/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.apps;

import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.swing.event.MouseInputAdapter;

import maspack.geometry.SignedDistanceGridCell;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.ViewerSelectionEvent;
import maspack.render.ViewerSelectionListener;
import maspack.render.GL.GLViewerFrame;

public class SignedDistanceGridViewer extends GLViewerFrame {
   private static final long serialVersionUID = 1L;
   ArrayList<SignedDistanceGridCell> gridCellList = new ArrayList<SignedDistanceGridCell> (10);
   ArrayList<int[]> selectedPnts = new ArrayList<int[]>();

   class SelectionHandler implements ViewerSelectionListener {
      private void clearSelection() {
         selectedPnts.clear();
      }

      public void itemsSelected (ViewerSelectionEvent e) {
         boolean holdSelection, selectAll;
         long modEx = e.getModifiersEx();
         holdSelection = ((modEx & MouseEvent.SHIFT_DOWN_MASK) != 0);
         selectAll = ((modEx & MouseEvent.ALT_DOWN_MASK) != 0);
         if (!holdSelection) {
            clearSelection();
         }
         if (e.numSelectedQueries() > 0) {
            List<LinkedList<?>> itemPaths = e.getSelectedObjects();
            for (LinkedList<?> path : itemPaths) {
               if (path.getFirst() instanceof SignedDistanceGridCell) {
                  SignedDistanceGridCell gridCell = (SignedDistanceGridCell)path.getFirst();
                  if (path.size() > 1 && path.get (1) instanceof Integer) {
                     // int idx = ((Integer)path.get (1)).intValue();
                     if (!gridCell.isSelected ()) {
                        gridCell.selectPoint (true);
                        selectedPnts.add (gridCell.getPoint());
                     }
                     else {
                        gridCell.selectPoint (false);
                        selectedPnts.remove (gridCell.getPoint());
                     }
                     if (!selectAll) {
                        break;
                     }
                  }
               }
            }
         }
      }
   }

   class MouseHandler extends MouseInputAdapter {
      boolean dragging = false;
      int lastX;
      int lastY;
      public void mousePressed (MouseEvent e) {
         // check for selection
         int modEx = e.getModifiersEx();
         if ((modEx & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
            if (selectedPnts.size() > 0) {
               dragging = true;
               lastX = e.getX();
               lastY = e.getY();
            }
         }
      }

      public void mouseReleased (MouseEvent e) {
         dragging = false;
      }
      // Might want to modify this. In nurbsViewer, you could click and drag
      // points around. I just want a selection area.
      public void mouseDragged (MouseEvent e) {
         if (dragging) {
            RigidTransform3d XV = new RigidTransform3d();
            viewer.getViewMatrix (XV);
            Vector3d del =
               new Vector3d (e.getX() - lastX, lastY - e.getY(), 0);
            del.inverseTransform (XV);
            del.scale (viewer.centerDistancePerPixel());
            for (Iterator<int[]> it = selectedPnts.iterator(); it.hasNext();) {
               int[] next = it.next ();
               next[0] += del.x;
               next[1] += del.y;
               next[2] += del.z;
            }
            lastX = e.getX();
            lastY = e.getY();
            viewer.getCanvas().repaint();
         }
      }
   }

   public SignedDistanceGridViewer (int w, int h) {
      super ("GridViewer", w, h);
      init();
   }

   private void init() {
      MouseHandler mouseHandler = new MouseHandler();
      viewer.getCanvas().addMouseListener (mouseHandler);
      viewer.getCanvas().addMouseMotionListener (mouseHandler);
      viewer.addSelectionListener (new SelectionHandler());
   }

   public void addCell (SignedDistanceGridCell gridCell) {
      viewer.addRenderable (gridCell);
      gridCellList.add (gridCell);
   }
}
