package hageldave.jplotter.renderables;

import java.awt.geom.Point2D;

public class PositioningRectangle {
    protected double x;
    protected double y;
    protected NewText text;
    protected Point2D.Double anchorPoint = new Point2D.Double();

    public PositioningRectangle(NewText text, int x, int y) {
        this.text = text;
        setX(x);
        setY(y);
    }

    public void setX(int x) {
        if (x < 0 || x > 2) {
            throw new IllegalArgumentException();
        }
        this.x = x;
        switch (x) {
            case 0:
                this.anchorPoint.setLocation(0, this.anchorPoint.getY());
                break;
            case 1:
                this.anchorPoint.setLocation(this.text.getBounds().getWidth() / 2, this.anchorPoint.getY());
                break;
            case 2:
                this.anchorPoint.setLocation(this.text.getBounds().getWidth(), this.anchorPoint.getY());
                break;
        }
    }

    public void setY(int y) {
        if (y < 0 || y > 2) {
            throw new IllegalArgumentException();
        }
        this.y = y;
        switch (y) {
            case 0:
                this.anchorPoint.setLocation(this.anchorPoint.getX(), 0);
                break;
            case 1:
                this.anchorPoint.setLocation(this.anchorPoint.getX(), this.text.getBounds().getHeight() / 2);
                break;
            case 2:
                this.anchorPoint.setLocation(this.anchorPoint.getX(), this.text.getBounds().getHeight());
                break;
        }
    }

    protected double getX() {
        return x;
    }

    protected double getY() {
        return y;
    }

    public Point2D.Double getAnchorPoint() {
        return anchorPoint;
    }
}
