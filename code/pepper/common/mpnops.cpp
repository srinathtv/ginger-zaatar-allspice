#include "utility.h"
#include "mpnops.h"

// Define table for mpz_t
static inline void mpz_t_init(mpz_t n) { mpz_set_ui(n, 0); }

template <> mpz_ops::zeroary_fn mpz_ops::alloc_init = alloc_init_scalar;
template <> mpz_ops::zeroary_fn mpz_ops::init = mpz_t_init;
template <> mpz_ops::zeroary_fn mpz_ops::clear = mpz_clear;
template <> mpz_ops::unary_fn mpz_ops::set = mpz_set;
template <> mpz_ops::binary_fn mpz_ops::add = mpz_add;
template <> mpz_ops::binary_fn mpz_ops::sub = mpz_sub;
template <> mpz_ops::binary_fn mpz_ops::mul = mpz_mul;
template <> mpz_ops::binary_fn mpz_ops::div = mpz_div;

// Define table for mpq_t
static inline void mpq_t_init(mpq_t n) { mpq_set_ui(n, 0, 1); }
static inline void mpq_alloc_init(mpq_t n) { mpq_init(n); mpq_t_init(n); }

template <> mpq_ops::zeroary_fn mpq_ops::alloc_init = mpq_alloc_init;
template <> mpq_ops::zeroary_fn mpq_ops::init = mpq_t_init;
template <> mpq_ops::zeroary_fn mpq_ops::clear = mpq_clear;
template <> mpq_ops::unary_fn mpq_ops::set = mpq_set;
template <> mpq_ops::binary_fn mpq_ops::add = mpq_add;
template <> mpq_ops::binary_fn mpq_ops::sub = mpq_sub;
template <> mpq_ops::binary_fn mpq_ops::mul = mpq_mul;
template <> mpq_ops::binary_fn mpq_ops::div = mpq_div;


