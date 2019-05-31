package hageldave.jplotter.renderables;

import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;

import hageldave.jplotter.Annotations.GLContextRequired;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Pair;

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
	
	public Legend setDirty() {
		this.isDirty = true;
		return this;
	}
	
	public Legend addGlyphLabel(Glyph glyph, Color color, String labeltxt){
		glyphLabels.add(Pair.of(labeltxt, Pair.of(glyph, color)));
		return setDirty();
	}
	
	public Legend addLineLabel(double thickness, Color color, String labeltxt){
		this.lineLabels.add(Pair.of(labeltxt, Pair.of(thickness, color)));
		return setDirty();
	}
	
	@Override
	public void initGL() {
		// will initialize renderables in updateGL
	}

	@Override
	public boolean isDirty() {
		return isDirty;
	}

	@Override
	public void updateGL() {
		clearGL();
		// do layout
		int leftPadding = 4;
		int fontStyle = Font.PLAIN;
		int fontSize = 10;
		int fontHeight = CharacterAtlas.boundsForText(1, fontSize, fontSize, true).getBounds().height;
		int maxTextWidth = glyphLabels.stream()
		.map(p->CharacterAtlas.boundsForText(p.first.length(), fontSize, fontSize, true).getBounds().width)
		.mapToInt(i->i)
		.max()
		.orElseGet(()->0);
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
			points.addPoint(currentX+5, currentY+fontHeight/2+1, color);
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

}
