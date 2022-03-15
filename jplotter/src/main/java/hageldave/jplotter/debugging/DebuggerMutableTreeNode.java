package hageldave.jplotter.debugging;

import javax.swing.tree.DefaultMutableTreeNode;

public class DebuggerMutableTreeNode extends DefaultMutableTreeNode {

    protected Object backObject;

    public DebuggerMutableTreeNode(Object userObject, Object backObject) {
        super(userObject, true);
        this.backObject = backObject;
    }
}
