# ArtisynthScript: "saveLoadTest"

def testSaveLoadDelay (modelName, sec) :
    loadModel (modelName)
    delay (sec)
    errorMsg = main.testSaveAndLoad ("test", "%g")
    if errorMsg != None:
        print 'Error'
        print errorMsg
        abort()
        main.maskFocusStealing (False)

def testSaveLoad (modelName) :
    testSaveLoadDelay (modelName, 0)

main.maskFocusStealing (True)


#testSaveLoad ("JawHemi") // Doesn't work
#testSaveLoad ("Articulator") // Doesn't work
#testSaveLoad ("JawFemMuscleTongue") // Doesn't work


testSaveLoad ("Spring Mesh")
testSaveLoad ("Rigid Body Spring")
testSaveLoad ("MechModel Demo")
testSaveLoad ("artisynth.models.mechdemos.NetDemo")
testSaveLoad ("artisynth.models.mechdemos.MultiSpringDemo")
testSaveLoad ("artisynth.models.mechdemos.SegmentedPlaneDemo")
testSaveLoad ("artisynth.models.femdemos.ArticulatedFem")
testSaveLoad ("artisynth.models.femdemos.FemMuscleDemo")
testSaveLoad ("artisynth.models.femdemos.FemSkinDemo")
testSaveLoad ("artisynth.models.alanMasseter.MasseterM16462John");
testSaveLoad ("artisynth.models.phuman.SimpleJointedArm");
testSaveLoad ("artisynth.models.mechdemos.ConstrainedParticle");
testSaveLoad ("artisynth.models.mechdemos.MeshDemo");
testSaveLoad ("HexBlock")
testSaveLoad ("TetBeam3d")
testSaveLoad ("PlaneConstrainedFem")
testSaveLoad ("artisynth.models.femdemos.AttachedBeamDemo")
testSaveLoad ("artisynth.models.femdemos.ViscousBeam")
testSaveLoad ("artisynth.models.mechdemos.BlockTest")
testSaveLoad ("artisynth.models.mechdemos.FrameSpringDemo")
testSaveLoad ("RigidBodyCollision")
testSaveLoad ("LaymanDemo")
testSaveLoad ("Fem Collision")
testSaveLoad ("LineMuscleTongue")
testSaveLoad ("FemMuscleTongue")
#testSaveLoad ("artisynth.models.inversedemos.TongueInvDemo") // Doesn't work
#testSaveLoad ("artisynth.models.inversedemos.HydrostatInvDemo") // Doesn't work

testSaveLoadDelay ("JawLarynx", 1)
testSaveLoad ("artisynth.models.mechdemos.SkinDemo")
testSaveLoad ("Jaw")
testSaveLoad ("DangTongue")
testSaveLoad ("MuscleArm")
testSaveLoad ("PointForceDemo")

#testSaveLoad ("artisynth.models.inversedemos.PointModel2d")
#testSaveLoad ("artisynth.models.inversedemos.PointModel3d")

main.maskFocusStealing (False)

