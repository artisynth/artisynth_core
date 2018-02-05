#!/bin/bash

usage="
usage: $(basename "$0") <jar> <package for TAG class> <native library> [<native library2> ... ]

Packages a native library (or libraries) into a jar file.  Also creates a dummy
TAG class for use in determining the appropriate resource path and library names.
The TAG class has a static String array
   public static final String[] LIBRARIES;
that stores the paths of all libraries contained within the jar file.  The native
libraries are all put into a '/natives/' folder.

e.g.
> $(basename "$0") library-1.0-windows-x86.jar my.package.name library-1.0.dll

Jar Layout:
library-1.0-windows-x86.jar
   /my/package/name/TAG.class
   /natives/library-1.0.dll
"

if [[ $# < 3 ]]; then
  echo "$usage"
  exit 1
fi

jarfile=$1
package=$2
shift 2
natives=$@

# create temporary directory
tmpdir=`mktemp -d 2>/dev/null || mktemp -d -t 'tmplibdir'`
jardir="$tmpdir/jar"
nativedir="$jardir/natives"
mkdir "$jardir"
mkdir "$nativedir"

# copy natives into native dir
cp $@ "$nativedir"

# create java TAG source
javafile="$tmpdir/TAG.java"
libraries="$libraries$(printf '\"/natives/%s\",' "$@")"
tagsource="package $package;

public class TAG {
   static final String[] LIBRARIES = {"${libraries}"};
}
"
echo "$tagsource" > "$javafile"

# compile java
javac -d "$jardir" "$javafile"

# create jar
jar cvf "$jarfile" -C "$jardir/" .

# remove temporary directory
rm -r "$tmpdir"



