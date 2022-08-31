package hageldave.jplotter.debugging.customPrinter;

import hageldave.jplotter.debugging.panelcreators.display.DisplayPanelCreator;

/**
 * The CustomPrinterInterface interface contains methods to print objects a certain way,
 * without changing their toString() method.
 */
public interface CustomPrinterInterface {
    /**
     * The print interface can be used to print the object a certain way.
     * If implemented in a printer class, it can be passed to a {@link DisplayPanelCreator}.
     * A custom printer that has been passed to a DisplayPanelCreator will then be
     * called for every variable in the currently selected object.
     *
     * @param object to print
     * @param variableName currently visited variable
     * @return custom print string of object
     */
    public String print(Object object, String variableName);
}
