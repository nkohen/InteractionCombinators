package FullParallelReduction;

public class Cell {
    enum Symbol {GAM, EPS, DEL}

    final Symbol symbol;

    Port principal;
    Port left;
    Port right;

    Term term = null;

    private Cell(Symbol symbol, boolean eps) {
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
}
