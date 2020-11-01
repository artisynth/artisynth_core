package maspack.matrix;

import java.util.*;
import maspack.util.ObjectSizeAgent;

/**
 * Class that determines the sizes of various objects
 * 
 * This program must run as
 *
 * java -javaagent:$AT/lib/ObjectSizeAgent.jar maspack.matrix.ObjectSizes
 */
public class ObjectSizes {

   public static void printSize (String name, Object obj) {
      System.out.println (name + ": " + ObjectSizeAgent.getObjectSize(obj));
   }
 
   public static void main(String[] arguments) {
 
      printSize ("Vector3d", new Vector3d());
      printSize ("Matrix3d", new Matrix3d());
      printSize ("double[0]", new double[0]);
      printSize ("double[10]", new double[10]);
      printSize ("int[0]", new int[0]);
      printSize ("int[10]", new int[10]);
      printSize ("Object[0]", new Object[0]);
      printSize ("Object[10]", new Object[10]);
      printSize ("ArrayList<Object>", new ArrayList<Object>());
   }

}
