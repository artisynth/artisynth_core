package artisynth.demos.test;

import java.io.*;
import java.awt.Color;
import java.util.*;
import java.awt.event.*;

import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import artisynth.core.gui.*;
import maspack.geometry.*;
import maspack.geometry.io.*;
import maspack.matrix.*;
import maspack.util.*;
import maspack.widgets.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class MeshViewer extends RootModel {

   MechModel myMech;

   public void build (String[] args) {

      myMech = new MechModel ("msmod");
      addModel (myMech);

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
                  myMech.addRigidBody (body);
               }
               else {
                  myMech.addMeshBody (new FixedMeshBody(mesh));
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
         //PolygonalMesh mesh = MeshFactory.createIcosahedralSphere (0.5, 0);

         //PolygonalMesh mesh = MeshFactory.createPlane (0.5, 0.5);
         //mesh.transform (new RigidTransform3d (0,0,0, 0,0,Math.PI/2));

         //PolygonalMesh mesh = MeshFactory.createBox (
         //   1.0, 0.5, 0.5, new Point3d(), 2, 2, 2, /*addNormals*/false);
         //mesh.clearHardEdges();

         RigidBody body = RigidBody.createFromMesh (
            null, mesh, /*density=*/1.0, /*scale=*/1.0);
         body.setDynamic (false);

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


         //RenderProps.setFaceStyle (mbody, FaceStyle.FRONT_AND_BACK);
         // RenderProps.setDrawEdges (mbody, true);
         // RenderProps.setShading (mbody, Shading.NONE);

         // PolygonalMesh mesh = MeshFactory.createIcosahedron(1.0);
         // mesh = MeshFactory.subdivide(mesh);
         // FixedMeshBody mbody = new FixedMeshBody ("mesh", mesh);
         myMech.addRigidBody (body);
      }
      double rad = RenderableUtils.getRadius (this);
      RenderProps.setSphericalPoints (myMech, 0.01*rad, Color.GREEN);
      RenderProps.setEdgeColor (myMech, Color.WHITE);
      RenderProps.setDrawEdges (myMech, true);
   }

   public boolean getMenuItems(List<Object> items) {
      items.add (
         GuiUtils.createMenuItem (
            this, "quadricEdgeCollapse", "apply quadric edge collapse"));
      items.add (
         GuiUtils.createMenuItem (
            this, "sqrt3Subdivide", "apply sqrt(3) subdivision"));
      items.add (
         GuiUtils.createMenuItem (
            this, "mergeCoplanarFaces", "merge faces in the same plane"));
      return true;
   }   

   public void actionPerformed(ActionEvent event) {
      if (event.getActionCommand().equals ("quadricEdgeCollapse")) {
         for (RigidBody body : myMech.rigidBodies()) {
            PolygonalMesh mesh = body.getSurfaceMesh();
            if (mesh != null) {
               // Euler graph formula:
               int numEdges = mesh.numVertices()+mesh.numFaces()-2;
               MeshUtilities.quadricEdgeCollapse (mesh, numEdges/4);
               body.setSurfaceMesh (mesh);
            }
         }
      }
      else if (event.getActionCommand().equals ("sqrt3Subdivide")) {
         for (RigidBody body : myMech.rigidBodies()) {
            PolygonalMesh mesh = body.getSurfaceMesh();
            if (mesh != null) {
               MeshUtilities.sqrt3Subdivide (mesh, 1);
               body.setSurfaceMesh (mesh);
            }
         }
      }
      else if (event.getActionCommand().equals ("mergeCoplanarFaces")) {
         for (RigidBody body : myMech.rigidBodies()) {
            PolygonalMesh mesh = body.getSurfaceMesh();
            if (mesh != null) {
               mesh = mesh.clone();
               mesh.mergeCoplanarFaces (0.99);
               body.setSurfaceMesh (mesh);
            }
         }
      }
   } 


}



