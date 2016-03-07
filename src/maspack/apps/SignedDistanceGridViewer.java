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

import javax.swing.event.MouseInputAdapter;

import maspack.geometry.SignedDistanceGridCell;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector4d;
import maspack.render.GL.GLSelectionEvent;
import maspack.render.GL.GLSelectionListener;
import maspack.render.GL.GLViewerFrame;

public class SignedDistanceGridViewer extends GLViewerFrame {
   private static final long serialVersionUID = 1L;
   ArrayList<SignedDistanceGridCell> gridCellList = new ArrayList<SignedDistanceGridCell> (10);
   ArrayList<int[]> selectedPnts = new ArrayList<int[]>();

   class SelectionHandler implements GLSelectionListener {
      private void clearSelection() {
         selectedPnts.clear();
      }

      public void itemsSelected (GLSelectionEvent e) {
         boolean holdSelection, selectAll;
         long modEx = e.getModifiersEx();
         holdSelection = ((modEx & MouseEvent.SHIFT_DOWN_MASK) != 0);
         selectAll = ((modEx & MouseEvent.ALT_DOWN_MASK) != 0);
         if (!holdSelection) {
            clearSelection();
         }
         if (e.numSelectedQueries() > 0) {
            LinkedList[] itemPaths = e.getSelectedObjects();
            for (int i = 0; i < itemPaths.length; i++) {
               LinkedList path = itemPaths[i];
               if (path.getFirst() instanceof SignedDistanceGridCell) {
                  SignedDistanceGridCell gridCell = (SignedDistanceGridCell)path.getFirst();
                  if (path.size() > 1 && path.get (1) instanceof Integer) {
                     int idx = ((Integer)path.get (1)).intValue();
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
            Vector4d del4d = new Vector4d (del.x, del.y, del.z, 0);
            for (Iterator it = selectedPnts.iterator(); it.hasNext();) {
               ((Vector4d)it.next()).add (del4d);
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
