package artisynth.demos.tutorial;

import java.awt.*;

import maspack.matrix.*;
import maspack.util.PathFinder;
import maspack.render.RenderProps;
import maspack.geometry.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.SimpleAxialMuscle;
import artisynth.core.gui.ControlPanel;
import artisynth.core.workspace.RootModel;

public class TalusWrapping extends RootModel {

   private static Color BONE = new Color (1f, 1f, 0.8f);
   private static double DTOR = Math.PI/180.0;

   public void build (String[] args) {
      
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      // read in the talus bone mesh
      PolygonalMesh mesh = null;
      try {
         mesh = new PolygonalMesh (
            PathFinder.findSourceDir(this) + "/data/TalusBone.obj");
      }
      catch (Exception e) {
         System.out.println ("Error reading mesh:" + e);
      }
      // smooth the mesh using 20 iterations of regular Laplacian smoothing
      LaplacianSmoother.smooth (mesh, /*count=*/20, /*lambda=*/1, /*mu=*/0);
      // create the talus body from the mesh
      RigidBody talus = RigidBody.createFromMesh (
         "talus", mesh, /*density=*/1000, /*scale=*/1.0);
      mech.addRigidBody (talus);
      talus.setDynamic (false);
      RenderProps.setFaceColor (talus, BONE);
      
      // create start and end points for the spring
      Particle p0 = new Particle (/*mass=*/0, /*x,y,z=*/2, 0, 0);
      p0.setDynamic (false);
      mech.addParticle (p0);
      Particle p1 = new Particle (/*mass=*/0, /*x,y,z=*/-2, 0, 0);
      p1.setDynamic (false);
      mech.addParticle (p1);

      // create a wrappable spring using a SimpleAxialMuscle material
      MultiPointSpring spring = new MultiPointSpring ("spring");
      spring.setMaterial (
         new SimpleAxialMuscle (/*k=*/0.5, /*d=*/0, /*maxf=*/0.04));
      spring.addPoint (p0);
      // add an initial point to the wrappable segment to make sure it wraps
      // around the bone the right way
      spring.setSegmentWrappable (
         100, new Point3d[] { new Point3d (0.0, -1.0, 0.0) });            
      spring.addPoint (p1);
      spring.addWrappable (talus);
      spring.updateWrapSegments(); // update the wrapping path
      mech.addMultiPointSpring (spring);

      // set render properties
      DistanceGridComp gcomp = talus.getDistanceGridComp();
      RenderProps.setSphericalPoints (mech, 0.05, Color.BLUE); // points
      RenderProps.setLineWidth (gcomp, 0); // normal rendering off
      RenderProps.setCylindricalLines (spring, 0.03, Color.RED); // spring
      RenderProps.setSphericalPoints (spring, 0.05, Color.WHITE); // knots

      // create a control panel for interactive control
      ControlPanel panel = new ControlPanel();
      panel.addWidget (talus, "gridSurfaceRendering");
      panel.addWidget (gcomp, "resolution");
      panel.addWidget (gcomp, "maxResolution");
      panel.addWidget (gcomp, "renderGrid");
      panel.addWidget (gcomp, "renderRanges");
      panel.addWidget (spring, "drawKnots");
      panel.addWidget (spring, "wrapDamping");
      addControlPanel (panel);
   }
}
