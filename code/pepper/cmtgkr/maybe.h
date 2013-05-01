#ifndef CODE_PEPPER_CMTGKR_MAYBE_H_
#define CODE_PEPPER_CMTGKR_MAYBE_H_

template<class T>
struct Maybe
{
  std::pair<bool, T> option;

  Maybe() { invalidate(); }
  Maybe(const T val) { validate(val); }

  void invalidate()
  {
    if (!isValid())
    {
      option.first = false;
      option.second = T();
    }
  }

  T& validate() {
    option.first = true;
    return option.second;
  }

  T& validate(const T val)
  {
    validate() = val;
    return option.second;
  }

  const T& value() const { return option.second; }
  bool isValid() const { return option.first; }
};


#endif

