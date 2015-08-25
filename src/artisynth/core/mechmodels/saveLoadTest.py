# ArtisynthScript: "saveLoadTest"

def testSaveLoadFull (modelName, sec, fmt) :
    loadModel (modelName)
    delay (sec)
    errorMsg = main.testSaveAndLoad ("test", fmt)
    if errorMsg != None:
        print 'Error'
        print errorMsg
        abort()
        main.maskFocusStealing (False)

def testSaveLoad (modelName) :
    testSaveLoadFull (modelName, 0, "%g")

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
testSaveLoad ("artisynth.demos.tutorial.CylinderWrapping")
testSaveLoad ("artisynth.models.alanMasseter.MasseterM16462John")
testSaveLoad ("artisynth.models.phuman.SimpleJointedArm")
testSaveLoad ("artisynth.demos.mech.ConstrainedParticle")
testSaveLoad ("artisynth.demos.fem.Hex3dBlock")
testSaveLoadFull ("artisynth.demos.fem.TetBeam3d", 0, "%.10g")
testSaveLoad ("artisynth.demos.fem.PlaneConstrainedFem")
testSaveLoad ("artisynth.demos.fem.AttachedBeamDemo")
testSaveLoad ("artisynth.demos.fem.ViscousBeam")
testSaveLoad ("artisynth.demos.mech.BlockTest")
testSaveLoad ("artisynth.demos.mech.FrameSpringDemo")
testSaveLoad ("artisynth.demos.mech.RigidBodyCollision")
testSaveLoad ("artisynth.demos.mech.LaymanDemo")
testSaveLoad ("artisynth.demos.fem.FemCollision")
testSaveLoad ("artisynth.models.tongue3d.HexTongueDemo")
testSaveLoad ("artisynth.models.tongue3d.FemMuscleTongueDemo")
#testSaveLoad ("artisynth.models.inversedemos.TongueInvDemo") // Doesn't work
#testSaveLoad ("artisynth.models.inversedemos.HydrostatInvDemo") // Doesn't work

testSaveLoadFull ("artisynth.models.dynjaw.JawLarynxDemo", 1, "%g")
testSaveLoad ("artisynth.demos.mech.SkinDemo")
testSaveLoad ("artisynth.models.dynjaw.JawDemo")
testSaveLoad ("artisynth.models.dangTongue.FemTongueDemo")
testSaveLoadFull ("artisynth.demos.fem.FemMuscleArm", 0, "%.12g")
testSaveLoad ("artisynth.demos.mech.PointForceDemo")

#testSaveLoad ("artisynth.models.inversedemos.PointModel2d")
#testSaveLoad ("artisynth.models.inversedemos.PointModel3d")

main.maskFocusStealing (False)

