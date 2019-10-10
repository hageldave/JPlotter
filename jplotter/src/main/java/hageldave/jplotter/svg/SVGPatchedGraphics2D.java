package hageldave.jplotter.svg;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.awt.image.ImageObserver;

import org.apache.batik.svggen.ExtensionHandler;
import org.apache.batik.svggen.ImageHandler;
import org.apache.batik.svggen.SVGGeneratorContext;
import org.apache.batik.svggen.SVGGraphics2D;
import org.w3c.dom.Document;

public class SVGPatchedGraphics2D extends SVGGraphics2D {

	public SVGPatchedGraphics2D(Document domFactory, ImageHandler imageHandler, ExtensionHandler extensionHandler,
			boolean textAsShapes) {
		super(domFactory, imageHandler, extensionHandler, textAsShapes);
	}

	public SVGPatchedGraphics2D(Document domFactory) {
		super(domFactory);
	}

	public SVGPatchedGraphics2D(SVGGeneratorContext arg0, boolean arg1) {
		super(arg0, arg1);
	}

	public SVGPatchedGraphics2D(SVGGraphics2D arg0) {
		super(arg0);
	}
	
	@Override
	public boolean drawImage(Image img,
			int dx1, int dy1, int dx2, int dy2,
			int sx1, int sy1, int sx2, int sy2,
			ImageObserver observer)
	{
		if(dx2 < dx1){
			return drawImage(img, 
					dx2, dy1, dx1, dy2,
					sx2, sy1, sx1, sy2,
					observer);
		}
		if(dy2 < dy1){
			return drawImage(img, 
					dx1, dy2, dx2, dy1,
					sx1, sy2, sx2, sy1,
					observer);
		}
		
		int srcW = Math.abs(sx2-sx1);
		int srcH = Math.abs(sy2-sy1);
		BufferedImage src = new BufferedImage(srcW, srcH, BufferedImage.TYPE_INT_ARGB);
		Graphics2D g = src.createGraphics();
		g.drawImage(img, 0,0, srcW,srcH, sx1,sy1, sx2,sy2, null);
		g.dispose();
		
		return drawImage(src, dx1, dy1, dx2-dx1, dy2-dy1, observer);
	}

	@Override
	public Graphics create() {
		return new SVGPatchedGraphics2D(this);
	}
	
}
