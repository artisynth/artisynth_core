/**
 * Copyright (c) 2014, by the Authors: Antonio Sanchez (UBC)
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
package artisynth.core.renderables;

import maspack.geometry.Face;
import maspack.geometry.PolygonalMesh;
import maspack.render.RenderList;

public class EditablePolygonalMeshComp extends EditableMeshComp {

   PolygonalMesh pmesh;
   FaceList<FaceComponent> myFaceList = null;
   
   public EditablePolygonalMeshComp (PolygonalMesh mesh) {
      super(mesh);
      pmesh = mesh;
      myFaceList = new FaceList<FaceComponent>(FaceComponent.class, 
         "faces", "f", pmesh);
      add(myFaceList);
      updateFaces();
   }
   
   public void updateFaces() {
      myFaceList.clear();
      if (myMesh == null) {
         return;
      }
      
      for (Face face : pmesh.getFaces()) {
         myFaceList.add(new FaceComponent(face, pmesh));
      }
      
      
   }
   
   @Override
   public void prerender(RenderList list) {
      super.prerender(list);
      list.addIfVisible(myFaceList);
   }
   
   @Override
   public PolygonalMesh getMesh() {
      return pmesh;
   }
   
   public FaceList<FaceComponent> getFaceList() {
      return myFaceList;
   }
   
   public FaceComponent getFaceComponent(int idx) {
      return myFaceList.get (idx);
   }
   
}
