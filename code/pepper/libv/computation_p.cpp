#include <libv/zcomputation_p.h>

ComputationProver::
ComputationProver(int ph, int b_size, int num_r, int size_input,
                  const char *name_prover) : Prover(ph, b_size, num_r, size_input, name_prover) {
}

ComputationProver::~ComputationProver() {
}

static void zcomp_assert(const std::string& a, const std::string& b,
                         const std::string & error) {
  if (a != b) {
    gmp_printf(error.c_str());
    gmp_printf(", ");
    gmp_printf(b.c_str());
    gmp_printf("\\n");
  }
}

void ComputationProver::compute_poly(std::istream_iterator<std::string>& cmds, int tempNum) {
  std::istream_iterator<std::string> eos;

  mpq_t& polyTarget = temp_qs[tempNum];
  mpq_t& termTarget = temp_qs[tempNum+1];
  mpq_set_ui(polyTarget, 0, 1);
  if (tempNum >= temp_stack_size-1) {
    gmp_printf("ERROR IN PROVER - Polynomial required more than %d recursive calls \n", temp_stack_size);
    return;
  }

  bool hasTerms = false;
  bool hasFactors = false;
  bool isEmpty = true;
  bool negate = false;

  while(cmds != eos) {
    std::string tok = *cmds;
    cmds++;

    //Emit last term, if necessary:
    if (tok == "+" || tok == "-") {
      if (hasFactors) {
        if (negate) {
          mpq_neg(termTarget, termTarget);
        }
        if (!hasTerms) {
          mpq_set(polyTarget, termTarget);
        } else {
          mpq_add(polyTarget, polyTarget, termTarget);
        }
        hasTerms = true;
        isEmpty = false;
        hasFactors = false;
        negate = false;
      }
    }

    if (tok == "(") {
      //Recurse
      compute_poly(cmds, tempNum + 2);
      mpq_t& subresult = temp_qs[tempNum + 2];
      if (!hasFactors) {
        mpq_set(termTarget, subresult);
      } else {
        mpq_mul(termTarget, termTarget, subresult);
      }
      hasFactors = true;
    } else if (tok == ")") {
      break;
    } else if (tok == "E") {
      break;
    } else if (tok == "+" || tok == "*") {
      //handled below
    } else if (tok == "-") {
      negate = !negate;
      //remaining handled below
    } else {
      //Factor. (either constant or variable)
      mpq_t& factor = voc(tok, temp_q);
      if (!hasFactors) {
        mpq_set(termTarget, factor);
      } else {
        mpq_mul(termTarget, termTarget, factor);
      }
      hasFactors = true;
    }
  }

  //Emit last term, if necessary:
  if (hasFactors) {
    if (negate) {
      mpq_neg(termTarget, termTarget);
    }
    if (!hasTerms) {
      mpq_set(polyTarget, termTarget);
    } else {
      mpq_add(polyTarget, polyTarget, termTarget);
    }
    hasTerms = true;
    isEmpty = false;
    hasFactors = false;
    negate = false;
  }

  //Set to zero if the polynomial is empty
  if (isEmpty) {
    mpq_set_ui(polyTarget, 0, 1);
  }
}


// Expected format SI INPUT into LENGTH bits at FIRST_OUTPUT
void ComputationProver::compute_split_unsignedint(std::istream_iterator<std::string>& cmds) {
  mpq_t* in = NULL;
  //cout << *cmds << endl;
  if ((*cmds)[0] == 'V') {
    in = &F1_q[F1_index[atoi((*cmds).c_str() + 1)]];
  } else if ((*cmds)[0] == 'I') {
    in = &input_q[atoi((*cmds).c_str() + 1)];
  }
  cmds++;
  zcomp_assert(*cmds, "into", "Invalid SI");
  cmds++;
  int length = atoi((*cmds).c_str());
  cmds++;
  zcomp_assert(*cmds, "bits", "Invalid SI");
  cmds++;
  zcomp_assert(*cmds, "at", "Invalid SI");
  cmds++;
  //cout << *cmds << endl;
  int output_start = atoi((*cmds).c_str() + 1);
  cmds++;

  //Fill in the Ni with the bits of in.
  //Each bit is either 0 or 1
  //gmp_printf("%Zd\n", in);
  for(int i = 0; i < length; i++) {
    mpq_t& Ni = F1_q[F1_index[output_start + i]];
    int bit = mpz_tstbit(mpq_numref(*in), length - i - 1);
    //cout << bit << endl;
    mpq_set_ui(Ni, bit, 1);
    //gmp_printf("%Zd\n", Ni);
  }
}

// Expected format SIL (uint | int) bits <length> X <input> Y0 <first bit of output>
void ComputationProver::compute_split_int_le(std::istream_iterator<std::string>& cmds) {
  bool isSigned = (*cmds)[0] != 'u';
  cmds++;
  zcomp_assert(*cmds, "bits", "Invalid SIL");
  cmds++;
  int N = atoi((*cmds).c_str());
  cmds++;
  zcomp_assert(*cmds, "X", "Invalid SIL");
  cmds++;
  mpq_t& in = voc(*cmds, temp_q);
  cmds++;
  zcomp_assert(*cmds, "Y0", "Invalid SIL");
  cmds++;
  if ((*cmds)[0] != 'V'){
    gmp_printf("Assertion Error: Cannot output split gate bits to %s, a V# was required.\n", (*cmds).c_str());
  }
  int output_start = atoi((*cmds).c_str() + 1);
  cmds++;

  //Fill in the Ni with the bits of in 
  //Each bit is either 0 or 1
  //gmp_printf("%Zd\n", in);

  mpz_set(temp, mpq_numref(in));
  bool inIsNegative = mpz_sgn(temp) < 0;
  if (!isSigned && inIsNegative){
    gmp_printf("Assertion Error: Negative integer input to unsigned split gate\n");
  }
  if (inIsNegative){
    mpz_set_ui(temp2, 1);
    mpz_mul_2exp(temp2, temp2, N);
    mpz_add(temp, temp, temp2);
  }
  for(int i = 0; i < N; i++) {
    mpq_t& Ni = F1_q[F1_index[output_start + i]];
    if (i == N-1 && isSigned){
      mpz_set_ui(temp2, inIsNegative ? 1 : 0);      
    } else {
      mpz_tdiv_r_2exp(temp2, temp, 1);
      mpz_tdiv_q_2exp(temp, temp, 1);
    }
    mpq_set_z(Ni, temp2);
  }
}


void ComputationProver::compute_less_than_int(std::istream_iterator<std::string>& cmds) {
  zcomp_assert(*cmds, "N_0", "Invalid <I");
  cmds++;
  int N_start = atoi((*cmds).c_str() + 1);
  cmds++;
  zcomp_assert(*cmds, "N", "Invalid <I");
  cmds++;
  int N = atoi((*cmds).c_str());
  cmds++;
  zcomp_assert(*cmds, "Mlt", "Invalid <I");
  cmds++;
  mpq_t& Mlt = voc(*cmds, temp_q);
  cmds++;
  zcomp_assert(*cmds, "Meq", "Invalid <I");
  cmds++;
  mpq_t& Meq = voc(*cmds, temp_q);
  cmds++;
  zcomp_assert(*cmds, "Mgt", "Invalid <I");
  cmds++;
  mpq_t& Mgt = voc(*cmds, temp_q);
  cmds++;

  zcomp_assert(*cmds, "X1", "Invalid <I");
  cmds++;
  mpq_t& X1 = voc(*cmds, temp_q);
  cmds++;
  zcomp_assert(*cmds, "X2", "Invalid <I");
  cmds++;
  mpq_t& X2 = voc(*cmds, temp_q);
  cmds++;
  zcomp_assert(*cmds, "Y", "Invalid <I");
  cmds++;
  mpq_t& Y = voc(*cmds, temp_q);
  cmds++;

  int compare = mpq_cmp(X1, X2);
  if (compare < 0) {
    mpq_set_ui(Mlt, 1, 1);
    mpq_set_ui(Meq, 0, 1);
    mpq_set_ui(Mgt, 0, 1);
    mpq_sub(temp_qs[0], X1, X2);
  } else if (compare == 0) {
    mpq_set_ui(Mlt, 0, 1);
    mpq_set_ui(Meq, 1, 1);
    mpq_set_ui(Mgt, 0, 1);
    mpq_sub(temp_qs[0], X1, X2);
  } else if (compare > 0) {
    mpq_set_ui(Mlt, 0, 1);
    mpq_set_ui(Meq, 0, 1);
    mpq_set_ui(Mgt, 1, 1);
    mpq_sub(temp_qs[0], X2, X1);
  }
  mpq_set(Y, Mlt);

  mpz_set_ui(temp, 1);
  mpz_mul_2exp(temp, temp, N-1);
  mpz_add(temp, temp, mpq_numref(temp_qs[0]));

  //Fill in the Ni with the bits of the difference + 2^(N-1)
  //Each bit is either 0 or the power of two, so the difference = sum (Ni)
  for(int i = 0; i < N-1; i++) {
    mpq_t& Ni = F1_q[F1_index[N_start + i]];
    mpz_tdiv_r_2exp(temp2, temp, 1);
    mpq_set_z(Ni, temp2);
    mpq_mul_2exp(Ni, Ni, i);
    mpz_tdiv_q_2exp(temp, temp, 1);
  }
}

void ComputationProver::compute_less_than_float(std::istream_iterator<std::string>& cmds) {
  zcomp_assert(*cmds, "N_0", "Invalid <I");
  cmds++;
  int N_start = atoi((*cmds).c_str() + 1);
  cmds++;

  zcomp_assert(*cmds, "Na", "Invalid <I");
  cmds++;
  int Na = atoi((*cmds).c_str());
  cmds++;

  zcomp_assert(*cmds, "N", "Invalid <I");
  cmds++;
  mpq_t& N = voc(*cmds, temp_q);
  cmds++;

  zcomp_assert(*cmds, "D_0", "Invalid <I");
  cmds++;
  int D_start = atoi((*cmds).c_str() + 1);
  cmds++;

  zcomp_assert(*cmds, "Nb", "Invalid <I");
  cmds++;
  int Nb = atoi((*cmds).c_str());
  cmds++;

  zcomp_assert(*cmds, "D", "Invalid <I");
  cmds++;
  mpq_t& D = voc(*cmds, temp_q);
  cmds++;

  zcomp_assert(*cmds, "ND", "Invalid <I");
  cmds++;
  mpq_t& ND = voc(*cmds, temp_q);
  cmds++;

  zcomp_assert(*cmds, "Mlt", "Invalid <I");
  cmds++;
  mpq_t& Mlt = voc(*cmds, temp_q);
  cmds++;
  zcomp_assert(*cmds, "Meq", "Invalid <I");
  cmds++;
  mpq_t& Meq = voc(*cmds, temp_q);
  cmds++;
  zcomp_assert(*cmds, "Mgt", "Invalid <I");
  cmds++;
  mpq_t& Mgt = voc(*cmds, temp_q);
  cmds++;

  zcomp_assert(*cmds, "X1", "Invalid <I");
  cmds++;
  mpq_t& X1 = voc(*cmds, temp_q);
  cmds++;
  zcomp_assert(*cmds, "X2", "Invalid <I");
  cmds++;
  mpq_t& X2 = voc(*cmds, temp_q);
  cmds++;
  zcomp_assert(*cmds, "Y", "Invalid <I");
  cmds++;
  mpq_t& Y = voc(*cmds, temp_q);
  cmds++;

  int compare = mpq_cmp(X1, X2);
  if (compare < 0) {
    mpq_set_ui(Mlt, 1, 1);
    mpq_set_ui(Meq, 0, 1);
    mpq_set_ui(Mgt, 0, 1);
    mpq_sub(temp_q, X1, X2);
    mpq_set_z(N, mpq_numref(temp_q));
    mpq_set_z(D, mpq_denref(temp_q)); //should be positive
  } else if (compare == 0) {
    mpq_set_ui(Mlt, 0, 1);
    mpq_set_ui(Meq, 1, 1);
    mpq_set_ui(Mgt, 0, 1);
    mpq_set_si(N, -1, 1);
    mpq_set_ui(D, 1, 1);
  } else if (compare > 0) {
    mpq_set_ui(Mlt, 0, 1);
    mpq_set_ui(Meq, 0, 1);
    mpq_set_ui(Mgt, 1, 1);
    mpq_sub(temp_q, X2, X1);
    mpq_set_z(N, mpq_numref(temp_q));
    mpq_set_z(D, mpq_denref(temp_q)); //should be positive
  }
  mpq_set(Y, Mlt);

  mpz_set_ui(temp, 1);
  mpz_mul_2exp(temp, temp, Na);
  mpz_add(temp, temp, mpq_numref(N)); //temp = 2^Na + (numerator of difference)

  //Fill in the Ni with the bits of the numerator difference + 2^Na
  //Each bit is either 0 or the power of two, so N = sum (Ni)
  for(int i = 0; i < Na; i++) {
    mpq_t& Ni = F1_q[F1_index[N_start + i]];
    mpz_tdiv_r_2exp(temp2, temp, 1);
    mpq_set_z(Ni, temp2);
    mpq_mul_2exp(Ni, Ni, i);
    mpz_tdiv_q_2exp(temp, temp, 1);
  }

  mpz_set(temp, mpq_numref(D));

  //Fill in the Di with whether the denominator is a particular power of
  //two.
  for(int i = 0; i < Nb + 1; i++) {
    mpq_t& Di = F1_q[F1_index[D_start + i]];
    mpz_tdiv_r_2exp(temp2, temp, 1);
    mpq_set_z(Di, temp2);
    mpz_tdiv_q_2exp(temp, temp, 1);
  }

  //Invert D.
  mpq_inv(D, D);
  //Compute N D
  mpq_mul(ND, N, D);
}



/**
* If str is the name of a variable, return a reference to that variable.
* Otherwise, set use_if_constant to the constant variable held by the
* string, and return it.
*
* The method name stands for "variable or constant"
**/
mpq_t& ComputationProver::voc(const std::string& str, mpq_t& use_if_constant) {
  int index;
  const std::string& name = str;
  if (name[0] == 'I') {
    index = atoi(name.c_str() + 1);
    if (index < 0 || index >= size_input) {
      gmp_printf("PARSE ERROR - variable %s\n",str.c_str());
      return use_if_constant;
    }
    return input_output_q[index];
  } else if (name[0] == 'O') {
    index = atoi(name.c_str() + 1);
    if (index < size_input || index >= size_input + size_output) {
      gmp_printf("PARSE ERROR - variable %s\n",str.c_str());
      return use_if_constant;
    }
    return input_output_q[index];
  } else if (name[0] == 'V') {
    index = atoi(name.c_str() + 1);
    if (index < 0 || index >= num_vars) {
      gmp_printf("PARSE ERROR - variable %s\n",str.c_str());
      return use_if_constant;
    }
    return F1_q[F1_index[index]];
  }
  //Parse the rational constant
  mpq_set_str(use_if_constant, str.c_str(), 10);
  return use_if_constant;
}

/**
The computation may elect to simply execute a PWS file (prover work sheet).
This routine parses a PWS file (filename in a C-string) and parses it.
*/
void ComputationProver::compute_from_pws(const char* pws_filename) {
  std::ifstream cmdfile (pws_filename);
  if (cmdfile.fail()) {
    gmp_printf("Couldn't open prover worksheet file.\n");
    return;
  }
  //Read the entire file into memory
  std::stringstream buffer;
  buffer << cmdfile.rdbuf();

  //Iterate over the in-memory buffer
  std::istream_iterator<std::string> cmds(buffer);
  std::istream_iterator<std::string> eos;

  while(eos != cmds) {
    std::string tok = *cmds;
    cmds++;
    if (tok == "P") {
      mpq_t& Y = voc(*cmds,temp_q);
      cmds++;
      zcomp_assert(*cmds, "=", "Invalid POLY");
      cmds++;
      compute_poly(cmds, 0);
      mpq_set(Y, temp_qs[0]);
    } else if (tok == "<I") {
      compute_less_than_int(cmds);
    } else if (tok == "<F") {
      compute_less_than_float(cmds);
    } else if (tok == "!=") {
      //Not equals computation
      zcomp_assert(*cmds, "M", "Invalid !=");
      cmds++;
      mpq_t& M = voc(*cmds,temp_q);
      cmds++;
      zcomp_assert(*cmds, "X1", "Invalid !=");
      cmds++;
      mpq_t& X1 = voc(*cmds,temp_q);
      cmds++;
      zcomp_assert(*cmds, "X2", "Invalid !=");
      cmds++;
      mpq_t& X2 = voc(*cmds,temp_q2);
      cmds++;
      zcomp_assert(*cmds, "Y", "Invalid !=");
      cmds++;
      mpq_t& Y = voc(*cmds,temp_q);
      cmds++;

      if (mpq_equal(X1, X2)) {
        mpq_set_ui(M, 0, 1);
        mpq_set_ui(Y, 0, 1);
      } else {
        mpq_sub(temp_q, X1, X2);
        //f(a,b)^-1 = b*a^-1
        mpz_invert(temp, mpq_numref(temp_q), prime);
        mpz_mul(temp, temp, mpq_denref(temp_q));
        mpq_set_z(M, temp);
        mpq_set_ui(Y, 1, 1);
      }
    } else if (tok == "/") {
      //Exact division computation
      mpq_t& Y = voc(*cmds,temp_q);
      cmds++;
      zcomp_assert(*cmds, "=", "Invalid DIV");
      cmds++;
      mpq_t& X1 = voc(*cmds,temp_q);
      cmds++;
      zcomp_assert(*cmds, "/", "Invalid DIV");
      cmds++;
      mpq_t& X2 = voc(*cmds,temp_q2);
      cmds++;
      mpq_div(Y,X1,X2);
    } else if (tok == "SI") {
      //Split into bits (big endian, see implementation for format)
      compute_split_unsignedint(cmds);
    } else if (tok == "SIL") {
      //Split into bits (little endian, see implementation for format)
      compute_split_int_le(cmds);
    } else {
      gmp_printf("Unrecognized token: %s\n", tok.c_str());
    }
  }

  // convert output_q to output
  convert_to_z(size_output, output, output_q, prime);

  // convert F1_q to F1
  convert_to_z(num_vars, F1, F1_q, prime);
}
