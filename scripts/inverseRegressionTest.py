# ArtisynthScript: "BasicRegressionTest"
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
    mech.writePrintStateHeader(title)
    doRunStopReset()
    mech.closePrintStateFile()
    return mech;

def runTest(title, t, file) :
    mech = setModelOpts (t)
    pw = mech.reopenPrintStateFile(file)
    mech.writePrintStateHeader(title)
    doRunStopReset()
    mech.closePrintStateFile()
    return mech

def clearFile(file) :
    writer = FileWriter(file)
    writer.close()

# Initialize view and clear contents of old output file
main.maskFocusStealing (True)
dataFileName = "inverseRegressionTest.out"
clearFile(dataFileName)

# START OF MODELS TO TEST
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
