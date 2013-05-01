#!/usr/bin/python
import inspect
import math
import os
import random
import sys
import re
import zcc_parser_static
import math

INPUT_TAG = "INPUT"
OUTPUT_TAG = "OUTPUT"
CONSTRAINTS_TAG = "CONSTRAINTS"
VARIABLES_TAG = "VARIABLES"

END_TAG = "END_"
START_TAG = "START_"

m = 0  # input size
chi = 0 # number of constraints
NzA = 0 # Number of nonzero elements in the A matrices 
NzB = 0 # Number of nonzero elements in the B matrices
NzC = 0 # Number of nonzero elements in the C matrices
input_vars = []  # input 
output_vars = []  # output 
variables = []  #variables 
proverBugginess = 0 # a number from 0 to 1 determining the probability of
		    # the prover skipping a proof variable.

def read_file(filename):
  f = open(filename)
  text = f.read()
  f.close()
  return text

# helper method for read_vars_section. Returns the created variable object
def read_var(line, var_dict, index_offset):
  # handle comments
  line = line.split()

  if (line[0] in var_dict):
    return var_dict[line[0]]

  var_obj = {}
  var_obj["name"] = line[0]
  var_obj["index"] = (index_offset + len(var_dict)) #get the next available index

  if (len(line) >= 2):
    var_obj["comment"] = line[1]
    var_obj["type"] = line[2]
    if (var_obj["type"] == "int"):
      var_obj["na"] = int(line[4])
      var_obj["nb"] = 0
    elif (var_obj["type"] == "float"):
      var_obj["na"] = int(line[4])
      var_obj["nb"] = int(line[6])
    elif (var_obj["type"] == "uint"):
      var_obj["na"] = int(line[4])   
      var_obj["nb"] = 0

  var_dict[line[0]] = var_obj
  return var_obj

#decodes a variables block (i.e. INPUTS, OUTPUTS, or VARIABLES)
def read_vars_section(text, tag, index_offset):
  vars = {}
  start = text.find(START_TAG + tag)
  end = text.find(END_TAG + tag)
  txt_lines = text[start:end+1].split("\n")[1:-1]
  for line in txt_lines:
    read_var(line, vars, index_offset)
  
  return vars  

def parse_text(text):
  global input_vars
  input_vars = read_vars_section(text, INPUT_TAG, 0) #number inputs starting at 0
  global output_vars
  output_vars = read_vars_section(text, OUTPUT_TAG, len(input_vars)) #number outputs starting where inputs left off
  global variables
  variables = read_vars_section(text, VARIABLES_TAG, 0) #start over again in numbering variables
  #print "INPUT_VARS:", input_vars
  #print "OUTPUT_VARS:", output_vars
  #print "VARIABLES:", variables

def get_bits_difference(arg0, var0, arg1, var1):
  na1 = max(var0["na"], var1["na"])
  nb1 = max(var0["nb"], var1["nb"])

  if (nb1 == 0):
    # Special case for compare against 0.
    if (arg0 == "0" or arg1 == "0"):
      na1 = na1;
    else:
      na1 = na1 + 1;
  else:
    # Special case for compare against 0.
    if (arg0 == "0" or arg1 == "0"):
      na1 = na1;
    else:
      na1 = na1 + nb1 + 1;

  return (na1, nb1);

def less_than_as_basic_constraints(arg0, arg1, target):
  (var0, var0name) = to_var(arg0)
  (var1, var1name) = to_var(arg1)

  (na1, nb1) = get_bits_difference(arg0, var0, arg1, var1);

  if (nb1 == 0):
    return less_than_as_basic_constraints_i(na1, arg0, arg1, target)
  else:
    return less_than_as_basic_constraints_f(na1, nb1, arg0, arg1, target)
  
def less_than_as_basic_constraints_i(na2, arg0, arg1, target):
  def f(varName):
    return "%s$%s" % (target, varName)
  toRet = []

  # Check the relationship between Ni and N
  # Python ints are arbitrary precision! Yay!
  pot = 1
  diffFromPot = ""
  Nsum = "( ) * ( ) + ( "
  for i in range(0, na2-1):
    Ni = f("N%d" % (i))
    toRet += ["( %s ) * ( %s - %s ) + ( )" % (Ni, pot, Ni)]
    diffFromPot += " - %s" % (Ni)
    pot = pot * 2
  diffFromPot += " + %s" % (pot)

  # Check relationship between Mlt, Meq, Mgt
  Mlt = f("Mlt")
  Meq = f("Meq")
  Mgt = f("Mgt")
  toRet += [
    "( %s ) * ( %s - 1 ) + ( )" % (Mlt, Mlt),
    "( %s ) * ( %s - 1 ) + ( )" % (Meq, Meq),
    "( %s ) * ( %s - 1 ) + ( )" % (Mgt, Mgt),
    "( ) * ( ) + ( %s + %s + %s - 1 )" % (Mlt, Meq, Mgt),
    ]

  # If Mlt, check Y1 < Y2
  toRet += ["( %s ) * ( %s - %s + %s ) + ( )" % 
    (Mlt, arg0, arg1, diffFromPot)]

  # If Meq, check Y1 = Y2
  toRet += ["( %s ) * ( %s - %s ) + ( )" % 
    (Meq, arg0, arg1)]

  # If Mlt, check Y2 < Y1
  toRet += ["( %s ) * ( %s - %s + %s ) + ( )" % 
    (Mgt, arg1, arg0, diffFromPot)]

  # Output matching constraint
  toRet += ["( ) * ( ) + ( %s  - %s )" % (f("Mlt"), target)]
  return toRet


def less_than_as_basic_constraints_f(na2, nb2, arg0, arg1, target):
  def f(varName):
    return "%s$%s" % (target, varName)
  toRet = []

  #XXX Currently we ignore the type system, and always use fixed-size less than gates. 
  #When the type system starts being more agressive, we will use its bounds instead.
  
  #na2 = 100
  #nb2 = 32
  
  #print "Warning - na=", na2, "/nb=", nb2," '<' gate being used, ignoring input type!";

  # Check the relationship between Ni and N
  # Python ints are arbitrary precision! Yay!
  pot = 1
  Nsum = "( ) * ( ) + ( "
  for i in range(0, na2):
    Ni = f("N%d" % (i))
    toRet += ["( %s ) * ( %s - %s ) + ( )" % (Ni, pot, Ni)]
    pot = pot * 2
    Nsum += "- %s " % (Ni)
  Nsum += "+ %s + %s ) " % (f("N"), pot)
  toRet += [Nsum]

  # Check relationship between Di and D
  Dsum = "( ) * ( ) + ( "
  Dcount = "( ) * ( ) + ( "
  for i in range(0, nb2+1):
    Di = f("D%d" % (i))
    toRet += ["( %s ) * ( %s - 1 ) + ( )" % (Di, Di)]
    Dsum += "- %s * %s " % (2**(nb2 - i),Di)
    Dcount += "+ %s " % (Di)
  Dsum += "+ %s * %s ) " % (2**nb2, f("D"))
  Dcount += " - 1 ) "
  toRet += [Dsum, Dcount]

  # Check relationship between Mlt, Meq, Mgt
  Mlt = f("Mlt")
  Meq = f("Meq")
  Mgt = f("Mgt")
  toRet += [
    "( %s ) * ( %s - 1 ) + ( )" % (Mlt, Mlt),
    "( %s ) * ( %s - 1 ) + ( )" % (Meq, Meq),
    "( %s ) * ( %s - 1 ) + ( )" % (Mgt, Mgt),
    "( ) * ( ) + ( %s + %s + %s - 1 )" % (Mlt, Meq, Mgt),
    ]

  # Check relationship between N, D, and ND
  toRet += ["( %s ) * ( %s ) + ( - %s )" % (f("N"), f("D"), f("ND"))]

  # If Mlt, check Y1 < Y2
  toRet += ["( %s ) * ( %s - %s - %s ) + ( )" % 
    (Mlt, arg0, arg1, f("ND"))]

  # If Meq, check Y1 = Y2
  toRet += ["( %s ) * ( %s - %s ) + ( )" % 
    (Meq, arg0, arg1)]

  # If Mlt, check Y2 < Y1
  toRet += ["( %s ) * ( %s - %s - %s ) + ( )" % 
    (Mgt, arg1, arg0, f("ND"))]

  # Output matching constraint
  toRet += ["( ) * ( ) + ( %s  - %s )" % (f("Mlt"), target)]
  return toRet

def not_equal_as_basic_constraints(arg0, arg1, target):
  def f(varName):
    return "%s$%s" % (target, varName)
  toRet = []
  
  X1 = arg0
  X2 = arg1
  M = f("M")
  Y = target

  if target in output_vars and framework == "GINGER":
    Y = f("M2")

  # Constraint: Y - (X1 - X2) M 
  toRet += ["( %s ) * ( %s - %s ) + ( - %s )" % (M, X1, X2, Y)] 
  # Constraint: (X1 - X2) - (X1 - X2)*Y = Y*(X1 - X2) + (- X1 + X2) (multiplying by -1)
  toRet += ["( %s ) * ( %s - %s ) + ( - %s + %s )" % (Y, X1, X2, X1, X2)]

  if target in output_vars and framework == "GINGER":
    toRet += ["( ) * ( ) + ( %s - %s )" % (Y, target)]

  return toRet

def split_as_basic_constraints(terms):
  # No auxilliary variables needed. 
  toRet = []

  terms.pop(0) #SIL
  typename = terms.pop(0) #uint or int
  signed = {
    "int": True,
    "uint": False,
    } [typename]
  terms.pop(0) #bits
  numBits = int(terms.pop(0)) #length
  terms.pop(0) #X
  toSplit = terms.pop(0)
  terms.pop(0) #Y0
  output_start = int(terms.pop(0)[1:]) #output starting variable (must start with V. )

  matchConstraint = ""
  pot = 1
  for i in range(0, numBits):
    bitvar = "V%d"%(output_start + i)
    #Check that this var is boolean in value
    toRet += ["( %s ) * ( %s - 1 ) + ( )" % (bitvar, bitvar)]

    signedPot = pot
    if (i == numBits - 1 and signed):
      signedPot = -pot
    matchConstraint += "+ %s * %s " % (signedPot, bitvar)
    pot *= 2

  toRet += ["( ) * ( ) + ( %s - %s )" % (matchConstraint, toSplit)]
   
  return toRet


#converts a general constraint to a list of basic constraints, or throws an error if this is not possible.
def to_basic_constraints(line):
  #shortcircuiting
  if (line.startswith("}") or line.startswith("shortcircuit")):
    return []

  toRet = []
  if "!=" in line:
    terms = re.split("\\s+", line)
    toRet = not_equal_as_basic_constraints(terms[0], terms[2], terms[4])
  elif "<" in line:
    terms = re.split("\\s+", line)
    toRet = less_than_as_basic_constraints(terms[0], terms[2], terms[4])
  elif "SIL" in line:
    terms = re.split("\\s+", line)
    toRet = split_as_basic_constraints(terms)
  else:
    toRet = [line]

  toRet_expanded = []
  for bc in toRet:
    toRet_expanded += [expand_basic_constraint(bc)]
  toRet = toRet_expanded 

  return toRet

def expand_basic_constraint(bc):
  tokens = re.split("\\s+", bc)
  expansion = ""

  global framework
  if (framework == "GINGER"):
    tokens = ["("] + tokens + [")"]
    expansion += expand_polynomial_str(tokens)
  if (framework == "ZAATAR"):
    expansion += "( " 
    expansion += expand_polynomial_str(tokens)
    expansion += " ) " 
    expansion += tokens.pop(0)
    expansion += " ( " 
    expansion += expand_polynomial_str(tokens)
    expansion += " ) " 
    expansion += tokens.pop(0)
    expansion += " ( " 
    expansion += expand_polynomial_str(tokens)
    expansion += " )" 

  return expansion

def expand_polynomial_str(tokens):
  expanded = expand_polynomial(tokens)
  
  expanded_list = []
  for term in expanded:
    expanded_list += [" * ".join(term)]
  return " + ".join(expanded_list)

#Takes a polynomial which starts with a (, reads a polynomial until the matching ) is reached,
#and returns the expanded polynomial of the terms inside the parens.
#An expanded polynomial always has terms of the form constant * v1 * ... vn
def expand_polynomial(tokens):
  nesting = 1

  if (tokens.pop(0) != "("):
    raise Exception("Format error")

  multiply = []
  expansion = {}
  
  while(nesting > 0):
    if (tokens[0] == "("): #Recurse
      subPoly = expand_polynomial(tokens)
      if (multiply == []):
	multiply = subPoly
      else:
	newMult = []
        for term1 in multiply:
	  for term2 in subPoly:
	    newMult += product_term(term1,term2)
	multiply = newMult
    else:
      token = tokens.pop(0)
    
      if (token == ")" or token == "+" or token == "-"):
	for term in multiply:
	  expand_add_term(expansion, term)
	multiply = []
     
      if (token == ""):
	continue 
      elif (token == ")"):
	nesting = nesting - 1
      elif (token == "+"):
	pass
      elif (token == "-"):
	multiply += [["-1"]]    
      elif (token == "*"):
	pass
      else:
	if (multiply == []):
	  multiply = product_term(["1"], [token])
	else:
	  newMult = []
	  for term1 in multiply:
	    newMult += product_term([token], term1)
	    
	  multiply = newMult

  for term in multiply:
    expand_add_term(expansion, term)
  return expansion.values()

def expand_add_term(expansion, termlist):
  termlistNoConst = termlist
  mults = 1
  try:
    a = int(termlist[0])
    mults = a
    termlistNoConst = termlist[1:]
  except:
    pass

  termlistNoConst.sort()
  key = "!".join(termlistNoConst)
  if key in expansion:
    gotlist = expansion[key]
    del expansion[key]
    multo = int(gotlist[0])
    multo += mults;
    gotlist[0] = str(multo)
    if (multo != 0):
      expansion[key] = gotlist
  else:
    termo = [str(mults)]
    termo += termlistNoConst
    expansion[key] = termo

#Returns a list holding the list of the union of two factor lists, but always puts the constant term at the start of the union
#Note that if the constant term ends up being 0, the empty list is returned
def product_term(a, b):
  if (a == [] or b == [] or a == ['']):
    raise Exception("Assertion error")

  aHasConst = True
  bHasConst = True
  try:
    int(a[0])
  except:
    aHasConst = False

  try:
    int(b[0])
  except:
    bHasConst = False

  if (aHasConst and bHasConst):
    product = int(a[0]) * int(b[0])
    if (product == 0):	
      return []
    return [[str(product)] + a[1:] + b[1:]]
  if (bHasConst):
    return [[b[0]] + a + b[1:]]
  return [a + b]

#converts a basic constraint (a degree two polynomial constraint) to a tuple useful in filling in the gamma0 / gamma1/2 vectors.
def parse_basic_constraint(line):
  #print constraint
  deg1_pos = []
  deg1_coeff = []
  deg2_pos = []
  deg2_coeff = []
  ip_op_pos = []
  ip_op_coeff = []
  consts = []
  
  #Split on terms
  line = re.sub("\\s+\\*\\s+", "*", line)
  terms = re.split("\\s+", line)
  while terms != []:
    neg = False
    while True:    
      if (terms[0] == "+"):
	terms = terms[1:]
      elif (terms[0] == "-"):
	neg = not neg
	terms = terms[1:]
      elif (terms[0] == ""):
	terms = terms[1:]
      else:
	break

    term = terms[0]   
    terms = terms[1:]

    coeff = "1"
    factors = term.split("*")
    for i in range(0, len(factors)):
      factor = factors[i]
      if factor[i].isdigit() or (factor[i] == "-"):
	coeff = factor
	factors[i] = ""
	break
    factors = filter(None, factors)    
    term = "*".join(factors) # Factors not including the coeff
  
    if neg:
      coeff = "-" + coeff
    
    if (term != ""): 
      if term in input_vars: #Input / Output
	index = input_vars[term]["index"]
	ip_op_pos.append(index)
	ip_op_coeff.append(coeff)

      elif term in output_vars:
	index = output_vars[term]["index"]
	ip_op_pos.append(index)
	ip_op_coeff.append(coeff)

      else: # Variables
	degree = term.count("*") + 1
	if degree >= 3:
	  print "ERROR: degree of a term more than 2 in constraint"
	  print line
	  sys.exit(1)

	if degree == 1:
	  index = variables[term]["index"]
	  deg1_pos.append(" F1_index[%d] " % (index))
	  deg1_coeff.append(coeff)

	elif degree == 2:
	  term_vars = term.split("*")
	  index1 = variables[term_vars[0]]["index"]
	  index2 = variables[term_vars[1]]["index"]
	  index = " F1_index[%d] * num_vars + F1_index[%d] " % (index1, index2)
	  deg2_pos.append(index)
	  deg2_coeff.append(coeff)
	  
    else:
      consts.append(coeff)

  return (consts, ip_op_pos, ip_op_coeff, deg1_pos, deg1_coeff, deg2_pos, deg2_coeff)

def generate_ginger_comp_params(text):
  num_constraints = chi #count_lines(text, CONSTRAINTS_TAG)
  num_inputs = len(input_vars) #count_lines(text, INPUT_TAG)
  num_outputs = len(output_vars) #count_lines(text, OUTPUT_TAG)
  num_vars = len(variables) # count_lines(text, VARIABLES_TAG)
  file_name_f1_index = "bin/" + class_name + ".f1index"  
  
  code = """
  num_cons = %s;
  num_inputs = %s;
  num_outputs = %s;
  num_vars = %s;
  const char *file_name_f1_index = \"%s\";
  """ % (num_constraints, num_inputs, num_outputs, num_vars, file_name_f1_index)

  if (printMetrics):
    print("""
metric_num_constraints %s %d
metric_num_input_vars %s %d
metric_num_output_vars %s %d
metric_num_intermediate_vars %s %d
    """ % (class_name, num_constraints, class_name, num_inputs, class_name, num_outputs, class_name, num_vars))
 
  return code

def generate_zaatar_comp_params():
  num_constraints = chi #count_lines(text, CONSTRAINTS_TAG)
  num_inputs = len(input_vars) #count_lines(text, INPUT_TAG)
  num_outputs = len(output_vars) #count_lines(text, OUTPUT_TAG)
  num_vars = len(variables) # count_lines(text, VARIABLES_TAG)
  file_name_qap = "bin/" + class_name + ".qap"  
  file_name_f1_index = "bin/" + class_name + ".f1index"  

  num_aij = NzA
  num_bij = NzB
  num_cij = NzC

  code = """
  num_cons = %s;
  num_inputs = %s;
  num_outputs = %s;
  num_vars = %s;
  num_aij = %s;
  num_bij = %s;
  num_cij = %s;
  const char *file_name_qap = \"%s\";
  const char *file_name_f1_index = \"%s\";
  """ % (num_constraints, num_inputs, num_outputs, num_vars, num_aij, num_bij, num_cij, file_name_qap, file_name_f1_index)

  if (printMetrics):
    print("""
metric_num_constraints %s %d
metric_num_input_vars %s %d
metric_num_output_vars %s %d
metric_num_intermediate_vars %s %d
metric_num_Nz(A) %s %d
metric_num_Nz(B) %s %d
metric_num_Nz(C) %s %d
    """ % (class_name, num_constraints, class_name, num_inputs,
    class_name, num_outputs, class_name, num_vars, class_name, num_aij,
    class_name, num_bij, class_name, num_cij))
 
  return code

def convert_to_compressed_polynomial(j, polynomial, shuffled_indices):
  num_inputs = len(input_vars) #count_lines(text, INPUT_TAG)
  num_outputs = len(output_vars) #count_lines(text, OUTPUT_TAG)
  num_vars = len(variables) # count_lines(text, VARIABLES_TAG)
  
  i = -1
  coefficient = 0
  entries = ""
  
  terms = polynomial.split(" + ")
  for term in terms:
    i = -1
    coefficient = 0
    if term.find(" * ") == -1:
      # a constant term
      i = 0
      term = term.lstrip()
      term = term.rstrip()
      if (0 == int(term)):
        continue
      else:
        coefficient = term
    else:
      (coefficient, variable) = term.split(" * ")
      index = int(variable[1:]) #remove the first character and store in index
      if (variable.startswith("V")):
        i = 1 + shuffled_indices[index]
      elif (variable.startswith("I")):
        i = 1 + num_vars + index
      elif (variable.startswith("O")):
        i = 1 + num_vars + index

    entries += "%d %d %s\n" % (i, j, coefficient)
  return entries

def append_files(fp, file_name_to_append):
  with open(file_name_to_append) as file_object:
    for line in file_object:
      fp.write(line);

def generate_zaatar_matrices(text, shuffled_indices, qap_file_name):
  #print "In generate_zaatar_matrices"
  file_matrix_a = qap_file_name + ".matrix_a";
  file_matrix_b = qap_file_name + ".matrix_b";
  file_matrix_c = qap_file_name + ".matrix_c";

  fp_matrix_a = open(file_matrix_a, "w");
  fp_matrix_b = open(file_matrix_b, "w");
  fp_matrix_c = open(file_matrix_c, "w");

  start = text.find(START_TAG + CONSTRAINTS_TAG)
  end = text.find(END_TAG + CONSTRAINTS_TAG)
  lines = text[start:end+1].split("\n")[1:-1]
  
  #Alist = []
  #Blist = []
  #Clist = []

  global NzA
  global NzB
  global NzC
 
  NzA = 0
  NzB = 0
  NzC = 0

 
  num_constraints = 0 
  j = 1
  for line in lines:
    basic_constraints = to_basic_constraints(line)  
    for bc in basic_constraints:
      tokens = re.split("\\s+", bc)
      (nasub, A2) = expand_polynomial_matrixrow(tokens)
      A3 = convert_to_compressed_polynomial(j, A2, shuffled_indices)
      #A += A2 + "\n"
      #Alist += [A3]
      fp_matrix_a.write(A3);
      NzA = NzA + nasub
      if (tokens.pop(0) != "*"):
	raise Exception("Format error")
      (bsub, B2) = expand_polynomial_matrixrow(tokens)
      #B += B2 + "\n"
      B3 = convert_to_compressed_polynomial(j, B2, shuffled_indices,)
      #Blist += [B3]
      fp_matrix_b.write(B3);
      NzB = NzB + bsub
      if (tokens.pop(0) != "+"):
	raise Exception("Format error")
      (csub, C2) = expand_polynomial_matrixrow(tokens)
      NzC = NzC + csub
      C3 = convert_to_compressed_polynomial(j, C2, shuffled_indices)
      #C += C2 + "\n"
      #Clist += [C3]
      fp_matrix_c.write(C3)
      
      j = j + 1
      num_constraints = num_constraints + 1
  
  # The following is much faster than repeated concatenation
  #A = ''.join(Alist)
  #B = ''.join(Blist)
  #C = ''.join(Clist)

  global chi
  # set it to next power of 2 minus 1 so that \chi+1 is a power of 2
  num = num_constraints + 1;
  #bit_length = num.bit_length();
  #chi = int(math.pow(2, bit_length)) - 1
  chi = int(pow(2, math.ceil(math.log(num, 2)))) - 1
  #chi = num_constraints
  
  print "metric_num_constraints_before_round %s %d" % (class_name, num_constraints);
  print "metric_num_constraints_after_round %s %d" % (class_name, chi);

  print "metric_num_constraints_nonpot %s %d" % (class_name, num_constraints);

  if NzA == 0:
    NzA = 1
    fp_matrix_a.write("0 0 0\n")

  if NzB == 0:
    NzB = 1
    fp_matrix_b.write("0 0 0\n")

  if NzC == 0:
    NzC = 1
    fp_matrix_c.write("0 0 0\n")


  fp_matrix_a.close()
  fp_matrix_b.close()
  fp_matrix_c.close()

  fp = open(qap_file_name, "w")
  append_files(fp, file_matrix_a);
  fp.write("\n");
  append_files(fp, file_matrix_b);
  fp.write("\n");
  append_files(fp, file_matrix_c);
  fp.write("\n");
  fp.close();
  return (NzA, NzB, NzC, chi)

# Expands a polynomial, and replaces variable names with the (unshuffled) variable numbering
def expand_polynomial_matrixrow(tokens):
  expanded = expand_polynomial(tokens)

  variablesChanged = []
  for term in expanded:
    newList = []
    for factor in term:
      (var, renumbered_name) = to_var(factor)
      newList += [renumbered_name]
	
    variablesChanged += [newList]
      
  expanded = variablesChanged

  if (expanded == []):
    return (0, "0")
  
  numNonZeroTerms = 0
  expanded_list = []
  for term in expanded:
    numNonZeroTerms = numNonZeroTerms + 1
    expanded_list += [" * ".join(term)]
  return (numNonZeroTerms, " + ".join(expanded_list))

def generate_gamma0(text):
  num_deg1_terms = 0
  num_deg2_terms = 0
  deg1_pos = []
  deg2_pos = []
  ip_op_pos = []
  deg1_alpha = []
  deg2_alpha = []
  ip_op_alpha = []
  deg1_coeff = []
  deg2_coeff = []
  ip_op_coeff = []
  count = 0

  start = text.find(START_TAG + CONSTRAINTS_TAG)
  end = text.find(END_TAG + CONSTRAINTS_TAG)
  lines = text[start:end+1].split("\n")[1:-1]
  constraint_id = 0
  code = []
  
  for line in lines:
    basic_constraints = to_basic_constraints(line)  
    for bc in basic_constraints:
      (consts, io_varid, io_coeff, deg1_varid, deg1_coeff, deg2_varid, deg2_coeff) = parse_basic_constraint(bc)
      
      # use alpha[constraint_id] and fill in \gamma_0
     
      #literal const * input/output variable constants
      term_id = 0
      for var_id in io_varid:
	code+= ["G %s %d %d" % (io_coeff[term_id], constraint_id, var_id)]
	term_id = term_id + 1

      #literal constants
      for const in consts:
	code+= ["C %s %d 0" % (const, constraint_id)]

      constraint_id = constraint_id + 1
  
  #We have just discovered how many basic constraints are in the specification.
  global chi
  chi = constraint_id

  if (printMetrics):
    print "metric_num_gamma0 %s %d" % (class_name, len(code)) 

  return "\n".join(code)

#always called BEFORE generate_gamma0 (in the actual ginger framework)
def generate_gamma12(text):
  num_deg1_terms = 0
  num_deg2_terms = 0
  deg1_pos = []
  deg2_pos = []
  ip_op_pos = []
  deg1_alpha = []
  deg2_alpha = []
  ip_op_alpha = []
  deg1_coeff = []
  deg2_coeff = []
  ip_op_coeff = []
  count = 0

  start = text.find(START_TAG + CONSTRAINTS_TAG)
  end = text.find(END_TAG + CONSTRAINTS_TAG)
  lines = text[start:end+1].split("\n")[1:-1]
  constraint_id = 0
  code = []
  
  for line in lines:
    basic_constraints = to_basic_constraints(line)  
    for bc in basic_constraints:
     
      (consts, io_varid, io_coeff, deg1_varid, deg1_coeff, deg2_varid, deg2_coeff) = parse_basic_constraint(bc)
 
      # use alpha[constraint_id] and fill in \gamma_1, & \gamma_2
     
      term_id = 0
      for var_id in deg1_varid:
        var_id = var_id.replace(" F1_index[", "");
        var_id = var_id.replace("] ", "");
	code += ["1 %s %s %s 0" % (deg1_coeff[term_id], constraint_id, var_id) ]

	term_id = term_id + 1

      term_id = 0
      for var_id in deg2_varid:
        var_id = var_id.replace(" F1_index[", "");
        var_id = var_id.replace("] ", "");
        var_id = var_id.replace("num_vars +", "");
        (i, j) = var_id.split("*");
        code += ["2 %s %s %s %s" % (deg2_coeff[term_id], constraint_id, i, j) ]

	term_id = term_id + 1
      
      constraint_id = constraint_id + 1

  #We have just discovered how many basic constraints are in the specification.
  global chi
  chi = constraint_id

  if (printMetrics):
    print "metric_num_gamma12 %s %d" % (class_name, len(code)) 

  return "\n".join(code)

##########################
## Generate computation dynamic (i.e. with pws file)
##########################

#Returns (null, varname) if varname is not a constant or variable.
def to_var(varname):
  if varname in input_vars:
    var = input_vars[varname]
    return (var, "I%d" % (var["index"])) 
  elif varname in output_vars:
    var = output_vars[varname]
    return (var, "O%d" % (var["index"])) 
  elif varname in variables:
    var = variables[varname]
    return (var, "V%d" % (var["index"])) 
  elif varname == "-":
    return (None, varname)
  elif varname[0].isdigit() or varname[0] == '-':
    #Fractions are handled in the frontend. All constants here are integers.
    constVar = {}
    constVar["name"] = varname
    val = abs(int(varname))
    if val == 0: 
      constVar["na"] = 1
    else:
      constVar["na"] = int(math.log(val,2) + 1)
    constVar["nb"] = 0
    return (constVar, varname)
  else:
    return (None, varname)

#helper function for adding variables during the prover's computation
def prover_var(prefix, varName):
  return read_var("%s$%s" % (prefix, varName), variables, 0) # New variable

# Honest prover's implementation of not equal
def generate_computation_not_equals(arg0, arg1, target):
  def pv(name):
    var = prover_var(target, name)
    return "V%d" % (var["index"])
  def f(varname):
    (var, renumbered_name) = to_var(varname)
    return renumbered_name

  Mvar = pv("M")
  if target in output_vars and framework == "GINGER":
    # Make an intermediate for the output in this case
    Mvar2 = pv("M2")  
    return "".join(["!= M %s X1 %s X2 %s Y %s\n" % (Mvar, f(arg0), f(arg1), Mvar2),
		   "P %s = %s E\n" %(f(target), Mvar2)])
  else:
    return "!= M %s X1 %s X2 %s Y %s\n" % (Mvar, 
	f(arg0), f(arg1), f(target))

# Honest prover's implementation of less than
def generate_computation_less(arg0, arg1, target):
  (var0, var0name) = to_var(arg0)
  (var1, var1name) = to_var(arg1)

  (na1, nb1) = get_bits_difference(arg0, var0, arg1, var1);

  if (nb1 == 0):
    return generate_computation_less_i(na1, arg0, arg1, target)
  else:
    return generate_computation_less_f(na1, nb1, arg0, arg1, target)

def generate_computation_less_i(N, arg0, arg1, target):
  def pv(name):
    var = prover_var(target, name)
    return "V%d" % (var["index"])
  def f(varname):
    (var, renumbered_name) = to_var(varname)
    return renumbered_name

  MltVar = pv("Mlt")
  MeqVar = pv("Meq")
  MgtVar = pv("Mgt")

  N0Var = pv("N0")
  for i in range(1,N-1): #1, ... N-2
    pv("N%d" % (i))    
  
  return "<I N_0 %s N %d Mlt %s Meq %s Mgt %s X1 %s X2 %s Y %s\n" % (
      N0Var, N, MltVar, MeqVar, MgtVar, 
      f(arg0), f(arg1), f(target))

def generate_computation_less_f(na2, nb2, arg0, arg1, target):
  def pv(name):
    var = prover_var(target, name)
    return "V%d" % (var["index"])
  def f(varname):
    (var, renumbered_name) = to_var(varname)
    return renumbered_name

  MltVar = pv("Mlt")
  MeqVar = pv("Meq")
  MgtVar = pv("Mgt")
  NumVar = pv("N")
  DenVar = pv("D")
  NDVar = pv("ND")

  N0Var = pv("N0")
  for i in range(1,na2): #1, ... na2 - 1
    pv("N%d" % (i))    

  D0Var = pv("D0")
  for i in range(1,nb2+1): #1, ... nb2
    pv("D%d" % (i))    
  
  return "<F N_0 %s Na %d N %s D_0 %s Nb %d D %s ND %s Mlt %s Meq %s Mgt %s X1 %s X2 %s Y %s\n" % ( 
      N0Var, na2, NumVar, D0Var, nb2,
      DenVar, NDVar, MltVar, MeqVar, MgtVar, 
      f(arg0), f(arg1), f(target))

def generate_computation_split(terms):
  def f(varname):
    (var, renumbered_name) = to_var(varname)
    return renumbered_name

  toRet = ""
  for term in terms:
    toRet += f(term) + " "
  toRet += "\n"
  
  return toRet 

def generate_computation_exact_divide(target, source, constant):
  def f(varname):
    (var, renumbered_name) = to_var(varname)
    return renumbered_name

  return  "/ %s = %s / %s\n" % (f(target), f(source), constant)

# Honest prover's implementation of a polynomial, passed in as a sequence of tokens
def generate_computation_poly(target, tokens):
  def f(varname):
    (var, renumbered_name) = to_var(varname)
    return renumbered_name

  poly = "P %s = " % (f(target)) 
  
  for token in tokens:
    if (token != ""):
      poly += f(token) + " "
  poly += "E\n"
  return poly

#Given a list of tokens that starts with a (, pop(0) tokens from tokens until the matching ) is found. Pop that matching paren.
#All tokens between the parenthesis are returned as a list.
def read_poly(tokens):
  toRet = []
  nesting = 1
  if (tokens.pop(0) != "("):
    raise Exception("Format error")
  while(nesting > 0):
    token = tokens.pop(0)
    if (token == ")"):
      nesting = nesting-1
    elif (token == "("):
      nesting = nesting+1
      
    toRet += [token]
  
  toRet.pop() #Dont return the last )
  return toRet
    
def generate_computation_line(line):
  #determine what kind of computation is taking place
  if ("!=" in line):
    terms = re.split("\\s+",line)
    return generate_computation_not_equals(terms[0], terms[2], terms[4])
  elif ("<" in line):
    terms = re.split("\\s+",line)
    return generate_computation_less(terms[0], terms[2], terms[4])
  elif ("SIL" in line):
    terms = re.split("\\s+",line)
    return generate_computation_split(terms);
  else:
    tokens = re.split("\\s+", line)
    # Depends on whether we have zaatar or ginger constraints 
    global framework
    if (framework == "GINGER"):
      (constant, target) = get_poly_output(tokens)
      worksheet = ""
      worksheet += generate_computation_poly(target, tokens)
      if (constant != "1"): 
        worksheet += generate_computation_exact_divide(target, target, constant)
      return worksheet
    if (framework == "ZAATAR"):    
      polyA = read_poly(tokens)
      star = tokens.pop(0)
      if (star != "*"):
	raise Exception("Format error")
      polyB = read_poly(tokens)
      plus = tokens.pop(0)
      if (plus != "+"):
	raise Exception("Format error")
      polyC = read_poly(tokens) 
      (constant, target) = get_poly_output(polyC)

      poly = []
      if ((polyA == [] and polyB != []) or (polyA != [] and polyB == [])):
	raise Exception("Format error - nonempty A but empty B %s )" % line)
      if (polyA != []):
	poly += ["("] + polyA + [")","*","("] + polyB + [")"]
      if (polyC != []):
	if (polyA != []):
	  poly += ["+"]
	poly += polyC

      worksheet = ""
      worksheet += generate_computation_poly(target, poly)
      if (constant != "1"): 
        worksheet += generate_computation_exact_divide(target, target, constant)
      return worksheet
 
# For a polynomial constraint such as x1 * x2 - 4 * x3, returns ("4", x3)
# For a polynomial constraint x1 * x2 - x3, returns ("1", x3)
def get_poly_output(tokens): 
  target = tokens.pop()
  constant = "1"
  if (tokens[-1] == "*"):
    tokens.pop()
    constant = tokens.pop()
  if (tokens[-1] != "-"):
    raise Exception("Polynomial expression didn't end with - (some variable), to provide an output variable")
  tokens.pop() # -
  return (constant, target)


def generate_computation_worksheet(text):
  start = text.find(START_TAG + CONSTRAINTS_TAG)
  end = text.find(END_TAG + CONSTRAINTS_TAG)
  lines = text[start:end+1].split("\n")[1:-1]

  worksheet = ""

  for line in lines:
    computationForLine = generate_computation_line(line)

    if (random.random() > float(proverBugginess)):
	worksheet += computationForLine

  global input_vars
  global output_vars
  global variables
  return (len(input_vars) + len(output_vars), len(variables), worksheet)

def generate_load_qap(qap_file):
  code = """load_qap("%s");""" % (qap_file)
  return code

def generate_computation_dynamic(worksheet_file):
  code = """compute_from_pws("%s");""" % (worksheet_file)
  return code

##########################
## Generate computation static (i.e. no pws file)
##########################

def generate_computation_static(text):
  global m
  global chi
  global variables

  zcc_parser_static.m = m
  zcc_parser_static.chi = chi
  zcc_parser_static.variables = variables
  zcc_parser_static.input_vars = input_vars
  zcc_parser_static.output_vars = output_vars
  
  code = zcc_parser_static.generate_computation(text)
  
  m = zcc_parser_static.m
  chi = zcc_parser_static.chi
  variables = zcc_parser_static.variables

  return code

##########################
## Other functions
##########################

def generate_F1_index():
  shuffled_indices = range(0, len(variables))
  random.shuffle(shuffled_indices)

  code = ""
  for i in range(0, len(variables)):
    code += "%d " % (shuffled_indices[i])
  return (code, shuffled_indices)

def generate_constants(text):
  code = ""

  lines = text.split("\n")
  for line in lines:
    line = line.strip()
    if (line != ""):
	code += """
    const %s;
	""" % (line)

  return code

##########################
## Generate input
##########################

def generate_create_input(text):
  code = """
  //gmp_printf("Creating inputs\\n");
  """
 
  for k in sorted(input_vars.keys()): 
    ivar = input_vars[k];
    i = ivar["index"]
    if (ivar["type"] == "int"):
       code += """
    v->get_random_signedint_vec(1, input_q + %d, %d);
    """ % (i, ivar["na"])

    elif (ivar["type"] == "uint"):
       code += """
    v->get_random_vec_priv(1, input_q + %d, %d);
    """ % (i, ivar["na"])
   
    elif (ivar["type"] == "float"):  
      code += """
    v->get_random_rational_vec(1, input_q + %d, %d, %d);
    """ % (i, ivar["na"], ivar["nb"])
   
    else:
      raise Exception("Untyped input variable %s" % (ivar["name"]))  

  return code
