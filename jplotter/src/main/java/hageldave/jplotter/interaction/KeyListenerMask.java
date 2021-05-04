package hageldave.jplotter.interaction;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.HashMap;

/**
 *
 */
public class KeyListenerMask extends KeyAdapter {
    protected boolean keyTyped = false;
    protected boolean noMasking = false;
    final protected HashMap<Integer, Boolean> keyTypedInMask = new HashMap<>();

    public KeyListenerMask(final int[] extModifierMask) {
        for (int i = 0; i < extModifierMask.length; i++) {
            this.keyTypedInMask.put(extModifierMask[i], false);
        }
    }

    public KeyListenerMask(final int extModifierMask) {
        if (extModifierMask == 0) {
            this.noMasking = true;
        } else {
            this.keyTypedInMask.put(extModifierMask, false);
        }
    }

    @Override
    public void keyPressed(KeyEvent e) {
        if (!noMasking) {
            if (keyTypedInMask.containsKey(e.getKeyCode())) {
                keyTypedInMask.put(e.getKeyCode(), true);
            }
            boolean temp = true;
            for (Boolean values : keyTypedInMask.values()) {
                if (!values) {
                    temp = false;
                }
            }
            if (temp)
                keyTyped = true;
        }
    }

    @Override
    public void keyReleased(KeyEvent e) {
        if (!noMasking) {
            keyTyped = false;
            if (keyTypedInMask.containsKey(e.getKeyCode())) {
                keyTypedInMask.put(e.getKeyCode(), false);
            }
        }
    }

    public boolean isKeyTyped() {
        if (noMasking) {
            return true;
        }
        return keyTyped;
    }
}
