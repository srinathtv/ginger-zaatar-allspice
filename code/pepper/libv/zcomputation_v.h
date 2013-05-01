#ifndef CODE_PEPPER_APPS_ZCOMPUTATION_V_H_
#define CODE_PEPPER_APPS_ZCOMPUTATION_V_H_

#include <libv/verifier.h>
#include <libv/input_creator.h>
#include <libv/zpcp.h>

#ifdef INTERFACE_MPI
#include <libv/zcomputation_p.h>
#endif

class ZComputationVerifier : public Verifier {
  protected:
    int chi, n;
    int n_prime;

    mpz_t omega;
    poly_compressed *poly_A, *poly_B, *poly_C;
    mpz_t *eval_poly_A, *eval_poly_B, *eval_poly_C;
    int num_aij, num_bij, num_cij;
    mpz_t A_tau, B_tau, C_tau;

    mpz_t *d_star, *A_tau_io, *B_tau_io, *C_tau_io;
    mpz_t *set_v;
    mpz_t *f1_q1, *f1_q2, *f1_q3, *f1_q4, *f1_commitment, *f1_consistency;
    mpz_t *f2_q1, *f2_q2, *f2_q3, *f2_q4, *f2_commitment, *f2_consistency;
    mpz_t temp, temp2, temp3, lhs, rhs;
    mpz_t *temp_arr, *temp_arr2;
    InputCreator* input_creator;

    void init_state();
    void init_qap(const char *);
    void create_input();
    bool run_interactive_tests(uint32_t beta);
    void create_plain_queries();
    void populate_answers(mpz_t *f_answers, int rho, int num_repetitions, int beta);
    bool run_correction_and_circuit_tests(uint32_t beta);
  public:
    ZComputationVerifier(int batch, int reps, int ip_size, int out_size,
                         int num_vars, int num_cons, int optimize_answers, char *prover_url,
                         const char *name_prover, int, int, int, const char *file_name_qap);
    ~ZComputationVerifier();
};
#endif  // CODE_PEPPER_APPS_POLYEVAL_D2_V_H_
