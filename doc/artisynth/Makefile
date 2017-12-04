#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/artisynth

.PHONY: html pdf sectioned infocenter

SOURCES = \
	artisynth.tex \
	../texinputs/artisynthDoc.tex

default: pdf html

html: $(HTML_DIR)/artisynth.html

sectioned: $(HTML_DIR)/artisynthToc.html

infocenter: $(HTML_DIR)/artisynthToc.xml

pdf: $(PDF_DIR)/artisynth.pdf

$(PDF_DIR)/artisynth.pdf: artisynth.pdf
	cp artisynth.pdf $(PDF_DIR)

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

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/artisynth
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/artisynth

install_pdf: pdf
	rsync $(PDF_DIR)/artisynth.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
