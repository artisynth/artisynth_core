#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR_ROOT = ..
HTML_DIR = ../html/modelguide
PDF_TARG = ../pdf/modelguide.pdf

.PHONY: html pdf sectioned infocenter

SOURCES = \
	modelguide.tex \
	overview.tex \
	supportingClasses.tex \
	mechanicalModelsI.tex \
	mechanicalModelsII.tex \
	simulationControl.tex \
	femModels.tex \
	muscleWrapping.tex \
	skinning.tex \
        dicom.tex \
	appendix.tex \
	../texinputs/artisynthDoc.tex

IDR_EPS_FILES = $(addsuffix .eps,$(basename $(wildcard images/*.idr)))
IMG_FILES = $(wildcard images/*.png images/*.jpg) $(IDR_EPS_FILES)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

default: pdf html

html: $(HTML_DIR)/modelguide.html

sectioned: $(HTML_DIR)/modelguideToc.html

infocenter: $(HTML_DIR)/modelguideToc.xml

pdf: $(PDF_TARG)

$(PDF_TARG): modelguide.pdf
	cp modelguide.pdf $(PDF_TARG)

# individual chapters:
%.pdf: %.tex $(EPS_FILES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) $*.ps

modelguide.pdf: $(SOURCES) $(EPS_FILES)
	latex modelguide.tex #extra call because of bibtex
	biber modelguide
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

install: install_html install_pdf

install_html: html infocenter
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/modelguide
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/modelguide

install_pdf: $(PDF_TARG)
	rsync $(PDF_TARG) $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

test:
	latexml test.tex >test.xml	

clean:
	$(DO_CLEAN)

-include ../Makedefs
