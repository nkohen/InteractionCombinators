package FourCombinatorImplementation;

public class TestUtil {
    final static int TEST_SIZE = 10;
    final static String TEST_NAME = "testname";

    public static Cell[] epsilonPath(int length) {
        Cell eps1 = Cell.makeEpsilon();
        Cell eps2 = Cell.makeEpsilon();

        Port current = eps1.principal;
        for (int i = 0; i < length; i++) {
            Wire wire = new Wire();
            current.link = wire;
            wire.port1 = current;

            Port port = new Port();
            port.temp = wire;
            wire.port2 = port;

            current = port;
        }
        Wire.link(current, eps2.principal);

        Cell[] endpoints = {eps1, eps2};
        return endpoints;
    }

    public static String churchNumString(int n, String f, String a) {
        if (n == 0)
            return "L " + f + " . L " + a + " . " + a;

        StringBuilder builder = new StringBuilder("L ").append(f).append(" . L ").append(a).append(" . ");
        for (int i = 1; i < n; i++) {
            builder.append(f).append(" (");
        }
        builder.append(f).append(" ").append(a);
        for (int i = 1; i < n; i++) {
            builder.append(")");
        }
        return builder.toString();
    }
}
