package artisynth.core.opensim.components;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import org.w3c.dom.Element;

import maspack.util.ClassFinder;
import maspack.util.Logger;

/**
 * Stores a set of OpenSimComponentFactory objects for use in parsing OpenSim files
 */
public class OpenSimObjectFactoryStore {

   // set of factory classes for given component
   private static HashSet<Class<? extends OpenSimObjectFactory<? extends OpenSimObject>>> 
      myFactoryClasses;
   
   private static HashSet<Class<? extends OpenSimObjectFactory<? extends OpenSimObject>>>  findFactoryClasses() {
      
      // factory set to populate
      HashSet<Class<? extends OpenSimObjectFactory<? extends OpenSimObject>>>  factories = new HashSet<>();
      
      // search current package
      ArrayList<Class<?>> classes = ClassFinder.findClasses (OpenSimObjectFactoryStore.class.getPackage (), OpenSimObjectFactory.class);
      for (Class<?> clazz : classes) {

         // if not abstract or an interface, add the factory to the list
         if (!Modifier.isAbstract(clazz.getModifiers()) 
            && !Modifier.isInterface(clazz.getModifiers())) {

            // instantiate new factory
            @SuppressWarnings("unchecked")
            Class<? extends OpenSimObjectFactory<? extends OpenSimObject>> factoryClass = (Class<? extends OpenSimObjectFactory<? extends OpenSimObject>>)clazz;
            
            // add factory class
            factories.add (factoryClass);
          
         } // non-abstract class
      } // all classes
      
      return factories;
   }
   
   /**
    * Retrieves set of registered classes
    * @return factory classes
    */
   private static HashSet<Class<? extends OpenSimObjectFactory<? extends OpenSimObject>>> getFactoryClasses() {
      if (myFactoryClasses == null) {
         myFactoryClasses = findFactoryClasses();
      }
      return myFactoryClasses;
   }
   
   /**
    * Register a factory class for parsing OpenSimComponent's
    *
    * @param factoryClass factory class
    */
   public static void registerFactoryClass(Class<? extends OpenSimObjectFactory<? extends OpenSimObject>> factoryClass) {
      getFactoryClasses ().add (factoryClass);
   }
   
   // list of factories
   private HashMap<Class<? extends OpenSimObject>, OpenSimObjectFactory<? extends OpenSimObject>> myFactories;
   
   public OpenSimObjectFactoryStore() {
      myFactories = null;
   }
   
   /**
    * Returns a map of default component factories, building the map if required
    * @return factory map
    */
   private HashMap<Class<? extends OpenSimObject>, OpenSimObjectFactory<? extends OpenSimObject>> getFactories() {
      if (myFactories == null) {
         myFactories = createDefaultFactoryInstances();
      }
      return myFactories;
   }
   
   /**
    * Sets the factory to use for parsing specific components
    * @param componentClass component type to create
    * @param factory factory object
    */
   public <C extends OpenSimObject> void setFactory(Class<C> componentClass, OpenSimObjectFactory<? extends C> factory) {
      getFactories ().put (componentClass, factory);
      factory.setFactoryStore (this);
   }
   
   /**
    * Gets a factory for parsing the given component type
    * @param component component class
    */
   public <C extends OpenSimObject> OpenSimObjectFactory<? extends C> getFactory(Class<C> component) {
      @SuppressWarnings("unchecked")
      OpenSimObjectFactory<? extends C> factory = (OpenSimObjectFactory<? extends C>)getFactories().get(component);
      return factory;
   }
   
   
   /**
    * Creates a set of default factory instances from the set of registered factory classes
    * @return new list of default instances
    */
   private HashMap<Class<? extends OpenSimObject>, OpenSimObjectFactory<? extends OpenSimObject>> createDefaultFactoryInstances() {
      HashMap<Class<? extends OpenSimObject>, OpenSimObjectFactory<? extends OpenSimObject>> factories = new HashMap<> ();

      for (Class<? extends OpenSimObjectFactory<? extends OpenSimObject>> factoryClass : getFactoryClasses ()) {
        
         try {
            OpenSimObjectFactory<? extends OpenSimObject> factory = factoryClass.newInstance ();
            factory.setFactoryStore (this);
            factories.put (factory.getComponentClass (), factory);
         }
         catch (Exception | Error e) {}
         
      }
      
      return factories;
   }
   

   /**
    * Finds a factory that can parse a given element
    * @param baseClass base class of component to generate
    * @param elem element to parse
    * @return factory or null if none exists
    */
   public <F extends OpenSimObject> OpenSimObjectFactory<? extends F> findFactory(Class<F> baseClass, Element elem) {
      
      for (Entry<Class<? extends OpenSimObject>, OpenSimObjectFactory<? extends OpenSimObject>> entry : getFactories ().entrySet ()) {
         Class<? extends OpenSimObject> componentClass = entry.getKey ();
         
         if (baseClass.isAssignableFrom (componentClass)) {
            OpenSimObjectFactory<? extends OpenSimObject> factory = entry.getValue ();
            if (factory.canParse (elem)) {
               @SuppressWarnings("unchecked")
               OpenSimObjectFactory<? extends F> out = (OpenSimObjectFactory<? extends F>)factory;
               return out;
            }
         }
      }
      
      Logger.getSystemLogger ().warn ("Cannot find factory for " + elem.getNodeName ());
      
      return null;      
   }

   /**
    * Finds a factory that can parse a given element
    * @param elem element to parse
    * @return factory or null if none exists
    */
   public OpenSimObjectFactory<? extends OpenSimObject> findFactory(Element elem) {
      
      for (Entry<Class<? extends OpenSimObject>, OpenSimObjectFactory<? extends OpenSimObject>> entry : getFactories ().entrySet ()) {

         OpenSimObjectFactory<? extends OpenSimObject> factory = entry.getValue ();
         if (factory.canParse (elem)) {
            return factory;
         }

      }
      
      return null;
      
   }


}
