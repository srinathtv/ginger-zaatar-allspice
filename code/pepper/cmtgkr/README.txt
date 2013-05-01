==========================
Implementing a new computation in CMT-GKR.
==========================

Note: See hamdist_cmtgkr.cpp (not matmul_cmtgkr.cpp) for an example.

--------
Overview
--------
Implementing a new computation is as simple as inheriting from the CmtgkrApp
class and implementing the appropriate methods. Specifically, the general steps
are:
  1) Implement construct_circuit()
     a) Build the circuit.
     b) For each layer:
            Implement the mult_i and add_i functions.
            They are the multilinear extension of the wiring
            predicates of each layer.
  2) Implement interact_with_prover()
     There's not much to do here. Just a straight copy of the version in
     HamdistCmtgkr would probably work, as long as the vectors are allocated
     correctly (i.e., with more than enough elements).
       - There used to be more work here. See the matmul implementation if you
         care.
  3) Implement main()
     Again, just call run_gkr_app() with the new class as the template
     parameter.

Once completed, building the computation is as easy as doing:
    % make bin/cmt_gkr/<name_of_computation>_cmtgkr
The main method *must* be in the file "cmt_gkr/<name_of_computation>_cmtgkr.cpp"

The executable will take in a single variable. This specifies the log of the
input size. For instance, for matrix multiply, of m x m matrixes, the variable
specifies log2(m). For Hamming distance, it specifies log2() of the input
vectors.


-----------------
Build the circuit
-----------------
The general that you'd need to build is done in 3 stages:
    1) Input layer
    2) Compute Layers
    3) FLT layer.

The input layer consists of gates for the input, output, and a '0' gate.
The output is the prover's claimed output and is negated.

The compute layers does the actual computation with the inputs. It also
preserves the output and '0' gate at each layer by adding them with the '0'
gate.

The FLT layer adds the computed input (from the compute layer) to the
prover's claimed output pairwise, then FLT each of the sums. Finally, it sums
up all of the FLT'ed sums. The circuit is valid if the resulting sum is equal
to 0.

For example, to compute a + b = c, we have:
    Input layer:        a   b  -c    0
                        \   /   \   /
                         \ /     \ / 
     Computes a + b  -->  +       + <-- Preserves the claimed output.
                           \_   _/
                             \ /
                              +
                              |
                        FLT The output.


If a and b are vectors of length n, then the above circuit would be reproduced
n times, along with an sum at the end to add the n FLT-ed results.


There are quite a lot of code designed to abstract away the index jugglings
that might arise when building a circuit. Important ones are:

Circuit::make_shell(int)
    - This must be called first with the depth of the circuit. (Should not be
      required, but it was the easiest to do). This should be the only time you
      need to manually compute indexes.

Circuit::make_level(int level_size, fnptr add_i, fnptr mult_i)
    - The add_i and mult_i functions are the MLE of the wiring predicate.
    - You don't have to specify add_i and mult_i, but it makes things easier.
      Otherwise, you'd have to write lots more code in interact_with_prover().
      See the matmul implementation for the gory details.
    - This method will also set various variables so that Circuit::cgates()
      and Circuit::cgates_len() work correctly.

Circuit::cgates(int offset = 0)
    - With no parameters, this method returns the array of Gates for the
      current level -- that is, the level last created by a call to make_level.
    - The parameter is an offset that can be useful.

Circuit::cgates_len(int offset = 0)
    - Same deal as cgates.

Circuit::print()
    - Pretty print the circuit to stdout. There's a commented out piece of code
      in cmtgkr_app.cpp after the call to evaluate_circuit(). It's very useful
      when implementin the mult_i and add_i functions.

Circuit::make_flt*
Circuit::add_to_last_gate
Circuit::[add|mul]_gates
Circuit::reduce
     - These automatically construct common circuit patterns in the current
       layer (as set by make_level()).
     - See the implementation for more details.
     - The FLT methods are slightly annoying, since FLT is not super regular.

class Circuit
     - The class contains lots of data, including the prime and prime-1 (for
       use with FLT). The prime is read by default from the file
       "computation_state/prime.txt". It uses load_txt_scalar(), so the folder
       might change.


-----------------
The MLE Functions
-----------------
Every circuit level induces a single mult_i and add_i function based on the
wiring predicate. You need to make a different one for every layer pattern
in the circuit.

The functions have the signature:
fn(mpz_t rop, mpz_t *r, int ni, int nip1, int mi, int mip1, mpz_t prime);

Parameters:
   - The result should the stored in rop.
   - r is the input vector.
     - Going by CMT's notations, these functions take (p, \omega1, \omega2).
         - p       = r[0]...r[mi-1]                 - output gate
         - \omega1 = r[mi]...r[mi+mip1-1]           - in1
         - \omega2 = r[mi+mip1]...r[mi+2*mip1-1]    - in2
       They represent gate indexes in each gate's respective level.

On boolean inputs, the function should agree with the wiring predicate, which
is equal to 1 iff (p, \omega1, \omega2) represent an actual connection (gate)
in the current level.

The construction is not hard to do if the circuit level layouts are all
regular. See Appendix A of CMT's paper for examples.

There are also some functions to make things easier.

one_sub(mpz_t rop, mpz_t op)
    - computes rop = 1 - op.

modmult() and its flavors, addmodmult() and its flavors, modadd()
    - Self explanatory.
    - Try not to use the mod functions too much. For example, in loops or at
      the end of functions. Using them too much may adversely affect
      efficiency.

check_equal() and its flavors
    - Check that a certain bit range is the same between two or three gate
      indexes.

check_zero() and its flavors
    - Check that a certain bit range is 0 for some gate indexes.

chi() and mul_chi()
    - Compute the MLE for a single gate index. The mul_* version multiples the
      result with and returning the product instead of just returning the
      result.

check_wiring()
    - Compute the MLE for a single gate connection. This function will add the
      result to rop and return the sum. (Be careful).

Lots of other stuff.
    - These are specific to other computations, but they might still be useful.


--------------------------------
Implement interact_with_prover()
--------------------------------
This is pretty straightforward. The only thing to note is the use of
compute_first_level(). This is an optimization of the log-tree sum that appears
at the end of every circuit. Essentially CMT is treating that as a specialized
sum gate that has more than 2 inputs, which can be verified all at once instead
of in log2(n) iterations for computations with outputs of length n.

The last parameter that the function takes is the level at which this final
log-tree sum begins. It should always be called first.
