package FullParallelReduction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class TermTest {

    @Test
    public void varTest() {
        Term var = Term.var(TestUtil.TEST_NAME);
        assertEquals(Term.VAR, var.type);
        assertNull(var.left);
        assertNull(var.right);
        assertEquals(TestUtil.TEST_NAME, var.name);
    }

    @Test
    public void prettyPrintTest() {
        for (int i = 0; i < TestUtil.TEST_SIZE; i++) {
            String termString = TestUtil.churchNumString(i, "f", "a");
            assertEquals(termString, new Term(termString).prettyPrint());
        }
    }

    @Test
    public void toStringTest() {
        assertEquals("LAM(VAR(f), LAM(VAR(a), APP(VAR(f), VAR(a))))",
                new Term("L f . L a . f a").toString());

        assertEquals("APP(LAM(VAR(x), VAR(x)), VAR(x))",
                new Term("(L x . x) x").toString());

        assertEquals("APP(APP(VAR(a), VAR(b)), VAR(c))",
                new Term("a b c").toString());
    }

    @Test
    public void alphaEqualsTest() {
        for (int i = 0; i < TestUtil.TEST_SIZE; i++) {
            assertTrue(new Term(TestUtil.churchNumString(i, "f", "a"))
                    .alphaEquals(new Term(TestUtil.churchNumString(i, "aaa", "fff"))));
        }
    }
}
