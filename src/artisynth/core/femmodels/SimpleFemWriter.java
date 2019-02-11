/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.femmodels;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;

import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Vertex3d;
import maspack.util.NumberFormat;

/**
 * NOTE: Does not currently support shell elements
 */
public class SimpleFemWriter {
   
   NumberFormat myFmt = new NumberFormat("%g");
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
   
   public void setNumberFormat(String fmt) {
      myFmt = new NumberFormat(fmt);
   }
   public void setNumberFormat(NumberFormat fmt) {
      myFmt = new NumberFormat(fmt);
   }
   public NumberFormat getNumberFormat() {
      return myFmt;
   }
   
   public static void write(FemModel3d fem, String fileName, String fmtStr) {
      SimpleFemWriter writer = new SimpleFemWriter();
      writer.setNumberFormat(fmtStr);
      writer.write(fem, fileName);
   }
   
   public void write(FemModel3d fem, String fileName) {
      try {
         PrintWriter pw = new PrintWriter (new File (fileName));
         write (fem, pw);
         pw.close();
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();
      }
   }
   
   public void write(FemModel3d fem, PrintWriter writer) {
      writeNodeFile(fem, writer);
      writeElemFile(fem, writer);
      writeSurfaceFile(fem, writer);
   }
   
   public void writeNodeFile (FemModel3d fem, String fileName) {
      try {
         PrintWriter pw = new PrintWriter (new File (fileName));
         writeNodeFile (fem, pw);
         pw.close();
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();
      }
   }
   
   public void writeNodeFile (FemModel3d fem, PrintWriter nodeWriter) {

      for (FemNode3d n : fem.getNodes()) {
         nodeWriter.println (nodeToken + " " + myFmt.format (n.getPosition().x) +
            " " + myFmt.format (n.getPosition().y) +
            " " + myFmt.format (n.getPosition().z));
      }
   }

   public void writeElemFile (FemModel3d fem, String fileName) {
      try {
         PrintWriter pw = new PrintWriter (new File (fileName));
         writeElemFile (fem, pw);
         pw.close();
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();
      }
   }

   
   public void writeElemFile (FemModel3d fem, PrintWriter elemWriter) {
      
      for (FemElement3dBase e : fem.getElements()) {
         FemNode3d[] nodes = e.getNodes();
         elemWriter.print(elemToken);
         for (FemNode3d node : nodes) {
            elemWriter.print(" " + (node.getNumber()+nodeOffset));
         }
         elemWriter.println();
      }
      
   }
   
   public void writeSurfaceFile (FemModel3d fem, String fileName) {
      try {
         PrintWriter pw = new PrintWriter (new File (fileName));
         writeSurfaceFile (fem, pw);
         pw.close();
      }
      catch (FileNotFoundException e) {
         e.printStackTrace();
      }
   }
   
   public void writeSurfaceFile (FemModel3d fem, PrintWriter surfaceWriter) {
      
      PolygonalMesh mesh = fem.getSurfaceMesh();
      
      for (Face face : mesh.getFaces()) {
         surfaceWriter.print(faceToken);
         
         for (Vertex3d vtx : face.getVertices()) {
            FemNode3d node = fem.getSurfaceNode (vtx);
            if (node != null) {
               surfaceWriter.print(" " + (node.getNumber()+nodeOffset));
            }
         }
         surfaceWriter.println();
      }
      
   }
   
}
