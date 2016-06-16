/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.LinkedList;

import maspack.matrix.Point3d;

public abstract class RenderableBase extends IsRenderableBase
   implements Renderable {

   protected RenderProps myRenderProps;

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return false;
   }

   /**
    * {@inheritDoc}
    */
   public int numSelectionQueriesNeeded() {
      return -1;
   }
   
   /**
    * {@inheritDoc}
    */
   public void getSelection (LinkedList<Object> list, int qid) {
   }

   /**
    * {@inheritDoc}
    */
   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   /**
    * {@inheritDoc}
    */
   public void setRenderProps (RenderProps props) {
      if (props == null) {
         throw new IllegalArgumentException ("Render props cannot be null");
      }
      myRenderProps = createRenderProps();
      myRenderProps.set (props);
   }

}
