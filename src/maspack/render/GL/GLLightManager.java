/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render.GL;

import java.util.ArrayList;

import maspack.render.Light;

public class GLLightManager {

   int maxLights;
   float maxIntensity;

   private ArrayList<Light> lights = null;

   public GLLightManager () {
      lights = new ArrayList<Light>();
      maxLights = Integer.MAX_VALUE;
      maxIntensity = 1.0f;
   }
   
   public void setMaxLights(int max) {
      maxLights = max;
   }

   public Light createLight(float[] position, float[] ambient,
      float[] diffuse, float[] specular) {
      Light light = new Light(position, ambient, diffuse, specular);
      addLight(light);
      return light;
   }

   public void addLight(Light light) {
      lights.add(light);
      light.setId(lights.size() - 1);
      if (light.getId() < maxLights) {
         light.setEnabled(true);
      } else {
         light.setEnabled(false);
      }
   }

   private void doRemoveLight (int idx) {
      Light light = lights.get(idx);
      lights.remove(idx);
      // correct light IDs in list
      for (int i=idx; i<lights.size(); ++i) {
         lights.get(i).setId(i);
      }
      light.setId(-1);        
   }
   
   public boolean removeLight(Light light) {
      int idx = lights.indexOf(light);
      if (idx >= 0) {
         doRemoveLight (idx);
         return true;
      }
      else {
         return false;
      }
   }

   public boolean removeLight(int id) {
      if (id >= 0 && id < lights.size()) {
         doRemoveLight (id);
         return true;
      }
      else {
         return false;
      }
   }

   public Light getLight(int id) {
      if (id >= 0 && id < lights.size()) {
         return lights.get(id);
      }
      return null;
   }
   
   public int indexOfLight (Light light) {
      return lights.indexOf (light);
   }
   
   public ArrayList<Light> getLights() {
      return lights;
   }

   public int numLights() {
      return lights.size();
   }

   public int maxLights() {
      return maxLights;
   }

   public void clearLights() {
      lights.clear();
   }
   
   /**
    * Intensity for scaling light parameters for {@code HDR->LDR}
    */
   public void setMaxIntensity(float intensity) {
      maxIntensity = intensity;
   }
   
   /**
    * Intensity for scaling light parameters for {@code HDR->LDR}
    * 
    * @return maximum intensity
    */
   public float getMaxIntensity() {
      return maxIntensity;
   }

}
