package maspack.image.dti;

import java.nio.ByteBuffer;

import maspack.properties.PropertyList;
import maspack.util.DoubleInterval;

/**
 * Computes scalar values from the field
 * @author Antonio
 *
 */
public class ScalarColorComputer extends DTIColorComputer {

   public enum Scalar {
      FRACTIONAL_ANISOTROPY,
      MEAN_DIFFUSIVITY,
      AXIAL_DIFFUSIVITY,
      RADIAL_DIFFUSIVITY,
      E1,
      E2,
      E3,
      D00,
      D01,
      D02,
      D11,
      D12,
      D22
   }
   
   static PropertyList myProps = new PropertyList(ScalarColorComputer.class, DTIColorComputer.class);
   static {
      myProps.add("scalar", "scalar to show", Scalar.FRACTIONAL_ANISOTROPY);
      myProps.add("range", "range to display", new DoubleInterval(0,1));
   }
   
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   Scalar myScalar;
   DoubleInterval myRange;
   
   public ScalarColorComputer() {
      this(Scalar.FRACTIONAL_ANISOTROPY);
   }
   
   public ScalarColorComputer (Scalar scalar) {
      myScalar = scalar;
      myRange = new DoubleInterval(0, 1);
   }
   
   public void setScalar(Scalar scalar) {
      if (scalar != myScalar) {
         myScalar = scalar;
         notifyModified();
      }
   }
   
   public Scalar getScalar() {
      return myScalar;
   }
   
   public void setRange(DoubleInterval range) {
      myRange.set(range);
      notifyModified();
   }
   
   public DoubleInterval getRange() {
      return myRange;
   }
   
   @Override
   public Format getFormat() {
      return Format.GRAYSCALE;
   }
   
   public byte get(DTIVoxel voxel) {
      double v = myRange.getLowerBound();
      switch (myScalar) {
         case AXIAL_DIFFUSIVITY:
            v = voxel.getAD();
            break;
         case D00:
            v = voxel.getD().m00;
            break;
         case D01:
            v = voxel.getD().m01;
            break;
         case D02:
            v = voxel.getD().m02;
            break;
         case D11:
            v = voxel.getD().m11;
            break;
         case D12:
            v = voxel.getD().m12;
            break;
         case D22:
            v = voxel.getD().m22;
            break;
         case E1:
            v = voxel.getE1();
            break;
         case E2:
            v = voxel.getE2();
            break;
         case E3:
            v = voxel.getE3();
            break;
         case FRACTIONAL_ANISOTROPY:
            v = voxel.getFA();
            break;
         case MEAN_DIFFUSIVITY:
            v = voxel.getMD();
            break;
         case RADIAL_DIFFUSIVITY:
            v = voxel.getRD();
            break;
      }
      // scale to within range
      double g = (v-myRange.getLowerBound())/myRange.getRange();
      if (g > 1) {
         g = 1;
      } else if (g < 0) {
         g = 0;
      }
      return (byte)(255*g);
   }

   @Override
   public void get(DTIVoxel voxel, ByteBuffer colors) {
      colors.put(get(voxel));
   }
   
   @Override
   public void getRGBA(DTIVoxel voxel, byte[] colors, int coffset) {
      byte c = get(voxel);
      colors[coffset++] = c;
      colors[coffset++] = c;
      colors[coffset++] = c;
      colors[coffset++] = (byte)255;
      
   }
   
   

}
