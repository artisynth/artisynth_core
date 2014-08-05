/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.awt.Color;

import maspack.matrix.Vector4d;
import maspack.properties.PropertyList;
import maspack.render.GLLight;
import maspack.render.GLLight.LightSpace;
import artisynth.core.modelbase.ModelComponentBase;

public class LightComponent extends ModelComponentBase {
   GLLight myLight;
   
   private static PropertyList myProps = new PropertyList(LightComponent.class);
   static {
      myProps.add("enabled isEnabled setEnabled", "light enabled", true);
      myProps.add("position", "homogeneus position", Vector4d.Z_UNIT);
      myProps.add("lightSpace", "Lighting space", LightSpace.CAMERA);
      myProps.add("ambient", "ambient color", Color.WHITE);
      myProps.add("diffuse", "diffuse color", Color.WHITE);
      myProps.add("specular", "specular color", Color.WHITE);
   }
   
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public LightComponent(GLLight light) {
      myLight = light;
   }
   
   public GLLight getLight() {
      return myLight;
   }
   
   public void setAmbient(Color color) {
      float[] rgba = new float[4];
      color.getColorComponents(rgba);
      myLight.setAmbient(rgba[0], rgba[1], rgba[2], rgba[3]);
   }
   
   public Color getAmbient() {
      float[] rgba = myLight.getAmbient();
      return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
   }
   
   public void setDiffuse(Color color) {
      float[] rgba = new float[4];
      color.getColorComponents(rgba);
      myLight.setDiffuse(rgba[0], rgba[1], rgba[2], rgba[3]);
   }
   
   public Color getDiffuse() {
      float[] rgba = myLight.getDiffuse();
      return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
   }
   
   public void setSpecular(Color color) {
         float[] rgba = new float[4];
         color.getColorComponents(rgba);
         myLight.setSpecular(rgba[0], rgba[1], rgba[2], rgba[3]);
   }
   
   public Color getSpecular() {
      float[] rgba = myLight.getSpecular();
      return new Color(rgba[0], rgba[1], rgba[2], rgba[3]);
   }
   
   public LightSpace getLightSpace() {
      return myLight.getLightSpace();
   }
   
   public void setLightSpace(LightSpace space) {
      myLight.setLightSpace(space);
   }
   
   public void setPosition(Vector4d pos) {
      myLight.setPosition((float)pos.x, (float)pos.y, (float)pos.z, (float)pos.w);
   }
   
   public Vector4d getPosition() {
      float[] pos = myLight.getPosition();
      return new Vector4d(pos[0], pos[1], pos[2], pos[3]);
   }
   
   public int getId() {
      return myLight.getId();
   }
   
   public boolean isEnabled() {
      return myLight.isEnabled();
   }
   
   public void setEnabled(boolean set) {
      myLight.setEnabled(set);
   }
   
}
