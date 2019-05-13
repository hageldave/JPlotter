package jplotter.globjects;

import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;

public enum DefaultGlyph implements Glyph {
	CIRCLE(DefaultGlyph::mkCircle, 10, GL11.GL_LINE_LOOP),
	SQUARE(DefaultGlyph::mkSquare, 4, GL11.GL_LINE_LOOP),
	;
	
	private Consumer<VertexArray> vertexGenerator;
	private int numVertices;
	private int primitiveType;
	
	private DefaultGlyph(Consumer<VertexArray> vertGen, int numVerts, int primType) {
		this.vertexGenerator = vertGen;
		this.numVertices = numVerts;
		this.primitiveType = primType;
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
	
	static void mkCircle(VertexArray va){
		float[] verts = new float[10*2];
		int[] indices = new int[10];
		for(int i=0; i<10;i++){
			verts[i*2+0] = (float)Math.cos(i*2*Math.PI/10)*0.5f;
			verts[i*2+1] = (float)Math.sin(i*2*Math.PI/10)*0.5f;
			indices[i] = i;
		}
		va.setBuffer(0, 2, verts);
		va.setIndices(indices);
	}
	
	static void mkSquare(VertexArray va){
		va.setBuffer(0, 2, -.5f,-.5f,  .5f,-.5f,  .5f,.5f, -.5f,.5f);
		va.setIndices(0,1,2,3);
	}
	
	
}
