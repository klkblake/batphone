include config.mk

all: version-name version-code bin/batphone.apk

version-name:
	./git describe > version-name

version-code:
	./git rev-list HEAD | wc -l | sed 's/ //g' > version-code

bin/batphone.apk: bin/batphone-unaligned.apk
	rm -f $@
	zipalign 4 $< $@

bin/batphone-unaligned.apk: bin/batphone.ap_ bin/classes.dex
	# XXX apkbuilder is technically deprecated -- it's library support is
	# completely broken. Google has not yet decided whether or not to
	# scrap it completely. If it does go away, replacing it (or rather,
	# the part of it that we actually use) should be trivial.
	apkbuilder $@ -z $< -f bin/classes.dex -rf src -rj libs

bin/classes.dex: $(shell find src/ gen/ -type f -name "*.java")
	mkdir -p bin/classes
	# XXX We use Ant for increased build speed, pending JEP 139.
	ant -Dandroid.target.classpath=$(SDK_PLATFORM_DIR)/android.jar -f make.xml
	dx --dex --output=$@ bin/classes

bin/batphone.ap_: bin/res assets/serval.zip
	mkdir -p gennew
	aapt package -f -M AndroidManifest.xml -A assets -S res -I $(SDK_PLATFORM_DIR)/android.jar -J gennew -F bin/batphone.ap_
	for file in $$(ls gennew/); do \
		echo $$file; \
		cmp gennew/$$file gen/$$file ||  mv gennew/$$file gen/$$file; \
	done
	rm -rf gennew

bin/res: $(shell find res/ -type f -name '*.png')
	aapt crunch -S res -C bin/res
	touch bin/res

assets/serval.zip: $(shell find data/ -type f) data/bin data/conf/chipset.zip
	zip --filesync -r9 assets/serval.zip data/*
	./git ls-files -o -s data/ > assets/manifest

data/bin: native/adhoc libs/armeabi/batmand libs/armeabi/dna libs/armeabi/iwlist libs/armeabi/olsrd
	cp -u $^ data/bin/
	touch data/bin

native/adhoc: $(shell find native/ -type f -name '*.[chyd]')
	$(MAKE) -C native

libs/armeabi/batmand libs/armeabi/dna libs/armeabi/iwlist libs/armeabi/olsrd: $(shell find jni/ -type f -name '*.[chyd]' | grep -v 'jni/olsrd/src/builddata_android.c')
	git submodule update --init
	ndk-build

data/conf/chipset.zip: $(shell find data/conf/wifichipsets/ -type f)
	zip --filesync -r9 data/conf/chipset.zip data/conf/wifichipsets/*

clean:
	rm -rf bin/*
	rm -rf gen/*
	rm -f assets/{serval.zip,manifest}
	rm -f libs/armeabi/*
	rm -f data/bin/{adhoc,batmand,dna,iwlist,olsrd}

.PHONY: all clean version-name version-code
