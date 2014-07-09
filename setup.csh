# Sets the Artisynth environment for csh or tcsh. Should be called
# from the ArtiSynth install directory like this:
#
# > source setup.csh
#
# ARTISYNTH_HOME is set to the current working directory.
# 
#---------------------------------------------------------------------

setenv ARTISYNTH_HOME `pwd`
setenv ARTISYNTH_PATH .":"$HOME":"$ARTISYNTH_HOME

# set CLASSPATH to empty string if not already set
if ( $?CLASSPATH == 0) then
  setenv CLASSPATH ""
endif

set OSNAME=`uname -s`
echo $OSNAME | grep -q CYGWIN
if ( $status == 0 ) then
   set OSNAME=CYGWIN
endif
if ( $OSNAME == "Linux" ) then
   # Linux ...
   uname -m | grep -q 64 
   # set LD_LIBRARY_PATH to empty string if not already set 
   if ( $?LD_LIBRARY_PATH == 0) then 
     setenv LD_LIBRARY_PATH ""
   endif
   if ( $status == 0 ) then
      # 64 bit machine
      setenv LD_LIBRARY_PATH $ARTISYNTH_HOME/lib/Linux64":"$LD_LIBRARY_PATH
   else
      # 32 bit machine
      setenv LD_LIBRARY_PATH $ARTISYNTH_HOME/lib/Linux":"$LD_LIBRARY_PATH
   endif
   setenv CLASSPATH "$ARTISYNTH_HOME/classes:$ARTISYNTH_HOME/lib/*:$CLASSPATH"
   setenv PATH $ARTISYNTH_HOME/bin":"$PATH
else if ( $OSNAME == "Darwin" ) then
   # MacOS ...
   # set DYLD_LIBRARY_PATH to empty string if not already set 
   if ( $?DYLD_LIBRARY_PATH == 0) then 
     setenv DYLD_LIBRARY_PATH ""
   endif
   setenv DYLD_LIBRARY_PATH $ARTISYNTH_HOME/lib/Darwin-x86_64":"$DYLD_LIBRARY_PATH
   setenv CLASSPATH "$ARTISYNTH_HOME/classes:$ARTISYNTH_HOME/lib/*:$CLASSPATH"
   setenv PATH $ARTISYNTH_HOME/bin":"$PATH
else if ( $OSNAME == "CYGWIN" ) then
   # Cygwin ...
   uname -s | grep -q 64
   if ( $status == 0 ) then
      # 64 bit machine
      setenv PATH $ARTISYNTH_HOME/bin":"$ARTISYNTH_HOME/Windows64":$PATH"
   else
      # 32 bit machine
      setenv PATH $ARTISYNTH_HOME/bin":"$ARTISYNTH_HOME/Windows":$PATH"
   endif
   # On Windows, need to set ARTISYNTH_HOME, ARTISYNTH_PATH, and CLASSPATH 
   # to use Windows path style
   set AH=`cygpath -w $ARTISYNTH_HOME`
   setenv CLASSPATH "$AH\classes;$AH\lib\*;$CLASSPATH"
   setenv ARTISYNTH_HOME $AH
   setenv ARTISYNTH_PATH ".;`cygpath -w $HOME`;$AH"
endif
