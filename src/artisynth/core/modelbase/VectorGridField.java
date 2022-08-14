package artisynth.core.modelbase;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import artisynth.core.modelbase.ScanWriteUtils.ClassInfo;
import artisynth.core.util.ScanToken;
import maspack.geometry.InterpolatingGridBase;
import maspack.geometry.VectorGrid;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.matrix.VectorObject;
import maspack.properties.PropertyList;
import maspack.util.IndentingPrintWriter;
import maspack.util.InternalErrorException;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

/**
 * A vector field defined over a regular 3D grid, using values set at the
 * grid's vertices, with values at other points obtained using trilinear
 * interpolation. The field is implemented using an embedded {@link
 * VectorGrid} component. Vectors are of type {@code T}, which must be an
 * instance of {@link VectorObject}.
 */
public class VectorGridField<T extends VectorObject<T>>
   extends GridFieldBase implements VectorFieldComponent<T> {

   VectorGrid<T> myGrid = null;

   public static PropertyList myProps =
      new PropertyList (VectorGridField.class, GridFieldBase.class);

   static {
      //myProps.add ("renderProps * *", "render properties", null);
   }
  
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   /**
    * This constructor should not be called by applications, unless {@link
    * #scan} is called immediately after.
    */
   public VectorGridField() {
      super();
   }

   /**
    * Constructs a field using a given grid.
    *
    * @param grid vector grid used to implement the field.
    */
   public VectorGridField (VectorGrid<T> grid) {
      super();
      setGrid (grid);
   }

   /**
    * Constructs a named field using a given grid.
    *
    * @param grid name name of the field
    * @param grid vector grid used to implement the field
    */
   public VectorGridField (String name, VectorGrid<T> grid) {
      super (name);
      setGrid (grid);
   }

   /**
    * Returns the number of vertices in the grid.
    *
    * @return number of grid vertices
    */
   public int numVertices() {
      return myGrid.numVertices();
   }
   
   /**
    * Queries the field value associated with this grid at a specified position.
    * The position is assumed to be in either local or world coordinates
    * depending on whether {@link #getUseLocalValuesForField} returns {@code true}.
    *
    * <p>If the query point is outside the grid, then the value for the nearest
    * grid point is returned if the property {@code cliptoGrid} is {@code
    * true}; otherwise {@code null} is returned.
    * 
    * @param pos query position
    * @return value at the query position
    */
   public T getValue (Point3d pos) {
      if (myUseLocalValuesForFieldP) {
         return myGrid.getLocalValue (pos, myClipToGrid);
      }
      else {
         return myGrid.getWorldValue (pos, myClipToGrid);
      }
   }

   /**
    * Gets the position of the vertex indexed by its indices along the x, y,
    * and z axes. The position is in either local or world coordinates
    * depending on whether {@link #getUseLocalValuesForField} returns {@code
    * true}.
    *
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return position of the vertex
    */
   public Point3d getVertexPosition (int xi, int yj, int zk) {
      Point3d coords = new Point3d();
      if (myUseLocalValuesForFieldP) {
         return (Point3d)myGrid.getLocalVertexCoords (
            coords, new Vector3i(xi, yj, zk));
      }
      else {
         return (Point3d)myGrid.getWorldVertexCoords (
            coords, new Vector3i(xi, yj, zk));
      }
   }

   /**
    * Gets the position of the vertex indexed by {@code vi}.  See {@link
    * #setVertexValue(int,VectorObject)} for a description of how {@code vi} is
    * computed. The position is in either local or world coordinates depending
    * on whether {@link #getUseLocalValuesForField} returns {@code true}.
    *
    * @param vi vertex index
    * @return position of the vertex
    */
   public Point3d getVertexPosition (int vi) {
      Point3d coords = new Point3d();
      if (myUseLocalValuesForFieldP) {
         return (Point3d)myGrid.getLocalVertexCoords (coords, vi);
      }
      else {
         return (Point3d)myGrid.getWorldVertexCoords (coords, vi);
      }
   }

   public T getValue (FieldPoint fp) {
      Point3d pos;
      if (myUseFemRestPositions) {
         pos = fp.getRestPos();
      }
      else {
         pos = fp.getSpatialPos();
      }
      if (myUseLocalValuesForFieldP) {
         return myGrid.getLocalValue (pos, myClipToGrid);
      }
      else {
         return myGrid.getWorldValue (pos, myClipToGrid);
      }
   }

   public T getValue (MeshFieldPoint fp) {
      if (myUseLocalValuesForFieldP) {
         return myGrid.getLocalValue (fp.getPosition(), myClipToGrid);
      }
      else {
         return myGrid.getWorldValue (fp.getPosition(), myClipToGrid);
      }
   }

   public VectorGrid<T> getGrid() {
      return myGrid;
   }

   /**
    * Returns the value at the grid vertex indexed by {@code vi}.  See {@link
    * #setVertexValue(int,VectorObject)} for a description of how {@code vi} is
    * computed.
    *
    * @param vi index of the vertex
    * @return value at the vertex
    */
   public T getVertexValue (int vi) {
      return myGrid.getVertexValue (vi);
   }

   /**
    * Sets the value at the grid vertex indexed by its indices along the x,
    * y, and z axes.
    *
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @param value new vertex value
    */
   public void setVertexValue (int xi, int yj, int zk, T value) {
      myGrid.setVertexValue (xi, yj, zk, value);
   }

   /**
    * Sets the value at the grid vertex indexed by {@code vi}, which is
    * computed from the axial vertex indices {@code xi}, {@code yj}, {@code zk}
    * according to
    * <pre>
    * vi = xi + nx*yj + (nx*ny)*zk
    * </pre>
    * where <code>nx</code> and <code>ny</code> are the number
    * of vertices along x and y axes.
    *
    * @param vi index of the vertex
    * @param value new vertex value
    */
   public void setVertexValue (int vi, T value) {
      myGrid.setVertexValue (vi, value);
   }

   /**
    * Returns the value at the grid vertex indexed by its indices along the x,
    * y, and z axes.
    *
    * @param xi x vertex index
    * @param yj y vertex index
    * @param zk z vertex index
    * @return value for the vertex
    */
   public T getVertexValue (int xi, int yj, int zk) {
      return myGrid.getVertexValue (xi, yj, zk);
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

   /* --- I/O methods --- */

   protected void writeGrid (PrintWriter pw, NumberFormat fmt)
      throws IOException {
      if (myGrid != null) {
         String classTag = null;
         if (myGrid.hasParameterizedType()) {
            classTag = ScanWriteUtils.getParameterizedClassTag (
               myGrid, myGrid.getParameterType());
         }
         else {
            classTag = ScanWriteUtils.getClassTag (myGrid);
         }
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
