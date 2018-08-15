/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Deque;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Plane;
import maspack.matrix.Point3d;
import maspack.matrix.RotationMatrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.DrawMode;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;

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
      props.setFaceStyle (Renderer.FaceStyle.FRONT_AND_BACK);
      return props;
   }

   public static PropertyList myProps =
      new PropertyList (
         ParticlePlaneConstraint.class, ParticleConstraintBase.class);

   static {
      myProps.add (
         "offset", "offset from center of the plane in normal direction", 0);
      myProps.add ("renderProps", "render properties", defaultRenderProps(null));
      myProps.add ("planeSize", "plane size", defaultPlaneSize);
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

   public void setPlane(Plane p) {
      myNrm.set (p.normal);
      myOff = p.getOffset();
      myCenter.scale(myOff, myNrm);
   }
   
   public Plane getPlane() {
      return new Plane(myNrm, myOff);
   }
   
   public void setOffset(double off) {
      double ooff = myOff;
      myOff = off;
      myCenter.scaledAdd(off-ooff, myNrm);
      updateConstraints (0, 0);
   }
   
   public double getOffset() {
      return myOff;
   }

   public Point3d getCenter() {
      return new Point3d (myCenter);
   }
   
   public void setCenter(Point3d c) {
      myCenter.set (c);
      myOff = c.dot (myNrm);
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

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
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

      if (myPlaneSize > 0) {
         computeRenderVtxs ();

         RenderProps props = myRenderProps;

         Shading savedShading = renderer.setPropsShading (props);
         renderer.setFaceColoring (props, isSelected());
         renderer.setFaceStyle (props.getFaceStyle());

         renderer.beginDraw (DrawMode.TRIANGLE_STRIP);
         renderer.setNormal (myNrm.x, myNrm.y, myNrm.z);
         renderer.addVertex (myRenderVtxs[3]);
         renderer.addVertex (myRenderVtxs[0]);
         renderer.addVertex (myRenderVtxs[2]);
         renderer.addVertex (myRenderVtxs[1]);
         renderer.endDraw();

         renderer.setShading (savedShading);
         renderer.setFaceStyle (FaceStyle.FRONT); // set default
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
