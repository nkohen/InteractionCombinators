package FourCombinatorImplementation;

import java.util.*;

public class InteractionNet {
    Port handle;
    static Queue<Wire> cuts = new LinkedList<>();
    static Set<Cell> deltaPrimes = new HashSet<>();

    void linkToNet(Port port, InteractionNet net) {
        Wire.link(port, net.handle.getLinkedPort());
    }

    // TODO: On deletion, traverse the tree to be deleted and remove from cuts and deltaPrimes
    /*
     * To reduce:
     * Reduce each cut and add to the Queue newly created cuts until the queue is empty
     * Then if there are Cells with symbol DEL_PRIME that point to aux ports of Gammas, force duplication
     * Repeat until there are no cuts and no DEL_PRIMEs (except for those pointing at free ports)
     */
    void reduce() {
        int num = 0;
        do {
            Set<Cell> needsDelta = new HashSet<>();
            for (Cell deltaPrime : deltaPrimes) {
                if (deltaPrime.principal.getLinkedPort().cell.symbol == Cell.Symbol.GAM)
                    needsDelta.add(deltaPrime.principal.getLinkedPort().cell);
            }
            for (Cell cell : needsDelta) { // Force Duplication of Gammas
                insertDeltas(cell);
            }

            while (!cuts.isEmpty()) { // Normalize
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
                if (num == 100000) { // Every 10000 reduction steps, do garbage collection to remove deleted parts
                    num = 0;
                    purge();
                }
            }

            // Remove from deltaPrimes Cells pointing to free ports
            deltaPrimes.removeIf(deltaPrime -> deltaPrime.principal.getLinkedPort().cell == null);
        } while (!deltaPrimes.isEmpty());
    }

    // Inserts a DeltaPrime pointing up at cell and a Delta pointing down at what cell was pointing at
    private void insertDeltas(Cell cell) {
        Port out = cell.principal.getLinkedPort();

        Cell delta = Cell.makeDelta();
        Cell deltaPrime = Cell.makeDeltaPrime();
        Wire.link(delta.left, deltaPrime.right);
        Wire.link(delta.right, deltaPrime.left);

        Wire.link(out, delta.principal);
        Wire cut = Wire.link(cell.principal, deltaPrime.principal);

        cuts.add(cut);
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
