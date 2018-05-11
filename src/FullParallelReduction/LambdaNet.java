package FullParallelReduction;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LambdaNet extends InteractionNet {
    static Map<String, Port> free = new HashMap<>();

    public LambdaNet(Term term) {
        this(term, new HashMap<>());
    }

    public LambdaNet(Term term, Map<String, Port> bound) {
        handle = new Port();

        switch (term.type) {
            case Term.VAR:
                if (bound.containsKey(term.name)) {
                    Cell delta = Cell.makeDelta();
                    Wire.link(delta.principal, bound.get(term.name));
                    bound.put(term.name, delta.right);
                    Wire.link(handle, delta.left);
                } else if (free.containsKey(term.name)) {
                    Wire.link(handle, free.get(term.name));
                    free.get(term.name).link = null;
                } else {
                    Port var = new Port();
                    var.name = term.name;
                    Wire.link(handle, var);
                    var.link = null;
                    free.put(var.name, var);
                }
                break;
            case Term.LAM:
                Cell lam = Cell.makeGamma();
                lam.left.name = term.left.name;
                Wire.link(handle, lam.principal);
                Port shadowed = bound.getOrDefault(term.left.name, null);
                bound.put(term.left.name, lam.left);
                linkToNet(lam.right, new LambdaNet(term.right, bound));
                if (shadowed != null) {
                    bound.put(term.left.name, shadowed);
                }
                fixEnd(lam.left);
                break;
            case Term.APP:
                Cell app = Cell.makeGamma();
                Wire.link(handle, app.right);
                linkToNet(app.principal, new LambdaNet(term.left, bound));
                linkToNet(app.left, new LambdaNet(term.right, bound));
                if (app.principal.link.isCut())
                    cuts.add(app.principal.link);
                break;
            default:
                throw new RuntimeException("Illegal term type");
        }
    }

    private void fixEnd(Port port) {
        if (port.link == null) {
            Cell eps = Cell.makeEpsilon();
            Wire.link(port, eps.principal);
        } else if (port.getLinkedPort().cell.right.link == null) {
            Wire.link(port, port.getLinkedPort().cell.left.getLinkedPort());
        } else {
            fixEnd(port.getLinkedPort().cell.right);
        }
    }

    public Term toTerm() {
        return toTerm(handle, new HashSet<>(free.keySet()));
    }

    private Term toTerm(Port root, Set<String> names) {
        for (Map.Entry<String, Port> entry : free.entrySet()) {
            if (entry.getValue() == root.getLinkedPort())
                return Term.var(entry.getKey());
        }


        if (root.getLinkedPort().cell.symbol == Cell.Symbol.DEL) {
            Cell del = root.getLinkedPort().cell;
            if (del.term == null) {
                del.term = toTerm(del.principal, names);
                return del.term;
            } else {
                Term result = del.term;
                del.term = null;
                return result;
            }
        }

        if (root.getLinkedPort().port == Port.PRINCIPAL) {
            fixName(root.getLinkedPort().cell.left, names);
            names.add(root.getLinkedPort().cell.left.name);
            Term term =  new Term(Term.LAM, Term.var(root.getLinkedPort().cell.left.name), toTerm(root.getLinkedPort().cell.right, names), null);
            names.remove(root.getLinkedPort().cell.left.name);
            return term;
        } else if (root.getLinkedPort().port == Port.RIGHT) {
            return new Term(Term.APP, toTerm(root.getLinkedPort().cell.principal, names), toTerm(root.getLinkedPort().cell.left, names), null);
        } else {
            return Term.var(root.getLinkedPort().name);
        }
    }

    private static void fixName(Port port, Set<String> names) {
        if (names.contains(port.name)) {
            port.name = port.name + "'";
            fixName(port, names);
        }
    }

    public static void test(Term term) {
        System.out.println(term.prettyPrint());
        LambdaNet net = new LambdaNet(term);
        long start = System.currentTimeMillis();
        net.reduce();
        long stop = System.currentTimeMillis();
        System.out.println(stop-start);
        System.out.println(net.toTerm().prettyPrint());
        System.out.println();
        LambdaNet.free.clear();
    }

    public static void main(String[] args) {
        test(new Term("(L y . y) x"));

        test(new Term("(L x . x x) (L f . L a . a f f)"));

        test(new Term("L x . (L y . y) x"));
        test(new Term("L x . (L y . y y) (x x)"));

        test(new Term("L z . L x . (L y . y) z"));
        test(new Term("(L x . x) (L y . y)"));
        test(new Term("L y . (L a . L b . a b) (L x . x) (L x . y)"));

        test(new Term("(L f . L a . f (f a)) (L g . L b . (g b))"));
        //test(new Term("L a . (L g . L b . g b) ((L g . L b . g b) a)"));
        //test(new Term("L a . L b . (L b . a b) b"));

        test(new Term("(L f . L a . f (f a)) (L g . L b . g (g b))"));
        //test(new Term("L a . (L g . L b . g (g b)) ((L g . L b . g (g b)) a)"));
        //test(new Term("L a . L b . (L b . a (a b)) ((L b . a (a b)) b)"));

        test(new Term("(L x . x x) (L f . L a . f (f a))"));

        test(new Term("(L x . y) ((L x . x x) (L x . x x))"));

        test(new Term("((L a . L b . ((L c . b) (a a b))) (L a . (a a)))"));

        test(new Term("(L x . x) (L x . x) (L x . x)"));

        test(new Term("(L x . y) z"));

        test(new Term("(L f . L a . f (f (f a))) (L g . L b . g (g b))"));
    }

    @Override
    void reduce() {
        while (!cuts.isEmpty()) {
            Set<Wire> possibleCuts = new HashSet<>();
            while (!cuts.isEmpty()) { // This loop can be made concurrent
                possibleCuts.addAll(cuts.poll().reduce());
            }
            Wire.fixExtra(possibleCuts);
            Term term = this.toTerm();
            //System.out.println(term.prettyPrint());
            handle = new LambdaNet(term).handle;
        }
    }
}