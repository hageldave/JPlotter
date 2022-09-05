package hageldave.jplotter.debugging.annotations;

import hageldave.jplotter.debugging.panelcreators.control.ControlPanelCreator;
import hageldave.jplotter.debugging.panelcreators.display.DisplayPanelCreator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any setter method annotated with the DebugSetter annotation will create the
 * {@link ControlPanelCreator} passed to the annotation.
 * The key of the DebugSetter is used to couple it with the corresponding DebugGetter (has to have the same key).
 * With the DebugGetter the current information of the object can be retrieved, while the DebugSetter is used to set the information
 * that has been manipulated by the ControlPanelCreator.
 * If a {@link DebugGetter} is used in combination with a DebugSetter (they share the same key), the {@link DisplayPanelCreator} of the DebugGetter will be ignored.
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DebugSetter {
    /**
     * @return the (unique) identifier of the setter method (used to connect it with a corresponding {@link DebugGetter}, needs the same key for that)
     */
    String key();

    /**
     * @return the {@link ControlPanelCreator} that should be created in the debugger for this setter method
     */
    Class<? extends ControlPanelCreator> creator();
}
