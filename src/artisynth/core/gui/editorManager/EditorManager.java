/**
 * Copyright (c) 2014, by the Authors: John Lloyd (UBC), Tracy Wilkinson (UBC) and
 * ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */package artisynth.core.gui.editorManager;

import java.util.*;

import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.mechmodels.*;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.gui.selectionManager.SelectionManager;

/**
 * This class is responsible for managing the editing of components.
 * 
 * The EditorManger knows which components can be added to others, so it can be
 * queried to see if one component can be added to another. Given two types of
 * components, one to be added to another, the EditorManager will return the
 * appropriate Editor for those components, if they are allowed to be added to
 * each other. The EditorManager gets the information about which components can
 * be added to others by querying each of the Editor classes that extend
 * BaseEditor.
 * 
 * The EditorManager stores a HashMap with components as keys and array lists of
 * components as values. The keys are components that can be edited and the
 * values are the list of components that can be added to them.
 * 
 * NOTE: THIS CLASS IS STILL IN PROGRESS
 * 
 */
public class EditorManager {
   // a map where the key is a ModelComponent class and the value is it's
   // corresponding editor
   private static LinkedList<EditorBase> myEditors;
   private Main myMain;
   // lock to enforce exclusive editing operations
   private boolean myEditLockedP;

   public EditorManager (Main main) {
      myMain = main;
      myEditors = new LinkedList<EditorBase>();

      // create the list of editors;
      myEditors.add (new RootModelEditor (myMain, this));
      myEditors.add (new MechModelEditor (myMain, this));
      myEditors.add (new RigidBodyEditor (myMain, this));
      myEditors.add (new MeshBodyEditor (myMain, this));
      myEditors.add (new FemModel3dEditor (myMain, this));
      myEditors.add (new FemMeshCompEditor (myMain, this));
      myEditors.add (new FemMuscleModelEditor (myMain, this));
      myEditors.add (new FrameMarkerEditor (myMain, this));
      myEditors.add (new MuscleEditor (myMain, this));
      myEditors.add (new MuscleBundleEditor (myMain, this));
      myEditors.add (new MuscleExciterEditor (myMain, this));
      myEditors.add (new TrackingControllerEditor (myMain, this));
      myEditors.add (new IsRenderableEditor (myMain, this));
      myEditors.add (new ProbeEditor (myMain, this));
      // myEditors.add(Muscle.class, new MuscleEditor(myMain));
   }
   
   public void addEditor(EditorBase editor) {
      if (!myEditors.contains(editor)) {
         editor.init(myMain, this);
         myEditors.add(editor);   
      }
   }

   public EditActionMap getActionMap (SelectionManager selManager) {

      EditActionMap actions = new EditActionMap();

      for (EditorBase editor : myEditors) {
         editor.addActions (actions, selManager);
      }
      return actions;
   }

   public synchronized boolean acquireEditLock () {
      if (!myEditLockedP) {
         myEditLockedP = true;
         return true;
      }
      else {
         return false;
      }
   }

   public synchronized void releaseEditLock () {
      myEditLockedP = false;
   }

   public boolean isEditLocked () {
      return myEditLockedP;
   }

   // public EditorBase getEditor(Class parent)
   // {
   // do
   // { EditorBase editor = editorsMap.get(parent);
   // if (editor != null)
   // { return editor;
   // }
   // parent = parent.getSuperclass();
   // }
   // while (parent != null);
   // return null;
   // }
}
