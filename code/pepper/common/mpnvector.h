#ifndef CODE_PEPPER_COMMON_MPNVECTOR_H_  
#define CODE_PEPPER_COMMON_MPNVECTOR_H_  

#include <algorithm>
#include <gmp.h>

template <typename T>
class MPNVector
{
  protected:
    size_t len;
    T* vec;

  public:
    typedef const T ConstT;

    MPNVector(size_t s = 0);
    MPNVector(const MPNVector<T>& other);
    ~MPNVector();

    inline size_t size()    const { return len; }
    inline bool   empty()   const { return size() == 0; }
    inline T*     raw_vec() const { return vec; }

    inline const T& operator[] (unsigned index) const { return vec[index]; }
    inline T&       operator[] (unsigned index)       { return vec[index]; }

    inline const T& front() const { return vec[0]; }
    inline T&       front()       { return vec[0]; }

    inline const T& back() const { return vec[size() - 1]; }
    inline T&       back()       { return vec[size() - 1]; }

    void set(int index, ConstT val);
    void copy(const MPNVector<T>& other); 
    void copy(const MPNVector<T>& other, size_t startOther, size_t endOther, size_t startThis = 0); 

    MPNVector<T>& operator= (const MPNVector<T>& other);
    MPNVector<T>& operator*=(ConstT factor);

    void resize(size_t s);    // Resize vector.
    void reserve(size_t s);   // Resize vector.
};

typedef MPNVector<mpz_t> MPZVector;
typedef MPNVector<mpq_t> MPQVector;

#endif  // CODE_PEPPER_COMMON_MPNVECTOR_H_

