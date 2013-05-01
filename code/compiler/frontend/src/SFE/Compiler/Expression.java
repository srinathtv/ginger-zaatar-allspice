// Expression.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Collection;

/**
 * Abstract class for representing expressions that can be defined
 * in the program.
 */
public abstract class Expression {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns the number of bits needed to represent this expression.
   * @return the number of bits needed to represent this expression.
   */
  public abstract int size();

  /**
   * This method should be overriden by subclasses that can return a single bit
   * from their expression. This implementations returns this if i = 0 and throws an arrayindexoutofboundsexception otherwise.
   *
   * WARNING: calling bitAt can change the state of an object. Do not assume that calling bitAt with the same index
   * twice returns the same result.
   * @return null
   */
  public Expression bitAt(int i) {
    if (i==0)
      return this;
    throw new ArrayIndexOutOfBoundsException(i);
  }

  /**
   * Recursivly calculates inner arithmetic expression. This implementation
   * returns this. Expressions that return something more complicated
   * should override this method.
   *
   * The name of the intermediate variable created is (goal):(tmpName)
   *
   * @param as the AssignmentStatement that holds this expression (as rhs).
   * @param size - the size of the temporary variable (must be known before hand)
   * @param result the BlockStatement to hold the result in.
   * @return the result expression.
   */
  public Expression evaluateExpression(String goal, String tempName, int size, BlockStatement result) {
    return this;
  }

  /**
   * Tells the expression that it has been duplicated in a function.
   *
   * Used for static_function operations
   */
  public void duplicatedInFunction() {
    //Nothing.
  }

  /**
     * Change the references this expression makes, using unique to resolve variables,
     * and return the resulting expression.
     */
  public abstract Expression changeReference(VariableLUT unique);

  /**
     * Returns an array of the input LvalExpressions of this expression
     * which are not already marked as being referenced (have a zero
     * reference count.).
     *
     * It is O.K. for implementations of this method to return lvalexpressions
     * which have a nonzero reference count as well. But this leads to unecessary
     * work, as marking something referenced twice is equivalent to once.
     */
  public abstract Collection<LvalExpression> getUnrefLvalInputs();

  /**
   * Returns this expression. Expression are not duplicated.
   * @return this expression. Expression are not duplicated.
   */
  public Expression duplicate() {
    return this;
  }

  /**
   * Returns the type of this expression.
   */
  public abstract Type getType();

  /**
   * Context variable, used by various compilers to add metadata to expressions.
   */
  public Type metaType;
}
