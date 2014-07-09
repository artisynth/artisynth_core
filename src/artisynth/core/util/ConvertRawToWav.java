/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.*;
import javax.sound.sampled.*;

public class ConvertRawToWav {

   public static String convert (double srate, String fn) throws Exception {
      String fnout = fn + ".wav";
      FileInputStream inStream = new FileInputStream (new File (fn));
      File out = new File (fnout);
      int bytesAvailable = inStream.available();
      int sampleSizeInBits = 16;
      int channels = 1;
      boolean signed = false;
      boolean bigEndian = false;
      AudioFormat audioFormat =
         new AudioFormat (
            (float)srate, sampleSizeInBits, channels, signed, bigEndian);
      AudioInputStream audioInputStream =
         new AudioInputStream (inStream, audioFormat, bytesAvailable / 2);
      AudioSystem.write (audioInputStream, AudioFileFormat.Type.WAVE, out);
      audioInputStream.close();
      inStream.close();
      
      return fnout;
   }

   public static void main (String args[]) throws Exception {
      double srate = 44100.;
      srate = Double.parseDouble (args[1]);
      String fn = args[0];
      ConvertRawToWav.convert (srate, fn);

   }
}
