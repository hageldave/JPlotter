package hageldave.jplotter.util;

import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class QuadTree {
    protected int level;
    protected QuadTree lowerLeft;
    protected QuadTree lowerRight;
    protected QuadTree upperLeft;
    protected QuadTree upperRight;
    protected Rectangle2D bounds;
    protected static final int MAX_CAPACITY = 8;
    protected List<Pair<double[], Integer>> nodes;

    public QuadTree(int level, Rectangle2D bounds) {
        this.level = level;
        this.bounds = bounds;
        this.nodes = new ArrayList<>();
    }

    public static void insert(QuadTree qt, Pair<double[], Integer> coords) {
        double x = coords.first[0];
        double y = coords.first[1];

        if (!qt.bounds.contains(x, y)) {
            return;
        }

        if (qt.nodes.size() < MAX_CAPACITY) {
            qt.nodes.add(coords);
        } else {
            // Exceeded the capacity so split it
            if (qt.lowerLeft == null) {
                split(qt);
            }

            // Check coordinates belongs to which partition
            if (qt.upperLeft.bounds.getBounds2D().contains(x, y)) {
                insert(qt.upperLeft, new Pair<>(coords.first, coords.second));
            } else if (qt.upperRight.bounds.getBounds2D().contains(x, y)) {
                insert(qt.upperRight, new Pair<>(coords.first, coords.second));
            } else if (qt.lowerLeft.bounds.getBounds2D().contains(x, y)) {
                insert(qt.lowerLeft, new Pair<>(coords.first, coords.second));
            } else if (qt.lowerRight.bounds.getBounds2D().contains(x, y)) {
                insert(qt.lowerRight, new Pair<>(coords.first, coords.second));
            }
        }
    }

    protected static void split(QuadTree qt) {
        double xOffset = qt.bounds.getMinX() + qt.bounds.getWidth() / 2.0;
        double yOffset = qt.bounds.getMinY() + qt.bounds.getHeight() / 2.0;

        Rectangle2D UL = new Rectangle2D.Double(qt.bounds.getMinX(), yOffset, qt.bounds.getWidth()/2.0, qt.bounds.getHeight()/2.0);
        Rectangle2D UR = new Rectangle2D.Double(xOffset, yOffset, qt.bounds.getWidth()/2.0, qt.bounds.getHeight()/2.0);
        Rectangle2D LL = new Rectangle2D.Double(qt.bounds.getMinX(), qt.bounds.getMinY(), qt.bounds.getWidth()/2.0, qt.bounds.getHeight()/2.0);
        Rectangle2D LR = new Rectangle2D.Double(xOffset, qt.bounds.getMinY(), qt.bounds.getWidth()/2.0, qt.bounds.getHeight()/2.0);

        qt.upperLeft = new QuadTree(qt.level + 1, UL);
        qt.upperRight = new QuadTree(qt.level + 1, UR);
        qt.lowerLeft = new QuadTree(qt.level + 1, LL);
        qt.lowerRight = new QuadTree(qt.level + 1, LR);
    }

    public static void getPointsInArea(LinkedList<Pair<double[], Integer>> pointsInArea, QuadTree qt, Rectangle2D area) {
        if (qt.lowerLeft != null) {
            if (area.intersects(qt.lowerLeft.bounds)) {
                getPointsInArea(pointsInArea, qt.lowerLeft, area.createIntersection(qt.lowerLeft.bounds.getBounds2D()));
            }
            if (area.intersects(qt.upperLeft.bounds)) {
                getPointsInArea(pointsInArea, qt.upperLeft, area.createIntersection(qt.upperLeft.bounds.getBounds2D()));
            }
            if (area.intersects(qt.lowerRight.bounds)) {
                getPointsInArea(pointsInArea, qt.lowerRight, area.createIntersection(qt.lowerRight.bounds.getBounds2D()));
            }
            if (area.intersects(qt.upperRight.bounds)) {
                getPointsInArea(pointsInArea, qt.upperRight, area.createIntersection(qt.upperRight.bounds.getBounds2D()));
            }
        }

        for (Pair<double[], Integer> node: qt.nodes) {
            if (area.contains(node.first[0], node.first[1])) {
                pointsInArea.add(node);
            }
        }
    }
}
