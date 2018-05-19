package FourCombinatorImplementation;

public class Cell {
    enum Symbol {GAM, EPS, DEL, DEL_PRIME}

    final Symbol symbol;

    Port principal;
    Port left;
    Port right;

    // For use by DEL and DEL_PRIME to avoid duplicating work
    Term term = null;

    private Cell(Symbol symbol, boolean eps) { // Constructor is private, used by factory methods
        this.symbol = symbol;

        principal = new Port();
        principal.port = Port.PRINCIPAL;
        principal.cell = this;

        if (!eps) {
            left = new Port();
            left.port = Port.LEFT;
            left.cell = this;

            right = new Port();
            right.port = Port.RIGHT;
            right.cell = this;
        }
    }

    public static Cell makeDelta() {
        return new Cell(Symbol.DEL, false);
    }

    public static Cell makeGamma() {
        return new Cell(Symbol.GAM, false);
    }

    public static Cell makeEpsilon() {
        return new Cell(Symbol.EPS, true);
    }

    public static Cell makeDeltaPrime() {
        return new Cell(Symbol.DEL_PRIME, false);
    }

    public static Cell makeCell(Symbol symbol) {
        return new Cell(symbol, (symbol == Symbol.EPS));
    }

    public boolean isLambda() {
        return symbol == Symbol.GAM && left.name != null;
    }

    public boolean isApplication() {
        return symbol == Symbol.GAM && left.name == null;
    }
}
