/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import java.awt.Color;

import artisynth.core.modelbase.ModelComponentBase;
import maspack.matrix.Vector3d;
import maspack.properties.PropertyList;
import maspack.render.Light;
import maspack.render.Light.LightSpace;
import maspack.render.Light.LightType;

public class LightComponent extends ModelComponentBase {
   Light myLight;
   
   private static PropertyList myProps = new PropertyList(LightComponent.class);
   static {
      myProps.add("enabled isEnabled setEnabled", "light enabled", true);
      myProps.add("position", "homogeneus position", Vector3d.Z_UNIT);
      myProps.add("direction", "pointed direction", Vector3d.Z_UNIT);
      myProps.add("lightSpace", "Lighting space", LightSpace.CAMERA);
      myProps.add("ambient", "ambient color", Color.WHITE);
      myProps.add("diffuse", "diffuse color", Color.WHITE);
      myProps.add("specular", "specular color", Color.WHITE);
      myProps.add("attenuation", "attenuation", Vector3d.X_UNIT);
      myProps.add("type", "light type", LightType.DIRECTIONAL);
      myProps.add("spotCutoff", "radian cutoff for spot light", Math.PI/6, "[0,1.58]");
      myProps.add("spotExponent", "spot light exponent", 0);
   }
   
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   public LightComponent(Light light) {
      myLight = light;
   }
   
   public Light getLight() {
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
   
   public void setPosition(Vector3d pos) {
      myLight.setPosition((float)pos.x, (float)pos.y, (float)pos.z);
   }
   
   public Vector3d getPosition() {
      float[] pos = myLight.getPosition();
      return new Vector3d(pos[0], pos[1], pos[2]);
   }
   
   public void setDirection(Vector3d dir) {
      myLight.setDirection((float)dir.x, (float)dir.y, (float)dir.z);
   }
   
   public Vector3d getDirection() {
      float[] dir = myLight.getDirection();
      return new Vector3d(dir[0], dir[1], dir[2]);
   }
   
   public Vector3d getAttenuation() {
      return new Vector3d(myLight.getConstantAttenuation(), 
         myLight.getLinearAttenuation(), myLight.getQuadraticAttenuation());
   }
   
   public void setAttenuation(Vector3d a) {
      myLight.setConstantAttenuation((float)a.x);
      myLight.setLinearAttenuation((float)a.y);
      myLight.setQuadraticAttenuation((float)a.z);
   }
   
   public LightType getType() {
      return myLight.getType();
   }
   
   public void setType(LightType type) {
      myLight.setType(type);
   }
   
   public double getSpotCutoff() {
      return myLight.getSpotCutoff();
   }
   
   public void setSpotCutoff(double cs) {
      myLight.setSpotCutoff((float)cs);
   }
   
   public double getSpotExponent() {
      return myLight.getSpotExponent();
   }
   
   public void setSpotExponent(double e) {
      myLight.setSpotExponent((float)e);
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
