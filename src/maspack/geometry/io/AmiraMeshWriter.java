/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */

package maspack.geometry.io;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;

import maspack.geometry.Face;
import maspack.geometry.HalfEdge;
import maspack.geometry.MeshBase;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.Polyline;
import maspack.geometry.PolylineMesh;
import maspack.geometry.Vertex3d;

public class AmiraMeshWriter extends MeshWriterBase {
   
   public static class AmiraMeshWriterFactory implements MeshWriterFactory {

      @Override
      public AmiraMeshWriter newWriter(String fileName) throws IOException {
         return new AmiraMeshWriter(fileName);
      }

      @Override
      public MeshWriter newWriter(File file) throws IOException {
         return new AmiraMeshWriter(file);
      }

      @Override
      public MeshWriter newWriter(OutputStream stream) {
         return new AmiraMeshWriter(stream);
      }

      @Override
      public String[] getFileExtensions() {
         return new String[] {".am", ".amiramesh"};
      }
      
   }

   private static final String FILE_HEADER = "# AmiraMesh 3D ASCII 2.0";
   private static final int DEFAULT_MAX_LINE_INDEX_SIZE = 10;
   private int maxLineIndexSize = DEFAULT_MAX_LINE_INDEX_SIZE;
   
   public AmiraMeshWriter (OutputStream os) {
      super (os);
      setFormat ("%.10g");
   }
   
   public AmiraMeshWriter (File file) throws IOException {
      super (file);
      setFormat ("%.10g");
   }

   public AmiraMeshWriter (String fileName) throws IOException {
      this (new File(fileName));
   }
   
   public static void writeMesh(String fileName, PolylineMesh mesh) throws IOException {
      AmiraMeshWriter writer = null;
      try {
         writer = new AmiraMeshWriter(fileName);
         writer.writeMesh(mesh);
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (writer != null) {
            writer.close();
         }
      }
   }
   
   @Override
   public void writeMesh(MeshBase mesh) throws IOException {
      
      if (mesh instanceof PolylineMesh) {
         writeMesh( (PolylineMesh)mesh );
      } else if (mesh instanceof PolygonalMesh) {
         writeMesh( (PolygonalMesh)mesh );
      } else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported by AmiraMeshWriter");
      }
   }
   
   public void writeMesh(PolylineMesh mesh) {
      PrintWriter pw = new PrintWriter (
         new BufferedWriter (new OutputStreamWriter (myOstream)));
      
      int nLineSize = 0;
      for (Polyline line : mesh.getLines()) {
         int nlineVerts = line.numVertices();
         if (nlineVerts > 0) {
            nLineSize += nlineVerts + 1;
         }
      }
      
      int nVerts = mesh.numVertices();
      
      pw.println(FILE_HEADER);
      // mesh name
      if (mesh.getName() != null) {
         pw.println("# " + mesh.getName());
      }
      pw.println();
      
      pw.println("define Lines " + nLineSize);
      pw.println("define Vertices " + nVerts);
      pw.println();
      
      pw.println("Parameters {");
      pw.println("    ContentType \"HxLineSet\"");
      pw.println("}");
      pw.println();
      
      pw.println("Vertices { float[3] Coordinates } = @1");
      pw.println("Lines { int LineIdx } = @2");
      pw.println();
      
      pw.println("@1 # xyz vertex coordinates");
      int idx = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
         vtx.setIndex(idx++);
         pw.println(vtx.getPosition().toString(myFmt));
      }
      pw.println();
      
      pw.println("@2 # line indices, terminating with -1");
      for (Polyline line : mesh.getLines()) {
         idx = 0;
         Vertex3d[] vtxs = line.getVertices();
         
         if (vtxs.length > 0) {
            pw.print(vtxs[0].getIndex());
            for (int i=1; i<vtxs.length; i++) {
               if ( (i % maxLineIndexSize) == 0) {
                  pw.println();
               } else {
                  pw.print(" ");
               }
               pw.print(vtxs[i].getIndex());
            }
            if ( (vtxs.length % maxLineIndexSize) == 0) {
               pw.println();
            } else {
               pw.print(" ");
            }
            pw.println(-1);
         } // end if non-null line
      } // end loop through lines
      pw.flush();
      
   }
   
   public void writeMesh(PolygonalMesh mesh) {
      PrintWriter pw = new PrintWriter (
         new BufferedWriter (new OutputStreamWriter (myOstream)));
      
      if (!mesh.isTriangular()) {
         throw new IllegalArgumentException("Mesh must be triangular");
      }
      
      int nVerts = mesh.numVertices();
      int nFaces = mesh.numFaces();
      
      pw.println(FILE_HEADER);
      // mesh name
      if (mesh.getName() != null) {
         pw.println("# " + mesh.getName());
      }
      pw.println();
      
      pw.println("define Nodes " + nVerts);
      pw.println("define Triangles " + nFaces);
      pw.println();
      
      pw.println("Parameters {");
      // pw.println("    ContentType \"HxLineSet\"");
      // XXX confirm with amira about valid parameter set
      pw.println("}");
      pw.println();
      
      pw.println("Nodes { float[3] Coordinates } = @1");
      pw.println("Triangles { int[3] Nodes } = @2");
      pw.println();
      
      pw.println("@1 # xyz vertex coordinates");
      int idx = 0;
      for (Vertex3d vtx : mesh.getVertices()) {
         vtx.setIndex(idx++);
         pw.println(vtx.getPosition().toString(myFmt));
      }
      pw.println();
      
      pw.println("@2 # triangular face indices");
      for (Face face : mesh.getFaces()) {
         HalfEdge he = face.firstHalfEdge();
         Vertex3d vtx = he.getHead();
         pw.print(vtx.getIndex());
         he = he.getNext();
         vtx = he.getHead();
         pw.print(" " + vtx.getIndex());
         he = he.getNext();
         vtx = he.getHead();
         pw.println(" " + vtx.getIndex());
      } // end loop through faces
      pw.flush();
      
   }   
   
}
