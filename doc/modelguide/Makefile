#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/modelguide

.PHONY: html pdf sectioned infocenter

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

default: pdf html

html: $(HTML_DIR)/modelguide.html

sectioned: $(HTML_DIR)/modelguideToc.html

infocenter: $(HTML_DIR)/modelguideToc.xml

pdf: $(PDF_DIR)/modelguide.pdf

$(PDF_DIR)/modelguide.pdf: modelguide.pdf
	cp modelguide.pdf $(PDF_DIR)

modelguide.pdf: $(SOURCES) $(EPS_FILES)
	latex modelguide.tex #extra call because of bibtex
	bibtex modelguide
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) modelguide.ps

modelguide.xml: $(SOURCES) $(IMG_FILES)
	latexml $< > modelguide.xml

$(HTML_DIR)/modelguide.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f modelguide.xml

$(HTML_DIR)/modelguideToc.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_SECTIONED_HTML)
	rm -f modelguide.xml

$(HTML_DIR)/modelguideToc.xml: $(HTML_DIR)/modelguideToc.html
	$(BUILD_INFOCENTER_TOC)

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/modelguide
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/modelguide

install_pdf: pdf
	rsync $(PDF_DIR)/modelguide.pdf $(WEB_DOC_URL)/pdf

test:
	latexml test.tex >test.xml	

-include ../Makedefs
