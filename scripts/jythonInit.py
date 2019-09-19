def getMain() :
    return Main.getMain()

def loadModel (name, *args) :
    main = getMain()
    classname = main.getDemoClassName (name)
    if classname == None:
        print "No class found for model " + name
        return False
    if len(args) == 0:
       main.loadModel (classname, name, None)
    else:
       main.loadModel (classname, name, args)

#def testArgs (name, *args) :
#    print "name=" + name;
#    print "args=",  args;

def loadModelFile (name) :
    fp = File (name)
    if not fp.canRead():
        print "File '" + name + "' not found or not readable"
        return False
    getMain().loadModelFile (fp)

def saveModelFile (name, saveWayPoints=False, coreCompsOnly=False) :
    fp = File (name)
    getMain().saveModelFile (fp, None, saveWayPoints, coreCompsOnly)

#def setRoot (name, root) :
#    main.clearRootModel ()
#    Main.setRootModel (name, root)

def play (time=None) :
    main = getMain()
    if time == None:
        main.play()
    else:
        main.play (time)

def run (time=None) :
    main = getMain()
    if time == None:
        main.play()
    else:
        main.play (time)

def pause () :
    getMain().pause()

def delay (t) :
    getMain().delay (t)

def waitForStop () :
    getMain().waitForStop()

def getSimulationException () :
    getMain().getSimulationException()

def isPlaying() :
    return getMain().isSimulating()

def getTime() :
    return getMain().getTime()

def reset () :
    getMain().reset()

def rewind () :
    return getMain().rewind()

def forward () :
    return getMain().forward()

def step () :
    getMain().step()

def reload () :
    getMain().reloadModel()

def addWayPoint (t) :
    return getMain().addWayPoint (t)

def addBreakPoint (t) :
    return getMain().addBreakPoint (t)

def getWayPoint (t) :
    return getMain().getWayPoint (t)

def removeWayPoint (time) :
    return getMain().removeWayPoint (time)

def clearWayPoints () :
    getMain().clearWayPoints ()

def loadWayPoints (filename) :
    fp = File (filename)
    if not fp.canRead():
        print "File '" + name + "' not found or not readable"
        return False
    getMain().loadWayPoints (File (filename))

def saveWayPoints (filename) :
    getMain().saveWayPoints (File (filename))

def root () :
    return getMain().getRootModel ()

# test
def script (name) :
    getMain().getJythonConsole().executeScript (name)

def find (name) :
    root = getMain().getRootModel()
    if root != None :
        return root.findComponent (name)
    else:
        return None

def getsel (idx=None) :
    selman = getMain().getSelectionManager()
    cursel = selman.getCurrentSelection()
    if idx == None:
        return cursel
    elif cursel == None or idx < 0 or idx >= cursel.size() :
        return None
    else:
        return cursel.get(idx)

def quit () :
    getMain().quit()

def objectToMatlab (obj, matlabName) :
    mi = getMain().getMatlabConnection()
    if mi != None:
        mi.objectToMatlab (obj, matlabName)
    else:
        raise Exception ('No MATLAB connection')

def matrixFromMatlab (matlabName) :
    mi = getMain().getMatlabConnection()
    if mi != None:
        return mi.matrixFromMatlab (matlabName)
    else:
        raise Exception ('No MATLAB connection')

def vectorFromMatlab (matlabName) :
    mi = getMain().getMatlabConnection()
    if mi != None:
        return mi.vectorFromMatlab (matlabName)
    else:
        raise Exception ('No MATLAB connection')

def arrayFromMatlab (matlabName) :
    mi = getMain().getMatlabConnection()
    if mi != None:
        return mi.arrayFromMatlab (matlabName)
    else:
        raise Exception ('No MATLAB connection')

def iprobeToMatlab (probeName, matlabName=None) :
    mi = getMain().getMatlabConnection()
    if mi == None:
        raise Exception ('No MATLAB connection')
    root = getMain().getRootModel()
    probe = root.findComponent ('inputProbes/'+probeName)
    if probe == None:
        raise Exception ('Input probe '+probeName+' not found')
    probe.saveToMatlab (mi, matlabName);

def iprobeFromMatlab (probeName, matlabName=None) :
    mi = getMain().getMatlabConnection()
    if mi == None:
        raise Exception ('No MATLAB connection')
    root = getMain().getRootModel()
    probe = root.findComponent ('inputProbes/'+probeName)
    if probe == None:
        raise Exception ('Input probe '+probeName+' not found')
    probe.loadFromMatlab (mi, matlabName);

def oprobeToMatlab (probeName, matlabName=None) :
    mi = getMain().getMatlabConnection()
    if mi == None:
        raise Exception ('No MATLAB connection')
    root = getMain().getRootModel()
    probe = root.findComponent ('outputProbes/'+probeName)
    if probe == None:
        raise Exception ('Output probe '+probeName+' not found')
    probe.saveToMatlab (mi, matlabName);

def oprobeFromMatlab (probeName, matlabName=None) :
    mi = getMain().getMatlabConnection()
    if mi == None:
        raise Exception ('No MATLAB connection')
    root = getMain().getRootModel()
    probe = root.findComponent ('outputProbes/'+probeName)
    if probe == None:
        raise Exception ('Output probe '+probeName+' not found')
    probe.loadFromMatlab (mi, matlabName);

def rundemos (fileName, time) :
    f = open(fileName, 'r')
    for line in f:
       # remove comments
       idx = line.find ('#')
       if idx != -1 :
          line = line[:idx]
       # strip leading and trailing whitespace
       demoname = (line.rstrip()).lstrip()
       # run demo if string is not empty
       if demoname :
          print ('Running demo ' + demoname)
          System.out.println ('Running demo ' + demoname)
          loadModel (demoname)
          play (time)
          waitForStop()
