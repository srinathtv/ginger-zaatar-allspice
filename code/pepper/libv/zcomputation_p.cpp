#include <libv/zcomputation_p.h>

ZComputationProver::
ZComputationProver(int ph, int b_size, int num_r, int size_input,
                   int size_output, int num_intermediate_variables, int num_constraints,
                   const char *name_prover, int size_aij, int size_bij, int size_cij,
                   const char *file_name_qap,
                   const char *file_name_f1_index) : ComputationProver(ph, b_size, NUM_REPS_PCP, size_input, name_prover) {
  this->size_input = size_input;
  this->size_output = size_output;
  size_constants = 0;

  chi = num_constraints;
  cout<<"LOG: Number of constraints is "<<num_constraints<<endl;
  num_aij = size_aij;
  num_bij = size_bij;
  num_cij = size_cij;

  num_vars = num_intermediate_variables;
  n_prime = num_intermediate_variables;
  n = n_prime + size_input + size_output + size_constants;

  size_f1_vec = n_prime;
  size_f2_vec = chi+1;

  num_local_runs = NUM_LOCAL_RUNS;
  init_state(file_name_f1_index);
  init_qap(file_name_qap);
}

void ZComputationProver::init_qap(const char *file_name_qap) {
  // set the roots
  qap_roots.SetLength(size_f2_vec);
  qap_roots2.SetLength(size_f2_vec-1);
  single_root.SetLength(1);
  set_v.SetLength(size_f2_vec);
  v_prime.SetLength(size_f2_vec);

  alloc_init_vec(&set_v_z, size_f2_vec);
  
  #if FAST_FOURIER_INTERPOLATION == 1
    mpz_init(omega);
    alloc_init_vec(&powers_of_omega, size_f2_vec);
    
    v->generate_root_of_unity(size_f2_vec, prime);
    v->get_root_of_unity(&omega);
    v->compute_set_v(size_f2_vec, set_v_z, omega, prime);

    mpz_init(omega_inv);
    mpz_invert(omega_inv, omega, prime);
    mpz_set_ui(powers_of_omega[0], 1);
    for (int i=1; i<size_f2_vec; i++) {
      mpz_mul(powers_of_omega[i], powers_of_omega[i-1], omega_inv);
      mpz_mod(powers_of_omega[i], powers_of_omega[i], prime);
    }
  #else
    v->compute_set_v(size_f2_vec, set_v_z, prime);
  #endif

  char str[BUFLEN];
  for (int i=0; i<size_f2_vec; i++) {
    mpz_get_str(str, 10, set_v_z[i]);
    conv(set_v[i], to_ZZ(str));
  }

  poly_tree = new ZZ_pX[2*size_f2_vec-1];
  interpolation_tree = new ZZ_pX[2*size_f2_vec-1];
  num_levels = ((log(size_f2_vec)/log(2)));
    
  z_poly_A_c.SetMaxLength(size_f2_vec);
  z_poly_B_c.SetMaxLength(size_f2_vec);
  z_poly_C_c.SetMaxLength(size_f2_vec);

  #if FAST_FOURIER_INTERPOLATION == 1
  ZZ_p omega_zz;
  mpz_get_str(str, 10, omega);
  conv(omega_zz, to_ZZ(str));
 
  qap_roots[0] = 1;
  for (int i=1; i<size_f2_vec; i++)
    qap_roots[i] = qap_roots[i-1] * omega_zz;

  for (int i=1; i<size_f2_vec; i++)
    qap_roots2[i-1] = qap_roots[i];
  #else
  for (int i=0; i<size_f2_vec; i++)
    qap_roots[i] = i;

  for (int i=1; i<size_f2_vec; i++)
    qap_roots2[i-1] = i;

  build_poly_tree(num_levels, 0, 0);

  z_poly_A_pv.SetLength(size_f2_vec);
  z_poly_B_pv.SetLength(size_f2_vec);
  z_poly_C_pv.SetLength(size_f2_vec);
  #endif

  
  // step 4 in H(t) business
  BuildFromRoots(z_poly_D_c, qap_roots2);

  // compute these based on values set by the compiler
  poly_A = (poly_compressed *) malloc(num_aij * sizeof(poly_compressed));
  poly_B = (poly_compressed *) malloc(num_bij * sizeof(poly_compressed));
  poly_C = (poly_compressed *) malloc(num_cij * sizeof(poly_compressed));

  for (int i=0; i<num_aij; i++)
    alloc_init_scalar(poly_A[i].coefficient);

  for (int i=0; i<num_bij; i++)
    alloc_init_scalar(poly_B[i].coefficient);

  for (int i=0; i<num_cij; i++)
    alloc_init_scalar(poly_C[i].coefficient);

  alloc_init_vec(&poly_A_pv, size_f2_vec);
  alloc_init_vec(&poly_B_pv, size_f2_vec);
  alloc_init_vec(&poly_C_pv, size_f2_vec);

  for (int i=0; i<size_f2_vec; i++) {
    mpz_set_ui(poly_A_pv[i], 0);
    mpz_set_ui(poly_B_pv[i], 0);
    mpz_set_ui(poly_C_pv[i], 0);
  }

  // create vectors to store the evaluations of the polynomial at r_star
  alloc_init_vec(&eval_poly_A, n+1);
  alloc_init_vec(&eval_poly_B, n+1);
  alloc_init_vec(&eval_poly_C, n+1);

  // the polynomials in the compressed form to be initialized by the COMPILER
  // open the file
  FILE *fp = fopen(file_name_qap, "r");
  if (fp == NULL) {
    cout<<"Cannot read "<<file_name_qap<<endl;
    exit(1);
  }

  char line[BUFLEN];
  mpz_t temp;
  alloc_init_scalar(temp);

  // fill the array of struct: poly_A, poly_B, and poly_C
  int line_num = 0;
  while (fgets(line, sizeof line, fp) != NULL) {
    if (line[0] == '\n')
      continue;
    if (line_num < num_aij) {
      gmp_sscanf(line, "%d %d %Zd", &poly_A[line_num].i, &poly_A[line_num].j, poly_A[line_num].coefficient);
    } else if (line_num >= num_aij && line_num < num_aij+num_bij) {
      gmp_sscanf(line, "%d %d %Zd", &poly_B[line_num-num_aij].i, &poly_B[line_num-num_aij].j, poly_B[line_num-num_aij].coefficient);
    } else {
      gmp_sscanf(line, "%d %d %Zd", &poly_C[line_num-num_aij-num_bij].i, &poly_C[line_num-num_aij-num_bij].j, poly_C[line_num-num_aij-num_bij].coefficient);
    }
    line_num++;
  }
  fclose(fp);
}

void ZComputationProver::init_state(const char *file_name_f1_index) {
  string str(file_name_f1_index);
  if (str.find("bisect_sfdl") != std::string::npos) {
    num_bits_in_prime = 220;
  } else if (str.find("pd2_sfdl") != std::string::npos) {
    num_bits_in_prime = 220;
  } else {
    num_bits_in_prime = 128;
  }
  cout<<"LOG: Using a prime of size "<<num_bits_in_prime<<endl;

  temp_stack_size = 16;

  crypto_in_use = CRYPTO_ELGAMAL;
  png_in_use = PNG_CHACHA;

  Prover::init_state();

  // sets prime modulus for all NTL's operations
  char prime_str[BUFLEN];
  mpz_get_str(prime_str, 10, prime);
  ZZ z_prime = to_ZZ(prime_str);
  ZZ_p::init(z_prime);

  num_lin_pcp_queries = NUM_LIN_PCP_QUERIES;

  alloc_init_vec(&F1, size_f1_vec);
  alloc_init_vec(&F1_q, size_f1_vec);
  alloc_init_vec(&F2, size_f2_vec);

  F1_index = new uint32_t[size_f1_vec];
  load_vector(size_f1_vec, F1_index, file_name_f1_index);

  alloc_init_vec(&input_output_q, size_input+size_output);
  input_q = &input_output_q[0];
  output_q = &input_output_q[size_input];

  alloc_init_vec(&input_output, size_input+size_output);
  input = &input_output[0];
  output = &input_output[size_input];

  alloc_init_vec(&temp_qs, temp_stack_size);

  alloc_init_scalar(temp);
  alloc_init_scalar(temp2);
  alloc_init_scalar(temp_q);
  alloc_init_scalar(temp_q2);

  alloc_init_vec(&f1_commitment, expansion_factor*size_f1_vec);
  alloc_init_vec(&f1_consistency, expansion_factor*size_f1_vec);
  alloc_init_vec(&f1_q1, size_f1_vec);
  alloc_init_vec(&f1_q2, size_f1_vec);
  alloc_init_vec(&f1_q3, size_f1_vec);
  alloc_init_vec(&f1_q4, size_f1_vec);

  alloc_init_vec(&f2_commitment, expansion_factor*size_f2_vec);
  alloc_init_vec(&f2_consistency, expansion_factor*size_f2_vec);
  alloc_init_vec(&f2_q1, size_f2_vec);
  alloc_init_vec(&f2_q2, size_f2_vec);
  alloc_init_vec(&f2_q3, size_f2_vec);
  alloc_init_vec(&f2_q4, size_f2_vec);

  alloc_init_vec(&f_answers, NUM_REPS_PCP * NUM_LIN_PCP_QUERIES);
  alloc_init_scalar(answer);

  F_ptrs.clear();
  F_ptrs.push_back(F1);
  F_ptrs.push_back(F2);

  f_q_ptrs.clear();
  f_q_ptrs.push_back(f1_q1);
  f_q_ptrs.push_back(f2_q1);

  f_q2_ptrs.clear();
  f_q2_ptrs.push_back(f1_q2);
  f_q2_ptrs.push_back(f2_q2);

  find_cur_qlengths();
}

ZComputationProver::~ZComputationProver() {
  delete[] F1_index;
  clear_scalar(omega);
  clear_vec(size_f2_vec, powers_of_omega);
  clear_vec(size_f2_vec, set_v_z);
}

void ZComputationProver::
find_cur_qlengths() {
  sizes.clear();
  sizes.push_back(size_f1_vec);
  sizes.push_back(size_f2_vec);

  qquery_sizes.clear();
  for (int i=0; i<NUM_REPS_LIN; i++) {
    qquery_sizes.push_back(size_f1_vec);
    qquery_sizes.push_back(size_f1_vec);
    qquery_sizes.push_back(size_f1_vec);
    qquery_sizes.push_back(size_f2_vec);
    qquery_sizes.push_back(size_f2_vec);
    qquery_sizes.push_back(size_f2_vec);
  }

  qquery_sizes.push_back(size_f1_vec);
  qquery_sizes.push_back(size_f1_vec);
  qquery_sizes.push_back(size_f1_vec);
  qquery_sizes.push_back(size_f2_vec);

  qquery_f_ptrs.clear();
  for (int i=0; i<NUM_REPS_LIN; i++) {
    qquery_f_ptrs.push_back(f1_q1);
    qquery_f_ptrs.push_back(f1_q1);
    qquery_f_ptrs.push_back(f1_q1);
    qquery_f_ptrs.push_back(f2_q1);
    qquery_f_ptrs.push_back(f2_q1);
    qquery_f_ptrs.push_back(f2_q1);
  }

  qquery_f_ptrs.push_back(f1_q1);
  qquery_f_ptrs.push_back(f1_q1);
  qquery_f_ptrs.push_back(f1_q1);
  qquery_f_ptrs.push_back(f2_q1);

  qquery_F_ptrs.clear();
  for (int i=0; i<NUM_REPS_LIN; i++) {
    qquery_F_ptrs.push_back(F1);
    qquery_F_ptrs.push_back(F1);
    
    //setting it to NULL so that the prover will add up answers to
    //previous two queries as answer to the third linearity query. see
    //libv/prover.cpp: prover_answer_queries() function for more details
    //qquery_F_ptrs.push_back(F1);
    qquery_F_ptrs.push_back(NULL);
    
    qquery_F_ptrs.push_back(F2);
    qquery_F_ptrs.push_back(F2);
    //qquery_F_ptrs.push_back(F2);
    qquery_F_ptrs.push_back(NULL);
  }

  qquery_F_ptrs.push_back(F1);
  qquery_F_ptrs.push_back(F1);
  qquery_F_ptrs.push_back(F1);
  qquery_F_ptrs.push_back(F2);

  qquery_q_ptrs.clear();
  for (int i=0; i<NUM_REPS_PCP*NUM_LIN_PCP_QUERIES; i++)
    qquery_q_ptrs.push_back(i);
}

void ZComputationProver::compute_assignment_vectors() {
  // code to compute H(t)
  mpz_t temp;
  alloc_init_scalar(temp);

  // 1. Figure out A_w(t), B_w(t), and C_w(t) at the roots selected for the QAP

  for (int i=0; i<size_f2_vec; i++) {
    mpz_set_ui(poly_A_pv[i], 0);
    mpz_set_ui(poly_B_pv[i], 0);
    mpz_set_ui(poly_C_pv[i], 0);
  }

  int index;
  for (int i=0; i<num_aij; i++) {
    index = poly_A[i].i;
    if (index == 0)
      mpz_set(temp, poly_A[i].coefficient);
    else if (index <= size_f1_vec)
      mpz_mul(temp, poly_A[i].coefficient, F1[index - 1]);
    else if (index > size_f1_vec && index <= size_f1_vec + size_input)
      mpz_mul(temp, poly_A[i].coefficient, input[index - 1 - size_f1_vec]);
    else
      mpz_mul(temp, poly_A[i].coefficient, output[index - 1 - size_input - size_f1_vec]);
    mpz_add(poly_A_pv[poly_A[i].j], poly_A_pv[poly_A[i].j], temp);
    mpz_mod(poly_A_pv[poly_A[i].j], poly_A_pv[poly_A[i].j], prime);
  }

  for (int i=0; i<num_bij; i++) {
    index = poly_B[i].i;
    if (index == 0)
      mpz_set(temp, poly_B[i].coefficient);
    else if (index <= size_f1_vec)
      mpz_mul(temp, poly_B[i].coefficient, F1[index-1]);
    else if (index > size_f1_vec && index <= size_f1_vec + size_input)
      mpz_mul(temp, poly_B[i].coefficient, input[index - 1 - size_f1_vec]);
    else
      mpz_mul(temp, poly_B[i].coefficient, output[index - 1 - size_input - size_f1_vec]);

    mpz_add(poly_B_pv[poly_B[i].j], poly_B_pv[poly_B[i].j], temp);
    mpz_mod(poly_B_pv[poly_B[i].j], poly_B_pv[poly_B[i].j], prime);
  }

  for (int i=0; i<num_cij; i++) {
    index = poly_C[i].i;
    if (index == 0)
      mpz_set(temp, poly_C[i].coefficient);
    else if (index <= size_f1_vec)
      mpz_mul(temp, poly_C[i].coefficient, F1[index-1]);
    else if (index > size_f1_vec && index <= size_f1_vec + size_input)
      mpz_mul(temp, poly_C[i].coefficient, input[index - 1 - size_f1_vec]);
    else
      mpz_mul(temp, poly_C[i].coefficient, output[index - 1 - size_input - size_f1_vec]);

    mpz_add(poly_C_pv[poly_C[i].j], poly_C_pv[poly_C[i].j], temp);
    mpz_mod(poly_C_pv[poly_C[i].j], poly_C_pv[poly_C[i].j], prime);
  }

  // 2. Interpolate them to get them in the coefficients form
  char z_str[BUFLEN];
  ZZ x;
  ZZ_p x_p;
  #if FAST_FOURIER_INTERPOLATION == 1
    mpz_t *poly_A_c = zcomp_fast_interpolate(size_f2_vec, poly_A_pv, omega_inv, prime);
    mpz_set_ui(temp, size_f2_vec);
    mpz_invert(temp, temp, prime);
    for (int i=0; i<size_f2_vec; i++) {
      mpz_mul(poly_A_c[i], poly_A_c[i], temp);
      mpz_mod(poly_A_c[i], poly_A_c[i], prime);
      mpz_get_str(z_str, 10, poly_A_c[i]);
      x = to_ZZ(z_str);
      conv(x_p, x);
      SetCoeff(z_poly_A_c, i, x_p);
    }
    //clear_vec(size_f2_vec, poly_A_c);
    
    mpz_t *poly_B_c = zcomp_fast_interpolate(size_f2_vec, poly_B_pv, omega_inv, prime);
    for (int i=0; i<size_f2_vec; i++) {
      mpz_mul(poly_B_c[i], poly_B_c[i], temp);
      mpz_mod(poly_B_c[i], poly_B_c[i], prime);
      
      mpz_get_str(z_str, 10, poly_B_c[i]);
      x = to_ZZ(z_str);
      conv(x_p, x);
      SetCoeff(z_poly_B_c, i, x_p);
    }
    //clear_vec(size_f2_vec, poly_B_c);
    
    mpz_t *poly_C_c = zcomp_fast_interpolate(size_f2_vec, poly_C_pv, omega_inv, prime);
    for (int i=0; i<size_f2_vec; i++) {
      mpz_mul(poly_C_c[i], poly_C_c[i], temp);
      mpz_mod(poly_C_c[i], poly_C_c[i], prime);
      
      mpz_get_str(z_str, 10, poly_C_c[i]);
      x = to_ZZ(z_str);
      conv(x_p, x);
      SetCoeff(z_poly_C_c, i, x_p);
    }
    //clear_vec(size_f2_vec, poly_C_c);
  #else
    for (int i=0; i<size_f2_vec; i++) {
      mpz_get_str(z_str, 10, poly_A_pv[i]);
      x = to_ZZ(z_str);
      conv(z_poly_A_pv[i], x);

      mpz_get_str(z_str, 10, poly_B_pv[i]);
      x = to_ZZ(z_str);
      conv(z_poly_B_pv[i], x);

      mpz_get_str(z_str, 10, poly_C_pv[i]);
      x = to_ZZ(z_str);
      conv(z_poly_C_pv[i], x);
    }

    // 3. Find P_w(t) = A_w(t) * B_w(t) - C_w(t)
    // old NTL interpolation
    //interpolate(z_poly_A_c, qap_roots, z_poly_A_pv);
    //interpolate(z_poly_B_c, qap_roots, z_poly_B_pv);
    //interpolate(z_poly_C_c, qap_roots, z_poly_C_pv);

    zcomp_interpolate(num_levels, 0, 0, &z_poly_A_pv);
    z_poly_A_c = interpolation_tree[0];
  
    zcomp_interpolate(num_levels, 0, 0, &z_poly_B_pv);
    z_poly_B_c = interpolation_tree[0];

    zcomp_interpolate(num_levels, 0, 0, &z_poly_C_pv);
    z_poly_C_c = interpolation_tree[0];
  #endif

  mul(z_poly_P_c2, z_poly_A_c, z_poly_B_c);
  add(z_poly_P_c, z_poly_P_c2, z_poly_C_c);

  // 4. Compute D(t)
  // already done

  // 5. Find H_w(t) = P_w(t)/D(t)
  int out = divide(z_poly_H_c, z_poly_P_c, z_poly_D_c);
  
  // 6. Set the coefficients of H_w(t) as F2
  // degree from lower to higher.
  for (int i=0; i<size_f2_vec; i++) {
    x_p = coeff(z_poly_H_c, i);
    stringstream coefficient;
    coefficient<<x_p;
    mpz_set_str(F2[i], (coefficient.str()).c_str(), 10);
    coefficient.clear();
  }
}

void ZComputationProver::prover_do_computation() {
  for (int i=batch_start; i<=batch_end; i++) {
    bool passed_test;
    if (i == batch_start)
      m_computation.begin_with_init();
    else
      m_computation.begin_with_history();

    cout << "Running baseline" << endl;
    for (int g=0; g<num_local_runs; g++) {
      snprintf(scratch_str, BUFLEN-1, "input1_b_%d", i);
      load_vector(size_input, input, scratch_str, FOLDER_WWW_DOWNLOAD);

      snprintf(scratch_str, BUFLEN-1, "input1_q_b_%d", i);
      load_vector(size_input, input_q, scratch_str, FOLDER_WWW_DOWNLOAD);

      exogenous_checker->baseline(input_q, size_input, output_q, size_output);

      snprintf(scratch_str, BUFLEN-1, "output2_b_%d", i);
      dump_vector(size_output, output_q, scratch_str, FOLDER_WWW_DOWNLOAD);
    }
    m_computation.end();


    cout << "Prover is computing Y, Z from X" << endl;

    //Run Zaatar implementation
    if (i == batch_start)
      m_interpret_cons.begin_with_init();
    else
      m_interpret_cons.begin_with_history();

    snprintf(scratch_str, BUFLEN-1, "input1_b_%d", i);
    load_vector(size_input, input, scratch_str, FOLDER_WWW_DOWNLOAD);

    snprintf(scratch_str, BUFLEN-1, "input1_q_b_%d", i);
    load_vector(size_input, input_q, scratch_str, FOLDER_WWW_DOWNLOAD);

    interpret_constraints();


    snprintf(scratch_str, BUFLEN-1, "output_b_%d", i);
    dump_vector(size_output, output, scratch_str, FOLDER_WWW_DOWNLOAD);
    m_interpret_cons.end();

    cout << "Running exogenous_check" << endl;
    passed_test = exogenous_checker->exogenous_check(input, input_q, size_input, output, output_q, size_output, prime);
    if (passed_test)
      cout << "Exogenous check passed." << endl;
    else
      cout << "Exogenous check failed." << endl;


    cout << "Prover is filling in the proof vector (lagrange interpolation)" << endl;

    if (i == batch_start)
      m_proofv_creation.begin_with_init();
    else
      m_proofv_creation.begin_with_history();

#ifndef DEBUG_MALICIOUS_PROVER
    compute_assignment_vectors();
#endif

    m_proofv_creation.end();


    snprintf(scratch_str, BUFLEN-1, "f1_assignment_vector_b_%d", i);
    dump_vector(size_f1_vec, F1, scratch_str, FOLDER_WWW_DOWNLOAD);

    snprintf(scratch_str, BUFLEN-1, "f2_assignment_vector_b_%d", i);
    dump_vector(size_f2_vec, F2, scratch_str, FOLDER_WWW_DOWNLOAD);
  }

  delete[] poly_tree;
  delete[] interpolation_tree;
  z_poly_A_pv.SetLength(0);
  z_poly_B_pv.SetLength(0);
  z_poly_C_pv.SetLength(0);
}

//PROVER's CODE
void ZComputationProver::prover_computation_commitment() {
  // execute the computation
  prover_do_computation();

  prover_interactive();
}

void ZComputationProver::prover_interactive() {
  // answer commitment query
  load_vector(expansion_factor*size_f1_vec, f1_commitment, (char *)"f1_commitment_query", FOLDER_WWW_DOWNLOAD);
  load_vector(expansion_factor*size_f2_vec, f2_commitment, (char *)"f2_commitment_query", FOLDER_WWW_DOWNLOAD);

  for (int i=batch_start; i<=batch_end; i++) {
    if (i == 0)
      m_answer_queries.begin_with_init();
    else
      m_answer_queries.begin_with_history();

    snprintf(scratch_str, BUFLEN-1, "f1_assignment_vector_b_%d", i);
    load_vector(size_f1_vec, F1, scratch_str, FOLDER_WWW_DOWNLOAD);

    snprintf(scratch_str, BUFLEN-1, "f2_assignment_vector_b_%d", i);
    load_vector(size_f2_vec, F2, scratch_str, FOLDER_WWW_DOWNLOAD);

    if (crypto_in_use == CRYPTO_ELGAMAL)
      v->dot_product_enc(size_f1_vec, f1_commitment, F1, dotp[0], dotp[1]);

    if (crypto_in_use == CRYPTO_ELGAMAL)
      v->dot_product_enc(size_f2_vec, f2_commitment, F2, dotp2[0], dotp2[1]);

    v->add_enc(dotp[0], dotp[1], dotp[0], dotp[1], dotp2[0], dotp2[1]);

    snprintf(scratch_str, BUFLEN-1, "f_commitment_answer_b_%d", i);
    dump_vector(expansion_factor, dotp, scratch_str, FOLDER_WWW_DOWNLOAD);
    m_answer_queries.end();
  }

  clear_vec(size_f1_vec*expansion_factor, f1_commitment);
  clear_vec(size_f2_vec*expansion_factor, f2_commitment);
}

void ZComputationProver::deduce_queries() {
  int query_id;
  m_plainq.begin_with_init();
  for (int rho=0; rho<num_repetitions; rho++) {
    if (rho == 0) m_plainq.begin_with_init();
    else m_plainq.begin_with_history();

    query_id = 1;
    // create linearity test queries
    for (int i=0; i<NUM_REPS_LIN; i++) {
      v->create_lin_test_queries(size_f1_vec, f1_q1, f1_q2, f1_q3, NULL,
                                 0, NULL, prime);

      v->create_lin_test_queries(size_f2_vec, f2_q1, f2_q2, f2_q3, NULL,
                                 0, NULL, prime);

      //TODO: can be folded into a function
      snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
      dump_vector(size_f1_vec, f1_q1, scratch_str);

      snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
      dump_vector(size_f1_vec, f1_q2, scratch_str);
      
      // don't dump, but increment query_id
      query_id++;

      snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
      dump_vector(size_f2_vec, f2_q1, scratch_str);

      snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
      dump_vector(size_f2_vec, f2_q2, scratch_str);
      
      query_id++;

      // use one of the linearity queries as self correction queries
      if (i == 0) {
        for (int i=0; i<size_f1_vec; i++)
          mpz_set(f1_q4[i], f1_q1[i]);

        for (int i=0; i<size_f2_vec; i++)
          mpz_set(f2_q4[i], f2_q1[i]);
      }
    }

    for (int i=0; i<n+1; i++) {
      mpz_set_ui(eval_poly_A[i], 0);
      mpz_set_ui(eval_poly_B[i], 0);
      mpz_set_ui(eval_poly_C[i], 0);
    }

    // create zquad correction queries: create q9, q10, q11, and q12
    v->create_div_corr_test_queries(n, size_f1_vec, size_f2_vec,
                                    f1_q1, f1_q2, f1_q3, 
                                    f2_q1, 
                                    NULL, 0, NULL, 
                                    NULL, 0, NULL, 
                                    f1_q4, f2_q4, 
                                    NULL, 
                                    num_aij, num_bij, num_cij, 
                                    set_v_z, 
                                    poly_A, poly_B, poly_C, 
                                    eval_poly_A, eval_poly_B, eval_poly_C,
                                    prime);

    snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
    dump_vector(size_f1_vec, f1_q1, scratch_str);

    snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
    dump_vector(size_f1_vec, f1_q2, scratch_str);

    snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
    dump_vector(size_f1_vec, f1_q3, scratch_str);

    snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
    dump_vector(size_f2_vec, f2_q1, scratch_str);
  }
  m_plainq.end();

  clear_vec(n+1, eval_poly_A);
  clear_vec(n+1, eval_poly_B);
  clear_vec(n+1, eval_poly_C);

  for (int i=0; i<num_aij; i++)
    clear_scalar(poly_A[i].coefficient);

  for (int i=0; i<num_bij; i++)
    clear_scalar(poly_B[i].coefficient);

  for (int i=0; i<num_cij; i++)
    clear_scalar(poly_C[i].coefficient);

  free(poly_A);
  free(poly_B);
  free(poly_C);

  clear_vec(size_f2_vec, poly_A_pv);
  clear_vec(size_f2_vec, poly_B_pv);
  clear_vec(size_f2_vec, poly_C_pv);
}

// Credits: the bitreversal code is from
// http://graphics.stanford.edu/~seander/bithacks.html quoted from
// http://stackoverflow.com/questions/746171/best-algorithm-for-bit-reversal-from-msb-lsb-to-lsb-msb-in-c
// variables are renamed
uint32_t ZComputationProver::zreverse (uint32_t v) {
return ((bit_reversal_table[v & 0xff] << 24) | 
    (bit_reversal_table[(v >> 8) & 0xff] << 16) | 
    (bit_reversal_table[(v >> 16) & 0xff] << 8) |
    (bit_reversal_table[(v >> 24) & 0xff]));
}

mpz_t* ZComputationProver::zcomp_fast_interpolate(int k, mpz_t *A, mpz_t omega_k, mpz_t prime) {
  #ifndef RECURSIVE_FFT
  mpz_t temp, temp2;
  alloc_init_scalar(temp);
  alloc_init_scalar(temp2);

  // bit reversal trick to arrange for in-place FFT
  uint32_t n = (uint32_t) k;
  int num_leading_zeros = ffs(zreverse(k));
  for (uint32_t i=0; i<n; i++) {
    uint32_t j = zreverse(i) >> num_leading_zeros;
    if (j > i) {
      mpz_set(temp, A[i]);
      mpz_set(A[i], A[j]);
      mpz_set(A[j], temp);
    }
  }
    
  mpz_t *m;

  // butterfly updates
  int level = n/2; 
  for (uint32_t L=2; L<=n; L=L+L) {
    int K = level % n;
    for (uint32_t j=0; j<n/L; j++) {
      int k2 = L/2;
      int base = L * j;
      int idx = 0;
      for (int i=0; i<k2; i++) {
        m = &powers_of_omega[idx];
        
        mpz_mul(temp, *m, A[base+k2+i]);
        mpz_set(temp2, A[base+i]);

        mpz_add(A[base+i], A[base+i], temp);
        if (L%4 == 0 || L==n) //improves performance
          mpz_mod(A[base+i], A[base+i], prime);

        mpz_sub(A[base+k2+i], temp2, temp);
        if (L%4 == 0 || L==n) //improves performance
          mpz_mod(A[base+k2+i], A[base+k2+i], prime);
        
        idx = (idx + K)%n;
      }
    }
    level = level/2;
  }
  clear_scalar(temp);
  return A;
  #else
  mpz_t *A_hat;
  alloc_init_vec(&A_hat, k);
  if (k == 2) {
    mpz_add(A_hat[0], A[0], A[1]);
    mpz_sub(A_hat[1], A[0], A[1]);
    return A_hat;
  }

  mpz_t *A_even;
  mpz_t *A_odd;
  alloc_init_vec(&A_even, k/2);
  alloc_init_vec(&A_odd, k/2);

  for (int i=0; i<k/2; i++) {
    mpz_set(A_even[i], A[2*i]);
    mpz_set(A_odd[i], A[2*i+1]);
  }
  
  mpz_t omega_sqr;
  mpz_init(omega_sqr);
  mpz_mul(omega_sqr, omega_k, omega_k);
  mpz_mod(omega_sqr, omega_sqr, prime);

  mpz_t *A_even_hat = zcomp_fast_interpolate(k/2, A_even, omega_sqr, prime);
  mpz_t *A_odd_hat = zcomp_fast_interpolate(k/2, A_odd, omega_sqr, prime);
  
  mpz_clear(omega_sqr);

  mpz_t m, temp;
  mpz_init_set_ui(m, 1);
  mpz_init(temp);

  int k2 = k/2, i = 0;
  for (int i=0; i<k2; i++) { 
    mpz_mul(temp, m, A_odd_hat[i]);
    
    mpz_add(A_hat[i], A_even_hat[i], temp);
    mpz_mod(A_hat[i], A_hat[i], prime);

    mpz_sub(A_hat[i+k2], A_even_hat[i], temp);
    mpz_mod(A_hat[i+k2], A_hat[i+k2], prime);
    
    mpz_mul(m, m, omega_k);
    mpz_mod(m, m, prime);
  }
 
  mpz_clear(m);
  mpz_clear(temp);
  clear_vec(k2, A_even_hat);
  clear_vec(k2, A_odd_hat);
  return A_hat;
  #endif
}

void ZComputationProver::zcomp_interpolate(int level, int j, int index, vec_ZZ_p *evals) {
  if (level == 0) {
    interpolation_tree[index] = set_v[j] * (*evals)[j];
  } else {
    zcomp_interpolate(level-1, 2*j, 2*index+1, evals);
    zcomp_interpolate(level-1, 2*j+1, 2*index+2, evals);

    mul(interpolation_tree[2*index+1], interpolation_tree[2*index+1], poly_tree[2*index+2]);
    mul(interpolation_tree[2*index+2], interpolation_tree[2*index+2], poly_tree[2*index+1]);

    add(interpolation_tree[index], interpolation_tree[2*index+1], interpolation_tree[2*index+2]);
  }
}

void ZComputationProver::build_poly_tree(int level, int j, int index) {
  if (level == 0) {
    single_root[0] = qap_roots[j];
    BuildFromRoots(poly_tree[index], single_root);
  } else {
    build_poly_tree(level-1, 2*j, 2*index+1);
    build_poly_tree(level-1, 2*j+1, 2*index+2);
    mul(poly_tree[index], poly_tree[2*index+1], poly_tree[2*index+2]);
  }
}
