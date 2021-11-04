package hageldave.jplotter.renderables;

import hageldave.jplotter.renderers.BarRenderer;
import hageldave.jplotter.util.AlignmentConstants;
import hageldave.jplotter.util.Pair;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.LinkedList;
import java.util.TreeMap;

// renderable class for bars
public class BarGroup {
    final protected TreeMap<Integer, BarStruct> groupedBars = new TreeMap<>();
    protected String label = "";

    public BarGroup() {}

    public BarGroup(final String label) {
        this.label = label;
    }

    public BarGroup addBar(final int ID, final double val, final Color color, final String structLabel) {
        return addData(new int[]{ID}, new double[]{val}, new Color[]{color}, new String[]{structLabel});
    }

    // what if stack is added, and user passes description?!
    public BarGroup addData(final int[] IDs, final double[] data, final Color[] color, final String[] structLabel) {
        if (!(IDs.length == data.length && data.length == color.length && color.length == structLabel.length))
            throw new IllegalArgumentException("All arrays have to have equal size!");
        for (int i = 0; i < data.length; i++) {
            if (this.groupedBars.containsKey(IDs[i])) {
                this.groupedBars.get(IDs[i]).barStacks.add(new BarStack(data[i], color[i]));
                this.groupedBars.get(IDs[i]).description = structLabel[i];
            } else {
                this.groupedBars.put(IDs[i], new BarStruct(data[i], color[i], structLabel[i], IDs[i]));
            }
        }
        return this;
    }

    /**
     *
     * @param structs these BarStructs will be added to the BarGroup
     * @return this for chaining
     */
    public BarGroup addBar(final BarStruct... structs) {
        for (BarStruct struct:structs) {
            this.groupedBars.put(struct.ID, struct);
        }
        return this;
    }

    /**
     *
     * @param IDs BarStructs with the corresponding ID will be removed of the BarGroup
     * @return this for chaining
     */
    public BarGroup removeBars(final int... IDs) {
        for (int ID: IDs) {
            this.groupedBars.remove(ID);
        }
        return this;
    }

    /**
     * @param structs these BarStructs will be removed of the BarGroup
     * @return this for chaining
     */
    public BarGroup removeBars(final BarStruct... structs) {
        for (BarStruct struct: structs) {
            this.groupedBars.remove(struct.ID);
        }
        return this;
    }

    /**
     * important! bounds do not represent the coordinate system
     * @return
     */
    public Rectangle2D getBounds(final int alignment) {
        double minValueBar = groupedBars.values().parallelStream()
                        .map(BarStruct::getBounds)
                        .mapToDouble(e->e.first)
                        .min().orElse(0);
        double maxValueBar = groupedBars.values().parallelStream()
                .map(BarStruct::getBounds)
                .mapToDouble(e->e.second)
                .max().orElse(0);
        double start = 0;
        double end = groupedBars.size();
        if (alignment == AlignmentConstants.VERTICAL) {
            return new Rectangle2D.Double(start, minValueBar, end, maxValueBar);
        } else if (alignment == AlignmentConstants.HORIZONTAL) {
            return new Rectangle2D.Double(minValueBar, start, maxValueBar, end);
        }
        return null;
    }

    protected void copyContent(final Collection<BarStruct> c1,
                               final Collection<BarStruct> c2) {
        c1.clear(); c1.addAll(c2);
    }

    /**
     * @return the grouplabel
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the BarStructs contained by the BarGroup
     */
    public TreeMap<Integer, BarStruct> getGroupedBars() {
        return groupedBars;
    }

    /**
     * This class defines the BarStructs which consist of a number of BarStacks.
     * A BarStruct can also have a description that will be displayed by the {@link BarRenderer} as the bar label.
     */
    public static class BarStruct {
        // barStacks holds the stacks of the BarStruct
        final public LinkedList<BarStack> barStacks = new LinkedList<>();
        public String description;
        public int ID;

        public BarStruct(final double length, final Color color, final String description, final int ID) {
            this.barStacks.add(new BarStack(length, color));
            this.description = description;
            this.ID = ID;
        }

        /**
         *
         * @param barStack the stack will be added to this bar struct
         * @return this for chaining
         */
        public BarStruct addStack(final BarStack barStack) {
            this.barStacks.add(barStack);
            return this;
        }

        /**
         * @return the bounding dimensions (min. value, max. value) of this bar struct.
         */
        public Pair<Double, Double> getBounds() {
            double minVal = 0; double maxVal = 0;
            double tempStackLength = 0;
            for (BarStack barStack : barStacks) {
                if (barStack.length > 0 && tempStackLength >= 0) {
                    maxVal += barStack.length;
                } else if (barStack.length < 0 && tempStackLength <= 0) {
                    minVal += barStack.length;
                } else if (barStack.length > 0 && tempStackLength < 0) {
                    if (maxVal + barStack.length > maxVal) {
                        maxVal += barStack.length;
                    }
                } else if (barStack.length < 0 && tempStackLength > 0) {
                    if (minVal + barStack.length < minVal) {
                        minVal += barStack.length;
                    }
                }
                tempStackLength += barStack.length;
            }
            return new Pair<>(minVal, maxVal);
        }
    }

    /**
     * The BarStack class defines the length and color of a stack
     * that is used in a struct.
     * A BarStack is the fundamental element in each barchart, as each bar struct is essentially a barstack.
     */
    public static class BarStack {
        public Color stackColor;
        public double length;
        public int pickColor;

        public BarStack(final double length, final Color stackColor) {
            this.stackColor = stackColor;
            this.length = length;
        }

        /**
         * Sets the picking color.
         * When a non 0 transparent color is specified its alpha channel will be set to 0xff to make it opaque.
         * @param pickID picking color of the point (see {@link Points} for details)
         * @return this for chaining
         */
        public BarStack setPickColor(int pickID){
            if(pickID != 0)
                pickID = pickID | 0xff000000;
            this.pickColor = pickID;
            return this;
        }
    }
}
