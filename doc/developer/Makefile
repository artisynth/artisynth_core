#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR_ROOT = ..
HTML_DIR = ../html/developer
PDF_TARG = ../pdf/codingStandard.pdf

.PHONY: html pdf sectioned infocenter

SOURCES = \
	codingStandard.tex \
	../texinputs/artisynthDoc.tex

default: pdf html

html: $(HTML_DIR)/codingStandard.html

sectioned: $(HTML_DIR)/codingStandardToc.html

infocenter: $(HTML_DIR)/codingStandardToc.xml

pdf: $(PDF_TARG)

$(PDF_TARG): codingStandard.pdf
	cp codingStandard.pdf $(PDF_TARG)

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

install: install_html install_pdf

install_html: html infocenter
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/developer
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/developer

install_pdf: $(PDF_TARG)
	rsync $(PDF_TARG) $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

clean:
	$(DO_CLEAN)

-include ../Makedefs
