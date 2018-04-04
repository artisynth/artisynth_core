#!/bin/bash
#
# Copyright adder

# Usage copyright_adder <options> files
#   options: -a author(s)
#            -y year

AUTHORS="ArtiSynth Team Members"
YEAR=2015
VERBOSE=0

while getopts "a:y:v" opt; do
   case $opt in
      a)
         echo "Author: $OPTARG" >&2
	     AUTHORS=$OPTARG
	     ;;
	  y)
	     echo "Year: $OPTARG" >&2
         YEAR=$OPTARG
         ;;
      v)
         echo "verbose" >&2
         VERBOSE=1
         ;;
      \?)
         echo "Invalid option: -$OPTARG" >&2
         ;;
   esac
done
shift $(expr $OPTIND - 1 )

COPYRIGHT="/**
 * Copyright (c) $YEAR, by the Authors: $AUTHORS
 *
 * This software is freely available under a 2-clause BSD license. Please see
 * the LICENSE file in the ArtiSynth distribution directory for details.
 */
"

echo "Copyright:"
echo -e "$COPYRIGHT"

while test $# -gt 0; do
   # echo "Parsing file: $1"
   FILE=`echo $1 | tr '\\' '/'`

   if grep -q "* the LICENSE file in the ArtiSynth distribution directory for details." "$FILE"
   then
      # code already contains copyright
      if [[ $VERBOSE -eq 1 ]]
      then
         echo "$FILE already contains copyright"
      fi
   else
      # add copyright
      if [[ $VERBOSE -eq 1 ]]
      then
         echo "Adding copyright to file: $FILE"
      fi
      echo -e "$COPYRIGHT" > "$FILE.tmp"
      cat "$FILE" >> "$FILE.tmp"
      mv "$FILE.tmp" "$FILE"
   fi

   shift
done

exit 0
