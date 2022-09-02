# ArtisynthScript: "contactTest"
#
# For repeatable results, set the environment variable OMP_NUM_THREADS
# to 1
#
# most recent test was run using:
#   1 step of numerical refinement in Pardiso
#   a time step of 0.01 in RigidBodyCollision
global mech

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
#SurfaceMeshCollider.useAjlCollision = True;
PardisoSolver.setDefaultNumThreads (1)
MurtyMechSolver.setDefaultAdaptivelyRebuildA (False)

main.maskFocusStealing (True)
dataFileName = "contactTest.out"
if len(sys.argv) > 0:
   dataFileName = sys.argv[0]

loadModel ("artisynth.demos.mech.MechModelDemo")
mech = setModelOpts (2.5, dataFileName)
pw = mech.openPrintStateFile (dataFileName)
mech.writePrintStateHeader ("MechModel ForwardEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("MechModel RungeKutta");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("MechModel BackwardEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("MechModel ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.mech.MechModelCollide")
mech = setModelOpts (2, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ForwardEuler)
mech.writePrintStateHeader ("MechModelCollide ForwardEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("MechModelCollide SymplecticEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("MechModelCollide RungeKutta");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("MechModelCollide BackwardEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("MechModelCollide ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.mech.RigidBodyCollision")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("RigidBodyCollision SymplecticEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("RigidBodyCollision RungeKutta");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("RigidBodyCollision BackwardEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("RigidBodyCollision ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.mech.RigidCompositeCollide")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("RigidCompositeCollide SymplecticEuler");
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("RigidCompositeCollide ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.mech.LaymanDemo")
mech = setModelOpts (1.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("LaymanDemo SymplecticEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.RungeKutta4)
mech.writePrintStateHeader ("LaymanDemo RungeKutta");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("LaymanDemo BackwardEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("LaymanDemo ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.fem.FemCollision")
mech = setModelOpts (1, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("FemCollision BackwardEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("FemCollision ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.fem.SelfCollision")
mech = setModelOpts (0.7, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.BackwardEuler)
mech.writePrintStateHeader ("SelfCollision BackwardEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("SelfCollision ConstrainedBackwardEuler");
dorun()
loadModel ("artisynth.demos.mech.EmbeddedCollisionTest")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("EmbeddedCollisionTest ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.fem.MultiCollisionTest")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("MultiCollisionTest ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.fem.SkinCollisionTest")
mech = setModelOpts (2.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("SkinCollisionTest ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.test.FemSkinCollide", "-dcon", "-twoFems")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("FemSkinCollide -dcon -twoFems");
dorun()

loadModel ("artisynth.demos.mech.RigidCollisionTest")
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("RigidCollisionTest ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.fem.RedundantCollisionTest")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("RedundantCollisionTest ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.fem.SignedDistanceCollide")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("SignedDistanceCollide");
dorun()

loadModel ("artisynth.demos.fem.SignedDistanceCollide", "-top", "FEM_ELLIPSOID")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("SignedDistanceCollide FEM_ELLIPSOID");
dorun()

loadModel ("artisynth.demos.fem.SignedDistanceCollide", "-top", "DUMBBELL")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("SignedDistanceCollide DUMBBELL");
dorun()

loadModel ("artisynth.demos.mech.RigidVertexCollide");
mech = setModelOpts (0.5, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.writePrintStateHeader ("RigidVertexCollide ConstrainedBackwardEuler");
dorun()
mech.setIntegrator (MechSystemSolver.Integrator.SymplecticEuler)
mech.writePrintStateHeader ("RigidVertexCollide SymplecticEuler");
dorun()

loadModel ("artisynth.demos.fem.EmbeddedEmbeddedCollide")
mech = setModelOpts (0.7, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("EmbeddedEmbeddedCollision ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.fem.EmbeddedEmbeddedCollide", "-friction", "0.0005")
mech = setModelOpts (0.7, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("EmbeddedEmbeddedCollision ConstrainedBackwardEuler -friction 0.0005");
dorun()

loadModel ("artisynth.demos.fem.EdgeEdgeCollisionTest")
mech = setModelOpts (2.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("EdgeEdgeCollisionTest ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.test.Trampoline")
mech = setModelOpts (2.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("Trampoline ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.tutorial.RollingFem")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("RollingFem ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.tutorial.SlidingFem")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("SlidingFem ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.tutorial.FemSelfCollide")
mech = setModelOpts (1.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("FemSelfCollide ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.tutorial.ContactForceMonitor")
mech = setModelOpts (2.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("ContactForceMonitor ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.tutorial.JointedBallCollide")
mech = setModelOpts (2.0, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("JointedBallCollide ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.tutorial.VariableElasticContact")
mech = setModelOpts (0.2, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("VariableElasticContact ConstrainedBackwardEuler");
dorun()

loadModel ("artisynth.demos.tutorial.ElasticFoundationContact")
mech = setModelOpts (0.2, dataFileName)
pw = mech.reopenPrintStateFile (dataFileName)
mech.setIntegrator (MechSystemSolver.Integrator.ConstrainedBackwardEuler)
mech.writePrintStateHeader ("ElasticFoundationContact ConstrainedBackwardEuler");
dorun()

print SurfaceMeshIntersector.numRegularCalls
print SurfaceMeshIntersector.numRobustCalls
print SurfaceMeshIntersector.numClosestIntersectionCalls

main.maskFocusStealing (False)
if main.getMainFrame() == None:
   main.quit()

