/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.*;

import maspack.geometry.GeometryTransformer;
import maspack.render.*;
import maspack.util.*;
import maspack.matrix.*;
import maspack.properties.*;
import artisynth.core.modelbase.*;
import artisynth.core.util.ScalableUnits;
import maspack.render.*;

import java.util.*;

import javax.media.opengl.*;

public class PointList<P extends Point> extends RenderableComponentList<P>
implements ScalableUnits {
   protected static final long serialVersionUID = 1;
   private double myPointDamping;
   private PropertyMode myPointDampingMode = PropertyMode.Inherited;

   public static PropertyList myProps =
      new PropertyList (PointList.class, RenderableComponentList.class);

   static {
      myProps.addInheritable (
         "pointDamping", "intrinsic damping force", 0.0, "%.8f");
      myProps.get ("renderProps").setDefaultValue (new PointRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getPointDamping() {
      return myPointDamping;
   }

   public void setPointDamping (double d) {
      myPointDamping = d;
      myPointDampingMode =
         PropertyUtils.propagateValue (
            this, "pointDamping", d, myPointDampingMode);
   }

   public PropertyMode getPointDampingMode() {
      return myPointDampingMode;
   }

   public void setPointDampingMode (PropertyMode mode) {
      myPointDampingMode =
         PropertyUtils.setModeAndUpdate (
            this, "pointDamping", myPointDampingMode, mode);
   }

   public PointList (Class<P> type) {
      this (type, null, null);
   }

   public PointList (Class<P> type, String name) {
      super (type, name);
      setRenderProps (createRenderProps());
   }

   public PointList (Class<P> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
   }

   // public PointList (Class type, String name, String shortName,
   // CompositeComponent parent)
   // {
   // super (type, name, shortName, parent);
   // setRenderProps(createRenderProps());
   // }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createPointProps (this);
   }

   public void prerender (RenderList list) {
      for (int i = 0; i < size(); i++) {
         Point p = get (i);
         if (p.getRenderProps() != null) {
            list.addIfVisible (p);
         }
         else {
            p.prerender (list);
         }
      }
   }

   public boolean rendersSubComponents() {
      return true;
   }

   public void render (GLRenderer renderer, int flags) {
      renderer.drawPoints (myRenderProps, iterator());
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return size();
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < size()) {
         list.addLast (get (qid));
      }
   }
   
   public void scaleDistance (double s) {
      for (int i = 0; i < size(); i++) {
         get (i).scaleDistance (s);
      }
      if (myRenderProps != null) {
         RenderableUtils.cloneRenderProps (this);
         myRenderProps.scaleDistance (s);
      }
   }

   public void scaleMass (double s) {
      myPointDamping *= s;
      for (int i = 0; i < size(); i++) {
         get (i).scaleMass (s);
      }
   }

}
