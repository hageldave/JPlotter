package hageldave.jplotter.renderers;

import java.awt.geom.Rectangle2D;

import org.lwjgl.opengl.GL11;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import hageldave.jplotter.svg.SVGUtils;

public class SplitScreenRenderer implements Renderer, AdaptableView {

	Renderer r1;
	Renderer r2;
	double spaceRatio;
	boolean verticalSplit;
	boolean isEnabled = true;

	public SplitScreenRenderer() {
		this(0.5,false);
	}

	public SplitScreenRenderer(double ratio, boolean vertical) {
		this(null,null,ratio,vertical);
	}

	public SplitScreenRenderer(Renderer r1, Renderer r2, double ratio, boolean vertical) {
		this.r1=r1;
		this.r2=r2;
		this.spaceRatio=ratio;
		this.verticalSplit=vertical;
	}

	public SplitScreenRenderer setSpaceRatio(double spaceRatio) {
		this.spaceRatio = spaceRatio;
		return this;
	}

	public SplitScreenRenderer setVerticalSplit(boolean verticalSplit) {
		this.verticalSplit = verticalSplit;
		return this;
	}

	public SplitScreenRenderer setR1(Renderer r1) {
		this.r1 = r1;
		return this;
	}

	public SplitScreenRenderer setR2(Renderer r2) {
		this.r2 = r2;
		return this;
	}

	public Renderer getR1() {
		return r1;
	}

	public Renderer getR2() {
		return r2;
	}

	public double getSpaceRatio() {
		return spaceRatio;
	}

	public boolean isVerticalSplit() {
		return verticalSplit;
	}

	@Override
	public void setView(Rectangle2D view) {
		if(r1 instanceof AdaptableView)
			((AdaptableView) r1).setView(view);
		if(r2 instanceof AdaptableView)
			((AdaptableView) r2).setView(view);
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

		int w1 = verticalSplit ? (int)Math.round(w*spaceRatio):w;
		int h1 = verticalSplit ? h:(int)Math.round(h*spaceRatio);
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
	public void renderSVG(Document doc, Element parent, int w, int h) {
		if(!isEnabled())
			return;

		int w1 = verticalSplit ? (int)Math.round(w*spaceRatio):w;
		int h1 = verticalSplit ? h:(int)Math.round(h*spaceRatio);
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
