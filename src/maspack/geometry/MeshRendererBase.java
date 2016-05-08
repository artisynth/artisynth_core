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
import maspack.render.RenderObject;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.ColorInterpolation;

public abstract class MeshRendererBase {
   
   private MeshBase myMesh;
   private RenderObject myRob;
   private RobSignature mySignature;

   protected class RobSignature {
      MeshBase mesh;
      int version;

      public RobSignature (MeshBase mesh, RenderProps props) {
         this.mesh = mesh;
         this.version = mesh.getVersion();
      }
      
      public MeshBase getMesh() {
         return mesh;
      }

      public boolean equals (RobSignature other) {
         return (other != null &&
         other.mesh == mesh &&
         other.version == version);
      }
   }

   
   public MeshRendererBase(MeshBase mesh) {
      myMesh = mesh;
      mySignature = null;
   }


   protected boolean usingHSV (MeshBase mesh) {
      return (mesh.hasColors() && 
      mesh.getColorInterpolation() == ColorInterpolation.HSV);
   }

   protected RobSignature createSignature (MeshBase mesh, RenderProps props) {
      return new RobSignature (mesh, props);
   }

   private boolean renderObjectNeedsBuilding (
      MeshBase mesh, RenderProps props, RobSignature a, RobSignature b) {
      if (!a.equals (b)) {
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
   }

   protected void addNormals (RenderObject r, MeshBase mesh) {
      if (mesh.hasNormals()) {
         ArrayList<Vector3d> nrms = mesh.getNormals();
         for (int i=0; i<nrms.size(); i++) {
            Vector3d nrm = nrms.get(i);
            r.addNormal((float)nrm.x, (float)nrm.y, (float)nrm.z);
         }
      }
   }

   protected void addColors (RenderObject r, MeshBase mesh) {
      if (mesh.hasColors()) {
         ArrayList<float[]> colors = mesh.getColors();
         for (int i=0; i<colors.size(); i++) {
            r.addColor(colors.get(i));
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
            r.addTextureCoord((float)coord.x, (float)coord.y);
         }
      }
   }
   
   protected void updateTextureCoords (RenderObject r, MeshBase mesh) {
      if (mesh.hasTextureCoords()) {
         ArrayList<Vector3d> coords = mesh.getTextureCoords ();
         for (int i=0; i<coords.size(); i++) {
            Vector3d color = coords.get(i);
            r.setTextureCoord (i, (float)color.x, (float)color.y);
         }
      }
   }

   protected RenderObject buildRenderObject (MeshBase mesh, RenderProps props) {

      RenderObject r = new RenderObject();
      addPositions (r, mesh);
      addNormals (r, mesh);
      addColors (r, mesh);
      addTextureCoords (r, mesh);

      return r;
   }

   protected void updateRenderObject (MeshBase mesh, RenderProps props, RenderObject robj) {
      if (!mesh.isFixed()) {
         updatePositions (robj, mesh);
         updateNormals (robj, mesh);
      }
      if (!mesh.isColorsFixed()) {
         updateColors (robj, mesh);
      }
      if (!mesh.isTextureCoordsFixed ()) {
         updateTextureCoords (robj, mesh);
      }
   }
   
   public MeshBase getMesh() {
      return myMesh;
   }
   
   public RenderObject getRenderObject() {
      return myRob;
   }

   /**
    * Updates rendering information
    * @param props render properties
    */
   public void prerender (RenderProps props) {

      RobSignature sig = createSignature (myMesh, props);
      if (myRob == null ||
          renderObjectNeedsBuilding (myMesh, props, sig, mySignature)) {
         RenderObject robj = buildRenderObject (myMesh, props);
         myRob = robj;
         mySignature = sig;
      }
      else {
         updateRenderObject (myMesh, props, myRob);
      }
   }


   public abstract void render (Renderer renderer, RenderProps props, int flags);

}

