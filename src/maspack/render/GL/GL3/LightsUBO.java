package maspack.render.GL.GL3;

import java.nio.ByteBuffer;
import java.util.List;

import com.jogamp.opengl.GL3;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.Light;
import maspack.render.Light.LightSpace;
import maspack.render.Light.LightType;

public class LightsUBO extends UniformBufferObject {

   // light structure:
   // struct LightSource {
   // vec4 diffuse;
   // vec4 ambient;
   // vec4 specular;
   // vec4 position;    // in camera-space, point indicator
   // vec4 direction;   // (dir, spot cutoff)
   // vec4 attenuation; // (exponent, constant, linear, quadratic)
   // };
   static final String[] ATTRIBUTES_PER_LIGHT = { "diffuse", "ambient",
                                                  "specular", "position",
                                                  "direction", "attenuation" };
   static final String[] ATTRIBUTES = { "intensity_scale" };
   
   static final String LIGHTS_NAME = "Lights";
   int numLights;

   private static String[] createLightAttributes(int nLights) {

      String[] out = new String[nLights * ATTRIBUTES_PER_LIGHT.length + ATTRIBUTES.length];
      int idx = 0;
      for (int i = 0; i < nLights; ++i) {
         for (int j=0; j<ATTRIBUTES_PER_LIGHT.length; ++j) {
            out[idx++] = "light[" + i + "]." + ATTRIBUTES_PER_LIGHT[j];
         }
      }
      for (int i=0; i<ATTRIBUTES.length; ++i) {
         out[idx++] = ATTRIBUTES[i];
      }
      
      return out;
   }

   private LightsUBO(GL3 gl, int progId, int nLights) {
      super(gl, progId, LIGHTS_NAME, createLightAttributes(nLights), GL3.GL_DYNAMIC_DRAW);
      numLights = nLights;
   }

   public void updateLights(GL3 gl, List<Light> lights, float intensityScale, 
      RigidTransform3d viewMatrix) {
      
      ByteBuffer buff = getBuffer();

      for (int i=0; i<numLights; i++) {
         int idx = i*ATTRIBUTES_PER_LIGHT.length;
         // fill in properties
         Light light = lights.get(i);
         buff.position (getByteOffset(idx++));
         putFloat (buff, light.getDiffuse(), 4);   // diffuse
         buff.position(getByteOffset(idx++));
         putFloat(buff, light.getAmbient(), 4);   // ambient
         buff.position(getByteOffset(idx++));
         putFloat(buff, light.getSpecular(), 4);  // specular
         
         // position
         buff.position (getByteOffset(idx++));
         // maybe adjust to camera space
         if (light.getLightSpace() == LightSpace.WORLD) {
            float[] flpos = light.getPosition();
            Point3d lpos = new Point3d(flpos[0], flpos[1], flpos[2]);
            lpos.transform(viewMatrix);
            buff.putFloat ((float)(lpos.x));
            buff.putFloat ((float)(lpos.y));
            buff.putFloat ((float)(lpos.z));
         } else {
            putFloat (buff, light.getPosition(), 3);  // position
         }
         // directional indicator
         if (light.getType() == LightType.DIRECTIONAL) {
            buff.putFloat (0f);
         } else {
            buff.putFloat (1f);
         }
         
         // direction
         buff.position(getByteOffset(idx++));
         // maybe adjust to camera space
         if (light.getLightSpace() == LightSpace.WORLD) {
            float[] fldir = light.getDirection();
            Vector3d ldir = new Vector3d(fldir[0], fldir[1], fldir[2]);
            ldir.transform(viewMatrix);
            buff.putFloat ((float)(ldir.x));
            buff.putFloat ((float)(ldir.y));
            buff.putFloat ((float)(ldir.z));
         } else {
            putFloat(buff,light.getDirection(), 3); // direction
         }
         
         // spot indicator
         if (light.getType() == LightType.SPOT) {
            buff.putFloat ((float)Math.cos (light.getSpotCutoff()));
         } else {
            buff.putFloat (-1f); // allow all light
         }
         
         // attenuation
         buff.position (getByteOffset(idx++));
         buff.putFloat (light.getConstantAttenuation()); // attenuation
         buff.putFloat (light.getLinearAttenuation());
         buff.putFloat (light.getQuadraticAttenuation());
         buff.putFloat (light.getSpotExponent());
      }
      
      // light intensity
      int idx = numLights*ATTRIBUTES_PER_LIGHT.length;
      buff.position(getByteOffset(idx++));
      buff.putFloat (intensityScale);

      buff.flip ();
      update(gl, buff);
   }

   private static void putFloat(ByteBuffer buff, float[] f, int len) {
      for (int i=0; i<len; ++i) {
         buff.putFloat (f[i]);
      }
   }
   
   @Override
   public LightsUBO acquire () {
      return (LightsUBO)super.acquire ();
   }
   
   public static LightsUBO generate(GL3 gl, int progId, int nLights) {
      return new LightsUBO (gl, progId, nLights);
   }

}
