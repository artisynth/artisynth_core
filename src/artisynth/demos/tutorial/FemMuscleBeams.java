package artisynth.demos.tutorial;

import java.awt.Color;
import java.io.IOException;

import javax.swing.JSeparator;

import maspack.matrix.*;
import maspack.render.RenderProps;
import maspack.render.Renderer;
import maspack.render.Renderer.LineStyle;
import maspack.widgets.LabeledComponentBase;
import artisynth.core.femmodels.*;
import artisynth.core.gui.ControlPanel;
import artisynth.core.materials.*;
import artisynth.core.mechmodels.*;
import artisynth.core.workspace.DriverInterface;
import artisynth.core.workspace.RootModel;

public class FemMuscleBeams extends RootModel {
   
   // Main build function
   @Override
   public void build(String[] args) throws IOException {
      super.build(args);
      
      // Mech model
      MechModel mech = new MechModel("mech");
      addModel(mech);
      mech.setGravity(0, 0, 0); // disable gravity
      
      // Muscle properties
      double length = 0.03;
      double width = 0.01;
      int [] res = {9, 3, 3};
      FemMaterial femMat = new MooneyRivlinMaterial(1000, 0, 0, 400, 0, 100000);
      MuscleMaterial muscleMat = new BlemkerMuscle(1.4, 1.0, 3000, 0, 0);
      // Need to rescale fibre material based on fibre length
      AxialMuscleMaterial muscleFibreMat = 
                  new BlemkerAxialMuscle(
                  1.4*length/res[0], 1.0*length/res[0], 
                  3000*length/res[0]*length/res[0], 0, 0);
      muscleFibreMat.setForceScaling(1.0);
      
      double density = 1000;
      double eps = 1e-6;        // for detecting left boundary nodes
      
      //-------------------------------------------------------------
      // FIBRE MUSCLE
      //-------------------------------------------------------------
      
      // Create basic muscle beam with spring-like fibres
      FemMuscleModel beamFibres = new FemMuscleModel("Fibre beam");
      FemFactory.createHexGrid(beamFibres, length, width, width, res[0], res[1], res[2]);
      beamFibres.setDensity(density);
      beamFibres.setMaterial(femMat);
      mech.addModel(beamFibres);
      
      // Left boundary condition
      for (FemNode3d node : beamFibres.getNodes()) {
         if (node.getPosition().x < -length/2 + eps) {
            node.setDynamic(false);
         }
      }
      
      // Add muscle bundles
      MuscleBundle topFibres = new MuscleBundle("top");
      double dy = width/res[1];
      for (int i=0; i<=res[1]; i++) {
         Point3d start = new Point3d(-length/2, -width/2+i*dy, width/2);
         Point3d stop = new Point3d(length/2, start.y, start.z);
         addFibreStrand(beamFibres, topFibres, muscleFibreMat, start, stop, res[0]);
      }
      // Enable use of fibres (default is disabled)
      topFibres.setFibresActive(true);
      beamFibres.addMuscleBundle(topFibres);
      
      MuscleBundle middleFibres = new MuscleBundle("middle");
      for (int i=0; i<=res[1]; i++) {
         Point3d start = new Point3d(-length/2, -width/2+i*dy, 0);
         Point3d stop = new Point3d(length/2, start.y, start.z);
         addFibreStrand(beamFibres, middleFibres, muscleFibreMat, start, stop, res[0]);
      }
      // Enable use of fibres (default is disabled)
      middleFibres.setFibresActive(true);
      beamFibres.addMuscleBundle(middleFibres);
      
      MuscleBundle bottomFibres = new MuscleBundle("bottom");
      for (int i=0; i<=res[1]; i++) {
         Point3d start = new Point3d(-length/2, -width/2+i*dy, -width/2);
         Point3d stop = new Point3d(length/2, start.y, start.z);
         addFibreStrand(beamFibres, bottomFibres, muscleFibreMat, start, stop, res[0]);
      }
      // Enable use of fibres (default is disabled)
      bottomFibres.setFibresActive(true);
      beamFibres.addMuscleBundle(bottomFibres);
      
      // Add an exciter that activates ALL bundles
      MuscleExciter allFibres = new MuscleExciter("all");
      allFibres.addTarget(topFibres, 1.0);
      allFibres.addTarget(middleFibres, 1.0);
      allFibres.addTarget(bottomFibres, 1.0);
      beamFibres.addMuscleExciter(allFibres);
      
      // Render properties
      RenderProps.setLineStyle(beamFibres.getMuscleBundles(), LineStyle.SPINDLE);
      RenderProps.setLineRadius(beamFibres.getMuscleBundles(), 0.0002);
      RenderProps.setLineColor(topFibres, Color.RED);
      RenderProps.setLineColor(middleFibres, Color.GREEN);
      RenderProps.setLineColor(bottomFibres, Color.BLUE);
      
      //-------------------------------------------------------------
      // EMBEDDED MATERIAL MUSCLE
      //-------------------------------------------------------------
      
      // Create basic muscle beam with embedded material
      FemMuscleModel beamMaterial = new FemMuscleModel("Material beam");
      FemFactory.createHexGrid(beamMaterial, length, width, width, res[0], res[1], res[2]);
      beamMaterial.setDensity(1000);
      beamMaterial.setMaterial(femMat);
      beamMaterial.setMuscleMaterial(muscleMat);        // embedded material
      // translate to sit beside other beam
      beamMaterial.transformGeometry(
         new RigidTransform3d(new Vector3d(0, 2*width, 0), AxisAngle.IDENTITY));
      mech.addModel(beamMaterial);
      
      // Left boundary condition
      for (FemNode3d node : beamMaterial.getNodes()) {
         if (node.getPosition().x < -length/2 + eps) {
            node.setDynamic(false);
         }
      }
      
      Point3d centroid = new Point3d();
      Vector3d dir = Vector3d.X_UNIT;
      
      // Add muscle bundles
      MuscleBundle topBundle = new MuscleBundle("top");
      MuscleBundle middleBundle = new MuscleBundle("middle");
      MuscleBundle bottomBundle = new MuscleBundle("bottom");
      
      for (FemElement3d elem : beamMaterial.getElements()) {
         elem.computeCentroid(centroid);
         if (centroid.z > eps) {
            topBundle.addElement(elem, dir);
         } else if (centroid.z < -eps) {
            bottomBundle.addElement(elem, dir);
         } else {
            middleBundle.addElement(elem, dir);
         }
      }
      
      beamMaterial.addMuscleBundle(topBundle);
      beamMaterial.addMuscleBundle(middleBundle);
      beamMaterial.addMuscleBundle(bottomBundle);
      
      // Add an exciter that activates ALL bundles
      MuscleExciter allBundles = new MuscleExciter("all");
      allBundles.addTarget(topBundle, 1.0);
      allBundles.addTarget(middleBundle, 1.0);
      allBundles.addTarget(bottomBundle, 1.0);
      beamMaterial.addMuscleExciter(allBundles);
      
      // Render properties
      beamMaterial.setDirectionRenderLen(0.5);  // draw muscle line 
      RenderProps.setLineStyle(beamMaterial.getMuscleBundles(), LineStyle.CYLINDER);
      RenderProps.setLineRadius(beamMaterial.getMuscleBundles(), 0.00015);
      RenderProps.setLineColor(topBundle, Color.RED);
      RenderProps.setLineColor(middleBundle, Color.GREEN);
      RenderProps.setLineColor(bottomBundle, Color.BLUE);

      addControlPanel(createControlPanel());
   }
   
   protected void addFibreStrand(FemMuscleModel fem, 
      MuscleBundle bundle, AxialMuscleMaterial fibreMat,
      Point3d start, Point3d stop, int numDivisions) {
      
      // Find nearest node to start of strand, 
      //   or create a FemMarker if not close enough
      double maxDist = 1e-4;
      Point pStart = fem.findNearestNode(start, maxDist);
      if (pStart == null) {
         pStart = fem.addMarker(start);
      }
      
      // Add fibres
      Point pPrev = pStart;
      double dt = 1.0/numDivisions;
      Point3d pnt = new Point3d();
      for (int i=1; i<= numDivisions; i++) {
         
         // Compute next position
         double t = i*dt;
         pnt.interpolate(start, t, stop);
         
         // Find nearest node or create FemMarker
         Point pNext = fem.findNearestNode(pnt, maxDist);
         if (pNext == null) {
            pNext = fem.addMarker(pnt);
         }
          
         // add a fibre between pPrev and pNext
         bundle.addFibre(pPrev, pNext, fibreMat);
         pPrev = pNext;
      }
    
   }
   

   protected ControlPanel createControlPanel() {
      MechModel mech = (MechModel)models().get("mech");
      FemMuscleModel beamFibres = (FemMuscleModel)(mech.models().get("Fibre beam"));
      FemMuscleModel beamMaterial = (FemMuscleModel)(mech.models().get("Material beam"));
      
      ControlPanel panel = new ControlPanel("Muscle controls");
      
      panel.addLabel("Fibre beam:");
      for (MuscleBundle mb : beamFibres.getMuscleBundles()) {
         LabeledComponentBase comp = panel.addWidget(mb.getName(), mb, "excitation");
         comp.setLabelFontColor(mb.getRenderProps().getLineColor());
      }
      for (MuscleExciter mc : beamFibres.getMuscleExciters()) {
         panel.addWidget(mc.getName(), mc, "excitation");
      }
      
      panel.addWidget(new JSeparator());
      panel.addLabel("Material beam:");
      for (MuscleBundle mb : beamMaterial.getMuscleBundles()) {
         LabeledComponentBase comp = panel.addWidget(mb.getName(), mb, "excitation");
         comp.setLabelFontColor(mb.getRenderProps().getLineColor());
      }
      for (MuscleExciter mc : beamMaterial.getMuscleExciters()) {
         panel.addWidget(mc.getName(), mc, "excitation");
      }
      
      return panel;
      
   }
}
