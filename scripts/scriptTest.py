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
dataFileName = "scriptTest.out"
clearFile(dataFileName)

# START OF MODELS TO TEST
loadModel ("Spring Mesh")
runIntegratorTest("#SpringMesh ForwardEuler", 1, dataFileName, Integrator.ForwardEuler)
runIntegratorTest("#SpringMesh RungeKutta", 1, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("#SpringMesh BackwardEuler", 1, dataFileName, Integrator.BackwardEuler)

loadModel ("Rigid Body Spring")
runIntegratorTest("#RigidBody ForwardEuler", 1.5, dataFileName, Integrator.ForwardEuler)
runIntegratorTest("#RigidBody RungeKutta", 1.5, dataFileName, Integrator.RungeKutta4)
runIntegratorTest("#RigidBody BackwardEuler", 1.5, dataFileName, Integrator.BackwardEuler)
runIntegratorTest("#RigidBody ConstrainedBackwardEuler", 1.5, dataFileName, Integrator.ConstrainedBackwardEuler)

