#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR_ROOT = ..
HTML_DIR = ../html/documentation
PDF_TARG = ../pdf/documentation.pdf

.PHONY: html pdf sectioned infocenter

SOURCES = \
	documentation.tex \
	../texinputs/artisynthDoc.tex

default: pdf html

html: $(HTML_DIR)/documentation.html

sectioned: $(HTML_DIR)/documentationToc.html

infocenter: $(HTML_DIR)/documentationToc.xml

pdf: $(PDF_TARG)

$(PDF_TARG): documentation.pdf
	cp documentation.pdf $(PDF_TARG)

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

install: install_html install_pdf

install_html: html infocenter
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/documentation
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/documentation

install_pdf: $(PDF_TARG)
	rsync $(PDF_TARG) $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

clean:
	$(DO_CLEAN)

-include ../Makedefs
