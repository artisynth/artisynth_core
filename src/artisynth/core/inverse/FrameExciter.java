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
import java.util.InputMismatchException;
import java.util.List;
import java.util.ArrayList;

import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.properties.PropertyList;
import maspack.spatialmotion.Wrench;
import artisynth.core.util.ScanToken;
import artisynth.core.mechmodels.ExcitationComponent;
import artisynth.core.mechmodels.ExcitationUtils;
import artisynth.core.mechmodels.ForceComponent;
import artisynth.core.mechmodels.Frame;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.ExcitationSourceList;
import artisynth.core.mechmodels.ExcitationUtils;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.CompositeComponent;

public class FrameExciter extends ModelComponentBase implements
ExcitationComponent, ForceComponent {

   public enum WrenchComponent {
      FX,
      FY,
      FZ,
      MX,
      MY,
      MZ
   };
   
   protected Frame myFrame;
   protected WrenchComponent myComponent = WrenchComponent.FX;
   protected double myMaxForce;
   protected double myExcitation;
   protected Wrench myTmp = new Wrench();

   protected CombinationRule myComboRule = CombinationRule.Sum;
   protected ExcitationSourceList myExcitationSources;
   
   public FrameExciter (String name, Frame body, WrenchComponent comp, double maxForce) {
      super (name);
      myFrame = body;
      myComponent = comp;
      myMaxForce = maxForce;
   }
   
   public FrameExciter (String name) {
      super (name);
      myFrame = null;
   }

   public FrameExciter () {
      this(null);
   }

   public static PropertyList myProps = new PropertyList (
      FrameExciter.class, ModelComponentBase.class);

   static {
      myProps.add ("maxForce * *", "external force magnitude for excitation=1", null);
      myProps.add ("excitation * *", "excitation for the external force", null);
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
      myFrame.addForce (myTmp);
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
    * helper method to add frame exciters for all 6 components of a wrench on the given frame
    */
   public static FrameExciter[] addFrameExciters(MechModel mech, Frame frame, double maxForce) {
      FrameExciter[] fes = new FrameExciter[6];
      for (WrenchComponent comp : WrenchComponent.values ()) {
         FrameExciter fe = new FrameExciter (frame.getName()+"_"+comp.name(), frame, comp, maxForce);
         mech.addForceEffector (fe);
         fes[comp.ordinal()]=fe;
      }
      return fes;
   }

   /**
    * Adds frame exciters for up to 6 components of a wrench on a frame.
    * Each component can have its own maximum force and is only added if desired.
    * @param mech the MechModel
    * @param frame the frame on which the wrench is applied
    * @param enabled array[6] indicating which of the 6 components should be added
    * @param maxForces array[6] indicating the max force for each component (value does not matter for unused components)
    * @return an array containing only the active frame exciters
    */
   public static FrameExciter[] addFrameExciters(MechModel mech, Frame frame, boolean[] enabled, double[] maxForces) {
      if (maxForces.length != 6) {
         throw new InputMismatchException ("maxForces[] must have 6 elements");
      }
      if (enabled.length != 6) {
         throw new InputMismatchException ("enabled[] must have 6 elements");
      }
      
      /* count how many force effectors there will be */
      int b = 0;
      for (boolean bool : enabled) {
         if (bool) {
            b++;
         }
      }
      FrameExciter[] fes = new FrameExciter[b];
      
      int i = 0;
      int j = 0;
      for (WrenchComponent comp : WrenchComponent.values()) {
         if (enabled[i]) {
            FrameExciter fe = new FrameExciter(frame.getName()+"_"+comp.name (), frame, comp, maxForces[i]);
            mech.addForceEffector(fe);
            fes[j++] = fe;
         }
         i++;
      }      
      return fes;
   }
   
   
   /*
    * helper method to add frame exciters for all 6 components of a wrench on the given frame
    */
   public static FrameExciter[] addLinearFrameExciters(MechModel mech, Frame frame, double maxForce) {
      FrameExciter[] fes = new FrameExciter[3];
      for (WrenchComponent comp : WrenchComponent.values ()) {
         FrameExciter fe = new FrameExciter (frame.getName()+"_"+comp.name(), frame, comp, maxForce);
         mech.addForceEffector (fe);
         fes[comp.ordinal()]=fe;
         if (comp.ordinal () >= fes.length-1) 
            break;
      }
      return fes;
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
