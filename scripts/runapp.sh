#!/bin/bash
# A shell script for running a java class

ART=$ARTISYNTH_HOME
if [[ -z $ART ]]; then
    ART=".."
fi

# OS Detection
# Classpath needs to be set to main classes directory and jar files
# in $ARTISYNTH_HOME/lib
OSNAME=`uname -s`
if [ "$OSNAME" = "Linux" ] ; then
    if uname -m | grep -q 64 ; then
       OS=$OSNAME"64"
    else
       OS=$OSNAME
       MEM_LIMIT="-Xmx2G"
    fi
    contains $LD_LIBRARY_PATH $ARTISYNTH_HOME/lib/$OS
    if [ $? -ne 0 ] || [ -z $LD_LIBRARY_PATH ] ; then
        export LD_LIBRARY_PATH=$ART/lib/$OS:$LD_LIBRARY_PATH
    fi
    PATH="$PATH:$ART/bin"
    CLASSPATH="$ART/classes:$ART/lib/*"
elif [ "$OSNAME" = "Darwin" ] ; then
    if [ `uname -p` = "powerpc" ] ; then
        ARCH="ppc"
    elif java -version 2>&1 | fgrep -q 'version "1.6' ; then
        ARCH="x86_64"
    else
        ARCH="i386"
    fi
    contains $DYLD_LIBRARY_PATH $ART/lib/$OSNAME-$ARCH
    if [ $? -ne 0 ] || [ -z $DYLD_LIBRARY_PATH ]; then
        export DYLD_LIBRARY_PATH=$ART/lib/$OSNAME-$ARCH:$DYLD_LIBRARY_PATH
    fi
    PATH="$PATH:$ART/bin"
    CLASSPATH="$ART/classes:$ART/lib/*"
elif echo "$OSNAME" | grep CYGWIN 1>/dev/null 2>&1 ; then
    export ARTISYNTH_HOME=`cygpath -w $ART`
    export ARTISYNTH_PATH=".;`cygpath -w $HOME`;$ART"
    if uname -m | grep -q 64 ; then
       # echo Cygwin on Windows64
       export PATH=`cygpath "$ART\lib\Windows64"`:$PATH
    else
       # echo Cygwin on Windows32
       export PATH=`cygpath "$ART\lib\Windows"`:$PATH
       MEM_LIMIT="-Xmx1G"
    fi
    PATH="$PATH:$ART/bin:"
    CLASSPATH="$ART\classes;$ART\lib\*"
else     
    echo Unknown operating system: $OSNAME
fi
export CLASSPATH
export PATH

java "$@" 
