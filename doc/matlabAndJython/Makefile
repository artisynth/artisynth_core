#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR_ROOT = ..
HTML_DIR = ../html/matlabAndJython
PDF_TARG = ../pdf/matlabAndJython.pdf

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

pdf: $(PDF_TARG)

$(PDF_TARG): matlabAndJython.pdf 
	cp matlabAndJython.pdf $(PDF_TARG)

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

install: install_html install_pdf

install_html: html infocenter
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/matlabAndJython
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/matlabAndJython

install_pdf: $(PDF_TARG)
	rsync $(PDF_TARG) $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

test:
	latexml test.tex >test.xml	

clean:
	$(DO_CLEAN)

-include ../Makedefs
