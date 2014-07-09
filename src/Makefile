ROOT_DIR = ..

# Dynamically create JAVA_SUBDIRS, depending on whether artisynth
# sub-directory is present or not. This allows up to create
# maspack-only distributions
JAVA_SUBDIRS = $(wildcard maspack* artisynth*)

default: build

-include $(ROOT_DIR)/Makefile.base
