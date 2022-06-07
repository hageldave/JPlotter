package hageldave.jplotter.renderers;

import hageldave.jplotter.debugging.controlHandler.annotations.DebugGetter;
import hageldave.jplotter.debugging.controlHandler.annotations.DebugSetter;
import hageldave.jplotter.debugging.controlHandler.panelcreators.control.ButtonCreator;
import hageldave.jplotter.debugging.controlHandler.panelcreators.control.PercentageDoubleSliderCreator;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.lwjgl.opengl.GL11;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.*;

/**
 * The SplitScreenRenderer is a space dividing renderer which defines two separate
 * view ports for two other renderers.
 * The space can be divided either vertically or horizontally ({@link #setVerticalSplit(boolean)}).
 * The location of the divider is defined by a relative value determining the area ratio between 
 * the first view port and the whole area (0.5 is equally split, divider in the middle).
 * ({@link #setDividerLocation(double)}).
 * The first renderer will be put on top/left, the second on bottom/right.
 * 
 * @author hageldave
 */
public class SplitScreenRenderer implements Renderer {

	Renderer r1;
	Renderer r2;
	double dividerLocation;
	boolean verticalSplit;
	boolean isEnabled = true;

	/**
	 * Creates a new SplitScreenRenderer that is horizontally and equally split.
	 */
	public SplitScreenRenderer() {
		this(0.5,false);
	}

	/**
	 * Creates a new SplitScreenRenderer.
	 * @param divider relative location of the divider
	 * @param vertical splitting vertically (true) or horizontally (false)
	 */
	public SplitScreenRenderer(double divider, boolean vertical) {
		this(null,null,divider,vertical);
	}

	/**
	 * Creates a new {@link SplitScreenRenderer}
	 * @param r1 left/top renderer
	 * @param r2 right/bottom renderer
	 * @param divider relative location of the divider
	 * @param vertical splitting vertically (true) or horizontally (false)
	 */
	public SplitScreenRenderer(Renderer r1, Renderer r2, double divider, boolean vertical) {
		this.r1=r1;
		this.r2=r2;
		this.dividerLocation=divider;
		this.verticalSplit=vertical;
	}

	/**
	 * Sets the dividers relative location. This determines the viewport sizes for the split.
	 * @param location value in [0.0 .. 1.0], 0.5 is equal, 0.75 uses 3/4 of the space for renderer 1.
	 * @return this for chaining.
	 */
	@DebugSetter(key = "dividerLocation", creator = PercentageDoubleSliderCreator.class)
	public SplitScreenRenderer setDividerLocation(double location) {
		this.dividerLocation = location;
		return this;
	}

	/**
	 * Sets the split orientation.
	 * @param verticalSplit, when true the renderers are put left and right, 
	 * when false they are put top and bottom.
	 * @return this for chaining
	 */
	@DebugSetter(key = "verticalSplit", creator = ButtonCreator.class)
	public SplitScreenRenderer setVerticalSplit(boolean verticalSplit) {
		this.verticalSplit = verticalSplit;
		return this;
	}

	/**
	 * Sets the top/left renderer.
	 * @param r1 renderer to render in the top/left view port.
	 * @return this for chaining
	 */
	public SplitScreenRenderer setR1(Renderer r1) {
		this.r1 = r1;
		return this;
	}

	/**
	 * Sets the bottom/right renderer.
	 * @param r2 renderer to render in the bottom/right view port.
	 * @return this for chaining
	 */
	public SplitScreenRenderer setR2(Renderer r2) {
		this.r2 = r2;
		return this;
	}

	/**
	 * @return the top/left renderer.
	 */
	public Renderer getR1() {
		return r1;
	}

	/**
	 * @return the bottom/right renderer.
	 */
	public Renderer getR2() {
		return r2;
	}

	/**
	 * @return the relative location of the divider.
	 */
	@DebugGetter(key = "dividerLocation")
	public double getDividerLocation() {
		return dividerLocation;
	}

	/**
	 * @return whether the split orientation is vertical
	 */
	@DebugGetter(key = "verticalSplit")
	public boolean isVerticalSplit() {
		return verticalSplit;
	}

	@Override
	public void glInit() {
		if(r1 != null)
			r1.glInit();
		if(r2 != null)
			r2.glInit();
	}

	@Override
	public void render(int vpx, int vpy, int w, int h) {
		if(!isEnabled())
			return;

		glInit();

		int w1 = verticalSplit ? (int)Math.round(w*dividerLocation):w;
		int h1 = verticalSplit ? h:(int)Math.round(h*dividerLocation);
		int w2 = verticalSplit ? w-w1:w;
		int h2 = verticalSplit ? h:h-h1;
		int x1 = 0;
		int y1 = verticalSplit ? 0:h2;
		int x2 = verticalSplit ? w1:0;
		int y2 = 0;

		if(r1 != null) {
			GL11.glViewport( x1+vpx, y1+vpy, w1, h1 );
			r1.render(       x1+vpx, y1+vpy, w1, h1 );
		}
		if(r2 != null) {
			GL11.glViewport( x2+vpx, y2+vpy, w2, h2 );
			r2.render(       x2+vpx, y2+vpy, w2, h2 );
		}
		GL11.glViewport(vpx, vpy, w, h);
	}

	@Override
	public void renderFallback(Graphics2D g, Graphics2D p, int w, int h) {
		if(!isEnabled())
			return;

		int w1 = verticalSplit ? (int)Math.round(w*dividerLocation):w;
		int h1 = verticalSplit ? h:(int)Math.round(h*dividerLocation);
		int w2 = verticalSplit ? w-w1:w;
		int h2 = verticalSplit ? h:h-h1;
		int x1 = 0;
		int y1 = verticalSplit ? 0:h2;
		int x2 = verticalSplit ? w1:0;
		int y2 = 0;

		if(r1 != null) {
			// create translated and clipped graphics for the content
			Graphics2D g_ = (Graphics2D)g.create(x1,y1,w1,h1);
			Graphics2D p_ = (Graphics2D)p.create(x1,y1,w1,h1);
			r1.renderFallback(g_,p_, w1, h1);
		}
		if(r2 != null) {
			// create translated and clipped graphics for the content
			Graphics2D g_ = (Graphics2D)g.create(x2,y2,w2,h2);
			Graphics2D p_ = (Graphics2D)p.create(x2,y2,w2,h2);
			r2.renderFallback(g_,p_, w2, h2);
		}
	}

	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		if(!isEnabled())
			return;

		int w1 = verticalSplit ? (int)Math.round(w*dividerLocation):w;
		int h1 = verticalSplit ? h:(int)Math.round(h*dividerLocation);
		int w2 = verticalSplit ? w-w1:w;
		int h2 = verticalSplit ? h:h-h1;
		int x1 = 0;
		int y1 = verticalSplit ? 0:h2;
		int x2 = verticalSplit ? w1:0;
		int y2 = 0;

		if(r1 != null) {
			// create a new group for the content
			Element contentGroup = SVGUtils.createSVGElement(doc, "g");
			parent.appendChild(contentGroup);
			// define the clipping rectangle for the content (rect of vieport size)
			Node defs = SVGUtils.getDefs(doc);
			Element clip = SVGUtils.createSVGElement(doc, "clipPath");
			String clipDefID = SVGUtils.newDefId();
			clip.setAttributeNS(null, "id", clipDefID);
			clip.appendChild(SVGUtils.createSVGRect(doc, 0, 0, w1, h1));
			defs.appendChild(clip);
			// transform the group according to the viewport position and clip it
			contentGroup.setAttributeNS(null, "transform", "translate("+(x1)+","+(y1)+")");
			contentGroup.setAttributeNS(null, "clip-path", "url(#"+clipDefID+")");
			// render the content into the group
			r1.renderSVG(doc, contentGroup, w1, h1);
		}
		if(r2 != null) {
			// create a new group for the content
			Element contentGroup = SVGUtils.createSVGElement(doc, "g");
			parent.appendChild(contentGroup);
			// define the clipping rectangle for the content (rect of vieport size)
			Node defs = SVGUtils.getDefs(doc);
			Element clip = SVGUtils.createSVGElement(doc, "clipPath");
			String clipDefID = SVGUtils.newDefId();
			clip.setAttributeNS(null, "id", clipDefID);
			clip.appendChild(SVGUtils.createSVGRect(doc, 0, 0, w2, h2));
			defs.appendChild(clip);
			// transform the group according to the viewport position and clip it
			contentGroup.setAttributeNS(null, "transform", "translate("+(x2)+","+(y2)+")");
			contentGroup.setAttributeNS(null, "clip-path", "url(#"+clipDefID+")");
			// render the content into the group
			r2.renderSVG(doc, contentGroup, w2, h2);
		}
	}

	@Override
	public void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h) {
		if(!isEnabled())
			return;

		int w1 = verticalSplit ? (int)Math.round(w*dividerLocation):w;
		int h1 = verticalSplit ? h:(int)Math.round(h*dividerLocation);
		int w2 = verticalSplit ? w-w1:w;
		int h2 = verticalSplit ? h:h-h1;
		int x1 = 0;
		int y1 = verticalSplit ? 0:h2;
		int x2 = verticalSplit ? w1:0;
		int y2 = 0;

		x1=x1+x;x2=x2+x;y1=y1+y;y2=y2+y;

		if(r1 != null) {
			r1.renderPDF(doc, page, x1, y1, w1, h1);
		}
		if(r2 != null) {
			r2.renderPDF(doc, page, x2, y2, w2, h2);
		}
	}

	@Override
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

}
