package hageldave.jplotter.util;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class QuadTree<T> {
    protected int maxCapacity;
    protected int level;
    protected QuadTree<T> lowerLeft;
    protected QuadTree<T> lowerRight;
    protected QuadTree<T> upperLeft;
    protected QuadTree<T> upperRight;
    protected Rectangle2D bounds;
    protected List<T> nodes;
    protected Function<T, Point2D> xyAccessor;

    public QuadTree(int level, int maxCapacity, Rectangle2D bounds, Function<T, Point2D> xyAccessor) {
        this.level = level;
        this.maxCapacity = maxCapacity;
        this.bounds = bounds;
        this.xyAccessor = xyAccessor;
        this.nodes = new ArrayList<>();
    }

    public QuadTree(int level, Rectangle2D bounds, Function<T, Point2D> xyAccessor) {
        this(level, 4, bounds, xyAccessor);
    }

    public static <T> void insert(QuadTree<T> qt, T node) {
        Point2D coords = qt.getXyAccessor().apply(node);
        double x = coords.getX();
        double y = coords.getY();

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
        Function<T, Point2D> accessor = qt.getXyAccessor();

        double xOffset = bounds.getMinX() + bounds.getWidth() / 2.0;
        double yOffset = bounds.getMinY() + bounds.getHeight() / 2.0;

        Rectangle2D UL = new Rectangle2D.Double(bounds.getMinX(), yOffset, bounds.getWidth()/2.0, bounds.getHeight()/2.0);
        Rectangle2D UR = new Rectangle2D.Double(xOffset, yOffset, bounds.getWidth()/2.0, bounds.getHeight()/2.0);
        Rectangle2D LL = new Rectangle2D.Double(bounds.getMinX(), bounds.getMinY(), bounds.getWidth()/2.0, bounds.getHeight()/2.0);
        Rectangle2D LR = new Rectangle2D.Double(xOffset, bounds.getMinY(), bounds.getWidth()/2.0, bounds.getHeight()/2.0);

        qt.setUpperLeft(new QuadTree<>(newLevel, UL, accessor));
        qt.setUpperRight(new QuadTree<>(newLevel, UR, accessor));
        qt.setLowerLeft(new QuadTree<>(newLevel, LL, accessor));
        qt.setLowerRight(new QuadTree<>(newLevel, LR, accessor));
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
            Point2D point = qt.getXyAccessor().apply(node);
            if (area.contains(point.getX(), point.getY())) {
                pointsInArea.add(node);
            }
        }
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

    public Function<T, Point2D> getXyAccessor() {
        return xyAccessor;
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
