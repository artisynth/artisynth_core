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

public class MultiPointSpringList<S extends MultiPointSpring>
   extends PointSpringList<S> {

   protected static final long serialVersionUID = 1;

   public MultiPointSpringList (Class<S> type) {
      this (type, null, null);
   }
   
   public MultiPointSpringList (Class<S> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
   }

   public void render (Renderer renderer, int flags) {
      for (int i=0; i<size(); i++) {
         MultiPointSpring spr = get (i);
         if (spr.getRenderProps() == null) {
            if (renderer.isSelecting()) {
               renderer.beginSelectionQuery (i);
            }
            spr.dorender (renderer, myRenderProps);
            if (renderer.isSelecting()) {
               renderer.endSelectionQuery ();
            }
         }
      }
   }
}
