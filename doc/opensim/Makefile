#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR_ROOT = ..

.PHONY: html pdf sectioned infocenter

IMG_FILES = $(wildcard images/*.png images/*.jpg images/*.svg)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

SOURCES = \
	opensim.tex

opensim.pdf: $(SOURCES) $(EPS_FILES)
	latex opensim.tex #extra call because of biber
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) main.ps

clean:
	$(DO_CLEAN)

-include ../Makedefs
