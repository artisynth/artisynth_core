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
	pdflatex documentation.tex
	pdflatex documentation.tex
	rm -rf $(LATEX_JUNK_FILES)

documentation.xml: documentation.tex
	latexml documentation.tex >documentation.xml

$(HTML_DIR)/documentation.html: documentation.tex
	$(MAKE_HTML_DIR_IF_NECESSARY)
	latexml documentation.tex >documentation.xml
	$(LATEXMLPOST) documentation.xml 
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/documentation.html
	rm -f documentation.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/documentation
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/documentation

install_pdf: pdf
	rsync $(PDF_DIR)/documentation.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
