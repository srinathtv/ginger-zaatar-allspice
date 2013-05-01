// LessOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import java.io.PrintWriter;

import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.BooleanConstant;
import SFE.Compiler.BooleanType;
import SFE.Compiler.Expression;
import SFE.Compiler.FloatConstant;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.OutputWriter;
import SFE.Compiler.RestrictedUnsignedIntType;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;


/**
 * A class for representing &lt; operator expressions that can be defined
 * in the program.
 */
public class LessOperator extends Operator implements OutputWriter, SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   */
  public String toString() {
    return "<";
  }

  /**
   * Returns 2 as the arity of this PlusOperator.
   * Arity is 1 for unary ops; 2 for binary ops; 3 for ternary ops;
   * 0 for constants
   * @return 2 as the arity of this PlusOperator.
   */
  public int arity() {
    return 2;
  }

  /**
   * Transforms this multibit expression into singlebit statements
   * and returns the result.
   * Note:  x-y&lt;0 &lt;==&gt; x&lt;y.
   * @param obj the AssignmentStatement that holds this GreaterOperator.
   * @return a BlockStatement containing the result statements.
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    AssignmentStatement as     = ((AssignmentStatement) obj);
    LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
    BinaryOpExpression  rhs    = (BinaryOpExpression) (as.getRHS());
    BlockStatement      result = new BlockStatement();

    Expression          left  = rhs.getLeft();
    Expression          right = rhs.getRight();

    //-1 to 1, 0 and 1 to 0 == (x-1)*(x)/2

    result.addStatement(new AssignmentStatement(
                          lhs.lvalBitAt(0),
                          new BinaryOpExpression(this, left.bitAt(0), right.bitAt(0))
                        ));
    return result;

    /*
    Expression comparison = new BinaryOpExpression(new ComparisonOperator(), left, right).evaluateExpression(as, result);
    Expression less = new BinaryOpExpression(new TimesOperator(), new FloatConstant(1,2),
    		new BinaryOpExpression(new TimesOperator(),
    				comparison,
    				new BinaryOpExpression(new PlusOperator(), comparison, new IntConstant(-1))
    				)).evaluateExpression(as, result);

    //We don't have less gates. We have a sign gate, and polynomials.
    result.addStatement(new AssignmentStatement(
    		lhs,
    		new UnaryOpExpression(new UnaryPlusOperator(), less)
    		).toSLPTCircuit(null));

    return result;
    */
  }

  public Expression inlineOp(StatementBuffer assignments, Expression ... args) {
    BooleanConstant bc = resolve(args);
    if (bc != null){
      return bc;
    }
    
    Expression left = args[0];
    Expression right = args[1];

    Expression leftLvalConst = FloatConstant.toFloatConstant(left);
    Expression rightLvalConst = FloatConstant.toFloatConstant(right);

    if (leftLvalConst == null) {
      leftLvalConst = LvalExpression.toLvalExpression(left);
    }

    if (rightLvalConst == null) {
      rightLvalConst = LvalExpression.toLvalExpression(right);
    }

    if (leftLvalConst == null || rightLvalConst == null) {
      return null;
    }

    return new BinaryOpExpression(this, leftLvalConst, rightLvalConst);
  }
  public BooleanConstant resolve(Expression... args) {
    Expression left = args[0];
    Expression right = args[1];
    FloatConstant valA = FloatConstant.toFloatConstant(left);
    FloatConstant valB = FloatConstant.toFloatConstant(right);

    if (valA != null && valB != null) {
      return new BooleanConstant(valA.compareTo(valB) < 0);
    }
    if (valB != null && valB.isZero() && (left.getType() instanceof RestrictedUnsignedIntType)){
      return BooleanConstant.FALSE;
    }
    return null;
  }

  public Type getType(Object obj) {
    return new BooleanType();
  }

  /**
   * Returns an int theat represents the priority of the operator
   * @return an int theat represents the priority of the operator
   */
  public int priority() {
    return 1;
  }

  public void toCircuit(Object obj, PrintWriter circuit) {
    BinaryOpExpression expr = ((BinaryOpExpression)obj);
    ((OutputWriter)expr.getLeft()).toCircuit(null, circuit);
    circuit.print(" < ");
    ((OutputWriter)expr.getRight()).toCircuit(null, circuit);
  }
}
