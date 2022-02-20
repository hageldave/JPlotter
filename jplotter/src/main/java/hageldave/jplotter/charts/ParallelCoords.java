package hageldave.jplotter.charts;

import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.color.DefaultColorMap;
import hageldave.jplotter.renderables.Legend;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.ParallelCoordsRenderer;
import hageldave.jplotter.util.Pair;
import hageldave.jplotter.util.PickingRegistry;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeSet;

public class ParallelCoords {
    protected JPlotterCanvas canvas;
    protected ParallelCoordsRenderer coordsys;
    protected CompleteRenderer content;
    final protected PickingRegistry<Object> pickingRegistry = new PickingRegistry<>();
    final protected TreeSet<Integer> freedPickIds = new TreeSet<>();

    final protected ParallelCoordsDataModel dataModel = new ParallelCoordsDataModel();
    final protected ArrayList<Lines> linesPerDataChunk = new ArrayList<>();
    final protected ArrayList<Integer> legendElementPickIds = new ArrayList<>();
    final protected Legend legend = new Legend();
    protected ParallelCoordsVisualMapping visualMapping = new ParallelCoordsVisualMapping(){};

    public ParallelCoords(final boolean useOpenGL) {
        this(useOpenGL ? new BlankCanvas() : new BlankCanvasFallback(), "X", "Y");
    }

    public ParallelCoords(final JPlotterCanvas canvas, final String xLabel, final String yLabel) {
        this.canvas = canvas;
        this.canvas.asComponent().setPreferredSize(new Dimension(400, 400));
        this.canvas.asComponent().setBackground(Color.WHITE);
        this.coordsys = new ParallelCoordsRenderer();
        this.content = new CompleteRenderer();
        this.coordsys.setContent(content);
        this.canvas.setRenderer(coordsys);

        this.dataModel.addListener(new ParallelCoordsDataModel.ParallelCoordsDataModelListener() {
            @Override
            public void featureAdded(ParallelCoordsRenderer.Feature feature) {
                onFeatureAdded(feature);
            }

            @Override
            public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription, int xIdx) {
                onDataAdded(chunkIdx, chunkData, chunkDescription, xIdx);
            }

            @Override
            public void dataChanged(int chunkIdx, double[][] chunkData) {
                // TODO: To implement
            }
        });
    }

    protected synchronized int registerInPickingRegistry(Object obj) {
        int id = freedPickIds.isEmpty() ? pickingRegistry.getNewID() : freedPickIds.pollFirst();
        pickingRegistry.register(obj, id);
        return id;
    }

    protected synchronized Object deregisterFromPickingRegistry(int id) {
        Object old = pickingRegistry.lookup(id);
        pickingRegistry.register(null, id);
        freedPickIds.add(id);
        return old;
    }

    public ParallelCoordsVisualMapping getVisualMapping() {
        return visualMapping;
    }

    public void setVisualMapping(ParallelCoordsVisualMapping visualMapping) {
        this.visualMapping = visualMapping;
        for(Lines p: linesPerDataChunk)
            p.setDirty();
        this.canvas.scheduleRepaint();
    }

    public ParallelCoordsDataModel getDataModel() {
        return dataModel;
    }

    public JPlotterCanvas getCanvas() {
        return canvas;
    }

    public ParallelCoordsRenderer getCoordsys() {
        return coordsys;
    }

    public CompleteRenderer getContent() {
        return getContent();
    }

    protected synchronized void onFeatureAdded(ParallelCoordsRenderer.Feature feature) {
        this.coordsys.addFeature(feature);

        //
        this.canvas.scheduleRepaint();
    }

    protected synchronized void onDataAdded(int chunkIdx, double[][] dataChunk, String chunkDescription, int featureCount) {
        Lines lines = new Lines();
        linesPerDataChunk.add(lines);
        content.addItemToRender(lines);
        for(int i=0; i<dataChunk.length; i++) {
            int i_=i;
            double[] datapoint = dataChunk[i];
            for (int j = 0; j < datapoint.length-1; j++) {
                Lines.SegmentDetails segmentDetails =
                        lines.addSegment(new Point2D.Double(
                                (double) j / (featureCount-1), normalizeValue(datapoint[j], getDataModel().getFeature(j))),
                                new Point2D.Double((double) (j + 1) / (featureCount-1), normalizeValue(datapoint[j+1], getDataModel().getFeature(j+1))));
                segmentDetails.setColor(() -> getVisualMapping().getColorForChunk(chunkIdx));
                segmentDetails.setPickColor(registerInPickingRegistry(new int[]{chunkIdx, i}));
            }
        }
        // create a picking ID for use in legend for this data chunk
        this.legendElementPickIds.add(registerInPickingRegistry(chunkIdx));
        visualMapping.createLegendElementForChunk(legend, chunkIdx, chunkDescription, legendElementPickIds.get(chunkIdx));
        this.canvas.scheduleRepaint();
    }

    protected static double normalizeValue(double value, ParallelCoordsRenderer.Feature feature) {
        return (value - feature.min)/(feature.max - feature.min);
    }

    public static class ParallelCoordsDataModel {
        protected ArrayList<double[][]> dataChunks = new ArrayList<>();
        protected ArrayList<ParallelCoordsRenderer.Feature> features = new ArrayList<>();

        protected ArrayList<String> descriptionPerChunk = new ArrayList<>();

        protected LinkedList<ParallelCoordsDataModelListener> listeners = new LinkedList<>();

        public static interface ParallelCoordsDataModelListener {
            public void featureAdded(ParallelCoordsRenderer.Feature feature);

            public void dataAdded(int chunkIdx, double[][] chunkData, String chunkDescription, int xIdx);

            public void dataChanged(int chunkIdx, double[][] chunkData);
        }

        public synchronized ParallelCoordsDataModel addFeature(double min, double max, String label) {
            ParallelCoordsRenderer.Feature feature = new ParallelCoordsRenderer.Feature(min, max, label);
            features.add(feature);
            notifyFeatureAdded(feature);


            return this;
        }

        public synchronized ParallelCoordsDataModel addFeature(ParallelCoordsRenderer.Feature... features) {
            this.features.addAll(Arrays.asList(features));
            return this;
        }

        public synchronized void addData(double[][] dataChunk, String chunkDescription) {
            int chunkIdx = this.dataChunks.size();
            this.dataChunks.add(dataChunk);
            this.descriptionPerChunk.add(chunkDescription);

            notifyDataAdded(chunkIdx);
        }

        public int numChunks() {
            return dataChunks.size();
        }

        public double[][] getDataChunk(int chunkIdx){
            return dataChunks.get(chunkIdx);
        }

        public int chunkSize(int chunkIdx) {
            return getDataChunk(chunkIdx).length;
        }

        public synchronized void setDataChunk(int chunkIdx, double[][] dataChunk){
            if(chunkIdx >= numChunks())
                throw new ArrayIndexOutOfBoundsException("specified chunkIdx out of bounds: " + chunkIdx);
            this.dataChunks.set(chunkIdx, dataChunk);
            this.notifyDataChanged(chunkIdx);
        }

        public int getFeatureCount() {
            return features.size();
        }

        public ArrayList<ParallelCoordsRenderer.Feature> getFeatures() {
            return this.features;
        }

        public ParallelCoordsRenderer.Feature getFeature(int index) {
            return this.features.get(index);
        }

        public String getChunkDescription(int chunkIdx) {
            return descriptionPerChunk.get(chunkIdx);
        }

        public TreeSet<Integer> getIndicesOfPointsInArea(int chunkIdx, Rectangle2D area){
            // naive search for contained points
            // TODO: quadtree supported search (quadtrees per chunk have to be kept up to date)
            /*int xIdx = getXIdx();
            int yIdx = getYIdx(chunkIdx);
            double[][] data = getDataChunk(chunkIdx);
            TreeSet<Integer> containedPointIndices = new TreeSet<>();
            for(int i=0; i<data.length; i++) {
                if(area.contains(data[i][xIdx], data[i][yIdx]))
                    containedPointIndices.add(i);
            }
            return containedPointIndices;*/
            return null;
        }

        public int getGlobalIndex(int chunkIdx, int idx) {
            int globalIdx=0;
            for(int i=0; i<chunkIdx; i++) {
                globalIdx += chunkSize(i);
            }
            return globalIdx + idx;
        }

        public Pair<Integer, Integer> locateGlobalIndex(int globalIdx){
            int chunkIdx=0;
            while(globalIdx >= chunkSize(chunkIdx)) {
                globalIdx -= chunkSize(chunkIdx);
                chunkIdx++;
            }
            return Pair.of(chunkIdx, globalIdx);
        }

        public int numDataPoints() {
            int n = 0;
            for(int i=0; i<numChunks(); i++)
                n+=chunkSize(i);
            return n;
        }

        public synchronized ParallelCoordsDataModelListener addListener(ParallelCoordsDataModelListener l) {
            listeners.add(l);
            return l;
        }

        public synchronized void removeListener(ParallelCoordsDataModelListener l) {
            listeners.remove(l);
        }

        public synchronized void notifyFeatureAdded(ParallelCoordsRenderer.Feature feature) {
            for(ParallelCoordsDataModelListener l:listeners)
                l.featureAdded(feature);
        }

        public synchronized void notifyDataAdded(int chunkIdx) {
            for(ParallelCoordsDataModelListener l:listeners)
                l.dataAdded(chunkIdx, getDataChunk(chunkIdx), getChunkDescription(chunkIdx), getFeatureCount());
        }

        public synchronized void notifyDataChanged(int chunkIdx) {
            for(ParallelCoordsDataModelListener l:listeners)
                l.dataChanged(chunkIdx, getDataChunk(chunkIdx));
        }
    }

    public static interface ParallelCoordsVisualMapping {
        public default int getColorForChunk(int chunkIdx) {
            DefaultColorMap colorMap = DefaultColorMap.Q_8_SET2;
            return colorMap.getColor(chunkIdx%colorMap.numColors());
        }

        public default void createLegendElementForChunk(Legend legend, int chunkIdx, String chunkDescr, int pickColor) {
            int color = getColorForChunk(chunkIdx);
            legend.addLineLabel(1, color, chunkDescr, pickColor);
        }

        public default void createGeneralLegendElements(Legend legend) {};
    }

    /**
     * Sets up JFrame boilerplate, puts this plot into it, and sets the
     * frame visible on the AWT event dispatch thread.
     * @param title of the window
     * @return the JFrame
     */
    public JFrame display(String title) {
        JFrame frame = new JFrame();
        frame.setTitle(title);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().setLayout(new BorderLayout());
        frame.getContentPane().add(this.canvas.asComponent());
        this.canvas.addCleanupOnWindowClosingListener(frame);

        SwingUtilities.invokeLater( ()->{
            frame.pack();
            frame.setVisible(true);
        });

        return frame;
    }
}
