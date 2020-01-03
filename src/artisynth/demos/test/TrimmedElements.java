package artisynth.demos.test;

import java.awt.Color;
import java.awt.Font;
import java.io.IOException;
import java.util.Arrays;

import artisynth.core.femmodels.FemElement3d;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.renderables.TextLabeller3d;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.render.RenderProps;
import maspack.render.Renderer.PointStyle;
import maspack.render.GL.GLViewer.BlendFactor;

public class TrimmedElements extends RootModel {

   @Override
   public void build (String[] args) throws IOException {
      super.build (args);
      
      addPyr ();
      addPyr2 ();
      addTet ();
      addPyrPyr ();
      addPyrTetTet ();
      addWdge ();
      
      addLabeller ();
      
   }
   
   FemModel3d createModel(String name, int[][] elems) {
      double a = 1;
      FemNode3d nodes[] = {
                           new FemNode3d(-a, -a, -a),
                           new FemNode3d( a, -a, -a),
                           new FemNode3d( a,  a, -a),
                           new FemNode3d(-a,  a, -a),
                           new FemNode3d(-a, -a,  a),
                           new FemNode3d( a, -a,  a),
                           new FemNode3d( a,  a,  a),
                           new FemNode3d(-a,  a,  a),
      };
      
      FemModel3d fem = new FemModel3d(name);
      fem.addNodes (Arrays.asList (nodes));
      
      char c = 'A';
      for (FemNode3d node : nodes) {
         node.setName (Character.toString ((char)(c + node.getNumber ())));
      }
      
      for (int[] e : elems) {
         FemNode3d[] enodes = new FemNode3d[e.length];
         for (int i=0; i<enodes.length; ++i) {
            enodes[i] = nodes[e[i]];
         }
         fem.addElement (FemElement3d.createElement (enodes));
      }
      
      setRenderProps (fem);
      
      return fem;
   }
   
   void addPyr() {
      int[][] elems = {{0, 1, 2, 3, 4},
                       {2, 6, 7, 3, 4},
                       {1, 5, 6, 2, 4},
      };
      FemModel3d fem = createModel ("pyr", elems);
      fem.getElement (1).setElementWidgetSize (0);
      fem.getElement (2).setElementWidgetSize (0);
      addRenderable (fem);
   }
   
   void addPyr2() {
      int[][] elems = {
                       {2, 5, 4, 3, 0},
                       {2, 5, 6, 3, 4, 7},
                       {0, 1, 2, 5}
      };
      FemModel3d fem = createModel ("pyr2", elems);
      fem.getElement (1).setElementWidgetSize (0);
      fem.getElement (2).setElementWidgetSize (0);
      addRenderable (fem);
   }
   
   void addTet() {
      int[][] elems = { 
                       {0, 1, 3, 4},
                       {1, 2, 3, 5, 6, 7},
                       {1, 5, 7, 3, 4},
      };
      FemModel3d fem = createModel ("tetwdgpyr", elems);
      fem.getElement (1).setElementWidgetSize (0);
      fem.getElement (2).setElementWidgetSize (0);
      addRenderable (fem);
   }
   
   void addPyrPyr() {
      int[][] elems = {
                       {0, 2, 6, 4, 5},
                       {0, 4, 6, 2, 3},
                       {3, 4, 6, 7},
                       {0, 2, 5, 1}
      };
      FemModel3d fem = createModel ("pyrpyr", elems);
      fem.getElement (2).setElementWidgetSize (0);
      fem.getElement (3).setElementWidgetSize (0);
      addRenderable (fem);
   }
   
   void addPyrTetTet() {
      int[][] elems = {
                       {0, 1, 2, 3, 5},
                       {0, 3, 7, 5},
                       {2, 7, 3, 5},
                       {0, 5, 7, 4},
                       {2, 7, 5, 6}
                       
      };
      FemModel3d fem = createModel ("pyrtettet", elems);
      fem.getElement (3).setElementWidgetSize (0);
      fem.getElement (4).setElementWidgetSize (0);
      addRenderable (fem);
   }
   
   void addWdge() {
      int[][] elems = {
                       {1, 5, 2, 0, 4, 3},
                       {2, 5, 6, 3, 4, 7}
      };
      FemModel3d fem = createModel ("wdg", elems);
      fem.getElement (1).setElementWidgetSize (0);
      addRenderable (fem);
   }
   
  
   void addLabeller() {
      FemModel3d fem = createModel ("dummy", new int[][] {{0,3,2,1,4,7,6,5}});
      fem.getElement (0).setElementWidgetSize (0);
      setRenderProps (fem);
      label (fem);
      RenderProps.setPointStyle (fem.getNodes (), PointStyle.SPHERE);
      RenderProps.setPointRadius (fem.getNodes (), 0.03);
      RenderProps.setPointColor (fem.getNodes (), new Color(100, 100, 255));
      addRenderable (fem);
   }
   
   void setRenderProps(FemModel3d fem) {
      //      RenderProps.setPointStyle (fem.getNodes (), PointStyle.SPHERE);
      //      RenderProps.setPointRadius (fem.getNodes (), 0.03);
      //      RenderProps.setPointColor (fem.getNodes (), new Color(100, 100, 255));
      RenderProps.setFaceColor (fem, new Color(200, 200, 255));
      RenderProps.setLineWidth (fem, 2);
      // RenderProps.setAlpha (fem.getElements (), 0.3);
      fem.setElementWidgetSize (0.95);
   }
   
   @Override
   public void attach (DriverInterface driver) {
      super.attach (driver);
      driver.getViewer ().setBackgroundColor (new float[] {1,1,1,0});
      driver.getViewer ().setBlendDestFactor (BlendFactor.GL_ONE_MINUS_SRC_ALPHA);
   }
   
   void label(FemModel3d fem) {
      TextLabeller3d labeller = new TextLabeller3d (fem.getName () + " labels");
      labeller.setTextSize (0.3);
      for (FemNode3d node : fem.getNodes ()) {
         String name = node.getName ();
         if (name == null) {
            name = Integer.toString (node.getNumber ());
         }
         labeller.addItem (name, node.getPosition (), true);
      }
      addRenderable (labeller);
      labeller.setTextOffset (0.03, 0.03);
      labeller.setTextColor (Color.BLACK);
      labeller.setFont (new Font(Font.SANS_SERIF, Font.BOLD, 64));
   }
   
}
