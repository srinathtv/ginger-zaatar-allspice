#!/usr/bin/python
import os
import sys
import time
import random
from optparse import OptionParser
import math
import re

BUILD_COMMAND = "make"

def get_number_int(line):
  metric = re.findall(r"\d+", line)
  return int(metric[0])

def get_number_float(line):
  metric = re.findall(r"\d+.\d+", line)
  return float(metric[1])

def read_micro_entry(entry):
  cost = -1
  fp = open("micro2.rslt", "r")
  lines = fp.readlines()
  fp.close()

  for line in lines:
    if (line.find(entry) != -1):
      cost = get_number_float(line)
    else:
      continue
  return cost

def run_local_command(cmd):
  print "Executing :", cmd
  os.system(cmd)

def build_sfdl(computation, framework):
  build_command = \
      BUILD_COMMAND + " FRAMEWORK=" + framework  + \
      " SFDL_FILES=" + computation + " C_FILES=\"\" > build_output.txt"
  
  # Compile the computation under Ginger and Zaatar to get number of
  # constraints and variables; build files locally 
  command = "cd ../pepper/; make clean; " + build_command + "; cd"
  run_local_command(command)
  file_build_output = "../pepper/build_output.txt"
  return file_build_output

def read_file_as_lines(filename):
  fp = open(filename, "r")
  lines = fp.readlines()
  fp.close()
  return lines

def clean_build_information(filename):
  command = "cd ../pepper/; rm " + filename + "; cd"
  run_local_command(command)

def prepare_sfdl(computation, inputsize):
  file_path_sfdl = "../pepper/apps_sfdl/" + computation + ".sfdl"
  fp = open(file_path_sfdl, "r")
  lines = fp.read()
  fp.close()

  search_line = ""
  replace_line = ""
  if (computation == "fannkuch"):
    search_line = 'const L = \d+;'
    replace_line = "const L = " + str(inputsize) + ";" 
  else:
    search_line = 'const m = \d+;'
    replace_line = "const m = " + str(inputsize) + ";"

  lines = re.sub(search_line, replace_line, lines)
  fp = open(file_path_sfdl, "w")
  fp.write(lines)
  fp.close();

  # additional replacement in input size needed for dna_align
  if (computation == "dna_align"):
    file_path_sfdl = "../pepper/apps_sfdl/" + str(computation) + ".sfdl"
    fp = open(file_path_sfdl, "r")
    lines = fp.read()
    fp.close()
    search_line = 'const n = \d+;'
    replace_line = "const n = " + str(inputsize) + ";"
    lines = re.sub(search_line, replace_line, lines)
    fp = open(file_path_sfdl, "w")
    fp.write(lines)
    fp.close();

def main():
  # First take computation's name and input size
  parser = OptionParser()
  parser.add_option("-p", "--computation", dest="computation", default="0")
  parser.add_option("-i", "--inputsize", dest="inputsize", default="0")
  parser.add_option("-o", "--bigoh", dest="bigoh", default="0")
  (opt, args) = parser.parse_args()

  if (opt.computation == "0"):
    print "Pass --computation=[name]"
    sys.exit(1)
  elif (opt.inputsize == "0"):
    print "Pass --inputsize=[size]"
    sys.exit(1)
  elif (opt.bigoh == "0"):
    print "Pass --bigoh=[2|3|4]"
    sys.exit(1)

  if (opt.bigoh == 1):
    print "Outsourcing will not save work to the client under all three protocols"
    sys.exit(1)

  e = read_micro_entry("e_128_avg")
  d = read_micro_entry("d_128_avg")
  h = read_micro_entry("h_simel_128_avg")
  c = read_micro_entry("c_128_avg")
  f = read_micro_entry("f_mod_128_avg")
  f_div = read_micro_entry("f_div_128_128_avg");

  # run once for each protocol 
  frameworks = ["GINGER", "ZAATAR"]
  v_setup_cost_ging = 0
  v_setup_cost_zat = 0 
  v_setup_cmt = 0

  prepare_sfdl(opt.computation, opt.inputsize)
  for framework in frameworks:    
    file_build_output = build_sfdl(opt.computation, framework) 
    lines = read_file_as_lines(file_build_output)
    clean_build_information(file_build_output)

    num_variables = -1
    num_constraints = -1
    num_inputs = -1
    num_outputs = -1
    
    for line in lines:
      if (line.find("metric_num_constraints") != -1):
        num_constraints = get_number_int(line)
      elif (line.find("metric_num_intermediate_vars") != -1):
        num_variables = get_number_int(line)
      elif (line.find("metric_num_input_vars") != -1):
        num_inputs = get_number_int(line)
      elif (line.find("metric_num_output_vars") != -1):
        num_outputs = get_number_int(line)
      else:
        continue;
   
    if (num_variables == -1 or num_constraints == -1):
      print "Couldn't read compiler's output to extract #vars & #constraints"
      sys.exit(1)

    rho = 8
  
    if (framework == "GINGER"):
      # analyze Ginger protocol here
      rho_lin = 15
      l_ging = 47
      s = int(num_variables)
      n = s + s*s
      Q = num_constraints * c
      issue_commit_queries = (e + 2 * c) * n
      issue_pcp_queries = rho * (Q + n * (2 * rho_lin * c + (l_ging + 1) * f))
      v_setup_cost_ging = issue_commit_queries + issue_pcp_queries
    
    elif (framework == "ZAATAR"):
      # analyze both Zaatar protocol
      u = num_variables + num_constraints
      C = num_constraints
      K = num_constraints
      K2 = K
      rho_lin = 20
      l_prime = 6*rho_lin + 4

      const_comp_specific_queries = \
        rho * (c + (f_div + 5 * f) * C + f*K + 3 * f * K2) 
      
      const_comp_obl_queries = \
        (e+2*c + rho * (2*rho_lin*c+l_prime*f)) * u

      v_setup_cost_zat = \
        const_comp_specific_queries + const_comp_obl_queries
     
      # analyze batch-CMT
      command = "cd ../pepper/; ./bin/cmtgkr/apps/pws_stat bin/" + \
          opt.computation + ".pws > build_output_cmt.txt"
      run_local_command(command)
      
      fp = open("../pepper/build_output_cmt.txt", "r")
      lines2 = fp.readlines()
      fp.close()
      
      command = "cd ../pepper/; rm build_output_cmt.txt; cd"
      run_local_command(command)
 
      d_psi = -1;
      G_psi_ck = -1;
      for line2 in lines2:
        if (line2.find("num_layers") != -1):
          d_psi = get_number_int(line2)
        elif (line2.find("num_avg_gates_per_layer") != -1):
          G_psi_ck = get_number_int(line)
        else:
          continue;
      if (d_psi == -1 or G_psi_ck == -1):
        print "Couldn't read pws_stat output"
        sys.exit(1)
      
      v_setup_cmt = f * d_psi * G_psi_ck * 11 
  
  # Use opt.bigoh to check if CMT-batching leads to a breakeven batch size
  input_sizes = [5,10] 
  cmp_ops = [0,0]
  ineq_ops = [0,0]
  i = 0
  for input_size in input_sizes:
    prepare_sfdl(opt.computation, input_size)
    file_build_output = build_sfdl(opt.computation, "ZAATAR")
    clean_build_information(file_build_output)

    command = "cd ../pepper/; ./bin/cmtgkr/apps/pws_stat bin/" + \
          opt.computation + ".pws > build_output_cmt.txt"
    run_local_command(command)
      
    fp = open("../pepper/build_output_cmt.txt", "r")
    lines2 = fp.readlines()
    fp.close()
      
    command = "cd ../pepper/; rm build_output_cmt.txt; cd"
    run_local_command(command)
 
    num_local_cmps = -1
    num_local_ineqs = -1
    for line2 in lines2:
      if (line2.find("num_local_ineqs") != -1):
        num_local_ineqs = get_number_int(line2)
      elif (line2.find("num_local_cmps") != -1):
        num_local_cmps = get_number_int(line2)
      else:
        continue
    
    if (num_local_ineqs == -1 or num_local_cmps == -1):
      print "Couldn't read pws_stat output"
      sys.exit(1)

    # Set to 1 if the number of ops is 0 
    if (num_local_cmps == 0):
      num_local_cmps = 1
    if (num_local_ineqs == 0):
      num_local_ineqs = 1

    cmp_ops[i] = num_local_cmps
    ineq_ops[i] = num_local_ineqs
    i = i + 1

  growth_cmps = float(cmp_ops[1])/float(cmp_ops[0])
  growth_ineq = float(ineq_ops[1])/float(ineq_ops[0])
 
  

  if (growth_cmps >= 2**int(opt.bigoh) or growth_ineq >= 2**int(opt.bigoh)):
    # CMT isn't applicable; so set its costs to max of Ginger's and Zaatar's
    if (v_setup_cost_ging < v_setup_cost_zat):
      v_setup_cmt = v_setup_cost_zat + 1
    else:
      v_setup_cmt = v_setup_cost_ging + 1
    
  # Prints a command to run that protocol
  if (v_setup_cost_ging < v_setup_cost_zat and v_setup_cost_ging < v_setup_cmt):
    file_build_output = build_sfdl(opt.computation, "GINGER") 
    clean_build_information(file_build_output)
    print "Ginger will have the lowest breakeven batch sizes"
    print "Run ./bin/" + opt.computation
  elif (v_setup_cost_zat < v_setup_cost_ging and v_setup_cost_zat < v_setup_cmt):
    file_build_output = build_sfdl(opt.computation, "ZAATAR") 
    clean_build_information(file_build_output)
    print "Zaatar will have the lowest breakeven batch sizes"
    print "Run ./bin/" + opt.computation
  else:
    file_build_output = build_sfdl(opt.computation, "ZAATAR") 
    clean_build_information(file_build_output)
    print "CMT will have the lowest breakeven batch sizes"
    print "Run ./bin/cmtgkr/pws_cmtgkr bin/" + opt.computation + ".pws" 
  
  print "Growth in comparisons is " + str(growth_cmps)
  print "Growth in ineq is " + str(growth_ineq)

if __name__ == "__main__":
  main()
