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
   
   public ModelInfo(ModelType type, String classNameOrFile, String shortName, String[] args) {
      
      this.classNameOrFile = classNameOrFile;
      this.shortName = shortName;
      this.args = args;
      this.type = type;
      
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
      // lazy initialization
      if (type == null) {
         type = detectType(classNameOrFile);
      }
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
      final int prime = 31;
      int result = 1;
      result = prime * result + Arrays.hashCode(args);
      result = prime * result
            + ((classNameOrFile == null) ? 0 : classNameOrFile.hashCode());
      result = prime * result + ((shortName == null) ? 0 : shortName.hashCode());
      result = prime * result + ((type == null) ? 0 : type.hashCode());
      return result;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null) {
         return false;
      }
      if (getClass() != obj.getClass()) {
         return false;
      }
      
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
      } else if (!classNameOrFile.equals(other.classNameOrFile)) {
         return false;
      }
      
      if (shortName == null) {
         if (other.shortName != null) {
            return false;
         }
      } else if (!shortName.equals(other.shortName)) {
         return false;
      }
      
      return true;
   }
   
   @Override
   public String toString() {
      return classNameOrFile + (args == null ? "" : Arrays.toString(args));
   }
   
}
