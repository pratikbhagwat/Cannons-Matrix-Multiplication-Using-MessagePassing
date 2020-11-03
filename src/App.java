import mpi.*;

import java.io.IOException;
import java.util.ArrayList;

public class App {
    public static void main(String[] args) throws IOException {
        MPI.Init(args);
        int processRank = MPI.COMM_WORLD.Rank();
        int numberOfIterations = Integer.parseInt(args[5]);
        ArrayList<Long> timeList = new ArrayList<>();
        for (int i = 0;i<numberOfIterations;i++){
            timeList.add( runTheMatrixMultiplicationAlgorithm(args) );
        }
        if (processRank==0){
            System.out.println( timeList.stream().mapToDouble(x->x).sum() / numberOfIterations);
            System.out.println(timeList);
        }
        MPI.Finalize();
    }

    public static long runTheMatrixMultiplicationAlgorithm(String[] args) throws IOException {

        int processRank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();
        if (processRank == 0){
            long startTime = System.currentTimeMillis();
            NewMasterWorkerProcess masterWorkerProcess = new NewMasterWorkerProcess(args[3],args[4],size);
            masterWorkerProcess.doTheJob();
            long endTime = System.currentTimeMillis();
            return endTime-startTime;
        }else {
            NewWorkerProcess workerProcess = new NewWorkerProcess();
            workerProcess.doTheJob((int)Math.sqrt(MPI.COMM_WORLD.Size()));
        }
        return 0;
    }
}








