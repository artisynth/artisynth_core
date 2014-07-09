/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;

import javax.swing.filechooser.FileFilter;

import maspack.matrix.Point3d;
import maspack.util.ReaderTokenizer;

/**
 * A class to read an ascii landmark file exported from Amira
 */
public class AmiraLandmarkReader {

   /**
    * Creates an array of 3D points from the landmark data from a file
    * 
    * @param fileName
    * path name of the .landmarksAscii file
    * @return list of points read from file
    * @throws IOException
    * if this is a problem reading the file
    */
   public static Point3d[] read (String fileName) throws IOException {
      return read (fileName, 1.0);
   }
   /**
    * Creates an 2D array of 3D points from the landmark data from a file
    * 
    * @param fileName
    * path name of the .landmarksAscii file
    * @return list of points read from file
    * @throws IOException
    * if this is a problem reading the file
    */
   public static Point3d[][] readSets (String fileName) throws IOException {
      return readSets (fileName, 1.0);
   }
   /**
    * Creates an array of 3D points from the landmark data from a file
    * 
    * @param fileName
    * path name of the .landmarksAscii file
    * @param scale
    * factor by which node coordinate data should be scaled
    * @return list of points read from file
    * @throws IOException
    * if this is a problem reading the file
    */
   public static Point3d[] read (String fileName, double scale)
      throws IOException {
      Reader reader = new FileReader (fileName);
      Point3d[] pts = read (reader, scale);
      reader.close();
      return pts;
   }
   /**
    * Creates an 2D array of 3D points from the landmark data from a file
    * 
    * @param fileName
    * path name of the .landmarksAscii file
    * @param scale
    * factor by which node coordinate data should be scaled
    * @return list of points read from file
    * @throws IOException
    * if this is a problem reading the file
    */
   public static Point3d[][] readSets (String fileName, double scale)
      throws IOException {
      Reader reader = new FileReader (fileName);
      Point3d[][] pts = readSets (reader, scale);
      reader.close();
      return pts;
   }
   /**
    * Creates an array of 3D points from the landmark data from a Reader
    * 
    * @param reader
    * reader from which to read amira landmark data
    * @param scale
    * factor by which node coordinate data should be scaled
    * @return list of points read from file
    * @throws IOException
    * if this is a problem reading the file
    */
   public static Point3d[] read (Reader reader, double scale)
      throws IOException {
      ArrayList<Point3d> pts = new ArrayList<Point3d>();
      ReaderTokenizer rtok = new ReaderTokenizer (new BufferedReader (reader));
      rtok.wordChars ("./@");

      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         if (rtok.ttype == ReaderTokenizer.TT_WORD
         && rtok.sval.compareTo ("@1") == 0) {

            rtok.nextToken();
            if (rtok.ttype != ReaderTokenizer.TT_NUMBER) {
               rtok.pushBack(); // not at data yet, look for next "@1" word
               continue;
            }
            else {
               rtok.pushBack(); // we have found landmark data - scan into
               // Point3ds until EOF

               while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
                  rtok.pushBack();
                  Point3d point = new Point3d();
                  point.scan (rtok);
                  point.scale (scale);
                  pts.add (point);
               }
               break;
            }
         }
      }

      Point3d[] ptarray = new Point3d[pts.size()];
      for (int i = 0; i < pts.size(); i++)
         ptarray[i] = pts.get (i);
      return ptarray;
   }
   /**
    * Creates an 2D array of 3D points from the landmark data from a Reader
    * 
    * @param reader
    * reader from which to read amira landmark data
    * @param scale
    * factor by which node coordinate data should be scaled
    * @return list of points read from file
    * @throws IOException
    * if this is a problem reading the file
    */
   public static Point3d[][] readSets (Reader reader, double scale)
      throws IOException {
      Point3d[][] ptarray = null;
      int numSets = 0, length = 0;
      ReaderTokenizer rtok = new ReaderTokenizer (new BufferedReader (reader));
      rtok.wordChars ("./@");

      String prevToken;
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         if(numSets==0 || length==0) {
            if (rtok.ttype == ReaderTokenizer.TT_WORD) {
               prevToken = rtok.sval;
               rtok.nextToken();
               if (rtok.ttype != ReaderTokenizer.TT_NUMBER) {
                  rtok.pushBack(); // not at data yet, look for next "@1" word
                  continue;
               } else {
                  if(prevToken.compareTo ("Markers") == 0) {
                     length = (int)rtok.nval;
                  } else if (prevToken.compareTo ("NumSets") == 0) {
                     numSets = (int)rtok.nval;
                  }
               }
            }
         } else {
            if(ptarray==null) {
               ptarray = new Point3d[numSets][length];
            }
            if (rtok.ttype == ReaderTokenizer.TT_WORD
            && rtok.sval.matches ("@[\\d]+")) {
               int setIdx = Integer.parseInt(rtok.sval.replace ("@", ""))-1;
               
               rtok.nextToken();
               if (rtok.ttype != ReaderTokenizer.TT_NUMBER) {
                  rtok.pushBack(); // not at data yet, look for next "@1" word
                  continue;
               } else {
                  rtok.pushBack(); // we have found landmark data - scan into
                  // Point3ds until the next set
                  int count = 0;
                  while ( rtok.nextToken() != ReaderTokenizer.TT_EOF
                  && rtok.ttype != ReaderTokenizer.TT_WORD) {
                     rtok.pushBack();
                     Point3d point = new Point3d();
                     point.scan (rtok);
                     if(!point.equals (new Point3d(0,0,0))) {
                        point.scale (scale);
                        ptarray[setIdx][count] = point;
                        count++;
                     }
                  }
                  rtok.pushBack ();
               }
            }
         }
      }
      return ptarray;
   }
   public static class AmiraLandmarkFileFilter extends FileFilter {

      public boolean accept (File f) {
         return (f.isDirectory() || f.isFile()
         && f.getName().endsWith (".landmarkAscii"));
      }

      public String getDescription() {
         return ".landmarkAscii files";
      }
   }

}
