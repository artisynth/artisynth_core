#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/matlabAndJython

SOURCES = \
	../texinputs/artisynthDoc.tex \
	matlabAndJython.tex

.PHONY: html pdf chunked

default: pdf html

html: $(HTML_DIR)/matlabAndJython.html $(COPIED_IMAGE_FILES)

pdf: $(PDF_DIR)/matlabAndJython.pdf

$(PDF_DIR)/matlabAndJython.pdf: matlabAndJython.pdf
	cp matlabAndJython.pdf $(PDF_DIR)

#chunked: $(HTML_DIR)/chunked $(COPIED_IMAGE_FILES)

matlabAndJython.pdf: $(SOURCES)
	pdflatex matlabAndJython.tex
	#bibtex matlabAndJython1
	#pdflatex matlabAndJython.tex	
	pdflatex matlabAndJython.tex

matlabAndJython.xml: $(SOURCES)
	latexml matlabAndJython.tex >matlabAndJython.xml

$(HTML_DIR)/matlabAndJython.html: $(SOURCES)
	$(MAKE_HTML_DIR_IF_NECESSARY)
	latexml matlabAndJython.tex >matlabAndJython.xml
	$(LATEXMLPOST) matlabAndJython.xml 
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/matlabAndJython.html
#	rm -f matlabAndJython.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/matlabAndJython
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/matlabAndJython

install_pdf: pdf
	rsync $(PDF_DIR)/matlabAndJython.pdf $(WEB_DOC_URL)/pdf

test:
	latexml test.tex >test.xml	

-include ../Makedefs
