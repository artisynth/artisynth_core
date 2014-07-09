/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;

/**
 * Gobbles up an input stream, printing out results... useful for when calling external 
 * command-line programs.
 * @author Antonio
 *
 */
public class StreamGobbler extends Thread {
   InputStream is;
   PrintStream out;
   String prefix;

   public StreamGobbler(InputStream is, PrintStream out, String prefix) {
       this.is = is;
       this.out = out;
       this.prefix = prefix;
   }

   @Override
   public void run() {
       try {
           InputStreamReader isr = new InputStreamReader(is);
           BufferedReader br = new BufferedReader(isr);
           String line = null;
           while ((line = br.readLine()) != null) {
               out.println(prefix + line);
           }
       }
       catch (IOException ioe) {
           ioe.printStackTrace();
       }
   }
}
