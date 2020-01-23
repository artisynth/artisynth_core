ROOT_DIR = ..
DOC_DIR_ROOT = .

# list of subdirectories in which certain global commands should be executed
# build uiguide first because others reference its image files
SUBDIRS = \
	uiguide \
	installation \
	documentation \
	developer \
	maspack \
        matlabAndJython \
	modelguide \
        artisynth \
	uiguide \
	updates

.PHONY: javadocs

# Assumes $ARTISYNTH_HOME/bin is in the PATH, so findJavaPackages can be found
FIND_PACKAGES=findJavaPackages

JAVADOC_XOPTS = -Xdoclint:all,-missing  # For Java 8 - relax missing tag warnings
#JAVADOC_XOPTS = 

all:	javadocs PDF HTML INFOCENTER

# build the javadocs
javadocs:
        # clear any existing javadoc directory
	rm -rf javadocs
	mkdir javadocs
	javadoc -classpath "$(ROOT_DIR)/classes:$(ROOT_DIR)/lib/*"\
		-author -d javadocs -public -sourcepath $(ROOT_DIR)/src \
		-windowtitle "Artisynth API documentation" \
		-J-Xms500m -J-Xmx500m -encoding UTF-8 $(JAVADOC_XOPTS) \
		`$(FIND_PACKAGES) -d $(ROOT_DIR)/src | tr '\n' ' ' | tr '\r' ' '`

#
# Recursively call make into a set of subdirectories.
# If an error is encountered, then force an exit if 
# the letter 'k' does not appear in MFLAGS.
#
define RUN_MAKE_IN_SUBDIRS
	@for dd in $^ _XX_STOP_XX_ ; do \
           if [ -d $$dd ] ; then \
              echo Running $(MAKE) $(MFLAGS) $@ in $$dd -- ; \
              if (cd $$dd; $(MAKE) $(MFLAGS) $@) ; then true ; else \
		 if echo $(MFLAGS) | grep -q -v k ; then exit 1; fi ; \
              fi ; \
           fi ; \
        done
endef

pdf:

html:

infocenter:

echo_subdirs:
	@echo $(SUBDIRS)

.PHONY: CLEAN
CLEAN::	$(SUBDIRS)
	$(RUN_MAKE_IN_SUBDIRS)

.PHONY: HTML
HTML::	$(SUBDIRS)
	$(RUN_MAKE_IN_SUBDIRS)

.PHONY: INFOCENTER
INFOCENTER::	$(SUBDIRS)
	$(RUN_MAKE_IN_SUBDIRS)

.PHONY: PDF
PDF::	$(SUBDIRS)
	$(RUN_MAKE_IN_SUBDIRS)

.PHONY: CLEAN_ALL
CLEAN_ALL: CLEAN
        # remove javadocs as well
	rm -rf javadocs/[a-z]*  # preserve DONT_DELETE and CVS

clean_pdf:
	@for dd in $(SUBDIRS) ; do \
	   echo $$dd ; \
	   rm -f $$dd/*.pdf ; \
	done

.PHONY: install
install:	HTML INFOCENTER PDF	
	rsync -rCtO --exclude=DONT_DELETE html/* $(WEB_DOC_URL)/html
	rsync -rCtO --exclude=DONT_DELETE pdf/* $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)

.PHONY: install_all
install_all: install_javadocs install

install_html: HTML INFOCENTER
	rsync -rCtO --exclude=DONT_DELETE html/* $(WEB_DOC_URL)/html
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html

install_single_html: HTML
	rsync -rCtO --exclude=DONT_DELETE html/* $(WEB_DOC_URL)/html
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html

install_pdf: PDF
	rsync -rCtO --exclude=DONT_DELETE pdf/* $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

# Need to do rsync on javadocs/artisynth/core because rsync does not
# always recurse down far enough otherwise.
install_javadocs:
	rsync -rvCtO --delete --exclude=DONT_DELETE javadocs/* $(WEB_DOC_URL)/javadocs
	rsync -rvCtO --delete --exclude=DONT_DELETE javadocs/artisynth/core/ $(WEB_DOC_URL)/javadocs/artisynth/core
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/javadocs

test:
	(cd documentation; make html)

-include Makedefs
