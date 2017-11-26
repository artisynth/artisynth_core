package artisynth.demos.fem;

import java.awt.Color;
import java.io.File;

import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.geometry.DistanceGrid;
import maspack.matrix.AffineTransform3d;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;
import maspack.matrix.Vector3i;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;
import maspack.render.Renderable;
import maspack.render.RenderableUtils;
import maspack.render.Renderer;
import maspack.util.InternalErrorException;
import artisynth.core.driver.Main;
import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.femmodels.TetGenReader;
import artisynth.core.femmodels.TetGenWriter;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.gui.ControlPanel;
import artisynth.core.mechmodels.Collidable;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.modelbase.ComponentList;
import artisynth.core.modelbase.ModelComponent;
import artisynth.core.modelbase.TransformableGeometry;
import artisynth.core.probes.NumericInputProbe;
import artisynth.core.probes.Probe;
import artisynth.core.util.ArtisynthPath;
import artisynth.core.util.TimeBase;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.demos.mech.LaymanDemo;

public class SimpleCollide extends RootModel {
   private static String meshPath =
      "src/artisynth/core/femmodels/meshes";

   private double mySize = 1.0;

   private double myTopScale = 1.0;

   private double myBottomScale = 1.2;

   private double mySeparation = 2.5;

   private double myFriction = 0.2;

   private double myDensity = 1000;

   private double myYoungsModulus = 100000;

   private double myPoissonsRatio = 0.33;

   private double myParticleDamping = 2.0;

   private double myStiffnessDamping = 0.002;

   private IncompMethod myIncompressible = IncompMethod.OFF;

   private Collidable myTopObject = null;

   private ObjectType myTopType = null;

   private Collidable myBottomObject = null;

   private ObjectType myBottomType = null;

   private enum ObjectType {
      FemEllipsoid, FemCube, FemSphere, Box, Molar, Bin, Paw, House
   }

   public static PropertyList myProps =
      new PropertyList (SimpleCollide.class, RootModel.class);

   static {
      myProps.add (
         "topObject", "type of the top object", ObjectType.FemEllipsoid);
      // ObjectType.Box);
      myProps.add ("bottomObject", "type of the bottom object", ObjectType.Box);
      myProps.add ("friction", "friction coefficient", 0);
   }

   public void setFriction (double mu) {
      MechModel mechMod = (MechModel)models().get (0);
      myFriction = mu;
      if (mechMod != null) {
         mechMod.setFriction (mu);
      }
   }

   public double getFriction() {
      return myFriction;
   }

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   private FemModel3d createFem (String name) {
      FemModel3d fem = new FemModel3d (name);
      fem.setDensity (myDensity);
      fem.setParticleDamping (myParticleDamping);
      fem.setStiffnessDamping (myStiffnessDamping);
//      fem.setPoissonsRatio (myPoissonsRatio);
//      fem.setYoungsModulus (myYoungsModulus);
      fem.setLinearMaterial (myYoungsModulus, myPoissonsRatio, true);
      RenderProps.setPointStyle (fem, Renderer.PointStyle.SPHERE);
      RenderProps.setLineWidth (fem, 0);
      RenderProps.setPointRadius (fem, mySize / 30.0);
      RenderProps.setVisible (fem.getNodes(), false);
      //RenderProps.setPointRadius (fem, 0);
      return fem;
   }

   Collidable createObject (String name, ObjectType type) {
      Collidable comp = null;

      switch (type) {
         case FemEllipsoid: {
            FemModel3d fem = createFem (name);
            String path =
               ArtisynthPath.getHomeRelativePath (meshPath, ".");
            fem.setIncompressible (myIncompressible);
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
            comp = fem;
            break;
         }
         case FemSphere: {
            FemModel3d fem = createFem (name);
            String path =
               ArtisynthPath.getHomeRelativePath (meshPath, ".");
            fem.setIncompressible (myIncompressible);
            try {
               TetGenReader.read (
                  fem, fem.getDensity(), 
                  path + "/sphere2.1.node",
                  path + "/sphere2.1.ele",
                  new Vector3d (mySize, mySize, mySize));
            }
            catch (Exception e) {
               throw new InternalErrorException (
                  "Can't create TetGen FEM from " + path);
            }
            fem.setSurfaceRendering (SurfaceRender.Shaded);
            comp = fem;
            break;
         }
         case FemCube: {
            FemModel3d fem = createFem (name);
            FemFactory.createHexGrid (
               fem, 3 * mySize, 2 * mySize, mySize, 1, 1, 1);
            fem.setIncompressible (myIncompressible);
            fem.setSurfaceRendering (SurfaceRender.Shaded);
            // for (FemNode3d n : fem.getNodes())
            // { if (n.getPosition().x < -3*mySize/2+1e-8)
            // { n.setDynamic(false);
            // }
            // }
            comp = fem;
            break;
         }
         case Box: {
            RigidBody body = new RigidBody (name);
            body.setMesh (MeshFactory.createBox (
               3 * mySize, 2 * mySize, mySize), null);
            body.setInertiaFromDensity (myDensity);
            // RenderProps.setAlpha (body, 0.3);
            comp = body;
            break;
         }
         case Molar: {
            RigidBody body = new RigidBody (name);
            PolygonalMesh mesh = null;
            try {
               mesh =
                  new PolygonalMesh (new File (
                     ArtisynthPath.getSrcRelativePath (
                        DistanceGrid.class, "sampleData/molar1.2.obj")));
               mesh.scale (2.0);
               mesh.triangulate();
            }
            catch (Exception e) {
               e.printStackTrace();
            }
            body.setMesh (mesh, null);
            body.setInertiaFromDensity (myDensity);
            //RenderProps.setAlpha (body, 0.3);
            comp = body;
            break;
         }
         case Bin: {
            RigidBody body = new RigidBody (name);
            PolygonalMesh mesh = null;
            try {
               mesh =
                  new PolygonalMesh (new File (
                     ArtisynthPath.getSrcRelativePath (
                        LaymanDemo.class, "geometry/taperedBin.obj")));
               mesh.scale (0.125);
               mesh.triangulate();   
            }
            catch (Exception e) {
               e.printStackTrace();
            }
            body.setMesh (mesh, null);
            body.setInertiaFromDensity (myDensity);
            //RenderProps.setAlpha (body, 0.3);
            comp = body;
            break;
         }
         case Paw: {
            RigidBody body = new RigidBody (name);
            PolygonalMesh mesh = null;
            try {
               mesh =
                  new PolygonalMesh (new File (
                     ArtisynthPath.getSrcRelativePath (
                        LaymanDemo.class, "geometry/hand.obj")));
               mesh.scale (0.1);
               mesh.triangulate();
            }
            catch (Exception e) {
               e.printStackTrace();
            }
            body.setMesh (mesh, null);
            body.setInertiaFromDensity (myDensity);
            body.transformGeometry (new RigidTransform3d (
               0, 0, 0, 1, 0, 0, Math.PI / 2));
            // RenderProps.setAlpha (body, 0.3);
            comp = body;
            break;
         }
         case House: {
            RigidBody body = new RigidBody (name);
            body.setMesh (MeshFactory.createPointedCylinder (
               mySize, mySize, mySize/4, 4), null);
            body.setInertiaFromDensity (myDensity);
            // RenderProps.setAlpha (body, 0.3);
            comp = body;
            break;
         }
         default: {
            throw new InternalErrorException ("Unimplemented type " + type);
         }
      }
      // RenderProps.setFaceStyle ((Renderable)comp, RenderProps.Faces.NONE);
      // RenderProps.setDrawEdges ((Renderable)comp, true);
      return comp;
   }

   public void setTopObject (ObjectType type) {
      MechModel mechMod = (MechModel)models().get (0);
      if (myTopType != type) {
         if (myTopObject != null) {
            if (myBottomObject != null) {
               mechMod.clearCollisionBehavior (myTopObject, myBottomObject);
            }
            removeObject (myTopObject, myTopType);
         }
         Collidable comp = createObject ("topObject", type);
         setTopObject (comp, type);
         addObject (comp, type);
         createAndAddControlPanel ("Top object", comp);
         if (myBottomObject != null) {
            mechMod.setCollisionBehavior (
               myTopObject, myBottomObject, true, myFriction);
         }
      }
   }

   public ObjectType getTopObject() {
      return myTopType;
   }

   public void setBottomObject (ObjectType type) {
      MechModel mechMod = (MechModel)models().get (0);
      if (myBottomType != type) {
         if (myBottomObject != null) {
            if (myTopObject != null) {
               mechMod.clearCollisionBehavior (myTopObject, myBottomObject);
            }
            removeObject (myBottomObject, myBottomType);            
         }
         Collidable comp = createObject ("bottomObject", type);
         setBottomObject (comp, type);
         addObject (comp, type);
         createAndAddControlPanel ("Bottom object", comp);
         if (myTopObject != null) {
            mechMod.setCollisionBehavior (
               myTopObject, myBottomObject, true, myFriction);
         }
         //addBreakPoint (0.33);
      }
   }

   ControlPanel createAndAddControlPanel (String name, ModelComponent comp) {
      ControlPanel panel = new ControlPanel (name);
      if (comp instanceof FemModel3d) {
         panel.addWidget (comp, "material");
         panel.addWidget (comp, "density");
         panel.addWidget (comp, "incompressible");
         panel.addWidget (comp, "volume");
      }
      else if (comp instanceof RigidBody) {
         panel.addWidget (comp, "position");
         panel.addWidget (comp, "orientation");
      }
      else {
         throw new InternalErrorException (
            "No control panel code for components of type " + comp.getClass());
      }
      ControlPanel existingPanel = getControlPanels().get (name);
      if (existingPanel != null) {
         int idx = getControlPanels().indexOf (existingPanel);
         removeControlPanel (existingPanel);
         existingPanel.dispose();
         addControlPanel (panel, idx);
         Main.getMain().arrangeControlPanels (this);
      }
      else {
         addControlPanel (panel);
      }
      return panel;
   }

   public ObjectType getBottomObject() {
      return myBottomType;
   }

   private void removeObject (ModelComponent comp, ObjectType type) {
      MechModel mechMod = (MechModel)models().get (0);
      ComponentList<Probe> myProbes = getInputProbes();
      for (Probe p: myProbes) {
         System.out.println ("type's name: " + type.name ());
         System.out.println ("probe's name: " + p.getName());
         if (type.name ().equals (p.getName())) {
            removeInputProbe (p);
         }
         
      }
      switch (type) {
         case FemEllipsoid:
         case FemSphere:
         case FemCube: {
            mechMod.removeModel ((FemModel3d)comp);
            break;
         }
         case Box:
         case Molar:
         case Bin:
         case Paw:
         case House: {
            mechMod.removeRigidBody ((RigidBody)comp);
            break;
         }
         default: {
            throw new InternalErrorException ("Unimplemented type " + type);
         }
      }
   }

   private void addObject (ModelComponent comp, ObjectType type) {
      MechModel mechMod = (MechModel)models().get (0);
      switch (type) {
         case FemEllipsoid:
         case FemSphere:
         case FemCube: {
            mechMod.addModel ((FemModel3d)comp);
            break;
         }
         case Box: {
            mechMod.addRigidBody ((RigidBody)comp);
            // NumericInputProbe iprobe =
            //    new NumericInputProbe (
            //       mechMod, "rigidBodies/bottomObject:position", 0, 6);
            // iprobe.setName ("Box");
            // iprobe.addData (
            //    new double[] { 0, 0, 0, 0, 1, 0, 0, 2, 2, 0, 0, 0, 3, 0, 0,
            //                   2, 4, 0, 0, 0,},
            // NumericInputProbe.EXPLICIT_TIME);
            // iprobe.setActive (true);
            // addInputProbe (iprobe);
            break;
         }
         case Molar: {
            mechMod.addRigidBody ((RigidBody)comp);
            break;
         }
         case Bin: {
            mechMod.addRigidBody ((RigidBody)comp);
            // NumericInputProbe iprobe =
            //    new NumericInputProbe (
            //       mechMod, "rigidBodies/bottomObject:position", 0, 6);
            // iprobe.setName ("Bin");
            // iprobe.addData (
            //    new double[] { 0, 0, 0, 0, 1, 0.5, 1, 0, 2, -0.25, -0.5, 0, 3,
            //                   0.5, 1, 0, 4, 0, 0, 0,},
            // NumericInputProbe.EXPLICIT_TIME);
            // iprobe.setActive (true);
            // addInputProbe (iprobe);
            break;
         }
         case Paw:
         case House: {
            mechMod.addRigidBody ((RigidBody)comp);
            break;
         }
         default: {
            throw new InternalErrorException ("Unimplemented type " + type);
         }
      }
   }

   private void setTopObject (Collidable comp, ObjectType type) {
      Renderable rcomp = (Renderable)comp;
      ((TransformableGeometry)comp).transformGeometry (new RigidTransform3d (0, /*-0.3*/
      0.0, mySeparation));
      ((TransformableGeometry)comp).transformGeometry (
         AffineTransform3d.createScaling (myTopScale));

      RenderProps.setFaceColor (rcomp, Color.red);
      switch (type) {
         case FemEllipsoid:
         case FemSphere:
         case FemCube: {
            RenderProps.setPointColor (rcomp, Color.gray);
            RenderProps.setAlpha (rcomp, 0.3);
            RenderProps.setDrawEdges (rcomp, true);
            RenderProps.setVisible (((FemModel3d)rcomp).getElements(), false);
            break;
         }
         case House: {
            ((TransformableGeometry)comp).transformGeometry (
               new RigidTransform3d (
                  0, 0, 2 * mySeparation, 1, 1, 0, Math.toRadians (170)));
            break;
         }
         case Box:
         case Molar:
         case Bin:
         case Paw: {
            break;
         }
         default: {
            throw new InternalErrorException ("Unimplemented type " + type);
         }
      }
      myTopObject = comp;
      myTopType = type;
   }

   private void setBottomObject (Collidable comp, ObjectType type) {
      Renderable rcomp = (Renderable)comp;
      ((TransformableGeometry)comp).transformGeometry (
         AffineTransform3d.createScaling (myBottomScale));
      switch (type) {
         case FemEllipsoid: 
         case FemSphere: {
            RenderProps.setPointColor (rcomp, Color.green);
            // fix the lower nodes
            Point3d min = new Point3d();
            RenderableUtils.getBounds (rcomp, min, null);
            FemModel3d fem = (FemModel3d)comp;
            fem.getSurfaceMesh();
            for (FemNode3d n : fem.getNodes()) {
               if (fem.isSurfaceNode (n)
               && n.getPosition().z <= (min.z + mySize * 0.5)) {
                  n.setDynamic (false);
               }
            }
            fem.resetRestPosition();
            break;
         }
         case FemCube: {
            RenderProps.setPointColor (rcomp, Color.green);
            // fix the lower nodes
            Point3d min = new Point3d();
            RenderableUtils.getBounds (rcomp, min, null);
            FemModel3d fem = (FemModel3d)comp;
            fem.getSurfaceMesh();
            for (FemNode3d n : fem.getNodes()) {
               if (fem.isSurfaceNode (n)
               && n.getPosition().z <= (min.z + mySize * 0.1)) {
                  n.setDynamic (false);
               }
            }
            // RenderProps.setAlpha (rcomp, 0.3);
            fem.resetRestPosition();
            break;
         }
         case Box: {
            ((RigidBody)comp).setDynamic (false);
            ((RigidBody)comp).setDistanceGridRes (new Vector3i(10,5,5));
            break;
         }
         case Molar: {
            ((RigidBody)comp).setPose (new RigidTransform3d (
               0, 0, 0, 0, -1, 0, Math.toRadians (172.09)));
            ((RigidBody)comp).setDynamic (false);
            break;
         }
         case Bin: {
            ((RigidBody)comp).setPose (new RigidTransform3d (
               0, 0, 0, 0, 0, 0, 0));
            ((RigidBody)comp).setDynamic (false);
            break;
         }
         case Paw:
         case House: {
            ((RigidBody)comp).setDynamic (false);
            break;
         }
         default: {
            throw new InternalErrorException ("Unimplemented type " + type);
         }
      }
      myBottomObject = comp;
      myBottomType = type;
   }

   public void build (String[] args) {

      MechModel mechMod = new MechModel ("mechModel");
      mechMod.setMaxStepSize (0.01);
      mechMod.setIntegrator (Integrator.ConstrainedBackwardEuler);
      addModel (mechMod);
      // for block/block friction test
      // mySeparation = 1.09;
      // mySeparation = .615;

      CollisionManager cm = mechMod.getCollisionManager();
      cm.setColliderType (ColliderType.SIGNED_DISTANCE);
      cm.setDrawIntersectionContours(true);
      RenderProps.setEdgeWidth (cm, 2);
      RenderProps.setEdgeColor (cm, Color.YELLOW);
      RenderProps.setVisible (cm, true);

      setTopObject (ObjectType.FemEllipsoid);
      setBottomObject (ObjectType.Box);

      // for block/block friction test
      //mechMod.transformGeometry (
      //  new RigidTransform3d (0, 0, 0, 0, 1, 0, Math.toRadians(20)));
      mechMod.setProfiling (false);

      ControlPanel panel = new ControlPanel ("controls");
      panel.addWidget (this, "topObject");
      panel.addWidget (this, "bottomObject");
      panel.addWidget (mechMod, "integrator");
      panel.addWidget (this, "friction");
      panel.addWidget (mechMod, "collisionManager:compliance");
      panel.addWidget (mechMod, "collisionManager:damping");
      addControlPanel (panel, 0);

      // mechMod.transformGeometry (
      // new RigidTransform3d (0, 0, 0, 0, 1, 0, Math.PI));

      Main.getMain().arrangeControlPanels (this);
      // panel.pack();
      // panel.setVisible (true);
      // java.awt.Point loc = Main.getMainFrame().getLocation();
      // panel.setLocation(loc.x + Main.getMainFrame().getWidth(), loc.y);
      // addControlPanel (panel);

     NumericInputProbe iprobe =
       new NumericInputProbe (
          mechMod, "rigidBodies/bottomObject:targetOrientation", 0, 6);
     //  new NumericInputProbe (
     //    mechMod, "rigidBodies/bottomObject:targetPosition", 0, 6);
         
     iprobe.addData (
        new double[] { 0, 0, 1, 0, 0, 1, 0, 1, 0, 0, 3, 0, 1, 0, 90, 5, 0, 1,
                       0, 180, 6, 0, 1, 0, 180, },
        NumericInputProbe.EXPLICIT_TIME);
//      iprobe.addData (
//      new double[] { 0, 0, 0, 0, 1, 0, 0, 2, 2, 0, 0, 0, 3, 0, 0,
//                    2, 4, 0, 0, 0,},
//      NumericInputProbe.EXPLICIT_TIME);
//      
//
      iprobe.setActive (true);
      addInputProbe (iprobe);
      addBreakPoint (1.34);
   }

   public void attach (DriverInterface driver) {
      super.attach (driver);
      MechModel mechMod = (MechModel)models().get (0);
      // mechMod.rigidBodies().get(0).setDynamic(false);

      // NumericInputProbe inprobe =
      // new NumericInputProbe(
      // mechMod, "rigidBodies/0:position", 0, 5);
      // inprobe.addData (2.5, new Point3d(0,0,0.8));
      // addInputProbe (inprobe);
   }

}
