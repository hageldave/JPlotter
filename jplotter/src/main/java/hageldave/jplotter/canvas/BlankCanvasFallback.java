package hageldave.jplotter.canvas;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.ImageObserver;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import org.apache.batik.svggen.SVGGraphics2D;

import hageldave.imagingkit.core.Img;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.util.Utils;

public class BlankCanvasFallback extends JComponent implements JPlotterCanvas {
	private static final long serialVersionUID = 1L;
	private static final ImageObserver obs_allbits = Utils.imageObserver(ImageObserver.ALLBITS);

	protected AtomicBoolean repaintIsSheduled = new AtomicBoolean(false);
	protected Img mainRenderBuffer = new Img(0,0);
	protected Img pickingRenderBuffer = new Img(0,0);
	protected Img displayBuffer = new Img(0,0);
	protected Renderer renderer;
	protected boolean isRenderSvgAsImage = false;
	
	public BlankCanvasFallback() {
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				repaint();
			}
		});
	}

	@Override
	public BlankCanvasFallback setRenderer(Renderer renderer) {
		this.renderer = renderer;
		return this;
	}

	@Override
	public Renderer getRenderer() {
		return renderer;
	}

	@Override
	public void scheduleRepaint() {
		if(repaintIsSheduled.compareAndSet(false, true)){
			SwingUtilities.invokeLater(this::repaint);
		}
	}

	@Override
	public void repaint() {
		if(SwingUtilities.isEventDispatchThread()){
			repaintIsSheduled.set(false);
			render();
			super.repaint();
		} else {
			scheduleRepaint();
		}
	}

	protected void render() {
		int w=getWidth(); int h=getHeight();
		if(mainRenderBuffer.getWidth()!=w || mainRenderBuffer.getHeight()!=h) {
			mainRenderBuffer = new Img(w, h);
			pickingRenderBuffer = new Img(w, h);
		}
		if(w==0 && h==0)
			return;
		// clear / fill with clear color
		mainRenderBuffer.fill(getBackground().getRGB());
		pickingRenderBuffer.fill(0xff000000);
		// setup render graphics
		Graphics2D g=null,p=null;
		try {
			g=mainRenderBuffer.createGraphics();
			
			p=pickingRenderBuffer.createGraphics();
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_GASP);
			
			p.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			p.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_NORMALIZE);
			p.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
			
			g.translate(0, h);
			g.scale(1.0, -1.0);
			p.translate(0, h);
			p.scale(1.0, -1.0);
			render(g,p, w,h);
		} finally {
			if(g!=null)g.dispose();
			if(p!=null)p.dispose();
		}
	}

	protected void render(Graphics2D g, Graphics2D p, int w, int h) {
		if(renderer != null)
			renderer.renderFallback(g, p, w, h);
	}
	
	

	@Override
	public void paint(Graphics g) {
		// test if this is SVG painting
		if(g instanceof SVGGraphics2D && !isSvgAsImageRenderingEnabled()){
			return;
		}
		
		g.clearRect(0, 0, getWidth(), getHeight());
		int w=mainRenderBuffer.getWidth();
		int h=mainRenderBuffer.getHeight();
		if(w>0&&h>0) {
			g.drawImage(mainRenderBuffer.getRemoteBufferedImage(), 
					0, 0, getWidth(), getHeight(), 
					0, 0, w, h, 
					obs_allbits);
		}
	}

	@Override
	public void enableSvgAsImageRendering(boolean enable){
		this.isRenderSvgAsImage = enable;
	}
	
	@Override
	public boolean isSvgAsImageRenderingEnabled(){
		return isRenderSvgAsImage;
	}

	@Override
	public Img toImg() {
		return mainRenderBuffer.copy();
	}

	@Override
	public int getPixel(int x, int y, boolean picking, int areaSize) {
		Img img = picking ? pickingRenderBuffer:mainRenderBuffer;
		Img area = new Img(areaSize, areaSize);
		area.forEach(px->{
			int v = img.getValue(x+px.getX()-areaSize/2, y+px.getY()-areaSize/2, 0);
			px.setValue(v);
		});
		int[] colors = area.getData();
		return JPlotterCanvas.mostProminentColor(colors, areaSize);
	}
	
	@Override
	public BlankCanvasFallback asComponent() {
		return this;
	}

}
