package SFE.Compiler;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConstraintCleaner {

  //All whitespace should be removed before running.
  private static Pattern renameZaatar = Pattern.compile("\\A\\Q()*()+(\\E(V\\d+)-(O\\d+)\\)\\z");
  private static Pattern variableRef = Pattern.compile("\\b(V\\d+)\\b");
  /*
   * toContraints sometimes prints out constraints of the form
   *
   * ( ) * ( ) + ( V<num> - O<num> ) (zaatar)
   *
   * but we can remove these.
   *
   * Note: this method should only be called in Zaatar mode.
   */
  public static void cleanupConstraints(String uncleanConstraints, String constraintsFile) {
    try {
      //First pass: Detect such constraints, and form a replacement map (size of map bounded by # designated output variables)
      HashMap<String, String> replacements = new HashMap();
      Scanner in = new Scanner(new File(uncleanConstraints));

      //Scan until the constraints start
      while(in.hasNextLine()) {
        String line = in.nextLine();

        if (line.equals("START_CONSTRAINTS")) {
          break;
        }
      }
      //Look for constraints of the right form
      while(in.hasNextLine()) {
        String line = in.nextLine();
        if (line.equals("END_CONSTRAINTS")) {
          break;
        }

        //Remove whitespace
        line = line.replaceAll("\\s","");

        Matcher matcher = renameZaatar.matcher(line);
        if(matcher.find()) {
          replacements.put(matcher.group(1), matcher.group(2));
        }
      }
      in.close();

      //Now print out the clean constraints
      in = new Scanner(new File(uncleanConstraints));
      PrintWriter out = new PrintWriter(new File(constraintsFile));

      //Everything is the same until the VARIABLES block
      while(in.hasNextLine()) {
        String line = in.nextLine();
        out.println(line);
        if (line.equals("START_VARIABLES")) {
          break;
        }
      }
      while(in.hasNextLine()) {
        String line = in.nextLine();
        boolean print = true;
        if (!line.isEmpty()) {
          String varName = line.split("\\s+",2)[0];
          if (replacements.containsKey(varName)) {
            print = false;
          }
        }
        if (print) {
          out.println(line);
        }
        if (line.equals("END_VARIABLES")) {
          break;
        }
      }
      //Scan to the contraints block
      while(in.hasNextLine()) {
        String line = in.nextLine();
        out.println(line);
        if (line.equals("START_CONSTRAINTS")) {
          break;
        }
      }
      //Process constraints
      while(in.hasNextLine()) {
        String line = in.nextLine();
        //is this constraint one we are removing?
        if (renameZaatar.matcher(line.replaceAll("\\s", "")).matches()) {
          continue;
        }

        if (line.equals("END_CONSTRAINTS")) {
          out.println(line);
          break;
        }
        //Make all replacements.
        StringBuffer sb = new StringBuffer();
        Matcher matcher = variableRef.matcher(line);
        while(matcher.find()) {
          String word = matcher.group(1);
          String rep = replacements.get(word);
          if (rep != null) {
            word = rep;
          }
          matcher.appendReplacement(sb, word);
        }
        matcher.appendTail(sb);
        out.println(sb);
      }
      //Write out remaining lines
      while(in.hasNextLine()) {
        String line = in.nextLine();
        out.println(line);
      }
      in.close();
      out.close();
    } catch (FileNotFoundException e) {
      throw new RuntimeException(e);
    }
  }
}
