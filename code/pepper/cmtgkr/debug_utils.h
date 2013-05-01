
#ifndef CODE_PEPPER_CMTGKR_DEBUG_UTILS_H_
#define CODE_PEPPER_CMTGKR_DEBUG_UTILS_H_

template<typename T> bool
inRange(const T& val, const T& min, const T& max)
{
  return (val >= min) && (val < max);
}

#endif /* CODE_PEPPER_CMTGKR_DEBUG_UTILS_H_ */
