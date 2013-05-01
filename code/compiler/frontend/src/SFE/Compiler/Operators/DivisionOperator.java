package SFE.Compiler.Operators;

import java.math.BigInteger;

import SFE.Compiler.AnyType;
import SFE.Compiler.BinaryOpExpression;
import SFE.Compiler.BitwiseOperator;
import SFE.Compiler.BlockStatement;
import SFE.Compiler.Expression;
import SFE.Compiler.Function;
import SFE.Compiler.IntConstant;
import SFE.Compiler.IntType;
import SFE.Compiler.LvalExpression;
import SFE.Compiler.RestrictedSignedIntType;
import SFE.Compiler.RestrictedUnsignedIntType;
import SFE.Compiler.StatementBuffer;
import SFE.Compiler.Type;


public class DivisionOperator extends BitwiseOperator{
  public static final int REMAINDER = 0,
                    QUOTIENT = REMAINDER+1;
  
  private int mode;
  public DivisionOperator(int mode){
    this.mode = mode;
  }
  
  public String toString(){
    switch(mode){
    case REMAINDER: return "%";
    case QUOTIENT: return "/";
    default: throw new RuntimeException("Assertion error");
    }
  }
  public int arity() {
    return 2;
  }
  public int priority() {
    throw new RuntimeException("Not implemented");
  }
  public Type getType(Object obj) {
    return new AnyType();
  }
  
  private static class MagicNumber {
    public MagicNumber(BigInteger m, int shift) {
      this.M = m;
      this.shift = shift;
    }
    public BigInteger M;
    public int shift;
    public String toString(){
      return "multiply by "+M+" and then use p ="+shift;
    }
  }

  private MagicNumber getMagicNumber(IntType n_t, BigInteger d) {
    if (d.compareTo(BigInteger.valueOf(2)) < 0){
      throw new RuntimeException("I don't know how to compute the magic number for divisor "+d);
    }
    
    BigInteger nMax = IntType.getMaxInt(n_t);
    BigInteger nMin = IntType.getMinInt(n_t);
    
    for(int p = 0; true; p++){
      BigInteger twoP = BigInteger.ONE.shiftLeft(p);
      //Round twoP up to the next multiple of d, Md, then Md / d is an even integer.
      BigInteger Md = twoP.add(d).subtract(twoP.mod(d));
      //Furthermore, (Md - twoP)*n/(twoP) must be in [0, 1) for n nonnegative.
      if (Md.subtract(twoP).multiply(nMax).compareTo(twoP) >= 0){
        continue;
      }
      //Also, ((Md - twoP)*n + 1)/(twoP) must be in (-1, 0] for n negative.
      if (Md.subtract(twoP).multiply(nMin).add(BigInteger.ONE).negate().compareTo(twoP) >= 0){
        continue;
      }

      //Let M = Md / d.
      BigInteger M = Md.divide(d);
      //Let n >= 0.
      //Then Mn/twoP = Mdn/(dtwoP) = (Md - twoP)*n/(dtwoP) + n/d must be in n/d + [0, 1/d).
      //Hence, Mn >> p = n/d for n >= 0.
      
      //Let n < 0.
      //Then (Mn + twoP) >> p = Mn >> p + 1 = ceil(Mn/twoP + 1/(dtwoP))
      //and Mn/twoP + 1/(dtwoP) = 
      //(Mdn + 1)/(dtwoP) = ((Md - twoP)*n + twoP*n + 1)/(dtwoP)
      //= ceil((-1/d, 0] + n/d) = n/d. Done!
      
      //Note: Can use either (Mn + twoP) >> p or Mn >> p when n = 0.
      //But, when n = 1, ceil((-1/d, 0] + 1/d) = 1, whereas floor(1/d + [0,1/d)) = 0.
      //So we have a split.

      return new MagicNumber(M, p);
    }
  }
  
  public static void main(String[] args){
    BigInteger d = BigInteger.valueOf(7);
     
    DivisionOperator divisionOperator = new DivisionOperator(QUOTIENT);
    IntType testType = new RestrictedSignedIntType(6);
    MagicNumber mc = divisionOperator.getMagicNumber(testType, d);
    BigInteger twoP = BigInteger.ONE.shiftLeft(mc.shift);
    System.out.println(mc);
     
    //Division circuit:
    BigInteger maxInt = IntType.getMaxInt(testType);
    BigInteger minInt = IntType.getMinInt(testType);
    for (BigInteger n = minInt; n.compareTo(maxInt) <= 0; n = n.add(BigInteger.ONE)) {
       BigInteger q;
       if (n.signum()<0){
         
         //Floored division
         //-1 - (M(-n-1)) >> p)
         //q = BigInteger.valueOf(-1).subtract(mc.M.multiply(n.negate().subtract(BigInteger.ONE)).shiftRight(mc.shift));
         
         //Division
         //(Mn + twoP)>>p
         q = mc.M.multiply(n).add(twoP).shiftRight(mc.shift);
       } else {
         //Mn >> p
         q = mc.M.multiply(n).shiftRight(mc.shift);
       }
       System.out.println(n+" / "+d+" = "+q);       
     }
  }
  
  public Expression inlineOp(StatementBuffer sb, Expression... args) {
    final IntConstant divisor = IntConstant.toIntConstant(args[1]);
    if (divisor == null){
      throw new RuntimeException("Division by non-constant "+args[1]+" not supported.");
    }
    if (mode == REMAINDER && divisor.signum() < 0){
      throw new RuntimeException("Evaluating remainder by negative divisor not currently supported.");
    }
    if (divisor.equals(IntConstant.ZERO)){
      throw new RuntimeException("Division by zero.");
    }

    Expression arg0 = LvalExpression.toLvalExpression(args[0]);
    if (arg0 == null){
      arg0 = IntConstant.toIntConstant(args[0]);
    }
    if (arg0 == null){
      throw new RuntimeException("Assertion error");
    }
    final boolean isSignedResult = arg0.getType() instanceof RestrictedSignedIntType;
    
    if (divisor.equals(IntConstant.ONE)){
      switch(mode){
      case REMAINDER:
        return IntConstant.ZERO;
      case QUOTIENT:
        return args[0];
      default:
         throw new RuntimeException("Assertion error");
      }
    }
    
    MagicNumber magicNumber = getMagicNumber((IntType)arg0.getType(), divisor.value());
    IntConstant twoP = IntConstant.valueOf(BigInteger.ONE.shiftLeft(magicNumber.shift));
    
    //Can we resolve without adding additional variables?
    IntConstant M = IntConstant.valueOf(magicNumber.M);
    
    BinaryOpExpression product = new BinaryOpExpression(new TimesOperator(), arg0, M);
    //if (arg0.getType() instanceof RestrictedSignedIntType){
      //if arg0 < 0, add twoP to product. Of course this isn't necessary if arg0 is unsigned.
      product = new BinaryOpExpression(new PlusOperator(), product,
          new BinaryOpExpression(new TimesOperator(),
              new BinaryOpExpression(new LessOperator(), arg0, IntConstant.ZERO), 
              twoP));
    //}
    
    BitwiseShiftOperator shiftRight = new BitwiseShiftOperator(BitwiseShiftOperator.RIGHT_SHIFT);
    shiftRight.setBitwiseEncoding(product.getType()); //HMMM investigate further.
    
    Expression quotient = new BinaryOpExpression(shiftRight, product, IntConstant.valueOf(magicNumber.shift));
    
    Expression result;
    switch(mode){
    case REMAINDER:
      //r = a - intdiv(a/d)*d = a - q*d;
      result = new BinaryOpExpression(new MinusOperator(), arg0, 
          new BinaryOpExpression(new TimesOperator(), quotient, divisor));
      break;
    case QUOTIENT:
      result = quotient;
      break;
    default:
      throw new RuntimeException("Assertion error");
    }
    
    BlockStatement block = new BlockStatement();
    Expression toRet = result.evaluateExpression("__divisionop:"+uid, "a", 1, block);
    uid++; //We don't have enough naming information to differentiate the divisions, so do a cludgy uid hack. XXX
    block.toAssignmentStatements(sb);
    
    toRet = toRet.changeReference(Function.getVars());
    if (toRet instanceof LvalExpression){
      LvalExpression retLval = (LvalExpression)toRet;

      //Give the type system some help on the remainder case
      if (mode == REMAINDER){
        int N = IntType.getBits((IntType)divisor.getType());
        if (isSignedResult){
          retLval.getLvalue().setType(new RestrictedSignedIntType(N+1));
        } else {
          retLval.getLvalue().setType(new RestrictedUnsignedIntType(N));
        }
      }
    }
    
    return toRet;
  }
  private static int uid = 0;

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

  public Expression getOutputBit(int i, Expression... args) {
    throw new RuntimeException("Not implemented");
  }
}
