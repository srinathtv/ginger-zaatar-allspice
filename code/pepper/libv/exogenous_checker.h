#ifndef CODE_PEPPER_LIBV_EXOGENOUS_CHECKER_H_
#define CODE_PEPPER_LIBV_EXOGENOUS_CHECKER_H_

#include <common/utility.h>

class ExogenousChecker {
  public:
    ExogenousChecker();
    virtual bool exogenous_check(const mpz_t* input, const mpq_t* input_q,
                                 int num_inputs, const mpz_t* output, const mpq_t* output_q, int num_outputs, mpz_t prime) = 0;
    virtual void baseline(const mpq_t* input_q, int num_inputs,
                          mpq_t* output_recomputed, int num_outputs) = 0;
};
#endif  // CODE_PEPPER_LIBV_EXOGENOUS_CHECKER_H_
