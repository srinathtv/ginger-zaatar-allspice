#ifndef CODE_PEPPER_LIBV_INPUT_CREATOR_H_
#define CODE_PEPPER_LIBV_INPUT_CREATOR_H_

#include<common/utility.h>

class InputCreator {
  public:
    InputCreator();
    virtual void create_input(mpq_t* input_q, int num_inputs) = 0;
};

#endif  // CODE_PEPPER_LIBV_INPUT_CREATOR_H_
