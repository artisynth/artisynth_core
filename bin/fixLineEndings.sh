#!/bin/bash
for f in `find . -name '*.java'`; 	do 	
	dos2unix $f;
	svn propset svn:eol-style native $f; 
done;


