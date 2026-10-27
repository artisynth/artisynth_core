#!/bin/bash
# Just in case java is not working set the path here explicit
#  export JAVA_HOME=/usr/local/sun/jdk1.6.0
#  export PATH=$JAVA_HOME/bin:$PATH

HELP=no
SILENT=no
NO_GUI=no
JAVA_OPTS=
ART_HOME=
declare -a MAIN_OPTS
IDX=0

#---------------------------------------------------------
# Process command line options
#---------------------------------------------------------
function parse_options()
{
    while [ $# -gt 0 ] ; do
        case "$1" in
            -fast)
                JAVA_OPTS="$JAVA_OPTS -server -Dsun.java2d.opengl=true"
                FAST=yes
                shift ;;
            -help)
                HELP=yes
                shift ;;
            -home)
                ART_HOME=$2
                shift 2 ;;
            -s)
                SILENT=yes
                shift ;;
            -noGui)
                MAIN_OPTS[$IDX]=$1
                IDX=$(($IDX+1))
                NO_GUI=yes
                shift ;;
            *)
                MAIN_OPTS[$IDX]=$1
                IDX=$(($IDX+1))
                shift ;;
        esac
    done
}

# Print help summary.
function help()
{
    cat <<EOF
synopsis:

  artisynth [PROGRAM_OPTIONS]

options:

  -fast                 run using -server option
  -s                    suppress all console output
  -home <AHOME>         set ARTISYNTH_HOME to AHOME
EOF
    java artisynth.core.driver.Launcher -options
}

parse_options "$@"

if [ -n "$ART_HOME" ] ; then
    export ARTISYNTH_HOME=$ART_HOME
    export ARTISYNTH_PATH=".":$HOME:$ARTISYNTH_HOME
elif [ -z "$ARTISYNTH_HOME" ] ; then
    # try to figure out ARTISYNTH_HOME
    D=`dirname "$0"`/..
    export ARTISYNTH_HOME="`cd \"$D\" 2>/dev/null && pwd || echo \"$D\"`"
    export ARTISYNTH_PATH=".":$HOME:$ARTISYNTH_HOME
fi
AT=$ARTISYNTH_HOME
OSNAME=`uname -s`
if [[ "$OSNAME" == *"CYGWIN"* || "$OSNAME" == *"MINGW"* ]]; then
   # Windows based system
   ATW=`cygpath -w "$AT"`
   export ARTISYNTH_HOME=$ATW
   HOMEW=`cygpath -w "$HOME"`
   export ARTISYNTH_PATH=".;$HOMEW;$ATW"
   CP_SEP=";"
   CP_DEFAULT="$ATW\classes;$ATW\lib\*"
elif [[ "$OSNAME" == *"Linux"* || "$OSNAME" == *"Darwin"* ]] ; then
   # Linux or Mac system
   CP_SEP=":"
   CP_DEFAULT="$AT/classes:$AT/lib/*"
   # Note: DYLD_LIBRARY_PATH is not set on the Mac. The native libraries are
   # loaded by absolute path, and their non-system dependencies (libiomp5
   # for Pardiso) are pre-loaded by the solvers. macOS also strips DYLD_*
   # variables when java is run via /usr/bin/java.
else     
    echo Unknown operating system: $OSNAME
fi
# Classpath consists of the main classes directory and the jar files listed
# in $ARTISYNTH_HOME/lib/LIBRARIES, followed by the existing CLASSPATH minus
# any entries for lib/* or jar files in lib/ (so that older versions of the
# libraries that may still be in lib/ are excluded). If LIBRARIES can't be
# read, use lib/* followed by the existing CLASSPATH.
ACP=`"$AT/bin/makeClasspath" -addEnv 2>/dev/null`
if [ -n "$ACP" ] ; then
   CLASSPATH=$ACP
elif [ -z "$CLASSPATH" ] ; then
   CLASSPATH=$CP_DEFAULT
else
   CLASSPATH=$CP_DEFAULT$CP_SEP$CLASSPATH
fi
export CLASSPATH

if [ $HELP = yes ] ; then
   help ; exit 0
fi

# John Lloyd, June 2021: Removed logging. The log file file did not
# record much that was useful, and was anyway inapplicable when
# running from an IDE

# LOG=$ARTISYNTH_HOME/tmp/artisynth.log
#if [ ! -e $LOG ] ; then
#    echo " " > $LOG
#else
#    echo "--Start Artisynth----------------" >> $LOG
#fi
#date >> $LOG
#uname -a >> $LOG
#echo script: $0 >> $LOG
#echo ARTISYNTH_HOME= $ARTISYNTH_HOME >> $LOG
#echo PATH= $PATH >> $LOG
#echo CLASSPATH= $CLASSPATH >> $LOG
#java -version 2>> $LOG
#java artisynth.core.util.JVMInfo >> $LOG
# echo "-------------------------------------" >> $LOG

if [ $? != "0" ] ; then 
    echo Error: Java executable not found. Please edit line 3+4 of $0
    exit 1
fi
# Enable native access (used by JOGL and ArtiSynth's native libraries), to
# avoid warnings from Java 24 and later, and blocking by future versions.
# The option is supported only by Java 17 and later.
JAVA_MAJOR=`java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1`
if [ -n "$JAVA_MAJOR" ] && [ "$JAVA_MAJOR" -ge 17 ] ; then
   JAVA_OPTS="$JAVA_OPTS --enable-native-access=ALL-UNNAMED"
fi
if [ $NO_GUI = yes ] ; then
   JAVA_OPTS="$JAVA_OPTS -Djava.awt.headless=true"
fi
if [ $SILENT = yes ] ; then
    java $JAVA_OPTS artisynth.core.driver.Launcher "${MAIN_OPTS[@]}" > /dev/null 2>&1
else
    # java $JAVA_OPTS artisynth.core.driver.Launcher ${MAIN_OPTS[@]} 2>&1 | tee -a $LOG     
    java $JAVA_OPTS artisynth.core.driver.Launcher "${MAIN_OPTS[@]}"
fi
