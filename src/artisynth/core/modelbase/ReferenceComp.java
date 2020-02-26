/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.io.*;
import java.util.*;

import maspack.util.*;
import artisynth.core.util.*;

/**
 * A simple component that provides a reference to another component. Used as
 * the building block for ReferenceLists.
 */
public class ReferenceComp<C extends ModelComponent> 
   extends ModelComponentBase {
   
   C myRef;

   public ReferenceComp (C ref) {
      myRef = ref;
   }
   
   public ReferenceComp () {
      this (null);
   }
   
   public C getReference() {
      return myRef;
   }

   public void setReference (C ref) {
      if (ref != myRef) {
         myRef = ref;
      }      
   }

   /**
    * {@inheritDoc}
    */
   public void getHardReferences (List<ModelComponent> refs) {
      if (myRef != null) {
         refs.add (myRef);
      }
   }

   public boolean scanItem (
   ReaderTokenizer rtok, Deque<ScanToken> tokens) throws IOException {
      rtok.nextToken();
      if (scanAndStoreReference (rtok, "ref", tokens)) {
         return true;
      }
      rtok.pushBack();
      return super.scanItem (rtok, tokens);
   }

   protected boolean postscanItem (
   Deque<ScanToken> tokens, CompositeComponent ancestor) throws IOException {
      if (postscanAttributeName (tokens, "ref")) {
         myRef = (C)postscanReference (tokens, ModelComponent.class, ancestor);
         return true;
      }
      return super.postscanItem (tokens, ancestor);
   }
  
   public void writeItems (
   PrintWriter pw, NumberFormat fmt, CompositeComponent ancestor)
   throws IOException {
      super.writeItems (pw, fmt, ancestor);
      if (myRef != null) {
         pw.println ("ref=" + ComponentUtils.getWritePathName (ancestor, myRef));
      }
   }
   
}
