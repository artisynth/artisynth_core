package maspack.image.dti;

import java.nio.ByteBuffer;

import maspack.properties.PropertyList;
import maspack.util.DoubleInterval;

/**
 * RGB color of eigenvalues
 * @author Antonio
 *
 */
public class ScaledEigenvectorColorComputer extends DTIColorComputer {
   
   private static enum EigenVector {
      EV1,
      EV2,
      EV3,
      V1,
      V2,
      V3
   }
   
   static PropertyList myProps = new PropertyList(ScaledEigenvectorColorComputer.class, DTIColorComputer.class);
   static {
      myProps.add("range", "range to display", new DoubleInterval(0,1));
      myProps.add("vector", "eigenvector to display", EigenVector.V1);
   }
   
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   DoubleInterval myRange;
   EigenVector vector;
   
   public ScaledEigenvectorColorComputer() {
      myRange = new DoubleInterval(0,1);
      vector = EigenVector.V1;
   }
   
   @Override
   public Format getFormat() {
      return Format.RGB;
   }
   
   public void setRange(DoubleInterval range) {
      myRange.set(range);
      notifyModified();
   }
   
   public DoubleInterval getRange() {
      return myRange;
   }
   
   public void setVector(EigenVector v) {
      vector = v;
      notifyModified();
   }
   
   public EigenVector getVector() {
      return vector;
   }
   
   private double torange(double v) {
      double g = (v-myRange.getLowerBound())/myRange.getRange();
      if (g > 1) {
         g = 1;
      } else if (g < 0) {
         g = 0;
      }
      return g;
   }

   @Override
   public void get(DTIVoxel voxel, ByteBuffer colors) {
      double e = 1;
      double v0 = 0;
      double v1 = 0;
      double v2 = 0;
      switch (vector) {
         case EV1:
            e = voxel.getE1();
         case V1: {
            v0 = voxel.getV().m00;
            v1 = voxel.getV().m10;
            v2 = voxel.getV().m20;
            break;
         }
         case EV2:
            e = voxel.getE2();
         case V2: {
            v0 = voxel.getV().m01;
            v1 = voxel.getV().m11;
            v2 = voxel.getV().m21;
            break;
         }
         case EV3:
            e = voxel.getE3();
         case V3: {
            v0 = voxel.getV().m02;
            v1 = voxel.getV().m12;
            v2 = voxel.getV().m22;
            break;
         }
      }
      
      colors.put((byte)(255*torange(e*v0)));
      colors.put((byte)(255*torange(e*v1)));
      colors.put((byte)(255*torange(e*v2)));

   }
   
   @Override
   public void getRGBA(DTIVoxel voxel, byte[] colors, int coffset) {
      double e = 1;
      double v0 = 0;
      double v1 = 0;
      double v2 = 0;
      switch (vector) {
         case EV1:
            e = voxel.getE1();
         case V1: {
            v0 = voxel.getV().m00;
            v1 = voxel.getV().m10;
            v2 = voxel.getV().m20;
            break;
         }
         case EV2:
            e = voxel.getE2();
         case V2: {
            v0 = voxel.getV().m01;
            v1 = voxel.getV().m11;
            v2 = voxel.getV().m21;
            break;
         }
         case EV3:
            e = voxel.getE3();
         case V3: {
            v0 = voxel.getV().m02;
            v1 = voxel.getV().m12;
            v2 = voxel.getV().m22;
            break;
         }
      }
      
      colors[coffset++] = (byte)(255*torange(e*v0));
      colors[coffset++] = (byte)(255*torange(e*v1));
      colors[coffset++] = (byte)(255*torange(e*v2));
      colors[coffset++] = (byte)255;
   }

}
