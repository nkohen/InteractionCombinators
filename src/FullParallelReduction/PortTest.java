package FullParallelReduction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class PortTest {
    @Test
    public void getLinkedPort() {
        for (int i = 0; i < TestUtil.TEST_SIZE; i++) {
            Cell[] endpoints = TestUtil.epsilonPath(i);
            Cell eps1 = endpoints[0];
            Cell eps2 = endpoints[1];
            Port current = eps1.principal.getLinkedPort();
            for (int j = 0; j < i; j++) {
                assertNull(current.cell);
                current = current.getLinkedPort();
            }
            assertEquals(eps2, current.cell);
        }
    }

    @Test
    public void wipeTest() {
        Wire wire1 = new Wire();
        Wire wire2 = new Wire();
        Port port = new Port();
        port.link = wire1;
        port.temp = wire2;
        port.cell = Cell.makeGamma();
        port.port = Port.PRINCIPAL;
        port.name = TestUtil.TEST_NAME;

        port.wipe();
        assertEquals(wire1, port.link);
        assertEquals(wire2, port.temp);
        assertNull(port.cell);
        assertEquals(Port.ERROR, port.port);
        assertEquals(TestUtil.TEST_NAME, port.name);
    }

    @Test
    public void getOtherTest() {
        for (int i = 0; i < TestUtil.TEST_SIZE; i++) {
            Cell[] endpoints = TestUtil.epsilonPath(i);
            Cell eps1 = endpoints[0];
            Cell eps2 = endpoints[1];
            Port current = eps1.principal.getLinkedPort();
            Wire prev = eps1.principal.link;
            Wire next = current.link;
            for (int j = 0; j < i; j++) {
                assertEquals(prev, current.getOther(next));
                assertEquals(next, current.getOther(prev));
                current = current.getLinkedPort();
                prev = next;
                next = current.link;
            }
            assertEquals(eps2, current.cell);
        }
    }
}
