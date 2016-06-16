#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/maspack

SOURCES = \
	maspack.tex \
	../texinputs/artisynthDoc.tex \
	properties.tex \
	rendering.tex

EPS_FILES = $(addsuffix .eps,$(basename $(wildcard images/*.png images/*.jpg)))

.PHONY: html pdf chunked

html: $(HTML_DIR)/maspack.html $(COPIED_IMAGE_FILES)

pdf: $(PDF_DIR)/maspack.pdf

$(PDF_DIR)/maspack.pdf: maspack.pdf
	cp maspack.pdf $(PDF_DIR)

#chunked: $(HTML_DIR)/chunked $(COPIED_IMAGE_FILES)

maspack.pdf: $(SOURCES) $(EPS_FILES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) maspack.ps

maspack.xml: maspack.tex
	latexml maspack.tex >maspack.xml

$(HTML_DIR)/maspack.html: $(SOURCES)
	$(BUILD_HTML)
	rm -f maspack.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/maspack
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/maspack

install_pdf: pdf
	rsync $(PDF_DIR)/maspack.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
