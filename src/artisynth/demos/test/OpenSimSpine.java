package artisynth.demos.test;

import java.awt.Color;
import java.io.*;
import java.util.*;

import artisynth.core.workspace.*;
import artisynth.core.mechmodels.*;
import artisynth.core.mechmodels.JointBase;
import artisynth.core.modelbase.*;
import artisynth.core.femmodels.*;
import artisynth.core.materials.*;
import artisynth.core.probes.*;
import artisynth.core.opensim.OpenSimParser;
import artisynth.core.opensim.customjoint.OpenSimCustomJoint;

import maspack.util.*;
import maspack.fileutil.*;
import maspack.fileutil.uri.URIx;
import maspack.matrix.*;
import maspack.geometry.*;
import maspack.render.*;
import maspack.render.Renderer.*;
import maspack.properties.*;

public class OpenSimSpine extends RootModel {

   public static boolean omitFromMenu = false;

   String data_url =
      "https://www.artisynth.org/files/data/Female_Thoracolumbar_Spine_V1.zip";

   private static class VertebraCentre {
      RigidBody rb;
      Point3d loc;
      String name;

      public VertebraCentre(RigidBody rb, double x, double y, double z, String name) {
         this.rb = rb;
         this.loc = new Point3d (x, y, z);
         this.name = name;
      }
   }

   public void build (String[] args) throws IOException {
      
      MechModel mech = new MechModel ("mech");
      addModel (mech);

      String localPath = PathFinder.findSourceDir(OpenSimSpine.class);
      String dataPath = localPath+"/Female_Thoracolumbar_Spine_V1";
      if (!(new File(dataPath)).exists()) {
         System.out.println ("Downloading "+data_url+" ...");
         try {
            ZipUtility.unzip (new URIx(data_url), new File(localPath));
         }
         catch (Exception e) {
            e.printStackTrace(); 
            throw e;
         }
      }

      File osimFile =
         new File (dataPath+"/Female_Thoracolumbar_Spine_Model.osim");
      String geometryPath = dataPath + "/Geometry/";
      OpenSimParser parser = new OpenSimParser (osimFile);

      parser.setGeometryPath (new File(geometryPath));

      // create model
      parser.createModel (mech);


      // don't allow ground to move
      @SuppressWarnings("unchecked")
      ComponentList<RigidBody> bodies =
         (ComponentList<RigidBody>)mech.get("bodyset");
      {
         // remove gnd-sacrum joint
         RigidBody sacrum = bodies.get ("sacrum");
         @SuppressWarnings("unchecked")
         RenderableComponentList<JointBase> joint =
            (RenderableComponentList<JointBase>)sacrum.get("joint");
         sacrum.remove(joint);

         // remove ground altogether
         RigidBody ground = bodies.get ("ground");
         if (ground != null) {
            bodies.remove(ground);
         }
      }

      // replace spine joints with framesprings
      ArrayList<FrameSpring> toAdd = new ArrayList<>();
      for (RigidBody rb : bodies) {

         ArrayList<JointBase> toRemove = new ArrayList<>();

         @SuppressWarnings("unchecked")
         // RenderableComponentList<RenderableComponentList<JointBase>> joints =
         //    (RenderableComponentList<RenderableComponentList<JointBase>>)rb.get("joint");
         // if (joints != null) {
         //    for (RenderableComponentList<JointBase> list : joints) {
         //       for (JointBase bc : list) {
         //          if (bc instanceof OpenSimCustomJoint) {
         //             OpenSimCustomJoint j = (OpenSimCustomJoint)bc;
         //             toRemove.add (j);
         //             FrameSpring fs = new FrameSpring (j.getName ());

         //             FrameFrameAttachment fa =
         //                (FrameFrameAttachment)j.getFrameAttachmentA ();
         //             FrameFrameAttachment fb =
         //                (FrameFrameAttachment)j.getFrameAttachmentB ();

         //             fs.setFrames (
         //                fa.getMaster(), fa.getTFM(), fb.getMaster(), fb.getTFM());
         //             fs.setMaterial (
         //                new LinearFrameMaterial (100000, 1000, 0, 0));
         //             fs.setName (j.getName ());
         //             toAdd.add (fs);
         //          }
         //       }
         //    }
         RenderableComponentList<JointBase> joints =
            (RenderableComponentList<JointBase>)rb.get("joint");
         if (joints != null) {
            for (JointBase bc : joints) {
               if (bc instanceof OpenSimCustomJoint) {
                  OpenSimCustomJoint j = (OpenSimCustomJoint)bc;
                  toRemove.add (j);
                  FrameSpring fs = new FrameSpring (j.getName ());

                  FrameFrameAttachment fa =
                     (FrameFrameAttachment)j.getFrameAttachmentA ();
                  FrameFrameAttachment fb =
                     (FrameFrameAttachment)j.getFrameAttachmentB ();

                  fs.setFrames (
                     fa.getMaster(), fa.getTFM(), fb.getMaster(), fb.getTFM());
                  fs.setMaterial (
                     new LinearFrameMaterial (100000, 1000, 0, 0));
                  fs.setName (j.getName ());
                  toAdd.add (fs);
               }
            }
            for (JointBase joint : toRemove) {
               joints.remove (joint);
            }
         }
      }
      for (FrameSpring fs : toAdd) {
         mech.addFrameSpring (fs);
      }

      // remove gravity
      mech.setGravity (Vector3d.ZERO);

      // hide wrap bodies
      for (RigidBody rb : bodies) {
         if (rb.getName ().contains ("ylinder")) {
            RenderProps.setVisible (rb, false);
         } else if (rb.getName ().endsWith ("rap")) {
            RenderProps.setVisible (rb, false);
         }
      }

      @SuppressWarnings("unchecked")
      RenderableComponentList<ModelComponent> forceset =
         (RenderableComponentList<ModelComponent>)mech.get ("forceset");
      RenderProps.setLineColor (forceset, Color.RED.darker ());
      RenderProps.setLineColor (forceset, Color.RED.darker ().darker ());
      RenderProps.setShading (forceset, Shading.SMOOTH);

      for (RigidBody rb : bodies) {
         for (RigidMeshComp rmc : rb.getMeshComps ()) {
            RenderProps.setAlphaMode (rmc, PropertyMode.Inherited);
            RenderProps.setAlphaMode (rmc, PropertyMode.Inherited);
         }
      }

      for (ModelComponent force : forceset) {
         RenderProps.setAlphaMode (
            (RenderableComponent)force, PropertyMode.Inherited);
      }

      RenderProps.setLineStyle (forceset, LineStyle.SPINDLE);
      RenderProps.setLineRadius (forceset, 0.0015);

      addVertabraBodyCentres (mech);

      mech.setGravity (new Vector3d (0, -9.8, 0)); // y-up
      mech.setFrameDamping (0.01);
      mech.setRotaryDamping (0.2);

      // fix sacrum
      RigidBody sac = bodies.get ("sacrum");
      sac.setDynamic (false);

      setDefaultViewOrientation (AxisAngle.IDENTITY);
   }

   protected static void addVertabraBodyCentres(MechModel mech) {
      PointList<FrameMarker> vbcenters =
         new PointList<> (FrameMarker.class, "vbcenters");
      mech.add (vbcenters);

      @SuppressWarnings("unchecked")
      RenderableComponentList<RigidBody> bodies =
         (RenderableComponentList<RigidBody>)mech.get ("bodyset"); 

      VertebraCentre[] centres = {
         new VertebraCentre (bodies.get("head_neck"), 0.010845179+0.0130444, 0.68081092-0.572635, 0, "C1"),
         new VertebraCentre (bodies.get("head_neck"), 0.0099211354+0.0130444, 0.66669745-0.572635, 0.0, "C2"),
         new VertebraCentre (bodies.get("head_neck"), 0.0113211+0.0130444, 0.64869585-0.572635, 0, "C3"),
         new VertebraCentre (bodies.get("head_neck"), 0.0098136677+0.0130444, 0.6317569-0.572635, 0, "C4"),
         new VertebraCentre (bodies.get("head_neck"), 0.0063905003+0.0130444, 0.61240175-0.572635, 0, "C5"),
         new VertebraCentre (bodies.get("head_neck"), 0.0020847734+0.0130444, 0.59335006-0.572635, 0, "C6"),
         new VertebraCentre (bodies.get("head_neck"), 0.01128486145784045, 0.00626070665103572, 0.0, "C7"),
         new VertebraCentre (bodies.get("thoracic1"), -8.878277086681011E-4, 0.009322573446504284, 0.0, "T1"),
         new VertebraCentre (bodies.get("thoracic2"), -6.547303170882524E-4, 0.008903461232463565, 0.0, "T2"),
         new VertebraCentre (bodies.get("thoracic3"), -5.981592433877527E-4, 0.010067837184247155, 0.0, "T3"),
         new VertebraCentre (bodies.get("thoracic4"), 4.6396325768649016E-4, 0.010231750107258382, 0.0, "T4"),
         new VertebraCentre (bodies.get("thoracic5"), -3.5351991244491135E-4, 0.009885238698468288, 0.0, "T5"),
         new VertebraCentre (bodies.get("thoracic6"), -0.0022399820129868206, 0.010484310629114327, 0.0, "T6"),
         new VertebraCentre (bodies.get("thoracic7"), -7.379152270659972E-4, 0.011116687388512484, 0.0, "T7"),
         new VertebraCentre (bodies.get("thoracic8"), 2.060226108995122E-4, 0.010769260378083699, 0.0, "T8"),
         new VertebraCentre (bodies.get("thoracic9"), -4.783802942212433E-4, 0.011371645921848044, 0.0, "T9"),
         new VertebraCentre (bodies.get("thoracic10"), -7.016240175332059E-4, 0.013496807379381307, 0.0, "T10"),
         new VertebraCentre (bodies.get("thoracic11"), 5.92111546846475E-4, 0.014201985632002277, 0.0, "T11"),
         new VertebraCentre (bodies.get("thoracic12"), 0.0026959693880027557, 0.013715405281559843, 0.0, "T12"),
         new VertebraCentre (bodies.get("lumbar1"), 0.0030748829923575925, 0.017736729195811884, 0.0, "L1"),
         new VertebraCentre (bodies.get("lumbar2"), 0.002091835980128806, 0.018122022302308777, 0.0, "L2"),
         new VertebraCentre (bodies.get("lumbar3"), 0.0012246399306041801, 0.021132917935878678, 0.0, "L3"),
         new VertebraCentre (bodies.get("lumbar4"), 0.00285994339573816, 0.018801373322584795, 0.0, "L4"),
         new VertebraCentre (bodies.get("lumbar5"), 0.00346615180062, 0.0210934781446, -0.00185007058661, "L5"),
         new VertebraCentre (bodies.get("lumbar5"), 0.00346615180062, 0.0210934781446, -0.00185007058661, "L6"), // duplicate
         new VertebraCentre (bodies.get("sacrum"), -0.021075829-0.08, 0.090446488-0.03, -0.0018500706, "S1"),
         new VertebraCentre (bodies.get("sacrum"), -0.035613918-0.08, 0.072705836-0.03, -0.0018500706, "S2"),
         new VertebraCentre (bodies.get("sacrum"), -0.051467018-0.08, 0.056479689-0.03, -0.0018500706, "S3"),
         new VertebraCentre (bodies.get("sacrum"), -0.065852827-0.08, 0.038072911-0.03, -0.0018500706, "S4"),
         new VertebraCentre (bodies.get("sacrum"), -0.080623859-0.08, 0.014370836-0.03, -0.0018500706, "S5"), 
      };

      for (VertebraCentre vc : centres) { 
         FrameMarker mkr = new FrameMarker (vc.rb, vc.loc);
         mkr.setName (vc.name);
         vbcenters.add (mkr);
      }

      RenderProps.setPointStyle (vbcenters, PointStyle.SPHERE);
      RenderProps.setPointColor (vbcenters, Color.GREEN);
      RenderProps.setPointRadius (vbcenters, 0.01);
      RenderProps.setVisible (vbcenters, true);

      // hips
      PointList<FrameMarker> landmarks =
         new PointList<> (FrameMarker.class, "landmarks");
      mech.add (landmarks);

      RigidBody pelvis = bodies.get ("pelvis");
      FrameMarker mkr = new FrameMarker(pelvis, new Point3d(-0.08, -0.079, 0.075));
      mkr.setName ("FHR");  // right femoral head
      landmarks.add (mkr);

      mkr = new FrameMarker(pelvis, new Point3d(-0.08, -0.079, -0.075));
      mkr.setName ("FHL");  // left femoral head
      landmarks.add (mkr);

      RenderProps.setPointStyle (landmarks, PointStyle.SPHERE);
      RenderProps.setPointColor (landmarks, Color.GREEN);
      RenderProps.setPointRadius (landmarks, 0.01);
      RenderProps.setVisible (landmarks, true);

   }


}
