package hageldave.jplotter.renderers;

import hageldave.jplotter.color.ColorOperations;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderables.Triangles;
import hageldave.jplotter.renderables.Triangles.TriangleDetails;
import hageldave.jplotter.svg.SVGTriangleRendering;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.BarycentricGradientPaint;
import hageldave.jplotter.util.ShaderRegistry;
import hageldave.jplotter.util.Utils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Objects;

/**
 * The TrianglesRenderer is an implementation of the {@link GenericRenderer}
 * for {@link Triangles}.
 * <br>
 * Its fragment shader draws the picking color into the second render buffer
 * alongside the 'visible' color that is drawn into the first render buffer.
 * 
 * @author hageldave
 */
public class TrianglesRenderer extends GenericRenderer<Triangles> {
	
	protected static final char NL = '\n';
	protected static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in uvec2 in_colors;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform vec4 viewTransform;"
			+ NL + "out vec4 vColor;"
			+ NL + "out vec4 vPickColor;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"

			+ NL + "void main() {"
			+ NL + "   vec3 pos = vec3(in_position,1);"
			+ NL + "   pos = pos - vec3(viewTransform.xy,0);"
			+ NL + "   pos = pos * vec3(viewTransform.zw,1);"
			+ NL + "   gl_Position = projMX*vec4(pos,1);"
			+ NL + "   vColor = unpackARGB(in_colors.x);"
			+ NL + "   vPickColor = unpackARGB(in_colors.y);"
			+ NL + "}"
			+ NL
			;
	protected static final String fragmentShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) out vec4 frag_color;"
			+ NL + "layout(location = 1) out vec4 pick_color;"
			+ NL + "in vec4 vColor;"
			+ NL + "in vec4 vPickColor;"
			+ NL + "uniform float alphaMultiplier;"
			+ NL + "void main() {"
			+ NL + "   frag_color = vec4(vColor.rgb, vColor.a*alphaMultiplier);"
			+ NL + "   pick_color = vPickColor;"
			+ NL + "}"
			+ NL
			;
	
	protected String svgTriangleStrategy=null;

	/**
	 * Creates the shader if not already created and 
	 * calls {@link Renderable#initGL()} for all items 
	 * already contained in this renderer.
	 * Items that are added later on will be initialized during rendering.
	 */
	@Override
	@GLContextRequired
	public void glInit() {
		if(Objects.isNull(shader)){
			shader = ShaderRegistry.getOrCreateShader(this.getClass().getName(),()->new Shader(vertexShaderSrc, fragmentShaderSrc));
			itemsToRender.forEach(Renderable::initGL);
		}
	}

	/** 
	 * Disposes of GL resources, i.e. closes the shader.
	 * It also deletes (closes) all {@link Triangles} contained in this
	 * renderer.
	 */
	@Override
	@GLContextRequired
	public void close() {
		if(Objects.nonNull(shader)){
			ShaderRegistry.handbackShader(shader);
			shader = null;
		}
		closeAllItems();
	}

	/**
	 * Disables {@link GL11#GL_DEPTH_TEST},
	 * enables {@link GL11#GL_BLEND}
	 * and sets {@link GL11#GL_SRC_ALPHA}, {@link GL11#GL_ONE_MINUS_SRC_ALPHA}
	 * as blend function.
	 */
	@Override
	@GLContextRequired
	protected void renderStart(int w, int h) {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();
		int loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewTransform");
		GL20.glUniform4f(loc, (float)translateX, (float)translateY, (float)scaleX, (float)scaleY);
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
	}

	@Override
	@GLContextRequired
	protected void renderItem(Triangles item) {
		if(item.numTriangles() < 1){
			return;
		}
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "alphaMultiplier");
		GL20.glUniform1f(loc, item.getGlobalAlphaMultiplier());
		// draw things
		item.bindVertexArray();
		GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, item.numTriangles()*3);
		item.releaseVertexArray();
	}

	/**
	 * disables {@link GL11#GL_BLEND},
	 * enables {@link GL11#GL_DEPTH_TEST}
	 */
	@Override
	@GLContextRequired
	protected void renderEnd() {
		GL11.glDisable(GL11.GL_BLEND);
		GL11.glEnable(GL11.GL_DEPTH_TEST);
	}
	
	@Override
	public void renderFallback(Graphics2D g, Graphics2D p, int w, int h) {
		if(!isEnabled()){
			return;
		}
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
		
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();
		
		Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);
		
		float[][] tricoords = new float[2][3];
		for(Triangles tris : getItemsToRender()){
			if(tris.isHidden()){
				continue;
			}
			if(tris.isAAinFallbackEnabled()) g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			else g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			
			for(TriangleDetails tri : tris.getIntersectingTriangles(view != null ? view:viewportRect)){
				double x0,y0, x1,y1, x2,y2;
				x0=tri.p0.getX(); y0=tri.p0.getY(); x1=tri.p1.getX(); y1=tri.p1.getY(); x2=tri.p2.getX(); y2=tri.p2.getY();
				
				x0-=translateX; x1-=translateX; x2-=translateX;
				y0-=translateY; y1-=translateY; y2-=translateY;
				x0*=scaleX; x1*=scaleX; x2*=scaleX;
				y0*=scaleY; y1*=scaleY; y2*=scaleY;
				
				tricoords[0][0]=(float)x0; tricoords[0][1]=(float)x1; tricoords[0][2]=(float)x2;
				tricoords[1][0]=(float)y0; tricoords[1][1]=(float)y1; tricoords[1][2]=(float)y2;
				
				
				Color c0 = new Color(ColorOperations.scaleColorAlpha(tri.c0.getAsInt(), tris.getGlobalAlphaMultiplier()),true);
				Color c1 = new Color(ColorOperations.scaleColorAlpha(tri.c1.getAsInt(), tris.getGlobalAlphaMultiplier()),true);
				Color c2 = new Color(ColorOperations.scaleColorAlpha(tri.c2.getAsInt(), tris.getGlobalAlphaMultiplier()),true);
				
				g.setPaint(new BarycentricGradientPaint(tricoords[0], tricoords[1], c0, c1, c2));
				
				
				int minx = (int)Utils.min3(x0, x1, x2);
				int miny = (int)Utils.min3(y0, y1, y2);
				double maxx = Utils.max3(x0, x1, x2);
				double maxy = Utils.max3(y0, y1, y2);
				g.fillRect((minx), (miny), (int)Math.ceil(maxx-minx), (int)Math.ceil(maxy-miny));
				if(tri.pickColor != 0) {
					Color pick=new Color(tri.pickColor);
					p.setPaint(new BarycentricGradientPaint(tricoords[0], tricoords[1], pick, pick, pick));
					p.fillRect((minx), (miny), (int)Math.ceil(maxx-minx), (int)Math.ceil(maxy-miny));
				}
			}
		}
		
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
	}
	
	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		if(!isEnabled()){
			return;
		}
		String svgTriangleStrategy = getSvgTriangleStrategy();
		
		Element mainGroup = SVGUtils.createSVGElement(doc, "g");
		parent.appendChild(mainGroup);
		
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

		Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);
		
		for(Triangles tris : getItemsToRender()){
			if(tris.isHidden()){
				continue;
			}
			Element trianglesGroup = SVGUtils.createSVGElement(doc, "g");
			mainGroup.appendChild(trianglesGroup);
			if(tris.isCrispEdgesForSVGEnabled()){
				trianglesGroup.setAttributeNS(null, "shape-rendering", "crispEdges");
			}
			for(TriangleDetails tri : tris.getTriangleDetails()){
				double x0,y0, x1,y1, x2,y2;
				x0=tri.p0.getX(); y0=tri.p0.getY(); x1=tri.p1.getX(); y1=tri.p1.getY(); x2=tri.p2.getX(); y2=tri.p2.getY();
				
				x0-=translateX; x1-=translateX; x2-=translateX;
				y0-=translateY; y1-=translateY; y2-=translateY;
				x0*=scaleX; x1*=scaleX; x2*=scaleX;
				y0*=scaleY; y1*=scaleY; y2*=scaleY;
				
				SVGTriangleRendering.addSVGTriangle(
						doc, 
						trianglesGroup, 
						new double[]{x0,y0,x1,y1,x2,y2}, 
						new int[]{tri.c0.getAsInt(), tri.c1.getAsInt(), tri.c2.getAsInt()}, 
						tris.getGlobalAlphaMultiplier(), 
						svgTriangleStrategy, 
						viewportRect);
			}
		}
	}

	@Override
	public void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h) {
		if(!isEnabled()){
			return;
		}

		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

		try {
			PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);
			for(Triangles tris : getItemsToRender()){
				if(tris.isHidden()){
					continue;
				}

				for(TriangleDetails tri : tris.getTriangleDetails()){
					double x0,y0, x1,y1, x2,y2;
					x0=tri.p0.getX(); y0=tri.p0.getY(); x1=tri.p1.getX(); y1=tri.p1.getY(); x2=tri.p2.getX(); y2=tri.p2.getY();
					x0-=translateX; x1-=translateX; x2-=translateX;
					y0-=translateY; y1-=translateY; y2-=translateY;
					x0*=scaleX; x1*=scaleX; x2*=scaleX;
					y0*=scaleY; y1*=scaleY; y2*=scaleY;
					x0=x0+x; y0=y0+y; x1=x1+x; y1=y1+y; x2=x2+x; y2=y2+y;

					// clipping area
					contentStream.saveGraphicsState();
					contentStream.addRect(x, y, w, h);
					contentStream.closePath();
					contentStream.clip();
					PDFUtils.createPDFShadedTriangle(contentStream, new Point2D.Double(x0, y0), new Point2D.Double(x1,y1),
							new Point2D.Double(x2, y2), new Color(tri.c0.getAsInt()), new Color(tri.c1.getAsInt()), new Color(tri.c2.getAsInt()));
					contentStream.restoreGraphicsState();
				}
			}
			contentStream.close();
		} catch (IOException e) {
			e.printStackTrace();
		}


	}

	protected String getSvgTriangleStrategy() {
		String strategy = this.svgTriangleStrategy;
		if(Objects.isNull(strategy)){
			strategy = System.getProperty("jplotter_svg_triangle_strategy");
		}
		return Objects.nonNull(strategy) ? strategy:"SUBDIVIDE";
	}
	
	/**
	 * Sets the strategy used for SVG triangle rendering.
	 * <p>
	 * Since SVG does not support for barycentric interpolation of colors
	 * in triangles, a {@link TriangleDetails} object cannot be expressed
	 * correctly in SVG if a triangle has different colors per vertex.
	 * <p>
	 * To address this issue, there are the following strategies:
	 * <ul>
	 * <li>{@code "AVG_COLOR"} - averages all three vertex colors and creates a monochrome SVG triangle</li>
	 * <li>{@code "SUBDIVIDE"} - if a triangle's edge exceeds the threshold length (10px in screen coordinates)
	 * the triangle is subdivided until all resulting triangle edges conform to the threshold length.
	 * The resulting sub triangles will be rendered with the AVG_COLOR strategy.
	 * If a triangles vertex colors only differ by a small amount (maximum of 6 per 8bit channel)
	 * it will not be further subdivided since the small color deviation will not be discernible by
	 * human eye.
	 * </li>
	 * </ul>
	 * If this is set to null (=default) the strategy is read from the system property
	 * {@code "jplotter_svg_triangle_strategy"} and if the system property is not set,
	 * the strategy defaults to {@code "SUBDIVIDE"}.
	 * 
	 * @param svgTriangleStrategy the strategy to set, either {@code "AVG_COLOR"} or {@code "SUBDIVIDE"}
	 */
	public void setSvgTriangleStrategy(String svgTriangleStrategy) {
		this.svgTriangleStrategy = svgTriangleStrategy;
	}

}
