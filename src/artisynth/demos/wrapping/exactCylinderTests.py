# ArtisynthScript: "cylinderTests"
#
import sys
from java.io import File
from artisynth.demos.wrapping.ParametricTestBase.ParametricMotionControllerBase import PointMotion
from artisynth.demos.wrapping.AnalyticGeometryManager import Geometry
from artisynth.demos.wrapping.AnalyticGeometryManager import WrapMethod
from maspack.geometry.DistanceGrid import DistanceMethod
DistanceGrid.DEFAULT_DISTANCE_METHOD = DistanceMethod.BVH # more accurate
main.maskFocusStealing (False)
wm = WrapMethod.ANALYTIC
numk = 50
gres = 20
i = 0
while i < len(sys.argv):
   arg = sys.argv[i]
   if arg == '-numk':
       i = i+1
       numk = int(sys.argv[i])
   elif arg == '-gres':
       i = i+1
       wm = WrapMethod.SIGNED_DISTANCE_GRID
       gres = int(sys.argv[i])
   i = i+1

loadModel ("artisynth.demos.wrapping.AnalyticGeometryTests")
gt = root()
gt.setNumSegments(numk)
gt.setExplicitGridRes (Vector3i (gres, gres, 10))
#gt.setExplicitGridRes (Vector3i (40, 40, 10))
#gt.setExplicitGridRes (Vector3i (80, 80, 10))
mc = gt.getControllers().get(0)
gm = gt.getGeometryManager()
gm.setResolution (400)
gm.setWrapMethod (wm)
mm = gt.findComponent ("models/mechMod")
spr = mm.findComponent ("multiPointSprings/spring")
spr.setMaxWrapIterations (20)
spr.setConvergenceTol (1e-8)
motions = [ PointMotion.FIXED, PointMotion.ATTACHED, PointMotion.PARAMETRIC ]
for m1 in motions:
   mc.setOriginMotion (m1)
   for m2 in motions:
      mc.setInsertionMotion (m2)
      mstr = m1.toString()+" "+m2.toString()
      System.out.println ("CYLINDER " + mstr)
      delay(1)
      run(10)
      waitForStop()
      reset()

if main.getViewer() == None:
   quit()

