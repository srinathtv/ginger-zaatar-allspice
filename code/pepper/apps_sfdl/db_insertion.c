#include <stdint.h>
struct In {int32_t address; };
struct Out {int32_t success; };

/*
  Increments the value at address input->address of the database
  using getdb and putdb (special language features)

  For now, the getdb and putdb functions assume a single database.
  Eventually, they will take a compile time resolvable handle to
  a database.
*/
void compute(struct In *input, struct Out *output){
  int32_t a;
  a = getdb(input->address);
  putdb(input->address, a + 1);
  output->success = 1;
}
