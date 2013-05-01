package SFE.Compiler.Operators;

import SFE.Compiler.AnyType;
import SFE.Compiler.ArrayType;
import SFE.Compiler.CompileTimeOperator;
import SFE.Compiler.Expression;
import SFE.Compiler.Function;
import SFE.Compiler.IntConstant;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;

public class ArrayAccessOperator extends Operator implements CompileTimeOperator{
  public int arity() {
    return 2;
  }
  public int priority() {
    throw new RuntimeException("Not implemented");
  }
  public Type getType(Object obj) {
    if (true) throw new RuntimeException("Not implemented");
    //Can be determined, but this doesn't need to be implemented. 
    return new AnyType();
  }
  public Expression inlineOp(StatementBuffer sb, Expression... args) {
    //Force this expression to resolve.
    return resolve(args[0]);
  }
  
  public LvalExpression resolve(Expression... args) {
    LvalExpression a = LvalExpression.toLvalExpression(args[0]);
    int index = IntConstant.toIntConstant(args[1]).toInt();
    
    //Check bounds.
    if (!(a.getType() instanceof ArrayType)){
      throw new RuntimeException("Cannot perform array access on value "+a+" with type "+a.getType());
    }
    int len = ((ArrayType)a.getType()).getLength();
    if (index < 0 || index >= len){
      throw new RuntimeException("Cannot perform array access on array "+a+" with length "+len+" and index "+index);
    }
    
    return Function.getVars().getVar(a.getName()+"["+index+"]").changeReference(Function.getVars());
  }
}
