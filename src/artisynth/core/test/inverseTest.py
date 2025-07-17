# ArtisynthScript: "inverseTest"
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
# MechSystemSolver.myDefaultHybridSolveP = False
MechSystemBase.setDefaultStabilization (PosStabilization.GlobalMass)
FemModel3d.noIncompressStiffnessDamping = False
SurfaceMeshCollider.useAjlCollision = False
PardisoSolver.setDefaultNumThreads (1)
MurtyMechSolver.setDefaultAdaptivelyRebuildA (False)

main.maskFocusStealing (True)
dataFileName = "inverseTest.out"
loadModel ("artisynth.models.tongue3d.TongueInvDemo")
mech = setModelOpts (0.1, dataFileName)
pw = mech.openPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("TongueInvDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()
# reset to false since it was set by the TongueInvDemo
InverseManager.useLegacyNames = False

loadModel ("artisynth.demos.inverse.FemSurfaceTargetDemo")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("FemSurfaceTargetDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.inverse.ForceTargetDemo")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("ForceTargetDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.inverse.FrameTargetDemo")
mech = setModelOpts (2.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("FrameTargetDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()

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
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("HydrostatInvDemo ConstrainedBackwardEuler");
run()
waitForStop()
reset()
mech.setIntegrator (MechSystemSolver.Integrator.Trapezoidal)
mech.writePrintStateHeader ("HydrostatInvDemo Trapezoidal");
run()
waitForStop()
reset()

loadModel ("artisynth.demos.tutorial.InverseParticle")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("InverseParticle ConstrainedBackwardEuler");
run()
waitForStop()

loadModel ("artisynth.demos.tutorial.InverseSpringForce")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("InverseSpringForce ConstrainedBackwardEuler");
run()
waitForStop()

loadModel ("artisynth.demos.tutorial.InverseMuscleArm")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("InverseMuscleArm ConstrainedBackwardEuler");
run()
waitForStop()

loadModel ("artisynth.demos.tutorial.InverseFrameExciterArm")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("InverseFrameExciterArm ConstrainedBackwardEuler");
run()
waitForStop()

loadModel ("artisynth.demos.tutorial.InverseMuscleFem")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("InverseMuscleFem ConstrainedBackwardEuler");
run()
waitForStop()

main.maskFocusStealing (False)
if main.getMainFrame() == None:
   main.quit()

