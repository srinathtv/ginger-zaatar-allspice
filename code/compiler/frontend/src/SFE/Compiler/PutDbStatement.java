package SFE.Compiler;

import java.io.PrintWriter;
import java.util.Collection;

/**
 * A statement which writes to the database.
 */
public class PutDbStatement extends StatementWithOutputLine implements OutputWriter{
  private LvalExpression[] addrs;
  private LvalExpression value;
  private int outputLine;

  public PutDbStatement(LvalExpression[] addrs, LvalExpression value) {
    this.addrs = addrs;
    this.value = value;
  }

  private static LvalExpression[] col2Arr(Collection<LvalExpression> a){
    LvalExpression[] toRet = new LvalExpression[a.size()];
    int p = 0;
    for(LvalExpression q : a){
      toRet[p++]= q;
    }
    return toRet;
  }
  public PutDbStatement(Collection<LvalExpression> addrs, LvalExpression value) {
    this(col2Arr(addrs),value);
  }

  public Statement toSLPTCircuit(Object obj) {
    for(int i = 0; i < addrs.length; i++){
      addrs[i] = addrs[i].bitAt(0);
    }
    value = value.bitAt(0);
    
    return this; 
  }

  public Statement duplicate() {
    return new PutDbStatement(addrs, value);
  }

  public void toAssignmentStatements(StatementBuffer assignments) {
    //Change refs, and reference them
    for(int i = 0; i < addrs.length; i++){
      addrs[i] = addrs[i].changeReference(Function.getVars());
      addrs[i].addReference();
    }
    value = value.changeReference(Function.getVars());
    value.addReference();
    
    outputLine = Program.getLineNumber();
    assignments.add(this);
  }
  public void toAssignmentStatements_NoChangeRef(StatementBuffer assignments) {
    //Change refs, and reference them
    for(int i = 0; i < addrs.length; i++){
      addrs[i].addReference();
    }
    value.addReference();
    
    outputLine = Program.getLineNumber();
    assignments.add(this);
  }  

  public int getOutputLine() {
    return outputLine;
  }
  
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print(getOutputLine()+" putdb ");
    circuit.print("inputs [ ADDR ");
    for(LvalExpression q : this.addrs){
      q.toCircuit(obj, circuit);
      circuit.print(" ");
    }
    circuit.print("X ");
    value.toCircuit(obj, circuit);
    circuit.print(" ]\t//");
    circuit.print("void");
    circuit.println();
  }
}
