/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import artisynth.core.mechmodels.Particle;
import artisynth.core.modelbase.*;
import artisynth.core.util.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.util.*;

import java.util.*;
import java.io.*;

public abstract class FemNode extends Particle {
   
   protected boolean myMassValidP = false;
   protected boolean myMassExplicitP = false;
   
   public static PropertyList myProps =
   new PropertyList (FemNode.class, Particle.class);

   static {
      myProps.get ("mass").setAutoWrite (false);
//      myProps.add (
//         "massExplicit", "if false, mass is set from density", false, "NW");
   }
   
   public FemNode() {
      super();
      myMass = 0;
      myMassValidP = false;
      myMassExplicitP = false;
      //myEffectiveMass = 0;
   }

   public double getMass() {
      if (!myMassExplicitP && !myMassValidP) {
         myMass = computeMassFromDensity();
         myMassValidP = true;
      }
      return myMass;
   }
   
   public void setMass (double m) {
      myMass = m;
      myMassValidP = true;
   }

   public void addMass (double m) {
      myMass += m;
      myMassValidP = true; // assume we are building it up in a valid way
   }
   
   @Override
   public void scaleMass(double s) {
      if (myMassExplicitP) {
         myMass *= s;
      }
      else {
         myMassValidP = false;
      }
      super.scaleMass(s);
   }
   
   public void clearMass() {
      if (!myMassExplicitP) {
         myMass = 0;
         myMassValidP = false;
      }
   }
   
   /**
    * Sets the mass for this node to an explicit value. This means that
    * the mass will no longer be determined from the masses of the surrounding
    * elements.
    * 
    * @param mass explicit mass value
    */
   public void setExplicitMass (double mass) {
      myMass = mass;
      myMassExplicitP = true;
      myMassValidP = true;
   }
   
   /**
    * Unsets an explicit mass for this node. This means that the mass will
    * be determined from the masses of the surrounding elements.
    */
   public void unsetExplicitMass () {
      myMassExplicitP = false;
      myMassValidP = false;
   }  
   
   /**
    * Queries whether the mass for this node has been explicitly set.
    * 
    * @return true if the mass has been explicitly set
    */
   public boolean isMassExplicit() {
      return myMassExplicitP;
   }

   public void invalidateMassIfNecessary () {
      if (!myMassExplicitP) {
         myMassValidP = false;
      } 
   }
   
   protected abstract void invalidateAdjacentNodeMasses();
   
   public abstract double computeMassFromDensity();
   
   @Override
   public boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {

      rtok.nextToken();
      if (scanAttributeName (rtok, "mass")) {
         double mass = rtok.scanNumber();
         setExplicitMass (mass);
         return true;
      } 
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {
      super.writeItems (pw, fmt, ancestor);
      if (myMassExplicitP) {
         pw.println ("mass=" + fmt.format(myMass));
      }
   }

   public FemNode copy (
      int flags, Map<ModelComponent,ModelComponent> copyMap) {
      FemNode node = (FemNode)super.copy (flags, copyMap);
      node.myMass = myMass;
      if (myMassExplicitP) {
         node.myMassValidP = true;
         node.myMassExplicitP = true;
      }
      else {
         node.myMassValidP = false;
         node.myMassExplicitP = false;
      }
      return node;
   }

   public abstract Point3d getRestPosition();

   // public void setPosition (Point3d pos) {
   //    super.setPosition (pos);
   //    // send a geometry change event to cause the BVHierachy to be
   //    // invalidated:
   //    notifyParentOfChange (new GeometryChangeEvent (this,null));
   // }
}
