package FullParallelReduction;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

// These were just copied from the four-combinator implementation's test
public class FunctionalTest {
    private static void assertReduces(String expected, String term) {
        assertEquals(new Term(expected), reduceTerm(term));
    }

    private static Term reduceTerm(String term) {
        LambdaNet.free.clear();
        LambdaNet net = new LambdaNet(new Term(term));
        net.reduce();
        return net.toTerm();
    }

    private static String churchNum(int num) {
        StringBuilder builder = new StringBuilder("L f . L a . ");
        if (num == 0)
            return builder.append("a").toString();

        builder.append("f ");

        for (int i = 1; i < num; i++) {
            builder.append("(f ");
        }

        builder.append("a");

        for (int i = 1; i < num; i++) {
            builder.append(")");
        }

        return builder.toString();
    }

    private static String exp(int base, int exponent) {
        return "(" + churchNum(exponent) + ") (" + churchNum(base) + ")";
    }

    @Test
    public void nothingTest() {
        assertReduces("L x . x", "L x . x");
        assertReduces("x", "x");
    }

    @Test
    public void sanityBetaTest() {
        assertReduces("L x . x", "(L x . x) (L x . x)");
        assertReduces("y", "(L x . x) y");
        assertReduces("L x . x", "L x . (L y . y) x");
        assertReduces("L z . L x . z", "L z . L x . (L y . y) z");
        assertReduces("L y . L x . y", "L y . (L a . L b . a b) (L x . x) (L x . y)");
        assertReduces("L x . x", "(L a . L b . a b) (L x . x)");
        assertReduces("L a . L b . a b", "(L f . L a . f (f a)) (L g . L b . g b)");
    }

    @Test
    public void duplicatingDuplicationTest() {
        assertReduces("L a . a (L f . L a' . a' f f) (L f . L a' . a' f f)",
                "(L x . x x) (L f . L a . a f f");
        assertReduces("L a . L a' . a (a (a (a a')))", "(L x . x x) (L f . L a . f (f a))");
        assertReduces("L x . (x x) (x x)", "L x . (L y . y y) (x x)");
        assertReduces("L a . L b . a (a (a (a b)))", "(L f . L a . f (f a)) (L g . L b . g (g b))");
    }

    @Test
    public void deletionTest() {
        assertReduces("y", "(L x . y) z");
        assertReduces("y", "(L x . y) ((L x . x x) (L x . x))");
        assertReduces("y", "(L x . y) ((L x . x x) (L x . x x))");
        assertReduces("L b . b", "((L a . L b . ((L c . b) (a a b))) (L a . (a a)))");
        assertReduces("y", "(L x . x x) (L x . y)");
    }

    @Test
    public void doubleDelPrime() {
        assertReduces("L a . L b . a (a (a (a (a (a (a (a b)))))))",
                "(L f . L a . f (f (f a))) (L g . L b . g (g b))");
    }

    @Test
    public void expTest() {
        for (int i = 1; i <= 4; i++) {
            for (int j = 1; j <= 5; j++) {
                assertReduces(churchNum((int)Math.pow(i, j)), exp(i, j));
            }
        }
    }
}
