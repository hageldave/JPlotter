package hageldave.jplotter.debugging.annotations;

import hageldave.jplotter.debugging.customPrinter.CustomPrinterInterface;
import hageldave.jplotter.debugging.customPrinter.StandardPrinter;
import hageldave.jplotter.debugging.panelcreators.display.DisplayPanelCreator;
import hageldave.jplotter.debugging.panelcreators.display.StandardPanelCreator;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Any getter method annotated with the DebugGetter annotation will result in a panel ({@link StandardPanelCreator} by default)
 * in the debugger which uses the information provided by the getter.
 * The ID property is used for identification of the property, as it can be coupled with the
 * setter of the corresponding property (see {@link DebugSetter}).
 * A custom {@link DisplayPanelCreator} &amp; {@link CustomPrinterInterface} also can be passed.
 * If a {@link DebugGetter} is used in combination with a DebugSetter (they share the same ID), the {@link DisplayPanelCreator} of the DebugGetter will be ignored.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DebugGetter {
    /**
     * @return the (unique) identifier of the getter method (used to connect it with a corresponding {@link DebugSetter}, needs the same ID for that)
     */
    String ID();

    /**
     * @return the {@link DisplayPanelCreator} that should be created in the debugger for this getter method,
     * the default is {@link StandardPanelCreator} if there's no DisplayPanelCreator set
     */
    Class<? extends DisplayPanelCreator> creator() default StandardPanelCreator.class;

    /**
     *
     * @return the {@link CustomPrinterInterface} should be used, when printing objects in the {@link DisplayPanelCreator},
     * the default is {@link StandardPrinter} if there's no CustomPrinterInterface set
     */
    Class<? extends CustomPrinterInterface> objectPrinter() default StandardPrinter.class;
}
