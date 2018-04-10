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
    export CLASSPATH=$ARTISYNTH_HOME/classes:$ARTISYNTH_HOME/lib/'*':$CLASSPATH
    export PATH=$ARTISYNTH_HOME/bin:$PATH
elif [ "$OSNAME" = "Darwin" ] ; then
    # MacOS ...
    export CLASSPATH=$ARTISYNTH_HOME/classes:$ARTISYNTH_HOME/lib/'*':$CLASSPATH
    export PATH=$ARTISYNTH_HOME/bin:$PATH
elif echo "$OSNAME" | grep CYGWIN 1>/dev/null 2>&1 ; then
    # Windows running Cygwin ...
    PATH=$ARTISYNTH_HOME/bin:$PATH
    # On Windows, need to set ARTISYNTH_HOME, ARTISYNTH_PATH, and CLASSPATH 
    # to use Windows path style
    AH=`cygpath -w $ARTISYNTH_HOME`
    export CLASSPATH="$AH\classes;$AH\lib"'\*;'"$CLASSPATH"
    ARTISYNTH_HOME=$AH
    ARTISYNTH_PATH=".;`cygpath -w $HOME`;$AH"
fi
