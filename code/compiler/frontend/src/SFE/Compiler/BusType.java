// BusType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;


/**
 * A class representing a value of the primitive bus of an arbitrary length.
 * An object of type bus contains a single field whose type is int and represents the
 * its length. An bus type of one bit is actualy a boolean variable.
 */
public class BusType extends Type {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Holds the bus's length
   */
  int length;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new bus from a given length.
   * @param length the length of the bus.
   */
  public BusType(int length) {
    this.length = length;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns the size of the bus in bits
   * @return the size of the bus in bits
   */
  public int size() {
    return length;
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "Bus<" + length + ">";
  }

  /**
   * Returns a string representation of the object for the format file.
   * @return a string representation of the object for the format file.
   */
  public String toFormat() {
    return "integer";
  }
}
