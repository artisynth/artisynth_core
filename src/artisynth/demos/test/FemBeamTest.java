package artisynth.demos.test;

import java.awt.Color;
import java.io.IOException;

import artisynth.core.femmodels.FemFactory;
import artisynth.core.femmodels.FemModel.SurfaceRender;
import artisynth.core.femmodels.FemModel3d;
import artisynth.core.femmodels.FemNode3d;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.FemMaterial;
import artisynth.core.materials.LinearMaterial;
import artisynth.core.materials.MaterialBase;
import artisynth.core.mechmodels.MechModel;
import artisynth.core.modelbase.DynamicActivityChangeEvent;
import artisynth.core.modelbase.PropertyChangeEvent;
import artisynth.core.modelbase.PropertyChangeListener;
import artisynth.core.modelbase.RenderableComponentList;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;
import maspack.matrix.RigidTransform3d;
import maspack.properties.PropertyList;
import maspack.render.RenderProps;

public class FemBeamTest extends RootModel implements PropertyChangeListener {
   
   double widthX = 0.05;
   double widthY = 0.02;
   double widthZ = 0.02;
   
   int numX = 10;
   int numY = 4;
   int numZ = 4;
   
   MechModel mech;
   
   FemMaterial mat = null;
   RenderableComponentList<FemModel3d> fems;
   
   static PropertyList myProps = new PropertyList(FemBeamTest.class, RootModel.class);
   static {
      myProps.add("material", "fem material", createDefaultMaterial());
   }
   
   @Override
   public PropertyList getAllPropertyInfo() {
      return myProps;
   }
   
   private static FemMaterial createDefaultMaterial() {
      return new LinearMaterial(5000, 0.33, true);
   }
   
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      mech = new MechModel("mech");
      addModel(mech);
      
      fems = new RenderableComponentList<>(FemModel3d.class);
      
      FemModel3d tet = FemFactory.createTetGrid(null, widthX, widthY, widthZ, numX, numY, numZ);
      tet.setName("tet");
      fems.add(tet);
      FemModel3d pyr = FemFactory.createPyramidGrid(null, widthX, widthY, widthZ, numX, numY, numZ);
      pyr.setName("pyr");
      fems.add(pyr);
      FemModel3d wed = FemFactory.createWedgeGrid(null, widthX, widthY, widthZ, numX, numY, numZ);
      wed.setName("wed");
      fems.add(wed);
      FemModel3d hex = FemFactory.createHexGrid(null, widthX, widthY, widthZ, numX, numY, numZ);
      hex.setName("hex");
      fems.add(hex);
      
      FemModel3d qtet = FemFactory.createQuadtetGrid(null, widthX, widthY, widthZ, numX, numY, numZ);
      qtet.setName("qtet");
      fems.add(qtet);
      FemModel3d qpyr = FemFactory.createQuadpyramidGrid(null, widthX, widthY, widthZ, numX, numY, numZ);
      qpyr.setName("qpyr");
      fems.add(qpyr);
      FemModel3d qwed = FemFactory.createQuadwedgeGrid(null, widthX, widthY, widthZ, numX, numY, numZ);
      qwed.setName("qwed");
      fems.add(qwed);
      FemModel3d qhex = FemFactory.createQuadhexGrid(null, widthX, widthY, widthZ, numX, numY, numZ);
      qhex.setName("qhex");
      fems.add(qhex);
      
      // freeze lhs nodes
      double eps = 1e-10;
      for (FemModel3d fem : fems) {
         for (FemNode3d node : fem.getNodes()) {
            if (node.getRestPosition().x < -widthX/2+eps) {
               node.setDynamic(false);
            }
         }
      }
      
      // distribute
      double delta = widthY*0.25;
      RigidTransform3d translate = new RigidTransform3d();
      for (FemModel3d fem : fems) {
         fem.transformGeometry(translate);
         translate.p.y += widthY + delta;
      }
      
      mech.add(fems);
      
      // render properties
      int nfems = fems.size();
      int ifem = 0;
      for (FemModel3d fem : fems) {
         fem.setSurfaceRendering(SurfaceRender.Shaded);
         float h = (float)ifem/nfems;
         RenderProps.setFaceColor(fem, Color.getHSBColor(h, 1.0f, 1.0f));
         ++ifem;
      }
      
      setMaterial(createDefaultMaterial());
   }
   
   @Override
   public void attach(DriverInterface driver) {
      super.attach(driver);
      
      ControlPanel panel = new ControlPanel("material controls");
      panel.addWidget(this, "material");
      panel.pack();
      
      addControlPanel(panel);
   }
  
   
   public void setMaterial(FemMaterial mat) {
      
      this.mat = (FemMaterial)MaterialBase.updateMaterial (
         this, "material", this.mat, mat);
      componentChanged (DynamicActivityChangeEvent.defaultEvent);
      updateMaterials();
   }
   
   public FemMaterial getMaterial() {
      return mat;
   }
   
   private void updateMaterials() {
      for (FemModel3d fem : fems) {
         fem.setMaterial(mat);
      }
   }

   @Override
   public void propertyChanged(PropertyChangeEvent e) {
      if (e.getHost() == mat) {
         updateMaterials();
      }
   }

}
