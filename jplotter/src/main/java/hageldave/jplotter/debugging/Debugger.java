package hageldave.jplotter.debugging;

import hageldave.jplotter.canvas.JPlotterCanvas;
import hageldave.jplotter.renderables.Renderable;
import hageldave.jplotter.renderers.Renderer;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeNode;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;

public class Debugger {
    protected static TreeNode getAllRenderersOnCanvas(JPlotterCanvas canvas) throws IllegalAccessException {
        // canvas get renderer
        Renderer ren = canvas.getRenderer();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode(ren);
        constructTree(ren, root);
        return root;
    }

    protected static TreeNode constructTree(Renderer ren, DefaultMutableTreeNode node) throws IllegalAccessException {
        Class<? extends Renderer> class1 = ren.getClass();
        //Class<?>[] interfaces = class1.getInterfaces();
        Class<?> superclass = class1.getSuperclass();


        /*ArrayList<Field> interfaceFields = new ArrayList<>();
        for (Class<?> singleInterface : interfaces) {
            Collections.addAll(interfaceFields, singleInterface.getDeclaredFields());
        }*/

        Field[] fields = class1.getDeclaredFields();
        Field[] superclassFields = superclass.getDeclaredFields();

        ArrayList<Field> allFields = new ArrayList<>();
        Collections.addAll(allFields, fields);
        Collections.addAll(allFields, superclassFields);
        //Collections.addAll(allFields, interfaceFields.toArray(new Field[]{}));

        for (Field field : allFields) {
            Class<?> type = field.getType();
            field.setAccessible(true);

            boolean rendererAssignable = Renderer.class.isAssignableFrom(type);
            boolean renderableAssignable = Renderable.class.isAssignableFrom(type);
            boolean isIterable = java.util.List.class.isAssignableFrom(type);

            if (rendererAssignable) {
                Renderer nested = (Renderer) field.get(ren);
                // null check, as renderers might be null in coordsysrenderer
                if (nested != null) {
                    DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(nested);
                    node.add(newNode);
                    constructTree(nested, newNode);
                }
            } else if (renderableAssignable) {
                Renderable nested = (Renderable) field.get(ren);
                DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(nested);
                node.add(newNode);
            } else if (isIterable) {
                java.util.List<?> iterableField = (java.util.List<?>) field.get(ren);
                for (Object iteratedObject : iterableField) {
                    Class<?> iteratedClass = iteratedObject.getClass();
                    if (Renderable.class.isAssignableFrom(iteratedClass)) {
                        DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(iteratedObject);
                        node.add(newNode);
                    }
                }
            }
        }
        return node;
    }
}


