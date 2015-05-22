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
import maspack.matrix.AffineTransform3d;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.AxisAngle;
import maspack.matrix.Point3d;
import maspack.matrix.Quaternion;
import maspack.matrix.RigidTransform3d;
import maspack.properties.PropertyList;
import maspack.render.GLRenderer;
import maspack.render.RenderList;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.util.ScanToken;
import artisynth.core.util.TransformableGeometry;

public class FixedMesh extends MeshComponent {

   // use a FrameState to store the position even though we ignore velocity
   FrameState myState = new FrameState();

   public static PropertyList myProps =
      new PropertyList (FixedMesh.class, MeshComponent.class);

   static {
      myProps.add ("pose * *", "pose state", null, "NE NW");
      myProps.add ("position", "position of the body coordinate frame",null,"NW");
      myProps.add (
         "orientation", "orientation of the body coordinate frame", null, "NW");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public void setPose (RigidTransform3d XFrameToWorld) {
      myState.setPose (XFrameToWorld);
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
   }

   public FixedMesh () {
      super();
   }
   
   public FixedMesh(String name) {
      this();
      setName(name);
   }

   public FixedMesh (MeshBase mesh) {
      this();
      setMesh (mesh);
   }
   
   public FixedMesh(String name, MeshBase mesh) {
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

   public void updateBounds (Point3d pmin, Point3d pmax) {
      myMeshInfo.myMesh.updateBounds (pmin, pmax);
   }

   public void scaleDistance (double s) {
      super.scaleDistance (s);
      myState.scaleDistance (s);
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setMeshToWorld (myState.XFrameToWorld);
         myRenderProps.clearMeshDisplayList();
      }
   }

   /**
    * Sets a mesh for this body.
    */
   public void setMesh (MeshBase mesh, String fileName, AffineTransform3dBase X) {
      super.setMesh (mesh, fileName, X);
      if (mesh != null) {
         mesh.setFixed (true);
         mesh.setMeshToWorld (myState.XFrameToWorld);
      }
   }   

   public void updatePosState (int flags) {
      MeshBase mesh = getMesh();
      if (mesh != null) {
         mesh.setMeshToWorld (myState.XFrameToWorld);
      }         
   }

   public void transformMesh (AffineTransform3dBase X) {
      getMesh().transform (X);
      if (myRenderProps != null) {
         myRenderProps.clearMeshDisplayList();
      }
   }

   public void transformGeometry (
      AffineTransform3dBase X, TransformableGeometry topObject, int flags) {

      RigidTransform3d Xpose = new RigidTransform3d();
      AffineTransform3d Xlocal = new AffineTransform3d();
      Xpose.set (myState.XFrameToWorld);
      if (myMeshInfo.transformGeometry (X, Xpose, Xlocal)) {
         // mesh was transformed in addition to having its transform set
         // so clear the display list (if set)
         if (myRenderProps != null) {
            myRenderProps.clearMeshDisplayList();
         }
      }
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


}
