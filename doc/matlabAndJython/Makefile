#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/matlabAndJython

.PHONY: html pdf sectioned infocenter

SOURCES = \
	matlabAndJython.tex \
	../texinputs/artisynthDoc.tex

IMG_FILES = $(wildcard images/*.png images/*.jpg)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

default: pdf html

html: $(HTML_DIR)/matlabAndJython.html

sectioned: $(HTML_DIR)/matlabAndJythonToc.html

infocenter: $(HTML_DIR)/matlabAndJythonToc.xml

pdf: $(PDF_DIR)/matlabAndJython.pdf

$(PDF_DIR)/matlabAndJython.pdf: matlabAndJython.pdf 
	cp matlabAndJython.pdf $(PDF_DIR)

matlabAndJython.pdf: $(SOURCES) $(EPS_FILES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) matlabAndJython.ps 

matlabAndJython.xml: $(SOURCES) $(IMG_FILES)
	latexml $< > matlabAndJython.xml

$(HTML_DIR)/matlabAndJython.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f matlabAndJython.xml

$(HTML_DIR)/matlabAndJythonToc.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_SECTIONED_HTML)
	rm -f matlabAndJython.xml

$(HTML_DIR)/matlabAndJythonToc.xml: $(HTML_DIR)/matlabAndJythonToc.html
	$(BUILD_INFOCENTER_TOC)

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/matlabAndJython
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/matlabAndJython

install_pdf: pdf
	rsync $(PDF_DIR)/matlabAndJython.pdf $(WEB_DOC_URL)/pdf

test:
	latexml test.tex >test.xml	

-include ../Makedefs
