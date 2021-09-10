# ArtisynthScript: "simple script"
#
# Simple demonstration script showing how to load a model and run it
#
loadModel ("artisynth.demos.tutorial.NetDemo")
addBreakPoint(2) # add a breakpoint at time 2.0
play()           # run the model and wait for it to complete
waitForStop()
reset()          # reset the model
