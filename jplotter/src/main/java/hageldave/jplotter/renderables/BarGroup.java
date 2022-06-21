package hageldave.jplotter.renderables;

import hageldave.jplotter.renderers.BarRenderer;
import hageldave.jplotter.util.AlignmentConstants;
import hageldave.jplotter.util.Pair;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.function.DoubleSupplier;

/**
 * The BarGroup class is a collection of BarStack elements.
 * Each BarStack element itself is a collection of BarStruct elements.
 * The BarStruct is the atomic element of a BarChart, which contains the properties length, color and pickColor.
 * That means that the BarStructs are the visible elements ("bars") in the BarChart.
 *
 * In consequence when a BarStack is created, a BarStruct implicitly
 * is added to the BarStack with the parameters from the BarStack constructor.
 *
 */
public class BarGroup {
    final protected TreeMap<Integer, BarStack> groupedBars = new TreeMap<>();
    protected String label = "";

    public BarGroup() {}

    public BarGroup(final String label) {
        this.label = label;
    }

    /**
     * Adds a Bar to the BarGroup.
     * Internally a {@link BarStack} will be created, if there isn't one already with the given ID.
     *
     * If there is a BarStack with the given ID, a {@link BarStruct} will be appended to the Stack.
     * Then the label of the existing struct will be overridden by the structLabel parameter.
     *
     * @param ID of the bar
     * @param length of the added bar
     * @param color of the added bar
     * @param stackLabel of the added bar
     * @return this for chaining
     */
    public BarGroup addBarStack(final int ID, final double length, final Color color, final String stackLabel) {
        return addData(new int[]{ID}, new double[]{length}, new Color[]{color}, new String[]{stackLabel});
    }

    /**
     * Adds (multiple) BarStacks (see {@link BarStack}) to the BarGroup.
     * Based on the ID, a BarStack or a BarStruct will be created.
     *
     * If the ID is new and hasn't been added to the BarGroup, a new BarStack will be instantiated and
     * a {@link BarStruct} will be added with the length & color of the corresponding data & color array entry to it.
     * If the ID already exists, a BarStruct will be added to the BarStack with the given ID.
     *
     * All arrays have to be of equal length so that for each ID entry a new BarStruct can be created.
     *
     * @param IDs of the bars
     * @param data of the added bars
     * @param colors of the added bars
     * @param stackLabels of the added bars
     * @return this for chaining
     */
    public BarGroup addData(final int[] IDs, final double[] data, final Color[] colors, final String[] stackLabels) {
        if (!(IDs.length == data.length && data.length == colors.length && colors.length == stackLabels.length))
            throw new IllegalArgumentException("All arrays have to have equal size!");
        for (int i = 0; i < data.length; i++) {
            if (this.groupedBars.containsKey(IDs[i])) {
                this.groupedBars.get(IDs[i]).barStructs.add(new BarStruct(data[i], colors[i]));
                this.groupedBars.get(IDs[i]).description = stackLabels[i];
            } else {
                this.groupedBars.put(IDs[i], new BarStack(IDs[i], data[i], colors[i], stackLabels[i]));
            }
        }
        return this;
    }

    /**
     * Adds BarStacks (see {@link BarStack}) to the BarGroup.
     * @param barStacks these BarStacks will be added to the BarGroup
     * @return this for chaining
     */
    public BarGroup addBarStack(final BarStack... barStacks) {
        for (BarStack struct:barStacks) {
            this.groupedBars.put(struct.ID, struct);
        }
        return this;
    }

    /**
     * Removes the BarStacks (see {@link BarStack}) (and all its contained BarStructs) with the given IDs from the BarGroup.
     * @param IDs BarStacks with the corresponding ID will be removed of the BarGroup
     * @return this for chaining
     */
    public BarGroup removeBarStack(final int... IDs) {
        for (int ID: IDs) {
            this.groupedBars.remove(ID);
        }
        return this;
    }

    /**
     * Removes the BarStack (see {@link BarStack}) (and all its contained BarStructs) from the BarGroup.
     * @param barStacks these BarStacks will be removed of the BarGroup
     * @return this for chaining
     */
    public BarGroup removeBarStack(final BarStack... barStacks) {
        for (BarStack struct: barStacks) {
            this.groupedBars.remove(struct.ID);
        }
        return this;
    }

    /**
     * Returns the bounds of the BarGroup. Based on the alignment they will be computed differently.
     * @param alignment of the BarChart (see {@link AlignmentConstants}).
     * @return the rectangle enclosing the BarGroup.
     */
    public Rectangle2D getBounds(final int alignment) {
        double minValueBar = groupedBars.values().parallelStream()
                        .map(BarStack::getBounds)
                        .mapToDouble(e->e.first)
                        .min().orElse(0);
        double maxValueBar = groupedBars.values().parallelStream()
                .map(BarStack::getBounds)
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

    /**
     * @return the label of the BarGroup.
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return the BarStacks (see {@link BarStack}) contained in the BarGroup
     */
    public TreeMap<Integer, BarStack> getGroupedBars() {
        return groupedBars;
    }

    /**
     * This class defines the BarStack which consists of a number of BarStructs (see {@link BarStruct}).
     * A BarStruct can also have a description that will be displayed by the {@link BarRenderer} as the bar label.
     *
     * There is also a global alpha multiplier ({@link #setGlobalAlphaMultiplier(double)})
     * which scales every structs color alpha value, which can be used to introduce transparency for all structs of this
     * collection.
     * Similarly, the global saturation multiplier ({@link #setGlobalSaturationMultiplier(double)} (double)}) can be used to
     * scale every struct's saturation of this BarStack object by a specific factor.
     */
    public static class BarStack {
        final public LinkedList<BarStruct> barStructs = new LinkedList<>();
        public String description;
        public int ID;
        protected DoubleSupplier globalSaturationMultiplier = () -> 1.0;
        protected DoubleSupplier globalAlphaMultiplier = () -> 1.0;

        public BarStack(final int ID, final double length, final Color color, final String stackLabel) {
            this.barStructs.add(new BarStruct(length, color));
            this.description = stackLabel;
            this.ID = ID;
        }

        /**
         * Adds a {@link BarStruct} to the BarStruct.
         * @param barStruct the stack will be added to this bar struct
         * @return this for chaining
         */
        public BarStack addStruct(final BarStruct barStruct) {
            this.barStructs.add(barStruct);
            return this;
        }

        /**
         * @return the bounding dimensions (min. value, max. value) of this bar struct.
         */
        public Pair<Double, Double> getBounds() {
            double minVal = 0; double maxVal = 0;
            double tempStackLength = 0;
            for (BarStruct barStruct : barStructs) {
                if (barStruct.length > 0 && tempStackLength >= 0) {
                    maxVal += barStruct.length;
                } else if (barStruct.length < 0 && tempStackLength <= 0) {
                    minVal += barStruct.length;
                } else if (barStruct.length > 0 && tempStackLength < 0) {
                    if (maxVal + barStruct.length > maxVal) {
                        maxVal += barStruct.length;
                    }
                } else if (barStruct.length < 0 && tempStackLength > 0) {
                    if (minVal + barStruct.length < minVal) {
                        minVal += barStruct.length;
                    }
                }
                tempStackLength += barStruct.length;
            }
            return new Pair<>(minVal, maxVal);
        }


        /**
         * Sets the global alpha multiplier parameter of this {@link BarStack} object.
         * The value will be multiplied with each stacks point's alpha color value when rendering.
         * The segment will then be rendered with the opacity {@code alpha = globalAlphaMultiplier * point.alpha}.
         * @param globalAlphaMultiplier of the structs in this collection
         * @return this for chaining
         */
        public BarStack setGlobalAlphaMultiplier(DoubleSupplier globalAlphaMultiplier) {
            this.globalAlphaMultiplier = globalAlphaMultiplier;
            return this;
        }

        /**
         * Sets the global alpha multiplier parameter of this {@link BarStack} object.
         * The value will be multiplied with each stacks point's alpha color value when rendering.
         * The segment will then be rendered with the opacity {@code alpha = globalAlphaMultiplier * point.alpha}.
         * @param globalAlphaMultiplier of the structs in this collection
         * @return this for chaining
         */
        public BarStack setGlobalAlphaMultiplier(double globalAlphaMultiplier) {
            return setGlobalAlphaMultiplier(() -> globalAlphaMultiplier);
        }

        /**
         * @return the global alpha multiplier of the stacks in this struct
         */
        public float getGlobalAlphaMultiplier() {
            return (float)globalAlphaMultiplier.getAsDouble();
        }


        /**
         * Sets the saturation multiplier for this Renderable.
         * The effective saturation of the colors results form multiplication of
         * the respective color's saturation by this value.
         * @param saturation change of saturation, default is 1
         * @return this for chaining
         */
        public BarStack setGlobalSaturationMultiplier(DoubleSupplier saturation) {
            this.globalSaturationMultiplier = saturation;
            return this;
        }

        /**
         * Sets the saturation multiplier for this Renderable.
         * The effective saturation of the colors results form multiplication of
         * the respective color's saturation by this value.
         * @param saturation change of saturation, default is 1
         * @return this for chaining
         */
        public BarStack setGlobalSaturationMultiplier(double saturation) {
            return setGlobalSaturationMultiplier(() -> saturation);
        }

        /** @return the saturation multiplier of this renderable */
        public float getGlobalSaturationMultiplier() {
            return (float)globalSaturationMultiplier.getAsDouble();
        }
    }

    /**
     * The BarStruct class defines the length and color of a stack
     * that is used in a struct.
     * A BarStruct is the fundamental element in each barchart, as each BarStack with only one element is essentially a BarStruct.
     */
    public static class BarStruct {
        public Color stackColor;
        public double length;
        public int pickColor;

        public BarStruct(final double length, final Color stackColor) {
            this.stackColor = stackColor;
            this.length = length;
        }

        /**
         * Sets the picking color.
         * When a non 0 transparent color is specified its alpha channel will be set to 0xff to make it opaque.
         * @param pickID picking color of the point (see {@link Points} for details)
         * @return this for chaining
         */
        public BarStruct setPickColor(int pickID){
            if(pickID != 0)
                pickID = pickID | 0xff000000;
            this.pickColor = pickID;
            return this;
        }
    }
}
