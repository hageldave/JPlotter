package hageldave.jplotter.renderers;

import hageldave.imagingkit.core.Pixel;
import hageldave.jplotter.color.ColorOperations;
import hageldave.jplotter.gl.Shader;
import hageldave.jplotter.misc.Glyph;
import hageldave.jplotter.renderables.Points;
import hageldave.jplotter.renderables.Points.PointDetails;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.Annotations.GLContextRequired;
import hageldave.jplotter.util.ShaderRegistry;
import org.apache.pdfbox.multipdf.LayerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.graphics.form.PDFormXObject;
import org.apache.pdfbox.pdmodel.graphics.state.PDExtendedGraphicsState;
import org.apache.pdfbox.util.Matrix;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL31;
import org.lwjgl.opengl.GL40;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Objects;

/**
 * The PointsRenderer is an implementation of the {@link GenericRenderer}
 * for {@link Points}.
 * It uses an instanced GL draw call for each of the {@link Points} objects
 * to render.
 * There is an third glyph scaling parameter in this renderer that is multiplied
 * to the existing 2 scalings of a single point instance
 * (see {@link Points#setGlobalScaling(double)}) to control the scaling on a
 * per renderer basis.
 * <br>
 * Its fragment shader draws the picking color into the second render buffer
 * alongside the 'visible' color that is drawn into the first render buffer.
 * 
 * @author hageldave
 */
public class PointsRenderer extends GenericRenderer<Points> {

	protected static final char NL = '\n';
	protected static final String vertexShaderSrcD = ""
			+ "" + "#version 410"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in dvec2 in_pointpos;"
			+ NL + "layout(location = 2) in vec2 in_rotAndScale;"
			+ NL + "layout(location = 3) in uvec2 in_colors;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform dvec4 viewTransform;"
			+ NL + "uniform vec2 modelScaling;"
			+ NL + "uniform float globalScaling;"
			+ NL + "uniform bool roundposition;"
			+ NL + "out vec4 vColor;"
			+ NL + "out vec4 vPickColor;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"

			+ NL + "mat2 rotationMatrix(float angle){"
			+ NL + "   float s = sin(angle), c = cos(angle);"
			+ NL + "   return mat2(c,s,-s,c);"
			+ NL + "}"

			+ NL + "mat2 scalingMatrix(float s){"
			+ NL + "   return mat2(s,0,0,s);"
			+ NL + "}"
			
			+ NL + "float rnd(float f){return float(int(f+0.5));}"

			+ NL + "vec2 roundToIntegerValuedVec(vec2 v){"
			+ NL + "   return vec2(rnd(v.x),rnd(v.y));"
			+ NL + "}"

			+ NL + "void main() {"
			+ NL + "   mat2 rotMX = rotationMatrix(in_rotAndScale.x);"
			+ NL + "   mat2 scaleMX = scalingMatrix(in_rotAndScale.y);"
			+ NL + "   dvec3 pos = dvec3(globalScaling*(scaleMX*rotMX*in_position)*modelScaling+in_pointpos, 1);"
			+ NL + "   pos = pos - dvec3(viewTransform.xy,0);"
			+ NL + "   pos = pos * dvec3(viewTransform.zw,1);"
			+ NL + "   if(roundposition){pos = dvec3(roundToIntegerValuedVec(vec2(pos.xy)),pos.z);}"
			+ NL + "   gl_Position = projMX*vec4(pos,1);"
			+ NL + "   vColor = unpackARGB(in_colors.x);"
			+ NL + "   vPickColor = unpackARGB(in_colors.y);"
			+ NL + "}"
			+ NL
			;	
	protected static final String vertexShaderSrc = ""
			+ "" + "#version 330"
			+ NL + "layout(location = 0) in vec2 in_position;"
			+ NL + "layout(location = 1) in vec2 in_pointpos;"
			+ NL + "layout(location = 2) in vec2 in_rotAndScale;"
			+ NL + "layout(location = 3) in uvec2 in_colors;"
			+ NL + "uniform mat4 projMX;"
			+ NL + "uniform vec4 viewTransform;"
			+ NL + "uniform vec2 modelScaling;"
			+ NL + "uniform float globalScaling;"
			+ NL + "uniform bool roundposition;"
			+ NL + "out vec4 vColor;"
			+ NL + "out vec4 vPickColor;"

			+ NL + "vec4 unpackARGB(uint c) {"
			+ NL + "   uint mask = uint(255);"
			+ NL + "   return vec4( (c>>16)&mask, (c>>8)&mask, (c)&mask, (c>>24)&mask )/255.0;"
			+ NL + "}"

			+ NL + "mat2 rotationMatrix(float angle){"
			+ NL + "   float s = sin(angle), c = cos(angle);"
			+ NL + "   return mat2(c,s,-s,c);"
			+ NL + "}"

			+ NL + "mat2 scalingMatrix(float s){"
			+ NL + "   return mat2(s,0,0,s);"
			+ NL + "}"
			
			+ NL + "float rnd(float f){return float(int(f+0.5));}"

			+ NL + "vec2 roundToIntegerValuedVec(vec2 v){"
			+ NL + "   return vec2(rnd(v.x),rnd(v.y));"
			+ NL + "}"

			+ NL + "void main() {"
			+ NL + "   mat2 rotMX = rotationMatrix(in_rotAndScale.x);"
			+ NL + "   mat2 scaleMX = scalingMatrix(in_rotAndScale.y);"
			+ NL + "   vec3 pos = vec3(globalScaling*(scaleMX*rotMX*in_position)*modelScaling+in_pointpos, 1);"
			+ NL + "   pos = pos - vec3(viewTransform.xy,0);"
			+ NL + "   pos = pos * vec3(viewTransform.zw,1);"
			+ NL + "   if(roundposition){pos = vec3(roundToIntegerValuedVec(pos.xy),pos.z);}"
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

	protected float glyphScaling = 1f;

	/**
	 * Sets the renderers glyph scaling value. 
	 * Default is 1.0.
	 * @param glyphScaling scaling for the glyphs that are rendered
	 * @return this for chaining
	 */
	public PointsRenderer setGlyphScaling(double glyphScaling) {
		this.glyphScaling = (float) glyphScaling;
		return this;
	}
	
	/**
	 * @return the renderers glyph scaling value
	 */
	public float getGlyphScaling() {
		return glyphScaling;
	}
	
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
			itemsToRender.forEach(Renderable::initGL);;
		}
		if(Objects.isNull(shaderD) && isGLDoublePrecisionEnabled) {
			shaderD = ShaderRegistry.getOrCreateShader(this.getClass().getName()+"#D",()->new Shader(vertexShaderSrcD, fragmentShaderSrc));
		}
	}

	/**
	 * Disposes of GL resources, i.e. closes the shader.
	 * It also deletes (closes) all {@link Points} contained in this
	 * renderer.
	 */
	@Override
	@GLContextRequired
	public void close() {
		if(Objects.nonNull(shaderF))
			ShaderRegistry.handbackShader(shaderF);
		shaderF = null;
		if(Objects.nonNull(shaderD))
			ShaderRegistry.handbackShader(shaderD);
		shaderD = null;
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
		if(shader == shaderD /* double precision shader */)
		{
			GL40.glUniform4d(loc, translateX, translateY, scaleX, scaleY);
		}
		else
		{
			GL20.glUniform4f(loc, (float)translateX, (float)translateY, (float)scaleX, (float)scaleY);		  
		}		

		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "modelScaling");
		GL20.glUniform2f(loc, (float)(1/scaleX), (float)(1/scaleY));
	   
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "projMX");
		GL20.glUniformMatrix4fv(loc, false, orthoMX);
	}

	@Override
	@GLContextRequired
	protected void renderItem(Points item, Shader shader) {
		if(item.numPoints() < 1){
			return;
		}
		int loc;
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "globalScaling");
		GL20.glUniform1f(loc, this.glyphScaling * item.glyph.pixelSize() * item.getGlobalScaling());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "alphaMultiplier");
		GL20.glUniform1f(loc, item.getGlobalAlphaMultiplier());
		loc = GL20.glGetUniformLocation(shader.getShaderProgID(), "roundposition");
		GL20.glUniform1i(loc, item.isVertexRoundingEnabled() ? 1:0);
		// draw things
		item.bindVertexArray();
		if(item.glyph.useElementsDrawCall()){
			GL31.glDrawElementsInstanced(item.glyph.primitiveType(), item.glyph.numVertices(), GL11.GL_UNSIGNED_INT, 0, item.numPoints());
		} else {
			GL31.glDrawArraysInstanced(item.glyph.primitiveType(), 0, item.glyph.numVertices(), item.numPoints());
		}
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
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

		Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);
	
		g.setStroke(new BasicStroke());
		p.setStroke(new BasicStroke());
		
		for(Points points : getItemsToRender()){
			if(points.isHidden()){
				continue;
			}
			Glyph glyph = points.glyph;
			
			for(PointDetails point : points.getPointDetails()){
				double x1,y1;
				x1=point.location.getX(); y1=point.location.getY();
				
				x1-=translateX;
				y1-=translateY;
				x1*=scaleX;
				y1*=scaleY;
				
				if(!viewportRect.intersects(
						x1-glyph.pixelSize()/2, 
						y1-glyph.pixelSize()/2, 
						glyph.pixelSize(), 
						glyph.pixelSize()))
				{
					continue;
				}
				
				
				Graphics2D g_ = (Graphics2D) g.create();
				AffineTransform xform = new AffineTransform();
				xform.translate(x1, y1);
				if(point.rot.getAsDouble() != 0.0){
					xform.rotate(point.rot.getAsDouble());
				}
				g_.transform(xform);
				int color = ColorOperations.scaleColorAlpha(point.color.getAsInt(),points.getGlobalAlphaMultiplier());
				g_.setColor(new Color(color, true));
				glyph.drawFallback(g_, (float)(glyphScaling*points.getGlobalScaling()*point.scale.getAsDouble()));
				
				if(point.pickColor != 0) {
					Graphics2D p_ = (Graphics2D) p.create();
					p_.transform(xform);
					p_.setColor(new Color(point.pickColor));
					glyph.drawFallback(p_, (float)(glyphScaling*points.getGlobalScaling()*point.scale.getAsDouble()));
				}
			}
		}
		
	}
	
	@Override
	public void renderSVG(Document doc, Element parent, int w, int h) {
		if(!isEnabled()){
			return;
		}
		Element mainGroup = SVGUtils.createSVGElement(doc, "g");
		parent.appendChild(mainGroup);
		
		double translateX = Objects.isNull(view) ? 0:view.getX();
		double translateY = Objects.isNull(view) ? 0:view.getY();
		double scaleX = Objects.isNull(view) ? 1:w/view.getWidth();
		double scaleY = Objects.isNull(view) ? 1:h/view.getHeight();

		Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);
		
		for(Points points : getItemsToRender()){
			if(points.isHidden()){
				continue;
			}
			Element pointsGroup = SVGUtils.createSVGElement(doc, "g");
			mainGroup.appendChild(pointsGroup);
			Glyph glyph = points.glyph;
			String symbolID = SVGUtils.createGlyphSymbolDef(doc, glyph, "glyph_"+glyph.glyphName());
			for(PointDetails point : points.getPointDetails()){
				double x1,y1;
				x1=point.location.getX(); y1=point.location.getY();
				
				x1-=translateX;
				y1-=translateY;
				x1*=scaleX;
				y1*=scaleY;
				
				if(!viewportRect.intersects(
						x1-glyph.pixelSize()/2, 
						y1-glyph.pixelSize()/2, 
						glyph.pixelSize(), 
						glyph.pixelSize()))
				{
					continue;
				}
				
				Element pointElement = SVGUtils.createSVGElement(doc, "use");
				pointsGroup.appendChild(pointElement);
				pointElement.setAttributeNS(null, "xlink:href", "#"+symbolID);
				if(glyph.isFilled()){
					pointElement.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(point.color.getAsInt()));
					pointElement.setAttributeNS(null, "fill-opacity", 
							SVGUtils.svgNumber(points.getGlobalAlphaMultiplier()*Pixel.a_normalized(point.color.getAsInt())));
				} else {
					pointElement.setAttributeNS(null, "stroke", SVGUtils.svgRGBhex(point.color.getAsInt()));
					pointElement.setAttributeNS(null, "stroke-opacity", 
							SVGUtils.svgNumber(points.getGlobalAlphaMultiplier()*Pixel.a_normalized(point.color.getAsInt())));
					pointElement.setAttributeNS(null, "fill-opacity", "0");
				}
				String transform = "";
				transform += "translate("+SVGUtils.svgNumber(x1)+","+SVGUtils.svgNumber(y1)+")";
				if(point.rot.getAsDouble() != 0){
					transform += " rotate("+SVGUtils.svgNumber(point.rot.getAsDouble()*180/Math.PI)+")";
				}
				if(glyphScaling*point.scale.getAsDouble() != 1){
					transform += " scale("+SVGUtils.svgPoints(points.getGlobalScaling()*glyphScaling*point.scale.getAsDouble(), points.getGlobalScaling()*glyphScaling*point.scale.getAsDouble())+")";
				}
				
				pointElement.setAttributeNS(null, "transform", transform);
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

		Rectangle2D viewportRect = new Rectangle2D.Double(0, 0, w, h);

		try {
			PDPageContentStream contentStream = new PDPageContentStream(doc, page,
					PDPageContentStream.AppendMode.APPEND, false);

			for (Points points : getItemsToRender()) {
				if (points.isHidden()) {
					continue;
				}
				Glyph glyph = points.glyph;

				PDDocument glyphDoc = new PDDocument();
				PDPage glyphPage = new PDPage();
				PDPage rectPage = new PDPage();
				glyphDoc.addPage(glyphPage);
				glyphDoc.addPage(rectPage);
				PDPageContentStream glyphCont = new PDPageContentStream(glyphDoc, glyphPage);
				PDPageContentStream rectCont = new PDPageContentStream(glyphDoc, rectPage);

				glyph.createPDFElement(glyphCont);
				rectCont.addRect(x, y, w, h);

				LayerUtility layerUtility = new LayerUtility(doc);
				rectCont.close();
				glyphCont.close();
				PDFormXObject glyphForm = layerUtility.importPageAsForm(glyphDoc, 0);
				PDFormXObject rectForm = layerUtility.importPageAsForm(glyphDoc, 1);
				glyphDoc.close();

				PDExtendedGraphicsState graphicsState = new PDExtendedGraphicsState();
				graphicsState.setStrokingAlphaConstant(points.getGlobalAlphaMultiplier());
				graphicsState.setNonStrokingAlphaConstant(points.getGlobalAlphaMultiplier());
				contentStream.setGraphicsStateParameters(graphicsState);

				for (PointDetails point : points.getPointDetails()) {
					double x1, y1;
					x1 = point.location.getX();
					y1 = point.location.getY();

					x1 -= translateX;
					y1 -= translateY;
					x1 *= scaleX;
					y1 *= scaleY;

					if (!viewportRect.intersects(
							x1 - glyph.pixelSize() / 2,
							y1 - glyph.pixelSize() / 2,
							glyph.pixelSize(),
							glyph.pixelSize())) {
						continue;
					}

					// clipping area
					contentStream.saveGraphicsState();
					contentStream.drawForm(rectForm);
					contentStream.closePath();
					contentStream.clip();

					// transform
					contentStream.transform(new Matrix(1, 0, 0, 1, (float) x1 + x, (float) y1 + y));
					if(point.rot.getAsDouble() != 0){
						// rotation
						contentStream.transform(new Matrix((float) Math.cos(-point.rot.getAsDouble()),(float) -Math.sin(-point.rot.getAsDouble()),
								(float) Math.sin(-point.rot.getAsDouble()),(float) Math.cos(-point.rot.getAsDouble()), 0, 0));
					}
					// scale
					contentStream.transform(new Matrix((float) (glyphScaling*points.getGlobalScaling()*point.scale.getAsDouble()), 0, 0,
						(float) (glyphScaling*points.getGlobalScaling()*point.scale.getAsDouble()), 0, 0));

					contentStream.drawForm(glyphForm);

					if(glyph.isFilled()){
						contentStream.setNonStrokingColor(new Color(point.color.getAsInt()));
						contentStream.fill();
					} else {
						contentStream.setLineWidth((float) (1/(glyphScaling*points.getGlobalScaling()*point.scale.getAsDouble())));
						contentStream.setStrokingColor(new Color(point.color.getAsInt()));
						contentStream.stroke();
					}
					// restore graphics
					contentStream.restoreGraphicsState();
				}
			}
			contentStream.close();
		} catch (IOException e) {
			throw new RuntimeException("Error occurred!");
		}
	}
}
