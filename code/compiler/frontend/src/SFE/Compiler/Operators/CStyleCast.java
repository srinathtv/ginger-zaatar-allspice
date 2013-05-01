package SFE.Compiler.Operators;

import java.io.PrintWriter;

import SFE.Compiler.AssignmentStatement;
import SFE.Compiler.BitString;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.Expression;
import SFE.Compiler.IntType;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.OutputWriter;
import SFE.Compiler.SLPTReduction;
import SFE.Compiler.Statement;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;
import SFE.Compiler.TypeHeirarchy;
import SFE.Compiler.UnaryOpExpression;


public class CStyleCast extends Operator implements SLPTReduction, OutputWriter {
  private Type targetType;

  public CStyleCast(Type targetType) {
    if (!(targetType instanceof IntType) || targetType.size() > 1){
      throw new RuntimeException("Currently can only do C-style casts of int types");
    }
    this.targetType = targetType;
  }

  public int arity() {
    return 1;
  }

  public int priority() {
    throw new RuntimeException("Not implemented");
  }

  public Type getType(Object obj) {
    return targetType;
  }

  public Statement toSLPTCircuit(Object obj) {
    AssignmentStatement as     = ((AssignmentStatement) obj);
    BlockStatement      result = new BlockStatement();
    LvalExpression lhs = as.getLHS();

    UnaryOpExpression rhs = (UnaryOpExpression)as.getRHS();
    Expression middle = rhs.getMiddle();

    middle = middle.evaluateExpression(lhs.getName(), "M", 1, result);
    AssignmentStatement subAs = new AssignmentStatement(lhs.lvalBitAt(0),new UnaryOpExpression(this, middle.bitAt(0)));
    result.addStatement(subAs);

    return result;
  }

  public Expression inlineOp(StatementBuffer sb, Expression ... args) {
    Expression in = args[0];
    if (TypeHeirarchy.isSubType(in.getType(), targetType)){
      //No problem~!
      return in;
    }
    //Whoops, have to cast down.
    return BitString.toBitString(targetType, sb, in);
  }

  public void toCircuit(Object obj, PrintWriter circuit) {
    UnaryOpExpression expr = (UnaryOpExpression)obj;
    ((OutputWriter)expr.getMiddle()).toCircuit(null, circuit);
  }

  public String toString() {
    return "C-style-cast("+targetType+")";
  }

  public Expression resolve(Expression... args) {
    //leave it for inlineOp to take care of.
    return null;
  }
}
