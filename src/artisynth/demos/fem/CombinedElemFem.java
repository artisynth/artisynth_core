package artisynth.demos.fem;

import java.awt.Point;
import java.util.*;
import java.io.*;

import javax.swing.*;

import java.awt.event.*;

import javax.swing.event.*;

import maspack.geometry.*;
import maspack.spatialmotion.*;
import maspack.matrix.*;
import maspack.properties.PropertyList;
import maspack.render.*;
import maspack.render.Renderer;
import maspack.util.*;
import maspack.widgets.DoubleFieldSlider;
import maspack.interpolation.Interpolation;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel.IncompMethod;
import artisynth.core.femmodels.*;
import artisynth.core.materials.AxialMuscleMaterial;
import artisynth.core.materials.MooneyRivlinMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.MechSystemSolver.Integrator;
import artisynth.core.gui.*;
import artisynth.core.gui.selectionManager.SelectionEvent;
import artisynth.core.gui.selectionManager.SelectionListener;
import artisynth.core.modelbase.*;
import artisynth.core.probes.*;
import artisynth.core.util.*;
import artisynth.core.workspace.RootModel;
import maspack.render.*;
import artisynth.core.driver.*;

import java.awt.*;
import java.util.*;

public class CombinedElemFem extends RootModel {

   public void build (String[] args) {

      MechModel mech = new MechModel ("mech");
      addModel (mech);

      FemModel3d fem = new FemModel3d ("fem");
      FemFactory.createHexGrid (fem, 1.0, 1.0, 0.5, 2, 2, 1);

      FemModel3d xfem = FemFactory.createTetGrid (null, 1.0, 1.0, 0.5, 2, 2, 1);
      xfem.transformGeometry (new RigidTransform3d (0, 0, -0.5));
      FemFactory.addFem (fem, xfem);

      xfem = FemFactory.createWedgeGrid (null, 1.0, 1.0, 0.5, 2, 2, 1);
      xfem.transformGeometry (new RigidTransform3d (0, 0, -1.0));
      FemFactory.addFem (fem, xfem);      
      
      xfem = FemFactory.createPyramidGrid (null, 1.0, 1.0, 0.5, 2, 2, 1);
      xfem.transformGeometry (new RigidTransform3d (0, 0, -1.5));
      FemFactory.addFem (fem, xfem);      

      xfem = FemFactory.createShellTriGrid (null, 1.0, 1.0, 2, 2, 0.01, false);
      xfem.transformGeometry (new RigidTransform3d (0, 0, 0.25));
      FemFactory.addFem (fem, xfem);      

      xfem = FemFactory.createShellQuadGrid (null, 1.0, 1.0, 2, 2, 0.01, false);
      xfem.transformGeometry (new RigidTransform3d (0, 0, -1.75));
      FemFactory.addFem (fem, xfem);      

      setRenderProperties (fem, 1.0);

      fem.getNode(13).setDynamic (false);
      fem.setMaterial (new LinearMaterial (100000.0, 0.49));
      fem.setSurfaceRendering (SurfaceRender.None);
      fem.setElementWidgetSize (0.8);

      mech.addModel (fem);
   }

   public void setRenderProperties (FemModel3d mod, double length) {
      
      mod.setSurfaceRendering (SurfaceRender.Shaded);
      RenderProps.setShading (mod, Renderer.Shading.FLAT);
      RenderProps.setFaceColor (mod, new Color (0.7f, 0.7f, 0.9f));
      RenderProps.setLineWidth (mod.getElements(), 3);
      RenderProps.setLineColor (mod.getElements(), Color.blue);
      RenderProps.setPointRadius (mod, 0.02*length);
      RenderProps.setPointStyle (mod, Renderer.PointStyle.SPHERE);
      RenderProps.setPointColor (mod.getNodes(), Color.GREEN);
   }

}
