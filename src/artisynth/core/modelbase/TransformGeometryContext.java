/**
 * Copyright (c) 2015, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.modelbase;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import maspack.geometry.GeometryTransformer;
import maspack.matrix.AffineTransform3dBase;

/**
 * Manages the geometry transformation for a set of transformable components.
 * Maintains a set of all the components being transformed, along with
 * actions that should be called after their 
 * {@link TransformableGeometry#transformGeometry} methods have been
 * called. The transform operation is carried out by the context's 
 * {@link #apply} method.
 */
public class TransformGeometryContext {
   
   public static final int IS_TRANSFORMED = 0x0001;

   private LinkedHashMap<TransformableGeometry,Integer> myTransformables;
   private LinkedHashSet<TransformGeometryAction> myActions;
   private LinkedHashSet<CompositeComponent> myNotifyParents;

   /**
    * Creates a new TransformGeometryContext.
    */
   public TransformGeometryContext() {
      myTransformables = new LinkedHashMap<TransformableGeometry,Integer>();
      myActions = new LinkedHashSet<TransformGeometryAction>();
      myNotifyParents = new LinkedHashSet<CompositeComponent>();
   }

   /**
    * Creates a new TransformGeometryContext and adds a single
    * transformable component to it.
    * 
    * @param tg transformable component to add
    */
   public TransformGeometryContext (TransformableGeometry tg) {
      this();
      add (tg);
   }

   /**
    * Creates a new TransformGeometryContext and adds a set of
    * transformable components to it.
    * 
    * @param tgens transformable components to add
    */
   public TransformGeometryContext (
      Iterable<? extends TransformableGeometry> tgens) {
      this();
      for (TransformableGeometry tg : tgens) {
         add (tg);
      }
   }

   /**
    * Creates a new TransformGeometryContext and adds a set of
    * transformable components to it.
    * 
    * @param tgens transformable components to add
    */
   public TransformGeometryContext (TransformableGeometry[] tgens) {
      this();
      for (TransformableGeometry tg : tgens) {
         add (tg);
      }
   }

   /**
    * Adds a transformable component to this context. Components are
    * stored as a set, so the same component cannot be added twice.
    * 
    * @param tg transformable component to add
    */
   public void add (TransformableGeometry tg) {
      myTransformables.put (tg, 0);
   }

   /**
    * Adds a transformable component to this context, with the specified
    * flags settings.
    * 
    * @param tg transformable component to add
    * @param flags flag settings for the component
    * @see #add(TransformableGeometry)
    */
   public void add (TransformableGeometry tg, int flags) {
      myTransformables.put (tg, flags);
   }

  /**
    * Adds a set of transformable components to this context.
    * 
    * @param tgens transformable components to add
    * @see #add(TransformableGeometry)
    */
   public void addAll (Collection<? extends TransformableGeometry> tgens) {
      for (TransformableGeometry o : tgens) {
         myTransformables.put (o, 0);
      }
   }

   /**
    * Returns <code>true</code> if a specified transformable component
    * is contained in this context.
    * 
    * @param tg transformable component to check
    * @return <code>true</code> if <code>tg</code> is 
    * contained in this context.
    */
   public boolean contains (TransformableGeometry tg) {
      return myTransformables.containsKey (tg);
   }

   /**
    * Returns <code>true</code> if all members of a set of transformable 
    * components are contained in this context.
    * 
    * @param tgens transformable components to check
    * @return <code>true</code> if all components are 
    * contained in this context.
    */
   public boolean containsAll (Iterable<? extends TransformableGeometry> tgens) {
      for (TransformableGeometry tg : tgens) {
         if (!contains (tg)) {
            return false;
         }
      }
      return true;
   }            

   /**
    * Returns <code>true</code> if all members of a set of transformable 
    * components are contained in this context.
    * 
    * @param tgens transformable components to check
    * @return <code>true</code> if all components are 
    * contained in this context.
    */
   public boolean containsAll (TransformableGeometry[] tgens) {
      for (TransformableGeometry tg : tgens) {
         if (!contains (tg)) {
            return false;
         }
      }
      return true;
   }            

   /**
    * Returns a set view of all the transformable components contained in this
    * context.
    * 
    * @return all transformables in this context.
    */
   public Set<TransformableGeometry> getTransformables() {
      return myTransformables.keySet();
   }

   /**
    * Returns <code>true</code> if any member of a set of transformable 
    * components is contained in this context.
    * 
    * @param tgens transformable components to check
    * @return <code>true</code> if any of the components are 
    * contained in this context.
    */
   public boolean containsAny (Iterable<? extends TransformableGeometry> tgens) {
      for (TransformableGeometry tg : tgens) {
         if (contains (tg)) {
            return true;
         }
      }
      return false;
   }            

   /**
    * Returns <code>true</code> if any member of a set of transformable 
    * components is contained in this context.
    * 
    * @param tgens transformable components to check
    * @return <code>true</code> if any of the components are 
    * contained in this context.
    */
   public boolean containsAny (TransformableGeometry[] tgens) {
      for (TransformableGeometry tg : tgens) {
         if (contains (tg)) {
            return true;
         }
      }
      return false;
   }            

   /**
    * Returns the number of transformable components in this context.
    * 
    * @return number of transformable components.
    */
   public int numTransformables() {
      return myTransformables.size();
   }

   /**
    * Returns <code>true</code> if a specified transformable component
    * is marked as having been transformed. Components are automatically
    * marked as having been transformed after their  
    * {@link TransformableGeometry#transformGeometry} method
    * is called within this context's {@link #apply} method. Components
    * can also be marked as being transformed using 
    * {@link #markTransformed}. The {@link #apply} method will not
    * call the {@link TransformableGeometry#transformGeometry} method
    * for any component which is already marked as transformed.
    * 
    * @param tg component to check
    * @return <code>true</code> if <code>tg</code> is marked as transformed
    */
   public boolean isTransformed (TransformableGeometry tg) {
      return ((getFlags(tg) & IS_TRANSFORMED) != 0);
   }      

   /**
    * Mark a specified transformable component as having been transformed.
    * 
    * @param tg component to mark as transformed
    */
   public void markTransformed (TransformableGeometry tg) {
      add (tg, getFlags(tg) | IS_TRANSFORMED);
   }      

   /**
    * Returns <code>true</code> if all members of a specified set of
    * transformable components are marked as having been transformed.
    * For more information on what this means, see {@link 
    * #isTransformed}.
    * 
    * @param tgens components to check
    * @return <code>true</code> if all components are marked as transformed
    */
   public boolean allTransformed (
      Collection<? extends TransformableGeometry> tgens) {
      for (TransformableGeometry o : tgens) {
         if ((getFlags(o) & IS_TRANSFORMED) != 0) {
            return false;
         }
      }
      return true;
   }      

   /**
    * Returns <code>true</code> if all members of a specified set of
    * transformable components are marked as having been transformed.
    * For more information on what this means, see {@link 
    * #isTransformed}.
    * 
    * @param tgens components to check
    * @return <code>true</code> if all components are marked as transformed
    */
   public boolean allTransformed (TransformableGeometry[] tgens) {
      for (TransformableGeometry o : tgens) {
         if ((getFlags(o) & IS_TRANSFORMED) != 0) {
            return false;
         }
      }
      return true;
   }      

   /**
    * Adds an action to be called within the {@link #apply} method
    * after the {@link TransformableGeometry#transformGeometry}
    * methods have been called for all the transformables. Actions
    * are stored as a set, so the same action can only be
    * added once.
    * 
    * @param action action to be called after all
    * <code>transformGeometry</code> methods.
    */
   public void addAction (TransformGeometryAction action) {
      myActions.add (action);
   }

   /**
    * Returns <code>true</code> if this context contains the specified
    * action.
    * 
    * @param action action to be checked
    * @return <code>true</code> if this context contains <code>action</code> 
    */
   public boolean containsAction (TransformGeometryAction action) {
      return myActions.contains (action);
   }

   /**
    * Adds a parent component to be notified of the change in geometry
    * using a {@link GeometryChangeEvent}. More information about when
    * the notification occurs is contained in the documentation for
    * the {@link #apply} method. Parents to be notified are stored as a set,
    * and so cannot be added twice.
    * 
    * @param comp parent component to be notified
    */
   public void addParentToNotify (CompositeComponent comp) {
      if (comp != null) {
         myNotifyParents.add (comp);
      }
   }

   /**
    * Returns <code>true</code> if this context contains the
    * specified parent component to notified of the change in geometry.
    * 
    * @param comp component to be tested
    * @return <code>true</code> if <code>comp</code> is contained
    * in this context
    */
   public boolean containsParentToNotify (CompositeComponent comp) {
      return myNotifyParents.contains (comp);
   }

   protected int getFlags (TransformableGeometry tg) {
      Integer flags = myTransformables.get (tg);
      return (flags != null ? flags.intValue() : 0);
   }

   private boolean isTransforming (ModelComponent comp) {
      return (comp instanceof TransformableGeometry &&
              contains ((TransformableGeometry)comp));
   }

   private void clearParents() {
      myNotifyParents.clear();
   }
   
   private void collectParents() {
      for (TransformableGeometry tg : getTransformables()) {
         if (tg instanceof ModelComponent) {
            CompositeComponent parent = ((ModelComponent)tg).getParent();
            if (parent != null && !isTransforming(parent)) {
               myNotifyParents.add (parent);
            }
         }
      }      
   }
   
   private void notifyParentsOfChange (GeometryTransformer gtr) {
      // try to optimize by removing notifications of ancestors which
      // will get notified anyway by descendants ...
      ArrayList<CompositeComponent> allParents =
         new ArrayList<CompositeComponent>();
      allParents.addAll (myNotifyParents);
      for (int i=0; i<allParents.size(); i++) {
         CompositeComponent p = allParents.get(i).getParent();
         while (p != null) {
            if (myNotifyParents.contains(p)) {
               myNotifyParents.remove (p);
            }
            p = p.getParent();
         }
      }
      GeometryChangeEvent e = new GeometryChangeEvent (null, gtr);
      for (CompositeComponent parent : myNotifyParents) {
         parent.componentChanged (e);
      }
   }

   /**
    * Applies a geometry transformation to all the transformable
    * components currently stored in this context. This involves
    * the following steps:
    * 
    * <ul>
    * <li>Call {@link TransformableGeometry#addTransformableDependencies} for
    * each transformable to add additional components that need
    * to be transformed, such as descendants or certain connected components.
    * 
    * <li>Call {@link TransformableGeometry#transformGeometry} for
    * each transformable, and then use {@link #markTransformed} to
    * mark that component as being transformed. The call is skipped
    * for components that have already been marked as being transformed.
    * This gives the components some ability to control the transform
    * process: within the <code>transformGeometry</code> method for
    * component A, it may be desirable to call the 
    * <code>transformGeometry</code> method for another component B
    * directly. Component A may do so, and then mark B as being transformed.
    * 
    * <li>Send a {@link GeometryChangeEvent} to any parent components
    * for which notification was requested (using {@link #addParentToNotify})
    * during step 2.
    * 
    * <li>Call the {@link TransformGeometryAction#transformGeometry}
    * method of any actions that were requested (
    * using {@link TransformGeometryContext#addAction})
    * by the <code>transformGeometry</code> methods called during step 2.
    * The actions themselves may request additional actions; the process
    * will continue until all actions have been called.
    * 
    * <li>Send a {@link GeometryChangeEvent} to all the parents of
    * the transformable components, along with any additional parent
    * components for which notification was requested during step 4.
    * 
    * </ul><p>
    * 
    * @param gtr transformation to apply
    * @param flags specifies conditions associated with the transformation
    */
   public void apply (GeometryTransformer gtr, int flags) {

      ArrayList<TransformableGeometry> tgens =
         new ArrayList<TransformableGeometry>();
      tgens.addAll (myTransformables.keySet());
      for (TransformableGeometry tg : tgens) {
         tg.addTransformableDependencies (this, flags);
      }
      tgens.clear();
      tgens.addAll (myTransformables.keySet());
      for (TransformableGeometry tg : tgens) {
         int tgflags = getFlags(tg);
         if ((tgflags & IS_TRANSFORMED) == 0) {
            tg.transformGeometry (gtr, this, flags);
            add (tg, tgflags | IS_TRANSFORMED);
         }
      }
      notifyParentsOfChange (gtr);
      clearParents();
      ArrayList<TransformGeometryAction> actions =
         new ArrayList<TransformGeometryAction>();
      actions.addAll (myActions);
      while (actions.size() > 0) {
          for (TransformGeometryAction a : actions) {    
             a.transformGeometry (gtr, this, flags);
             myActions.remove (a);
          }
          actions.clear();
          actions.addAll (myActions);
      }
      collectParents();
      notifyParentsOfChange (gtr);
   }
   
   /**
    * Adds transformable descendants of a composite component to this context.
    * The method searches recursively for transformable components among
    * the descendants of <code>comp</code>. When a transformable is
    * found, it is added to this context, and its 
    * {@link TransformableGeometry#addTransformableDependencies} method
    * is called to add any dependencies of that component to this context.
    *  
    * @param comp component for which transformable descendants should be added
    * @param flags specifies conditions associated with the transformation
    */
   public void addTransformableDescendants (
      CompositeComponent comp, int flags) {
      
      for (int i=0; i<comp.numComponents(); i++) {
         ModelComponent c = comp.get (i);
         if (c instanceof TransformableGeometry) {
            TransformableGeometry tg = (TransformableGeometry)c;
            add (tg);
            tg.addTransformableDependencies (this, flags);            
         }
         else if (c instanceof CompositeComponent) {
            addTransformableDescendants (((CompositeComponent)c), flags);
         }
      }
   }

   /**
    * Applies a geometry transform to a specified transformable component.
    * A context is created for the component and its {@link #apply} method
    * is called.
    * 
    * @param tg component to transform
    * @param gtr transformer to effect the transformation
    * @param flags specifies conditions associated with the transformation
    */
   public static void transform (
      TransformableGeometry tg, GeometryTransformer gtr, int flags) {
      TransformGeometryContext context = new TransformGeometryContext (tg);
      context.apply (gtr, flags);
   }

   /**
    * Applies a geometry transform to a specified set of transformable 
    * components. A context is created for the component and its 
    * {@link #apply} method is called.
    * 
    * @param tgens components to transform
    * @param gtr transformer to effect the transformation
    * @param flags specifies conditions associated with the transformation
    */
   public static void transform (
      Iterable<TransformableGeometry> tgens, 
      GeometryTransformer gtr, int flags) {
      
      TransformGeometryContext context = new TransformGeometryContext (tgens);
      context.apply (gtr, flags);
   }

   /**
    * Applies a geometry transform to a specified set of transformable 
    * components. A context is created for the component and its 
    * {@link #apply} method is called.
    * 
    * @param tgens components to transform
    * @param gtr transformer to effect the transformation
    * @param flags specifies conditions associated with the transformation
    */
   public static void transform (
      TransformableGeometry[] tgens, GeometryTransformer gtr, int flags) {
      TransformGeometryContext context = new TransformGeometryContext (tgens);
      context.apply (gtr, flags);
   }

   /**
    * Applies a rigid or affine transform to a specified transformable 
    * component. A context is created for the component and its {@link #apply}
    * method is called using a transformer created for <code>X</code>.
    * 
    * @param tg component to transform
    * @param X rigid or affine transform (used to create the transformer)
    * @param flags specifies conditions associated with the transformation
    */
   public static void transform (
      TransformableGeometry tg, AffineTransform3dBase X, int flags) {
      TransformGeometryContext context = new TransformGeometryContext (tg);
      context.apply (GeometryTransformer.create(X), flags);
   }

   /**
    * Applies a rigid or affine transform to a specified set of transformable 
    * components. A context is created for the components and its 
    * {@link #apply} method is called using a transformer created for 
    * <code>X</code>.
    * 
    * @param tgens components to transform
    * @param X rigid or affine transform (used to create the transformer)
    * @param flags specifies conditions associated with the transformation
    */
   public static void transform (
      Iterable<TransformableGeometry> tgens, 
      AffineTransform3dBase X, int flags) {
      
      TransformGeometryContext context = new TransformGeometryContext (tgens);
      context.apply (GeometryTransformer.create(X), flags);
   }

   /**
    * Applies a rigid or affine transform to a specified set of transformable 
    * components. A context is created for the components and its 
    * {@link #apply} method is called using a transformer created for 
    * <code>X</code>.
    * 
    * @param tgens components to transform
    * @param X rigid or affine transform (used to create the transformer)
    * @param flags specifies conditions associated with the transformation
    */
   public static void transform (
      TransformableGeometry[] tgens, AffineTransform3dBase X, int flags) {
      TransformGeometryContext context = new TransformGeometryContext (tgens);
      context.apply (GeometryTransformer.create(X), flags);
   }


   
}
