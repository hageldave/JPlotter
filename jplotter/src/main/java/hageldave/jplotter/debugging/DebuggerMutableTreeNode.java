package hageldave.jplotter.debugging;

import javax.swing.tree.DefaultMutableTreeNode;

public class DebuggerMutableTreeNode extends DefaultMutableTreeNode {

    protected Object hiddenObject;

    public DebuggerMutableTreeNode(Object userObject, Object hiddenObject) {
        super(userObject, true);
        this.hiddenObject = hiddenObject;
    }

    public Object getHiddenObject() {
        return hiddenObject;
    }

    public void setHiddenObject(Object hiddenObject) {
        this.hiddenObject = hiddenObject;
    }
}
