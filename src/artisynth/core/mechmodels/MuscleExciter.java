/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import maspack.properties.PropertyList;
import maspack.util.*;
import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.CompositeComponentBase;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.util.*;

public class MuscleExciter extends ModelComponentBase implements
ExcitationComponent {
   protected double myExcitation; // default = 0.0;
   protected ExcitationSourceList myExcitationSources; 
   protected CombinationRule myComboRule = CombinationRule.Sum;

   public static PropertyList myProps =
      new PropertyList (MuscleExciter.class, ModelComponentBase.class);
   static {
      myProps.add (
         "excitation", "percentage of muscle activation", 0.0, "[0,1] NW");
   };

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   // private class TargetInfo {
   //    ExcitationComponent myComponent;
   //    double myGain;

   //    TargetInfo (ExcitationComponent ex, double gain) {
   //       myComponent = ex;
   //       myGain = gain;
   //    }
   // }

   public int findTarget (ExcitationComponent ex) {
      for (int i = 0; i < myTargets.size(); i++) {
         if (myTargets.get(i) == ex) {
            return i;
         }
      }
      return -1;
   }

   protected ArrayList<ExcitationComponent> myTargets;

   // private class TargetIterator implements Iterator<ExcitationComponent> {
   //    Iterator<TargetInfo> myIterator;

   //    TargetIterator() {
   //       myIterator = myTargets.iterator();
   //    }

   //    public boolean hasNext() {
   //       return myIterator.hasNext();
   //    }

   //    public ExcitationComponent next() {
   //       return myIterator.next().myComponent;
   //    }

   //    public void remove() {
   //       throw new UnsupportedOperationException();
   //    }
   // }

   private class TargetView implements ListView<ExcitationComponent> {
      public Iterator<ExcitationComponent> iterator() {
         return myTargets.iterator();
      }

      public ExcitationComponent get (int idx) {
         return myTargets.get (idx);
      }

      public int size() {
         return myTargets.size();
      }

      public int indexOf (Object elem) {
         for (int i = 0; i < myTargets.size(); i++) {
            if (myTargets.get(i) == elem) {
               return i;
            }
         }
         return -1;
      }

      public boolean contains (Object elem) {
         return indexOf (elem) != -1;
      }
   }

   public MuscleExciter() {
      myTargets = new ArrayList<ExcitationComponent>();
   }

   public MuscleExciter (String name) {
      this();
      setName (name);
   }

   /**
    * {@inheritDoc}
    */
   public double getExcitation() {
      return myExcitation;
   }

   /**
    * {@inheritDoc}
    */
   public void initialize (double t) {
      if (t == 0) {
         setExcitation (0);         
      }
   }

   /**
    * {@inheritDoc}
    */
   public void setExcitation (double a) {
      // set activation within valid range
      myExcitation = a;
   }

   /**
    * {@inheritDoc}
    */
   public void setCombinationRule (CombinationRule rule) {
      myComboRule = rule;
   }

   /**
    * {@inheritDoc}
    */
   public CombinationRule getCombinationRule() {
      return myComboRule;
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
   public double getNetExcitation() {
      return ExcitationUtils.combine (
         myExcitation, myExcitationSources, myComboRule);
   }

   public int numTargets() {
      return myTargets.size();
   }

   public void addTarget (ExcitationComponent ex) {
      myTargets.add (ex);
      ex.addExcitationSource (this, 1.0);
   }
   
   public void addTarget (ExcitationComponent ex, double gain) {
      myTargets.add (ex);
      ex.addExcitationSource (this, gain);
   }

   public int removeTarget (ExcitationComponent ex) {
      int idx = findTarget (ex);
      if (idx != -1) {
         myTargets.remove (idx);
         ex.removeExcitationSource (this);
      }
      return idx;
   }

   public void removeAllTargets() {
      for (ExcitationComponent t : myTargets) {
         t.removeExcitationSource (this);
      }
      myTargets.clear();
   }

   public ExcitationComponent getTarget (int idx) {
      if (idx >= myTargets.size()) {
         throw new ArrayIndexOutOfBoundsException ("index " + idx
         + " is out of bounds");
      }
      return myTargets.get (idx);
   }

   public ListView<ExcitationComponent> getTargetView() {
      return new TargetView();
   }

   public double getGain (int idx) {
      if (idx >= myTargets.size()) {
         throw new ArrayIndexOutOfBoundsException ("index " + idx
         + " is out of bounds");
      }
      return myTargets.get(idx).getExcitationGain (this);
   }

   public void setGain (int idx, double gain) {
      if (idx >= myTargets.size()) {
         throw new ArrayIndexOutOfBoundsException ("index " + idx
         + " is out of bounds");
      }
      if (gain == -1) {
         (new Throwable()).printStackTrace(); 
      }
      myTargets.get (idx).setExcitationGain (this, gain);
   }

   public void setGain (ExcitationComponent ex, double gain) {
      int idx = findTarget (ex);
      if (idx == -1) {
         throw new IllegalArgumentException (
            "Excitable component not contained in target list");
      }
      if (gain == -1) {
         (new Throwable()).printStackTrace(); 
      }
      myTargets.get (idx).setExcitationGain (this, gain);
   }
   
   /**
    * Scale the gains for all the targets controller by this excitation
    * component.
    * 
    * @param s gain scale factor
    */
   public void scaleGains (double s) {
      for (ExcitationComponent ex : myTargets) {
         ex.setExcitationGain (this, s*ex.getExcitationGain(this));
      }
   }

//   public double getDefaultActivationWeight() {
//      double w = 0;
//      for (ExcitationComponent tinfo : myTargets) {
//         w += tinfo.getDefaultActivationWeight();
//      }
//      return w;
//   }

   // @Override
   // public void getDependencies (
   //    List<ModelComponent> deps, ModelComponent ancestor) {
   //    super.getDependencies (deps, ancestor);
   //    if (myExcitationSources != null) {
   //       ComponentUtils.addDependencies (deps, myExcitationSources, ancestor);
   //    }
   // }

   @Override
   public void getSoftReferences (List<ModelComponent> refs) {
      super.getSoftReferences (refs);
      for (int i = 0; i < myTargets.size(); i++) {
         refs.add (myTargets.get(i));
      }
      if (myExcitationSources != null) {
         myExcitationSources.getSoftReferences (refs);
      }
   }

//    private void printTargets (PrintWriter pw, NumberFormat fmt, Object ref)
//       throws IOException {
//       CompositeComponent ancestor =
//          ComponentUtils.castRefToAncestor (ref);
//       if (myTargets.size() == 0) {
//          pw.println ("targets=[]");
//       }
//       else {
//          pw.println ("targets=[");
//          IndentingPrintWriter.addIndentation (pw, 2);
//          for (int i = 0; i < myTargets.size(); i++) {
//             TargetInfo info = myTargets.get (i);
//             String pathName =
//                ComponentUtils.getWritePathName (
//                   ancestor, info.myComponent);
//             pw.println (pathName + " " + fmt.format (info.myGain));
//          }
//          IndentingPrintWriter.addIndentation (pw, -2);
//          pw.println ("]");
//       }
//    }

//    private void scanTargets (ReaderTokenizer rtok, Deque<ScanToken> tokens)
//       throws IOException {
//       rtok.scanToken ('[');
//       tokens.offer (ScanToken.BEGIN);
//       while (ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
//          myTargets.add (new TargetInfo (null, rtok.scanNumber()));
//       }
//       if (rtok.ttype != ']') {
//          throw new IOException ("Expected ']', got " + rtok);
//       }      
// //      while (rtok.nextToken() != ']') {
// //         rtok.pushBack();
// //         ScanWriteUtils.scanReferenceToken (rtok, tokens);
// //         myTargets.add (new TargetInfo (null, rtok.scanNumber()));
// //      }
//       tokens.offer (ScanToken.END);
//    }

   /**
    * {@inheritDoc}
    */
   public void updateReferences (boolean undo, Deque<Object> undoInfo) {
      super.updateReferences (undo, undoInfo);
      ComponentUtils.updateReferences (this, myTargets, undo, undoInfo);
      myExcitationSources = ExcitationUtils.updateReferences (
         this, myExcitationSources, undo, undoInfo);      
   }

   /**
    * {@inheritDoc}
    */
   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      super.writeItems (pw, fmt, ancestor);
      pw.print ("targets=");
      ScanWriteUtils.writeBracketedReferences (pw, myTargets, ancestor);
      if (myExcitationSources != null) {
         myExcitationSources.write (pw, "excitationSources", fmt, ancestor);
      }
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAndStoreReferences (rtok, "targets", tokens) >= 0) {
         return true;
      }
      else if (scanAttributeName (rtok, "excitationSources")) {
         myExcitationSources =
            ExcitationUtils.scan (rtok, "excitationSources", tokens);
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "targets")) {
         myTargets.clear();
         ScanWriteUtils.postscanReferences (
            tokens, myTargets, ExcitationComponent.class, ancestor);
         // for (ExcitationComponent ex : myTargets) {
         //    ex.addExcitationSource (this, 1.0);
         // }
         return true;
      }
      else if (postscanAttributeName (tokens, "excitationSources")) {
         myExcitationSources.postscan (tokens, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
   
   

}
