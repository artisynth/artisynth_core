/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.*;
import java.io.*;

import maspack.util.*;
import artisynth.core.util.*;
import artisynth.core.modelbase.*;

public class ExcitationSourceList extends ArrayList<ExcitationSource> {

   public void add (ExcitationComponent ex) {
      add (new ExcitationSource (ex, 1));
   }

   public void add (ExcitationComponent ex, double gain) {
      add (new ExcitationSource (ex, gain));
   }

   public int indexOf (ExcitationComponent ex) {
      for (int i=0; i<size(); i++) {
         if (get(i).myComp == ex) {
            return i;
         }
      }
      return -1;
   }      

   public boolean remove (ExcitationComponent ex) {
      int idx = indexOf (ex);
      if (idx != -1) {
         remove (idx);
         return true;
      }
      else {
         return false;
      }
   }

   public ExcitationSource get (ExcitationComponent ex) {
      int idx = indexOf (ex);
      return (idx != -1 ? get(idx) : null);
   }

   public boolean contains (ExcitationComponent ex) {
      return get (ex) != null;
   }

   public double getGain (ExcitationComponent ex) {
      ExcitationSource src = get (ex);
      if (src != null) {
         return src.myGain;
      }
      else {
         return -1;
      }
   }

   public boolean setGain (ExcitationComponent ex, double gain) {
      ExcitationSource src = get (ex);
      if (src != null) {
         src.myGain = gain;
         return true;
      }
      else {
         return false;
      }
   }

   public void getSoftReferences (List<ModelComponent> refs) {
      for (int i=0; i<size(); i++) {
         refs.add (get(i).myComp);
      }
   }

   public void write (
      PrintWriter pw, String name, NumberFormat fmt, CompositeComponent ancestor) 
      throws IOException {

      int numw = 0;
      for (ExcitationSource src : this) {
         if (src.myComp.isWritable()) {
            numw++;
         }
      }
      if (numw == 0) {
         pw.println (name + "=[]");
      }
      else {
         pw.println (name + "=[");
         IndentingPrintWriter.addIndentation (pw, 2);
         for (int i = 0; i < size(); i++) {
            ExcitationSource src = get (i);
            if (src.myComp.isWritable()) {
               String pathName =
                  ComponentUtils.getWritePathName (ancestor, src.myComp);
               pw.println (pathName + " " + fmt.format (src.myGain));
            }
         }
         IndentingPrintWriter.addIndentation (pw, -2);
         pw.println ("]");
      }     
   }

   public void postscan (
      Deque<ScanToken> tokens, CompositeComponent ancestor)
      throws IOException {

      ExcitationComponent[] comps =
         ScanWriteUtils.postscanReferences (
            tokens, ExcitationComponent.class, ancestor);
      if (comps.length != size()) {
         throw new InternalErrorException (
            "scan and postscan excitor lists have different sizes");
      }
      for (int i=0; i<size(); i++) {
         get(i).myComp = comps[i];
         //excitors[i].addExcitationSource (this);
      }        
   }

//   public ListRemove<ExcitationSource> updateReferences (
//      ModelComponent host, Object undoInfo) {
//      if (undoInfo != null) {
//         if (!(undoInfo instanceof ListRemove<?>)) {
//            throw new IllegalStateException (
//               "undoInfo: expecting ListRemove<ExcitationSource>, got " +
//               undoInfo.getClass());
//         }
//         ((ListRemove<ExcitationSource>)undoInfo).undo();
//         return null;
//      }
//      else {
//         ListRemove<ExcitationSource> remove = null;
//         for (int i=0; i<size(); i++) {
//            if (!ComponentUtils.areConnected (
//                   host, get(i).myComp)) {
//               if (remove == null) {
//                  remove = new ListRemove<ExcitationSource>(this);
//               }
//               remove.requestRemove(i);
//            }
//         }
//         if (remove != null) {
//            remove.remove();
//         }
//         return remove;
//      }
//   }
}
