package hageldave.jplotter.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

/**
 *
 * @param <T>
 */
public class QuadTree<T> {
    protected final int maxCapacity;
    protected int level;
    protected QuadTree<T> lowerLeft;
    protected QuadTree<T> lowerRight;
    protected QuadTree<T> upperLeft;
    protected QuadTree<T> upperRight;
    protected Rectangle2D bounds;
    protected List<T> nodes;
    protected ToDoubleFunction<T> xCoordAccessor;
    protected ToDoubleFunction<T> yCoordAccessor;


    /**
     *
     * @param level
     * @param maxCapacity
     * @param bounds
     * @param xCoordAccessor
     * @param yCoordAccessor
     */
    public QuadTree(int level, int maxCapacity, Rectangle2D bounds, ToDoubleFunction<T> xCoordAccessor, ToDoubleFunction<T> yCoordAccessor) {
        if (maxCapacity < 1) {
            throw new IllegalArgumentException("Capacity has to be larger than 0");
        }
        this.level = level;
        this.maxCapacity = maxCapacity;
        this.bounds = bounds;
        this.xCoordAccessor = xCoordAccessor;
        this.yCoordAccessor = yCoordAccessor;
        this.nodes = new ArrayList<>();
    }

    public QuadTree(int maxCapacity, Rectangle2D bounds, ToDoubleFunction<T> xCoordAccessor, ToDoubleFunction<T> yCoordAccessor) {
        this(0, maxCapacity, bounds, xCoordAccessor, yCoordAccessor);
    }

    public QuadTree(Rectangle2D bounds, ToDoubleFunction<T> xCoordAccessor, ToDoubleFunction<T> yCoordAccessor) {
        this(0, 4, bounds, xCoordAccessor, yCoordAccessor);
    }

    /**
     * Inserts the given node into the QuadTree.
     * If the capacity of this QuadTree is used up, sub-quadtrees will be created and the node will be inserted into that.
     *
     * @param quadTree in which the node will be inserted to
     * @param node that's inserted to the QuadTree
     * @param <T> generic type of the nodes in this QuadTree
     */
    public static <T> void insert(QuadTree<T> quadTree, T node) {
        double x = quadTree.getxCoordAccessor().applyAsDouble(node);
        double y = quadTree.getyCoordAccessor().applyAsDouble(node);

        if (!quadTree.getBounds().contains(x, y)) {
            return;
        }

        if (quadTree.nodes.size() < quadTree.getMaxCapacity()) {
            quadTree.nodes.add(node);
        } else {
            // Exceeded the capacity so split it
            if (quadTree.getLowerLeft() == null) {
                split(quadTree);
            }

            // Check coordinates belongs to which partition
            if (quadTree.getUpperLeft().getBounds().contains(x, y)) {
                insert(quadTree.getUpperLeft(), node);
            } else if (quadTree.getUpperRight().getBounds().contains(x, y)) {
                insert(quadTree.getUpperRight(), node);
            } else if (quadTree.getLowerLeft().getBounds().contains(x, y)) {
                insert(quadTree.getLowerLeft(), node);
            } else if (quadTree.getLowerRight().getBounds().contains(x, y)) {
                insert(quadTree.getLowerRight(), node);
            }
        }
    }

    protected static <T> void split(QuadTree<T> qt) {
        Rectangle2D bounds = qt.getBounds();
        int newLevel = qt.getLevel() + 1;
        int maxCapacity = qt.getMaxCapacity();

        ToDoubleFunction<T> xAccessor = qt.getxCoordAccessor();
        ToDoubleFunction<T> yAccessor = qt.getyCoordAccessor();

        double xOffset = bounds.getMinX() + bounds.getWidth() / 2.0;
        double yOffset = bounds.getMinY() + bounds.getHeight() / 2.0;

        Rectangle2D UL = new Rectangle2D.Double(bounds.getMinX(), yOffset, bounds.getWidth()/2.0, bounds.getHeight()/2.0);
        Rectangle2D UR = new Rectangle2D.Double(xOffset, yOffset, bounds.getWidth()/2.0, bounds.getHeight()/2.0);
        Rectangle2D LL = new Rectangle2D.Double(bounds.getMinX(), bounds.getMinY(), bounds.getWidth()/2.0, bounds.getHeight()/2.0);
        Rectangle2D LR = new Rectangle2D.Double(xOffset, bounds.getMinY(), bounds.getWidth()/2.0, bounds.getHeight()/2.0);

        qt.setUpperLeft(new QuadTree<>(newLevel, maxCapacity, UL, xAccessor, yAccessor));
        qt.setUpperRight(new QuadTree<>(newLevel, maxCapacity, UR, xAccessor, yAccessor));
        qt.setLowerLeft(new QuadTree<>(newLevel, maxCapacity, LL, xAccessor, yAccessor));
        qt.setLowerRight(new QuadTree<>(newLevel, maxCapacity, LR, xAccessor, yAccessor));
    }

    /**
     * Returns all the points of the QuadTree in the given {@link Rectangle2D} area.
     *
     * @param quadTree QuadTree that should be searched
     * @param area all points in this area will be returned
     * @param <T> generic type of the nodes in this QuadTree
     */
    public static <T> List<T> getPointsInArea(QuadTree<T> quadTree, Rectangle2D area) {
        List<T> pointsInArea = new LinkedList<>();
        if (quadTree.getLowerLeft() != null) {
            if (area.intersects(quadTree.getUpperLeft().getBounds())) {
                pointsInArea.addAll(getPointsInArea(quadTree.getUpperLeft(), area.createIntersection(quadTree.getUpperLeft().getBounds())));
            }
            if (area.intersects(quadTree.getLowerLeft().getBounds())) {
                pointsInArea.addAll(getPointsInArea(quadTree.getLowerLeft(), area.createIntersection(quadTree.getLowerLeft().getBounds())));
            }
            if (area.intersects(quadTree.getLowerRight().getBounds())) {
                pointsInArea.addAll(getPointsInArea(quadTree.getLowerRight(), area.createIntersection(quadTree.getLowerRight().getBounds())));
            }
            if (area.intersects(quadTree.getUpperRight().getBounds())) {
                pointsInArea.addAll(getPointsInArea(quadTree.getUpperRight(), area.createIntersection(quadTree.getUpperRight().getBounds())));
            }
        }

        for (T node: quadTree.getNodes()) {
            double xCoord = quadTree.getxCoordAccessor().applyAsDouble(node);
            double yCoord = quadTree.getyCoordAccessor().applyAsDouble(node);

            if (area.contains(xCoord, yCoord)) {
                pointsInArea.add(node);
            }
        }
        return pointsInArea;
    }

    /**
     * Prints this tree to the console.
     * The method will be called recursively by the contained QuadTrees to print each level.
     *
     * @param quadTree that will be printed
     * @param nodeFormatter {@link Function} that defines how to convert the generic {@link T} to a String.
     * @param rectanglePosition describes in which position (upper left, lower left, upper right, lower right) the QuadTree is located (only relevant for QuadTrees on lower levels)
     * @param <T> generic type of the nodes in this QuadTree
     */
    public static <T> void printTree(QuadTree<T> quadTree, Function<T, String> nodeFormatter, String rectanglePosition) {
        System.out.println("-----------------------------");
        if (rectanglePosition == null) {
            System.out.println("Level: " + quadTree.getLevel() + ", Rectangle position: " + "None");
        } else {
            System.out.println("Level: " + quadTree.getLevel() + ", Rectangle position: " + rectanglePosition);
        }
        System.out.println("Nodes: ");
        int i = 0;
        for (T node: quadTree.getNodes()) {
            System.out.println(" - Node " + i + ": " + nodeFormatter.apply(node));
            i++;
        }
        System.out.println("Bounds: " + quadTree.getBounds());

        if (quadTree.getLowerLeft() != null) {
            printTree(quadTree.getLowerLeft(), nodeFormatter, "Lower left");
            printTree(quadTree.getLowerRight(), nodeFormatter, "Lower right");
            printTree(quadTree.getUpperLeft(), nodeFormatter, "Upper left");
            printTree(quadTree.getUpperRight(), nodeFormatter, "Upper right");
        }
        System.out.println("-----------------------------");
    }

    /**
     * @return the maximal number of nodes this QuadTree (and its child QuadTrees) can have until a split happens
     */
    public int getMaxCapacity() {
        return maxCapacity;
    }

    /**
     * @return the level of this {@link QuadTree}.
     * As the QuadTree is created recursively (each QuadTree contains four different QuadTrees, if enough points are contained),
     * the level of a QuadTree represents the count of recursive repetitions.
     */
    public int getLevel() {
        return level;
    }

    /**
     * @return the contained {@link QuadTree} that is located in the lower left corner of this QuadTree.
     * The contained QuadTree will be created when there are more points inserted than the constant {@link QuadTree#maxCapacity} allows.
     * Additionally, QuadTrees in the upper right, upper left and the lower right will be created, so that the space of this QuadTree is evenly divided.
     */
    public QuadTree<T> getLowerLeft() {
        return lowerLeft;
    }

    /**
     * @return the contained {@link QuadTree} that is located in the lower right corner of this QuadTree.
     * The contained QuadTree will be created when there are more points inserted than the constant {@link QuadTree#maxCapacity} allows.
     * Additionally, QuadTrees in the upper right, lower left and the upper left will be created, so that the space of this QuadTree is evenly divided.
     */
    public QuadTree<T> getLowerRight() {
        return lowerRight;
    }

    /**
     * @return the contained {@link QuadTree} that is located in the upper left corner of this QuadTree.
     * The contained QuadTree will be created when there are more points inserted than the constant {@link QuadTree#maxCapacity} allows.
     * Additionally, QuadTrees in the upper right, lower left and the lower right will be created, so that the space of this QuadTree is evenly divided.
     */
    public QuadTree<T> getUpperLeft() {
        return upperLeft;
    }

    /**
     * @return the contained {@link QuadTree} that is located in the upper right corner of this QuadTree.
     * The contained QuadTree will be created when there are more points inserted than the constant {@link QuadTree#maxCapacity} allows.
     * Additionally, QuadTrees in the upper left, lower left and the lower right will be created, so that the space of this QuadTree is evenly divided.
     */
    public QuadTree<T> getUpperRight() {
        return upperRight;
    }

    /**
     *
     * @return the bounds / dimensions of this {@link QuadTree}
     */
    public Rectangle2D getBounds() {
        return bounds;
    }

    /**
     * @return copy of the list containing the nodes of this {@link QuadTree}
     */
    public List<T> getNodes() {
        return new ArrayList<>(nodes);
    }

    protected ToDoubleFunction<T> getxCoordAccessor() {
        return xCoordAccessor;
    }

    protected ToDoubleFunction<T> getyCoordAccessor() {
        return yCoordAccessor;
    }

    protected void setLowerLeft(QuadTree<T> lowerLeft) {
        this.lowerLeft = lowerLeft;
    }

    protected void setLowerRight(QuadTree<T> lowerRight) {
        this.lowerRight = lowerRight;
    }

    protected void setUpperLeft(QuadTree<T> upperLeft) {
        this.upperLeft = upperLeft;
    }

    protected void setUpperRight(QuadTree<T> upperRight) {
        this.upperRight = upperRight;
    }
}
