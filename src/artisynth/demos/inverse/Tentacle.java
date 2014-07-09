package artisynth.demos.inverse;

import java.awt.Color;
import java.io.File;
import java.io.IOException;

import maspack.geometry.PolygonalMesh;
import maspack.matrix.Point3d;
import maspack.matrix.Vector3d;
import maspack.render.RenderProps;
import maspack.render.TextureProps;
import maspack.render.TextureProps.Mode;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.modelbase.RenderableComponent;
import artisynth.core.util.ArtisynthPath;

public class Tentacle extends HydrostatInvDemo {

   public static boolean passive = false;
   public static final String dataDir = ArtisynthPath.getSrcRelativePath (Tentacle.class, "data/");
   
   public Tentacle () throws IOException {
   }

   public Tentacle (String name) throws IOException {
      super (name);
      
      hydro.setSurfaceRendering (SurfaceRender.None);
      //Vector3d bounds = getBounds(hydro);
      // double r = bounds.minElement ()/2;
      // double l = bounds.maxElement () - r;
      
//      PolygonalMesh tentacle = MeshFactory.createRoundedCylinder (r, l, 32);
//      RigidTransform3d X = new RigidTransform3d ();
//      X.R.setAxisAngle (0,1,0,Math.PI/2);
//      X.p.x = r/2;
//      tentacle.transform (X);
      String meshFilename = dataDir+"/tentacle_t.obj";
      PolygonalMesh tentacle = new PolygonalMesh (new File(meshFilename));
//      tentacle.write (new PrintWriter (new File("tentacle_quads.obj")), "%g");
      
      TextureProps tp = new TextureProps ();
      tp.setFileName (dataDir+"/tongue.jpg");
      tp.setMode (Mode.DECAL);
      tp.setEnabled (true);
      
      RenderProps rp = new RenderProps(hydro.getRenderProps ());
      rp.setFaceColor (new Color(255, 187, 187));
      rp.setDrawEdges (true);
      rp.setLineColor (Color.BLACK);
      rp.setTextureProps (tp);
      tentacle.setRenderProps (rp);
      
//      RigidBody tent = new RigidBody("tentacle");
//      tent.setMesh (tentacle, meshFilename);
//      tent.setDynamic (false);
//      tent.setRenderProps (rp);
//      mech.addRigidBody (tent);
      
      RenderProps.setVisible (hydro.getMuscleBundles (), false);
      
      hydro.addMesh (tentacle);
//      hydro.saveEmbeddedSurfaces ("tentacle");
      
      
//      if (passive) {
//         RenderProps.setVisible (hydro.getMuscleBundles (), false);
//         trackingController.setEnabled (false);
//         for (Particle p : mech.particles ()) {
//            if (p.getName ().endsWith ("_ref")) {
//               RenderProps.setVisible (p, false);
//               // make corresponding fem node visible
//            }
//         }
//      }
      
      
   }

   public static Vector3d getBounds(RenderableComponent comp) {
      Point3d pmin = new Point3d(Double.MAX_VALUE,Double.MAX_VALUE,Double.MAX_VALUE);
      Point3d pmax = new Point3d();
      pmax.negate (pmin);
      comp.updateBounds (pmin, pmax);
      pmax.sub (pmin); // bounds
      return pmax;
   }

}
