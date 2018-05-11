package FourCombinatorImplementation;

import java.util.*;

public class Wire {
    Port port1;
    Port port2;

    // Returns the Port that is not the given Port
    Port getOther(Port port) {
        return (port == port1)? port2 : port1;
    }

    // Returns the first Port that is part of a cell in the direction away from the given port
    // Also marks Wires as duplicate if they are reached
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

    // Removes extra Ports and and duplicate Wires
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

    // This method assumes isCut() == true
    // Returns Wires that are possible new cuts
    Set<Wire> reduce() {
        Cell cell1 = port1.cell;
        Cell cell2 = port2.cell;

        if (cell1.symbol == cell2.symbol) {
            if (cell1.symbol == Cell.Symbol.EPS)
                return Set.of();
            else {
                if (cell1.symbol == Cell.Symbol.DEL_PRIME) {
                    InteractionNet.deltaPrimes.remove(cell1);
                    InteractionNet.deltaPrimes.remove(cell2);
                }
                wipeAndLinkTemp(cell1.left, cell2.left);
                wipeAndLinkTemp(cell1.right, cell2.right);
                return new HashSet<>((List.of(cell1.left.link, cell1.right.link)));
            }
        }

        if (cell1.symbol == Cell.Symbol.EPS) {
            return epsCase(cell2);
        } else if (cell2.symbol == Cell.Symbol.EPS) {
            return epsCase(cell1);
        } else if (cell1.symbol == Cell.Symbol.DEL && cell2.symbol == Cell.Symbol.GAM) {
            return delGamCase(cell1, cell2);
        } else if (cell1.symbol == Cell.Symbol.GAM && cell2.symbol == Cell.Symbol.DEL) {
            return delGamCase(cell2, cell1);
        } else {
            return delPrimeCase(cell1, cell2);
        }
    }

    private Set<Wire> epsCase(Cell other) {
        if (other.symbol == Cell.Symbol.DEL_PRIME) {
            InteractionNet.deltaPrimes.remove(other);
        }

        Cell epsL = Cell.makeEpsilon();
        Cell epsR = Cell.makeEpsilon();

        stealPrincipalPort(epsL, other.left);
        stealPrincipalPort(epsR, other.right);

        return new HashSet<>(List.of(other.left.link, other.right.link));
    }

    private Set<Wire> delGamCase(Cell del, Cell gam) {
        Cell delL = Cell.makeDeltaPrime();
        Cell delR = Cell.makeDeltaPrime();
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

        InteractionNet.deltaPrimes.add(delL);
        InteractionNet.deltaPrimes.add(delR);

        return new HashSet<>(List.of(del.left.link, del.right.link, gam.left.link, gam.right.link));
    }

    private Set<Wire> delPrimeCase(Cell top, Cell bottom) {
        if (top.symbol == Cell.Symbol.DEL_PRIME) {
            InteractionNet.deltaPrimes.remove(top);
        }
        if (bottom.symbol == Cell.Symbol.DEL_PRIME) {
            InteractionNet.deltaPrimes.remove(bottom);
        }

        Cell topL = Cell.makeCell(bottom.symbol);
        Cell topR = Cell.makeCell(bottom.symbol);
        Cell bottomL = Cell.makeCell(top.symbol);
        Cell bottomR = Cell.makeCell(top.symbol);

        stealPrincipalPort(topL, top.right);
        stealPrincipalPort(topR, top.left);
        stealPrincipalPort(bottomL, bottom.left);
        stealPrincipalPort(bottomR, bottom.right);

        link(topL.left, bottomL.right);
        link(topL.right, bottomR.right);
        link(topR.left, bottomL.left);
        link(topR.right, bottomR.left);

        topL.left.name = bottom.left.name;
        topR.left.name = bottom.left.name;
        bottomL.left.name = top.left.name;
        bottomR.left.name = top.left.name;

        if (top.symbol == Cell.Symbol.DEL_PRIME) {
            InteractionNet.deltaPrimes.add(bottomL);
            InteractionNet.deltaPrimes.add(bottomR);
        }
        if (bottom.symbol == Cell.Symbol.DEL_PRIME) {
            InteractionNet.deltaPrimes.add(topL);
            InteractionNet.deltaPrimes.add(topR);
        }

        return new HashSet<>(List.of(top.left.link, top.right.link, bottom.left.link, bottom.right.link));
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
