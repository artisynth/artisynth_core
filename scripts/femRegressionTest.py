# ArtisynthScript: "FemRegressionTest"
#
#  For repeatable results, set the environment variable OMP_NUM_THREADS
# to 1
#

# IMPORTS
from artisynth.core.mechmodels.MechSystemSolver import Integrator

def getMechModel() :
    mech = find ("models/0")
    return mech

def getFemModel() :
    fem = find ("models/0/models/fem")
    return fem

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
    System.out.println(title)
    doRunStopReset()
    mech.closePrintStateFile()
    return mech;

def runTest(title, t, file) :
    mech = setModelOpts (t)
    pw = mech.reopenPrintStateFile(file)
    mech.writePrintStateHeader (title)
    System.out.println(title)
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
dataFileName = "femRegressionTest.out"
clearFile(dataFileName)

# START OF MODELS TO TEST
loadModel ("artisynth.demos.fem.ArticulatedFem")
runIntegratorTest("ArticulatedFem BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("ArticulatedFem ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.TetBeam3d")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
prop = root().getProperty("excitation0")
prop.set (0.5)
runIntegratorTest("TetBeam3d BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
prop.set (0.5)
runIntegratorTest("TetBeam3d ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.PyramidBeam3d")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
prop = root().getProperty("excitation0")
prop.set (0.5)
runIntegratorTest("PyramidBeam3d BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
prop.set (0.5)
runIntegratorTest("PyramidBeam3d ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.WedgeBeam3d")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
prop = root().getProperty("excitation0")
prop.set (0.5)
runIntegratorTest("WedgeBeam3d BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
prop.set (0.5)
runIntegratorTest("WedgeBeam3d ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.PlaneConstrainedFem")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
runIntegratorTest("PlaneConstrainedFem BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("PlaneConstrainedFem ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

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

loadModel ("artisynth.demos.fem.HexBeam3d")
mech = getMechModel()
fem = find ("models/0/models/0");
addBreakPoint (1.0)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("FemMaterial Test")
mech.setPrintState("%g")
mech.writePrintStateHeader ("FemMaterial Test MooneyRivlinMaterial")
fem.setMaterial(MooneyRivlinMaterial())
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test MooneyRivlinMaterial 1037 486")
fem.setMaterial(MooneyRivlinMaterial (1037, 0, 0, 486, 0, 10370))
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test LinearMaterial")
fem.setMaterial (LinearMaterial())
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test LinearMaterial 7000 0.49 corotated")
fem.setMaterial (LinearMaterial(7000, 0.49, True))
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test LinearMaterial 7000 0.49")
fem.setMaterial (LinearMaterial(7000, 0.49, False))
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test LinearMaterial 6912 0.333 corotated")
fem.setMaterial (LinearMaterial(6912, 0.333, True))
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test LinearMaterial 6912 0.333")
fem.setMaterial (LinearMaterial(6912, 0.333, False))
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test CubicHyperelastic")
fem.setMaterial (CubicHyperelastic())
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test FungMaterial")
fem.setMaterial (FungMaterial())
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test OgdenMaterial")
fem.setMaterial (OgdenMaterial())
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test NeoHookeanMaterial")
fem.setMaterial (NeoHookeanMaterial())
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test IncompNeoHookeanMaterial")
fem.setMaterial (IncompNeoHookeanMaterial())
doRunStopReset()
mech.writePrintStateHeader ("FemMaterial Test StVenantKirchoffMaterial")
fem.setMaterial (StVenantKirchoffMaterial())
doRunStop()

loadModel ("artisynth.demos.fem.AttachedBeamDemo")
mech = getMechModel()
fem = mech.findComponent ("models/beam1")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
runIntegratorTest("AttachedBeamDemo BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("AttachedBeamDemo ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.SelfCollision")
runIntegratorTest("SelfCollision BackwardEuler", 0.7, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("SelfCollision ConstrainedBackwardEuler", 0.7, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.FemMuscleDemo")
runIntegratorTest("FemMuscleDemo BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("FemMuscleDemo ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.ViscousBeam")
runIntegratorTest("ViscousBeam BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("ViscousBeam ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.FemCollision")
runIntegratorTest("FemCollision BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("FemCollision ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.models.inversedemos.TongueInvDemo")
runIntegratorTest("TongueInvDemo ConstrainedBackwardEuler", 0.1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.inverse.HydrostatInvDemo")
runIntegratorTest("HydrostatInvDemo Trapezoidal", 1, dataFileName, Integrator.Trapezoidal)

# Quadratic element tests
loadModel ("artisynth.demos.fem.QuadhexBeam3d")
fem = getFemModel()
fem.setMaterial (LinearMaterial())
fem.setIncompressible (FemModel.IncompMethod.OFF)
fem.setSoftIncompMethod (FemModel.IncompMethod.ELEMENT)
runTest("QuadhexBeam3d Linear imcomp: OFF, ELEMENT", 0.5, dataFileName)
fem.setIncompressible (FemModel.IncompMethod.ELEMENT)
runTest("QuadhexBeam3d Linear imcomp: ELEMENT, ELEMENT", 0.5, dataFileName)
fem.setMaterial (MooneyRivlinMaterial())
fem.setIncompressible (FemModel.IncompMethod.OFF)
runTest("QuadhexBeam3d MooneyRivlin imcomp: OFF, ELEMENT", 0.5, dataFileName)
fem.setIncompressible (FemModel.IncompMethod.ELEMENT)
runTest("QuadhexBeam3d MooneyRivlin imcomp: ELEMENT, ELEMENT", 0.5, dataFileName)
fem.setSoftIncompMethod (FemModel.IncompMethod.FULL)
runTest("QuadhexBeam3d MooneyRivlin imcomp: ELEMENT, FULL", 0.5, dataFileName)

loadModel ("artisynth.demos.fem.QuadtetBeam3d")
fem = getFemModel()
fem.setMaterial (LinearMaterial())
fem.setIncompressible (FemModel.IncompMethod.OFF)
fem.setSoftIncompMethod (FemModel.IncompMethod.ELEMENT)
runTest("QuadtetBeam3d Linear imcomp: OFF, ELEMENT", 0.5, dataFileName)
fem.setIncompressible (FemModel.IncompMethod.ELEMENT)
runTest("QuadtetBeam3d Linear imcomp: ELEMENT, ELEMENT", 0.5, dataFileName)
fem.setMaterial (MooneyRivlinMaterial())
fem.setIncompressible (FemModel.IncompMethod.OFF)
runTest("QuadtetBeam3d MooneyRivlin imcomp: OFF, ELEMENT", 0.5, dataFileName)
fem.setIncompressible (FemModel.IncompMethod.ELEMENT)
runTest("QuadtetBeam3d MooneyRivlin imcomp: ELEMENT, ELEMENT", 0.5, dataFileName)
fem.setSoftIncompMethod (FemModel.IncompMethod.FULL)
runTest("QuadtetBeam3d MooneyRivlin imcomp: ELEMENT, FULL", 0.5, dataFileName)

loadModel ("artisynth.demos.fem.QuadwedgeBeam3d")
fem = getFemModel()
fem.setMaterial (LinearMaterial())
fem.setIncompressible (FemModel.IncompMethod.OFF)
fem.setSoftIncompMethod (FemModel.IncompMethod.ELEMENT)
runTest("QuadwedgeBeam3d Linear imcomp: OFF, ELEMENT", 0.5, dataFileName)
fem.setIncompressible (FemModel.IncompMethod.ELEMENT)
runTest("QuadwedgeBeam3d Linear imcomp: ELEMENT, ELEMENT", 0.5, dataFileName)
fem.setMaterial (MooneyRivlinMaterial())
fem.setIncompressible (FemModel.IncompMethod.OFF)
runTest("QuadwedgeBeam3d MooneyRivlin imcomp: OFF, ELEMENT", 0.5, dataFileName)
fem.setIncompressible (FemModel.IncompMethod.ELEMENT)
runTest("QuadwedgeBeam3d MooneyRivlin imcomp: ELEMENT, ELEMENT", 0.5, dataFileName)
fem.setSoftIncompMethod (FemModel.IncompMethod.FULL)
runTest("QuadwedgeBeam3d MooneyRivlin imcomp: ELEMENT, FULL", 0.5, dataFileName)

loadModel ("artisynth.demos.test.FrameFemConstraintTest")
runTest("FrameFemConstraintTest", 0.5, dataFileName)

# Exit
main.maskFocusStealing (False)
main.quit()

