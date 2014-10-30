_interpreter_.set ("play", main.play)
_interpreter_.set ("pause", main.pause)
_interpreter_.set ("delay", main.delay)
_interpreter_.set ("waitForStop", main.waitForStop)
_interpreter_.set ("reset", main.reset)
_interpreter_.set ("step", main.step)
_interpreter_.set ("reload", main.reloadModel)
_interpreter_.set ("addWayPoint", main.addWayPoint)
_interpreter_.set ("addBreakPoint", main.addBreakPoint)
_interpreter_.set ("clearWayPoints", main.clearWayPoints)
_interpreter_.set ("root", main.getRootModel)
_interpreter_.set ("script", main.getJythonConsole().executeScript)
#_interpreter_.set ("abort", main.getJythonInterface().abortScript)

def loadModel (name, *args) :
    classname = main.getDemoClassName (name)
    if classname == None:
        print "No class found for model " + name
        return False
    if len(args) == 0:
       main.loadModel (classname, name, None)
    else:
       main.loadModel (classname, name, args)

def testArgs (name, *args) :
    print "name=" + name;
    print "args=",  args;

def loadModelFile (name) :
    file = File (name)
    if not file.canRead():
        print "File '" + name + "' not found or not readable"
        return False
    main.loadModelFile (file)

def setRoot (name, root) :
    main.clearRootModel ()
    main.setRootModel (name, root)

def run (time=None) :
    if time == None:
        main.play()
    else:
        main.play (time)

def removeWayPoint (time) :
    main.removeWayPoint (time)

def find (name) :
    root = main.getRootModel()
    if root != None :
        return root.findComponent (name)
    else:
        return None
