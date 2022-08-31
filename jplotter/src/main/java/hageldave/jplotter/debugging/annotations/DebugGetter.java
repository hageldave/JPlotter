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
 * The key property is used for identification of the property, as it can be coupled with the
 * setter of the corresponding property (see {@link DebugSetter}).
 *
 * A custom {@link DisplayPanelCreator} & {@link CustomPrinterInterface} also can be passed.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface DebugGetter {
    public String key();
    public Class<? extends DisplayPanelCreator> creator() default StandardPanelCreator.class;
    public Class<? extends CustomPrinterInterface> objectPrinter() default StandardPrinter.class;
}
