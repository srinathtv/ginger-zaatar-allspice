// IfStatement.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import SFE.Compiler.Operators.NotEqualOperator;
import SFE.Compiler.PolynomialExpression.PolynomialTerm;


/**
 * A class for representing if statement that can be defined
 * in the program.
 */
public class IfStatement extends Statement implements Optimizable {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Holds the condition of the if statement.
   */
  private Expression condition;

  /*
   * Holds a variable name guaranteed not to collide with program structures
   */
  private String nameOfCondition;

  /*
   * Holds the block of the if statement.
   */
  private Statement thenBlock;

  /*
   * Holds the else block of the if statement.
   */
  private Statement elseBlock;

  //~ Constructors -----------------------------------------------------------

  /**
   * Construct a new if statement.
   * @param condition the condition of the if statement.
   * @param thenBlock the block of the if statement.
   * @param elseBlock the block of the else statement.
   */
  public IfStatement(Expression condition, Statement thenBlock, Statement elseBlock, String nameOfCondition) {
    this.condition     = condition;
    this.thenBlock     = thenBlock;
    this.elseBlock     = elseBlock;
    this.nameOfCondition = nameOfCondition;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Unique vars transformations.
  public Statement uniqueVars() {
  	//Uniquify the condition first. Though, it will do nothing at this point.
  	condition = condition.changeReference(Function.getVars());

  	if (elseBlock == null){
  		throw new RuntimeException("Assertion error");
  	}

  	if (condition.size() != 1){
  		throw new RuntimeException("Error: condition was not a boolean value");
  	}

  	BlockStatement newBlock = new BlockStatement();

  	//start new scope
  	Function.pushScope();

  	//unique vars transformations on the if block
  	newBlock.addStatement(thenBlock.uniqueVars());

  	//end the scope
  	Map<String, LvalExpression> thenScope = Function.popScope();

  	//start new scope
  	Function.pushScope();

  	//unique vars transformations on the if block
  	newBlock.addStatement(elseBlock.uniqueVars());

  	//end the scope
  	Map<String, LvalExpression> elseScope = Function.popScope();

  	//Get the scope holding the if statement
  	VariableLUT beforeIf = Function.getVars();

  	HashSet<String> ConflictVariables = new HashSet();
  	ArrayList<String> ConflictVariablesSorted = new ArrayList();
  	for(String lvalName : thenScope.keySet()){
  		LvalExpression lvalBeforeIf = Function.getVars().getVar(lvalName);
  		if (lvalBeforeIf != null){
  			ConflictVariables.add(lvalName);
  			ConflictVariablesSorted.add(lvalName);
  		}
  	}
  	for(String lvalName : elseScope.keySet()){
  		if (ConflictVariables.contains(lvalName)){
  			continue;
  		}
  		LvalExpression lvalBeforeIf = Function.getVars().getVar(lvalName);
  		if (lvalBeforeIf != null){
  			ConflictVariables.add(lvalName);
  			ConflictVariablesSorted.add(lvalName);
  		}
  	}

  	Collections.sort(ConflictVariablesSorted);

  	for(String lvalName : ConflictVariablesSorted){
  		LvalExpression valueTrue, valueFalse, valueMostRecent;
  		{
  			//lval before if block
  			LvalExpression lvalBeforeIf = Function.getVars().getVar(lvalName);
  			if (lvalBeforeIf.size() != 1){
  				throw new RuntimeException("Error: non primitive type in if statement evaluation");
  			}
  			LvalExpression lvalInThen = (LvalExpression) thenScope.get(lvalName);
  			if (lvalInThen != null){
  				//The variable is overwritten in the then block.
  				LvalExpression lvalInElse = (LvalExpression) elseScope.get(lvalName);
  				if (lvalInElse != null){
  					//The variable is overwritten in the else block
  					valueTrue = lvalInThen;
  					valueFalse = lvalInElse;
  					valueMostRecent = lvalInElse;
  				} else {
  					valueTrue = lvalInThen;
  					valueFalse = lvalBeforeIf;
  					valueMostRecent = lvalInThen;
  				}
  			} else {
  				//The variable is not overwritten in the then block.
  				LvalExpression lvalInElse = (LvalExpression) elseScope.get(lvalName);
  				//The variable is overwritten in the else block.
  				valueTrue = lvalBeforeIf;
  				valueFalse = lvalInElse;
  				valueMostRecent = lvalInElse;
  			}

  			if (valueTrue.size()!=1 || valueFalse.size()!=1){
  				throw new RuntimeException("Error: non primitive type in if statement evaluation");
  			}
  		}

  		//valueTrue * condition + valueFalse + valueFalse * (-1 * condition) = x*c + y*(1-c)
  		OperationExpression mux = FloatExpressions.mux(condition, valueTrue, valueFalse);

  		//We can't call updateVars on this created assignment, because the right hand side uses variables inside the
  		//if scope, which assignmentStatement.updateVars() will mistakenly redirect to the variables on the outer scope!
  		Function.addVar(valueMostRecent);
  		valueMostRecent = Function.getVar(valueMostRecent);
  		AssignmentStatement newAs = new AssignmentStatement(valueMostRecent,mux); //perform the mux
  		newAs.dedicateAssignment();
  		newAs.setOutputLine(Program.getLineNumber()); //Set the line number of the as
  		newBlock.addStatement(newAs); //Add it to the reult
  	}

  	return newBlock;
  }
   */

  /**
   * Transforms this multibit AssignmentStatement into singlebit statements
   * and returns the result.
   * @param obj not needed (null).
   * @return a BlockStatement containing the result statements.
   */
  public BlockStatement toSLPTCircuit(Object obj) {
    BlockStatement result = new BlockStatement();

    // create a temp var that holds the condition result
    LvalExpression condition_asBoolean =
      Function.addTempLocalVar("conditionResult" + nameOfCondition,
                               new BooleanType());

    // create the assignment statement that assings the result
    AssignmentStatement conditionResultAs =
      new AssignmentStatement(condition_asBoolean,
                              new BinaryOpExpression(new NotEqualOperator(),
                                  condition,
                                  new BooleanConstant(false)));

    // evaluate the condition and stores it in the conditionResult
    result.addStatement(conditionResultAs.toSLPTCircuit(null));

    condition = condition_asBoolean.bitAt(0);

    //Defer this processing until toAssignments.

    //thenBlock = thenBlock.toSLPTCircuit(null);

    //elseBlock = elseBlock.toSLPTCircuit(null);

    result.addStatement(this);

    return result;
  }

  /**
   * Returns a replica of this IfStatement.
   * @return a replica of this IfStatement.
   */
  public Statement duplicate() {
    return new IfStatement(condition.duplicate(), thenBlock.duplicate(), elseBlock.duplicate(), nameOfCondition);
  }

  /**
   * Returns a string representation of this IfStatement.
   * @return a string representation of this IfStatement.
   */
  public String toString() {
    return "IF (" + condition + ")\nTHEN\n" + thenBlock + "ELSE\n" +
           elseBlock;
  }

  //~ Static fields/initializers ---------------------------------------------

  public void optimize(Optimization job) {
    //This optimization is only done in the high level stage, because uniqueVars erases if statements
    switch(job) {
    case RENUMBER_ASSIGNMENTS:
      //Allow the if and else block to receive numberings.
      ((Optimizable)thenBlock).optimize(Optimization.RENUMBER_ASSIGNMENTS);
      if (elseBlock != null) ((Optimizable)elseBlock).optimize(Optimization.RENUMBER_ASSIGNMENTS);
      break;
    case DUPLICATED_IN_FUNCTION:
      ((Optimizable)thenBlock).optimize(Optimization.DUPLICATED_IN_FUNCTION);
      ((Optimizable)elseBlock).optimize(Optimization.DUPLICATED_IN_FUNCTION);
      condition.duplicatedInFunction();
      break;
    default:
      //It's potentially dangerous to perform an optimization on a system which doesn't implement it
      throw new RuntimeException("Optimization not implemented: "+job);
    }
  }

  public void blockOptimize(BlockOptimization job, List body) {
    switch(job) {
    case DEADCODE_ELIMINATE:
      //Deadcode elimination currently does nothing
      break;
    default:
      //It's potentially dangerous if we perform an optimization and only some parts of our system implement it.
      //Catch that.
      throw new RuntimeException("Optimization not implemented: "+job);
    }
  }
  public void buildUsedStatementsHash() {
    //Currently deadcode elimination of if statements not implemented
    Optimizer.putUsedStatement(this);

    //The if and else block are privately owned by this if, so we hide them from the optimizer

    //Ensure that the condition can be evaluated
    Collection<LvalExpression> v = condition.getUnrefLvalInputs();

    for(LvalExpression q : v) {
      Statement as = q.getAssigningStatement();
      Optimizer.putUsedStatement(as);
    }
  }

  public void toAssignmentStatements(StatementBuffer statements) {
    condition = Function.getVar((LvalExpression)condition);

    BooleanConstant asConst = BooleanConstant.toBooleanConstant(condition);

    if (elseBlock == null) {
      throw new RuntimeException("Assertion error");
    }

    if (condition.size() != 1) {
      throw new RuntimeException("Error: condition was not a boolean value");
    }

    //Constant condition evaluation?
    if (asConst != null) {
      if (asConst.getConst()) {
        thenBlock = thenBlock.toSLPTCircuit(null);
        thenBlock.toAssignmentStatements(statements);
        return;
      } else {
        elseBlock = elseBlock.toSLPTCircuit(null);
        elseBlock.toAssignmentStatements(statements);
        return;
      }
    }

    //so condition is an lvalue.

    //start new scope
    Function.pushScope();
    //statements.pushShortCircuit((LvalExpression)condition, IntConstant.ONE);

    statements.pushUncertainty();
    //toSLPT must occur here, so that the created intermediate variables are correctly scoped
    thenBlock = thenBlock.toSLPTCircuit(null);
    //unique vars transformations on the if block
    thenBlock.toAssignmentStatements(statements);

    //end the scope
    //statements.popShortCircuit();
    Map<String, LvalExpression> thenScope = Function.popScope();

    //start new scope
    Function.pushScope();
    //statements.pushShortCircuit((LvalExpression)condition, new IntConstant(0));

    //toSLPT must occur here, so that the created intermediate variables are correctly scoped
    elseBlock = elseBlock.toSLPTCircuit(null);
    //unique vars transformations on the if block
    elseBlock.toAssignmentStatements(statements);
    statements.popUncertainty();

    //end the scope
    //statements.popShortCircuit();
    Map<String, LvalExpression> elseScope = Function.popScope();

    //Get the scope holding the if statement
    VariableLUT beforeIf = Function.getVars();

    HashSet<String> ConflictVariables = new HashSet();
    TreeMap<Integer, String> ConflictVariablesSorted = new TreeMap();
    for(LvalExpression lvalInThen : thenScope.values()) {
      String name = lvalInThen.getName();
      LvalExpression lvalBeforeIf = Function.getVars().getVar(name);
      if (lvalBeforeIf != null && lvalBeforeIf.getOutputLine() != -1) { //There is some amount of cruft in the variable lookup table. FIXME
        ConflictVariables.add(name);
        ConflictVariablesSorted.put(((AssignmentStatement)lvalInThen.getAssigningStatement()).getOutputLine(), name);
      }
    }
    for(LvalExpression lvalInElse : elseScope.values()) {
      String name = lvalInElse.getName();
      if (ConflictVariables.contains(name)) {
        continue;
      }
      LvalExpression lvalBeforeIf = Function.getVars().getVar(name);
      if (lvalBeforeIf != null && lvalBeforeIf.getOutputLine() != -1) {
        ConflictVariables.add(name);
        ConflictVariablesSorted.put(((AssignmentStatement)lvalInElse.getAssigningStatement()).getOutputLine(), name);
      }
    }

    for(String lvalName : ConflictVariablesSorted.values()) {
      LvalExpression valueTrue, valueFalse, valueMostRecent;
      {
        //lval before if block
        LvalExpression lvalBeforeIf = Function.getVars().getVar(lvalName);

        if (lvalBeforeIf.size() != 1) {
          throw new RuntimeException("Error: non primitive type in if statement evaluation");
        }
        LvalExpression lvalInThen = (LvalExpression) thenScope.get(lvalName);
        if (lvalInThen != null) {
          //The variable is overwritten in the then block.
          LvalExpression lvalInElse = (LvalExpression) elseScope.get(lvalName);
          if (lvalInElse != null) {
            //The variable is overwritten in the else block
            valueTrue = lvalInThen;
            valueFalse = lvalInElse;
            valueMostRecent = lvalInElse;
          } else {
            valueTrue = lvalInThen;
            valueFalse = lvalBeforeIf;
            valueMostRecent = lvalInThen;
          }
        } else {
          //The variable is not overwritten in the then block.
          LvalExpression lvalInElse = (LvalExpression) elseScope.get(lvalName);
          //The variable is overwritten in the else block.
          valueTrue = lvalBeforeIf;
          valueFalse = lvalInElse;
          valueMostRecent = lvalInElse;
        }

        if (valueTrue.size()!=1 || valueFalse.size()!=1) {
          throw new RuntimeException("Error: non primitive type in if statement evaluation");
        }
      }

      //valueTrue * condition + valueFalse + valueFalse * (-1 * condition) = x*c + y*(1-c)

      OperationExpression mux = null;

      if (AssignmentStatement.combineExpressions) {
        //If valueTrue or valueFalse have mux values, can we combine expressions?
        Statement valueTrue_ = valueTrue.getAssigningStatement();
        Statement valueFalse_ = valueFalse.getAssigningStatement();

        if (valueTrue_ instanceof AssignmentStatement && !valueTrue.isReferenced()) {
          MuxPolynomialExpression rhs_ = MuxPolynomialExpression.toMuxExpression(((AssignmentStatement)valueTrue_).getLHS());

          if (rhs_ != null) {
            if (mux == null && rhs_.getValueFalse() == valueFalse) {
              //So mux has the form condition ? (condition2 ? rhs_.getValueTrue() : valueFalse) : valueFalse
              //Hence replace mux with (c1) * (c2) ? rhs_.getValueTrue() : valueFalse
              PolynomialExpression c1c2 = new PolynomialExpression();
              PolynomialTerm t1 = new PolynomialTerm();
              t1.addFactor((LvalExpression)condition);
              t1.addFactor(rhs_.getCondition());
              c1c2.addMultiplesOfTerm(IntConstant.ONE, t1);
              LvalExpression c1c2_ = newLvalOrExisting(c1c2, valueMostRecent.getName(), statements);
              mux = new MuxPolynomialExpression(c1c2_, rhs_.getValueTrue(), valueFalse);
              //System.out.println("0");
            }
            if (mux == null && rhs_.getValueTrue() == valueFalse) {
              //So mux has the form condition ? (condition2 ? valueFalse : rhs_.getValueFalse()) : valueFalse
              //Hence replace mux with (c1) * (1 - c2) ? rhs_.getValueFalse() : valueFalse
              PolynomialExpression cond = new PolynomialExpression();
              PolynomialTerm t = new PolynomialTerm();
              t.addFactor((LvalExpression)condition);
              cond.addMultiplesOfTerm(IntConstant.ONE, t);
              {
                PolynomialExpression oneMinC2 = new PolynomialExpression();
                PolynomialTerm one = new PolynomialTerm();
                PolynomialTerm t2 = new PolynomialTerm();
                t2.addFactor(rhs_.getCondition());
                oneMinC2.addMultiplesOfTerm(IntConstant.ONE, one);
                oneMinC2.addMultiplesOfTerm(IntConstant.NEG_ONE, t2);
                t.addFactor(oneMinC2);
              }
              LvalExpression cond_ = newLvalOrExisting(cond, valueMostRecent.getName(), statements);
              mux = new MuxPolynomialExpression(cond_, rhs_.getValueFalse(), valueFalse);
              //System.out.println("1");
            }
          }
        }
        if (valueFalse_ instanceof AssignmentStatement && !valueFalse.isReferenced()) {
          MuxPolynomialExpression rhs_ = MuxPolynomialExpression.toMuxExpression(((AssignmentStatement)valueFalse_).getLHS());

          if (rhs_ != null) {
            if (mux == null && rhs_.getValueFalse() == valueTrue) {
              //So mux has the form condition ? valueTrue : (condition2 ? rhs_.getValueTrue() : valueTrue)
              //So replace it with (1-c1)*(c2) ? rhs_.getValueTrue() : valueTrue
              PolynomialExpression cond = new PolynomialExpression();
              PolynomialTerm t = new PolynomialTerm();
              {
                PolynomialExpression oneMinC1 = new PolynomialExpression();
                PolynomialTerm one = new PolynomialTerm();
                PolynomialTerm t2 = new PolynomialTerm();
                t2.addFactor(condition);
                oneMinC1.addMultiplesOfTerm(IntConstant.ONE, one);
                oneMinC1.addMultiplesOfTerm(IntConstant.NEG_ONE, t2);
                t.addFactor(oneMinC1);
              }
              t.addFactor(rhs_.getCondition());
              cond.addMultiplesOfTerm(IntConstant.ONE, t);
              LvalExpression cond_ = newLvalOrExisting(cond, valueMostRecent.getName(), statements);
              mux = new MuxPolynomialExpression(cond_, rhs_.getValueTrue(), valueTrue);
              //System.out.println("2");
            }
            if (mux == null && rhs_.getValueTrue() == valueTrue) {
              //So mux has the form condition ? valueTrue : (condition2 ? valueTrue : rhs_.getValueFalse())
              //So replace it with (1-c1)*(1-c2) ? rhs_.getValueFalse() : valueTrue
              PolynomialExpression cond = new PolynomialExpression();
              PolynomialTerm t = new PolynomialTerm();
              {
                PolynomialExpression oneMinC1 = new PolynomialExpression();
                PolynomialTerm one = new PolynomialTerm();
                PolynomialTerm t2 = new PolynomialTerm();
                t2.addFactor(condition);
                oneMinC1.addMultiplesOfTerm(IntConstant.ONE, one);
                oneMinC1.addMultiplesOfTerm(IntConstant.NEG_ONE, t2);
                t.addFactor(oneMinC1);
              }
              {
                PolynomialExpression oneMinC2 = new PolynomialExpression();
                PolynomialTerm one = new PolynomialTerm();
                PolynomialTerm t2 = new PolynomialTerm();
                t2.addFactor(rhs_.getCondition());
                oneMinC2.addMultiplesOfTerm(IntConstant.ONE, one);
                oneMinC2.addMultiplesOfTerm(IntConstant.NEG_ONE, t2);
                t.addFactor(oneMinC2);
              }
              cond.addMultiplesOfTerm(IntConstant.ONE, t);
              LvalExpression cond_ = newLvalOrExisting(cond, valueMostRecent.getName(), statements);
              mux = new MuxPolynomialExpression(cond_, rhs_.getValueFalse(), valueTrue);
              //System.out.println("3");
            }
          }
        }
      }

      //Inlining was not possible? Output as-is.
      if (mux == null) {
        mux = new MuxPolynomialExpression((LvalExpression)condition, valueTrue, valueFalse);
        //System.out.println("-1");
      }

      //Assign the mux to the output variable
      Function.addVar(valueMostRecent);
      valueMostRecent = Function.getVar(valueMostRecent);

      AssignmentStatement valueas = new AssignmentStatement(valueMostRecent, ((MuxPolynomialExpression)mux).toBinaryOps());

      BlockStatement result = (BlockStatement) valueas.toSLPTCircuit(null);

      //Make sure that the mux information is preserved
      List<Statement> resultStates = result.getStatements();
      AssignmentStatement actualMuxAssign = ((AssignmentStatement)resultStates.get(resultStates.size() - 1));
      actualMuxAssign.addAlternativeRHS(mux);

      for(Statement as : result.getStatements()) {
        ((AssignmentStatement)as).toAssignmentStatements_NoChangeRef(statements);
      }
      //We can't call updateVars on this created assignment, because the right hand side uses variables inside the
      //if scope, which assignmentStatement.updateVars() will mimuxstakenly redirect to the variables on the outer scope!
      //newAs.toAssignmentStatements_NoChangeRef(statements);
    }
  }

  /**
   * If the optimizer has an lval associated with the given expression, return it.
   *
   * Otherwise, create a new lval to hold the given expression and an assignment to store
   * the expression in that new lval, and add that assignment to the statement buffer.
   * During this process, changeReferences is never called on the input expression. This means
   * this method is safe to use within the if statement transformation.
   * Also associate the created lval with the given expression in the optimizer.
   */
  private LvalExpression newLvalOrExisting(Expression expr, String prefixForNewLval, StatementBuffer statements) {
    LvalExpression existing = Optimizer.getLvalFor(expr);

    if (existing == null) {
      BlockStatement result = new BlockStatement();
      LvalExpression newLval = (LvalExpression) expr.evaluateExpression(prefixForNewLval, "condProduct", expr.size(), result).bitAt(0);
      ((AssignmentStatement)result.getStatements().get(0)).toAssignmentStatements_NoChangeRef(statements);
      Optimizer.addAssignment(expr, newLval);
      return newLval;
    } else {
      return existing;
    }
  }
}
