package hageldave.jplotter.renderables;

import hageldave.jplotter.canvas.FBOCanvas;
import hageldave.jplotter.color.ColorMap;
import hageldave.jplotter.color.ColorScheme;
import hageldave.jplotter.color.DefaultColorScheme;
import hageldave.jplotter.font.CharacterAtlas;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderables.Lines.SegmentDetails;
import hageldave.jplotter.renderables.Points.PointDetails;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.Utils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.List;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

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
 * Layouting happens on {@link #updateGL(boolean)}.
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

	protected ColorScheme colorScheme;

	public Legend() {
		this.colorScheme = DefaultColorScheme.LIGHT.get();
	}

	/**
	 * To synchronize the text colors with other components,
	 * a {@link ColorScheme} can be hand over. The text color in the legend are defined by the ColorProvider.
	 *
	 * @param colorScheme defines the colors for the legend text
	 */
	public Legend(final ColorScheme colorScheme) {
		this.colorScheme = colorScheme;
	}

	public static class GlyphLabel {
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

	public static class LineLabel {
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

	public static class ColormapLabel {
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

	protected static interface LegendElement {
		public void translate(int dx, int dy);

		public Rectangle2D getSize();
	}

	public void setColorScheme(final ColorScheme colorScheme) {
		this.colorScheme = colorScheme;
		setDirty();
	}

	/**
	 * Sets the {@link #isDirty()} state of this legend to true.
	 * This indicates that a call to {@link #updateGL(boolean)} is necessary
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
		if (this.colorScheme != null) {
			return addGlyphLabel(glyph, color, labeltxt, this.colorScheme.getColorText());
		}
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
	public void updateGL(boolean useGLDoublePrecision) {
		clearGL();
		setup();
	}

	/**
	 * creates the legend elements and computes the layout
	 *
	 *
	 */
	protected void setup() {
		// do layout
		final int leftPadding = 4;
		final int elementVSpace = 4;
		final int elementHSpace = 6;
		final int fontStyle = Font.PLAIN;
		final int fontSize = 11;
		final int textColor = this.colorScheme.getColorText();

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
		maxTextWidth = Math.max(maxTextWidth,colormapLabels.stream()
				.map(l->CharacterAtlas.boundsForText(l.labelText.length(), fontSize, fontStyle).getBounds().width-20)
				.mapToInt(i->i)
				.max()
				.orElseGet(()->0)
				);
		maxTextWidth = Math.max(maxTextWidth,colormapLabels.stream()
				.flatMap(l->Arrays.stream(l.ticklabels))
				.map(t->CharacterAtlas.boundsForText(t.length(), fontSize-2, fontStyle).getBounds().width)
				.mapToInt(i->i)
				.max()
				.orElseGet(()->0)
				);

		LinkedList<LegendElement> elements = new LinkedList<>();
		for(GlyphLabel glyphLabel : glyphLabels) {
			Glyph glyph = glyphLabel.glyph;
			if(!glyph2points.containsKey(glyph)){
				glyph2points.put(glyph, new Points(glyph));
			}
			Points points = glyph2points.get(glyph);
			elements.add(new LegendElement() {
				Text lbltxt;
				PointDetails pd;
				Rectangle2D rect;
				{
					lbltxt = new Text(glyphLabel.labelText, fontSize, fontStyle, textColor)
							.setPickColor(glyphLabel.pickColor)
							.setOrigin(itemWidth+itemTextSpacing,0);
					texts.add(lbltxt);
					pd = points.addPoint(itemWidth/2, fontHeight/2+1).setColor(glyphLabel.color).setPickColor(glyphLabel.pickColor);
					rect = new Rectangle(itemWidth+itemTextSpacing+lbltxt.getTextSize().width, fontHeight);
				}
				@Override
				public void translate(int dx, int dy) {
					Utils.translate(lbltxt.getOrigin(), dx, dy);
					Utils.translate(pd.location, dx, dy);
					Utils.translate(rect, dx, dy);
				}
				@Override
				public Rectangle2D getSize() {
					return rect;
				}
			});
		}
		// layout
		int currentY = viewPortHeight-fontHeight;
		RectangleLayout layout = new RectangleLayout();
		for(LegendElement e:elements){
			LegendElement e_=e;
			layout.addComponent(()->{
				Rectangle2D size = e_.getSize();
				return new Dimension((int)size.getWidth(), (int)size.getHeight());
			}, 
					(x,y)->e_.translate(x, y)
					);
		}
		int layoutH = layout.calculateFlowLayout(viewPortWidth-leftPadding, elementHSpace, elementVSpace)
				.flipYAxis(0)
				.translate(leftPadding, currentY)
				.apply()
				.getLayoutSize().height;
		currentY -= layoutH + (layoutH > 0 ? elementVSpace:0);
		elements.clear();


		for(LineLabel lineLabel : lineLabels) {
			int pattern = lineLabel.strokePattern;
			if(!pattern2lines.containsKey(pattern)){
				Lines lines = new Lines();
				lines
				.setStrokePattern(pattern)
				.setVertexRoundingEnabled(true);
				pattern2lines.put(pattern, lines);
			}
			Lines lines = pattern2lines.get(pattern);
			elements.add(new LegendElement() {
				Text lbltxt;
				SegmentDetails seg;
				Rectangle2D rect;
				{
					lbltxt = new Text(lineLabel.labelText, fontSize, fontStyle, textColor)
							.setPickColor(lineLabel.pickColor)
							.setOrigin(itemWidth+itemTextSpacing, 0);
					;
					texts.add(lbltxt);
					seg = lines.addSegment(0, fontHeight/2+1, itemWidth, fontHeight/2+1)
							.setColor(lineLabel.color)
							.setPickColor(lineLabel.pickColor)
							.setThickness(lineLabel.thickness);
					rect = new Rectangle(itemWidth+itemTextSpacing+lbltxt.getTextSize().width, fontHeight);
				}
				@Override
				public void translate(int dx, int dy) {
					Utils.translate(lbltxt.getOrigin(), dx, dy);
					Utils.translate(seg.p0, dx, dy);
					Utils.translate(seg.p1, dx, dy);
					Utils.translate(rect, dx, dy);
				}

				@Override
				public Rectangle2D getSize() {
					return rect;
				}
			});
		}
		// layout
		layout = new RectangleLayout();
		for(LegendElement e:elements){
			LegendElement e_=e;
			layout.addComponent(()->{
				Rectangle2D size = e_.getSize();
				return new Dimension((int)size.getWidth(), (int)size.getHeight());
			}, 
					(x,y)->e_.translate(x, y)
					);
		}
		layoutH = layout.calculateFlowLayout(viewPortWidth-leftPadding, elementHSpace, elementVSpace)
				.flipYAxis(0)
				.translate(leftPadding, currentY)
				.apply()
				.getLayoutSize().height;
		currentY -= layoutH + (layoutH > 0 ? elementVSpace:0);
		elements.clear();


		for(ColormapLabel cmlabel : colormapLabels) {
			// get line object for outline
			int pattern = 0xffff;
			if(!pattern2lines.containsKey(pattern)){
				Lines lines = new Lines();
				lines
				.setStrokePattern(pattern)
				.setVertexRoundingEnabled(true);
				pattern2lines.put(pattern, lines);
			}
			Lines lines = pattern2lines.get(pattern);
			elements.add(new LegendElement() {
				Text lbltxt;
				List<Text> ticks=new LinkedList<>();
				List<SegmentDetails> segs=new LinkedList<>();
				Triangles tris;
				Rectangle2D rect;
				{
					int elementHeight = 0;
					lbltxt = new Text(cmlabel.labelText, fontSize, fontStyle);
					lbltxt.setPickColor(cmlabel.pickColor);
					if(!lbltxt.getTextString().isEmpty()){
						texts.add(lbltxt);
						elementHeight += fontHeight;
					}
					// keep track of current element's width
					int elementWidth = lbltxt.getTextSize().width;
					// create color map element
					tris = Utils.colormap2Tris(cmlabel.cmap, cmlabel.vertical);
					triangles.add(tris);
					int cmapSize = 12;
					// VERTICAL CMAP
					if(cmlabel.vertical){
						// put color map beneath label
						int colormapinset = 3;
						int maptextoffset = 5;
						int currX = colormapinset;
						int currY;
						if(!lbltxt.getTextString().isEmpty()){
							currY = -fontSize+4;
							elementHeight += fontSize;
						} else {
							currY = 4; 
						}
						int w = cmapSize;
						int h = Math.max(cmapSize*3, (fontSize+2)*cmlabel.ticklabels.length);
						// stretch triangles to correct size and translate to correct location
						tris.getTriangleDetails().forEach(t->{
							Arrays.asList(t.p0,t.p1,t.p2).forEach(p->{
								p.setLocation(p.getX()*w+currX, currY-h+p.getY()*h);
							});
						});
						elementHeight += h+(fontSize-2)/2;
						// draw frame
						segs = lines.addLineStrip(currX,currY, currX+w,currY, currX+w,currY-h, currX,currY-h, currX,currY);
						// update element width
						elementWidth = Math.max(elementWidth, colormapinset+cmapSize);
						// add ticks (& tick labels)
						for(int i=0; i<cmlabel.ticks.length; i++){
							double tick = cmlabel.ticks[i];
							String lbl = cmlabel.ticklabels.length==0 ? "":cmlabel.ticklabels[i];
							double x = currX+w; double y=currY-h+tick*h;
							segs.add(lines.addSegment(x, y, x+3, y));
							if(!lbl.isEmpty()){
								Text ticklbl = new Text(lbl, fontSize-2, fontStyle);
								ticklbl.setOrigin((int)(x+maptextoffset), (int)(y-(fontSize-2)/2));
								ticks.add(ticklbl);
								texts.add(ticklbl);
								// update element width
								elementWidth = Math.max(elementWidth, colormapinset+cmapSize+maptextoffset+ticklbl.getTextSize().width);
							}
						}
						rect = new Rectangle(elementWidth, elementHeight);
					} else {
						// put color map beneath label
						int colormapinset = 0;
						int maptextoffset = 5;
						int currX = colormapinset;
						int currY;
						if(!lbltxt.getTextString().isEmpty()){
							currY = -fontSize+8;
						} else {
							currY = 8;
						}
						int h = cmapSize;
						int w = Math.max(cmapSize*3, (fontSize+2)*cmlabel.ticklabels.length);
						// stretch triangles to correct size and translate to correct location
						int currY_ = currY;
						tris.getTriangleDetails().forEach(t->{
							Arrays.asList(t.p0,t.p1,t.p2).forEach(p->{
								p.setLocation(p.getX()*w+currX, currY_-h+p.getY()*h);
							});
						});
						elementWidth = Math.max(elementWidth, w);
						// draw frame
						segs = lines.addLineStrip(currX,currY, currX+w,currY, currX+w,currY-h, currX,currY-h, currX,currY);
						currY -= h;
						// add ticks (& tick labels)
						for(int i=0; i<cmlabel.ticks.length; i++){
							double tick = cmlabel.ticks[i];
							String lbl = cmlabel.ticklabels.length==0 ? "":cmlabel.ticklabels[i];
							double x = currX+tick*w; double y=currY;
							segs.add(lines.addSegment(x, y, x, y-3));
							if(!lbl.isEmpty()){
								Text ticklbl = new Text(lbl, fontSize-2, fontStyle);
								int tickW = ticklbl.getTextSize().width;
								int xtick = (tick==0.0 ? 0 : ( tick==1.0 ? (int)(x-tickW+2) : (int)(x-tickW/2)));
								ticklbl.setOrigin(xtick, (int)(y-maptextoffset-(fontSize-2)));
								ticks.add(ticklbl);
								texts.add(ticklbl);
								// update element width
								elementWidth = Math.max(elementWidth, xtick+tickW);
							}
						}
						currY -= maptextoffset+fontSize-2;
						elementHeight = -currY+fontHeight;
						rect = new Rectangle(elementWidth, elementHeight);
					}
				}
				@Override
				public void translate(int dx, int dy) {
					Utils.translate(lbltxt.getOrigin(), dx, dy);
					ticks.forEach(t->Utils.translate(t.getOrigin(), dx, dy));
					segs.forEach(s->{
						Utils.translate(s.p0, dx, dy);
						Utils.translate(s.p1, dx, dy);
					});
					tris.getTriangleDetails().forEach(t->{
						Utils.translate(t.p0, dx, dy);
						Utils.translate(t.p1, dx, dy);
						Utils.translate(t.p2, dx, dy);
					});
					Utils.translate(rect, dx, dy);
				}

				@Override
				public Rectangle2D getSize() {
					return rect;
				}
			});
		}
		// layout
		layout = new RectangleLayout();
		for(LegendElement e:elements){
			LegendElement e_=e;
			layout.addComponent(()->{
				Rectangle2D size = e_.getSize();
				return new Dimension((int)size.getWidth(), (int)size.getHeight());
			}, 
					(x,y)->e_.translate(x, y)
					);
		}
		layoutH = layout.calculateFlowLayout(viewPortWidth-leftPadding, elementHSpace, elementVSpace)
				.flipYAxis(0)
				.translate(leftPadding, currentY)
				.apply()
				.getLayoutSize().height;
		currentY -= layoutH + (layoutH > 0 ? elementVSpace:0);
		elements.clear();


		// add renderables to delegate
		glyph2points.values().forEach(p->{
			delegate.addItemToRender(p);
		});
		pattern2lines.values().forEach(l->{
			delegate.addItemToRender(l);
		});
		texts.forEach(t->{
			delegate.addItemToRender(t);
		});
		triangles.forEach(t->{
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
			updateGL(false);
		}
		delegate.render(vpx, vpy, w, h);
	}

	@Override
	public void renderFallback(Graphics2D g, Graphics2D p, int w, int h) {
		if(!isEnabled()){
			return;
		}
		if(w == 0 || h == 0){
			return;
		}
		if(isDirty() || viewPortWidth != w || viewPortHeight != h){
			viewPortWidth = w;
			viewPortHeight = h;
			updateGL(false); // only clearGL requires GL context, but all GL resources are null, so no prob.
		}
		delegate.renderFallback(g, p, w, h);
	}

	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		if(!isEnabled()){
			return;
		}
		delegate.renderSVG(doc, parent, w, h);
	}

	@Override
	public void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h) {
		if(!isEnabled()){
			return;
		}
		delegate.renderPDF(doc, page, x, y, w, h);
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

	
	/**
	 * Class for arranging rectangles inspired by {@link java.awt.FlowLayout}.
	 * Used to layout Legend elements.
	 */
	private static class RectangleLayout {

		static abstract class LayoutComp {
			int x,y;
			abstract Dimension getSize();
			abstract void apply();
		}
		
		ArrayList<LayoutComp> comps = new ArrayList<>();
		int width, height;
		
		Dimension getLayoutSize(){
			return new Dimension(width, height);
		}
		
		RectangleLayout addComponent(Supplier<Dimension> dim, BiConsumer<Integer, Integer> setLocation){
			this.comps.add(new LayoutComp() {
				@Override
				Dimension getSize() {
					return dim.get();
				}
				
				@Override
				void apply() {
					setLocation.accept(x, y);
				}
			});
			return this;
		}
		
		RectangleLayout calculateFlowLayout(final int maxWidth, int hspace, int vspace){
			int currX=0, currY=0, lineHeight=0;
			this.width=this.height=0;
			for(int i=0; i<comps.size(); i++){
				LayoutComp c = comps.get(i);
				Dimension size = c.getSize();
				if(i==0){
					c.x=currX;
					c.y=currY;
					currX+=size.width+hspace;
					lineHeight = size.height;
					this.width = currX-hspace;
				} else {
					// enough space
					if(maxWidth-currX >= size.width){
						c.x=currX;
						c.y=currY;
						currX+=size.width+hspace;
						lineHeight = Math.max(lineHeight, size.height);
						this.width = Math.max(this.width, currX-hspace);
					// next line
					} else {
						currX=0;
						currY+=lineHeight+vspace;
						c.x=currX;
						c.y=currY;
						currX+=size.width+hspace;
						lineHeight = size.height;
						this.width = Math.max(this.width, currX-hspace);
					}
				}
			}
			this.height = currY+lineHeight;
			return this;
		}
		
		RectangleLayout flipYAxis(int height){
			comps.forEach(c->c.y=height-c.y);
			return this;
		}
		
		RectangleLayout translate(int dx, int dy){
			comps.forEach(c->{c.x+=dx;c.y+=dy;}); 
			return this;
		}
		
		RectangleLayout apply(){
			comps.forEach(LayoutComp::apply);
			return this;
		}
		
	}

	@Override
	public boolean isGLDoublePrecision() {
		return false;
	}
	
}
