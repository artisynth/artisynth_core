ROOT_DIR = ../..
PACKAGE_NAME = maspack

JAVA_SUBDIRS = \
	collision \
	fileutil \
	geometry \
	interpolation \
	matlab \
	matrix \
	numerics \
	properties \
	render \
	solvers \
	spatialmotion \
	util

default: build

-include $(ROOT_DIR)/Makefile.base

.PHONY: javadocs
javadocs: #make the javadocs
	@ if [ ! -d javadocs ] ; then \
            echo mkdir javadocs ; \
            mkdir javadocs; \
        fi
	javadoc -author -d javadocs -public \
		-windowtitle "Maspack API documentation" \
		`make -s LIST_PACKAGES`

