package hageldave.jplotter;

import hageldave.jplotter.util.QuadTree;

import java.awt.geom.Rectangle2D;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public class QuadTreeTest {
    public final static int MAX_CAPACITY = 4;

    public static void main(String[] args) {
        // Unit Tests
        QuadTree<double[]> quadTree = new QuadTree<>(MAX_CAPACITY, new Rectangle2D.Double(0, 0, 100, 100), (double[] entry) -> entry[0], (double[] entry) -> entry[1]);
        List<double[]> allPoints = insertPoints(quadTree);
        testIfPointsAreReturned(quadTree, allPoints);

        // Test if the correct amount of subrectangles is created
        testThatAllSubRectanglesAreCreated(quadTree);

        // Test if the max capacity isn't too high in rects
        testCapacity(quadTree);

        // Print the tree
        QuadTree.printTree(quadTree, Arrays::toString, "None");
    }

    public static List<double[]> insertPoints(QuadTree<double[]> quadTree) {
        List<double[]> allPoints = new LinkedList<>();

        // insert points to tree
        allPoints.add(new double[]{0, 0});
        allPoints.add(new double[]{80, 45});
        allPoints.add(new double[]{80, 80});
        allPoints.add(new double[]{45, 80});
        allPoints.add(new double[]{20, 20});
        allPoints.add(new double[]{30, 20});
        allPoints.add(new double[]{30, 30});
        allPoints.add(new double[]{45, 30});
        allPoints.add(new double[]{30, 45});
        allPoints.add(new double[]{45, 45});
        allPoints.add(new double[]{100, 100});

        for (int i = 0; i < allPoints.size(); i++) {
            QuadTree.insert(quadTree, allPoints.get(i));
        }
        return allPoints;
    }

    // Test if all points are found in the whole area
    // Test if points are found in subareas
    // Test edge cases with subareas / e.g. subareas that are overlapping
    public static void testIfPointsAreReturned(QuadTree<double[]> quadTree, List<double[]> allPoints) {
        List<double[]> foundPointsInWholeArea = QuadTree.getPointsInArea(quadTree, new Rectangle2D.Double(0, 0, 100, 100));

        if (!(allPoints.containsAll(foundPointsInWholeArea) && foundPointsInWholeArea.containsAll(allPoints))) {
            throw new RuntimeException();
        }

        List<double[]> foundPointsInSubarea = QuadTree.getPointsInArea(quadTree, new Rectangle2D.Double(25, 25, 25, 25));

        if (foundPointsInSubarea.size() != 4) {
            throw new RuntimeException();
        }

        foundPointsInSubarea = QuadTree.getPointsInArea(quadTree, new Rectangle2D.Double(0, 0, 50, 50));

        if (foundPointsInSubarea.size() != 7) {
            throw new RuntimeException();
        }

        foundPointsInSubarea = QuadTree.getPointsInArea(quadTree, new Rectangle2D.Double(10, 70, 80, 50));

        if (foundPointsInSubarea.size() != 2) {
            throw new RuntimeException();
        }
    }

    public static void testThatAllSubRectanglesAreCreated(QuadTree<double[]> quadTree) {
        if (Stream.of(quadTree.getLowerLeft(), quadTree.getUpperLeft(), quadTree.getLowerRight(), quadTree.getUpperRight()).anyMatch(Objects::isNull)) {
            if (Stream.of(quadTree.getLowerLeft(), quadTree.getUpperLeft(), quadTree.getLowerRight(), quadTree.getUpperRight()).anyMatch(Objects::nonNull)) {
                throw new RuntimeException();
            }
        }
    }

    public static void testCapacity(QuadTree<double[]> quadTree) {
        if (quadTree.getNodes().size() > MAX_CAPACITY) {
            throw new RuntimeException();
        }
        if (quadTree.getLowerLeft() != null) {
            testCapacity(quadTree.getLowerLeft());
            testCapacity(quadTree.getLowerRight());
            testCapacity(quadTree.getUpperLeft());
            testCapacity(quadTree.getUpperRight());
        }
    }
}
