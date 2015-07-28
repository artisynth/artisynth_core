package maspack.render.GL.GL3;

import java.nio.FloatBuffer;
import java.util.List;

import javax.media.opengl.GL3;

import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.render.GL.GLLight;
import maspack.render.GL.GLLight.LightSpace;
import maspack.render.GL.GLLight.LightType;

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

   public LightsUBO(GL3 gl, int progId, int nLights) {
      super(gl, progId, LIGHTS_NAME, createLightAttributes(nLights), GL3.GL_DYNAMIC_DRAW);
      numLights = nLights;
   }

   public void updateLights(GL3 gl, List<GLLight> lights, float intensityScale, RigidTransform3d viewMatrix) {
      float[] lightbuff = new float[getSize() / Float.BYTES];

      for (int i=0; i<numLights; i++) {
         int idx = i*ATTRIBUTES_PER_LIGHT.length;
         // fill in properties
         int pos = getOffset(idx++) / Float.BYTES;
         GLLight light = lights.get(i);
         
         copy(lightbuff, pos, light.getDiffuse(), 4);   // diffuse
         pos = getOffset(idx++) / Float.BYTES;
         copy(lightbuff, pos, light.getAmbient(), 4);   // ambient
         pos = getOffset(idx++) / Float.BYTES;
         copy(lightbuff, pos, light.getSpecular(), 4);  // specular
         
         // position
         pos = getOffset(idx++) / Float.BYTES;
         // maybe adjust to camera space
         if (light.getLightSpace() == LightSpace.WORLD) {
            float[] flpos = light.getPosition();
            Point3d lpos = new Point3d(flpos[0], flpos[1], flpos[2]);
            lpos.transform(viewMatrix);
            lightbuff[pos] = (float)(lpos.x);
            lightbuff[pos+1] = (float)(lpos.y);
            lightbuff[pos+2] = (float)(lpos.z);
         } else {
            copy(lightbuff, pos, light.getPosition(), 3);  // position
         }
         // directional indicator
         if (light.getType() == LightType.DIRECTIONAL) {
            lightbuff[pos + 3] = 0f;
         } else {
            lightbuff[pos + 3] = 1f;
         }
         
         // direction
         pos = getOffset(idx++) / Float.BYTES;
         // maybe adjust to camera space
         if (light.getLightSpace() == LightSpace.WORLD) {
            float[] fldir = light.getDirection();
            Vector3d ldir = new Vector3d(fldir[0], fldir[1], fldir[2]);
            ldir.transform(viewMatrix);
            lightbuff[pos] = (float)(ldir.x);
            lightbuff[pos+1] = (float)(ldir.y);
            lightbuff[pos+2] = (float)(ldir.z);
         } else {
            copy(lightbuff, pos, light.getDirection(), 3); // direction
         }
         
         // spot indicator
         if (light.getType() == LightType.SPOT) {
            lightbuff[pos + 3] = light.getSpotCosCutoff();
         } else {
            lightbuff[pos + 3] = -2; // allow all light
         }
         pos = getOffset(idx++) / Float.BYTES;
         lightbuff[pos++] = light.getConstantAttenuation(); // attenuation
         lightbuff[pos++] = light.getLinearAttenuation();
         lightbuff[pos++] = light.getQuadraticAttenuation();
         lightbuff[pos++] = light.getSpotExponent();
      }
      
      // light intensity
      int idx = numLights*ATTRIBUTES_PER_LIGHT.length;
      int pos = getOffset(idx++) / Float.BYTES;
      lightbuff[pos] = intensityScale;

      FloatBuffer data = FloatBuffer.wrap(lightbuff);
      update(gl, data);
   }

   private static void copy(float[] out, int offset, float[] in, int len) {
      int idx = offset;
      for (int i = 0; i < len; ++i) {
         out[idx++] = in[i];
      }
   }

}
