/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.HashSet;
import java.util.Arrays;
import java.util.ArrayList;

import maspack.matrix.*;
import maspack.geometry.*;

/**
 * Implements a ContactMaster that combines the weighted contact
 * masters for a single vertex-based contact
 */
public class VertexContactMaster implements ContactMaster {
   
   ArrayList<ContactMaster>[] myMasterLists;
   double[] myWgts;

   public VertexContactMaster (
      CollidableBody collidable, Vertex3d[] vtxs, double[] wgts) {
      
      myWgts = Arrays.copyOf (wgts, wgts.length);
      myMasterLists = new ArrayList[vtxs.length];
      for (int i=0; i<vtxs.length; i++) {
         myMasterLists[i] = new ArrayList<>();
         collidable.collectVertexMasters (
            myMasterLists[i], vtxs[i]);
      }
   }

   public void add1DConstraintBlocks (
      SparseBlockMatrix GT, int bj, double scale, 
      ContactPoint cpnt, Vector3d dir) {

      for (int i=0; i<myMasterLists.length; i++) {
         for (ContactMaster cm : myMasterLists[i]) {
            cm.add1DConstraintBlocks (GT, bj, scale*myWgts[i], cpnt, dir);
         }
      }
   }

   public void add2DConstraintBlocks (
      SparseBlockMatrix GT, int bj, double scale,
      ContactPoint cpnt, Vector3d dir0, Vector3d dir1) {

      for (int i=0; i<myMasterLists.length; i++) {
         for (ContactMaster cm : myMasterLists[i]) {
            cm.add2DConstraintBlocks (
               GT, bj, scale*myWgts[i], cpnt, dir0, dir1);
         }
      }
   }
   
   public void addRelativeVelocity (
      Vector3d vel, double scale, ContactPoint cpnt) {

      for (int i=0; i<myMasterLists.length; i++) {
         for (ContactMaster cm : myMasterLists[i]) {
            cm.addRelativeVelocity (vel, scale*myWgts[i], cpnt);
         }
      }
   }

   public boolean isControllable () {
      for (int i=0; i<myMasterLists.length; i++) {
         for (ContactMaster cm : myMasterLists[i]) {
            if (cm.isControllable()) {
               return true;
            }
         }
      }
      return false;
   }

   public int collectMasterComponents (
      HashSet<DynamicComponent> masters, boolean activeOnly) {

      int num = 0;
      for (int i=0; i<myMasterLists.length; i++) {
         for (ContactMaster cm : myMasterLists[i]) {
            num += cm.collectMasterComponents(masters, activeOnly);
         }
      }
      return num;
   }
}
