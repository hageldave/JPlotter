package hageldave.jplotter.canvas;

import hageldave.imagingkit.core.Img;
import hageldave.jplotter.pdf.FontCachedPDDocument;
import hageldave.jplotter.renderers.Renderer;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.awt.*;
import java.awt.event.WindowListener;
import java.awt.geom.Rectangle2D;
import java.io.IOException;
import java.util.Arrays;

/**
 * This interface defines the methods required by an implementation of a 
 * canvas {@link Component} for use with JPlotter {@link Renderer}s such as 
 * {@link BlankCanvas} or {@link BlankCanvasFallback}.
 * 
 * The most important required operations on such a component are:
 * <ul>
 * <li>setting a {@link Renderer} that takes care of generating the displayed content</li>
 * <li>exporting what the component displays to an image</li>
 * <li>querying a pixel color at a specific location of the component (especially important for picking)</li>
 * <li>scheduling a repaint operation of the component</li>
 * <li>getting this as an awt {@link Component} (implicit cast) since implementations have to be an instance of this class</li>
 * </ul>
 * <p>
 * This interface is intended to enable development without explicitly choosing between 
 * {@link BlankCanvas} or {@link BlankCanvasFallback}.
 * This way a fallback mode for an application can be easily realized, e.g. for macOS which is not supported by
 * lwjgl3-awt and thus cannot use the OpenGL backed BlankCanvas.
 * <p>
 * JPlotterCanvas also provides the ability to export to scalable vector graphics (SVG) 
 * and portable document format (PDF) with the {@link #paintSVG()} and {@link #paintPDF()} methods.
 * 
 * 
 * @author hageldave
 */
public interface JPlotterCanvas {

	/**
	 * On AWT event dispatch thread:<br>
	 * Uses the set {@link Renderer} render to render display contents, then calls super.repaint() to display rendered content.
	 * <p>
	 * Schedules a repaint call call on the AWT event dispatch thread if not on it.
	 * <p>
	 * <b>This method is only deprecated for calling directly, call {@link #scheduleRepaint()} instead.</b><br>
	 * Of course super.repaint() is implemented by the implementing {@link Component} already.
	 */
	@Deprecated
	public void repaint();
	
	/**
	 * Schedules a repaint call on the AWT event dispatch thread.
	 * If a repaint is already pending, this method will not schedule an
	 * additional call until the render method within repaint is about to be executed.
	 */
	public void scheduleRepaint();
	
	/**
	 * En/disables SVG rendering as image.
	 * When rendering to SVG and this is enabled, instead of translating the 
	 * contents of the renderers into SVG elements, the current framebuffer image 
	 * is used and put into the dom.
	 * <p>
	 * This can be useful for example when too many SVG elements would be created
	 * resulting in a huge dom and file size when exporting as SVG.
	 * 
	 * @param enable true when no SVG elements should be created from the content
	 * of this JPlotterCanvas but instead a simple image element with the framebuffer's
	 * content.
	 */
	public void enableSvgAsImageRendering(boolean enable);
	
	/**
	 * @return true when enabled
	 * @see #enableSvgAsImageRendering(boolean)
	 */
	public boolean isSvgAsImageRenderingEnabled();

	/**
	 * En/disables PDF rendering as image.
	 * When rendering to PDF and this is enabled, instead of translating the
	 * contents of the renderers into PDF elements, the current framebuffer image
	 * is used and put into the pdf document.
	 * <p>
	 * This can be useful for example when too many PDF elements would be created
	 * resulting in a huge dom and file size when exporting as PDF.
	 *
	 * @param enable true when no PDF elements should be created from the content
	 * of this JPlotterCanvas but instead a simple image element with the framebuffer's
	 * content.
	 */
	public void enablePDFAsImageRendering(boolean enable);

	/**
	 * @return true when enabled
	 * @see #enablePDFAsImageRendering(boolean) (boolean)
	 */
	public boolean isPDFAsImageRenderingEnabled();


	/**
	 * Fetches the current contents of the framebuffer and returns them as an {@link Img}.
	 * @return image of the current framebuffer.
	 */
	public Img toImg();
	
	/**
	 * Reads the color value of the pixel at the specified location if areaSize == 1.
	 * This can be used to get the color or picking color under the mouse cursor.
	 * <p>
	 * Since the cursor placement may be inexact and thus miss the location the user
	 * was actually interested in, the areaSize parameter can be increased to create
	 * a window of pixels around the specified location.
	 * This window area will be examined and the most prominent non zero color value will
	 * be returned.
	 * @param x coordinate of the pixels location
	 * @param y coordinate of the pixels location
	 * @param picking whether the picking color or the visible color should be retrieved.
	 * @param areaSize width and height of the area around the specified location.
	 * @return the most prominent color in the area as integer packed ARGB value.
	 * If the returned value is to be used as an object id from picking color, then the
	 * alpha value probably has to be discarded first using {@code 0x00ffffff & returnValue}.
	 */
	public int getPixel(int x, int y, boolean picking, int areaSize);
	
	/**
	 * Creates a new SVG {@link Document} and renders this canvas as SVG elements.
	 * Will call {@link #paintToSVG(Document, Element, int, int)} after setting up
	 * the document and creating the initial elements.
	 * @return the created document
	 */
	public default Document paintSVG(){
		Document document = SVGUtils.createSVGDocument(asComponent().getWidth(), asComponent().getHeight());
		paintSVG(document, document.getDocumentElement());
		return document;
	}
	
	/**
	 * Renders this canvas as SVG elements under the specified parent element.
	 * Will call {@link #paintToSVG(Document, Element, int, int)} after creating 
	 * the initial elements.
	 * @param document document to create SVG elements with
	 * @param parent the parent node to which this canvas is supposed to be rendered
	 * to.
	 */
	public default void paintSVG(Document document, Element parent) {
		int w,h;
		if((w=asComponent().getWidth()) >0 && (h=asComponent().getHeight()) >0){
			if(SVGUtils.getDefs(document) == null){
				Element defs = SVGUtils.createSVGElement(document, "defs");
				defs.setAttributeNS(null, "id", "JPlotterDefs");
				document.getDocumentElement().appendChild(defs);
			}
			
			Element rootGroup = SVGUtils.createSVGElement(document, "g");
			parent.appendChild(rootGroup);
			rootGroup.setAttributeNS(null, "transform", "scale(1,-1) translate(0,-"+h+")");
			
			// define the clipping rectangle for the content (rect of vieport size)
			Node defs = SVGUtils.getDefs(document);
			Element clip = SVGUtils.createSVGElement(document, "clipPath");
			String clipDefID = SVGUtils.newDefId();
			clip.setAttributeNS(null, "id", clipDefID);
			clip.appendChild(SVGUtils.createSVGRect(document, 0, 0, w, h));
			defs.appendChild(clip);
			// clip the root group
			rootGroup.setAttributeNS(null, "clip-path", "url(#"+clipDefID+")");
			
			
			Element background = SVGUtils.createSVGElement(document, "rect");
			rootGroup.appendChild(background);
			background.setAttributeNS(null, "id", "background"+"@"+hashCode());
			background.setAttributeNS(null, "width", ""+w);
			background.setAttributeNS(null, "height", ""+h);
			background.setAttributeNS(null, "fill", SVGUtils.svgRGBhex(asComponent().getBackground().getRGB()));
			
			paintToSVG(document, rootGroup, w,h);
		}
	}
	
	/**
	 * Renders this {@link JPlotterCanvas} in terms of SVG elements
	 * to the specified parent element of the specified SVG document.
	 * <p>
	 * This method has to be overridden when the implementing
	 * class can express its contents in terms of SVG.
	 * 
	 * @param doc document to create svg elements with
	 * @param parent to append svg elements to
	 * @param w width of the viewport (the width of this Canvas)
	 * @param h height of the viewport (the height of this Canvas)
	 */
	public default void paintToSVG(Document doc, Element parent, int w, int h){
		Renderer renderer = getRenderer();
		if(renderer != null) 
			renderer.renderSVG(doc, parent, w, h);
	}

	/**
	 * Creates a new PDF document {@link PDDocument} and renders this canvas on the PDF document.
	 * Will call {@link #paintPDF()} after creating
	 * the document and the first page.
	 *
	 * @return the resulting pdf document with all the rendered content
	 * @throws IOException If there is an error while creating the document.
	 */
	public default PDDocument paintPDF() throws IOException {
		PDDocument document = new FontCachedPDDocument();
		PDPage page = new PDPage();
		document.addPage(page);
		paintPDF(document, page);
		return document;
	}

	/**
	 * Sets up the PDF document (size, background color, ...).
	 * Will call {@link #paintToPDF(PDDocument, PDPage, int, int)} after setting up
	 * the document and creating the initial elements.
	 *
	 * @param document PDF document holding the page
	 * @param page Page in the document to create PDF elements in
	 * @throws IOException If there is an error while creating the document.
	 */
	public default void paintPDF(PDDocument document, PDPage page) throws IOException {
		int w,h;
		if ((w=asComponent().getWidth()) > 0 && (h=asComponent().getHeight()) > 0) {
			// do we need the mediabox?
			page.setMediaBox(new PDRectangle(w, h));
			paintPDF(document, page, new Rectangle2D.Float(0, 0, w, h));
		}
	}

	
	public default void paintPDF(PDDocument document, PDPage page, Rectangle2D renderLoc) throws IOException {
		PDPageContentStream contentStream = new PDPageContentStream(document, page, PDPageContentStream.AppendMode.APPEND, false);
		// drawing the background color of the canvas in the given render location
		contentStream.addRect((float) renderLoc.getBounds2D().getX(), (float) (page.getMediaBox().getHeight()-renderLoc.getBounds2D().getY()-renderLoc.getBounds2D().getHeight()),
				(float) renderLoc.getBounds2D().getWidth(), (float) renderLoc.getBounds2D().getHeight());
		contentStream.setNonStrokingColor(asComponent().getBackground());
		contentStream.fill();
		contentStream.close();
		// draw the rest (render to pdf)
		paintToPDF(document, page, renderLoc);
	}


	public default void paintToPDF(PDDocument document, PDPage page, Rectangle2D renderLoc) {
		Renderer renderer = getRenderer();
		if(renderer != null) {
			renderer.renderPDF(document, page,
					(int) renderLoc.getX(),
					(int) (page.getMediaBox().getHeight()-renderLoc.getMaxY()),
					(int) renderLoc.getWidth(),
					(int) (renderLoc.getHeight()));
		}
	}

	/**
	 * Renders this {@link JPlotterCanvas} in terms of PDF elements
	 * to the specified page of the specified PDF document.
	 *
	 * @param document PDF document holding the page
	 * @param page page in the document to create PDF elements in
	 * @param w width of the page
	 * @param h height of the page
	 */
	public default void paintToPDF(PDDocument document, PDPage page, int w, int h) {
		Renderer renderer = getRenderer();
		if(renderer != null)
			renderer.renderPDF(document, page, 0, 0, w, h);
	}

	/**
	 * Sets the renderer of this canvas.
	 * @param renderer to draw contents.
	 * @return this for chaining
	 */
	public JPlotterCanvas setRenderer(Renderer renderer);
	
	/**
	 * @return the current renderer
	 */
	public Renderer getRenderer();
	
	/**
	 * Implicit cast of this canvas to a class extending {@link Component}.
	 * This implies that the implementing class is a {@link Component}.
	 * @return this, but cast to {@link Component}
	 */
	public default Component asComponent() {
		return (Component)this;
	}
	
	/**
	 * Determines the most prominent value in a square shaped area.
	 * This method should be used by {@link #getPixel(int, int, boolean, int)} implementations.
	 * @param colors of the square shaped area
	 * @param areaSize widht or height of the area
	 * @return mode of colors, with +1 count for the center color
	 */
	public static int mostProminentColor(int[] colors, int areaSize) {
		if(areaSize == 1){
			return colors[0];
		}
		int center = areaSize*(areaSize/2)+(areaSize/2);
		int centerValue = colors[center];
		int centerBonus = centerValue == 0 ? 0:1;
		// calculate most prominent color (mode)
		Arrays.sort(colors);
		int currentValue = colors[0]; 
		int mostValue = currentValue; 
		int count = currentValue == centerValue ? 1+centerBonus:1; // center color gets bonus
		int maxCount=count;
		for(int i = 1; i < colors.length; i++){
			if(colors[i]==currentValue && currentValue != 0xff000000){
				count++;
			} else {
				if(count > maxCount){
					maxCount = count;
					mostValue = currentValue;
				}
				currentValue = colors[i];
				count = currentValue == centerValue ? 1+centerBonus:1; // center color gets bonus
			}
		}
		return mostValue;
	}
	
	/**
	 * Adds a {@link WindowListener} to the specified window that takes care of
	 * cleanup (GL resources) when the window is about to close. 
	 * <p>
	 * This method only has an effect when this {@link JPlotterCanvas} is an instance of {@link FBOCanvas}
	 * which uses GL resources (see {@link FBOCanvas#createCleanupOnWindowClosingListener()}). 
	 * Otherwise no WindowListener is created or added.
	 * 
	 * @param window the window to add the listener to (should be the window containing this canvas)
	 * @return the added listener when this is an FBOCanvas, else null
	 */
	public default WindowListener addCleanupOnWindowClosingListener(Window window) {
		if(this instanceof FBOCanvas) {
			WindowListener l = ((FBOCanvas)this).createCleanupOnWindowClosingListener();
			window.addWindowListener(l);
			return l;
		}
		return null;
	}
	
}
