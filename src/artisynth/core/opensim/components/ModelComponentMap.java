package artisynth.core.opensim.components;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import artisynth.core.modelbase.ModelComponent;

/**
 * Helper class for relating OpenSimObjects to ArtiSynth ModelComponents, for use in
 * building models
 */
public class ModelComponentMap implements Map<OpenSimObject,ModelComponent>{

   private HashMap<String,ArrayList<OpenSimObject>> nameMap;
   private HashMap<String,OpenSimObject> pathMap;
   private HashMap<OpenSimObject,ModelComponent> componentMap;
   
   public ModelComponentMap() {
      nameMap = new HashMap<> ();
      pathMap = new HashMap<> ();
      componentMap = new HashMap<>();
   }
   
   /**
    * Adds a mapping from name to object if name is not null, so we can
    * later search for an item by name
    * @param name object name
    * @param obj object
    */
   private void addToNameMap(String name, OpenSimObject obj) {
      if (name != null) {
         ArrayList<OpenSimObject> olist = nameMap.get (name);
         if (olist == null) {
            olist = new ArrayList<> ();
            nameMap.put (name, olist);
         }
         olist.add (obj);
      }
   }
   
   /**
    * Finds the first instance of an object assignable to baseClass with the provided name
    * @param baseClass base class for type conversion
    * @param name object name
    * @return object if found, null otherwise
    */
   public <O extends OpenSimObject> O findObjectByName(Class<O> baseClass, String name) {
      ArrayList<OpenSimObject> olist = nameMap.get (name);
      
      if (olist != null) {
         for (OpenSimObject obj : olist) {
            if (baseClass.isAssignableFrom (obj.getClass ())) {
               @SuppressWarnings("unchecked")
               O out = (O)obj;
               return out;
            }
         }
      }
      return null;
   }
   
   /**
    * Finds the first instance of an object assignable to baseClass with the provided path
    * or name
    * @param baseClass base class for type conversion
    * @param ref reference object for resolving relative paths
    * @param pathOrName object path or name
    * @return object if found, null otherwise
    */
   public <O extends OpenSimObject> O findObjectByPathOrName(Class<O> baseClass, OpenSimObject ref, String pathOrName) {
      
      OpenSimObject obj = findObjectByPath (ref, pathOrName);
      if (obj != null && baseClass.isAssignableFrom (obj.getClass ())) {
         @SuppressWarnings("unchecked")
         O out = (O)obj;
         return out;
      }
      return findObjectByName (baseClass, pathOrName);
   }
   
   /**
    * Finds the first instance of an object with the provided name
    * @param name object name
    * @return object if found, null otherwise
    */
   public OpenSimObject findObjectByName(String name) {
      ArrayList<OpenSimObject> olist = nameMap.get (name);
      
      OpenSimObject obj = null;
      if (olist != null) {
         if (olist != null && olist.size () > 0) {
            obj = olist.get (0);
         }
      }
      
      return obj;
   }
   
   /**
    * Adds a mapping from a fully-qualified path to an object
    * @param path full path
    * @param obj object
    */
   private void addToPathMap(String path, OpenSimObject obj) {
      if (path != null) {
         pathMap.put (path, obj);
      }
   }
   
   /**
    * Returns a component by its fully qualified path
    * @param obj object from which path is requested (for relative path resolution)
    * @param path absolute or relative path
    * @return component at path if exists
    */
   public OpenSimObject findObjectByPath(OpenSimObject obj, String path) {
      if (!path.startsWith ("/")) {
         // relative path
         String parentPath = obj.getPath ();
         if (parentPath != null) {
            if (!parentPath.endsWith ("/")) {
               path = parentPath + "/" + path;
            } else {
               path = parentPath + path;
            }
            // normalize url
            try {
              URI uri = new URI(path);
              path = uri.normalize ().toString ();
            }
            catch (URISyntaxException e) {
               e.printStackTrace();
            }
         }
      }
      if (path != null) {
         // trim trailing slash
         if (path.length() > 1 && path.endsWith ("/")) {
            path = path.substring (0, path.length ()-1);
         }
         return pathMap.get (path);
      }
      return null;
   }
   
   /**
    * Adds the component to the map
    * @param obj OpenSimObject
    * @param comp corresponding ModelComponent
    */
   public ModelComponent put(OpenSimObject obj, ModelComponent comp) {

      ModelComponent old = componentMap.put (obj, comp);
      // add name and path if not already there
      if (old == null) {
         addToNameMap (obj.getName (), obj);
         addToPathMap (obj.getPath ().toString (), obj);
      }
      
      return old;
      
   }
   
   /**
    * Gets a model component by OpenSimObject
    * @param obj object
    * @return corresponding model component if it exists
    */
   public ModelComponent get(OpenSimObject obj) {
      return componentMap.get (obj);
   }

   @Override
   public int size () {
      return componentMap.size ();
   }

   @Override
   public boolean isEmpty () {
      return componentMap.isEmpty ();
   }

   @Override
   public boolean containsKey (Object key) {
      return componentMap.containsKey (key);
   }

   @Override
   public boolean containsValue (Object value) {
      return componentMap.containsValue (value);
   }

   @Override
   public ModelComponent get (Object key) {
      return componentMap.get (key);
   }

   @Override
   public ModelComponent remove (Object key) {
      
      if (key == null) {
         return null;
      }
      
      ModelComponent comp = null;
      if (key instanceof OpenSimObject) {
         OpenSimObject obj = (OpenSimObject)key;
         
         comp = componentMap.remove (obj);
         
         // remove from name-map
         String name = obj.getName ();
         if (name != null) {
            ArrayList<OpenSimObject> olist = nameMap.get (name);
            if (olist != null) {
               olist.remove (obj);
            }
         }
         
         // remove path
         String path = obj.getPath ().toString ();
         if (path != null) {
            pathMap.remove (path, obj);
         }
      }
      
      return comp;
   }

   @Override
   public void putAll ( Map<? extends OpenSimObject,? extends ModelComponent> m) {
      for (Entry<? extends OpenSimObject,? extends ModelComponent> entry : m.entrySet ()) {
         put(entry.getKey (), entry.getValue ());
      }
   }

   @Override
   public void clear () {
      nameMap.clear ();
      pathMap.clear ();
      componentMap.clear ();
   }

   @Override
   public Set<OpenSimObject> keySet () {
      return componentMap.keySet ();
   }

   @Override
   public Collection<ModelComponent> values () {
      return componentMap.values ();
   }

   @Override
   public Set<Entry<OpenSimObject,ModelComponent>> entrySet () {
      return componentMap.entrySet ();
   }
     
}
