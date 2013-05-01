// IntType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

/**
 * A class representing the a signed integer of a certain number of bits.
 *
 * We require that n >= 1.
 *
 * So, it describes the set {-2^(n-1), -2^(n-1) + 1, ... 2^(n-1) - 1}.
 */
public class RestrictedSignedIntType extends IntType implements FiniteType {
  //~ Instance fields --------------------------------------------------------

  /*
   * Holds the length of this Int type, >= 1.
   */
  private final int length;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a RestrictedIntType object of a given length.
   */
  public RestrictedSignedIntType(int length) {
    if (length <= 0) {
      throw new RuntimeException("Signed integer of length "+length+" not defined");
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
    return "int bits " + length;
  }
}
