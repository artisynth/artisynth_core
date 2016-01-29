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

public class PointMeshTest extends MeshTestBase {

   public static PropertyList myProps =
      new PropertyList (PointMeshTest.class, MeshTestBase.class);

   static {
      myProps.add ("normalLen", "mesh normal length", 0);
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public double getNormalLen() {
      return ((PointMesh)myMesh).getNormalRenderLen();
   }

   public void setNormalLen (double len) {
      ((PointMesh)myMesh).setNormalRenderLen(len);
   }

   void addControlPanel (MeshComponent meshBody) {
      ControlPanel panel = createControlPanel (meshBody);
      panel.addWidget (meshBody, "renderProps.pointStyle");
      panel.addWidget (meshBody, "renderProps.pointColor");
      panel.addWidget (meshBody, "renderProps.pointRadius");
      panel.addWidget (meshBody, "renderProps.pointSize");
      panel.addWidget (this, "normalLen");
      panel.addWidget (meshBody, "renderProps.lineColor");
      panel.addWidget (meshBody, "renderProps.lineWidth");
      addControlPanel (panel);
   }

   public void build (String[] args) {

      RandomGenerator.setSeed (0x1234);

      MechModel msmod = new MechModel ("msmod");
      PolygonalMesh template = MeshFactory.createSphere (2.0, 30);
      PointMesh mesh = new PointMesh();
      myMesh = mesh;

      for (int i=0; i<template.numVertices(); i++) {
         Point3d pnt = template.getVertex(i).pnt;
         mesh.addVertex (pnt);
      }
      setHasNormals (true);

      //System.out.println ("num vertices: " + mesh.numVertices());
      FixedMeshBody meshBody = new FixedMeshBody (mesh);
      RenderProps.setPointSize (meshBody, 2);
      RenderProps.setPointRadius (meshBody, 0.1);

      msmod.addMeshBody (meshBody);
      addModel (msmod);
      addControlPanel (meshBody);
   }


}
