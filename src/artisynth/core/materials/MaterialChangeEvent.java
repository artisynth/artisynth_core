package artisynth.core.materials;

import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.ModelComponent;
import maspack.properties.HasProperties;

/**
 * Class for reporting changes in Material settings within model subcomponents.
 * Considered to be a DynamicActivityChangeEvent since changing a material can
 * affect solve matrix symmetry.
 */
public class MaterialChangeEvent extends PropertyChangeEvent {

   private boolean myTangentSymmetryChanged = false;
   
   public MaterialChangeEvent (
      ModelComponent comp, String name, 
      boolean stateChanged, boolean symmetryChanged) {
      super (comp, name);
      setStateChanged (stateChanged);
      setTangentSymmetryChanged (symmetryChanged);
   }
   
   public MaterialChangeEvent (
      HasProperties host, String name, 
      boolean stateChanged, boolean symmetryChanged) {
      super (host, name);
      setStateChanged (stateChanged);
      setTangentSymmetryChanged (symmetryChanged);
   }
   
   public MaterialChangeEvent (ModelComponent comp, MaterialChangeEvent mce) {
      this (comp, mce.getPropertyName(), 
         mce.stateChanged(), mce.tangentSymmetryChanged());
   }

   public boolean tangentSymmetryChanged() {
      return myTangentSymmetryChanged;
   }
   
   public void setTangentSymmetryChanged(boolean changed) {
      myTangentSymmetryChanged = changed;
   }
   
   public boolean stateOrSymmetryChanged() {
      return myTangentSymmetryChanged || stateChanged();
   }
}
