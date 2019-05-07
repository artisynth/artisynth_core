package artisynth.demos.fem;

import maspack.matrix.*;
import maspack.util.*;
import maspack.geometry.*;
import maspack.render.*;

import artisynth.core.workspace.RootModel;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.util.*;

public class SignedDistanceCollide extends RootModel {
   
   private double mySize = 1;
   private double myDensity = 1000;
   private double myParticleDamping = 2.0;
   private double myStiffnessDamping = 0.002;   
   private double myYoungsModulus = 3000000;
   private double myPoissonsRatio = 0.33;

   private double myContactCompliance = 0.0000001;
   private double myContactDamping = 200000.0;

   private enum BodyType {
      DUMBBELL,
      SPHERE,
      FEM_ELLIPSOID
   };

   private FemModel3d createFem (String name) {
      FemModel3d fem = new FemModel3d (name);
      fem.setDensity (myDensity);
      fem.setParticleDamping (myParticleDamping);
      fem.setStiffnessDamping (myStiffnessDamping);
      fem.setLinearMaterial (myYoungsModulus, myPoissonsRatio, true);
      RenderProps.setPointStyle (fem, Renderer.PointStyle.SPHERE);
      RenderProps.setLineWidth (fem, 0);
      RenderProps.setPointRadius (fem, mySize / 30.0);
      RenderProps.setVisible (fem.getNodes(), false);
      return fem;
   }

   Collidable createTopBody (MechModel mech, BodyType type) {
      
      switch (type) {
         case SPHERE: {
            RigidBody body = RigidBody.createIcosahedralSphere (
               "top", mySize, myDensity, 1);
            body.setPose (
               new RigidTransform3d (0.05*mySize, 0, 3*mySize, 0.1, 0.2, 0.3));
            mech.addRigidBody (body);
            return body;
         }
         case DUMBBELL: {
            PolygonalMesh ball1 =
               MeshFactory.createIcosahedralSphere (0.75*mySize, 1);
            ball1.transform (new RigidTransform3d (1.5*mySize, 0, 0));
            PolygonalMesh ball2 =
               MeshFactory.createIcosahedralSphere (0.75*mySize, 1);
            ball2.transform (new RigidTransform3d (-1.5*mySize, 0, 0));
            PolygonalMesh axis =
               MeshFactory.createCylinder (0.2*mySize, 1.9*mySize, 12);
            axis.transform (new RigidTransform3d (0, 0, 0, 0, Math.PI/2, 0));
            
            RigidBody body = new RigidBody ("top");
            body.setDensity (10);
            body.addMesh (ball1);
            body.addMesh (ball2);
            body.addMesh (axis);
            body.setPose (new RigidTransform3d (
                             0.01*mySize, 0, 3*mySize, 0.4, 0.2, 0.1));
            mech.addRigidBody (body);
            return body;
         }
         case FEM_ELLIPSOID:{
            FemModel3d fem = createFem ("top");
            String path =
               ArtisynthPath.getHomeRelativePath (
                  "src/artisynth/core/femmodels/meshes", ".");
            try {
               TetGenReader.read (
                  fem, fem.getDensity(), 
                  path+"/sphere3.1.node", 
                  path+"/sphere3.1.ele",
                  new Vector3d (1.5 * mySize, mySize, mySize));
               //TetGenWriter.writeNodeFile (fem, path + "_new.1.node");
               //TetGenWriter.writeElemFile (fem, path + "_new.1.ele");
            }
            catch (Exception e) {
               throw new InternalErrorException (
                  "Can't create TetGen FEM from " + path);
            }
            fem.setSurfaceRendering (SurfaceRender.Shaded);
            fem.transformGeometry (
               new RigidTransform3d (0.05*mySize, 0, 3*mySize));
            mech.addModel (fem);
            return fem;
         }
         default: {
            throw new UnsupportedOperationException (
               "Unknown body type " + type);
         }
      }
   }

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      mech.setGravity (0, 0, -98.0);

      BodyType topType = BodyType.SPHERE;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-top")) {
            if (++i == args.length) {
               System.out.println (
                  "Warning: option '-top' expects a following argument");
               break;
            }
            try {
               topType = BodyType.valueOf (args[i]);
            }
            catch (Exception e) {
               System.out.println (
                  "Warning: unrecognized body type "+args[i]);
            }
         }
         else {
            System.out.println (
               "Warning: unrecognized model option "+args[i]);
         }
      }

      Collidable topBody = createTopBody (mech, topType);
      RigidBody bottomBody =
         RigidBody.createEllipsoid (
            "bottom", 4*mySize, 2*mySize, mySize, myDensity, 20);
      mech.addRigidBody (bottomBody);
      bottomBody.setDynamic (false);

      CollisionManager cm = mech.getCollisionManager();
      cm.setColliderType (ColliderType.SIGNED_DISTANCE);

      if (topBody instanceof RigidBody) {
         cm.setDamping (myContactDamping);
         cm.setCompliance (myContactCompliance);
      }
      mech.setCollisionBehavior (topBody, bottomBody, true);
   }

}



