#ifndef CODE_PEPPER_COMMON_MPNOPS_H_
#define CODE_PEPPER_COMMON_MPNOPS_H_

#include <gmp.h>
// Singleton of function pointers to GMP functions. This simulates a vtable
// to fake inheritance for templated types.
template<typename T>
class mpn_ops
{
  private:
    mpn_ops() {}

  public:
    // Workaround. For the type void (*binary_fn)(T, const T, const T), the
    // compiler will for some reason ignore the const modifiers when
    // compiling. compiler bug?
    typedef const T ConstT;
    typedef void (*binary_fn)(T rop, ConstT op1, ConstT op2);
    static binary_fn add;
    static binary_fn sub;
    static binary_fn mul;
    static binary_fn div;

    typedef void (*zeroary_fn)(T op);
    static zeroary_fn alloc_init;
    static zeroary_fn init;
    static zeroary_fn clear;

    typedef void (*unary_fn)(T rop, ConstT op);
    static unary_fn set;
};

typedef mpn_ops<mpz_t> mpz_ops;
typedef mpn_ops<mpq_t> mpq_ops;
#endif  // CODE_PEPPER_COMMON_MPNOPS_H_
