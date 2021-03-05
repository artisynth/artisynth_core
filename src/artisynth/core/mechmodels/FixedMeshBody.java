/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.geometry.MeshBase;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.Quaternion;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.Renderer;
import maspack.render.RenderableUtils;
import maspack.render.RenderList;
import maspack.properties.PropertyList;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.modelbase.HasCoordinateFrame;
import artisynth.core.mechmodels.Frame.AxisDrawStyle;
import artisynth.core.util.ScanToken;

public class FixedMeshBody extends MeshComponent implements HasCoordinateFrame {

   // use a FrameState to store the position even though we ignore velocity
   FrameState myState = new FrameState();
   // double buffer the frame for rendering:
   protected RigidTransform3d myRenderFrame = new RigidTransform3d();

   protected static final double DEFAULT_AXIS_LENGTH = 0;
   double myAxisLength = DEFAULT_AXIS_LENGTH;
   protected static final AxisDrawStyle DEFAULT_AXIS_RENDER_STYLE =
      AxisDrawStyle.LINE;
   protected AxisDrawStyle myAxisDrawStyle = DEFAULT_AXIS_RENDER_STYLE;

   public static PropertyList myProps =
      new PropertyList (FixedMeshBody.class, MeshComponent.class);

   static {
      myProps.add ("pose * *", "pose state", null, "NE NW");
      myProps.add ("position", "position of the body coordinate frame",null,"NW");
      myProps.add (
         "orientation", "orientation of the body coordinate frame", null, "NW");
      myProps.add (
         "axisLength * *", "length of rendered frame axes", DEFAULT_AXIS_LENGTH);
      myProps.add (
         "axisDrawStyle", "rendering style for the frame axes",
         DEFAULT_AXIS_RENDER_STYLE);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setPose (RigidTransform3d XFrameToWorld) {
      myState.setPose (XFrameToWorld);
      updatePosState();
   }

   public RigidTransform3d getPose() {
      return myState.XFrameToWorld;
   }

   public void getPose (RigidTransform3d XFrameToWorld) {
      myState.getPose (XFrameToWorld);
   }

   public Point3d getPosition() {
      return new Point3d (myState.XFrameToWorld.p);
   }

   public void setPosition (Point3d pos) {
      myState.setPosition (pos);
      updatePosState();
   }

   public AxisAngle getOrientation() {
      return myState.getAxisAngle();
   }

   public void setOrientation (AxisAngle axisAng) {
      RigidTransform3d X = new RigidTransform3d (myState.XFrameToWorld);
      X.R.setAxisAngle (axisAng);
      setPose (X);
   }

   public Quaternion getRotation() {
      return myState.getRotation();
   }

   public void setRotation (Quaternion q) {
      myState.setRotation (q);
      updatePosState();
   }

   public double getAxisLength() {
      return myAxisLength;
   }

   public void setAxisLength (double len) {
      myAxisLength = Math.max (0, len);
   }

   public AxisDrawStyle getAxisDrawStyle() {
      return myAxisDrawStyle;
   }

   public void setAxisDrawStyle (AxisDrawStyle style) {
      myAxisDrawStyle = style;
   }

   public FixedMeshBody () {
      super();
   }
   
   public FixedMeshBody(String name) {
      this();
      setName(name);
   }

   public FixedMeshBody (MeshBase mesh) {
      this();
      setMesh (mesh);
   }
   
   public FixedMeshBody(String name, MeshBase mesh) {
      this(name);
      setMesh(mesh);
   }

   public void setMeshFromFile (MeshBase mesh, String fileName) throws IOException {
      setMeshFromFile (mesh, fileName, null);
   }

   public void setMeshFromFile (
      MeshBase mesh, String fileName, AffineTransform3dBase X) throws IOException {
      
      mesh.read (new BufferedReader(new FileReader(new File(fileName))));
      if (X != null && !X.isIdentity()) {
         mesh.transform (X);
      }
      setMesh (mesh, fileName, X);            
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myState.scaleDistance (s);
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setMeshToWorld (myState.XFrameToWorld);
      }
   }

   /**
    * Sets a mesh for this body.
    */
   public void setMesh (
      MeshBase mesh, String fileName, AffineTransform3dBase X) {
      super.setMesh (mesh, fileName, X);
      if (mesh != null) {
         mesh.setFixed (true);
         mesh.setMeshToWorld (myState.XFrameToWorld);
      }
   }   

   protected void updatePosState () {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setMeshToWorld (myState.XFrameToWorld);
      }         
   }
   
   public void scaleMesh (double sx, double sy, double sz) {
      myMeshInfo.scale (sx, sy, sz);
   }
   
   public void scaleMesh (double s) {
      scaleMesh (s, s, s);
   }

   public void transformMesh (AffineTransform3dBase X) {
      getMesh().transform (X);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      // transform the pose
      RigidTransform3d Xpose = new RigidTransform3d();
      Xpose.set (myState.XFrameToWorld);
      gtr.transform (Xpose);
      myMeshInfo.transformGeometryAndPose (gtr, null);
      setPose (Xpose);      
   }   

   public void scan (ReaderTokenizer rtok, Object ref) throws IOException {
      super.scan (rtok, ref);
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setFixed (true);
         mesh.setMeshToWorld (myState.XFrameToWorld);         
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "mesh")) {
         myMeshInfo.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "position")) {
         Point3d pos = new Point3d();
         pos.scan (rtok);
         setPosition (pos);
         return true;
      }
      else if (scanAttributeName (rtok, "rotation")) {
         Quaternion q = new Quaternion();
         q.scan (rtok);
         setRotation (q);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.println ("position=[ " + myState.getPosition().toString (fmt) + "]");
      pw.println ("rotation=[ " + myState.getRotation().toString (fmt) + "]");
   }

   // overrides of the Renderable methods so we can draw axes

   public void prerender (RenderList list) {
      super.prerender (list);
      myRenderFrame.set (myState.XFrameToWorld);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      myMeshInfo.myMesh.updateBounds (pmin, pmax);
      RenderableUtils.updateFrameBounds (pmin, pmax, getPose(), myAxisLength);
   }

   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      if (myAxisLength > 0) {
         Frame.renderAxes (
            renderer, myRenderFrame, myAxisDrawStyle, 
            myAxisLength, myRenderProps.getLineWidth(), isSelected());
      }
   }

}
