#ifndef CODE_PEPPER_LIBV_COMPUTATION_P_H_
#define CODE_PEPPER_LIBV_COMPUTATION_P_H_

#include <libv/prover.h>
#include <libv/exogenous_checker.h>
#include <iterator>
#include <sstream>

#include <NTL/ZZ.h>
#include <NTL/ZZ_p.h>
#include <NTL/vec_ZZ_p.h>
#include <NTL/ZZ_pX.h>
NTL_CLIENT


class ComputationProver : public Prover {
  protected:
    uint32_t *F1_index;
    int temp_stack_size;
    int num_vars, num_cons;
    mpz_t temp, temp2;
    mpz_t *input, *output, *input_output;
    mpq_t *input_q, *output_q, *input_output_q, *temp_qs, *F1_q;
    mpq_t temp_q, temp_q2, temp_q3;
    int size_input, size_output, size_constants, size_f1_vec, size_f2_vec;

    ExogenousChecker* exogenous_checker;
    mpz_t *F1, *F2;
    mpz_t *f1_commitment, *f2_commitment;
    mpz_t *f1_consistency, *f2_consistency;
    mpz_t *f1_q1, *f1_q2, *f1_q3, *f1_q4, *f1_q5;
    mpz_t *f2_q1, *f2_q2, *f2_q3, *f2_q4;

    void compute_from_pws(const char* pws_filename);
    mpq_t& voc(const std::string& str, mpq_t& use_if_constant);
    void compute_poly(std::istream_iterator<std::string>& cmds, int);
    void compute_less_than_int(std::istream_iterator<std::string>& cmds);
    void compute_less_than_float(std::istream_iterator<std::string>& cmds);
    void compute_split_unsignedint(std::istream_iterator<std::string>& cmds);
    void compute_split_int_le(std::istream_iterator<std::string>& cmds);

  public:
    ComputationProver(int ph, int b_size, int num_r, int size_input, const char *name_prover);
    ~ComputationProver();
};

#endif  // CODE_PEPPER_LIBV_COMPUTATION_P_H_
