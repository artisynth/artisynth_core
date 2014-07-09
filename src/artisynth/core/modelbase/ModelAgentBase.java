/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.io.*;
import java.util.Deque;

import maspack.util.*;
import artisynth.core.util.*;

/**
 * Base class for Probes, Controllers, and Monitors.
 */
public abstract class ModelAgentBase extends ModelComponentBase
   implements ModelAgent {

   protected Model myModel;

   public void initialize(double t) {
   }
   
   static Model findModel (ModelComponent m) {
      // if m is not a Model, try to locate the most immediate ancestor this is
      while (m != null && !(m instanceof Model)) {
         m = m.getParent();
      }
      return (Model)m;
   }

   public void setModelFromComponent (ModelComponent comp) {
      myModel = findModel (comp);
   }
   
   public void setModel (Model model) {
      myModel = model;
   }

   public Model getModel() {
      return myModel;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
      pw.println ("model=" + ComponentUtils.getWritePathName (
                     ancestor, myModel));      
   }

   public void write (PrintWriter pw, NumberFormat fmt, Object ref)
      throws IOException {
      dowrite (pw, fmt, ref);
   }

   protected boolean scanItem (ReaderTokenizer rtok, Deque<ScanToken> tokens)
      throws IOException {
      
      rtok.nextToken();
      if (scanAndStoreReference (rtok, "model", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {

      if (postscanAttributeName (tokens, "model")) {
         myModel = postscanReference (tokens, Model.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }

   /** 
    * Remove resources used by this agent. Should be overridden by sub-classes
    * as necessary.
    * 
    */
   public void dispose() {
   }

   @Override
   public void finalize() {
      dispose();
   }
   
   

}
