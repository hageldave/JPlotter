package hageldave.jplotter.renderables;

import java.awt.geom.Point2D;

/**
 * TODO
 */
public class PositioningRectangle {
    protected int x;
    protected int y;

    /**
     * Creates a positioning rectangle with the given x- and y-parameters.
     * The parameters define where the text object will be aligned. Their values can be between 0 and 2.
     * For x 0 means left, 1 means middle and 2 means right.
     * For y 0 means bottom, 1 means middle and 2 means top.
     *
     * @param x defines where the text will be aligned horizontally
     * @param y defines where the text will be aligned vertically
     */
    public PositioningRectangle(int x, int y) {
        this.x = x;
        this.y = y;
    }

    protected int getX() {
        return x;
    }

    protected int getY() {
        return y;
    }


    /**
     * Calculates the so-called anchor point of the text object given the x- and y-parameter.
     * The anchor point determines at which point the text object is oriented.
     * This influences both the positioning and the rotation of the text object.
     * E.g. with (x, y) being (2, 2) the text object will be rotated around the upper right corner.
     *
     * @param txt the text object that's anchor point should be determined
     * @return the anchor point
     */
    public Point2D.Double getAnchorPoint(NewText txt) {
        Point2D.Double anchorPoint = new Point2D.Double();
        switch (x) {
            case 0:
                anchorPoint.setLocation(0, anchorPoint.getY());
                break;
            case 1:
                anchorPoint.setLocation(txt.getBounds().getWidth() / 2, anchorPoint.getY());
                break;
            case 2:
                anchorPoint.setLocation(txt.getBounds().getWidth(), anchorPoint.getY());
                break;
        }
        switch (y) {
            case 0:
                anchorPoint.setLocation(anchorPoint.getX(), 0);
                break;
            case 1:
                anchorPoint.setLocation(anchorPoint.getX(), txt.getBounds().getHeight() / 2);
                break;
            case 2:
                anchorPoint.setLocation(anchorPoint.getX(), txt.getBounds().getHeight());
                break;
        }
        return anchorPoint;
    }

    /**
     * Calculates the so-called anchor point of the text object that should be exported given the x- and y-parameter.
     * The anchor point determines at which point the text object is oriented.
     * This influences both the positioning and the rotation of the text object.
     * E.g. with (x, y) being (2, 2) the text object will be rotated around the upper right corner.
     *
     * @param txt the text object that's anchor point should be determined
     * @return the anchor point
     */
    public Point2D.Double getAnchorPointExport(NewText txt) {
        Point2D.Double anchorPoint = new Point2D.Double();
        switch (x) {
            case 0:
                anchorPoint.setLocation(0, anchorPoint.getY());
                break;
            case 1:
                anchorPoint.setLocation(txt.getBoundsExport().getWidth() / 2, anchorPoint.getY());
                break;
            case 2:
                anchorPoint.setLocation(txt.getBoundsExport().getWidth(), anchorPoint.getY());
                break;
        }
        switch (y) {
            case 0:
                anchorPoint.setLocation(anchorPoint.getX(), 0);
                break;
            case 1:
                anchorPoint.setLocation(anchorPoint.getX(), txt.getBoundsExport().getHeight() / 2);
                break;
            case 2:
                anchorPoint.setLocation(anchorPoint.getX(), txt.getBoundsExport().getHeight());
                break;
        }
        return anchorPoint;
    }
}
