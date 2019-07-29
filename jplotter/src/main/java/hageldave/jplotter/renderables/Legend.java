package hageldave.jplotter.renderables;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.jplotter.misc.CharacterAtlas;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Pair;
import hageldave.jplotter.util.Annotations.GLContextRequired;

/**
 * The Legend class is {@link Renderable} and its own {@link Renderer} at once.
 * It is intended to be used to display labels and corresponding visual representatives 
 * such as a colored {@link Glyph} or line segment, in order to explain the meaning 
 * of the contents of a visualization.
 * <p>
 * To add items to the legend, the methods {@link #addGlyphLabel(Glyph, Color, String)} and 
 * {@link #addLineLabel(double, Color, String)} can be used.
 * The layout of the items is very similar to {@link FlowLayout} in which the items
 * are positioned next to each other until no more space is available to the right
 * and a line break happens, then positioning continues in the next row.
 * A slight difference is that glyph labels are always first in order and followed by
 * line labels.
 * Layouting happens on {@link #updateGL()}.
 * 
 * @author hageldave
 */
public class Legend implements Renderable, Renderer {
	
	protected ArrayList<Pair<String, Pair<Glyph,Color>>> glyphLabels = new ArrayList<>();
	
	protected ArrayList<Pair<String, Pair<Double,Color>>> lineLabels = new ArrayList<>();
	
	protected Map<Glyph, Points> glyph2points = new LinkedHashMap<>();
	
	protected Map<Double, Lines> thickness2lines = new LinkedHashMap<>();
	
	protected LinkedList<Text> texts = new LinkedList<>();
	
	protected CompleteRenderer delegate = new CompleteRenderer();
	
	protected boolean isDirty = true;
	
	protected int viewPortWidth = 0;
	
	protected int viewPortHeight = 0;
	
	/**
	 * Sets the {@link #isDirty()} state of this legend to true.
	 * This indicates that a call to {@link #updateGL()} is necessary
	 * to sync GL resources with this legends state.
	 * @return this for chaining
	 */
	public Legend setDirty() {
		this.isDirty = true;
		return this;
	}
	
	/**
	 * Adds a label for a glyph to this legend.
	 * @param glyph to appear in front of the label text
	 * @param color color of the glyph
	 * @param labeltxt text of the label
	 * @return this for chaining
	 */
	public Legend addGlyphLabel(Glyph glyph, Color color, String labeltxt){
		glyphLabels.add(Pair.of(labeltxt, Pair.of(glyph, color)));
		return setDirty();
	}
	
	/**
	 * Adds a label for a line to this legend.
	 * @param thickness of the line to appear in front of the label text
	 * @param color of the line
	 * @param labeltxt text of the label
	 * @return this for chaining
	 */
	public Legend addLineLabel(double thickness, Color color, String labeltxt){
		this.lineLabels.add(Pair.of(labeltxt, Pair.of(thickness, color)));
		return setDirty();
	}
	
	/**
	 * NOOP
	 */
	@Override
	public void initGL() {
		// will initialize renderables in updateGL
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	/**
	 * Purges all {@link Points}, {@link Lines} and {@link Text}s of this Legend.
	 * Then these Renderables are created again while laying them out according to
	 * the available viewport size.
	 */
	@Override
	@GLContextRequired
	public void updateGL() {
		clearGL();
		// do layout
		int leftPadding = 4;
		int fontStyle = Font.PLAIN;
		int fontSize = 10;
		int fontHeight = CharacterAtlas.boundsForText(1, fontSize, fontStyle, true).getBounds().height;
		int maxTextWidth = glyphLabels.stream()
				.map(p->CharacterAtlas.boundsForText(p.first.length(), fontSize, fontStyle, true).getBounds().width)
				.mapToInt(i->i)
				.max()
				.orElseGet(()->0
		);
		maxTextWidth = Math.max(maxTextWidth,lineLabels.stream()
				.map(p->CharacterAtlas.boundsForText(p.first.length(), fontSize, fontStyle, true).getBounds().width)
				.mapToInt(i->i)
				.max()
				.orElseGet(()->0)
		);
		int currentX = leftPadding;
		int currentY = viewPortHeight-fontHeight-2;
		// glyphs first
		for(Pair<String, Pair<Glyph, Color>> glyphLabel : glyphLabels) {
			Text lbltxt = new Text(glyphLabel.first, fontSize, fontStyle, true);
			texts.add(lbltxt);
			Glyph glyph = glyphLabel.second.first;
			Color color = glyphLabel.second.second;
			if(!glyph2points.containsKey(glyph)){
				glyph2points.put(glyph, new Points(glyph));
			}
			Points points = glyph2points.get(glyph);
			points.addPoint(currentX+5, currentY+fontHeight/2+1).setColor(color);
			currentX += 14;
			lbltxt.setOrigin(currentX, currentY);
			currentX += lbltxt.getTextSize().width + fontHeight;
			if(viewPortWidth-currentX < 14 + maxTextWidth){
				// new line
				currentX = leftPadding;
				currentY -= Math.max(10, fontHeight)+4; 
			}
		}
		// lines second
		for(Pair<String, Pair<Double, Color>> lineLabel : lineLabels) {
			Text lbltxt = new Text(lineLabel.first, fontSize, fontStyle, true);
			texts.add(lbltxt);
			Double thickness = lineLabel.second.first;
			Color color = lineLabel.second.second;
			if(!thickness2lines.containsKey(thickness)){
				Lines lines = new Lines();
				lines
				.setThickness(thickness.floatValue())
				.setVertexRoundingEnabled(thickness.intValue()==thickness.floatValue());
				thickness2lines.put(thickness, lines);
			}
			Lines lines = thickness2lines.get(thickness);
			lines.addSegment(currentX, currentY+fontHeight/2+1, currentX+10, currentY+fontHeight/2+1).setColor(color);
			currentX += 14;
			lbltxt.setOrigin(currentX, currentY);
			currentX += lbltxt.getTextSize().width + fontHeight;
			if(viewPortWidth-currentX < 14 + maxTextWidth){
				// new line
				currentX = leftPadding;
				currentY -= Math.max(10, fontHeight)+4; 
			}
		}
		
		// initialize renderables
		glyph2points.values().forEach(p->{
			p.initGL();
			delegate.addItemToRender(p);
		});
		thickness2lines.values().forEach(l->{
			l.initGL();
			delegate.addItemToRender(l);
		});
		texts.forEach(t->{
			t.initGL();
			delegate.addItemToRender(t);
		});
		isDirty = false;
	}

	@Override
	@GLContextRequired
	public void close() {
		clearGL();
		delegate.close();
	}
	
	
	@GLContextRequired
	protected void clearGL() {
		glyph2points.values().forEach(p->{
			delegate.points.removeItemToRender(p);
			p.close();
		});
		glyph2points.clear();
		thickness2lines.values().forEach(l->{
			delegate.lines.removeItemToRender(l);
			l.close();
		});
		thickness2lines.clear();
		texts.forEach(t->{
			delegate.text.removeItemToRender(t);
			t.close();
		});
		texts.clear();
	}

	/**
	 * Initializes the delegate {@link Renderer}.
	 */
	@Override
	public void glInit() {
		delegate.glInit();
	}

	
	@Override
	public void render(int w, int h) {
		if(w == 0 || h == 0){
			return;
		}
		if(isDirty() || viewPortWidth != w || viewPortHeight != h){
			viewPortWidth = w;
			viewPortHeight = h;
			updateGL();
		}
		delegate.render(w, h);
	}
	
	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		delegate.renderSVG(doc, parent, w, h);
	}
	
	
	/**
	 * Always return false.
	 */
	@Override
	public boolean intersects(Rectangle2D rect) {
		return false;
	}

}
