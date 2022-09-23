package hageldave.jplotter.debugging.customPrinter;

/**
 * The default object printer. It just prints the object using its toString() method.
 */
public class StandardPrinter implements CustomPrinterInterface {
    @Override
    public String print(Object object, String variableName) {
        return object.toString();
    }
}
