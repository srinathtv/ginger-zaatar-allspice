// BooleanType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;


/**
 * A class representing the float primitive type
 * that can be defined in the program.
 */
public class FloatType extends Type {
  //~ Methods ----------------------------------------------------------------

  /**
   * A float is a primitive type, so its size, in number of
   * primitive types, is 1.
   */
  public int size() {
    return 1;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "float";
  }

  public Type sumType(int N) {
    return null;
  }
}
