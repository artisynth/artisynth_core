# ArtisynthScript: "FemRegressionTest"
#
# For repeatable result, enable the following settings:
#
#   -posCorrection GlobalMass
#   -disableHybridSolves
#
# and disable these setting:
#
#  -noIncompressDamping
#  -useAjlCollision
#
# Also, set the environment variable OMP_NUM_THREADS to 1
#
#---------------------------------------------------
#
# most recent test was run using:
#   1 step of numerical refinement in Pardiso
#   a time step of 0.01 in RigidBodyCollision
#   cnt=2 in MechBodySolver projectPositionConstraints

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
    pw.println(title)
    doRunStopReset()
    mech.closePrintStateFile()
    return mech;

def runTest(title, t, file) :
    mech = setModelOpts (t)
    pw = mech.reopenPrintStateFile(file)
    pw.println(title)
    doRunStopReset()
    mech.closePrintStateFile()
    return mech

def clearFile(file) :
    writer = FileWriter(file)
    writer.close()

# Initialize view and clear contents of old output file
main.maskFocusStealing (True)
dataFileName = "femRegressionTest.out"
clearFile(dataFileName)

# START OF MODELS TO TEST
loadModel ("artisynth.demos.fem.ArticulatedFem")
runIntegratorTest("#ArticulatedFem BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("#ArticulatedFem ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("TetBeam3d")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
prop = root().getProperty("excitation0")
prop.set (0.5)
runIntegratorTest("#TetBeam3d BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
prop.set (0.5)
runIntegratorTest("#TetBeam3d ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("PyramidBeam3d")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
prop = root().getProperty("excitation0")
prop.set (0.5)
runIntegratorTest("#PyramidBeam3d BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
prop.set (0.5)
runIntegratorTest("#PyramidBeam3d ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("WedgeBeam3d")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
prop = root().getProperty("excitation0")
prop.set (0.5)
runIntegratorTest("#WedgeBeam3d BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
prop.set (0.5)
runIntegratorTest("#WedgeBeam3d ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("PlaneConstrainedFem")
mech = getMechModel()
fem = mech.findComponent ("models/fem")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
runIntegratorTest("#PlaneConstrainedFem BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("#PlaneConstrainedFem ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.HexBeam3d")
mech = getMechModel()
fem = find ("models/0/models/0");
fem.setMaterial (MooneyRivlinMaterial())
addBreakPoint (0.3)
addBreakPoint (0.6)
addBreakPoint (0.9)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setPrintState("%g")
pw.println ("#NodalIncompTest ConstrainedBackwardEuler")
doRunStop()
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
doRunStop()
fem.setMaterial (LinearMaterial())
doRunStopReset()
fem.setMaterial (MooneyRivlinMaterial())
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
doRunStopReset()
mech.setIntegrator (Integrator.BackwardEuler)
pw.println ("#NodalIncompTest BackwardEuler")
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
pw.println ("#FemMaterial Test")
mech.setPrintState("%g")
pw.println ("#FemMaterial Test MooneyRivlinMaterial")
fem.setMaterial(MooneyRivlinMaterial())
doRunStopReset()
pw.println ("#FemMaterial Test MooneyRivlinMaterial 1037 486")
fem.setMaterial(MooneyRivlinMaterial (1037, 0, 0, 486, 0, 10370))
doRunStopReset()
pw.println ("#FemMaterial Test LinearMaterial")
fem.setMaterial (LinearMaterial())
doRunStopReset()
pw.println ("#FemMaterial Test LinearMaterial 7000 0.49 corotated")
fem.setMaterial (LinearMaterial(7000, 0.49, True))
doRunStopReset()
pw.println ("#FemMaterial Test LinearMaterial 7000 0.49")
fem.setMaterial (LinearMaterial(7000, 0.49, False))
doRunStopReset()
pw.println ("#FemMaterial Test LinearMaterial 6912 0.333 corotated")
fem.setMaterial (LinearMaterial(6912, 0.333, True))
doRunStopReset()
pw.println ("#FemMaterial Test LinearMaterial 6912 0.333")
fem.setMaterial (LinearMaterial(6912, 0.333, False))
doRunStopReset()
pw.println ("#FemMaterial Test CubicHyperelastic")
fem.setMaterial (CubicHyperelastic())
doRunStopReset()
pw.println ("#FemMaterial Test FungMaterial")
fem.setMaterial (FungMaterial())
doRunStopReset()
pw.println ("#FemMaterial Test OgdenMaterial")
fem.setMaterial (OgdenMaterial())
doRunStopReset()
pw.println ("#FemMaterial Test NeoHookeanMaterial")
fem.setMaterial (NeoHookeanMaterial())
doRunStopReset()
pw.println ("#FemMaterial Test IncompNeoHookeanMaterial")
fem.setMaterial (IncompNeoHookeanMaterial())
doRunStopReset()
pw.println ("#FemMaterial Test StVenantKirchoffMaterial")
fem.setMaterial (StVenantKirchoffMaterial())
doRunStop()

loadModel ("artisynth.demos.fem.AttachedBeamDemo")
mech = getMechModel()
fem = mech.findComponent ("models/beam1")
fem.setIncompressible (FemModel.IncompMethod.AUTO)
runIntegratorTest("#AttachedBeamDemo BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("#AttachedBeamDemo ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.SelfCollision")
runIntegratorTest("#SelfCollision BackwardEuler", 0.7, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("#SelfCollision ConstrainedBackwardEuler", 0.7, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.FemMuscleDemo")
runIntegratorTest("#FemMuscleDemo BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("#FemMuscleDemo ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.fem.ViscousBeam")
runIntegratorTest("#ViscousBeam BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("#ViscousBeam ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("Fem Collision")
runIntegratorTest("#FemCollision BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("#FemCollision ConstrainedBackwardEuler", 1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.models.inversedemos.TongueInvDemo")
runIntegratorTest("#TongueInvDemo ConstrainedBackwardEuler", 0.1, dataFileName, Integrator.ConstrainedBackwardEuler)

loadModel ("artisynth.demos.inverse.HydrostatInvDemo")
runIntegratorTest("#HydrostatInvDemo Trapezoidal", 1, dataFileName, Integrator.Trapezoidal)

# Exit
main.maskFocusStealing (False)
main.quit()
