DOC_DIR = ..
HTML_DIR = ../html/installation

.PHONY: html pdf chunked

html: $(HTML_DIR)/windowsInstallation.html \
      $(HTML_DIR)/linuxInstallation.html \
      $(HTML_DIR)/macosInstallation.html

pdf: $(PDF_DIR)/windowsInstallation.pdf \
     $(PDF_DIR)/linuxInstallation.pdf \
     $(PDF_DIR)/macosInstallation.pdf 

$(PDF_DIR)/windowsInstallation.pdf: windowsInstallation.pdf
	cp windowsInstallation.pdf $(PDF_DIR)

windowsInstallation.pdf: windowsInstallation.tex installation.tex
	pdflatex windowsInstallation.tex
	pdflatex windowsInstallation.tex
	pdflatex windowsInstallation.tex

windowsInstallation.xml: windowsInstallation.tex installation.tex
	latexml windowsInstallation.tex >windowsInstallation.xml

$(HTML_DIR)/windowsInstallation.html: windowsInstallation.tex installation.tex
	$(MAKE_HTML_DIR_IF_NECESSARY)
	latexml windowsInstallation.tex >windowsInstallation.xml
	$(LATEXMLPOST) windowsInstallation.xml 
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/windowsInstallation.html
	rm -f windowsInstallation.xml

$(PDF_DIR)/linuxInstallation.pdf: linuxInstallation.pdf
	cp linuxInstallation.pdf $(PDF_DIR)

linuxInstallation.pdf: linuxInstallation.tex installation.tex
	pdflatex linuxInstallation.tex
	pdflatex linuxInstallation.tex
	pdflatex linuxInstallation.tex

linuxInstallation.xml: linuxInstallation.tex installation.tex
	latexml linuxInstallation.tex >linuxInstallation.xml

$(HTML_DIR)/linuxInstallation.html: linuxInstallation.tex installation.tex
	$(MAKE_HTML_DIR_IF_NECESSARY)
	latexml linuxInstallation.tex >linuxInstallation.xml
	$(LATEXMLPOST) linuxInstallation.xml 
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/linuxInstallation.html
	rm -f linuxInstallation.xml

$(PDF_DIR)/macosInstallation.pdf: macosInstallation.pdf
	cp macosInstallation.pdf $(PDF_DIR)

macosInstallation.pdf: macosInstallation.tex installation.tex
	pdflatex macosInstallation.tex
	pdflatex macosInstallation.tex
	pdflatex macosInstallation.tex

macosInstallation.xml: macosInstallation.tex installation.tex
	latexml macosInstallation.tex >macosInstallation.xml

$(HTML_DIR)/macosInstallation.html: macosInstallation.tex installation.tex
	$(MAKE_HTML_DIR_IF_NECESSARY)
	latexml macosInstallation.tex >macosInstallation.xml
	$(LATEXMLPOST) macosInstallation.xml 
	$(FIX_HTML_OUTPUT) > _tmp_.html
	mv _tmp_.html $(HTML_DIR)/macosInstallation.html
	rm -f macosInstallation.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/installation
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/installation

install_pdf: pdf
	rsync $(PDF_DIR)/installation.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
