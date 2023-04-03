package hageldave.jplotter;

import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.renderables.NewText;
import hageldave.jplotter.renderables.Text;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.svg.SVGUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

public class TextComparison {
    @SuppressWarnings("resource" /* compiler is too dumb to realize there is no leak */)
    public static void main(String[] args) {
        int fontSize = 19;
        int fontStyle = Font.PLAIN;

        // Test old text features here
        Text oldTextObject1 = new Text("Test object 1", 19, Font.PLAIN, Color.BLUE);
        oldTextObject1.setOrigin(10, 100);
        oldTextObject1.setBackground(Color.RED);
        oldTextObject1.setAngle(0.2);

        Text oldTextObject2 = new Text("Test object 2", 23, Font.ITALIC, Color.DARK_GRAY);
        oldTextObject2.setOrigin(0, 50);
        oldTextObject2.setAngle(0.2);

        NewText newTextObject1 = new NewText("Test object 1", fontSize, fontStyle, Color.BLUE);
        newTextObject1.setOrigin(10, 100);
        newTextObject1.setBackground(Color.RED);
        newTextObject1.setAngle(0.2);

        NewText newTextObject2 = new NewText("Test object 2", 23, Font.ITALIC, Color.DARK_GRAY);
        newTextObject2.setOrigin(0, 50);
        newTextObject2.setAngle(0.2);


        // This sets up the first frame, whcih tests the old text object / renderer
        CoordSysRenderer oldCoordsys = new CoordSysRenderer();

        // okay we're good to go, lets display the data in a coordinate system
        CompleteRenderer oldContent = new CompleteRenderer();
        oldCoordsys.setContent(oldContent
                .addItemToRender(oldTextObject1)
                .addItemToRender(oldTextObject2));
        // lets set the coordinate view to cover the whole sampling space
        oldCoordsys.setCoordinateView(-550, -20, 550, 210);

        // display the coordinate system on a blank canvas
        boolean useOpenGL = false;
        JPlotterCanvas oldTextCanvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
        oldTextCanvas.setRenderer(oldCoordsys);

        new CoordSysPanning(oldTextCanvas, oldCoordsys).register();



        // lets put a JFrame around it all and launch
        JFrame oldTextFrame = new JFrame("Compare text objects | old");
        oldTextFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        oldTextFrame.getContentPane().add(oldTextCanvas.asComponent());
        oldTextCanvas.asComponent().setPreferredSize(new Dimension(800, 500));
        oldTextCanvas.asComponent().setBackground(Color.WHITE);

        oldTextCanvas.addCleanupOnWindowClosingListener(oldTextFrame);

        SwingUtilities.invokeLater(()->{
            oldTextFrame.pack();
            oldTextFrame.setVisible(true);
        });

        // add a pop up menu (on right click) for exporting to SVG or PNG
        PopupMenu menu = new PopupMenu();
        oldTextCanvas.asComponent().add(menu);
        oldTextCanvas.asComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e))
                    menu.show(oldTextCanvas.asComponent(), e.getX(), e.getY());
            }
        });
        MenuItem svgExport = new MenuItem("SVG export");
        svgExport.addActionListener(e->{
            Document svg = SVGUtils.containerToSVG(oldTextFrame.getContentPane());
            SVGUtils.documentToXMLFile(svg, new File("text_comparison_old.svg"));
            System.out.println("exported SVG.");
        });
        menu.add(svgExport);

        MenuItem pdfExport = new MenuItem("PDF export");
        pdfExport.addActionListener(e->{
            try {
                PDDocument pdf = PDFUtils.containerToPDF(oldTextFrame.getContentPane());
                pdf.save("text_comparison_old.pdf");
                pdf.close();
                System.out.println("exported PDF.");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        });
        menu.add(pdfExport);

        MenuItem pngExport = new MenuItem("PNG export");
        pngExport.addActionListener(e->{
            Img img = new Img(oldTextFrame.getContentPane().getSize());
            img.paint(g -> oldTextFrame.getContentPane().paintAll(g));
            ImageSaver.saveImage(img.getRemoteBufferedImage(), "text_comparison_old.png");
            System.out.println("exported PNG.");
        });
        menu.add(pngExport);


        // This sets up the second frame which tests the new text object / renderer
        CoordSysRenderer newCoordsys = new CoordSysRenderer();

        // okay we're good to go, lets display the data in a coordinate system
        CompleteRenderer newContent = new CompleteRenderer();
        newCoordsys.setContent(newContent
                .addItemToRender(newTextObject1)
                .addItemToRender(newTextObject2));
        // lets set the coordinate view to cover the whole sampling space
        newCoordsys.setCoordinateView(-550, -20, 550, 210);

        // display the coordinate system on a blank canvas
        JPlotterCanvas newTextCanvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
        newTextCanvas.setRenderer(newCoordsys);

        new CoordSysPanning(newTextCanvas, newCoordsys).register();

        // lets put a JFrame around it all and launch
        JFrame newTextFrame = new JFrame("Compare text objects | new");
        newTextFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        newTextFrame.getContentPane().add(newTextCanvas.asComponent());
        newTextCanvas.asComponent().setPreferredSize(new Dimension(800, 500));
        newTextCanvas.asComponent().setBackground(Color.WHITE);

        newTextCanvas.addCleanupOnWindowClosingListener(newTextFrame);

        SwingUtilities.invokeLater(()->{
            newTextFrame.pack();
            newTextFrame.setVisible(true);
        });

        // add a pop up menu (on right click) for exporting to SVG or PNG
        PopupMenu newMenu = new PopupMenu();
        newTextCanvas.asComponent().add(newMenu);
        newTextCanvas.asComponent().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(SwingUtilities.isRightMouseButton(e))
                    newMenu.show(newTextCanvas.asComponent(), e.getX(), e.getY());
            }
        });
        MenuItem newSvgExport = new MenuItem("SVG export");
        newSvgExport.addActionListener(e->{
            Document svg = SVGUtils.containerToSVG(newTextFrame.getContentPane());
            SVGUtils.documentToXMLFile(svg, new File("text_comparison_new.svg"));
            System.out.println("exported SVG.");
        });
        newMenu.add(newSvgExport);

        MenuItem newPdfExport = new MenuItem("PDF export");
        newPdfExport.addActionListener(e->{
            try {
                PDDocument pdf = PDFUtils.containerToPDF(newTextFrame.getContentPane());
                pdf.save("text_comparison_new.pdf");
                pdf.close();
                System.out.println("exported PDF.");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        });
        newMenu.add(newPdfExport);

        MenuItem newPngExport = new MenuItem("PNG export");
        newPngExport.addActionListener(e->{
            Img img = new Img(newTextFrame.getContentPane().getSize());
            img.paint(g -> newTextFrame.getContentPane().paintAll(g));
            ImageSaver.saveImage(img.getRemoteBufferedImage(), "text_comparison_new.png");
            System.out.println("exported PNG.");
        });
        newMenu.add(newPngExport);
    }
}
