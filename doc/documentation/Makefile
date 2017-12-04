#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/documentation

.PHONY: html pdf sectioned infocenter

SOURCES = \
	documentation.tex \
	../texinputs/artisynthDoc.tex

default: pdf html

html: $(HTML_DIR)/documentation.html

sectioned: $(HTML_DIR)/documentationToc.html

infocenter: $(HTML_DIR)/documentationToc.xml

pdf: $(PDF_DIR)/documentation.pdf

$(PDF_DIR)/documentation.pdf: documentation.pdf
	cp documentation.pdf $(PDF_DIR)

documentation.pdf: $(SOURCES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) documentation.ps

documentation.xml: $(SOURCES)
	latexml $< > documentation.xml

$(HTML_DIR)/documentation.html: $(SOURCES)
	$(BUILD_HTML)
	rm -f documentation.xml

$(HTML_DIR)/documentationToc.html: $(SOURCES)
	$(BUILD_SECTIONED_HTML)
	rm -f documentation.xml

$(HTML_DIR)/documentationToc.xml: $(HTML_DIR)/documentationToc.html
	$(BUILD_INFOCENTER_TOC)

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/documentation
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/documentation

install_pdf: pdf
	rsync $(PDF_DIR)/documentation.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
