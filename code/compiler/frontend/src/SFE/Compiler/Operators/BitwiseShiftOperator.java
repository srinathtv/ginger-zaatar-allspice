package SFE.Compiler.Operators;

import SFE.Compiler.AnyType;
import SFE.Compiler.BitwiseOperator;
import SFE.Compiler.Expression;
import SFE.Compiler.IntConstant;
import SFE.Compiler.IntType;
import SFE.Compiler.RestrictedSignedIntType;
import SFE.Compiler.Type;

public class BitwiseShiftOperator extends BitwiseOperator{
  public static final int LEFT_SHIFT = 1;
  public static final int RIGHT_SHIFT = -1;
  private int direction;
  public BitwiseShiftOperator(int direction){
    this.direction = direction;
    if (!(direction == LEFT_SHIFT || direction == RIGHT_SHIFT)){
      throw new RuntimeException("I don't know how to shift in direction "+direction);
    }
  }
  public String toString(){
    return "bitwiseshift";
  }
  public int arity() {
    return 2;
  }
  public int priority() {
    throw new RuntimeException("Not implemented");
  }
  public Type getType(Object obj) {
    //No information until we've resolved the pointer during inlining.
    return new AnyType();
  }
  public boolean needsBitStringArgument(int i){
    if (i == 0){
      return true;
    }
    return false;
  }
  /* 
   * Left shifts are positive shifts, right shifts are negative.
   */
  public Expression getOutputBit(int i, Expression ... args) {
    int shiftVal = IntConstant.toIntConstant(args[1]).toInt();
    int N = IntType.getBits((IntType)getBitwiseEncoding());
    if (shiftVal < 0 || shiftVal >= N){
      throw new RuntimeException("Shifting amount exceeds size of datatype: "+shiftVal+" shift in direction "+direction);
    }
    //shiftval is nonnegative.
    int target = i - shiftVal*direction; //Go backwards.
    if (target < 0){
      return IntConstant.ZERO; //Logical left shift
    }
    if (target >= N){
      if (getBitwiseEncoding() instanceof RestrictedSignedIntType){
        return args[0].bitAt(N-1); //Arithmetic right shift.
      } else {
        return IntConstant.ZERO; //Logical right shift
      }
    }
    return args[0].bitAt(target);
  }
  public IntConstant resolve(Expression ... args) {
    //Without the power of creating additional variables, we must have constant arguments.
    /* The following code is not safe, because it doesn't handle signed arguments correctly.
    Expression left = args[0];
    Expression right = args[1];
  
    IntConstant lc = IntConstant.toIntConstant(left);
    IntConstant rc = IntConstant.toIntConstant(right);
    if (lc != null && rc != null){
      if (direction == LEFT_SHIFT){
        return new IntConstant(lc.value() << rc.value());
      } else if (direction == RIGHT_SHIFT){
        return new IntConstant(lc.value() >>> rc.value());
      }
    }
    */
    return null;
  }

}
