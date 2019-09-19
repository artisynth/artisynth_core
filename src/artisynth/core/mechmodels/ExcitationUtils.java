/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.Stack;
import java.io.PrintWriter;
import java.io.IOException;

import artisynth.core.modelbase.ComponentUtils;
import artisynth.core.modelbase.CompositeComponent;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.ModelComponentBase;
import artisynth.core.modelbase.ScanWriteUtils;
import artisynth.core.mechmodels.ExcitationComponent.CombinationRule;
import artisynth.core.util.ScanToken;
import artisynth.core.util.StringToken;
import maspack.util.InternalErrorException;
import maspack.util.ListRemove;
import maspack.util.ObjectHolder;
import maspack.util.ReaderTokenizer;

/**
 * Support routines for Muscle excitations.
 */
public class ExcitationUtils {
   
   public static double getAncestorNetExcitation (
      ModelComponent comp, int height) {
      
      CompositeComponent ancestor = comp.getParent();
      for (int i=0; i<height && ancestor != null; i++) {
         if (ancestor instanceof ExcitationComponent) {
            return ((ExcitationComponent)ancestor).getNetExcitation();
         }
         ancestor = ancestor.getParent();
      }
      return 0;
   }
   
   public static double combineWithAncestor (
      ExcitationComponent ecomp, ExcitationSourceList sources, 
      int height, CombinationRule rule) {
      double net = ecomp.getExcitation();
      double ea = getAncestorNetExcitation (ecomp, height);
      switch (rule) {
         case Sum: {
            net += ea;
            if (sources != null) {
               for (int i = 0; i < sources.size(); i++) {
                  ExcitationSource src = sources.get(i);
                  net += src.myGain*src.myComp.getNetExcitation();
               }
            }
            break;
         }
         default: {
            throw new InternalErrorException (
               "combination method not implemented for " + rule);
         }
      }
      return net;
   }
   
   public static double combine (
      double e, ExcitationSourceList sources, CombinationRule rule) {
      double net = e;
      if (sources != null) {
         switch (rule) {
            case Sum: {
               for (int i = 0; i < sources.size(); i++) {
                  ExcitationSource src = sources.get(i);
                  net += src.myGain*src.myComp.getNetExcitation();
               }
               break;
            }
            default: {
               throw new InternalErrorException (
                  "combination method not implemented for " + rule);
            }
         }
      }
      return net;
   }
   
//   /**
//    * Look for the first ancestor of an ExcitationComponent, up to
//    * a prescribed height, that happens to also be an ExcitationComponent.
//    * If one is found, add it to the component as a source.
//    * 
//    * @param ecomp Component for which ancestors should be checked
//    * @param height Maximum ancestor height (1 = parent, 2 = grandparent, etc.)
//    */
//   public static void addAncestorAsSource (
//      ExcitationComponent ecomp, int height) {
//      
//      CompositeComponent ancestor = ecomp.getParent();
//      for (int i=0; i<height && ancestor != null; i++) {
//         if (ancestor instanceof ExcitationComponent) {
//            ecomp.addExcitationSource ((ExcitationComponent)ancestor);
//         }
//         ancestor = ancestor.getParent();
//      }
//   }
   
//   /**
//    * Look for the first ancestor of an ExcitationComponent, up to
//    * a prescribed height, that happens to also be an ExcitationComponent.
//    * If one is found, remove it from the component as a source.
//    * 
//    * @param ecomp Component for which ancestors should be checked
//    * @param height Maximum ancestor height (1 = parent, 2 = grandparent, etc.)
//    */
//   public static void removeAncestorAsSource (
//      ExcitationComponent ecomp, int height) {
//      
//      CompositeComponent ancestor = ecomp.getParent();
//      for (int i=0; i<height && ancestor != null; i++) {
//         if (ancestor instanceof ExcitationComponent) {
//            ecomp.removeExcitationSource ((ExcitationComponent)ancestor);
//         }
//         ancestor = ancestor.getParent();
//      }
//   }

//   public static void writeSources (
//      PrintWriter pw, String name, Collection<ExcitationComponent> sources,
//      CompositeComponent ancestor) throws IOException {
//      
//      if (sources != null && sources.size() > 0) {
//         pw.print (name + "=");
//         ScanWriteUtils.writeBracketedReferences (pw, sources, ancestor);
//      }
//   }
//
//   public static ArrayList<ExcitationComponent> postscanSources (
//      Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
//
//      ArrayList<ExcitationComponent> sources =
//         new ArrayList<ExcitationComponent>();
//      ScanWriteUtils.postscanReferences (
//         tokens, sources, ExcitationComponent.class, ancestor);
//      return sources;
//   }

   public static double getGain (
      ExcitationSourceList sources, ExcitationComponent ex) {
      if (sources != null) {
         return sources.getGain (ex);
      }
      else {
         return -1;
      }      
   }

   public static boolean setGain (
      ExcitationSourceList sources, ExcitationComponent ex, double gain) {
      if (sources != null) {
         return sources.setGain (ex, gain);
      }
      else {
         return false;
      }
   }

   public static ExcitationSourceList scan (
      ReaderTokenizer rtok, String name, Deque<ScanToken> tokens)
      throws IOException {
   
      tokens.offer (new StringToken (name, rtok.lineno()));
      ExcitationSourceList list = new ExcitationSourceList();
      rtok.scanToken ('[');
      tokens.offer (ScanToken.BEGIN);
      while (ScanWriteUtils.scanAndStoreReference (rtok, tokens)) {
         // component will be filled in during postscan
         list.add (new ExcitationSource (null, rtok.scanNumber()));
      }
      if (rtok.ttype != ']') {
         throw new IOException ("Expected ']', got " + rtok);
      }
      tokens.offer (ScanToken.END);
      return list.size() > 0 ? list : null;
   }

//   public static ExcitationSourceList updateReferences (
//      ObjectHolder res, ModelComponent host,
//      ExcitationSourceList sources, Object undoInfo) {
//      res.value = null;
//      if (undoInfo != null) {
//         ListRemove<ExcitationSource> remove = 
//            (ListRemove<ExcitationSource>)undoInfo;
//         if (sources == null) {
//            sources = (ExcitationSourceList)remove.getList();
//         }
//         remove.undo();
//      }
//      else if (sources != null) {
//         ListRemove<ExcitationSource> remove = null;
//         for (int i=0; i<sources.size(); i++) {
//            if (!ComponentUtils.haveCommonAncestor (
//                   host, sources.get(i).myComp)) {
//               if (remove == null) {
//                  remove = new ListRemove<ExcitationSource>(sources);
//               }
//               remove.remove(i);
//            }
//         }
//         if (remove != null) {
//            remove.execute();
//            if (sources.size() == 0) {
//               sources = null;
//            }
//         }
//         res.value = remove;
//      }
//      return sources;
//   }

   public static ExcitationSourceList updateReferences (
      ModelComponent host, ExcitationSourceList sources,
      boolean undo, Deque<Object> undoInfo) {

      if (undo) {
         Object obj = undoInfo.removeFirst();
         if (obj != ModelComponentBase.NULL_OBJ) {
            ListRemove<ExcitationSource> remove =
               (ListRemove<ExcitationSource>)obj;
            if (sources == null) {
               sources = (ExcitationSourceList)remove.getList();
            }
            remove.undo();
         }
      }
      else {
         ListRemove<ExcitationSource> remove = null;
         if (sources != null) {
            for (int i=0; i<sources.size(); i++) {
               if (!ComponentUtils.areConnected (
                      host, sources.get(i).myComp)) {
                  if (remove == null) {
                     remove = new ListRemove<ExcitationSource>(sources);
                  }
                  remove.requestRemove(i);
               }
            }
         }
         if (remove != null) {
            remove.remove();
            if (sources.size() == 0) {
               sources = null;
            }
            undoInfo.addLast (remove);
         }
         else {
            undoInfo.addLast (ModelComponentBase.NULL_OBJ);
         }
      }
      return sources;
   }

}


