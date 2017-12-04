#
# This Makefile uses commands and definitions supplied in ../Makedefs
#
DOC_DIR = ..
HTML_DIR = ../html/maspack

.PHONY: html pdf sectioned infocenter

SOURCES = \
	maspack.tex \
	../texinputs/artisynthDoc.tex \
	properties.tex \
	rendering.tex

IMG_FILES = $(wildcard images/*.png images/*.jpg images/menu/*.png)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

default: pdf html

html: $(HTML_DIR)/maspack.html

sectioned: $(HTML_DIR)/maspackToc.html

infocenter: $(HTML_DIR)/maspackToc.xml

pdf: $(PDF_DIR)/maspack.pdf

$(PDF_DIR)/maspack.pdf: maspack.pdf
	cp maspack.pdf $(PDF_DIR)

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

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/maspack
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/maspack

install_pdf: pdf
	rsync $(PDF_DIR)/maspack.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
