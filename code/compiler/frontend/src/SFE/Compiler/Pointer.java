package SFE.Compiler;

import java.util.ArrayList;
import java.util.Collection;

import SFE.Compiler.Operators.ArrayAccessOperator;

import ccomp.CMemoryMap;

/**
 * An abstract implementation of a pointer. 
 * 
 * A pointer holds a reference to an array of possible targets, incrementing a pointer transforms it 
 * so that it points to one of its other targets. 
 */
public class Pointer extends Expression implements Inlineable, OutputsToPolynomial{
  private int index;
  private int addressOfList;
  private int addressOfPointer;
  private LvalExpression[] list;
  private LvalExpression array;
  private PointerType myType;
  public Pointer(Type pointsToType, LvalExpression[] list, int addressOfList, int addressOfPointer){
    myType = new PointerType(pointsToType);
    this.list = list;
    this.addressOfList = addressOfList;
    this.addressOfPointer = addressOfPointer;
    this.index = addressOfPointer - addressOfList;
    if (index < 0 || index > maxIndex()){
      throw new ArrayIndexOutOfBoundsException("Invalid pointer address.");
    }
  }
  private static int getMemoryLocation(LvalExpression lval) {
    LvalExpression c = lval;
    //Unfurl all multidimensional arrays.
    while(c.getType() instanceof ArrayType){
      c = new ArrayAccessOperator().resolve(c, IntConstant.ZERO);
    }
    return c.getOutputLine() + CMemoryMap.STACK;
  }
  private Pointer(LvalExpression array, int index){
    this.list = null;
    this.array = array;
    this.addressOfList = getMemoryLocation(array);
    this.addressOfPointer = getMemoryLocation(new ArrayAccessOperator().resolve(array, IntConstant.valueOf(index)));
    this.index = index;
    ArrayType at = (ArrayType)array.getType();
    myType = new PointerType(at.getBaseType());
    if (index < 0 || index > maxIndex()){
      throw new ArrayIndexOutOfBoundsException("Invalid pointer address.");
    }
  }
  public Pointer increment(IntConstant ic){
    return increment(ic.toInt());
  }
  public Pointer increment(int i){
    if (index + i < 0 || index + i > maxIndex()){
      throw new ArrayIndexOutOfBoundsException("Invalid pointer indirection.");
    }
    if (list != null){
      return new Pointer(myType.getPointedToType(), list, addressOfList, addressOfPointer + i);
    } else {
      return new Pointer(array, index + i);
    }
  }
  private int maxIndex() {
    if (array == null){
      return list.length;
    }
    return ((ArrayType)array.getType()).getLength();
  }
  public LvalExpression access(){
    if (list != null){
      return list[index];
    }
    return new ArrayAccessOperator().resolve(array, IntConstant.valueOf(index));
  }
  public int size() {
    return 1; //A pointer can be held in a single field element 
  }
  /**
   * Duplicate the pointer across all of its bits.
   */
  public Expression bitAt(int i){
    return this;
  }
  public Expression changeReference(VariableLUT unique) {
    if (list != null){
      for(int i = 0; i < list.length; i++){
        list[i] = list[i].changeReference(unique);
      }
    }
    if (array != null){
      array = array.changeReference(unique);
    }
    return this;
  }
  public Collection<LvalExpression> getUnrefLvalInputs() {
    //A pointer stands alone - the data it points to is not strictly speaking referenced by the pointer.
    return new ArrayList();
  }
  public Type getType() {
    return myType;
  }
  public IntConstant value() {
    return IntConstant.valueOf(addressOfPointer);
  }
  public static Pointer toPointer(Expression c) {
    if (c instanceof Pointer){
      return (Pointer)c;
    }
    if (c instanceof UnaryOpExpression) {
      UnaryOpExpression uo = ((UnaryOpExpression)c);
      return toPointer(uo.getOperator().resolve(uo.getMiddle()));
    }
    if (c instanceof BinaryOpExpression) {
      BinaryOpExpression bo = ((BinaryOpExpression)c);
      return toPointer(bo.getOperator().resolve(bo.getLeft(), bo.getRight()));
    }
    if (c instanceof LvalExpression){
      LvalExpression lvc = (LvalExpression)c;
      if (lvc.getType() instanceof ArrayType){
        return new Pointer(lvc, 0);
      }
    }
    AssignmentStatement as = AssignmentStatement.getAssignment(c);
    if (as != null){
      for(Expression q : as.getAllRHS()){
        Pointer got = toPointer(q);
        if (got != null){
          return got;
        }
      }
    }
    return null;
  }
  public Expression inline(Object obj, StatementBuffer history) {
    return this;
  }
}
