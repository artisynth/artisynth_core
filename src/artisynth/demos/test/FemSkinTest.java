package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.SkinMeshBody.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;

import maspack.util.*;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class FemSkinTest extends RootModel {

   public static boolean omitFromMenu = false;

   SkinMeshBody mySkin = null;
   RenderProps mySkinProps;

   public void build (String[] args) {
      boolean addHandle = false;
      boolean fixHalf = false;
      int nelems = 8;
      int nfaces = 10;
      FemConnectionType contype = FemConnectionType.ELEMENT;
      boolean useTwoFems = false;

      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-handle")) {
            addHandle = true;
         }
         else if (args[i].equals ("-dcon")) {
            contype = FemConnectionType.DISPLACEMENT;
         }
         else if (args[i].equals ("-twoFems")) {
            useTwoFems = true;
         }
         else if (args[i].equals ("-fixHalf")) {
            fixHalf = true;
         }
         else if (args[i].equals ("-nelems")) {
            if (i == args.length-1) {
               System.out.println (
                  "WARNING option -nelems needs an integer argument");
            }
            i++;
            nelems = Integer.valueOf (args[i]);
         }
         else if (args[i].equals ("-nfaces")) {
            if (i == args.length-1) {
               System.out.println (
                  "WARNING option -nfaces needs an integer argument");
            }
            i++;
            nfaces = Integer.valueOf (args[i]);
         }
         else {
            System.out.println ("WARNING: unrecognized option "+args[i]);
         }
      }

      MechModel mech = new MechModel ("mech");
      addModel (mech);
      mech.setGravity (0, 0, -9.8);

      double lenx = (useTwoFems ? 0.5 : 1.0);
      int nelemsx = (useTwoFems ? nelems/2 : nelems);
      FemModel3d fem0 = FemFactory.createHexGrid (
         null, lenx, 0.25, 0.25, nelemsx, nelems/2, nelems/2);
      fem0.setName ("fem0");
      fem0.setMaterial (new LinearMaterial (50000.0, 0.49));
      mech.addModel (fem0);

      FemModel3d fem1 = null;
      if (useTwoFems) {
         // create another fem and join it to fem0
         fem1 = FemFactory.createHexGrid (
            null, lenx, 0.25, 0.25, nelemsx, nelems/2, nelems/2);
         fem1.setName ("fem1");
         fem1.setMaterial (new LinearMaterial (50000.0, 0.49));
         mech.addModel (fem1);

         fem0.transformGeometry (new RigidTransform3d (-lenx/2, 0, 0));
         fem1.transformGeometry (new RigidTransform3d (lenx/2, 0, 0));

         for (FemNode3d n1 : fem1.getNodes()) {
            if (Math.abs(n1.getPosition().x) < 1e-8) {
               FemNode3d n0 = fem0.findNearestNode (n1.getPosition(), 1e-8);
               mech.attachPoint (n1, n0);
            }
         }
      }

      // fix leftmost nodes on fem0
      for (FemNode3d n : fem0.getNodes()) {
         if (Math.abs(n.getPosition().x+0.5) < 1e-8) {
            n.setDynamic (false);
         }
      }
      
      PolygonalMesh mesh = 
         MeshFactory.createRoundedCylinder (
            /*r=*/0.3, /*h=*/1.25, /*nslices=*/(int)(nfaces*1.3),
            /*nsegs=*/nfaces,
            /*flatbotton=*/false);
      // flip aout y axis
      mesh.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));

      mySkin = new SkinMeshBody ("skin", mesh);
      mySkin.setFemConnectionType (contype);
      mySkin.addMasterBody (fem0);
      if (fem1 != null) {
         mySkin.addMasterBody (fem1);
      }
      mySkin.computeAllVertexConnections();

      if (fixHalf) {
         for (Vertex3d vtx : mesh.getVertices()) {
            if (vtx.getPosition().y > 0) {
               mySkin.clearVertexConnections (vtx.getIndex());
            }
         }
      }

      mySkinProps = new RenderProps();
      mySkinProps.setPointRadius (0.005);
      mySkinProps.setPointColor (Color.CYAN);
      mySkinProps.setLineRadius (0.0025);
      mySkinProps.setLineColor (Color.GREEN);

      mySkin.addMarker (new Point3d (0.90216387, 0, 0.11480503));
      mech.addMeshBody (mySkin);

      if (addHandle) {
         FemModel3d fem = useTwoFems ? fem1 : fem0;
         PolygonalMesh cmesh = MeshFactory.createCylinder (0.05, 0.4, 10);
         cmesh.transform (
            new RigidTransform3d (0.2, 0, 0, 0, Math.PI/2, 0));
         RigidBody handle = RigidBody.createFromMesh (
            "handle", cmesh, 10.0, /*scale=*/1.0);
         handle.setPose (new RigidTransform3d (0.5, -0.04, 0.04));
         FemNode3d n19 = fem.getNodeByNumber(19);
         FemNode3d n24 = fem.getNodeByNumber(24);
         FemNode3d n39 = fem.getNodeByNumber(39);
         mech.addRigidBody (handle);
         // mech.attachPoint (n19, handle);
         // mech.attachPoint (n24, handle);
         // mech.attachPoint (n39, handle);
         mech.attachFrame (handle, fem);
      }
      
      RenderProps.setFaceStyle (mySkin, FaceStyle.NONE);
      RenderProps.setLineColor (mySkin, Color.CYAN);
      RenderProps.setDrawEdges (mySkin, true);
      RenderProps.setEdgeWidth (mySkin, 2);
      fem0.setSurfaceRendering (FemModel.SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem0, new Color (0.6f, 0.6f, 1f));
      RenderProps.setLineColor (fem0, new Color (0, 0, 1f));
      RenderProps.setLineWidth (fem0, 2);
      if (fem1 != null) {
         fem1.setSurfaceRendering (FemModel.SurfaceRender.Shaded);
         RenderProps.setFaceColor (fem1, new Color (0.6f, 1f, 0.6f));
         RenderProps.setLineColor (fem1, new Color (0, 0, 1f));
         RenderProps.setLineWidth (fem1, 2);
      }
      RenderProps.setSphericalPoints (mySkin, 0.01, Color.RED);

   }

   public void render (Renderer r, int flags) {
      super.render (r, flags);
   }


}
