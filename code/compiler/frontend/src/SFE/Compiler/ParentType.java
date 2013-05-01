// ParentType.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;

import java.util.Vector;


/**
 * this class represents types that contain other types such as structs and
 * arrays
 */
public abstract class ParentType extends Type {
  //~ Methods ----------------------------------------------------------------

  /**
   * Returns a vector of all the derived lvalue of this type.
   * type that shold return more then one lvalue (derived from the type itself)
   * in the vector must overide this method.
   * @param base the lavalue that call the this method (base.type == this)
   */
  public abstract Vector getDerivedLvalues(Lvalue base);

  /**
   * Returns true, if these types has drived vars.
   */
  public boolean hasDerives() {
    return true;
  }

  /**
   * Returns a string representing the real name of the given object.
   * @param baseName the name of a child
   * @param i the offset in bits of this object.
   * @return a string representation of the real name of bit i.
   */
  public abstract String getNameAt(String baseName, int i);

  /**
   * @param i the offset in bits of this object.
   * @return the type of the child of this type at position i.
   */
  public abstract Type getDerivedTypeAt(int i);
}
