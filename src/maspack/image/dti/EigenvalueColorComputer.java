package maspack.image.dti;

import java.nio.ByteBuffer;

import maspack.properties.PropertyList;
import maspack.util.DoubleInterval;

/**
 * RGB color of eigenvalues
 * @author Antonio
 *
 */
public class EigenvalueColorComputer extends DTIColorComputer {
   
   static PropertyList myProps = new PropertyList(EigenvalueColorComputer.class, DTIColorComputer.class);
   static {
      myProps.add("range", "range to display", new DoubleInterval(0,1));
   }
   
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   DoubleInterval myRange;
   
   public EigenvalueColorComputer() {
      myRange = new DoubleInterval(0,1);
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
      colors.put((byte)(255*torange(voxel.getE1())));
      colors.put((byte)(255*torange(voxel.getE2())));
      colors.put((byte)(255*torange(voxel.getE3())));
   }
   
   @Override
   public void getRGBA(DTIVoxel voxel, byte[] colors, int coffset) {
      colors[coffset++] = (byte)(255*torange(voxel.getE1()));
      colors[coffset++] = (byte)(255*torange(voxel.getE2()));
      colors[coffset++] = (byte)(255*torange(voxel.getE3()));
      colors[coffset] = (byte)255;
   }

}
