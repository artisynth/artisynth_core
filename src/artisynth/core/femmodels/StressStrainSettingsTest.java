package artisynth.core.femmodels;

import java.io.*;
import java.util.*;
import artisynth.core.femmodels.*;
import artisynth.core.femmodels.FemModel.*;
import artisynth.core.mechmodels.*;
import artisynth.core.materials.*;
import maspack.util.*;
import maspack.properties.*;
import maspack.matrix.*;

/**
 * Tests the surfaceRendering, stressPlotRanging and stressPlotRange properties
 * for FemModel, FemMeshComp and FemCutPlane, making sure that they interact in
 * the correct way.
 */
public class StressStrainSettingsTest extends UnitTest {

   SurfaceRender STRAIN = SurfaceRender.Strain;
   SurfaceRender STRESS = SurfaceRender.Stress;
   SurfaceRender ENERGY_DENSITY = SurfaceRender.EnergyDensity;
   SurfaceRender SHADED = SurfaceRender.Shaded;

   private void printValues (
      FemModel3d fem, HashMap<String,Object> checkVals) {
      for (Map.Entry<String,Object> entry : checkVals.entrySet()) {
         String ppath = entry.getKey();
         Object value = entry.getValue();
         Property prop = fem.getProperty (ppath);
         System.out.print ("  " + ppath + ": " + prop.get());
         if (prop instanceof InheritableProperty) {
            InheritableProperty iprop = (InheritableProperty)prop;
            System.out.println ("  " + iprop.getMode());
         }
         else {
            System.out.println ("");
         }
      }
   }

   private void checkValues (
      FemModel3d fem, HashMap<String,Object> checkVals) {
      for (Map.Entry<String,Object> entry : checkVals.entrySet()) {
         String ppath = entry.getKey();
         Object value = entry.getValue();
         Property prop = fem.getProperty (ppath);
         if (prop == null) {
            throw new TestException ("Can't find property " + ppath);
         }
         if (value instanceof DoubleInterval) {
            DoubleInterval chk = (DoubleInterval)value;
            DoubleInterval rng = (DoubleInterval)prop.get();
            double eps = 1e-8*chk.getUpperBound();
            if (!rng.epsilonEquals (chk, eps)) {
               throw new TestException (
                  "Property "+ppath+": value is "+rng+", expecting "+chk);
            }
         }
         else if (value instanceof Double) {
            Double chk = (Double)value;
            Double val = (Double)prop.get();
            double eps = 1e-8*Math.max(1, Math.abs((chk+val)/2));
            if (Math.abs(val-chk) > eps) {
               throw new TestException (
                  "Property "+ppath+": value is "+val+", expecting "+chk);
            }
         }
         else {
            if (!prop.get().equals (value)) {
               throw new TestException (
                  "Property "+ppath+": value is "+prop.get()+
                  ", expecting "+value);
            }
         }
      }
   }

   /**
    * Create a testing model consisting of a FEM with two submeshes and two
    * cutplanes. This example is adapted from tutorials.FemSelfCollide.
    */
   public void test() {
      HashMap<String,Object> checkVals = new LinkedHashMap<>();

      MechModel mech = new MechModel ("mech");

      // create FEM model based on a partial (open) torus, with an
      // opening (gap) of angle of PI/4.
      FemModel3d fem = new FemModel3d("fem");
      FemFactory.createPartialHexTorus (
         fem, 0.15, 0.03, 0.06, 10, 20, 2, 7*Math.PI/4);
      // rotate the model so that the gap is at the botom
      fem.transformGeometry (
         new RigidTransform3d (0, 0, 0, 0, 3*Math.PI/8, 0));
      // set material and particle damping
      fem.setMaterial (new LinearMaterial (5e4, 0.45));
      fem.setParticleDamping (1.0);
      mech.addModel (fem);   

      // anchor the FEM by fixing the top center nodes
      for (FemNode3d n : fem.getNodes()) {
         if (Math.abs(n.getPosition().x) < 1e-15) {
            n.setDynamic(false);
         }
      }

      // Create and add meshes to the left and right tips of the FEM
      LinkedHashSet<FemElement3d> elems =
         new LinkedHashSet<>(); // elements for mesh bulding
      FemMeshComp leftMesh = new FemMeshComp (fem, "leftMesh");
      // elements near the left end have numbers in the range 180 - 199
      for (int n=180; n<200; n++) {
         elems.add (fem.getElementByNumber(n));
      }
      leftMesh.createVolumetricSurface (elems);
      fem.addMeshComp (leftMesh);

      FemMeshComp rightMesh = new FemMeshComp (fem, "rightMesh");
      elems.clear();
      // elements at the right end have numbers in the range 0 - 19
      for (int n=0; n<20; n++) {
         elems.add (fem.getElementByNumber(n));
      }
      rightMesh.createVolumetricSurface (elems);
      fem.addMeshComp (rightMesh);

      // Add a cut plane
      FemCutPlane cutplane = 
         new FemCutPlane (new RigidTransform3d (0,0,0, 0,0,Math.PI/2));
      fem.addCutPlane (cutplane);

      // advance the model 15 steps
      mech.initialize(0);
      double h = 0.01;
      for (int i=0; i<15; i++) {
         mech.preadvance (h*i, h*(i+1), 0);
         mech.advance (h*i, h*(i+1), 0);
      }

      DoubleInterval stressRng = new DoubleInterval(0, 9531.25);
      DoubleInterval strainRng = new DoubleInterval(0, 0.188093319);
      DoubleInterval energyRng = new DoubleInterval(0, 1036.88928);
      double strainEnergy = 1.68356720;

      // Turn on Stress rendering in the FEM. All meshes should exhibit Stress
      // rendering.
      fem.setSurfaceRendering (STRESS);

      checkVals.put("surfaceRendering", STRESS);
      checkVals.put("stressPlotRanging", Ranging.Auto);
      checkVals.put("stressPlotRange", stressRng);
      checkVals.put("strainEnergy", 0.0);
      checkVals.put("meshes/surface:surfaceRendering", STRESS);
      checkVals.put("meshes/surface:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/surface:stressPlotRange", stressRng);
      checkVals.put("meshes/rightMesh:surfaceRendering", STRESS);
      checkVals.put("meshes/rightMesh:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/rightMesh:stressPlotRange", stressRng);
      checkVals.put("meshes/leftMesh:surfaceRendering", STRESS);
      checkVals.put("meshes/leftMesh:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/leftMesh:stressPlotRange", stressRng);
      checkVals.put("cutPlanes/0:surfaceRendering", STRESS);
      checkVals.put("cutPlanes/0:stressPlotRanging", Ranging.Auto);
      checkVals.put("cutPlanes/0:stressPlotRange", stressRng);

      checkValues (fem, checkVals);

      // Turn on strain energy. Strain energy should appear.
      fem.setComputeStrainEnergy (true);
      checkVals.put("strainEnergy", strainEnergy);
      checkValues (fem, checkVals);

      // Turn off strain energy. Strain energy should go to 0.
      fem.setComputeStrainEnergy (false);
      checkVals.put("strainEnergy", 0.0);
      checkValues (fem, checkVals);

      // Change surface rendering for leftMesh to Strain. Only left mesh should
      // change.
      leftMesh.setSurfaceRendering (STRAIN);
      checkVals.put("meshes/leftMesh:surfaceRendering", STRAIN);
      checkVals.put("meshes/leftMesh:stressPlotRange", strainRng);
      checkValues (fem, checkVals);

      // Change FEM surface rendering to Energy. All meshes except
      // the left mesh should change.
      fem.setSurfaceRendering (ENERGY_DENSITY);
      checkVals.put("surfaceRendering", ENERGY_DENSITY);
      checkVals.put("stressPlotRange", energyRng);
      checkVals.put("meshes/surface:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("meshes/surface:stressPlotRange", energyRng);
      checkVals.put("meshes/rightMesh:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("meshes/rightMesh:stressPlotRange", energyRng);
      checkVals.put("cutPlanes/0:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("cutPlanes/0:stressPlotRange", energyRng);
      checkValues (fem, checkVals);

      // Change left mesh to Energy rendering.
      leftMesh.setSurfaceRendering (ENERGY_DENSITY);
      checkVals.put("meshes/leftMesh:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyRng);
      checkValues (fem, checkVals);

      // Change left mesh back to Stress rendering
      leftMesh.setSurfaceRendering (STRESS);
      checkVals.put("meshes/leftMesh:surfaceRendering", STRESS);
      checkVals.put("meshes/leftMesh:stressPlotRange", stressRng);
      checkValues (fem, checkVals);

      // change surface rendering range to Fixed
      DoubleInterval energyFixed = new DoubleInterval (0, 1000.0);
      fem.setStressPlotRanging (Ranging.Fixed);
      fem.setStressPlotRange (energyFixed);
      checkVals.put("stressPlotRanging", Ranging.Fixed);
      checkVals.put("stressPlotRange", energyFixed);
      checkVals.put("meshes/surface:stressPlotRanging", Ranging.Fixed);
      checkVals.put("meshes/surface:stressPlotRange", energyFixed);
      checkVals.put("meshes/rightMesh:stressPlotRanging", Ranging.Fixed);
      checkVals.put("meshes/rightMesh:stressPlotRange", energyFixed);
      checkVals.put("cutPlanes/0:stressPlotRanging", Ranging.Fixed);
      checkVals.put("cutPlanes/0:stressPlotRange", energyFixed);
      checkVals.put("meshes/leftMesh:stressPlotRanging", Ranging.Fixed);
      checkValues (fem, checkVals);

      // Change left mesh to Energy rendering.
      leftMesh.setSurfaceRendering (ENERGY_DENSITY);
      checkVals.put("meshes/leftMesh:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyFixed);
      checkValues (fem, checkVals);

      // Give left mesh an explict range
      DoubleInterval range_0_10 = new DoubleInterval(0,10);
      DoubleInterval range_0_0 = new DoubleInterval(0,0);
      leftMesh.setStressPlotRange (range_0_10);
      checkVals.put("meshes/leftMesh:stressPlotRange", range_0_10);
      checkValues (fem, checkVals);

      // Make left mesh range inherited again. Range should shift back
      leftMesh.setStressPlotRangeMode (PropertyMode.Inherited);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyFixed);
      checkValues (fem, checkVals);

      // Change left mesh to Stress rendering. Range should not change because
      // ranging is fixed and rendering is different from FEM.
      leftMesh.setSurfaceRendering (STRESS);
      checkVals.put("meshes/leftMesh:surfaceRendering", STRESS);
      checkValues (fem, checkVals);

      // Change left mesh to Auto ranging. Range should change.
      leftMesh.setStressPlotRanging (Ranging.Auto);
      checkVals.put("meshes/leftMesh:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/leftMesh:stressPlotRange", stressRng);
      checkValues (fem, checkVals);

      // Change left mesh rangingMode to Inherited. Ranging value
      // should flip back to fixed, but no range change because rendering
      // differs from the FEM
      leftMesh.setStressPlotRangingMode (PropertyMode.Inherited);
      checkVals.put("meshes/leftMesh:stressPlotRanging", Ranging.Fixed);
      checkValues (fem, checkVals);

      // Explicitly set range for left mesh
      leftMesh.setStressPlotRange (range_0_10);
      checkVals.put("meshes/leftMesh:stressPlotRange", range_0_10);
      checkValues (fem, checkVals);

      // Set left mesh stressPlotRangeMode to Inherited. Value
      // should reset to default since rendering differs from FEM
      leftMesh.setStressPlotRangeMode (PropertyMode.Inherited);
      //checkVals.put("meshes/leftMesh:stressPlotRange", range_0_0);
      checkValues (fem, checkVals);

      // Change left mesh to EnergyDensity rendering. Range will change
      // to energyFixed since it be inherited from FEM
      leftMesh.setSurfaceRendering (ENERGY_DENSITY);
      checkVals.put("meshes/leftMesh:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyFixed);

      // Change left mesh to Stress rendering, and then set it back again by
      // setting surfaceRenderingMode to Inherited.
      leftMesh.setSurfaceRendering (STRESS);
      checkVals.put("meshes/leftMesh:surfaceRendering", STRESS);
      checkValues (fem, checkVals);
      leftMesh.setStressPlotRange (range_0_10);
      checkVals.put("meshes/leftMesh:stressPlotRange", range_0_10);
      checkValues (fem, checkVals);
      leftMesh.setStressPlotRangeMode (PropertyMode.Inherited);
      //checkVals.put("meshes/leftMesh:stressPlotRange", range_0_0);
      checkValues (fem, checkVals);
      leftMesh.setSurfaceRenderingMode (PropertyMode.Inherited);
      checkVals.put("meshes/leftMesh:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyFixed);
      checkValues (fem, checkVals);

      // Change left mesh to Auto ranging. Range should change.
      leftMesh.setStressPlotRanging (Ranging.Auto);
      checkVals.put("meshes/leftMesh:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyRng);
      checkValues (fem, checkVals);

      // Change left mesh rangingMode back to Inherited. Ranging value should
      // flip back to fixed, and range will now be inherited from FEM
      leftMesh.setStressPlotRangingMode (PropertyMode.Inherited);
      checkVals.put("meshes/leftMesh:stressPlotRanging", Ranging.Fixed);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyFixed);
      checkValues (fem, checkVals);

      // Change range in FEM. All components should change
      DoubleInterval energyFixed2 = new DoubleInterval (0, 2000.0);
      fem.setStressPlotRange (energyFixed2);
      checkVals.put("stressPlotRange", energyFixed2);
      checkVals.put("meshes/surface:stressPlotRange", energyFixed2);
      checkVals.put("meshes/rightMesh:stressPlotRange", energyFixed2);
      checkVals.put("cutPlanes/0:stressPlotRange", energyFixed2);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyFixed2);
      checkValues (fem, checkVals);

      // Set Fixed ranging in left mesh and Auto in FEM. Ranges
      // should change everywhere but left mesh
      leftMesh.setStressPlotRanging (Ranging.Fixed);
      fem.setStressPlotRanging (Ranging.Auto);
      checkVals.put("stressPlotRange", energyRng);
      checkVals.put("stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/surface:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/surface:stressPlotRange", energyRng);
      checkVals.put("meshes/rightMesh:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/rightMesh:stressPlotRange", energyRng);
      checkVals.put("cutPlanes/0:stressPlotRanging", Ranging.Auto);
      checkVals.put("cutPlanes/0:stressPlotRange", energyRng);
      checkValues (fem, checkVals);

      // Explicitly set FEM surfaceRendering to None, which should not affect
      // anything else except the surfaceRendering for surface
      fem.setSurfaceRendering (SurfaceRender.None);
      checkVals.put("surfaceRendering", SurfaceRender.None);
      checkVals.put("meshes/surface:surfaceRendering", SurfaceRender.None);
      checkVals.put("meshes/rightMesh:surfaceRendering", SurfaceRender.None);
      checkVals.put("cutPlanes/0:surfaceRendering", SurfaceRender.None);
      checkVals.put("meshes/leftMesh:surfaceRendering", SurfaceRender.None);
      checkValues (fem, checkVals);
      // Set FEM surfaceRenderingMde to Inherited, which should set rendering
      // to None, which should not affect anything except causing
      // surfaceRendering for left and right meshes to revert to their
      // inherited values of Shaded
      fem.setSurfaceRenderingMode (PropertyMode.Inherited);
      checkVals.put("meshes/rightMesh:surfaceRendering", SurfaceRender.Shaded);
      checkVals.put("cutPlanes/0:surfaceRendering", SurfaceRender.Shaded);
      checkVals.put("meshes/leftMesh:surfaceRendering", SurfaceRender.Shaded);
      checkValues (fem, checkVals);

      // Set FEM rendering to Strain
      fem.setSurfaceRendering (SurfaceRender.Strain);
      checkVals.put("surfaceRendering", STRAIN);
      checkVals.put("stressPlotRange", strainRng);
      checkVals.put("meshes/surface:surfaceRendering", STRAIN);
      checkVals.put("meshes/surface:stressPlotRange", strainRng);
      checkVals.put("meshes/rightMesh:surfaceRendering", STRAIN);
      checkVals.put("meshes/rightMesh:stressPlotRange", strainRng);
      checkVals.put("cutPlanes/0:surfaceRendering", STRAIN);
      checkVals.put("cutPlanes/0:stressPlotRange", strainRng);
      checkVals.put("meshes/leftMesh:surfaceRendering", STRAIN);
      checkValues (fem, checkVals);

      // Set FEM stressPlotRange. Will only affect FEM because ranging is Auto
      fem.setStressPlotRange (range_0_10);
      checkVals.put("stressPlotRange", range_0_10);
      checkValues (fem, checkVals);

      // Set FEM ranging to Fixed. Range should now propagte to surface and
      // right meshes
      fem.setStressPlotRanging (Ranging.Fixed);
      checkVals.put("stressPlotRanging", Ranging.Fixed);
      checkVals.put("meshes/surface:stressPlotRanging", Ranging.Fixed);
      checkVals.put("meshes/surface:stressPlotRange", range_0_10);
      checkVals.put("meshes/rightMesh:stressPlotRanging", Ranging.Fixed);
      checkVals.put("meshes/rightMesh:stressPlotRange", range_0_10);
      checkVals.put("cutPlanes/0:stressPlotRanging", Ranging.Fixed);
      checkVals.put("cutPlanes/0:stressPlotRange", range_0_10);
      checkValues (fem, checkVals);

      // Set FEM ranging mode to inherited. This shouldn't cause anything to
      // change.
      fem.setStressPlotRangeMode (PropertyMode.Inherited);
      checkValues (fem, checkVals);

      // Reset FEM ranging to Auto by making it inherited. Ranging and range
      // values should revert.
      fem.setStressPlotRangingMode (PropertyMode.Inherited);
      checkVals.put("stressPlotRanging", Ranging.Auto);
      checkVals.put("stressPlotRange", strainRng);
      checkVals.put("meshes/surface:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/surface:stressPlotRange", strainRng);
      checkVals.put("meshes/rightMesh:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/rightMesh:stressPlotRange", strainRng);
      checkVals.put("cutPlanes/0:stressPlotRanging", Ranging.Auto);
      checkVals.put("cutPlanes/0:stressPlotRange", strainRng);
      checkValues (fem, checkVals);

      // Change surface rendering for cutplane to Stress. Only cutplane should
      // change.
      cutplane.setSurfaceRendering (STRESS);
      checkVals.put("cutPlanes/0:surfaceRendering", STRESS);
      checkVals.put("cutPlanes/0:stressPlotRange", stressRng);
      checkValues (fem, checkVals);

      // Change FEM surface rendering to Energy, and make left mesh ranging
      // inherited. All meshes except the cutplane should change. Left mesh
      fem.setSurfaceRendering (ENERGY_DENSITY);
      leftMesh.setStressPlotRangingMode (PropertyMode.Inherited);
      checkVals.put("surfaceRendering", ENERGY_DENSITY);
      checkVals.put("stressPlotRange", energyRng);
      checkVals.put("meshes/surface:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("meshes/surface:stressPlotRange", energyRng);
      checkVals.put("meshes/rightMesh:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("meshes/rightMesh:stressPlotRange", energyRng);
      checkVals.put("meshes/leftMesh:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyRng);
      checkVals.put("meshes/leftMesh:stressPlotRanging", Ranging.Auto);
      checkValues (fem, checkVals);

      // ******

      // Change cutplane to Energy rendering.
      cutplane.setSurfaceRendering (ENERGY_DENSITY);
      checkVals.put("cutPlanes/0:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("cutPlanes/0:stressPlotRange", energyRng);
      checkValues (fem, checkVals);

      // Change cutplane back to Stress rendering
      cutplane.setSurfaceRendering (STRESS);
      checkVals.put("cutPlanes/0:surfaceRendering", STRESS);
      checkVals.put("cutPlanes/0:stressPlotRange", stressRng);
      checkValues (fem, checkVals);

      // change surface rendering range to Fixed
      fem.setStressPlotRanging (Ranging.Fixed);
      fem.setStressPlotRange (energyFixed);
      checkVals.put("stressPlotRanging", Ranging.Fixed);
      checkVals.put("stressPlotRange", energyFixed);
      checkVals.put("meshes/surface:stressPlotRanging", Ranging.Fixed);
      checkVals.put("meshes/surface:stressPlotRange", energyFixed);
      checkVals.put("meshes/rightMesh:stressPlotRanging", Ranging.Fixed);
      checkVals.put("meshes/rightMesh:stressPlotRange", energyFixed);
      checkVals.put("meshes/leftMesh:stressPlotRanging", Ranging.Fixed);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyFixed);
      checkVals.put("cutPlanes/0:stressPlotRanging", Ranging.Fixed);
      checkValues (fem, checkVals);

      // Change cutplane to Energy rendering.
      cutplane.setSurfaceRendering (ENERGY_DENSITY);
      checkVals.put("cutPlanes/0:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("cutPlanes/0:stressPlotRange", energyFixed);
      checkValues (fem, checkVals);

      // Give cutplane an explict range
      cutplane.setStressPlotRange (range_0_10);
      checkVals.put("cutPlanes/0:stressPlotRange", range_0_10);
      checkValues (fem, checkVals);

      // Make cutplane range inherited again. Range should shift back
      cutplane.setStressPlotRangeMode (PropertyMode.Inherited);
      checkVals.put("cutPlanes/0:stressPlotRange", energyFixed);
      checkValues (fem, checkVals);

      // Change cutplane to Stress rendering. Range should not change because
      // ranging is fixed and rendering is different from FEM.
      cutplane.setSurfaceRendering (STRESS);
      checkVals.put("cutPlanes/0:surfaceRendering", STRESS);
      checkValues (fem, checkVals);

      // Change cutplane to Auto ranging. Range should change.
      cutplane.setStressPlotRanging (Ranging.Auto);
      checkVals.put("cutPlanes/0:stressPlotRanging", Ranging.Auto);
      checkVals.put("cutPlanes/0:stressPlotRange", stressRng);
      checkValues (fem, checkVals);

      // Change cutplane rangingMode to Inherited. Ranging value
      // should flip back to fixed, but no range change because rendering
      // differs from the FEM
      cutplane.setStressPlotRangingMode (PropertyMode.Inherited);
      checkVals.put("cutPlanes/0:stressPlotRanging", Ranging.Fixed);
      checkValues (fem, checkVals);

      // Explicitly set range for cutplane
      cutplane.setStressPlotRange (range_0_10);
      checkVals.put("cutPlanes/0:stressPlotRange", range_0_10);
      checkValues (fem, checkVals);

      // Set cutplane stressPlotRangeMode to Inherited. Value
      // should reset to default since rendering differs from FEM
      cutplane.setStressPlotRangeMode (PropertyMode.Inherited);
      //checkVals.put("cutPlanes/0:stressPlotRange", range_0_0);
      checkValues (fem, checkVals);

      // Change cutplane to EnergyDensity rendering. Range will change
      // to energyFixed since it be inherited from FEM
      cutplane.setSurfaceRendering (ENERGY_DENSITY);
      checkVals.put("cutPlanes/0:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("cutPlanes/0:stressPlotRange", energyFixed);

      // Change cutplane to Stress rendering, and then set it back again by
      // setting surfaceRenderingMode to Inherited.
      cutplane.setSurfaceRendering (STRESS);
      checkVals.put("cutPlanes/0:surfaceRendering", STRESS);
      checkValues (fem, checkVals);
      cutplane.setStressPlotRange (range_0_10);
      checkVals.put("cutPlanes/0:stressPlotRange", range_0_10);
      checkValues (fem, checkVals);
      cutplane.setStressPlotRangeMode (PropertyMode.Inherited);
      //checkVals.put("cutPlanes/0:stressPlotRange", range_0_0);
      checkValues (fem, checkVals);
      cutplane.setSurfaceRenderingMode (PropertyMode.Inherited);
      checkVals.put("cutPlanes/0:surfaceRendering", ENERGY_DENSITY);
      checkVals.put("cutPlanes/0:stressPlotRange", energyFixed);
      checkValues (fem, checkVals);

      // Change cutplane to Auto ranging. Range should change.
      cutplane.setStressPlotRanging (Ranging.Auto);
      checkVals.put("cutPlanes/0:stressPlotRanging", Ranging.Auto);
      checkVals.put("cutPlanes/0:stressPlotRange", energyRng);
      checkValues (fem, checkVals);

      // Change cutplane rangingMode back to Inherited. Ranging value should
      // flip back to fixed, and range will now be inherited from FEM
      cutplane.setStressPlotRangingMode (PropertyMode.Inherited);
      checkVals.put("cutPlanes/0:stressPlotRanging", Ranging.Fixed);
      checkVals.put("cutPlanes/0:stressPlotRange", energyFixed);
      checkValues (fem, checkVals);

      // Change range in FEM. All components should change
      fem.setStressPlotRange (energyFixed2);
      checkVals.put("stressPlotRange", energyFixed2);
      checkVals.put("meshes/surface:stressPlotRange", energyFixed2);
      checkVals.put("meshes/rightMesh:stressPlotRange", energyFixed2);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyFixed2);
      checkVals.put("cutPlanes/0:stressPlotRange", energyFixed2);
      checkValues (fem, checkVals);

      // Set Fixed ranging in cutplane and Auto in FEM. Ranges
      // should change everywhere but cutplane
      cutplane.setStressPlotRanging (Ranging.Fixed);
      fem.setStressPlotRanging (Ranging.Auto);
      checkVals.put("stressPlotRange", energyRng);
      checkVals.put("stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/surface:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/surface:stressPlotRange", energyRng);
      checkVals.put("meshes/rightMesh:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/rightMesh:stressPlotRange", energyRng);
      checkVals.put("meshes/leftMesh:stressPlotRanging", Ranging.Auto);
      checkVals.put("meshes/leftMesh:stressPlotRange", energyRng);
      checkValues (fem, checkVals);

      //printValues (fem, checkVals);
   }

   public static void main (String[] args) {
      RandomGenerator.setSeed (0x1234);
      StressStrainSettingsTest tester = new StressStrainSettingsTest();
      tester.runtest();
   }

}
