#include <stdint.h>
#define DIVISOR 0x49276
struct In { int32_t a; };
struct Out { int32_t amodp; };

void compute(struct In *input, struct Out *output){
  output->amodp = input->a % DIVISOR;
}

int main(int argc, char **argv){
  struct In input;
  struct Out output;
  input.a = -51;
  compute(&input, &output);
  printf("%d\n", output.amodp);
}
