/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.io.*;
import java.util.*;

import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.util.*;
import maspack.properties.*;

/**
 * Internal model component that contains collision behavior information for a
 * particular collision pair. This model component is not normally directly
 * exposed to the application.
 */
public class CollisionComponent extends ModelComponentBase {

   CollidablePair myPair;
   CollisionBehavior myBehavior;

   public static PropertyList myProps =
      new PropertyList (CollisionComponent.class, ModelComponentBase.class);

   static {
      myProps.add (
         "enabled isEnabled",
         "true if collisions are enabled for this pair", false);
      myProps.add ("friction", "friction coefficient", 0, "%.8g");
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public CollisionComponent () {
      myPair = new CollidablePair (
         Collidable.Default, Collidable.Default);
      myBehavior = new CollisionBehavior();
      myBehavior.myPair = myPair;
   }

   public CollisionComponent (
      Collidable a, Collidable b, CollisionBehavior behavior) {

      // if (CollidablePair.isGeneric(a) || CollidablePair.isGeneric(b)) {
      //    throw new IllegalArgumentException (
      //       "Generic collidable types not permitted");
      // }
      myPair = new CollidablePair (a, b);
      myBehavior = new CollisionBehavior(behavior);
      myBehavior.myPair = myPair;
   }

   public CollisionComponent (
      CollidablePair pair, CollisionBehavior behavior) {
      myPair = new CollidablePair(pair);
      myBehavior = new CollisionBehavior(behavior);
      myBehavior.myPair = myPair;
   }

   public void setBehavior (CollisionBehavior v) {
      myBehavior.set (v);
   }
   
   /**
    * Returns the behavior associated with this collision component.
    *
    * @return behavior (should not be modified).
    */
   public CollisionBehavior getBehavior() {
      return myBehavior;
   }

   /**
    * Returns the pair of Collidables associated with this collision component.
    *
    * @return Collidable pair (should not be modified).
    */
   public CollidablePair getPair() {
      return myPair;
   }

   public double getFriction() {
      return myBehavior.getFriction();
   }

   public void setFriction (double mu) {
      myBehavior.setFriction (mu);
   }

   public boolean isEnabled() {
      return myBehavior.isEnabled();
   }

   public void setEnabled (boolean enabled) {
      myBehavior.setEnabled (enabled);
   }

   /** 
    * Returns true if the settings of this collision component are
    * equal to the settings another.
    *
    * @param r component to test
    * @return true if the settings of this and r are equal
    */
   public boolean equalValues (CollisionComponent r) {
      return (myBehavior.equals (r.myBehavior));
   }

   private String getReferenceName (
      Collidable col, CompositeComponent ancestor)
      throws IOException {
      if (col == Collidable.Deformable) {
         return "Deformable";
      }
      else if (col == Collidable.RigidBody) {
         return "RigidBody";
      }
      else {
         return ComponentUtils.getWritePathName (ancestor, col);
      }
   }

   @Override
   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      
      rtok.nextToken();
      if (scanAndStoreReference (rtok, "compA", tokens)) {
         return true;
      }         
      else if (scanAndStoreReference (rtok, "compB", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "compA")) {
         myPair.myCompA = postscanReference (
            tokens, Collidable.class, ancestor);
         return true;
      }
      else if (postscanAttributeName (tokens, "compB")) {
         myPair.myCompB = postscanReference (
            tokens, Collidable.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      CompositeComponent ancestor =
         ComponentUtils.castRefToAncestor (ref);
      pw.print ("[ ");
      IndentingPrintWriter.addIndentation (pw, 2);
      pw.println ("compA=" + getReferenceName (myPair.myCompA, ancestor));
      pw.println ("compB=" + getReferenceName (myPair.myCompB, ancestor));
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
      IndentingPrintWriter.addIndentation (pw, -2);
      pw.println ("]");
   }

   @Override
   public void getHardReferences (List<ModelComponent> refs) {
      super.getHardReferences (refs);
      if (myPair.myCompA != Collidable.Self) {
         refs.add (myPair.myCompA);
      }
      if (myPair.myCompB != Collidable.Self) {
         refs.add (myPair.myCompB);
      }
   }
   
//   @Override
//   public void connectToHierarchy () {
//      super.connectToHierarchy ();
//      if (myPair.myCompA != Collidable.Self) {
//         if (myPair.myCompA instanceof ModelComponentBase) {
//            ((ModelComponentBase)myPair.myCompA).addBackReference (this);
//         }
//      }
//      if (myPair.myCompB != Collidable.Self) {
//         if (myPair.myCompB instanceof ModelComponentBase) {
//            ((ModelComponentBase)myPair.myCompB).addBackReference (this);
//         }
//      }
////      try {
////         myBehavior.myModel = (MechModel)getGrandParent();
////      }
////      catch (ClassCastException e) {
////         throw new InternalErrorException (
////            "GrandParent of CollisionComponent is not a MechModel");
////      }
//   }

//   @Override
//   public void disconnectFromHierarchy() {
//      super.disconnectFromHierarchy();
//      if (myPair.myCompA != Collidable.Self) {
//         if (myPair.myCompA instanceof ModelComponentBase) {
//            ((ModelComponentBase)myPair.myCompA).removeBackReference (this);
//         }
//      }
//      if (myPair.myCompB != Collidable.Self) {
//         if (myPair.myCompB instanceof ModelComponentBase) {
//            ((ModelComponentBase)myPair.myCompB).removeBackReference (this);
//         }
//      }
//   }

   public Collidable getCollidableA (){
      return myPair.myCompA;
   }

   public Collidable getCollidableB (){
      return myPair.myCompB;
   }

   private int getDepth() {
      return ComponentUtils.getDepth(this);
   }

//   public MechModel getModel() {
//      return myBehavior.getModel();
//   }

}
