package hageldave.jplotter.misc;

import java.awt.Graphics2D;
import java.util.List;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL31;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.renderers.PointsRenderer;

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
	 * A {@link Glyph} implementation has to guarantee for that.
	 */
	public boolean useElementsDrawCall();

	/**
	 * Creates SVG elements that represent this Glyph in an
	 * SVG context. This is used to create an SVG {@code symbol}.
	 * The {@link #isFilled()} method determines whether the "fill"
	 * or "stroke" attribute is used for coloring an instance of the symbol.
	 * @param doc to create the elements with.
	 * @return list of SVG elements.
	 */
	public List<Element> createSVGElements(Document doc);
	
	public void drawFallback(Graphics2D g, float scaling);
	
	/**
	 * @return name of this Glyph, used as part of an SVG identifier for
	 * the corresponding symbol definition.
	 */
	public String glyphName();
	
	/**
	 * @return whether this glyph is filled or not, e.g. a cross is not filled but a circle can.
	 */
	public boolean isFilled();
}
