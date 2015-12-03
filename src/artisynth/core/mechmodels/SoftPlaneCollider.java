/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.render.*;

import java.util.*;

import maspack.matrix.*;
import maspack.geometry.*;
import maspack.properties.*;

import java.io.*;

import maspack.render.*;
import maspack.util.*;

import javax.media.opengl.*;

import java.awt.Color;

public class SoftPlaneCollider extends RenderableComponentBase implements
ScalableUnits, ForceComponent, TransformableGeometry {
   Plane myPlane = new Plane();
   double myStiffness;
   double myDamping;
   protected Point3d myCenter = new Point3d();
   protected Vector3d myNormal = new Vector3d();
   protected double mySize = 50;
   protected ArrayList<MechModel> myModels;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createMeshProps (host);
      props.setFaceColor (new Color (0.5f, 0.5f, 0.5f));
      props.setAlpha (0.8);
      props.setShininess (32);
      return props;
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public static PropertyList myProps =
      new PropertyList (SoftPlaneCollider.class, RenderableComponentBase.class);

   static {
      myProps.add (
         "renderProps * *", "renderer properties", defaultRenderProps (null));
      myProps.add ("stiffness", "spring stiffness", 0);
      myProps.add ("damping", "spring damping", 0);
      myProps.add ("size", "size of the plane for rendering", 1);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public SoftPlaneCollider() {
      super();
      myCenter = new Point3d();
      myNormal = new Vector3d();
      myModels = new ArrayList<MechModel>();
      setRenderProps (defaultRenderProps (null));
   }

   public SoftPlaneCollider (String name, Vector3d normal, Point3d center,
   double k, double d) {
      this();
      setName (name);
      setCenter (center);
      setNormal (normal);
      myStiffness = k;
      myDamping = d;
   }

   public void addMechModel (MechModel ms) {
      myModels.add (ms);
   }

   public void removeMechModel (MechModel ms) {
      myModels.remove (ms);
   }

   public void setRenderPosition (Point3d center, double size) {
      setCenter (center);
      setSize (size);
   }

   public void setCenter (Point3d center) {
      myCenter.set (center);
   }

   public Point3d getCenter() {
      return myCenter;
   }

   public void setNormal (Vector3d normal) {
      myNormal.set (normal);
   }

   public Vector3d getNormal() {
      return myNormal;
   }

   public void setSize (double size) {
      mySize = size;
   }

   public double getSize() {
      return mySize;
   }

   public void setStiffness (double stiffness) {
      myStiffness = stiffness;
   }

   public double getStiffness() {
      return myStiffness;
   }

   public void setDamping (double damping) {
      myDamping = damping;
   }

   public double getDamping() {
      return myDamping;
   }

   public void applyForces (double t) {
      myPlane.set (myNormal, myCenter);
      for (MechModel ms : myModels) {
         for (Particle part : ms.particles()) {
            double dist = myPlane.distance (part.getPosition());
            Vector3d nrml = myPlane.getNormal();
            if (dist < 0) {
               double v = part.getVelocity().dot (nrml);
               part.addScaledForce (-myStiffness * dist - myDamping * v, nrml);
               // extForce.scaledAdd (-myStiffness*dist - myDamping*v,
               // nrml, extForce);
            }
         }
      }
   }

   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
   }

   public void addVelJacobian (SparseNumberedBlockMatrix M, double h) {
   }

   public void addPosJacobian (SparseNumberedBlockMatrix M, double h) {
   }

   public int getJacobianType() {
      return Matrix.SPD;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      pw.print ("models=");
      ScanWriteUtils.writeBracketedReferences (pw, myModels, ancestor);
      pw.println ("normal=[" + myNormal.toString (fmt) + "]");
      pw.println ("center=[" + myCenter.toString (fmt) + "]");
      super.writeItems (pw, fmt, ancestor);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "normal")) {
         myNormal.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "center")) {
         myCenter.scan (rtok);
         return true;
      }
      else if (scanAndStoreReferences (rtok, "models", tokens) >= 0) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }


   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "models")) {
         myModels.clear();
         postscanReferences (tokens, myModels, MechModel.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   public void updateBounds (Point3d pmin, Point3d pmax) {
      myCenter.updateBounds (pmin, pmax);
   }

   public void render (GLRenderer renderer, int flags) {
      GL2 gl = renderer.getGL2().getGL2();
      // GLU glu = renderer.getGLU();

      Point3d renderCenter = new Point3d();
      myPlane.set (myNormal, myCenter);
      myPlane.project (renderCenter, myCenter);

      RigidTransform3d X = new RigidTransform3d();
      Vector3d nrml = myPlane.getNormal();
      X.R.setZDirection (nrml);
      X.p.set (renderCenter);
      gl.glPushMatrix();
      GLViewer.mulTransform (gl, X);

      
      if (!renderer.isSelecting()) {
    	  gl.glEnable (GL2.GL_BLEND);
          gl.glBlendFunc (GL2.GL_SRC_ALPHA, GL2.GL_ONE);
      }
      gl.glDepthMask (false);
      gl.glDisable (GL2.GL_CULL_FACE);

      renderer.setMaterialAndShading (myRenderProps, myRenderProps.getFaceMaterial(), false);
      gl.glBegin (GL2.GL_POLYGON);
      gl.glNormal3d (nrml.x, nrml.y, nrml.z);
      gl.glVertex3d (mySize, mySize, 0);
      gl.glVertex3d (-mySize, mySize, 0);
      gl.glVertex3d (-mySize, -mySize, 0);
      gl.glVertex3d (mySize, -mySize, 0);
      gl.glEnd();
      renderer.restoreShading (myRenderProps);

      gl.glEnable (GL2.GL_CULL_FACE);
      gl.glDepthMask (true);
      
      if (!renderer.isSelecting()) {
    	  gl.glDisable (GL2.GL_BLEND);
      }

      gl.glPopMatrix();
   }

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {
      
      Plane plane = new Plane (myNormal, myCenter.dot(myNormal));
      gtr.transform (plane, myCenter);
      myNormal.set (plane.getNormal());
      gtr.transformPnt (myCenter);
      // should also update size, based on uniform scaling
   }
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   public void scaleDistance (double s) {
      myCenter.scale (s);
      mySize *= s;
   }

   public void scaleMass (double s) {
      myStiffness *= s;
      myDamping *= s;
   }

}
