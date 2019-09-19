package maspack.geometry.io;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshBase;
import maspack.util.BinaryInputStream;

/**
 * Reads a polygonal mesh from the OpenSim binary ASC format Big-endian
 * 
 * 0x00 00 00 85, # vertices, # faces, # indices for faces, # max nodes/face
 * coordinates... number vertices for each face ... faces ...
 * 
 * @author antonio
 */

public class OpenSimBinReader extends MeshReaderBase {

   public static int FILE_ID = 0x00000085;
   private static boolean verbose = false;

   public OpenSimBinReader (InputStream is) throws IOException {
      super (is);
   }

   public OpenSimBinReader (File file) throws IOException {
      super (file);
   }

   public OpenSimBinReader (String fileName) throws IOException {
      this (new File(fileName));
   }

   public PolygonalMesh readMesh (PolygonalMesh mesh)
      throws IOException {

      if (mesh == null) {
         mesh = new PolygonalMesh();
      } else {
         mesh.clear();
      }
      
      int nVertices = 0;
      int nFaces = 0;
      @SuppressWarnings("unused")
      int nMaxFaces = 4;
      @SuppressWarnings("unused")
      int nFaceNodesTotal = 0;
      int[] nNodes;

      
      // 'istream' should be closed by calling method
      @SuppressWarnings("resource")
      BinaryInputStream bin = 
        new BinaryInputStream(new BufferedInputStream (myIstream));
      
      // read first symbol, which should be
      int fileID = bin.readInt();
      if (fileID != FILE_ID) {
         throw new IOException("Invalid file identifier");
      }

      nVertices = bin.readInt();
      nFaces = bin.readInt();
      nFaceNodesTotal = bin.readInt();
      nMaxFaces = bin.readInt();
      nNodes = new int[nFaces];

      bin.skipBytes(2 * 3 * 8); // no idea why

      if (verbose) {
         System.out.println("============  Vertices  =============");
      }
      
      for (int i = 0; i < nVertices; i++) {
         long bI = bin.getByteCount();
         double x = bin.readDouble();
         double y = bin.readDouble();
         double z = bin.readDouble();
         mesh.addVertex(x, y, z);

         if (verbose) {
            System.out.printf(
               "%d:\t %f %f %f, %x %x %x\n", bI, x, y, z,
               Double.doubleToLongBits(x), Double.doubleToLongBits(y),
               Double.doubleToLongBits(z));
         }
      }
      
      if (verbose) {
         System.out.println("============  Normals  =============");
      }
      for (int i=0; i<nVertices; i++) {
         long bI = bin.getByteCount();
         float x = bin.readFloat();
         float y = bin.readFloat();
         float z = bin.readFloat();
         
         if (verbose) {
            System.out.printf(
               "%d:\t %f %f %f, %x %x %x\n", bI, x, y, z,
               Float.floatToIntBits(x), Float.floatToIntBits(y),
               Float.floatToIntBits(z));
         }
      }

      for (int i = 0; i < nFaces; i++) {
         nNodes[i] = bin.readInt();
      }

      for (int i = 0; i < nFaces; i++) {
         int[] face = new int[nNodes[i]];
         for (int j = 0; j < nNodes[i]; j++) {
            face[j] = bin.readInt();
         }
         mesh.addFace(face);
      }
      
      return mesh;
      
   }

   @Override
   public PolygonalMesh readMesh() throws IOException {
      return (PolygonalMesh)readMesh (new PolygonalMesh());
   }

   public MeshBase readMesh (MeshBase mesh) throws IOException {
      if (mesh instanceof PolygonalMesh) {
         return readMesh ((PolygonalMesh)mesh);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for OpenSim files");
      }
   }

   public static PolygonalMesh read (File file) throws IOException {
      OpenSimBinReader reader = null;
      try {
         reader = new OpenSimBinReader (file);
         return (PolygonalMesh)reader.readMesh (new PolygonalMesh());
      }
      catch (IOException e) {
         throw e;
      }
      finally {
         if (reader != null) {
            reader.close();
         }
      }     
    }

   public static PolygonalMesh read (String fileName) throws IOException {
      return read (new File(fileName));
    }

}
