
#include <cmtgkr/base/main_impl.h>
#include "../circuit/pws_circuit.h"

int main(int argc, char** argv)
{
  return run_gkr_netapp<PWSCircuitBuilder>(argc, argv, construct_circuit_batching);
}

