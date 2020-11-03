To run the program you need to specify the vm arguments as -jar <path for the directory which has starter jar>/starter.jar -np <number of processes>
You need to set up the MPI environment by setting up the environment variable whose value will be the root of the MPI package.
You need to give 3 program arguments as fillows:
1) the matrix 1 file name
2) the matrix 2 file name
3) the dimension of matrix
4) the number of iterations you want to do for testing time

Assumptions made for the canon's algorithm implementation

(number of elements in the matrix / number of available processes) must be a square of an integer.

the matrix input must be comma seperated text file
eg:
1, 2, 3, 4
5, 6, 7, 8
9, 10, 11, 12
13, 14, 15, 16


Running ins


Hope you have fun seeing the Conon's Algorithm in action :D.