/**
 * Copyright (c) 2014, by the Authors: Ian Stavness (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.inverse;

import java.io.PrintWriter;
import java.io.IOException;
import java.util.Deque;
import java.util.List;
import java.util.ArrayList;

import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import artisynth.core.util.ScanToken;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.ExcitationSourceList;
import artisynth.core.mechmodels.ExcitationUtils;
import artisynth.core.mechmodels.ForceComponent;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.CompositeComponent;

public class PointExciter extends ModelComponentBase implements
ExcitationComponent, ForceComponent {
   
   public enum PointForceComponent {
      FX,
      FY,
      FZ
   };

   protected Point myPoint;
   protected PointForceComponent myComponent = PointForceComponent.FX;
   protected double myMaxForce;
   protected double myExcitation;
   protected Vector3d myTmp = new Vector3d();

   protected CombinationRule myComboRule = CombinationRule.Sum;
   protected ExcitationSourceList myExcitationSources;

   public PointExciter (String name, Point point, PointForceComponent comp, double maxForce) {
      super (name);
      myPoint = point;
      myComponent = comp;
      myMaxForce = maxForce;
   }
   
   public PointExciter (String name) {
      super (name);
      myPoint = null;
   }

   public PointExciter () {
      this(null);
   }

   public static PropertyList myProps = new PropertyList (
      PointExciter.class, ModelComponentBase.class);

   static {
      myProps.add (
         "maxForce * *", "external force magnitude for excitation=1", null);
      myProps.add (
         "excitation * *", "excitation for the external force", null);
   }

   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   @Override
   public void setCombinationRule (CombinationRule rule) {
      myComboRule = rule;
   }

   @Override
   public CombinationRule getCombinationRule () {
      return myComboRule;
   }

   /**
    * {@inheritDoc}
    */
   public void initialize (double t) {
      if (t == 0) {
         setExcitation (0);   
      }
   }

   @Override
   public void setExcitation (double e) {
      myExcitation = e;
   }

   @Override
   public double getExcitation () {
      return myExcitation;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void addExcitationSource (ExcitationComponent ex, double gain) {
      if (myExcitationSources == null) {
         myExcitationSources = new ExcitationSourceList();
      }
      myExcitationSources.add (ex, gain);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean removeExcitationSource (ExcitationComponent ex) {
      boolean removed = false;
      if (myExcitationSources != null) {
         removed = myExcitationSources.remove (ex);
         if (myExcitationSources.size() == 0) {
            myExcitationSources = null;
         }
      }
      return removed;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double getExcitationGain (ExcitationComponent ex) {
      return ExcitationUtils.getGain (myExcitationSources, ex);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public boolean setExcitationGain (ExcitationComponent ex, double gain) {
      return ExcitationUtils.setGain (myExcitationSources, ex, gain);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public double getNetExcitation () {
      return ExcitationUtils.combine (
         myExcitation, myExcitationSources, myComboRule);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      if (myExcitationSources != null) {
         myExcitationSources.getSoftReferences (refs);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);
      myExcitationSources = ExcitationUtils.updateReferences (
         this, myExcitationSources, undo, undoInfo);
   }

   public double getMaxForce () {
      return myMaxForce;
   }

   public void setMaxForce (double maxForce) {
      myMaxForce = maxForce;
   }

   @Override
   public void applyForces (double t) {
      myTmp.set (myComponent.ordinal (), myExcitation*myMaxForce);
      myPoint.addForce (myTmp);
   }

   @Override
   public void addSolveBlocks (SparseNumberedBlockMatrix M) {
      // constant force
   }

   @Override
   public void addPosJacobian (SparseNumberedBlockMatrix M, double s) {
      // constant force
   }

   @Override
   public void addVelJacobian (SparseNumberedBlockMatrix M, double s) {
      // constant force
   }

   @Override
   public int getJacobianType () {
      // constant force
      return 0;
   }
   
   /*
    * helper method to add point exciters for all 3 components of the force on the given point
    */
   public static PointExciter[] addPointExciters(MechModel mech, Point point, double maxForce) {
      PointExciter[] pes = new PointExciter[3];
      for (PointForceComponent comp : PointForceComponent.values ()) {
         PointExciter pe = new PointExciter (point.getName()+"_"+comp.name(), point, comp, maxForce);
         mech.addForceEffector (pe);
         pes[comp.ordinal()] = pe;
      }
      return pes;
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor) 
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      if (myExcitationSources != null) {
         myExcitationSources.write (pw, "excitationSources", fmt, ancestor);
      }
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      rtok.nextToken();
      if (scanAttributeName (rtok, "excitationSources")) {
         myExcitationSources =
            ExcitationUtils.scan (rtok, "excitationSources", tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   /**
    * {@inheritDoc}
    */
   @Override
   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "excitationSources")) {
         myExcitationSources.postscan (tokens, ancestor);
         return true;
      }   
      return super.postscanItem (tokens, ancestor);
   }

}
