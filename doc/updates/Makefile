DOC_DIR = ..
HTML_DIR = ../html/updates

.PHONY: html

html: $(HTML_DIR)/updates.html

pdf: $(PDF_DIR)/updates.pdf

$(PDF_DIR)/updates.pdf: updates.pdf
	cp updates.pdf $(PDF_DIR)

updates.pdf: updates.tex
	pdflatex updates.tex
	pdflatex updates.tex
	rm -rf $(LATEX_JUNK_FILES)

updates.xml: updates.tex
	latexml updates.tex >updates.xml

$(HTML_DIR)/updates.html: updates.tex
	$(MAKE_HTML_DIR_IF_NECESSARY)
	latexml updates.tex >updates.xml
	$(LATEXMLPOST) updates.xml
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/updates.html
	rm -f updates.xml

install_html: html
	rsync $(HTML_DIR)/updates.html $(WEB_DOC_URL)/html/updates

install_pdf: pdf
	rsync $(PDF_DIR)/updates.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
