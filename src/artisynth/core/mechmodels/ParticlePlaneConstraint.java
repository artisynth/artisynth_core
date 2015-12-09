/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import javax.media.opengl.*;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.*;
import maspack.properties.*;
import maspack.util.*;
import maspack.spatialmotion.*;
import maspack.render.*;
import maspack.render.GL.GLViewer;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.MechSystem.ConstraintInfo;
import artisynth.core.util.*;

public class ParticlePlaneConstraint extends ParticleConstraintBase
   implements ScalableUnits, TransformableGeometry {

   Vector3d myNrm;
   double myOff;
   Point3d myCenter;

   private Point3d[] myRenderVtxs;
   private double myPlaneSize = defaultPlaneSize;
   private static final double defaultPlaneSize = 1;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createFaceProps (null);
      props.setFaceStyle (RenderProps.Faces.FRONT_AND_BACK);
      return props;
   }

   public static PropertyList myProps =
      new PropertyList (
         ParticlePlaneConstraint.class, ParticleConstraintBase.class);

   static {
      myProps.add ("renderProps", "render properties", defaultRenderProps(null));
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ParticlePlaneConstraint () {
      myNrm = new Vector3d();
      myCenter = new Point3d();
      myRenderVtxs = new Point3d[4];
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i] = new Point3d();
      }
      myRenderProps = createRenderProps();
      myParticleInfo = new ArrayList<ParticleInfo>();
   }

   public ParticlePlaneConstraint (Plane plane) {
      this();
      myNrm.set (plane.normal);
      myOff = plane.offset;
      myCenter.scale (myOff, plane.normal);
   }

   public ParticlePlaneConstraint (Vector3d nrml, Point3d center) {
      this();
      Vector3d n = new Vector3d (nrml);
      n.normalize();
      myNrm.set (n);
      myCenter.set (center);
      myOff = n.dot (myCenter);
   }

   public ParticlePlaneConstraint (Particle p, Plane plane) {
      this (plane);
      addParticle (p);
   }

   public ParticlePlaneConstraint (Particle p, Vector3d nrml, Point3d center) {
      this (nrml, center);
      addParticle (p);
   }

   public Point3d getCenter() {
      return new Point3d (myCenter);
   }

   public Vector3d getNormal() {
      return new Vector3d (myNrm);
   }

   public double getPlaneSize() {
      return myPlaneSize;
   }

   public void setPlaneSize (double len) {
      myPlaneSize = len;
   }

   protected ParticleInfo createParticleInfo (Particle p) {
      return new ParticleInfo (p);
   }

   public double updateConstraints (double t, int flags) {

      boolean setEngaged = ((flags & MechSystem.UPDATE_CONTACTS) == 0);
      double maxpen = 0;

      for (int i=0; i<myParticleInfo.size(); i++) {
         ParticleInfo pi = myParticleInfo.get(i);

         pi.myBlk.set (myNrm);
         pi.myDist = myNrm.dot (pi.myPart.getPosition()) - myOff;

         if (setEngaged && myUnilateralP) {
            maxpen = updateEngagement (pi, maxpen);
         }
      }
      return maxpen;
   }

   protected void computeRenderVtxs () {
      RotationMatrix3d RPD = new RotationMatrix3d();
      RPD.setZDirection (myNrm);
      myRenderVtxs[0].set (myPlaneSize / 2, myPlaneSize / 2, 0);
      myRenderVtxs[1].set (-myPlaneSize / 2, myPlaneSize / 2, 0);
      myRenderVtxs[2].set (-myPlaneSize / 2, -myPlaneSize / 2, 0);
      myRenderVtxs[3].set (myPlaneSize / 2, -myPlaneSize / 2, 0);
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i].transform (RPD);
         myRenderVtxs[i].add (myCenter);
      }
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      computeRenderVtxs ();
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i].updateBounds (pmin, pmax);
      }
   }

   public RenderProps createRenderProps() {
      return defaultRenderProps (this);
   }

   public void prerender (RenderList list) {
      super.prerender (list);
   }

   public void render (Renderer renderer, int flags) {

      if (!(renderer instanceof GLViewer)) {
         return;
      }
      GLViewer viewer = (GLViewer)renderer;

      if (myPlaneSize > 0) {
         computeRenderVtxs ();

         GL2 gl = viewer.getGL2().getGL2();
         RenderProps props = myRenderProps;

         renderer.setMaterialAndShading (
            props, props.getFaceMaterial(), isSelected());
         renderer.setFaceMode (props.getFaceStyle());
         gl.glBegin (GL2.GL_POLYGON);
         gl.glNormal3d (myNrm.x, myNrm.y, myNrm.z);
         for (int i = 0; i < myRenderVtxs.length; i++) {
            Point3d p = myRenderVtxs[i];
            gl.glVertex3d (p.x, p.y, p.z);
         }
         gl.glEnd();
         renderer.restoreShading (props);
         renderer.setDefaultFaceMode();
      }
   }
   
   public void scaleMass (double s) {
   }

   public void scaleDistance (double s) {
      myOff *= s;
   }

   public void transformGeometry (AffineTransform3dBase X) {
      TransformGeometryContext.transform (this, X, 0);
   }

   public void transformGeometry (
      GeometryTransformer gtr, TransformGeometryContext context, int flags) {

      // Simply return if this constraint is not being transformed. This
      // could happen if (a) this class were to register with each particle
      // using their addConstrainer() methods, and then (b) one of the
      // particles was transformed, hence invoking a call to this method.
      if (!context.contains(this)) {
         return;
      }
      
      Plane plane = new Plane (myNrm, myOff);
      gtr.transformPnt (myCenter);
      gtr.transform (plane, myCenter);
      myNrm.set (plane.normal);
      myOff = myNrm.dot(myCenter);
   }  
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "center")) {
         myCenter.scan (rtok);
         myOff = myNrm.dot(myCenter);
         return true;
      }
      else if (scanAttributeName (rtok, "normal")) {
         myNrm.scan (rtok);
         myOff = myNrm.dot(myCenter);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);      
      pw.println ("normal=[" + myNrm.toString (fmt) + "]");
      pw.println ("center=[" + myCenter.toString (fmt) + "]");
   }

}     
