#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/developer

.PHONY: html pdf sectioned infocenter

SOURCES = \
	codingStandard.tex \
	../texinputs/artisynthDoc.tex

default: pdf html

html: $(HTML_DIR)/codingStandard.html

sectioned: $(HTML_DIR)/codingStandardToc.html

infocenter: $(HTML_DIR)/codingStandardToc.xml

pdf: $(PDF_DIR)/codingStandard.pdf

$(PDF_DIR)/codingStandard.pdf: codingStandard.pdf
	cp codingStandard.pdf $(PDF_DIR)

codingStandard.pdf: $(SOURCES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) codingStandard.ps

codingStandard.xml: $(SOURCES)
	latexml $< > codingStandard.xml

$(HTML_DIR)/codingStandard.html: $(SOURCES)
	$(BUILD_HTML)
	rm -f codingStandard.xml

$(HTML_DIR)/codingStandardToc.html: $(SOURCES)
	$(BUILD_SECTIONED_HTML)
	rm -f codingStandard.xml

$(HTML_DIR)/codingStandardToc.xml: $(HTML_DIR)/codingStandardToc.html
	$(BUILD_INFOCENTER_TOC)

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/developer
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/developer

install_pdf: pdf
	rsync $(PDF_DIR)/codingStandard.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
