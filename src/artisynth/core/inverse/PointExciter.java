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
import maspack.matrix.Matrix;
import maspack.properties.PropertyList;
import artisynth.core.util.ScanToken;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.ExcitationSourceList;
import artisynth.core.mechmodels.ExcitationUtils;
import artisynth.core.mechmodels.ForceComponent;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.Point;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.CompositeComponent;

public class PointExciter extends ModelComponentBase implements
ExcitationComponent, ForceComponent {

   /**
    * Specifies the degree-of-freedom along which the force of this exciter
    * will be applied to the point.
    */
   public enum ForceDof {
      /**
       * Force along x
       */
      FX,
      /**
       * Force along y
       */
      FY,
      /**
       * Force along z
       */
      FZ
   };
   
   /**
    * @deprecated Use {@link ForceDof} instead.
    */
   public enum PointForceComponent {
      FX(ForceDof.FX),
      FY(ForceDof.FY),
      FZ(ForceDof.FZ);

      ForceDof myDof;
      PointForceComponent (ForceDof dof) {
         myDof = dof;
      }
   };
   
   protected Point myPoint;
   protected ForceDof myDof = ForceDof.FX;
   protected double myMaxForce;
   protected double myExcitation;
   protected Vector3d myTmp = new Vector3d();

   protected CombinationRule myComboRule = CombinationRule.Sum;
   protected ExcitationSourceList myExcitationSources;

   /**
    * Creates a PointExciter.
    *
    * @param name name of the exciter, or {@code null}
    * @param point point being activated
    * @param dof degree of freedom being controlled 
    * @param maxForce maximum force along the dof
    */
   public PointExciter (
      String name, Point point, ForceDof dof, double maxForce) {
      super (name);
      myPoint = point;
      myDof = dof;
      myMaxForce = maxForce;
   }
   
   /**
    * Creates a PointExciter.
    *
    * @param point point being activated
    * @param dof degree of freedom being controlled 
    * @param maxForce maximum force along the dof
    */
   public PointExciter (
      Point point, ForceDof dof, double maxForce) {
      this (null, point, dof, maxForce);
   }
   
   /**
    * @deprecated Use {@link #PointExciter(String,Point,ForceDof,double)}
    * instead.
    */
   public PointExciter (
      String name, Point point, PointForceComponent comp, double maxForce) {
      this (name, point, comp.myDof, maxForce);
   }
   
   /**
    * @deprecated Use {@link #PointExciter(Point,ForceDof,double)}
    * instead.
    */
   public PointExciter (
      Point point, PointForceComponent comp, double maxForce) {
      this (null, point, comp.myDof, maxForce);
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
      myTmp.set (myDof.ordinal (), myExcitation*myMaxForce);
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
      return Matrix.SPD;
   }

   /**
    * Creates a set of three PointExciters to control the translational force
    * on a given point. If {@code mech} is non-null, the exciters are added to
    * its list of force effectors. If {@code createName} is {@code true} and
    * the point has a name, each exciter is given a name based on the frame
    * name.
    *
    * @param mech optional MechModel to add the exciters to
    * @param point point for which the exciters should be created
    * @param maxForce maximum translational force along any axis
    * @param createNames if {@code true}, creates names for the exciters.
    * @return list of the created exciters.
    */   
   public static ArrayList<PointExciter> createPointExciters (
      MechModel mech, Point point, double maxForce, boolean createNames) {
      ArrayList<PointExciter> exs = new ArrayList<>();
      exs.add (new PointExciter (null, point, ForceDof.FX, maxForce));
      exs.add (new PointExciter (null, point, ForceDof.FY, maxForce));
      exs.add (new PointExciter (null, point, ForceDof.FZ, maxForce));
      // if the point has a name, use this to create names for the exciters
      if (createNames && point.getName() != null) {
         exs.get(0).setName (point.getName()+"_fx");
         exs.get(1).setName (point.getName()+"_fy");
         exs.get(2).setName (point.getName()+"_fz");
      }
      if (mech != null) {
         for (PointExciter ex : exs) {
            mech.addForceEffector (ex);
         }
      }
      return exs;        
   }
   
   /*
    * helper method to add point exciters for all 3 components of the force on the given point
    */
   public static PointExciter[] addPointExciters(MechModel mech, Point point, double maxForce) {
      PointExciter[] pes = new PointExciter[3];
      for (ForceDof dof : ForceDof.values ()) {
         PointExciter pe = new PointExciter (
            point.getName()+"_"+dof.name(), point, dof, maxForce);
         mech.addForceEffector (pe);
         pes[dof.ordinal()] = pe;
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
      pw.println ("dof=" + myDof);
      if (myExcitationSources != null) {
         myExcitationSources.write (pw, "excitationSources", fmt, ancestor);
      }
      if (myPoint != null) {
         pw.println (
            "point="+ComponentUtils.getWritePathName (ancestor,myPoint));
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
      else if (scanAndStoreReference (rtok, "point", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "dof")) {
         myDof = rtok.scanEnum (ForceDof.class);
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
      else if (postscanAttributeName (tokens, "point")) {
         myPoint = postscanReference (tokens, Point.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

}
