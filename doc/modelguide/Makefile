#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/modelguide

.PHONY: html pdf chunked

html: $(HTML_DIR)/modelguide.html $(COPIED_IMAGE_FILES)

pdf: $(PDF_DIR)/modelguide.pdf

$(PDF_DIR)/modelguide.pdf: modelguide.pdf
	cp modelguide.pdf $(PDF_DIR)

#chunked: $(HTML_DIR)/chunked $(COPIED_IMAGE_FILES)

modelguide.pdf: modelguide.tex
	pdflatex modelguide.tex
	pdflatex modelguide.tex

modelguide.xml: modelguide.tex
	latexml modelguide.tex >modelguide.xml

$(HTML_DIR)/modelguide.html: modelguide.xml
	$(MAKE_HTML_DIR_IF_NECESSARY)
	$(LATEXMLPOST) modelguide.xml 
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/modelguide.html

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/modelguide
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/modelguide

install_pdf: pdf
	rsync $(PDF_DIR)/modelguide.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
