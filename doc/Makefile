ROOT_DIR = ..
WEBINSTALL_DIR=/cs/web/artisynth.magic.ubc.ca/docs/artisynth/artisynth/doc
WEBLOC = $(WEBSERVER):$(WEB_DOC_DIR)
DOC_DIR = .

SUBDIRS = \
	documentation \
	developer \
	installation \
	maspack \
        matlabAndJython \
	modelguide \
        artisynth \
	uiguide \
	updates

WEBDOCS = \
	javadocs \
	html \
	pdf

.PHONY: javadocs

# Assumes $ARTISYNTH_HOME/bin is in the PATH, so findJavaPackages can be found
FIND_PACKAGES=findJavaPackages

javadocs: #make the javadocs
	@ if [ ! -d javadocs ] ; then \
            echo mkdir javadocs ; \
            mkdir javadocs ; \
        fi
	javadoc -author -d javadocs -public -sourcepath $(ROOT_DIR)/src \
		-windowtitle "Artisynth API documentation" \
		-J-Xms500m -J-Xmx500m \
		`$(FIND_PACKAGES) -d $(ROOT_DIR)/src | tr '\n' ' ' | tr '\r' ' '`

# This command needs to be run on the webserver
.PHONY: webinstall
webinstall: #install documentation on the web
	cp -rp $(WEBDOCS) $(WEBINSTALL_DIR)
	find $(WEBINSTALL_DIR) -user `whoami` -exec chgrp magicweb {} \;
	find $(WEBINSTALL_DIR) -user `whoami` -exec chmod g+w {} \;
	find $(WEBINSTALL_DIR) -user `whoami` -type d -exec chmod g+sx {} \;

# This command needs to be run on the webserver
fixpermissions:
	find $(WEBINSTALL_DIR) -user `whoami` -exec chgrp magicweb {} \;
	find $(WEBINSTALL_DIR) -user `whoami` -exec chmod g+w {} \;
	find $(WEBINSTALL_DIR) -user `whoami` -type d -exec chmod g+sx {} \;

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

echo_subdirs:
	@echo $(SUBDIRS)

.PHONY: CLEAN
CLEAN::	$(SUBDIRS)
	$(RUN_MAKE_IN_SUBDIRS)

.PHONY: HTML
HTML::	$(SUBDIRS)
        # Does not work because of some environment issues related to
	# LaTeXML. Need to call ./makeHTML instead.
	$(RUN_MAKE_IN_SUBDIRS)

.PHONY: XHTML
XHTML::	$(SUBDIRS)
	$(RUN_MAKE_IN_SUBDIRS)

.PHONY: PDF
PDF::	$(SUBDIRS)
	$(RUN_MAKE_IN_SUBDIRS)

.PHONY: CLEAN_ALL
CLEAN_ALL: CLEAN
	rm -rf pdf/*.pdf
	rm -rf javadocs/[a-z]*  # preserve DONT_DELETE and CVS
	rm -rf html/[a-z]*      # preserve DONT_DELETE and CVS

#.PHONY: CHUNKED
#CHUNKED:: $(SUBDIRS)
#	$(RUN_MAKE_IN_SUBDIRS)

install_all: install_dir install_javadocs install_html install_pdf 

install_dir:
	ssh $(ARTISYNTH_WEB_ACCOUNT)@$(WEBSERVER) mkdir -p $(WEB_DOC_DIR)

install_html: HTML
	rsync -va --exclude=DONT_DELETE html/* $(WEB_DOC_URL)/html
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html

install_pdf: PDF
	rsync -va --exclude=DONT_DELETE pdf/* $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

install_javadocs:
	rsync -va --delete --exclude=DONT_DELETE javadocs/* $(WEB_DOC_URL)/javadocs
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/javadocs

test:
	(cd documentation; make html)

-include Makedefs
