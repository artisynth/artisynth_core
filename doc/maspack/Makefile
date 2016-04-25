#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/maspack

SOURCES = \
	../texinputs/artisynthDoc.tex \
	maspack.tex \
	properties.tex \
	rendering.tex

.PHONY: html pdf chunked

html: $(HTML_DIR)/maspack.html $(COPIED_IMAGE_FILES)

pdf: $(PDF_DIR)/maspack.pdf

$(PDF_DIR)/maspack.pdf: maspack.pdf
	cp maspack.pdf $(PDF_DIR)

#chunked: $(HTML_DIR)/chunked $(COPIED_IMAGE_FILES)

maspack.pdf: $(SOURCES)
	pdflatex maspack.tex
	pdflatex maspack.tex
	pdflatex maspack.tex
	rm -rf $(LATEX_JUNK_FILES)

maspack.xml: maspack.tex
	latexml maspack.tex >maspack.xml

$(HTML_DIR)/maspack.html: $(SOURCES)
	$(MAKE_HTML_DIR_IF_NECESSARY)
	latexml maspack.tex >maspack.xml
	$(LATEXMLPOST) maspack.xml
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/maspack.html
	rm -f maspack.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/maspack
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/maspack

install_pdf: pdf
	rsync $(PDF_DIR)/maspack.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
