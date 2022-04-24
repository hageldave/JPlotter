package hageldave.jplotter.interaction.kml;

import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The KeyMaskListener class provides a way to check if a predefined set of keys on the keyboard/mouse/... is pressed.
 *
 * Therefore, a set of keys has to be passed in the constructor.
 * The developer can then check if the specified set of keys is currently pressed by the
 * {@link KeyMaskListener#areKeysPressed} method.
 * The {@link KeyMaskListener#areKeysPressed} method also checks if there are more keys pressed than initially specified.
 * If that is the case, it will also return false.
 *
 * A common use case would be the restriction of interaction interfaces (e.g. {@link CoordSysScrollZoom}) to only work
 * when a specific set of keys is pressed to prevent multiple events happening at the same time (such as Panning and rectangle selection).
 *
 * To make the {@link KeyMaskListener#areKeysPressed} return true only when no keys are currently pressed,
 * no keys (or the integer value 0) has to be passed to the constructor.
 *
 * All the interaction interfaces JPlotter offers ({@link CoordSysScrollZoom}, {@link CoordSysPanning}, ...), support the use of the KeyMaskListener.
 *
 */
public class KeyMaskListener extends KeyAdapter {
    protected boolean areKeysPressed = false;
    protected boolean noMasking = false;
    final protected HashMap<Integer, Boolean> keyPressedMap = new HashMap<>();
    final protected ArrayList<Integer> keysToPress = new ArrayList<>();

    /**
     * Creates a new {@link KeyMaskListener} object which can be used to check if the
     * predefined set of keys is pressed.
     *
     * @param keys which have to be pressed
     */
    public KeyMaskListener(final int... keys) {
        // checks if either 0 arguments are passed or the number 0 as the only argument
        if (keys.length == 0 || (keys.length == 1 && keys[0] == 0)) {
            this.noMasking = true;
        }
        for (int j : keys) {
            this.keysToPress.add(j);
        }
        Collections.sort(this.keysToPress);
    }

    @Override
    public void keyPressed(KeyEvent e) {
        keyPressedMap.put(e.getKeyCode(), true);
        areKeysPressed = this.keysToPress.equals(getPressedKeys());
    }

    @Override
    public void keyReleased(KeyEvent e) {
        keyPressedMap.put(e.getKeyCode(), false);
        areKeysPressed = this.keysToPress.equals(getPressedKeys());
    }

    /**
     * @return true if the keys currently pressed are exactly the same as specified in the constructor;
     *         false if there are more, less or wrong keys currently pressed.
     */
    public boolean areKeysPressed() {
        if (noMasking)
            return getPressedKeys().size() <= 0;
        return areKeysPressed;
    }

    protected ArrayList<Integer> getPressedKeys() {
        ArrayList<Integer> pressedKeys = new ArrayList<>(keyPressedMap.size());
        for (Map.Entry<Integer, Boolean> v : keyPressedMap.entrySet())
            if (v.getValue())
                pressedKeys.add(v.getKey());

        Collections.sort(pressedKeys);
        return pressedKeys;
    }
}
