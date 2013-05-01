// Variable.java.
// Copyright (C) 2004 Naom Nisan, Ziv Balshai, Amir Levy.
// See full copyright license terms in file ../GPL.txt

package SFE.Compiler;


/**
 * A type representing a variable in the program.
 * A variable is composed of three things:
 *  1) A name (multiple variables can have the same name, corresponding to different assignments to the same variable name)
 *  2) A type (this is the type of the value of the variable, as far as the compiler can determine)
 *  3) A declared type (this is the type of the variable which occurs in the variable declaration)
 */
public class Variable {
  //~ Instance fields --------------------------------------------------------

  // data members

  /*
   * The name of the variable.
   */
  private String name;

  /*
   * The type of the variable, as far as the compiler can determine
   */
  private Type type;

  /*
   * The declared type of the variable, which appears in the variable declaration of this variable
   */
  private Type declaredType;

  //~ Constructors -----------------------------------------------------------

  /**
   * Constructs a new variable object of a given name and declared type.
   * The type of the variable is assumed to be equal to the declared type.
   */
  public Variable(String name, Type declaredType) {
    this(name, declaredType, declaredType);
  }
  /**
   * Constructs a new variable object of a given name, type, and declared type.
   */
  public Variable(String name, Type type, Type declaredType) {
    String sameName = VariableLUT.STRING_CACHE.get(name);
    if (sameName != null) {
      name = sameName;
    } else {
      VariableLUT.STRING_CACHE.put(name, name);
      //System.out.println(VariableLUT.STRING_CACHE.size()+" "+name);
    }

    this.name     = name;
    this.type     = type;
    this.declaredType = declaredType;
  }

  //~ Methods ----------------------------------------------------------------

  /**
   * Returns the Type of this varable.
   * @return the Type of this varable.
   */
  public Type getType() {
    return type;
  }

  /**
   * Returns the declared type of this variable
   */
  public Type getDeclaredType() {
    return declaredType;
  }

  /**
   * Sets the Type of this varable.
   */
  public void setType(Type t) {
    this.type = t;
  }

  /**
   * Returns a string representing the name of this variable.
   * @return a string representing the name of this variable.
   */
  public String getName() {
    return name;
  }

  /**
   * Returns an int representing the size of this variable object in bits.
   * @return an int representing the size of this variable object in bits.
   */
  public int size() {
    return type.size();
  }

  /**
   * Returns a string representation of the object.
   * @return a string representation of the object.
   */
  public String toString() {
    return type.toString() + " " + name;
  }
}
