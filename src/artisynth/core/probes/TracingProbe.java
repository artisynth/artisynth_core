/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.probes;

import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.modelbase.RenderableComponentBase;
import artisynth.core.modelbase.Traceable;
import maspack.render.*;
import maspack.properties.*;
import maspack.util.*;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;

import java.util.*;

public abstract class TracingProbe extends NumericOutputProbe implements
RenderableComponent {
   protected RenderProps myRenderProps = null;
   protected double myTraceInterval = 0.05;

   public static PropertyList myProps =
      new PropertyList (TracingProbe.class, NumericOutputProbe.class);

   static {
      myProps.add ("renderProps * *", "render properties", null);
      myProps.add ("renderInterval", "time between trace points", 20);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getRenderInterval() {
      return myTraceInterval;
   }

   public void setRenderInterval (double h) {
      myTraceInterval = h;
   }

   public TracingProbe() {
      super();
   }

   public TracingProbe (Property[] props, double interval, double ymin,
                        double ymax) {
      super (props, interval, ymin, ymax);
   }

   public TracingProbe (Property[] props, double interval) {
      this (props, interval, 0, 0);
   }

   public TracingProbe (Property prop, double interval) {
      this (new Property[] { prop }, interval, 0, 0);
   }

   /**
    * Returns true if this tracing probe is tracing the single property
    * <code>propName</code> associated with the specified host.
    * If <code>host</code> is <code>null</code>, then
    * the any host will match.
    */
   public boolean isTracing (HasProperties host, String propName) {
      if (myPropList.size() != 1) {
         return false;
      }
      Property prop = myPropList.get(0);
      if (!prop.getName().equals (propName)) {
         return false;
      }
      if (host != null && prop.getHost() != host) {
         return false;
      }
      return true;
   }

   /**
    * Returns the host associated with the <code>idx</code>-th property
    * in this probe, or null if there is no such property.
    */
   public HasProperties getHost (int idx) {
      if (idx >= myPropList.size()) {
         return null;
      }
      else {
         return myPropList.get(idx).getHost();
      }
   }

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public void setRenderProps (RenderProps props) {
      myRenderProps =
         RenderableComponentBase.updateRenderProps (this, myRenderProps, props);
   }

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   public void prerender (RenderList list) {
   }

   public abstract void updateBounds (Vector3d pmin, Vector3d pmax);

   public abstract void render (Renderer renderer, int flags);

   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return -1;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= TRANSPARENT;
      }
      return code;
   }

   static public TracingProbe create (Traceable host, String traceableName) {
      Property prop = host.getProperty (traceableName);
      if (prop == null) {
         throw new InternalErrorException (
            "Property '"+traceableName+"' not found in Traceable " + host);
      }
      Class<?> valueClass = prop.getInfo().getValueClass();
      if (Point3d.class.isAssignableFrom (valueClass)) {
         // property is point - use a point tracing probe
         return new PointTracingProbe (host, prop, 1.0);
      }
      else {
         // assume that the probe is a vector probe
         String refPosName = host.getTraceablePositionProperty (traceableName);
         if (refPosName == null) {
            throw new InternalErrorException (
               "No position property found for '"+traceableName+"'");
         }
         boolean renderAsPush = true;
         if (refPosName.startsWith ("-")) {
            refPosName = refPosName.substring (1);
         }
         else if (refPosName.startsWith ("+")) {
            refPosName = refPosName.substring (1);
            renderAsPush = false;
         }
         Property refPosProp = host.getProperty (refPosName);
         if (refPosProp == null) {
            throw new InternalErrorException (
               "Property '"+refPosName+"' not found in Traceable " + host);
         }       
         VectorTracingProbe prb =
            new VectorTracingProbe (host, prop, refPosProp, 1.0);
         prb.setRenderAsPush (renderAsPush);
         return prb;
      }
   }
}
