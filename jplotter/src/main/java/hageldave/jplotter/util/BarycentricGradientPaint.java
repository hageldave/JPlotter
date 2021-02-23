package hageldave.jplotter.util;

import java.awt.Color;
import java.awt.Paint;
import java.awt.PaintContext;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.ColorModel;
import java.awt.image.DataBufferInt;
import java.awt.image.DirectColorModel;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import java.lang.ref.WeakReference;

import hageldave.imagingkit.core.Img;
import sun.awt.image.IntegerComponentRaster;

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
		
		Point2D.Float p1=new Point2D.Float();
	    Point2D.Float p2=new Point2D.Float();
	    Point2D.Float p3=new Point2D.Float();
	    
	    private float x1,x2,x3,y1,y2,y3;
	    private float x12,x23,x13,y12,y23,y13;
	    private float denom;
	    
	    Color color1;
	    Color color2;
	    Color color3;
	    final DirectColorModel cm = new DirectColorModel(32,
				0x00ff0000,       // Red
                0x0000ff00,       // Green
                0x000000ff,       // Blue
                0xff000000        // Alpha
                );
	    WritableRaster saved;
	    WeakReference<int[]> cache;

		public BarycentricGradientPaintContext(
				Point2D p1, Point2D p2, Point2D p3,
				Color color1, Color color2, Color color3,
				AffineTransform xform) 
		{
			this.color1 = color1;
			this.color2 = color2;
			this.color3 = color3;
			xform.transform(p1, this.p1);
			xform.transform(p2, this.p2);
			xform.transform(p3, this.p3);
			
			setupBarycentricConstants();
		}
		
		private void setupBarycentricConstants() {
			// constants for barycentric coords
	        x1=p1.x; x2=p2.x; x3=p3.x; y1=p1.y; y2=p2.y; y3=p3.y;
	        x12=x1-x2; x23=x2-x3; x13=x1-x3; y12=y1-y2; y23=y2-y3; y13=y1-y3;
	        denom=1f/((y23*x13)-(x23*y13));
		}

		@Override
		public void dispose() {
			cacheRaster(saved);
			saved = null;
		}

		@Override
		public ColorModel getColorModel() {
			return cm;
		}

		@Override
		public Raster getRaster(int xA, int yA, int w, int h) {
			WritableRaster rast = saved;
	        if (rast == null) {
	        	rast = getCachedOrCreateRaster(w, h);
	        	saved = rast;
	        } else if(rast.getWidth() != w || rast.getHeight() != h) {
	        	int[] data = dataFromRaster(rast);
	        	if(data.length < w*h) {
	        		data = new int[w*h];
	        	}
	        	rast = createRaster(w, h, data);
	        	saved = rast;
	        }
	        
	        // fill data array with interpolated colors (barycentric coords)
	        int[] data = dataFromRaster(rast);
	        
	        for(int i=0; i<h; i++) {
	        	float y = yA+i;
	        	for(int j=0; j<w; j++) {
	        		int idx = i*w+j;
	        		float x = xA+j;
	        		// TODO: calculate coordinates and mix colors
	        	}
	        }
	        
	        return rast;
		}
		
		private WritableRaster getCachedOrCreateRaster(int w, int h) {
			if(cache != null) {
				int[] data = cache.get();
				if (data != null && data.length >= w*h)
				{
					cache = null;
					return createRaster(w, h, data);
				}
			}
			return createRaster(w, h, new int[w*h]);
		}
		
		private void cacheRaster(WritableRaster ras) {
			int[] toCache = dataFromRaster(ras);
			if (cache != null) {
	            int[] data = cache.get();
	            if (data != null) {
	                if (toCache.length < data.length) {
	                    return;
	                }
	            }
	        }
	        cache = new WeakReference<>(toCache);
		}
		
		private WritableRaster createRaster(int w, int h, int[] data) {
			DataBufferInt buffer = new DataBufferInt(data, w*h);
			WritableRaster raster = Raster.createPackedRaster(buffer, w, h, w, cm.getMasks(), null);
			return raster;
		}
		
		private static int[] dataFromRaster(WritableRaster wr) {
			return ((DataBufferInt)wr.getDataBuffer()).getData();
		}
		
	}
    

}
