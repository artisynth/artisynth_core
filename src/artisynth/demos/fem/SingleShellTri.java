package artisynth.demos.fem;

import java.awt.Color;

import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemElement.ElementClass;
import artisynth.core.femmodels.*;
import artisynth.core.materials.NeoHookeanMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.*;
import artisynth.core.workspace.RootModel;
import maspack.matrix.Vector3d;
import maspack.matrix.VectorNd;
import maspack.matrix.Matrix3d;
import maspack.matrix.SVDecomposition3d;
import maspack.render.RenderProps;
import maspack.render.Renderer;

/**
 * Interactive demo of a single triangle shell element of 3 shell nodes and 
 * 9 gauss points. Drag the nodes around.
 */
public class SingleShellTri extends RootModel {
   
   protected final double myDensity = 10000;
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

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      FemModel3d fem = new FemModel3d();

      FemNode3d n0 = new FemNode3d (0, 0, 0);      
      FemNode3d n1 = new FemNode3d (1, 0, 0);       
      FemNode3d n2 = new FemNode3d (1, 1, 0);        
      
      n0.setDynamic (false);
      //n2.setDynamic (false);

      ShellTriElement triShell = new ShellTriElement (n0, n1, n2, 0.01, membrane);
      fem.addNode (n0);
      fem.addNode (n1);
      fem.addNode (n2);
      fem.addShellElement (triShell);

      //fem.setMaterial (new NeoHookeanMaterial());
      fem.setMaterial (new LinearMaterial());
      fem.setDensity (myDensity);
      fem.setParticleDamping (myParticleDamping);
      fem.setGravity (0,0,-9.8);
      
      fem.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setFaceColor (fem, Color.PINK);
      RenderProps.setShininess (
         fem, fem.getRenderProps().getShininess() * 10);
      RenderProps.setVisible (fem, true);
      RenderProps.setFaceStyle (fem, Renderer.FaceStyle.FRONT);
      RenderProps.setPointStyle (fem.getNodes(), Renderer.PointStyle.SPHERE);
      RenderProps.setPointRadius (fem.getNodes(), myNodeRadius);

      mech.addModel (fem);
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
         curDir.scaledAdd (20.0, n.getDirector());
         
         renderer.setColor (Color.RED);
         renderer.drawArrow (curPos, curDir, 0.01, true);
      }
   }
}
