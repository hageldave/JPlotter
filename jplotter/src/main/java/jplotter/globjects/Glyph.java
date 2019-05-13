package jplotter.globjects;

public interface Glyph {

	public void fillVertexArray(VertexArray va);
	
	public int numVertices();
	
	public int primitiveType();

	public int pixelSize();
	
	public boolean useElementsDrawCall();
}
