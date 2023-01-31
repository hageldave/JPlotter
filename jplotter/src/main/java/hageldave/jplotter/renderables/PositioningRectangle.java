package hageldave.jplotter.renderables;

import java.awt.geom.Point2D;

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


    public Point2D.Double getAnchorPoint(NewText txt) {
        Point2D.Double anchorPoint = new Point2D.Double();
        switch (this.x) {
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

    public Point2D.Double getAnchorPointPDF(NewText txt) {
        Point2D.Double anchorPoint = new Point2D.Double();
        switch (this.x) {
            case 0:
                anchorPoint.setLocation(0, anchorPoint.getY());
                break;
            case 1:
                anchorPoint.setLocation(txt.getBoundsPDF().getWidth() / 2, anchorPoint.getY());
                break;
            case 2:
                anchorPoint.setLocation(txt.getBoundsPDF().getWidth(), anchorPoint.getY());
                break;
        }
        switch (y) {
            case 0:
                anchorPoint.setLocation(anchorPoint.getX(), 0);
                break;
            case 1:
                anchorPoint.setLocation(anchorPoint.getX(), txt.getBoundsPDF().getHeight() / 2);
                break;
            case 2:
                anchorPoint.setLocation(anchorPoint.getX(), txt.getBoundsPDF().getHeight());
                break;
        }
        return anchorPoint;
    }

    public Point2D.Double getAnchorPointSVG(NewText txt) {
        Point2D.Double anchorPoint = new Point2D.Double();
        switch (this.x) {
            case 0:
                anchorPoint.setLocation(0, anchorPoint.getY());
                break;
            case 1:
                anchorPoint.setLocation(txt.getBoundsSVG().getWidth() / 2, anchorPoint.getY());
                break;
            case 2:
                anchorPoint.setLocation(txt.getBoundsSVG().getWidth(), anchorPoint.getY());
                break;
        }
        switch (y) {
            case 0:
                anchorPoint.setLocation(anchorPoint.getX(), 0);
                break;
            case 1:
                anchorPoint.setLocation(anchorPoint.getX(), txt.getBoundsSVG().getHeight() / 2);
                break;
            case 2:
                anchorPoint.setLocation(anchorPoint.getX(), txt.getBoundsSVG().getHeight());
                break;
        }
        return anchorPoint;
    }
}
