package FullParallelReduction;

public class Port {
    final static int ERROR = -1;
    final static int PRINCIPAL = 0;
    final static int LEFT = 1;
    final static int RIGHT = 2;

    Wire link;
    Cell cell;
    int port = ERROR;
    Wire temp = null;
    String name;

    boolean isExtra() { return temp != null; }

    Port getLinkedPort() {
        return link.getOther(this);
    }

    void wipe() {
        cell = null;
        port = ERROR;
    }

    Wire getOther(Wire wire) {
        return (wire == link)? temp : link;
    }
}
