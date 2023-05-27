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
    protected static final int MAX_CAPACITY = 4;
    public static int depth = 0;
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
            // Exceeded the capacity so split it in FOUR

            if (depth < 16) {
                System.out.println("level " + qt.level + " bounds " + qt.bounds);
            }

            if (qt.lowerLeft == null) {
                split(qt);
            }

            // Check coordinates belongs to which partition
            if (qt.upperLeft.bounds.contains(x, y)) {
                insert(qt.upperLeft, new Pair<>(coords.first, coords.second));
            } else if (qt.upperRight.bounds.contains(x, y)) {
                insert(qt.upperRight, new Pair<>(coords.first, coords.second));
            } else if (qt.lowerLeft.bounds.contains(x, y)) {
                insert(qt.lowerLeft, new Pair<>(coords.first, coords.second));
            } else if (qt.lowerRight.bounds.contains(x, y)) {
                insert(qt.lowerRight, new Pair<>(coords.first, coords.second));
            } else {
                double x_1 = x;
                double y_1 = y;
                System.out.printf("ERROR : Unhandled partition" + x + " " + y);
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

        depth+=1;
    }

    public static List<Pair<double[], Integer>> getPointsInArea(LinkedList<Pair<double[], Integer>> pointsInArea, QuadTree qt, Rectangle2D area) {
        if (qt.lowerLeft != null) {
            if (area.intersects(qt.lowerLeft.bounds)) {
                /*pointsInArea.addAll(*/getPointsInArea(pointsInArea, qt.lowerLeft, area.getBounds().intersection(qt.lowerLeft.bounds.getBounds()));
            } else if (area.intersects(qt.upperLeft.bounds)) {
                /*pointsInArea.addAll(*/getPointsInArea(pointsInArea, qt.upperLeft, area.getBounds().intersection(qt.upperLeft.bounds.getBounds()));
            } else if (area.intersects(qt.lowerRight.bounds)) {
                /*pointsInArea.addAll(*/getPointsInArea(pointsInArea, qt.lowerRight, area.getBounds().intersection(qt.lowerRight.bounds.getBounds()));
            } else if (area.intersects(qt.upperRight.bounds)) {
                /*pointsInArea.addAll(*/getPointsInArea(pointsInArea, qt.upperRight, area.getBounds().intersection(qt.upperRight.bounds.getBounds()));
            }
        }

        for (Pair<double[], Integer> node: qt.nodes) {
            if (area.contains(node.first[0], node.first[1])) {
                pointsInArea.add(node);
            }
        }

        return pointsInArea;
    }
}
