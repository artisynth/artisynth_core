# Sets the Artisynth environment for the bash shell. Should be called
# from the ArtiSynth install directory like this:
#
# > source setup.bash
#
# ARTISYNTH_HOME is set to the current working directory.
# 
#---------------------------------------------------------------------

export ARTISYNTH_HOME=`pwd`
export ARTISYNTH_PATH=.:$HOME:$ARTISYNTH_HOME

OSNAME=`uname -s`
if [ "$OSNAME" = "Linux" ] ; then
    # Linux ...
    if uname -m | grep -q 64 ; then
       # 64 bit machine 
       export LD_LIBRARY_PATH=$ARTISYNTH_HOME/lib/Linux64:$LD_LIBRARY_PATH
    else
       # 32 bit machine 
       export LD_LIBRARY_PATH=$ARTISYNTH_HOME/lib/Linux:$LD_LIBRARY_PATH
    fi
    export CLASSPATH=$ARTISYNTH_HOME/classes:$ARTISYNTH_HOME/lib/'*':$CLASSPATH
    export PATH=$ARTISYNTH_HOME/bin:$PATH
elif [ "$OSNAME" = "Darwin" ] ; then
    # MacOS ...
    export DYLD_LIBRARY_PATH=$ARTISYNTH_HOME/lib/Darwin-x86_64:$DYLD_LIBRARY_PATH
    export CLASSPATH=$ARTISYNTH_HOME/classes:$ARTISYNTH_HOME/lib/'*':$CLASSPATH
    export PATH=$ARTISYNTH_HOME/bin:$PATH
elif echo "$OSNAME" | grep CYGWIN 1>/dev/null 2>&1 ; then
    # Windows running Cygwin ...
    if echo "$OSNAME" | grep -q 64 ; then
       # 64 bit machine 
       PATH=$ARTISYNTH_HOME/bin:$ARTISYNTH_HOME/lib/Windows64:$PATH
    else
       # 32 bit machine 
       PATH=$ARTISYNTH_HOME/bin:$ARTISYNTH_HOME/lib/Windows:$PATH
    fi
    # On Windows, need to set ARTISYNTH_HOME, ARTISYNTH_PATH, and CLASSPATH 
    # to use Windows path style
    AH=`cygpath -w $ARTISYNTH_HOME`
    export CLASSPATH="$AH\classes;$AH\lib"'\*;'"$CLASSPATH"
    ARTISYNTH_HOME=$AH
    ARTISYNTH_PATH=".;`cygpath -w $HOME`;$AH"
fi


