#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/artisynth

.PHONY: html pdf chunked

html: $(HTML_DIR)/artisynth.html $(COPIED_IMAGE_FILES)

pdf: $(PDF_DIR)/artisynth.pdf

$(PDF_DIR)/artisynth.pdf: artisynth.pdf
	cp artisynth.pdf $(PDF_DIR)

#chunked: $(HTML_DIR)/chunked $(COPIED_IMAGE_FILES)

artisynth.pdf: artisynth.tex
	pdflatex artisynth.tex
	pdflatex artisynth.tex
	rm -rf $(LATEX_JUNK_FILES)

artisynth.xml: artisynth.tex
	latexml artisynth.tex >artisynth.xml

$(HTML_DIR)/artisynth.html: artisynth.tex
	$(MAKE_HTML_DIR_IF_NECESSARY)
	latexml artisynth.tex >artisynth.xml
	$(LATEXMLPOST) artisynth.xml 
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/artisynth.html
	rm -f artisynth.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/artisynth
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/artisynth

install_pdf: pdf
	rsync $(PDF_DIR)/artisynth.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
