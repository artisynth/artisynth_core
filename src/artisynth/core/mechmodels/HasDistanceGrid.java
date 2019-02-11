package artisynth.core.mechmodels;

import maspack.geometry.DistanceGrid;
import maspack.matrix.Vector3i;

/**
 * Methods exported by components which support a signed distance grid.
 */
public interface HasDistanceGrid {

   /**
    * Returns <code>true</code> if this component maintains a signed distance
    * grid. Among other possible conditions, a grid is available only if either
    * {@link #getDistanceGridMaxRes} or {@link #getDistanceGridRes} return
    * positive values.
    * 
    * @return <code>true</code> if a signed distance grid is available
    * for this RigidBody
    * @see #getDistanceGridMaxRes
    * @see #getDistanceGridRes
    */
   public boolean hasDistanceGrid();

   /**
    * Returns a signed distance grid maintained by this body, or
    * <code>null</code> if a grid is not available (i.e., if {@link
    * #hasDistanceGrid} returns <code>false</code>).  The number of divisons in
    * the grid is controlled explicitly by the x, y, z values returned by
    * {@link #getDistanceGridRes}, or, if those are 0, by the maximum grid
    * resolution returned by {@link #getDistanceGridMaxRes}. If that is 0 as
    * well, no grid is available and <code>null</code> will be returned.
    *
    * @return signed distance grid, or <code>null</code> if a grid is
    * not available for this component
    * @see #getDistanceGridMaxRes
    * @see #getDistanceGridRes
    */
   public DistanceGrid getDistanceGrid();

   /**
    * Sets the default maximum cell resolution that should be used when
    * constructing a signed distance grid for this component. This is the
    * number of cells that will be used along the maximum length in
    * either the x, y, or z direction,
    * with the number of cells in other directions adjusted accordingly so as
    * to provide cells of uniform size. If <code>max</code> is {@code <=} 0,
    * the value will be set to 0. If the values returned by {@link
    * #getDistanceGridRes}) are non-zero, those will be used to specify the
    * cell resolutions instead. If the maximum cell resolution and the values
    * returned by {@link #getDistanceGridRes} are all 0, then no signed
    * distance grid will be available for this component and {@link
    * #hasDistanceGrid} will return false.
    *
    * @param max default maximum cell resolution for constructing a signed
    * distance grid
    */
   public void setDistanceGridMaxRes (int max);

   /**
    * Returns the default maximum cell resolution that should be used when
    * constructing a signed distance grid for this component. See {@link
    * #getDistanceGridMaxRes} for a more detailed description.
    *
    * @return default maximum cell resolution for constructing a signed
    * distance grid
    */
   public int getDistanceGridMaxRes();

   /**
    * Sets the cell resolution (in the x, y, and z directions) that should be
    * used when constructing a signed distance grid for this component.  This
    * specifies the number of cells that the grid should have along each
    * axis. If any of the values are {@code <=} 0, then all of the values are
    * set to zero and the value returned by {@link #getDistanceGridMaxRes}) is
    * used to determine the grid divisions instead.
    *
    * @param res cell resolution along x, y, z axes to be used in constructing 
    * a signed distance grid
    * @see #getDistanceGridRes
    */
   public void setDistanceGridRes (Vector3i res);

   /**
    * Returns the cell resolutions (in the x, y, and z directions) that should
    * be used when constructing a signed distance grid for this component. See
    * {@link #setDistanceGridRes} for a more detailed description.
    *
    * @return x, y, and z divisions to be used in constructing a signed
    * distance grid
    * @see #setDistanceGridRes
    */
   public Vector3i getDistanceGridRes ();

   /**
    * Queries whether this component's signed distance grid should be rendered.
    *
    * @return <code>true</code> if distance grid rendering is enabled
    */
   public boolean getRenderDistanceGrid();

   /**
    * Enables or disables rendering of this component's signed distance grid.
    *
    * @param enable if <code>true</code>, enables distance grid rendering
    */
   public void setRenderDistanceGrid (boolean enable);

   /**
    * Returns a string describing the x, y, z vertex ranges used when rendering
    * this component's signed distance grid. See {@link
    * #setDistanceGridRenderRanges} for a more detailed description.
    * 
    * @return string describing the render ranges
    * @see #setDistanceGridRenderRanges
    */
   public String getDistanceGridRenderRanges();
   
   /**
    * Specifies the x, y, z vertex ranges used when rendering this component's
    * signed distance grid. The signed distance grid will have
    * <code>numVX</code> X <code>numVY</code> X <code>numVZ</code> vertices in
    * the x, y, z directions, where <code>numVX</code>, <code>numVY</code>, and
    * <code>numVZ</code> are each one greater than the x, y, z cell resolution
    * values returned by the {@link DistanceGrid#getResolution}
    * method of the distance grid itself. In general, the range string should
    * contain three range specifications, one for each axis, where each
    * specification is either <code>*</code> (all vertices), <code>n:m</code>
    * (vertices in the index range <code>n</code> to <code>m</code>, inclusive),
    * or <code>n</code> (vertices only at index <code>n</code>). A
    * range specification of <code>"* * *"</code> (or <code>"*"</code>)
    * means draw all vertices, which is the default behavior. Other
    * examples include:
    * <pre>
    *  "* 7 *"      - all vertices along x and z, and those at index 7 along y
    *  "0 2 3"      - a single vertex at indices (0, 2, 3)
    *  "0:3 4:5 *"  - all vertices between indices 0 and 3 along x, and 4 and 5
    *                 along y
    * </pre>
    * 
    * @param ranges describing the render ranges
    * @see #getDistanceGridRenderRanges
    * @throws IllegalArgumentException if the range syntax is invalid
    * or out of range
    */
   public void setDistanceGridRenderRanges (String ranges);
   
}
