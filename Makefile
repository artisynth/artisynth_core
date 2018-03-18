ROOT_DIR = .
JAVA_SUBDIRS = \
	src

.PHONY: default
default:
	cd src && make build

.PHONY: javadocs
javadocs: #make the javadocs
	cd doc && make -s javadocs

HELP_MSGS += "default: 'make build'"
HELP_MSGS += "build: compiles all java files in all subdirectories"
HELP_MSGS += "javadocs: creates javadocs"
HELP_MSGS += "klean: quickly deletes all class and backup files"

-include $(ROOT_DIR)/Makefile.base
