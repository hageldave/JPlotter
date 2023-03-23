package hageldave.jplotter.renderables;

import java.awt.geom.Point2D;


public class NewPosRect {
    protected double x;
    protected double y;

    /**
     * Creates a positioning rectangle with the given x- and y-parameters.
     * The parameters define where the text object will be aligned. Their values can be between 0 and 2.
     * For x 0 means left, 0.5 means middle and 1 means right.
     * For y 0 means bottom, 0.5 means middle and 1 means top.
     *
     * @param x defines where the text will be aligned horizontally
     * @param y defines where the text will be aligned vertically
     */
    public NewPosRect(double x, double y) {
        if (x < 0 || x > 1 || y < 0 || y > 1) {
            throw new IllegalArgumentException("Values have to be between 0 and 1");
        }
        this.x = x;
        this.y = y;
    }

    protected double getX() {
        return x;
    }

    protected double getY() {
        return y;
    }

    /**
     * Calculates the so-called anchor point of the text object given the x- and y-parameter.
     * The anchor point determines at which point the text object is oriented.
     * This influences both the positioning and the rotation of the text object.
     * E.g. with (x, y) being (1, 1) the text object will be rotated around the upper right corner.
     *
     * @param txt the text object that's anchor point should be determined
     * @return the anchor point
     */
    public Point2D.Double getAnchorPoint(NewText txt) {
        return new Point2D.Double(txt.getBounds().getWidth()*x, txt.getBounds().getHeight()*y);
    }

    /**
     * Calculates the so-called anchor point of the text object that should be exported given the x- and y-parameter.
     * The anchor point determines at which point the text object is oriented.
     * This influences both the positioning and the rotation of the text object.
     * E.g. with (x, y) being (1, 1) the text object will be rotated around the upper right corner.
     *
     * @param txt the text object that's anchor point should be determined
     * @return the anchor point
     */
    public Point2D.Double getAnchorPointExport(NewText txt) {
        return new Point2D.Double(txt.getBoundsExport().getWidth()*x, txt.getBoundsExport().getHeight()*y);
    }
}
