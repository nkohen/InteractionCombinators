package Regex;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * <p>
 * This class describes a Non-deterministic Finite Automaton for a given regular expression
 * </p>
 *
 * <p>
 * NFA does not currently support matching a String against a regex,
 * if this is your intent, see {@link DFA} which turns an NFA into an equivalent Deterministic Finite Automaton
 * </p>
 *
 * <p>
 * For supported ways of writing regular expressions, see {@link RegexAST}
 * </p>
 */
public class NFA {
    /**
     * The char value of the wildcard character (currently DEL)
     */
    public static final char WILDCARD = '\177';

    static class Node {
        // If Node is an acceptState, this is what it matches (for use in Lexer)
        String regexMatch;

        // Transitions where '\0' is the empty transition
        Map<Character, List<Node>> neighbors = new HashMap<>();

        // Add transition from key to neighbor
        void put(char key, Node neighbor) {
            if (!neighbors.containsKey(key)) {
                neighbors.put(key, new ArrayList<>());
            }

            neighbors.get(key).add(neighbor);
        }
    }

    Node startState = null;
    List<Node> acceptStates = new ArrayList<>();

    /**
     * Constructs an NFA that matches any of the given regular expressions
     * where accepting states are labeled with given names
     * @param names An array of the labels where {@code names[i]} corresponds to {@code regex[i]}
     * @param regex An array of valid regular expressions to be matched
     * @return The constructed NFA
     */
    public static NFA makeNFA(String[] names, String[] regex) {
        List<RegexAST> trees = Stream.of(regex).map(RegexAST::new).collect(Collectors.toList());
        RegexAST[] treesArray = new RegexAST[trees.size()];
        return makeNFA(names, trees.toArray(treesArray));
    }

    /**
     * Constructs an NFA that matches any of the given regular expressions
     * where accepting states are labeled with given names
     * @param names An array of the labels where {@code names[i]} corresponds to {@code regex[i]}
     * @param regex An array of {@link RegexAST}s to be matched
     * @return The constructed NFA
     */
    public static NFA makeNFA(String[] names, RegexAST[] regex) {
        List<NFA> nfas = Stream.of(regex).map(NFA::makeNFA).collect(Collectors.toList());
        for (int i = 0; i < names.length; i++) {
            final int index = i;
            nfas.get(i).acceptStates.forEach(node -> node.regexMatch = names[index]);
        }

        NFA result = nfas.get(0);
        for (int i = 1; i < nfas.size(); i++) {
            NFA newResult = new NFA();
            newResult.startState = new Node();
            newResult.acceptStates = result.acceptStates;
            newResult.acceptStates.addAll(nfas.get(i).acceptStates);
            newResult.startState.put('\0', result.startState);
            newResult.startState.put('\0', nfas.get(i).startState);
            result = newResult;
        }

        return result;
    }

    /**
     * Constructs an NFA that matches the given regular expression
     * @param regex The valid regular expression to be matched
     * @return The constructed NFA
     */
    public static NFA makeNFA(String regex) {
        return makeNFA(new RegexAST(regex));
    }

    /**
     * Constructs an NFA that matches the given regular expression
     * @param regex A {@link RegexAST} to be matched
     * @return The constructed NFA
     */
    public static NFA makeNFA(RegexAST regex) {
        NFA nfa = new NFA();

        // If regex is emptyword, then create an NFA that matches only emptyword
        if (regex.isEmptyWord()) {
            nfa.startState = new Node();
            nfa.acceptStates.add(nfa.startState);
            return nfa;
        }

        // If regex is the wildcard, then create NFA that matches any single character
        if (regex.isWildcard()) {
            Node start = new Node();
            Node end = new Node();
            start.put(WILDCARD, end);
            nfa.startState = start;
            nfa.acceptStates.add(end);
            return nfa;
        }

        // If regex is just a character, create an NFA that matches only that character
        if (!regex.isOperator()) {
            Node start = new Node();
            Node end = new Node();
            start.put(regex.value(), end);
            nfa.startState = start;
            nfa.acceptStates.add(end);
            return nfa;
        }

        // If regex's root is an operator, call makeNFA on the children and combine them
        switch (regex.operator()) {
            case '^':
                NFA nfaLeft = makeNFA(regex.left());
                NFA nfaRight = makeNFA(regex.right());
                nfa.startState = nfaLeft.startState;
                nfa.acceptStates = nfaRight.acceptStates;
                for (Node node : nfaLeft.acceptStates) {
                    node.put('\0', nfaRight.startState);
                }
                break;
            case '|':
                nfaLeft = makeNFA(regex.left());
                nfaRight = makeNFA(regex.right());
                nfa = new NFA();
                nfa.startState = new Node();
                nfa.acceptStates = nfaLeft.acceptStates;
                nfa.acceptStates.addAll(nfaRight.acceptStates);
                nfa.startState.put('\0', nfaLeft.startState);
                nfa.startState.put('\0', nfaRight.startState);
                break;
            case '*':
                nfa = makeNFA(regex.left());
                for (Node node : nfa.acceptStates) {
                    node.put('\0', nfa.startState);
                }
                nfa.acceptStates.add(nfa.startState);
        }
        return nfa;
    }

    /**
     * Returns A GraphViz representation of this NFA
     * @return A GraphViz representation of this NFA with labeled edges for transitions
     * (where eps, short for epsilon, is the label for the empty word)
     */
    public String toString() {
        StringBuilder out = new StringBuilder("digraph G {\nahead [shape = plaintext, label = \"\"];\nahead-> a0;\n");
        int nextName = 0;
        Map<Node, Integer> name = new HashMap<>();
        java.util.Queue<Node> toProcess = new LinkedList<>();
        toProcess.add(startState);
        name.put(startState, nextName++);
        while (!toProcess.isEmpty()) {
            Node currentNode = toProcess.poll();
            for (Character c : currentNode.neighbors.keySet()) {
                for(Node neighbor : currentNode.neighbors.get(c)) {
                    if (!name.keySet().contains(neighbor)) {
                        name.put(neighbor, nextName++);
                        toProcess.add(neighbor);
                    }

                    String label;
                    switch (c) {
                        case '\0': label = "eps"; break;
                        case NFA.WILDCARD: label = "WILDCARD"; break;
                        case ' ': label = "SPACE"; break;
                        case '\n': label = "NEWLINE"; break;
                        case '\t': label = "TAB"; break;
                        case '\f': label = "FORMFEED"; break;
                        case '\r': label = "CARRIAGERETURN"; break;
                        case '\\': label = "BACKSLASH"; break;
                        default: label = Character.toString(c);
                    }

                    out.append("a").append(name.get(currentNode)).append(" -> a").append(name.get(neighbor))
                            .append(" [label = \"").append(label).append("\"];\n");
                }
            }
        }

        for (Node node : acceptStates) {
            //out += "a" + name.get(node) + " [shape = doublecircle];\n";
            out.append("a").append(name.get(node)).append(" [shape = doublecircle");
            if (node.regexMatch != null && !node.regexMatch.isEmpty())
                out.append(", label = \"").append(node.regexMatch).append("\"");
            out.append("];\n");
        }

        out.append("}\n");

        return out.toString();
    }
}
