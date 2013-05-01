// Consts.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;


/**
 * The Consts class stores the constants defeined in the program.
 */
public class Consts {

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a ConstExpression representing the const of the specified type name, or null if there was no
   * such type defined for this type name.
   * @param name the name of the constant whose associated ConstExpression is to be returned.
   * @return ConstExpression representing the const, or null if there was no such constant defined.
   */
  public static ConstExpression fromName(String name) {
    return ((ConstExpression) (constsTable.get(name)));
  }

  /**
   * Associates the specified new constant name with the specified integer constant.
   * @param newConstName the new constant name with which the specified constant is to be associated.
   * @param constant the constant to be associated with the specified newConstName.
   * @throws IllegalArgumentException if the newConstName is already defined.
   */
  public static void defineName(ConstExpression value) throws IllegalArgumentException {
    declaredConsts.put(value.getName(), value);
    for(ConstExpression ce : value.getDerivedCvalues()) {
      String name = ce.getName();
      constsTable.put(name, ce);
    }
  }

  /**
   * Returns a list of all constExpressions which have been passed to Consts through defineName().
   *
   * Does not return all of the derived constants produced as a result of those additions.
   */
  public static Collection<ConstExpression> getDeclaredConsts() {
    return declaredConsts.values();
  }


  //~ Static fields

  /*
   * holds the constants defined in the program.
   */
  private static Map<String, ConstExpression> constsTable = new HashMap();
  private static Map<String, ConstExpression> declaredConsts = new TreeMap();
}
