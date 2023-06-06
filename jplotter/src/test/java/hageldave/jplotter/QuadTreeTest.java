package hageldave.jplotter;

import hageldave.jplotter.util.QuadTree;

import java.awt.geom.Rectangle2D;
import java.util.LinkedList;
import java.util.List;

public class QuadTreeTest {
    public static void main(String[] args) {
        // Unit Tests

        QuadTree<double[]> quadTree = new QuadTree<>(4, new Rectangle2D.Double(0, 0, 100, 100), (double[] entry) -> entry[0], (double[] entry) -> entry[1]);

        List<double[]> allPoints = new LinkedList<>();
        allPoints.add(new double[]{0, 0});
        allPoints.add(new double[]{20, 20});
        allPoints.add(new double[]{30, 20});
        allPoints.add(new double[]{30, 30});
        allPoints.add(new double[]{45, 30});
        allPoints.add(new double[]{30, 45});
        allPoints.add(new double[]{30, 45});
        allPoints.add(new double[]{45, 45});
        allPoints.add(new double[]{45, 45});
        allPoints.add(new double[]{80, 45});
        allPoints.add(new double[]{45, 80});
        allPoints.add(new double[]{80, 80});
//        allPoints.add(new double[]{100, 100});

        for (int i = 0; i < allPoints.size(); i++) {
            QuadTree.insert(quadTree, allPoints.get(i));
        }

        List<double[]> foundPointsInWholeArea = new LinkedList<>();
        QuadTree.getPointsInArea(foundPointsInWholeArea, quadTree, new Rectangle2D.Double(0, 0, 100, 100));

        if (!(allPoints.containsAll(foundPointsInWholeArea) && foundPointsInWholeArea.containsAll(allPoints))) {
            throw new RuntimeException();
        }

        List<double[]> foundPointsInSubarea = new LinkedList<>();
        QuadTree.getPointsInArea(foundPointsInSubarea, quadTree, new Rectangle2D.Double(25, 25, 25, 25));

        if (foundPointsInSubarea.size() != 6) {
            throw new RuntimeException();
        }

        foundPointsInSubarea = new LinkedList<>();
        QuadTree.getPointsInArea(foundPointsInSubarea, quadTree, new Rectangle2D.Double(0, 0, 50, 50));

        if (foundPointsInSubarea.size() != 9) {
            throw new RuntimeException();
        }

        foundPointsInSubarea = new LinkedList<>();
        QuadTree.getPointsInArea(foundPointsInSubarea, quadTree, new Rectangle2D.Double(10, 70, 80, 50));

        if (foundPointsInSubarea.size() != 2) {
            throw new RuntimeException();
        }

        // Test if all points are found in the whole area
        // Test if points are found in subareas
        // Test edge cases with subareas / e.g. subareas that are overlapping

        // Test if the correct amount of subrectangles is created
        // Test if the points are inserted at the correct level
        // Test if the max capacity isn't too high in rects

        // Level 1
        QuadTree<double[]> level_1_LL = quadTree.getLowerLeft();
        QuadTree<double[]> level_1_LR = quadTree.getLowerRight();
        QuadTree<double[]> level_1_UL = quadTree.getUpperLeft();
        QuadTree<double[]> level_1_UR = quadTree.getUpperRight();

        // Level 2
        QuadTree<double[]> level_2_LL = level_1_LL.getLowerLeft();
        QuadTree<double[]> level_2_LR = level_1_LL.getLowerRight();
        QuadTree<double[]> level_2_UL = level_1_LL.getUpperLeft();
        QuadTree<double[]> level_2_UR = level_1_LL.getUpperRight();

        // Level 3
        QuadTree<double[]> level_3_LL = level_2_UR.getLowerLeft();
        QuadTree<double[]> level_3_LR = level_2_UR.getLowerRight();
        QuadTree<double[]> level_3_UL = level_2_UR.getUpperLeft();
        QuadTree<double[]> level_3_UR = level_2_UR.getUpperRight();

        // Test null checks

    }
}
