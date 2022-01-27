package hageldave.jplotter.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

/**
 * The PDFRenderer interface defines the method
 * {@link #renderPDF(PDDocument, PDPage, int, int, int, int)}
 * which 'renders' the PDFRenderers's content as pdf objects
 * in content streams, i.e. fills content streams with elements and appends them to the specified page.
 *
 */
public interface PDFRenderer {
    /**
     * Renders this PDFRenderers contents, that is creating
     * pdf elements and appending them to the specified page
     * within the corresponding document.
     *
     * @param doc the PDF document holding the page
     * @param page page in pdf doc to which elements are to be appended
     * @param x x coordinate of the current viewport
     * @param y y coordinate of the current viewport
     * @param w width of the current viewport
     * @param h height of the current viewport
     */
    public default void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h){}
}
