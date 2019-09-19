/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.mechmodels;

import java.util.LinkedList;

import maspack.properties.PropertyList;
import maspack.render.Renderer;
import maspack.render.MeshRenderProps;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import artisynth.core.modelbase.RenderableComponentList;

public class MeshComponentList<P extends MeshComponent> 
   extends RenderableComponentList<P> implements HasSlaveObjects {
   
   protected static final long serialVersionUID = 1;

   public static PropertyList myProps =
      new PropertyList (MeshComponentList.class, RenderableComponentList.class);

   static {
      myProps.get ("renderProps").setDefaultValue (new MeshRenderProps());
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public MeshComponentList (Class<P> type) {
      this (type, null, null);
   }
   
  public MeshComponentList (Class<P> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
   }
   
   /**
    * Calls updatePos() for all meshes in this list
    */
   public void updateSlavePos() {
      for (MeshComponent mc : this) {
         mc.updateSlavePos();
      }
   }
   
   public void updateSlaveVel() {
      // nothing to do - meshes don't have velocities
   }

   /* ======== Renderable implementation ======= */

   public RenderProps createRenderProps() {
      return RenderProps.createMeshProps (this);
   }

   public void prerender(RenderList list) {
      for (int i=0; i< size(); i++) {
         MeshComponent mc = get(i);
         if (mc.getRenderProps() != null) {
            list.addIfVisible(mc);
         } else {
            mc.prerender(list);
         }
      }
   }

   public boolean rendersSubComponents() {
      return true;
   }

   public void render(Renderer renderer, int flags) {
      
      boolean selecting = renderer.isSelecting();
      if (isSelected())  {
         flags |= Renderer.HIGHLIGHT;
      }
      
      for (int i=0; i<size(); i++) {
         MeshComponent mc = get(i);
         if (mc.getRenderProps() == null) {
            if (selecting) {
               if (renderer.isSelectable (mc)) {
                  renderer.beginSelectionQuery (i);
                  mc.render (renderer, flags);
                  renderer.endSelectionQuery ();
               }
            }
            else {
               mc.render (renderer, flags);
            }
         }
      }      
   }

   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return true;
   }

   public int numSelectionQueriesNeeded() {
      return size();
   }

//   public void handleSelection (
//      LinkedList<IsRenderable> list, int[] namestack, int idx) {
//      int k = namestack[idx];
//      if (k >= 0 && k < size()) {
//         list.addLast (get (k));
//      }
//   }

   public void getSelection (LinkedList<Object> list, int qid) {
      if (qid >= 0 && qid < size()) {
         list.add (get (qid));
      }
   }

}
