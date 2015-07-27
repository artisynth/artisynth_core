/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.awt.Color;
import java.io.*;

import maspack.render.RenderList;
import maspack.render.*;
import maspack.render.Renderable;
import maspack.util.*;
import artisynth.core.modelbase.*;
import maspack.render.*;
import maspack.properties.*;
import artisynth.core.util.*;
import java.util.*;
import com.jogamp.opengl.*;

public class AxialSpringList<S extends AxialSpring> extends PointSpringList<S> {

   protected static final long serialVersionUID = 1;

   public AxialSpringList (Class<S> type) {
      this (type, null, null);
   }
   
   public AxialSpringList (Class<S> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
   }

   public int numSelectionQueriesNeeded() {
      return size();
   }

   public void prerender (RenderList list) {
      for (int i=0; i<size(); i++) {
         PointSpringBase spr = get (i);
         if (spr.getRenderProps() != null) {
            list.addIfVisible (spr);
         }
         else {
            // spring will be rendered directly by this list, but call
            // prerender directly because we may still need to set things there
            spr.prerender (list);
         }
      }
   }

   public boolean rendersSubComponents() {
      return true;
   }

   public void render (Renderer renderer, int flags) {
      renderer.drawLines (myRenderProps, iterator());
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < size()) {
         list.addLast (get (qid));
      }
   }

}
