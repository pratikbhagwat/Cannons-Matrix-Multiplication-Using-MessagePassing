import mpi.MPI;

import java.util.ArrayList;

public class NewWorkerProcess {
    /**
     * This method will just get the 2 input chunks from the master process and it will calculate its answer by multiplying each element from chunk 1 with its respective element from chunk 2.
     * @param numberOfTimesToListen this is the number of times the master will try to communicate with the worker. This is nothing but the NDIM as master will communicate with worker for Ndim times.
     */
    public void doTheJob(int numberOfTimesToListen){
        for (int iteration = 0;iteration<numberOfTimesToListen;iteration++){
            int processRank = MPI.COMM_WORLD.Rank();
            Object[] recvBuffer = new Object[2];

            MPI.COMM_WORLD.Recv(recvBuffer,0,2,MPI.OBJECT,0,processRank);
            ArrayList<ArrayList<Double>> matrixChunk1 = (ArrayList<ArrayList<Double>>)recvBuffer[0];
            ArrayList<ArrayList<Double>> matrixChunk2 = (ArrayList<ArrayList<Double>>)recvBuffer[1];
            ArrayList<ArrayList<Double>> answerChunk = new ArrayList<>();

            for (int i = 0;i<matrixChunk1.size();i++){
                ArrayList<Double> arrayListToBeAdded = new ArrayList<>();
                for (int j = 0;j<matrixChunk1.size();j++){
                    arrayListToBeAdded.add(0.0);
                }
                answerChunk.add(arrayListToBeAdded);
            }

            for (int i = 0;i<matrixChunk1.size();i++){

                for (int j =0;j<matrixChunk1.get(0).size();j++){
                    for (int k = 0 ; k< matrixChunk1.get(0).size();k++){
                        answerChunk.get(i).set(j,answerChunk.get(i).get(j)+matrixChunk1.get(i).get(k)*matrixChunk2.get(k).get(j));
                    }
                }
            }
            Object[] sendBuffer = new Object[1];
            sendBuffer[0] = answerChunk;
            MPI.COMM_WORLD.Send(sendBuffer,0,1,MPI.OBJECT,0,processRank);
        }
    }
}