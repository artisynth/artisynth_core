package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.ArrayList;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.geometry.*;
import maspack.geometry.io.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class MeshViewer extends RootModel {

   public void build (String[] args) {

      MechModel mech = new MechModel ("msmod");
      addModel (mech);

      ArrayList<String> meshFileNames = new ArrayList<>();

      for (int i=0; i<args.length; i++) {
         if (!args[i].startsWith ("-")) {
            meshFileNames.add (args[i]);
         }
         else {
            System.out.println ("Warning: unrecognized model option "+args[i]);
            System.out.println ("Usage: MeshViewer <meshFile> ...");
         }
      }

      if (meshFileNames.size() > 0) {
         for (String fileName : meshFileNames) {
            try {
               MeshReader reader = GenericMeshReader.createReader (fileName);
               MeshBase mesh = reader.readMesh(null);
               if (reader instanceof WavefrontReader) {
                  RenderProps props = ((WavefrontReader)reader).getRenderProps();
                  if (props != null) {
                     mesh.setRenderProps (props);
                  }
                  else {
                     System.out.println ("No render props specified");
                  }
               }
               if (mesh instanceof PolygonalMesh) {
                  // use rigid body so we can add markers to it
                  RigidBody body = RigidBody.createFromMesh (
                     null, (PolygonalMesh)mesh, /*density=*/1.0, /*scale=*/1.0);
                  body.setDynamic (false);
                  mech.addRigidBody (body);
               }
               else {
                  mech.addMeshBody (new FixedMeshBody(mesh));
               }
            }
            catch (Exception e) {
               System.out.println ("Can't read mesh " + fileName);
               e.printStackTrace(); 
            }
         }
      }
      else {
         PolygonalMesh mesh = MeshFactory.createIcosahedralBowl (1.0, 1.1, 4);
         FixedMeshBody mbody = new FixedMeshBody ("mesh", mesh);

         // PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (1.0, 3);
         // double ang = Math.toRadians (58.3 - 0.017474411461004962);
         // mesh.transform (
         //    new RigidTransform3d(0, 0, 0, 0, 0, ang));
         // ArrayList<Point3d> equator = new ArrayList<>();
         // for (Vertex3d v : mesh.getVertices()) {
         //    if (Math.abs(v.pnt.z) < 0.05) {
         //       equator.add (new Point3d(v.pnt));
         //    }
         // }
         // Plane plane = new Plane();
         // plane.fit (equator);
         // System.out.println ("plane.normal=" + plane.normal);
         // System.out.println ("plane.offset=" + plane.offset);
         // System.out.println (
         //    "angle=" + Math.toDegrees(
         //       Math.atan2 (plane.normal.y, plane.normal.z)));
         // FixedMeshBody mbody = new FixedMeshBody ("mesh", mesh);


         RenderProps.setFaceStyle (mbody, FaceStyle.FRONT_AND_BACK);
         // RenderProps.setDrawEdges (mbody, true);
         // RenderProps.setShading (mbody, Shading.NONE);

         // PolygonalMesh mesh = MeshFactory.createIcosahedron(1.0);
         // mesh = MeshFactory.subdivide(mesh);
         // FixedMeshBody mbody = new FixedMeshBody ("mesh", mesh);
         mech.addMeshBody (mbody);
      }
      double rad = RenderableUtils.getRadius (this);
      RenderProps.setSphericalPoints (mech, 0.01*rad, Color.GREEN);
   }
}



