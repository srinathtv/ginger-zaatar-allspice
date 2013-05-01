#include <libv/libv.h>
#include "mpnvector.h"
#include "mpnops.h"
#include "utility.h"

using namespace std;

template<typename T>
MPNVector<T>::
MPNVector(size_t s) : len(s) {
  alloc_init_vec(&vec, len);
}

template<typename T>
MPNVector<T>::
MPNVector(const MPNVector<T>& other) {
  len = other.size();
  alloc_init_vec(&vec, len);

  for (size_t i = 0; i < len; i++)
    mpn_ops<T>::set(vec[i], other.raw_vec()[i]);
}

template<typename T>
MPNVector<T>::
~MPNVector() {
  clear_del_vec(vec, len);
}

template<typename T>
void MPNVector<T>::
set(int index, ConstT val) {
  mpn_ops<T>::set(vec[index], val);
}

template<typename T>
void MPNVector<T>::
copy(const MPNVector<T>& other) {
  copy(other, 0, other.size(), 0);
}

template<typename T>
void MPNVector<T>::
copy(const MPNVector<T>& other, size_t startOther, size_t len, size_t startThis) {
  len = min(len, min(size() - startThis, other.size() - startOther));
  for (unsigned i = 0; i < len; i++)
    mpn_ops<T>::set(vec[startThis + i], other.raw_vec()[startOther + i]);
}

template<typename T>
MPNVector<T>& MPNVector<T>::
operator*=(const T factor) {
  for (size_t i = 0; i < size(); i++)
    mpn_ops<T>::mul((*this)[i], (*this)[i], factor);
  return *this;
}

template<typename T> MPNVector<T>& MPNVector<T>::
operator=(const MPNVector<T>& other) {
  if (this != &other) {
    resize(other.len);
    for (size_t i = 0; i < size(); i++)
      mpn_ops<T>::set((*this)[i], other[i]);
  }
  return *this;
}

template<typename T>
void MPNVector<T>::
resize(size_t s) {
  clear_del_vec(vec, len);
  alloc_init_vec(&vec, s);
  len = s;
}

template<typename T>
void MPNVector<T>::
reserve(size_t s) {
  if (s <= len)
    return;

  size_t newSize = ceil(max<double>(len * 1.5, s));
  resize(newSize);
}

template class MPNVector<mpz_t>;
template class MPNVector<mpq_t>;

