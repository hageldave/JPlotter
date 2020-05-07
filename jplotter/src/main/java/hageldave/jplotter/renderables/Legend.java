package hageldave.jplotter.renderables;

import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.font.CharacterAtlas;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.Utils;

/**
 * The Legend class is {@link Renderable} and its own {@link Renderer} at once.
 * It is intended to be used to display labels and corresponding visual representatives 
 * such as a colored {@link Glyph} or line segment, in order to explain the meaning 
 * of the contents of a visualization.
 * <p>
 * To add items to the legend, the methods {@link #addGlyphLabel(Glyph, int, String)} and 
 * {@link #addLineLabel(double, int, String)} can be used.
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
	
	protected ArrayList<GlyphLabel> glyphLabels = new ArrayList<>(0);
	
	protected ArrayList<LineLabel> lineLabels = new ArrayList<>(0);
	
	protected ArrayList<ColormapLabel> colormapLabels = new ArrayList<>(0);
	
	protected Map<Glyph, Points> glyph2points = new LinkedHashMap<>();
	
	protected Map<Integer, Lines> pattern2lines = new LinkedHashMap<>();
	
	protected LinkedList<Triangles> triangles = new LinkedList<>();
	
	protected LinkedList<Text> texts = new LinkedList<>();
	
	protected CompleteRenderer delegate = new CompleteRenderer();
	
	protected boolean isDirty = true;
	
	protected int viewPortWidth = 0;
	
	protected int viewPortHeight = 0;
	
	protected boolean isEnabled=true;
	
	protected static class GlyphLabel {
		public String labelText;
		public Glyph glyph;
		public int color;
		public int pickColor;
		
		public GlyphLabel(String labelText, Glyph glyph, int color, int pickColor) {
			this.labelText = labelText;
			this.glyph = glyph;
			this.color = color;
			this.pickColor = pickColor;
		}
	}
	
	protected static class LineLabel {
		public String labelText;
		public double thickness;
		public int color;
		public int pickColor;
		public int strokePattern;
		
		public LineLabel(String labelText, double thickness, int color, int pickColor, int strokePattern) {
			this.labelText = labelText;
			this.thickness = thickness;
			this.color = color;
			this.pickColor = pickColor;
			this.strokePattern = strokePattern;
		}
		
		
		public LineLabel(String labelText, double thickness, int color, int pickColor){
			this(labelText, thickness, color, pickColor, 0xffff);
		}
	}
	
	protected static class ColormapLabel {
		public String labelText;
		public ColorMap cmap;
		public boolean vertical;
		public int pickColor;
		public double[] ticks;
		public String[] ticklabels;
		
		public ColormapLabel(String labelText, ColorMap cmap, boolean vertical, int pickColor, double[] ticks, String[] ticklabels) {
			this.labelText = labelText;
			this.cmap = cmap;
			this.vertical = vertical;
			this.pickColor = pickColor;
			this.ticks = ticks == null ? new double[0]:ticks;
			this.ticklabels = ticklabels == null ? new String[0]:ticklabels;
		}
		
		public ColormapLabel(String labelText, ColorMap cmap, boolean vertical, int pickColor, double[] ticks) {
			this(labelText, cmap, vertical, pickColor, ticks, null);
		}
		
		public ColormapLabel(String labelText, ColorMap cmap, boolean vertical, int pickColor) {
			this(labelText, cmap, vertical, pickColor, null, null);
		}
		
		
	}
	
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
	 * @param color integer packed ARGB color value of the glyph
	 * @param labeltxt text of the label
	 * @param pickColor picking color (see {@link FBOCanvas})
	 * @return this for chaining
	 */
	public Legend addGlyphLabel(Glyph glyph, int color, String labeltxt, int pickColor){
		glyphLabels.add(new GlyphLabel(labeltxt, glyph, color, pickColor));
		return setDirty();
	}
	
	/**
	 * Adds a label for a glyph to this legend.
	 * @param glyph to appear in front of the label text
	 * @param color integer packed ARGB color value of the glyph
	 * @param labeltxt text of the label
	 * @return this for chaining
	 */
	public Legend addGlyphLabel(Glyph glyph, int color, String labeltxt){
		return addGlyphLabel(glyph, color, labeltxt, 0);
	}
	
	/**
	 * Adds a label for a line to this legend.
	 * @param thickness of the line to appear in front of the label text
	 * @param color integer packed ARGB color value of the glyph
	 * @param strokePattern lines stroke pattern (see {@link Lines#setStrokePattern(int)})
	 * @param labeltxt text of the label
	 * @param pickColor picking color (see {@link FBOCanvas})
	 * @return this for chaining
	 */
	public Legend addLineLabel(double thickness, int color, int strokePattern, String labeltxt, int pickColor){
		this.lineLabels.add(new LineLabel(labeltxt, thickness, color, pickColor, strokePattern));
		return setDirty();
	}
	
	/**
	 * Adds a label for a line to this legend.
	 * @param thickness of the line to appear in front of the label text
	 * @param color integer packed ARGB color value of the glyph
	 * @param labeltxt text of the label
	 * @param pickColor picking color (see {@link FBOCanvas})
	 * @return this for chaining
	 */
	public Legend addLineLabel(double thickness, int color, String labeltxt, int pickColor){
		return addLineLabel(thickness, color, 0xffff, labeltxt, pickColor);
	}
	
	/**
	 * Adds a label for a line to this legend.
	 * @param thickness of the line to appear in front of the label text
	 * @param color integer packed ARGB color value of the glyph
	 * @param labeltxt text of the label
	 * @return this for chaining
	 */
	public Legend addLineLabel(double thickness, int color, String labeltxt){
		return addLineLabel(thickness, color, labeltxt, 0);
	}
	
	/**
	 * Adds a label for a color map to this legend.
	 * @param labelText text for the label
	 * @param cmap color map to label
	 * @param vertical orientation of the colormap
	 * @param pickColor picking color (see {@link FBOCanvas})
	 * @param ticks tick marks for the map
	 * @param ticklabels labels for the tick marks
	 * @return this for chaining
	 */
	public Legend addColormapLabel(String labelText, ColorMap cmap, boolean vertical, int pickColor, double[] ticks, String[] ticklabels){
		colormapLabels.add(new ColormapLabel(labelText, cmap, vertical, pickColor, ticks, ticklabels));
		return setDirty();
	}
	
	/**
	 * Adds a label for a color map to this legend.
	 * @param labelText text for the label
	 * @param cmap color map to label
	 * @param vertical orientation of the colormap
	 * @param pickColor picking color (see {@link FBOCanvas})
	 * @param ticks tick marks for the map
	 * @return this for chaining
	 */
	public Legend addColormapLabel(String labelText, ColorMap cmap, boolean vertical, int pickColor, double[] ticks){
		return addColormapLabel(labelText, cmap, vertical, pickColor, ticks, null);
	}
	
	/**
	 * Adds a label for a color map to this legend.
	 * @param labelText text for the label
	 * @param cmap color map to label
	 * @param vertical orientation of the colormap
	 * @param pickColor picking color (see {@link FBOCanvas})
	 * @return this for chaining
	 */
	public Legend addColormapLabel(String labelText, ColorMap cmap, boolean vertical, int pickColor){
		return addColormapLabel(labelText, cmap, vertical, pickColor, null, null);
	}
	
	/**
	 * Adds a label for a color map to this legend.
	 * @param labelText text for the label
	 * @param cmap color map to label
	 * @param vertical orientation of the colormap
	 * @param ticks tick marks for the map
	 * @param ticklabels labels for the tick marks
	 * @return this for chaining
	 */
	public Legend addColormapLabel(String labelText, ColorMap cmap, boolean vertical, double[] ticks, String[] ticklabels){
		return addColormapLabel(labelText, cmap, vertical, 0, ticks, ticklabels);
	}
	
	/**
	 * Adds a label for a color map to this legend.
	 * @param labelText text for the label
	 * @param cmap color map to label
	 * @param vertical orientation of the colormap
	 * @param ticks tick marks for the map
	 * @return this for chaining
	 */
	public Legend addColormapLabel(String labelText, ColorMap cmap, boolean vertical, double[] ticks){
		return addColormapLabel(labelText, cmap, vertical, 0, ticks, null);
	}
	
	/**
	 * Adds a label for a color map to this legend.
	 * @param labelText text for the label
	 * @param cmap color map to label
	 * @param vertical orientation of the colormap
	 * @return this for chaining
	 */
	public Legend addColormapLabel(String labelText, ColorMap cmap, boolean vertical){
		return addColormapLabel(labelText, cmap, vertical, 0, null, null);
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
	 * Purges all {@link Points}, {@link Lines}, {@link Triangles} and {@link Text}s of this Legend.
	 * Then these Renderables are created again while laying them out according to
	 * the available viewport size.
	 */
	@Override
	@GLContextRequired
	public void updateGL() {
		clearGL();
		// do layout
		final int leftPadding = 4;
		final int fontStyle = Font.PLAIN;
		final int fontSize = 11;
		final int fontHeight = CharacterAtlas.boundsForText(1, fontSize, fontStyle).getBounds().height;
		final int itemWidth = 16;
		final int itemTextSpacing = 4;
		int maxTextWidth = glyphLabels.stream()
				.map(l->CharacterAtlas.boundsForText(l.labelText.length(), fontSize, fontStyle).getBounds().width)
				.mapToInt(i->i)
				.max()
				.orElseGet(()->0
		);
		maxTextWidth = Math.max(maxTextWidth,lineLabels.stream()
				.map(l->CharacterAtlas.boundsForText(l.labelText.length(), fontSize, fontStyle).getBounds().width)
				.mapToInt(i->i)
				.max()
				.orElseGet(()->0)
		);
		int currentX = leftPadding;
		int currentY = viewPortHeight-fontHeight-2;
		// glyphs first
		for(GlyphLabel glyphLabel : glyphLabels) {
			Text lbltxt = new Text(glyphLabel.labelText, fontSize, fontStyle);
			lbltxt.setPickColor(glyphLabel.pickColor);
			texts.add(lbltxt);
			Glyph glyph = glyphLabel.glyph;
			if(!glyph2points.containsKey(glyph)){
				glyph2points.put(glyph, new Points(glyph));
			}
			Points points = glyph2points.get(glyph);
			points.addPoint(currentX+itemWidth/2, currentY+fontHeight/2+1)
				.setColor(glyphLabel.color)
				.setPickColor(glyphLabel.pickColor);
			currentX += itemWidth+itemTextSpacing;
			lbltxt.setOrigin(currentX, currentY);
			currentX += lbltxt.getTextSize().width + fontHeight;
			if(viewPortWidth-currentX < (itemWidth+itemTextSpacing+maxTextWidth) ){
				// new line
				currentX = leftPadding;
				currentY -= Math.max(10, fontHeight)+5; 
			}
		}
		// lines second
		for(LineLabel lineLabel : lineLabels) {
			Text lbltxt = new Text(lineLabel.labelText, fontSize, fontStyle);
			lbltxt.setPickColor(lineLabel.pickColor);
			texts.add(lbltxt);
			int pattern = lineLabel.strokePattern;
			if(!pattern2lines.containsKey(pattern)){
				Lines lines = new Lines();
				lines
				.setStrokePattern(pattern)
				.setVertexRoundingEnabled(true);
				pattern2lines.put(pattern, lines);
			}
			Lines lines = pattern2lines.get(pattern);
			lines.addSegment(currentX, currentY+fontHeight/2+1, currentX+itemWidth, currentY+fontHeight/2+1)
				.setColor(lineLabel.color)
				.setPickColor(lineLabel.pickColor)
				.setThickness(lineLabel.thickness);
			currentX += itemWidth+itemTextSpacing;
			lbltxt.setOrigin(currentX, currentY);
			currentX += lbltxt.getTextSize().width + fontHeight;
			if(viewPortWidth-currentX < (itemWidth+itemTextSpacing+maxTextWidth) ){
				// new line
				currentX = leftPadding;
				currentY -= Math.max(10, fontHeight)+5; 
			}
		}
		// colormaps third
		for(ColormapLabel cmlabel : colormapLabels) {
			Text lbltxt = new Text(cmlabel.labelText, fontSize, fontStyle);
			lbltxt.setPickColor(cmlabel.pickColor);
			if(!lbltxt.getTextString().isEmpty())
				texts.add(lbltxt);
			int pattern = 0xffff;
			if(!pattern2lines.containsKey(pattern)){
				Lines lines = new Lines();
				lines
				.setStrokePattern(pattern)
				.setVertexRoundingEnabled(true);
				pattern2lines.put(pattern, lines);
			}
			Lines lines = pattern2lines.get(pattern);
			Triangles tris = Utils.colormap2Tris(cmlabel.cmap, cmlabel.vertical);
			triangles.add(tris);
			int cmapSize = 12;
			if(cmlabel.vertical){
				// put label on top
				int currX = currentX+4;
				int currY;
				if(!lbltxt.getTextString().isEmpty()){
					lbltxt.setOrigin(currentX, currentY);
					currY = currentY-fontSize+2;
				} else {
					currY = currentY;
				}
				int w = cmapSize;
				int h = Math.max(cmapSize*3, (fontSize+2)*cmlabel.ticklabels.length);
				// stretch triangles to correct size and translate to correct location
				tris.getTriangleDetails().forEach(t->{
					Arrays.asList(t.p0,t.p1,t.p2).forEach(p->{
						p.setLocation(p.getX()*w+currX, currY-h+p.getY()*h);
					});
				});
				// draw frame
				lines.addLineStrip(currX,currY, currX+w,currY, currX+w,currY-h, currX,currY-h, currX,currY);
				// add ticks (& tick labels)
				for(int i=0; i<cmlabel.ticks.length; i++){
					double tick = cmlabel.ticks[i];
					String lbl = cmlabel.ticklabels.length==0 ? "":cmlabel.ticklabels[i];
					double x = currX+w; double y=currY-h+tick*h;
					lines.addSegment(x, y, x+3, y);
					if(!lbl.isEmpty()){
						Text ticklbl = new Text(lbl, fontSize-2, fontStyle).setOrigin((int)(x+5), (int)(y-(fontSize-2)/2));
						texts.add(ticklbl);
					}
				}
				
			} else {
				/*
				 * 
				 * TODO
				 * 
				 * 
				 */
			}
		}
		
		// initialize renderables
		glyph2points.values().forEach(p->{
			p.initGL();
			delegate.addItemToRender(p);
		});
		pattern2lines.values().forEach(l->{
			l.initGL();
			delegate.addItemToRender(l);
		});
		texts.forEach(t->{
			t.initGL();
			delegate.addItemToRender(t);
		});
		triangles.forEach(t->{
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
		pattern2lines.values().forEach(l->{
			delegate.lines.removeItemToRender(l);
			l.close();
		});
		pattern2lines.clear();
		triangles.forEach(t->{
			t.close();
			delegate.triangles.removeItemToRender(t);
		});
		triangles.clear();
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
	public void render(int vpx, int vpy, int w, int h) {
		if(!isEnabled()){
			return;
		}
		if(w == 0 || h == 0){
			return;
		}
		if(isDirty() || viewPortWidth != w || viewPortHeight != h){
			viewPortWidth = w;
			viewPortHeight = h;
			updateGL();
		}
		delegate.render(vpx, vpy, w, h);
	}
	
	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		if(!isEnabled()){
			return;
		}
		delegate.renderSVG(doc, parent, w, h);
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
	 * Always return false.
	 */
	@Override
	public boolean intersects(Rectangle2D rect) {
		return false;
	}

}
