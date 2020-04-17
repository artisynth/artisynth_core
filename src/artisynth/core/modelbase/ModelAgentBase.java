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
import maspack.properties.*;
import artisynth.core.util.*;

/**
 * Base class for Probes, Controllers, and Monitors.
 */
public abstract class ModelAgentBase extends ModelComponentBase
   implements ModelAgent {

   protected Model myModel;
   
   protected static boolean defaultActiveP = true;
   protected boolean myActiveP = defaultActiveP;
   
   public static PropertyList myProps =
      new PropertyList (ModelAgentBase.class, ModelComponentBase.class);

   static {
      myProps.add (
         "active isActive *", 
         "true if this model agent is active", defaultActiveP);
   }
   
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public boolean isActive() {
      return myActiveP;
   }
   
   public void setActive (boolean enable) {
      if (enable != myActiveP) {
         myActiveP = enable;
         // changing activity changes whether or not probe has state 
         notifyParentOfChange (new StructureChangeEvent(this));
      }
   }

   public void initialize(double t) {
   }
   
   static Model findModel (ModelComponent c) {
      // If c is not a Model, try to locate the nearest ancestor that is
      Model model = null;
      while (c != null && !(c instanceof Model)) {
         c = c.getParent();
      }
      if (c == null) {
         return null;
      }
      else {
         model = (Model)c;
      }
      // now try to find a higher model that is not a root model
      for (c=c.getParent(); c != null; c=c.getParent()) {
         if (c instanceof Model) {
            model = (Model)c;
         }
      }
      return model;
   }

   public void setModelFromComponent (ModelComponent comp) {
      myModel = findModel (comp);
   }
   
   public void setModel (Model model) {
      if (model != myModel) {
         myModel = model;
         notifyParentOfChange (
            new StructureChangeEvent (this, /*stateIsChanged=*/false));
      }
   }

   public Model getModel() {
      return myModel;
   }

   protected void writeItems (
      PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
      throws IOException {

      //getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt);
      getAllPropertyInfo().writeNonDefaultProps (this, pw, fmt, ancestor);
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

   protected NumericState castToNumericState (ComponentState state) {
      if (!(state instanceof NumericState)) {
         throw new IllegalArgumentException (
            "state not an instance of NumericState");
      }
      return (NumericState)state;
   }
   
   

}
