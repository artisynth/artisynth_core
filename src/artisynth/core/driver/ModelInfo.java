package artisynth.core.driver;

import java.util.Arrays;

/**
 * For better handling of loading models in Main
 * @author antonio
 *
 */
public class ModelInfo {

   public enum ModelType {
      CLASS, FILE;
   }
   
   String classNameOrFile;
   String shortName;
   ModelType type;
   String[] args;
   boolean hashcodeValid = false;
   int hashcode;

   public ModelInfo (
      ModelType type, String classNameOrFile, String shortName, String[] args) {
      
      this.classNameOrFile = classNameOrFile;
      this.shortName = shortName;
      this.args = args;
      this.type = detectType(classNameOrFile);
   }
   
   public String getClassNameOrFile() {
      return classNameOrFile;
   }
   
   public String getLongName() {
      return classNameOrFile;
   }
   
   public String getShortName() {
      return shortName;
   }
   
   public String[] getArgs() {
      return args;
   }
   
   public ModelType getType() {
      return type;
   }
   
   public ModelInfo(String classNameOrFile, String shortName, String[] args) {
      this(null, classNameOrFile, shortName, args);
   }
   
   private static ModelType detectType (String classNameOrFile) {
      
      // see if corresponds to a class on the classpath
      try {
         Class.forName (classNameOrFile, false, ModelInfo.class.getClassLoader());
         return ModelType.CLASS;
      }
      catch (ClassNotFoundException e) {
      }
      return ModelType.FILE;
      
   }
   
   @Override
   public int hashCode() {
      if (!hashcodeValid) {
         final int prime = 31;
         int hash = 1;
         hash = prime*hash + Arrays.hashCode(args);
         hash = prime*hash
            + ((classNameOrFile == null) ? 0 : classNameOrFile.hashCode());
         hash = prime*hash + ((shortName == null) ? 0 : shortName.hashCode());
         hash = prime*hash + ((type == null) ? 0 : type.hashCode());
         hashcode = hash;
         hashcodeValid = true;
      }
      return hashcode;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj instanceof ModelInfo) {
         ModelInfo other = (ModelInfo)obj;
         if (!Arrays.equals(args, other.args)) {
            return false;
         }
      
         if (type != other.type) {
            return false;
         }
         if (classNameOrFile == null) {
            if (other.classNameOrFile != null) {
               return false;  
            }
         }
         else if (!classNameOrFile.equals(other.classNameOrFile)) {
            return false;
         }
      
         if (shortName == null) {
            if (other.shortName != null) {
               return false;
            }
         }
         else if (!shortName.equals(other.shortName)) {
            return false;
         }
         return true;
      }
      return false;
   }
   
   @Override
   public String toString() {
      return classNameOrFile + (args == null ? "" : Arrays.toString(args));
   }
   
}
