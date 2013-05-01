package SFE.Compiler.Operators;

import SFE.Compiler.AnyType;
import SFE.Compiler.ArrayType;
import SFE.Compiler.CompileTimeOperator;
import SFE.Compiler.Expression;
import SFE.Compiler.Function;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.StructType;
import SFE.Compiler.Type;

public class StructAccessOperator extends Operator implements CompileTimeOperator{
  private String field;
  public StructAccessOperator(String field) {
    this.field = field;
  }
  public int arity() {
    return 1;
  }
  public String getField(){
    return field;
  }
  public int priority() {
    throw new RuntimeException("Not implemented");
  }
  public Type getType(Object obj) {
    //Can be determined, but this doesn't need to be implemented. 
    return new AnyType();
  }
  public Expression inlineOp(StatementBuffer assignments, Expression... args) {
    return resolve(args);
  }
  public LvalExpression resolve(Expression... args) {
    LvalExpression a = LvalExpression.toLvalExpression(args[0]);

    //Check bounds.
    if (!(a.getType() instanceof StructType)){
      throw new RuntimeException("Cannot perform struct field access on value "+a+" with type "+a.getType());
    }
    
    boolean hasField = ((StructType)a.getType()).getFields().contains(field);
    if (!hasField){
      throw new RuntimeException("Cannot access field "+field+" on variable "+a+" with type "+a.getType());
    }
    
    return Function.getVars().getVar(a.getName()+"."+field).changeReference(Function.getVars());
  }
}
