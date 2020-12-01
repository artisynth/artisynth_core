/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.*;

import maspack.properties.*;
import maspack.render.*;
import maspack.matrix.*;

public class RenderableComponentList<C extends ModelComponent> extends
ComponentList<C> implements RenderableComponentListView<C>, RenderableComponent {

   public static PropertyList myProps =
      new PropertyList (RenderableComponentList.class, ComponentList.class);

   protected RenderProps myRenderProps;

   static {
      myProps.add (
         "renderProps * *", "render properties for this component",
         Property.AutoValue);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public RenderableComponentList (Class<C> type) {
      this (type, null, null);
   }

   public RenderableComponentList (Class<C> type, String name) {
      super (type, name);
      setRenderProps (createRenderProps());
   }

   public RenderableComponentList (Class<C> type, String name, String shortName) {
      super (type, name, shortName);
      setRenderProps (createRenderProps());
   }

   // public RenderableComponentList (
   // Class type, String name, String shortName,
   // CompositeComponent parent)
   // {
   // super(type, name, shortName, parent);
   // }

   public RenderProps getRenderProps() {
      return myRenderProps;
   }

   public RenderProps createRenderProps() {
      return RenderProps.createRenderProps (this);
   }

   /**
    * Sets the render properties for this object.
    * 
    * @param props
    * new render properties for this object
    */
   public void setRenderProps (RenderProps props) {
      myRenderProps =
         RenderableComponentBase.updateRenderProps (this, myRenderProps, props);
   }

   public void updateBounds (Vector3d pmin, Vector3d pmax) {
      for (int i = 0; i < size(); i++) {
         ModelComponent comp = get (i);
         if (comp instanceof Renderable) {
            ((Renderable)comp).updateBounds (pmin, pmax);
         }
      }
   }

   public void prerender (RenderList list) {
      for (int i = 0; i < size(); i++) {
         ModelComponent comp = get (i);
         if (comp instanceof Renderable) {
            list.addIfVisible ((Renderable)comp);
         }
      }
   }

   /**
    * Queries if this list automatically renders any sub-components which do
    * not have their own render props.
    *
    * @return <code>true</code> if this list renders sub-components
    * without render props
    */
   public boolean rendersSubComponents() {
      return false;
   }

   public void render (Renderer renderer, int flags) {
   }

   public int getRenderHints() {
      int code = 0;
      if (myRenderProps != null && myRenderProps.isTransparent()) {
         code |= TRANSPARENT;
      }
      return code;
   }

   public void getSelection (LinkedList<Object> list, int qid) {
   }
   
   /**
    * {@inheritDoc}
    */
   public boolean isSelectable() {
      return false;
   }
   
   public int numSelectionQueriesNeeded() {
      return -1;
   }

   

}
