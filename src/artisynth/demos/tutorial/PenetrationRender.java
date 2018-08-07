package artisynth.demos.tutorial;

import java.awt.Color;

import maspack.geometry.PolygonalMesh;
import maspack.geometry.MeshFactory;
import maspack.matrix.RigidTransform3d;
import maspack.render.*;
import maspack.render.Renderer.FaceStyle;
import maspack.render.Renderer.Shading;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.CollisionManager.ColliderType;
import artisynth.core.mechmodels.CollisionBehavior.ColorMapType;
import artisynth.core.util.ScalarRange;
import artisynth.core.workspace.RootModel;
import artisynth.core.renderables.ColorBar;
import maspack.render.color.JetColorMap;

public class PenetrationRender extends RootModel {

   // Convenience method for creating colors from [0-255] RGB values
   private static Color createColor (int r, int g, int b) {
      return new Color (r/255.0f, g/255.0f, b/255.0f);
   }         

   private static Color CREAM = createColor (255, 255, 200);
   private static Color GOLD = createColor (255, 150, 0);

   // Creates and returns a rigid body built from a hemispherical mesh.  The
   // body is centered at the origin, has a radius of 'rad', and the z axis is
   // scaled by 'zscale'.
   RigidBody createHemiBody (
      MechModel mech, String name, double rad, double zscale, boolean flipMesh) {

      PolygonalMesh mesh = MeshFactory.createHemisphere (
         rad, /*slices=*/20, /*levels=*/10);
      mesh.scale (1, 1, zscale); // scale mesh in the z direction
      if (flipMesh) {
         // flip upside down is requested
         mesh.transform (new RigidTransform3d (0, 0, 0, 0, 0, Math.PI));
      }
      RigidBody body = RigidBody.createFromMesh (
         name, mesh, /*density=*/1000, /*scale=*/1.0);
      mech.addRigidBody (body);
      body.setDynamic (false);  // body is only parametrically controlled
      return body;
   }

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

      // create first body and set its rendering properties
      RigidBody body0 = createHemiBody (mech, "body0", 2, -0.5, false);
      RenderProps.setFaceStyle (body0, FaceStyle.FRONT_AND_BACK);
      RenderProps.setFaceColor (body0, CREAM);
      
      // create second body and set its pose and rendering properties
      RigidBody body1 = createHemiBody (mech, "body1", 1, 2.0, true);
      body1.setPose (new RigidTransform3d (0, 0, 0.75));
      RenderProps.setFaceStyle (body1, FaceStyle.NONE); // set up 
      RenderProps.setShading (body1, Shading.NONE);     // wireframe
      RenderProps.setDrawEdges (body1, true);           // rendering
      RenderProps.setEdgeColor (body1, GOLD);

      // create and set a collision behavior between body0 and body1, and make
      // collisions INACTIVE since we only care about graphical display
      CollisionBehavior behav = new CollisionBehavior (true, 0);
      behav.setMethod (CollisionBehavior.Method.INACTIVE);

      behav.setDrawColorMap (ColorMapType.PENETRATION_DEPTH); 
      behav.setColorMapCollidable (1); // show penetration of mesh 0
      behav.getColorMapRange().setUpdating (
         ScalarRange.Updating.AUTO_FIT);
      mech.setCollisionBehavior (body0, body1, behav);

      CollisionManager cm = mech.getCollisionManager();
      // works better with open meshes if AJL_CONTOUR is selected
      cm.setColliderType (ColliderType.AJL_CONTOUR); 
      // set other rendering properities in the collision manager:
      RenderProps.setVisible (cm, true);    // enable collision rendering
      cm.setDrawIntersectionContours(true); // draw contours ...
      RenderProps.setEdgeWidth (cm, 3);     // with a line width of 3
      RenderProps.setEdgeColor (cm, Color.BLUE); // and a blue color
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

      // create a separate color bar to show depth values associated with the
      // color map
      ColorBar cbar = createColorBar();
      cbar.setColorMap (map);
   }

   public void prerender(RenderList list) {
      // In prerender, we update the color bar labels based on the updated
      // penetration range stored in the collision behavior.
      //
      // Object references are obtained by name using 'findComponent'. This is
      // more robust than using class member variables, since the latter will
      // be lost if we save and restore this model from a file.
      ColorBar cbar = (ColorBar)(renderables().get("colorBar"));
      MechModel mech = (MechModel)findComponent ("models/mech");
      RigidBody body0 = (RigidBody)mech.findComponent ("rigidBodies/body0");
      RigidBody body1 = (RigidBody)mech.findComponent ("rigidBodies/body1");

      CollisionBehavior behav = mech.getCollisionBehavior(body0, body1);
      ScalarRange range = behav.getPenetrationDepthRange();
      cbar.updateLabels(0, 1000*range.getUpperBound());
      super.prerender(list); // call the regular prerender method
   }
}
