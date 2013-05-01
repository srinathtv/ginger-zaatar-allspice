package SFE.Compiler;

import java.io.PrintWriter;

/**
 * A special kind of assignment statement with multiple LHS's.
 * <lhs1, lhs2, lhs3, ... lhsn> = <bit0, bit1, bit2, ... bitn> of an lval with an integer value.
 */
public class SplitStatement extends StatementWithOutputLine implements OutputWriter{
  private LvalExpression toSplit;
  private AssignmentStatement[] subStatements;
  private Type bitwiseEncoding;
  public SplitStatement(Type bitwiseEncoding, LvalExpression toSplit, LvalExpression[] splitBits){
    this.toSplit = toSplit;
    this.bitwiseEncoding = bitwiseEncoding;
    int N = splitBits.length;
    this.subStatements = new AssignmentStatement[N];
    for(int i = 0; i < N; i++){
      subStatements[i] = new AssignmentStatement(splitBits[i], new Expression[0]);
    }
  }
  public LvalExpression bitAt(int i){
    return subStatements[i].getLHS();
  }
  public Statement toSLPTCircuit(Object obj) {
    if (toSplit.size() != 1){
      throw new RuntimeException("Cannot split expression: "+toSplit);
    }
    toSplit = toSplit.bitAt(0);
    for(int i = 0; i < subStatements.length; i++){
      subStatements[i] = new AssignmentStatement(subStatements[i].getLHS().bitAt(0),new Expression[0]);
    }
    return this;
  }
  public int getOutputLine() {
    return subStatements[0].getOutputLine();
  }
  public Statement duplicate() {
    LvalExpression[] splitBits = new LvalExpression[subStatements.length];
    for(int i = 0; i < splitBits.length; i++){
      splitBits[i] = subStatements[i].getLHS();
    }
    return new SplitStatement(bitwiseEncoding, toSplit, splitBits);
  }
  public void toAssignmentStatements(StatementBuffer assignments) {
    //Change refs
    toSplit = toSplit.changeReference(Function.getVars());
    
    AssignmentStatement as = AssignmentStatement.getAssignment(toSplit);
    if (as != null){
      for(Expression q : as.getAllRHS()){
        boolean success = toAssignmentStatements_(assignments, q);
        if (success){ //Only accept lvalExpressions.
          return;
        }
      }
    }
    
    toAssignmentStatements_(assignments, toSplit);
  }

  public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
    AssignmentStatement as = AssignmentStatement.getAssignment(toSplit);
    if (as != null){
      for(Expression q : as.getAllRHS()){
        boolean success = toAssignmentStatements_(assignments, q);
        if (success){ //Only accept lvalExpressions.
          return;
        }
      }
    }
    
    toAssignmentStatements_(assignments, toSplit);
  }
  private boolean toAssignmentStatements_(StatementBuffer assignments, Expression q) {
    IntConstant ic = IntConstant.toIntConstant(q); 
    if (ic != null){
      //We can break up the splitStatement, because the toSplit is a constant.
      if (true) throw new RuntimeException("Split a constant "+ic+" to bits!");
      return true;
    }
    
    LvalExpression lval = LvalExpression.toLvalExpression(q);
    if (lval != null){
      toSplit = lval;
      //We can set the input to the type it needs to be.
      for(int i = 0; i < subStatements.length; i++){
        subStatements[i].setOutputLine(Program.getLineNumber());
      }
      //Reference the toSplit
      lval.addReference();
      assignments.add(this);
      return true;
    }
    
    return false;
  }
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(getOutputLine()+" split ");
    for(AssignmentStatement q : this.subStatements){
      q.getLHS().toCircuit(obj, circuit);
      circuit.print(" ");
    }
    circuit.print("inputs [ ");
    toSplit.toCircuit(obj, circuit);
    circuit.print(" ]\t//");
    circuit.print(subStatements[0].getLHS().getName()+" "+bitwiseEncoding);
    circuit.println();
  }
}
