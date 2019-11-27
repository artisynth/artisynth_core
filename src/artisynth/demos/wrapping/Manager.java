package artisynth.demos.wrapping;

import java.util.HashMap;
import java.util.Map;

import artisynth.core.modelbase.ModelComponentBase;
import maspack.properties.Property;
import maspack.properties.PropertyList;

/**
 * A {@code Manager} manages objects.
 * <p>
 * Based on the values of a collection of its {@link Property Properties}, a
 * {@code Manager} decides which of the objects it manages is currently
 * "active".
 *
 * @author Francois Roewer-Despres
 * @param <T>
 * the type of object that is managed
 */
public abstract class Manager<T> extends ModelComponentBase {

   /**
    * Every time the currently "active" object changes, the {@link #update()}
    * method of this {@link Manager}'s {@code Updatable} is called.
    *
    * @author Francois Roewer-Despres
    */
   public static interface Updatable {

      /**
       * A callback called whenever this {@link Manager}'s currently "active"
       * object changes.
       */
      void update ();
   }

   /**
    * Every time this {@link Manager#getActive()} is called, if the managed
    * object does not yet exist, it is created using a {@code Creator}.
    *
    * @author Francois Roewer-Despres
    * @param <T>
    * the type of object that is created
    */
   public static abstract class Creator<T> {
      
      protected Manager<T> myManager;

      /**
       * Creates the currently "active" managed object.
       *
       * @return the created object
       */
      public abstract T create ();
   }

   /** Whether the NONE value of Enums is supported. */
   public static final boolean DEFAULT_SUPPORTS_NONE = false;

   public static PropertyList myProps =
      new PropertyList (Manager.class, ModelComponentBase.class);

   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }

   static {
      myProps.add (
         "supportsNONE",
         "Toggles whether the NONE value of Enums is supported.",
         DEFAULT_SUPPORTS_NONE);
   }

   protected Creator<T> myCreator;
   protected Updatable myUpdatable;
   protected boolean mySupportsNoneP = DEFAULT_SUPPORTS_NONE;
   protected Map<String,T> myManagedObjects = new HashMap<> ();

   /**
    * Creates a new {@link Manager} with the given {@link Creator} and
    * {@link Updatable}.
    *
    * @param creator
    * the {@code Creator} of this {@code Manager}
    * @param updatable
    * the {@code Updatable} of this {@code Manager}
    */
   public Manager (Creator<T> creator, Updatable updatable) {
      this (null, creator, updatable);
   }

   /**
    * Creates a new {@link Manager} with the given name, {@link Creator}, and
    * {@link Updatable}.
    *
    * @param name
    * the name of this {@code Manager}
    * @param creator
    * the {@code Creator} of this {@code Manager}
    * @param updatable
    * the {@code Updatable} of this {@code Manager}
    */
   public Manager (String name, Creator<T> creator, Updatable updatable) {
      setName (name);
      myCreator = creator;
      myUpdatable = updatable;
      myCreator.myManager = this;
   }

   /**
    * Returns whether the NONE value of Enums is supported.
    *
    * @return whether the NONE value of Enums is supported
    */
   public boolean getSupportsNONE () {
      return mySupportsNoneP;
   }

   /**
    * Sets whether the NONE value of Enums is supported to "enable".
    *
    * @param enable
    * the new value for the supportsNONE property
    */
   public void setSupportsNONE (boolean enable) {
      mySupportsNoneP = enable;
   }

   /**
    * Returns the name of the currently "active" managed object.
    *
    * @return the name of the currently "active" managed object
    */
   protected abstract String getActiveName ();

   /**
    * Returns the currently "active" managed object.
    *
    * @return the currently "active" managed object
    */
   public T getActive () {
      String name = getActiveName ();
      if (!myManagedObjects.containsKey (name)) {
         T obj = myCreator.create ();
         myManagedObjects.put (name, obj);
      }
      return myManagedObjects.get (name);
   }
}
