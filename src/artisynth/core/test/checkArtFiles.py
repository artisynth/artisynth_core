# ArtisynthScript: "checkArtFiles"

def checkArtFile (modelName, *args) :
    if loadModel (modelName, *args) == False:
        print "Model %s not found" % modelName
        return
    folder = File (main.getScriptFile().getParentFile(), "artfiles")
    basename = main.getRootModel().getName()
    for arg in args : 
       basename = basename + arg
    artfile = File (folder, basename + ".art")
    chkfile = File (folder, basename + ".chk")
    if not chkfile.canRead() :
        print "FAIL: chk file for " + basename + " not present"
        return
    main.saveModelFile (artfile)
    errorMsg = ComponentTestUtils.compareArtFiles (chkfile, artfile, True)
    if errorMsg != None:
        print 'FAIL'
        print errorMsg

# Adjust certain solver settings to ensure repeatable results:
MechSystemSolver.myDefaultHybridSolveP = False
MechSystemBase.setDefaultStabilization (PosStabilization.GlobalMass)
FemModel3d.noIncompressStiffnessDamping = False
SurfaceMeshCollider.useAjlCollision = True
PardisoSolver.setDefaultNumThreads (1)
MurtyMechSolver.setDefaultAdaptivelyRebuildA (False)

main.maskFocusStealing (True)

checkArtFile ("artisynth.demos.mech.SpringMeshDemo")
checkArtFile ("artisynth.demos.mech.RigidBodyDemo")
checkArtFile ("artisynth.demos.mech.MechModelDemo")
checkArtFile ("artisynth.demos.mech.MechModelCollide")
checkArtFile ("artisynth.demos.mech.MultiSpringDemo")
checkArtFile ("artisynth.demos.mech.SegmentedPlaneDemo")
checkArtFile ("artisynth.demos.mech.HingeJointDemo")
checkArtFile ("artisynth.demos.mech.SliderJointDemo")
checkArtFile ("artisynth.demos.mech.CylindricalJointDemo")
checkArtFile ("artisynth.demos.mech.SlottedHingeJointDemo")
checkArtFile ("artisynth.demos.mech.UniversalJointDemo")
checkArtFile ("artisynth.demos.mech.SphericalJointDemo")
checkArtFile ("artisynth.demos.mech.GimbalJointDemo")
checkArtFile ("artisynth.demos.mech.PlanarJointDemo")
checkArtFile ("artisynth.demos.mech.PlanarTranslationJointDemo")
checkArtFile ("artisynth.demos.mech.EllipsoidJointDemo")
checkArtFile ("artisynth.demos.mech.ConstrainedParticle")
checkArtFile ("artisynth.demos.mech.BlockTest")
checkArtFile ("artisynth.demos.mech.FrameSpringDemo")
checkArtFile ("artisynth.demos.mech.RigidBodyCollision")
checkArtFile ("artisynth.demos.mech.RigidCompositeCollide")
checkArtFile ("artisynth.demos.mech.LaymanDemo")
checkArtFile ("artisynth.demos.mech.WrappedMuscleArm")
checkArtFile ("artisynth.demos.mech.SkinDemo")
checkArtFile ("artisynth.demos.mech.ConditionalMarkerDemo")
checkArtFile ("artisynth.demos.mech.ConditionalMarkerDemo", "-wrapping")
checkArtFile ("artisynth.demos.mech.CoordinateCouplingDemo")
checkArtFile ("artisynth.demos.mech.JointLimitDemo")

checkArtFile ("artisynth.demos.fem.ArticulatedFem")
checkArtFile ("artisynth.demos.fem.AttachDemo")
checkArtFile ("artisynth.demos.fem.AttachedBeamDemo")
checkArtFile ("artisynth.demos.fem.CombinedShellFem")
checkArtFile ("artisynth.demos.fem.CombinedElemFem")
checkArtFile ("artisynth.demos.fem.FemCollision")
checkArtFile ("artisynth.demos.fem.FemMuscleArm")
checkArtFile ("artisynth.demos.fem.FemMuscleDemo")
checkArtFile ("artisynth.demos.fem.FemPlaneCollide")
checkArtFile ("artisynth.demos.fem.FemSkinDemo")
checkArtFile ("artisynth.demos.fem.Hex3dBlock")
checkArtFile ("artisynth.demos.fem.LeafDemo")
checkArtFile ("artisynth.demos.fem.PlaneConstrainedFem")
checkArtFile ("artisynth.demos.fem.SelfCollision")
checkArtFile ("artisynth.demos.fem.ShellTriPatch")
checkArtFile ("artisynth.demos.fem.ShellTriPatch", "-membrane")
checkArtFile ("artisynth.demos.fem.ShellQuadPatch")
checkArtFile ("artisynth.demos.fem.ShellQuadPatch", "-membrane")
checkArtFile ("artisynth.demos.fem.ShellBlock")
checkArtFile ("artisynth.demos.fem.ShellBlock", "-membrane")
checkArtFile ("artisynth.demos.fem.TetBeam3d")
checkArtFile ("artisynth.demos.fem.ViscousBeam")
checkArtFile ("artisynth.demos.fem.SkinCollisionTest")
checkArtFile ("artisynth.demos.fem.HexCube")
checkArtFile ("artisynth.demos.fem.HexCube", "-quad")
checkArtFile ("artisynth.demos.fem.TetCube")
checkArtFile ("artisynth.demos.fem.TetCube", "-quad")

checkArtFile ("artisynth.models.alanMasseter.MasseterM16462John")
#checkArtFile ("artisynth.models.phuman.SimpleJointedArm") // not repeatable

checkArtFile ("artisynth.models.tongue3d.HexTongueDemo", "-exciter", "GGP")
checkArtFile ("artisynth.models.tongue3d.FemMuscleTongueDemo")
#checkArtFile ("artisynth.models.inversedemos.TongueInvDemo") // Doesn't work
#checkArtFile ("artisynth.models.inversedemos.HydrostatInvDemo") // Doesn't work

checkArtFile ("artisynth.models.dynjaw.JawLarynxDemo")
# JawDemo needs delay since rendering sets Jaw textureProps.enabled=false
checkArtFile ("artisynth.models.dynjaw.JawDemo")
checkArtFile ("artisynth.models.dangTongue.FemTongueDemo")

checkArtFile ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "CYLINDER")
checkArtFile ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "SPHERE")
checkArtFile ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "ELLIPSOID")
checkArtFile ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "TORUS")
checkArtFile ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "PHALANX")

checkArtFile ("artisynth.demos.tutorial.CylinderWrapping")
checkArtFile ("artisynth.demos.tutorial.FemBeam")
#checkArtFile ("artisynth.demos.tutorial.FemBeamColored") # member variables
checkArtFile ("artisynth.demos.tutorial.FemBeamWithBlock")
checkArtFile ("artisynth.demos.tutorial.FemBeamWithFemSphere")
checkArtFile ("artisynth.demos.tutorial.FemBeamWithMuscle")
checkArtFile ("artisynth.demos.tutorial.FemCollisions")
checkArtFile ("artisynth.demos.tutorial.FemEmbeddedSphere")
checkArtFile ("artisynth.demos.tutorial.FemMuscleBeams")
checkArtFile ("artisynth.demos.tutorial.FrameBodyAttachment")
checkArtFile ("artisynth.demos.tutorial.FrameFemAttachment")
checkArtFile ("artisynth.demos.tutorial.JointedFemBeams")
checkArtFile ("artisynth.demos.tutorial.LumbarFrameSpring")
checkArtFile ("artisynth.demos.tutorial.NetDemo")
checkArtFile ("artisynth.demos.tutorial.NetDemoWithPan")
checkArtFile ("artisynth.demos.tutorial.NetDemoWithRefs")
checkArtFile ("artisynth.demos.tutorial.ParticleAttachment")
checkArtFile ("artisynth.demos.tutorial.ParticleSpring")
checkArtFile ("artisynth.demos.tutorial.PenetrationRender")
checkArtFile ("artisynth.demos.tutorial.PhalanxWrapping")
checkArtFile ("artisynth.demos.tutorial.PointFemAttachment")
checkArtFile ("artisynth.demos.tutorial.RigidBodyJoint")
checkArtFile ("artisynth.demos.tutorial.RigidBodySpring")
checkArtFile ("artisynth.demos.tutorial.RigidCompositeBody")
checkArtFile ("artisynth.demos.tutorial.SimpleMuscle")
checkArtFile ("artisynth.demos.tutorial.RadialMuscle")
checkArtFile ("artisynth.demos.tutorial.VariableStiffness")
checkArtFile ("artisynth.demos.tutorial.MaterialBundleDemo")
#checkArtFile ("artisynth.demos.tutorial.SimpleMuscleWithController") # scan not implemented
checkArtFile ("artisynth.demos.tutorial.SimpleMuscleWithPanel")
checkArtFile ("artisynth.demos.tutorial.SimpleMuscleWithProbes")
checkArtFile ("artisynth.demos.tutorial.SphericalTextureMapping")
checkArtFile ("artisynth.demos.tutorial.TalusWrapping")
checkArtFile ("artisynth.demos.tutorial.TorusWrapping")
checkArtFile ("artisynth.demos.tutorial.ViaPointMuscle")
checkArtFile ("artisynth.demos.tutorial.AllBodySkinning")
checkArtFile ("artisynth.demos.tutorial.PhalanxSkinWrapping")
checkArtFile ("artisynth.demos.tutorial.SkinBodyCollide")
checkArtFile ("artisynth.demos.tutorial.RollingFem")
checkArtFile ("artisynth.demos.tutorial.SlidingFem")
checkArtFile ("artisynth.demos.tutorial.FemSelfCollide")
checkArtFile ("artisynth.demos.tutorial.ScalarFieldVisualization")
checkArtFile ("artisynth.demos.tutorial.FemCutPlaneDemo")

checkArtFile ("artisynth.demos.test.OneBasedNumbering")
checkArtFile ("artisynth.demos.test.ReflectedBodies")
checkArtFile ("artisynth.demos.test.TorusWrapTest")
checkArtFile ("artisynth.demos.test.LinearPointConstraintTest")
checkArtFile ("artisynth.demos.test.FemFieldTest")
checkArtFile ("artisynth.demos.test.ShellVolumeAttach")
checkArtFile ("artisynth.demos.test.ShellVolumeAttach", "-membrane")
checkArtFile ("artisynth.demos.test.ShellShellAttach")
checkArtFile ("artisynth.demos.test.ShellShellAttach", "-membrane1")
checkArtFile ("artisynth.demos.test.ShellShellAttach", "-membrane1", "-membrane2")
checkArtFile ("artisynth.demos.test.ShellShellAttach", "-membrane2")
checkArtFile ("artisynth.demos.test.ActuatedSkinning")
checkArtFile ("artisynth.demos.test.ActuatedSkinning", "-dq")
checkArtFile ("artisynth.demos.test.PointPlaneForceTest")
checkArtFile ("artisynth.demos.test.PointPlaneForceTest", "-quadratic")

#checkArtFile ("artisynth.demos.tutorial.SimpleMuscleWithProperties") # member variables

checkArtFile ("artisynth.demos.mech.PointForceDemo")

setCompliantContactForImplicitFriction = True

checkArtFile ("artisynth.demos.tutorial.JointedCollide")
checkArtFile ("artisynth.demos.tutorial.BallPlateCollide")
checkArtFile ("artisynth.demos.tutorial.DeformedJointedCollide")
checkArtFile ("artisynth.demos.tutorial.ContactForceMonitorSavable")
checkArtFile ("artisynth.demos.tutorial.JointedBallCollide")
checkArtFile ("artisynth.demos.tutorial.VariableElasticContact")
checkArtFile ("artisynth.demos.tutorial.ElasticFoundationContact")

checkArtFile ("artisynth.demos.test.EquilibriumMuscleTest")
checkArtFile ("artisynth.demos.test.EquilibriumMuscleTest", "-thelen")
checkArtFile ("artisynth.demos.tutorial.ControlPanelDemo")
checkArtFile ("artisynth.demos.tutorial.EquilibriumMuscleDemo")

main.maskFocusStealing (False)
if main.getMainFrame() == None:
   main.quit()

