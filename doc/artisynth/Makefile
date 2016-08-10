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
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) artisynth.ps

artisynth.xml: artisynth.tex
	latexml artisynth.tex >artisynth.xml

$(HTML_DIR)/artisynth.html: artisynth.tex
	$(BUILD_HTML)
	rm -f artisynth.xml

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/artisynth
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/artisynth

install_pdf: pdf
	rsync $(PDF_DIR)/artisynth.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
