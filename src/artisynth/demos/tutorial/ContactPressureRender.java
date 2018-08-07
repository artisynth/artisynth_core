package artisynth.demos.tutorial;

import java.awt.Color;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.CollisionBehavior;
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
import artisynth.core.mechmodels.CollisionManager;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.renderables.ColorBar;
import artisynth.core.util.ScalarRange;
import artisynth.core.workspace.RootModel;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderList;
import maspack.render.RenderProps;
import maspack.render.color.JetColorMap;

public class ContactPressureRender extends RootModel {

   double density = 1000;
   double EPS = 1e-10;

   // Convenience method for creating colors from [0-255] RGB values
   private static Color createColor (int r, int g, int b) {
      return new Color (r/255.0f, g/255.0f, b/255.0f);
   }         

   private static Color CREAM = createColor (255, 255, 200);
   private static Color BLUE_GRAY = createColor (153, 153, 255);

   // Creates and returns a ColorBar renderable object
   public ColorBar createColorBar() {
      ColorBar cbar = new ColorBar();
      cbar.setName("colorBar");
      cbar.setNumberFormat("%.2f");      // 2 decimal places
      cbar.populateLabels(0.0, 1.0, 10); // Start with range [0,1], 10 ticks
      cbar.setLocation(-100, 0.1, 20, 0.8);
      cbar.setTextColor (Color.WHITE);
      addRenderable(cbar);               // add to root model's renderables
      return cbar;
   }      

   public void build (String[] args) {
      MechModel mech = new MechModel ("mech");
      addModel (mech);
      
      // create FEM ball
      FemModel3d ball = new FemModel3d("ball");
      ball.setDensity (density);
      FemFactory.createIcosahedralSphere (ball, /*radius=*/0.1, /*ndivs=*/2, 1);
      ball.setMaterial (new LinearMaterial (100000, 0.4));
      mech.addModel (ball);

      // create FEM sheet
      FemModel3d sheet = new FemModel3d("sheet");
      sheet.setDensity (density);
      FemFactory.createHexGrid (
         sheet, /*wx*/0.5, /*wy*/0.3, /*wz*/0.05, /*nx*/20, /*ny*/10, /*nz*/1);
      sheet.transformGeometry (new RigidTransform3d (0, 0, -0.2));
      sheet.setMaterial (new LinearMaterial (500000, 0.4));
      sheet.setSurfaceRendering (SurfaceRender.Shaded);
      mech.addModel (sheet);

      // fix the side nodes of the surface
      for (FemNode3d n : sheet.getNodes()) {
         double x = n.getPosition().x;
         if (Math.abs(x-(-0.25)) <= EPS || Math.abs(x-(0.25)) <= EPS) {
            n.setDynamic (false);
         }
      }

      // create and set a collision behavior between the ball and surface.
      CollisionBehavior behav = new CollisionBehavior (true, 0);
      behav.setDrawColorMap (ColorMapType.CONTACT_PRESSURE); 
      behav.setColorMapCollidable (1); // show color map on collidable 1 (sheet);
      behav.setColorMapRange (new ScalarRange(0, 1.3));
      mech.setCollisionBehavior (ball, sheet, behav);

      CollisionManager cm = mech.getCollisionManager();
      // set rendering properties in the collision manager:
      RenderProps.setVisible (cm, true);    // enable collision rendering
      // create a custom color map for rendering the penetration depth
      JetColorMap map = new JetColorMap();
      map.setColorArray (
         new Color[] {
            CREAM,                       // no penetration
            createColor (255, 204, 153),
            createColor (255, 153, 102),
            createColor (255, 102, 51),
            createColor (255, 51, 0),
            createColor (204, 0, 0),     // most penetration
         });
      cm.setColorMap (map);

      // create a separate color bar to show color map pressure values
      ColorBar cbar = createColorBar();
      cbar.updateLabels(0, 1.3);
      cbar.setColorMap (map);

      // set color for all bodies
      RenderProps.setFaceColor (mech, CREAM);
      RenderProps.setLineColor (mech, BLUE_GRAY);
   }
}
