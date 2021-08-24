package hageldave.jplotter.pdf;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

public interface PDFRenderer {
    public default void renderPDF(PDDocument doc, PDPage page, int x, int y, int w, int h){}
}
