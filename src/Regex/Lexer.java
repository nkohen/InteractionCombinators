package Regex;

import java.util.*;

/**
 * <p>
 * This class describes a Lexical analyser which, given an array of regular expressions,
 * can tokenize any input String by matching those tokens by using a Determinist Finite Automaton as in {@link DFA}
 * </p>
 *
 * <p>
 * For supported ways of writing regular expressions, see {@link RegexAST}
 * </p>
 */
public class Lexer extends DFA { // TODO: Add Unit tests
    /**
     * Matches a single digit, i.e. [0-9]
     */
    public static final String DIGIT = "(0|1|2|3|4|5|6|7|8|9)";

    /**
     * Matches a single lowercase letter, i.e. [a-z]
     */
    public static final String LOWER_CASE = "(a|b|c|d|e|f|g|h|i|j|k|l|m|n|o|p|q|r|s|t|u|v|w|x|y|z)";

    /**
     * Matches a single uppercase letter, i.e. [A-Z]
     */
    public static final String UPPER_CASE = "(A|B|C|D|E|F|G|H|I|J|K|L|M|N|O|P|Q|R|S|T|U|V|W|X|Y|Z)";

    /**
     * Matches a single uppercase or lowercase letter, i.e. [a-z]|[A-Z]
     */
    public static final String LETTER = "(" + LOWER_CASE + "|" + UPPER_CASE + ")";

    /**
     * Matches a single whitespace character
     */
    public static final String SINGLE_WHITESPACE = "( |\t|\n|\f|\r)";

    /**
     * Matches any number of consecutive whitespace characters
     */
    public static final String OPTIONAL_WHITESPACE = SINGLE_WHITESPACE + "*";

    /**
     * Matches any positive number (at least one) of consecutive whitespace characters
     */
    public static final String WHITESPACE = SINGLE_WHITESPACE + "+";

    /**
     * This class describes a single matched token,
     * storing both the token's value and which regex was matched
     */
    public static class Token {
        public String value;
        public String type;

        /**
         * Constructs a Lexer token
         * @param value The substring matched
         * @param type The name/label of the regex that matched this token
         */
        public Token(String value, String type) {
            this.value = value;
            this.type = type;
        }

        public String toString() {
            return value + " : " + type;
        }
    }

    /**
     * Constructs a Lexer with the given regular expressions and their names
     * @param names An array of the labels where {@code names[i]} corresponds to {@code tokenRegex[i]}
     * @param tokenRegex An array of valid regular expressions to be matched
     */
    public Lexer(String[] names, String[] tokenRegex) {
        super(names, tokenRegex);
        this.omitNames = new ArrayList<>();
        this.priority = null;
    }

    /**
     * Constructs a Lexer with the given regular expressions and their names which skips certain specified tokens
     * @param names An array of the labels where {@code names[i]} corresponds to {@code tokenRegex[i]}
     * @param tokenRegex An array of valid regular expressions to be matched
     * @param omitNames An array of labels in {@code names} that should be skipped/omitted
     */
    public Lexer(String[] names, String[] tokenRegex, String[] omitNames) {
        super(names, tokenRegex);
        this.omitNames = List.of(omitNames);
        this.priority = null;
    }

    /**
     * Constructs a Lexer with the given prioritized regular expressions and their names
     * @param names An array of the labels where {@code names[i]} corresponds to {@code tokenRegex[i]}
     * @param priority A map from labels in {@code names} to Integers to disambiguate when multiple tokens are matched
     *                 higher numbers correspond to higher priorities and if a label is absent from the map it gets the
     *                 default value 0
     * @param tokenRegex An array of valid regular expressions to be matched
     */
    public Lexer(String[] names, Map<String, Integer> priority, String[] tokenRegex) {
        super(names, tokenRegex);
        this.omitNames = new ArrayList<>();
        this.priority = priority;
    }

    /**
     * Constructs a Lexer with the given prioritized regular expressions and their names
     * which skips certain specified tokens
     * @param names An array of the labels where {@code names[i]} corresponds to {@code tokenRegex[i]}
     * @param priority A map from labels in {@code names} to Integers to disambiguate when multiple tokens are matched
     *                 higher numbers correspond to higher priorities and if a label is absent from the map it gets the
     *                 default value 0
     * @param tokenRegex An array of valid regular expressions to be matched
     * @param omitNames An array of labels in {@code names} that should be skipped/omitted
     */
    public Lexer(String[] names, Map<String, Integer> priority, String[] tokenRegex, String[] omitNames) {
        super(names, tokenRegex);
        this.omitNames = List.of(omitNames);
        this.priority = priority;
    }

    private int index;
    private String input;
    private String nextToken;
    private List<String> omitNames;
    private Set<String> lastMatchNames;
    private Map<String, Integer> priority;

    /**
     * Initializes the Lexer with a String to tokenize
     * Every call resets the Lexer to read from the beginning of the input
     * @param input The input String to tokenize
     * @return This Lexer for fluent calls
     */
    public Lexer init(String input) {
        this.index = 0;
        this.input = input;
        nextToken = null;

        return this;
    }

    /**
     * Resets this Lexer on the current input String
     * @return This Lexer for fluent calls
     */
    public Lexer reset() {
        index = 0;
        nextToken = null;
        lastMatchNames = null;

        return this;
    }

    /**
     * Returns the label of the last token matched
     * @return The label of the last token matched (filtered for priority if applicable)
     */
    public String lastMatchType() {
        if (priority != null) {
            return DFA.toName(highestPriority(lastMatchNames));
        } else {
            return DFA.toName(lastMatchNames);
        }
    }

    private Set<String> highestPriority(Set<String> nameSet) {
        Set<String> highest = new HashSet<>();
        int max = Integer.MIN_VALUE;
        for (String name : nameSet) {
            int num = priority.getOrDefault(name, 0);

            if (num > max) {
                max = num;
                highest = new HashSet<>();
            }
            if (max == num)
                highest.add(name);
        }
        return highest;
    }

    /**
     * Returns the next token matched and not skipped
     * @return The next token matched and not skipped in the input String
     */
    public Token nextToken() {
        return new Token(next(), lastMatchType());
    }

    /**
     * Returns the next String matched and not skipped
     * @return The next String matched and not skipped in the input String
     */
    public String next() {
        String nextToken = null;
        boolean skip = true;
        while (skip) {
            nextToken = nextMatch();
            skip = false;
            for (String name : lastMatchNames) {
                if (omitNames.contains(name)) {
                    skip = true;
                    break;
                }
            }
        }
        return nextToken;
    }

    /**
     * Returns the next token matched
     * @return The next token matched in the input String (possibly an omitted type)
     */
    public Token nextMatchedToken() {
        return new Token(nextMatch(), lastMatchType());
    }

    /**
     * Returns the next String matched
     * @return The next String matched in the input String (possibly an omitted type)
     */
    public String nextMatch() {
        if (nextToken != null) {
            String temp = nextToken;
            nextToken = null;
            return temp;
        }
        lastMatchNames = null;

        Node current = startState;
        int startIndex = index;
        int lastMatchIndex = -1;

        while (index < input.length()) {
            if (!current.neighbors.containsKey(input.charAt(index))) {
                if (current.neighbors.containsKey(NFA.WILDCARD)) {
                    current = current.neighbors.get(NFA.WILDCARD);
                } else {
                    index = lastMatchIndex;
                    break;
                }
            } else
                current = current.neighbors.get(input.charAt(index));

            index++;
            if (acceptStates.contains(current)) {
                lastMatchIndex = index;
                lastMatchNames = current.regexMatch;
            }
        }

        if (lastMatchIndex == -1) {
            index = startIndex;
            lastMatchNames = null;
            throw new NoSuchElementException();
        } else {
            index = lastMatchIndex;
            return input.substring(startIndex, lastMatchIndex);
        }
    }

    /**
     * Returns true if the input has a next match
     * @return True if the input String has any more non-omitted tokens to be matched
     */
    public boolean hasNext() {
        if (nextToken != null)
            return true;

        try {
            nextToken = next();
        } catch (NoSuchElementException e) {
            return false;
        }

        return true;
    }

    /**
     * Returns true if the input String has any tokens left
     * @return True if the input String has any tokens left to be matched (includes tokens to be skipped)
     */
    public boolean hasNextMatch() {
        if (nextToken != null)
            return true;

        try {
            nextToken = nextMatch();
        } catch (NoSuchElementException e) {
            return false;
        }

        return true;
    }

    /**
     * Returns the remaining unread substring
     * @return The rest of the input that can be matched as an array of {@link Token}s<br>
     * The unmatched portion of the input is added as the last Token whose type is "UNMATCHED by Lexer"
     */
    public Token[] tokenize() {
        List<Token> tokens = new ArrayList<>();
        while (hasNext()) {
            tokens.add(nextToken());
        }
        if(!remaining().isEmpty())
            tokens.add(new Token(remaining(), "UNMATCHED by Lexer"));
        Token[] finalTokens = new Token[tokens.size()];
        tokens.toArray(finalTokens);
        return finalTokens;
    }

    /**
     * Returns the remaining unseen portion of the input String <br>
     * Note that a call to {@code Lexer::hasNext} or {@code Lexer::hasNextMatch} will cause this method to be missing
     * the next token because that part of the input String has been seen
     * @return The remaining unseen portion of the input String.
     */
    public String remaining() {
        return input.substring(index);
    }

    public static void main(String[] args) {
        String identifier = "(" + LETTER + "|" + DIGIT + ")*";
        String number = DIGIT + "+";
        String operation = "\\+|\\*|/|-|%";
        String comment = "\\\\\\*.*\\*\\\\|//.*\n";

        String[] omit = {"WhiteSpace", "Comment"};
        String[] names = {"Name", "Int", "Operation", "WhiteSpace", "EQ", "Comment"};
        String[] tokens = {identifier, number, operation, OPTIONAL_WHITESPACE, "=", comment};
        Lexer lexer = new Lexer(names, tokens);
        Lexer lexer1 = new Lexer(names, tokens, omit);
        Map<String, Integer> priority = new HashMap<>();
        priority.put("Name", -1);
        Lexer lexer2 = new Lexer(names, priority, tokens, omit);

        lexer.init("AYY +LMAO\\* Just a comment sakd.f/qer89\nqpon;asoifj\0\127*\\= 42// Hi!\n");
        System.out.println("lexer\n------");
        while(lexer.hasNext()) {
            System.out.println(lexer.next() + " : " + lexer.lastMatchType());
        }

        System.out.println();

        lexer1.init("AYY +LMAO\\* Just a comment sakd.f/qer89\nqpon;asoifj\0\127*\\= 42// Hi!\n");
        System.out.println("lexer1\n------");
        while(lexer1.hasNext()) {
            System.out.println(lexer1.next() + " : " + lexer1.lastMatchType());
        }

        System.out.println();

        lexer2.init("AYY +LMAO\\* Just a comment sakd.f/qer89\nqpon;asoifj\0\127*\\= 42// Hi!\n?");
        System.out.println("lexer2\n------");
        while(lexer2.hasNext()) {
            System.out.println(lexer2.next() + " : " + lexer2.lastMatchType());
        }

        System.out.println();

        System.out.println("lexer3");
        Arrays.stream(new Lexer(names, priority, tokens, omit)
                .init("Hello + World= \\*Oops*\\Java9?Oops again, can't have ?").tokenize()).forEach(System.out::println);
    }
}
