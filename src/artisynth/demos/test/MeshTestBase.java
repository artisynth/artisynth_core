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

public class MeshTestBase extends RootModel {

   public static PropertyList myProps =
      new PropertyList (MeshTestBase.class, RootModel.class);

   MeshBase myMesh;

   static {
      myProps.add ("hasNormals", "mesh has normals defined", false);
      myProps.add ("hasVertexColoring", "vertex coloring enabled", false);
      myProps.add ("hasFeatureColoring", "feature coloring enabled", false);
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

   ControlPanel createControlPanel (MeshComponent meshBody) {
      ControlPanel panel = new ControlPanel();
      panel.addWidget (this, "hasNormals");
      panel.addWidget (this, "hasVertexColoring");
      panel.addWidget (this, "hasFeatureColoring");
      panel.addWidget (meshBody, "renderProps.shading");
      return panel;
   }

}
