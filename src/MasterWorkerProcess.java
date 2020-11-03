import mpi.MPI;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MasterWorkerProcess {
    public ArrayList<ArrayList<Double>> matrix1;
    public ArrayList<ArrayList<Double>> matrix2;
    public ArrayList<ArrayList<Double>> answerMatrix;
    public int chunkSize;
    public int totalNumberOfProcesses;

    /**
     *
     * @param matrix1Filename file name for matrix 1
     * @param matrix2Filename file name for matrix 2
     * @param numberOfSpawnedProcesses number of processes in the multi processes environment
     * @throws IOException
     */
    public MasterWorkerProcess(String matrix1Filename,String matrix2Filename,int numberOfSpawnedProcesses) throws IOException {
        this.matrix1 = getMatrix(matrix1Filename);
        this.matrix2 = getMatrixInColumnFormat(matrix2Filename); // this matrix is stored in its transposed format. This is done for improving performance.
        this.totalNumberOfProcesses = numberOfSpawnedProcesses;
        this.chunkSize = (int) Math.sqrt((this.matrix1.size()*this.matrix1.size())/this.totalNumberOfProcesses);
    }

    /**
     * performs the canons algorithm
     */
    public void doTheJob(){
        this.answerMatrix = initializeTheAnswerMatrix();
        arrangeTheInitialOrderOfBothMatrices();

        for (int iteration = 0;iteration<matrix1.size(); iteration++){ // do the procedire ndim number of times.
            sendCorrectDataToCorrectProcess();// send correct chunks of data to correct processes
            doTheSelfComputation();//master process will also do its own part of the computation
            modifyTheResultMatrix();//modify the result matrix by recieving the answer chunks from all the spawned processes
            doTheWrapAroundRotation();// do the wrap around
        }
//        this.answerMatrix.stream().forEach(System.out::println); // uncomment this to see the result. Also specify a file name in terminal command to get the output as the output in console might not fit in console.
    }

    /**
     * does the wrap around for both the matrices
     */
    private void doTheWrapAroundRotation() {
        for (int i = 0;i<this.matrix1.size();i++){

            List<Double> part1 = this.matrix1.get(i).subList(1,this.matrix1.get(i).size());
            double roundAboutElement = this.matrix1.get(i).get(0);
            part1.add(roundAboutElement);
            ArrayList<Double> replacement = new ArrayList<>();
            replacement.addAll(part1);

            this.matrix1.set(i,replacement);

            part1 = this.matrix2.get(i).subList(1,this.matrix2.get(i).size());
            roundAboutElement = this.matrix2.get(i).get(0);
            part1.add(roundAboutElement);
            replacement = new ArrayList<>();
            replacement.addAll(part1);

            this.matrix2.set(i,replacement);
        }
    }

    /**
     * Modifies the result of the answer matrix after recieving the data from all the worker processsses
     */
    private void modifyTheResultMatrix() {
        Object[] recvBuffer = new Object[1];
        for (int processIndex = 1;processIndex<this.totalNumberOfProcesses;processIndex++){
            MPI.COMM_WORLD.Recv(recvBuffer,0,1,MPI.OBJECT,processIndex,processIndex);
            ArrayList<ArrayList<Double>> recvdChunk = (ArrayList<ArrayList<Double>>)recvBuffer[0];
            int[] rowColumnOffset = getRowColumnOffset(processIndex);

            int rowIndexForTheRecievedChunk = 0;
            int colIndexForTheRecievedChunk = 0;
            for (int rowNumber = rowColumnOffset[0];rowNumber<rowColumnOffset[0]+this.chunkSize;rowNumber++){
                for (int colNumber = rowColumnOffset[1];colNumber<rowColumnOffset[1]+this.chunkSize;colNumber++){
                    this.answerMatrix.get(rowNumber).set(colNumber, this.answerMatrix.get(rowNumber).get(colNumber)+recvdChunk.get(rowIndexForTheRecievedChunk).get(colIndexForTheRecievedChunk));
                    colIndexForTheRecievedChunk+=1;
                }
                rowIndexForTheRecievedChunk+=1;
                colIndexForTheRecievedChunk=0;
            }
        }
    }

    /**
     * Does the computation for master process
     */
    private void doTheSelfComputation() {
        for (int i = 0;i<this.chunkSize;i++){
            for (int j =0;j<this.chunkSize;j++){
                this.answerMatrix.get(i).set(j, this.answerMatrix.get(i).get(j) + this.matrix1.get(i).get(j) * this.matrix2.get(j).get(i));
            }
        }
    }

    /**
     * Arranges the initial order of the matrices
     */
    private void arrangeTheInitialOrderOfBothMatrices() {
        for (int i=0;i<this.matrix1.size();i++){
            List<Double> part1 = this.matrix1.get(i).subList(0,i);
            List<Double> part2 = this.matrix1.get(i).subList(i,this.matrix1.size());
            part2.addAll(part1);
            ArrayList<Double> newArraylist = new ArrayList<>();
            newArraylist.addAll(part2);
            this.matrix1.set(i,newArraylist);
        }

        for (int i=0;i<this.matrix2.size();i++){
            List<Double> part1 = this.matrix2.get(i).subList(0,i);
            List<Double> part2 = this.matrix2.get(i).subList(i,this.matrix2.size());
            part2.addAll(part1);
            ArrayList<Double> newArraylist = new ArrayList<>();
            newArraylist.addAll(part2);
            this.matrix2.set(i,newArraylist);
        }
    }

    /**
     * initializing the answer matrix
     * @return answer matrix with all the zeros
     */
    private ArrayList<ArrayList<Double>> initializeTheAnswerMatrix() {
        ArrayList<ArrayList<Double>> result = new ArrayList<>();
        for (int i=0;i<this.matrix1.size();i++){
            ArrayList<Double> resultRow = new ArrayList<>();
            for (int j=0;j<this.matrix1.size();j++){
                resultRow.add(0.0);
            }
            result.add(resultRow);
        }
        return result;
    }

    /**
     * sending the correct data to correct process
     */
    private void sendCorrectDataToCorrectProcess() {
        for (int processIndex = 1;processIndex<this.totalNumberOfProcesses;processIndex++){
            Object[] buffer = getTheDataFOrTheProcess(processIndex);
            MPI.COMM_WORLD.Send(buffer,0,2,MPI.OBJECT,processIndex,processIndex);
        }
    }

    /**
     * gets the data for the process
     * @param processIndex index or rank of the processs
     * @return data to be passed on for the particular process
     */
    private Object[] getTheDataFOrTheProcess(int processIndex) {
        Object[] result = new Object[2];
        int[] xyOffset = getRowColumnOffset(processIndex);
        ArrayList<ArrayList<Double>> chunkFromMatrix1 = new ArrayList<>();
        for (int i = xyOffset[0];i<xyOffset[0]+this.chunkSize;i++){
            ArrayList<Double> row = new ArrayList<>();
            for (int j=xyOffset[1];j<xyOffset[1]+this.chunkSize;j++){
                try {
                    row.add(this.matrix1.get(i).get(j));
                }catch (Exception e) {
                    System.out.println("check whether n*n/p is a perfect square");
                    System.out.println(i + " " + j);
                }

            }
            chunkFromMatrix1.add(row);
        }

        ArrayList<ArrayList<Double>> chunkFromMatrix2 = new ArrayList<>();
        for (int i = xyOffset[0];i<xyOffset[0]+this.chunkSize;i++){
            ArrayList<Double> row = new ArrayList<>();
            for (int j=xyOffset[1];j<xyOffset[1]+this.chunkSize;j++){
                row.add(this.matrix2.get(j).get(i));
            }
            chunkFromMatrix2.add(row);
        }
        result[0] = chunkFromMatrix1;
        result[1] = chunkFromMatrix2;
        return result;
    }

    /**
     *
     * @param processIndex index or rank of the processs
     * @return gets the column and row offset for the chunk or block of the data i.e. indexes of the 1st element of the block.
     * This is used during sending the data to multiple processes and also during modifying the results. So that we know which block in the actual result matrix is to be modifiedd.
     */
    private int[] getRowColumnOffset(int processIndex) {
        int[] result = new int[2]; // it will contain row and column offset
        int numberOfProcessorsInARow = this.matrix1.size() / this.chunkSize;
        result[0] = (processIndex/numberOfProcessorsInARow)*this.chunkSize;
        result[1] = (processIndex%numberOfProcessorsInARow)*this.chunkSize;
        return result;
    }

    /**
     *
     * @param matrix1Filename file name for the matrix
     * @return matrix in arraylist format
     * @throws IOException
     */
    private ArrayList<ArrayList<Double>> getMatrix(String matrix1Filename) throws IOException {
        ArrayList<ArrayList<Double>> matrix = new ArrayList<>();
        List<String> allLines =  Files.readAllLines(Paths.get(matrix1Filename));
        for (String line : allLines){
            List<String> rowStringList = Arrays.asList( line.split(",") );
            ArrayList<Double> rowDoubleList = new ArrayList<>();
            for (String numString:rowStringList){
                rowDoubleList.add(Double.parseDouble(numString));
            }
            matrix.add(rowDoubleList);
        }
        return matrix;
    }

    /**
     *
     * @param matrix2Filename file name for the matrix
     * @return transpose of matrix in arraylist format
     * @throws IOException
     */
    private ArrayList<ArrayList<Double>> getMatrixInColumnFormat(String matrix2Filename) throws IOException {
        ArrayList<ArrayList<Double>> matrix = new ArrayList<>();
        ArrayList<List<String>> matrixInStringFormat = new ArrayList<>();
        List<String> allLines =  Files.readAllLines(Paths.get(matrix2Filename));
        for (String line : allLines){
            List<String> rowStringList = Arrays.asList( line.split(",") );
            matrixInStringFormat.add(rowStringList);
        }
        for (int j = 0;j<matrixInStringFormat.size();j++){
            ArrayList<Double> rowDoubleList = new ArrayList<>();
            for (int i = 0;i<matrixInStringFormat.size();i++){
                rowDoubleList.add(Double.parseDouble(matrixInStringFormat.get(i).get(j)));
            }
            matrix.add(rowDoubleList);
        }
        return matrix;
    }
}
