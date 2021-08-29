package hageldave.jplotter.renderers;

import hageldave.jplotter.util.Annotations.GLContextRequired;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.Rectangle2D;

/**
 * The chained renderer is used to realize the methods {@link Renderer#withAppended(Renderer)}
 * and {@link Renderer#withPrepended(Renderer)}.
 * It consists of two other {@link Renderer}s which it executes after each other.
 * All {@link Renderer} and {@link AdaptableView} method calls are delegated to both.
 * 
 * @author hageldave
 */
public class ChainedRenderer implements Renderer, AdaptableView, GLDoublePrecisionSupport {

	protected Renderer r1,r2;
	protected boolean isEnabled=true;

	public ChainedRenderer(Renderer r1, Renderer r2) {
		this.r1 = r1;
		this.r2 = r2;
	}

	@Override
	public void setView(Rectangle2D rect) {
		if(r1 instanceof AdaptableView)
			((AdaptableView) r1).setView(rect);
		if(r2 instanceof AdaptableView)
			((AdaptableView) r2).setView(rect);
	}

	@Override
	@GLContextRequired
	public void glInit() {
		r1.glInit();
		r2.glInit();
	}

	@Override
	@GLContextRequired
	public void render(int vpx, int vpy, int w, int h) {
		if(!isEnabled()){
			return;
		}
		r1.render(vpx, vpy, w, h);
		r2.render(vpx, vpy, w, h);
	}

	@Override
	public void renderFallback(Graphics2D g, Graphics2D p, int w, int h) {
		if(!isEnabled()){
			return;
		}
		r1.renderFallback(g,p, w, h);
		r2.renderFallback(g,p, w, h);
	}

	@Override
	@GLContextRequired
	public void close() {
		if(r1 != null)
			r1.close();
		if(r2 != null)
			r2.close();
	}

	@Override
	public void setEnabled(boolean enable) {
		this.isEnabled = enable;
	}

	@Override
	public boolean isEnabled() {
		return isEnabled;
	}
	
	/**
	 * Forwards {@link GLDoublePrecisionSupport#setGLDoublePrecisionEnabled(boolean)}
	 * calls to nested {@link Renderer}s.
	 */
	@Override
	public void setGLDoublePrecisionEnabled(boolean enable) {
		if(r1 instanceof GLDoublePrecisionSupport)
			((GLDoublePrecisionSupport) r1).setGLDoublePrecisionEnabled(enable);
		if(r2 instanceof GLDoublePrecisionSupport)
			((GLDoublePrecisionSupport) r2).setGLDoublePrecisionEnabled(enable);
	}

	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		if(!isEnabled()){
			return;
		}
		r1.renderSVG(doc, parent, w, h);
		r2.renderSVG(doc, parent, w, h);
	}

	@Override
	public void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h) {
		if(!isEnabled()){
			return;
		}
		r1.renderPDF(doc, page, x, y, w, h);
		r2.renderPDF(doc, page, x, y, w, h);
	}
}
