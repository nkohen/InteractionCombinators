package FullParallelReduction;

import java.util.*;

public class Wire {
    Port port1;
    Port port2;

    Port getOther(Port port) {
        return (port == port1)? port2 : port1;
    }

    Port getOtherExtended(Port port, Set<Wire> marked, Set<Wire> possibleExtra) {
        Port other = getOther(port);

        if (possibleExtra.contains(this))
            marked.add(this);

        if (other.isExtra()) {
            return other.getOther(this).getOtherExtended(other, marked, possibleExtra);
        } else {
            return other;
        }
    }

    static Set<Wire> fixExtra(Set<Wire> possibleExtra) {
        Set<Wire> marked = new HashSet<>();
        Set<Wire> possibleCuts = new HashSet<>();
        for (Wire wire : possibleExtra) {
            if (marked.contains(wire))
                continue;

            Port newPort1 = wire.getOtherExtended(wire.port2, marked, possibleExtra);
            Port newPort2 = wire.getOtherExtended(wire.port1, marked, possibleExtra);
            link(newPort1, newPort2, wire);
            possibleCuts.add(wire);
        }
        return possibleCuts;
    }

    boolean isCut() {
        return port1.port == Port.PRINCIPAL && port2.port == Port.PRINCIPAL;
    }

    Set<Wire> reduce() {
        Cell cell1 = port1.cell;
        Cell cell2 = port2.cell;
        if (cell1.symbol == cell2.symbol) {
            if (cell1.symbol == Cell.Symbol.EPS)
                return Set.of();
            else {
                wipeAndLinkTemp(cell1.left, cell2.left);
                wipeAndLinkTemp(cell1.right, cell2.right);
                return new HashSet<>((List.of(cell1.left.link, cell1.right.link)));
            }
        }

        if (cell1.symbol == Cell.Symbol.EPS) {
            return epsCase(cell2);
        } else if (cell2.symbol == Cell.Symbol.EPS) {
            return epsCase(cell1);
        } else if (cell1.symbol == Cell.Symbol.GAM){
            return gamDelCase(cell1, cell2);
        } else {
            return gamDelCase(cell2, cell1);
        }
    }

    private Set<Wire> epsCase(Cell other) {
        Cell epsL = Cell.makeEpsilon();
        Cell epsR = Cell.makeEpsilon();

        stealPrincipalPort(epsL, other.left);
        stealPrincipalPort(epsR, other.right);

        return Set.of(other.left.link, other.right.link);
    }

    private Set<Wire> gamDelCase(Cell gam, Cell del) {
        Cell delL = Cell.makeDelta();
        Cell delR = Cell.makeDelta();
        Cell gamL = Cell.makeGamma();
        Cell gamR = Cell.makeGamma();

        stealPrincipalPort(delL, gam.right);
        stealPrincipalPort(delR, gam.left);
        stealPrincipalPort(gamL, del.left);
        stealPrincipalPort(gamR, del.right);

        link(delL.left, gamL.right);
        link(delL.right, gamR.right);
        link(delR.left, gamL.left);
        link(delR.right, gamR.left);

        gamL.left.name = gam.left.name;
        gamR.left.name = gam.left.name;

        return new HashSet<>(List.of(del.left.link, del.right.link, gam.left.link, gam.right.link));
    }

    private static void stealPrincipalPort(Cell theif, Port port) {
        theif.principal = port;
        port.cell = theif;
        port.port = Port.PRINCIPAL;
    }

    private static Wire wipeAndLinkTemp(Port port1, Port port2) {
        port1.wipe();
        port2.wipe();

        Wire wire = new Wire();
        wire.port1 = port1;
        port1.temp = wire;
        wire.port2 = port2;
        port2.temp = wire;
        return wire;
    }

    static Wire link(Port port1, Port port2) {
        Wire wire = new Wire();
        wire.port1 = port1;
        port1.link = wire;
        wire.port2 = port2;
        port2.link = wire;
        return wire;
    }

    static void link(Port port1, Port port2, Wire wire) {
        wire.port1 = port1;
        port1.link = wire;
        wire.port2 = port2;
        port2.link = wire;
    }
}

