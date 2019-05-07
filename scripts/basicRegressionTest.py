# ArtisynthScript: "BasicRegressionTest"
#
# For repeatable results, set the environment variable OMP_NUM_THREADS
# to 1
#

# IMPORTS
from artisynth.core.mechmodels.MechSystemSolver import Integrator

def getMechModel() :
    mech = find ("models/0")
    return mech

def setModelOpts (t) :
    mech = getMechModel()
    mech.setPrintState ("%g")
    addBreakPoint (t)
    return mech

def doRunStop() :
	run()
	waitForStop()

def doRunStopReset() :
    run()
    waitForStop()
    reset()

def runIntegratorTest(title, t, file, integrator) :
    mech = setModelOpts (t)
    pw = mech.reopenPrintStateFile(file)
    mech.setIntegrator (integrator)
    mech.writePrintStateHeader (title)
    doRunStopReset()
    mech.closePrintStateFile()
    return mech;

def runTest(title, t, file) :
    mech = setModelOpts (t)
    pw = mech.reopenPrintStateFile(file)
    mech.writePrintStateHeader (title)
    doRunStopReset()
    mech.closePrintStateFile()
    return mech

def clearFile(file) :
    writer = FileWriter(file)
    writer.close()

# Adjust certain solver settings to ensure repeatable results:
MechSystemSolver.myDefaultHybridSolveP = False
MechSystemBase.setDefaultStabilization (PosStabilization.GlobalMass)
FemModel3d.noIncompressStiffnessDamping = False
SurfaceMeshCollider.useAjlCollision = False
PardisoSolver.setDefaultNumThreads (1)

# Initialize view and clear contents of old output file
main.maskFocusStealing (True)
dataFileName = "basicRegressionTest.out"
clearFile(dataFileName)

# START OF MODELS TO TEST
loadModel ("Spring Mesh")
runIntegratorTest("SpringMesh ForwardEuler", 1, dataFileName, Integrator.ForwardEuler)
runIntegratorTest("SpringMesh RungeKutta", 1, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("SpringMesh BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)

loadModel ("Rigid Body Spring")
runIntegratorTest("RigidBody ForwardEuler", 1.5, dataFileName, Integrator.ForwardEuler)
runIntegratorTest("RigidBody RungeKutta", 1.5, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("RigidBody BackwardEuler", 1.5, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("RigidBody ConstrainedBackwardEuler", 1.5, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("MechModel Demo")
runIntegratorTest("MechModel SymplecticEuler", 2.5, dataFileName, Integrator.SymplecticEuler)
runIntegratorTest("MechModel RungeKutta", 2.5, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("MechModel BackwardEuler", 2.5, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("MechModel ConstrainedBackwardEuler", 2.5, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.mech.MultiSpringDemo")
runIntegratorTest("MultiSpringDemo SymplecticEuler", 1, dataFileName, Integrator.SymplecticEuler)
runIntegratorTest("MultiSpringDemo RungeKutta", 1, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("MultiSpringDemo BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("MultiSpringDemo ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.mech.SegmentedPlaneDemo")
runIntegratorTest("SegmentedPlaneDemo SymplecticEuler", 2, dataFileName, Integrator.SymplecticEuler)
runIntegratorTest("SegmentedPlaneDemo RungeKutta", 2, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("SegmentedPlaneDemo BackwardEuler", 2, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("SegmentedPlaneDemo ConstrainedBackwardEuler", 2, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.ArticulatedFem")
runIntegratorTest("ArticulatedFem BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("ArticulatedFem ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("HexBlock")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
runIntegratorTest("HexBlock BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("HexBlock ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("TetBeam3d")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
prop = root().getProperty("excitation0")
prop.set (0.5)
runIntegratorTest("TetBeam3d BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
prop.set (0.5)
runIntegratorTest("TetBeam3d ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("PlaneConstrainedFem")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
runIntegratorTest("PlaneConstrainedFem BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("PlaneConstrainedFem ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.AttachedBeamDemo")
mech = getMechModel()
fem = mech.findComponent ("models/beam1")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
runIntegratorTest("AttachedBeamDemo BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("AttachedBeamDemo ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.HexBeam3d")
mech = getMechModel()
fem = find ("models/0/models/0");
fem.setMaterial (MooneyRivlinMaterial())
addBreakPoint (0.3)
addBreakPoint (0.6)
addBreakPoint (0.9)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setPrintState("%g")
mech.writePrintStateHeader ("NodalIncompTest ConstrainedBackwardEuler")
doRunStop()
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
doRunStop()
fem.setMaterial (LinearMaterial())
doRunStopReset()
fem.setMaterial (MooneyRivlinMaterial())
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
doRunStopReset()
mech.setIntegrator (Integrator.BackwardEuler)
mech.writePrintStateHeader ("NodalIncompTest BackwardEuler")
doRunStop()
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
doRunStop()
fem.setMaterial (LinearMaterial())
doRunStopReset()
fem.setMaterial (MooneyRivlinMaterial())
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
doRunStopReset()

loadModel ("artisynth.demos.fem.SelfCollision")
runIntegratorTest("SelfCollision BackwardEuler", 0.7, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("SelfCollision ConstrainedBackwardEuler", 0.7, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.FemMuscleDemo")
runIntegratorTest("FemMuscleDemo BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("FemMuscleDemo ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.ViscousBeam")
runIntegratorTest("ViscousBeam BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("ViscousBeam ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.mech.ArticulatedBeamBody")
runIntegratorTest("ArticulatedBeamBody SymplecticEuler", 1, dataFileName, Integrator.SymplecticEuler)

loadModel ("artisynth.demos.mech.BlockTest")
runIntegratorTest("BlockTest SymplecticEuler", 2, dataFileName, Integrator.SymplecticEuler)
runIntegratorTest("BlockTest RungeKutta", 2, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("BlockTest ConstrainedBackwardEuler", 2, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.mech.FrameSpringDemo")
runIntegratorTest("FrameSpring SymplecticEuler", 1, dataFileName, Integrator.SymplecticEuler)
runIntegratorTest("FrameSpring RungeKutta", 1, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("FrameSpring ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("RigidBodyCollision")
runIntegratorTest("RigidBodyCollision SymplecticEuler", 1, dataFileName, Integrator.SymplecticEuler)
runIntegratorTest("RigidBodyCollision RungeKutta", 1, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("RigidBodyCollision BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("RigidBodyCollision ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("LaymanDemo")
runIntegratorTest("LaymanDemo SymplecticEuler", 1.5, dataFileName, Integrator.SymplecticEuler)
runIntegratorTest("LaymanDemo RungeKutta", 1.5, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("LaymanDemo BackwardEuler", 1.5, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("LaymanDemo ConstrainedBackwardEuler", 1.5, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("Fem Collision")
runIntegratorTest("FemCollision BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("FemCollision ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.models.dynjaw.JawLarynxDemo");
# there is an earlier breakpoint at 0.575
runIntegratorTest("JawLarynxDemo SymplecticEuler", 2, dataFileName, Integrator.SymplecticEuler)
runIntegratorTest("JawLarynxDemo RungeKutta", 2, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("JawLarynxDemo Trapezoidal", 2, dataFileName, Integrator.Trapezoidal)

loadModel ("artisynth.models.inversedemos.TongueInvDemo")
runIntegratorTest("TongueInvDemo ConstrainedBackwardEuler", 0.1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.inverse.PointModel2d")
runIntegratorTest("PointInv2d Trapezoidal", 1, dataFileName, Integrator.Trapezoidal)

loadModel ("artisynth.demos.inverse.PointModel3d")
runIntegratorTest("PointInv3d Trapezoidal", 1, dataFileName, Integrator.Trapezoidal)

loadModel ("artisynth.demos.inverse.HydrostatInvDemo")
runIntegratorTest("HydrostatInvDemo Trapezoidal", 1, dataFileName, Integrator.Trapezoidal)

# Exit
main.maskFocusStealing (False)
main.quit()
