package SFE.Compiler.Operators;

import ccomp.CMemoryMap;
import SFE.Compiler.AnyType;
import SFE.Compiler.CompileTimeOperator;
import SFE.Compiler.Expression;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.Pointer;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.StatementWithOutputLine;
import SFE.Compiler.Type;

public class GetPointerOperator extends Operator implements CompileTimeOperator{
  public int arity() {
    return 1;
  }
  public int priority() {
    throw new RuntimeException("Not implemented");
  }
  public Type getType(Object obj) {
    //No information until we've resolved the pointer during inlining.
    return new AnyType();
  }
  public Expression inlineOp(StatementBuffer assignments, Expression... args) {
    return resolve(args);
  }
  public Pointer resolve(Expression ... args) {
    LvalExpression a = LvalExpression.toLvalExpression(args[0]);
    StatementWithOutputLine as = a.getAssigningStatement();
    int outputLine = as.getOutputLine();
    return new Pointer(a.getType(), new LvalExpression[]{a}, CMemoryMap.STACK+outputLine, CMemoryMap.STACK+outputLine);
  }
}
