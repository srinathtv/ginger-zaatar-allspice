BINDIR :=bin
BEAVERCC :=thirdparty/Beaver/lib/beaver-cc.jar
JFLEX :=thirdparty/jflex/lib/JFlex.jar
GENSRC :=gensrc
TEMP :=temp
JAVA :=java
JAVAC :=javac
SRCDIR :=src

SCANNERS := $(GENSRC)/ccomp/parser/CScanner.java
PARSERS := $(GENSRC)/ccomp/parser/CParser.java 
MAIN := zcc/ZCC.java

all: $(SCANNERS) $(PARSERS) geninterp

#Beaver
$(GENSRC)/ccomp/parser/%Parser.java : $(SRCDIR)/%.grammar 
	$(JAVA) -jar $(BEAVERCC) \
	  -d $(GENSRC) \
	  $<

#Jflex
$(GENSRC)/ccomp/parser/%Scanner.java : $(SRCDIR)/%.flex 
	$(JAVA) -jar $(JFLEX) \
	  -d $(GENSRC)/ccomp/parser \
	  $<

#javac
geninterp:
	@mkdir -p $(TEMP)
	cp -r $(GENSRC)/* $(TEMP)/
	cp -r $(SRCDIR)/* $(TEMP)/
	@mkdir -p $(BINDIR)
	$(JAVAC) -sourcepath $(TEMP) \
		 -d $(BINDIR) \
	  	 $(TEMP)/$(MAIN)

# For deleting the build
clean:	
	rm -rf $(BINDIR)
	rm -rf $(GENSRC)
	rm -rf $(TEMP)
