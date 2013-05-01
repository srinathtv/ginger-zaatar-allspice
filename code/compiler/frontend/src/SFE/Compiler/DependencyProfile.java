package SFE.Compiler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

public class DependencyProfile {
  public DependencyProfile() {
  }
  /**
   * Read in a .circuit.tac file, and produce a .profile.tac file which indicates
   * which lines are used, and how often.
   *
   * Output format:
   *
   * a b c d e
   *
   * a - line number
   * b - 1 if used (output lines count as used), 0 otherwise.
   * If b is 0, c, d, and e are ignored.
   * c - is output line
   * d - reference count
   * e - 'kill point', line number of last referencer
   *
   * Note that lines where b = 0 are not usually output.
   *
   *
   * The lines are written out in reverse line number order.
   * Use .tac to switch the lines in the .profile.tac file produced.
   */
  public void makeProfile(File circuitBackwards, File outFile) {
    Set<String> outputVariables = new HashSet();

    Map<Integer, int[]> refDatas = new HashMap();

    //ReadBackwardsTextFile rbtf = new ReadBackwardsTextFile(circuit, 1 << 20); //Use about 1 megabyte of memory

    try {
      //Construct the BufferedReader object
      BufferedReader bufferedReader = new BufferedReader(new FileReader(circuitBackwards));

      PrintWriter out = new PrintWriter(outFile);
      String line = null;
      while ((line = bufferedReader.readLine()) != null) {
        //Process the data, here we just print it out
        parseAssignment(line, out, outputVariables, refDatas);
      }
      bufferedReader.close();
      out.close();
    } catch (Throwable e) {
      throw new RuntimeException(e);
    }

    /*

    try {
    	in2 = new Scanner(circuitBackwards);
    } catch (FileNotFoundException e1) {
    	throw new RuntimeException(e1);
    }
    int lastLineNumber = Integer.MAX_VALUE;
    while(in2.hasNextLine()){

    	String line = in2.nextLine();

    		parseAssignment(lineNumber, output, in, out, refDatas);
    	}
    }
    	*/
  }

  private static Pattern wspace = Pattern.compile("\\s+");
  public static boolean printUpdates = false;
  private static int lastPrint = 0;

  private void parseAssignment(String line, PrintWriter out, Set<String> outputVariables, Map<Integer, int[]> refDatas) {
    String[] in = wspace.split(line);
    int in_p = 0;
    String firstTerm = in[in_p++];

    int lineNumber = Integer.parseInt(firstTerm);
    if (printUpdates) {
      if (Math.abs(lineNumber - lastPrint) > 1000000) {
        lastPrint = lineNumber;
        System.out.println("Profiling has "+ lineNumber + " lines to go");
      }
    }

    String type = in[in_p++];
    if (type.equals("input")) {
      out.printf("%d 1 0 -1 -1\n", lineNumber); //This line is used, it is not output, reference count and kill points don't matter for input lines
    } else if (type.equals("split")){
      //Read output lines
      int[] ref = new int[2];
      boolean used = false;
      while(true) {
        String next = in[in_p++];
        if (next.equals("[")) {
          break;
        }
        //Integer tokens are output lines
        try {
          int i = Integer.parseInt(next);
          if (refDatas.containsKey(i)) {
            int[] subRef = refDatas.remove(i);
            ref[0] += subRef[0];
            ref[1] = Math.max(ref[1], subRef[1]);
            used = true;
          }
        } catch (Throwable e) {
        }
      }
      if (used){
        while(true) {
          String next = in[in_p++];
          if (next.equals("]")) {
            break;
          }
          //Integer tokens are variable references
          try {
            int i = Integer.parseInt(next);

            int[] oldCount = refDatas.get(i);
            if (oldCount == null) {
              //Last reference to this variable found. Set i's kill point to this line.
              oldCount = new int[] {1, lineNumber};
            } else {
              oldCount[0]++;
            }
            refDatas.put(i, oldCount);
          } catch (Throwable e) {
          }
        }
        //Emit statement
        //Print out this line
        out.printf("%d 1 %d %d %d\n",
                   lineNumber,
                   0, //split statement is never output.
                   ref[0],
                   ref[1]
                  ); //This line is used, it may be output output ref count and kill point
      }
    } else {
      boolean output = false;
      if (type.equals("output")) {
        //We only want the final assignment to output variables.
        String varName = wspace.split(line.substring(line.indexOf("//")+2),2)[0];
        if (!outputVariables.contains(varName)) {
          outputVariables.add(varName);
          output = true;
          //Ensure that the variable is referenced.
          if (!refDatas.containsKey(lineNumber)) {
            refDatas.put(lineNumber, new int[] {1, Integer.MAX_VALUE}); //Reference from infinity
          }
        }
      }
      
      if (type.equals("putdb")){
        //Ensure that the variable is referenced.
        if (refDatas.containsKey(lineNumber)) {
          throw new RuntimeException("Assertion error");
        }
        refDatas.put(lineNumber, new int[] {1, Integer.MAX_VALUE}); //Reference from infinity
      }
      
      //This is an assignment statement. Is it used?
      if (refDatas.containsKey(lineNumber)) {
        int[] refs = refDatas.remove(lineNumber);
        //Print out this line
        out.printf("%d 1 %d %d %d\n",
                   lineNumber,
                   output?1:0,
                   refs[0],
                   refs[1]
                  ); //This line is used, it may be output output ref count and kill point

        //Mark all dependencies of the RHS of this assignment statement as used
        while(true) {
          String next = in[in_p++];
          if (next.equals("[")) {
            break;
          }
        }
        while(true) {
          String next = in[in_p++];
          if (next.equals("]")) {
            break;
          }
          //Integer tokens are variable references
          try {
            int i = Integer.parseInt(next);

            int[] oldCount = refDatas.get(i);
            if (oldCount == null) {
              //Last reference to this variable found. Set i's kill point to this line.
              oldCount = new int[] {1, lineNumber};
            } else {
              oldCount[0]++;
            }
            refDatas.put(i, oldCount);
          } catch (Throwable e) {
          }
        }
      } else {
        //Don't output unused lines
      }
    }
  }
}
