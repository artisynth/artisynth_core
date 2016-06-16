#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/uiguide

.PHONY: html pdf chunked

html: $(HTML_DIR)/uiguide.html

IMG_FILES = $(wildcard images/*.png images/*.jpg images/menu/*.png)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

eps: $(EPS_FILES)

pdf: $(PDF_DIR)/uiguide.pdf

$(PDF_DIR)/uiguide.pdf: uiguide.pdf
	cp uiguide.pdf $(PDF_DIR)

#chunked: $(HTML_DIR)/chunked $(COPIED_IMAGE_FILES)

uiguide.pdf: uiguide.tex $(EPS_FILES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) uiguide.ps

uiguide.xml: uiguide.tex
	latexml uiguide.tex >uiguide.xml

$(HTML_DIR)/uiguide.html: uiguide.tex
	$(BUILD_HTML)
	rm -f uiguide.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/uiguide
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/uiguide

install_pdf: pdf
	rsync $(PDF_DIR)/uiguide.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
