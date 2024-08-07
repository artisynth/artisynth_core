DOC_DIR_ROOT = ..
# explicitly set DOC_DIR_FROM_HTML since installation HTML files are
# another level down
DOC_DIR_FROM_HTML = ../../..
HTML_DIR = ../html/installation
PDF_DIR = ../pdf
PDF_TARG = \
   $(PDF_DIR)/windowsInstallation.pdf \
   $(PDF_DIR)/linuxInstallation.pdf \
   $(PDF_DIR)/macosInstallation.pdf \
   $(PDF_DIR)/quickInstallation.pdf \
   $(PDF_DIR)/windowsGitEclipseInstall.pdf \
   $(PDF_DIR)/macosGitEclipseInstall.pdf \
   $(PDF_DIR)/linuxGitEclipseInstall.pdf 


IMG_FILES = $(wildcard images/*.png images/*.jpg)
EPS_FILES = $(addsuffix .eps,$(basename $(IMG_FILES)))

WIN_SOURCES = windowsInstallation.tex installation.tex
LINUX_SOURCES = linuxInstallation.tex installation.tex
MACOS_SOURCES = macosInstallation.tex installation.tex
WIN_GE_SOURCES = windowsGitEclipseInstall.tex gitEclipseInstall.tex
LINUX_GE_SOURCES = linuxGitEclipseInstall.tex gitEclipseInstall.tex
MACOS_GE_SOURCES = macosGitEclipseInstall.tex gitEclipseInstall.tex
QUICK_SOURCES = quickInstallation.tex

.PHONY: html pdf chunked

html: $(HTML_DIR)/windows/windowsInstallation.html \
      $(HTML_DIR)/linux/linuxInstallation.html \
      $(HTML_DIR)/macos/macosInstallation.html \
      $(HTML_DIR)/windows/windowsGitEclipseInstall.html \
      $(HTML_DIR)/linux/linuxGitEclipseInstall.html \
      $(HTML_DIR)/macos/macosGitEclipseInstall.html \
   $(HTML_DIR)/quickInstallation.html

sectioned: $(HTML_DIR)/windows/windowsInstallationToc.html \
      $(HTML_DIR)/linux/linuxInstallationToc.html \
      $(HTML_DIR)/macos/macosInstallationToc.html

infocenter: $(HTML_DIR)/windows/windowsInstallationToc.xml \
      $(HTML_DIR)/linux/linuxInstallationToc.xml \
      $(HTML_DIR)/macos/macosInstallationToc.xml

pdf: $(PDF_TARG)

$(PDF_DIR)/windowsInstallation.pdf: windowsInstallation.pdf
	cp windowsInstallation.pdf $(PDF_DIR)

windowsInstallation.pdf: $(WIN_SOURCES) $(EPS_FILES)
	latex -interaction=batchmode $<
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
	latex -interaction=batchmode $< 
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
	latex -interaction=batchmode $<
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

$(PDF_DIR)/quickInstallation.pdf: quickInstallation.pdf
	cp quickInstallation.pdf $(PDF_DIR)

$(PDF_DIR)/macosGitEclipseInstall.pdf: macosGitEclipseInstall.pdf
	cp macosGitEclipseInstall.pdf $(PDF_DIR)

macosGitEclipseInstall.pdf: $(MACOS_GE_SOURCES) $(EPS_FILES)
	latex -interaction=batchmode $<
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) macosGitEclipseInstall.ps

$(HTML_DIR)/macos/macosGitEclipseInstall.html: $(MACOS_GE_SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f macosGitEclipseInstall.xml

$(PDF_DIR)/windowsGitEclipseInstall.pdf: windowsGitEclipseInstall.pdf
	cp windowsGitEclipseInstall.pdf $(PDF_DIR)

windowsGitEclipseInstall.pdf: $(WIN_GE_SOURCES) $(EPS_FILES)
	latex -interaction=batchmode $<
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) windowsGitEclipseInstall.ps

$(HTML_DIR)/windows/windowsGitEclipseInstall.html: $(WIN_GE_SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f windowsGitEclipseInstall.xml

$(PDF_DIR)/linuxGitEclipseInstall.pdf: linuxGitEclipseInstall.pdf
	cp linuxGitEclipseInstall.pdf $(PDF_DIR)

linuxGitEclipseInstall.pdf: $(LINUX_GE_SOURCES) $(EPS_FILES)
	latex -interaction=batchmode $<
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) linuxGitEclipseInstall.ps

$(HTML_DIR)/linux/linuxGitEclipseInstall.html: $(LINUX_GE_SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f linuxGitEclipseInstall.xml

%.pdf: %.tex $(EPS_FILES)
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) $*.ps

quickInstallation.pdf: $(QUICK_SOURCES) $(EPS_FILES)
	latex -interaction=batchmode $<
	$(BUILD_PDF)
	rm -rf $(LATEX_JUNK_FILES) quickInstallation.ps 

quickInstallation.xml: $(QUICK_SOURCES) $(IMG_FILES)
	latexml $< > quickInstallation.xml

$(HTML_DIR)/quickInstallation.html: $(QUICK_SOURCES) $(IMG_FILES)
	$(BUILD_HTML)
	rm -f quickInstallation.xml

install: install_html install_pdf

install_html: html infocenter
	rsync -rCtO $(HTML_DIR)/ $(WEB_DOC_URL)/html/installation
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/html/installation

install_pdf: $(PDF_TARG)
	rsync $(PDF_TARG) $(WEB_DOC_URL)/pdf
	$(FIX_PERMISSIONS) $(WEB_DOC_DIR)/pdf

clean:
	rm -f $(HTML_DIR)/*/*.html $(PDF_TARG)
	$(REMOVE_JUNK_FILES)	

-include ../Makedefs
