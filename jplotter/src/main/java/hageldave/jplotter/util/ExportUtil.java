package hageldave.jplotter.util;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
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

    public static JMenuBar createSaveMenu(JPlotterCanvas canvas, String path) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        JMenuBar menuBar = new JMenuBar();
        JMenu exportMenu = new JMenu("Export");
        menuBar.add(exportMenu);

        JMenuItem svgExport = new JMenuItem("SVG export");
        svgExport.addActionListener(e -> {
            Document svg = canvas.paintSVG();
            SVGUtils.documentToXMLFile(svg, new File(path + ".svg"));
            System.out.println("exported SVG.");
        });
        exportMenu.add(svgExport);

        JMenuItem pdfExport = new JMenuItem("PDF export");
        pdfExport.addActionListener(e -> {
            try {
                PDDocument pdf = canvas.paintPDF();
                pdf.save(path + ".pdf");
                pdf.close();
                System.out.println("exported PDF.");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        });
        exportMenu.add(pdfExport);

        JMenuItem pngExport = new JMenuItem("PNG export");
        pngExport.addActionListener(e -> {
            Img img = new Img(canvas.asComponent().getSize());
            img.paint(g -> canvas.asComponent().paintAll(g));
            ImageSaver.saveImage(img.getRemoteBufferedImage(), path + ".png");
            System.out.println("exported PNG.");
        });
        exportMenu.add(pngExport);

        return menuBar;
    }

    public static JMenuBar createSaveFileChooserMenu(JPlotterCanvas canvas) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        JFileChooser chooser = new JFileChooser();

        JMenuBar menuBar = new JMenuBar();
        JMenu exportMenu = new JMenu("Export");
        menuBar.add(exportMenu);

        JMenuItem svgExport = new JMenuItem("SVG export");
        svgExport.addActionListener(e -> {
            int result = chooser.showSaveDialog(canvas.asComponent());
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    Document svg = canvas.paintSVG();
                    SVGUtils.documentToXMLFile(svg, new File(chooser.getSelectedFile() + ".svg"));
                    System.out.println("exported SVG.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        exportMenu.add(svgExport);

        JMenuItem pdfExport = new JMenuItem("PDF export");
        pdfExport.addActionListener(e -> {
            int result = chooser.showSaveDialog(canvas.asComponent());
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    PDDocument pdf = canvas.paintPDF();
                    pdf.save(chooser.getSelectedFile() + ".pdf");
                    pdf.close();
                    System.out.println("exported PDF.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        exportMenu.add(pdfExport);

        JMenuItem pngExport = new JMenuItem("PNG export");
        pngExport.addActionListener(e -> {
            int result = chooser.showSaveDialog(canvas.asComponent());
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    Img img = new Img(canvas.asComponent().getSize());
                    img.paint(g -> canvas.asComponent().paintAll(g));
                    ImageSaver.saveImage(img.getRemoteBufferedImage(), chooser.getSelectedFile() + ".png");
                    System.out.println("exported PNG.");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        exportMenu.add(pngExport);

        return menuBar;
    }
}
