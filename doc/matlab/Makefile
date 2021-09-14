#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR_ROOT = ..
HTML_DIR = ../html/matlab
PDF_TARG = ../pdf/matlab.pdf

.PHONY: html pdf sectioned infocenter

SOURCES = \
	matlab.tex \
	../texinputs/artisynthDoc.tex

IMG_FILES = $(wildcard images/*.png images/*.jpg)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

default: pdf html

html: $(HTML_DIR)/matlab.html

sectioned: $(HTML_DIR)/matlabToc.html

infocenter: $(HTML_DIR)/matlabToc.xml

pdf: $(PDF_TARG)

$(PDF_TARG): matlab.pdf 
	cp matlab.pdf $(PDF_TARG)

matlab.pdf: $(SOURCES) $(EPS_FILES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) matlab.ps 

matlab.xml: $(SOURCES) $(IMG_FILES)
	latexml $< > matlab.xml

$(HTML_DIR)/matlab.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f matlab.xml

$(HTML_DIR)/matlabToc.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_SECTIONED_HTML)
	rm -f matlab.xml

$(HTML_DIR)/matlabToc.xml: $(HTML_DIR)/matlabToc.html
	$(BUILD_INFOCENTER_TOC)

install: install_html install_pdf

install_html: html infocenter
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/matlab
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/matlab

install_pdf: $(PDF_TARG)
	rsync $(PDF_TARG) $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

test:
	latexml test.tex >test.xml	

clean:
	$(DO_CLEAN)

-include ../Makedefs
