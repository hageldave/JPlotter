package hageldave.jplotter.util;

import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.Raster;

public class BarycentricGradientPaint implements Paint {
	
	Point2D.Float p1;
    Point2D.Float p2;
    Point2D.Float p3;
    Color color1;
    Color color2;
    Color color3;
   
    
	public BarycentricGradientPaint(Point2D p1, Point2D p2, Point2D p3, Color color1, Color color2, Color color3) {
		this.p1 = new Point2D.Float((float)p1.getX(), (float)p1.getY());
		this.p2 = new Point2D.Float((float)p2.getX(), (float)p2.getY());
		this.p3 = new Point2D.Float((float)p3.getX(), (float)p3.getY());
		this.color1 = color1;
		this.color2 = color2;
		this.color3 = color3;
	}
	
	public BarycentricGradientPaint(float x1, float y1, float x2, float y2, float x3, float y3, Color color1, Color color2, Color color3) {
		this.p1 = new Point2D.Float(x1, y1);
		this.p2 = new Point2D.Float(x2, y2);
		this.p3 = new Point2D.Float(x3, y3);
		this.color1 = color1;
		this.color2 = color2;
		this.color3 = color3;
	}


	/**
     * Returns the transparency mode for this {@code GradientPaint}.
     * @return an integer value representing this {@code GradientPaint}
     * object's transparency mode.
     * @see Transparency
     */
    @Override
    public int getTransparency() {
        int a1 = color1.getAlpha();
        int a2 = color2.getAlpha();
        int a3 = color3.getAlpha();
        return (((a1 & a2 & a3) == 0xff) ? OPAQUE : TRANSLUCENT);
    }
    
    
	@Override
	public PaintContext createContext(ColorModel cm, Rectangle deviceBounds, Rectangle2D userBounds,
			AffineTransform xform, RenderingHints hints) {
		return new BarycentricGradientPaintContext(p1,p2,p3, color1,color2,color3, xform);
	}
    
	private static class BarycentricGradientPaintContext implements PaintContext {

		public BarycentricGradientPaintContext(
				Point2D p1, Point2D p2, Point2D p3,
				Color color1, Color color2, Color color3,
				AffineTransform xform) 
		{
			// TODO Auto-generated constructor stub
		}

		@Override
		public void dispose() {
			// TODO Auto-generated method stub
			
		}

		@Override
		public ColorModel getColorModel() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Raster getRaster(int x, int y, int w, int h) {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
    

}
