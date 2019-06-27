package hageldave.jplotter.misc;

import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import org.lwjgl.opengl.GL11;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.svg.SVGUtils;

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
	CROSS(DefaultGlyph::mkCross, 4, GL11.GL_LINES, 6, false, false, DefaultGlyph::mkCrossSVG),
	/** a square glyph */
	SQUARE(DefaultGlyph::mkSquare, 4, GL11.GL_LINE_LOOP, 6, false, false, DefaultGlyph::mkSquareSVG),
	/** a filled square glyph */
	SQUARE_F(DefaultGlyph::mkSquareF, 4, GL11.GL_TRIANGLE_STRIP, 6, false, true, DefaultGlyph::mkSquareSVG),
	/** a triangle glyph */
	TRIANGLE(DefaultGlyph::mkTriangle, 3, GL11.GL_LINE_LOOP, 7, false, false, DefaultGlyph::mkTriangleSVG),
	/** a filled triangle glyph */
	TRIANGLE_F(DefaultGlyph::mkTriangle, 3, GL11.GL_TRIANGLES, 7, false, true, DefaultGlyph::mkTriangleSVG),
	/** a circle glyph  (20 line segments) */
	CIRCLE(DefaultGlyph::mkCircle, 20, GL11.GL_LINE_LOOP, 8, true, false, DefaultGlyph::mkCircleSVG),
	/** a filled circle glyph (20 line segments) */
	CIRCLE_F(DefaultGlyph::mkCircleWithCenter, 22, GL11.GL_TRIANGLE_FAN, 8, true, true, DefaultGlyph::mkCircleSVG),
	/** an arrow glyph, pointing to the right */
	ARROW(DefaultGlyph::mkArrow, 6, GL11.GL_LINES, 12, false, false, DefaultGlyph::mkArrowSVG),
	;
	
	private Consumer<VertexArray> vertexGenerator;
	private int numVertices;
	private int primitiveType;
	private int pixelSize;
	private boolean drawAsElements;
	private boolean isFilled;
	private BiFunction<Document,Integer,List<Element>> svgElementGenerator;
	
	private DefaultGlyph(Consumer<VertexArray> vertGen, int numVerts, int primType, int pixelSize, boolean elements, boolean isFilled, BiFunction<Document,Integer,List<Element>> svgGen) {
		this.vertexGenerator = vertGen;
		this.numVertices = numVerts;
		this.primitiveType = primType;
		this.pixelSize = pixelSize;
		this.drawAsElements = elements;
		this.isFilled = isFilled;
		this.svgElementGenerator = svgGen;
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
	
	@Override
	public List<Element> createSVGElements(Document doc) {
		return svgElementGenerator.apply(doc, pixelSize);
	}
	
	@Override
	public boolean isFilled() {
		return isFilled;
	}
	
	@Override
	public String glyphName() {
		return name();
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
	
	static List<Element> mkCircleSVG(Document doc, Integer pixelSize){
		final int numVerts = 20;
		float[] verts = new float[numVerts*2];
		for(int i=0; i<numVerts;i++){
			verts[i*2+0] = (float)Math.cos(i*2*Math.PI/numVerts)*0.5f;
			verts[i*2+1] = (float)Math.sin(i*2*Math.PI/numVerts)*0.5f;
		}
		String points = "";
		for(int i=0; i<numVerts;i++){
			points += SVGUtils.svgNumber(verts[i*2+0]*pixelSize+pixelSize);
			points += ",";
			points += SVGUtils.svgNumber(verts[i*2+1]*pixelSize+pixelSize);
			if(i < numVerts*2-1)
				points += " ";
		}
		Element element = SVGUtils.createSVGElement(doc, "polygon");
		element.setAttributeNS(null, "points", points);
		element.setAttributeNS(null, "transform", "translate(-"+pixelSize+",-"+pixelSize+")");
		return Arrays.asList(element);
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
	
	static List<Element> mkSquareSVG(Document doc, Integer pixelSize){
		return Arrays.asList(
				SVGUtils.createSVGRect(doc, -.5*pixelSize, -.5*pixelSize, pixelSize, pixelSize));
	}
	
	static void mkSquareF(VertexArray va){
		va.setBuffer(0, 2,   -.5f,-.5f,  .5f,-.5f,  -.5f,.5f, .5f,.5f);
	}
	
	static void mkArrow(VertexArray va){
		va.setBuffer(0, 2,  -.5f,0f,  .5f,0f,  .1f,-.2f, .5,0, .1f,.2f, .5f,0f);
	}
	
	static List<Element> mkArrowSVG(Document doc, Integer pixelSize){
		Element l1 = SVGUtils.createSVGElement(doc, "polyline");
		Element l2 = SVGUtils.createSVGElement(doc, "polyline");
		l1.setAttributeNS(null, "points", SVGUtils.svgPoints(-.5f*pixelSize,0,  .5f*pixelSize,0));
		l2.setAttributeNS(null, "points", SVGUtils.svgPoints(.1f*pixelSize,.2f*pixelSize,  .5f*pixelSize,0, .1f*pixelSize,-.2f*pixelSize));
		return Arrays.asList(l1,l2);
	}
	
	static void mkCross(VertexArray va){
		va.setBuffer(0, 2,  -.5f,-.5f,  .5f,.5f,  .5f,-.5f,  -.5f,.5f);
	}
	
	static List<Element> mkCrossSVG(Document doc, Integer pixelSize){
		Element l1 = SVGUtils.createSVGElement(doc, "polyline");
		Element l2 = SVGUtils.createSVGElement(doc, "polyline");
		l1.setAttributeNS(null, "points", SVGUtils.svgPoints(-.5f*pixelSize,-.5f*pixelSize,  .5f*pixelSize,.5f*pixelSize));
		l2.setAttributeNS(null, "points", SVGUtils.svgPoints(.5f*pixelSize,-.5f*pixelSize,  -.5f*pixelSize,.5f*pixelSize));
		return Arrays.asList(l1,l2);
	}
	
	static void mkTriangle(VertexArray va){
		va.setBuffer(0, 2,  -.5,-.5,  .5,-.5, 0,.5);
	}
	
	static List<Element> mkTriangleSVG(Document doc, Integer pixelSize){
		return Arrays.asList(
				SVGUtils.createSVGTriangle(doc, -.5*pixelSize, -.5*pixelSize, .5*pixelSize, -.5*pixelSize, 0, .5*pixelSize));
	}
	
}
