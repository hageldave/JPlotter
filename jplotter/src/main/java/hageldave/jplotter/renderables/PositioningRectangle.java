package hageldave.jplotter.renderables;

import java.awt.geom.Point2D;

/**
 * TODO
 */
public class PositioningRectangle {
    protected int x;
    protected int y;

    public PositioningRectangle(int x, int y) {
        this.x = x;
        this.y = y;
    }

    protected double getX() {
        return x;
    }

    protected double getY() {
        return y;
    }


    // TODO: respect the inset too here
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
