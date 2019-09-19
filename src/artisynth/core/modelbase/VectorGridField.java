package artisynth.core.modelbase;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import artisynth.core.modelbase.*;
import artisynth.core.modelbase.ScanWriteUtils.ClassInfo;
import artisynth.core.modelbase.FieldUtils.VectorFieldFunction;
import artisynth.core.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.properties.*;

/**
 * Component that encapsulates a VectorGrid.
 */
public class VectorGridField<T extends VectorObject<T>>
   extends GridCompBase implements VectorField<T>, FieldComponent {

   VectorGrid<T> myGrid = null;

   public static PropertyList myProps =
      new PropertyList (VectorGridField.class, GridCompBase.class);

   static {
      //myProps.add ("renderProps * *", "render properties", null);
   }
  
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public class RestFieldFunction extends VectorFieldFunction<T> {

      public RestFieldFunction () {
      }

      public VectorGridField getField() {
         return VectorGridField.this;
      }

      public T eval (FieldPoint def) {
         return getValue (def.getRestPos());
      }
   }

   public class SpatialFieldFunction extends VectorFieldFunction<T> {

      public SpatialFieldFunction () {
      }

      public VectorGridField getField() {
         return VectorGridField.this;
      }

      public T eval (FieldPoint def) {
         return getValue (def.getRestPos());
      }

      public boolean useRestPos() {
         return false;
      }
   }

   public VectorFieldFunction<T> createFieldFunction (boolean useRestPos) {
      if (useRestPos) {
         return new RestFieldFunction();
      }
      else {
         return new SpatialFieldFunction();
      }
   }

   public VectorGridField() {
      super();
   }

   public VectorGridField (String name) {
      super (name);
   }

   public VectorGridField (String name, VectorGrid<T> grid) {
      super (name);
      setGrid (grid);
   }

   public VectorGridField (VectorGrid<T> grid) {
      super();
      setGrid (grid);
   }

   /**
    * Queries the field value associated with this grid at a specifed position.
    * The position is assumed to be in either local or world coordinates
    * depeneding on whether {@link #getLocalValuesForField} returns {@code true}.
    * 
    * @param pos query position
    */
   public T getValue (Point3d pos) {
      if (myLocalValuesForFieldP) {
         return myGrid.getLocalValue (pos);
      }
      else {
         return myGrid.getWorldValue (pos);
      }
   }

   public VectorGrid<T> getGrid() {
      return myGrid;
   }

   /**
    * Sets the grid for this VectorGridField. The grid is set by reference
    * (i.e., it is not copied). The grid's {@code renderRanges} and {@code
    * localToWorld} transform are updated from the current {@code renderRanges}
    * and {@code gridtoWorld} transform values for this component.
    *
    * @param grid grid to set
    */
   public void setGrid (VectorGrid<T> grid) {
      if (grid == null) {
         throw new IllegalArgumentException ("Grid cannot be null");
      }
      super.setGrid (grid); // set myBaseGrid in the super class
      myGrid = grid;
      grid.setLocalToWorld (myLocalToWorld);
      grid.setRenderRanges (myRenderRanges);
   }

   protected void writeGrid (PrintWriter pw, NumberFormat fmt)
      throws IOException {
      if (myGrid != null) {
         String classTag = 
            ScanWriteUtils.getParameterizedClassTag (
               myGrid, myGrid.getParameterType());
         pw.print ("grid="+classTag+" ");
         IndentingPrintWriter.addIndentation (pw, 2);
         getGrid().write (pw, fmt, null);
         IndentingPrintWriter.addIndentation (pw, -2);
      }
   }

   protected InterpolatingGridBase scanGrid (
      ReaderTokenizer rtok) throws IOException {

      ClassInfo<?> classInfo =
         ScanWriteUtils.scanClassInfo (rtok, VectorGrid.class);
      myGrid = (VectorGrid<T>)ScanWriteUtils.newComponent (
         rtok, classInfo, /*warnOnly=*/false);
      myGrid.scan (rtok, null);
      return myGrid;
   }

}      
