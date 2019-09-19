package maspack.geometry.io;

import java.io.*;
import java.util.ArrayList;

import maspack.geometry.MeshBase;
import maspack.geometry.PointMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.util.BinaryInputStream;

public class XyzbReader extends MeshReaderBase {

   protected boolean myLittleEndian = true;
   protected int mySkip = 1;

   public boolean isLittleEndian () {
      return myLittleEndian;
   }

   public void setLittleEndian (boolean enable) {
      myLittleEndian = enable;
   }

   public XyzbReader (InputStream is) throws IOException {
      super (is);
   }

   public XyzbReader (File file) throws IOException {
      super (file);
   }

   public XyzbReader (String fileName) throws IOException {
      this (new File (fileName));
   }

   public int getSkip() {
      return mySkip;
   }

   public void setSkip (int skip) {
      mySkip = skip;
   }

   public PointMesh readMesh (PointMesh mesh, InputStream in)
      throws IOException {

      BinaryInputStream bin = new BinaryInputStream (
         new BufferedInputStream (in));

      bin.setLittleEndian (myLittleEndian);
      if (mesh == null) {
         mesh = new PointMesh();
      }
      mesh.clear();
      ArrayList<Point3d> vtxs = new ArrayList<Point3d>();
      ArrayList<Vector3d> nrms = new ArrayList<Vector3d>();
      boolean done = false;
      int cnt = 0;
      while (!done) {
         try {
            double vx = bin.readFloat();
            double vy = bin.readFloat();
            double vz = bin.readFloat();
            double nx = bin.readFloat();
            double ny = bin.readFloat();
            double nz = bin.readFloat();
            if (cnt%mySkip == 0) {
               vtxs.add (new Point3d (vx, vy, vz));
               nrms.add (new Vector3d (nx, ny, nz));
            }
         }
         catch (EOFException e) {
            done = true;
         }
         cnt++;
      }   
      bin.close();
      
      mesh.set (vtxs.toArray(new Point3d[0]), nrms.toArray(new Vector3d[0]));
      return mesh;
   }
   
//   public PointMesh read (PointMesh mesh, String fileName)
//      throws IOException {
//      return read (mesh, new File (fileName));
//   }
//
//   public PointMesh read (PointMesh mesh, File file) throws IOException {
//      
//      BinaryInputStream bin = new BinaryInputStream (
// new BufferedInputStream (new FileInputStream (file)));
//      
//      PointMesh out = null;
//      try {
//         out = read (mesh, bin);
//      } catch (IOException e) {
//         throw e;
//      } finally {
//         closeQuietly(bin);
//      }
//      
//      return out;
//   }
   
   private void closeQuietly(InputStream in) {
      if (in != null) {
         try {
            in.close();
         } catch (IOException e) {}
      }
   }

   @Override 
   public PointMesh readMesh() throws IOException {
      return (PointMesh) readMesh (new PointMesh());
   }

   public PointMesh readMesh (MeshBase mesh) throws IOException {
      if (mesh == null) {
         mesh = new PointMesh();
      }
      if (mesh instanceof PointMesh) {
         return readMesh ((PointMesh)mesh, myIstream);
      }
      else {
         throw new UnsupportedOperationException (
            "Mesh type "+mesh.getClass()+" not supported for '.xyzb' files");
      }
   }
   
   public static PointMesh read(File file) throws IOException {
      XyzbReader reader = null;
      try {
         reader = new XyzbReader(file);
         return reader.readMesh(null);
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
