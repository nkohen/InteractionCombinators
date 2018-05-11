package FullParallelReduction;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

public class InteractionNet {
    Port handle;
    static Queue<Wire> cuts = new LinkedList<>();

    void linkToNet(Port port, InteractionNet net) {
        Wire.link(port, net.handle.getLinkedPort());
    }

    void reduce() { // This gets overriden
        while (!cuts.isEmpty()) {
            Set<Wire> possibleCuts = cuts.poll().reduce();

            possibleCuts = Wire.fixExtra(possibleCuts);

            for (Wire wire: possibleCuts) {
                if (wire.isCut()) {
                    cuts.add(wire);
                }
            }
        }
    }
}
