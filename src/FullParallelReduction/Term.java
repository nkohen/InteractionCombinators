package FullParallelReduction;

import Regex.Lexer;

import java.util.HashMap;
import java.util.Map;

public class Term {
    public final static int VAR = 0;
    public final static int LAM = 1;
    public final static int APP = 2;

    public final static String LAMBDA_SYMBOL = "L";

    private final static String[] patterns = {LAMBDA_SYMBOL, "(" + Lexer.LETTER + "|')*", "\\.", "\\(", "\\)", " *"};
    private final static String[] names = {"Lambda", "Name", "Dot", "Open", "Close", "Whitespace"};
    private final static Map<String, Integer> priority = Map.of("Lambda", 1);
    private final static String[] omit = {"Whitespace"};
    public final static Lexer lexer = new Lexer(names, priority, patterns, omit);

    int type;
    Term left;
    Term right;
    String name;

    public Term(String term) {
        lexer.init(term);

        Term thisTerm = matchTerm();
        this.type = thisTerm.type;
        this.left = thisTerm.left;
        this.right = thisTerm.right;
        this.name = thisTerm.name;
    }

    Term(int type, Term left, Term right, String name) {
        this.type = type;
        this.left = left;
        this.right = right;
        this.name = name;
    }

    public static Term var(String name) {
        return new Term(VAR, null, null, name);
    }

    private Term matchTerm() {
        Lexer.Token token = lexer.nextToken();
        switch (token.type) {
            case "Lambda":
                Lexer.Token nextToken = lexer.nextToken();
                if (!"Name".equals(nextToken.type)) {
                    throw new RuntimeException("Illegal name at: " + token.value + " " + nextToken.value);
                }
                String name = nextToken.value;
                nextToken = lexer.nextToken();
                if(!"Dot".equals(nextToken.type)) {
                    throw new RuntimeException("Expected punctuation at: " + token.value + " " + name);
                }
                return new Term(LAM, new Term(VAR, null, null, name), matchTerm(), null);
            case "Name":
                Term var = new Term(VAR, null, null, token.value);
                return matchAppTerm(var);
            case "Dot":
                throw new RuntimeException("Unexpected Punctuation");
            case "Open":
                return matchAppTerm(matchTerm());
            case "Close":
                throw new RuntimeException("Unexpected ')'");
            default:
                throw new RuntimeException("Illegal token at: " + token.value);
        }
    }

    private Term matchNext(Lexer.Token currentToken) {
        Lexer.Token token = currentToken;
        if (currentToken == null) {
            token = lexer.nextToken();
        }
        switch (token.type) {
            case "Name":
                return new Term(VAR, null, null, token.value);
            case "Open":
                return matchTerm();
            default:
                throw new RuntimeException("matchNext() should not have been called");
        }
    }

    private Term matchAppTerm(Term current) {
        if (!lexer.hasNext()) {
            return current;
        }

        while (lexer.hasNext()) {
            Lexer.Token nextToken = lexer.nextToken();
            if ("Close".equals(nextToken.type)) {
                return current;
            }
            current = new Term(APP, current, matchNext(nextToken), null);
        }
        return current;
    }

    public String prettyPrint() { // TODO: Get rid of unnecessary parentheses in applications
        switch (type) {
            case VAR:
                return name;
            case LAM:
                return "L " + left.prettyPrint() + " . " + right.prettyPrint();
            case APP:
                String fun = left.prettyPrint();
                String arg = right.prettyPrint();
                if (left.type != VAR)
                    fun = "(" + fun + ")";
                if (right.type != VAR)
                    arg = "(" + arg + ")";
                return fun + " " + arg;
        }
        return "";
    }

    public String toString() {
        switch (type) {
            case VAR:
                return "VAR(" + name + ")";
            case LAM:
                return "LAM(" + left + ", " + right + ")";
            case APP:
                return "APP(" + left + ", " + right + ")";
        }
        return "";
    }

    public boolean equals(Object other) {
        return (other instanceof Term) && alphaEquals((Term) other);
    }

    public boolean alphaEquals(Term other) {
        return alphaEquals(other, new HashMap<>());
    }

    public boolean alphaEquals(Term other, Map<String, String> boundMap) {
        if (this.type != other.type)
            return false;

        if (this.type == VAR) {
            if (boundMap.containsKey(this.name)) {
                return boundMap.get(this.name).equals(other.name);
            } else if (boundMap.containsValue(other.name)){
                return false;
            } else {
                return this.name.equals(other.name);
            }
        }

        if (this.type == APP) {
            return this.left.alphaEquals(other.left, boundMap) && this.right.alphaEquals(other.right, boundMap);
        }

        if (this.type == LAM) {
            String shadowed = boundMap.getOrDefault(this.left.name, null);
            boundMap.put(this.left.name, other.left.name);
            boolean equal = this.right.alphaEquals(other.right, boundMap);
            if (shadowed != null)
                boundMap.put(this.left.name, shadowed);
            return equal;
        }

        throw new RuntimeException("Illegal term type");
    }
}
