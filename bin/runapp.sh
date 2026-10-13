#!/bin/bash
#
# Runs a Java class with the environment needed by ArtiSynth applications:
#
#   runapp.sh [JAVA_OPTIONS] <className> [ARGS]
#
# ARTISYNTH_HOME is set to '..' relative to this script, unless it is
# already set. The classpath consists of ArtiSynth's classes directory and
# the jar files listed in lib/LIBRARIES (see bin/makeClasspath), followed
# by any existing CLASSPATH.

if [ -z "$ARTISYNTH_HOME" ] ; then
   D=`dirname "$0"`/..
   ARTISYNTH_HOME="`cd \"$D\" 2>/dev/null && pwd || echo \"$D\"`"
fi
AT=$ARTISYNTH_HOME

# Classpath consists of the main classes directory and the jar files listed
# in lib/LIBRARIES (so that older versions of these that may still be in
# lib/ are excluded). If LIBRARIES can't be read, use lib/*.
ACP=`"$AT/bin/makeClasspath" 2>/dev/null`

case "`uname -s`" in
   CYGWIN*|MINGW*|MSYS*)
      # MumpsJNI links the Intel Fortran runtime, whose Ctrl-C handler would
      # otherwise abort the process instead of letting the JVM shut down.
      export FOR_DISABLE_CONSOLE_CTRL_HANDLER=1
      # native libraries can depend on other libraries in lib/Windows64
      PATH="$AT/lib/Windows64:$PATH"
      ATW=`cygpath -w "$AT"`
      export ARTISYNTH_HOME=$ATW
      export ARTISYNTH_PATH=".;`cygpath -w "$HOME"`;$ATW"
      CP_SEP=";"
      if [ -z "$ACP" ] ; then
         ACP="$ATW\classes;$ATW\lib\*"
      fi
      ;;
   *)
      # Linux and MacOS: native libraries are loaded by absolute path, so
      # no library path needs to be set
      export ARTISYNTH_HOME=$AT
      export ARTISYNTH_PATH=".:$HOME:$AT"
      CP_SEP=":"
      if [ -z "$ACP" ] ; then
         ACP="$AT/classes:$AT/lib/*"
      fi
      ;;
esac
if [ -z "$CLASSPATH" ] ; then
   CLASSPATH=$ACP
else
   CLASSPATH=$ACP$CP_SEP$CLASSPATH
fi
export CLASSPATH
export PATH="$PATH:$AT/bin"

exec java "$@"
