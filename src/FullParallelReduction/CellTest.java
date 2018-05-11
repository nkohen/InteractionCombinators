package FullParallelReduction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class CellTest {
    @Test
    public void deltaTest() {
        Cell delta = Cell.makeDelta();
        assertEquals(Cell.Symbol.DEL, delta.symbol);
        assertNotNull(delta.principal);
        assertNotNull(delta.left);
        assertNotNull(delta.right);
    }

    @Test
    public void gammaTest() {
        Cell gamma = Cell.makeGamma();
        assertEquals(Cell.Symbol.GAM, gamma.symbol);
        assertNotNull(gamma.principal);
        assertNotNull(gamma.left);
        assertNotNull(gamma.right);
    }

    @Test
    public void epsilonTest() {
        Cell epsilon = Cell.makeEpsilon();
        assertEquals(Cell.Symbol.EPS, epsilon.symbol);
        assertNotNull(epsilon.principal);
        assertNull(epsilon.left);
        assertNull(epsilon.right);
    }
}
