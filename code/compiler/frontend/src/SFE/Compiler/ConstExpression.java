// ConstExpression.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Vector;

import SFE.Compiler.Operators.UnaryPlusOperator;

/**
 * The ConstExpression class represents consts expressions that
 * can appear in the program.
 */
public abstract class ConstExpression extends Expression implements Inlineable {
  private String name;

  public ConstExpression(String name) {
    this.name = name;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * If null is passed in, than this constant is not in the Consts map.
   *
   * Otherwise, set a str such that Consts.fromName(str) returns this.
   */
  public void setName(String string) {
    this.name = string;
  }
  /**
   * If this returns null, than this constant is not in the Consts map.
   *
   * Otherwise, Returns str such that Consts.fromName(str) returns this.
   */
  public String getName() {
    return name;
  }

  public Expression changeReference(VariableLUT unique) {
    //Const expressions dont reference other variables
    return this;
  }

  public Vector getUnrefLvalInputs() {
    //Const expressions dont have any inputs
    return new Vector();
  }

  public Expression inline(Object obj, StatementBuffer constraints) {
    //Default implementation returns this
    return this;
  }

  public ConstExpression bitAt(int i) {
    return null;
  }

  public Vector<ConstExpression> getDerivedCvalues() {
    return getType().getDerivedCvalues(this);
  }

  public static ConstExpression toConstExpression(Expression base) {
    if (base instanceof ConstExpression) {
      return (ConstExpression)base;
    }
    if (base instanceof UnaryOpExpression){
      UnaryOpExpression bo = (UnaryOpExpression) base;
      if (bo.op instanceof UnaryPlusOperator){
        return toConstExpression(bo.getMiddle());
      }
    }
    if (base instanceof LvalExpression) {
      LvalExpression lv = (LvalExpression)base;
      Statement as = lv.getAssigningStatement();
      if (as instanceof AssignmentStatement) {
        for(Expression q : ((AssignmentStatement) as).getAllRHS()) {
          ConstExpression got = ConstExpression.toConstExpression(q);
          if (got != null) {
            return got;
          }
        }
      }
    }
    return null;
  }
}
