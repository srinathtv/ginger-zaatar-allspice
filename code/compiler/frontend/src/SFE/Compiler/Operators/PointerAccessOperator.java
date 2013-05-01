package SFE.Compiler.Operators;

import SFE.Compiler.AnyType;
import SFE.Compiler.CompileTimeOperator;
import SFE.Compiler.Expression;
import SFE.Compiler.Function;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.Pointer;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;

public class PointerAccessOperator extends Operator implements CompileTimeOperator{
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
  public LvalExpression resolve(Expression ... args) {
    Pointer a = Pointer.toPointer(args[0]);
    return a.access().changeReference(Function.getVars());
  }
}
