/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.awt.Color;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Deque;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.properties.Property;
import maspack.properties.PropertyList;
import maspack.properties.PropertyMode;
import maspack.render.Renderer;
import maspack.render.HasRenderProps;
import maspack.render.RenderProps;
import maspack.render.Renderer.LineStyle;
import maspack.util.*;
import artisynth.core.driver.Main;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.util.*;

public class VectorTracingProbe extends TracingProbe {
   ModelComponent myComponent;
   Property myRefPositionProp;
   double myLengthScaling = 1d / 10d;
   boolean renderAsPush = true;

   VectorNd tmpVec = new VectorNd (3);
   Vector3d vec = new Vector3d();
   Point3d startpt = new Point3d();
   Point3d endpt = new Point3d();
   float[] startCoords = new float[3];
   float[] endCoords = new float[3];

   public static PropertyList myProps =
      new PropertyList (VectorTracingProbe.class, TracingProbe.class);

   static {
      myProps.remove ("renderInterval");
      myProps.add (
         "lengthScaling", "scaling from vector units to length for rendering",
         1.0);
      myProps.add("renderAsPush", "draw arrow to end at reference point to look like a pushing vector, otherwise start arrow at reference point to look like a pulling vector", 
         true);
   }

   public double getLengthScaling() {
      return myLengthScaling;
   }

   public void setLengthScaling (double lengthScaling) {
      this.myLengthScaling = lengthScaling;
   }
   
   public boolean getRenderAsPush() {
      return renderAsPush;
   }
   
   public void setRenderAsPush(boolean push) {
      renderAsPush = push;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public RenderProps createRenderProps() {
      return RenderProps.createLineProps (this);
   }

   private void initRenderProps (ModelComponent comp) {
      RenderProps newProps = createRenderProps();
      if (comp instanceof HasRenderProps) {
         HasRenderProps rcomp = (HasRenderProps)comp;
         RenderProps compProps = rcomp.getRenderProps();
         if (compProps == null) {
            compProps = rcomp.createRenderProps();
         }
         newProps.set (compProps);
         // if component point color is explicitly set, set the
         // line and point colors of render props to match
         if (compProps.getPointColorMode() == PropertyMode.Explicit) {
            float[] pointColor = compProps.getPointColorF();
            newProps.setPointColor (pointColor);
            newProps.setLineColor (pointColor);
         }
      }
      newProps.setLineWidth (3);
      newProps.setLineStyle (LineStyle.CYLINDER);
      setRenderProps (newProps);
   }

   public VectorTracingProbe() {
      super();
      createNumericList (3);
   }

   public VectorTracingProbe (ModelComponent comp, Property vectorProp,
   Property referencePositionProp, double interval) {
      // vector prop is the property used by the numeric output probe
      super (vectorProp, interval);
      myComponent = comp;
      // ref pos property is used to get the render origin for the vector
      myRefPositionProp = referencePositionProp;
      initRenderProps (comp);
      if (getVsize() != 3) {
         throw new IllegalArgumentException ("property '"
         + vectorProp.getName() + "' does not map to a 3-tuple");
      }
      if (myRefPositionProp == null ||
          !(myRefPositionProp.get() instanceof Point3d)) {
         throw new IllegalArgumentException ("reference position property '"
         + referencePositionProp.getName() + "' does not map to a Point3d");
      }
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      startpt.updateBounds (pmin, pmax);
      endpt.updateBounds (pmin, pmax);
   }

   public void render (Renderer renderer, int flags) {

      double t = Main.getMain().getTime();

      if (myNumericList.getLast() == null ||
          myRefPositionProp == null ||
          t < getStartTime() || t > getStopTime()) {
         return;
      }

      myNumericList.interpolate (
         tmpVec, t, 
         myNumericList.getInterpolation().getOrder(), true/* extend data */,
         myNumericList.getLast());
      vec.set (tmpVec);
      vec.scale (myLengthScaling);

      if (renderAsPush) {
         endpt = (Point3d)myRefPositionProp.get();
         startpt.sub (endpt, vec);         
      }
      else {
         startpt = (Point3d)myRefPositionProp.get();
         endpt.add (startpt, vec);
      }

      set (startCoords, startpt);
      set (endCoords, endpt);

      renderer.drawArrow (
         myRenderProps, startCoords, endCoords, true/* capped */, isSelected());
      // renderer.drawLine (myRenderProps, endCoords, startCoords,
      // true/*capped*/, isSelected());
   }

   private void set (float[] dest, Vector3d src) {
      dest[0] = (float)src.get (0);
      dest[1] = (float)src.get (1);
      dest[2] = (float)src.get (2);
   }

   public Object clone() throws CloneNotSupportedException {
      VectorTracingProbe probe = (VectorTracingProbe)super.clone();
      probe.setRenderProps (createRenderProps());
      return probe;
   }

   @Override
   public boolean scanItem (
      ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {

      rtok.nextToken();
      if (ScanWriteUtils.scanAndStorePropertyPath (rtok, "refPositionProp", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "refPositionProp")) {
         myRefPositionProp = ScanWriteUtils.postscanProperty (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   @Override
   public void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      // TODO Auto-generated method stub
      super.writeItems (pw, fmt, ancestor);
      if (myRefPositionProp != null) {
         pw.print ("refPositionProp=");
         pw.println (ComponentUtils.getWritePropertyPathName (
            myRefPositionProp, ancestor));
      }

   }

}
