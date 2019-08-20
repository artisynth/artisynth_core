package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.materials.NeoHookeanMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.femmodels.*;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.RigidTransform3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;

/**
 * Interactive demo of a single square shell element of 4 shell nodes and 
 * 8 gauss points. Drag the nodes around.
 */
public class SingleShellQuad extends RootModel {
   
   protected final double myDensity = 1000;
   protected final double myParticleDamping = 10;              
   protected final double myNodeRadius = 0.05;

   public void build (String[] args) {

      boolean membrane = false;
      for (int i=0; i<args.length; i++) {
         if (args[i].equals ("-membrane")) {
            membrane = true;
         }
         else {
            System.out.println ("Warning: unknown model argument '"+args[i]+"'");
         }
      }

      FemModel3d fem = new FemModel3d();
      
      FemNode3d n0 = new FemNode3d (0, 0, 0);        
      FemNode3d n1 = new FemNode3d (1, 0, 0);      
      FemNode3d n2 = new FemNode3d (1, 1, 0);    
      FemNode3d n3 = new FemNode3d (0, 1, 0);        
      n0.setDynamic (false);
      n3.setDynamic (false);
     
      ShellQuadElement el = new ShellQuadElement(n0, n1, n2, n3, 0.01, membrane);
      fem.addNode (n0);
      fem.addNode (n1);
      fem.addNode (n2);
      fem.addNode (n3);
      fem.addShellElement (el);
      
      //fem.setMaterial (new NeoHookeanMaterial());
      fem.setMaterial (new LinearMaterial(100000.0, 0.33, true));
      fem.setGravity (0, 0, -9.8);
      fem.setDensity (myDensity);
      fem.setParticleDamping (myParticleDamping);

      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, Color.PINK);
      RenderProps.setShininess (fem, fem.getRenderProps().getShininess() * 10);
      RenderProps.setVisible (fem, true);
      RenderProps.setFaceStyle (fem, Renderer.FaceStyle.FRONT);
      RenderProps.setPointStyle (fem.getNodes(), Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (fem.getNodes(), myNodeRadius);

      MechModel mech = new MechModel ("mech");
      mech.addModel (fem);
      addModel (mech);
   }   
   
   
   /**
    * For each node, draw a white arrow between the rest position and current
    * position (i.e. displacement).
    * 
    * Also, draw a red arrow of the 3dof direction vector, starting from the
    * node current position. You'll notice that this red arrow is identical
    * to the white arrow.
    */
   public void render (Renderer renderer, int flags) {
      super.render (renderer, flags);
      
      FemModel3d fem = (FemModel3d)findComponent ("models/mech/models/0");
      for (FemNode3d n : fem.getNodes()) {
         Vector3d restPos = n.getRestPosition ();
         Vector3d curPos = n.getPosition ();
         
         renderer.setColor (Color.WHITE);
         renderer.drawArrow (restPos, curPos, 0.01, true);
         
         Vector3d curDir = new Vector3d(curPos);
         curDir.scaledAdd (20, n.getDirector());
         
         renderer.setColor (Color.RED);
         renderer.drawArrow (curPos, curDir, 0.01, true);
        
      }
   }
}
