#!/bin/bash
#
# Runs a Java class with the environment needed by ArtiSynth applications:
#
#   runapp.sh [JAVA_OPTIONS] <className> [ARGS]
#
# ARTISYNTH_HOME is set to '..' relative to this script, unless it is
# already set. The classpath consists of ArtiSynth's classes directory and
# the jar files listed in lib/LIBRARIES (see bin/makeClasspath), followed
# by any existing CLASSPATH, minus entries for ArtiSynth's lib folder.

if [ -z "$ARTISYNTH_HOME" ] ; then
   D=`dirname "$0"`/..
   ARTISYNTH_HOME="`cd \"$D\" 2>/dev/null && pwd || echo \"$D\"`"
fi
AT=$ARTISYNTH_HOME

# Classpath consists of the main classes directory and the jar files listed
# in lib/LIBRARIES, followed by the existing CLASSPATH minus any entries for
# lib/* or jar files in lib/ (so that older versions of the libraries that
# may still be in lib/ are excluded). If LIBRARIES can't be read, use lib/*
# followed by the existing CLASSPATH.
ACP=`"$AT/bin/makeClasspath" -addEnv 2>/dev/null`

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
      CP_DEFAULT="$ATW\classes;$ATW\lib\*"
      ;;
   *)
      # Linux and MacOS: native libraries are loaded by absolute path, so
      # no library path needs to be set
      export ARTISYNTH_HOME=$AT
      export ARTISYNTH_PATH=".:$HOME:$AT"
      CP_SEP=":"
      CP_DEFAULT="$AT/classes:$AT/lib/*"
      ;;
esac
if [ -n "$ACP" ] ; then
   CLASSPATH=$ACP
elif [ -z "$CLASSPATH" ] ; then
   CLASSPATH=$CP_DEFAULT
else
   CLASSPATH=$CP_DEFAULT$CP_SEP$CLASSPATH
fi
export CLASSPATH
export PATH="$PATH:$AT/bin"

# Enable native access (used by JOGL and ArtiSynth's native libraries), to
# avoid warnings from Java 24 and later, and blocking by future versions.
# The option is supported only by Java 17 and later.
NATIVE_ACCESS=
JAVA_MAJOR=`java -version 2>&1 | sed -n 's/.*version "\([0-9]*\).*/\1/p' | head -n 1`
if [ -n "$JAVA_MAJOR" ] && [ "$JAVA_MAJOR" -ge 17 ] ; then
   NATIVE_ACCESS="--enable-native-access=ALL-UNNAMED"
fi

exec java $NATIVE_ACCESS "$@"
