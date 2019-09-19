package maspack.geometry.io;

import java.io.*;
import java.util.ArrayList;

import maspack.geometry.MeshBase;
import maspack.geometry.PointMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.ReaderTokenizer;

public class XyzReader extends MeshReaderBase {

   public XyzReader (InputStream is) throws IOException {
      super (is);
   }

   public XyzReader (File file) throws IOException {
      super (file);
   }

   public XyzReader (String fileName) throws IOException {
      this (new File (fileName));
   }

   public PointMesh read (PointMesh mesh, InputStream in)
      throws IOException {

      ReaderTokenizer rtok =
         new ReaderTokenizer (new BufferedReader (new InputStreamReader (in)));
      
      if (mesh == null) {
         mesh = new PointMesh();
      }
      mesh.clear();
      ArrayList<Point3d> vtxs = new ArrayList<Point3d>();
      ArrayList<Vector3d> nrms = new ArrayList<Vector3d>();
      while (rtok.nextToken() != ReaderTokenizer.TT_EOF) {
         rtok.pushBack();
         double vx = rtok.scanNumber();
         double vy = rtok.scanNumber();
         double vz = rtok.scanNumber();
         double nx = rtok.scanNumber();
         double ny = rtok.scanNumber();
         double nz = rtok.scanNumber();
         vtxs.add (new Point3d (vx, vy, vz));
         nrms.add (new Vector3d (nx, ny, nz));
      }

      mesh.set (vtxs.toArray(new Point3d[0]), nrms.toArray(new Vector3d[0]));
      return mesh;
   }
   
   public PointMesh read (PointMesh mesh, String fileName)
      throws IOException {
      return read (mesh, new File (fileName));
   }

   public PointMesh read (PointMesh mesh, File file) throws IOException {
      
      FileInputStream fin = new FileInputStream (file);
      
      PointMesh out = null;
      try {
         out = read (mesh, fin);
      } catch (IOException e) {
         throw e;
      } finally {
         closeQuietly(fin);
      }
      
      return out;
   }
   
   private void closeQuietly(InputStream in) {
      if (in != null) {
         try {
            in.close();
         } catch (IOException e) {}
      }
   }

   @Override 
   public PointMesh readMesh() throws IOException {
      return (PointMesh) read (new PointMesh(), myIstream);
   }

   public MeshBase readMesh (MeshBase mesh) throws IOException {
      if (mesh instanceof PointMesh) {
         return read ((PointMesh)mesh, myIstream);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.xyz' files");
      }
   }

//   @Override
//   public MeshBase read(MeshBase mesh, InputStream istream) throws IOException {
//      if (!(mesh instanceof PointMesh) ) {
//         throw new IllegalArgumentException("Mesh must be of type PointMesh");
//      }
//      return read( (PointMesh)mesh, istream);
//   }

//   @Override
//   public MeshBase read(MeshBase mesh, File file) throws IOException {
//      if (!(mesh instanceof PointMesh) ) {
//         throw new IllegalArgumentException("Mesh must be of type PointMesh");
//      }
//      return read( (PointMesh)mesh, file);
//   }

//   @Override
//   public MeshBase read(MeshBase mesh, String filename) throws IOException {
//      if (!(mesh instanceof PointMesh) ) {
//         throw new IllegalArgumentException("Mesh must be of type PointMesh");
//      }
//      return read( (PointMesh)mesh, filename);
//   }
   
   public static PointMesh read(File file) throws IOException {
      XyzbReader reader = null;
      try {
         reader = new XyzbReader(file);
         return reader.readMesh ();
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

   public static PointMesh read(String filename) throws IOException {
      return read(new File(filename));
   }

}
