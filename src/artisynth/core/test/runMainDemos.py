# ArtisynthScript: "runMainDemos"
#
# Runs all the demos in artisynth_core/demoModels.txt for one second
from artisynth.core.util import *
demofile = ArtisynthPath.getHomeRelativePath ("demoModels.txt", ".")
table = AliasTable (File(demofile))
for s in table.getNames():
    print 'Loading and running ' + s
    loadModel (s)
    play (1)
    waitForStop()

