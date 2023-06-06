package hageldave.jplotter.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.ToDoubleFunction;

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


    public QuadTree(int level, int maxCapacity, Rectangle2D bounds, ToDoubleFunction<T> xCoordAccessor, ToDoubleFunction<T> yCoordAccessor) {
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

    public static <T> void insert(QuadTree<T> qt, T node) {
        double x = qt.getxCoordAccessor().applyAsDouble(node);
        double y = qt.getyCoordAccessor().applyAsDouble(node);

        if (!qt.getBounds().contains(x, y)) {
            return;
        }

        if (qt.getNodes().size() < qt.getMaxCapacity()) {
            qt.getNodes().add(node);
        } else {
            // Exceeded the capacity so split it
            if (qt.getLowerLeft() == null) {
                split(qt);
            }

            // Check coordinates belongs to which partition
            if (qt.getUpperLeft().getBounds().contains(x, y)) {
                insert(qt.getUpperLeft(), node);
            } else if (qt.getUpperRight().getBounds().contains(x, y)) {
                insert(qt.getUpperRight(), node);
            } else if (qt.getLowerLeft().getBounds().contains(x, y)) {
                insert(qt.getLowerLeft(), node);
            } else if (qt.getLowerRight().getBounds().contains(x, y)) {
                insert(qt.getLowerRight(), node);
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

    public static <T> void getPointsInArea(List<T> pointsInArea, QuadTree<T> qt, Rectangle2D area) {
        if (qt.getLowerLeft() != null) {
            if (area.intersects(qt.getUpperLeft().getBounds())) {
                getPointsInArea(pointsInArea, qt.getUpperLeft(), area.createIntersection(qt.getUpperLeft().getBounds()));
            }
            if (area.intersects(qt.getLowerLeft().getBounds())) {
                getPointsInArea(pointsInArea, qt.getLowerLeft(), area.createIntersection(qt.getLowerLeft().getBounds()));
            }
            if (area.intersects(qt.getLowerRight().getBounds())) {
                getPointsInArea(pointsInArea, qt.getLowerRight(), area.createIntersection(qt.getLowerRight().getBounds()));
            }
            if (area.intersects(qt.getUpperRight().getBounds())) {
                getPointsInArea(pointsInArea, qt.getUpperRight(), area.createIntersection(qt.getUpperRight().getBounds()));
            }
        }

        for (T node: qt.getNodes()) {
            double xCoord = qt.getxCoordAccessor().applyAsDouble(node);
            double yCoord = qt.getyCoordAccessor().applyAsDouble(node);

            if (area.contains(xCoord, yCoord)) {
                pointsInArea.add(node);
            }
        }
    }

    public static <T> void printTree(QuadTree<T> quadTree, Function<T, String> nodeFormatter) {
        System.out.println("-----------------------------");
        System.out.println("Level: " + quadTree.getLevel() + ", Role: None");
        System.out.println("Nodes: ");
        int i = 0;
        for (T node: quadTree.getNodes()) {
            System.out.println("Node " + i + ": " + nodeFormatter.apply(node));
            i++;
        }
        System.out.println("Bounds: " + quadTree.getBounds());

        if (quadTree.getLowerLeft() != null) {
            printTree(quadTree.getLowerLeft(), nodeFormatter, "LowerLeft");
            printTree(quadTree.getLowerRight(), nodeFormatter, "LowerRight");
            printTree(quadTree.getUpperLeft(), nodeFormatter, "UpperLeft");
            printTree(quadTree.getUpperRight(), nodeFormatter, "UpperRight");
        }
        System.out.println("-----------------------------");
    }

    public static <T> void printTree(QuadTree<T> quadTree, Function<T, String> nodeFormatter, String role) {
        System.out.println("-----------------------------");
        System.out.println("Level: " + quadTree.getLevel() + ", Role: " + role);
        System.out.println("Nodes: ");
        int i = 0;
        for (T node: quadTree.getNodes()) {
            System.out.println("Node " + i + ": " + nodeFormatter.apply(node));
            i++;
        }
        System.out.println("Bounds: " + quadTree.getBounds());

        if (quadTree.getLowerLeft() != null) {
            printTree(quadTree.getLowerLeft(), nodeFormatter, "LowerLeft");
            printTree(quadTree.getLowerRight(), nodeFormatter, "LowerRight");
            printTree(quadTree.getUpperLeft(), nodeFormatter, "UpperLeft");
            printTree(quadTree.getUpperRight(), nodeFormatter, "UpperRight");
        }
        System.out.println("-----------------------------");
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public int getLevel() {
        return level;
    }

    public QuadTree<T> getLowerLeft() {
        return lowerLeft;
    }

    public QuadTree<T> getLowerRight() {
        return lowerRight;
    }

    public QuadTree<T> getUpperLeft() {
        return upperLeft;
    }

    public QuadTree<T> getUpperRight() {
        return upperRight;
    }

    public Rectangle2D getBounds() {
        return bounds;
    }

    public List<T> getNodes() {
        return nodes;
    }

    public ToDoubleFunction<T> getxCoordAccessor() {
        return xCoordAccessor;
    }

    public ToDoubleFunction<T> getyCoordAccessor() {
        return yCoordAccessor;
    }

    public void setLowerLeft(QuadTree<T> lowerLeft) {
        this.lowerLeft = lowerLeft;
    }

    public void setLowerRight(QuadTree<T> lowerRight) {
        this.lowerRight = lowerRight;
    }

    public void setUpperLeft(QuadTree<T> upperLeft) {
        this.upperLeft = upperLeft;
    }

    public void setUpperRight(QuadTree<T> upperRight) {
        this.upperRight = upperRight;
    }
}
