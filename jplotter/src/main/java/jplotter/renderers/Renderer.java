package jplotter.renderers;

public interface Renderer extends AutoCloseable {

	public void glInit();
	
	public void render(int w, int h);
	
	public void close();
	
	public default boolean drawsPicking(){return false;}
	
}
