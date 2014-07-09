package artisynth.demos.fem;

import java.awt.Color;
import java.awt.Point;

import javax.swing.*;

import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.widgets.DoubleFieldSlider;
import maspack.geometry.MeshFactory;
import maspack.geometry.PolygonalMesh;
import maspack.matrix.*;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.gui.*;
import artisynth.core.modelbase.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import artisynth.core.femmodels.*;

public abstract class SheetDemo extends RootModel {
   FemModel3d femMod;
   MechModel myMechMod;

   private double youngsModulus;

   private double poissonsRatio;

   private boolean warping;

   private boolean zCollision;

   // private boolean sphereCollision;
   private RigidBody myCollidingSphere;

   private RigidBody myCollidingDisc;

   public static PropertyList myProps =
      new PropertyList (SheetDemo.class, RootModel.class);

   public PropertyList getAllPropertyInfo() {
      return myProps;
   }

   public enum ElementType {
      Tet, Hex, Quadtet
   }
   
   public SheetDemo()
   {
      super(null);
   }

   public SheetDemo(String name)
   {
      super(name);
   }

   public SheetDemo (String name, ElementType type, int nx, int ny, int nz) {
      super (name);

      int nn, xSize, ySize, zSize;
      double xdim, ydim, zdim;

      nn = 1;
      xSize = nn * 6;
      ySize = nn * 6;

      zSize = nn * 2;
      xdim = 10.0;
      ydim = 10.0;
      zdim = 2.0;

      double planeZPos = -4.0;
      double sphereRadius = 3.0;
      double discRadius = 10.0;
      double discHeight = 1.0;
      Point3d spherePos = new Point3d (0.0, 0.0, -5.0);

      myMechMod = new MechModel();
      myMechMod.setIntegrator (MechSystemSolver.Integrator.BackwardEuler);
      myMechMod.setProfiling (false);

      switch (type) {
         case Tet: {
            femMod = new FemModel3d ("tetSheet");
            femMod.setDensity (1000);
            FemFactory.createTetGrid (femMod, xdim, ydim, zdim, nx, ny, nz);
            break;
         }
         case Hex: {
            femMod = new FemModel3d ("hexSheet");
            femMod.setDensity (1000);
            FemFactory.createHexGrid (femMod, xdim, ydim, zdim, nx, ny, nz);
            break;
         }
         case Quadtet: {
            femMod = new FemModel3d ("quadSheet");
            femMod.setDensity (1000);
            FemFactory.createQuadtetGrid (femMod, xdim, ydim, zdim, nx, ny, nz);
            break;
         }
      }
      myMechMod.addModel (femMod);

      RigidBody plane;
      RigidBody sphere;

      // double lenx0 = 20;
      // double leny0 = 20;
      // double lenz0 = 0.0007;
      // plane = new RigidBody("plane");
      //      
      // PolygonalMesh mesh = MeshFactory.createBox (lenx0, leny0, lenz0);
      // plane.setMesh (mesh, /*fileName=*/null);
      //   
      // plane.setDynamic (false);
      //      
      // myMechMod.addRigidBody(plane);
      //      
      // double pos[] = {0.1, 0.0, planeZPos, 0.0, 0.0, 0.1, 0.0};
      // plane.setPosState(pos,0);
      //      
      // RenderProps p = plane.getRenderProps();
      // p.setAlpha(0.2);

      addModel (myMechMod);

      sphere = new RigidBody ("sphere");
      sphere.setMesh (
         MeshFactory.createSphere (sphereRadius, 24), null);
      double pos[] = new double[] { 0.0, 0.0, spherePos.z, 0.0, 0.0, 0.1, 0.0 };
      sphere.setPosState (pos, 0);
      sphere.setDynamic (false);
      RenderProps.setFaceColor (sphere, new Color (50, 50, 255));
      RenderProps.setShading (sphere, RenderProps.Shading.GOURARD);

      RigidBody disc = new RigidBody ("disc");
      disc.setMesh (MeshFactory.createCylinder (
                       discRadius, discHeight, 24), null);
      RigidTransform3d X = new RigidTransform3d();
      X.p.set (0, 0, spherePos.z - discHeight / 2);
      disc.setPose (X);
      disc.setDynamic (false);
      RenderProps.setFaceColor (disc, new Color (50, 50, 255));
      // RenderProps.setShading (disc, RenderProps.Shading.GOURARD);

      myMechMod.addRigidBody (sphere);
      myMechMod.addRigidBody (disc);

      myMechMod.setCollisionBehavior (femMod, sphere, true);
      myMechMod.setCollisionBehavior (femMod, disc, true);
//       setSphereCollision (sphere);
//       setDiscCollision (disc);

      // myMechMod.setCollisions (sphere, quadMod, true);

      setRenderProps(type);
   }

   ControlPanel myControlPanel;

   @Override
      public void attach (DriverInterface driver) {
      JFrame frame = driver.getFrame();

      if (getControlPanels().size() == 0) {
         myControlPanel = new ControlPanel ("options", "");
         myControlPanel.addWidget (femMod, "material");
         myControlPanel.addWidget (femMod, "incompressible");
         myControlPanel.addWidget (myMechMod, "integrator");
         myControlPanel.addWidget (myMechMod, "maxStepSize");

         // myControlPanel.addWidget(mod, "profile");

         myControlPanel.pack();
         myControlPanel.setVisible (true);
         Point loc = frame.getLocation();
         myControlPanel.setLocation (loc.x + frame.getWidth(), loc.y);
         addControlPanel (myControlPanel);
      }
   }

   @Override
      public void detach (DriverInterface driver) {
      super.detach (driver);
   }

   private void setRenderProps(ElementType type) {
      // tetMod.setSurfaceRendering (SurfaceRender.Shaded);
      // hexMod.setSurfaceRendering (SurfaceRender.Shaded);
      // quadMod.setSurfaceRendering (SurfaceRender.Shaded);

      switch (type) {
         case Tet: {
            femMod.setSurfaceRendering (SurfaceRender.Shaded);
            RenderProps.setLineWidth (femMod, 2);
            RenderProps.setLineColor (femMod, Color.blue);
            RenderProps.setDrawEdges (femMod, true);
            RenderProps.setFaceColor (femMod, new Color (153, 153, 255));
            RenderProps.setShading (femMod, RenderProps.Shading.GOURARD);
            break;
         }
         case Hex: {
            femMod.setSurfaceRendering (SurfaceRender.Shaded);
            RenderProps.setLineWidth (femMod, 2);
            RenderProps.setLineColor (femMod, Color.green);
            RenderProps.setFaceColor (femMod, new Color (153, 153, 255));
            RenderProps.setShading (femMod, RenderProps.Shading.GOURARD);
            break;
         }
         case Quadtet: {
            femMod.setSurfaceRendering (SurfaceRender.Shaded);
            RenderProps.setLineWidth (femMod, 2);
            RenderProps.setLineColor (femMod, Color.red);
            RenderProps.setFaceColor (femMod, new Color (153, 153, 255));
            RenderProps.setShading (femMod, RenderProps.Shading.GOURARD);
            break;
         }
      }
      
      // RenderProps.setPointStyle (quadMod, RenderProps.PointStyle.SPHERE);
      // RenderProps.setPointColor (quadMod, Color.green);
      // RenderProps.setPointRadius (quadMod, 0.1);

      // RenderProps pprops = tetMod.getNodes().getRenderProps();
      // pprops.setPointStyle (RenderProps.PointStyle.SPHERE);
      // pprops.setPointRadius (0.02);
      // pprops.setPointColor (Color.red);
      //      
      // pprops = hexMod.getNodes().getRenderProps();
      // pprops.setPointStyle (RenderProps.PointStyle.SPHERE);
      // pprops.setPointRadius (0.02);
      // pprops.setPointColor (Color.red);
      //      
      // pprops = quadMod.getNodes().getRenderProps();
      // pprops.setPointStyle (RenderProps.PointStyle.SPHERE);
      // pprops.setPointRadius (0.02);
      // pprops.setPointColor (Color.red);
   }

}
