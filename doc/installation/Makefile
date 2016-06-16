DOC_DIR = ..
HTML_DIR = ../html/installation

EPS_FILES = $(addsuffix .eps,$(basename $(wildcard images/*.png images/*.jpg)))

.PHONY: html pdf chunked

html: $(HTML_DIR)/windowsInstallation.html \
      $(HTML_DIR)/linuxInstallation.html \
      $(HTML_DIR)/macosInstallation.html

pdf: $(PDF_DIR)/windowsInstallation.pdf \
     $(PDF_DIR)/linuxInstallation.pdf \
     $(PDF_DIR)/macosInstallation.pdf 

$(PDF_DIR)/windowsInstallation.pdf: windowsInstallation.pdf
	cp windowsInstallation.pdf $(PDF_DIR)

windowsInstallation.pdf: windowsInstallation.tex installation.tex $(EPS_FILES)
	latex windowsInstallation.tex
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) windowsInstallation.ps 

windowsInstallation.xml: windowsInstallation.tex installation.tex
	latexml windowsInstallation.tex >windowsInstallation.xml

$(HTML_DIR)/windowsInstallation.html: windowsInstallation.tex installation.tex
	$(BUILD_HTML)
	rm -f windowsInstallation.xml

$(PDF_DIR)/linuxInstallation.pdf: linuxInstallation.pdf
	cp linuxInstallation.pdf $(PDF_DIR)

linuxInstallation.pdf: linuxInstallation.tex installation.tex $(EPS_FILES)
	latex linuxInstallation.tex
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) linuxInstallation.ps 

linuxInstallation.xml: linuxInstallation.tex installation.tex
	latexml linuxInstallation.tex >linuxInstallation.xml

$(HTML_DIR)/linuxInstallation.html: linuxInstallation.tex installation.tex
	$(BUILD_HTML)
	rm -f linuxInstallation.xml

$(PDF_DIR)/macosInstallation.pdf: macosInstallation.pdf
	cp macosInstallation.pdf $(PDF_DIR)

macosInstallation.pdf: macosInstallation.tex installation.tex $(EPS_FILES)
	latex macosInstallation.tex
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) macosInstallation.ps 

macosInstallation.xml: macosInstallation.tex installation.tex
	latexml macosInstallation.tex >macosInstallation.xml

$(HTML_DIR)/macosInstallation.html: macosInstallation.tex installation.tex
	$(BUILD_HTML)
	rm -f macosInstallation.xml

install_html: html
	rsync -rCt $(HTML_DIR)/ $(WEB_DOC_URL)/html/installation
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/installation

install_pdf: pdf
	rsync $(PDF_DIR)/installation.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
