/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.awt.*;
import java.awt.event.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;

import artisynth.core.driver.*;
import artisynth.core.modelbase.*;
import artisynth.core.gui.selectionManager.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.GL.GLViewer;
import maspack.util.*;
import maspack.widgets.ButtonMasks;

/**
 * This class is responsible for duplicating selections of components.
 */
public class DuplicateAgent extends EditingAgent implements ActionListener {
   private LinkedList<ModelComponent> myCopyList;
   private LinkedList<MutableCompositeComponent<?>> myParentList;
   private LocationListener myLocationListener;

   private MouseRayEvent myRay;

   public DuplicateAgent (Main main, LinkedList<ModelComponent> copyList,
   LinkedList<MutableCompositeComponent<?>> parentList) {
      super (main);
      myCopyList = copyList;
      myParentList = parentList;
   }

   public void show() {
      mySelectionManager.setPopupMenuEnabled (false);
      myViewerManager.setSelectionEnabled (false);
      boolean hasTransformableGeometry = false;
      for (ModelComponent c : myCopyList) {
         if (c instanceof TransformableGeometry) {
            hasTransformableGeometry = true;
         }
      }
      if (hasTransformableGeometry) {
         installLocationListener();
      }
      else {
         duplicate (RigidTransform3d.IDENTITY);
      }
   }

   private void dispose() {
      uninstallLocationListener();
      myViewerManager.setSelectionEnabled (true);
      mySelectionManager.setPopupMenuEnabled (true);
   }

   private void duplicate (RigidTransform3d X) {
      mySelectionManager.clearSelections();
      AddComponentsCommand cmd =
         new AddComponentsCommand ("duplicate", myCopyList, myParentList);
      myUndoManager.saveStateAndExecute (cmd);
      ArrayList<TransformableGeometry> transformList = new ArrayList<>();
      for (ModelComponent c : myCopyList) {
         if (c instanceof TransformableGeometry) {
            transformList.add ((TransformableGeometry)c);
         }
         mySelectionManager.addSelected (c);
      }
      TransformGeometryContext.transform (transformList, X, 0);
      // myMain.rerender();
      dispose();
   }

   private void cancel() {
      dispose();
   }

   private static final double inf = Double.POSITIVE_INFINITY;

   public void handleRayEvent (GLViewer viewer, MouseRayEvent rayEvent) {
      RigidTransform3d X = new RigidTransform3d();

      // find the center of the components
      Point3d max = new Point3d (-inf, -inf, -inf);
      Point3d min = new Point3d (inf, inf, inf);
      for (ModelComponent c : myCopyList) {
         if (c instanceof Renderable) {
            ((Renderable)c).updateBounds (min, max);
         }
      }
      Point3d center = new Point3d();
      center.add (min, max);
      center.scale (0.5);

      // use this center to define a view plane, which we intersect
      // with the ray to find a new center
      Point3d newCenter =
         intersectViewPlane (rayEvent.getRay(), center, viewer);

      X.p.sub (newCenter, center);

      // // get the point of intersection from the mesh
      // Point3d isectPoint =
      // editorUtils.intersectWithMesh(myRigidBody.getMesh(),
      // rayEvent);

      // if (isectPoint != null)
      // {
      // FrameMarker marker = new FrameMarker();
      // marker.setLocation (isectPoint);
      // myMarkerListModel.add (marker);
      // myRigidBody.addMarker (marker);
      // // marker.setWorldLocation (isectPoint);
      // myMain.rerender();
      // }
      duplicate (X);
   }

   void installLocationListener() {
      myLocationListener = new LocationListener();
      myViewerManager.addMouseListener (myLocationListener);
      myViewerManager.setCursor (
         Cursor.getPredefinedCursor (Cursor.CROSSHAIR_CURSOR));
   }

   void uninstallLocationListener() {
      if (myLocationListener != null) {
         myViewerManager.setCursor (Cursor.getDefaultCursor());
         myViewerManager.removeMouseListener (myLocationListener);
      }
   }

   public void actionPerformed (ActionEvent e) {
      String cmd = e.getActionCommand();

      if (cmd == "cancel") {
         cancel();
      }
      else {
         throw new InternalErrorException ("unknown action: " + cmd);
      }
   }

   class LocationListener extends MouseInputAdapter {
      public void mouseClicked (MouseEvent e) {
         if (e.getButton() == MouseEvent.BUTTON1) {
            GLViewer viewer =
               ViewerManager.getViewerFromComponent (e.getComponent());
            if (viewer != null) {
               handleRayEvent (viewer, MouseRayEvent.create (e, viewer));
            }
         }
         else if (e.getModifiersEx() == ButtonMasks.getContextMenuMask()) {
            cancel();
         }
      }
   }

}
