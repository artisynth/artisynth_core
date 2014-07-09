/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import javax.media.opengl.GL2;

public class GLLight {

   public static enum LightSpace {
      CAMERA, WORLD
   }
   
   private LightSpace myLightSpace = LightSpace.CAMERA;
   private float[] position = new float[] { 0, 0, 0, 0 },
   ambient = new float[] { 0, 0, 0, 0 }, diffuse = new float[] { 0, 0, 0, 0 },
   specular = new float[] { 0, 0, 0, 0 };
   int id = 0;
   boolean enabled = true;
   
   public GLLight() {
   }

   public GLLight (float[] iposition, float[] iambient, float[] idiffuse,
   float[] ispecular) {

      if (iposition.length >= 4)
         for (int c = 0; c < 4; c++)
            position[c] = iposition[c];

      if (iambient.length >= 4)
         for (int c = 0; c < 4; c++)
            ambient[c] = iambient[c];

      if (idiffuse.length >= 4)
         for (int c = 0; c < 4; c++)
            diffuse[c] = idiffuse[c];

      if (ispecular.length >= 4)
         for (int c = 0; c < 4; c++)
            specular[c] = ispecular[c];
   }

   public void setupLight(GL2 gl) {
      setupLight(GL2.GL_LIGHT0+id, gl, myLightSpace);
   }
   
   public void setupLight (int lightConstant, GL2 gl) {
      setupLight(lightConstant, gl, myLightSpace);
   }
   
   public void setupLight (int lightConstant, GL2 gl, LightSpace lightSpace) {
      
      if (enabled != isEnabled(gl)) {
         setEnabled(gl, enabled);
      }
      
      if (lightSpace == LightSpace.CAMERA) {
         gl.glPushMatrix();
         gl.glLoadIdentity();
      }
      gl.glLightfv (lightConstant, GL2.GL_POSITION, position, 0);
      gl.glLightfv (lightConstant, GL2.GL_AMBIENT, ambient, 0);
      gl.glLightfv (lightConstant, GL2.GL_DIFFUSE, diffuse, 0);
      gl.glLightfv (lightConstant, GL2.GL_SPECULAR, specular, 0);
      
      if (myLightSpace == LightSpace.CAMERA) {
         gl.glPopMatrix();
      }
   }

   public void setPosition (float x, float y, float z, float w) {
      position[0] = x;
      position[1] = y;
      position[2] = z;
      position[3] = w;
   }

   public void setAmbient (float x, float y, float z, float w) {
      ambient[0] = x;
      ambient[1] = y;
      ambient[2] = z;
      ambient[3] = w;
   }

   public void setDiffuse (float x, float y, float z, float w) {
      diffuse[0] = x;
      diffuse[1] = y;
      diffuse[2] = z;
      diffuse[3] = w;
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
   
   public boolean isEnabled(GL2 gl) {
      return gl.glIsEnabled(GL2.GL_LIGHT0+id);
   }
   
   public void setEnabled(GL2 gl, boolean set) {
      if (set != isEnabled(gl)) {
         if (set) {
            gl.glEnable(GL2.GL_LIGHT0+id);
         } else {
            gl.glDisable(GL2.GL_LIGHT0+id);
         }
      }
   }

}
