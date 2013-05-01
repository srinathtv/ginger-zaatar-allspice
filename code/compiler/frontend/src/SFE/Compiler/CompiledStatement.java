package SFE.Compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.TreeMap;

import SFE.Compiler.PolynomialExpression.PolynomialTerm;
import SFE.Compiler.Operators.LessOperator;
import SFE.Compiler.Operators.NotEqualOperator;
import SFE.Compiler.Operators.PlusOperator;
import SFE.Compiler.Operators.TimesOperator;

/**
 * A statement based on an already-compiled .circuit file
 */
public class CompiledStatement extends Statement {
  /*
   * Fields
   */
  private File profile;
  private File priorCircuit;
  private HashMap<Integer, LvalExpression> varByNumber;
  private TreeMap<Integer, List<Integer>> killList;

  public CompiledStatement(File profile, File priorCircuit) {
    this.profile = profile;
    this.priorCircuit = priorCircuit;
  }

  /*
   * Methods
   */
  public Statement toSLPTCircuit(Object obj) {
    //Compiled .circuit files are already in SLPT form.
    return this;
  }

  public Statement duplicate() {
    return new CompiledStatement(profile, priorCircuit);
  }

  private BufferedReader profileReader;
  /**
   * Read in the precompiled computation, and simply emit all assignments in that file.
   */
  public void toAssignmentStatements(StatementBuffer assignments) {
    varByNumber = new HashMap();
    killList = new TreeMap();

    try {
      //Set up the profile reader
      profileReader = new BufferedReader(new FileReader(profile));
      //Construct the BufferedReader object
      BufferedReader bufferedReader = new BufferedReader(new FileReader(priorCircuit));

      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        toAssignmentStatements_(assignments, line);
      }
      bufferedReader.close();
      profileReader.close();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private void toAssignmentStatements_(StatementBuffer assignments, String line) {
    Scanner in = new Scanner(line);

    int varNum = nextInt(in);

    while(!killList.isEmpty() && killList.firstKey() < varNum) {
      List<Integer> toKill = killList.remove(killList.firstKey());
      for(Integer q : toKill) {
        varByNumber.remove(q);
      }
    }

    String type = in.next();
    if (type.equals("input")) {
      parseInputLine(varNum, in, assignments);
      return;
    }

    //Not an input statement. See if this statement is used

    ReferenceProfile rp = getReferenceProfileFor(varNum);

    if (rp == null || !rp.isUsed) {
      //No profiling information or unused = ignore this line.
      return;
    }
    
    if (type.equals("split")){
      parseSplitLine(varNum, rp, in, assignments);
      return;
    }
    if (type.equals("putdb")){
      parsePutDBLine(varNum, rp, in, assignments);
      return;
    }

    if (type.equals("output")) {
      in.next();
    }

    Expression rhs = parseOperation(in);

    String varname = in.next().substring("//".length());
    Type vartype_ = readType(in);

    emitAssignment(varNum, varname, vartype_, rp, rhs, assignments);

    //Does this assignment kill off any variables? This optimization is NOT SAFE if inlining is performed this round.
    //Better just to let another round of dead code elimination take care of these, if needed.
    /*
    for (Integer kill : Optimizer.getKillList(varNum)){
    	LvalExpression remove = varByNumber.remove(kill);
    	remove.removeReference(null); //Remove the anticipatory reference that killing a variable removes
    	if (!remove.isReferenced() && (remove.getAssigningStatement() instanceof AssignmentStatement)){
    		//It was never used.
    		assignments.callbackAssignment((AssignmentStatement)remove.getAssigningStatement());
    	}
    }
    */
  }

  private void parseSplitLine(int varNum, ReferenceProfile rp, Scanner in, StatementBuffer assignments) {
    while(true){
      String token = in.next();
      if (token.equals("[")){
        break;
      }
    }
    LvalExpression toSplit = (LvalExpression)parseLvalConst(in.next());
    in.next(); //"]"
    String varname = in.next().substring("//".length()); //of the first bit, i.e. ends with :0$0
    Type bitwiseEncoding = readType(in);
    emitSplitStatement(varNum, varname, bitwiseEncoding, toSplit, rp, assignments);
  }
  private void parsePutDBLine(int varNum, ReferenceProfile rp, Scanner in, StatementBuffer assignments) {
    while(true){
      String token = in.next();
      if (token.equals("[")){
        break;
      }
    }
    in.next(); //"ADDR"
    ArrayList<LvalExpression> addrs = new ArrayList();
    while(true){
      String token = in.next();
      if (token.equals("X")){
        break;
      }
      addrs.add((LvalExpression)parseLvalConst(token));
    }
    LvalExpression value = (LvalExpression)parseLvalConst(in.next());

    Program.resetCounter(varNum); //Force the next statement to have the correct line number
    PutDbStatement ss = new PutDbStatement(addrs, value);
    ss.toAssignmentStatements_NoChangeRef(assignments);
  }

  private void parseInputLine(int varNum, Scanner in, StatementBuffer assignments) {
    String inputName = in.next().substring("//".length());
    Type vartype_ = readType(in);
    Lvalue lval = new VarLvalue(new Variable(inputName, vartype_), false);
    //Function.getVars().add(lval, false);
    LvalExpression lvalExpr = new LvalExpression(lval); //Function.getVars().getVar(inputName);
    varByNumber.put(varNum, lvalExpr);
    Program.resetCounter(varNum); //Force the next statement to have the correct line number
    InputStatement is = new InputStatement(lvalExpr);
    is.toAssignmentStatements(assignments);
  }

  private Expression parseOperation(Scanner in) {
    Expression rhs = null;

    String operation = in.next();
    in.next();
    in.next();
    if (operation.equals("<")) {
      String left = in.next();
      in.next();
      String right = in.next();
      rhs = new BinaryOpExpression(new LessOperator(), parseLvalConst(left), parseLvalConst(right));
      in.next(); //]
    } else if (operation.equals("!=")) {
      String left = in.next();
      in.next();
      String right = in.next();
      rhs = new BinaryOpExpression(new NotEqualOperator(), parseLvalConst(left), parseLvalConst(right));
      in.next(); //]
    } else if (operation.equals("poly") || operation.equals("identity")) {
      PolynomialExpression pe = parsePoly(in, "]"); //Read until matching "]"

      rhs = simplifyPoly(pe);
    } else if (operation.equals("getdb")) {
      ArrayList<LvalExpression> address = new ArrayList();
      in.next(); //ADDR
      while(true){
        String token = in.next();
        if (token.equals("]")){
          break;
        }
        address.add((LvalExpression)parseLvalConst(token));
      }
      
      rhs = new GetDbExpression(new IntType(), address);
    } else {
      throw new RuntimeException("I don't know how to parse operator " + operation);
    }

    return rhs;
  }

  private Expression simplifyPoly(PolynomialExpression pe) {

    //It's tricky to apply gate compacting to gates that have already been compacted.
    //If this polynomial is a simple sum of two lval/consts, or a simple product of two lval/consts, recast it as such
    List<PolynomialTerm> pt = pe.getTerms();
    if (pt.size() == 2) { // a + b
      Expression left = pt.get(0).toLvalConst();
      Expression right = pt.get(1).toLvalConst();
      if (left != null && right != null) {
        return new BinaryOpExpression(new PlusOperator(), left, right);
      }
    }
    if (pt.size() == 1) {
      PolynomialTerm onlyTerm = pt.get(0);
      if (onlyTerm.getNumPolynomialFactors() == 0) { //can't handle polynomial factors in this way
        if (onlyTerm.getDegree() == 1) { // c * a
          return new BinaryOpExpression(new TimesOperator(), onlyTerm.constant, onlyTerm.getMonomerFactor(0));
        }
        if (onlyTerm.getDegree() == 2 && onlyTerm.constant.isOne()) { // a * b
          return new BinaryOpExpression(new TimesOperator(), onlyTerm.getMonomerFactor(0), onlyTerm.getMonomerFactor(1));
        }
      }
    }
    return pe;
  }

  private void emitSplitStatement(int varNum, String varname, Type bitwiseEncoding, LvalExpression toSplit, ReferenceProfile rp, StatementBuffer assignments) {
    int N = IntType.getBits((IntType)bitwiseEncoding);
    
    LvalExpression[] lhss = new LvalExpression[N];
    for(int i = 0; i < lhss.length; i++){
      Lvalue lval = new VarLvalue(new Variable(allButLast(varname,4)+":"+i+"$0", new BooleanType(), new BooleanType()), rp.isOutput);
      LvalExpression lve = new LvalExpression(lval);
      lhss[i] = lve; 
      varByNumber.put(varNum+i, lve);
      
      //Reference information from profile
      lve.setUBRefCount(rp.refCount); //Set an upper bound on the number of statements referencing this assignment
      lve.setKillPoint(rp.killPoint); //Set an upper bound on the number of statements referencing this assignment
      addKill(rp.killPoint, varNum);
    }

    Program.resetCounter(varNum); //Force the next statement to have the correct line number
    SplitStatement ss = new SplitStatement(bitwiseEncoding, toSplit, lhss);
    ss.toAssignmentStatements_NoChangeRef(assignments);
  }
  
  private String allButLast(String a, int i) {
    return a.substring(0, a.length()-i);
  }

  private void emitAssignment(int varNum, String varname, Type vartype_, ReferenceProfile rp, Expression rhs, StatementBuffer assignments) {
    //Perform optimizations without making the number any larger.
    Lvalue lval = new VarLvalue(new Variable(varname, vartype_, vartype_), rp.isOutput);
    //Function.getVars().add(lval, false);
    LvalExpression lvalExpr = new LvalExpression(lval); //Function.getVars().getVar(varname);
    varByNumber.put(varNum, lvalExpr);
    Program.resetCounter(varNum); //Force the next statement to have the correct line number

    //Reference information from profile
    lvalExpr.setUBRefCount(rp.refCount); //Set an upper bound on the number of statements referencing this assignment
    lvalExpr.setKillPoint(rp.killPoint); //Set an upper bound on the number of statements referencing this assignment
    addKill(rp.killPoint, varNum);

    AssignmentStatement as = new AssignmentStatement(lvalExpr, rhs);
    as.toAssignmentStatements_NoChangeRef(assignments);
  }

  private void addKill(int killPoint, int varNum) {
    List<Integer> atKill = killList.get(killPoint);
    if (atKill == null) {
      atKill = new ArrayList();
      killList.put(killPoint, atKill);
    }
    atKill.add(varNum);
  }

  /**
   * Represents a line in the .profile file
   */
  private static class ReferenceProfile {
    public int lineNumber;
    public boolean isUsed;
    public boolean isOutput; //real output - i.e. the final assignment to an output variable
    public int refCount;
    public int killPoint; //When to kill this variable
  }

  private ReferenceProfile lookaheadRef;
  private ReferenceProfile getReferenceProfileFor(int varNum) {
    if (lookaheadRef != null) {
      if (lookaheadRef.lineNumber > varNum) {
        return null;
      }
      if (lookaheadRef.lineNumber == varNum) {
        ReferenceProfile rmLookahead = lookaheadRef;
        lookaheadRef = null;
        return rmLookahead;
      }
    }

    String line = null;
    try {
      while ((line = profileReader.readLine()) != null) {
        String[] line_ = line.split("\\s+");
        ReferenceProfile test = new ReferenceProfile();
        test.lineNumber = Integer.parseInt(line_[0]);
        test.isUsed = line_[1].equals("1");
        test.isOutput = line_[2].equals("1");
        test.refCount = Integer.parseInt(line_[3]);
        test.killPoint = Integer.parseInt(line_[4]);
        if (test.lineNumber == varNum) {
          return test;
        }
        if (test.lineNumber > varNum) {
          lookaheadRef = test;
          return null;
        }
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return null;
  }

  private Type readType(Scanner in) {
    String vartype = in.next();
    if (vartype.equals("float")) {
      in.next();
      int na = nextInt(in);
      in.next();
      int nb = nextInt(in);
      return new RestrictedFloatType(na, nb);
    } else if (vartype.equals("int")) {
      in.next();
      int na = nextInt(in);
      return new RestrictedSignedIntType(na);
    } else if (vartype.equals("uint")) {
      in.next();
      int na = nextInt(in);
      return new RestrictedUnsignedIntType(na);
    } else {
      throw new RuntimeException("Not yet implemented");
    }
  }

  private int nextInt(Scanner in) {
    return Integer.parseInt(in.next());
  }

  private PolynomialExpression parsePoly(Scanner in, String breakOnMatching) {
    PolynomialExpression pe = new PolynomialExpression();
    PolynomialTerm term = new PolynomialTerm();
    boolean isEmpty = true;
    while(true) {
      String string = in.next();
      if (string.equals(breakOnMatching)) {
        break;
      }
      if (string.equals("+")) {
        //add term to pe
        pe.addMultiplesOfTerm(IntConstant.ONE, term);
        //Create a new term
        term = new PolynomialTerm();
      } else if (string.equals("*")) {
        //No new term.
      } else if (string.equals("(")) {
        PolynomialExpression subPoly = parsePoly(in, ")"); //Reads until matching ")"
        ConstExpression asConst = ConstExpression.toConstExpression(subPoly);
        LvalExpression asLval = LvalExpression.toLvalExpression(subPoly);
        if (asConst != null) {
          term.addFactor(asConst);
        } else if (asLval != null) {
          term.addFactor(asLval);
        } else {
          term.addFactor(subPoly);
        }
        isEmpty = false;
      } else if (string.equals("-")) {
        term.addFactor(IntConstant.NEG_ONE);
      } else {
        //add factor to term, or set the constant
        term.addFactor(parseLvalConst(string));
        isEmpty = false;
      }
    }
    if (isEmpty) {
      //Special: ( ) = 0.
    } else {
      //add term to pe
      pe.addMultiplesOfTerm(IntConstant.ONE, term);
    }
    return pe;
  }

  /**
   * Gets the value of the identified variable as an lval/const.
   */
  private Expression parseLvalConst(String term) {
    if (term.startsWith("C")) {
      return FloatConstant.valueOf(term.substring(1));
    }
    //Don't inline fractional constants when they could potentially end up in nested polynomials
    //(workaround until the "max denominator" detection works for nested polynomials)
    LvalExpression original = varByNumber.get(new Integer(term));
    if (original == null) {
      throw new RuntimeException("No entry for term "+term);
    }
    FloatConstant asConst = FloatConstant.toFloatConstant(original);
    if (asConst != null) {
      if (asConst.getDenominator().equals(BigInteger.ONE)) {
        return asConst;
      }
    }
    //Inline lvals
    AssignmentStatement as = AssignmentStatement.getAssignment(original);
    if (as != null){
      for(Expression q : as.getAllRHS()){
        if (q instanceof LvalExpression){
          return q;
        }
      }
    }
    return original;
  }
}
