# ArtisynthScript: "saveLoadTest"

def testSaveLoadFull (sec, fmt, tsim, hsim, modelName, *args) :
    if loadModel (modelName, *args) == False:
        print "Model %s not found" % modelName
        return
    delay (sec)
    tester = TestCommands(main)
    basename = main.getRootModel().getName()
    errorMsg = tester.testSaveAndLoad (basename, fmt, tsim, hsim)
    if errorMsg != None:
        print 'Error'
        print errorMsg
        #abort()
        #main.maskFocusStealing (False)

def testSaveLoad (modelName, *args) :
    testSaveLoadFull (0, "%g", 1.0, 0.10, modelName, *args)

# Adjust certain solver settings to ensure repeatable results:
MechSystemSolver.myDefaultHybridSolveP = False
MechSystemBase.setDefaultStabilization (PosStabilization.GlobalMass)
FemModel3d.noIncompressStiffnessDamping = False
SurfaceMeshCollider.useAjlCollision = True
PardisoSolver.setDefaultNumThreads (1)

main.maskFocusStealing (True)

#testSaveLoad ("JawHemi") // Doesn't work
#testSaveLoad ("Articulator") // Doesn'twork
#testSaveLoad ("JawFemMuscleTongue") // Doesn't work

testSaveLoad ("artisynth.demos.mech.SpringMeshDemo")
testSaveLoad ("artisynth.demos.mech.RigidBodyDemo")
testSaveLoad ("artisynth.demos.mech.MechModelDemo")
testSaveLoad ("artisynth.demos.mech.MultiSpringDemo")
testSaveLoad ("artisynth.demos.mech.SegmentedPlaneDemo")

testSaveLoad ("artisynth.demos.fem.ArticulatedFem")
testSaveLoad ("artisynth.demos.fem.AttachDemo")
testSaveLoad ("artisynth.demos.fem.AttachedBeamDemo")
testSaveLoad ("artisynth.demos.fem.CombinedShellFem")
testSaveLoad ("artisynth.demos.fem.FemCollision")
testSaveLoadFull (0, "%g", 1.0, 0.10, "artisynth.demos.fem.FemMuscleArm")
testSaveLoad ("artisynth.demos.fem.FemMuscleDemo")
testSaveLoad ("artisynth.demos.fem.FemPlaneCollide");
testSaveLoad ("artisynth.demos.fem.FemSkinDemo")
testSaveLoad ("artisynth.demos.fem.Hex3dBlock")
testSaveLoad ("artisynth.demos.fem.LeafDemo")
testSaveLoad ("artisynth.demos.fem.PlaneConstrainedFem")
testSaveLoad ("artisynth.demos.fem.SelfCollision")
testSaveLoad ("artisynth.demos.fem.ShellTriPatch")
testSaveLoad ("artisynth.demos.fem.ShellTriPatch", "-membrane")
testSaveLoad ("artisynth.demos.fem.ShellQuadPatch")
testSaveLoad ("artisynth.demos.fem.ShellQuadPatch", "-membrane")
testSaveLoad ("artisynth.demos.fem.ShellBlock")
testSaveLoad ("artisynth.demos.fem.ShellBlock", "-membrane")
testSaveLoadFull (0, "%.10g", 1.0, 0.10, "artisynth.demos.fem.TetBeam3d")
testSaveLoad ("artisynth.demos.fem.ViscousBeam")

testSaveLoadFull (0,"%g",0.2,0.02, "artisynth.models.alanMasseter.MasseterM16462John")
testSaveLoad ("artisynth.models.phuman.SimpleJointedArm")
testSaveLoad ("artisynth.demos.mech.ConstrainedParticle")
testSaveLoadFull (0, "%g", 1.5, 0.10, "artisynth.demos.mech.BlockTest")
testSaveLoad ("artisynth.demos.mech.FrameSpringDemo")
testSaveLoad ("artisynth.demos.mech.RigidBodyCollision")
testSaveLoad ("artisynth.demos.mech.RigidCompositeCollide")
testSaveLoad ("artisynth.demos.mech.LaymanDemo")
testSaveLoad ("artisynth.demos.mech.WrappedMuscleArm")

testSaveLoadFull (0, "%g", 0.2, 0.02, "artisynth.models.tongue3d.HexTongueDemo")
testSaveLoadFull (0, "%g", 0.2, 0.02, "artisynth.models.tongue3d.FemMuscleTongueDemo")
#testSaveLoad ("artisynth.models.inversedemos.TongueInvDemo") // Doesn't work
#testSaveLoad ("artisynth.models.inversedemos.HydrostatInvDemo") // Doesn't work

testSaveLoadFull (1, "%g", 1.0, 0.10, "artisynth.models.dynjaw.JawLarynxDemo")
testSaveLoad ("artisynth.demos.mech.SkinDemo")
# JawDemo needs delay since rendering sets Jaw textureProps.enabled=false
testSaveLoadFull (0.5, "%g", 1.0, 0.10, "artisynth.models.dynjaw.JawDemo")
testSaveLoad ("artisynth.models.dangTongue.FemTongueDemo")

testSaveLoad ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "CYLINDER")
testSaveLoad ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "SPHERE")
testSaveLoad ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "ELLIPSOID")
testSaveLoad ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "TORUS")
testSaveLoad ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "PHALANX")

testSaveLoad ("artisynth.demos.tutorial.BallPlateCollide")
testSaveLoad ("artisynth.demos.tutorial.DeformedJointedCollide")
testSaveLoad ("artisynth.demos.tutorial.CylinderWrapping")
testSaveLoad ("artisynth.demos.tutorial.FemBeam")
#testSaveLoad ("artisynth.demos.tutorial.FemBeamColored") # member variables
testSaveLoad ("artisynth.demos.tutorial.FemBeamWithBlock")
testSaveLoad ("artisynth.demos.tutorial.FemBeamWithFemSphere")
testSaveLoad ("artisynth.demos.tutorial.FemBeamWithMuscle")
testSaveLoadFull (0, "%g", 0.04, 0.002, "artisynth.demos.tutorial.FemCollisions")
testSaveLoad ("artisynth.demos.tutorial.FemEmbeddedSphere")
testSaveLoad ("artisynth.demos.tutorial.FemMuscleBeams")
testSaveLoad ("artisynth.demos.tutorial.FrameBodyAttachment")
testSaveLoad ("artisynth.demos.tutorial.FrameFemAttachment")
testSaveLoad ("artisynth.demos.tutorial.JointedCollide")
testSaveLoad ("artisynth.demos.tutorial.JointedFemBeams")
testSaveLoad ("artisynth.demos.tutorial.LumbarFrameSpring")
testSaveLoad ("artisynth.demos.tutorial.NetDemo")
testSaveLoad ("artisynth.demos.tutorial.NetDemoWithPan")
testSaveLoad ("artisynth.demos.tutorial.NetDemoWithRefs")
testSaveLoad ("artisynth.demos.tutorial.ParticleAttachment")
testSaveLoad ("artisynth.demos.tutorial.ParticleSpring")
testSaveLoad ("artisynth.demos.tutorial.PenetrationRender")
testSaveLoad ("artisynth.demos.tutorial.PhalanxWrapping")
testSaveLoad ("artisynth.demos.tutorial.PointFemAttachment")
testSaveLoad ("artisynth.demos.tutorial.RigidBodyJoint")
testSaveLoad ("artisynth.demos.tutorial.RigidBodySpring")
testSaveLoad ("artisynth.demos.tutorial.RigidCompositeBody")
testSaveLoad ("artisynth.demos.tutorial.SimpleMuscle")
testSaveLoad ("artisynth.demos.tutorial.RadialMuscle")
testSaveLoad ("artisynth.demos.tutorial.VariableStiffness")
testSaveLoad ("artisynth.demos.tutorial.MaterialBundleDemo")
#testSaveLoad ("artisynth.demos.tutorial.SimpleMuscleWithController") # scan not implemented
testSaveLoad ("artisynth.demos.tutorial.SimpleMuscleWithPanel")
testSaveLoad ("artisynth.demos.tutorial.SimpleMuscleWithProbes")
testSaveLoad ("artisynth.demos.tutorial.SphericalTextureMapping")
testSaveLoad ("artisynth.demos.tutorial.TalusWrapping")
testSaveLoad ("artisynth.demos.tutorial.TorusWrapping")
testSaveLoad ("artisynth.demos.tutorial.ViaPointMuscle")

testSaveLoad ("artisynth.demos.test.OneBasedNumbering");
testSaveLoad ("artisynth.demos.test.ReflectedBodies");
testSaveLoad ("artisynth.demos.test.TorusWrapTest")
testSaveLoad ("artisynth.demos.test.LinearPointConstraintTest")
testSaveLoad ("artisynth.demos.test.FemFieldTest")
testSaveLoad ("artisynth.demos.test.ShellVolumeAttach")
testSaveLoad ("artisynth.demos.test.ShellVolumeAttach", "-membrane");
testSaveLoad ("artisynth.demos.test.ShellShellAttach")
testSaveLoad ("artisynth.demos.test.ShellShellAttach", "-membrane1");
testSaveLoad ("artisynth.demos.test.ShellShellAttach", "-membrane1", "-membrane2");
testSaveLoad ("artisynth.demos.test.ShellShellAttach", "-membrane2");

testSaveLoad ("artisynth.demos.fem.SkinCollisionTest");
testSaveLoad ("artisynth.demos.tutorial.AllBodySkinning")
testSaveLoad ("artisynth.demos.tutorial.PhalanxSkinWrapping")
testSaveLoad ("artisynth.demos.tutorial.SkinBodyCollide")
testSaveLoad ("artisynth.demos.test.ActuatedSkinning")
testSaveLoad ("artisynth.demos.test.ActuatedSkinning", "-dq")

testSaveLoad ("artisynth.demos.test.PointPlaneForceTest")
testSaveLoad ("artisynth.demos.test.PointPlaneForceTest", "-quadratic")

testSaveLoad ("artisynth.demos.mech.HingeJointDemo")
testSaveLoad ("artisynth.demos.mech.SliderJointDemo")
testSaveLoad ("artisynth.demos.mech.CylindricalJointDemo")
testSaveLoad ("artisynth.demos.mech.SlottedHingeJointDemo")
testSaveLoad ("artisynth.demos.mech.UniversalJointDemo")
testSaveLoad ("artisynth.demos.mech.SphericalJointDemo")
testSaveLoad ("artisynth.demos.mech.GimbalJointDemo")
testSaveLoad ("artisynth.demos.mech.PlanarJointDemo")
testSaveLoad ("artisynth.demos.mech.PlanarTranslationJointDemo")

#testSaveLoad ("artisynth.demos.tutorial.SimpleMuscleWithProperties") # member variables

#testSaveLoad ("artisynth.demos.mech.PointForceDemo")

main.maskFocusStealing (False)
if main.getMainFrame() == None:
   main.quit()

