package maspack.image.dti;

import java.nio.ByteBuffer;

import maspack.matrix.AxisAngle;
import maspack.matrix.Matrix3d;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;

/**
 * Uses principal direction (v1) times fractional anisotropy
 * (r,g,b) = fa*(abs(v1.x), abs(v1.y), abs(v1.z))
 * @author Antonio
 *
 */
public class PrincipalDiffusionColorComputer extends DTIColorComputer {
   
   AxisAngle orientation;
   
   public static PropertyList myProps = 
      new PropertyList (PrincipalDiffusionColorComputer.class, DTIColorComputer.class);
   static {
      myProps.add ("orientation", "orientation for colors", AxisAngle.IDENTITY);
   }
   @Override
   public PropertyList getAllPropertyInfo () {
      return myProps;
   }
   
   public PrincipalDiffusionColorComputer () {
      orientation = new AxisAngle(1, 0, 0, 0);
   }
   
   public AxisAngle getOrientation() {
      return orientation;
   }
   
   public void setOrientation(AxisAngle orient) {
      orientation.set (orient);
      notifyModified ();
   }
   
   @Override
   public Format getFormat() {
      return Format.RGB;
   }

   @Override
   public void get(DTIVoxel voxel, ByteBuffer colors) {
      double fa = voxel.getFA();
      Matrix3d V = voxel.getV();
      Vector3d v0 = new Vector3d(V.m00, V.m10, V.m20);
      orientation.transform (v0, v0);
      colors.put((byte)(255*fa*Math.abs(v0.x)));
      colors.put((byte)(255*fa*Math.abs(v0.y)));
      colors.put((byte)(255*fa*Math.abs(v0.z)));
   }

   @Override
   public void getRGBA(DTIVoxel voxel, byte[] colors, int coffset) {
      double fa = voxel.getFA();
      Matrix3d V = voxel.getV();
      Vector3d v0 = new Vector3d(V.m00, V.m10, V.m20);
      orientation.transform (v0, v0);
      colors[coffset++] = (byte)(255*fa*Math.abs(v0.x));
      colors[coffset++] = (byte)(255*fa*Math.abs(v0.y));
      colors[coffset++] = (byte)(255*fa*Math.abs(v0.z));
      colors[coffset++] = (byte)255;
   }
   
}
