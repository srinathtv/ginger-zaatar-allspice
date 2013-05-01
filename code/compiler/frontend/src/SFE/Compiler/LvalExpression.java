// LvalExpression.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;
import java.util.List;
import java.util.Vector;


/**
 * The LvalExpression class represents an Expression that can
 * appear as LHS in the program.
 */
public class LvalExpression extends Expression implements OutputWriter, Inlineable {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Holds the lvalue of this Expression
   */
  private Lvalue    lvalue;
  private BitString bitString;

  private StatementWithOutputLine assigningStatement = null;
  private int referenceCount = 0;
  private int referenceCountUB = -1;
  private int killPoint = Integer.MAX_VALUE;
  //private List<AssignmentStatement> references = null;

  //~ Constructors -----------------------------------------------------------

  /**
   * LvalExpression constractor
   * @param lvalue
   */
  public LvalExpression(Lvalue lvalue) {
    this.lvalue = lvalue;
  }

  //~ Methods ----------------------------------------------------------------

  /**
       * Returns the number of bits needed to represent this expression.
       * @return the number of bits needed to represent this expression.
       */
  public int size() {
    return lvalue.size();
  }

  /**
       * Returns a string representation of the object.
       * @return a string representation of the object.
       */
  public String toString() {
    if (assigningStatement == null) {
      return "broken-ref";
    }
    return Integer.toString(getOutputLine());
  }

  public int hashCode() {
    return getOutputLine();
  }

  public int getOutputLine() {
    if (assigningStatement == null) {
      throw new RuntimeException();
    }
    if (assigningStatement instanceof InputStatement) {
      return ((InputStatement) assigningStatement).getOutputLine();
    }
    return ((AssignmentStatement) assigningStatement).getOutputLine();
  }

  /**
   * Returns true if some output assignment (thus far) has this lvalexpression as a dependency.
   */
  public boolean isReferenced() {
    return referenceCount > 0;
  }

  public int getReferenceCount() {
    return referenceCount;
  }
  public int getReferenceCountUB() {
    return this.referenceCountUB;
  }

  public List<AssignmentStatement> getReferences() {
    return null; //references;
  }

  /**
   * Add a reference to this lval expression
   * @param as
   */
  public void addReference() {
    referenceCount++;
  }
  /**
   * Remove a reference from this lval expression
   */
  public void removeReference() {
    referenceCount--;
  }

  /**
     * Returns this Lavlue.
     * @return this Lavlue.
     */
  public Lvalue getLvalue() {
    return lvalue;
  }

  /**
     * Returns Expression that represents the bit at place i of this Expression
     * @return Expression that represents the bit at place i of this Expression
     */
  public LvalExpression bitAt(int i) {
    if (lvalue.getName().endsWith("$0")) {
      return this;
    }
    return Function.getVarBitAt(this, i);
  }

  /**
     * Returns LvalExpression that represents the bit at place i of this Expression
     * @return LvalExpression that represents the bit at place i of this Expression
     */
  public LvalExpression lvalBitAt(int i) {
    return (LvalExpression) bitAt(i);
  }

  /**
     * Returns the name of this LvalExpression's lvalue.
     * @return a string representing this LvalExpression's lvalue.
     */
  public String getName() {
    return lvalue.getName();
  }

  /**
     * Returns the Type of this LvalExpression's lvalue.
     * @return the Type of this LvalExpression's lvalue.
     */
  public Type getType() {
    return lvalue.getType();
  }

  /**
   * Returns the declared type of this LvalExpression's lvalue
   */
  public Type getDeclaredType() {
    return ((VarLvalue)lvalue).getDeclaredType();
  }

  /**
     * Sets the Type of this LvalExpression's lvalue.
     */
  public void setType(Type T) {
    lvalue.setType(T);
  }

  /**
      * Returns true if the this expression is a part out the circuit's output.
      * @return true if the this expression is a part out the circuit's output.
      */
  public boolean isOutput() {
    return lvalue.isOutput();
  }

  /**
     * Set the reference to this expressionn assigning statement, Which can be either AssignmentStatement
     * or InputStatement.
     * @param as the assigning statement.
     */
  public void setAssigningStatement(StatementWithOutputLine as) {
    if (this.assigningStatement == null) {
      this.assigningStatement = as;
    }
  }
  public void setAssigningStatement(StatementWithOutputLine as, boolean force) {
    if (this.assigningStatement == null || force) {
      this.assigningStatement = as;
    }
  }
  public void clearAssigningStatement() {
    assigningStatement = null;
  }

  /**
     * Returns the assigning statement of this lvalexpression.
     * @return the assigning statement of this lvalexpression.
     */
  public StatementWithOutputLine getAssigningStatement() {
    if (assigningStatement != null){
      if (assigningStatement.getOutputLine() < 0){
        return null; //Not valid until the assignment statement is actually output.
      }
    }
    return assigningStatement;
  }
  
  public Expression evaluateExpression(String goal, String tempName, int size, BlockStatement result) {
    if (size() != 1){
      throw new RuntimeException("Assertion error");
    }
    return bitAt(0);
  }

  /**
   * Prints this LvalExpression into the circuit.
   * @param circuit the circuit output file.
   */
  public void toCircuit(Object obj, PrintWriter circuit) {
    if (assigningStatement instanceof InputStatement) {
      circuit.print(((InputStatement) assigningStatement).getOutputLine());
    } else {
      circuit.print(((AssignmentStatement) assigningStatement).getOutputLine());
    }
  }

  public Expression inline(Object obj, StatementBuffer assignments) {
    //We couldn't inline the referenced assignment, but can we inline this object?
    if (assigningStatement instanceof AssignmentStatement) {
      //Try to substitute the assigning statement's rhs in, if available
      AssignmentStatement as = (AssignmentStatement)assigningStatement;
      //Inline the referenced expression (recursively inlines)
      Expression inlineRhs;
      if (referenceCountUB == 1 &&
          !this.isOutput() && //don't inline through the final output lines, because it won't remove a variable
          AssignmentStatement.combineExpressions) {
        //If an RHS is available, return it and trash the expression
        for(Expression toRet : ((AssignmentStatement) as).getAllRHS()) {
          assignments.callbackAssignment(as);
          return toRet;
        }
      } else {
        //Can't return value in general, but if we can return it as a const or lval, do so
        for(Expression toRet : ((AssignmentStatement) as).getAllRHS()) {
          ConstExpression ce = ConstExpression.toConstExpression(toRet);
          if (ce != null) {
            return ce;
          }
          LvalExpression le = LvalExpression.toLvalExpression(toRet);
          if (le != null) {
            return le;
          }
        }
      }
    }
    return this;
  }

  public LvalExpression changeReference(VariableLUT unique) {
    //Fix references that weren't covered in toSLPT.
    String name = getName();
    if (!getType().hasDerives()){
      if (!name.endsWith("$0")){
        name += "$0";
      }
    }
    //Return the unique version of the variable with this name.
    LvalExpression toRet = unique.getVar(name);
    if (toRet==null){
      throw new RuntimeException();
    }
    return toRet;
  }

  public Vector getUnrefLvalInputs() {
    Vector toRet = new Vector();
    if (!isReferenced()) {
      toRet.add(this);
    }
    return toRet;
  }

  /**
     * sets this LvalExpression as a pin that is not an output of this
     * circuit.
     */
  public void notOutput() {
    lvalue.notOutput();
  }

  /**
   * Indicate that this lval is referenced at most x times.
   * @param refCount
   */
  public void setUBRefCount(int refCount) {
    referenceCountUB = refCount;
  }

  /**
   * Indicate that this lval is not referenced after statement killPoint
   */
  public void setKillPoint(int killPoint) {
    if (killPoint < 0) {
      killPoint = Integer.MAX_VALUE;
    }
    this.killPoint = killPoint;
  }

  public int getKillPoint() {
    return killPoint;
  }

  public static LvalExpression toLvalExpression(Expression c) {
    if (c instanceof LvalExpression) {
      return (LvalExpression)c;
    }
    if (c instanceof UnaryOpExpression) {
      UnaryOpExpression uo = ((UnaryOpExpression)c);
      return toLvalExpression(uo.getOperator().resolve(uo.getMiddle()));
    }
    if (c instanceof BinaryOpExpression) {
      BinaryOpExpression bo = ((BinaryOpExpression)c);
      return toLvalExpression(bo.getOperator().resolve(bo.getLeft(), bo.getRight()));
    }
    return null;
  }

  public void setBitString(BitString other){
    this.bitString = other;
  }
  public BitString getBitString() {
    return bitString;
  }
}