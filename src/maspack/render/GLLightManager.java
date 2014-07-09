/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.util.ArrayList;

import javax.media.opengl.GL2;

public class GLLightManager {

   int maxLights = 0;
   GL2 gl;

   private ArrayList<GLLight> lights = null;

   public GLLightManager () {
      lights = new ArrayList<GLLight>();
   }
   
   public void init(GL2 gl) {
      int[] maxLightsHolder = new int[1];
      gl.glGetIntegerv(GL2.GL_MAX_LIGHTS, maxLightsHolder, 0);
      maxLights = maxLightsHolder[0];
      this.gl = gl;
      
      if (lights.size() > maxLights) {
         System.err.println("Too many lights!  Max: " + maxLights );
      }
      
      for (int i=maxLights; i < lights.size(); i++) {
         lights.get(i).setEnabled(false);
      }
   }

   public GLLight createLight(float[] position, float[] ambient,
      float[] diffuse, float[] specular) {
      GLLight light = new GLLight(position, ambient, diffuse, specular);
      addLight(light);
      return light;
   }

   public void addLight(GLLight light) {
      if (gl != null && lights.size() >= maxLights) {
         System.err.println("Too many lights enabled (max " + maxLights + ")");
      } else {
         lights.add(light);
         light.setId(lights.size() - 1);
         light.setEnabled(true);
      }
   }

   public void removeLight(GLLight light) {
      int idx = lights.indexOf(light);
      if (idx >= 0) {

         // disable light to remove
         light.setEnabled(gl, false);
         light.setId(-1);

         // replace light at index idx with the one at the end of the list
         GLLight endLight = lights.get(lights.size() - 1);
         // disable temporarily, since id will change (will re-enable on next render)
         endLight.setEnabled(gl, false);  
         
         lights.set(idx, endLight);
         endLight.setId(idx);

         // remove last light
         lights.remove(lights.size() - 1);

      }
   }

   public GLLight getLight(int id) {
      if (id >= 0 && id < lights.size()) {
         return lights.get(id);
      }
      return null;
   }
   
   ArrayList<GLLight> getLights() {
      return lights;
   }

   public int numLights() {
      return lights.size();
   }

   public int maxLights() {
      return maxLights;
   }

   public void clearLights() {
      for (GLLight light : lights) {
         if (light.isEnabled(gl)) {
            // disable every light
            light.setEnabled(gl, false);
         }
      }
      lights.clear();
   }

}
