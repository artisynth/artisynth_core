# ArtisynthScript: "wraptest"
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
from artisynth.demos.wrapping import StaticWrapTest
StaticWrapTest.highMeshRes = False;
StaticWrapTest.highGridRes = False;
PardisoSolver.setDefaultNumThreads (1)
MurtyMechSolver.setDefaultAdaptivelyRebuildA (False)

main.maskFocusStealing (True)
dataFileName = "wraptest.out"

loadModel ("artisynth.demos.tutorial.CylinderWrapping")
mech = setModelOpts (1.5, dataFileName)
pw = mech.openPrintStateFile (dataFileName)
mech.writePrintStateHeader ("CylinderWrapping")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.tutorial.TorusWrapping")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("TorusWrapping")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.tutorial.PhalanxWrapping")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("PhalanxWrapping")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.WrappedMuscleArm")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("WrappedMuscleArm")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.test.TorusWrapTest")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("TorusWrapTest")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "CYLINDER")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("DynamicWrapTest CYLINDER")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "SPHERE")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("DynamicWrapTest SPHERE")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "ELLIPSOID")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("DynamicWrapTest ELLIPSOID")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "TORUS")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("DynamicWrapTest TORUS")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.wrapping.DynamicWrapTest",
           "-geo", "CYLINDER", "-grid")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("DynamicWrapTest CYLINDER GRID")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.wrapping.DynamicWrapTest",
           "-geo", "SPHERE", "-grid")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("DynamicWrapTest SPHERE GRID")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.wrapping.DynamicWrapTest", 
           "-geo", "ELLIPSOID", "-grid")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("DynamicWrapTest ELLIPSOID GRID")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.wrapping.DynamicWrapTest",
           "-geo", "TORUS", "-grid")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("DynamicWrapTest TORUS GRID")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "TALUS")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("DynamicWrapTest TALUS")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.wrapping.DynamicWrapTest", "-geo", "PHALANX")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("DynamicWrapTest PHALANX")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.ConditionalMarkerDemo")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("ConditionalMarkerDemo")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.mech.ConditionalMarkerDemo", "-wrapping")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("ConditionalMarkerDemo wrapping")
run()
waitForStop()
reset()

main.maskFocusStealing (False)
if main.getMainFrame() == None:
   main.quit()

