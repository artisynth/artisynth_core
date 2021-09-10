# ArtisynthScript: "skinningTest"
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
dataFileName = "skinningTest.out"
loadModel ("artisynth.demos.fem.FemSkinDemo")
mech = setModelOpts (0.5, dataFileName)
pw = mech.openPrintStateFile (dataFileName)
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
run()
waitForStop()
reset()

loadModel ("artisynth.demos.tutorial.AllBodySkinning")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("AllBodySkinning ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.tutorial.PhalanxSkinWrapping")
mech = setModelOpts (0.2, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
skin = mech.meshBodies().get(0);
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("PhalanxSkinWrapping DUAL_QUATERNION blending")
run()
waitForStop()
reset()
skin.setFrameBlending(SkinMeshBody.FrameBlending.LINEAR)
mech.writePrintStateHeader ("PhalanxSkinWrapping LINEAR blending")
run()
waitForStop()
reset()

loadModel ("artisynth.demos.tutorial.SkinBodyCollide")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("SkinBodyCollide ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.test.ActuatedSkinning")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("ActuatedSkinning LINEAR blending");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.test.ActuatedSkinning", "-dq")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("ActuatedSkinning DUAL_QUATERNION blending");
run()
waitForStop()
reset()

main.maskFocusStealing (False)
if main.getMainFrame() == None:
   main.quit()

