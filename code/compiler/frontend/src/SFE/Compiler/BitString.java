package SFE.Compiler;

import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class BitString extends Expression implements OutputWriter{
  private Expression[] bits;
  private Type bitwiseEncoding;
  public BitString(Type bitwiseEncoding, Expression[] bits){
    this.bits = bits;
    this.bitwiseEncoding= bitwiseEncoding;
  }
  public Type getBitwiseEncoding() {
    return bitwiseEncoding;
  }
  public Expression bitAt(int i){
    /*
    if (i >= bits.length){
      return IntConstant.ZERO; //This allows easy conversion between longs, ints, etc.
    }
    */
    return bits[i];
  }
  public int size() {
    return bits.length;
  }
  public BitString changeReference(VariableLUT unique) {
    for(int i = 0; i < bits.length; i++){
      bits[i] = bits[i].changeReference(unique);
    }
    return this;
  }
  public Collection<LvalExpression> getUnrefLvalInputs() {
    return new ArrayList(); //Not helpful, but the polynomial extracted from this bitstring should be handling this.
  }
  public Type getType() {
    return new AnyType(); //Again, not helpful, but the polynomial extracted from this bitstring does the heavy lifting.
  }
  public static BitString toBitString(Type bitwiseEncoding, StatementBuffer sb, Expression c) {
    if (bitwiseEncoding.hasDerives()){
      //Recursively encode more complicated types.
      throw new RuntimeException("toBitString doesn't handle parent types yet.");
    }
    
    int N = IntType.getBits((IntType)bitwiseEncoding);
    
    Expression[] bs = new Expression[N];
    boolean signed = bitwiseEncoding instanceof RestrictedSignedIntType;
    IntConstant cv = IntConstant.toIntConstant(c);
    if (cv != null){
      BigInteger val = cv.value();
      for(int i = 0; i < N; i++, val = val.shiftRight(1)){
        if (i == N-1 && signed){
          bs[i] = BooleanConstant.valueOf(cv.signum() < 0);
        } else {
          bs[i] = BooleanConstant.valueOf(val.testBit(0));
        }
      }
      return new BitString(bitwiseEncoding, bs);
    }

    //TODO: sync this file with zaatar git.
    
    //Look for bitstrings in assignments
    AssignmentStatement as = AssignmentStatement.getAssignment(c);
    if (as != null){
      for(Expression r : as.getAllRHS()){
        if (r instanceof LvalExpression){
          LvalExpression asLval = (LvalExpression)r;
          BitString oldString = asLval.getBitString();
          if (oldString != null){
            return translate(oldString, bitwiseEncoding);
          }
        }
      }
    }
    
    LvalExpression asLval = LvalExpression.toLvalExpression(c);
    if (asLval != null){
      {
        //Look for a bitstring in the lval itself
        BitString oldString = asLval.getBitString();
        if (oldString != null){
          return translate(oldString, bitwiseEncoding);
        }
      }
      
      //Make a fresh bit string.
      
      //Expand the Lval to its bit string (dependent on its current type)
      int bitsInLval = IntType.getBits((IntType)asLval.getType()); //Ensure that we have enough space to hold the split bits
      LvalExpression[] split = new LvalExpression[bitsInLval];
      for(int i = 0; i < bitsInLval; i++){
        split[i] = Function.getVars().addVar(asLval.getName()+":"+i+"$0", new BooleanType(), false, false);
      }
      SplitStatement s = new SplitStatement(asLval.getType(), asLval, split);
      s.toAssignmentStatements(sb);
      BitString newBs = new BitString(asLval.getType(), split);
      
      //Tell the lval about its bitString
      if (asLval.getBitString() == null){
        asLval.setBitString(newBs);
      }
      
      //Translate this to the bitstring format we want.
      return translate(newBs, bitwiseEncoding);
    } else {
      throw new RuntimeException("I don't know how to make a Bitstring from "+c);
    }
  }

  
  private static BitString translate(BitString bs, Type bitwiseEncoding) {
    int lengthOld = IntType.getBits((IntType)bs.bitwiseEncoding);
    int lengthNew = IntType.getBits((IntType)bitwiseEncoding);
    boolean signedOld = bs.bitwiseEncoding instanceof RestrictedSignedIntType;
    boolean signedNew = bitwiseEncoding instanceof RestrictedSignedIntType;
    
    boolean wideningConversion = signedOld && signedNew && lengthNew > lengthOld;
    
    Expression[] exprs = new Expression[lengthNew];
    Arrays.fill(exprs, BooleanConstant.FALSE);
    for(int i = 0; i < lengthOld && i < lengthNew; i++){
      if (wideningConversion && i == lengthOld - 1){
        break; //Don't copy over the sign bit just yet.
      }
      exprs[i] = bs.bitAt(i);
    }
    //Move the sign bit to the right location, if we are doing a signed-to-signed upsample.
    if (wideningConversion){
      exprs[exprs.length-1] = bs.bitAt(lengthOld-1);
    }
    
    return new BitString(bitwiseEncoding, exprs);
  }
  /**
   * Interprets bits (in two's complement form, if signed) to a polynomial representation.
  public PolynomialExpression bitsAsInteger() {
    int N = IntType.getBits((IntType)bitwiseEncoding);
    boolean signed = bitwiseEncoding instanceof RestrictedSignedIntType;
    
    int pot = 1;
    if (N > 32){
      throw new RuntimeException("Converting bits to long int not yet supported.");
    }
    final Type targetType;
    if (signed){
      targetType = new RestrictedSignedIntType(N);
    } else {
      targetType = new RestrictedUnsignedIntType(N);
    }
    PolynomialExpression pe = new PolynomialExpression(){
      public Type getType(){
        return targetType;
      }
    };
    for(int i = 0; i < N; i++, pot <<= 1){
      int signedPot = pot;
      if (i == N-1 && signed){
        signedPot = -signedPot;
      }
      PolynomialTerm pt = new PolynomialTerm();
      pt.addFactor(bits[i]);
      pe.addMultiplesOfTerm(IntConstant.valueOf(signedPot), pt);
    }
    return pe;
  }
   */
  public void addReference() {
    for(Expression q : bits){
      if (q instanceof LvalExpression){
        ((LvalExpression) q).addReference();
      }
    }
  }
  public void toCircuit(Object obj, PrintWriter circuit) {
    for(Expression q : bits){
      ((OutputWriter)q).toCircuit(null, circuit);
      circuit.print(" ");
    }
  }
  /**
   * Outputs assignment statements assigning to the appropriate elements of target the bits of this string.
   * 
   * Takes care of things like assigning a string of 64 bits to a struct store {uint32_t a, int32_t b}. 
   */
  public void toAssignments(LvalExpression target, StatementBuffer assignments) {
    Type targetType = target.getDeclaredType();
    if (targetType.hasDerives()){
      //TODO
      throw new RuntimeException("BitString->struct not yet implemented");
    }
    if (bits.length != IntType.getBits((IntType)targetType)){
      throw new RuntimeException("Cannot assign a bit string of length "+bits.length+" to a variable of type "+target);
    }
    BitString newString = new BitString(targetType, bits); //Take signedness from the target type. 
    //PolynomialExpression poly = PolynomialExpression.toPolynomialExpression(newString);
    new AssignmentStatement(target.bitAt(0), newString).toAssignmentStatements(assignments);
  }
  /**
   * Outputs the number of bits needed to represent type t.
   * 
   * For int types, equivalent to IntType.getBits(t);
   */
  public static int getBits(Type t){
    if (t.hasDerives()){
      throw new RuntimeException("Representing structs in bit strings not yet implemented");
    }
    
    return IntType.getBits((IntType)t);
  }
}
