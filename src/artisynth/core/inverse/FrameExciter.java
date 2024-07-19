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

import maspack.util.DataBuffer;
import maspack.util.NumberFormat;
import maspack.util.ReaderTokenizer;
import maspack.matrix.SparseNumberedBlockMatrix;
import maspack.matrix.Matrix;
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
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.HasNumericState;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.CompositeComponent;

public class FrameExciter extends ModelComponentBase implements
ExcitationComponent, ForceComponent, HasNumericState {

   /**
    * Specifies the degree-of-freedom along (or about) which the force (or
    * moment) of this exciter will be applied to the frame.
    */
   public enum WrenchDof {
      /**
       * Force along x (in world coordinates)
       */
      FX,
      /**
       * Force along y (in world coordinates)
       */
      FY,
      /**
       * Force along z (in world coordinates)
       */
      FZ,
      /**
       * Moment about x (in world coordinates)
       */
      MX,
      /**
       * Moment about y (in world coordinates)
       */
      MY,
      /**
       * Moment about z (in world coordinates)
       */
      MZ
   };

   /**
    * @deprecated Use {@link WrenchDof} instead.
    */
   public enum WrenchComponent {
      FX(WrenchDof.FX),
      FY(WrenchDof.FY),
      FZ(WrenchDof.FZ),
      MX(WrenchDof.MX),
      MY(WrenchDof.MY),
      MZ(WrenchDof.MZ);

      WrenchDof myDof;
      WrenchComponent (WrenchDof dof) {
         myDof = dof;
      }
   };

   
   protected Frame myFrame;
   protected WrenchDof myDOF = WrenchDof.FX;
   protected double myMaxForce;
   protected double myExcitation;
   protected Wrench myTmp = new Wrench();

   protected CombinationRule myComboRule = CombinationRule.Sum;
   protected ExcitationSourceList myExcitationSources;

   /**
    * Creates a FrameExciter.
    * 
    * @param name name of the exciter, or {@code null}
    * @param frame frame being activated
    * @param dof degree of freedom being controlled (world coordinates)
    * @param maxForce maximum force along the dof
    */
   public FrameExciter (
      String name, Frame frame, WrenchDof dof, double maxForce) {
      super (name);
      myFrame = frame;
      myDOF = dof;
      myMaxForce = maxForce;
   }
   
   /**
    * @deprecated Use {@link #FrameExciter(String,Frame,WrenchDof,double)}
    * instead.
    */
   public FrameExciter (
      Frame frame, WrenchComponent comp, double maxForce) {
      this (null, frame, comp.myDof, maxForce);
   }

   /**
    * @deprecated Use {@link #FrameExciter(String,Frame,WrenchDof,double)}
    * instead.
    */
   public FrameExciter (
      String name, Frame frame, WrenchComponent comp, double maxForce) {
      this (null, frame, comp.myDof, maxForce);
   }
   
   public FrameExciter (
      Frame frame, WrenchDof dof, double maxForce) {
      this (null, frame, dof, maxForce);
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
      myTmp.set (myDOF.ordinal (), myExcitation*myMaxForce);
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
      return Matrix.SPD;
   }
   
   /**
    * Creates a set of three FrameExciters to control the translational force
    * on a given frame. If {@code mech} is non-null, the exciters are added to
    * its list of force effectors. If {@code createName} is {@code true} and
    * the frame has a name, each exciter is given a name based on the frame
    * name.
    *
    * @param mech optional MechModel to add the exciters to
    * @param frame frame for which the exciters should be created
    * @param maxForce maximum translational force along any axis
    * @param createNames if {@code true}, creates names for the exciters.
    * @return list of the created exciters.
    */   
   public static ArrayList<FrameExciter> createForceExciters (
      MechModel mech, Frame frame, double maxForce, boolean createNames) {
      ArrayList<FrameExciter> exs = new ArrayList<>();
      exs.add (new FrameExciter (null, frame, WrenchDof.FX, maxForce));
      exs.add (new FrameExciter (null, frame, WrenchDof.FY, maxForce));
      exs.add (new FrameExciter (null, frame, WrenchDof.FZ, maxForce));
      // if the frame has a name, use this to create names for the exciters
      if (createNames && frame.getName() != null) {
         exs.get(0).setName (frame.getName()+"_fx");
         exs.get(1).setName (frame.getName()+"_fy");
         exs.get(2).setName (frame.getName()+"_fz");
      }
      if (mech != null) {
         for (FrameExciter ex : exs) {
            mech.addForceEffector (ex);
         }
      }
      return exs;        
   }
   
   /**
    * Creates a set of three FrameExciters to control the moment force on a
    * given frame. If {@code mech} is non-null, the exciters are added to its
    * list of force effectors. If {@code createName} is {@code true} and the
    * frame has a name, each exciter is given a name based on the frame name.
    *
    * @param mech optional MechModel to add the exciters to
    * @param frame frame for which the exciters should be created
    * @param maxMoment maximum moment force about any axis
    * @param createNames if {@code true}, creates names for the exciters.
    * @return list of the created exciters.
    */   
   public static ArrayList<FrameExciter> createMomentExciters (
      MechModel mech, Frame frame, double maxMoment, boolean createNames) {
      ArrayList<FrameExciter> exs = new ArrayList<>();
      exs.add (new FrameExciter (null, frame, WrenchDof.MX, maxMoment));
      exs.add (new FrameExciter (null, frame, WrenchDof.MY, maxMoment));
      exs.add (new FrameExciter (null, frame, WrenchDof.MZ, maxMoment));
      // if the frame has a name, use this to create names for the exciters
      if (createNames && frame.getName() != null) {
         exs.get(0).setName (frame.getName()+"_mx");
         exs.get(1).setName (frame.getName()+"_my");
         exs.get(2).setName (frame.getName()+"_mz");
      }
      if (mech != null) {
         for (FrameExciter ex : exs) {
            mech.addForceEffector (ex);
         }
      }
      return exs;        
   }
   
   /**
    * Creates a complete set of FrameExciters for a given frame. If {@code
    * mech} is non-null, the exciters are added to its list of force
    * effectors. If {@code createName} is {@code true} and the frame has a
    * name, each exciter is given a name based on the frame name.
    *
    * @param mech optional MechModel to add the exciters to
    * @param frame frame for which the exciters should be created
    * @param maxForce maximum translational force along any axis
    * @param maxMoment maximum moment about any axis
    * @param createNames if {@code true}, creates names for the exciters.
    * @return list of the created exciters.
    */   
   public static ArrayList<FrameExciter> createFrameExciters (
      MechModel mech, Frame frame, 
      double maxForce, double maxMoment, boolean createNames) {
      ArrayList<FrameExciter> exs = new ArrayList<>();
      exs.addAll (createForceExciters (mech, frame, maxForce, createNames));
      exs.addAll (createMomentExciters (mech, frame, maxMoment, createNames));
      return exs;        
   }
   
   /*
    * helper method to add frame exciters for all 6 components of a wrench on the given frame
    */
   public static FrameExciter[] addFrameExciters(MechModel mech, Frame frame, double maxForce) {
      FrameExciter[] fes = new FrameExciter[6];
      for (WrenchDof comp : WrenchDof.values ()) {
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
      for (WrenchDof comp : WrenchDof.values()) {
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
      for (WrenchDof comp : WrenchDof.values ()) {
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
      pw.println ("dof=" + myDOF);
      if (myExcitationSources != null) {
         myExcitationSources.write (pw, "excitationSources", fmt, ancestor);
      }
      if (myFrame != null) {
         pw.println (
            "frame="+ComponentUtils.getWritePathName (ancestor,myFrame));
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
      else if (scanAndStoreReference (rtok, "frame", tokens)) {
         return true;
      }
      else if (scanAttributeName (rtok, "dof")) {
         myDOF = rtok.scanEnum (WrenchDof.class);
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
      else if (postscanAttributeName (tokens, "frame")) {
         myFrame = postscanReference (tokens, Frame.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   /* --- Implementation of HasNumericState to save/restore excitation --- */
   
   public void getState (DataBuffer data) {
      data.dput (myExcitation);
   }

   public void setState (DataBuffer data) {
      myExcitation = data.dget();
   }

   public boolean hasState() {
      return true;
   }
   
   /* --- End HasNumericState implementation --- */    
}
