package SFE.Compiler;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collection;

import SFE.Compiler.Operators.Operator;

/**
 * A statement that gets a value from the database and stores it in a result.
 */
public class GetDbExpression extends OperationExpression implements Inlineable{
  private LvalExpression[] address;
  private Type assumeReturnType;
  private static class GetDbOperator extends Operator {
    public int arity() {
      throw new RuntimeException("Not implemented");
    }
    public int priority() {
      throw new RuntimeException("Not implemented");
    }
    public Type getType(Object obj) {
      throw new RuntimeException("Not implemented");
    }
    public Expression inlineOp(StatementBuffer assignments, Expression... args) {
      throw new RuntimeException("Not implemented");
    }
    public Expression resolve(Expression... args) {
      throw new RuntimeException("Not implemented");
    }
    public String toString(){
      return "getdb";
    }
  }
  public GetDbExpression(Type assumeReturnType, LvalExpression ... address){
    super(new GetDbOperator());
    this.address = address;
    this.assumeReturnType = assumeReturnType;
  } 
  private static LvalExpression[] col2Arr(Collection<LvalExpression> a){
    LvalExpression[] toRet = new LvalExpression[a.size()];
    int p = 0;
    for(LvalExpression q : a){
      toRet[p++]= q;
    }
    return toRet;
  }
  public GetDbExpression(IntType assumeReturnType, ArrayList<LvalExpression> address) {
    this(assumeReturnType, col2Arr(address));
  }
  public int size() {
    return assumeReturnType.size();
  }
  public Expression changeReference(VariableLUT unique) {
    for(int i = 0; i < address.length; i++){
      this.address[i] = address[i].changeReference(unique);
    }
    return this;
  }
  public GetDbExpression duplicate() {
    return new GetDbExpression(assumeReturnType, address);
  }
  public Collection<LvalExpression> getUnrefLvalInputs() {
    ArrayList<LvalExpression> ins = new ArrayList();
    for(LvalExpression in : address){
      ins.add(in);
    }
    return ins;
  }
  public Expression bitAt(int i){
    if (i != 0){
      throw new RuntimeException("I don't know how to get the "+i+"th bit of a get statement");
    }
    return this;
  }
  public Type getType() {
    return assumeReturnType;
  }
  public Expression inline(Object obj, StatementBuffer history) {
    return this;
  }
  public Statement toSLPTCircuit(Object obj) {
    AssignmentStatement as = (AssignmentStatement)obj;
   
    for(int i = 0; i < address.length; i++){
      address[i] = address[i].bitAt(0);
    }
    
    return new AssignmentStatement(as.getLHS().bitAt(0), 
        this);
  }
  public void toCircuit(Object obj, PrintWriter out) {
    out.print("ADDR ");
    for(LvalExpression q : address){
      q.toCircuit(obj, out);
      out.print(" ");
    }
  }
  public OperationExpression sortInputs() {
    return this;
  }
}
