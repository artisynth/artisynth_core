package artisynth.core.driver;

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
   
}
