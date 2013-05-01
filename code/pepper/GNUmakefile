#
# This makefile system follows the structuring conventions
# recommended by Peter Miller in his excellent paper:
#
#	Recursive Make Considered Harmful
#	http://aegis.sourceforge.net/auug97.pdf
#
OBJDIR := obj
BINDIR := bin
PTXDIR := ptx
LIBDIR := lib
GENSRCDIR := apps_sfdl_gen
HWSRCDIR := apps_sfdl_hw

include flags

TOP = .

# try to infer the correct GCCPREFIX
ifndef GCCPREFIX
GCCPREFIX := 
endif

CUDA_HARDWARE := nvidia
CUDA_BASE := /usr/local/cuda

CC	:= $(GCCPREFIX)gcc -pipe
CPP	:= $(GCCPREFIX)mpicxx -pipe
AS	:= $(GCCPREFIX)as
AR	:= $(GCCPREFIX)ar rcs
LD	:= $(GCCPREFIX)ld
OBJCOPY	:= $(GCCPREFIX)objcopy
OBJDUMP	:= $(GCCPREFIX)objdump
NM	:= $(GCCPREFIX)nm
NVCC    := $(CUDA_BASE)/bin/nvcc

# Native commands
NCC	:= gcc $(CC_VER) -pipe
TAR	:= gtar
PERL	:= perl
PYTHON := python

CHECKER := $(PYTHON) cpplint.py --verbose 5 

# Compiler flags
CFLAGS := $(CFLAGS) -I$(TOP) -MD -m64 
CFLAGS += -Wall -Wno-format -Wno-unused -fopenmp -O3

# Temporary directory
CFLAGS += -DFOLDER_STATE=\"/tmp/$(USER)/computation_state\"

# Add -fno-stack-protector if the option exists.
CFLAGS += $(shell $(CC) -fno-stack-protector -E -x c /dev/null >/dev/null 2>&1 && echo -fno-stack-protector)

CUDA_CFLAGS := --use_fast_math -DMP_USE_64BIT=1 -gencode arch=compute_20,code=sm_20
CUDA_IFLAGS := -I$(CUDA_BASE)/include -I/$(CUDA_BASE)/samples/common/inc

ifeq ($(USE_GPU), 1) 
CUDA_LIBS := -L/usr/lib/nvidia-current-updates -lcudart -lcuda
CFLAGS += -DUSE_GPU=1
else
CFLAGS += -DUSE_GPU=0
endif

CFLAGS += -DNUM_PROVERS_MAX=$(NUM_PROVERS_MAX)

ZCC_CARGS :=
ifdef BENCHMARK_COMPILER
ZCC_CARGS += --metrics
endif

ifdef NAIVE_FAIRPLAY 
ZCC_CARGS += --no-inline
endif

ifdef SKIP_CIRCUIT_OPT 
ZCC_CARGS += --no-opt
endif

ifdef STATIC_COMPILED_PROVER 
ZCC_CARGS += --staticp
endif

ifdef ELIMINATE_COMMON_SUBEXPRS 
ZCC_CARGS += --ecs
endif

ifdef LIMIT_CONSTRAINT_COMPLEXITY
ZCC_CARGS += --limcsize
endif


# Common linker flags
LDFLAGS := -L/usr/local/lib -lm -lgmp -lgomp -lconfig++ -lgmpmee -lntl -lgmp -lrt -lfcgi
# LDFLAGS += -lcurl
LDFLAGS += -L/usr/local/lib/chacha -lchacha
LDFLAGS += -lpapi

# common include flags
IFLAGS := -I/usr/include -I/usr/local/include -pthread
IFLAGS += -I/usr/local/include/chacha

ifeq ($(USE_GPU), 1) 
CUDA_LDFLAGS := -L/usr/local/cuda/lib64 -L/usr/lib/nvidia-current-updates -lcudart -lcuda -lssl -lcrypto
endif

GCC_LIB := $(shell $(CC) $(CFLAGS) -print-libgcc-file-name)

# Lists that the */Makefrag makefile fragments will add to
OBJDIRS :=

# Make sure that 'all' is the first target
all:

# Target to build and run test code
test: 

# Eliminate default suffix rules
.SUFFIXES:

# Delete target files if there is an error (or make is interrupted)
.DELETE_ON_ERROR:

# make it so that intermediate .o files and generated source files are not deleted
.PRECIOUS: %.o $(OBJDIR)/%.o $(GENSRCFILES) 

# Set to nothing (i.e., V = ) to enable verbose outputs.
V = @

MAKEFILE_DEP_LINE = $@: GNUmakefile $(wildcard $(dir $<)Makefrag)

# Include Makefrags for subdirectories
include common/Makefrag
include crypto/Makefrag
include libv/Makefrag
include apps_tailored/Makefrag
ifeq ($(USE_GPU), 1)
include lib_gpu/Makefrag
include lib_gpu_tests/Makefrag
endif

ifneq ($(BUILD_CMT), 0)
include cmtgkr/Makefrag
endif
include apps_sfdl/Makefrag

APPS_BINS = $(patsubst apps_tailored/%.cpp, $(BINDIR)/%, $(EXEC_SRCFILES))

APPS_SFDL_BINS = $(patsubst %, $(BINDIR)/%, $(COMPUTATIONS))

COMMON_LIB_OBJFILES = \
	$(COMMON_OBJFILES) \
	$(CRYPTO_OBJFILES) \
	$(LIBV_OBJFILES) \
	$(PROVER_OBJFILES) \
	$(VERIFIER_OBJFILES) \
	$(APPS_OBJFILES) \
	$(APPS_SFDL_OBJFILES) \

# How to build apps
$(APPS_BINS) : $(BINDIR)/% : $(EXAMPLES_OBJFILES) $(OBJDIR)/apps_tailored/%.o $(COMMON_LIB_OBJFILES) 
	@mkdir -p $(@D)
	@echo + mk $@
	$(V)$(CPP) $(CFLAGS) $(IFLAGS) $(CUDA_IFLAGS) -o $@ $^ $(LDFLAGS) $(CUDA_LDFLAGS)

$(APPS_SFDL_BINS) : $(BINDIR)/% : $(EXAMPLES_OBJFILES) $(OBJDIR)/$(GENSRCDIR)/%.o $(COMMON_LIB_OBJFILES) 
	@mkdir -p $(@D)
	@echo + mk $@
	$(V)$(CPP) $(CFLAGS) $(IFLAGS) $(CUDA_IFLAGS) -o $@ $^ $(LDFLAGS) $(CUDA_LDFLAGS)



# Provers also need prover objfile.
# $(patsubst %, $(BINDIR)/%_p, $(COMPUTATION_APPS)): $(PROVER_OBJFILES)

# Verifiers also need verifier objfile.
# $(patsubst %, $(BINDIR)/%_v, $(COMPUTATION_APPS)): $(VERIFIER_OBJFILES)

all: $(APPS_SFDL_BINS) $(APPS_BINS)

# Run the code generation only (don't compile all the way to executables)
gensrc: $(GENSRCFILES)

computation_state:
	mkdir -p $@

run-%: $(BINDIR)/%_v $(BINDIR)/%_p computation_state
	rm -rf computation_state/*
	./run/$*.sh $(ARGS)


#
# This target informs make how our compiler works. It produces
# specially-named cpp and h files in $(GENSRCDIR) using .sfdl files.
#
$(GENSRCDIR)/%.cpp $(GENSRCDIR)/%_v.cpp $(GENSRCDIR)/%_p.cpp $(GENSRCDIR)/%_v.h $(GENSRCDIR)/%_p.h $(GENSRCDIR)/%_cons.h $(GENSRCDIR)/%_v_inp_gen.h $(GENSRCDIR)/%_v_inp_gen.cpp : apps_sfdl/%.sfdl
	@echo + compile $< $(ZCC_CARGS)
	@mkdir -p $(GENSRCDIR)
	@mkdir -p $(BINDIR)
	@cd ../compiler/; \
	./zcc -f ../pepper/$< \
		      -d ../pepper \
		      -b $(BUGGINESS) \
		      -w $(COMPILER_WINDOW_SIZE) \
		      -t $(FRAMEWORK) \
		      $(ZCC_CARGS) \
		      -prefix $(patsubst apps_sfdl/%.sfdl,$(GENSRCDIR)/%,$<) \

#
# Same as above, but for .c file compilation
#
$(GENSRCDIR)/%.cpp $(GENSRCDIR)/%_v.cpp $(GENSRCDIR)/%_p.cpp $(GENSRCDIR)/%_v.h $(GENSRCDIR)/%_p.h $(GENSRCDIR)/%_cons.h $(GENSRCDIR)/%_v_inp_gen.h $(GENSRCDIR)/%_v_inp_gen.cpp : apps_sfdl/%.c
	@echo + compile $< $(ZCC_CARGS)
	@mkdir -p $(GENSRCDIR)
	@mkdir -p $(BINDIR)
	@cd ../compiler/; \
	./zcc -f ../pepper/$< \
		      -d ../pepper \
		      -b $(BUGGINESS) \
		      -w $(COMPILER_WINDOW_SIZE) \
		      -t $(FRAMEWORK) \
		      $(ZCC_CARGS) \
		      -prefix $(patsubst apps_sfdl/%.c,$(GENSRCDIR)/%,$<) \

$(OBJDIR)/%.o: %.cu
	@echo + nvcc $<
	@mkdir -p $(@D)
	$(V)$(NVCC) $(CUDA_CFLAGS) $(CUDA_IFLAGS) -c $< -o $@
	$(V)$(NVCC) $(CUDA_CFLAGS) $(CUDA_IFLAGS) -M $< | sed 's/^$(notdir $*).o/$(subst /,\/,$@)/g' > $(@:o=d)
	$(V)echo "$(MAKEFILE_DEP_LINE)" >> $(@:o=d)

$(OBJDIR)/%.o: %.cpp
	@echo + cpp $<
	@mkdir -p $(@D)
	$(V)$(CPP) $(CFLAGS) -DMP_USE_64BIT $(IFLAGS) $(CUDA_IFLAGS) -c $< -o $@
	$(V)echo "$(MAKEFILE_DEP_LINE)" >> $(@:o=d)

# How to build vanilla binaries.
$(BINDIR)/%: $(OBJDIR)/%.o 
	@echo + mk $@
	@mkdir -p $(@D)
	$(V)$(CPP) $(CFLAGS) $(IFLAGS) $(CUDA_IFLAGS) -o $@ $^ $(LDFLAGS) $(CUDA_LDFLAGS)
	
# For running binaries.
run-%: $(BINDIR)/%
	./$(BINDIR)/$*

# For deleting the build
clean: clean-gensrc
	rm -rf $(LIBDIR) $(OBJDIR) $(BINDIR) $(PTXDIR) computation_state/*
	rm -f ../compiler/*circuit*
	rm -f ../compiler/*spec*
	rm -f ../compiler/*.c
	rm -f ../compiler/*.defines
	rm -f ../compiler/backend/*.pyc
	rm -f ../compiler/backend/tmptime
	rm -f ../compiler/tmptime

clean-gensrc: 
	rm -rf $(GENSRCDIR)
	
# This magic automatically generates makefile dependencies
# for header files included from C source files we compile,
# and keeps those dependencies up-to-date every time we recompile.
# See 'mergedep.pl' for more information.
$(OBJDIR)/.deps: $(foreach dir, $(OBJDIRS), $(wildcard $(OBJDIR)/$(dir)/*.d))
	@mkdir -p $(@D)
	@$(PERL) mergedep.pl $@ $^

-include $(OBJDIR)/.deps

always: 
	@:

.PHONY: always
