package artisynth.core.mechmodels;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.ArrayList;
import java.util.List;
import java.util.Collection;
import artisynth.core.modelbase.*;
import artisynth.core.modelbase.FieldUtils.ScalarFieldFunction;
import artisynth.core.util.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.properties.*;

/**
 * Component that encapsulates a DistanceGrid.
 */
public class DistanceGridComp extends GridCompBase {

   /**
    * Specfies the type of interpolation that should be used when constructing
    * an iso-surface for the grid.
    */
   public enum SurfaceType {
      /**
       * Construct the iso-surface using trilinear interpolation
       */
      TRILNEAR,

      /**
       * Construct the iso-surface using quadratic interpolation
       */
      QUADRATIC
   };

   DistanceGrid myGrid = null;
   boolean myGridValidP = false;
   boolean myGridExplicitP = false;
   PolygonalMesh mySurface = null;
   PolygonalMesh myRenderSurface = null;
   boolean mySurfaceValidP = false;
   ArrayList<MeshComponent> myGeneratingMeshes = new ArrayList<>();
   // list of generating meshes that are polygonal and have faces
   ArrayList<PolygonalMesh> myPolygonalMeshes = new ArrayList<>();
   // lists of faces from the generating meshes
   ArrayList<List<? extends Feature>> myGeneratingFaces =
      new ArrayList<List<? extends Feature>>();

   static Vector3i DEFAULT_RESOLUTION = new Vector3i(0, 0, 0);
   Vector3i myResolution = new Vector3i(DEFAULT_RESOLUTION);

   static int DEFAULT_MAX_RESOLUTION = 20;
   int myMaxResolution = DEFAULT_MAX_RESOLUTION;

   static boolean DEFAULT_FIT_WITH_OBB = false;
   boolean myFitWithOBB = DEFAULT_FIT_WITH_OBB;

   static double DEFAULT_MARGIN_FRACTION = 0.1;
   double myMarginFraction = DEFAULT_MARGIN_FRACTION;

   protected static SurfaceType DEFAULT_SURFACE_TYPE = SurfaceType.QUADRATIC;
   private SurfaceType mySurfaceType = DEFAULT_SURFACE_TYPE;

   protected static double DEFAULT_SURFACE_DISTANCE = 0;
   private double mySurfaceDistance = DEFAULT_SURFACE_DISTANCE;

   protected static boolean DEFAULT_RENDER_SURFACE = false;
   private boolean myRenderSurfaceP = DEFAULT_RENDER_SURFACE;

   // for debugging problems with surface tangent computation
   boolean writeTanProblem = false;
   String myTangentProblemFile = "tanProb.txt";

   public static PropertyList myProps =
      new PropertyList (DistanceGridComp.class, GridCompBase.class);

   static {
      myProps.add (
         "resolution", "divisions for grid along x, y, and z",
         DEFAULT_RESOLUTION);
      myProps.add (
         "maxResolution", 
         "max divisions for signed distance grid",
         DEFAULT_MAX_RESOLUTION);
      myProps.add (
         "fitWithOBB", 
         "if true, grid is fitted using an oriented bounded box (OBB)",
         DEFAULT_FIT_WITH_OBB);
      myProps.add (
         "marginFraction", 
         "margin fraction used when creating a grid",
         DEFAULT_MARGIN_FRACTION);
      myProps.add (
         "surfaceType", "type of iso-surface to generate for the grid",
         DEFAULT_SURFACE_TYPE);
      myProps.add (
         "surfaceDistance", "distance value associated with the iso-surface",
         DEFAULT_SURFACE_DISTANCE);
      myProps.add (
         "renderSurface", 
         "render the iso-surface of the grid in the viewer",
         DEFAULT_RENDER_SURFACE);
   }
  
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public DistanceGridComp() {
      setRenderProps (createRenderProps());
   }

   public DistanceGridComp (String name) {
      super (name);
   }

   protected void invalidateGrid() {
      myGridValidP = false;
      myGridExplicitP = false;
      mySurfaceValidP = false;
   }

   protected void handleChangedGeneratingMeshes() {
      // invalidate grid if it is not explicit
      if (!myGridExplicitP) {
         invalidateGrid();
      }
      // rebuild the list of polygonal meshes. Do this so we know in advance 
      // if we can actually build a grid from the meshes 
      // (if the list is empty, we can't)
      myPolygonalMeshes.clear();
      for (MeshComponent mcomp : myGeneratingMeshes) {
         if (mcomp.getMesh() instanceof PolygonalMesh) {
            PolygonalMesh mesh = (PolygonalMesh)mcomp.getMesh();
            if (mesh.numFaces() > 0) {
               myPolygonalMeshes.add (mesh);
            }
         }
      }
   }

   public void setGeneratingMeshes (Collection<MeshComponent> meshes) {
      myGeneratingMeshes.clear();
      myGeneratingMeshes.addAll (meshes);
      handleChangedGeneratingMeshes();
   }

   public boolean removeGeneratingMesh (MeshComponent mcomp) {
      if (myGeneratingMeshes.remove (mcomp)) {
         handleChangedGeneratingMeshes();
         return true;
      }
      else {
         return false;
      }
   }

   public void addGeneratingMesh (MeshComponent mcomp) {
      myGeneratingMeshes.add (mcomp);
      handleChangedGeneratingMeshes();
   }

   public void clearGeneratingMeshes () {
      myGeneratingMeshes.clear();
      handleChangedGeneratingMeshes();
   }

   /**
    * Returns the cell resolutions (in the x, y, and z directions) that should
    * be used when automatically constructing a grid. See
    * {@link #setResolution} for a more detailed description.
    *
    * @return x, y, and z divisions to be used in constructing a grid
    * @see #setResolution
    */
   public Vector3i getResolution () {
      return myResolution;
   }

   /**
    * Sets the cell resolution (in the x, y, and z directions) that should be
    * used when automatically constructing a grid.  This
    * specifies the number of cells that the grid should have along each
    * axis. If any of the values are {@code <=} 0, then all of the values are
    * set to zero and the value returned by {@link #getMaxResolution}) is
    * used to determine the grid divisions instead.
    *
    * @param res x, y, and z cell divisions to be used in constructing a grid
    * @see #getResolution
    */
   public void setResolution (Vector3i res) {
      if (!myGridExplicitP) {
         if (!myResolution.equals (res)) {
            if (res.x <= 0 || res.y <= 0 || res.z <= 0) {
               // MaxDivs == 0 and Divs == (0,0,0) disables grid
               myResolution.setZero();
            }
            else {
               myResolution.set (res);
            }
            invalidateGrid();
            myRenderRanges = DEFAULT_RENDER_RANGES;
         }
      }
   }

   /**
    * Returns the default maximum cell resolution that should be used when
    * automatically constructing a grid. See {@link #setMaxResolution} for
    * a more detailed description.
    *
    * @return default maximum cell resolution for constructing a grid
    */
   public int getMaxResolution () {
      return myMaxResolution;
   }

   /**
    * Sets the default maximum cell resolution that should be used when
    * automatically constructing a distance grid. This is the number of cells
    * that will be used along the maximum length in either the x, y, or z
    * direction, with the number of cells in other directions adjusted
    * accordingly so as to provide cells of uniform size. If <code>max</code>
    * is {@code <=} 0, the value will be set to 0. If the values returned by
    * {@link #getResolution}) are non-zero, those will be used to specify
    * the cell resolutions instead. If the maximum cell resolution and the
    * values returned by {@link #getResolution} are all 0, then no grid
    * will be generated.
    *
    * @param max default maximum cell resolution for constructing a grid
    */
   public void setMaxResolution (int max) {
      if (myMaxResolution != max) {
         if (myResolution.equals (Vector3i.ZERO)) {
            // will need to rebuild grid
            if (!myGridExplicitP) {
               invalidateGrid();
            }
         }
         if (max < 0) {
            // MaxDivs == 0 and Divs == (0,0,0) disables grid
            max = 0;
         }
         myMaxResolution = max;
         myRenderRanges = DEFAULT_RENDER_RANGES;
      }
   }

   /**
    * Queries whether an OBB is used when automatically constructing a grid.
    * See {@link #setFitWithOBB} for more details.
    * 
    * @return {@code true} if grid fitting is done using an OBB
    */
   public boolean getFitWithOBB () {
      return myFitWithOBB;
   }

   /**
    * Enables OBB fitting to be used when automatically constructing a grid.
    * This means that rather than aligning the grid with the coordinate axes of
    * the underlying mesh(es), the grid will be aligned with an OBB fit to the
    * mesh(es). This generally means that the grid will around the mesh(es)
    * more tightly.
    * 
    * @param enable if {@code true}, enables OBB fitting for grid construction
    */
   public void setFitWithOBB (boolean enable) {
      if (myFitWithOBB != enable) {
         myFitWithOBB = enable;
         if (!myGridExplicitP) {
            invalidateGrid();
         }
      }
   }

   // TODO get OBB coordinate frame?

   /**
    * Returns the margin fraction used when automatically constructing a grid.
    *
    * @return margin fraction
    */
   public double getMarginFraction() {
      return myMarginFraction;
   }

   /**
    * Sets the margin fraction used when automatically constructing a grid.
    *
    * @param frac margin fraction
    */
   public void setMarginFraction (double frac) {
      myMarginFraction = frac;
   }

   /**
    * Queries the type of interpolation used when creating the iso-surface
    * for this grid.
    *
    * @return iso-surface interpolation 
    */
   public SurfaceType getSurfaceType () {
      return mySurfaceType;
   }

   /**
    * Sets the type of interpolation used when creating the iso-surface
    * for this grid.
    *
    * @param type iso-surface interpolation 
    */
   public void setSurfaceType (SurfaceType type) {
      if (mySurfaceType != type) {
         mySurfaceType = type;
         mySurfaceValidP = false;
      }
   }

   /**
    * Returns the distance value used for creating this grid's iso-surface.
    *
    * @return iso-surface distance value
    */
   public double getSurfaceDistance() {
      return mySurfaceDistance;
   }

   /**
    * Sets the distance value used for creating this grid's iso-surface.
    *
    * @param dist iso-surface distance value
    */
   public void setSurfaceDistance (double dist) {
      if (mySurfaceDistance != dist) {
         mySurfaceDistance = dist;
         mySurfaceValidP = false;
      }
   }

   /**
    * Queries whether or not iso-surface rendering is enabled for this grid.
    *
    * @return {@code true} if iso-surface rendering is enabled 
    */
   public boolean getRenderSurface() {
      return myRenderSurfaceP;
   }

   /**
    * Sets whether or not iso-surface rendering is enabled for this grid.  If
    * enabled, the iso-surface associated with the grid is rendered in the
    * viewer.
    *
    * @param enable if {@code true}, enables iso-surface rendering
    */
   public void setRenderSurface (boolean enable) {
      myRenderSurfaceP = enable;
   }

   public void setLocalToWorld (RigidTransform3d TGW) {
      super.setLocalToWorld (TGW);
      if (mySurface != null) {
         mySurface.setMeshToWorld (TGW);
      }
   }

   public void scaleDistance (double s) {
      if (myGridExplicitP) {
         if (myGrid != null) {
            myGrid.scaleDistance (s);
         }
         if (mySurface != null) {
            mySurface.scale (s);
         }
      }
      else {
         invalidateGrid();
      }
      myLocalToWorld.p.scale (s);
   }

   protected boolean canGenerateGrid() {
      return (myPolygonalMeshes.size() > 0 &&
              (!myResolution.equals (Vector3i.ZERO) || myMaxResolution > 0));
   }

   public boolean hasGrid() {
      if (myGridValidP && myGrid != null) {
         return true;
      }
      else {
         return canGenerateGrid();
      }
   }

   public DistanceGrid getGrid() {
      if (!myGridValidP) {
         buildGridFromMeshes (myGeneratingMeshes);
      }
      return myGrid;
   }

   protected void buildGridFromMeshes (ArrayList<MeshComponent> meshes) {

      DistanceGrid grid = null;

      if (canGenerateGrid()) {
         
         // create the grid with the specified resolution
         int maxRes = myMaxResolution;
         if (!myResolution.equals (Vector3i.ZERO)) {
            grid = new DistanceGrid (myResolution);
            maxRes = 0;
         }
         else {
            // resolution will be recomputed in computeFromFeatures
            grid = new DistanceGrid (new Vector3i (1,1,1));
         }
                  
         // fit the grid to the faces
         ArrayList<List<? extends Feature>> faceLists = new ArrayList<>();
         for (PolygonalMesh mesh : myPolygonalMeshes) {
            faceLists.add (mesh.getFaces());
         }
         if (myFitWithOBB) {
            grid.fitToFeaturesOBB (
               faceLists, myMarginFraction, maxRes);
         }
         else {
            grid.fitToFeatures (
               faceLists, myMarginFraction, /*TCL=*/null, maxRes);
         }
         grid.setLocalToWorld (getLocalToWorld());

         grid.computeDistances (myPolygonalMeshes.get(0), /*signed=*/true);
         if (myPolygonalMeshes.size() > 1) {
            for (PolygonalMesh mesh : myPolygonalMeshes) {
               grid.computeUnion (mesh);
            }
            grid.computeDistances (
               grid.createDistanceSurface(), /*signed=*/true);
         }
         setRenderRanges (myRenderRanges);
         // estimate the minimum distance from the surface to the grid
         // boundary, and then mark any quad tets whose nodes have a greater
         // distance as being "outside"
         Vector3d margins = new Vector3d(grid.getWidths());
         margins.scale (myMarginFraction);
         //grid.markOutsideQuadtets (margins.minElement());         
      }
      super.setGrid (grid); // set myBaseGrid in the super class
      myGrid = grid;
      mySurfaceValidP = false;
      myGridValidP = true;
   }

   /**
    * Explicitly sets the grid for this DistanceGridComp. The grid is set by
    * reference (i.e., it is not copied). The grid's {@code renderRanges} and
    * {@code localToWorld} transform are updated from the current {@code
    * renderRanges} and {@code gridtoWorld} transform values for this
    * component. If the grid is set to be {@code null}, then any explicit grid
    * is cleared and automatic generation is reenabled.
    *
    * @param grid grid to set
    */
   public void setGrid (DistanceGrid grid) {
      super.setGrid (grid);  // set myBaseGrid in the super class
      if (grid == null) {
         invalidateGrid();
      }
      else {
         myGrid = grid;
         myGridValidP = true;
         myGridExplicitP = true;
         mySurfaceValidP = false;
         myGeneratingMeshes.clear();
         myPolygonalMeshes.clear();
         myResolution.set (grid.getResolution());

         grid.setLocalToWorld (myLocalToWorld);
         grid.setRenderRanges (myRenderRanges);
      }      
   }

   public PolygonalMesh getSurface() {
      if (!mySurfaceValidP) {
         DistanceGrid grid = getGrid();
         if (grid != null) {
            PolygonalMesh surf;
            if (mySurfaceType == SurfaceType.QUADRATIC) {
               surf = grid.createQuadDistanceSurface(mySurfaceDistance, 4);
               surf.setMeshToWorld (myLocalToWorld);
            }
            else if (mySurfaceType == SurfaceType.TRILNEAR) {
               surf = grid.createDistanceSurface(mySurfaceDistance, 4);
               surf.setMeshToWorld (myLocalToWorld);
            }
            else {
               throw new UnsupportedOperationException (
                  "Unsupported SurfaceType " + mySurfaceType);
            }
            // do we need to call prerender()?
            mySurface = surf;
         }
         mySurfaceValidP = true;
      }
      return mySurface;
   }

   /* --- Wrappble support --- */

   public void surfaceTangent (
      Point3d pr, Point3d pa, Point3d p1, double lam0, Vector3d sideNrm) {

      DistanceGrid grid = getGrid();
      if (grid == null) {
         // shouldn't happen, since if there is no grid penetrationDistance()
         // won't report any collisions and consequently this method won't be
         // called. Set pr to p1 as the best guess.
         pr.set (p1);         
         return;
      }
      boolean found = grid.findQuadSurfaceTangent (pr, p1, pa, sideNrm);
      if (!found || writeTanProblem) {
         if (!found) {
            System.out.println ("couldn't find tangent");
         }
         System.out.println ("p1=" + p1.toString ("%10.5f"));
         System.out.println ("pa=" + pa.toString ("%10.5f"));
         System.out.println ("nm=" + sideNrm.toString ("%10.5f"));
         if (myTangentProblemFile != null) {
            System.out.println ("Writing problem to "+myTangentProblemFile);
            DistanceGridSurfCalc.TangentProblem tprob =
               new DistanceGridSurfCalc.TangentProblem (
                  p1, pa, sideNrm, grid);
            tprob.write (myTangentProblemFile, "%g");
         }
         grid.setDebug (1);
         grid.findQuadSurfaceTangent (pr, p1, pa, sideNrm);
         grid.setDebug (0);
         // pr will already be set to either p0 or the nearest
         // surface poin ps
      }
   }

   public double penetrationDistance (Vector3d nrm, Matrix3d Dnrm, Point3d p0) {

      DistanceGrid grid = getGrid();
      if (grid == null) {
         return Wrappable.OUTSIDE;
      }
      Point3d p0loc = new Point3d();
      p0loc.inverseTransform (myLocalToWorld, p0);
      //double d = grid.getLocalDistanceAndNormal (nrm, Dnrm, p0loc);
      grid.useNormalsForQuadGrad = false;
      double d = grid.getQuadDistanceAndGradient (nrm, Dnrm, p0loc);
      grid.useNormalsForQuadGrad = false;
      RotationMatrix3d R = myLocalToWorld.R;
      if (Dnrm != null) {
         Dnrm.transform (R);
      }
      if (nrm != null) {
         nrm.transform (R);
      }
      if (d == DistanceGrid.OUTSIDE_GRID) {
         return Wrappable.OUTSIDE;
      }
      else {
         return d;
      }   
   }

   /* --- Renderable --- */

   public void prerender (RenderList list) {
      super.prerender (list);
      if (myRenderSurfaceP) {
         PolygonalMesh surf = getSurface();
         if (surf != null) {
            surf.prerender (myRenderProps); 
         }
         myRenderSurface = surf;
      }
      else {
         myRenderSurface = null;
      }
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      PolygonalMesh surf = myRenderSurface;
      if (surf != null) {
         surf.render (renderer, myRenderProps, flags);
      }
   }

   public void getSoftReferences (List<ModelComponent> refs) {
      refs.addAll (myGeneratingMeshes);
   }
   
   /**
    * {@inheritDoc}
    */
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);
      if (ComponentUtils.updateReferences (
             this, myGeneratingMeshes, undo, undoInfo)) {
         handleChangedGeneratingMeshes();
      }
   }  

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      pw.print ("generatingMeshes=");
      ScanWriteUtils.writeBracketedReferences (
         pw, myGeneratingMeshes, ancestor);
   } 

   protected void writeGrid (PrintWriter pw, NumberFormat fmt)
      throws IOException {
      if (myGridExplicitP) {
         pw.print ("grid=");
         IndentingPrintWriter.addIndentation (pw, 2);
         myGrid.write (pw, fmt, null);
         IndentingPrintWriter.addIndentation (pw, -2);
      }
   } 

   protected InterpolatingGridBase scanGrid (
      ReaderTokenizer rtok) throws IOException {
      myGrid = new DistanceGrid();
      myGrid.scan (rtok, null);
      myResolution.set (myGrid.getResolution());
      return myGrid;
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReferences (rtok, "generatingMeshes", tokens) >= 0) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }
   
   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "generatingMeshes")) {
         myGeneratingMeshes.clear();
         ScanWriteUtils.postscanReferences (
            tokens, myGeneratingMeshes, MeshComponent.class, ancestor);
         handleChangedGeneratingMeshes();
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
}      
