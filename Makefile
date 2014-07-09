DATE = `date +%Y%m%d`
CHECKOUT_NAME=artisynth_2_0
DISTRIBUTION_DIR=distribution
DISTRIBUTION_NAME=$(shell cat VERSION)
CVS_ROOT=:ext:ssh.ece.ubc.ca:/ubc/ece/home/hct/other/hct/cvsroot
WWW=www.artisynth.org

ROOT_DIR = .
JAVA_SUBDIRS = \
	src

.PHONY: default
default:
	cd src; make build

.PHONY: javadocs
javadocs: #make the javadocs
	cd doc; make -s javadocs

DISTRIBUTION_TARGET = $(DISTRIBUTION_DIR)/$(DISTRIBUTION_NAME)

.PHONY: distribution
# do a testcheckout and make a complete copy of the 
# distribution in $(DISTRIBUTION_DIR), including zip file
distribution:
	@if [ -e $(TESTCHECKOUT_DIR)/$(CHECKOUT_NAME) ] ; then \
	   echo "removing existing testcheckout ..."; \
	   rm -rf $(TESTCHECKOUT_DIR)/$(CHECKOUT_NAME) ; \
	   echo "done" ; \
	fi
	(cd $(TESTCHECKOUT_DIR) ; export CVS_RSH=ssh; \
	 cvs -d $(CVS_ROOT) checkout -P $(CHECKOUT_NAME) )
	@if [ ! -d  $(DISTRIBUTION_DIR) ] ; then \
           echo mkdir $(DISTRIBUTION_DIR);  \
	   mkdir $(DISTRIBUTION_DIR);  \
	fi
	@if [ -e $(DISTRIBUTION_TARGET) ] ; then \
           echo "removing existing distribution ..."; \
           rm -rf $(DISTRIBUTION_TARGET) ; \
           echo "done" ; \
	fi
	bin/copyDistribution $(TESTCHECKOUT_DIR)/$(CHECKOUT_NAME) $(DISTRIBUTION_TARGET)
	bin/buildDistribution $(DISTRIBUTION_TARGET)
	cd $(DISTRIBUTION_DIR) ; \
	 zip -r $(DISTRIBUTION_NAME).zip $(DISTRIBUTION_NAME) ; \
	 ln -sf $(DISTRIBUTION_NAME) artisynth ; \
	 ln -sf $(DISTRIBUTION_NAME).zip artisynth.zip 

.PHONY: testdistribution
# make a copy of the distribution in $(DISTRIBUTION_DIR). Assumes that
# 'make testcheckout' has just been called
testdistribution: 
	@if [ ! -d  $(DISTRIBUTION_DIR) ] ; then \
           echo mkdir $(DISTRIBUTION_DIR);  \
	   mkdir $(DISTRIBUTION_DIR);  \
	fi
	@if [ -e $(DISTRIBUTION_TARGET) ] ; then \
           echo "removing existing distribution ..."; \
           rm -rf $(DISTRIBUTION_TARGET) ; \
           echo "done" ; \
	fi
	bin/copyDistribution $(TESTCHECKOUT_DIR)/$(CHECKOUT_NAME) $(DISTRIBUTION_TARGET)
	bin/buildDistribution $(DISTRIBUTION_TARGET) -nodocs

.PHONY: testdistributionx
# make a copy of the distribution in $(DISTRIBUTION_DIR). Assumes that
# 'make testcheckout' has just been called
testdistributionx: 
	@if [ ! -d  $(DISTRIBUTION_DIR) ] ; then \
           echo mkdir $(DISTRIBUTION_DIR);  \
	   mkdir $(DISTRIBUTION_DIR);  \
	fi
	@if [ -e $(DISTRIBUTION_TARGET) ] ; then \
           echo "removing existing distribution ..."; \
           rm -rf $(DISTRIBUTION_TARGET) ; \
           echo "done" ; \
	fi
	bin/copyDistribution $(TESTCHECKOUT_DIR)/$(CHECKOUT_NAME) $(DISTRIBUTION_TARGET)
	bin/buildDistribution $(DISTRIBUTION_TARGET) -nodocs

.PHONY: builddistribution
# finishes building a distribution in $(DISTRIBUTION_DIR), after
# 'make testdistribution' has been called.
builddistribution: 
	bin/buildDistribution $(DISTRIBUTION_TARGET)
	cd $(DISTRIBUTION_DIR) ; \
	 zip -r $(DISTRIBUTION_NAME).zip $(DISTRIBUTION_NAME) ; \
	 ln -sf $(DISTRIBUTION_NAME) artisynth ; \
	 ln -sf $(DISTRIBUTION_NAME).zip artisynth.zip 

TESTCHECKOUT_DIR = tmp

.PHONY: testcheckout
testcheckout: # make test cvs checkout in tmp
	@if [ -e $(TESTCHECKOUT_DIR)/$(CHECKOUT_NAME) ] ; then \
           echo "removing existing testcheckout ..."; \
           rm -rf $(TESTCHECKOUT_DIR)/$(CHECKOUT_NAME) ; \
           echo "done" ; \
	fi
	(cd $(TESTCHECKOUT_DIR) ; export CVS_RSH=ssh; \
	 cvs -d $(CVS_ROOT) checkout -P $(CHECKOUT_NAME) )
	bin/buildDistribution $(TESTCHECKOUT_DIR)/$(CHECKOUT_NAME) -nodocs

.PHONY: list_dist_java_files
list_dist_java_files:
	@packageDirs=`cat DIST_PACKAGES | sed "s|.|/|g"`; \
	for d in $$packageDirs; do \
	   (cd src/$$d; make -s list_java_files); \
	done

.PHONY: list_all_dist_java_files
list_all_dist_java_files:
	@packageDirs=`cat DIST_PACKAGES | sed "s|.|/|g"`; \
	for d in $$packageDirs; do \
	   (cd src/$$d; ls -1 *.java | sed "s|^|$$d/|" ); \
	done

HELP_MSGS += "default: 'make build'"
HELP_MSGS += "build: compiles all java files in all subdirectories"
HELP_MSGS += "javadocs: creates javadocs"
HELP_MSGS += "klean: quickly deletes all class and backup files"

-include $(ROOT_DIR)/Makefile.base
