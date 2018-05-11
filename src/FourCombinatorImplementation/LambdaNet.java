package FourCombinatorImplementation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LambdaNet extends InteractionNet {
    // A map from free variables to the ports representing them in the net
    static Map<String, Port> free = new HashMap<>();

    // Encodes Terms into InteractionNets
    public LambdaNet(Term term) {
        this(term, new HashMap<>());
    }

    public LambdaNet(Term term, Map<String, Port> bound) {
        handle = new Port();

        // The queue of InteractionNet.cuts is populated in the Term.APP case
        switch (term.type) {
            case Term.VAR: // Variables are encoded as a wire to the place it is bound or to a free port
                if (bound.containsKey(term.name)) {
                    // If the variable is bound, link to where it is bound after adding a delta
                    // This results in an extra delta that is dealt with in the Term.LAM case
                    Cell delta = Cell.makeDelta();
                    Wire.link(delta.principal, bound.get(term.name));
                    bound.put(term.name, delta.right);
                    Wire.link(handle, delta.left);
                } else if (free.containsKey(term.name)) {
                    Wire.link(handle, free.get(term.name));
                    free.get(term.name).link = null;
                } else {
                    // If this variable is not bound and not in the free map, create a new port for this free variable
                    Port var = new Port();
                    var.name = term.name;
                    Wire.link(handle, var);
                    var.link = null;
                    free.put(var.name, var);
                }
                break;
            case Term.LAM: // Lambda abstractions are encoded as Gammas where the left port is variable
                Cell lam = Cell.makeGamma();
                lam.left.name = term.left.name;
                Wire.link(handle, lam.principal);

                // We retain the Port to be shadowed so that it can be put back in scope on returning
                Port shadowed = bound.getOrDefault(term.left.name, null);

                bound.put(term.left.name, lam.left);
                linkToNet(lam.right, new LambdaNet(term.right, bound));

                if (shadowed != null) {
                    bound.put(term.left.name, shadowed);
                }

                // This method puts an Epsilon on the left if the variable is never used
                // Otherwise, it removes the extra Delta created in the Term.VAR case
                fixEnd(lam.left);
                break;
            case Term.APP: // Applications are encoded with Gammas with function at principal and argument at left
                Cell app = Cell.makeGamma();
                Wire.link(handle, app.right);
                linkToNet(app.principal, new LambdaNet(term.left, bound));
                linkToNet(app.left, new LambdaNet(term.right, bound));

                // This is where cuts gets populated
                if (app.principal.link.isCut())
                    cuts.add(app.principal.link);
                break;
            default:
                throw new RuntimeException("Illegal term type");
        }
    }

    // To be called with a Port that binds a variable
    private void fixEnd(Port port) {
        if (port.link == null) {
            // If unused, place Epsilon
            Cell eps = Cell.makeEpsilon();
            Wire.link(port, eps.principal);
        } else if (port.getLinkedPort().cell.right.link == null) {
            // Else if port.getLinkedPort().cell is the extra Delta, bypass it
            Wire.link(port, port.getLinkedPort().cell.left.getLinkedPort());
        } else {
            // Otherwise, continue to the right until the extra Delta is reached
            fixEnd(port.getLinkedPort().cell.right);
        }
    }

    // Decodes InteractionNets into Terms
    public Term toTerm() {
        return toTerm(handle, new HashSet<>(free.keySet()));
    }

    // The Set names is used for renaming to avoid capturing free variables
    private Term toTerm(Port root, Set<String> names) {
        // If the root is linked to a free Port, return that variable
        for (Map.Entry<String, Port> entry : free.entrySet()) {
            if (entry.getValue() == root.getLinkedPort())
                return Term.var(entry.getKey());
        }

        // This should never happen and is only for debugging
        if (root.getLinkedPort().cell == null) {
            System.out.println(root.cell.symbol);
            System.out.println(root.port);
        }

        // If root is connected to a Delta, bypass that Delta (and store the work done for the other port of the Delta)
        if (root.getLinkedPort().cell.symbol == Cell.Symbol.DEL ||
                root.getLinkedPort().cell.symbol == Cell.Symbol.DEL_PRIME) {
            Cell del = root.getLinkedPort().cell;
            if (del.term == null) { // If the work has not already been done, do it
                del.term = toTerm(del.principal, names);
                return del.term;
            } else { // If the other side has already done the work, use it and remove term to free memory
                Term result = del.term;
                del.term = null;
                return result;
            }
        }

        // If we have not yet returned, then root is connected to a Gamma

        if (root.getLinkedPort().port == Port.PRINCIPAL) { // If root is connected at principal, this is a Lambda
            // Rename to avoid capture if necessary
            fixName(root.getLinkedPort().cell.left, names);
            // Add the variable bound to scope (in names)
            names.add(root.getLinkedPort().cell.left.name);

            Term term = new Term(
                    Term.LAM,
                    Term.var(root.getLinkedPort().cell.left.name),
                    toTerm(root.getLinkedPort().cell.right, names),
                    null);

            // Remove the variable bound from scope (in names)
            names.remove(root.getLinkedPort().cell.left.name);

            return term;
        } else if (root.getLinkedPort().port == Port.RIGHT) { // If root is connected at right, this is an Application
            return new Term(
                    Term.APP,
                    toTerm(root.getLinkedPort().cell.principal, names),
                    toTerm(root.getLinkedPort().cell.left, names),
                    null);
        } else { // Else, root is connected at left so that this is a variable
            return Term.var(root.getLinkedPort().name);
        }
    }

    // Add's ' to the end of the name of the port until the name is not contained in the Set of names
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
        LambdaNet.deltaPrimes.clear();
    }

    public static void main(String[] args) {
        test(new Term("(L y . y) x"));

        test(new Term("(L x . x x) (L f . L a . a f f)"));

        test(new Term("L x . (L y . y) x"));
        test(new Term("L x . (L y . y y) (x x)"));

        test(new Term("L z . L x . (L y . y) z"));
        test(new Term("(L x . x) (L y . y)"));
        test(new Term("L y . (L a . L b . a b) (L x . x) (L x . y)"));

        test(new Term("(L a . L b . a b) (L x . x)"));

        test(new Term("(L f . L a . f (f a)) (L g . L b . (g b))"));
        //test(new Term("L a . (L g . L b . g b) ((L g . L b . g b) a)"));
        //test(new Term("L a . L b . (L b . a b) b"));

        test(new Term("(L f . L a . f (f a)) (L g . L b . g (g b))"));
        //test(new Term("L a . (L g . L b . g (g b)) ((L g . L b . g (g b)) a)"));
        //test(new Term("L a . L b . (L b . a (a b)) ((L b . a (a b)) b)"));

        test(new Term("(L x . x x) (L f . L a . f (f a))"));

        test(new Term("(L x . y) z"));
        test(new Term("(L x . y) ((L x . x x) (L x . x))"));
        test(new Term("(L x . y) ((L x . x x) (L x . x x))"));

        test(new Term("((L a . L b . ((L c . b) (a a b))) (L a . (a a)))"));

        test(new Term("(L x . x x) (L x . y)"));

        test(new Term("(L f . L a . f (f (f (f (f a))))) (L g . L b . g (g (g (g (g b))))))"));
    }
}