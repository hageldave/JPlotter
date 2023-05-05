package hageldave.jplotter;


import hageldave.imagingkit.core.Img;
import hageldave.imagingkit.core.io.ImageSaver;
import hageldave.jplotter.canvas.BlankCanvas;
import hageldave.jplotter.canvas.BlankCanvasFallback;
import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.debugging.ui.DebuggerUI;
import hageldave.jplotter.interaction.CoordSysPanning;
import hageldave.jplotter.pdf.PDFUtils;
import hageldave.jplotter.renderables.Lines;
import hageldave.jplotter.renderables.NewText;
import hageldave.jplotter.renderers.CompleteRenderer;
import hageldave.jplotter.renderers.CoordSysRenderer;
import hageldave.jplotter.svg.SVGUtils;
import hageldave.jplotter.util.PickingRegistry;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.w3c.dom.Document;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;

import static hageldave.jplotter.renderables.NewText.STRIKETHROUGH;
import static hageldave.jplotter.renderables.NewText.UNDERLINE;

public class NewTextTest {
    @SuppressWarnings("resource" /* compiler is too dumb to realize there is no leak */)
    public static void main(String[] args) {
        int fontSize = 19;
        int fontStyle = Font.PLAIN;

        Lines textLines = new Lines();
        CoordSysRenderer coordsys = new CoordSysRenderer();
        PickingRegistry<NewText> pr = new PickingRegistry<>();

        // Test text underlining
        NewText textUnderline = new NewText("testing the underlining", fontSize, fontStyle);
        textUnderline.setOrigin(0, 135);
        textUnderline.setColor(Color.RED);
        textUnderline.setTextDecoration(UNDERLINE);
        textUnderline.setPickColor(pr.getNewID());
        textUnderline.setTransformationCenter(1.0, 1.0);

        // Test text strikethrough
        NewText textStrikeThrough = new NewText("testing the strikethrough", fontSize, fontStyle);
        textStrikeThrough.setOrigin(10, 135);
        textStrikeThrough.setColor(Color.BLUE);
        textStrikeThrough.setTextDecoration(STRIKETHROUGH);
        textStrikeThrough.setPickColor(pr.getNewID());
        textStrikeThrough.setTransformationCenter(0.0, 1.0);

        // Test anchor points
        NewText anchorPoints = new NewText("testing the anchorpoint \n (2, 2)", fontSize, fontStyle);
        anchorPoints.setOrigin(0, 100);
        anchorPoints.setBackground(Color.ORANGE);
        anchorPoints.setPickColor(pr.getNewID());
        anchorPoints.setTransformationCenter(1.0, 0.5);

        // Test text insets with background
        NewText textInsets = new NewText("testing the insets \n with multiple lines", fontSize, fontStyle);
        textInsets.setOrigin(10, 110);
        textInsets.setBackground(Color.MAGENTA);
        textInsets.setTextDecoration(UNDERLINE);
        textInsets.setPickColor(pr.getNewID());
        textInsets.setInsets(new Insets(10, 10, 10, 10));
        textInsets.setTransformationCenter(0.0, 1.0);

        // Test text insets with background and rotation
        NewText textInsetsWithRotation = new NewText("testing the insets \n with rotation", fontSize, fontStyle);
        textInsetsWithRotation.setOrigin(0, 50);
        Color textInsetsWithRotationColor = new Color(Color.GREEN.getRed(), Color.GREEN.getGreen(), Color.GREEN.getBlue(), 100);
        textInsetsWithRotation.setBackground(textInsetsWithRotationColor);
        textInsetsWithRotation.setPickColor(pr.getNewID());
        textInsetsWithRotation.setInsets(new Insets(10, 10, 10, 10));
        textInsetsWithRotation.setAngle(0.2);
        textInsetsWithRotation.setTransformationCenter(1.0, 1.0);

        // Testing strikethrough, insets, rotation all in one
        NewText textInsetsWithRotationAndStrikethrough = new NewText("testing the insets \n with rotation and strikethrough", fontSize, fontStyle);
        textInsetsWithRotationAndStrikethrough.setOrigin(0, 50);
        textInsetsWithRotationAndStrikethrough.setBackground(Color.CYAN);
        textInsetsWithRotationAndStrikethrough.setInsets(new Insets(10, 10, 10, 10));
        textInsetsWithRotationAndStrikethrough.setAngle(0.2);
        textInsetsWithRotationAndStrikethrough.setColor(new Color(0, 0, 255, 100));
        textInsetsWithRotationAndStrikethrough.setPickColor(pr.getNewID());
        textInsetsWithRotationAndStrikethrough.setTextDecoration(STRIKETHROUGH);
        textInsetsWithRotationAndStrikethrough.setTransformationCenter(0.0, 1.0);

        // Testing latex rendering in text mode
        NewText textLatex = new NewText("##BEGINLATEX## \\text{testing the latex rendering} \\\\ \\text{in text mode}", fontSize, fontStyle);
        textLatex.setOrigin(0, 200);
        textLatex.setBackground(Color.LIGHT_GRAY);
        textLatex.setTextDecoration(UNDERLINE);
        textLatex.setPickColor(pr.getNewID());
        textLatex.setAngle(0.2);
        textLatex.setInsets(new Insets(5, 10, 5, 10));
        textLatex.setTransformationCenter(1.0, 1.0);

        // Testing latex rendering in math mode
        NewText textLatexMath = new NewText("##BEGINLATEX## (\\bigwedge_{i=1}^{1} F_i) \\wedge (\\bigwedge_{i=1}^{1} G_i) \\equiv \\bigwedge_{i=1}^{1} (F_i \\wedge G_i)", fontSize, fontStyle);
        textLatexMath.setOrigin(10, 200);
        Color textLatexMathColor = new Color(Color.PINK.getRed(), Color.PINK.getGreen(), Color.PINK.getBlue(), 100);
        textLatexMath.setBackground(textLatexMathColor);
        textLatexMath.setPickColor(pr.getNewID());
        textLatexMath.setInsets(new Insets(5, 10, 5, 10));
        textLatexMath.setTransformationCenter(0.0, 1.0);

        NewText testYPositioning = new NewText("end", fontSize, fontStyle);
        testYPositioning.setLatex(true);
        testYPositioning.setTransformationCenter(0, 0);
        testYPositioning.setOrigin(-500, 10);
        testYPositioning.setColor(new Color(0, 0, 255, 100));
        testYPositioning.setBackground(new Color(255, 0, 0));

        NewText testYPositioningWithBackground = new NewText("start", fontSize, fontStyle);
        testYPositioningWithBackground.setLatex(true);
        testYPositioningWithBackground.setOrigin(-500, 0);
        testYPositioningWithBackground.setTransformationCenter(1, 0);
        testYPositioningWithBackground.setBackground(Color.RED);


        // okay we're good to go, lets display the data in a coordinate system
        CompleteRenderer content = new CompleteRenderer();
        coordsys.setContent(content
                .addItemToRender(textLines)
                .addItemToRender(anchorPoints)
                .addItemToRender(textUnderline)
                .addItemToRender(textStrikeThrough)
                .addItemToRender(textInsets)
                .addItemToRender(textInsetsWithRotation)
                .addItemToRender(textInsetsWithRotationAndStrikethrough)
                .addItemToRender(textLatex)
                .addItemToRender(textLatexMath)
                .addItemToRender(testYPositioning)
                .addItemToRender(testYPositioningWithBackground));
        // lets set the coordinate view to cover the whole sampling space
        coordsys.setCoordinateView(-550, -20, 550, 210);

        // display the coordinate system on a blank canvas
        boolean useOpenGL = false;
        JPlotterCanvas canvas = useOpenGL ? new BlankCanvas() : new BlankCanvasFallback();
        canvas.setRenderer(coordsys);

        new CoordSysPanning(canvas, coordsys).register();

        // Setup wrapping Container
        JPanel wrapper = new JPanel();
        wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));

        JPanel labelWrapper = new JPanel(new BorderLayout());
        labelWrapper.setBackground(Color.WHITE);
        labelWrapper.setBorder(new EmptyBorder(10, 0, 10, 0));

        JLabel undertitleLabel = new JLabel("Testing the new text features", SwingConstants.CENTER);

        labelWrapper.add(undertitleLabel, BorderLayout.CENTER);

        wrapper.add(canvas.asComponent());
        wrapper.add(labelWrapper);

        // lets put a JFrame around it all and launch
        JFrame frame = new JFrame("Text features example");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.getContentPane().add(wrapper);
        canvas.asComponent().setPreferredSize(new Dimension(800, 500));
        canvas.asComponent().setBackground(Color.WHITE);

        canvas.addCleanupOnWindowClosingListener(frame);

        SwingUtilities.invokeLater(()->{
            frame.pack();
            frame.setVisible(true);
        });

        DebuggerUI debuggerUI = new DebuggerUI(canvas);
        debuggerUI.display();

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
            SVGUtils.documentToXMLFile(svg, new File("new_text_test.svg"));
            System.out.println("exported SVG.");
        });
        menu.add(svgExport);

        MenuItem svgContainerExport = new MenuItem("SVG container export");
        svgContainerExport.addActionListener(e->{
            Document svg = SVGUtils.containerToSVG(frame.getContentPane());
            SVGUtils.documentToXMLFile(svg, new File("new_container_text_test.svg"));
            System.out.println("exported SVG container.");
        });
        menu.add(svgContainerExport);

        MenuItem pdfExport = new MenuItem("PDF export");
        pdfExport.addActionListener(e->{
            try {
                PDDocument pdf = canvas.paintPDF();
                pdf.save("new_text_test.pdf");
                pdf.close();
                System.out.println("exported PDF.");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        });
        menu.add(pdfExport);

        MenuItem pdfContainerExport = new MenuItem("PDF container export");
        pdfContainerExport.addActionListener(e->{
            try {
                PDDocument pdf = PDFUtils.containerToPDF(frame.getContentPane());
                pdf.save("new_container_text_test.pdf");
                pdf.close();
                System.out.println("exported PDF container.");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

        });
        menu.add(pdfContainerExport);

        MenuItem pngExport = new MenuItem("PNG export");
        pngExport.addActionListener(e->{
            Img img = new Img(frame.getContentPane().getSize());
            img.paint(g -> frame.getContentPane().paintAll(g));
            ImageSaver.saveImage(img.getRemoteBufferedImage(), "new_text_test.png");
            System.out.println("exported PNG.");
        });
        menu.add(pngExport);

        MenuItem exportAll = new MenuItem("Export all");
        exportAll.addActionListener(e->{
            // Single SVG
            Document svg = canvas.paintSVG();
            SVGUtils.documentToXMLFile(svg, new File("new_text_test.svg"));
            System.out.println("exported SVG.");

            // Container SVG
            Document containerSvg = SVGUtils.containerToSVG(frame.getContentPane());
            SVGUtils.documentToXMLFile(containerSvg, new File("new_container_text_test.svg"));
            System.out.println("exported SVG container.");

            try {
                // Single PDF
                PDDocument pdf = canvas.paintPDF();
                pdf.save("new_text_test.pdf");
                pdf.close();
                System.out.println("exported PDF.");

                // Container PDF
                PDDocument containerPdf = PDFUtils.containerToPDF(frame.getContentPane());
                containerPdf.save("new_container_text_test.pdf");
                containerPdf.close();
                System.out.println("exported PDF container.");
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }

            // PNG
            Img img = new Img(frame.getContentPane().getSize());
            img.paint(g -> frame.getContentPane().paintAll(g));
            ImageSaver.saveImage(img.getRemoteBufferedImage(), "new_text_test.png");
            System.out.println("exported PNG.");
        });
        menu.add(exportAll);

    }
}