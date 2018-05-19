package FourCombinatorImplementation;

import java.util.*;

public class InteractionNet {
    Port handle;
    static Queue<Wire> cuts = new LinkedList<>();
    static Set<Cell> deltaPrimes = new HashSet<>();

    private static int num = 0;

    public InteractionNet() {}

    private InteractionNet(Port handle) {
        this.handle = handle;
    }

    void linkToNet(Port port, InteractionNet net) {
        Wire.link(port, net.handle.getLinkedPort());
    }

    void normalize() {
        while (!cuts.isEmpty()) {
            Set<Wire> possibleCuts = cuts.poll().reduce();

            // Bypass extra Ports and remove duplicate Wires
            possibleCuts = Wire.fixExtra(possibleCuts);

            for (Wire wire : possibleCuts) {
                if (wire.isCut()) {
                    cuts.add(wire);
                }
            }

            // This is a short-term hack to be replaced
            num++;
            if (num >= 100000) { // Every 10000 reduction steps, do garbage collection to remove deleted parts
                num = 0;
                purge();
            }
        }
    }

    // TODO: On deletion, traverse the tree to be deleted and remove from cuts and deltaPrimes
    /*
     * To reduce:
     * Reduce each cut and add to the Queue newly created cuts until the queue is empty (normalize)
     * Then if there are Cells with symbol DEL_PRIME that point to right ports of Gammas, force duplication
     * Repeat until there are no cuts and no DEL_PRIMEs (except for those pointing at free ports)
     *
     * Actually some more complicated stuff happening now, will update my write-up soon
     */
    void reduce() {
        normalize();
        if (deltaPrimes.isEmpty())
            return;

        Cell top = handle.getLinkedPort().cell;
        if (top == null)
            return;

        if (top.symbol == Cell.Symbol.GAM && handle.getLinkedPort().port == Port.PRINCIPAL) {
            new InteractionNet(top.right).reduce();
        } else if (top.isApplication()) { // should only come from right??? I think
            // This is where multi-threading can happen for fixing?
            new InteractionNet(top.principal).reduce();
            if (handle.port == Port.PRINCIPAL && handle.cell.symbol == Cell.Symbol.DEL_PRIME &&
                    handle.getLinkedPort().port == Port.LEFT)
                new InteractionNet(top.left).reduce();
        } else if (top.symbol == Cell.Symbol.DEL) { // This is doing too much work? optimize here
            new InteractionNet(top.principal).reduce();
        } else if (top.symbol == Cell.Symbol.DEL_PRIME) { // This is doing too much work? optimize here
            new InteractionNet(top.principal).reduce();
            if (top.principal.cell != top) { // This means the cell has been changed to a DEL
                return;
            }
            Cell next = top.principal.getLinkedPort().cell;
            if (next == null || next.isLambda()) {
                removePrime(top); // This could change cells above it
            } else if (next.isApplication() && top.principal.getLinkedPort().port == Port.RIGHT) {
                forceDup(top);
                new InteractionNet(handle.getLinkedPort().cell.principal).reduce();
                new InteractionNet(handle.getLinkedPort().cell.left).reduce();
            } else if (next.symbol == Cell.Symbol.DEL) {
                changeSymbol(next, Cell.Symbol.DEL_PRIME);
                reduce();
            }
        }
    }

    private void forceDup(Cell del) {
        Cell gam = del.principal.getLinkedPort().cell;

        Cell gamL = Cell.makeGamma();
        Cell gamR = Cell.makeGamma();
        Cell delL = Cell.makeDeltaPrime();
        Cell delR = Cell.makeDeltaPrime();

        Wire.link(gamL.right, del.right.getLinkedPort());
        Wire.link(gamL.left, delR.right);
        Wire.link(gamL.principal, delL.right);
        Wire.link(gamR.right, del.left.getLinkedPort());
        Wire.link(gamR.left, delR.left);
        Wire.link(gamR.principal, delL.left);
        Wire.link(delL.principal, gam.principal.getLinkedPort());
        Wire.link(delR.principal, gam.left.getLinkedPort());

        deltaPrimes.remove(del);
        deltaPrimes.add(delL);
        deltaPrimes.add(delR);

        if (delL.principal.link.isCut())
            cuts.add(delL.principal.link);
        if (delR.principal.link.isCut())
            cuts.add(delR.principal.link);
    }

    private void changeSymbol(Cell cell, Cell.Symbol symbol) {
        Cell newCell = Cell.makeCell(symbol);

        newCell.principal = cell.principal;
        cell.principal.cell = newCell;

        newCell.left = cell.left;
        cell.left.cell = newCell;

        newCell.right = cell.right;
        cell.right.cell = newCell;

        if (cell.symbol == Cell.Symbol.DEL_PRIME)
            deltaPrimes.remove(cell);
        if (symbol == Cell.Symbol.DEL_PRIME)
            deltaPrimes.add(newCell);
    }

    private void removePrime(Cell delPrime) {
        changeSymbol(delPrime, Cell.Symbol.DEL);

        if (delPrime.left.getLinkedPort().cell != null &&
                delPrime.left.getLinkedPort().cell.symbol == Cell.Symbol.DEL_PRIME)
            removePrime(delPrime.left.getLinkedPort().cell);
        if (delPrime.right.getLinkedPort().cell != null &&
                delPrime.right.getLinkedPort().cell.symbol == Cell.Symbol.DEL_PRIME)
            removePrime(delPrime.right.getLinkedPort().cell);
    }

    // Do garbage collection: remove pointers to all that cannot be reached from handle
    private void purge() {
        if (handle.getLinkedPort().cell == null) { // If handle points to a free port, clear everything
            cuts.clear();
            deltaPrimes.clear();
            return;
        }

        // Traverse the net and mark each visited Cell
        Set<Cell> visited = new HashSet<>();
        visit(handle.getLinkedPort().cell, visited);

        // Remove from cuts and deltaPrimes the Wires and Cells that were not marked
        cuts.removeIf(wire -> !(visited.contains(wire.port1.cell) || visited.contains(wire.port2.cell)));
        deltaPrimes.removeIf(cell -> !visited.contains(cell));
    }

    // Traverse the net from cell and add to visited
    private void visit(Cell cell, Set<Cell> visited) {
        if (cell == null || visited.contains(cell))
            return;

        visited.add(cell);

        visit(cell.principal.getLinkedPort().cell, visited);
        if (cell.symbol != Cell.Symbol.DEL) { // Don't visit left and right of a Delta
            if (cell.left != null)
                visit(cell.left.getLinkedPort().cell, visited);
            if (cell.right != null)
                visit(cell.right.getLinkedPort().cell, visited);
        }
    }
}
