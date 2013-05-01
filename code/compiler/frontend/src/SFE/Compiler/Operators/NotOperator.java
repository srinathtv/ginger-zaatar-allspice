// PlusOperator.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler.Operators;

import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.BooleanConstant;
import SFE.Compiler.BooleanExpressions;
import SFE.Compiler.BooleanType;
import SFE.Compiler.Expression;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.PolynomialExpression;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;
import SFE.Compiler.UnaryOpExpression;
import SFE.Compiler.UnaryOperator;



/**
 * A class for representing binary ! operator expressions that can be defined
 * in the program.
 */
public class NotOperator extends Operator implements UnaryOperator, SLPTReduction {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a string representation of the object.
   */
  public String toString() {
    return "!";
  }

  public int arity() {
    return 1;
  }
  /**
   * Transforms this expression into an equivalent single level circuit of constraint-reducible gates
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    //No conversion needed, plus gate is an arithmetic gate

    AssignmentStatement as     = ((AssignmentStatement) obj);
    LvalExpression      lhs    = as.getLHS(); //LHS of the param statement
    BlockStatement      result = new BlockStatement();

    UnaryOpExpression rhs = (UnaryOpExpression)as.getRHS();

    Expression middle = rhs.getMiddle();
    //Logical type - so the intermediate variable holding the middle must be of size 1
    middle = middle.evaluateExpression(lhs.getName(), "M", 1, result);

    //For now, we require that the arguments are obviously booleans.
    /*
    result.addStatement(new AssignmentStatement(
    		as.getLHS(),
    		BooleanExpressions.not(middle)
    		).toSLPTCircuit(null));
    		*/
    result.addStatement(new AssignmentStatement(
                          as.getLHS().lvalBitAt(0),
                          new UnaryOpExpression(this, middle.bitAt(0))
                        ));

    return result;
  }

  public Expression inlineOp(StatementBuffer assignments, Expression ... args) {
    Expression middle = args[0];

    PolynomialExpression pmid = PolynomialExpression.toPolynomialExpression(middle);
    if (pmid != null) {
      if (pmid.getDegree() <= 2) {
        return BooleanExpressions.not(pmid);
      }
    }

    return null;
  }
  
  public Expression resolve(Expression ... args){
    Expression mid = args[0];

    BooleanConstant midc = BooleanConstant.toBooleanConstant(mid);
    if (midc != null){
      return new BooleanConstant((1 - midc.value()) == 1);
    }
    return null;
  }


  public Type getType(Object obj) {
    return new BooleanType();
  }

  /**
   * Returns an int that represents the priority of the operator
   * @return an int that represents the priority of the operator
   */
  public int priority() {
    return 4;
  }
}
