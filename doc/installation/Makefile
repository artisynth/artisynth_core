DOC_DIR = ..
HTML_DIR = ../html/installation

IMG_FILES = $(wildcard images/*.png images/*.jpg)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

WIN_SOURCES = windowsInstallation.tex installation.tex
LINUX_SOURCES = linuxInstallation.tex installation.tex
MACOS_SOURCES = macosInstallation.tex installation.tex

.PHONY: html pdf chunked

html: $(HTML_DIR)/windows/windowsInstallation.html \
      $(HTML_DIR)/linux/linuxInstallation.html \
      $(HTML_DIR)/macos/macosInstallation.html

sectioned: $(HTML_DIR)/windows/windowsInstallationToc.html \
      $(HTML_DIR)/linux/linuxInstallationToc.html \
      $(HTML_DIR)/macos/macosInstallationToc.html

infocenter: $(HTML_DIR)/windows/windowsInstallationToc.xml \
      $(HTML_DIR)/linux/linuxInstallationToc.xml \
      $(HTML_DIR)/macos/macosInstallationToc.xml

pdf: $(PDF_DIR)/windowsInstallation.pdf \
     $(PDF_DIR)/linuxInstallation.pdf \
     $(PDF_DIR)/macosInstallation.pdf 

$(PDF_DIR)/windowsInstallation.pdf: windowsInstallation.pdf
	cp windowsInstallation.pdf $(PDF_DIR)

windowsInstallation.pdf: $(WIN_SOURCES) $(EPS_FILES)
	latex $<
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) windowsInstallation.ps 

windowsInstallation.xml: $(WIN_SOURCES) $(IMG_FILES)
	latexml $< > windowsInstallation.xml

$(HTML_DIR)/windows/windowsInstallation.html: $(WIN_SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f windowsInstallation.xml

$(HTML_DIR)/windows/windowsInstallationToc.html: $(WIN_SOURCES) $(IMG_FILES)
	$(BUILD_SECTIONED_HTML)
	rm -f windowsInstallation.xml

$(HTML_DIR)/windows/windowsInstallationToc.xml: $(HTML_DIR)/windows/windowsInstallationToc.html
	$(BUILD_INFOCENTER_TOC)

$(PDF_DIR)/linuxInstallation.pdf: linuxInstallation.pdf
	cp linuxInstallation.pdf $(PDF_DIR)

linuxInstallation.pdf: $(LINUX_SOURCES) $(EPS_FILES)
	latex $<
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) linuxInstallation.ps 

linuxInstallation.xml: $(LINUX_SOURCES) $(IMG_FILES)
	latexml $< > linuxInstallation.xml

$(HTML_DIR)/linux/linuxInstallation.html: $(LINUX_SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f linuxInstallation.xml

$(HTML_DIR)/linux/linuxInstallationToc.html: $(LINUX_SOURCES) $(IMG_FILES)
	$(BUILD_SECTIONED_HTML)
	rm -f linuxInstallation.xml

$(HTML_DIR)/linux/linuxInstallationToc.xml: $(HTML_DIR)/linux/linuxInstallationToc.html
	$(BUILD_INFOCENTER_TOC)

$(PDF_DIR)/macosInstallation.pdf: macosInstallation.pdf
	cp macosInstallation.pdf $(PDF_DIR)

macosInstallation.pdf: $(MACOS_SOURCES) $(EPS_FILES)
	latex $<
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) macosInstallation.ps 

macosInstallation.xml: $(MACOS_SOURCES) $(IMG_FILES)
	latexml $< > macosInstallation.xml

$(HTML_DIR)/macos/macosInstallation.html: $(MACOS_SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f macosInstallation.xml

$(HTML_DIR)/macos/macosInstallationToc.html: $(MACOS_SOURCES) $(IMG_FILES)
	$(BUILD_SECTIONED_HTML)
	rm -f macosInstallation.xml

$(HTML_DIR)/macos/macosInstallationToc.xml: $(HTML_DIR)/macos/macosInstallationToc.html
	$(BUILD_INFOCENTER_TOC)

install_html: html
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/installation
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/installation

install_pdf: pdf
	rsync $(PDF_DIR)/installation.pdf $(WEB_DOC_URL)/pdf

-include ../Makedefs
