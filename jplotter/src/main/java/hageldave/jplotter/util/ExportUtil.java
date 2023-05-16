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

/**
 * The ExportUtil class contains several methods that contain boilerplate code for
 * exporting a {@link JPlotterCanvas} or a {@link JFrame} to either png, svg or pdf files.
 * It also contains methods for creating different save menus.
 *
 */
public class ExportUtil {

    /**
     * Exports {@link JPlotterCanvas} to PDF file.
     * It uses the given path as the export location.
     * The path has to include the .pdf as an ending.
     * <br/>
     * Prints to console, when exporting has been successful.
     *
     * @param canvas to export
     * @param path location where the file will be exported to
     */
    public static void canvasToPDF(JPlotterCanvas canvas, String path) {
        try {
            PDDocument doc = canvas.paintPDF();
            doc.save(path);
            doc.close();
            System.out.println("Exported as " + path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Exports {@link JFrame} to PDF file.
     * It uses the given path as the export location.
     * The path has to include the .pdf as an ending.
     * <br/>
     * Prints to console, when exporting has been successful.
     *
     * @param frame to export
     * @param path location where the file will be exported to
     */
    public static void frameToPDF(JFrame frame, String path) {
        try {
            PDDocument doc = PDFUtils.containerToPDF(frame);
            doc.save(path);
            doc.close();
            System.out.println("Exported as " + path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Exports {@link JPlotterCanvas} to SVG file.
     * It uses the given path as the export location.
     * The path has to include the .svg as an ending.
     * <br/>
     * Prints to console, when exporting has been successful.
     *
     * @param canvas to export
     * @param path location where the file will be exported to
     */
    public static void canvasToSVG(JPlotterCanvas canvas, String path) {
        Document doc = canvas.paintSVG();
        SVGUtils.documentToXMLFile(doc, new File(path));
        System.out.println("Exported as " + path);
    }

    /**
     * Exports {@link JFrame} to SVG file.
     * It uses the given path as the export location.
     * The path has to include the .svg as an ending.
     * <br/>
     * Prints to console, when exporting has been successful.
     *
     * @param frame to export
     * @param path location where the file will be exported to
     */
    public static void frameToSVG(JFrame frame, String path) {
        Document svg = SVGUtils.containerToSVG(frame.getContentPane());
        SVGUtils.documentToXMLFile(svg, new File("frame_export.svg"));
        System.out.println("Exported to " + path);
    }

    /**
     * Exports {@link JFrame} to PNG file.
     * It uses the given path as the export location.
     * The path has to include the .png as an ending.
     * <br/>
     * Prints to console, when exporting has been successful.
     *
     * @param canvas to export
     * @param path location where the file will be exported to
     */
    public static void canvasToPNG(JPlotterCanvas canvas, String path) {
        Img img = new Img(canvas.asComponent().getSize());
        img.paint(g -> canvas.asComponent().paintAll(g));
        ImageSaver.saveImage(img.getRemoteBufferedImage(), path);
        System.out.println("Exported to " + path);
    }

    /**
     * Exports {@link JFrame} to PNG file.
     * It uses the given path as the export location.
     * The path has to include the .png as an ending.
     * <br/>
     * Prints to console, when exporting has been successful.
     *
     * @param frame to export
     * @param path location where the file will be exported to
     */
    public static void frameToPNG(JFrame frame, String path) {
        Img img = new Img(frame.getContentPane().getSize());
        img.paint(g -> frame.getContentPane().paintAll(g));
        ImageSaver.saveImage(img.getRemoteBufferedImage(), path);
        System.out.println("Exported to " + path);
    }

    /**
     * Creates a {@link JMenuBar} with options to export canvas to PDF/SVG/PNG.
     * The desired path should not include .pdf/.svg/.png endings as they're added automatically.
     *
     * @param canvas to export
     * @param path where the files should be exported to
     * @return the created JMenuBar
     */
    public static JMenuBar createSaveMenu(JPlotterCanvas canvas, String path) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        JMenuBar menuBar = new JMenuBar();
        JMenu exportMenu = new JMenu("Export");
        menuBar.add(exportMenu);

        JMenuItem svgExport = new JMenuItem("SVG export");
        svgExport.addActionListener(e -> {
            canvasToSVG(canvas, path + ".svg");
        });
        exportMenu.add(svgExport);

        JMenuItem pdfExport = new JMenuItem("PDF export");
        pdfExport.addActionListener(e -> {
            canvasToPDF(canvas, path + ".pdf");
        });
        exportMenu.add(pdfExport);

        JMenuItem pngExport = new JMenuItem("PNG export");
        pngExport.addActionListener(e -> {
            canvasToPNG(canvas, path + ".png");
        });
        exportMenu.add(pngExport);
        return menuBar;
    }

    /**
     * Creates a {@link JMenuBar} with options to export canvas to PDF/SVG/PNG.
     * The desired path should not include .pdf/.svg/.png endings as they're added automatically.
     *
     * @param frame to export
     * @param path where the files should be exported to
     * @return the created JMenuBar
     */
    public static JMenuBar createSaveMenu(JFrame frame, String path) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        JMenuBar menuBar = new JMenuBar();
        JMenu exportMenu = new JMenu("Export");
        menuBar.add(exportMenu);

        JMenuItem svgExport = new JMenuItem("SVG export");
        svgExport.addActionListener(e -> {
            frameToSVG(frame, path + ".svg");
        });
        exportMenu.add(svgExport);

        JMenuItem pdfExport = new JMenuItem("PDF export");
        pdfExport.addActionListener(e -> {
            frameToSVG(frame, path + ".pdf");
        });
        exportMenu.add(pdfExport);

        JMenuItem pngExport = new JMenuItem("PNG export");
        pngExport.addActionListener(e -> {
            frameToSVG(frame, path + ".png");
        });
        exportMenu.add(pngExport);
        return menuBar;
    }

    /**
     * Creates a {@link JMenuBar} with options to export {@link JPlotterCanvas} to PDF/SVG/PNG by a {@link JFileChooser}.
     *
     * @param canvas to export
     * @return the created JMenuBar
     */
    public static JMenuBar createSaveFileChooserMenu(JPlotterCanvas canvas) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        JMenuBar menuBar = new JMenuBar();
        JMenu exportMenu = new JMenu("Export to");
        menuBar.add(exportMenu);

        JMenuItem svgExport = new JMenuItem("SVG export");
        svgExport.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(canvas.asComponent());
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    canvasToSVG(canvas, chooser.getSelectedFile() + ".svg");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        exportMenu.add(svgExport);

        JMenuItem pdfExport = new JMenuItem("PDF export");
        pdfExport.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(canvas.asComponent());
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    canvasToPDF(canvas, chooser.getSelectedFile() + ".pdf");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        exportMenu.add(pdfExport);

        JMenuItem pngExport = new JMenuItem("PNG export");
        pngExport.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(canvas.asComponent());
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    canvasToPNG(canvas, chooser.getSelectedFile() + ".png");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        exportMenu.add(pngExport);

        return menuBar;
    }

    /**
     * Creates a {@link JMenuBar} with options to export {@link JFrame} to PDF/SVG/PNG by a {@link JFileChooser}.
     *
     * @param frame to export
     * @return the created JMenuBar
     */
    public static JMenuBar createSaveFileChooserMenu(JFrame frame) {
        System.setProperty("apple.laf.useScreenMenuBar", "true");
        JMenuBar menuBar = new JMenuBar();
        JMenu exportMenu = new JMenu("Export to");
        menuBar.add(exportMenu);

        JMenuItem svgExport = new JMenuItem("SVG export");
        svgExport.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    frameToSVG(frame, chooser.getSelectedFile() + ".svg");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        exportMenu.add(svgExport);

        JMenuItem pdfExport = new JMenuItem("PDF export");
        pdfExport.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    frameToPDF(frame, chooser.getSelectedFile() + ".pdf");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        exportMenu.add(pdfExport);

        JMenuItem pngExport = new JMenuItem("PNG export");
        pngExport.addActionListener(e -> {
            JFileChooser chooser = new JFileChooser();
            int result = chooser.showSaveDialog(frame);
            if (result == JFileChooser.APPROVE_OPTION) {
                try {
                    frameToPNG(frame, chooser.getSelectedFile() + ".png");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        });
        exportMenu.add(pngExport);

        return menuBar;
    }
}
