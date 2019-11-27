package artisynth.demos.wrapping;

import artisynth.core.mechmodels.RigidBody;
import artisynth.core.mechmodels.Particle;
import artisynth.core.mechmodels.Wrappable;
import artisynth.core.modelbase.StepAdjustment;
import artisynth.demos.wrapping.GeneralWrapTest.GeometryType;
import maspack.matrix.Point3d;
import maspack.matrix.RigidTransform3d;
import maspack.matrix.Vector3d;

/**
 * Model that tests MultiPointSpring wrapping in a dynamic setting,
 * with various objects interacting dynamically with a pair of
 * wrappable strands.
 * 
 * @author John E. Lloyd
 */
public class DynamicWrapTest extends TwoStrandWrapBase {

   private Vector3d myParticleVel = new Vector3d(-1, 0.5, 1);
   GeometryType myGeometryType = GeometryType.CYLINDER;
   boolean myUseGrid = false;

   protected int matchArg (String[] args, int i) {
      int nm = super.matchArg(args, i);
      if (nm == 0) {
         if (args[i].equals ("-geo")) {
            nm++;
            if (i == args.length-1) {
               System.out.println (
                  "WARNING: -geo needs another argument");
            }
            else {
               myGeometryType = GeometryType.valueOf (args[++i]);
               nm++;
            }
         }
         else if (args[i].equals ("-grid")) {
            nm++;
            myUseGrid = true;
         }
      }
      return nm;
   }

   public void build (String[] args) {
      parseArgs (args);

      super.build (args);

      if (!myGeometryType.isAnalytic()) {
         myUseGrid = true;
      }
      if (myGeometryType == GeometryType.SPHERE) {
         myDensity /= 10;
      }
      RigidBody body = GeneralWrapTest.createGeometry (
         myGeometryType, size, myDensity, myUseGrid);

      RigidTransform3d pose;
      switch (myGeometryType) {
         case CYLINDER: {
            pose = new RigidTransform3d (0, 0, 1.5*size, 0, 0, Math.PI/2);
            break;
         }
         case SPHERE: {
            pose = new RigidTransform3d (0, 0, 3*size, 0, 0, Math.PI/2);
            break;
         }
         case TALUS: {
            pose = new RigidTransform3d (0, -size, 1.5*size, 0, 0, Math.PI/2);
            break;
         }
         case PHALANX: {
            pose = new RigidTransform3d (
               0, 0, 2*size, Math.PI/2, Math.PI/4, 0);
            break;
         }
         case TORUS:
         case ELLIPSOID:
         default: {
            pose = new RigidTransform3d (0, 0, 1.5*size, 0, 0, 0);
            break;
         }            
      }
      if (myUseGrid) {
         body.setGridSurfaceRendering (false);
      }
      body.setPose (pose);

      myMech.addRigidBody (body);
      mySpring.addWrappable ((Wrappable)body);
      //addBreakPoint (0.30);
      //myMech.setProfiling (true);
      createControlPanel(myMech);

      //myMech.setProfiling(true);
   }

   public StepAdjustment advance (double t0, double t1, int flags) {
      double h = t1-t0;

      if (myP0 == null) {
         myP0 = (Particle)findComponent ("models/mechMod/particles/0");
      }

      if (myParticleVel != null) {
         Point3d pos = new Point3d();

         if (t0 > 0) {
            myP0.getPosition (pos);
            pos.scaledAdd (h, myParticleVel);
            myP0.setPosition (pos);
         }
      }
      StepAdjustment sa = super.advance (t0, t1, flags);
      return sa;
   }

}
