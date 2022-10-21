package hageldave.jplotter.charts;

import hageldave.jplotter.util.Pair;

import java.util.ArrayList;


/**
 * The DataModel provides a way to store multiple {@link DataChunk} and perform operations on them.
 */
public abstract class DataModel {
    protected ArrayList<double[][]> dataChunks = new ArrayList<>();
    protected ArrayList<String> descriptionPerChunk = new ArrayList<>();

    /**
     * Returns the dataChunk which has the chunkIdx in the data model.
     *
     * @param chunkIdx of the desired dataChunk
     * @return the dataChunk
     */
    public double[][] getDataChunk(int chunkIdx){
        return dataChunks.get(chunkIdx);
    }

    /**
     * Returns the chunk description of the corresponding chunkIdx.
     *
     * @param chunkIdx specifies which chunk's description should be returned
     * @return chunk description
     */
    public String getChunkDescription(int chunkIdx) {
        return descriptionPerChunk.get(chunkIdx);
    }

    /**
     * Returns the size of the dataChunk which has the chunkIdx in the data model.
     *
     * @param chunkIdx of the desired dataChunk
     * @return size of the data chunk
     */
    public int chunkSize(int chunkIdx) {
        return getDataChunk(chunkIdx).length;
    }

    /**
     * Calculates the global index of idx if all values of the chunks
     * are viewed as one sequence.
     *
     * @param chunkIdx marks the starting point of the globalIndex before idx
     * @param idx will be added to the globalIndex after the sizes of all chunks before chunkIdx
     * @return the global index of idx
     */
    public int getGlobalIndex(int chunkIdx, int idx) {
        int globalIdx=0;
        for(int i=0; i<chunkIdx; i++) {
            globalIdx += chunkSize(i);
        }
        return globalIdx + idx;
    }

    /**
     * Locates the chunkIdx and pointIdx of a specified globalIdx.
     * As the data chunks are added sequentially to the data model,
     * the data implicitly has also a global index.
     *
     * @param globalIdx global index which should be mapped to chunkIdx and pointIdx
     * @return chunkIdx and pointIdx of the given globalIdx
     */
    public Pair<Integer, Integer> locateGlobalIndex(int globalIdx){
        int chunkIdx=0;
        while(globalIdx >= chunkSize(chunkIdx)) {
            globalIdx -= chunkSize(chunkIdx);
            chunkIdx++;
        }
        return Pair.of(chunkIdx, globalIdx);
    }

    /**
     * @return the number of values contained in the data model
     */
    public int numDataPoints() {
        int n = 0;
        for(int i=0; i<numChunks(); i++)
            n+=chunkSize(i);
        return n;
    }

    /**
     * @return number of data chunks added to the data model
     */
    public int numChunks() {
        return dataChunks.size();
    }
}
