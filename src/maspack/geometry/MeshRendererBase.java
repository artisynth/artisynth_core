/**
 * Copyright (c) 2014, by the Authors: John E Lloyd (UBC), Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package maspack.geometry;

import java.util.ArrayList;

import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.Material;
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.RenderProps.Shading;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;
import maspack.render.DiffuseTextureProps;

public abstract class MeshRendererBase {

   protected class RobSignature {
      MeshBase mesh;
      int version;
      boolean useTextures;

      public RobSignature (MeshBase mesh, RenderProps props) {
         this.mesh = mesh;
         this.version = mesh.getVersion();
         this.useTextures = false; // FINISH         
      }

      public boolean equals (RobSignature other) {
         return (other != null &&
                 other.mesh == mesh &&
                 other.version == version &&
                 other.useTextures == useTextures);
      }
   }

   protected RobSignature myRobSignature;
   protected RenderObject myRob;

   protected abstract RobSignature createSignature (
      MeshBase mesh, RenderProps props);

   private boolean renderObjectNeedsBuilding (
      MeshBase mesh, RenderProps props) {

      RobSignature sig = createSignature (mesh, props);
      if (!sig.equals (myRobSignature)) {
         myRobSignature = sig;
         return true;
      }
      else {
         return false;
      }
   }      

   protected void addPositions (RenderObject r, MeshBase mesh) {
      boolean useRenderData = mesh.isRenderBuffered() && !mesh.isFixed();
      for (int i=0; i<mesh.numVertices(); i++) {
         Vertex3d vtx = mesh.getVertex(i);
         Point3d pos = useRenderData ? vtx.myRenderPnt : vtx.pnt;
         r.addPosition((float)pos.x, (float)pos.y, (float)pos.z);
      }
      if (!mesh.isFixed()) {
         r.setPositionsDynamic (true);
      }
   }

   protected void addNormals (RenderObject r, MeshBase mesh) {
      if (mesh.hasNormals()) {
         ArrayList<Vector3d> nrms = mesh.getNormals();
         for (int i=0; i<nrms.size(); i++) {
            Vector3d nrm = nrms.get(i);
            r.addNormal((float)nrm.x, (float)nrm.y, (float)nrm.z);
         }
         if (!mesh.isFixed()) {
            r.setNormalsDynamic (true);
         }
      }
   }

   protected void addColors (RenderObject r, MeshBase mesh) {
      if (mesh.hasColors()) {
         ArrayList<float[]> colors = mesh.getColors();
         for (int i=0; i<colors.size(); i++) {
            r.addColor(colors.get(i));
         }
         if (!mesh.isColorsFixed()) {
            r.setColorsDynamic (true);
         }
      }
   }

   protected void updatePositions (RenderObject r, MeshBase mesh) {
      boolean useRenderData = mesh.isRenderBuffered() && !mesh.isFixed();
      int numv = mesh.numVertices();
      for (int i=0; i<numv; i++) {
         Vertex3d vtx = mesh.getVertex(i);
         Point3d pos = useRenderData ? vtx.myRenderPnt : vtx.pnt;
         r.setPosition(i, (float)pos.x, (float)pos.y, (float)pos.z);
      }
   }

   protected void updateNormals (RenderObject r, MeshBase mesh) {
       if (mesh.hasNormals()) {
          ArrayList<Vector3d> nrms = mesh.getNormals();
          for (int i=0; i<nrms.size(); i++) {
             Vector3d nrm = nrms.get(i);
             r.setNormal(i, (float)nrm.x, (float)nrm.y, (float)nrm.z);
          }
       }
   }

   protected void updateColors (RenderObject r, MeshBase mesh) {
      if (mesh.hasColors()) {
         ArrayList<float[]> colors = mesh.getColors();
         for (int i=0; i<colors.size(); i++) {
            float[] color = colors.get(i);
            r.setColor (i, color[0], color[1], color[2], color[3]);
         }
      }
   }

   protected void addTextureCoords (RenderObject r, MeshBase mesh) {
      if (mesh.hasTextureCoords()) {
         ArrayList<Vector3d> coords = mesh.getTextureCoords();
         for (int i=0; i<coords.size(); i++) {
            Vector3d coord = coords.get(i);
            r.addTextureCoord((float)coord.x, (float)(1-coord.y));
         }
      }
   }

   public void buildRenderObject (MeshBase mesh, RenderProps props) {

      RenderObject r = new RenderObject();
      addPositions (r, mesh);
      addNormals (r, mesh);
      addColors (r, mesh);
      addTextureCoords (r, mesh);

      myRob = r;
   }

   public void updateRenderObject (MeshBase mesh, RenderProps props) {

      RenderObject r = myRob;

      if (!mesh.isFixed()) {
         updatePositions (r, mesh);
         updateNormals (r, mesh);
      }
      if (!mesh.isColorsFixed()) {
         updateColors (r, mesh);
      }
   }

   public void prerender (MeshBase mesh, RenderProps props) {
      if (renderObjectNeedsBuilding (mesh, props)) {
         buildRenderObject (mesh, props);
      }
      else {
         updateRenderObject (mesh, props);
      }
   }

}

