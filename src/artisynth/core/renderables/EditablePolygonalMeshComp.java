/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import artisynth.core.mechmodels.MeshComponent;
import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshBase;
import maspack.render.RenderList;

public class EditablePolygonalMeshComp extends EditableMeshComp {

   FaceList<FaceComponent> myFaceList = null;
   
   public EditablePolygonalMeshComp () {
      super();
      myFaceList = new FaceList<FaceComponent>(
         FaceComponent.class, "faces", "f");
      add(myFaceList);
   }

   public EditablePolygonalMeshComp (PolygonalMesh mesh) {
      this();
      setMesh (mesh);
   }
   
   public EditablePolygonalMeshComp (MeshComponent mcomp) {
      this();
      if (!(mcomp.getMesh() instanceof PolygonalMesh)) {
         throw new IllegalArgumentException (
            "mesh component does not contain a PolygonalMesh");
      }
      setMeshComp (mcomp);
   }

   public void updateFaces() {
      myFaceList.clear();
      MeshBase mesh = getMesh();
      if (mesh == null || !(mesh instanceof PolygonalMesh)) {
         return;
      }
      PolygonalMesh pmesh = (PolygonalMesh)mesh;
      for (Face face : pmesh.getFaces()) {
         myFaceList.add(new FaceComponent(face, pmesh));
      }
   }
   
   public void updateComponents() {
      super.updateComponents();
      updateFaces();
   }
   
   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      list.addIfVisible(myFaceList);
   }
   
   // @Override
   // public PolygonalMesh getMesh() {
   //    return pmesh;
   // }
   
   public FaceList<FaceComponent> getFaceList() {
      return myFaceList;
   }
   
   public FaceComponent getFaceComponent(int idx) {
      return myFaceList.get (idx);
   }
   
}
