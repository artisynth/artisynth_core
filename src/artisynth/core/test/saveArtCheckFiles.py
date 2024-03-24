# ArtisynthScript: "saveArtCheckFiles"

def saveArtFile (modelName, *args) :
    if loadModel (modelName, *args) == False:
        print "Model %s not found" % modelName
        return
    folder = File (main.getScriptFile().getParentFile(), "artfiles")
    basename = main.getRootModel().getName()
    for arg in args : 
       basename = basename + arg
    artfile = File (folder, basename + ".chk")
    main.saveModelFile (artfile)

# Adjust certain solver settings to ensure repeatable results:
MechSystemSolver.myDefaultHybridSolveP = False
MechSystemBase.setDefaultStabilization (PosStabilization.GlobalMass)
FemModel3d.noIncompressStiffnessDamping = False
SurfaceMeshCollider.useAjlCollision = True
PardisoSolver.setDefaultNumThreads (1)
MurtyMechSolver.setDefaultAdaptivelyRebuildA (False)

main.maskFocusStealing (True)

saveArtFile ("artisynth.demos.mech.SpringMeshDemo")
saveArtFile ("artisynth.demos.mech.RigidBodyDemo")
saveArtFile ("artisynth.demos.mech.MechModelDemo")
saveArtFile ("artisynth.demos.mech.MechModelCollide")
saveArtFile ("artisynth.demos.mech.MultiSpringDemo")
saveArtFile ("artisynth.demos.mech.SegmentedPlaneDemo")
saveArtFile ("artisynth.demos.mech.HingeJointDemo")
saveArtFile ("artisynth.demos.mech.SliderJointDemo")
saveArtFile ("artisynth.demos.mech.CylindricalJointDemo")
saveArtFile ("artisynth.demos.mech.SlottedHingeJointDemo")
saveArtFile ("artisynth.demos.mech.UniversalJointDemo")
saveArtFile ("artisynth.demos.mech.SphericalJointDemo")
saveArtFile ("artisynth.demos.mech.GimbalJointDemo")
saveArtFile ("artisynth.demos.mech.PlanarJointDemo")
saveArtFile ("artisynth.demos.mech.PlanarTranslationJointDemo")
saveArtFile ("artisynth.demos.mech.EllipsoidJointDemo")
saveArtFile ("artisynth.demos.mech.ConstrainedParticle")
saveArtFile ("artisynth.demos.mech.BlockTest")
saveArtFile ("artisynth.demos.mech.FrameSpringDemo")
saveArtFile ("artisynth.demos.mech.RigidBodyCollision")
saveArtFile ("artisynth.demos.mech.RigidCompositeCollide")
saveArtFile ("artisynth.demos.mech.LaymanDemo")
saveArtFile ("artisynth.demos.mech.WrappedMuscleArm")
saveArtFile ("artisynth.demos.mech.SkinDemo")
saveArtFile ("artisynth.demos.mech.ConditionalMarkerDemo")
saveArtFile ("artisynth.demos.mech.ConditionalMarkerDemo", "-wrapping")
saveArtFile ("artisynth.demos.mech.CoordinateCouplingDemo")
saveArtFile ("artisynth.demos.mech.JointLimitDemo")

saveArtFile ("artisynth.demos.fem.ArticulatedFem")
saveArtFile ("artisynth.demos.fem.AttachDemo")
saveArtFile ("artisynth.demos.fem.AttachedBeamDemo")
saveArtFile ("artisynth.demos.fem.CombinedShellFem")
saveArtFile ("artisynth.demos.fem.CombinedElemFem")
saveArtFile ("artisynth.demos.fem.FemCollision")
saveArtFile ("artisynth.demos.fem.FemMuscleArm")
saveArtFile ("artisynth.demos.fem.FemMuscleDemo")
saveArtFile ("artisynth.demos.fem.FemPlaneCollide")
saveArtFile ("artisynth.demos.fem.FemSkinDemo")
saveArtFile ("artisynth.demos.fem.Hex3dBlock")
saveArtFile ("artisynth.demos.fem.LeafDemo")
saveArtFile ("artisynth.demos.fem.PlaneConstrainedFem")
saveArtFile ("artisynth.demos.fem.SelfCollision")
saveArtFile ("artisynth.demos.fem.ShellTriPatch")
saveArtFile ("artisynth.demos.fem.ShellTriPatch", "-membrane")
saveArtFile ("artisynth.demos.fem.ShellQuadPatch")
saveArtFile ("artisynth.demos.fem.ShellQuadPatch", "-membrane")
saveArtFile ("artisynth.demos.fem.ShellBlock")
saveArtFile ("artisynth.demos.fem.ShellBlock", "-membrane")
saveArtFile ("artisynth.demos.fem.TetBeam3d")
saveArtFile ("artisynth.demos.fem.ViscousBeam")
saveArtFile ("artisynth.demos.fem.SkinCollisionTest")
saveArtFile ("artisynth.demos.fem.HexCube")
saveArtFile ("artisynth.demos.fem.HexCube", "-quad")
saveArtFile ("artisynth.demos.fem.TetCube")
saveArtFile ("artisynth.demos.fem.TetCube", "-quad")

saveArtFile ("artisynth.models.alanMasseter.MasseterM16462John")
#saveArtFile ("artisynth.models.phuman.SimpleJointedArm") // not repeatable

saveArtFile ("artisynth.models.tongue3d.HexTongueDemo", "-exciter", "GGP")
saveArtFile ("artisynth.models.tongue3d.FemMuscleTongueDemo")
#saveArtFile ("artisynth.models.inversedemos.TongueInvDemo") // Doesn't work
#saveArtFile ("artisynth.models.inversedemos.HydrostatInvDemo") // Doesn't work

saveArtFile ("artisynth.models.dynjaw.JawLarynxDemo")
# JawDemo needs delay since rendering sets Jaw textureProps.enabled=false
saveArtFile ("artisynth.models.dynjaw.JawDemo")
saveArtFile ("artisynth.models.dangTongue.FemTongueDemo")

saveArtFile ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "CYLINDER")
saveArtFile ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "SPHERE")
saveArtFile ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "ELLIPSOID")
saveArtFile ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "TORUS")
saveArtFile ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "PHALANX")

saveArtFile ("artisynth.demos.tutorial.CylinderWrapping")
saveArtFile ("artisynth.demos.tutorial.FemBeam")
#saveArtFile ("artisynth.demos.tutorial.FemBeamColored") # member variables
saveArtFile ("artisynth.demos.tutorial.FemBeamWithBlock")
saveArtFile ("artisynth.demos.tutorial.FemBeamWithFemSphere")
saveArtFile ("artisynth.demos.tutorial.FemBeamWithMuscle")
saveArtFile ("artisynth.demos.tutorial.FemCollisions")
saveArtFile ("artisynth.demos.tutorial.FemEmbeddedSphere")
saveArtFile ("artisynth.demos.tutorial.FemMuscleBeams")
saveArtFile ("artisynth.demos.tutorial.FrameBodyAttachment")
saveArtFile ("artisynth.demos.tutorial.FrameFemAttachment")
saveArtFile ("artisynth.demos.tutorial.JointedFemBeams")
saveArtFile ("artisynth.demos.tutorial.LumbarFrameSpring")
saveArtFile ("artisynth.demos.tutorial.NetDemo")
saveArtFile ("artisynth.demos.tutorial.NetDemoWithPan")
saveArtFile ("artisynth.demos.tutorial.NetDemoWithRefs")
saveArtFile ("artisynth.demos.tutorial.ParticleAttachment")
saveArtFile ("artisynth.demos.tutorial.ParticleSpring")
saveArtFile ("artisynth.demos.tutorial.PenetrationRender")
saveArtFile ("artisynth.demos.tutorial.PhalanxWrapping")
saveArtFile ("artisynth.demos.tutorial.PointFemAttachment")
saveArtFile ("artisynth.demos.tutorial.RigidBodyJoint")
saveArtFile ("artisynth.demos.tutorial.RigidBodySpring")
saveArtFile ("artisynth.demos.tutorial.RigidCompositeBody")
saveArtFile ("artisynth.demos.tutorial.SimpleMuscle")
saveArtFile ("artisynth.demos.tutorial.RadialMuscle")
saveArtFile ("artisynth.demos.tutorial.VariableStiffness")
saveArtFile ("artisynth.demos.tutorial.MaterialBundleDemo")
#saveArtFile ("artisynth.demos.tutorial.SimpleMuscleWithController") # scan not implemented
saveArtFile ("artisynth.demos.tutorial.SimpleMuscleWithPanel")
saveArtFile ("artisynth.demos.tutorial.SimpleMuscleWithProbes")
saveArtFile ("artisynth.demos.tutorial.SphericalTextureMapping")
saveArtFile ("artisynth.demos.tutorial.TalusWrapping")
saveArtFile ("artisynth.demos.tutorial.TorusWrapping")
saveArtFile ("artisynth.demos.tutorial.ViaPointMuscle")
saveArtFile ("artisynth.demos.tutorial.AllBodySkinning")
saveArtFile ("artisynth.demos.tutorial.PhalanxSkinWrapping")
saveArtFile ("artisynth.demos.tutorial.SkinBodyCollide")
saveArtFile ("artisynth.demos.tutorial.RollingFem")
saveArtFile ("artisynth.demos.tutorial.SlidingFem")
saveArtFile ("artisynth.demos.tutorial.FemSelfCollide")
saveArtFile ("artisynth.demos.tutorial.ScalarFieldVisualization")
saveArtFile ("artisynth.demos.tutorial.FemCutPlaneDemo")

saveArtFile ("artisynth.demos.test.OneBasedNumbering")
saveArtFile ("artisynth.demos.test.ReflectedBodies")
saveArtFile ("artisynth.demos.test.TorusWrapTest")
saveArtFile ("artisynth.demos.test.LinearPointConstraintTest")
saveArtFile ("artisynth.demos.test.FemFieldTest")
saveArtFile ("artisynth.demos.test.ShellVolumeAttach")
saveArtFile ("artisynth.demos.test.ShellVolumeAttach", "-membrane")
saveArtFile ("artisynth.demos.test.ShellShellAttach")
saveArtFile ("artisynth.demos.test.ShellShellAttach", "-membrane1")
saveArtFile ("artisynth.demos.test.ShellShellAttach", "-membrane1", "-membrane2")
saveArtFile ("artisynth.demos.test.ShellShellAttach", "-membrane2")
saveArtFile ("artisynth.demos.test.ActuatedSkinning")
saveArtFile ("artisynth.demos.test.ActuatedSkinning", "-dq")
saveArtFile ("artisynth.demos.test.PointPlaneForceTest")
saveArtFile ("artisynth.demos.test.PointPlaneForceTest", "-quadratic")

#saveArtFile ("artisynth.demos.tutorial.SimpleMuscleWithProperties") # member variables

saveArtFile ("artisynth.demos.mech.PointForceDemo")

setCompliantContactForImplicitFriction = True

saveArtFile ("artisynth.demos.tutorial.JointedCollide")
saveArtFile ("artisynth.demos.tutorial.BallPlateCollide")
saveArtFile ("artisynth.demos.tutorial.DeformedJointedCollide")
saveArtFile ("artisynth.demos.tutorial.ContactForceMonitorSavable")
saveArtFile ("artisynth.demos.tutorial.JointedBallCollide")
saveArtFile ("artisynth.demos.tutorial.VariableElasticContact")
saveArtFile ("artisynth.demos.tutorial.ElasticFoundationContact")

saveArtFile ("artisynth.demos.test.EquilibriumMuscleTest")
saveArtFile ("artisynth.demos.test.EquilibriumMuscleTest", "-thelen")
saveArtFile ("artisynth.demos.tutorial.ControlPanelDemo")
saveArtFile ("artisynth.demos.tutorial.EquilibriumMuscleDemo")

main.maskFocusStealing (False)
if main.getMainFrame() == None:
   main.quit()

