// Type.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.HashMap;
import java.util.Vector;


/**
 * Abstract class for representing types that can be defined
 * in the program. This class also functions as a type table for the
 * defined types in the programs.
 */
public abstract class Type {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns the size of this type object in bits.
   * @return an integer representing size of this type object in bits.
   */
  public abstract int size();

  /**
   * Returns the object representing the type of the specified type name, or null if there was no
   * such type defined for this type name.
   * @param typeName the type name whose associated type is to be returned.
   * @return the object representing the type of the specified type name, or null if there was no
   * such type defined for this type name.
   */
  public static Type fromName(String typeName) {
    return (Type) (typeTable.get(typeName));
  }

  /**
   * Associates the specified newTypeName with the specified newType.
   * @param newTypeName the new Type name with which the specified Type is to be associated.
   * @param newType the Type to be associated with the specified newTypeName.
   * @throws IllegalArgumentException if the newTypeName is already defined.
   */
  public static void defineName(String newTypeName, Type newType)
  throws IllegalArgumentException {
    if (typeTable.containsValue(newTypeName)) {
      throw new IllegalArgumentException();
    }

    typeTable.put(newTypeName, newType);
  }

  /**
   * Returns a vector of all the derived lvalue of this type.
   * type that shold return more then one lvalue (derived from the type itself)
   * in the vector must overide this method.
   * @param base the lavalue that call the this method (base.type == this)
   */
  public Vector getDerivedLvalues(Lvalue base) {
    Vector result = new Vector();
    result.add(base);

    return result;
  }

  /**
   * Returns a vector of all the derived cvalue of this type.
   *
   * @param base the lavalue that call the this method (base.type == this)
   */
  public Vector<ConstExpression> getDerivedCvalues(ConstExpression base) {
    Vector result = new Vector();
    result.add(base);

    return result;
  }

  /**
   * Returns false, types that derive other types.
   * For example, struct type should override this method.
   * @return false.
   */
  public boolean hasDerives() {
    return false;
  }

  //~ Static fields/initializers ---------------------------------------------

  // data members

  /*
   * holds the types defined in the program.
   */
  private static HashMap typeTable = new HashMap();
}
