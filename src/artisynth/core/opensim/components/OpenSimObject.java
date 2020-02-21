package artisynth.core.opensim.components;

import maspack.util.Clonable;

/**
 * Basic OpenSim Component
 */
public class OpenSimObject implements Clonable {

   private String name;
   
   OpenSimObject parent;
   
   public OpenSimObject() {
      name = null;
      parent = null;
   }
   
   public OpenSimObject (String name) {
      setName(name);
   }

   public void setName (String name) {
      this.name = name;
   }

   public String getName () {
      return name;
   }
   
   
   /**
    * Sets parent component for use in navigating the model hierarchy
    * @param parent parent object
    */
   public void setParent(OpenSimObject parent) {
      this.parent = parent;
   }
   
   /**
    * Returns parent component for use in navigating the model hierarchy
    * @return parent object
    */
   public OpenSimObject getParent() {
      return parent;
   }
   
   /**
    * Determine absolute paths of objects within the model hierarchy,
    * using URI rules for path resolution.  By default, appends the object's
    * name to the parent's path.  
    * 
    * @return path
    */
   public String getPath() {
      String path = null;
      OpenSimObject parent = getParent();
      if (parent != null) {
         String parentPath = parent.getPath ();
         if (parentPath != null) {
            if (!parentPath.endsWith ("/")) {
               parentPath += "/";
            }
            path = parentPath + getName ();
         }
      } 
      if (path == null){
         System.err.println ("Cannot determine path for " + getName ());
      }
      return path;
   }
   
   @Override
   public OpenSimObject clone ()  {
      OpenSimObject obj = null;
      try {
         obj = (OpenSimObject)super.clone ();
         obj.setName (getName ());
         obj.setParent (null);
      }
      catch (CloneNotSupportedException e) {
      }
      return obj;
   }

}
