DOC_DIR_ROOT = ..
HTML_DIR = ../html/updates
PDF_TARG = ../pdf/updates.pdf

.PHONY: html pdf sectioned infocenter

IMG_FILES = $(wildcard images/*.png images/*.jpg images/menu/*.png)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

html: $(HTML_DIR)/updates.html

sectioned: # ignore

infocenter: # ignore

pdf: $(PDF_TARG)

$(PDF_TARG): updates.pdf
	cp updates.pdf $(PDF_TARG)

updates.pdf: updates.tex $(EPS_FILES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) updates.ps

updates.xml: updates.tex
	latexml updates.tex >updates.xml

$(HTML_DIR)/updates.html: updates.tex
	$(BUILD_HTML)
	rm -f updates.xml

install: install_html install_pdf

install_html: html infocenter
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/updates
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/updates

install_pdf: $(PDF_TARG)
	rsync $(PDF_TARG) $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

clean:
	$(DO_CLEAN)

-include ../Makedefs


