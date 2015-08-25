# ArtisynthScript: "MechmodelTest"
#
# For repeatable results, set the environment variable OMP_NUM_THREADS
# to 1
#
# most recent test was run using:
#   1 step of numerical refinement in Pardiso
#   a time step of 0.01 in RigidBodyCollision
def setModelOpts (t, file) :
    mech = find ("models/0")
    mech.setPrintState ("%g")
    addBreakPoint (t)
    return mech

# Adjust certain solver settings to ensure repeatable results:
MechSystemSolver.myDefaultHybridSolveP = False
MechSystemBase.setDefaultStabilization (PosStabilization.GlobalMass)
FemModel3d.noIncompressStiffnessDamping = False
SurfaceMeshCollider.useAjlCollision = False

#main.maskFocusStealing (True)
dataFileName = "mechmodelTest.out"
loadModel ("artisynth.demos.mech.SpringMeshDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.openPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ForwardEuler)
pw.println ("#SpringMesh ForwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
pw.println ("#SpringMesh RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#SpringMesh BackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.RigidBodyDemo")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ForwardEuler)
pw.println ("#RigidBody ForwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
pw.println ("#RigidBody RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#RigidBody BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#RigidBody ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.MechModelDemo")
mech = setModelOpts (2.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
pw.println ("#MechModel SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
pw.println ("#MechModel RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#MechModel BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#MechModel ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.MultiSpringDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
pw.println ("#MultiSpringDemo SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
pw.println ("#MultiSpringDemo RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#MultiSpringDemo BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#MultiSpringDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.SegmentedPlaneDemo")
mech = setModelOpts (2, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
pw.println ("#SegmentedPlaneDemo SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
pw.println ("#SegmentedPlaneDemo RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#SegmentedPlaneDemo BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#SegmentedPlaneDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.ArticulatedFem")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#ArticulatedFem BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#ArticulatedFem ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.Hex3dBlock")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#HexBlock BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#HexBlock ConstrainedBackwardEuler");
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
pw.println ("#TetBeam3d BackwardEuler");
run()
waitForStop()
reset()
prop.set (0.5)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#TetBeam3d ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.PlaneConstrainedFem")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#PlaneConstrainedFem BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#PlaneConstrainedFem ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.AttachedBeamDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
fem = mech.findComponent ("models/beam1")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#AttachedBeamDemo BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#AttachedBeamDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.HexBeam3d")
mech = find ("models/0")
mech.setPrintState ("%g")
fem = find ("models/0/models/0");
pw = mech.reopenPrintStateFile (dataFileName)
pw.println ("#NodalIncompTest ConstrainedBackwardEuler")
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
pw.println ("#NodalIncompTest BackwardEuler")
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
pw.println ("#SelfCollision BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#SelfCollision ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.FemMuscleDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#FemMuscleDemo BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#FemMuscleDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.ViscousBeam")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#ViscousBeam BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#ViscousBeam ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.ArticulatedBeamBody")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
pw.println ("#ArticulatedBeamBody SymplecticEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.AttachedBeamBody")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#AttachedBeamBody ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.BlockTest")
mech = setModelOpts (2, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
pw.println ("#BlockTest SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
pw.println ("#BlockTest RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#BlockTest ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.FrameSpringDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
pw.println ("#FrameSpring SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
pw.println ("#FrameSpring RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#FrameSpring ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.RigidBodyCollision")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
pw.println ("#RigidBodyCollision SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
pw.println ("#RigidBodyCollision RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#RigidBodyCollision BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#RigidBodyCollision ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.FemSkinDemo")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#FemSkinDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.models.collision.SkinCollisionTest")
mech = setModelOpts (2.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#SkinCollisionTest ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.SkinDemo")
mech = setModelOpts (2.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#SkinDemo ConstrainedBackwardEuler");
root().getInputProbes().get(0).setActive(True)
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.LaymanDemo")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
pw.println ("#LaymanDemo SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
pw.println ("#LaymanDemo RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#LaymanDemo BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#LaymanDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.fem.FemCollision")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
pw.println ("#FemCollision BackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#FemCollision ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.models.dynjaw.JawLarynxDemo");
mech = setModelOpts (2, dataFileName) # there is an earlier breakpoint at 0.575
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
pw.println ("#JawLarynxDemo SymplecticEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
pw.println ("#JawLarynxDemo RungeKutta");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.Trapezoidal)
pw.println ("#JawLarynxDemo Trapezoidal");
run()
waitForStop()
reset()

loadModel ("artisynth.models.inversedemos.TongueInvDemo")
mech = setModelOpts (0.1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
pw.println ("#TongueInvDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.inverse.PointModel2d")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.Trapezoidal)
pw.println ("#PointInv2d Trapezoidal");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.inverse.PointModel3d")
mech = setModelOpts (1, dataFileName)
mech.setMaxStepSize (0.05)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.Trapezoidal)
pw.println ("#PointInv3d Trapezoidal");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.inverse.HydrostatInvDemo")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.Trapezoidal)
pw.println ("#HydrostatInvDemo Trapezoidal");
run()
waitForStop()
reset()

#main.maskFocusStealing (False)
quit()


