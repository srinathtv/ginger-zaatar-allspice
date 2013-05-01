// OperationExpression.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import SFE.Compiler.Operators.Operator;
import SFE.Compiler.Operators.UnaryPlusOperator;


/**
 * class OperationExpression defines expressions containing operatioins, that can
 * be defined in the program.
 */
public abstract class OperationExpression extends Expression
  implements SLPTReduction, OutputWriter {
  //~ Instance fields --------------------------------------------------------

  /*
   * Holds the operator of this expression
   */
  public Operator op;

  //~ Constructors -----------------------------------------------------------

  /**
       * Constracts a new OperationExpression from a given Operator.
       * @param op this OperationExpression operator.
       */
  public OperationExpression(Operator op) {
    this.op     = op;
  }

  //~ Methods ----------------------------------------------------------------

  public Operator getOperator() {
    return op;
  }

  /**
         * Sorts the input gates according to their names and returns
         * the result OperationExpression. This method is used in the optimization
         * process. (Right is the smallest, left the biggest)
         * @return the OperationExpression with the sorted inputs.
         */
  public abstract OperationExpression sortInputs();

  /**
   * recursively calculates inner arithmetic expression and inserts them into the
   * proper function.
   * @param as the AssignmentStatement that is associated with this expression (note - used for naming purposes only)
   * @param result a block statement to insert statments if needed.
   * @param size - the size of the temporary variable (must be known before hand)
   * @return the new statement to use instead of as.
   */
  public Expression evaluateExpression(String goal, String tempName, int size, BlockStatement result) {
    if (op instanceof CompileTimeOperator){
      return this;
    }
    
    //For the unary plus operator, just fall through.
    if (op instanceof UnaryPlusOperator) {
      return ((UnaryOpExpression)this).getMiddle().evaluateExpression(goal, tempName, size, result);
    }

    if (goal.endsWith("$0")) {
      goal = goal.substring(0, goal.length()-2);
    }
    //goal = goal.replaceAll("[^:]", "");
    //System.out.println(goal);

    LvalExpression tmpLvalExp = Function.addTempLocalVar(goal + ":" + tempName /*+ tempLabel*/, new BusType(size));

    // create the assignment statement
    AssignmentStatement tempAs = new AssignmentStatement(tmpLvalExp, (OperationExpression) this);

    // evaluate the expression and store it in the tmp lval expression
    result.addStatement(tempAs.toSLPTCircuit(null));

    /*
    tempLabel++;
    if (tempLabel > 1000){
    	System.out.println(this);
    }
    */

    return tmpLvalExp;
  }

  /*
  public void toCircuitPolynomial(Expression expr, PrintWriter circuit){
  	boolean printed = false;
  	if (expr instanceof BinaryOpExpression){
  		BinaryOpExpression boe = ((BinaryOpExpression)expr);
  		if (boe.op instanceof OutputsToPolynomial){
  			toCircuitPolynomial(boe.getLeft(), circuit);
  			circuit.print(" "+boe.op.toString()+" ");
  			toCircuitPolynomial(boe.getRight(), circuit);
  			printed = true;
  		}
  	}
  	if (expr instanceof UnaryOpExpression){
  		UnaryOpExpression boe = ((UnaryOpExpression)expr);
  		if (boe.op instanceof OutputsToPolynomial){
  			if (!(boe.op instanceof UnaryPlusOperator)){
  				circuit.print(boe.op.toString()+" ");
  			}
  			toCircuitPolynomial(boe.getMiddle(), circuit);
  			printed = true;
  		}
  	}
  	if (!printed){
  		((OutputWriter)expr).toCircuit(circuit);
  	}
  }
  */


  /**
   * returns a replica of this Expression
   * @return a replica of this Expression
   */
  public abstract Expression duplicate();

}
