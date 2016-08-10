#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/matlabAndJython

SOURCES = \
	matlabAndJython.tex \
	../texinputs/artisynthDoc.tex

EPS_FILES = $(addsuffix .eps,$(basename $(wildcard images/*.png images/*.jpg)))

.PHONY: html pdf chunked

default: pdf html

html: $(HTML_DIR)/matlabAndJython.html $(COPIED_IMAGE_FILES)

pdf: $(PDF_DIR)/matlabAndJython.pdf

$(PDF_DIR)/matlabAndJython.pdf: matlabAndJython.pdf 
	cp matlabAndJython.pdf $(PDF_DIR)

#chunked: $(HTML_DIR)/chunked $(COPIED_IMAGE_FILES)

matlabAndJython.pdf: $(SOURCES) $(EPS_FILES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) matlabAndJython.ps 

matlabAndJython.xml: $(SOURCES)
	latexml matlabAndJython.tex >matlabAndJython.xml

$(HTML_DIR)/matlabAndJython.html: $(SOURCES)
	$(BUILD_HTML)
	rm -f matlabAndJython.xml

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/matlabAndJython
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/matlabAndJython

install_pdf: pdf
	rsync $(PDF_DIR)/matlabAndJython.pdf $(WEB_DOC_URL)/pdf

test:
	latexml test.tex >test.xml	

-include ../Makedefs
