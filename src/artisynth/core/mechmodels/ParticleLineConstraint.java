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

import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.TransformGeometryContext;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.util.ScalableUnits;
import artisynth.core.util.ScanToken;
import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;
import maspack.matrix.Line;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.properties.HasProperties;
import maspack.properties.PropertyList;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.Shading;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;

public class ParticleLineConstraint extends ParticleConstraintBase
   implements ScalableUnits, TransformableGeometry {

   Vector3d myDir;
   Point3d myOrigin;

   private Point3d[] myRenderVtxs;
   private double myLineSize = defaultLineSize;
   private static final double defaultLineSize = 1;

   protected static RenderProps defaultRenderProps (HasProperties host) {
      RenderProps props = RenderProps.createLineProps (null);
      return props;
   }

   public static PropertyList myProps =
      new PropertyList (
         ParticleLineConstraint.class, ParticleConstraintBase.class);

   static {
      myProps.add ("renderProps", "render properties", defaultRenderProps(null));
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public ParticleLineConstraint () {
      myDir = new Vector3d();
      myOrigin = new Point3d();
      myRenderVtxs = new Point3d[2];
      for (int i = 0; i < myRenderVtxs.length; i++) {
         myRenderVtxs[i] = new Point3d();
      }
      myRenderProps = createRenderProps();
      myParticleInfo = new ArrayList<ParticleInfo>();
   }

   public ParticleLineConstraint (Line line) {
      this();
      myDir.set (line.getDirection());
      myOrigin.set (line.getOrigin());
   }

   public ParticleLineConstraint (Vector3d dir, Point3d origin) {
      this();
      Vector3d d = new Vector3d (dir);
      d.normalize();
      myDir.set (d);
      myOrigin.set (origin);
   }

   public ParticleLineConstraint (Particle p, Line line) {
      this (line);
      addParticle (p);
   }

   public ParticleLineConstraint (Particle p, Vector3d dir, Point3d center) {
      this (dir, center);
      addParticle (p);
   }

   public Point3d getOrigin() {
      return new Point3d (myOrigin);
   }

   public Vector3d getDirection() {
      return new Vector3d (myDir);
   }

   public double getLineSize() {
      return myLineSize;
   }

   public void setLineSize (double len) {
      myLineSize = len;
   }

   protected ParticleInfo createParticleInfo (Particle p) {
      return new ParticleInfo (p);
   }

   public double updateConstraints (double t, int flags) {

      boolean setEngaged = ((flags & MechSystem.UPDATE_CONTACTS) == 0);
      double maxpen = 0;
      Vector3d d = new Vector3d();
      for (int i=0; i<myParticleInfo.size(); i++) {
         ParticleInfo pi = myParticleInfo.get(i);
         d.sub(pi.myPart.getPosition(), myOrigin);
         double a = d.dot(myDir);
         d.scaledAdd(a, myDir, myOrigin);
         d.sub(pi.myPart.getPosition());
         d.negate();
         
         a = d.norm();
         if (a > 0) {
            d.scale(1.0/a);
         } else {
            d.set(0,0,1);
         }
         
         pi.myBlk.set (d);
         pi.myDist = a;

         if (setEngaged && myUnilateralP) {
            maxpen = updateEngagement (pi, maxpen);
         }
      }
      return maxpen;
   }

   protected void computeRenderVtxs () {
      myRenderVtxs[0].scaledAdd(-myLineSize/2, myDir, myOrigin);
      myRenderVtxs[1].scaledAdd( myLineSize/2, myDir, myOrigin);
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

      if (myLineSize > 0) {
         computeRenderVtxs ();

         RenderProps props = myRenderProps;

         Shading savedShading = renderer.setPropsShading (props);
         renderer.setLineColoring (props, isSelected());
         renderer.drawLine(myRenderVtxs[0], myRenderVtxs[1]);         
         renderer.setShading (savedShading);

      }
   }
   
   public void scaleMass (double s) {
   }

   public void scaleDistance (double s) {
      myOrigin.scale(s);
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
      
      // Line line = new Line (myOrigin, myDir);
      gtr.transformPnt (myOrigin);
      gtr.transformVec(myDir, Vector3d.ZERO);

   }  
   
   public void addTransformableDependencies (
      TransformGeometryContext context, int flags) {
      // no dependencies
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "origin")) {
         myOrigin.scan (rtok);
         return true;
      }
      else if (scanAttributeName (rtok, "direction")) {
         myDir.scan (rtok);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);      
      pw.println ("direction=[" + myDir.toString (fmt) + "]");
      pw.println ("origin=[" + myOrigin.toString (fmt) + "]");
   }

}     
