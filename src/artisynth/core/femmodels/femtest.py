# ArtisynthScript: "femtest"
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
    fem = find ("models/0/models/0")
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
dataFileName = "femtest.out"
clearFile(dataFileName)

# START OF MODELS TO TEST
loadModel ("artisynth.demos.fem.LeafDemo")
fem = getFemModel()
linMat = LinearMaterial()
linMat.setCorotated(False)
fem.setMaterial (linMat)
runTest ("LeafDemo linear", 1.0, dataFileName)
fem.setMaterial (LinearMaterial())
runTest ("LeafDemo corotated linear", 1.0, dataFileName)
fem.setMaterial (MooneyRivlinMaterial())
runTest ("LeafDemo MooneyRivlin", 1.0, dataFileName)
fem.setMaterial (NeoHookeanMaterial())
runTest ("LeafDemo NeoHookean", 1.0, dataFileName)

loadModel ("artisynth.demos.fem.ShellTriPatch")
fem = getFemModel()
linMat = LinearMaterial()
linMat.setCorotated(False)
fem.setMaterial (linMat)
runTest ("ShellTriPatch linear", 0.5, dataFileName)
fem.setMaterial (LinearMaterial())
runTest ("ShellTriPatch corotated linear", 0.5, dataFileName)
fem.setMaterial (MooneyRivlinMaterial())
runTest ("ShellTriPatch MooneyRivlin", 0.5, dataFileName)
fem.setMaterial (NeoHookeanMaterial())
runTest ("ShellTriPatch NeoHookean", 0.5, dataFileName)

loadModel ("artisynth.demos.fem.ShellTriPatch", "-membrane")
fem = getFemModel()
linMat = LinearMaterial()
linMat.setCorotated(False)
fem.setMaterial (linMat)
runTest ("ShellTriPatch membrane linear", 0.5, dataFileName)
fem.setMaterial (LinearMaterial())
runTest ("ShellTriPatch membrane corotated linear", 0.5, dataFileName)
fem.setMaterial (MooneyRivlinMaterial())
runTest ("ShellTriPatch membrane MooneyRivlin", 0.5, dataFileName)
fem.setMaterial (NeoHookeanMaterial())
runTest ("ShellTriPatch membrane NeoHookean", 0.5, dataFileName)

loadModel ("artisynth.demos.fem.ShellQuadPatch")
fem = getFemModel()
linMat = LinearMaterial()
linMat.setCorotated(False)
fem.setMaterial (linMat)
runTest ("ShellQuadPatch linear", 0.5, dataFileName)
fem.setMaterial (LinearMaterial())
runTest ("ShellQuadPatch corotated linear", 0.5, dataFileName)
fem.setMaterial (MooneyRivlinMaterial())
runTest ("ShellQuadPatch MooneyRivlin", 0.5, dataFileName)
fem.setMaterial (NeoHookeanMaterial())
runTest ("ShellQuadPatch NeoHookean", 0.5, dataFileName)

loadModel ("artisynth.demos.fem.ShellQuadPatch", "-membrane")
fem = getFemModel()
linMat = LinearMaterial()
linMat.setCorotated(False)
fem.setMaterial (linMat)
runTest ("ShellQuadPatch membrane linear", 0.5, dataFileName)
fem.setMaterial (LinearMaterial())
runTest ("ShellQuadPatch membrane corotated linear", 0.5, dataFileName)
fem.setMaterial (MooneyRivlinMaterial())
runTest ("ShellQuadPatch membrane MooneyRivlin", 0.5, dataFileName)
fem.setMaterial (NeoHookeanMaterial())
runTest ("ShellQuadPatch membrane NeoHookean", 0.5, dataFileName)

loadModel ("artisynth.demos.fem.ShellBlock")
fem = getFemModel()
linMat = LinearMaterial()
linMat.setCorotated(False)
fem.setMaterial (linMat)
runTest ("ShellBlock linear", 0.25, dataFileName)
fem.setMaterial (LinearMaterial())
runTest ("ShellBlock corotated linear", 0.25, dataFileName)
fem.setMaterial (MooneyRivlinMaterial())
runTest ("ShellBlock MooneyRivlin", 0.25, dataFileName)

loadModel ("artisynth.demos.fem.ShellBlock", "-membrane")
fem = getFemModel()
linMat = LinearMaterial()
linMat.setCorotated(False)
fem.setMaterial (linMat)
runTest ("ShellBlock membrane linear", 0.25, dataFileName)
fem.setMaterial (LinearMaterial())
runTest ("ShellBlock membrane corotated linear", 0.25, dataFileName)
fem.setMaterial (MooneyRivlinMaterial())
runTest ("ShellBlock membrane MooneyRivlin", 0.25, dataFileName)

loadModel ("artisynth.demos.fem.CombinedShellFem")
runTest ("CombinedShellFem", 0.5, dataFileName)

loadModel ("artisynth.demos.fem.TetCube")
fem = find ("models/mech/models/tet")
fem.setSoftIncompMethod (FemModel3d.IncompMethod.ELEMENT)
runTest ("TetBlock SoftIncomp=ELEMENT", 0.3, dataFileName)
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
runTest ("TetBlock SoftIncomp=NODAL", 0.3, dataFileName)
fem.setIncompressible (FemModel3d.IncompMethod.NODAL)
fem.setMaterial (MooneyRivlinMaterial(2000, 0, 0, 0, 0, 50000))
runTest ("TetBlock HardIncomp=NODAL", 0.3, dataFileName)
fem.setIncompressible (FemModel3d.IncompMethod.NODAL)
fem.setMaterial (LinearMaterial (5000, 0.33))
runTest ("TetBlock HardIncomp=NODAL Linear", 0.3, dataFileName)

loadModel ("artisynth.demos.fem.HexCube")
fem = find ("models/mech/models/hex")
runTest ("HexBlock SoftIncomp=ELEMENT", 0.3, dataFileName)
fem.setSoftIncompMethod (FemModel3d.IncompMethod.NODAL)
runTest ("HexBlock SoftIncomp=NODAL", 0.3, dataFileName)
fem.setIncompressible (FemModel3d.IncompMethod.NODAL)
fem.setMaterial (MooneyRivlinMaterial(2000, 0, 0, 0, 0, 50000))
runTest ("HexBlock HardIncomp=NODAL", 0.3, dataFileName)
fem.setIncompressible (FemModel3d.IncompMethod.NODAL)
fem.setMaterial (LinearMaterial (5000, 0.33))
runTest ("HexBlock HardIncomp=NODAL Linear", 0.3, dataFileName)
fem.setIncompressible (FemModel3d.IncompMethod.ELEMENT)
fem.setMaterial (MooneyRivlinMaterial(2000, 0, 0, 0, 0, 50000))
runTest ("HexBlock HardIncomp=ELEMENT", 0.3, dataFileName)
fem.setIncompressible (FemModel3d.IncompMethod.ELEMENT)
fem.setMaterial (LinearMaterial (5000, 0.33))
runTest ("HexBlock HardIncomp=ELEMENT Linear", 0.3, dataFileName)

loadModel ("artisynth.demos.test.ShellVolumeAttach")
runTest ("ShellVolumeAttach", 0.2, dataFileName)
loadModel ("artisynth.demos.test.ShellVolumeAttach", "-membrane")
runTest ("ShellVolumeAttach membrane", 0.2, dataFileName)
loadModel ("artisynth.demos.test.ShellShellAttach")
runTest ("ShellShellAttach", 0.2, dataFileName)
loadModel ("artisynth.demos.test.ShellShellAttach", "-membrane1")
runTest ("ShellShellAttach membrane1", 0.2, dataFileName)
loadModel ("artisynth.demos.test.ShellShellAttach", "-membrane2")
runTest ("ShellShellAttach membrane2", 0.2, dataFileName)
loadModel ("artisynth.demos.test.ShellShellAttach", "-membrane1", "-membrane2")
runTest ("ShellShellAttach membrane1 membrane2", 0.2, dataFileName)

# Exit
main.maskFocusStealing (False)
if main.getMainFrame() == None:
   main.quit()

