/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;


public class Light {

   public static enum LightSpace {
      CAMERA, WORLD
   }
   
   public static enum LightType {
      DIRECTIONAL,
      POINT,
      SPOT
   }
   
   private LightSpace myLightSpace = LightSpace.CAMERA;
   private float[] position = new float[] { 0, 0, 0, 0 };
   private float[] direction = new float[] { 0, 0, 0, 0 };
   private float spotCutoff = 0;
   private float spotExponent = 0;
   private float constantAttenuation = 1;
   private float linearAttenuation = 0;
   private float quadraticAttenuation = 0;
   private float[] ambient = new float[] { 0, 0, 0, 0 };
   private float[] diffuse = new float[] { 1, 1, 1, 1 };
   private float[] specular = new float[] { 0, 0, 0, 0 };
   LightType type = LightType.DIRECTIONAL;
   
   int id = 0;
   boolean enabled = true;
   
   public Light() {
   }

   public Light (float[] iposition, float[] iambient, float[] idiffuse,
      float[] ispecular) {
      
      // detect type from position
      if (iposition[3] < 1e-2) {
         type = LightType.DIRECTIONAL;
         // negate direction (direction TO light)
         iposition[0] = -iposition[0];
         iposition[1] = -iposition[1];
         iposition[2] = -iposition[2];
         setDirection(iposition);
      } else {
         type = LightType.POINT;
         setPosition(iposition);
      }

      setAmbient(iambient);
      setDiffuse(idiffuse);
      setSpecular(ispecular);
      setLightSpace(LightSpace.CAMERA);
   }

   public void setPosition (float[] pos) {
      for (int i=0; i<3; ++i) {
         position[i] = pos[i];
      }
      position[3] = 1.0f;
   }
   
   public void setPosition (float x, float y, float z) {
      position[0] = x;
      position[1] = y;
      position[2] = z;
      position[3] = 1.0f;
   }
   
   public void setDirection (float[] dir) {
      float norm = (float)Math.sqrt(dir[0]*dir[0]+dir[1]*dir[1]+dir[2]*dir[2]);
      for (int i=0; i<3; ++i) {
         direction[i] = dir[i]/norm;
      }
      direction[3] = 0.0f;
   }
   
   public void setDirection(float x, float y, float z) {
      float norm = (float)Math.sqrt(x*x+y*y+z*z);
      direction[0] = x/norm;
      direction[1] = y/norm;
      direction[2] = z/norm;
      direction[3] = 0.0f;
   }
   
   public void setType(LightType type) {
      this.type = type;
   }
   
   public LightType getType() {
      return type;
   }

   public void setAmbient (float[] a) {
      for (int i=0; i<4; ++i) {
         ambient[i] = a[i];
      }
   }
   
   public void setAmbient (float x, float y, float z, float w) {
      ambient[0] = x;
      ambient[1] = y;
      ambient[2] = z;
      ambient[3] = w;
   }

   public void setDiffuse (float[] d) {
      for (int i=0; i<4; ++i) {
         diffuse[i] = d[i];
      }
   }
   
   public void setDiffuse (float x, float y, float z, float w) {
      diffuse[0] = x;
      diffuse[1] = y;
      diffuse[2] = z;
      diffuse[3] = w;
   }

   public void setSpecular (float[] s) {
      for (int i=0; i<4; ++i) {
         specular[i] = s[i];
      }
   }
   public void setSpecular (float x, float y, float z, float w) {
      specular[0] = x;
      specular[1] = y;
      specular[2] = z;
      specular[3] = w;
   }

   public float[] getPosition() {
      return position;
   }
   
   public float[] getDirection() {
      return direction;
   }
   
   /**
    * Sets the angular cut-off
    * @param rad angular cut-off (radians)
    */
   public void setSpotCutoff(float rad) {
      spotCutoff = rad;
   }
   
   public float getSpotCutoff() {
      return spotCutoff;
   }
   
   public float getSpotExponent() {
      return spotExponent;
   }
   
   public void setSpotExponent(float c) {
      spotExponent = c;
   }
   
   public void setConstantAttenuation(float a) {
      constantAttenuation = a;
   }
   
   public float getConstantAttenuation() {
      return constantAttenuation;
   }
   
   public void setLinearAttenuation(float c) {
      linearAttenuation = c;
   }
   
   public float getLinearAttenuation() {
      return linearAttenuation;
   }
   
   public void setQuadraticAttenuation(float c) {
      quadraticAttenuation = c;
   }
   
   public float getQuadraticAttenuation() {
      return quadraticAttenuation;
   }
   
   public void setAttenuation(float constant, float linear, float quadratic) {
      constantAttenuation = constant;
      linearAttenuation = linear;
      quadraticAttenuation = quadratic;
   }

   public float[] getAmbient() {
      return ambient;
   }

   public float[] getSpecular() {
      return specular;
   }

   public float[] getDiffuse() {
      return diffuse;
   }
   
   public void setLightSpace(LightSpace lightSpace) {
      myLightSpace = lightSpace;
   }
   
   public LightSpace getLightSpace() {
      return myLightSpace;
   }
   
   public void setId(int id) {
      this.id = id;
   }
   
   public int getId() {
      return id;
   }
   
   public boolean isEnabled() {
      return enabled;
   }
   
   public void setEnabled(boolean set) {
      enabled = set;
   }

}
