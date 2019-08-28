/**
 * Copyright (c) 2019, by the Authors: Antonio Sanchez (UBC)
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
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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
import maspack.image.dicom.DicomImage;
import maspack.image.dicom.DicomPixelInterpolator;
import maspack.image.dicom.DicomReader;
import maspack.image.dicom.DicomSlice;
import maspack.image.dicom.DicomTextureContent;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Line;
import maspack.matrix.Matrix3d;
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
import maspack.util.StringRange;

/**
 * Non-regular stacks of slices
 */
public class DicomViewer extends Frame 
   implements CompositeComponent, RenderableComponent, TransformableGeometry, 
   IsMarkable, HasSurfaceMesh {

   DicomImage myImage;
   DicomTextureContent texture;

   static int XY_IDX = 0;
   static int XZ_IDX = 1;
   static int YZ_IDX = 2;

   // position of planes
   Vector3d coord;

   public static PropertyList myProps = new PropertyList(DicomViewer.class, Frame.class);

   static {
      myProps.add("x * *", "coordinate for YZ plane", 0.5, "[0,1]");
      myProps.add("y * *", "coordinate for XZ plane", 0.5, "[0,1]");
      myProps.add("z * *", "coordinate for XY plane", 0.5, "[0,1]");
      myProps.add ("snap * *", "snap to slice/row/col", false);
      myProps.add("timeIndex * *", "time coordinate", 0);
      myProps.add("transform * *", "additional transform to apply to image",
         new AffineTransform3d(RigidTransform3d.IDENTITY) );
      myProps.add("drawYZ * *", "draw XY plane", true);
      myProps.add("drawXZ * *", "draw XY plane", true);
      myProps.add("drawXY * *", "draw XY plane", true);
      myProps.add("drawBox * *", "draw image box", true);
      myProps.addReadOnly("pixelInterpolator", "pixel converter");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public static RenderProps createDefaultRenderProps() {
      return new LineRenderProps();
   }

   // decompose transform into rigid frame and affine
   //     T_net = T_frame T_affine
   AffineTransform3d myResidualAffineTransform;
   AffineTransform3d myNetTransform;

   PointList<FrameMarker> markers;

   boolean drawBox;
   boolean snap;
   boolean drawSlice[];
   PolygonalMesh sliceSurfaces[];

   RenderObject robj;
   boolean robjValid;
   boolean surfacesValid;

   /**
    * Creates a new viewer widget, with supplied name and DICOM image
    * 
    * @param name widget name
    * @param image DICOM image data
    */
   public DicomViewer(String name, DicomImage image) {
      super();
      init(name, image);
   }

   /**
    * Creates a new viewer widget, with supplied DICOM image.  The
    * name of the component becomes the image name
    * 
    * @param image DICOM image data
    */
   public DicomViewer(DicomImage image) {
      this(image.getTitle(), image);
   }

   private void init(String name, DicomImage image) {
      setName(ModelComponentBase.makeValidName(name));

      markers = new PointList<FrameMarker>(FrameMarker.class, "markers");
      add(markers);

      myRenderProps = createRenderProps();
      robj = null;
      robjValid = false;

      myResidualAffineTransform = new AffineTransform3d(RigidTransform3d.IDENTITY);
      myNetTransform = new AffineTransform3d(RigidTransform3d.IDENTITY);
      drawBox = true;
      drawSlice = new boolean[]{true, true, true};

      snap = false;

      coord = new Vector3d();
      setImage(image);
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
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    * @param filePattern pattern for accepting/rejecting contained files.  The pattern is applied
    * to the absolute file names of all files contained in the imagePath
    * @param checkSubdirs recursively check sub-folders for further DICOM files
    */
   public DicomViewer(String name, String imagePath, Pattern filePattern, boolean checkSubdirs) {
      DicomImage im = null;
      try {
         DicomReader rs = new DicomReader();
         im = rs.read(im, imagePath, filePattern, checkSubdirs);
      } catch (IOException ioe) {
         throw new RuntimeException(ioe);
      } catch(Exception e) {
         throw new RuntimeException("Failed to read dicom images in " + imagePath, e);
      }
      if (im == null) {
         throw new RuntimeException("No image data loaded");
      }
      init(name, im);
   }

   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param files list of DICOM files to load
    */
   public DicomViewer(String name, List<File> files) {
      DicomImage im = null;
      try {
         DicomReader rs = new DicomReader();
         im = rs.read(im, files);
      } catch(Exception e) {
         throw new RuntimeException("Failed to read dicom images", e);
      }
      if (im == null) {
         throw new RuntimeException("No image data loaded");
      }
      init(name, im);
   }

   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    * @param filePattern pattern for accepting/rejecting contained files.  The pattern is applied
    * to the absolute file names of all files contained in the imagePath
    */
   public DicomViewer(String name, String imagePath, Pattern filePattern) {
      this(name, imagePath, filePattern, false);
   }

   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    */
   public DicomViewer(String name, String imagePath) {
      this(name, imagePath, null);
   }

   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    * @param filePattern pattern for accepting/rejecting contained files.  The pattern is applied
    * to the absolute file names of all files contained in the imagePath
    * @param checkSubdirs recursively check sub-folders for further DICOM files
    */
   public DicomViewer(String name, File imagePath, Pattern filePattern, boolean checkSubdirs) {
      this(name, imagePath.getAbsolutePath(), filePattern, checkSubdirs);
   }

   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    * @param filePattern pattern for accepting/rejecting contained files.  The pattern is applied
    * to the absolute file names of all files contained in the imagePath
    */
   public DicomViewer(String name, File imagePath, Pattern filePattern) {
      this(name, imagePath, filePattern, false);
   }

   /**
    * Reads DICOM files into a 3D (+time) image
    * @param name name of the viewer component
    * @param imagePath directory containing DICOM files
    */
   public DicomViewer(String name, File imagePath) {
      this(name, imagePath, null);
   }

   /**
    * @return the DICOM image being displayed
    */
   public DicomImage getImage() {
      return myImage;
   }

   /**
    * Sets the DICOM image to display
    * @param image
    */
   private void setImage(DicomImage image) {
      myImage = image;
      texture = new DicomTextureContent (image);
      myRenderProps.getColorMap ().setContent (texture);
   }

   /**
    * @return the number of interpolation windows available in the DICOM image
    */
   public int numWindows() {
      return texture.getWindowConverter ().numWindows ();
   }

   /**
    * @return the names of all possible interpolation windows available in the DICOM image
    */
   public String[] getWindowNames() {
      return texture.getWindowConverter ().getWindowNames ();
   }

   /**
    * Sets the current interpolation window to use, based on a preset name
    * @param presetName name of the interpolation window
    */
   public void setWindow(String presetName) {
      texture.getWindowConverter ().setWindow (presetName);
   }

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
    * 
    * @param coords normalized slice coordinates
    * @see DicomViewer#setSliceCoordinates(double, double, double)
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
    * XXX broken
    * Computes an image-to-world transform for a given normalized z coordinate
    * @param z normalized z position
    * @return T such that T[x y z]' gives the world coordinates
    */
   protected AffineTransform3d getZTransform(double z) {

      // compute lowerbound slice number
      int nslices = myImage.getNumSlices ();
      double Z = z*nslices - 0.5;
      int Zlow = (int)Math.floor(Z);

      RigidTransform3d t1;   // lower slice transform
      RigidTransform3d t2;   // upper slice transform
      Matrix3d S1;           // lower slice scale
      Matrix3d S2;           // upper slice scale
      Vector3d h1;           // lower corner offset from first pixel
      Vector3d h2;           // upper corner offset from first pixel

      double s = Z - Zlow;   // interpolation factor between low/high

      if (Zlow <= -1) {
         /* bottom */
         DicomSlice slice = myImage.getSlice (0);
         double thickness = slice.getThickness ();

         t2 = slice.getImagePose ();

         // shift down one slice thickness
         t1 = new RigidTransform3d(t2);
         t1.p.add (-thickness*t2.R.m02, -thickness*t2.R.m12, -thickness*t2.R.m22);

         double dx = slice.getColSpacing ();
         double dy = slice.getRowSpacing ();
         int nx = slice.getNumCols ();
         int ny = slice.getNumRows ();

         h1 = new Vector3d(-dx/2, -dy/2, 0);
         h2 = h1;

         S1 = new Matrix3d();
         S1.m00 = nx * dx;
         S1.m11 = ny * dy;
         S2 = S1;

      } else if (Zlow >= nslices - 1) {

         /* top */
         DicomSlice slice = myImage.getSlice (nslices-1);
         double thickness = slice.getThickness ();

         t1 = slice.getImagePose ();

         // shift up one slice thickness
         t2 = new RigidTransform3d(t1);
         t2.p.add (thickness*t1.R.m02, thickness*t1.R.m12, thickness*t1.R.m22);

         double dx = slice.getColSpacing ();
         double dy = slice.getRowSpacing ();
         int nx = slice.getNumCols ();
         int ny = slice.getNumRows ();

         h1 = new Vector3d(-dx/2, -dy/2, 0);
         h2 = h1;

         S1 = new Matrix3d();
         S1.m00 = nx * dx;
         S1.m11 = ny * dy;
         S2 = S1;

      } else {

         /* middle */
         DicomSlice slice1 = myImage.getSlice (Zlow);
         DicomSlice slice2 = myImage.getSlice (Zlow + 1);

         t1 = slice1.getImagePose ();
         t2 = slice2.getImagePose ();

         double dx1 = slice1.getColSpacing ();
         double dy1 = slice1.getRowSpacing ();
         int nx1 = slice1.getNumCols ();
         int ny1 = slice1.getNumRows ();
         h1 = new Vector3d(-dx1/2, -dy1/2, 0);
         S1 = new Matrix3d();
         S1.m00 = dx1*nx1;
         S1.m11 = dy1*ny1;

         double dx2 = slice2.getColSpacing ();
         double dy2 = slice2.getRowSpacing ();
         int nx2 = slice2.getNumCols ();
         int ny2 = slice2.getNumRows ();
         h2 = new Vector3d(-dx2/2, -dy2/2, 0);
         S2 = new Matrix3d();
         S2.m00 = dx2*nx2;
         S2.m11 = dy2*ny2;
      }

      //       
      // [T]x =  [ (1-s)R1S1 + sR2S2 ] x + (1-s)[R1h1 + t1] + s[R2h2 + t2]

      AffineTransform3d T = new AffineTransform3d();
      h1.transform (t1);
      h2.transform (t2);
      T.p.interpolate (h1, s, h2);

      S1.mul (t1.R, S1);
      S1.scale (1-s);
      S2.mul (t2.R, S2);
      S2.scale (s);
      T.A.add (S1, S2);

      return T;
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
   protected void getPosition(double x, double y, double z, Point3d out) {

      // compute lowerbound slice number
      int nslices = myImage.getNumSlices ();

      double Z = z*nslices - 0.5;
      int Zlow = (int)Math.floor(Z);

      if (Zlow <= -1) {

         // lowest voxel
         DicomSlice slice = myImage.getSlice (0);
         RigidTransform3d strans = slice.getImagePose ();

         double nx = slice.getNumCols ();
         double dx = slice.getColSpacing ();
         double ny = slice.getNumRows ();
         double dy = slice.getRowSpacing ();
         double dz = slice.getThickness ();

         out.set(x*dx*nx - dx/2, y*dy*ny - dy/2, Zlow*dz);
         out.transform (strans);


      } else if (Zlow >= nslices-1) {
         // highest voxel
         DicomSlice slice = myImage.getSlice (nslices-1);
         RigidTransform3d strans = slice.getImagePose ();

         double nx = slice.getNumCols ();
         double dx = slice.getColSpacing ();
         double ny = slice.getNumRows ();
         double dy = slice.getRowSpacing ();
         double dz = slice.getThickness ();

         out.set(x*dx*nx - dx/2, y*dy*ny - dy/2, (Z-Zlow)*dz);
         out.transform (strans);
      } else {
         // interpolate between two slices
         DicomSlice slice1 = myImage.getSlice (Zlow);
         RigidTransform3d t1 = slice1.getImagePose ();

         DicomSlice slice2 = myImage.getSlice (Zlow+1);
         RigidTransform3d t2 = slice2.getImagePose ();

         double s = Z-Zlow;  // range [0, 1]

         // linearly interpolate between two images
         int nx1 = slice1.getNumCols ();
         int ny1 = slice1.getNumRows ();
         double dx1 = slice1.getColSpacing ();
         double dy1 = slice1.getRowSpacing ();
         Point3d p1 = new Point3d(x*dx1*nx1 - dx1/2, y*dy1*ny1 - dy1/2, 0);
         p1.transform(t1);

         int nx2 = slice2.getNumCols ();
         int ny2 = slice2.getNumRows ();
         double dx2 = slice2.getColSpacing ();
         double dy2 = slice2.getRowSpacing ();
         Point3d p2 = new Point3d(x*dx2*nx2 - dx2/2, y*dy2*ny2 - dy2/2, 0);
         p2.transform (t2);

         out.interpolate (p1, s, p2);

      }

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

      int nslices = myImage.getNumSlices ();
      double dz = 1.0/nslices;

      // xy-slice
      ArrayList<Vector3d> xytextures = new ArrayList<>();
      Point2d[] texcoords = texture.getTextureCoordinates (DicomTextureContent.COL_ROW_PLANE);
      float[][] xycoords = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
      for (int i=0; i<4; ++i) {
         getPosition(xycoords[i][0], xycoords[i][1], z, pos);
         xyPlane.addVertex (pos);
         xytextures.add (new Vector3d(texcoords[i].x, texcoords[i].y, 0));
      }
      // triangles
      xyPlane.addFace (new int[] {0, 1, 2});
      xyPlane.addFace (new int[] {0, 2, 3});
      xyPlane.setTextureCoords (xytextures, xyPlane.createVertexIndices ());

      // xz-slice
      ArrayList<Vector3d> xztextures = new ArrayList<>();
      texcoords = texture.getTextureCoordinates (DicomTextureContent.COL_SLICE_PLANE);
      // bottom-left
      getPosition (0, y, 0, pos);
      xzPlane.addVertex (pos);
      xztextures.add (new Vector3d(texcoords[0].x, texcoords[0].y, 0));
      // up slices
      Point2d tcoord = new Point2d();
      for (int i=0; i<nslices; ++i) {
         getPosition (0, y, (i+0.5)*dz, pos);
         xzPlane.addVertex (pos);
         tcoord.interpolate (texcoords[0], (i+0.5)/nslices, texcoords[1]);
         xztextures.add (new Vector3d(tcoord.x, tcoord.y, 0));
      }
      // top
      getPosition(0, y, 1, pos);
      xzPlane.addVertex (pos);
      xztextures.add (new Vector3d(texcoords[1].x, texcoords[1].y, 0));
      // across
      getPosition (1, y, 1, pos);
      xzPlane.addVertex (pos);
      xztextures.add (new Vector3d(texcoords[2].x, texcoords[2].y, 0));
      // down slices
      for (int i=nslices; i-->0;) {
         getPosition(1, y, (i+0.5)*dz, pos);
         xzPlane.addVertex (pos);
         tcoord.interpolate (texcoords[3], (i+0.5)/nslices, texcoords[2]);
         xztextures.add (new Vector3d(tcoord.x, tcoord.y, 0));
      }
      // bottom
      getPosition (1, y, 0, pos);
      xzPlane.addVertex (pos);
      xztextures.add (new Vector3d(texcoords[3].x, texcoords[3].y, 0));
      // triangles
      int baseIdx = 0;
      for (int i=0; i <= nslices; ++i) {
         xzPlane.addFace (new int[] {baseIdx + i, baseIdx + i + 1, baseIdx + 2*nslices + 2 - i});
         xzPlane.addFace (new int[] {baseIdx + i, baseIdx + 2*nslices + 2 - i, baseIdx + 2*nslices + 3 - i});
      }
      xzPlane.setTextureCoords (xztextures, xzPlane.createVertexIndices ());

      // yz-slice
      ArrayList<Vector3d> yztextures = new ArrayList<>();
      texcoords = texture.getTextureCoordinates (DicomTextureContent.ROW_SLICE_PLANE);
      // bottom-left
      getPosition (x, 0, 0, pos);
      yzPlane.addVertex (pos);
      yztextures.add (new Vector3d(texcoords[0].x, texcoords[0].y, 0));
      // up slices
      for (int i=0; i<nslices; ++i) {
         getPosition (x, 0, (i+0.5)*dz, pos);
         yzPlane.addVertex (pos);
         tcoord.interpolate (texcoords[0], (i+0.5)/nslices, texcoords[1]);
         yztextures.add (new Vector3d(tcoord.x, tcoord.y, 0));
      }
      // top
      getPosition (x, 0, 1, pos);
      yzPlane.addVertex (pos);
      yztextures.add (new Vector3d(texcoords[1].x, texcoords[1].y, 0));
      // across
      getPosition (x, 1, 1, pos);
      yzPlane.addVertex (pos);
      yztextures.add (new Vector3d(texcoords[2].x, texcoords[2].y, 0));
      // down slices
      for (int i=nslices; i-->0;) {
         getPosition (x, 1, (i+0.5)*dz, pos);
         yzPlane.addVertex (pos);
         tcoord.interpolate (texcoords[3], (i+0.5)/nslices, texcoords[2]);
         yztextures.add (new Vector3d(tcoord.x, tcoord.y, 0));
      }
      // bottom
      getPosition (x, 1, 0, pos);
      yzPlane.addVertex (pos);
      yztextures.add (new Vector3d(texcoords[3].x, texcoords[3].y, 0));
      // triangles
      baseIdx = 0;
      for (int i=0; i <= nslices; ++i) {
         yzPlane.addFace (new int[] {baseIdx + i, baseIdx + i + 1, baseIdx + 2*nslices + 2 - i});
         yzPlane.addFace (new int[] {baseIdx + i, baseIdx + 2*nslices + 2 - i, baseIdx + 2*nslices + 3 - i});
      }
      yzPlane.setTextureCoords (yztextures, yzPlane.createVertexIndices ());

      return meshes;
   }

   protected void updateSurfaces() {
      
      float x = (float)getX();
      float y = (float)getY();
      float z = (float)getZ();

      int nslices = myImage.getNumSlices ();
      double dz = 1.0/nslices;

      PolygonalMesh xyPlane = sliceSurfaces[XY_IDX];
      PolygonalMesh xzPlane = sliceSurfaces[XZ_IDX];
      PolygonalMesh yzPlane = sliceSurfaces[YZ_IDX];

      // xy-slice
      float[][] xycoords = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
      Point3d pos = new Point3d();

      // AffineTransform3d T = getZTransform (z);
      int pidx = 0;
      for (int i=0; i<4; ++i) {
         getPosition( xycoords[i][0], xycoords[i][1], z, pos);
         xyPlane.getVertex (pidx++).setPosition (pos);
      }

      // xz-slice
      pidx = 0;
      getPosition (0, y, 0, pos);
      xzPlane.getVertex (pidx++).setPosition (pos);
      // up slices
      for (int i=0; i<nslices; ++i) {
         double w = (i+0.5)*dz;
         getPosition(0, y, w, pos);
         xzPlane.getVertex (pidx++).setPosition (pos);
      }
      // top
      getPosition(0, y, 1, pos);
      xzPlane.getVertex (pidx++).setPosition (pos);
      // across
      getPosition(1, y, 1, pos);
      xzPlane.getVertex (pidx++).setPosition (pos);
      // down slices
      for (int i=nslices; i-->0;) {
         double w = ((i+0.5)*dz);
         getPosition(1, y, w, pos);
         xzPlane.getVertex (pidx++).setPosition (pos);
      }
      // bottom
      getPosition(1, y, 0, pos);
      xzPlane.getVertex (pidx++).setPosition (pos);

      // yz-slice
      // bottom-left
      pidx = 0;
      getPosition(x, 0, 0, pos);
      yzPlane.getVertex (pidx++).setPosition (pos);
      // up slices
      for (int i=0; i<nslices; ++i) {
         double w = (i+0.5)*dz;
         getPosition(x, 0, w, pos);
         yzPlane.getVertex (pidx++).setPosition (pos);
      }
      // top
      getPosition(x, 0, 1, pos);
      yzPlane.getVertex (pidx++).setPosition (pos);
      // across
      getPosition(x, 1, 1, pos);
      yzPlane.getVertex (pidx++).setPosition (pos);
      // down slices
      for (int i=nslices; i-->0;) {
         double w = (i+0.5)*dz;
         getPosition(x, 1, w, pos);
         yzPlane.getVertex (pidx++).setPosition (pos);
      }
      // bottom
      getPosition(x, 1, 0, pos);
      yzPlane.getVertex (pidx++).setPosition (pos);

      for (PolygonalMesh surface : sliceSurfaces) {
         surface.notifyVertexPositionsModified ();
      }

      surfacesValid = true;
   }

   protected RenderObject buildRenderObject() {
      RenderObject robj = new RenderObject();

      Point3d pos = new Point3d();
      int nslices = myImage.getNumSlices ();
      double dz = 1.0/nslices;

      // box coordinates
      // base
      getPosition(0,0,0, pos);
      int base_vidx = robj.vertex (pos);
      getPosition(0,1,0, pos);
      robj.vertex (pos);
      getPosition(1,1,0, pos);
      robj.vertex (pos);
      getPosition(1,0,0, pos);
      robj.vertex (pos);
      // sides
      for (int i=0; i<nslices; ++i) {
         float w = (float)((i+0.5)*dz);
         getPosition(0,0,w, pos);
         robj.vertex (pos);
         getPosition(0,1,w, pos);
         robj.vertex (pos);
         getPosition(1,1,w, pos);
         robj.vertex (pos);
         getPosition(1,0,w, pos);
         robj.vertex (pos);
      }      
      // top
      getPosition(0,0,1, pos);
      int top_vidx = robj.vertex (pos);
      getPosition(0,1,1,pos);
      robj.vertex (pos);
      getPosition(1,1,1,pos);
      robj.vertex (pos);
      getPosition(1,0,1,pos);
      robj.vertex (pos);
      // lines
      // base
      robj.addLine (base_vidx, base_vidx+1);
      robj.addLine (base_vidx+1, base_vidx+2);
      robj.addLine (base_vidx+2, base_vidx+3);
      robj.addLine (base_vidx+3, base_vidx);
      // sides
      for (int i=0; i<=nslices; ++i) {
         robj.addLine (base_vidx+4*i, base_vidx+4*(i+1));
         robj.addLine (base_vidx+4*i+1, base_vidx+4*(i+1)+1);
         robj.addLine (base_vidx+4*i+2, base_vidx+4*(i+1)+2);
         robj.addLine (base_vidx+4*i+3, base_vidx+4*(i+1)+3);
      }
      // top
      robj.addLine (top_vidx, top_vidx+1);
      robj.addLine (top_vidx+1, top_vidx+2);
      robj.addLine (top_vidx+2, top_vidx+3);
      robj.addLine (top_vidx+3, top_vidx);

      return robj;
   }

   // updates positions of vertices
   protected void updateRenderObject() {

      Point3d pos = new Point3d();
      int nslices = myImage.getNumSlices ();
      double dz = 1.0/nslices;

      // box coordinates
      int pidx = 0;
      getPosition(0,0,0, pos);
      robj.setPosition( pidx++, pos);
      getPosition(0,1,0, pos);
      robj.setPosition( pidx++, pos);
      getPosition(1,1,0, pos);
      robj.setPosition( pidx++, pos);
      getPosition(1,0,0, pos);
      robj.setPosition( pidx++, pos);
      // sides
      for (int i=0; i<nslices; ++i) {
         float w = (float)((i+0.5)*dz);
         getPosition(0,0,w, pos);
         robj.setPosition( pidx++, pos);
         getPosition(0,1,w, pos);
         robj.setPosition( pidx++, pos);
         getPosition(1,1,w, pos);
         robj.setPosition( pidx++, pos);
         getPosition(1,0,w, pos);
         robj.setPosition( pidx++, pos);
      }      
      // top
      getPosition(0,0,1, pos);
      robj.setPosition( pidx++, pos);
      getPosition(0,1,1,pos);
      robj.setPosition( pidx++, pos);
      getPosition(1,1,1,pos);
      robj.setPosition( pidx++, pos);
      getPosition(1,0,1,pos);
      robj.setPosition( pidx++, pos);
      
   }

   protected RenderObject buildRenderObjectOld() {
      RenderObject robj = new RenderObject();

      float x = (float)getX();
      float y = (float)getY();
      float z = (float)getZ();

      Point3d pos = new Point3d();

      int nslices = myImage.getNumSlices ();
      double dz = 1.0/nslices;

      // xy-slice
      Point2d[] texcoords = texture.getTextureCoordinates (DicomTextureContent.COL_ROW_PLANE);
      float[][] xycoords = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
      for (int i=0; i<4; ++i) {
         getPosition(xycoords[i][0], xycoords[i][1], z, pos);
         robj.addPosition (pos);
         robj.addTextureCoord (texcoords[i]);
         robj.addVertex ();
      }
      // triangles
      robj.createTriangleGroup ();
      robj.addTriangle (0, 1, 2);
      robj.addTriangle (0, 2, 3);

      // xz-slice
      texcoords = texture.getTextureCoordinates (DicomTextureContent.COL_SLICE_PLANE);
      // bottom-left
      getPosition (0, y, 0, pos);
      robj.addPosition (pos);
      robj.addTextureCoord (texcoords[0]);
      robj.addVertex ();
      // up slices
      Point2d tcoord = new Point2d();
      for (int i=0; i<nslices; ++i) {
         getPosition (0, y, (i+0.5)*dz, pos);
         robj.addPosition (pos);
         tcoord.interpolate (texcoords[0], (i+0.5)/nslices, texcoords[1]);
         robj.addTextureCoord (tcoord);
         robj.addVertex ();
      }
      // top
      getPosition(0, y, 1, pos);
      robj.addPosition (pos);
      robj.addTextureCoord (texcoords[1]);
      robj.addVertex ();
      // across
      getPosition (1, y, 1, pos);
      robj.addPosition (pos);
      robj.addTextureCoord (texcoords[2]);
      robj.addVertex ();
      // down slices
      for (int i=nslices; i-->0;) {
         getPosition(1, y, (i+0.5)*dz, pos);
         robj.addPosition (pos);
         tcoord.interpolate (texcoords[3], (i+0.5)/nslices, texcoords[2]);
         robj.addTextureCoord (tcoord);
         robj.addVertex ();
      }
      // bottom
      getPosition (1, y, 0, pos);
      robj.addPosition (pos);
      robj.addTextureCoord (texcoords[3]);
      robj.addVertex ();
      // triangles
      int baseIdx = 4;
      robj.createTriangleGroup ();
      for (int i=0; i <= nslices; ++i) {
         robj.addTriangle (baseIdx + i, baseIdx + i + 1, baseIdx + 2*nslices + 2 - i);
         robj.addTriangle (baseIdx + i, baseIdx + 2*nslices + 2 - i, baseIdx + 2*nslices + 3 - i);
      }

      // yz-slice
      texcoords = texture.getTextureCoordinates (DicomTextureContent.ROW_SLICE_PLANE);
      // bottom-left
      getPosition (x, 0, 0, pos);
      robj.addPosition (pos);
      robj.addTextureCoord (texcoords[0]);
      robj.addVertex ();
      // up slices
      for (int i=0; i<nslices; ++i) {
         getPosition (x, 0, (i+0.5)*dz, pos);
         robj.addPosition (pos);
         tcoord.interpolate (texcoords[0], (i+0.5)/nslices, texcoords[1]);
         robj.addTextureCoord (tcoord);
         robj.addVertex ();
      }
      // top
      getPosition (x, 0, 1, pos);
      robj.addPosition (pos);
      robj.addTextureCoord (texcoords[1]);
      robj.addVertex ();
      // across
      getPosition (x, 1, 1, pos);
      robj.addPosition (pos);
      robj.addTextureCoord (texcoords[2]);
      robj.addVertex ();
      // down slices
      for (int i=nslices; i-->0;) {
         getPosition (x, 1, (i+0.5)*dz, pos);
         robj.addPosition (pos);
         tcoord.interpolate (texcoords[3], (i+0.5)/nslices, texcoords[2]);
         robj.addTextureCoord (tcoord);
         robj.addVertex ();
      }
      // bottom
      getPosition (x, 1, 0, pos);
      robj.addPosition (pos);
      robj.addTextureCoord (texcoords[3]);
      robj.addVertex ();
      // triangles
      baseIdx = 8 + 2*nslices;
      robj.createTriangleGroup ();
      for (int i=0; i <= nslices; ++i) {
         robj.addTriangle (baseIdx + i, baseIdx + i + 1, baseIdx + 2*nslices + 2 - i);
         robj.addTriangle (baseIdx + i, baseIdx + 2*nslices + 2 - i, baseIdx + 2*nslices + 3 - i);
      }

      // box coordinates
      // base
      getPosition(0,0,0, pos);
      int base_vidx = robj.vertex (pos);
      getPosition(0,1,0, pos);
      robj.vertex (pos);
      getPosition(1,1,0, pos);
      robj.vertex (pos);
      getPosition(1,0,0, pos);
      robj.vertex (pos);
      // sides
      for (int i=0; i<nslices; ++i) {
         float w = (float)((i+0.5)*dz);
         getPosition(0,0,w, pos);
         robj.vertex (pos);
         getPosition(0,1,w, pos);
         robj.vertex (pos);
         getPosition(1,1,w, pos);
         robj.vertex (pos);
         getPosition(1,0,w, pos);
         robj.vertex (pos);
      }      
      // top
      getPosition(0,0,1, pos);
      int top_vidx = robj.vertex (pos);
      getPosition(0,1,1,pos);
      robj.vertex (pos);
      getPosition(1,1,1,pos);
      robj.vertex (pos);
      getPosition(1,0,1,pos);
      robj.vertex (pos);
      // lines
      // base
      robj.addLine (base_vidx, base_vidx+1);
      robj.addLine (base_vidx+1, base_vidx+2);
      robj.addLine (base_vidx+2, base_vidx+3);
      robj.addLine (base_vidx+3, base_vidx);
      // sides
      for (int i=0; i<=nslices; ++i) {
         robj.addLine (base_vidx+4*i, base_vidx+4*(i+1));
         robj.addLine (base_vidx+4*i+1, base_vidx+4*(i+1)+1);
         robj.addLine (base_vidx+4*i+2, base_vidx+4*(i+1)+2);
         robj.addLine (base_vidx+4*i+3, base_vidx+4*(i+1)+3);
      }
      // top
      robj.addLine (top_vidx, top_vidx+1);
      robj.addLine (top_vidx+1, top_vidx+2);
      robj.addLine (top_vidx+2, top_vidx+3);
      robj.addLine (top_vidx+3, top_vidx);

      return robj;
   }

   // updates positions of vertices
   protected void updateRenderObjectOld() {
      float x = (float)getX();
      float y = (float)getY();
      float z = (float)getZ();

      int nslices = myImage.getNumSlices ();
      double dz = 1.0/nslices;

      // xy-slice
      float[][] xycoords = {{0, 0}, {0, 1}, {1, 1}, {1, 0}};
      Point3d pos = new Point3d();
      // AffineTransform3d T = getZTransform (z);
      int pidx = 0;
      for (int i=0; i<4; ++i) {
         getPosition( xycoords[i][0], xycoords[i][1], z, pos);
         robj.setPosition (pidx++, pos);
      }

      // xz-slice
      getPosition (0, y, 0, pos);
      robj.setPosition (pidx++, pos);

      // up slices
      for (int i=0; i<nslices; ++i) {
         double w = (i+0.5)*dz;
         getPosition(0, y, w, pos);
         robj.setPosition (pidx++, pos);
      }
      // top
      getPosition(0, y, 1, pos);
      robj.setPosition (pidx++, pos);
      // across
      getPosition(1, y, 1, pos);
      robj.setPosition (pidx++, pos);
      // down slices
      for (int i=nslices; i-->0;) {
         double w = ((i+0.5)*dz);
         getPosition(1, y, w, pos);
         robj.setPosition (pidx++, pos);
      }
      // bottom
      getPosition(1, y, 0, pos);
      robj.setPosition (pidx++, pos);

      // yz-slice
      // bottom-left
      getPosition(x, 0, 0, pos);
      robj.setPosition (pidx++, pos);
      // up slices
      for (int i=0; i<nslices; ++i) {
         double w = (i+0.5)*dz;
         getPosition(x, 0, w, pos);
         robj.setPosition (pidx++, pos);
      }
      // top
      getPosition(x, 0, 1, pos);
      robj.setPosition (pidx++, pos);
      // across
      getPosition(x, 1, 1, pos);
      robj.setPosition (pidx++, pos);
      // down slices
      for (int i=nslices; i-->0;) {
         double w = (i+0.5)*dz;
         getPosition(x, 1, w, pos);
         robj.setPosition (pidx++, pos);
      }
      // bottom
      getPosition(x, 1, 0, pos);
      robj.setPosition (pidx++, pos);
   }

   /**
    * Sets a 3D transform to apply to the image
    * 
    * @param trans 3D transform
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
   
   private boolean maybeUpdateRenderObject() {

      boolean modified = false;
      if (robj == null) {
         robj = buildRenderObject ();
         modified = true;
      } else  if (!robjValid) {         
         updateRenderObject();
         modified = true;
      }

      robjValid = true;

      return modified;
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
    * @return the current pixel interpolator
    */
   public DicomPixelInterpolator getPixelInterpolator() {
      return texture.getWindowConverter ();
   }

   /**
    * @return range of valid window names
    */
   public StringRange getWindowRange() {
      return texture.getWindowConverter().getWindowRange();
   }

   @Override
   public void updateBounds(Vector3d pmin, Vector3d pmax) {
      // super.updateBounds(pmin, pmax); ignore frame coordinates
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
      return coord.x;
   }

   /**
    * Sets the current normalized 'x' coordinate
    *
    * @param x normalized 'x' coordinate
    */
   public void setX(double x) {
      if (snap) {
         int nx = myImage.getNumCols ();
         double dx = 1.0/nx;
         x = Math.min(Math.floor(x/dx),nx-1)*dx + dx/2;
      }
      coord.x = x;
      texture.setX (x);
      robjValid = false;
      surfacesValid = false;
   }

   /**
    * 
    * @return the current normalized 'y' coordinate
    */
   public double getY() {
      return coord.y;
   }

   /**
    * Sets the current normalized 'y' coordinate
    * @param y normalized 'y' coordinate
    */
   public void setY(double y) {
      if (snap) {
         int ny = myImage.getNumRows ();
         double dy = 1.0/ny;
         y = Math.min(Math.floor(y/dy),ny-1)*dy + dy/2;
      }
      coord.y = y;
      texture.setY (y);
      robjValid = false;
      surfacesValid = false;
   }

   /**
    * @return the current normalized 'z' coordinate
    */
   public double getZ() {
      return coord.z;
   }

   /**
    * Sets the current normalized 'z' coordinate
    * @param z normalized 'z' coordinate
    */
   public void setZ(double z) {
      if (snap) {
         int nz = myImage.getNumSlices ();
         double dz = 1.0/nz;
         z = Math.min(Math.floor(z/dz), nz-1)*dz + dz/2;
      }
      coord.z = z;
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
    * @return the number of time indices available in the DICOM image
    */
   public IntegerInterval getTimeIndexRange() {
      return new IntegerInterval(0, myImage.getNumTimes()-1);
   }

   public boolean getSnap() {
      return snap;
   }

   public void setSnap(boolean set) {
      snap = set;
      setX (getX());
      setY (getY());
      setZ (getZ());
   }


   /**
    * @return whether or not to draw the YZ plane (at the current 'x' coordinate)
    */
   public boolean getDrawYZ() {
      return drawSlice[YZ_IDX];
   }

   /**
    * Sets whether or not to draw the YZ plane (at the current 'x' coordinate
    * 
    * @param enable if <code>true</code>, enables drawing of YZ plane
    */
   public void setDrawYZ(boolean enable) {
      drawSlice[YZ_IDX] = enable;
   }

   /**
    * @return whether or not to draw the XZ plane (at the current 'y' coordinate)
    */
   public boolean getDrawXZ() {
      return drawSlice[XZ_IDX];
   }

   /**
    * Sets whether or not to draw the XZ plane (at the current 'y' coordinate)
    * 
    * @param enable if <code>true</code>, enables drawing of XZ plane
    */
   public void setDrawXZ(boolean enable) {
      drawSlice[XZ_IDX] = enable;
   }

   /**
    * @return whether or not to draw the XY plane (at the current 'z' coordinate)
    */
   public boolean getDrawXY() {
      return drawSlice[XY_IDX];
   }

   /**
    * Sets whether or not to draw the XY plane (at the current 'z' coordinate)
    * @param enable if <code>true</code>, enables drawing of XY plane
    */
   public void setDrawXY(boolean enable) {
      drawSlice[XY_IDX] = enable;
   }

   /**
    * @return whether or not to draw the 3D bounding box outline
    */
   public boolean getDrawBox() {
      return drawBox;
   }

   /**
    * Sets whether or not to draw the 3D bounding box outline
    * @param enable if <code>true</code>, enables drawing of the 3D bounding box
    */
   public void setDrawBox(boolean enable) {
      drawBox = enable;
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

   public DicomViewer copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {

      DicomViewer ccomp =
         (DicomViewer)super.copy (flags, copyMap);

      ccomp.myComponents =
         new ComponentListImpl<ModelComponent>(ModelComponent.class, this);
      ccomp.myDisplayMode = myDisplayMode;

      return ccomp;
   }


}
