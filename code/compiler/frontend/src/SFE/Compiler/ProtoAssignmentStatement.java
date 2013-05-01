package SFE.Compiler;

import SFE.Compiler.Operators.CStyleCast;
import SFE.Compiler.Operators.Operator;



/**
 * A proto-assignment statement is an assignment of the form
 * LHS = RHS;
 * 
 * or potentially
 * 
 * LHS (op)= RHS
 * 
 * where LHS is an expression which resolves to an Lval and RHS is an expression. During expansion to 
 * assignment statements, the LHS is looked up first, the RHS is then looked up, and then an assignment
 * between the two results is emitted. op is a binaryOperator.
 */
public class ProtoAssignmentStatement extends Statement{
  private Expression lhsLookup;
  private Expression rhs;
  private Operator binaryOperator;
  private CStyleCast cast;
  
  private LvalExpression resolvedLHS; //Can be queried by expressions after toAssignmentStatements has been called
  
  public ProtoAssignmentStatement(Expression lhsLookup, Expression rhs, CStyleCast castResult) {
    this.lhsLookup = lhsLookup;
    this.rhs = rhs;
    this.cast = castResult;
  }

  public ProtoAssignmentStatement(Expression lhsLookup, Expression rhs, Operator binaryOperator, CStyleCast castResult) {
    this.lhsLookup = lhsLookup;
    this.rhs = rhs;
    this.binaryOperator = binaryOperator;
    this.cast = castResult;
  }

  public Statement toSLPTCircuit(Object obj) {
    return this; 
  }

  public Statement duplicate() {
    return new ProtoAssignmentStatement(lhsLookup.duplicate(), rhs.duplicate(), binaryOperator, cast);
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    //Apply unique vars to LHS lookup
    lhsLookup = lhsLookup.changeReference(Function.getVars());
    //Lookup the lhs
    resolvedLHS = LvalExpression.toLvalExpression(lhsLookup);
    if (resolvedLHS.getAssigningStatement() == null){
      //Zero out the variable, as this is the first assignment to it.
      throw new RuntimeException("Assignment to uninitialized variable in protoAssignmentStatement");
    }
    //Do we have an operation to perform?
    if (binaryOperator != null){
      rhs = new BinaryOpExpression(binaryOperator, resolvedLHS, rhs);
    }
    if (cast != null){
      rhs = new UnaryOpExpression(cast, rhs);
    }
    
    //Emit the AS, and process it to finish the job
    AssignmentStatement toRet = new AssignmentStatement(resolvedLHS, rhs);
    Statement slptCircuit = toRet.toSLPTCircuit(null);
    slptCircuit.toAssignmentStatements(assignments);
  }
  
  public Expression getResolvedLHS(){
    Expression toRet = new UnaryOpExpression(new Operator(){
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
        return resolve(args);
      }
      public LvalExpression resolve(Expression... args) {
        if (resolvedLHS == null){
          throw new RuntimeException("Resolve called too soon - lhsLookup not available");
        }
        return resolvedLHS.changeReference(Function.getVars());
      }
    }, IntConstant.ZERO);
    toRet.metaType = lhsLookup.metaType;
    return toRet;
  }
}
