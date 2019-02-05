#!/bin/bash
# A shell script to call the regression tests and run the comparison

# set some environment variables
export OMP_NUM_THREADS=1

ART=$ARTISYNTH_HOME
if [[ -z $ART ]]; then
    ART=".."
fi

# OS Detection
# Classpath needs to be set to main classses directory and jar files
# in $ARTISYNTH_HOME/lib
OSNAME=`uname -s`
if [ "$OSNAME" = "Linux" ] ; then
    if uname -m | grep -q 64 ; then
       OS=$OSNAME"64"
    else
       OS=$OSNAME
       MEM_LIMIT="-Xmx2G"
    fi
    # contains $LD_LIBRARY_PATH $ARTISYNTH_HOME/lib/$OS
    # if [ $? -ne 0 ] || [ -z $LD_LIBRARY_PATH ] ; then
    if [[ "$LD_LIBRARY_PATH" != *"$ARTISYNTH_HOME/lib/$OS"* ]] || [ -z $LD_LIBRARY_PATH ] ; then
        # echo exporting LD_LIBRARY_PATH=$ART/lib/$OS:$LD_LIBRARY_PATH
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
       echo Cygwin on Windows64
       export PATH=`cygpath "$ART\lib\Windows64"`:$PATH
    else
       echo Cygwin on Windows32
       export PATH=`cygpath "$ART\lib\Windows"`:$PATH
       MEM_LIMIT="-Xmx1G"
    fi
    PATH="$PATH:$ART/bin:"
    CLASSPATH="$ART\classes;$ART\lib\*"
elif echo "$OSNAME" | grep MINGW 1>/dev/null 2>&1 ; then
    export ARTISYNTH_HOME=$ART
    export ARTISYNTH_PATH=".;$HOME;$ART"
    
	echo MinGW on Windows64
    export PATH="$ART\lib\Windows64;$PATH"
    
    PATH="$PATH:$ART/bin:"
    CLASSPATH="$ART\classes;$ART\lib\*"
else     
    echo Unknown operating system: $OSNAME
fi
export CLASSPATH
export PATH

script=$1
output=$2
reference=$3

if [[ -z $script ]]; then
    script="basicRegressionTest.py"
fi
if [[ -z $output ]]; then
    output="${script%.py}.out"
fi
if [[ -z $reference ]]; then
    reference="${script%.py}.ref"
fi

artisynth -posCorrection GlobalMass -disableHybridSolves -script $script

# If reference file does not exist, prompt to create it
if [[ ! -f $reference ]]; then
    prompt="Reference file '$reference' does not exist.  Do you wish to set it?  [Y/n]  "
    read -p "$prompt" REPLY
    if [ -z "$REPLY" ]; then
        REPLY=Y
    fi

    if [[ $REPLY == [Yy]* ]]; then
        cp $output $reference
    fi
    exit 0
fi

echo -e "\n\n\nRunning Comparisons\n"
java artisynth.core.util.CompareStateFiles -a $output $reference

