package maspack.image.dti;

import java.nio.ByteBuffer;

import maspack.properties.CompositePropertyBase;
import maspack.util.DynamicArray;
import maspack.util.Versioned;

public abstract class DTIColorComputer extends CompositePropertyBase implements Versioned {
   
   static DynamicArray<Class<?>> mySubclasses = new DynamicArray<>(new Class<?>[] {});
   
   public static void registerSubclass(Class<? extends DTIColorComputer> cls) {
      if (!mySubclasses.contains(cls)) {
         mySubclasses.add(cls);
      }
   }
   public static Class<?>[] getSubClasses() {
      return mySubclasses.getArray();
   }
   
   static {
      registerSubclass(PrincipalDiffusionColorComputer.class);
      registerSubclass(ScalarColorComputer.class);
      registerSubclass(ScaledEigenvectorColorComputer.class);
      registerSubclass(EigenvalueColorComputer.class);
   }
   
   public static enum Format {
      /**
       * 1-byte grayscale
       */
      GRAYSCALE,
      /**
       * 2-byte grayscale-alpha
       */
      GRAYSCALE_ALPHA,
      /**
       * 3-byte RGB
       */
      RGB,
      /**
       * 4-byte RGBA
       */
      RGBA
   }
   
   public abstract Format getFormat();
   
   public abstract void get(DTIVoxel voxel, ByteBuffer colors);
   
   public abstract void getRGBA(DTIVoxel voxel, byte[] colors, int coffset);
   
   volatile boolean modified = true;
   protected void notifyModified() {
      modified = true;
   }
   volatile int version = -1;
   @Override
   public int getVersion() {
      if (modified) {
         ++version;
         modified = false;
      }
      return version;
   }
   

}
