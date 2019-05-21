package jplotter.renderables;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;

import jplotter.globjects.VertexArray;

/**
 * The default implementations of various {@link Glyph}s such as a
 * {@link #CROSS}, {@link #CIRCLE}, {@link #SQUARE} and {@link #TRIANGLE} glyph.
 * Apart from these there is an {@link #ARROW} glyph that can be used for
 * vector field visualizations.
 * 
 * @author hageldave
 */
public enum DefaultGlyph implements Glyph {
	/** a cross glyph, two diagonal lines */
	CROSS(DefaultGlyph::mkCross, 4, GL11.GL_LINES, 6, false),
	/** a square glyph */
	SQUARE(DefaultGlyph::mkSquare, 4, GL11.GL_LINE_LOOP, 6, false),
	/** a filled square glyph */
	SQUARE_F(DefaultGlyph::mkSquareF, 4, GL11.GL_TRIANGLE_STRIP, 6, false),
	/** a triangle glyph */
	TRIANGLE(DefaultGlyph::mkTriangle, 3, GL11.GL_LINE_LOOP, 7, false),
	/** a filled triangle glyph */
	TRIANGLE_F(DefaultGlyph::mkTriangle, 3, GL11.GL_TRIANGLES, 7, false),
	/** a circle glyph  (20 line segments) */
	CIRCLE(DefaultGlyph::mkCircle, 20, GL11.GL_LINE_LOOP, 8, true),
	/** a filled circle glyph (20 line segments) */
	CIRCLE_F(DefaultGlyph::mkCircleWithCenter, 22, GL11.GL_TRIANGLE_FAN, 8, true),
	/** an arrow glyph, pointing to the right */
	ARROW(DefaultGlyph::mkArrow, 6, GL11.GL_LINES, 12, false),
	;
	
	private Consumer<VertexArray> vertexGenerator;
	private int numVertices;
	private int primitiveType;
	private int pixelSize;
	private boolean drawAsElements;
	
	private DefaultGlyph(Consumer<VertexArray> vertGen, int numVerts, int primType, int pixelSize, boolean elements) {
		this.vertexGenerator = vertGen;
		this.numVertices = numVerts;
		this.primitiveType = primType;
		this.pixelSize = pixelSize;
		this.drawAsElements = elements;
	}

	@Override
	public void fillVertexArray(VertexArray va) {
		vertexGenerator.accept(va);
	}

	@Override
	public int numVertices() {
		return numVertices;
	}

	@Override
	public int primitiveType() {
		return primitiveType;
	}
	
	@Override
	public int pixelSize() {
		return pixelSize;
	}
	
	@Override
	public boolean useElementsDrawCall() {
		return drawAsElements;
	}
	
	static void mkCircle(VertexArray va){
		final int numVerts = 20;
		float[] verts = new float[numVerts*2];
		int[] indices = new int[numVerts];
		for(int i=0; i<numVerts;i++){
			verts[i*2+0] = (float)Math.cos(i*2*Math.PI/numVerts)*0.5f;
			verts[i*2+1] = (float)Math.sin(i*2*Math.PI/numVerts)*0.5f;
			indices[i] = i;
		}
		va.setBuffer(0, 2, verts);
		va.setIndices(indices);
	}
	
	static void mkCircleWithCenter(VertexArray va){
		final int numVerts = 22;
		float[] verts = new float[numVerts*2];
		int[] indices = new int[numVerts];
		for(int i=1; i<numVerts;i++){
			verts[i*2+0] = (float)Math.cos((i-1)*2*Math.PI/(numVerts-2))*0.5f;
			verts[i*2+1] = (float)Math.sin((i-1)*2*Math.PI/(numVerts-2))*0.5f;
			indices[i] = i;
		}
		verts[numVerts*2-2] = verts[2];
		verts[numVerts*2-1] = verts[3];
		
		va.setBuffer(0, 2, verts);
		va.setIndices(indices);
	}
	
	static void mkSquare(VertexArray va){
		va.setBuffer(0, 2,   -.5f,-.5f,  .5f,-.5f,  .5f,.5f, -.5f,.5f);
	}
	
	static void mkSquareF(VertexArray va){
		va.setBuffer(0, 2,   -.5f,-.5f,  .5f,-.5f,  -.5f,.5f, .5f,.5f);
	}
	
	static void mkArrow(VertexArray va){
		va.setBuffer(0, 2,  -.5f,0f,  .5f,0f,  .1f,-.2f, .5,0, .1f,.2f, .5f,0f);
	}
	
	static void mkCross(VertexArray va){
		va.setBuffer(0, 2,  -.5f,-.5f,  .5f,.5f,  .5f,-.5f,  -.5f,.5f);
	}
	
	static void mkTriangle(VertexArray va){
		va.setBuffer(0, 2,  -.5,-.5,  .5,-.5, 0,.5);
	}
	
}
