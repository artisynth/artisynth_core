DOC_DIR = ..
HTML_DIR = ../html/documentation

.PHONY: html pdf chunked

html: $(HTML_DIR)/documentation.html

pdf: $(PDF_DIR)/documentation.pdf

$(PDF_DIR)/documentation.pdf: documentation.pdf
	cp documentation.pdf $(PDF_DIR)

#chunked:
#	echo chunked not supported for documentation docs

documentation.pdf: documentation.tex
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) documentation.ps

documentation.xml: documentation.tex
	latexml documentation.tex >documentation.xml

$(HTML_DIR)/documentation.html: documentation.tex
	$(BUILD_HTML)
	rm -f documentation.xml

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/documentation
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/documentation

install_pdf: pdf
	rsync $(PDF_DIR)/documentation.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
