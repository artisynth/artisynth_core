DOC_DIR = ..
HTML_DIR = ../html/updates

.PHONY: html pdf sectioned infocenter

html: $(HTML_DIR)/updates.html

sectioned: # ignore

infocenter: # ignore

pdf: $(PDF_DIR)/updates.pdf

$(PDF_DIR)/updates.pdf: updates.pdf
	cp updates.pdf $(PDF_DIR)

updates.pdf: updates.tex
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) updates.ps

updates.xml: updates.tex
	latexml updates.tex >updates.xml

$(HTML_DIR)/updates.html: updates.tex
	$(BUILD_HTML)
	rm -f updates.xml

install_html: html
	rsync $(HTML_DIR)/updates.html $(WEB_DOC_URL)/html/updates

install_pdf: pdf
	rsync $(PDF_DIR)/updates.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
