#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR_ROOT = ..
HTML_DIR = ../html/modelguide
PDF_TARG = writingAndReading.pdf

.PHONY: html pdf sectioned infocenter

pdf: $(PDF_TARG)

creatingModels.pdf: creatingModels.tex
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) creatingModels.ps

writingAndReading.pdf: writingAndReading.tex
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) creatingModels.ps

clean:
	$(DO_CLEAN)

-include ../Makedefs
