ROOT_DIR = ..

default: what

.PHONY: what
what:
	@echo "make what?"

.PHONY: install
install:
	rsync -Crlgo *.jar $(WEB_LIB_URL)
	$(MAKE_HASHES_AND_FIX_PERMISSIONS)

-include $(ROOT_DIR)/Makefile.base
