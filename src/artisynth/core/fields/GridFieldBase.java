package artisynth.core.fields;

import artisynth.core.modelbase.FemFieldPoint;
import artisynth.core.modelbase.GridCompBase;
import maspack.properties.PropertyList;

/**
 * Base component for grid-based fields
 */
public abstract class GridFieldBase extends GridCompBase {

   protected static boolean DEFAULT_USE_LOCAL_VALUES_FOR_FIELD = true;
   protected boolean myUseLocalValuesForFieldP = 
      DEFAULT_USE_LOCAL_VALUES_FOR_FIELD;

   protected static boolean DEFAULT_USE_FEM_REST_POSITIONS = true;
   protected boolean myUseFemRestPositions = DEFAULT_USE_FEM_REST_POSITIONS;

   protected static boolean DEFAULT_CLIP_TO_GRID = true;
   protected boolean myClipToGrid = DEFAULT_CLIP_TO_GRID;

   public static PropertyList myProps =
      new PropertyList (GridFieldBase.class, GridCompBase.class);

   static {
      myProps.add (
         "useLocalValuesForField", 
         "use values in local coordinates for field values", 
         DEFAULT_USE_LOCAL_VALUES_FOR_FIELD);
      myProps.add (
         "clipToGrid",
         "clip query points outside the grid surface", 
         DEFAULT_CLIP_TO_GRID);
      myProps.add (
         "useFemRestPositions",
         "use rest positions when determining values within an FEM", 
         DEFAULT_USE_FEM_REST_POSITIONS);
   }
  
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   protected GridFieldBase () {
      super ();
   }

   protected GridFieldBase (String name) {
      super (name);
   }

   /**
    * Queries whether or local values should be used for field queries.
    *
    * @return {@code true} if local values should be used for field queries
    */
   public boolean getUseLocalValuesForField() {
      return myUseLocalValuesForFieldP;
   }

   /**
    * Sets whether or not local values should be used for field queries.
    *
    * @param enable if {@code true}, enables using local values for field
    * queries
    */
   public void setUseLocalValuesForField (boolean enable) {
      myUseLocalValuesForFieldP = enable;
   }

   /**
    * Queries whether or not rest positions are used when determining values
    * for points within an FEM mesh. See {@link #setUseFemRestPositions}.
    *
    * @return {@code true} if rest positions are used for values
    * within an FEM mesh
    */
   public boolean getUseFemRestPositions() {
      return myUseFemRestPositions;
   }

   /**
    * Sets whether or not rest positions are used when determining values for
    * points within an FEM mesh. If rest positions are not used, the point's
    * current spatial position is used instead. The default value is {@code
    * true}.  (Values within an FEM mesh are determined using special methods of
    * the form {@code getValue(FieldPoint)}, where {@link FemFieldPoint} contains
    * both spatial and rest positions.)
    *
    * @param enable if {@code true}, enables using rest positions
    * for values within an FEM mesh
    */
   public void setUseFemRestPositions (boolean enable) {
      myUseFemRestPositions = enable;
   }

   /**
    * Queries whether clip-to-grid is enabled for this grid field. See {@link
    * #setClipToGrid}.
    *
    * @return {@code true} if clip-to-grid is enabled
    */
   public boolean getClipToGrid() {
      return myClipToGrid;
   }

   /**
    * Sets whether clip-to-grid is enabled for this grid field.  If enabled,
    * queries for points that are outside the grid return the value for the
    * nearest grid point; i.e., the query point is clipped to the grid.  If not
    * enabled, queries for points outside the grid return {@link
    * ScalarGridField#OUTSIDE_GRID} for scalar grids and {@code null} for
    * vector grids. The default value is {@code true}.
    *
    * @param enable if {@code true}, enable clip-to-grid
    */
   public void setClipToGrid (boolean enable) {
      myClipToGrid = enable;
   }

}      
