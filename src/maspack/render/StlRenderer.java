/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC),
 * and ArtiSynth Team Members
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import maspack.matrix.AffineTransform2d;
import maspack.matrix.AffineTransform2dBase;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix3d;
import maspack.matrix.Matrix3dBase;
import maspack.matrix.Matrix4d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform2d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.SVDecomposition3d;
import maspack.matrix.Vector2d;
import maspack.matrix.Vector3d;
import maspack.render.RenderInstances.InstanceTransformType;
import maspack.util.BinaryOutputStream;
import maspack.util.NumberFormat;

/**
 * "Renders" a scene by writing it to an STL file.  Sequence of events should be:
 * <pre>
 *      stlRenderer.begin(fileName, true);  // open as binary
 *      //... render scene
 *      stlRenderer.end()                   // close file
 * </pre>
 * 
 * @author C Antonio Sanchez and ArtiSynth team members
 */
public class StlRenderer implements Renderer {


   private static interface StlWriter {
      public void open(File file) throws FileNotFoundException;
      public void writeHeader() throws IOException;
      public void writeTriangle(Point3d t1, Point3d t2, Point3d t3) throws IOException;
      public void writeFooter() throws IOException;
      public void close() throws IOException;
   }
   
   /**
    * Writes a binary stl file
    */
   private static class BinaryStlWriter implements StlWriter {

      BinaryOutputStream bos = null;
      private static final int BINARY_STL_HEADER_SIZE = 80;
      int numTriangles;
      File file;
      
      @Override
      public void open(File file) throws FileNotFoundException {
         this.file = file;
         bos = new BinaryOutputStream(new BufferedOutputStream(new FileOutputStream(file)));
         bos.setLittleEndian(true);
         numTriangles = 0;
      }

      @Override
      public void writeHeader() throws IOException {
         // 80 bytes
         for (int i=0; i<BINARY_STL_HEADER_SIZE; ++i) {
            bos.writeByte(0);
         }
         bos.writeInt(0);  // start with zero triangles
      }

      @Override
      public void writeTriangle(Point3d t1, Point3d t2, Point3d t3) throws IOException {
         
         // compute normal, CCW
         Vector3d nrm = new Vector3d();
         
         double d2x = t2.x - t1.x;
         double d2y = t2.y - t1.y;
         double d2z = t2.z - t1.z;
         double d3x = t3.x - t1.x;
         double d3y = t3.y - t1.y;
         double d3z = t3.z - t1.z;
         
         // d2 x d3
         nrm.x = d2y*d3z-d2z*d3y;
         nrm.y = -d2x*d3z+d2z*d3x;
         nrm.z = d2x*d3y-d2y*d3x;
         nrm.normalize();
         
         // first print normal
         bos.writeFloat((float)nrm.x);
         bos.writeFloat((float)nrm.y);
         bos.writeFloat((float)nrm.z);
         
         // then triangles
         bos.writeFloat((float)t1.x);
         bos.writeFloat((float)t1.y);
         bos.writeFloat((float)t1.z);
         bos.writeFloat((float)t2.x);
         bos.writeFloat((float)t2.y);
         bos.writeFloat((float)t2.z);
         bos.writeFloat((float)t3.x);
         bos.writeFloat((float)t3.y);
         bos.writeFloat((float)t3.z);
         
         // empty attribute
         bos.writeShort(0);
         
         ++numTriangles;
      }
      
      @Override
      public void writeFooter() throws IOException {
      }
      
      private int swapByteOrder(int x) {
         int out = 0;
         for (int i=0; i<4; ++i) {
            int b = x & 0xFF; 
            out = (out << 8) | b;
            x = x >> 8;
         }
         return out;
      }

      @Override
      public void close() throws IOException {
         bos.flush();
         bos.close();
         
         // fill in number of triangles
         RandomAccessFile raf = new RandomAccessFile(file, "rw");
         raf.seek(BINARY_STL_HEADER_SIZE);
         
         // swap to little-endian and write
         raf.writeInt(swapByteOrder(numTriangles));
         raf.close();
      }
      
   }
   
   /**
    * Writes an ASCII stl file (NOTE: could lead to VERY large files)
    */
   private static class AsciiStlWriter implements StlWriter {

      PrintWriter writer = null;
      final NumberFormat fmt = new NumberFormat("%.8g");
      
      @Override
      public void open(File file) throws FileNotFoundException {
         writer = new PrintWriter(file);
      }

      @Override
      public void writeHeader() throws IOException {
         writer.println("solid render");
      }

      @Override
      public void writeTriangle(Point3d t1, Point3d t2, Point3d t3)
         throws IOException {
         
         // compute normal, CCW
         Vector3d nrm = new Vector3d();
         
         double d2x = t2.x - t1.x;
         double d2y = t2.y - t1.y;
         double d2z = t2.z - t1.z;
         double d3x = t3.x - t1.x;
         double d3y = t3.y - t1.y;
         double d3z = t3.z - t1.z;
         
         // d2 x d3
         nrm.x = d2y*d3z-d2z*d3y;
         nrm.y = -d2x*d3z+d2z*d3x;
         nrm.z = d2x*d3y-d2y*d3x;
         nrm.normalize();
         
         // write triangle
         writer.println("facet normal " + fmt.format(nrm.x) + " " + fmt.format(nrm.y) + " " + fmt.format(nrm.z));
         writer.println("  outer loop");
         writer.println("    vertex " + fmt.format(t1.x) + " " + fmt.format(t1.y) + " " + fmt.format(t1.z));
         writer.println("    vertex " + fmt.format(t2.x) + " " + fmt.format(t2.y) + " " + fmt.format(t2.z));
         writer.println("    vertex " + fmt.format(t3.x) + " " + fmt.format(t3.y) + " " + fmt.format(t3.z));
         writer.println("  endloop");
         writer.println("endfacet");
         
      }
      
      @Override
      public void writeFooter() throws IOException {
         writer.println("endsolid render");
      }

      @Override
      public void close() throws IOException {
         writer.close();
      }
      
   }
   
   String filename;
   StlWriter writer;
   
   int width;
   int height;
   
   // frustum parameters
   protected static class ProjectionFrustrum {
      public double near = 1;
      public double far = 1000;
      public double left = -0.5;
      public double right = 0.5;
      public double top = 0.5;
      public double bottom = -0.5;

      public int depthBitOffset = 0;
      public int depthBits = 16;
      public double fov = 30;         // originally 70
      public double fieldHeight = 10; // originally 10
      public boolean orthographic = false;
      public boolean explicit = false;

      public ProjectionFrustrum clone() {
         ProjectionFrustrum c = new ProjectionFrustrum();
         c.near = near;
         c.far = far;
         c.left = left;
         c.right = right;
         c.top = top;
         c.bottom = bottom;
         c.depthBits = depthBits;
         c.depthBitOffset = depthBitOffset;
         c.fov = fov;
         c.fieldHeight = fieldHeight;
         c.orthographic = orthographic;
         c.explicit = explicit;
         return c;
      }
   }
   protected ProjectionFrustrum myFrustum = null;
   LinkedList<ProjectionFrustrum> frustrumStack = null;
   
   // matrices
   protected Matrix4d projectionMatrix;
   protected RigidTransform3d viewMatrix;
   protected AffineTransform3dBase modelMatrix;
   protected Matrix3d modelNormalMatrix;            // inverse-transform (for normals)
   protected AffineTransform2dBase textureMatrix;   // transforming texture coordinates
   protected boolean modelMatrixValidP = false;
   protected boolean viewMatrixValidP = false;
   protected boolean projectionMatrixValidP = false;
   protected boolean textureMatrixValidP = false;
   // stacks 
   private LinkedList<AffineTransform3dBase> modelMatrixStack;
   private LinkedList<Matrix3d> modelNormalMatrixStack;   // linked to model matrix
   
   protected Vector3d zDir = new Vector3d();     // used for determining zOrder

   protected static final float DEFAULT_DEPTH_OFFSET_INTERVAL = 1e-5f; // prevent z-fighting
   private static final Point3d DEFAULT_VIEWER_CENTER = new Point3d();

   protected static int DEFAULT_SURFACE_RESOLUTION = 32;
   protected int mySurfaceResolution = DEFAULT_SURFACE_RESOLUTION; 

   private static final float DEFAULT_POINT_SIZE = 1f;
   private static final float DEFAULT_LINE_WIDTH = 1f;
   private static final Shading DEFAULT_SHADING = Shading.FLAT;
   private static final FaceStyle DEFAULT_FACE_STYLE = FaceStyle.FRONT;
   private static final ColorMixing DEFAULT_COLOR_MIXING = ColorMixing.REPLACE;
   private static final ColorInterpolation DEFAULT_COLOR_INTERPOLATION =
      ColorInterpolation.RGB;
   private static final int DEFAULT_DEPTH_OFFSET = 0;

   // viewer state
   protected static class ViewState {
      protected Point3d myCenter = new Point3d (DEFAULT_VIEWER_CENTER);
      protected Point3d myUp = new Point3d (0, 0, 1);

      public ViewState clone() {
         ViewState vs = new ViewState();
         vs.myCenter.set(myCenter);
         vs.myUp.set(myUp);
         return vs;
      }
   }

   protected ViewState myViewState = null;
   protected LinkedList<ViewState> viewStateStack = null;
   protected ViewerState myCommittedViewerState = null;    // "committed" viewer state

   // Bits to indicate when state variables have been set to non-default values
   static private final int BACK_COLOR_BIT = 0x0001;
   static private final int EMISSION_BIT = 0x0002;
   static private final int SPECULAR_BIT = 0x0004;
   static private final int SHININESS_BIT = 0x0008;
   static private final int COLOR_INTERPOLATION_BIT = 0x0010;
   static private final int HIGHLIGHTING_BIT = 0x0020;
   
   static private final int FACE_STYLE_BIT = 0x0100;
   static private final int LINE_WIDTH_BIT = 0x0200;
   static private final int POINT_SIZE_BIT = 0x0400;
   static private final int SHADING_BIT = 0x0800;
   // surface resolution is not currently restored
   // static private final int SURFACE_RESOLUTION_BIT = 0x1000;
   static private final int COLOR_MIXING_BIT = 0x2000;
   static private final int DEPTH_OFFSET_BIT = 0x4000;

   // Status words to record when state variables have been touched or set to
   // non-default values.
   boolean myMappingsSet = false; // need to set
   boolean myModelMatrixSet = false;
   int myNonDefaultColorSettings = 0;
   int myNonDefaultGeneralSettings = 0;

   protected static class ViewerState {
      public boolean lightingEnabled;       // light equations
      public boolean depthEnabled;          // depth buffer
      public boolean depthWriteEnabled;     // depth writes
      public boolean colorEnabled;          // color buffer
      public boolean vertexColorsEnabled;   // use per-vertex colors
      public boolean textureMappingEnabled; // use texture maps
      public FaceStyle faceMode;
      public Shading shading;
      public boolean hsvInterpolationEnabled;  
      public ColorMixing colorMixing;       // method for combining material/vertex colors
      public boolean roundedPoints;
      public boolean transparencyEnabled;
      public float pointSize;
      public float lineWidth;
      
      public ViewerState() {
         setDefaults();
      }
      
      public void setDefaults() {
         lightingEnabled = true;
         depthEnabled = true;
         depthWriteEnabled = true;
         colorEnabled = true;
         vertexColorsEnabled = true;
         textureMappingEnabled = true;
         faceMode = FaceStyle.FRONT;
         shading = Shading.SMOOTH;
         hsvInterpolationEnabled = false;
         colorMixing = ColorMixing.REPLACE;
         roundedPoints = true;
         transparencyEnabled = false;
         pointSize = 1;
         lineWidth = 1;
      }

      public ViewerState clone() {
         ViewerState c = new ViewerState();
         c.lightingEnabled = lightingEnabled;
         c.depthEnabled = depthEnabled;
         c.depthWriteEnabled = depthWriteEnabled;
         c.colorEnabled = colorEnabled;
         c.faceMode = faceMode;
         c.shading = shading;
         c.vertexColorsEnabled = vertexColorsEnabled;
         c.hsvInterpolationEnabled = hsvInterpolationEnabled;
         c.textureMappingEnabled = textureMappingEnabled;
         c.colorMixing = colorMixing;
         c.roundedPoints = roundedPoints;
         c.transparencyEnabled = transparencyEnabled;
         c.pointSize = pointSize;
         c.lineWidth = lineWidth;
         return c;
      }
   }
   protected ViewerState myViewerState;
   protected LinkedList<ViewerState> viewerStateStack;
   
   // Colors
   protected static final Color DARK_RED = new Color (0.5f, 0, 0);
   protected static final Color DARK_GREEN = new Color (0, 0.5f, 0);
   protected static final Color DARK_BLUE = new Color (0, 0, 0.5f);
   
   protected float[] DEFAULT_MATERIAL_COLOR =    {0.8f, 0.8f, 0.8f, 1.0f};
   protected float[] DEFAULT_MATERIAL_EMISSION = {0.0f, 0.0f, 0.0f, 1.0f};
   protected float[] DEFAULT_MATERIAL_SPECULAR = {0.1f, 0.1f, 0.1f, 1.0f};
   protected float[] DEFAULT_HIGHLIGHT_COLOR =   {1f, 1f, 0f, 1f};
   protected float[] DEFAULT_SELECTING_COLOR =   {0f, 0f, 0f, 0f};
   protected float[] DEFAULT_BACKGROUND_COLOR =  {0f, 0f, 0f, 1f};
   protected float DEFAULT_MATERIAL_SHININESS = 32f;
   
   protected float[] myHighlightColor = Arrays.copyOf (DEFAULT_HIGHLIGHT_COLOR, 4);
   protected boolean myHighlightColorModified = true;
   protected HighlightStyle myHighlightStyle = HighlightStyle.COLOR;
   
   protected float[] mySelectingColor = Arrays.copyOf (DEFAULT_SELECTING_COLOR, 4); // color to use when selecting (color selection)
   protected boolean mySelectingColorModified = true;
   
   protected Material myCurrentMaterial = Material.createDiffuse(DEFAULT_MATERIAL_COLOR, 32f);
   protected float[] myBackColor = null;
   protected boolean myCurrentMaterialModified = true;  // trigger for detecting when material is updated
   protected float[] backgroundColor = Arrays.copyOf (DEFAULT_BACKGROUND_COLOR, 4);
   
   protected static enum ActiveColor {
      DEFAULT,
      HIGHLIGHT,
      SELECTING
   }
   protected ActiveColor myActiveColor = ActiveColor.DEFAULT;     // which front color is currently active
   protected ActiveColor myCommittedColor = null;  // color actually pushed to GPU
   
   // texture properties
   protected ColorMapProps myColorMapProps = null;
   protected NormalMapProps myNormalMapProps = null;
   protected BumpMapProps myBumpMapProps = null;
   
   // primitives
   private static enum PrimitiveType {
      SPHERE, SPINDLE, CAPPED_CYLINDER, UNCAPPED_CYLINDER
   }
   
   private static class PrimitiveKey {
      PrimitiveType type;
      int resolution;
      public PrimitiveKey(PrimitiveType type, int resolution) {
         this.type = type;
         this.resolution = resolution;
      }
      
      @Override
      public int hashCode() {
         return resolution*31 + type.hashCode();
      }
      
      @Override
      public boolean equals(Object obj) {
         if (this == obj) {
            return true;
         }
         if (obj == null || getClass() != obj.getClass()) {
            return false;
         }
         PrimitiveKey other = (PrimitiveKey)obj;
         return resolution == other.resolution && type == other.type;
      }
   }
   
   HashMap<PrimitiveKey,RenderObject> primitiveMap;
   
   public StlRenderer() {
      width = 1024;
      height = 1024;
      
      myFrustum = new ProjectionFrustrum();
      frustrumStack = new LinkedList<>();

      myViewState = new ViewState();
      viewStateStack = new LinkedList<>();

      myViewerState = new ViewerState();
      viewerStateStack = new LinkedList<>();

      // initialize matrices
      projectionMatrix = new Matrix4d();
      viewMatrix = new RigidTransform3d();
      modelMatrix = new RigidTransform3d();
      modelNormalMatrix = new Matrix3d(modelMatrix.getMatrix());
      textureMatrix = RigidTransform2d.IDENTITY.copy();

      modelMatrixStack = new LinkedList<>();
      modelNormalMatrixStack = new LinkedList<>();
      computeProjectionMatrix ();
      
      primitiveMap = new HashMap<>();
   }
   
   /**
    * Starts writing an STL file with the given filename, in binary mode
    * @param fileName STL file name
    */
   public void begin(String fileName) {
      begin(fileName, true);
   }
   
   /**
    * Starts writing an STL file
    * @param fileName STL file name
    * @param binary if true, writes in binary file format, otherwise in ASCII
    */
   public void begin(String fileName, boolean binary) {
      if (binary) {
         writer = new BinaryStlWriter();
      } else {
         writer = new AsciiStlWriter();
      }
      
      try {
         writer.open(new File(fileName));
         writer.writeHeader();
      } catch (IOException e) {
         e.printStackTrace();
      }
   }
   
   /**
    * Closes the STL rendering file
    */
   public void end() {
      try {
         writer.writeFooter();
         writer.close();
      } catch (IOException e) {
         e.printStackTrace();
      } finally {
    	 writer = null;
      }
   }
   
   @Override
   protected void finalize() throws Throwable {
	  if (writer != null) {
		 end();
	  }
      super.finalize();
   }
   
   protected void computeProjectionMatrix() {

      // from frustrum info
      double[] pvals = null;
      double w = myFrustum.right-myFrustum.left;
      double h = myFrustum.top-myFrustum.bottom;
      double d = myFrustum.far-myFrustum.near;
      
      // adjust offset to account for proper bin depth
      double zoffset = 0;
      if (myFrustum.depthBitOffset != 0) {
         zoffset = -2.0*myFrustum.depthBitOffset/(1 << (myFrustum.depthBits-1));
      }
      
      if (myFrustum.orthographic) {
         pvals = new double[]{
                              2/w, 0, 0, -(myFrustum.right+myFrustum.left)/w,
                              0, 2/h, 0, -(myFrustum.top+myFrustum.bottom)/h,
                              0,0,-2/d, -(myFrustum.far+myFrustum.near)/d+zoffset,
                              0, 0, 0, 1
         };
      } else {
         pvals = new double[] {
                               2*myFrustum.near/w, 0, (myFrustum.right+myFrustum.left)/w, 0,
                               0, 2*myFrustum.near/h, (myFrustum.top+myFrustum.bottom)/h, 0,
                               0, 0, -(myFrustum.far+myFrustum.near)/d-zoffset, -2*myFrustum.near*myFrustum.far/d,
                               0, 0, -1, 0
         };
      }

      projectionMatrix.set(pvals);

   }
   
   @Override
   public int getScreenHeight() {
      return height;
   }

   @Override
   public int getScreenWidth() {
      return width;
   }

   @Override
   public double getViewPlaneHeight() {
      if (myFrustum.orthographic) {
         return myFrustum.fieldHeight;
      }
      else {
         return myFrustum.top - myFrustum.bottom;
      }
   }

   @Override
   public double getViewPlaneWidth() {
      return (width / (double)height) * getViewPlaneHeight();
   }

   @Override
   public double distancePerPixel (Vector3d pnt) {
      if (myFrustum.orthographic) {
         return myFrustum.fieldHeight / height;
      }
      else {
         Point3d pntInEye = new Point3d (pnt);
         pntInEye.transform (viewMatrix);
         return Math.abs (pntInEye.z / myFrustum.near) * (myFrustum.top - myFrustum.bottom) / height;
      }
   }

   @Override
   public double centerDistancePerPixel() {
      return distancePerPixel (myViewState.myCenter);
   }

   @Override
   public Point3d getCenter() {
      return new Point3d (myViewState.myCenter);
   }

   @Override
   public boolean isOrthogonal() {
      return myFrustum.orthographic;
   }

   @Override
   public double getViewPlaneDistance() {
      return myFrustum.near;
   }

   @Override
   public double getFarPlaneDistance() {
      return myFrustum.far;
   }

   @Override
   public float getPointSize() {
      return myViewerState.pointSize;
   }

   @Override
   public void setPointSize(float size) {
      myViewerState.pointSize = size;
      if (size != DEFAULT_POINT_SIZE) {
         myNonDefaultGeneralSettings |= POINT_SIZE_BIT;
      }
      else {
         myNonDefaultGeneralSettings &= ~POINT_SIZE_BIT;
      }
   }

   @Override
   public float getLineWidth() {
      return myViewerState.lineWidth;
   }

   @Override
   public void setLineWidth(float width) {
      myViewerState.lineWidth = width;
      if (width != DEFAULT_LINE_WIDTH) {
         myNonDefaultGeneralSettings |= LINE_WIDTH_BIT;
      }
      else {
         myNonDefaultGeneralSettings &= ~LINE_WIDTH_BIT;
      }
   }

   @Override
   public FaceStyle getFaceStyle() {
      return myViewerState.faceMode;
   }

   @Override
   public FaceStyle setFaceStyle(FaceStyle style) {
      FaceStyle prev = myViewerState.faceMode;
      myViewerState.faceMode = style;
      if (style != DEFAULT_FACE_STYLE) {
         myNonDefaultGeneralSettings |= FACE_STYLE_BIT;
      }
      else {
         myNonDefaultGeneralSettings &= ~FACE_STYLE_BIT;
      }
      return prev;
   }

   @Override
   public ColorInterpolation getColorInterpolation() {
      if (myViewerState.hsvInterpolationEnabled) {
         return ColorInterpolation.HSV;
      }
      else {
         return ColorInterpolation.RGB;
      }
   }

   @Override
   public ColorInterpolation setColorInterpolation(ColorInterpolation interp) {
      ColorInterpolation prev = getColorInterpolation();
      myViewerState.hsvInterpolationEnabled = (interp==ColorInterpolation.HSV);
      
      if (interp != DEFAULT_COLOR_INTERPOLATION) {
         myNonDefaultColorSettings |= COLOR_INTERPOLATION_BIT;
      }
      else {
         myNonDefaultColorSettings &= ~COLOR_INTERPOLATION_BIT;
      }
      return prev;
   }

   @Override
   public boolean hasVertexColorMixing(ColorMixing cmix) {
      return false;
   }

   @Override
   public ColorMixing getVertexColorMixing() {
      return myViewerState.colorMixing;
   }

   @Override
   public ColorMixing setVertexColorMixing(ColorMixing cmix) {
      ColorMixing prev = myViewerState.colorMixing;
      if (hasVertexColorMixing(cmix)) {
         myViewerState.colorMixing = cmix;
         if (cmix != DEFAULT_COLOR_MIXING) {
            myNonDefaultGeneralSettings |= COLOR_MIXING_BIT;
         }
         else {
            myNonDefaultGeneralSettings &= ~COLOR_MIXING_BIT;
         }
      }
      return prev;
   }

   @Override
   public Shading getShading() {
      return myViewerState.shading;
   }

   @Override
   public Shading setShading(Shading shading) {
      Shading prev = myViewerState.shading;
      myViewerState.shading = shading;
      
      if (shading != DEFAULT_SHADING) {
         myNonDefaultGeneralSettings |= SHADING_BIT;
      }
      else {
         myNonDefaultGeneralSettings &= ~SHADING_BIT;
      }
      return prev;
   }

   @Override
   public void drawPoint(Vector3d pnt) {
      drawPoint(pnt.x, pnt.y, pnt.z);
   }

   @Override
   public void drawPoint(double px, double py, double pz) {
      // DRAW NOTHING
   }

   @Override
   public void drawPoint(float[] pnt) {
      drawPoint(pnt[0], pnt[1], pnt[2]);
   }

   @Override
   public void drawLine(Vector3d pnt0, Vector3d pnt1) {
      drawLine(pnt0.x, pnt0.y, pnt0.z, pnt1.x, pnt1.y, pnt1.z);
   }

   @Override
   public void drawLine(
      double px0, double py0, double pz0, double px1, double py1, double pz1) {
      // DRAW NOTHING
   }

   @Override
   public void drawLine(float[] pnt0, float[] pnt1) {
      drawLine(pnt0[0], pnt0[1], pnt0[2], pnt1[0], pnt1[1], pnt1[2]);
   }

   @Override
   public void drawTriangle(Vector3d pnt0, Vector3d pnt1, Vector3d pnt2) {
      
      Point3d p0 = new Point3d(pnt0);
      Point3d p1 = new Point3d(pnt1);
      Point3d p2 = new Point3d(pnt2);
      
      p0.transform(modelMatrix);
      p1.transform(modelMatrix);
      p2.transform(modelMatrix);
      
      try {
         writer.writeTriangle(p0, p1, p2);
      } catch (IOException e) {
         e.printStackTrace();
      }
   }

   private Vector3d toVector(float[] xyz) {
      return new Vector3d(xyz[0], xyz[1], xyz[2]);
   }
   
   private Point3d toPoint(float[] xyz) {
      return new Point3d(xyz[0], xyz[1], xyz[2]);
   }
   
   @Override
   public void drawTriangle(float[] pnt0, float[] pnt1, float[] pnt2) {
      drawTriangle(toVector(pnt0), toVector(pnt1), toVector(pnt2));
   }

   @Override
   public int getSurfaceResolution() {
      return mySurfaceResolution;
   }

   @Override
   public int setSurfaceResolution(int nsegs) {
      int prev = mySurfaceResolution;
      mySurfaceResolution = nsegs;
      //      if (nsegs != DEFAULT_SURFACE_RESOLUTION) {
      //         myNonDefaultGeneralSettings |= SURFACE_RESOLUTION_BIT;
      //      }
      //      else {
      //         myNonDefaultGeneralSettings &= ~SURFACE_RESOLUTION_BIT;
      //      }
      return prev;
   }

 
   @Override
   public void drawSphere(Vector3d pnt, double rad) {
      
      int res = getSurfaceResolution();
      PrimitiveKey key = new PrimitiveKey(PrimitiveType.SPHERE, res);
      
      RenderObject robj = primitiveMap.get(key);
      if (robj == null) {
         robj = RenderObjectFactory.createSphere(res, res/2);
         primitiveMap.put(key, robj);
      }

      pushModelMatrix();
      translateModelMatrix(pnt.x, pnt.y, pnt.z);
      scaleModelMatrix(rad);
      drawTriangles(robj, 0);
      popModelMatrix();
      
   }

   @Override
   public void drawSphere(float[] pnt, double rad) {
      drawSphere(toVector(pnt), rad);
   }

   @Override
   public void drawCube(Vector3d pnt, double w) {
      drawBox(new RigidTransform3d(pnt, AxisAngle.IDENTITY), new Vector3d(w,w,w));
   }

   @Override
   public void drawCube(float[] pnt, double w) {
      drawCube(toVector(pnt), w);
   }

   @Override
   public void drawBox(float[] pnt, double wx, double wy, double wz) {
      drawBox(toVector(pnt), new Vector3d(wx, wy, wz));
   }

   @Override
   public void drawBox(Vector3d pnt, Vector3d widths) {
      drawBox(new RigidTransform3d(pnt, AxisAngle.IDENTITY), widths);
   }

   @Override
   public void drawBox(RigidTransform3d TBM, Vector3d widths) {
      // 8 corners
      
      double dx = widths.x/2;
      double dy = widths.y/2;
      double dz = widths.z/2;
      
      Point3d p[] = new Point3d[8];
      p[0] = new Point3d(-dx,-dy,-dz);
      p[1] = new Point3d(-dx,-dy,dz);
      p[2] = new Point3d(-dx,dy,-dz);
      p[3] = new Point3d(-dx,dy,dz);
      p[4] = new Point3d(dx,-dy,-dz);
      p[5] = new Point3d(dx,-dy,dz);
      p[6] = new Point3d(dx,dy,-dz);
      p[7] = new Point3d(dx,dy,dz);
      
      // transform to correct space
      for (Point3d pp : p) {
         pp.transform(TBM);
         pp.transform(modelMatrix);
      }
      
      try {
         writer.writeTriangle(p[1], p[0], p[5]);
         writer.writeTriangle(p[5], p[0], p[4]);
         writer.writeTriangle(p[5], p[4], p[7]);
         writer.writeTriangle(p[7], p[4], p[6]);
         writer.writeTriangle(p[7], p[6], p[3]);
         writer.writeTriangle(p[3], p[6], p[2]);
         writer.writeTriangle(p[3], p[2], p[1]);
         writer.writeTriangle(p[1], p[2], p[0]);
         writer.writeTriangle(p[1], p[5], p[3]);
         writer.writeTriangle(p[3], p[5], p[7]);
         writer.writeTriangle(p[4], p[0], p[6]);
         writer.writeTriangle(p[6], p[0], p[2]);
      } catch(IOException e) {
         e.printStackTrace();
      }
   }
   
   protected RigidTransform3d getLineTransform(Vector3d p0, Vector3d p1) {
      RigidTransform3d X = new RigidTransform3d();

      Vector3d utmp = new Vector3d();
      utmp.set (p1.x - p0.x, p1.y - p0.y, p1.z - p0.z);
      X.p.set (p0.x, p0.y, p0.z);
      X.R.setZDirection (utmp);

      return X;
   }

   @Override
   public void drawSpindle(Vector3d pnt0, Vector3d pnt1, double rad) {
      if (rad < Double.MIN_NORMAL) {
         return;
      }

      double dx = pnt1.x-pnt0.x;
      double dy = pnt1.y-pnt0.y;
      double dz = pnt1.z-pnt0.z;
      double len = Math.sqrt(dx*dx+dy*dy+dz*dz);

      // compute required rotation
      RigidTransform3d lineRot = getLineTransform(pnt0, pnt1);
      
      int resolution = getSurfaceResolution();
      PrimitiveKey key = new PrimitiveKey(PrimitiveType.SPINDLE, resolution);
      RenderObject robj = primitiveMap.get(key);
      if (robj == null) {
         robj = RenderObjectFactory.createSpindle(resolution, resolution/2);
         primitiveMap.put(key, robj);
      }
      
      // scale and translate model matrix
      pushModelMatrix();
      mulModelMatrix(lineRot);
      scaleModelMatrix(rad, rad, len);

      // draw spindle
      drawTriangles(robj, 0);

      // revert matrix transform
      popModelMatrix();      
   }

   @Override
   public void drawSpindle(float[] pnt0, float[] pnt1, double rad) {
      drawSpindle(toVector(pnt0), toVector(pnt1), rad);
   }

   @Override
   public void drawCylinder(
      Vector3d pnt0, Vector3d pnt1, double rad, boolean capped) {
      
      if (rad < Double.MIN_NORMAL) {
         return;
      }

      double dx = pnt1.x-pnt0.x;
      double dy = pnt1.y-pnt0.y;
      double dz = pnt1.z-pnt0.z;
      double len = Math.sqrt(dx*dx+dy*dy+dz*dz);

      // compute required rotation
      RigidTransform3d lineRot = getLineTransform(pnt0, pnt1);
      
      int resolution = getSurfaceResolution();
      PrimitiveKey key;
      if (capped) {
         key = new PrimitiveKey(PrimitiveType.CAPPED_CYLINDER, resolution);
      } else {
         key = new PrimitiveKey(PrimitiveType.UNCAPPED_CYLINDER, resolution);
      }
      RenderObject robj = primitiveMap.get(key);
      if (robj == null) {
         robj = RenderObjectFactory.createCylinder(resolution, capped);
         primitiveMap.put(key, robj);
      }
      
      // scale and translate model matrix
      pushModelMatrix();
      mulModelMatrix(lineRot);
      scaleModelMatrix(rad, rad, len);

      // draw cylinder
      drawTriangles(robj, 0);

      // revert matrix transform
      popModelMatrix();      
      
   }

   @Override
   public void drawCylinder(
      float[] pnt0, float[] pnt1, double rad, boolean capped) {
      drawCylinder(toVector(pnt0), toVector(pnt1), rad, capped);
   }

   @Override
   public void drawCone(
      Vector3d pnt0, Vector3d pnt1, double rad0, double rad1, boolean capped) {
      
      // get a capped or uncapped cylinder
      double rad = 1;

      double dx = pnt1.x-pnt0.x;
      double dy = pnt1.y-pnt0.y;
      double dz = pnt1.z-pnt0.z;
      double len = Math.sqrt(dx*dx+dy*dy+dz*dz);

      // compute required rotation
      RigidTransform3d lineRot = getLineTransform(pnt0, pnt1);
      
      int resolution = getSurfaceResolution();
      PrimitiveKey key;
      if (capped) {
         key = new PrimitiveKey(PrimitiveType.CAPPED_CYLINDER, resolution);
      } else {
         key = new PrimitiveKey(PrimitiveType.UNCAPPED_CYLINDER, resolution);
      }
      RenderObject robj = primitiveMap.get(key);
      if (robj == null) {
         robj = RenderObjectFactory.createCylinder(resolution, capped);
         primitiveMap.put(key, robj);
      }
      
      // scale and translate model matrix
      pushModelMatrix();
      mulModelMatrix(lineRot);
      scaleModelMatrix(rad, rad, len);

      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      Point3d p2 = new Point3d();
      
      // draw cylinder
      robj.readLock(); {
         try {
            int[] tris = robj.getTriangles(0);
            int ntris = robj.numTriangles(0);
            int triStride = robj.getTriangleStride();
            
            for (int i=0; i<triStride*ntris; i += triStride) {
               float[] v0 = robj.getVertexPosition(tris[i]);
               float[] v1 = robj.getVertexPosition(tris[i+1]);
               float[] v2 = robj.getVertexPosition(tris[i+2]);
               
               // scale radially based on z
               p0 = toPoint(v0);
               double s0 = v0[2]*rad1+(1-v0[2])*rad0;
               p0.x *= s0;
               p0.y *= s0;
               
               p1 = toPoint(v1);
               double s1 = v1[2]*rad1+(1-v1[2])*rad0;
               p1.x *= s1;
               p1.y *= s1;
               
               p2 = toPoint(v2);
               double s2 = v2[2]*rad1+(1-v2[2])*rad0;
               p2.x *= s2;
               p2.y *= s2;
               
               // transform by model matrix
               p0.transform(modelMatrix);
               p1.transform(modelMatrix);
               p2.transform(modelMatrix);
               
               // write
               writer.writeTriangle(p0, p1, p2);
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      robj.readUnlock();
      
      // revert matrix transform
      popModelMatrix();      
      
   }

   @Override
   public void drawCone(
      float[] pnt0, float[] pnt1, double rad0, double rad1, boolean capped) {
      drawCone(toVector(pnt0), toVector(pnt1), rad0, rad1, capped);
   }

   @Override
   public void drawArrow(
      Vector3d pnt0, Vector3d pnt1, double rad, boolean capped) {

      if (rad < Double.MIN_NORMAL) {
         return;
      }

      double dx = pnt1.x-pnt0.x;
      double dy = pnt1.y-pnt0.y;
      double dz = pnt1.z-pnt0.z;
      double len = Math.sqrt(dx*dx+dy*dy+dz*dz);

      double arrowRad = 3*rad;
      double arrowLen = Math.min(2*arrowRad,len/2);
      double lenFrac = 1-arrowLen/len;
      Point3d mid = new Point3d(pnt0.x + (float)(lenFrac*dx),
                                      pnt0.y + (float)(lenFrac*dy), 
                                      pnt0.z + (float)(lenFrac*dz));

      drawCylinder(pnt0, mid, rad, capped);
      drawCone(mid, pnt1, arrowRad, 0, capped);
      
   }

   @Override
   public void drawArrow(
      float[] pnt0, float[] pnt1, double rad, boolean capped) {
      drawArrow(toVector(pnt0), toVector(pnt1), rad, capped);
   }

   @Override
   public void drawAxes(
      RigidTransform3d X, double len, int width, boolean highlight) {
      drawAxes(X, new double[]{len, len, len}, width, highlight);
   }

   @Override
   public void drawSolidAxes (
      RigidTransform3d X, double[] lens, double rad, boolean highlight) {
      
      boolean savedHighlighting = setHighlighting(highlight);
      // deal with transform and len
      double lx = lens[0];
      double ly = lens[1];
      double lz = lens[2];

      if (X == null) {
         X = RigidTransform3d.IDENTITY;
      }
      pushModelMatrix();

      mulModelMatrix(X);
      
      if (lx != 0) {
         setColor (Color.RED);
         drawArrow (Point3d.ZERO, new Point3d (lx, 0, 0), rad, true);
      }
      if (ly != 0) {
         setColor (Color.GREEN);
         drawArrow (Point3d.ZERO, new Point3d (0, ly, 0), rad, true);
      }
      if (lz != 0) {
         setColor (Color.BLUE);
         drawArrow (Point3d.ZERO, new Point3d (0, 0, lz), rad, true);
      }
      
      // revert matrix transform
      popModelMatrix();

      setHighlighting(savedHighlighting);
   }
   
   @Override
   public void drawSolidAxes (
      RigidTransform3d X, double len, double rad, boolean highlight) {
      
      drawSolidAxes (X, new double[] {len, len, len}, rad, highlight);
   }
   

   @Override
   public void drawAxes(
      RigidTransform3d X, double[] lens, int width, boolean highlight) {
      // DRAW NOTHING
   }

   @Override
   public void drawPoint(RenderProps props, float[] pnt, boolean highlight) {
      switch(props.getPointStyle()) {
         case CUBE:
            drawCube(pnt, 2*props.getPointRadius());
            break;
         case SPHERE:
            drawSphere(pnt, props.getPointRadius());
            break;
         case POINT:
         default:
            break;
         
      }
   }

   @Override
   public void drawLine(
      RenderProps props, float[] pnt0, float[] pnt1, boolean highlight) {
      drawLine(props, pnt0, pnt1, null, true, highlight);
   }

   @Override
   public void drawLine(
      RenderProps props, float[] pnt0, float[] pnt1, float[] color,
      boolean capped, boolean highlight) {

      LineStyle style = props.getLineStyle();
      switch(style) {
         case CYLINDER:
            drawCylinder(pnt0, pnt1, props.getLineRadius(), capped);
            break;
         case SOLID_ARROW:
            drawArrow(pnt0, pnt1, props.getLineRadius(), capped);
            break;
         case SPINDLE:
            drawSpindle(pnt0, pnt1, props.getLineRadius());
            break;
         case LINE:
         default:
            break;
         
      }
      
   }

   @Override
   public void drawLine(
      RenderProps props, float[] pnt0, float[] pnt1, boolean capped,
      boolean highlight) {
      drawLine(props, pnt0, pnt1, null, capped, highlight);
   }

   @Override
   public void drawArrow(
      RenderProps props, float[] pnt0, float[] pnt1, boolean capped,
      boolean highlight) {
      
      drawArrow(pnt0, pnt1, props.getLineRadius(), capped);
      
   }

   @Override
   public void drawLineStrip(
      RenderProps props, Iterable<float[]> pnts, LineStyle style,
      boolean highlight) {
      
      switch (style) {
         case CYLINDER: {
            double rad = props.getLineRadius();
            Iterator<float[]> it = pnts.iterator();
            float[] second = it.next();
            while (it.hasNext()) {
               float[] first = second;
               second = it.next();
               drawCylinder(first, second, rad, true);
            }
            break;
         }
         case SOLID_ARROW: {
            double rad = props.getLineRadius();
            Iterator<float[]> it = pnts.iterator();
            float[] second = it.next();
            while (it.hasNext()) {
               float[] first = second;
               second = it.next();
               drawArrow(first, second, rad, true);
            }
            break;
         }
         case SPINDLE: {
            double rad = props.getLineRadius();
            Iterator<float[]> it = pnts.iterator();
            float[] second = it.next();
            while (it.hasNext()) {
               float[] first = second;
               second = it.next();
               drawSpindle(first, second, rad);
            }
            break;
         }
         case LINE:
         default:
            // do not draw anything
            break;
      }
      
   }

   @Override
   public Rectangle2D getTextBounds(Font font, String str, double emSize) {
      return new Rectangle();
   }

   @Override
   public void setDefaultFont(Font font) {
   }

   @Override
   public Font getDefaultFont() {
      return new Font(Font.SANS_SERIF, Font.PLAIN, 12);
   }

   @Override
   public boolean hasTextRendering() {
      return false;
   }

   @Override
   public double drawText(String str, float[] pos, double emSize) {
      return drawText(getDefaultFont(), str, pos, emSize);
   }

   @Override
   public double drawText(Font font, String str, float[] pos, double emSize) {
      return drawText(font, str, toVector(pos), emSize);
   }

   @Override
   public double drawText(String str, Vector3d pos, double emSize) {
      return drawText(getDefaultFont(), str, pos, emSize);
   }

   @Override
   public double drawText(Font font, String str, Vector3d pos, double emSize) {
      // DRAW NOTHING
      return 0;
   }

   @Override
   public Vector3d getEyeZDirection() {
      Vector3d zdir = new Vector3d();
      viewMatrix.R.getRow(2, zdir);
      return zdir;
   }

   @Override
   public boolean begin2DRendering() {
      return begin2DRendering (getScreenWidth(), getScreenHeight());
   }

   @Override
   public boolean begin2DRendering(double w, double h) {
      return false;
   }

   @Override
   public boolean has2DRendering() {
      return false;
   }

   @Override
   public void end2DRendering() {
   }

   @Override
   public boolean is2DRendering() {
      return false;
   }

   @Override
   public void setColor(float[] rgba, boolean highlight) {
      setFrontColor (rgba);
      setBackColor (null);
      setHighlighting (highlight);    
   }

   @Override
   public void setColor(float[] rgba) {
      setFrontColor (rgba);
      setBackColor (null);
   }

   @Override
   public void setColor(Color color) {
      setColor (color.getColorComponents(null));
   }

   @Override
   public void setColor(float r, float g, float b) {
      setColor(new float[]{r,g,b,1.0f});
   }

   @Override
   public void setColor(float r, float g, float b, float a) {
      setColor(new float[]{r,g,b,a});
   }

   @Override
   public void setFrontColor(float[] rgba) {
      myCurrentMaterial.setDiffuse (rgba);
      myCurrentMaterialModified = true;
   }

   @Override
   public float[] getFrontColor(float[] rgba) {
      if (rgba == null) {
         rgba = new float[4];
      }
      myCurrentMaterial.getDiffuse (rgba);
      return rgba;
   }

   @Override
   public void setBackColor(float[] rgba) {
      if (rgba == null) {
         if (myBackColor != null) {
            myBackColor = null;
            myCurrentMaterialModified = true;
         }
         myNonDefaultColorSettings &= ~BACK_COLOR_BIT;
      } else {
         if (myBackColor == null) {
            myBackColor = new float[4];
         }
         myBackColor[0] = rgba[0];
         myBackColor[1] = rgba[1];
         myBackColor[2] = rgba[2];
         myBackColor[3] = (rgba.length > 3 ? rgba[3] : 1.0f);
         myCurrentMaterialModified = true;
         myNonDefaultColorSettings |= BACK_COLOR_BIT;
      }
   }

   @Override
   public float[] getBackColor(float[] rgba) {
      if (myBackColor == null) {
         return null;
      }
      if (rgba == null) {
         rgba = new float[4];
      }
      rgba[0] = myBackColor[0];
      rgba[1] = myBackColor[1];
      rgba[2] = myBackColor[2];
      if (rgba.length > 3) {
         rgba[3] = myBackColor[3];         
      }
      return rgba;
   }

   @Override
   public void setEmission(float[] rgb) {
      myCurrentMaterial.setEmission (rgb);
      if (rgb[0] != DEFAULT_MATERIAL_EMISSION[0] ||
          rgb[1] != DEFAULT_MATERIAL_EMISSION[1] ||
          rgb[2] != DEFAULT_MATERIAL_EMISSION[2]) {
         myNonDefaultColorSettings |= EMISSION_BIT;
      }
      else {
         myNonDefaultColorSettings &= ~EMISSION_BIT;
      }
      myCurrentMaterialModified = true;
   }

   @Override
   public float[] getEmission(float[] rgb) {
      if (rgb == null) {
         rgb = new float[3];
      }
      myCurrentMaterial.getEmission (rgb);
      return rgb;
   }

   @Override
   public void setSpecular(float[] rgb) {
      myCurrentMaterial.setSpecular (rgb);
      if (rgb[0] != DEFAULT_MATERIAL_SPECULAR[0] ||
          rgb[1] != DEFAULT_MATERIAL_SPECULAR[1] ||      
          rgb[2] != DEFAULT_MATERIAL_SPECULAR[2]) {
         myNonDefaultColorSettings |= SPECULAR_BIT;
      }
      else {
         myNonDefaultColorSettings &= ~SPECULAR_BIT;
      }
      myCurrentMaterialModified = true;
   }

   @Override
   public float[] getSpecular(float[] rgb) {
      if (rgb == null) {
         rgb = new float[3];
      }
      myCurrentMaterial.getSpecular (rgb);
      return rgb;
   }

   @Override
   public void setShininess(float s) {
      myCurrentMaterial.setShininess(s);
      if (s != DEFAULT_MATERIAL_SHININESS) {
         myNonDefaultColorSettings |= SHININESS_BIT;
      }
      else {
         myNonDefaultColorSettings &= ~SHININESS_BIT;
      }
      myCurrentMaterialModified = true;
   }

   @Override
   public float getShininess() {
      return myCurrentMaterial.getShininess();
   }

   @Override
   public void setFrontAlpha(float a) {
      myCurrentMaterial.setAlpha(a);
      myCurrentMaterialModified = true;
   }

   @Override
   public void setPointColoring(RenderProps props, boolean highlight) {
      setPropsColoring (props, props.getPointColorF(), highlight);
   }

   @Override
   public void setLineColoring(RenderProps props, boolean highlight) {
      setPropsColoring (props, props.getLineColorF(), highlight);
   }

   @Override
   public void setEdgeColoring(RenderProps props, boolean highlight) {
      float[] rgba = props.getEdgeColorF();
      if (rgba == null) {
         rgba = props.getLineColorF();
      }
      setPropsColoring (props, rgba, highlight);
      setShading (props.getShading());
   }

   @Override
   public void setFaceColoring(RenderProps props, boolean highlight) {
      setFaceColoring (props, props.getFaceColorF(), highlight);
   }

   @Override
   public void setFaceColoring(
      RenderProps props, float[] rgba, boolean highlight) {
      setFrontColor (rgba);
      if (rgba.length == 3) {
         setFrontAlpha ((float)props.getAlpha());
      }
      setBackColor (props.getBackColorF());
      setShininess (props.getShininess());
      setEmission (DEFAULT_MATERIAL_EMISSION);
      float[] specular = props.getSpecularF();
      setSpecular (specular != null ? specular : DEFAULT_MATERIAL_SPECULAR);
      setHighlighting (highlight);     
   }

   @Override
   public void setPropsColoring(
      RenderProps props, float[] rgba, boolean highlight) {
      setHighlighting (highlight);         
      setFrontColor (rgba);
      if (rgba.length == 3) {
         setFrontAlpha ((float)props.getAlpha());
      }
      setBackColor (null);
      setShininess (props.getShininess());
      setEmission (DEFAULT_MATERIAL_EMISSION);
      float[] specular = props.getSpecularF();
      setSpecular (specular != null ? specular : DEFAULT_MATERIAL_SPECULAR);
   }

   @Override
   public Shading setPointShading(RenderProps props) {
      Shading prevShading = getShading();
      Shading shading;
      if (props.getPointStyle() == PointStyle.POINT) {
         shading = Shading.NONE;
      }
      else {
         shading = props.getShading();
      }
      setShading (shading);
      return prevShading;
   }

   @Override
   public Shading setLineShading(RenderProps props) {
      Shading prevShading = getShading();
      Shading shading;
      if (props.getLineStyle() == LineStyle.LINE) {
         shading = Shading.NONE;
      }
      else {
         shading = props.getShading();
      }
      setShading (shading);      
      return prevShading;
   }

   @Override
   public Shading setPropsShading(RenderProps props) {
      Shading prevShading = getShading();
      setShading (props.getShading());      
      return prevShading;
   }

   @Override
   public boolean hasColorMapping() {
      return false;
   }

   @Override
   public boolean hasColorMapMixing(ColorMixing cmix) {
      return false;
   }

   @Override
   public ColorMapProps setColorMap(ColorMapProps props) {
      ColorMapProps old = myColorMapProps;
      if (hasColorMapping()) {
         if (props != null) {
            myColorMapProps = props.clone();
         } else {
            myColorMapProps = null;
         }
         myMappingsSet = true;
      }
      return old;
   }

   @Override
   public ColorMapProps getColorMap() {
      return myColorMapProps;
   }

   @Override
   public boolean hasNormalMapping() {
      return false;
   }

   @Override
   public NormalMapProps setNormalMap(NormalMapProps props) {
      NormalMapProps old = myNormalMapProps;
      if (hasNormalMapping()){
         if (props != null) {
            myNormalMapProps = props.clone();
         } else {
            myNormalMapProps = null;
         }
         myMappingsSet = true;
      }
      return old;
   }

   @Override
   public NormalMapProps getNormalMap() {
      return myNormalMapProps;
   }

   @Override
   public boolean hasBumpMapping() {
      return false;
   }

   @Override
   public BumpMapProps setBumpMap(BumpMapProps props) {
      BumpMapProps old = myBumpMapProps;
      if (hasBumpMapping()) {
         if (props != null) {
            myBumpMapProps = props.clone();
         } else {
            myBumpMapProps = null;
         }
         myMappingsSet = true;
      }
      return old;
   }

   @Override
   public BumpMapProps getBumpMap() {
      return myBumpMapProps;
   }

   @Override
   public void drawTriangles(RenderObject robj) {
      drawTriangles(robj, robj.getTriangleGroupIdx ());
   }

   @Override
   public void drawTriangles(RenderObject robj, int gidx) {
      drawTriangles(robj, gidx, 0, robj.numTriangles (gidx));
   }

   @Override
   public void drawTriangles(
      RenderObject robj, int gidx, int offset, int count) {
      
      Point3d p0 = new Point3d();
      Point3d p1 = new Point3d();
      Point3d p2 = new Point3d();
      
      robj.readLock(); {
         try {
            int[] tris = robj.getTriangles(gidx);
            int triStride = robj.getTriangleStride();
            
            for (int i=triStride*offset; i<triStride*count; i += triStride) {
               float[] v0 = robj.getVertexPosition(tris[i]);
               float[] v1 = robj.getVertexPosition(tris[i+1]);
               float[] v2 = robj.getVertexPosition(tris[i+2]);
               
               p0.transform(modelMatrix, toVector(v0));
               p1.transform(modelMatrix, toVector(v1));
               p2.transform(modelMatrix, toVector(v2));
               
               writer.writeTriangle(p0, p1, p2);
            }
         } catch (IOException e) {
            e.printStackTrace();
         }
      }
      robj.readUnlock();
   }

   @Override
   public void drawLines(RenderObject robj) {
      drawLines(robj, robj.getLineGroupIdx ());
   }

   @Override
   public void drawLines(RenderObject robj, int gidx) {
      // DRAW NOTHING
   }

   @Override
   public void drawLines(RenderObject robj, LineStyle style, double rad) {
      drawLines(robj, robj.getLineGroupIdx (), style, rad);
      
   }

   @Override
   public void drawLines(
      RenderObject robj, int gidx, LineStyle style, double rad) {
      drawLines(robj, gidx, 0, robj.numLines (gidx), style, rad);
   }

   @Override
   public void drawLines(
      RenderObject robj, int gidx, int offset, int count, LineStyle style,
      double rad) {
      
      switch(style) {
         case CYLINDER: {
            robj.readLock(); {
               int[] lines = robj.getLines(gidx);
               int lineStride = robj.getLineStride();

               for (int i=lineStride*offset; i<lineStride*count; i += lineStride) {
                  float[] v0 = robj.getVertexPosition(lines[i]);
                  float[] v1 = robj.getVertexPosition(lines[i+1]);

                  drawCylinder(v0, v1, rad, true);
               }
            }
            robj.readUnlock();
            break;
         }
         case SOLID_ARROW: {
            robj.readLock(); {
               int[] lines = robj.getLines(gidx);
               int lineStride = robj.getLineStride();

               for (int i=lineStride*offset; i<lineStride*count; i += lineStride) {
                  float[] v0 = robj.getVertexPosition(lines[i]);
                  float[] v1 = robj.getVertexPosition(lines[i+1]);

                  drawArrow(v0, v1, rad, true);
               }
            }
            robj.readUnlock();
            break;
         }
         case SPINDLE: {
            robj.readLock(); {
               int[] lines = robj.getLines(gidx);
               int lineStride = robj.getLineStride();

               for (int i=lineStride*offset; i<lineStride*count; i += lineStride) {
                  float[] v0 = robj.getVertexPosition(lines[i]);
                  float[] v1 = robj.getVertexPosition(lines[i+1]);

                  drawSpindle(v0, v1, rad);
               }
            }
            robj.readUnlock();
            break;
         }
         case LINE:
         default: {
            break;
         }
      }
     
   }

   @Override
   public void drawPoints(RenderObject robj) {
      drawPoints(robj, robj.getPointGroupIdx ());
   }

   @Override
   public void drawPoints(RenderObject robj, int gidx) {
      // DRAW NOTHING
   }

   @Override
   public void drawPoints(RenderObject robj, PointStyle style, double rad) {
      drawPoints(robj, robj.getPointGroupIdx (), style, rad);
   }

   @Override
   public void drawPoints(
      RenderObject robj, int gidx, PointStyle style, double rad) {
      drawPoints(robj, gidx, 0, robj.numPoints (gidx), style, rad);
   }

   @Override
   public void drawPoints(
      RenderObject robj, int gidx, int offset, int count, PointStyle style,
      double rad) {
      
      switch(style) {
         case CUBE: {
            robj.readLock(); {
               int[] points = robj.getPoints(gidx);
               int pointStride = robj.getPointStride();

               for (int i=pointStride*offset; i<pointStride*count; i += pointStride) {
                  float[] v0 = robj.getVertexPosition(points[i]);
                  drawCube(v0, 2*rad);
               }
            }
            robj.readUnlock();
            break;
         }
         case SPHERE: {
            robj.readLock(); {
               int[] points = robj.getPoints(gidx);
               int pointStride = robj.getPointStride();

               for (int i=pointStride*offset; i<pointStride*count; i += pointStride) {
                  float[] v0 = robj.getVertexPosition(points[i]);
                  drawSphere(v0, rad);
               }
            }
            robj.readUnlock();
            break;
         }
         case POINT:
         default:
            break;
         
      }
   }

   @Override
   public void drawVertices(RenderObject robj, DrawMode mode) {
      
      switch (mode) {
         case TRIANGLES: {
            if (robj.numVertices() < 3) {
               break;
            }
            
            Point3d p0 = new Point3d();
            Point3d p1 = new Point3d();
            Point3d p2 = new Point3d();
            
            robj.readLock(); {
               try {
                  
                  // add triangle triples
                  for (int i=0; i<robj.numVertices(); i+=3) {
                     float[] tmp = robj.getVertexPosition(i);
                     p0.set(tmp[0], tmp[1], tmp[2]);
                     p0.transform(modelMatrix);
                     
                     tmp = robj.getVertexPosition(i+1);
                     p1.set(tmp[0], tmp[1], tmp[2]);
                     p1.transform(modelMatrix);
                     
                     tmp = robj.getVertexPosition(i+2);
                     p2.set(tmp[0], tmp[1], tmp[2]);
                     p2.transform(modelMatrix);
                     
                     writer.writeTriangle(p0, p1, p2);
                  }
                 
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
            robj.readUnlock();
            
            break;
         }
         case TRIANGLE_FAN: {
            if (robj.numVertices() < 3) {
               break;
            }
          
            Point3d p0 = new Point3d();
            Point3d p1 = new Point3d();
            Point3d p2 = new Point3d();
            
            robj.readLock(); {
               try {
                  
                  // add pairs of triangles
                  float[] tmp = robj.getVertexPosition(0);
                  p0.set(tmp[0], tmp[1], tmp[2]);
                  p0.transform(modelMatrix);
                  
                  tmp = robj.getVertexPosition(1);
                  p2.set(tmp[0], tmp[1], tmp[2]);
                  p2.transform(modelMatrix);
                  
                  for (int i=2; i<robj.numVertices(); ++i) {
                     p1 = p2;
                     
                     tmp = robj.getVertexPosition(i);
                     p2.set(tmp[0], tmp[1], tmp[2]);
                     p2.transform(modelMatrix);
                     
                     writer.writeTriangle(p0, p1, p2);
                  }
                 
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
            robj.readUnlock();
            
            break;
         }
         case TRIANGLE_STRIP: {
            if (robj.numVertices() < 3) {
               break;
            }
            
            Point3d p0 = new Point3d();
            Point3d p1 = new Point3d();
            Point3d p2 = new Point3d();
            Point3d p3 = new Point3d();
            
            robj.readLock(); {
               try {
                  int vStart = 0;
                  int vEnd = robj.numVertices()-1;
                  
                  // add pairs of triangles
                  float[] tmp = robj.getVertexPosition(0);
                  p2.set(tmp[0], tmp[1], tmp[2]);
                  p2.transform(modelMatrix);
                  
                  tmp = robj.getVertexPosition(1);
                  p3.set(tmp[0], tmp[1], tmp[2]);
                  p3.transform(modelMatrix);
                  
                  for (int i=vStart+2; i<vEnd; i+=2) {
                     p0 = p2;
                     p1 = p3;
                     
                     tmp = robj.getVertexPosition(i);
                     p2.set(tmp[0], tmp[1], tmp[2]);
                     p2.transform(modelMatrix);
                     
                     tmp = robj.getVertexPosition(i+1);
                     p3.set(tmp[0], tmp[1], tmp[2]);
                     p3.transform(modelMatrix);
                     
                     writer.writeTriangle(p0, p1, p2);
                     writer.writeTriangle(p2, p1, p3);
                  }
                  
                  // add last triangle
                  int i = vEnd-vStart;
                  if (i > 2 && i % 2 == 0) {
                     p0 = p2;
                     p1 = p3;
                     tmp = robj.getVertexPosition(vEnd);
                     p2.set(tmp[0], tmp[1], tmp[2]);
                     p2.transform(modelMatrix);
                     
                     writer.writeTriangle(p0, p1, p2);
                  }  
                  
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
            robj.readUnlock();
            
           break;
         }
         case LINES:
         case LINE_LOOP:
         case LINE_STRIP:
         case POINTS:
         default:
            break;
      }
   }

   @Override
   public void drawVertices(
      RenderObject robj, VertexIndexArray idxs, DrawMode mode) {
      drawVertices(robj, idxs, 0, idxs.size (), mode);
   }

   @Override
   public void drawVertices(
      RenderObject robj, VertexIndexArray idxs, int offset, int count,
      DrawMode mode) {
      
      switch (mode) {
         case TRIANGLES: {
            if (count < 3) {
               break;
            }
            
            Point3d p0 = new Point3d();
            Point3d p1 = new Point3d();
            Point3d p2 = new Point3d();
            
            robj.readLock(); {
               try {
                  
                  // add triangle triples
                  for (int i=0; i<count; i+=3) {
                     float[] tmp = robj.getVertexPosition(idxs.get(offset+i));
                     p0.set(tmp[0], tmp[1], tmp[2]);
                     p0.transform(modelMatrix);
                     
                     tmp = robj.getVertexPosition(idxs.get(offset+i+1));
                     p1.set(tmp[0], tmp[1], tmp[2]);
                     p1.transform(modelMatrix);
                     
                     tmp = robj.getVertexPosition(idxs.get(offset+i+2));
                     p2.set(tmp[0], tmp[1], tmp[2]);
                     p2.transform(modelMatrix);
                     
                     writer.writeTriangle(p0, p1, p2);
                  }
                 
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
            robj.readUnlock();
            
            break;
         }
         case TRIANGLE_FAN: {
            if (count < 3) {
               break;
            }
          
            Point3d p0 = new Point3d();
            Point3d p1 = new Point3d();
            Point3d p2 = new Point3d();
            
            robj.readLock(); {
               try {
                  
                  // add pairs of triangles
                  float[] tmp = robj.getVertexPosition(idxs.get(offset));
                  p0.set(tmp[0], tmp[1], tmp[2]);
                  p0.transform(modelMatrix);
                  
                  tmp = robj.getVertexPosition(idxs.get(offset+1));
                  p2.set(tmp[0], tmp[1], tmp[2]);
                  p2.transform(modelMatrix);
                  
                  for (int i=2; i<count; ++i) {
                     p1 = p2;
                     
                     tmp = robj.getVertexPosition(idxs.get(offset+i));
                     p2.set(tmp[0], tmp[1], tmp[2]);
                     p2.transform(modelMatrix);
                     
                     writer.writeTriangle(p0, p1, p2);
                  }
                 
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
            robj.readUnlock();
            
            break;
         }
         case TRIANGLE_STRIP: {
            if (count < 3) {
               break;
            }
            
            Point3d p0 = new Point3d();
            Point3d p1 = new Point3d();
            Point3d p2 = new Point3d();
            Point3d p3 = new Point3d();
            
            robj.readLock(); {
               try {
                  int vStart = offset;
                  int vEnd = offset+count-1;
                  
                  // add pairs of triangles
                  float[] tmp = robj.getVertexPosition(idxs.get(vStart));
                  p2.set(tmp[0], tmp[1], tmp[2]);
                  p2.transform(modelMatrix);
                  
                  tmp = robj.getVertexPosition(idxs.get(vStart+1));
                  p3.set(tmp[0], tmp[1], tmp[2]);
                  p3.transform(modelMatrix);
                  
                  for (int i=vStart+2; i<vEnd; i+=2) {
                     p0 = p2;
                     p1 = p3;
                     
                     tmp = robj.getVertexPosition(idxs.get(i));
                     p2.set(tmp[0], tmp[1], tmp[2]);
                     p2.transform(modelMatrix);
                     
                     tmp = robj.getVertexPosition(idxs.get(i+1));
                     p3.set(tmp[0], tmp[1], tmp[2]);
                     p3.transform(modelMatrix);
                     
                     writer.writeTriangle(p0, p1, p2);
                     writer.writeTriangle(p2, p1, p3);
                  }
                  
                  // add last triangle
                  int i = vEnd-vStart;
                  if (i > 2 && i % 2 == 0) {
                     p0 = p2;
                     p1 = p3;
                     tmp = robj.getVertexPosition(idxs.get(vEnd));
                     p2.set(tmp[0], tmp[1], tmp[2]);
                     p2.transform(modelMatrix);
                     
                     writer.writeTriangle(p0, p1, p2);
                  }  
                  
               } catch (IOException e) {
                  e.printStackTrace();
               }
            }
            robj.readUnlock();
            
           break;
         }
         case LINES:
         case LINE_LOOP:
         case LINE_STRIP:
         case POINTS:
         default:
            break;
      }
      
   }

   @Override
   public void draw(RenderObject robj) {
      drawPoints (robj);
      drawLines (robj);
      drawTriangles (robj);
   }

  
   
   @Override
   public void drawTriangles(
      RenderObject robj, int gidx, RenderInstances rinst) {

      int idx = 0;
      int[] buff = rinst.getInstances();
      InstanceTransformType[] types = InstanceTransformType.values();
      
      rinst.readLock();
      robj.readLock();
      
      for (int i=0; i< rinst.numInstances(); ++i) {
         
         pushModelMatrix();
         
         int type = buff[idx++];
         int tidx = buff[idx++];
         int sidx = buff[idx++];
         
         InstanceTransformType tt = types[type];
         switch(tt) {
            case POINT: {
               // point
               float[] pnt = rinst.getPoint(tidx);
               translateModelMatrix(pnt[0], pnt[1], pnt[2]);
               break;
            }
            case FRAME: {
               RigidTransform3d frame = rinst.getFrame(tidx);
               mulModelMatrix(frame);
               break;
            }
            case AFFINE: {
               AffineTransform3d affine = rinst.getAffine(tidx);
               mulModelMatrix(affine);
               break;
            }
         }
         
         Double s = rinst.getScale(sidx);
         if (s != null && s.doubleValue() != 1.0) {
            scaleModelMatrix(s);
         }
         
         drawTriangles(robj);
         popModelMatrix();
      }
      
      robj.readUnlock();
      rinst.readUnlock();
      
   }

   @Override
   public void drawLines(RenderObject robj, int gidx, RenderInstances rinst) {
      // DRAW NOTHING
   }

   @Override
   public void drawPoints(RenderObject robj, int gidx, RenderInstances rinst) {
      // DRAW NOTHING
   }

   @Override
   public void pushModelMatrix() {
      modelMatrixStack.push(modelMatrix.copy());
      modelNormalMatrixStack.push(modelNormalMatrix.clone());
      myModelMatrixSet = true;
   }

   @Override
   public AffineTransform3dBase getModelMatrix() {
      return modelMatrix.copy();
   }

   @Override
   public void getModelMatrix(AffineTransform3d X) {
      X.set(modelMatrix);
   }

   @Override
   public void translateModelMatrix(double tx, double ty, double tz) {
      RigidTransform3d TR = new RigidTransform3d (tx, ty, tz);
      mulModelMatrix (TR);
   }

   @Override
   public void rotateModelMatrix(double zdeg, double ydeg, double xdeg) {
      RigidTransform3d TR = new RigidTransform3d (
         0, 0, 0, 
         Math.toRadians(zdeg), Math.toRadians(ydeg), Math.toRadians(xdeg)); 
      mulModelMatrix (TR);
   }

   @Override
   public void scaleModelMatrix(double s) {
      synchronized(modelMatrix) {
         AffineTransform3d am = new AffineTransform3d(modelMatrix);
         am.applyScaling(s, s, s);
         modelMatrix = am; // normal matrix is unchanged
      }
   }
   
   public void scaleModelMatrix(double sx, double sy, double sz) {
      synchronized(modelMatrix) {
         AffineTransform3d am = new AffineTransform3d(modelMatrix);
         am.applyScaling(sx, sy, sz);
         modelMatrix = am;
         if (sx == 0) {
            sx = Double.MAX_VALUE;
         } else {
            sx = 1.0/sx;
         }
         if (sy == 0) {
            sy = Double.MAX_VALUE;
         } else {
            sy = 1.0/sy;
         }
         if (sz == 0) {
            sz = Double.MAX_VALUE;
         } else {
            sz = 1.0/sz;
         }
         modelNormalMatrix.scaleColumn(0, sx);
         modelNormalMatrix.scaleColumn(1, sy);
         modelNormalMatrix.scaleColumn(2, sz);
      }
   }
   
   protected Matrix3d computeInverseTranspose(Matrix3dBase M) {
      Matrix3d out = new Matrix3d(M);
      if (!(M instanceof RotationMatrix3d)) {
         boolean success = out.invert();
         if (!success) {
            SVDecomposition3d svd3 = new SVDecomposition3d(M);
            svd3.pseudoInverse(out);
         }
         out.transpose();
      }
      return out;
   }

   @Override
   public void mulModelMatrix(AffineTransform3dBase trans) {
      synchronized(modelMatrix) {
         if (trans instanceof RigidTransform3d) {
            RigidTransform3d rigid = (RigidTransform3d)trans;
            modelMatrix.mul(rigid);
            modelNormalMatrix.mul(rigid.R);  
         }
         else {
            AffineTransform3d aff = new AffineTransform3d(modelMatrix);
            aff.mul(trans);
            modelMatrix = aff;
            modelNormalMatrix = computeInverseTranspose(aff.getMatrix());
         }
      }
   }

   @Override
   public void setModelMatrix(AffineTransform3dBase X) {
      synchronized(modelMatrix) {
         if (modelMatrix.getClass() == X.getClass()) {
            modelMatrix.set(X);
         } else {
            modelMatrix = X.copy();
         }
         modelNormalMatrix = computeInverseTranspose(X.getMatrix());
      }
   }

   @Override
   public void setModelMatrix2d(
      double left, double right, double bottom, double top) {
      AffineTransform3d XMW = new AffineTransform3d();
      double w = right-left;
      double h = top-bottom;      
      XMW.A.m00 = 2/w;
      XMW.A.m11 = 2/h;
      XMW.p.set (-(left+right)/w, -(top+bottom)/h, 0);
      setModelMatrix (XMW);
   }

   @Override
   public boolean popModelMatrix() {
      if (modelMatrixStack.size() == 0) {
         return false;
      } 
      modelMatrix = modelMatrixStack.pop();
      modelNormalMatrix = modelNormalMatrixStack.pop();
      myModelMatrixSet = true;
      return true;
   }

   @Override
   public RigidTransform3d getViewMatrix() {
      return viewMatrix.copy();
   }

   @Override
   public void getViewMatrix(RigidTransform3d TWE) {
      TWE.set(viewMatrix);
   }

   @Override
   public void setDepthOffset(int offset) {
      myFrustum.depthBitOffset = offset;
      if (offset != DEFAULT_DEPTH_OFFSET) {
         myNonDefaultGeneralSettings |= DEPTH_OFFSET_BIT;
      }
      else {
         myNonDefaultGeneralSettings &= ~DEPTH_OFFSET_BIT;
      }      
      computeProjectionMatrix ();
   }

   @Override
   public int getDepthOffset() {
      return myFrustum.depthBitOffset;
   }

   @Override
   public void setTextureMatrix(AffineTransform2dBase T) {
      synchronized(textureMatrix) {
         if (textureMatrix.getClass() == T.getClass()) {
            textureMatrix.set(T);
         } else {
            textureMatrix = T.copy();
         }
      }
   }

   @Override
   public AffineTransform2dBase getTextureMatrix() {
      return textureMatrix.copy ();
   }

   @Override
   public void getTextureMatrix(AffineTransform2d X) {
      X.set (textureMatrix);
   }

   @Override
   public boolean hasSelection() {
      return false;
   }

   @Override
   public boolean isSelecting() {
      return false;
   }

   @Override
   public HighlightStyle getHighlightStyle() {
      return myHighlightStyle;
   }

   @Override
   public void getHighlightColor(float[] rgba) {
      if (rgba.length < 3) {
         throw new IllegalArgumentException (
            "Argument rgba must have length of at least 3");
      }
      rgba[0] = myHighlightColor[0];
      rgba[1] = myHighlightColor[1];
      rgba[2] = myHighlightColor[2];
      if (rgba.length > 3) {
         rgba[3] = myHighlightColor[3];
      }
   }

   @Override
   public boolean setHighlighting(boolean enable) {
      boolean prev = (myActiveColor == ActiveColor.HIGHLIGHT);
      if (myHighlightStyle == HighlightStyle.COLOR) {
         if (enable != prev) {
            // indicate that we may need to update color state
            if (enable) {
               myNonDefaultColorSettings |= HIGHLIGHTING_BIT;
               myActiveColor = ActiveColor.HIGHLIGHT;
            }
            else {
               myNonDefaultColorSettings &= ~HIGHLIGHTING_BIT;
               myActiveColor = ActiveColor.DEFAULT;
            }
         }
      }
      else if (myHighlightStyle == HighlightStyle.NONE) {
         // don't do anything ...
      }
      else {
         throw new UnsupportedOperationException (
            "Unsupported highlighting: " + myHighlightStyle);
      }
      return prev;
   }

   @Override
   public boolean getHighlighting() {
      // for now, only color highlighting is implemented
      return myActiveColor == ActiveColor.HIGHLIGHT;
   }

   @Override
   public void beginSelectionQuery(int qid) {
      // nothing
   }

   @Override
   public void endSelectionQuery() {
      // nothing
   }

   @Override
   public void beginSubSelection(IsSelectable s, int qid) {
      // nothing
   }

   @Override
   public void endSubSelection() {
      // nothing
   }

   @Override
   public boolean isSelectable(IsSelectable s) {
      // nothing
      return false;
   }
   
   // data for "drawMode"
   protected DrawMode myDrawMode = null;
   protected boolean myDrawHasNormalData = false;
   protected boolean myDrawHasColorData = false;
   protected boolean myDrawHasTexcoordData = false;
   protected int myDrawVertexIdx = 0;
   protected float[] myDrawCurrentNormal = new float[3];
   protected float[] myDrawCurrentColor = new float[4];
   protected float[] myDrawCurrentTexcoord = new float[2];
   protected int myDrawDataCap = 0;
   protected float[] myDrawVertexData = null;
   protected float[] myDrawNormalData = null;
   protected float[] myDrawColorData = null;
   protected float[] myDrawTexcoordData = null;

   /**
    * Returns either Selected (if selected color is currently active)
    * or the current material's diffuse color otherwise
    * 
    * @return current color being used by this viewer
    */
   private float[] getCurrentColor() {
      switch (myActiveColor) {
         case HIGHLIGHT:
            return myHighlightColor;
         case SELECTING:
            return mySelectingColor;
         case DEFAULT:
         default:
            return myCurrentMaterial.getDiffuse();
      }
   }
   
   protected void ensureDrawDataCapacity () {
      if (myDrawVertexIdx >= myDrawDataCap) {
         int cap = myDrawDataCap;
         if (cap == 0) {
            cap = 1000;
            myDrawVertexData = new float[3*cap];
            if (myDrawHasNormalData) {
               myDrawNormalData = new float[3*cap];
            }
            if (myDrawHasColorData) {
               myDrawColorData = new float[4*cap];
            }
            if (myDrawHasTexcoordData) {
               myDrawTexcoordData = new float[2*cap];
            }
         }
         else {
            cap = (int)(cap*1.5); // make sure cap is a multiple of 3
            myDrawVertexData = Arrays.copyOf (myDrawVertexData, 3*cap);
            if (myDrawHasNormalData) {
               myDrawNormalData = Arrays.copyOf (myDrawNormalData, 3*cap);
            }
            if (myDrawHasColorData) {
               myDrawColorData = Arrays.copyOf (myDrawColorData, 4*cap);
            }
            if (myDrawHasTexcoordData) {
               myDrawTexcoordData = Arrays.copyOf(myDrawTexcoordData, 2*cap);
            }
         }
         myDrawDataCap = cap;
      }
   } 
   
   @Override
   public void beginDraw(DrawMode mode) {
      if (myDrawMode != null) {
         throw new IllegalStateException (
         "Currently in draw mode (i.e., beginDraw() has already been called)");
      }
      myDrawMode = mode;
      myDrawVertexIdx = 0;
      myDrawHasNormalData = false;
      myDrawHasColorData = false;
      myDrawHasTexcoordData = false;

      myDrawCurrentColor = Arrays.copyOf (getCurrentColor(), 4);
   }

   @Override
   public void addVertex(float px, float py, float pz) {
      if (myDrawMode == null) {
         throw new IllegalStateException (
            "Not in draw mode (i.e., beginDraw() has not been called)");
      }
      ensureDrawDataCapacity();

      // check if we need colors
      if (!myDrawHasColorData && myCurrentMaterialModified) {
         // we need to store colors
         myDrawHasColorData = true;
         myDrawColorData = new float[4*myDrawDataCap];
         int cidx = 0;
         // back-fill colors
         for (int i=0; i<myDrawVertexIdx; ++i) {
            myDrawColorData[cidx++] = myDrawCurrentColor[0];
            myDrawColorData[cidx++] = myDrawCurrentColor[1];
            myDrawColorData[cidx++] = myDrawCurrentColor[2];
            myDrawColorData[cidx++] = myDrawCurrentColor[3];
         }
      }

      int vbase = 3*myDrawVertexIdx;
      if (myDrawHasNormalData) {
         myDrawNormalData[vbase  ] = myDrawCurrentNormal[0];
         myDrawNormalData[vbase+1] = myDrawCurrentNormal[1];
         myDrawNormalData[vbase+2] = myDrawCurrentNormal[2];
      }
      if (myDrawHasColorData) {
         int cbase = 4*myDrawVertexIdx;
         myDrawCurrentColor = getCurrentColor ();
         myDrawColorData[cbase  ] = myDrawCurrentColor[0];
         myDrawColorData[cbase+1] = myDrawCurrentColor[1];
         myDrawColorData[cbase+2] = myDrawCurrentColor[2];
         myDrawColorData[cbase+3] = myDrawCurrentColor[3];
      }
      if (myDrawHasTexcoordData) {
         int tbase = 2*myDrawVertexIdx;
         myDrawTexcoordData[tbase ] = myDrawCurrentTexcoord[0];
         myDrawTexcoordData[tbase+1] = myDrawCurrentTexcoord[1];
      }
      myDrawVertexData[vbase] = px;
      myDrawVertexData[++vbase] = py;
      myDrawVertexData[++vbase] = pz;
      ++myDrawVertexIdx;
   }

   @Override
   public void addVertex(double px, double py, double pz) {
      addVertex ((float)px, (float)py, (float)pz);      
   }

   @Override
   public void addVertex(Vector3d pnt) {
      addVertex ((float)pnt.x, (float)pnt.y, (float)pnt.z);
   }

   @Override
   public void addVertex(float[] pnt) {
      addVertex (pnt[0], pnt[1], pnt[2]);
   }

   @Override
   public void setNormal(float nx, float ny, float nz) {
      if (myDrawMode == null) {
         throw new IllegalStateException (
            "Not in draw mode (i.e., beginDraw() has not been called)");
      }
      myDrawCurrentNormal[0] = nx;
      myDrawCurrentNormal[1] = ny;
      myDrawCurrentNormal[2] = nz;
      if (!myDrawHasNormalData) {
         // back-fill previous normal data
         for (int i=0; i<myDrawVertexIdx; i++) {
            myDrawNormalData[i] = 0f;
         }
         myDrawHasNormalData = true;
      }
   }

   @Override
   public void setNormal(double nx, double ny, double nz) {
      setNormal ((float)nx, (float)ny, (float)nz);
   }

   @Override
   public void setNormal(Vector3d nrm) {
      setNormal ((float)nrm.x, (float)nrm.y, (float)nrm.z);
   }

   @Override
   public void setTextureCoord(float tx, float ty) {
      if (myDrawMode == null) {
         throw new IllegalStateException (
            "Not in draw mode (i.e., beginDraw() has not been called)");
      }
      myDrawCurrentTexcoord[0] = tx;
      myDrawCurrentTexcoord[1] = ty;
      if (!myDrawHasTexcoordData) {
         // back-fill previous normal data
         for (int i=0; i<myDrawVertexIdx; i++) {
            myDrawTexcoordData[i] = 0f;
         }
         myDrawHasTexcoordData = true;
      }
   }

   @Override
   public void setTextureCoord(double tx, double ty) {
      setTextureCoord((float)tx, (float)ty);
   }

   @Override
   public void setTextureCoord(Vector2d tex) {
      setTextureCoord ((float)tex.x, (float)tex.y);
   }
   
   private void resetDraw() {
      myDrawMode = null;

      myDrawDataCap = 0;
      myDrawVertexData = null;
      myDrawNormalData = null;
      myDrawColorData = null;
      myDrawTexcoordData = null;
      
      myDrawVertexIdx = 0;
      myDrawHasNormalData = false;
      myDrawHasColorData = false;
      myDrawHasTexcoordData = false;     
   }

   protected void doDraw(DrawMode mode,
      int numVertices, float[] vertexData, 
      boolean hasNormalData, float[] normalData, 
      boolean hasColorData, float[] colorData,
      boolean hasTexcoordData, float[] texcoordData) {
      
      switch (mode) {
         case TRIANGLES: {
            if (numVertices < 3) {
               break;
            }
            
            Point3d p0 = new Point3d();
            Point3d p1 = new Point3d();
            Point3d p2 = new Point3d();
            
            try {

               // add triangle triples
               for (int i=0; i<numVertices; i+=3) {
                  int off = 3*i;
                  p0.set(vertexData[off], vertexData[off+1], vertexData[off+2]);
                  p0.transform(modelMatrix);

                  off += 3;
                  p1.set(vertexData[off], vertexData[off+1], vertexData[off+2]);
                  p1.transform(modelMatrix);
                  
                  off += 3;
                  p2.set(vertexData[off], vertexData[off+1], vertexData[off+2]);
                  p2.transform(modelMatrix);
                  
                  writer.writeTriangle(p0, p1, p2);
               }

            } catch (IOException e) {
               e.printStackTrace();
            }
            
            break;
         }
         case TRIANGLE_FAN: {
            if (numVertices < 3) {
               break;
            }
          
            Point3d p0 = new Point3d();
            Point3d p1 = new Point3d();
            Point3d p2 = new Point3d();

            try {

               // add pairs of triangles
               int off = 0;
               p0.set(vertexData[0], vertexData[1], vertexData[2]);
               p0.transform(modelMatrix);

               off += 3;
               p2.set(vertexData[off], vertexData[off+1], vertexData[off+2]);
               p2.transform(modelMatrix);

               for (int i=2; i<numVertices; ++i) {
                  p1 = p2;

                  off += 3;
                  p2.set(vertexData[off], vertexData[off+1], vertexData[off+2]);
                  p2.transform(modelMatrix);

                  writer.writeTriangle(p0, p1, p2);
               }

            } catch (IOException e) {
               e.printStackTrace();
            }
            
            break;
         }
         case TRIANGLE_STRIP: {
            if (numVertices < 3) {
               break;
            }
            
            Point3d p0 = new Point3d();
            Point3d p1 = new Point3d();
            Point3d p2 = new Point3d();
            Point3d p3 = new Point3d();
            
            try {
               int vStart = 0;
               int vEnd = numVertices-1;

               // add pairs of triangles
               int off = 0;
               p2.set(vertexData[off], vertexData[off+1], vertexData[off+2]);
               p2.transform(modelMatrix);

               off += 3;
               p3.set(vertexData[off], vertexData[off+1], vertexData[off+2]);
               p3.transform(modelMatrix);

               for (int i=vStart+2; i<vEnd; i+=2) {
                  p0 = p2;
                  p1 = p3;

                  off += 3;
                  p2.set(vertexData[off], vertexData[off+1], vertexData[off+2]);
                  p2.transform(modelMatrix);

                  off += 3;
                  p3.set(vertexData[off], vertexData[off+1], vertexData[off+2]);
                  p3.transform(modelMatrix);
                  
                  writer.writeTriangle(p0, p1, p2);
                  writer.writeTriangle(p2, p1, p3);
               }

               // add last triangle
               int i = vEnd-vStart;
               if (i > 2 && i % 2 == 0) {
                  p0 = p2;
                  p1 = p3;
                  
                  off += 3;
                  p2.set(vertexData[off], vertexData[off+1], vertexData[off+2]);
                  p2.transform(modelMatrix);

                  writer.writeTriangle(p0, p1, p2);
               }  

            } catch (IOException e) {
               e.printStackTrace();
            }
            
           break;
         }
         case LINES:
         case LINE_LOOP:
         case LINE_STRIP:
         case POINTS:
         default:
            break;
      }
      
   }
   
   @Override
   public void endDraw() {
      if (myDrawMode == null) {
         throw new IllegalStateException (
            "Not in draw mode (i.e., beginDraw() has not been called)");
      }
      doDraw(myDrawMode, myDrawVertexIdx, myDrawVertexData, 
         myDrawHasNormalData, myDrawNormalData, 
         myDrawHasColorData, myDrawColorData,
         myDrawHasTexcoordData, myDrawTexcoordData);

      resetDraw();
   }

   @Override
   public void restoreDefaultState(boolean strictChecking) {
      
      if (myMappingsSet) {
         if (myColorMapProps != null) {
            setColorMap (null);
         }
         if (myNormalMapProps != null) {
            setNormalMap (null);
         }
         if (myBumpMapProps != null) {
            setBumpMap (null);
         }
         myMappingsSet = false;
      }
      if (myNonDefaultColorSettings != 0) {
         if (myBackColor != null) {
            setBackColor (null);
         }
         if ((myNonDefaultColorSettings & EMISSION_BIT) != 0) {
            setEmission (DEFAULT_MATERIAL_EMISSION);
         }
         if ((myNonDefaultColorSettings & SPECULAR_BIT) != 0) {
            setSpecular (DEFAULT_MATERIAL_SPECULAR);
         }
         if ((myNonDefaultColorSettings & SHININESS_BIT) != 0) {
            setShininess (DEFAULT_MATERIAL_SHININESS);
         }
         if (getColorInterpolation() != DEFAULT_COLOR_INTERPOLATION) {
            setColorInterpolation (DEFAULT_COLOR_INTERPOLATION);
         }
         if (myActiveColor == ActiveColor.HIGHLIGHT) {
            setHighlighting (false);
         }
         myNonDefaultColorSettings = 0;
      }
      if (myNonDefaultGeneralSettings != 0) {
         if (myViewerState.faceMode != DEFAULT_FACE_STYLE) {
            setFaceStyle (DEFAULT_FACE_STYLE);
         }
         if ((myNonDefaultGeneralSettings & LINE_WIDTH_BIT) != 0) {
            setLineWidth (DEFAULT_LINE_WIDTH);
         }
         if ((myNonDefaultGeneralSettings & POINT_SIZE_BIT) != 0) {
            setPointSize (DEFAULT_POINT_SIZE);
         }
         if (myViewerState.shading != DEFAULT_SHADING) {
            setShading (DEFAULT_SHADING);
         }
//         if (mySurfaceResolution != DEFAULT_SURFACE_RESOLUTION) {
//            setSurfaceResolution (DEFAULT_SURFACE_RESOLUTION);
//         }
         if (myViewerState.colorMixing != DEFAULT_COLOR_MIXING) {
            setVertexColorMixing (DEFAULT_COLOR_MIXING);
         }
         if (getDepthOffset() != DEFAULT_DEPTH_OFFSET) {
            setDepthOffset (DEFAULT_DEPTH_OFFSET);
         }
         myNonDefaultGeneralSettings = 0;
      }
      if (myModelMatrixSet) {
         int mmsize = modelMatrixStack.size();
         if (mmsize > 0) {
            if (strictChecking) {
               throw new IllegalStateException (
                  "render() method exited with model matrix stack size of " +
                   mmsize);
            }
            else {
               while (mmsize > 0) {
                  modelMatrixStack.pop();
                  mmsize--;
               }
            }
         }
         else {
            synchronized (modelMatrix) {
               if (!modelMatrix.isIdentity()) {
                  modelMatrix = new RigidTransform3d(); // reset to identity
                  modelNormalMatrix = new Matrix3d();
               }
            }           
         }
         myModelMatrixSet = false;
      }
      if (myDrawMode != null) {
         if (strictChecking) {
            throw new IllegalStateException (
               "render() method exited while still in draw mode: "+myDrawMode);
         }
         else {
            resetDraw();
         }
      }
      
      // set front alpha if not one
      if (myCurrentMaterial.isTransparent()) {
         setFrontAlpha(1.0f);
      }
   }
  
   
}

