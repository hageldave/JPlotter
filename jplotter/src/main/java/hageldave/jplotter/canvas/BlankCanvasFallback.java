package hageldave.jplotter.canvas;

import java.awt.Canvas;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

import hageldave.imagingkit.core.Img;
import hageldave.jplotter.renderers.Renderer;

public class BlankCanvasFallback extends Canvas implements JPlotterCanvas {
	private static final long serialVersionUID = 1L;

	protected AtomicBoolean repaintIsSheduled = new AtomicBoolean(false);
	protected Img mainRenderBuffer = new Img(0,0);
	protected Img pickingRenderBuffer = new Img(0,0);
	protected Renderer renderer;
	
	public BlankCanvasFallback() {
		this.addComponentListener(new ComponentAdapter() {
			@Override
			public void componentResized(ComponentEvent e) {
				repaint();
			}
		});
	}

	/**
	 * Sets the renderer of this canvas.
	 * @param renderer to draw contents.
	 * @return this for chaining
	 */
	public BlankCanvasFallback setRenderer(Renderer renderer) {
		this.renderer = renderer;
		return this;
	}

	/**
	 * @return the current renderer
	 */
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
		// clear / fill with clear color
		mainRenderBuffer.fill(getBackground().getRGB());
		pickingRenderBuffer.fill(0xff000000);
		// setup render graphics
		Graphics2D g=null,p=null;
		try {
			g=mainRenderBuffer.createGraphics();
			p=pickingRenderBuffer.createGraphics();
			g.translate(0, h);
			g.scale(1.0, -1.0);
			render(g,p, w,h);
		} finally {
			if(g!=null)g.dispose();
			if(p!=null)p.dispose();
		}
	}

	protected void render(Graphics2D g, Graphics2D p, int w, int h) {
		renderer.renderFallback(g, p, w, h);
	}
	
	

	@Override
	public void paint(Graphics g) {
		int w=mainRenderBuffer.getWidth();
		int h=mainRenderBuffer.getHeight();
		if(w>0&&h>0) {
			g.drawImage(mainRenderBuffer.getRemoteBufferedImage(), 
					0, 0, w, h, 
					0, 0, w, h, 
					null);
		}
	}

	@Override
	public void enableSvgAsImageRendering(boolean enable) {
		// TODO Auto-generated method stub

	}

	@Override
	public boolean isSvgAsImageRenderingEnabled() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public Img toImg() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public int getPixel(int x, int y, boolean picking, int areaSize) {
		// TODO Auto-generated method stub
		return 0;
	}

}
