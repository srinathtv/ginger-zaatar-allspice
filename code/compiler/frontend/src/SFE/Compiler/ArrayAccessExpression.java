package SFE.Compiler;

import java.io.PrintWriter;
import java.util.Vector;

import SFE.Compiler.Operators.Operator;
import SFE.Compiler.Operators.UnaryPlusOperator;

public class ArrayAccessExpression extends OperationExpression {
  public static class ArrayAccessOperator extends Operator {
    public int arity() {
      return 0;
    }
    public int priority() {
      return 0;
    }
    public Type getType(Object obj) {
      throw new RuntimeException("Not implemented");
    }
    public Expression inlineOp(StatementBuffer sb, Expression... args) {
      throw new RuntimeException("Not implemented");
    }
    public Expression resolve(Expression... args) {
      throw new RuntimeException("Not implemented");
    }
  }

  private Expression base;
  private Vector<OperationExpression> expressions;
  //private Vector<Integer> lengths;
  private Type baseType;
  private Vector<Integer> bitID; //for each of these entries, recursively call .bitAt() before returning the lvalue

  public ArrayAccessExpression(Expression base, Vector<Expression> expressions, /*Vector<Integer> lengths,*/ Type baseType) {
    this(base, expressions, /*lengths,*/ baseType, new Vector(1));
  }

  private ArrayAccessExpression(Expression base, Vector<Expression> expressions, /*Vector<Integer> lengths,*/ Type baseType, Vector<Integer> bitID) {
    super(new ArrayAccessOperator());
    this.base = base;

    this.expressions = new Vector(1);
    for(int i = 0; i < expressions.size(); i++) {
      Expression expr = expressions.get(i);
      if (expr instanceof OperationExpression) {
        this.expressions.add((OperationExpression)expr);
      } else {
        this.expressions.add(new UnaryOpExpression(new UnaryPlusOperator(), expr));
      }
    }
    //this.lengths = lengths;
    this.baseType = baseType;
    this.bitID = bitID;
  }

  public Vector getUnrefLvalInputs() {
    throw new RuntimeException("not implemented");
  }
  /**
   * Returns a replica this statement.
   * @return a replica this statement.
   */
  public Expression duplicate() {
    Vector nExpressions = new Vector(1);
    for(Expression k : expressions) {
      nExpressions.add(k.duplicate());
    }

    return new ArrayAccessExpression(
             base,
             nExpressions,
             //lengths,
             baseType,
             bitID);
  }

  /**
   * Returns an arrayexpression holding the value of the i-th bit of this arrayexpression.
   */
  public Expression bitAt(int i) {
    //duplicate is not required, but do it just in case
    Vector nExpressions = new Vector(1);
    for(Expression k : expressions) {
      nExpressions.add(k.duplicate());
    }

    Vector<Integer> nBitID = new Vector(1);
    nBitID.addAll(bitID);
    nBitID.add(i);
    Type nType;
    if (baseType.hasDerives()) {
      nType = ((ParentType)baseType).getDerivedTypeAt(i);
    } else {
      nType = baseType; //Objects of types without derived types return themselves as bitAt(n), for any n.
    }

    return new ArrayAccessExpression(
             base,
             nExpressions,
             //lengths,
             nType,
             nBitID);
  }

  public int size() {
    throw new RuntimeException("Not yet implemented");
  }

  public BlockStatement toSLPTCircuit(Object obj) {
    BlockStatement toRet = new BlockStatement();

    AssignmentStatement as = ((AssignmentStatement) obj);
    LvalExpression lhs = as.getLHS();
    ArrayAccessExpression rhs = (ArrayAccessExpression)as.getRHS();

    //expand the expressions
    for(int i = 0; i < expressions.size(); i++) {
      Expression before = expressions.get(i);

      //String tempPrefix = "ArrayAccess"+array_access_ctr++ +":";

      //Each expression must evaluate to an index, which has size 1.
      Expression after = before.evaluateExpression(lhs.getName(), "aae"+i, 1, toRet); //performs toSLPT conversion
      after = after.bitAt(0);
      if (after instanceof OperationExpression) {
        expressions.set(i, (OperationExpression)after);
      } else {
        expressions.set(i,new UnaryOpExpression(new UnaryPlusOperator(), after));
      }
    }

    //Expand nonprimitive type
    for(int i = 0; i < lhs.size(); i++) {
      //result.addStatement(as);
      AssignmentStatement subAs = new AssignmentStatement(
        lhs.lvalBitAt(i), //is a single bit
        (OperationExpression)rhs.bitAt(i) //no need to further subdivide, array access is a single op
        //and all the expressions (above) have been evaluated.
      );
      toRet.addStatement(subAs);
    }

    return toRet;
  }

  /**
   * Called during uniqueVars()
   */
  public Expression changeReference(VariableLUT unique) {
    //Apply unique vars to the expressions
    for(int i = 0; i < expressions.size(); i++) {
      Expression after = expressions.get(i).changeReference(unique);
      if (after instanceof OperationExpression) {
        expressions.set(i, (OperationExpression)after);
      } else {
        expressions.set(i, new UnaryOpExpression(new UnaryPlusOperator(), after));
      }
    }
    //At this point, any of the references in any expression in expressions should be resolvable.
    int[] indexes = new int[expressions.size()];
    boolean isCompileTimeIndirection = true;
    for(int i = 0; i < expressions.size(); i++) {
      Expression before = expressions.get(i);
      ConstExpression after = IntConstant.toIntConstant(before);
      if (after != null) {
        IntConstant intConstant = IntConstant.toIntConstant((ConstExpression) after);
        indexes[i] = intConstant.toInt();
      } else {
        throw new RuntimeException("Could not resolve array index to constant: "+before);
      }
    }

    if (isCompileTimeIndirection) {
      String varName = null;
      ConstExpression asConst = ConstExpression.toConstExpression(base);
      LvalExpression asLval = LvalExpression.toLvalExpression(base);
      if (asConst != null) {
        varName = asConst.getName();
      } else if (asLval != null) {
        varName = asLval.getName();
      } else {
        throw new RuntimeException("I don't know how to treat " + base +" as an array.");
      }

      String[] varNameSplited = (varName+"$").split("\\[\\$\\]");
      String str = new String();

      for (int i = 0; i < indexes.length; i++)
        str += (varNameSplited[i] + "[" + indexes[i] + "]");

      if (varNameSplited.length > indexes.length) {
        str += varNameSplited[varNameSplited.length - 1];
      }

      str = str.substring(0,str.length() - 1);

      if (base instanceof ConstExpression) {
        ConstExpression val = Consts.fromName(str);
        for(int bit : bitID) {
          val = val.bitAt(bit);
        }
        //Calling changeReference on val won't do anything.
        return val;
      } else if (base instanceof LvalExpression) {
        LvalExpression lval = unique.getVar(str);
        for(int bit : bitID) {
          lval = lval.bitAt(bit);
        }
        lval = lval.changeReference(unique);

        return lval;
      } else {
        throw new RuntimeException("I don't know how to treat " + base +" as an array.");
      }
    } else {
      //Produce a dynamic-indirection block statement for this assignment
      throw new RuntimeException("Dynamic indirection not yet implemented");
    }
  }

  public String toString() {
    return base.toString() + " array access @: " + expressions;
  }

  public OperationExpression sortInputs() {
    //Inputs are sorted by default.
    return this;
  }

  public void toCircuit(Object obj, PrintWriter circuit) {
    throw new RuntimeException("not implemented");
  }

  public Type getType() {
    throw new RuntimeException("not implemented");
  }

}