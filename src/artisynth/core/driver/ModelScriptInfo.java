package artisynth.core.driver;

import java.io.File;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Contains the info associated with loading a model or running a script
 * @author antonio, lloyd
 *
 */
public class ModelScriptInfo {

   public enum InfoType {
      CLASS, FILE, SCRIPT;
   }
   
   String classNameOrFile;
   String shortName;
   InfoType type;
   String[] args;
   boolean hashcodeValid = false;
   int hashcode;

   public ModelScriptInfo (
      InfoType type, String classNameOrFile, String shortName, String[] args) {
      
      this.classNameOrFile = classNameOrFile;
      this.shortName = shortName;
      if (args != null) {
         this.args = Arrays.copyOf(args, args.length);
      }
      else {
         this.args = new String[0];
      }
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
   
   public String getArgsString() {
      return mergeArgs (args);
   }
   
   public InfoType getType() {
      return type;
   }
   
//   public ModelInfo(String classNameOrFile, String shortName, String[] args) {
//      this(null, classNameOrFile, shortName, args);
//   }

   public String getClassName() {
      return type == InfoType.CLASS ? classNameOrFile : "";
   }
   
   public File getFile() {
      return type == InfoType.CLASS ? null : new File(classNameOrFile);
   }
   
//   public ModelInfo(String classNameOrFile, String shortName, String argsStr) {
//      this(null, classNameOrFile, shortName, splitArgs(argsStr));
//   }
   
//   private static ModelType detectType (String classNameOrFile) {
//      
//      // see if corresponds to a class on the classpath
//      try {
//         Class.forName (classNameOrFile, false, ModelInfo.class.getClassLoader());
//         return ModelType.CLASS;
//      }
//      catch (ClassNotFoundException e) {
//      }
//      return ModelType.FILE;
//      
//   }
   
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
      if (obj instanceof ModelScriptInfo) {
         ModelScriptInfo other = (ModelScriptInfo)obj;
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
      return type + " " + classNameOrFile + " " + shortName + " " +
         (args == null ? "" : Arrays.toString(args));
   }

   public static String mergeArgs (String[] args) {
      StringBuilder argBuilder = new StringBuilder();
      
      if (args == null) {
         return "";
      }
      for (String arg : args) {
         argBuilder.append (' ');
         if (!arg.contains (" ")) {
            argBuilder.append (arg);
         } else if (!arg.contains ("\"")) {
            argBuilder.append ('"');
            argBuilder.append (arg);
            argBuilder.append ('"');
         } else if (!arg.contains ("\'")) {
            argBuilder.append ('\'');
            argBuilder.append (arg);
            argBuilder.append ('\'');
         } else {
            System.err.println (
               "Error: cannot append argument: " + arg +
               " because it has spaces and both types of quotes");
         }
      }
      
      return argBuilder.toString().trim();
   }
   
   public static String[] splitArgs (String argsStr) {
      List<String> matchList = new ArrayList<String>();
      Pattern regex = Pattern.compile ("[^\\s\"']+|\" ([^\"]*)\"|' ([^']*)'");
      Matcher regexMatcher = regex.matcher (argsStr);
      while (regexMatcher.find()) {
          if (regexMatcher.group (1) != null) {
              // Add double-quoted string without the quotes
              matchList.add (regexMatcher.group (1));
          }
          else if (regexMatcher.group (2) != null) {
              // Add single-quoted string without the quotes
              matchList.add (regexMatcher.group (2));
          }
          else {
              // Add unquoted word
              matchList.add (regexMatcher.group());
          }
      } 
      return matchList.toArray (new String[matchList.size()]);
   }
   
}
