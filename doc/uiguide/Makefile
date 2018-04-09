#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR_ROOT = ..
HTML_DIR = ../html/uiguide
PDF_TARG = ../pdf/uiguide.pdf

.PHONY: html pdf infocenter

SOURCES = \
	uiguide.tex \
	../texinputs/artisynthDoc.tex

IMG_FILES = $(wildcard images/*.png images/*.jpg images/menu/*.png)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

default: pdf html

html: $(HTML_DIR)/uiguide.html

sectioned: $(HTML_DIR)/uiguideToc.html

infocenter: $(HTML_DIR)/uiguideToc.xml

pdf: $(PDF_TARG)

$(PDF_TARG): uiguide.pdf
	cp uiguide.pdf $(PDF_TARG)

uiguide.pdf: $(SOURCES) $(EPS_FILES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) uiguide.ps

uiguide.xml: $(SOURCES) $(IMG_FILES)
	latexml $< > uiguide.xml

$(HTML_DIR)/uiguide.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f uiguide.xml

$(HTML_DIR)/uiguideToc.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_SECTIONED_HTML)
	rm -f uiguide.xml

$(HTML_DIR)/uiguideToc.xml: $(HTML_DIR)/uiguideToc.html
	$(BUILD_INFOCENTER_TOC)

install: install_html install_pdf

install_html: html infocenter
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/uiguide
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/uiguide

install_pdf: $(PDF_TARG)
	rsync $(PDF_TARG) $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

clean:
	$(DO_CLEAN)

-include ../Makedefs
