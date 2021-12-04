package hageldave.jplotter.charts;

/**
 * Used for picking registry
 */
public class DataChunk {
    protected int chunkID;
    protected int pointID;

    public DataChunk(int chunkID, int pointID) {
        this.chunkID = chunkID;
        this.pointID = pointID;
    }

    public int getChunkID() {
        return this.chunkID;
    }

    public int getPointID() {
        return this.pointID;
    }

    public void setChunkID(int chunkID) {
        this.chunkID = chunkID;
    }

    public void setPointID(int pointID) {
        this.pointID = pointID;
    }
}

