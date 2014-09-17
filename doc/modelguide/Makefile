#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/modelguide

SOURCES = \
	../texinputs/artisynthDoc.tex \
	modelguide.tex \
	overview.tex \
	supportingClasses.tex \
	mechanicalModelsI.tex \
	mechanicalModelsII.tex \
	simulationControl.tex \
	femModels.tex \
	appendix.tex

.PHONY: html pdf chunked

default: pdf html

html: $(HTML_DIR)/modelguide.html $(COPIED_IMAGE_FILES)

pdf: $(PDF_DIR)/modelguide.pdf

$(PDF_DIR)/modelguide.pdf: modelguide.pdf
	cp modelguide.pdf $(PDF_DIR)

#chunked: $(HTML_DIR)/chunked $(COPIED_IMAGE_FILES)

modelguide.pdf: $(SOURCES)
	pdflatex modelguide.tex
	bibtex modelguide1
	pdflatex modelguide.tex	
	pdflatex modelguide.tex

modelguide.xml: $(SOURCES)
	latexml modelguide.tex >modelguide.xml

$(HTML_DIR)/modelguide.html: $(SOURCES)
	$(MAKE_HTML_DIR_IF_NECESSARY)
	latexml modelguide.tex >modelguide.xml
	$(LATEXMLPOST) modelguide.xml 
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/modelguide.html
#	rm -f modelguide.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/modelguide
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/modelguide

install_pdf: pdf
	rsync $(PDF_DIR)/modelguide.pdf $(WEB_DOC_URL)/pdf

test:
	latexml test.tex >test.xml	

-include ../Makedefs
