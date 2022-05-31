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
        Renderer ren = canvas.getRenderer();

        Class<?> cnvsClass = canvas.getClass();
        Field[] fields = cnvsClass.getDeclaredFields();
        DebuggerMutableTreeNode root = new DebuggerMutableTreeNode(canvas.getClass().getSimpleName(), canvas.getClass());

        for (Field field : fields) {
            Class<?> type = field.getType();
            field.setAccessible(true);
            boolean rendererAssignable = Renderer.class.isAssignableFrom(type);

            if (rendererAssignable) {
                DebuggerMutableTreeNode firstRendererN = new DebuggerMutableTreeNode("(" + ren.getClass().getSimpleName() + ") " + field.getName(), ren);
                root.add(firstRendererN);
                constructTree(ren, firstRendererN);
            }
        }
        return root;
    }

    protected static TreeNode constructTree(Renderer ren, DefaultMutableTreeNode node) throws IllegalAccessException {
        Class<? extends Renderer> class1 = ren.getClass();
        Class<?> superclass = class1.getSuperclass();

        Field[] fields = class1.getDeclaredFields();
        Field[] superclassFields = superclass.getDeclaredFields();

        ArrayList<Field> allFields = new ArrayList<>();
        Collections.addAll(allFields, fields);
        Collections.addAll(allFields, superclassFields);

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
                    DebuggerMutableTreeNode newNode = new DebuggerMutableTreeNode("(" + nested.getClass().getSimpleName() + ") " + field.getName(), nested);
                    node.add(newNode);
                    constructTree(nested, newNode);
                }
            } else if (renderableAssignable) {
                Renderable nested = (Renderable) field.get(ren);
                DebuggerMutableTreeNode newNode = new DebuggerMutableTreeNode("(" + nested.getClass().getSimpleName() + ") " + field.getName(), nested);
                node.add(newNode);
            } else if (isIterable) {
                java.util.List<?> iterableField = (java.util.List<?>) field.get(ren);
                if (iterableField.size() > 0) {
                    DefaultMutableTreeNode listNode = new DefaultMutableTreeNode(field.getName());

                    for (Object iteratedObject : iterableField) {
                        Class<?> iteratedClass = iteratedObject.getClass();
                        if (Renderable.class.isAssignableFrom(iteratedClass)) {
                            DebuggerMutableTreeNode newNode = new DebuggerMutableTreeNode("(" + iteratedClass.getSimpleName() + ") Hash: @" + iteratedObject.hashCode(), iteratedObject);
                            listNode.add(newNode);
                        }
                    }
                    node.add(listNode);
                }
            }
        }
        return node;
    }
}


