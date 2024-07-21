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
	collisions.tex \
	muscleWrapping.tex \
	inverse.tex \
	skinning.tex \
        dicom.tex \
	appendix.tex \
	../texinputs/artisynthDoc.tex

IDR_EPS_FILES = $(addsuffix .eps,$(basename $(wildcard images/*.idr)))
IMG_FILES = $(wildcard images/*.png images/*.jpg images/*.svg) $(IDR_EPS_FILES)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

# source files for force length curves created using GNU plot
FLC_SOURCES = \
	images/peckFLC.txt \
	images/BlemkerAFL.txt \
	images/BlemkerPFL.txt \
	images/BlemkerCFL.txt \
	images/ThelenAFLC.txt \
	images/ThelenPFLC.txt \
	images/ThelenTFLC.txt \
	images/ThelenFVC_1.txt \
	images/ThelenFVC_h.txt \
	images/MillardAFLC.txt \
	images/MillardPFLC.txt \
	images/MillardTFLC.txt \
	images/MillardFVC.txt

# target files for force length curves created using GNU plot
FLC_TARGETS = \
	images/PeckAPFLC.eps \
	images/PeckCFLC.eps \
	images/BlemkerAPFL.eps \
	images/BlemkerCFL.eps \
	images/ThelenAFLC.eps \
	images/ThelenPFLC.eps \
	images/ThelenTFLC.eps \
	images/ThelenFVC.eps \
	images/MillardAFLC.eps \
	images/MillardPFLC.eps \
	images/MillardTFLC.eps \
	images/MillardFVC.eps

$(FLC_TARGETS) &: $(FLC_SOURCES)
	cd images; gnuplot -c makeMuscleCurvePlots

default: pdf html

html: $(HTML_DIR)/modelguide.html

sectioned: $(HTML_DIR)/modelguideToc.html

infocenter: $(HTML_DIR)/modelguideToc.xml

pdf: $(PDF_TARG)

$(PDF_TARG): modelguide.pdf
	cp modelguide.pdf $(PDF_TARG)

mth: $(HTML_DIR)/methodTable.html

mtp: methodTable.pdf

methodTable.pdf: methodTable.tex
	latex $<
	dvips -j0 methodTable
	$(FIX_POSTSCRIPT_OUTPUT) --out _tmp_.ps methodTable.ps
	mv _tmp_.ps methodTable.ps
	ps2pdf methodTable.ps
	rm -rf $(LATEX_JUNK_FILES) methodTable.ps

$(HTML_DIR)/methodTable.html: methodTable.tex
	$(BUILD_HTML)

# individual chapters:
%.pdf: %.tex $(EPS_FILES) $(FLC_TARGETS)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) $*.ps

modelguide.pdf: $(SOURCES) $(EPS_FILES) $(FLC_TARGETS)
	latex -interaction=batchmode modelguide.tex #extra call because of biber
	biber modelguide
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) modelguide.ps

modelguide.xml: $(SOURCES) $(IMG_FILES)
	latexml $< > modelguide.xml

modelguide_html: $(HTML_DIR)/modelguide.html

inverse_html: $(HTML_DIR)/inverse.html

$(HTML_DIR)/inverse.html: inverse.tex
	$(BUILD_HTML)

$(HTML_DIR)/modelguide.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f modelguide.xml

femModels_html: $(HTML_DIR)/femModels.html

$(HTML_DIR)/femModels.html: femModels.tex
	$(BUILD_HTML)
	rm -f femModels.xml

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

clean:
	$(DO_CLEAN)

-include ../Makedefs
