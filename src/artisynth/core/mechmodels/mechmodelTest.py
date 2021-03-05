# ArtisynthScript: "MechmodelTest"
#
# For repeatable results, set the environment variable OMP_NUM_THREADS
# to 1
def setModelOpts (t, file) :
    mech = find ("models/0")
    mech.setPrintState ("%g")
    addBreakPoint (t)
    return mech

def dorun() :
    run()
    waitForStop()
    reset()
    #sys.stdin.readline() # uncommet for single stepping 

# Adjust certain solver settings to ensure repeatable results:
MechSystemSolver.myDefaultHybridSolveP = False
MechSystemBase.setDefaultStabilization (PosStabilization.GlobalMass)
FemModel3d.noIncompressStiffnessDamping = False
SurfaceMeshCollider.useAjlCollision = False
PardisoSolver.setDefaultNumThreads (1)

main.maskFocusStealing (True)
dataFileName = "mechmodelTest.out"
loadModel ("artisynth.demos.mech.SpringMeshDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.openPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ForwardEuler)
mech.writePrintStateHeader ("SpringMesh ForwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("SpringMesh RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("SpringMesh BackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.RigidBodyDemo")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ForwardEuler)
mech.writePrintStateHeader ("RigidBody ForwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("RigidBody RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("RigidBody BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("RigidBody ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.MechModelDemo")
mech = setModelOpts (2.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("MechModel SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("MechModel RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("MechModel BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("MechModel ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.MultiSpringDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("MultiSpringDemo SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("MultiSpringDemo RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("MultiSpringDemo BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("MultiSpringDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.SegmentedPlaneDemo")
mech = setModelOpts (2, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("SegmentedPlaneDemo SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("SegmentedPlaneDemo RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("SegmentedPlaneDemo BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("SegmentedPlaneDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.BodyBodyAttachment")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("BodyBodyAttachment SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("BodyBodyAttachment RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("BodyBodyAttachment ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.BodyBodyJoint")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("BodyBodyJoint SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("BodyBodyJoint RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("BodyBodyJoint ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.ArticulatedFem")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("ArticulatedFem BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("ArticulatedFem ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.Hex3dBlock")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("HexBlock BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("HexBlock ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.TetBeam3d")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
prop = root().getProperty("excitation0")
prop.set (0.5)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("TetBeam3d BackwardEuler");
run()
waitForStop()
reset()
prop.set (0.5)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("TetBeam3d ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.PlaneConstrainedFem")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("PlaneConstrainedFem BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("PlaneConstrainedFem ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.AttachedBeamDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
fem = mech.findComponent ("models/beam1")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("AttachedBeamDemo BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("AttachedBeamDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.BodyFemAttachment")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("BodyFemAttachment ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.HexBeam3d")
mech = find ("models/0")
mech.setPrintState ("%g")
fem = find ("models/0/models/0");
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("NodalIncompTest ConstrainedBackwardEuler")
fem.setMaterial (MooneyRivlinMaterial())
addBreakPoint (0.3)
addBreakPoint (0.6)
addBreakPoint (0.9)
run()
waitForStop()
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
run()
waitForStop()
fem.setMaterial (LinearMaterial())
run()
waitForStop()
reset()
fem.setMaterial (MooneyRivlinMaterial())
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("NodalIncompTest BackwardEuler")
run()
waitForStop()
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
run()
waitForStop()
fem.setMaterial (LinearMaterial())
run()
waitForStop()
reset()
fem.setMaterial (MooneyRivlinMaterial())
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.SelfCollision")
mech = setModelOpts (0.7, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("SelfCollision BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("SelfCollision ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.FemMuscleDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("FemMuscleDemo BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("FemMuscleDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.ViscousBeam")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("ViscousBeam BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("ViscousBeam ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.ArticulatedBeamBody")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("ArticulatedBeamBody SymplecticEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.AttachedBeamBody")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("AttachedBeamBody ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.test.LinearPointConstraintTest")
mech = setModelOpts (0.4, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("LinearPointConstraintTest ConstrainedBackwardEuler");
run()
waitForStop()
reset()
mech.setMaxStepSize (0.001)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("LinearPointConstraintTest SymplecticEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.BeamBodyCollide")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("BeamBodyCollide ConstrainedBackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("BeamBodyCollide SymplecticEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.BlockTest")
mech = setModelOpts (2, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("BlockTest SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("BlockTest RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("BlockTest ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.FrameSpringDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("FrameSpring SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("FrameSpring RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("FrameSpring ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.RigidBodyCollision")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("RigidBodyCollision SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("RigidBodyCollision RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("RigidBodyCollision BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("RigidBodyCollision ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.RigidCompositeCollide")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("RigidCompositeCollide SymplecticEuler");
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("RigidCompositeCollide ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.fem.FemSkinDemo")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("FemSkinDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.SkinCollisionTest")
mech = setModelOpts (2.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("SkinCollisionTest ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.SkinDemo")
mech = setModelOpts (2.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("SkinDemo ConstrainedBackwardEuler");
root().getInputProbes().get(0).setActive(True)
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.LaymanDemo")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("LaymanDemo SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("LaymanDemo RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("LaymanDemo BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("LaymanDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.FemCollision")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("FemCollision BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("FemCollision ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.models.dynjaw.JawLarynxDemo");
mech = setModelOpts (2, dataFileName) # there is an earlier breakpoint at 0.575
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("JawLarynxDemo SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("JawLarynxDemo RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.Trapezoidal)
mech.writePrintStateHeader ("JawLarynxDemo Trapezoidal");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.tutorial.VariableStiffness")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("VariableStiffness ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.tutorial.RadialMuscle")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("RadialMuscle ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.tutorial.MaterialBundleDemo")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("MaterialBundleDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.tutorial.VariableStiffness")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("VariableStiffness ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.test.PointPlaneForceTest")
mech = setModelOpts (2.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("PointPlaneForceTest ConstrainedBackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("PointPlaneForceTest SymplecticEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.test.PointPlaneForceTest [-quadratic]")
mech = setModelOpts (2.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("PointPlaneForceTest quadratic ConstrainedBackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("PointPlaneForceTest quadratic SymplecticEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.HingeJointDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("HingeJointDemo");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.SliderJointDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("SliderJointDemo")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.CylindricalJointDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("CylindricalJointDemo")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.SlottedHingeJointDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("SlottedHingeJointDemo")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.UniversalJointDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("UniversalJointDemo")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.SphericalJointDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("SphericalJointDemo")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.GimbalJointDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("GimbalJointDemo")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.PlanarJointDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("PlanarJointDemo")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.PlanarTranslationJointDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("PlanarTranslationJointDemo")
run()
waitForStop()
reset()

loadModel ("artisynth.models.tongue3d.TongueInvDemo")
mech = setModelOpts (0.1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("TongueInvDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()
# reset to false since it was set by the TongueInvDemo
InverseManager.useLegacyNames = False

loadModel ("artisynth.demos.inverse.PointModel2d")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.Trapezoidal)
mech.writePrintStateHeader ("PointInv2d Trapezoidal");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.inverse.PointModel3d")
mech = setModelOpts (1, dataFileName)
mech.setMaxStepSize (0.05)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.Trapezoidal)
mech.writePrintStateHeader ("PointInv3d Trapezoidal");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.inverse.HydrostatInvDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.Trapezoidal)
mech.writePrintStateHeader ("HydrostatInvDemo Trapezoidal");
run()
waitForStop()
reset()

main.maskFocusStealing (False)
if main.getMainFrame() == None:
   main.quit()

