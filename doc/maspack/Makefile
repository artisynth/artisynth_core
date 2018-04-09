#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR_ROOT = ..
HTML_DIR = ../html/maspack
PDF_TARG = ../pdf/maspack.pdf

.PHONY: html pdf sectioned infocenter

SOURCES = \
	maspack.tex \
	../texinputs/artisynthDoc.tex \
	properties.tex \
	rendering.tex

IDR_EPS_FILES = $(addsuffix .eps,$(basename $(wildcard images/*.idr)))
IMG_FILES = $(wildcard images/*.png images/*.jpg) $(IDR_EPS_FILES)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

default: pdf html

html: $(HTML_DIR)/maspack.html

sectioned: $(HTML_DIR)/maspackToc.html

infocenter: $(HTML_DIR)/maspackToc.xml

pdf: $(PDF_TARG)

$(PDF_TARG): maspack.pdf
	cp maspack.pdf $(PDF_TARG)

maspack.pdf: $(SOURCES) $(EPS_FILES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) maspack.ps

maspack.xml: $(SOURCES) $(IMG_FILES)
	latexml $< > maspack.xml

$(HTML_DIR)/maspack.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f maspack.xml

$(HTML_DIR)/maspackToc.html: $(SOURCES) $(IMG_FILES)
	$(BUILD_SECTIONED_HTML)
	rm -f maspack.xml

$(HTML_DIR)/maspackToc.xml: $(HTML_DIR)/maspackToc.html
	$(BUILD_INFOCENTER_TOC)

install: install_html install_pdf

install_html: html infocenter
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/maspack
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/maspack

install_pdf: $(PDF_TARG)
	rsync $(PDF_TARG) $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

clean:
	$(DO_CLEAN)

eps:
	echo $(EPS_FILES)

img:
	echo $(IMG_FILES)


-include ../Makedefs
