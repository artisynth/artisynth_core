# ArtisynthScript: "stateTest"
#
# At present, extra state is contained by models with
#
# * contact
# * constraints (e.g., joint limits, impulses)
# * wrapping
# * Jaw
# * viscositys
# * muscles with aux state (myAuxStateMat != null)
#

# test types:

D = 1 # test model using default integrator
S = 2 # test model using stiff integrator
A = 3 # test model using all integrators

from artisynth.core.mechmodels.MechSystemSolver import Integrator

stiffIntegrators = [ Integrator.ConstrainedBackwardEuler ]
stiffIntegrators.append (Integrator.BackwardEuler)
stiffIntegrators.append (Integrator.Trapezoidal)

allIntegrators = [ Integrator.ForwardEuler ]
allIntegrators.append (Integrator.SymplecticEuler)
allIntegrators.append (Integrator.RungeKutta4)
allIntegrators.extend (stiffIntegrators)

def getRootState() :
    return root().getState(True)

def statesEqual (state0, state1) :
    errMsg = StringBuilder()
    if not state0.equals (state1, errMsg) :
        print errMsg
        return False
    else :
        return True

def defaultTest (tsim, nway) :
    for k in range(1,nway+1) :
        run()
        waitForStop()
        saveState = getRootState()
        way = getWayPoint (k*tsim/float(nway))
        if not statesEqual (way.getState(), saveState) :
            print str(getTime()) + " FAILED: way state != root state"
        TestCommands.testSaveLoadState (saveState)
        rewind()
        run()
        waitForStop() # check that new state is same as saved state
        if statesEqual (saveState, getRootState()) :
            print str(getTime()) + " OK"
        else :
            print str(getTime()) + " FAILED"

def integratorTest (tsim, nway, integratorList) :
    mech = find ("models/0")
    for integrator in integratorList :
        mech.setIntegrator (integrator)
        reset()
        for k in range(1,nway+1) :
           run()
           waitForStop()
           saveState = getRootState()
           way = getWayPoint (k*tsim/float(nway))
           if not statesEqual (way.getState(), saveState) :
                print str(getTime()) + " FAILED: way state != root state"
           rewind()
           run()
           waitForStop() # check that new state is same as saved state
           if statesEqual (saveState, getRootState()) :
               print str(integrator) + " " + str(getTime()) + " OK"
           else :
               print str(integrator) + " " + str(getTime()) + " FAILED"

def testState (testType, tsim, nway, modelName, *args) :
    if loadModel (modelName, *args) == False:
        print "Model %s not found" % modelName
        return
    if setCompliantContactForImplicitFriction:
        # need to set compliant contact if using implicit friction
        mech = find ("models/0")
        if mech.getUseImplicitFriction():
            mech.setCompliantContact()
    delay (0.01)
    clearWayPoints()
    for k in range(1,nway+1) :
        addBreakPoint (k*tsim/float(nway))
    defaultTest (tsim, nway)
    if testType == S :
       integratorTest (tsim, nway, stiffIntegrators)
    elif testType == A :
       integratorTest (tsim, nway, allIntegrators)

# Adjust certain solver settings to ensure repeatable results:
MechSystemSolver.myDefaultHybridSolveP = False
MechSystemBase.setDefaultStabilization (PosStabilization.GlobalMass)
FemModel3d.noIncompressStiffnessDamping = False
SurfaceMeshCollider.useAjlCollision = True
PardisoSolver.setDefaultNumThreads (1)
MurtyMechSolver.setDefaultAdaptivelyRebuildA(False)
#MechSystemSolver.setAlwaysAnalyze (True)

# specifies if we need to explicitly set compliant contact with implicit friction
setCompliantContactForImplicitFriction = False

main.maskFocusStealing (True)
testState (A, 1.0,  5,"artisynth.demos.mech.SpringMeshDemo")
testState (A, 1.0,  5,"artisynth.demos.mech.RigidBodyDemo")
testState (A, 2.0, 10,"artisynth.demos.mech.MechModelDemo")
testState (A, 1.0,  5,"artisynth.demos.mech.MechModelCollide")
testState (D, 1.0,  5,"artisynth.demos.mech.MultiSpringDemo")
testState (D, 1.0,  5,"artisynth.demos.mech.SegmentedPlaneDemo")
testState (D, 1.0, 5, "artisynth.demos.mech.HingeJointDemo")
testState (D, 1.0, 5, "artisynth.demos.mech.SliderJointDemo")
testState (D, 1.0, 5, "artisynth.demos.mech.CylindricalJointDemo")
testState (D, 1.0, 5, "artisynth.demos.mech.SlottedHingeJointDemo")
testState (D, 1.0, 5, "artisynth.demos.mech.UniversalJointDemo")
testState (D, 1.0, 5, "artisynth.demos.mech.SphericalJointDemo")
testState (D, 1.0, 5, "artisynth.demos.mech.GimbalJointDemo")
testState (D, 1.0, 5, "artisynth.demos.mech.PlanarJointDemo")
testState (D, 1.0, 5, "artisynth.demos.mech.PlanarTranslationJointDemo")
testState (D, 1.0,  5,"artisynth.demos.mech.ConstrainedParticle")
testState (D, 1.0,  5,"artisynth.demos.mech.BlockTest")
testState (D, 1.0,  5,"artisynth.demos.mech.FrameSpringDemo")
testState (A, 1.0,  5,"artisynth.demos.mech.RigidBodyCollision")
testState (D, 2.0, 10,"artisynth.demos.mech.RigidCompositeCollide")
testState (D, 1.0,  5,"artisynth.demos.mech.LaymanDemo")
testState (A, 1.0,  5,"artisynth.demos.mech.WrappedMuscleArm")
testState (D, 1.0,  5,"artisynth.demos.mech.SkinDemo")

testState (A, 1.0,  5,"artisynth.demos.mech.ArticulatedBeamBody")
testState (D, 1.0,  5,"artisynth.demos.mech.AttachedBeamBody")
testState (A, 1.0,  5,"artisynth.demos.mech.BeamBodyCollide")
testState (A, 1.3, 10,"artisynth.demos.mech.ConditionalMarkerDemo")
testState (A, 1.3, 10,"artisynth.demos.mech.ConditionalMarkerDemo", "-wrapping")
testState (A, 1.0, 10,"artisynth.demos.mech.CoordinateCouplingDemo")
testState (A, 1.0, 10,"artisynth.demos.mech.JointLimitDemo")

testState (S, 1.0,  5,"artisynth.demos.fem.ArticulatedFem")
testState (D, 1.0,  5,"artisynth.demos.fem.AttachDemo")
testState (S, 1.0,  5,"artisynth.demos.fem.AttachedBeamDemo")
testState (S, 1.0,  5,"artisynth.demos.fem.CombinedShellFem")
testState (S, 1.0,  5,"artisynth.demos.fem.CombinedElemFem")
testState (S, 2.0, 10,"artisynth.demos.fem.FemCollision")
testState (D, 1.0,  5,"artisynth.demos.fem.FemMuscleArm")
testState (S, 1.0,  5,"artisynth.demos.fem.FemMuscleDemo")
testState (D, 2.0, 10,"artisynth.demos.fem.FemPlaneCollide")
testState (D, 1.0,  5,"artisynth.demos.fem.FemSkinDemo")
testState (D, 1.0,  5,"artisynth.demos.fem.Hex3dBlock")
testState (S, 1.0,  5,"artisynth.demos.fem.LeafDemo")
testState (S, 1.0,  5,"artisynth.demos.fem.PlaneConstrainedFem")
testState (D, 2.0, 10,"artisynth.demos.fem.SelfCollision")
testState (D, 1.0,  5,"artisynth.demos.fem.ShellTriPatch")
testState (D, 1.0,  5,"artisynth.demos.fem.ShellTriPatch", "-membrane")
testState (D, 1.0,  5,"artisynth.demos.fem.ShellQuadPatch")
testState (D, 1.0,  5,"artisynth.demos.fem.ShellQuadPatch", "-membrane")
testState (D, 1.0,  5,"artisynth.demos.fem.ShellBlock")
testState (D, 1.0,  5,"artisynth.demos.fem.ShellBlock", "-membrane")
testState (D, 1.0,  5,"artisynth.demos.fem.TetBeam3d")
testState (S, 1.0,  5,"artisynth.demos.fem.ViscousBeam")
testState (A, 0.5,  5,"artisynth.demos.fem.SignedDistanceCollide")
testState (S, 0.5,  5,"artisynth.demos.fem.SignedDistanceCollide", "-top", "FEM_ELLIPSOID")
testState (A, 0.5,  5,"artisynth.demos.fem.SignedDistanceCollide", "-top", "DUMBBELL")
testState (D, 2.0, 10,"artisynth.demos.fem.SkinCollisionTest")

testState (D, 1.0,  5,"artisynth.models.alanMasseter.MasseterM16462John")
testState (D, 1.0,  5,"artisynth.models.phuman.SimpleJointedArm")
testState (D, 1.0,  5,"artisynth.models.tongue3d.HexTongueDemo", "-exciter", "GGP")
testState (D, 1.0,  5,"artisynth.models.tongue3d.FemMuscleTongueDemo")

testState (A, 1.0,  5,"artisynth.models.dynjaw.JawLarynxDemo")
# JawDemo needs delay since rendering sets Jaw textureProps.enabled=false
testState (D, 1.0,  5,"artisynth.models.dynjaw.JawDemo")
testState (D, 1.0,  5,"artisynth.models.dangTongue.FemTongueDemo")

testState (A, 1.0,  5,"artisynth.demos.wrapping.DynamicWrapTest", "-geo", "CYLINDER")
testState (D, 1.0,  5,"artisynth.demos.wrapping.DynamicWrapTest", "-geo", "SPHERE")
testState (D, 1.0,  5,"artisynth.demos.wrapping.DynamicWrapTest", "-geo", "ELLIPSOID")
testState (D, 1.0,  5,"artisynth.demos.wrapping.DynamicWrapTest", "-geo", "TORUS")
testState (D, 1.0,  5,"artisynth.demos.wrapping.DynamicWrapTest", "-geo", "PHALANX")

testState (D, 2.0, 10,"artisynth.demos.tutorial.CylinderWrapping")
testState (D, 1.0,  5,"artisynth.demos.tutorial.FemBeam")
#testState (D, 1.0,  5,"artisynth.demos.tutorial.FemBeamColored") # member variables
testState (D, 1.0,  5,"artisynth.demos.tutorial.FemBeamWithBlock")
testState (D, 1.0,  5,"artisynth.demos.tutorial.FemBeamWithFemSphere")
testState (D, 1.0,  5,"artisynth.demos.tutorial.FemBeamWithMuscle")
#testState (D, 1.0,  5,"artisynth.demos.tutorial.FemCollisions")
testState (D, 1.0,  5,"artisynth.demos.tutorial.FemEmbeddedSphere")
testState (D, 1.0,  5,"artisynth.demos.tutorial.FemMuscleBeams")
testState (D, 1.0,  5,"artisynth.demos.tutorial.FrameBodyAttachment")
testState (D, 1.0,  5,"artisynth.demos.tutorial.FrameFemAttachment")
testState (D, 1.0,  5,"artisynth.demos.tutorial.JointedFemBeams")
testState (D, 1.0,  5,"artisynth.demos.tutorial.LumbarFrameSpring")
testState (D, 1.0,  5,"artisynth.demos.tutorial.NetDemo")
testState (D, 1.0,  5,"artisynth.demos.tutorial.NetDemoWithPan")
testState (D, 1.0,  5,"artisynth.demos.tutorial.NetDemoWithRefs")
testState (D, 1.0,  5,"artisynth.demos.tutorial.ParticleAttachment")
testState (D, 1.0,  5,"artisynth.demos.tutorial.ParticleSpring")
testState (D, 1.0,  5,"artisynth.demos.tutorial.PenetrationRender")
testState (D, 1.0,  5,"artisynth.demos.tutorial.PhalanxWrapping")
testState (D, 1.0,  5,"artisynth.demos.tutorial.PointFemAttachment")
testState (D, 1.0,  5,"artisynth.demos.tutorial.RigidBodyJoint")
testState (D, 1.0,  5,"artisynth.demos.tutorial.RigidBodySpring")
testState (D, 1.0,  5,"artisynth.demos.tutorial.RigidCompositeBody")
testState (D, 1.0,  5,"artisynth.demos.tutorial.SimpleMuscle")
testState (D, 1.0,  5,"artisynth.demos.tutorial.RadialMuscle")
testState (D, 1.0,  5,"artisynth.demos.tutorial.VariableStiffness")
testState (D, 1.0,  5,"artisynth.demos.tutorial.MaterialBundleDemo")
#testState (D, 1.0,  5,"artisynth.demos.tutorial.SimpleMuscleWithController") # scan not implemented
testState (D, 1.0,  5,"artisynth.demos.tutorial.SimpleMuscleWithPanel")
testState (D, 1.0,  5,"artisynth.demos.tutorial.SimpleMuscleWithProbes")
testState (D, 1.0,  5,"artisynth.demos.tutorial.SphericalTextureMapping")
testState (D, 1.0,  5,"artisynth.demos.tutorial.TalusWrapping")
testState (D, 1.0,  5,"artisynth.demos.tutorial.TorusWrapping")
testState (D, 1.0,  5,"artisynth.demos.tutorial.ViaPointMuscle")
testState (A, 0.25, 5,"artisynth.demos.tutorial.PhalanxSkinWrapping")
testState (S, 0.5,  5,"artisynth.demos.tutorial.SkinBodyCollide")
testState (D, 1.0, 10, "artisynth.demos.tutorial.RollingFem")
testState (D, 1.0, 10, "artisynth.demos.tutorial.SlidingFem")
testState (D, 1.0, 10, "artisynth.demos.tutorial.FemSelfCollide")
testState (D, 1.0, 10,"artisynth.demos.tutorial.InverseParticle")
testState (D, 1.0, 10,"artisynth.demos.tutorial.InverseSpringForce")
testState (D, 1.0, 10,"artisynth.demos.tutorial.InverseMuscleArm")
testState (D, 1.0, 10,"artisynth.demos.tutorial.InverseFrameExciterArm")
testState (D, 1.0, 10,"artisynth.demos.tutorial.InverseMuscleArm")

testState (D, 1.0,  5,"artisynth.demos.test.OneBasedNumbering")
testState (D, 1.0,  5,"artisynth.demos.test.ReflectedBodies")
testState (A, 1.0,  5,"artisynth.demos.test.TorusWrapTest")
testState (D, 1.0,  5,"artisynth.demos.test.LinearPointConstraintTest")
testState (D, 0.4,  4,"artisynth.demos.test.ShellVolumeAttach")
testState (D, 0.4,  4,"artisynth.demos.test.ShellVolumeAttach", "-membrane")
testState (D, 0.4,  4,"artisynth.demos.test.ShellShellAttach")
testState (D, 0.4,  4,"artisynth.demos.test.ShellShellAttach", "-membrane1")
testState (D, 0.4,  4, "artisynth.demos.test.ShellShellAttach", "-membrane1", "-membrane2")
testState (D, 0.4,  4,"artisynth.demos.test.ShellShellAttach", "-membrane2")
testState (S, 2.0, 5, "artisynth.demos.test.ActuatedSkinning")
testState (S, 2.0, 5, "artisynth.demos.test.ActuatedSkinning", "-dq")
testState (S, 2.0, 5, "artisynth.demos.test.PointPlaneForceTest")

testState (S, 1.0, 10, "artisynth.demos.inverse.PointModel2d")
testState (S, 1.0, 10, "artisynth.demos.inverse.PointModel3d")
testState (S, 1.0, 10, "artisynth.demos.inverse.HydrostatInvDemo")
testState (S, 0.20, 4, "artisynth.models.inversedemos.TongueInvDemo")

MechSystemSolver.setAlwaysAnalyze (True)
testState (D, 2.0, 10, "artisynth.demos.test.FixedBallContact")

setCompliantContactForImplicitFriction = True

testState (D, 2.0, 10,"artisynth.demos.tutorial.JointedCollide")
testState (D, 2.0, 10,"artisynth.demos.tutorial.BallPlateCollide")
testState (D, 1.0,  5,"artisynth.demos.tutorial.DeformedJointedCollide")
testState (S, 2.0, 10, "artisynth.demos.tutorial.ContactForceMonitor")
testState (A, 2.0, 10, "artisynth.demos.tutorial.JointedBallCollide")
testState (A, 0.5, 5, "artisynth.demos.tutorial.VariableElasticContact")
testState (A, 0.5, 5, "artisynth.demos.tutorial.ElasticFoundationContact")
testState (D, 1.0, 5, "artisynth.demos.tutorial.IKMultiJointedArm");
testState (D, 1.0, 5, "artisynth.demos.tutorial.IKInverseMuscleArm");
testState (D, 1.0, 5, "artisynth.demos.tutorial.TRCMultiJointedArm");
testState (D, 1.0, 5, "artisynth.demos.tutorial.PositionProbes");
testState (D, 1.0, 5, "artisynth.demos.tutorial.MultiJointedArm");
testState (D, 1.0, 5, "artisynth.demos.opensim.OpenSimArm26");
testState (D, 1.0, 5, "artisynth.demos.opensim.Arm26FemHumerus");
testState (D, 1.0, 5, "artisynth.demos.opensim.OpenSimElbow");

testState (S, 1.0, 10, "artisynth.demos.test.EquilibriumMuscleTest")
testState (S, 1.0, 10, "artisynth.demos.test.EquilibriumMuscleTest", "-thelen")
testState (D, 1.0, 5, "artisynth.demos.tutorial.ControlPanelDemo")

main.maskFocusStealing (False)
if main.getMainFrame() == None:
   main.quit()

