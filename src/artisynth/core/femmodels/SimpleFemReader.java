/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;

/**
 * NOTE: Does not currently support shell elements
 */
public class SimpleFemReader {

   String nodeToken = "v";
   String elemToken = "e";
   String faceToken = "f";
   int nodeOffset = 1;
   
   public void setNodeOffset(int offset) {
      nodeOffset = offset;
   }
   public int getNodeOffset() {
      return nodeOffset;
   }
   
   public void setNodeToken(String fix) {
      nodeToken = fix;
   }
   public String getNodeToken() {
      return nodeToken;
   }
   
   public void setFaceToken(String fix) {
      faceToken = fix;
   }
   public String getFaceToken() {
      return faceToken;
   }
   
   public void setElemToken(String fix) {
      elemToken = fix;
   }
   public String getElemToken() {
      return elemToken;
   }
   
   public static FemModel3d read(String fileName) throws IOException {
      SimpleFemReader reader = new SimpleFemReader();
      return reader.read(null, fileName);
   }
   
   public FemModel3d read(FemModel3d fem, String fileName) throws IOException {
      return read(fem, fileName, new Vector3d(1,1,1));
   }
   
   public FemModel3d read(FemModel3d fem, String fileName, Vector3d scale) throws IOException {
      try {
         ReaderTokenizer rtok = new ReaderTokenizer(new FileReader(fileName));
         fem = read (fem, rtok, scale);
         rtok.close();
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();
      }
      return fem;
   }
   
   public FemModel3d read(FemModel3d fem, ReaderTokenizer rtok) throws IOException {
      return read(fem, rtok, new Vector3d(1,1,1));
   }
   
   public FemModel3d read(FemModel3d fem, ReaderTokenizer rtok, Vector3d scale) throws IOException {
      
      if (fem == null) {
         fem = new FemModel3d();
      } else {
         fem.clear();
      }
      
      double buff[] = new double[30];
         
      boolean flip = (scale.x*scale.y*scale.z < 0); 
         
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         
         if (rtok.ttype == ReaderTokenizer.TT_WORD) {
            if (rtok.sval.equalsIgnoreCase(nodeToken)) {
               // parse node
               double x = rtok.scanNumber();
               double y = rtok.scanNumber();
               double z = rtok.scanNumber();
               
               FemNode3d node = new FemNode3d(x*scale.x, y*scale.y, z*scale.z);
               fem.addNode(node);
            } else if (rtok.sval.equalsIgnoreCase(elemToken)) {
               
               // parse as many integers as possible
               int nVals = rtok.scanNumbers(buff, buff.length);
               rtok.pushBack();
               
               FemNode3d[] nodes = new FemNode3d[nVals];
               for (int i=0; i<nVals; i++) {
                  nodes[i] = fem.getNode((int)buff[i] - nodeOffset);
               }
               
               FemElement3d elem = FemElement3d.createElement(nodes, flip);
               fem.addElement(elem);
               
            }
         }
      }
      
      return fem;

   }
   
}
