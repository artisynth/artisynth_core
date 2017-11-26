#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/modelguide
#LATEXMLPOST_ARGS = --splitat=section

SOURCES = \
	modelguide.tex \
	overview.tex \
	supportingClasses.tex \
	mechanicalModelsI.tex \
	mechanicalModelsII.tex \
	simulationControl.tex \
	femModels.tex \
        dicom.tex \
	appendix.tex \
	../texinputs/artisynthDoc.tex

IMG_FILES = $(wildcard images/*.png images/*.jpg)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

.PHONY: html pdf chunked

default: pdf html

html: $(HTML_DIR)/modelguide.html $(COPIED_IMAGE_FILES)

pdf: $(PDF_DIR)/modelguide.pdf

$(PDF_DIR)/modelguide.pdf: modelguide.pdf
	cp modelguide.pdf $(PDF_DIR)

#chunked: $(HTML_DIR)/chunked $(COPIED_IMAGE_FILES)

modelguide.pdf: $(SOURCES) $(EPS_FILES)
	latex modelguide.tex #extra call because of bibtex
	bibtex modelguide
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) modelguide.ps

multipoint.pdf: multipoint.tex
	pdflatex multipoint.tex	
	pdflatex multipoint.tex

modelguide.xml: $(SOURCES)
	latexml modelguide.tex >modelguide.xml

$(HTML_DIR)/modelguide.html: $(SOURCES)
	$(BUILD_HTML)
	rm -f modelguide.xml

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/modelguide
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/modelguide

install_pdf: pdf
	rsync $(PDF_DIR)/modelguide.pdf $(WEB_DOC_URL)/pdf

test:
	latexml test.tex >test.xml	

-include ../Makedefs
