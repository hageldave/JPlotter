package hageldave.jplotter.util;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class ExportUtil {
    public static void canvasToPDF(JPlotterCanvas canvas, String path) {
        try {
            PDDocument doc = canvas.paintPDF();
            doc.save(path); // specifies the filename of the export
            doc.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void frameToPDF(JFrame frame, String path) {
        try {
            PDDocument doc = PDFUtils.containerToPDF(frame.getContentPane()); // pass the container to be exported
            doc.save(path); // specifies the filename of the export
            doc.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void canvasToSVG(JPlotterCanvas canvas, String path) {
        Document doc = canvas.paintSVG();
        SVGUtils.documentToXMLFile(doc, new File(path));
    }

    public static void frameToSVG(JFrame frame, String path) {
        Document svg = SVGUtils.containerToSVG(frame.getContentPane()); // pass the container to be exported
        SVGUtils.documentToXMLFile(svg, new File("frame_export.svg"));
    }

    public static void canvasToPNG(JPlotterCanvas canvas, String path) {
        Img img = new Img(canvas.asComponent().getSize());
        img.paint(g -> canvas.asComponent().paintAll(g));
        ImageSaver.saveImage(img.getRemoteBufferedImage(), path);
    }

    public static void frameToPNG(JFrame frame, String path) {
        Img img = new Img(frame.getContentPane().getSize());
        img.paint(g -> frame.getContentPane().paintAll(g));
        ImageSaver.saveImage(img.getRemoteBufferedImage(), path);
    }

    public static PopupMenu createSaveMenu(JPlotterCanvas canvas, String path) {
        // add a pop up menu (on right click) for exporting to SVG or PNG
        PopupMenu menu = new PopupMenu();
        canvas.asComponent().add(menu);
        canvas.asComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e))
                    menu.show(canvas.asComponent(), e.getX(), e.getY());
            }
        });
        MenuItem svgExport = new MenuItem("SVG export");
        svgExport.addActionListener(e->{
            Document svg = canvas.paintSVG();
            SVGUtils.documentToXMLFile(svg, new File(path + ".svg"));
            System.out.println("exported SVG.");
        });
        menu.add(svgExport);

        MenuItem pdfExport = new MenuItem("PDF export");
        pdfExport.addActionListener(e->{
            try {
                PDDocument pdf = canvas.paintPDF();
                pdf.save(path + ".pdf");
                pdf.close();
                System.out.println("exported PDF.");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        });
        menu.add(pdfExport);

        MenuItem pngExport = new MenuItem("PNG export");
        pngExport.addActionListener(e->{
            Img img = new Img(canvas.asComponent().getSize());
            img.paint(g -> canvas.asComponent().paintAll(g));
            ImageSaver.saveImage(img.getRemoteBufferedImage(), path + ".png");
            System.out.println("exported PNG.");
        });
        menu.add(pngExport);

        return menu;
    }
}
