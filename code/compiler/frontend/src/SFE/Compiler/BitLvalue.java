// BitLvalue.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;


/**
 * This class represents a bit of another Lvalue
 */
public class BitLvalue extends VarLvalue {
  //~ Instance fields --------------------------------------------------------

  // data member

  /*
   * Holds the bit in the Lvalue that this bit lvalue represents.
   */
  private int    bit;
  private Lvalue base;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new BitLvalue from a given VarLvalue - which cannot have any derived lvalues -
   * and the offset (in bits) in it.
   *
   * If the variable's type contains no more than one bit, that type is used. Otherwise,
   * the type of this lvalue is BooleanType (representing a binary bit)
   */
  public BitLvalue(Lvalue base, int bit) {
    super(new Variable(base.getName()+"$"+bit,chooseBitType(base)), base.isOutput());
    this.base         = base;
    this.bit          = bit;
  }
  private static Type chooseBitType(Lvalue base) {
    if (base.hasDerives()) {
      throw new RuntimeException("Unimplemented");
    }
    //The "bits" can be arbitrary types.
    if (base.size() == 1) {
      return base.getType();
    }
    //Default to binary bits
    return new BooleanType();
  }
}
