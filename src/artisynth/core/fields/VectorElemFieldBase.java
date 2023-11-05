package artisynth.core.fields;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemElement3dBase;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.ShellElement3d;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.FemFieldPoint;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.ScanToken;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorObject;
import maspack.render.RenderObject;
import maspack.properties.*;
import maspack.util.ReaderTokenizer;

/**
 * Base class for VectorElementField and VectorSubElemField. Defines
 * common properties and methods.
 */
public abstract class VectorElemFieldBase<T extends VectorObject<T>>
   extends VectorFemField<T> {

   public static boolean DEFAULT_VOLUME_ELEMS_VISIBLE = true;
   protected boolean myVolumeElemsVisible = DEFAULT_VOLUME_ELEMS_VISIBLE;

   public static boolean DEFAULT_SHELL_ELEMS_VISIBLE = true;
   protected boolean myShellElemsVisible = DEFAULT_SHELL_ELEMS_VISIBLE;

   public static PropertyList myProps =
      new PropertyList (VectorElemFieldBase.class, VectorFemField.class);

   static {
      myProps.add (
         "volumeElemsVisible", "field is visible for volume elements",
         DEFAULT_VOLUME_ELEMS_VISIBLE);
      myProps.add (
         "shellElemsVisible", "field is visible for shell elements",
         DEFAULT_SHELL_ELEMS_VISIBLE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }   

   public VectorElemFieldBase (Class<T> type) {
      super (type);
   }

   public VectorElemFieldBase (Class<T> type, FemModel3d fem) {
      super (type, fem);
   }

   public VectorElemFieldBase (Class<T> type, FemModel3d fem, T defaultValue) {
      super (type, fem, defaultValue);
   }

   public boolean getVolumeElemsVisible () {
      return myVolumeElemsVisible;
   }

   public void setVolumeElemsVisible (boolean visible) {
      myVolumeElemsVisible = visible;
   }

   public boolean getShellElemsVisible () {
      return myShellElemsVisible;
   }

   public void setShellElemsVisible (boolean visible) {
      myShellElemsVisible = visible;
   }

}
