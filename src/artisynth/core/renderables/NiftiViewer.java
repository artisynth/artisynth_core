/**
 * Copyright (c) 2015, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package artisynth.core.renderables;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;

import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.FrameMarker;
import artisynth.core.mechmodels.HasSurfaceMesh;
import artisynth.core.mechmodels.IsMarkable;
import artisynth.core.mechmodels.Marker;
import artisynth.core.mechmodels.PointList;
import artisynth.core.modelbase.ComponentChangeEvent;
import artisynth.core.modelbase.ComponentListImpl;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.StructureChangeEvent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScanToken;
import maspack.geometry.BVFeatureQuery;
import maspack.geometry.GeometryTransformer;
import maspack.geometry.PolygonalMesh;
import maspack.image.nifti.MappedPixelGenerator;
import maspack.image.nifti.NiftiImage;
import maspack.image.nifti.NiftiPixelGenerator;
import maspack.image.nifti.NiftiReader;
import maspack.image.nifti.NiftiTextureContent;
import maspack.image.nifti.NiftiHeader.XFormCode;
import maspack.image.nifti.NiftiImage.ImageSpace;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Line;
import maspack.matrix.Point2d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.properties.HierarchyNode;
import maspack.properties.PropertyList;
import maspack.render.ColorMapProps;
import maspack.render.LineRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import maspack.util.IntegerInterval;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class NiftiViewer extends Frame 
   implements CompositeComponent, RenderableComponent, TransformableGeometry, 
   IsMarkable, HasSurfaceMesh {

   static int XY_IDX = 0;
   static int XZ_IDX = 1;
   static int YZ_IDX = 2;
   
   NiftiImage myImage;
   NiftiPixelGenerator myVoxelator;
   NiftiTextureContent texture;

   public static PropertyList myProps = new PropertyList(
      NiftiViewer.class, Frame.class);
   
   static {
      myProps.add("x * *", "coordinate for YZ plane", 0.5, "[0,1]");
      myProps.add("y * *", "coordinate for XZ plane", 0.5, "[0,1]");
      myProps.add("z * *", "coordinate for XY plane", 0.5, "[0,1]");
      myProps.add("timeIndex * *", "time coordinate", 0);
      myProps.add("transform * *", "additional transform to apply to image",
         new AffineTransform3d(RigidTransform3d.IDENTITY) );
      myProps.add("imageSpace * *", "Nifti image space", ImageSpace.DETECT);
      myProps.addReadOnly("imageSpaceDesc", "Nifti image space");
      myProps.add("drawYZ * *", "draw XY plane", true);
      myProps.add("drawXZ * *", "draw XY plane", true);
      myProps.add("drawXY * *", "draw XY plane", true);
      myProps.add("drawBox * *", "draw image box", true);
      myProps.add("pixelGenerator", "voxel generator", new MappedPixelGenerator());
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public static RenderProps createDefaultRenderProps() {
      return new LineRenderProps();
   }
   
   ImageSpace myImageSpace;
   
   // decompose transform into rigid frame and affine
   //     T_net = T_frame T_affine
   AffineTransform3d myResidualAffineTransform;
   AffineTransform3d myNetTransform;

   PointList<FrameMarker> markers;
   
   boolean drawBox;
   boolean drawSlice[];
   PolygonalMesh sliceSurfaces[];

   RenderObject robj;
   boolean robjValid;
   boolean surfacesValid;

   /**
    * Creates a new viewer widget, with supplied name and Nifti image
    * @param name name of viewer
    * @param image image to display
    */
   public NiftiViewer(String name, NiftiImage image, NiftiPixelGenerator voxelator) {
      super();
      init(name, image, voxelator);
   }
   
   /**
    * Creates a new viewer widget, with supplied Nifti image.  The
    * name of the component becomes the image name
    * @param image image to display
    */
   public NiftiViewer(NiftiImage image) {
      this(image.getTitle(), image, null);
   }
   
   private void init(String name, NiftiImage image, NiftiPixelGenerator voxelator) {
      setName(ModelComponentBase.makeValidName(name));

      markers = new PointList<FrameMarker>(FrameMarker.class, "markers");
      add(markers);

      myRenderProps = createRenderProps();
      robj = null;
      robjValid = false;
      
      myResidualAffineTransform = new AffineTransform3d(RigidTransform3d.IDENTITY);
      myNetTransform = new AffineTransform3d(RigidTransform3d.IDENTITY);
      myImageSpace = ImageSpace.DETECT;
      drawBox = true;
      drawSlice = new boolean[]{true, true, true};
      
      if (voxelator == null) {
         MappedPixelGenerator mvox = new MappedPixelGenerator();
         mvox.detectDefault(image);
         voxelator = mvox;
      }
      setImage(image, voxelator);
      setSliceCoordinates(0.5, 0.5, 0.5);
      
      sliceSurfaces = buildSurfaces();
      surfacesValid = true;
   }
   
   @Override
   public RenderProps createRenderProps() {
      RenderProps props = RenderProps.createRenderProps (this);
      props.setFaceColor (Color.WHITE);
      props.setShading (Shading.NONE);
      ColorMapProps cprops = new ColorMapProps ();
      cprops.setEnabled (true);
      cprops.setColorMixing (ColorMixing.MODULATE);
      props.setColorMap (cprops);
      props.setFaceStyle (FaceStyle.FRONT_AND_BACK);

      return props;
   }
   
   /**
    * Reads a Nifti file into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath Nifti file
    */
   public NiftiViewer(String name, String imagePath) {
      NiftiImage im = null;
      try {
         im = NiftiReader.read(new File(imagePath));
      } catch (IOException ioe) {
         throw new RuntimeException(ioe);
      } catch(Exception e) {
         throw new RuntimeException("Failed to read nifti image " + imagePath, e);
      }
      if (im == null) {
         throw new RuntimeException("No image data loaded");
      }
      init(name, im, null);
   }
  
   /**
    * @return the Nifti image being displayed
    */
   public NiftiImage getImage() {
      return myImage;
   }
   
   /**
    * Sets the DICOM image to display
    * @param image
    */
   private void setImage(NiftiImage image, NiftiPixelGenerator voxelator) {
      myImage = image;
      texture = new NiftiTextureContent (image, voxelator);
      myVoxelator = voxelator;
      myRenderProps.getColorMap ().setContent (texture);
   }
   
   //   /**
   //    * Sets the Nifti image to display
   //    * @param image
   //    */
   //   private void setImage(NiftiImage image) {
   //      myImage = image;
   //      texture = new NiftiTextureContent (image, voxelator);
   //      myRenderProps.getColorMap ().setContent (texture);
   //   }
   
   /**
    * Sets the normalized slice coordinates to display
    * @param x in [0,1], sets the 'x' slice
    * @param y in [0,1], sets the 'y' slice
    * @param z in [0,1], sets the 'z' slice
    */
   public void setSliceCoordinates(double x, double y, double z) {
      
      if (x < 0) {
         x = 0;
      } else if (x > 1) {
         x = 1;
      }
      
      if (y < 0) {
         y = 0;
      } else if (y > 1) {
         y = 1;
      }
      
      if (z < 0) {
         z = 0;
      } else if (z > 1) {
         z = 1;
      }
      
      setX(x);
      setY(y);
      setZ(z);
   }
   
   /**
    * Sets the normalized slice coordinates to display
    * @param coords slice coordinate of image planes
    * @see NiftiViewer#setSliceCoordinates(double, double, double)
    */
   public void setSliceCoordinates(Vector3d coords) {
      setSliceCoordinates(coords.x, coords.y, coords.z);
   }
   
   protected void updateNetTransform() {
      myNetTransform.set(getPose ());  // rigid component
      myNetTransform.mul(myResidualAffineTransform); // affine adjustment
   }
     
   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      texture.prerender ();
      maybeUpdateRenderObject();
      updateNetTransform();
      
      list.addIfVisible (markers);
      // XXX hack to update all markers
      for (FrameMarker fm : markers) {
         fm.updatePosState ();
      }
      
      if (!surfacesValid) {
         updateSurfaces ();
      }
      for (int i=0; i<sliceSurfaces.length; ++i) {
         sliceSurfaces[i].prerender (list);
      }
   }
   
   /**
    * Computes 3D position of normalized coordinates, prior to additional image
    * transform
    * 
    * @param x [0,1]
    * @param y [0,1]
    * @param z [0,1]
    * @param out position
    */
   protected Point3d getPosition(double x, double y, double z, Point3d out) {

      // compute lowerbound slice number
      int nslices = myImage.getNumSlices ();

      double Z = z*nslices - 0.5;
      int Zlow = (int)Math.floor(Z);

      // slice to voxel
      out.set(x*myImage.getNumCols ()-0.5, y*myImage.getNumRows ()-0.5, z*myImage.getNumSlices ()-0.5);
      AffineTransform3d trans = myImage.getVoxelTransform (myImageSpace);
      // voxel to location
      out.transform (trans);
      return out;
   }
   
   protected PolygonalMesh[] buildSurfaces() {
      
      PolygonalMesh[] meshes = new PolygonalMesh[3];
      PolygonalMesh xyPlane = new PolygonalMesh();
      PolygonalMesh xzPlane = new PolygonalMesh();
      PolygonalMesh yzPlane = new PolygonalMesh();
      meshes[XY_IDX] = xyPlane;
      meshes[XZ_IDX] = xzPlane;
      meshes[YZ_IDX] = yzPlane;
      
      float x = (float)getX();
      float y = (float)getY();
      float z = (float)getZ();
      
      Point3d pos = new Point3d();
      
      // xy-slice
      ArrayList<Vector3d> xytextures = new ArrayList<>();
      Point2d[] texcoords = texture.getTextureCoordinates (NiftiTextureContent.COL_ROW_PLANE);
      float[][] coords = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
      for (int i=0; i<4; ++i) {
         getPosition(coords[i][0], coords[i][1], z, pos);
         xyPlane.addVertex (pos);
         xytextures.add (new Vector3d(texcoords[i].x, texcoords[i].y, 0));
      }
      // triangles
      xyPlane.addFace (new int[] {0, 1, 2});
      xyPlane.addFace (new int[] {0, 2, 3});
      xyPlane.setTextureCoords (xytextures, xyPlane.createVertexIndices ());
      
      // xz-slice
      ArrayList<Vector3d> xztextures = new ArrayList<>();
      texcoords = texture.getTextureCoordinates (NiftiTextureContent.COL_SLICE_PLANE);
      for (int i=0; i<4; ++i) {
         getPosition (coords[i][0], y, coords[i][1], pos);
         xzPlane.addVertex (pos);
         xztextures.add (new Vector3d(texcoords[i].x, texcoords[i].y, 0));
      }
      // triangles
      xzPlane.addFace (new int[] {0, 1, 2});
      xzPlane.addFace (new int[] {0, 2, 3});
      xzPlane.setTextureCoords (xztextures, xzPlane.createVertexIndices ());
      
      // yz-slice
      ArrayList<Vector3d> yztextures = new ArrayList<>();
      texcoords = texture.getTextureCoordinates (NiftiTextureContent.ROW_SLICE_PLANE);
      for (int i=0; i<4; ++i) {
         getPosition (x, coords[i][0], coords[i][1], pos);
         yzPlane.addVertex (pos);
         yztextures.add (new Vector3d(texcoords[i].x, texcoords[i].y, 0));
      }
      // triangles
      yzPlane.addFace (new int[] {0, 1, 2});
      yzPlane.addFace (new int[] {0, 2, 3});
      yzPlane.setTextureCoords (yztextures, yzPlane.createVertexIndices ());
      
      return meshes;
   }
   
   protected void updateSurfaces() {
      
      float x = (float)getX();
      float y = (float)getY();
      float z = (float)getZ();
      
      PolygonalMesh xyPlane = sliceSurfaces[XY_IDX];
      PolygonalMesh xzPlane = sliceSurfaces[XZ_IDX];
      PolygonalMesh yzPlane = sliceSurfaces[YZ_IDX];
      
      Point3d pos = new Point3d();
      
      // xy-slice
      float[][] coords = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
      int pidx = 0;
      for (int i=0; i<4; ++i) {
         getPosition(coords[i][0], coords[i][1], z, pos);
         xyPlane.getVertex (pidx++).setPosition (pos);
      }
      
      // xz-slice
      pidx = 0;
      for (int i=0; i<4; ++i) {
         getPosition (coords[i][0], y, coords[i][1], pos);
         xzPlane.getVertex (pidx++).setPosition (pos);
      }
      
      // yz-slice
      pidx = 0;
      for (int i=0; i<4; ++i) {
         getPosition (x, coords[i][0], coords[i][1], pos);
         yzPlane.getVertex (pidx++).setPosition (pos);
      }
      
      xyPlane.notifyVertexPositionsModified ();
      xzPlane.notifyVertexPositionsModified ();
      yzPlane.notifyVertexPositionsModified ();
      
      surfacesValid = true;
   }
   
   protected RenderObject buildRenderObject() {
      RenderObject robj = new RenderObject();

      Point3d pos = new Point3d();

      // box coordinates

      robj.vertex (getPosition(0,0,0, pos));
      robj.vertex (getPosition(0,1,0,pos));
      robj.vertex (getPosition(1,1,0,pos));
      robj.vertex (getPosition(1,0,0,pos));
      robj.vertex (getPosition(0,0,1,pos));
      robj.vertex (getPosition(0,1,1,pos));
      robj.vertex (getPosition(1,1,1,pos));
      robj.vertex (getPosition(1,0,1,pos));

      final int[][] edges = {{0,1},{1,2},{2,3},{3,0},{0,4},{1,5},
                             {4,5},{5,6},{6,7},{7,4},{2,6},{3,7}};

      for (int[] edge : edges) {
         robj.addLine (edge[0], edge[1]);
      }

      return robj;
   }
   
   private boolean maybeUpdateRenderObject() {
      
      boolean modified = false;
      if (robj == null) {
         robj = buildRenderObject ();
         modified = true;
      } else if (!robjValid) {
         Point3d pos = new Point3d();
         robj.setPosition (0, getPosition(0,0,0,pos));
         robj.setPosition (1, getPosition(0,1,0,pos));
         robj.setPosition (2, getPosition(1,1,0,pos));
         robj.setPosition (3, getPosition(1,0,0,pos));
         robj.setPosition (4, getPosition(0,0,1,pos));
         robj.setPosition (5, getPosition(0,1,1,pos));
         robj.setPosition (6, getPosition(1,1,1,pos));
         robj.setPosition (7, getPosition(1,0,1,pos));
         
         modified = true;
      }
      robjValid = true;
      return modified;
   }
   
   /**
    * Sets a 3D transform to apply to the image
    * @param trans transform
    */
   public void setTransform(AffineTransform3dBase trans) {
      if (trans instanceof AffineTransform3d) {
         setTransform((AffineTransform3d)trans);
      } else {
         setTransform(new AffineTransform3d(trans));
      }
   }
   
   /**
    * Sets a 3D transform to apply to the image.  Required for property.
    * 
    * @param trans 3D transform
    */
   public void setTransform(AffineTransform3d trans) {
      // transform geometry by net difference
      // T_diff = T_new T_old^(-1)
      AffineTransform3d diff = new AffineTransform3d(trans);
      updateNetTransform ();
      diff.mulInverse (myNetTransform);
      transformGeometry (diff);
   }
   
   /**
    * @return the 3D transform applied to the image.  By default,
    * this is the voxel-to-world transform
    */
   public AffineTransform3d getTransform() {
      updateNetTransform ();
      return myNetTransform;
   }
   
   public ImageSpace getImageSpace() {
      return myImageSpace;
   }
   
   public XFormCode getImageSpaceDesc() {
      return myImage.getVoxelTransformCode(myImageSpace);
   }
   
   public void setImageSpace(ImageSpace space) {
      if (space != myImageSpace) {
         myImageSpace = space;
         surfacesValid = false;
         robjValid = false;
      }
   }
   
   @Override
   public synchronized void render(Renderer renderer, int flags) {
      
      RenderProps rprops = getRenderProps();
      
      renderer.pushModelMatrix ();
      renderer.setModelMatrix (myNetTransform);
     
      if (drawBox) {
         // draw box
         Shading savedShading = renderer.setShading (Shading.NONE);
         renderer.setLineColoring (rprops, isSelected());
         renderer.drawLines (robj, 0);
         renderer.setShading (savedShading);
      }
      
      ColorMapProps oldColorMap = renderer.setColorMap (rprops.getColorMap ());
      FaceStyle oldFaceStyle = renderer.setFaceStyle (FaceStyle.FRONT_AND_BACK);
      Shading oldShading = renderer.setShading (rprops.getShading ());
      
      if (!renderer.isSelecting()) {
         renderer.setFaceColoring (rprops, isSelected());
      }      
      
      if (!surfacesValid) {
         updateSurfaces ();
      }

      for (int i=0; i<3; i++) {
         if (drawSlice[i]) {      
            sliceSurfaces[i].render (renderer, myRenderProps, 0);
         }
      }
      
      renderer.setShading (oldShading);
      renderer.setFaceStyle (oldFaceStyle);
      renderer.setColorMap (oldColorMap);
      
      renderer.popModelMatrix ();
   }
   
   /**
    * @return the current voxel generator
    */
   public NiftiPixelGenerator getPixelGenerator() {
      return myVoxelator;
   }
   
   public void setPixelGenerator(NiftiPixelGenerator voxelator) {
      this.myVoxelator = voxelator;
      texture.setPixelGenerator(voxelator);
   }
   
   @Override
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      super.updateBounds(pmin, pmax);
      maybeUpdateRenderObject();
      updateNetTransform();
      
      // update from corners
      Point3d p = new Point3d();
      for (int x=0; x<2; ++x) {
         for (int y=0; y<2; ++y) {
            for (int z=0; z<2; ++z) {
               getPosition (x, y, z, p);
               p.transform (myNetTransform);
               p.updateBounds (pmin, pmax);         
            }
         }
      }
      
   }
   
   @Override
   public boolean isSelectable() {
      return true;
   }
   
   /**
    * @return the current normalized 'x' coordinate
    */
   public double getX() {
      return texture.getX ();
   }
   
   /**
    * Sets the current normalized 'x' coordinate
    * @param x x-coordinate for yz=plane
    */
   public void setX(double x) {
      texture.setX (x);
      robjValid = false;
      surfacesValid = false;
   }
   
   /**
    * 
    * @return the current normalized 'y' coordinate
    */
   public double getY() {
      return texture.getY ();
   }
   
   /**
    * Sets the current normalized 'y' coordinate
    * @param y y-coordinate for xz-plane
    */
   public void setY(double y) {
      texture.setY (y);
      robjValid = false;
      surfacesValid = false;
   }
   
   /**
    * @return the current normalized 'z' coordinate
    */
   public double getZ() {
      return texture.getZ ();
   }
   
   /**
    * Sets the current normalized 'z' coordinate
    * @param z z-coordinate for xy-plane
    */
   public void setZ(double z) {
      texture.setZ (z);
      robjValid = false;
      surfacesValid = false;
   }
   
   /**
    * @return the current time index
    */
   public int getTimeIndex() {
      return texture.getTime ();
   }
   
   /**
    * Sets the current time index
    * 
    * @param idx time index
    */
   public void setTimeIndex(int idx) {
      if (idx < 0) {
         idx = 0;
      } else if (idx > myImage.getNumTimes()) {
         idx = myImage.getNumTimes()-1;
      }
      texture.setTime (idx);
   }
   
   /**
    * @return the number of time indices available in the Nifti image
    */
   public IntegerInterval getTimeIndexRange() {
      return new IntegerInterval(0, myImage.getNumTimes()-1);
   }
   
   /**
    * @return whether or not to draw the YZ plane (at the current 'x' coordinate)
    */
   public boolean getDrawYZ() {
      return drawSlice[YZ_IDX];
   }
   
   /**
    * Sets whether or not to draw the YZ plane (at the current 'x' coordinate)
    * @param set enable yz-plane
    */
   public void setDrawYZ(boolean set) {
      drawSlice[YZ_IDX] = set;
   }
   
   /**
    * @return whether or not to draw the XZ plane (at the current 'y' coordinate)
    */
   public boolean getDrawXZ() {
      return drawSlice[XZ_IDX];
   }

   /**
    * Sets whether or not to draw the XZ plane (at the current 'y' coordinate)
    * @param set xz-plane
    */
   public void setDrawXZ(boolean set) {
      drawSlice[XZ_IDX] = set;
   }
   
   /**
    * @return whether or not to draw the XY plane (at the current 'z' coordinate)
    */
   public boolean getDrawXY() {
      return drawSlice[XY_IDX];
   }
   
   /**
    * Sets whether or not to draw the XY plane (at the current 'z' coordinate)
    * @param set enable xy-plane
    */
   public void setDrawXY(boolean set) {
      drawSlice[XY_IDX] = set;
   }
   
   /**
    * @return whether or not to draw the 3D bounding box outline
    */
   public boolean getDrawBox() {
      return drawBox;
   }
   
   /**
    * Sets whether or not to draw the 3D bounding box outline
    * @param set enable bounding box
    */
   public void setDrawBox(boolean set) {
      drawBox = set;
   }
   
   @Override
   public void transformGeometry(AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   @Override
   public void transformGeometry(
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      // XXX double-check order here
      
      // update pose
      super.transformGeometry (gtr, context, flags);  // transform frame
      
      // remove rigid pose component
      AffineTransform3d trans = gtr.computeLocalAffineTransform (getPose(), null);
      myResidualAffineTransform.mul (trans, myResidualAffineTransform);
      

   }

   @Override
   public void addTransformableDependencies(
      TransformGeometryContext context, int flags) {
      super.addTransformableDependencies (context, flags);
      //      for (Point mkr : markers) {
      //         context.add (mkr);
      //      }
   }

   public PointList<FrameMarker> getMarkers() {
      return markers;
   }

   @Override
   public FrameMarker createMarker (Line ray) {

      // transform ray according to object's transform
      // since surfaces are stored in relative coords
      updateNetTransform ();
      if (!myNetTransform.isIdentity ()) {
         ray = new Line(ray);
         ray.transform (myNetTransform);
      }

      if (!surfacesValid) {
         updateSurfaces ();
      }

      double nearestDistance = Double.POSITIVE_INFINITY;
      Point3d nearest = new Point3d();

      // intersect with visible meshes
      for (int i=0; i < sliceSurfaces.length; ++i) {
         if (drawSlice[i]) {
            Point3d pos = BVFeatureQuery.nearestPointAlongRay (
               sliceSurfaces[i], ray.getOrigin(), ray.getDirection());
            if (pos != null) {
               double d = pos.distance(ray.getOrigin());
               if (d < nearestDistance) {
                  nearestDistance = d;
                  nearest.set (pos);
               }
            }        
         }
      }

      if (nearestDistance != Double.POSITIVE_INFINITY) {
         FrameMarker mkr = createMarker(nearest);
         return mkr;
      }

      return null;
   }

   @Override
   public FrameMarker createMarker(Point3d pnt) {
      FrameMarker marker = new FrameMarker();
      marker.setFrame (this);
      marker.setWorldLocation (pnt);
      return marker;
   }
   
   @Override
   public boolean canAddMarker (Marker mkr) {
      if (mkr instanceof FrameMarker) {
         return true;
      }
      return false;
   }

   @Override
   public boolean addMarker (Marker mkr) {
      if (mkr instanceof FrameMarker) {
         markers.add ((FrameMarker)mkr);
         return true;
      }
      return false;
   }

   @Override
   public PolygonalMesh getSurfaceMesh () {
      PolygonalMesh surface = new PolygonalMesh();
      for (int i = 0; i < drawSlice.length; ++i) {
         if (drawSlice[i]) {
            surface.addMesh (sliceSurfaces[i]);
         }
      }
      return surface;
   }

   @Override
   public int numSurfaceMeshes () {
      int nsurfaces = 0;
      for (int i = 0; i < drawSlice.length; ++i) {
         if (drawSlice[i]) {
            ++nsurfaces;
         }
      }
      return nsurfaces;
   }

   @Override
   public PolygonalMesh[] getSurfaceMeshes () {
      PolygonalMesh[] surfaces = new PolygonalMesh[numSurfaceMeshes ()];
      int idx = 0;
      for (int i = 0; i < drawSlice.length; ++i) {
         if (drawSlice[i]) {
            surfaces[idx++] = sliceSurfaces[i];
         }
      }
      return surfaces;
   }

   /*
    * CompositeComponent 
    */
   protected ComponentListImpl<ModelComponent> myComponents =
   new ComponentListImpl<ModelComponent>(ModelComponent.class, this);

   private NavpanelDisplay myDisplayMode = NavpanelDisplay.NORMAL;

   // ========== Begin ModelComponent overrides ==========

   public Iterator<? extends HierarchyNode> getChildren() {
      return myComponents.iterator();
   }

   public Iterator<ModelComponent> iterator() {
      return myComponents.iterator();
   }

   public boolean hasChildren() {
      // hasChildren() might be called in the super() constructor, from the
      // property progagation code, before myComponents has been instantiated
      return myComponents != null && myComponents.size() > 0;
   }

   public boolean hasState() {
      return true;
   }

   // ========== End ModelComponent overrides ==========

   // ========== Begin CompositeComponent implementation ==========

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (String nameOrNumber) {
      return myComponents.get (nameOrNumber);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent get (int idx) {
      return myComponents.get (idx);
   } 

   /**
    * {@inheritDoc}
    */
   public ModelComponent getByNumber (int num) {
      return myComponents.getByNumber (num);
   }

   /**
    * {@inheritDoc}
    */
   public int numComponents() {
      return myComponents.size();
   }

   /**
    * {@inheritDoc}
    */
   public int indexOf (ModelComponent comp) {
      return myComponents.indexOf (comp);
   }

   /**
    * {@inheritDoc}
    */
   public ModelComponent findComponent (String path) {
      return ComponentUtils.findComponent (this, path);
   }

   /**
    * {@inheritDoc}
    */
   public int getNumberLimit() {
      return myComponents.getNumberLimit();
   }

   /**
    * {@inheritDoc}
    */
   public NavpanelDisplay getNavpanelDisplay() {
      return myDisplayMode;
   }

   /**
    * Sets the display mode for this component. This controls
    * how the component is displayed in a navigation panel. The default
    * setting is <code>NORMAL</code>.
    *
    * @param mode new display mode
    */
   public void setDisplayMode (NavpanelDisplay mode) {
      myDisplayMode = mode;
   }

   /**
    * {@inheritDoc}
    */
   public void componentChanged (ComponentChangeEvent e) {
      myComponents.componentChanged (e);
      notifyParentOfChange (e);
   }

   /**
    * {@inheritDoc}
    */
   public void updateNameMap (
      String newName, String oldName, ModelComponent comp) {
      myComponents.updateNameMap (newName, oldName, comp);
   }

   /**
    * {@inheritDoc}
    */
   public boolean hierarchyContainsReferences() {
      return false;
   }

   // ========== End CompositeComponent implementation ==========

   protected void add (ModelComponent comp) {
      myComponents.add (comp);
   }

   protected boolean remove (ModelComponent comp) {
      return myComponents.remove (comp);
   }

   protected void removeAll() {
      myComponents.removeAll();
   }

   protected void notifyStructureChanged (Object comp) {
      notifyStructureChanged (comp, /*stateIsChanged=*/true);
   }

   protected void notifyStructureChanged (Object comp, boolean stateIsChanged) {
      if (comp instanceof CompositeComponent) {
         notifyParentOfChange (
            new StructureChangeEvent ((CompositeComponent)comp,stateIsChanged));
      }
      else if (!stateIsChanged) {
         notifyParentOfChange (
            StructureChangeEvent.defaultStateNotChangedEvent);
      }
      else {
         notifyParentOfChange (
            StructureChangeEvent.defaultEvent);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
   throws IOException {

      rtok.nextToken();
      if (ScanWriteUtils.scanProperty (rtok, this, tokens)) {
         return true;
      }
      else if (myComponents.scanAndStoreComponentByName (rtok, tokens)) {
         return true;
      }
      rtok.pushBack();
      return false;
   }

   protected boolean postscanItem (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (myComponents.postscanComponent (tokens, ancestor)) {
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   @Override
   public void scan (
      ReaderTokenizer rtok, Object ref) throws IOException {

      myComponents.scanBegin();
      super.scan (rtok, ref);
   }

   @Override
   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (hierarchyContainsReferences()) {
         ancestor = this;
      }
      super.postscan (tokens, ancestor);
      myComponents.scanEnd();
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      myComponents.writeComponentsByName (pw, fmt, ancestor);
   }

   public NiftiViewer copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      NiftiViewer ccomp =
         (NiftiViewer)super.copy (flags, copyMap);

      ccomp.myComponents =
         new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      ccomp.myDisplayMode = myDisplayMode;

      return ccomp;
   }
}
