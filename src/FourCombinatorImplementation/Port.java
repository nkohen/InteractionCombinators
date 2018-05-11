package FourCombinatorImplementation;

public class Port {
    final static int ERROR = -1;
    final static int PRINCIPAL = 0;
    final static int LEFT = 1;
    final static int RIGHT = 2;

    Wire link; // The Wire this Port is connected to
    Cell cell; // The Cell this Port is part of
    int port = ERROR; // A value signifying where this port is in this.cell
    Wire temp = null; // For use when this Port is connecting two Wires instead of being part of a Cell

    String name; // For use when this Port represents a variable

    boolean isExtra() { return temp != null; }

    Port getLinkedPort() {
        return link.getOther(this);
    }

    void wipe() {
        cell = null;
        port = ERROR;
    }

    // Returns the Wire that is not the given wire
    Wire getOther(Wire wire) {
        return (wire == link)? temp : link;
    }
}
