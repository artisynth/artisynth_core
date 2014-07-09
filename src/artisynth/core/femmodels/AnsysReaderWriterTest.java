/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;

import maspack.util.ReaderTokenizer;

public class AnsysReaderWriterTest {

   public static final String testNodeStr = 
      "       1                   0                   0    0.05000000000000\n" +
      "       2    0.05000000000000                   0    0.05000000000000\n" +
      "       3    0.05000000000000                   0                   0\n" +
      "       4                   0                   0                   0\n" +
      "       5                   0    0.05000000000000    0.05000000000000\n" +
      "       6    0.05000000000000    0.05000000000000    0.05000000000000\n" +
      "       7    0.05000000000000    0.05000000000000                   0\n" +
      "       8                   0    0.05000000000000                   0\n" +
      "      15    0.05000000000000    0.05000000000000    0.05000000000000\n" +
      "      16     0.1000000000000    0.05000000000000    0.05000000000000\n" +
      "      17     0.1000000000000    0.05000000000000                   0\n" +
      "      18    0.05000000000000    0.05000000000000                   0\n" +
      "      19    0.05000000000000     0.1000000000000    0.05000000000000\n" +
      "      20     0.1000000000000     0.1000000000000    0.05000000000000\n" +
      "      21     0.1000000000000     0.1000000000000                   0\n" +
      "      22    0.05000000000000     0.1000000000000                   0\n" +
      "      23    0.07500000000000    0.05000000000000    0.05000000000000\n" +
      "      24     0.1000000000000    0.05000000000000    0.02500000000000\n" +
      "      25    0.07500000000000    0.05000000000000                   0\n" +
      "      26    0.05000000000000    0.05000000000000    0.02500000000000\n" +
      "      27    0.07500000000000     0.1000000000000    0.05000000000000\n" +
      "      28     0.1000000000000     0.1000000000000    0.02500000000000\n" +
      "      29    0.07500000000000     0.1000000000000                   0\n" +
      "      30    0.05000000000000     0.1000000000000    0.02500000000000\n" +
      "      31    0.05000000000000    0.07500000000000    0.05000000000000\n" +
      "      32     0.1000000000000    0.07500000000000    0.05000000000000\n" +
      "      33     0.1000000000000    0.07500000000000                   0\n" +
      "      34    0.05000000000000    0.07500000000000                   0\n" +
      "      40     0.1000000000000    0.03106695477042    0.07106695477042\n" +
      "      41    0.07011805555556    0.03004861111111     0.1000000000000\n" +
      "      42     0.1000000000000    0.05250000000000     0.1000000000000\n" +
      "      43    0.06375578703704    0.06443865740741     0.1000000000000\n" +
      "      50    0.03012824074074                   0    0.07012824074074\n" +
      "      51                   0                   0     0.1000000000000\n" +
      "      52    0.05000000000000    0.05000000000000     0.1000000000000\n" +
      "      53                   0    0.05000000000000     0.1000000000000\n" +
      "      54    0.01506412037037                   0    0.08506412037037\n" +
      "      55    0.02500000000000    0.02500000000000     0.1000000000000\n" +
      "      56    0.04006412037037    0.02500000000000    0.08506412037037\n" +
      "      57    0.01506412037037    0.02500000000000    0.08506412037037\n" +
      "      58                   0    0.02500000000000     0.1000000000000\n" +
      "      59    0.02500000000000    0.05000000000000     0.1000000000000\n" +
      "      65     0.1000000000000     0.1000000000000     0.1000000000000\n" +
      "      66     0.1500000000000     0.1250000000000     0.1000000000000\n" +
      "      67     0.1000000000000     0.1500000000000     0.1000000000000\n" +
      "      68     0.1000000000000     0.1000000000000     0.1500000000000\n" +
      "      69     0.1500000000000     0.1250000000000     0.1500000000000\n" +
      "      70     0.1000000000000     0.1500000000000     0.1500000000000\n";

   public static final String testElemStr =
      "     1     2     3     4     5     6     7     8     1     1     1     1     0     1\n" +
      "    15    16    17    18    19    20    21    22     1     3     1     1     0     3\n" +
      "    23    24    25    26    27    28    29    30    31    32    33    34\n" +
      "    40    41    42    43     0     0     0     0     1     5     1     1     0     5\n" +
      "    50    51    52    53    54    55    56    57     1     7     1     1     0     7\n" +
      "    58    59\n" +
      "    65    66    67    67    68    69    70    70     1     9     1     1     0     9\n";

   
   public static void main (String args[]) {
      StringReader nodeReader = new StringReader (testNodeStr);
      StringReader elemReader = new StringReader (testElemStr);
      
      FemModel3d fem = new FemModel3d();
      
      try {
         AnsysReader.read (fem, nodeReader, elemReader, 1.0, null, /*options=*/0);
      }
      catch (IOException e) {
         e.printStackTrace();
         System.exit (1);
      }
      
      StringWriter nodeWriter = new StringWriter ();
      StringWriter elemWriter = new StringWriter ();
      
      AnsysWriter.writeNodeFile (fem, new PrintWriter (nodeWriter));
      AnsysWriter.writeElemFile (fem, new PrintWriter (elemWriter));
      
      String nodeStr = nodeWriter.toString ();
      String elemStr = elemWriter.toString ();
      
      try {
         ReaderTokenizer rtok1 = new ReaderTokenizer(new StringReader(testNodeStr));
         ReaderTokenizer rtok2 = new ReaderTokenizer(new StringReader(nodeStr));
         rtok1.eolIsSignificant (true);
         rtok2.eolIsSignificant (true);

         while (rtok1.nextToken () != ReaderTokenizer.TT_EOF &&
            rtok2.nextToken () != ReaderTokenizer.TT_EOF) {
            
            if (rtok1.ttype == ReaderTokenizer.TT_EOL &&
               rtok2.ttype == ReaderTokenizer.TT_EOL ) {
               continue;
            }
            
            if (rtok1.nval != rtok2.nval) {
               throw new Exception ("Input and output node strings do not equal");
            }
         }
         
         rtok1 = new ReaderTokenizer (new StringReader (testElemStr));
         rtok2 = new ReaderTokenizer (new StringReader (elemStr));
         rtok1.eolIsSignificant (true);
         rtok2.eolIsSignificant (true);

         while (rtok1.nextToken () != ReaderTokenizer.TT_EOF &&
            rtok2.nextToken () != ReaderTokenizer.TT_EOF) {
            
            if (rtok1.ttype == ReaderTokenizer.TT_EOL &&
               rtok2.ttype == ReaderTokenizer.TT_EOL ) {
               continue;
            }
            
            if (rtok1.nval != rtok2.nval) {
               throw new Exception ("Input and output elem strings do not equal");
            }
         }
      }
      catch (Exception e) {
         e.printStackTrace();
         System.exit (1);
      }
      
      System.out.println ("\nPassed\n");
   }
}
