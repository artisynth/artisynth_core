#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/developer

.PHONY: html pdf chunked

html: $(HTML_DIR)/codingStandard.html

pdf: $(PDF_DIR)/codingStandard.pdf

$(PDF_DIR)/codingStandard.pdf: codingStandard.pdf
	cp codingStandard.pdf $(PDF_DIR)

codingStandard.pdf: codingStandard.tex
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) codingStandard.ps

codingStandard.xml: codingStandard.tex
	latexml codingStandard.tex >codingStandard.xml

$(HTML_DIR)/codingStandard.html: codingStandard.tex
	$(BUILD_HTML)
	rm -f codingStandard.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/developer
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/developer

install_pdf: pdf
	rsync $(PDF_DIR)/codingStandard.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
