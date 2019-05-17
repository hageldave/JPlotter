package jplotter.globjects;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;

import jplotter.renderers.PointsRenderer;

/**
 * The Glyph interface has to be implemented by a class that realizes
 * a graphical representation of a 2D point (e.g. a cross or a circle).
 * The interface declares the {@link #fillVertexArray(VertexArray)} method 
 * that fills a vertex array so that it contains the vertices for the 
 * shape of the glyph.
 * The other methods describe how the glyph has to be drawn
 * by the {@link PointsRenderer}.
 * 
 * @author hageldave
 */
public interface Glyph {

	/**
	 * Fills the the first attribute (idx=0) of the specified vertex array
	 * with the vertices that make up the shape of this glyph.
	 * If necessary the ELEMENT_ARRAY_BUFFER (indices) of the VA can also be
	 * written to.
	 * The glyphs vertices should be centered around the origin (0,0), 
	 * it is also recommended that the vertices are within unit object coordinates
	 * i.e. {@code (x,y) in [-0.5, 0.5]}.
	 * 
	 * @param va the VertexArray to fill
	 */
	public void fillVertexArray(VertexArray va);
	
	/**
	 * @return number of vertices this glyph has.
	 */
	public int numVertices();
	
	/**
	 * @return the GL primitive to be used for drawing.
	 * For example {@link GL11#GL_LINES} or {@link GL11#GL_TRIANGLE_STRIP}
	 */
	public int primitiveType();

	/**
	 * @return the size in pixels this glyph should be scaled to.
	 */
	public int pixelSize();
	
	/**
	 * @return true if an elements draw call is to be used e.g. 
	 * {@link GL31#glDrawElementsInstanced(int, int, int, long, int)},
	 * false if an arrays draw call is to be used e.g.
	 * {@link GL31#glDrawArraysInstanced(int, int, int, int)}.<br>
	 * Of course an elements draw call can only be used if the VertexArray 
	 * has indices, i.e. the ELEMENT_ARRAY_BUFFER is set.
	 */
	public boolean useElementsDrawCall();
}
