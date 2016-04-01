package maspack.render.GL.test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.Random;

import javax.swing.JOptionPane;

public class ArrayVsListsTest {

   private static void doIterationTest() {
      
   }
   
   private static void doMemoryFootprintTest() throws IOException {
      
      Random random = new Random();
      int numEntries = 1000000;
      int numElements = 3;
      
      float[] array = new float[numEntries*numElements];  // array
      ArrayList<float[]> list = new ArrayList<>(numEntries);        // list
      
      int idx = 0;
      for (int i=0; i<numEntries; ++i) {
         float[] e = new float[numElements];
         for (int j=0; j<numElements; ++j) {
            e[j] = random.nextFloat ();
            array[idx++] = e[j];
         }
         list.add (e);
      }
      
      ByteArrayOutputStream bstream = new ByteArrayOutputStream ();
      ObjectOutputStream ostream = new ObjectOutputStream (bstream);
      int size = 0;
      
      //      // simple objects for calibration
      //      class SimpleObject extends Object {
      //         public SimpleObject() {}
      //      }
      //      SimpleObject simple = new SimpleObject ();
      //      Object object = new Object();
      //      
      //      ostream.writeObject (object);
      //      ostream.flush ();
      //      int objectsize = bstream.size()-size;
      //      size = bstream.size ();
      //      
      //      ostream.writeObject (simple);
      //      ostream.flush ();
      //      int simplesize = bstream.size()-size;
      //      size = bstream.size ();
      
      ostream.writeObject (array);
      ostream.flush ();
      int arraysize = bstream.size ()-size;
      size = bstream.size ();
      
      
      ostream.writeObject (list);
      ostream.flush ();
      int listsize = bstream.size ()-size;
      size = bstream.size ();
      
           
      System.out.println ("Size estimates from bytestream serialization:");
      //      System.out.println ("  Single object: " + objectsize + " bytes");
      //      System.out.println ("         simple: " + simplesize + " bytes");
      System.out.println ("  Single array: " + arraysize + " bytes");
      System.out.println ("  List of arrays: " + listsize + " bytes");
            
      JOptionPane.showMessageDialog (null, "Paused to allow for profiler memory analysis.  Look for 'list' and 'array'.");
      
   }
   
   public static void main (String[] args) throws IOException {
      doMemoryFootprintTest ();
   }
   
}
