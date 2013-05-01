// IntType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

/**
 * A class representing an unsigned integer of a certain number of bits.
 *
 * We require that n >= 1.
 *
 * So, it describes the set {0, 1, ... 2^n - 1}.
 */
public class RestrictedUnsignedIntType extends IntType implements FiniteType {
  //~ Instance fields --------------------------------------------------------

  /*
   * Holds the length of this Int type, >= 1.
   */
  private final int length;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a RestrictedIntType object of a given length.
   * 
   * When length == 0, it is interpreted as length = 1.
   */
  public RestrictedUnsignedIntType(int length) {
    if (length < 0){
      throw new RuntimeException("I don't know how to construct an unsigned int type with "+length+" bits.");
    }
    if (length == 0) {
      length = 1;
    }

    this.length = length;
  }

  //~ Methods ----------------------------------------------------------------

  public int getLength() {
    return length;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "uint bits " + length;
  }
}
