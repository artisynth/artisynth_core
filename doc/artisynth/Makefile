#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR_ROOT = ..
HTML_DIR = ../html/artisynth
PDF_TARG = ../pdf/artisynth.pdf

.PHONY: html pdf sectioned infocenter

SOURCES = \
	artisynth.tex \
	../texinputs/artisynthDoc.tex

default: pdf html

html: $(HTML_DIR)/artisynth.html

sectioned: $(HTML_DIR)/artisynthToc.html

infocenter: $(HTML_DIR)/artisynthToc.xml

pdf: $(PDF_TARG)

$(PDF_TARG): artisynth.pdf
	cp artisynth.pdf $(PDF_TARG)

artisynth.pdf: $(SOURCES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) artisynth.ps

artisynth.xml: $(SOURCES)
	latexml $< > artisynth.xml

$(HTML_DIR)/artisynth.html: $(SOURCES)
	$(BUILD_HTML)
	rm -f artisynth.xml

$(HTML_DIR)/artisynthToc.html: $(SOURCES)
	$(BUILD_SECTIONED_HTML)
	rm -f artisynth.xml

$(HTML_DIR)/artisynthToc.xml: $(HTML_DIR)/artisynthToc.html
	$(BUILD_INFOCENTER_TOC)

install: install_html install_pdf

install_html: html infocenter
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/artisynth
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/artisynth

install_pdf: $(PDF_TARG)
	rsync $(PDF_TARG) $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

clean:
	$(DO_CLEAN)

-include ../Makedefs
