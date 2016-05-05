# ArtisynthScript: "saveLoadTest"

def testSaveLoadFull (modelName, sec, fmt, tsim, hsim) :
    loadModel (modelName)
    delay (sec)
    tester = TestCommands(main)
    errorMsg = tester.testSaveAndLoad ("test", fmt, tsim, hsim)
    if errorMsg != None:
        print 'Error'
        print errorMsg
        #abort()
        #main.maskFocusStealing (False)

def testSaveLoad (modelName) :
    testSaveLoadFull (modelName, 0, "%g", 1.0, 0.10)

# Adjust certain solver settings to ensure repeatable results:
MechSystemSolver.myDefaultHybridSolveP = False
MechSystemBase.setDefaultStabilization (PosStabilization.GlobalMass)
FemModel3d.noIncompressStiffnessDamping = False
SurfaceMeshCollider.useAjlCollision = True
PardisoSolver.setDefaultNumThreads (1)

main.maskFocusStealing (True)

#testSaveLoad ("JawHemi") // Doesn't work
#testSaveLoad ("Articulator") // Doesn't work
#testSaveLoad ("JawFemMuscleTongue") // Doesn't work

testSaveLoad ("artisynth.demos.mech.SpringMeshDemo")
testSaveLoad ("artisynth.demos.mech.RigidBodyDemo")
testSaveLoad ("artisynth.demos.mech.MechModelDemo")
testSaveLoad ("artisynth.demos.mech.NetDemo")
testSaveLoad ("artisynth.demos.mech.MultiSpringDemo")
testSaveLoad ("artisynth.demos.mech.SegmentedPlaneDemo")
testSaveLoad ("artisynth.demos.fem.ArticulatedFem")
testSaveLoad ("artisynth.demos.fem.FemMuscleDemo")
testSaveLoad ("artisynth.demos.fem.FemSkinDemo")
#testSaveLoadFull ("artisynth.models.alanMasseter.MasseterM16462John",0,"%g",0.2,0.02)
#testSaveLoad ("artisynth.models.phuman.SimpleJointedArm")
testSaveLoad ("artisynth.demos.mech.ConstrainedParticle")
testSaveLoad ("artisynth.demos.fem.Hex3dBlock")
testSaveLoadFull ("artisynth.demos.fem.TetBeam3d", 0, "%.10g", 1.0, 0.10)
testSaveLoad ("artisynth.demos.fem.PlaneConstrainedFem")
testSaveLoad ("artisynth.demos.fem.AttachedBeamDemo")
testSaveLoad ("artisynth.demos.fem.ViscousBeam")
testSaveLoadFull ("artisynth.demos.mech.BlockTest", 0, "%g", 1.5, 0.10)
testSaveLoad ("artisynth.demos.mech.FrameSpringDemo")
testSaveLoad ("artisynth.demos.mech.RigidBodyCollision")
testSaveLoad ("artisynth.demos.fem.SelfCollision")
testSaveLoad ("artisynth.demos.fem.FemPlaneCollide");
testSaveLoad ("artisynth.demos.mech.LaymanDemo")
testSaveLoad ("artisynth.demos.fem.FemCollision")
#testSaveLoadFull ("artisynth.models.tongue3d.HexTongueDemo", 0, "%g", 0.2, 0.02)
#testSaveLoadFull ("artisynth.models.tongue3d.FemMuscleTongueDemo", 0, "%g", 0.2, 0.02)
#testSaveLoad ("artisynth.models.inversedemos.TongueInvDemo") // Doesn't work
#testSaveLoad ("artisynth.models.inversedemos.HydrostatInvDemo") // Doesn't work

#testSaveLoadFull ("artisynth.models.dynjaw.JawLarynxDemo", 1, "%g", 1.0, 0.10)
testSaveLoad ("artisynth.demos.mech.SkinDemo")
#testSaveLoad ("artisynth.models.dynjaw.JawDemo")
#testSaveLoad ("artisynth.models.dangTongue.FemTongueDemo")
testSaveLoadFull ("artisynth.demos.fem.FemMuscleArm", 0, "%g", 1.0, 0.10)

testSaveLoad ("artisynth.demos.tutorial.BallPlateCollide")
testSaveLoad ("artisynth.demos.tutorial.DeformedJointedCollide")
testSaveLoad ("artisynth.demos.tutorial.FemBeam")
#testSaveLoad ("artisynth.demos.tutorial.FemBeamColored") # member variables
testSaveLoad ("artisynth.demos.tutorial.FemBeamWithBlock")
testSaveLoad ("artisynth.demos.tutorial.FemBeamWithFemSphere")
testSaveLoad ("artisynth.demos.tutorial.FemBeamWithMuscle")
testSaveLoadFull ("artisynth.demos.tutorial.FemCollisions", 0, "%g", 0.04, 0.002)
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
testSaveLoad ("artisynth.demos.tutorial.PointFemAttachment")
testSaveLoad ("artisynth.demos.tutorial.RigidBodyJoint")
testSaveLoad ("artisynth.demos.tutorial.RigidBodySpring")
testSaveLoad ("artisynth.demos.tutorial.SimpleMuscle")
#testSaveLoad ("artisynth.demos.tutorial.SimpleMuscleWithController") # scan not implemented
testSaveLoad ("artisynth.demos.tutorial.CylinderWrapping")
testSaveLoad ("artisynth.demos.tutorial.SimpleMuscleWithPanel")
testSaveLoad ("artisynth.demos.tutorial.SimpleMuscleWithProbes")
testSaveLoad ("artisynth.demos.tutorial.SphericalTextureMapping")
#testSaveLoad ("artisynth.demos.tutorial.SimpleMuscleWithProperties") # member variables


#testSaveLoad ("artisynth.demos.mech.PointForceDemo")

#testSaveLoad ("artisynth.models.inversedemos.PointModel2d")
#testSaveLoad ("artisynth.models.inversedemos.PointModel3d")

main.maskFocusStealing (False)

