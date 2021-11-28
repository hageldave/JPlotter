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
import org.apache.pdfbox.cos.*;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.color.PDDeviceRGB;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroup;
import org.apache.pdfbox.pdmodel.graphics.form.PDTransparencyGroupAttributes;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShading;
import org.apache.pdfbox.pdmodel.graphics.shading.PDShadingType4;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.awt.*;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
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
	protected static final String vertexShaderSrcD = ""
			+ "" + "#version 410"
			+ NL + "layout(location = 0) in dvec2 in_position;"
			+ NL + "layout(location = 1) in uvec2 in_colors;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform dvec4 viewTransform;"
			+ NL + "uniform float saturationScaling;"
			+ NL + "out vec4 vColor;"
			+ NL + "out vec4 vPickColor;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"
			
			+ NL + "vec4 scaleSaturation(vec4 rgba, float sat) {"
			+ NL + "   float l = rgba.x*0.2126 + rgba.y*0.7152 + rgba.z*0.0722; // luminance"
			+ NL + "   vec3 drgb = rgba.xyz-vec3(l);"
			+ NL + "   float s=sat;"
			+ NL + "   if(s > 1.0) {"
			+ NL + "      // find maximal saturation that will keep channel values in range [0,1]"
			+ NL + "      s = min(s, drgb.x<0.0 ? -l/drgb.x : (1-l)/drgb.x);" 
			+ NL + "      s = min(s, drgb.y<0.0 ? -l/drgb.y : (1-l)/drgb.y);" 
			+ NL + "      s = min(s, drgb.z<0.0 ? -l/drgb.z : (1-l)/drgb.z);"
			+ NL + "   }"
			+ NL + "   return vec4(vec3(l)+s*drgb, rgba.w);"
			+ NL + "}"

			+ NL + "void main() {"
			+ NL + "   dvec3 pos = dvec3(in_position,1);"
			+ NL + "   pos = pos - dvec3(viewTransform.xy,0);"
			+ NL + "   pos = pos * dvec3(viewTransform.zw,1);"
			+ NL + "   gl_Position = projMX*vec4(pos,1);"
			+ NL + "   vColor = scaleSaturation(unpackARGB(in_colors.x), saturationScaling);"
			+ NL + "   vPickColor = unpackARGB(in_colors.y);"
			+ NL + "}"
			+ NL
			;
	protected static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in uvec2 in_colors;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform vec4 viewTransform;"
			+ NL + "uniform float saturationScaling;"
			+ NL + "out vec4 vColor;"
			+ NL + "out vec4 vPickColor;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"
			
			+ NL + "vec4 scaleSaturation(vec4 rgba, float sat) {"
			+ NL + "   float l = rgba.x*0.2126 + rgba.y*0.7152 + rgba.z*0.0722; // luminance"
			+ NL + "   vec3 drgb = rgba.xyz-vec3(l);"
			+ NL + "   float s=sat;"
			+ NL + "   if(s > 1.0) {"
			+ NL + "      // find maximal saturation that will keep channel values in range [0,1]"
			+ NL + "      s = min(s, drgb.x<0.0 ? -l/drgb.x : (1-l)/drgb.x);" 
			+ NL + "      s = min(s, drgb.y<0.0 ? -l/drgb.y : (1-l)/drgb.y);" 
			+ NL + "      s = min(s, drgb.z<0.0 ? -l/drgb.z : (1-l)/drgb.z);"
			+ NL + "   }"
			+ NL + "   return vec4(vec3(l)+s*drgb, rgba.w);"
			+ NL + "}"

			+ NL + "void main() {"
			+ NL + "   vec3 pos = vec3(in_position,1);"
			+ NL + "   pos = pos - vec3(viewTransform.xy,0);"
			+ NL + "   pos = pos * vec3(viewTransform.zw,1);"
			+ NL + "   gl_Position = projMX*vec4(pos,1);"
			+ NL + "   vColor = scaleSaturation(unpackARGB(in_colors.x), saturationScaling);"
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
		if(Objects.isNull(shaderF)){
			shaderF = ShaderRegistry.getOrCreateShader(this.getClass().getName()+"#F",()->new Shader(vertexShaderSrc, fragmentShaderSrc));
			itemsToRender.forEach(Renderable::initGL);
		}
		if(Objects.isNull(shaderD) && isGLDoublePrecisionEnabled){
			shaderD = ShaderRegistry.getOrCreateShader(this.getClass().getName()+"#D",()->new Shader(vertexShaderSrcD, fragmentShaderSrc));
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
		if(Objects.nonNull(shaderF)){
			ShaderRegistry.handbackShader(shaderF);
			shaderF = null;
		}
		if(Objects.nonNull(shaderD)){
			ShaderRegistry.handbackShader(shaderD);
			shaderD = null;
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
	protected void renderStart(int w, int h, Shader shader) {
		GL11.glDisable(GL11.GL_DEPTH_TEST);
		GL11.glEnable(GL11.GL_BLEND);
		GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();
		int loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "viewTransform");
		if (shader == shaderD /* double precision shader */)
		{
			GL40.glUniform4d(loc, translateX, translateY, scaleX, scaleY);
		}
		else
		{
			GL20.glUniform4f(loc, (float)translateX, (float)translateY, (float)scaleX, (float)scaleY);
		}
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
	}

	@Override
	@GLContextRequired
	protected void renderItem(Triangles item, Shader shader) {
		if(item.numTriangles() < 1){
			return;
		}
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "alphaMultiplier");
		GL20.glUniform1f(loc, item.getGlobalAlphaMultiplier());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "saturationScaling");
		GL20.glUniform1f(loc, item.getGlobalSaturationMultiplier());
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

				int c0 = ColorOperations.changeSaturation(tri.c0.getAsInt(), tris.getGlobalSaturationMultiplier());
				c0 = ColorOperations.scaleColorAlpha(c0, tris.getGlobalAlphaMultiplier());
				int c1 = ColorOperations.changeSaturation(tri.c1.getAsInt(), tris.getGlobalSaturationMultiplier());
				c1 = ColorOperations.scaleColorAlpha(c1, tris.getGlobalAlphaMultiplier());
				int c2 = ColorOperations.changeSaturation(tri.c2.getAsInt(), tris.getGlobalSaturationMultiplier());
				c2 = ColorOperations.scaleColorAlpha(c2, tris.getGlobalAlphaMultiplier());
				
				g.setPaint(new BarycentricGradientPaint(tricoords[0], tricoords[1], new Color(c0, true), new Color(c1, true), new Color(c2, true)));

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

				int c0 = ColorOperations.changeSaturation(tri.c0.getAsInt(), tris.getGlobalSaturationMultiplier());
				// not needed: alpha multiplier is passed via SVGTriangleRendering - c0 = ColorOperations.scaleColorAlpha(c0, tris.getGlobalAlphaMultiplier());
				int c1 = ColorOperations.changeSaturation(tri.c1.getAsInt(), tris.getGlobalSaturationMultiplier());
				int c2 = ColorOperations.changeSaturation(tri.c2.getAsInt(), tris.getGlobalSaturationMultiplier());

				SVGTriangleRendering.addSVGTriangle(
						doc, 
						trianglesGroup, 
						new double[]{x0,y0,x1,y1,x2,y2}, 
						new int[]{c0, c1, c2},
						tris.getGlobalAlphaMultiplier(), 
						svgTriangleStrategy, 
						viewportRect);
			}
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

	@Override
	public void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h) {
		if(!isEnabled()){
			return;
		}

		float factor;
		int maxValue;

		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

		try {
			PDPageContentStream contentStream = new PDPageContentStream(doc, page, PDPageContentStream.AppendMode.APPEND, false);

			double minX = 0.0;
			double minY = 0.0;
			double maxX = w;
			double maxY = h;

			contentStream.saveGraphicsState();
			// clipping
			contentStream.addRect(x, y, w, h);
			contentStream.clip();

			ArrayList<Triangles> allTriangles = new ArrayList<>(getItemsToRender().size());
			for(Triangles tris : getItemsToRender()){
				if(tris.isHidden()){
					continue;
				}
				Triangles modifiedTriangle = new Triangles();
				modifiedTriangle.setGlobalAlphaMultiplier(tris.getGlobalAlphaMultiplier());
				modifiedTriangle.setGlobalSaturationMultiplier(tris.getGlobalSaturationMultiplier());

				for(TriangleDetails tri : tris.getTriangleDetails()) {
					double x0,y0, x1,y1, x2,y2;
					x0=tri.p0.getX(); y0=tri.p0.getY(); x1=tri.p1.getX(); y1=tri.p1.getY(); x2=tri.p2.getX(); y2=tri.p2.getY();
					x0-=translateX; x1-=translateX; x2-=translateX;
					y0-=translateY; y1-=translateY; y2-=translateY;
					x0*=scaleX; x1*=scaleX; x2*=scaleX;
					y0*=scaleY; y1*=scaleY; y2*=scaleY;
					x0=x0+x; y0=y0+y; x1=x1+x; y1=y1+y; x2=x2+x; y2=y2+y;
					TriangleDetails triangleDetails = tri.clone();
					triangleDetails.p0 = new Point2D.Double(x0, y0);
					triangleDetails.p1 = new Point2D.Double(x1, y1);
					triangleDetails.p2 = new Point2D.Double(x2, y2);
					modifiedTriangle.getTriangleDetails().add(triangleDetails);
				}
				allTriangles.add(modifiedTriangle);
			}

			// remove all triangles details and add intersecting triangles back
			for(Triangles tris : allTriangles) {
				ArrayList<TriangleDetails> list = new ArrayList<>(tris.getIntersectingTriangles(new Rectangle2D.Double(x, y, w, h)));
				tris.removeAllTriangles();
				for (TriangleDetails details : list)
					tris.getTriangleDetails().add(details);
			}

			// calculate the min/max values (the bounds) of all triangles
			for(Triangles tris : allTriangles){
				for(TriangleDetails tri : tris.getTriangleDetails()) {
					double x0,y0, x1,y1, x2,y2;
					x0=tri.p0.getX(); y0=tri.p0.getY(); x1=tri.p1.getX(); y1=tri.p1.getY(); x2=tri.p2.getX(); y2=tri.p2.getY();
					// first get min/max vertex x/y value of triangle (Math.min(Math.min(x0, x1), x2))
					// then update min/max values if necessary
					minX = Math.min(Math.min(Math.min(x0, x1), x2), minX);
					minY = Math.min(Math.min(Math.min(y0, y1), y2), minY);
					maxX = Math.max(Math.max(Math.max(x0, x1), x2), maxX);
					maxY = Math.max(Math.max(Math.max(y0, y1), y2), maxY);
				}
			}

			// calculate the factor/maxValue Attributes
			// maybe there's an error: contourplot seems to be "too accurate"
			if ((maxX-minX) > (maxY-minY))
				factor = (float) ((Math.pow(2, 16)-1) / (maxX-minX));
			else
				factor = (float) ((Math.pow(2, 16)-1) / (maxY-minY));
			maxValue = (int) ((Math.pow(2, 16)-1) / factor);

			double shiftX = 0.0;
			double shiftY = 0.0;

			// calculate how much the content has to be shifted
			for(Triangles tris : allTriangles){
				for(TriangleDetails tri : tris.getTriangleDetails()) {
					double x0,y0, x1,y1, x2,y2;
					x0=tri.p0.getX(); y0=tri.p0.getY(); x1=tri.p1.getX(); y1=tri.p1.getY(); x2=tri.p2.getX(); y2=tri.p2.getY();

					x0 *= factor; x1 *= factor; x2 *= factor;
					y0 *= factor; y1 *= factor; y2 *= factor;

					// check if one coordinate is negative
					double triMinX = Math.min(Math.min(x0, x1), x2);
					double triMinY = Math.min(Math.min(y0, y1), y2);
					shiftX = Math.min(triMinX, shiftX);
					shiftY = Math.min(triMinY, shiftY);
				}
			}

			for(Triangles tris : allTriangles){
				PDShadingType4 gouraudShading = new PDShadingType4(doc.getDocument().createCOSStream());
				gouraudShading.setShadingType(PDShading.SHADING_TYPE4);
				gouraudShading.setBitsPerFlag(8);
				gouraudShading.setBitsPerCoordinate(16);
				gouraudShading.setBitsPerComponent(8);

				COSArray decodeArray = new COSArray();
				decodeArray.add(COSInteger.ZERO);
				decodeArray.add(COSInteger.get(maxValue));
				decodeArray.add(COSInteger.ZERO);
				decodeArray.add(COSInteger.get(maxValue));
				decodeArray.add(COSInteger.ZERO);
				decodeArray.add(COSInteger.ONE);
				decodeArray.add(COSInteger.ZERO);
				decodeArray.add(COSInteger.ONE);
				decodeArray.add(COSInteger.ZERO);
				decodeArray.add(COSInteger.ONE);
				gouraudShading.setDecodeValues(decodeArray);
				gouraudShading.setColorSpace(PDDeviceRGB.INSTANCE);

				OutputStream os = ((COSStream) gouraudShading.getCOSObject()).createOutputStream();
				MemoryCacheImageOutputStream mcos = new MemoryCacheImageOutputStream(os);

				// soft masking for triangle transparency
				PDDocument maskDoc = new PDDocument();
				PDPage maskPage = new PDPage();
				maskDoc.addPage(maskPage);
				maskPage.setMediaBox(new PDRectangle(w+x, h+y));
				PDPageContentStream maskCS = new PDPageContentStream(maskDoc, maskPage, PDPageContentStream.AppendMode.APPEND, false);

				PDShadingType4 gouraudShadingMask = new PDShadingType4(doc.getDocument().createCOSStream());
				gouraudShadingMask.setShadingType(PDShading.SHADING_TYPE4);
				gouraudShadingMask.setBitsPerFlag(8);
				gouraudShadingMask.setBitsPerCoordinate(16);
				gouraudShadingMask.setBitsPerComponent(8);

				COSArray decodeArrayMask = new COSArray();
				decodeArrayMask.add(COSInteger.ZERO);
				decodeArrayMask.add(COSInteger.get(maxValue));
				decodeArrayMask.add(COSInteger.ZERO);
				decodeArrayMask.add(COSInteger.get(maxValue));
				decodeArrayMask.add(COSInteger.ZERO);
				decodeArrayMask.add(COSInteger.ONE);
				decodeArrayMask.add(COSInteger.ZERO);
				decodeArrayMask.add(COSInteger.ONE);
				decodeArrayMask.add(COSInteger.ZERO);
				decodeArrayMask.add(COSInteger.ONE);
				gouraudShadingMask.setDecodeValues(decodeArrayMask);
				gouraudShadingMask.setColorSpace(PDDeviceRGB.INSTANCE);

				OutputStream osMask = ((COSStream) gouraudShadingMask.getCOSObject()).createOutputStream();
				MemoryCacheImageOutputStream mcosMask = new MemoryCacheImageOutputStream(osMask);

				PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
				graphicsState.setNonStrokingAlphaConstant(tris.getGlobalAlphaMultiplier());
				graphicsState.setStrokingAlphaConstant(tris.getGlobalAlphaMultiplier());
				contentStream.setGraphicsStateParameters(graphicsState);

				for(TriangleDetails tri : tris.getTriangleDetails()){
					double x0,y0, x1,y1, x2,y2;
					x0=tri.p0.getX(); y0=tri.p0.getY(); x1=tri.p1.getX(); y1=tri.p1.getY(); x2=tri.p2.getX(); y2=tri.p2.getY();

					// rescale x,y coordinates
					x0 *= factor; x1 *= factor; x2 *= factor;
					y0 *= factor; y1 *= factor; y2 *= factor;
					x0 -= shiftX; x1 -= shiftX; x2 -= shiftX;
					y0 -= shiftY; y1 -= shiftY; y2 -= shiftY;

					int c0 = ColorOperations.changeSaturation(tri.c0.getAsInt(), tris.getGlobalSaturationMultiplier());
					int c1 = ColorOperations.changeSaturation(tri.c1.getAsInt(), tris.getGlobalSaturationMultiplier());
					int c2 = ColorOperations.changeSaturation(tri.c2.getAsInt(), tris.getGlobalSaturationMultiplier());

					int c02 = new Color(tri.c0.getAsInt(), true).getAlpha();
					int c12 = new Color(tri.c1.getAsInt(), true).getAlpha();
					int c22 = new Color(tri.c2.getAsInt(), true).getAlpha();

					// write shaded triangle to mask stream
					PDFUtils.writeShadedTriangle(mcosMask,
							new Point2D.Double(x0, y0),
							new Point2D.Double(x1,y1),
							new Point2D.Double(x2, y2),
							new Color(c02, c02, c02),
							new Color(c12, c12, c12),
							new Color(c22, c22, c22));

					// write shaded (colored) triangle to normal stream
					PDFUtils.writeShadedTriangle(mcos, new Point2D.Double(x0, y0), new Point2D.Double(x1,y1),
							new Point2D.Double(x2, y2), new Color(c0), new Color(c1), new Color(c2));
				}

				maskCS.saveGraphicsState();
				maskCS.transform(new Matrix(1,0,0,1, (float) shiftX/factor, (float) shiftY/factor));
				maskCS.shadingFill(gouraudShadingMask);
				maskCS.restoreGraphicsState();
				mcosMask.close();
				osMask.close();
				maskCS.close();
				// import b/w triangle as a mask
				LayerUtility maskLayer = new LayerUtility(doc);
				PDFormXObject maskForm = maskLayer.importPageAsForm(maskDoc, 0);
				maskDoc.close();

				PDTransparencyGroupAttributes transparencyGroupAttributes = new PDTransparencyGroupAttributes();
				transparencyGroupAttributes.getCOSObject().setItem(COSName.CS, COSName.DEVICEGRAY);

				PDTransparencyGroup transparencyGroup = new PDTransparencyGroup(doc);
				transparencyGroup.setBBox(new PDRectangle(w+x, h+y));
				transparencyGroup.setResources(new PDResources());
				transparencyGroup.getCOSObject().setItem(COSName.GROUP, transparencyGroupAttributes);
				try (PDFormContentStream canvas = new PDFormContentStream(transparencyGroup)) {
					canvas.drawForm(maskForm);
				}

				COSDictionary softMaskDictionary = new COSDictionary();
				softMaskDictionary.setItem(COSName.S, COSName.LUMINOSITY);
				softMaskDictionary.setItem(COSName.G, transparencyGroup);

				PDExtendedGraphicsState extendedGraphicsState = new PDExtendedGraphicsState();
				extendedGraphicsState.getCOSObject().setItem(COSName.SMASK, softMaskDictionary);

				contentStream.saveGraphicsState();
				contentStream.setGraphicsStateParameters(extendedGraphicsState);
				contentStream.transform(new Matrix(1,0,0,1, (float) shiftX/factor, (float) shiftY/factor));
				contentStream.shadingFill(gouraudShading);
				contentStream.restoreGraphicsState();

				mcos.close();
				os.close();
			}
			contentStream.restoreGraphicsState();
			contentStream.close();
		} catch (IOException e) {
			System.out.println(e);
			throw new RuntimeException("Error occurred!");
		}
	}
}