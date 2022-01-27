package hageldave.jplotter.misc;

import hageldave.jplotter.gl.VertexArray;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.batik.ext.awt.geom.Polygon2D;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.lwjgl.opengl.GL11;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * The default implementations of various {@link Glyph}s such as a
 * {@link #CROSS}, {@link #CIRCLE}, {@link #SQUARE} and {@link #TRIANGLE} glyph.
 * Apart from these there are {@link #ARROW} and {@link #ARROWHEAD} glyphs that can be used for
 * directional information as in vector field visualizations.
 * 
 * @author hageldave
 */
public enum DefaultGlyph implements Glyph {
	/** a cross glyph, two diagonal lines */
	CROSS(DefaultGlyph::mkCross, 4, GL11.GL_LINES, 6, false, false, DefaultGlyph::mkCrossSVG, DefaultGlyph::mkCrossPDF, DefaultGlyph::drawCross),
	/** a square glyph */
	SQUARE(DefaultGlyph::mkSquare, 4, GL11.GL_LINE_LOOP, 6, false, false, DefaultGlyph::mkSquareSVG, DefaultGlyph::mkSquarePDF, DefaultGlyph::drawSquare),
	/** a filled square glyph */
	SQUARE_F(DefaultGlyph::mkSquareF, 4, GL11.GL_TRIANGLE_STRIP, 6, false, true, DefaultGlyph::mkSquareSVG, DefaultGlyph::mkSquarePDF, DefaultGlyph::drawSquareF),
	/** a triangle glyph */
	TRIANGLE(DefaultGlyph::mkTriangle, 3, GL11.GL_LINE_LOOP, 7, false, false, DefaultGlyph::mkTriangleSVG, DefaultGlyph::mkTrianglePDF, DefaultGlyph::drawTriangle),
	/** a filled triangle glyph */
	TRIANGLE_F(DefaultGlyph::mkTriangle, 3, GL11.GL_TRIANGLES, 7, false, true, DefaultGlyph::mkTriangleSVG, DefaultGlyph::mkTrianglePDF, DefaultGlyph::drawTriangleF),
	/** a circle glyph  (20 line segments) */
	CIRCLE(DefaultGlyph::mkCircle, 20, GL11.GL_LINE_LOOP, 8, true, false, DefaultGlyph::mkCircleSVG, DefaultGlyph::mkCirclePDF, DefaultGlyph::drawCircle),
	/** a filled circle glyph (20 line segments) */
	CIRCLE_F(DefaultGlyph::mkCircleWithCenter, 22, GL11.GL_TRIANGLE_FAN, 8, true, true, DefaultGlyph::mkCircleSVG, DefaultGlyph::mkCirclePDF, DefaultGlyph::drawCircleF),
	/** an arrow glyph, pointing to the right */
	ARROW(DefaultGlyph::mkArrow, 6, GL11.GL_LINES, 12, false, false, DefaultGlyph::mkArrowSVG, DefaultGlyph::mkArrowPDF, DefaultGlyph::drawArrow),
	/** an arrow head glyph, pointing to the right */
	ARROWHEAD(DefaultGlyph::mkArrowHead, 4, GL11.GL_LINE_LOOP, 12, false, false, DefaultGlyph::mkArrowHeadSVG, DefaultGlyph::mkArrowHeadPDF, DefaultGlyph::drawArrowHead),
	/** a filled arrow head glyph, pointing to the right */
	ARROWHEAD_F(DefaultGlyph::mkArrowHead, 4, GL11.GL_TRIANGLE_FAN, 12, false, true, DefaultGlyph::mkArrowHeadSVG, DefaultGlyph::mkArrowHeadPDF, DefaultGlyph::drawArrowHeadF),
	;
	
	private Consumer<VertexArray> vertexGenerator;
	private int numVertices;
	private int primitiveType;
	private int pixelSize;
	private boolean drawAsElements;
	private boolean isFilled;
	private BiFunction<Document,Integer,List<Element>> svgElementGenerator;
	private BiFunction<PDPageContentStream,Integer,PDPageContentStream> pdfElementGenerator;
	private Graphics2DDrawing fallbackDraw;
	
	private DefaultGlyph(Consumer<VertexArray> vertGen, int numVerts, int primType, int pixelSize, boolean elements, boolean isFilled, BiFunction<Document,Integer,List<Element>> svgGen, BiFunction<PDPageContentStream,Integer,PDPageContentStream> pdfGen, Graphics2DDrawing fallbackDraw) {
		this.vertexGenerator = vertGen;
		this.numVertices = numVerts;
		this.primitiveType = primType;
		this.pixelSize = pixelSize;
		this.drawAsElements = elements;
		this.isFilled = isFilled;
		this.svgElementGenerator = svgGen;
		this.pdfElementGenerator = pdfGen;
		this.fallbackDraw = fallbackDraw;
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
	public PDPageContentStream createPDFElement(PDPageContentStream contentStream) {
		return pdfElementGenerator.apply(contentStream, pixelSize);
	}


	@Override
	public boolean isFilled() {
		return isFilled;
	}
	
	@Override
	public String glyphName() {
		return name();
	}
	
	@Override
	public void drawFallback(Graphics2D g, float scaling) {
		this.fallbackDraw.draw(g, pixelSize, scaling);
	}
	
	
	private static final int numCircVerts = 20;
	private static final float[][] sincosLUT = ((Supplier<float[][]>)(()->{
		float[][] lut = new float[2][numCircVerts];
		for(int i=0; i<numCircVerts;i++){
			lut[0][i] = (float)Math.sin(i*2*Math.PI/(numCircVerts));
			lut[1][i] = (float)Math.cos(i*2*Math.PI/(numCircVerts));
		}
		return lut;
	})).get();
	
	static void mkCircle(VertexArray va){
		final int numVerts = numCircVerts;
		float[] verts = new float[numVerts*2];
		int[] indices = new int[numVerts];
		for(int i=0; i<numVerts;i++){
			verts[i*2+0] = sincosLUT[1][i]*0.5f;//cos
			verts[i*2+1] = sincosLUT[0][i]*0.5f;//sin
			indices[i] = i;
		}
		va.setBuffer(0, 2, verts);
		va.setIndices(indices);
	}
	
	
	static void drawCircle(Graphics2D g, int pixelSize, float scaling) {
		float[][] verts = new float[2][numCircVerts];
		for(int i=0; i<numCircVerts;i++){
			verts[0][i] = sincosLUT[1][i]*0.5f*pixelSize*scaling;
			verts[1][i] = sincosLUT[0][i]*0.5f*pixelSize*scaling;
		}
		g.draw(new Polygon2D(verts[0], verts[1], numCircVerts));
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

	static PDPageContentStream mkCirclePDF(PDPageContentStream contentStream, Integer pixelSize) {
		try {
			PDFUtils.createPDFPoint(contentStream, 0,0, pixelSize/2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentStream;
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
	
	static void drawCircleF(Graphics2D g, int pixelSize, float scaling) {
		float[][] verts = new float[2][numCircVerts];
		for(int i=0; i<numCircVerts;i++){
			verts[0][i] = sincosLUT[1][i]*0.5f*pixelSize*scaling;
			verts[1][i] = sincosLUT[0][i]*0.5f*pixelSize*scaling;
		}
		g.fill(new Polygon2D(verts[0], verts[1], numCircVerts));
	}
	
	static void mkSquare(VertexArray va){
		va.setBuffer(0, 2,   -.5f,-.5f,  .5f,-.5f,  .5f,.5f, -.5f,.5f);
	}
	
	static void drawSquare(Graphics2D g, int pixelSize, float scaling) {
		float size = pixelSize*scaling;
		g.draw(new Rectangle2D.Double(-.5*size, -.5*size, size, size));
	}
	
	static List<Element> mkSquareSVG(Document doc, Integer pixelSize){
		return Arrays.asList(
				SVGUtils.createSVGRect(doc, -.5*pixelSize, -.5*pixelSize, pixelSize, pixelSize));
	}

	static PDPageContentStream mkSquarePDF(PDPageContentStream contentStream, Integer pixelSize) {
		try {
			contentStream.addRect((float) -.5*pixelSize, (float) -.5*pixelSize, pixelSize, pixelSize);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentStream;
	}

	static void mkSquareF(VertexArray va){
		va.setBuffer(0, 2,   -.5f,-.5f,  .5f,-.5f,  -.5f,.5f, .5f,.5f);
	}
	
	static void drawSquareF(Graphics2D g, int pixelSize, float scaling) {
		float size = pixelSize*scaling;
		g.fill(new Rectangle2D.Float(-.5f*size, -.5f*size, size, size));
	}
	
	static void mkArrow(VertexArray va){
		va.setBuffer(0, 2,  -.5f,0f,  .5f,0f,  .1f,-.2f, .5f,0f, .1f,.2f, .5f,0f);
	}
	
	static void drawArrow(Graphics2D g, int pixelSize, float scaling) {
		float size = pixelSize*scaling;
		g.draw(new Line2D.Float(-.5f*size, 0f      , .5f*size, 0f));
		g.draw(new Line2D.Float( .1f*size, .2f*size, .5f*size, 0f));
		g.draw(new Line2D.Float( .1f*size,-.2f*size, .5f*size, 0f));
	}
	
	static List<Element> mkArrowSVG(Document doc, Integer pixelSize){
		Element l1 = SVGUtils.createSVGElement(doc, "polyline");
		Element l2 = SVGUtils.createSVGElement(doc, "polyline");
		l1.setAttributeNS(null, "points", SVGUtils.svgPoints(-.5f*pixelSize,0,  .5f*pixelSize,0));
		l2.setAttributeNS(null, "points", SVGUtils.svgPoints(.1f*pixelSize,.2f*pixelSize,  .5f*pixelSize,0, .1f*pixelSize,-.2f*pixelSize));
		return Arrays.asList(l1,l2);
	}

	static PDPageContentStream mkArrowPDF(PDPageContentStream contentStream, Integer pixelSize) {
		try {
			PDFUtils.createPDFSegment(contentStream, new Point2D.Double(-.5f*pixelSize,0), new Point2D.Double(.5f*pixelSize,0));
			PDFUtils.createPDFSegment(contentStream, new Point2D.Double(.1f*pixelSize,.2f*pixelSize), new Point2D.Double(.5f*pixelSize,0));
			PDFUtils.createPDFSegment(contentStream, new Point2D.Double(.5f*pixelSize,0), new Point2D.Double(.1f*pixelSize,-.2f*pixelSize));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentStream;
	}
	
	static void mkArrowHead(VertexArray va){
		va.setBuffer(0, 2,  -.2f,0f,  -.5f,-.3f,  .5f,0f, -.5f,.3f);
	}
	
	static void drawArrowHead(Graphics2D g, int pixelSize, float scaling) {
		float size = pixelSize*scaling;
		g.draw(new Polygon2D(
				new float[]{-.2f*size, -.5f*size, .5f*size, -.5f*size}, 
				new float[]{  0f*size, -.3f*size,  0f*size,  .3f*size}, 
				4));
	}
	
	static void drawArrowHeadF(Graphics2D g, int pixelSize, float scaling) {
		float size = pixelSize*scaling;
		g.fill(new Polygon2D(
				new float[]{-.2f*size, -.5f*size, .5f*size, -.5f*size}, 
				new float[]{  0f*size, -.3f*size,  0f*size,  .3f*size}, 
				4));
	}
	
	static List<Element> mkArrowHeadSVG(Document doc, Integer pixelSize){
		Element line = SVGUtils.createSVGElement(doc, "polygon");
		line.setAttributeNS(null, "points", SVGUtils.svgPoints(
				-.2f*pixelSize,0,  
				-.5f*pixelSize,-.3*pixelSize,
				.5f*pixelSize,0,
				-.5f*pixelSize, .3*pixelSize));
		return Arrays.asList(line);
	}

	static PDPageContentStream mkArrowHeadPDF(PDPageContentStream contentStream, Integer pixelSize) {
		try {
			PDFUtils.createPDFPolygon(contentStream, new double[]{-.2f*pixelSize, -.5f*pixelSize, .5f*pixelSize, -.5f*pixelSize},
					new double[]{  0f*pixelSize, -.3f*pixelSize,  0f*pixelSize, .3f*pixelSize});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentStream;
	}

	static void mkCross(VertexArray va){
		va.setBuffer(0, 2,  -.5f,-.5f,  .5f,.5f,  .5f,-.5f,  -.5f,.5f);
	}
	
	static void drawCross(Graphics2D g, int pixelSize, float scaling) {
		float size = pixelSize*scaling*.5f;
		g.draw(new Line2D.Float(-size, -size, size, size));
		g.draw(new Line2D.Float( size, -size,-size, size));
	}
	
	static List<Element> mkCrossSVG(Document doc, Integer pixelSize){
		Element l1 = SVGUtils.createSVGElement(doc, "polyline");
		Element l2 = SVGUtils.createSVGElement(doc, "polyline");
		l1.setAttributeNS(null, "points", SVGUtils.svgPoints(-.5f*pixelSize,-.5f*pixelSize,  .5f*pixelSize,.5f*pixelSize));
		l2.setAttributeNS(null, "points", SVGUtils.svgPoints(.5f*pixelSize,-.5f*pixelSize,  -.5f*pixelSize,.5f*pixelSize));
		return Arrays.asList(l1,l2);
	}

	static PDPageContentStream mkCrossPDF(PDPageContentStream contentStream, Integer pixelSize) {
		try {
			PDFUtils.createPDFSegment(contentStream,
					new Point2D.Double(-.5f*pixelSize,-.5f*pixelSize), new Point2D.Double(.5f*pixelSize,.5f*pixelSize));
			PDFUtils.createPDFSegment(contentStream,
					new Point2D.Double(.5f*pixelSize,-.5f*pixelSize), new Point2D.Double(-.5f*pixelSize,.5f*pixelSize));
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentStream;
	}
	
	static void mkTriangle(VertexArray va){
		va.setBuffer(0, 2,  -.5f,-.5f,  .5f,-.5f, 0f,.5f);
	}
	
	static void drawTriangle(Graphics2D g, int pixelSize, float scaling) {
		float size = pixelSize*scaling*.5f;
		g.draw(new Polygon2D(
				new float[] {-size, size, 0   }, 
				new float[] {-size,-size, size},
				3));
	}
	
	static void drawTriangleF(Graphics2D g, int pixelSize, float scaling) {
		float size = pixelSize*scaling*.5f;
		g.fill(new Polygon2D(
				new float[] {-size, size, 0   }, 
				new float[] {-size,-size, size},
				3));
	}
	
	static List<Element> mkTriangleSVG(Document doc, Integer pixelSize){
		return Arrays.asList(
				SVGUtils.createSVGTriangle(doc, -.5*pixelSize, -.5*pixelSize, .5*pixelSize, -.5*pixelSize, 0, .5*pixelSize));
	}

	static PDPageContentStream mkTrianglePDF(PDPageContentStream contentStream, Integer pixelSize) {
		float size = pixelSize*.5f;
		try {
			PDFUtils.createPDFPolygon(contentStream, new double[] {-size, size, 0},
					new double[] {-size,-size, size});
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contentStream;
	}
	
	private static interface Graphics2DDrawing {
		public void draw(Graphics2D g, int pixelSize, float scaling);
	}
	
}
