package SFE.Compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Converts .circuit files to .spec files (constraints)
 *
 * The conversion is different depending on whether we are producing Zaatar or Ginger constraints.
 */
public class ConstraintWriter {
  private File circuitFile;
  public ConstraintWriter(String circuitFile) throws IOException {
    this.circuitFile = new File(circuitFile);
    if (!this.circuitFile.exists()) {
      throw new FileNotFoundException();
    }
  }

  private PrintWriter out;
  private TreeMap<Integer, String> inputVariables;
  private TreeSet<Integer> outputVariables;

  public void toConstraints(String constraintsFile) throws FileNotFoundException {
    out = new PrintWriter(constraintsFile);

    inputVariables = new TreeMap<Integer, String>();
    outputVariables = new TreeSet<Integer>();

    {
      Scanner in = new Scanner(new FileReader(circuitFile));
      out.println("START_INPUT");
      while (in.hasNext()) {
        String firstTerm = in.next();
        if (firstTerm.equals("shortcircuit") || firstTerm.equals("}")) {
          in.nextLine();
          continue;
        }
        int varNum = new Integer(firstTerm);
        if (in.next().equals("input")) {
          String comment = in.nextLine().split("//")[1];
          inputVariables.put(varNum, comment);
          String variableName = "I"+varNum;
          out.println(variableName + " //" + comment);
        } else {
          in.nextLine();
        }
      }
      out.println("END_INPUT");
      in.close();
    }

    out.println();

    {
      Scanner in = new Scanner(new FileReader(circuitFile));
      out.println("START_OUTPUT");
      TreeMap<Integer, String> outputVarLines = new TreeMap();
      while (in.hasNext()) {
        String firstTerm = in.next();
        if (firstTerm.equals("shortcircuit") || firstTerm.equals("}")) {
          in.nextLine();
          continue;
        }
        int varNum = new Integer(firstTerm);
        if (in.next().equals("output")) {
          outputVariables.add(varNum);
          String variableName = "O"+varNum;
          outputVarLines.put(varNum, variableName + " //" + in.nextLine().split("//")[1]);
        } else {
          in.nextLine();
        }
      }
      //Outputs sorted.
      for (String line : outputVarLines.values()) {
        out.println(line);
      }
      out.println("END_OUTPUT");
      in.close();
    }

    out.println();

    {
      Scanner in = new Scanner(new FileReader(circuitFile));
      out.println("START_VARIABLES");

      if (Optimizer.optimizationTarget == Optimizer.Target.GINGER) {
        //In GINGER, we need an additional variable for input binding
        for (Entry<Integer, String> i : inputVariables.entrySet()) {
          String variableName = "V"+i.getKey();
          out.println(variableName + " //" + i.getValue());
        }
      }
      while (in.hasNext()) {
        String firstTerm = in.next();
        if (firstTerm.equals("shortcircuit") || firstTerm.equals("}")) {
          in.nextLine();
          continue;
        }
        int varNum = new Integer(firstTerm);
        String type = in.next();
        if (type.equals("split")){
          //Add all variables
          ArrayList<Integer> vars = new ArrayList();
          while(true){
            String got = in.next();
            try {
              int val = Integer.parseInt(got);
              vars.add(val);
            } catch (Throwable e){
              break;
            }
          }
          String varName = " //" + (in.nextLine().split("//")[1].split("\\s+")[0]);
          for(int i = 0; i < vars.size(); i++){
            out.println("V"+vars.get(i)+allButLast(varName,4)+":"+i+"$0 uint bits 1");
          }
        } else if (type.equals("putdb")){
          //Do nothing.
          in.nextLine();
        } else {
          if (!inputVariables.containsKey(varNum) && !outputVariables.contains(varNum)) {
            String variableName = "V"+varNum;
            out.println(variableName + " //" + in.nextLine().split("//")[1]);
          } else {
            in.nextLine();
          }
        }
      }
      out.println("END_VARIABLES");
      in.close();
    }

    out.println();
    {
      Scanner in = new Scanner(new FileReader(circuitFile));
      out.println("START_CONSTRAINTS");
      if (Optimizer.optimizationTarget == Optimizer.Target.GINGER) {
        //In GINGER, we need an additional variable for input binding
        for (Integer i : inputVariables.keySet()) {
          out.println("I" + i + " - V" + i); // Vi is the unknown
        }
      }
      while (in.hasNextLine()) {
        String line = in.nextLine();
        String[] lines = line.split("\\s+");
        if (lines[0].equals("}")) {
          out.println("}");
        } else if (lines[0].equals("shortcircuit")) {
          lines[3] = getConstraintVarName(new Integer(lines[3])); // Add variable prefix to the variable
          lines[5] = lines[5].substring(1); // remove C prefix from
          // the constant
          for (String k : lines) {
            out.print(k + " ");
          }
          out.println();
        } else {
          compileConstraintsLine(line);
        }
      }
      out.println("END_CONSTRAINTS");
      in.close();
    }

    out.println();

    out.close();
  }

  private String allButLast(String a, int i) {
    return a.substring(0,a.length()-i);
  }

  private int nextInt(Scanner in) {
    return Integer.parseInt(in.next());
  }

  private void compileConstraintsLine(String line) {
    Scanner in = new Scanner(line);

    in = new Scanner(line);
    int varNum = nextInt(in);
    String variableName = getConstraintVarName(varNum);
    String type = in.next();
    if (type.equals("output")) {
      type = in.next();
    }
    //Sequential
    if (type.equals("gate")) {
      String gateType = in.next();
      in.next();
      in.next();
      if (line.contains("!=") || line.contains("<")) {
        compileNonPolyConstraint(variableName, in);
      } else if (gateType.equals("getdb")){
        compileGetDbConstraint(variableName, in);
      } else {
        compilePolyConstraint(variableName, in);
      }
    } else if (type.equals("split")){
      compileSplitConstraint(in);
    } else if (type.equals("putdb")){
      compilePutDbConstraint(in);
    } else if (type.equals("input")){
      //Nothing to do.
    } else {
      throw new RuntimeException("I don't know how to convert circuit line to constraints: "+line);
    }
  }

  private void compileGetDbConstraint(String variableName, Scanner in) {
    in.next(); //ADDR
    StringBuffer addrs = new StringBuffer();
    while(true){
      String got = in.next();
      if (got.equals("]")){
        break;
      }
      addrs.append(getConstraintVarName(new Integer(got)));
    }
    in.nextLine();
    
    out.println("GETDB ADDR "+addrs+" Y "+variableName);     
  }

  private void compilePutDbConstraint(Scanner in) {
    in.next(); //"inputs"
    in.next(); //"["
    in.next(); //"ADDR"
    StringBuffer addrs = new StringBuffer();
    while(true){
      String got = in.next();
      if (got.equals("X")){
        break;
      }
      addrs.append(getConstraintVarName(new Integer(got)));
    }
    String value = getConstraintVarName(in.nextInt());
    in.nextLine();
    
    out.println("PUTDB ADDR "+addrs+" X "+value); 
  }

  private void compileSplitConstraint(Scanner in) {
    ArrayList<Integer> outs = new ArrayList();
    while(true){
      String got = in.next();
      try {
        int y = Integer.parseInt(got);
        outs.add(y);
      } catch (Throwable e){
        break;
      }
    }
    while(true){
      String got = in.next();
      if (got.equals("[")){
        break;
      }
    }
    String toSplit = getConstraintVarName(in.nextInt()); //Cannot be a constant.
    String type = in.nextLine().split("//",2)[1].split("\\s+",2)[1];
    
    out.println("SIL "+type+" X "+toSplit+" Y0 "+"V"+outs.get(0));
  }

  /**
   * Mapping as follows:
   * 	- Are an integer -> the appropriatve var name (see getConstraintVarName)
   *  - starts with C -> just the substring that follows.
   * 	- Otherwise, return as is
   */
  private String getConstraintTerm(String term) {
    int termVarNum;
    try {
      termVarNum = new Integer(term);
    } catch (Throwable e) {
      termVarNum = -1;
    }
    // convert to variable name
    if (termVarNum < 0) {
      if (term.startsWith("C")) {
        term = term.substring(1);
      }
      // Other cases include operators
    } else {
      term = getConstraintVarName(termVarNum);
    }
    return term;
  }

  /**
   * In ZAATAR, we allow references to input and output variables everywhere.
   *
   * In GINGER, input variables have to be bound to actual variables at the start of the computation
   * (we can't do multiplication with constants in GINGER)
   */
  private String getConstraintVarName(int varNum) {
    switch(Optimizer.optimizationTarget) {
    case ZAATAR:
      if (outputVariables.contains(varNum)) {
        return "O"+varNum;
      }
      if (inputVariables.containsKey(varNum)) {
        return "I"+varNum;
      }
      return "V"+varNum;
    case GINGER:
      return (outputVariables.contains(varNum) ? "O" : "V") + varNum;
    }
    throw new RuntimeException();
  }


  private void compilePolyConstraint(String variableName, Scanner in) {
    FloatConstant multiplier = FloatConstant.valueOf(in.next().substring(1));
    if (!multiplier.getNumerator().equals(BigInteger.ONE)) {
      throw new RuntimeException("Assertion error "+multiplier);
    }
    String multiplierDenom = "";
    if (!multiplier.isOne()) {
      multiplierDenom = multiplier.getDenominator() + " * ";
    }
    in.next(); //*
    in.next();

    switch(Optimizer.optimizationTarget) {
    case GINGER:
      compilePolyConstraint_(in);
      out.print(" - " +multiplierDenom + variableName);
      break;
    case ZAATAR:
      //pA
      in.next();
      out.print("( ");
      compilePolyConstraint_(in);
      out.print(" )");
      //* pB
      in.next(); //*
      in.next(); //(
      out.print(" * ( ");
      compilePolyConstraint_(in);
      out.print(" )");
      //+ pC
      in.next(); //*
      in.next(); //(
      out.print(" + ( ");
      compilePolyConstraint_(in);
      //Add in output binding term
      out.print(" - " +multiplierDenom + variableName);
      out.print(" )");
      in.next(); //")"
      break;
    }

    if (!in.next().equals("]")) {
      throw new RuntimeException("Assertion error");
    }

    out.println();
  }

  /**
   * Reads off a polynomial expression, ending when the parenthesis nesting depth is negative one.
   */
  private void compilePolyConstraint_(Scanner in) {
    int nestingDepth = 0;
    boolean hadPreviousTerm = false;
    while(true) {
      String term = in.next();
      if (term.equals("(")) {
        nestingDepth++;
      }
      if (term.equals(")")) {
        nestingDepth --;
        if (nestingDepth < 0) {
          break;
        }
      }
      if (hadPreviousTerm) {
        out.print(" ");
      }
      out.print(getConstraintTerm(term));
      hadPreviousTerm = true;
    }
  }

  private void compileNonPolyConstraint(String variableName, Scanner in) {
    do {
      String term = in.next();
      if (term.equals("]")) {
        break;
      }
      term = getConstraintTerm(term);
      out.print(term+" ");
    } while (true);
    out.println("- " + variableName);
  }
}
