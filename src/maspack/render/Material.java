/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.render;

import java.awt.Color;

public class Material {

   private float[] diffuse;  // ambient color will always track diffuse
   private float[] specular;
   private float[] emission;
   private float[] power;
   private float shininess;

   public static final float[] default_specular = {0.1f, 0.1f, 0.1f, 1f};
   public static final float[] default_diffuse = {0.8f, 0.8f, 0.8f, 1f};
   public static final float[] default_emission = {0f, 0f, 0f, 1f};
   public static final float[] default_power = {1f, 1f, 1f, 1f};

   public static final int BLACK = 0;
   public static final int WHITE = 1;
   public static final int RED = 2;
   public static final int BLUE = 3;
   public static final int GREEN = 4;
   public static final int CYAN = 5;
   public static final int MAGENTA = 6;
   public static final int YELLOW = 7;
   public static final int GOLD = 8;
   public static final int GRAY = 9;
   public static final int SILVER = 10;

   public Material() {
      diffuse = new float[4];
      specular = new float[4];
      emission = new float[4];
      power = new float[4];
      setDefaults();
   }
   
   private void setDefaults() {
      setSpecular(default_specular);
      setDiffuse(default_diffuse);
      setEmission(default_emission);
      setPower(default_power);
   }

   public Material (Material mat) {
      this();
      set (mat);
   }

   public Material (float[] diff, float[] spec, float[] em, float shin) {
      this();
      set (diff, spec, em, shin);
   }
   
   public Material (Color diff, Color spec, Color em, float shin) {
      this();
      set (diff, spec, em, shin);
   }
   
   public void set (float[] diff, float[] spec, float[] em, float shin) {
      setDiffuse (diff);
      setSpecular (spec);
      setEmission(em);
      setShininess (shin);
   }
   
   public void set (Color diff, Color spec, Color em, float shin) {
      setDiffuse (diff);
      setSpecular (spec);
      setEmission(em);
      setShininess (shin);
   }

   public void set (Material mat) {
      set (mat.diffuse, mat.specular, mat.emission, mat.shininess);
   }

   public void setShininess (float s) {
      if (s < 0) {
         s = 0;
      }
      else if (s > 128) {
         s = 128; // max supported under GL
      }
      shininess = s;
   }

   public float getShininess() {
      return shininess;
   }

   public void setSpecular (float r, float g, float b) {
      specular[0] = r;
      specular[1] = g;
      specular[2] = b;
      specular[3] = 1.0f;
   }
   
   public void setSpecular(Color c) {
      c.getComponents (specular);
   }

   public void setSpecular (float[] spec) {
      specular[0] = spec[0];
      specular[1] = spec[1];
      specular[2] = spec[2];
      specular[3] = 1.0f;
   }
   
   public void getSpecular (float[] spec) {
      spec[0] = specular[0];
      spec[1] = specular[1];
      spec[2] = specular[2];
      if (spec.length > 3) {
         spec[3] = 1.0f;
      }
   }

   public float[] getSpecular() {
      return specular;
   }

   public void setDiffuse (float r, float g, float b, float a) {
      diffuse[0] = r;
      diffuse[1] = g;
      diffuse[2] = b;
      diffuse[3] = a;
   }

   public void setDiffuse (Color c) {
      float[] diff = new float[4];
      c.getComponents (diff);
      diffuse[0] = diff[0];
      diffuse[1] = diff[1];
      diffuse[2] = diff[2];
      diffuse[3] = diff[3];

   }

   public void setDiffuse (float[] diff) {
      diffuse[0] = diff[0];
      diffuse[1] = diff[1];
      diffuse[2] = diff[2];
      if (diff.length > 3) {
         diffuse[3] = diff[3];
      } else {
         diffuse[3] = 1;
      }
   }

   public void getDiffuse (float[] diff) {
      diff[0] = diffuse[0];
      diff[1] = diffuse[1];
      diff[2] = diffuse[2];
      if (diff.length > 3) {
         diff[3] = diffuse[3];
      }
   }

   public float[] getDiffuse() {
      return diffuse;
   }

   public void setEmission(float r, float g, float b) {
      emission[0] = r;
      emission[1] = g;
      emission[2] = b;
      emission[3] = 1.0f;
   }
   
   public void setEmission (Color c) {
      c.getComponents (emission);
   }
   
   public void setEmission(float[] em) {
      emission[0] = em[0];
      emission[1] = em[1];
      emission[2] = em[2];
      emission[3] = 1.0f;
   }
   
   public void getEmission(float[] em) {
      em[0] = emission[0];
      em[1] = emission[1];
      em[2] = emission[2];
      if (em.length > 3) {
         em[3] = 1.0f;
      }
   }
   
   public float[] getEmission() {
      return emission;
   }
   
   public void setPower(float[] p) {
      power[0] = p[0];
      power[1] = p[1];
      power[2] = p[2];
      power[3] = p[3];
   }
   
   public void getPower(float[] p) {
      p[0] = power[0];
      p[1] = power[1];
      p[2] = power[2];
      p[3] = power[3];
   }
   
   public float[] getPower() {
      return power;
   }
   
   public void setAmbientPower(float a) {
      power[0] = a;
   }
   
   public float getAmbientPower() {
      return power[0];
   }
   
   public void setDiffusePower(float d) {
      power[1] = d;
   }
   
   public float getDiffusePower() {
      return power[1];
   }
   
   public void setSpecularPower(float s) {
      power[2] = s;
   }
   
   public float getSpecularPower() {
      return power[2];
   }
   
   public void setEmissionPower(float e) {
      power[3] = e;
   }
   
   public float getEmissionPower() {
      return power[3];
   }
   
//   public void apply (GL2 gl) {
//      apply (gl, GL2.GL_FRONT_AND_BACK, null);
//   }
//
//   public void apply (GL2 gl, float[] diffuseOverride) {
//      apply (gl, GL2.GL_FRONT_AND_BACK, diffuseOverride);
//   }
//
//   public void apply (GL2 gl, int sides) {
//      apply (gl, sides, null);
//   }
//
//   private void applyMat(GL2 gl, int sides, int target, float[] v, float scale) {
//      float[] m = new float[4];
//      for (int i=0; i<3; ++i) {
//         m[i] = v[i]*scale;
//      }
//      m[3] = v[3];
//      gl.glMaterialfv(sides, target, m, 0);
//   }
//   
//   public void apply (GL2 gl, int sides, float[] diffuseOverride) {
//      
//      float[] diff = diffuse;
//      
//      applyMat(gl, sides, GL2.GL_EMISSION, emission, power[3]);
//      applyMat(gl, sides, GL2.GL_SPECULAR, specular, power[2]);
//      gl.glMaterialf (sides, GL2.GL_SHININESS, shininess);
//      if (diffuseOverride != null) {
//         float[] temp = new float[4];
//         temp[0] = diffuseOverride[0];
//         temp[1] = diffuseOverride[1];
//         temp[2] = diffuseOverride[2];
//         temp[3] = diffuse[3];
//         diff = temp;
//      }
//      applyMat(gl, sides, GL2.GL_DIFFUSE, diff, power[1]);
//      applyMat(gl, sides, GL2.GL_AMBIENT, diff, power[0]);
//   }

   private String floatArrayToString (float[] array) {
      return array[0] + " " + array[1] + " " + array[2] + " " + array[3];
   }

   public String toString() {
      String s = "";
      s += floatArrayToString (specular) + "\n";
      s += floatArrayToString (diffuse) + "\n";
      s += floatArrayToString (emission) + "\n";
      s += shininess + "\n";
      s += floatArrayToString (power);
      return s;
   }

   public void setAlpha (double a) {
      //      ambient[3] = (float)a;
      //      specular[3] = (float)a;
      //      emission[3] = (float)a;
      // According to OpenGL reference, only diffuse alpha
      // is used in light model equation
      diffuse[3] = (float)a;
   }
   
   public float getAlpha() {
      return diffuse[3];
   }

   public boolean isTransparent() {
      // return (ambient[3] != 1.0f || specular[3] != 1.0f 
      //   || diffuse[3] != 1.0f || emission[3] != 1.0f);
      
      // According to OpenGL reference, only diffuse alpha
      // used in light model equation
      return (diffuse[3] != 1.0f);
   }

   public static Material createDiffuse (
      float r, float g, float b, float a, float shine) {
      Material mat = new Material();
      mat.setShininess (shine);
      mat.setSpecular (default_specular);
      mat.setEmission(default_emission);
      mat.setDiffuse (r, g, b, 1f);
      mat.setAlpha (a);
      mat.setPower (default_power);
      return mat;
   }

   public static Material createDiffuse (float[] rgba, float shine) {
      return createDiffuse (rgba[0], rgba[1], rgba[2], rgba[3], shine);
   }

   public static Material createDiffuse (float[] rgb, float alpha, float shine) {
      return createDiffuse (rgb[0], rgb[1], rgb[2], alpha, shine);
   }

   public static Material createDiffuse (Color c, float shine) {
      float[] rgba = new float[4];
      c.getComponents (rgba);
      return createDiffuse (rgba, shine);
   }

   public static Material createDiffuse (Color c, float alpha, float shine) {
      float[] rgb = new float[4];
      c.getRGBComponents (rgb);
      return createDiffuse (rgb, alpha, shine);
   }

   public static Material createSpecial (int code) {
      switch (code) {
         case WHITE: {
            return createDiffuse (1f, 1f, 1f, 1f, 64f);
         }
         case RED: {
            return createDiffuse (1f, 0f, 0f, 1f, 64f);
         }
         case BLUE: {
            return createDiffuse (0f, 0f, 1f, 1f, 64f);
         }
         case GREEN: {
            return createDiffuse (0f, 1f, 0f, 1f, 64f);
         }
         case CYAN: {
            return createDiffuse (0f, 1f, 1f, 1f, 64f);
         }
         case MAGENTA: {
            return createDiffuse (1f, 0f, 1f, 1f, 64f);
         }
         case YELLOW: {
            return createDiffuse (1f, 1f, 0f, 1f, 64f);
         }
         case BLACK: {
            return createDiffuse (0f, 0f, 0f, 1f, 64f);
         }
         case GRAY: {
            return createDiffuse (0.5f, 0.5f, 0.5f, 1f, 32f);
         }
         case SILVER: {
            return createDiffuse (0.5f, 0.45f, 0.4f, 1f, 128f);
         }
         case GOLD: {
            return createDiffuse (0.93f, 0.8f, 0.063f, 1f, 128f);
         }
         default: {
            throw new ArrayIndexOutOfBoundsException (code);
         }
      }
   }

   /**
    * Returns true if this material is exactly equal to another material.
    * 
    * @param mat
    * material to compare to
    * @return true if the materials are equal
    */
   public boolean equal (Material mat) {
      for (int i = 0; i < 3; i++) {
         if (specular[i] != mat.specular[i]) {
            return false;
         }
         if (diffuse[i] != mat.diffuse[i]) {
            return false;
         }
         if (emission[i] != mat.emission[i]) {
            return false;
         }
         if (power[i] != mat.power[i]) {
            return false;
         }
      }
      if (shininess != mat.shininess) {
         return false;
      }
      // alpha value
      if (diffuse[3] != mat.diffuse[3]) {
         return false;
      }
      
      if (power[3] != mat.power[3]) {
         return false;
      }
      return true;
   }

}
