#!/usr/bin/python

import inspect
import os
import re
from optparse import OptionParser
import sys
sys.path.append("/home/01934/srinath/pkg/cheetah/lib/python")

from Cheetah.Template import Template
import zcc_parser

# Go in apps_sfdl_gen/
PROVER_H = "_p.h"
VERIFIER_H = "_v.h" #Output in ginger
PROVER_IMPL = "_p.cpp"
VERIFIER_IMPL = "_v.cpp" #Output in ginger
CONSTANTS_H = "_cons.h"

VERIFIER_INP_GEN_H = "_v_inp_gen.h"
VERIFIER_INP_GEN_IMPL = "_v_inp_gen.cpp"

# Go in bin/
F1_INDEX = ".f1index"
GAMMA12 = ".gamma12"
GAMMA0 = ".gamma0"
QAP = ".qap" #Output in zaatar
PROVER_WORKSHEET = ".pws" #Prover worksheet (output if in worksheet mode)

# Go in apps_sfdl_hw/
VERIFIER_INP_GEN_HW_H = "_v_inp_gen_hw.h"
VERIFIER_INP_GEN_HW_IMPL = "_v_inp_gen_hw.cpp"
PROVER_EXO_HW_H = "_p_exo.h"
PROVER_EXO_HW_IMPL = "_p_exo.cpp"

#Directory that stores templates
DIR_TMPL = "templates/" 

#Templates
PROVER_WORKSHEET_TMPL = DIR_TMPL + "pws.tmpl"
GAMMA12_TMPL = DIR_TMPL + "gamma12.tmpl"
GAMMA0_TMPL = DIR_TMPL + "gamma0.tmpl"
CONSTANTS_H_TMPL = DIR_TMPL + "prover_cons.h.tmpl"
F1_INDEX_TMPL = DIR_TMPL + "f1_index.tmpl"

PROVER_GINGER_H_TMPL = DIR_TMPL + "prover.ginger.h.tmpl"
PROVER_GINGER_CC_TMPL = DIR_TMPL + "prover.ginger.cc.tmpl"
PROVER_ZAATAR_H_TMPL = DIR_TMPL + "prover.zaatar.h.tmpl"
PROVER_ZAATAR_CC_TMPL = DIR_TMPL + "prover.zaatar.cc.tmpl"

VERIFIER_INP_GEN_HW_H_TMPL = DIR_TMPL + "verifier_inp_gen_hw.h.tmpl"
VERIFIER_INP_GEN_HW_CC_TMPL = DIR_TMPL + "verifier_inp_gen_hw.cc.tmpl"
PROVER_EXO_HW_H_TMPL = DIR_TMPL + "prover_exo.h.tmpl"
PROVER_EXO_HW_CC_TMPL = DIR_TMPL + "prover_exo.cc.tmpl"

VERIFIER_GINGER_H_TMPL = DIR_TMPL + "verifier.ginger.h.tmpl"
VERIFIER_GINGER_CC_TMPL = DIR_TMPL + "verifier.ginger.cc.tmpl"
VERIFIER_ZAATAR_H_TMPL = DIR_TMPL + "verifier.zaatar.h.tmpl"
VERIFIER_ZAATAR_CC_TMPL = DIR_TMPL + "verifier.zaatar.cc.tmpl"

VERIFIER_INP_GEN_H_TMPL = DIR_TMPL + "verifier_inp_gen.h.tmpl"
VERIFIER_INP_GEN_CC_TMPL = DIR_TMPL + "verifier_inp_gen.cc.tmpl"

MAIN_GINGER_TMPL = DIR_TMPL + "main.ginger.cc.tmpl"
MAIN_ZAATAR_TMPL = DIR_TMPL + "main.zaatar.cc.tmpl"

class CodeGenerator():
	
  def __init__(self, output_dir, output_prefix, class_name, framework, worksheetMode):
    self.output_dir = output_dir
    self.output_prefix = output_prefix
    self.class_name = class_name
    zcc_parser.class_name = class_name
    self.framework = framework
    zcc_parser.framework = framework
    self.worksheetMode = worksheetMode
    
  def write_to_file(self, name, contents):
    f = open(os.path.join(self.output_dir,name), "w")
    f.write(contents)
    f.close()
  
  def generate_code_from_template(self, spec):
    text = zcc_parser.read_file(spec)
    zcc_parser.parse_text(text)

    computation_name = os.path.splitext(os.path.split(spec)[1])[0]

    OUTPUT_PREFIX = re.sub(r'/',r'_',self.output_prefix).upper()

    defs = {}
    defs['computation_name'] = computation_name
    defs['computation_classname'] = self.class_name
    defs['OUTPUT_PREFIX'] = OUTPUT_PREFIX
    defs['output_prefix'] = self.output_prefix

    (defs['num_io_vars'], defs['num_z_vars'], defs['computation_worksheet']) = zcc_parser.generate_computation_worksheet(text) #leads to variable creation

    if (self.worksheetMode):
      defs['computation'] = zcc_parser.generate_computation_dynamic("bin/" + self.class_name + PROVER_WORKSHEET)
    else:
      defs['computation'] = zcc_parser.generate_computation_static(text)

    #number of variables, number of basic constraints (chi) fixed from this point onwards

    t = Template(file=PROVER_WORKSHEET_TMPL, searchList=[defs]) 
    self.write_to_file("bin/" + self.class_name +  PROVER_WORKSHEET, t.__str__())

    #Generate the variable shuffling
    (defs['F1_index'], shuffledIndices) = zcc_parser.generate_F1_index()
    t = Template(file=F1_INDEX_TMPL, searchList=[defs]) 
    self.write_to_file("bin/" + self.class_name+F1_INDEX, t.__str__())

    if (self.framework=="GINGER"):
      defs['NzA'] = "n/a"
      defs['NzB'] = "n/a"
      defs['NzC'] = "n/a"
    else:
      qap_file_name = os.path.join(self.output_dir, "bin/" + self.class_name + QAP);
      (defs['NzA'], defs['NzB'], defs['NzC'], defs['num_constraints']) = zcc_parser.generate_zaatar_matrices(text, shuffledIndices, qap_file_name)
    
    #Write the prover
    if (self.framework=="GINGER"):
      t = Template(file=PROVER_GINGER_H_TMPL, searchList=[defs]) 
    else:
      t = Template(file=PROVER_ZAATAR_H_TMPL, searchList=[defs]) 
    self.write_to_file(self.output_prefix + PROVER_H, t.__str__())

    if (self.framework=="GINGER"):
      defs['gamma12_file_name'] = "bin/" + self.class_name + GAMMA12;
      t = Template(file=PROVER_GINGER_CC_TMPL, searchList=[defs]) 
    else:
      t = Template(file=PROVER_ZAATAR_CC_TMPL, searchList=[defs]) 
    self.write_to_file(self.output_prefix + PROVER_IMPL, t.__str__())

    #Write the constants file
    defs['constants'] = zcc_parser.generate_constants(zcc_parser.read_file(spec+".cons"))
    t = Template(file=CONSTANTS_H_TMPL, searchList=[defs]) 
    self.write_to_file(self.output_prefix + CONSTANTS_H, t.__str__())

    protectedFiles = {(PROVER_EXO_HW_H, PROVER_EXO_HW_H_TMPL), 
	(PROVER_EXO_HW_IMPL, PROVER_EXO_HW_CC_TMPL),
	(VERIFIER_INP_GEN_HW_H, VERIFIER_INP_GEN_HW_H_TMPL),
	(VERIFIER_INP_GEN_HW_IMPL, VERIFIER_INP_GEN_HW_CC_TMPL)}

    for (targetfile, tmplfile) in protectedFiles:
      filename = "apps_sfdl_hw/" + self.class_name + targetfile;
      try:
	filename_ = os.path.join(self.output_dir, filename)
	with file(filename_, 'r'):
	  os.utime(filename_, None) # Touch it if it exists
      except IOError as e:
	#The file doesn't exist, create it:
	t = Template(file=tmplfile, searchList=[defs]) 
	self.write_to_file(filename, t.__str__())

    #Create the input generator
    defs['create_input'] = zcc_parser.generate_create_input(text)
    t = Template(file=VERIFIER_INP_GEN_H_TMPL, searchList=[defs]) 
    self.write_to_file(self.output_prefix + VERIFIER_INP_GEN_H, t.__str__())
    t = Template(file=VERIFIER_INP_GEN_CC_TMPL, searchList=[defs]) 
    self.write_to_file(self.output_prefix + VERIFIER_INP_GEN_IMPL, t.__str__())

    # Produce the verifier code (ginger) or A,B,C matrices (Zaatar) and drivers
    if (self.framework=="GINGER"):
      self.write_ginger(defs, text)
    else:
      self.write_zaatar(defs, text)

  def write_ginger(self, defs, text):
    #Write verifier's header
    t = Template(file=VERIFIER_GINGER_H_TMPL, searchList=[defs]) 
    self.write_to_file(self.output_prefix + VERIFIER_H, t.__str__())

    #Write verifier's code
    defs['create_gamma0'] = zcc_parser.generate_gamma0(text)
    defs['create_gamma12'] = zcc_parser.generate_gamma12(text) #these routines generate chi
    defs['gamma12_file_name'] = "bin/" + self.class_name + GAMMA12;
    defs['gamma0_file_name'] = "bin/" + self.class_name + GAMMA0;
    t = Template(file=VERIFIER_GINGER_CC_TMPL, searchList=[defs])     
    self.write_to_file(self.output_prefix + VERIFIER_IMPL, t.__str__())
    
    t = Template(file=GAMMA12_TMPL, searchList=[defs])     
    self.write_to_file("bin/"+self.class_name+GAMMA12, t.__str__())
 
    t = Template(file=GAMMA0_TMPL, searchList=[defs])     
    self.write_to_file("bin/"+self.class_name+GAMMA0, t.__str__())
 
    #Write the driver
    defs['comp_parameters'] = zcc_parser.generate_ginger_comp_params(text) 
    t = Template(file=MAIN_GINGER_TMPL, searchList=[defs])     
    self.write_to_file(self.output_prefix + ".cpp", t.__str__()) 

  def write_zaatar(self, defs, text):
    #Write verifier's header
    t = Template(file=VERIFIER_ZAATAR_H_TMPL, searchList=[defs]) 
    self.write_to_file(self.output_prefix + VERIFIER_H, t.__str__())

    #Write verifier's code
    defs['load_qap'] = zcc_parser.generate_load_qap("bin/" + self.class_name + QAP)

    t = Template(file=VERIFIER_ZAATAR_CC_TMPL, searchList=[defs])     
    self.write_to_file(self.output_prefix + VERIFIER_IMPL, t.__str__())

    #Write A,B,C matrices
    #t = Template(file=QAP_TMPL, searchList=[defs])
    #self.write_to_file("bin/"+ self.class_name +  QAP, t.__str__()) 
 
    #Write the driver
    defs['comp_parameters'] = zcc_parser.generate_zaatar_comp_params() 
    t = Template(file=MAIN_ZAATAR_TMPL, searchList=[defs])     
    self.write_to_file(self.output_prefix + ".cpp", t.__str__()) 
    
def main():
  parser = OptionParser()
  parser.add_option("-c", "--classname", dest="classname")
  parser.add_option("-s", "--spec_file", dest="spec")
  parser.add_option("-o", "--output_prefix", dest="output_prefix")
  parser.add_option("-d", "--output_dir", dest="output_dir", default=".")
  parser.add_option("-b", "--bugginess", dest="bugginess", default= 0)
  parser.add_option("-t", "--framework", dest="framework", default="GINGER")
  parser.add_option("-m", "--metrics", dest="metrics", default=0)
  parser.add_option("-w", "--worksheetMode", dest="worksheetMode", default=1)
  (opt, args) = parser.parse_args()
  
  mandatories = ['output_prefix', 'classname', 'spec', 'framework']
  for m in mandatories:
    if not opt.__dict__[m]:
        parser.print_help()
        exit(-1)
    
  zcc_parser.printMetrics = int(opt.metrics);
  zcc_parser.proverBugginess = float(opt.bugginess);

  gen = CodeGenerator(opt.output_dir, opt.output_prefix, opt.classname,	
      opt.framework, int(opt.worksheetMode))
  gen.generate_code_from_template(opt.spec)
  
if __name__ == "__main__":
	main()
