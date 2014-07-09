/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import java.awt.Color;

import artisynth.core.modelbase.*;
import artisynth.core.util.TimeBase;
import maspack.render.*;
import maspack.properties.*;
import maspack.matrix.Point3d;
import maspack.matrix.VectorNd;
import maspack.interpolation.*;

import java.util.*;

public class PointTracingProbe extends TracingProbe {
   ModelComponent myComponent;
   boolean myPointTracing = false;

   public static PropertyList myProps =
      new PropertyList (PointTracingProbe.class, TracingProbe.class);

   static {
      myProps.add ("pointTracing", "trace as points vs. lines", false);
   }

   public boolean getPointTracing() {
      return myPointTracing;
   }

   public void setPointTracing (boolean enable) {
      myPointTracing = enable;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public RenderProps createRenderProps() {
      return RenderProps.createPointLineProps (this);
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
            Color pointColor = compProps.getPointColor();
            newProps.setPointColor (pointColor);
            newProps.setLineColor (pointColor);
         }
      }
      newProps.setLineWidth (3);
      setRenderProps (newProps);
   }

   private class VertexIterator implements Iterator<float[]> {
      double myTime = 0;
      double myStep;
      double myEndTime;
      NumericListKnot myKnot = null;
      VectorNd myVec = new VectorNd (3);
      double[] myBuf = myVec.getBuffer();
      float[] myVtx = new float[3];

      public boolean hasNext() {
         return (myTime <= myEndTime);
      }

      public float[] next() {
         if (myTime > myEndTime) {
            throw new NoSuchElementException();
         }
         myKnot =
            myNumericList.interpolate (
               myVec, myTime, getInterpolation(), myKnot);
         myVtx[0] = (float)myBuf[0];
         myVtx[1] = (float)myBuf[1];
         myVtx[2] = (float)myBuf[2];
         if (myTime < myEndTime) {
            myTime = Math.min (myTime + myStep, myEndTime);
         }
         else {
            myTime = myEndTime + 1;
         }
         return myVtx;
      }

      public void remove() {
         throw new UnsupportedOperationException();
      }

      VertexIterator (double step, double tend) {
         myStep = step;
         myEndTime = tend;
      }
   }

   private class VertexIterable implements Iterable<float[]> {
      double myEndTime;

      VertexIterable (double endTime) {
         myEndTime = endTime;
      }

      public Iterator<float[]> iterator() {
         double step = myTraceInterval / myScale;
         return new VertexIterator (step, myEndTime);
      }
   }

   public PointTracingProbe() {
      super();
      createNumericList (3);
   }

   public PointTracingProbe (
      ModelComponent comp, Property prop, double interval) {
      super (prop, interval);
      myComponent = comp;
      initRenderProps (comp);
      if (getVsize() != 3) {
         throw new IllegalArgumentException ("property '" + prop.getName()
         + "' does not map to a 3-tuple");
      }
   }

   public void updateBounds (Point3d pmin, Point3d pmax) {
      if (myNumericList == null) {
         return;
      }
      Point3d pnt = new Point3d();
      for (NumericListKnot knot : myNumericList) {
         VectorNd v = knot.v;
         pnt.x = v.get (0);
         pnt.y = v.get (1);
         pnt.z = v.get (2);
         pnt.updateBounds (pmin, pmax);
      }
   }

   public void render (GLRenderer renderer, int flags) {
      if (myNumericList == null || myNumericList.isEmpty()) {
         return;
      }
      double tend = myNumericList.getLast().t;
      VertexIterable vi = new VertexIterable (tend);
      if (myPointTracing) {
         for (float[] vtx : vi) {
            renderer.drawPoint (myRenderProps, vtx, isSelected());
         }
      }
      else {
         GLViewer.drawLineStrip (renderer, vi, myRenderProps, isSelected());
      }
   }

   public Object clone() throws CloneNotSupportedException {
      PointTracingProbe probe = (PointTracingProbe)super.clone();
      probe.setRenderProps (createRenderProps());
      return probe;
   }

}
