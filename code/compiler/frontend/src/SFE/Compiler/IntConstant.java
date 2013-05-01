
// IntConstant.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.io.PrintWriter;
import java.math.BigInteger;


/**
 * The IntConstant class represents integer consts expressions that can
 * appear in the program.
 */
public class IntConstant extends ConstExpression implements Comparable<IntConstant>, OutputWriter {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * Holds the integer constant of this IntConstant
   */
  private final BigInteger intConst;


  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new IntConstant from a given integer const
   * @param intConst the given integer constant
   */
  public IntConstant(BigInteger intConst) {
    super(null);
    this.intConst = intConst;
  }
  public IntConstant(int i){
    this(BigInteger.valueOf(i));
  }

  public static IntConstant toIntConstant(Expression c) {
    if (c instanceof IntConstant) {
      return (IntConstant)c;
    }
    FloatConstant fc = FloatConstant.toFloatConstant(c);
    if (fc != null) {
      BigInteger divisible = fc.getNumerator().mod(fc.getDenominator());
      if (divisible.signum() == 0) {
        return IntConstant.valueOf(fc.getNumerator().divide(fc.getDenominator()));
      }
    }
    return null;
  }

  //~ Methods ----------------------------------------------------------------


  public Type getType() {
    if (equals(ONE) || equals(ZERO)) {
      return new BooleanType();
    }
    switch(intConst.signum()){
    case 0:
      throw new AssertionError();
    case 1:
      return new RestrictedUnsignedIntType(intConst.bitLength());
    case -1:
      //-1 -> 2
      if (equals(NEG_ONE)){
        return new RestrictedSignedIntType(1);
      }
      //-2 -> 2
      //-3 -> 3
      //-4 -> 3
      //-5 -> 4
      //-6 -> 4
      //-7 -> 4
      //-8 -> 4
      return new RestrictedSignedIntType(intConst.bitLength()+1); //this is negative, so the minimal rep + 1 for sign bit
    default: 
      throw new AssertionError();
    }
  }
  public boolean equals(Object o){
    return ((IntConstant)o).intConst.equals(intConst);
  }

  /**
   * Since float is our primitive type, this returns 1.
   */
  public int size() {
    return 1;
  }



  /**
   * Returns the value stored in this IntConstant
   * @return the value stored in this IntConstant
   */
  public BigInteger value() {
    return intConst;
  }

  /**
   * Returns this (because an int is a primitive value)
   */
  public ConstExpression bitAt(int i) {
    return this;
  }

  //~ Static fields/initializers ---------------------------------------------

  /**
   * Writes this constant into the circuit file.
   * @param circuit the output circuit.
   */
  public void toCircuit(Object obj, PrintWriter circuit) {
    circuit.print("C"+intConst);
  }
  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return "C"+intConst.toString(10);
  }

  public int compareTo(IntConstant other) {
    return intConst.compareTo(other.intConst);
  }

  //Public static fields
  public static final IntConstant ONE = new IntConstant(1);
  public static final IntConstant NEG_ONE = new IntConstant(-1);
  public static final IntConstant ZERO = new IntConstant(0);


  /**
   * Non-type safe valueOf. Argument must either be an Integer or a BigInteger.
   */
  public static IntConstant valueOf(Number i_) {
    BigInteger i;
    if (i_ instanceof Integer){
      i = BigInteger.valueOf((Integer)i_); 
    } else {
      i = (BigInteger)i_;
    }
    for(IntConstant q : new IntConstant[]{ONE, NEG_ONE, ZERO}){
      if (i.equals(q.intConst)){
        return q;
      }
    }    
    return new IntConstant(i);
  }

  public boolean isPOT() {
    return intConst.signum() > 0 && (intConst.bitCount() == 1);
  }
  public int signum() {
    return intConst.signum();
  }
  public int toInt() {
    BigInteger MIN_INT = BigInteger.valueOf(Integer.MIN_VALUE);
    BigInteger MAX_INT = BigInteger.valueOf(Integer.MAX_VALUE);
    if (intConst.compareTo(MIN_INT) < 0 || intConst.compareTo(MAX_INT) > 0){
      throw new RuntimeException("Cannot coerce safely to int: "+intConst);
    }
    return intConst.intValue();
  }
}
