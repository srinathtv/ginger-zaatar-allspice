#include <libv/zcomputation_v.h>

ZComputationVerifier::ZComputationVerifier(int batch, int reps, int ip_size,
    int out_size, int num_variables, int num_constraints, int optimize_answers,
    char *prover_url, const char *name_prover, int size_aij, int size_bij,
    int size_cij, const char *file_name_qap) : Verifier(batch, NUM_REPS_PCP, ip_size, optimize_answers, prover_url, name_prover) {
  chi = num_constraints;
  n_prime = num_variables;
  num_aij = size_aij;
  num_bij = size_bij;
  num_cij = size_cij;

  n = n_prime + ip_size + out_size;
  size_input = ip_size;
  size_output = out_size;

  size_f1_vec = n_prime;
  size_f2_vec = chi + 1;

  num_verification_runs = NUM_VERIFICATION_RUNS;
  init_qap(file_name_qap);
  init_state();
}

ZComputationVerifier::~ZComputationVerifier() {
  clear_scalar(A_tau);
  clear_scalar(B_tau);
  clear_scalar(C_tau);
  clear_vec(size_input+size_output, input);
  clear_vec(size_input, input_q);
  clear_vec(size_f2_vec, set_v);

  clear_vec(num_repetitions * num_lin_pcp_queries, f_answers);
  clear_vec(expansion_factor, temp_arr);
  clear_vec(expansion_factor, temp_arr2);

  clear_scalar(temp);
  clear_scalar(temp2);
  clear_scalar(temp3);
  clear_scalar(lhs);
  clear_scalar(rhs);
  clear_vec(NUM_REPS_PCP, d_star);
  clear_scalar(omega);
}

void ZComputationVerifier::init_qap(const char *file_name_qap) {
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

  // create vectors to store the evaluations of the polynomial at tau
  alloc_init_vec(&eval_poly_A, n+1);
  alloc_init_vec(&eval_poly_B, n+1);
  alloc_init_vec(&eval_poly_C, n+1);

  alloc_init_vec(&A_tau_io, num_repetitions*(size_input+size_output+1));
  alloc_init_vec(&B_tau_io, num_repetitions*(size_input+size_output+1));
  alloc_init_vec(&C_tau_io, num_repetitions*(size_input+size_output+1));


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
  clear_scalar(temp);
  
  // set prime size based on name of the computation in case of Zaatar
  string str(file_name_qap);
  if (str.find("bisect_sfdl") != std::string::npos) {
    num_bits_in_prime = 220;
  } else if (str.find("pd2_sfdl") != std::string::npos) {
    num_bits_in_prime = 220;
  } else {
    num_bits_in_prime = 128;
  }

  cout<<"LOG: Using a prime of size "<<num_bits_in_prime<<endl;
}

void ZComputationVerifier::init_state() {
//  num_bits_in_prime = 128; this is set in init_qap based on the
//  computation for zaatar
  num_bits_in_input = 32;

  crypto_in_use = CRYPTO_ELGAMAL;
  png_in_use = PNG_CHACHA;

  num_lin_pcp_queries = NUM_LIN_PCP_QUERIES;

  Verifier::init_state();

  // allocate input and output contiguously
  alloc_init_vec(&input, size_input+size_output);
  output = &input[size_input];
  alloc_init_vec(&input_q, size_input);

  alloc_init_vec(&set_v, size_f2_vec);
  
  #if FAST_FOURIER_INTERPOLATION ==1
    alloc_init_scalar(omega);
    v->generate_root_of_unity(size_f2_vec, prime);
    v->get_root_of_unity(&omega);
    v->compute_set_v(size_f2_vec, set_v, omega, prime);
  #else
    v->compute_set_v(size_f2_vec, set_v, prime);
  #endif

  alloc_init_vec(&f1_commitment, expansion_factor*size_f1_vec);
  alloc_init_vec(&f2_commitment, expansion_factor*size_f2_vec);
  alloc_init_vec(&f1_consistency, size_f1_vec);
  alloc_init_vec(&f2_consistency, size_f2_vec);

  alloc_init_vec(&f1_q1, size_f1_vec);
  alloc_init_vec(&f1_q2, size_f1_vec);
  alloc_init_vec(&f1_q3, size_f1_vec);
  alloc_init_vec(&f1_q4, size_f1_vec);

  alloc_init_vec(&f2_q1, size_f2_vec);
  alloc_init_vec(&f2_q2, size_f2_vec);
  alloc_init_vec(&f2_q3, size_f2_vec);
  alloc_init_vec(&f2_q4, size_f2_vec);

  alloc_init_vec(&f_answers, num_repetitions * num_lin_pcp_queries);
  alloc_init_vec(&temp_arr, expansion_factor);
  alloc_init_vec(&temp_arr2, expansion_factor);

  alloc_init_scalar(temp);
  alloc_init_scalar(temp2);
  alloc_init_scalar(temp3);
  alloc_init_scalar(lhs);
  alloc_init_scalar(rhs);
  alloc_init_vec(&d_star, NUM_REPS_PCP);

  alloc_init_scalar(A_tau);
  alloc_init_scalar(B_tau);
  alloc_init_scalar(C_tau);

  // To create consistency and commitment queries.
  commitment_query_sizes.clear();
  commitment_query_sizes.push_back(size_f1_vec);
  commitment_query_sizes.push_back(size_f2_vec);

  f_commitment_ptrs.clear();
  f_commitment_ptrs.push_back(f1_commitment);
  f_commitment_ptrs.push_back(f2_commitment);

  f_consistency_ptrs.clear();
  f_consistency_ptrs.push_back(f1_consistency);
  f_consistency_ptrs.push_back(f2_consistency);

  temp_arr_ptrs.clear();
  temp_arr_ptrs.push_back(temp_arr);
  temp_arr_ptrs.push_back(temp_arr2);

  Q_list.clear();
  for (int i=0; i<NUM_REPS_PCP*NUM_LIN_PCP_QUERIES; i++)
    Q_list.push_back(i);
}

void ZComputationVerifier::create_input() {
  // as many computations as inputs
  for (int k=0; k<batch_size; k++) {
    int input_size = size_input;
    //v->get_random_vec(size_input, input, num_bits_in_input);
    //v->add_sign(size_input, input);

    input_creator->create_input(input_q, input_size);

    snprintf(scratch_str, BUFLEN-1, "input1_q_b_%d", k);
    dump_vector(input_size, input_q, scratch_str);
    send_file(scratch_str);

    convert_to_z(input_size, input, input_q, prime);

    snprintf(scratch_str, BUFLEN-1, "input1_b_%d", k);
    dump_vector(size_input, input, scratch_str);
    send_file(scratch_str);
  }
}


void ZComputationVerifier::create_plain_queries() {
  clear_vec(size_f1_vec*expansion_factor, f1_commitment);
  clear_vec(size_f2_vec*expansion_factor, f2_commitment);

  m_plainq.begin_with_init();
  // keeps track of #filled coins
  int f_con_filled = -1;
  int query_id;

  for (int rho=0; rho<num_repetitions; rho++) {
    if (rho == 0) m_plainq.begin_with_init();
    else m_plainq.begin_with_history();

    // create linearity test queries
    query_id = 1;
    for (int i=0; i<NUM_REPS_LIN; i++) {
      v->create_lin_test_queries(size_f1_vec, f1_q1, f1_q2, f1_q3, f1_consistency,
                                 f_con_filled, f_con_coins, prime);

      f_con_filled += 3;

      v->create_lin_test_queries(size_f2_vec, f2_q1, f2_q2, f2_q3, f2_consistency,
                                 f_con_filled, f_con_coins, prime);

      f_con_filled += 3;

      //TODO: can be folded into a function
      /*
      snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
      dump_vector(size_f1_vec, f1_q1, scratch_str);
      send_file(scratch_str);


      snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
      dump_vector(size_f1_vec, f1_q2, scratch_str);
      send_file(scratch_str);
      
      // don't dump, but increment query_id
      query_id++;
      //snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
      //dump_vector(size_f1_vec, f1_q3, scratch_str);
      //send_file(scratch_str);

      snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
      dump_vector(size_f2_vec, f2_q1, scratch_str);
      send_file(scratch_str);

      snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
      dump_vector(size_f2_vec, f2_q2, scratch_str);
      send_file(scratch_str);
      
      query_id++;
      //snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
      //dump_vector(size_f2_vec, f2_q3, scratch_str);
      //send_file(scratch_str);
      */

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
    v->create_div_corr_test_queries(n, size_f1_vec, size_f2_vec, f1_q1, f1_q2, f1_q3, f2_q1, f_con_coins, f_con_filled, f1_consistency, f_con_coins, f_con_filled+3, f2_consistency, f1_q4, f2_q4, d_star[rho], num_aij, num_bij, num_cij, set_v, poly_A, poly_B, poly_C, eval_poly_A, eval_poly_B, eval_poly_C, prime);

    f_con_filled += 4;

    int base = rho * (1 + size_input + size_output);
    mpz_set(A_tau_io[base+0], eval_poly_A[0]);
    mpz_set(B_tau_io[base+0], eval_poly_B[0]);
    mpz_set(C_tau_io[base+0], eval_poly_C[0]);

    for (int i=0; i<size_input+size_output; i++) {
      mpz_set(A_tau_io[base+1+i], eval_poly_A[1+n_prime+i]);
      mpz_set(B_tau_io[base+1+i], eval_poly_B[1+n_prime+i]);
      mpz_set(C_tau_io[base+1+i], eval_poly_C[1+n_prime+i]);
    }

    /*
    snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
    dump_vector(size_f1_vec, f1_q1, scratch_str);
    send_file(scratch_str);

    snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
    dump_vector(size_f1_vec, f1_q2, scratch_str);
    send_file(scratch_str);

    snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
    dump_vector(size_f1_vec, f1_q3, scratch_str);
    send_file(scratch_str);

    snprintf(scratch_str, BUFLEN-1, "q%d_qquery_r_%d", query_id++, rho);
    dump_vector(size_f2_vec, f2_q1, scratch_str);
    send_file(scratch_str);
    */
  }

  dump_vector(size_f1_vec, f1_consistency, (char *)"f1_consistency_query");
  send_file((char *)"f1_consistency_query");

  dump_vector(size_f2_vec, f2_consistency, (char *)"f2_consistency_query");
  send_file((char *)"f2_consistency_query");
  m_plainq.end();

  // cleanup time
  clear_vec(n+1, eval_poly_A);
  clear_vec(n+1, eval_poly_B);
  clear_vec(n+1, eval_poly_C);
  clear_vec(size_f1_vec, f1_consistency);
  clear_vec(size_f2_vec, f2_consistency);

  for (int i=0; i<num_aij; i++)
    clear_scalar(poly_A[i].coefficient);

  for (int i=0; i<num_bij; i++)
    clear_scalar(poly_B[i].coefficient);

  for (int i=0; i<num_cij; i++)
    clear_scalar(poly_C[i].coefficient);

  free(poly_A);
  free(poly_B);
  free(poly_C);

  clear_vec(size_f1_vec, f1_q1);
  clear_vec(size_f1_vec, f1_q2);
  clear_vec(size_f1_vec, f1_q3);
  clear_vec(size_f1_vec, f1_q4);
  clear_vec(size_f2_vec, f2_q1);
  clear_vec(size_f2_vec, f2_q2);
  clear_vec(size_f2_vec, f2_q3);
  clear_vec(size_f2_vec, f2_q4);

  // Ginger's codebase did some run test part of work in plain query
  // creation; so the base class calls with history;
  m_runtests.reset();
}

void ZComputationVerifier::populate_answers(mpz_t *f_answers, int rho, int num_repetitions, int beta) { }

bool ZComputationVerifier::run_interactive_tests(uint32_t beta) {
  bool lin1, lin2, corr;
  bool result = true;

  for (int rho=0; rho<num_repetitions; rho++) {
    // linearity test
    for (int i=0; i<NUM_REPS_LIN; i++) {
      int base = i*NUM_LIN_QUERIES;
      lin1 = v->lin_test(f_answers[rho*num_lin_pcp_queries + base + Q1],
                         f_answers[rho*num_lin_pcp_queries + base + Q2],
                         f_answers[rho*num_lin_pcp_queries + base + Q3],
                         prime);

      lin2 = v->lin_test(f_answers[rho*num_lin_pcp_queries + base + Q4],
                         f_answers[rho*num_lin_pcp_queries + base + Q5],
                         f_answers[rho*num_lin_pcp_queries + base + Q6],
                         prime);

#if VERBOSE == 1
      if (false == lin1 || false == lin2)
        cout<<"LOG: F1, F2 failed the linearity test"<<endl;
      else
        cout<<"LOG: F1, F2 passed the linearity test"<<endl;
#endif

      result = result & lin1 & lin2;
    }

    // divisibilty correction test
    mpz_set(lhs, f_answers[rho*num_lin_pcp_queries + Q10]);
    mpz_sub(lhs, lhs, f_answers[rho*num_lin_pcp_queries + Q4]);

    mpz_mul(lhs, lhs, d_star[rho]);
    mpz_mod(lhs, lhs, prime);

    int base = rho * (1 + size_input + size_output);
    mpz_set(A_tau, A_tau_io[base+0]);
    mpz_set(B_tau, B_tau_io[base+0]);
    mpz_set(C_tau, C_tau_io[base+0]);

    // note: input and output are contiguous; so we can do this in one
    // loop
    for (int i=0; i<size_input+size_output; i++) {
      mpz_mul(rhs, input[i], A_tau_io[base+1+i]);
      mpz_add(A_tau, A_tau, rhs);

      mpz_mul(rhs, input[i], B_tau_io[base+1+i]);
      mpz_add(B_tau, B_tau, rhs);

      mpz_mul(rhs, input[i], C_tau_io[base+1+i]);
      mpz_add(C_tau, C_tau, rhs);
    }

    mpz_sub(rhs, f_answers[rho*num_lin_pcp_queries + Q7], f_answers[rho*num_lin_pcp_queries + Q1]);
    mpz_add(A_tau, A_tau, rhs);

    mpz_sub(rhs, f_answers[rho*num_lin_pcp_queries + Q8], f_answers[rho*num_lin_pcp_queries + Q1]);
    mpz_add(B_tau, B_tau, rhs);

    mpz_sub(rhs, f_answers[rho*num_lin_pcp_queries + Q9], f_answers[rho*num_lin_pcp_queries + Q1]);
    mpz_add(C_tau, C_tau, rhs);

    mpz_mul(rhs, A_tau, B_tau);
    mpz_add(rhs, rhs, C_tau);
    mpz_mod(rhs, rhs, prime);
    corr = mpz_cmp(lhs, rhs);
#if VERBOSE == 1
    if (0 == corr)
      cout <<"LOG: F1, F2 passed the divisibility correction test"<<endl;
    else
      cout <<"LOG: F1, F2 failed the divisibility correction test"<<endl;
#endif
    if (0 == corr)
      result = result & true;
    else
      result = result & false;
  }
  return result;
}

bool ZComputationVerifier::run_correction_and_circuit_tests(uint32_t beta) {
  return run_interactive_tests(beta);
}
