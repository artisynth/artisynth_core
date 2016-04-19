package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.geometry.*;
import maspack.properties.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.render.*;
import maspack.render.Renderer.ColorMixing;
import maspack.render.Renderer.ColorInterpolation;

public class MeshTestBase extends RootModel {

   public static PropertyList myProps =
      new PropertyList (MeshTestBase.class, RootModel.class);

   MeshBase myMesh;

   static {
      myProps.add ("hasNormals", "mesh has normals defined", false);
      myProps.add ("hasVertexColoring", "vertex coloring enabled", false);
      myProps.add ("hasFeatureColoring", "feature coloring enabled", false);
      myProps.add ("colorMixing", "color coloring mix", ColorMixing.REPLACE);
      myProps.add (
         "colorInterpolation", "color interpolation", ColorInterpolation.RGB);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public boolean getHasNormals () {
      return myMesh.hasNormals();
   }

   public void setHasNormals (boolean enabled) {
      if (enabled != myMesh.hasNormals()) {
         if (enabled) {
            ArrayList<Vector3d> normals = new ArrayList<Vector3d>();
            for (int i=0; i<myMesh.numVertices(); i++) {
               Point3d pnt = myMesh.getVertex(i).pnt;
               Vector3d nrm = new Vector3d(pnt);
               nrm.normalize();
               normals.add (nrm);
            }
            myMesh.setNormals (normals, null);
         }
         else {
            myMesh.clearNormals();
         }
      }
   }
 
  private float[][] myPalette = new float[][] {
      new float[] { 1.0f, 0.0f, 0.0f },
      new float[] { 0.0f, 0.5f, 0.0f },
      new float[] { 0.0f, 0.0f, 0.5f }
   };

   public boolean getHasVertexColoring () {
      return myMesh.getVertexColoringEnabled();
   }

   public void setHasVertexColoring (boolean enabled) {
      if (enabled != myMesh.getVertexColoringEnabled()) {
         if (enabled) {
            myMesh.setVertexColoringEnabled();
            for (int i=0; i<myMesh.numVertices(); i++) {
               myMesh.setColor (i, myPalette[i%myPalette.length]);
            }
         }
         else {
            myMesh.clearColors();
         }
      }
   }

   public boolean getHasFeatureColoring () {
      return myMesh.getFeatureColoringEnabled();
   }

   public void setHasFeatureColoring (boolean enabled) {
      if (enabled != myMesh.getFeatureColoringEnabled()) {
         if (enabled) {
            myMesh.setFeatureColoringEnabled();
            for (int i=0; i<myMesh.numFeatures(); i++) {
               myMesh.setColor (i, myPalette[i%myPalette.length]);
            }
         }
         else {
            myMesh.clearColors();
         }
      }
   }

   public ColorMixing getColorMixing() {
      return myMesh.getVertexColorMixing ();
   }

   public void setColorMixing (ColorMixing cmix) {
      myMesh.setVertexColorMixing (cmix);
   }

   public ColorInterpolation getColorInterpolation() {
      return myMesh.getColorInterpolation ();
   }

   public void setColorInterpolation (ColorInterpolation cmix) {
      myMesh.setColorInterpolation (cmix);
   }

   ControlPanel createControlPanel (MeshComponent meshBody) {
      ControlPanel panel = new ControlPanel();
      panel.addWidget (this, "hasNormals");
      panel.addWidget (this, "hasVertexColoring");
      panel.addWidget (this, "hasFeatureColoring");
      panel.addWidget (meshBody, "renderProps.shading");
      panel.addWidget (this, "colorMixing");
      panel.addWidget (this, "colorInterpolation");
      return panel;
   }

}
