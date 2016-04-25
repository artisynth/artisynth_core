#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/uiguide

.PHONY: html pdf chunked

html: $(HTML_DIR)/uiguide.html

pdf: $(PDF_DIR)/uiguide.pdf

$(PDF_DIR)/uiguide.pdf: uiguide.pdf
	cp uiguide.pdf $(PDF_DIR)

#chunked: $(HTML_DIR)/chunked $(COPIED_IMAGE_FILES)

uiguide.pdf: uiguide.tex
	pdflatex uiguide.tex
	pdflatex uiguide.tex
	pdflatex uiguide.tex # extra extra invocation for cross references
	rm -rf $(LATEX_JUNK_FILES)

uiguide.xml: uiguide.tex
	latexml uiguide.tex >uiguide.xml

$(HTML_DIR)/uiguide.html: uiguide.tex
	$(MAKE_HTML_DIR_IF_NECESSARY)
	latexml uiguide.tex >uiguide.xml
	$(LATEXMLPOST) uiguide.xml 
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/uiguide.html
	rm -f uiguide.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/uiguide
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/uiguide

install_pdf: pdf
	rsync $(PDF_DIR)/uiguide.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
