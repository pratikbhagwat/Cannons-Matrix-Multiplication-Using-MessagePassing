import mpi.*;

import java.util.Arrays;

public class App2 {
    public static void main(String[] args) {
        MPI.Init(args);
        double[] resultMatrix = new double[]{0,0,0,0,0,0,0,0,0,0,0,0,0,0,0,0};
        multiplyMatricesUsingCannons(new double[]{1,2,3,7,4,2,1,3,6,3,2,1,1,7,3,6},new double[]{1,2,3,7,4,2,1,3,6,3,2,1,1,7,3,6},resultMatrix);

        Arrays.stream(resultMatrix).forEach( System.out::println );
        MPI.Finalize();
    }

    public static void multiplyMatricesUsingCannons(double[] matrix1,double[] matrix2,double[] resultMatrix){
        int numberOfProcesses = MPI.COMM_WORLD.Size();
        int commWorldRank = MPI.COMM_WORLD.Rank();
        int[] cartesianTopology = new int[2];
        cartesianTopology[0] = (int)Math.sqrt(numberOfProcesses);
        cartesianTopology[1] = (int)Math.sqrt(numberOfProcesses);

        boolean[] wrapAroundPeriods = new boolean[]{true,true};
        Cartcomm cartComm = MPI.COMM_WORLD.Create_cart(cartesianTopology,wrapAroundPeriods,true);

        int cartCommRank = cartComm.Rank();
        int[] cartCoOrdinates = cartComm.Coords(cartCommRank);
        ShiftParms horizontalShiftparams = cartComm.Shift(0,-1);
        ShiftParms verticalShiftParams = cartComm.Shift(1,-1);

        int chunkDimension = matrix1.length / numberOfProcesses;

        ShiftParms initialShifts = cartComm.Shift(0,-cartCoOrdinates[0]);
        cartComm.Sendrecv_replace(matrix1,0,chunkDimension*chunkDimension,MPI.DOUBLE,initialShifts.rank_dest,1,initialShifts.rank_source,1);
        initialShifts = cartComm.Shift(0,-cartCoOrdinates[1]);
        cartComm.Sendrecv_replace(matrix2,0,chunkDimension*chunkDimension,MPI.DOUBLE,initialShifts.rank_dest,1,initialShifts.rank_source,1);

        for (int i=0;i<cartesianTopology[0];i++){
            multiplyMatrixSerially(chunkDimension,matrix1,matrix2,resultMatrix);
            cartComm.Sendrecv_replace(matrix1,0,chunkDimension*chunkDimension,MPI.DOUBLE,horizontalShiftparams.rank_dest,1,horizontalShiftparams.rank_source,1);
            cartComm.Sendrecv_replace(matrix2,0,chunkDimension*chunkDimension,MPI.DOUBLE,verticalShiftParams.rank_dest,1,verticalShiftParams.rank_source,1);

        }
    }

    private static void multiplyMatrixSerially(int chunkDimension, double[] matrix1, double[] matrix2, double[] resultMatrix) {
        for (int i=0;i<chunkDimension;i++){
            for (int j=0;j<chunkDimension;j++){
                for (int k=0;k<chunkDimension;k++){
                    resultMatrix[i*chunkDimension+j] += matrix1[i*chunkDimension+k]*matrix2[k*chunkDimension+j];
                }
            }
        }
    }

}
