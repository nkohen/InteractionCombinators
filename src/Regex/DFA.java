package Regex;

import java.util.*;

/**
 * <p>
 * This class describes a Deterministic Finite Automaton for a given regular expression
 * </p>
 *
 * <p>
 * DFA is constructed by first creating a {@link NFA} and then constructing an equivalent deterministic automaton,
 * the DFA can then match a given string against the regular expression it was constructed with
 * </p>
 *
 * <p>
 * For supported ways of writing regular expressions, see {@link RegexAST}
 * </p>
 */
public class DFA {
    class Node {
        // If Node is an acceptState, these are what it matches (for use in Lexer)
        Set<String> regexMatch;

        // For use in traversals
        boolean marked = false;

        // true iff this Node's neighbors field has been finished
        boolean neighborSet = false;

        // Transitions
        Map<Character, Node> neighbors = new HashMap<>();

        // The set of NFA nodes this Node represents
        Set<NFA.Node> set;

        Node(Set<NFA.Node> set) {
            this.set = set;
        }

        Node() {}
    }

    /**
     * Returns the elements of {@code names} separated by " | "
     * @param names A set of labels
     * @return The elements of {@code names} separated by " | "
     */
    public static String toName(Set<String> names) {
        StringBuilder name = new StringBuilder();
        names.forEach(n -> name.append(n).append(" | "));
        if (name.length() > 0)
            name.delete(name.length()-3, name.length());
        return name.toString();
    }

    Node startState;
    List<Node> acceptStates = new ArrayList<>();

    /**
     * Constructs a DFA that matches any of the given regular expressions
     * where accepting states are labeled with given names<br>
     *
     * The DFA is minimal except for its separation of accepting states with different labels
     * @param names An array of the labels where {@code names[i]} corresponds to {@code regex[i]}
     * @param regex An array of valid regular expressions to be matched
     */
    public DFA(String[] names, String[] regex) {
        initFrom(NFA.makeNFA(names, regex));
    }

    /**
     * Constructs a minimal DFA that matches the given regular expression
     * @param regex A valid regular expression to be matched
     */
    public DFA(String regex) {
        initFrom(NFA.makeNFA(regex));
    }

    /**
     * Constructs a DFA that is equivalent to the given NFA
     *
     * The DFA is minimal except for its separation of accepting states with different labels
     * if the accepting states are labeled
     * @param nfa A {@link NFA} to be compiled
     */
    public DFA(NFA nfa) {
        initFrom(nfa);
    }

    private void initFrom(NFA nfa) {
        List<Node> allNodes = new ArrayList<>();

        Set<NFA.Node> startSet = closure(List.of(nfa.startState));

        startState = new Node(startSet);
        allNodes.add(startState);

        // Add startState to acceptStates if startSet contains any of nfa's acceptStates
        if (containsFinalState(nfa.acceptStates, startSet))
            acceptStates.add(startState);

        // Nodes to be set
        Queue<Node> nodeLine = new LinkedList<>();
        // Beginning with startState
        nodeLine.add(startState);

        while (!nodeLine.isEmpty()) {
            Node currentNode = nodeLine.poll();

            // Map each character to the set of NFA Nodes that can be reached from currentNode.set
            Map<Character, Set<NFA.Node>> transition = new HashMap<>();

            // Initialize transition
            for (NFA.Node node : currentNode.set) {
                for (Character c : node.neighbors.keySet()) {
                    // Ignore empty transitions
                    if (c == '\0')
                        continue;

                    if (!transition.containsKey(c))
                        transition.put(c, new HashSet<>());
                    transition.get(c).addAll(closure(node.neighbors.get(c)));

                    // IMPORTANT: Currently non-wildcard transitions have priority over wildcard transitions
                    //            Should this instead add those reached by the wildcard to every transition also?
                }
            }

            // Initialize currentNode.transition (using put)
            for (Character c : transition.keySet()) {
                Set<NFA.Node> set = transition.get(c);
                Node neighbor = getOrCreateNode(allNodes, set);
                currentNode.neighbors.put(c, neighbor);

                // If neighbor has not yet been processed or added to nodeLine, then add it to nodeLine
                if (!neighbor.neighborSet && !nodeLine.contains(neighbor) && neighbor != currentNode) {
                    nodeLine.add(neighbor);
                    allNodes.add(neighbor);

                    // If neighbor.set (= set) contains an accept state of nfa, then add neighbor to acceptStates
                    if (containsFinalState(nfa.acceptStates, set)) {
                        acceptStates.add(neighbor);
                        initRegexMatch(nfa.acceptStates, neighbor);
                    }
                }
            }

            // Mark currentNode as initialized
            currentNode.neighborSet = true;
        }

        // Remove information in Nodes that doesn't pertain to transitions
        clearPowerSetStates(allNodes);

        // Make into a minimal DFA
        //minimize();
    }

    private void initRegexMatch(List<NFA.Node> finalStates, Node node) {
        node.regexMatch = new HashSet<>();

        for (NFA.Node n : node.set) {
            if (finalStates.contains(n)) {
                if (n.regexMatch == null) {
                    node.regexMatch = null;
                    break;
                }
                node.regexMatch.add(n.regexMatch);
            }
        }
    }

    // Turns the current DFA into an equivalent minimal DFA (with fewest states)
    private void minimize() {
        // Maps Nodes to the sets containing them
        Map<Node, Set<Node>> membership = new HashMap<>();

        Set<Node> finalStates = new HashSet<>(acceptStates);
        for (Node finalState : finalStates) {
            membership.put(finalState, finalStates);
        }

        Set<Node> notFinalStates = new HashSet<>();
        initNotFinal(startState, notFinalStates);
        for (Node notFinalState : notFinalStates) {
            membership.put(notFinalState, notFinalStates);
        }

        List<Set<Node>> partition = new LinkedList<>();
        partition.add(notFinalStates);
        partition.addAll(refineFinalStates(finalStates, membership));

        boolean done = false;
        while(!done) {
            done = true;

            for (int i = 0; i < partition.size(); i++) {
                // HOW IS THIS HAPPENING?
                if (partition.get(i).size() < 1) {
                    partition.remove(i);
                    i--;
                    continue;
                }

                // Refine the current subset
                Collection<Set<Node>> refinement = refine(partition.get(i), membership);

                // If new subsets were created during refinement,
                // replace the current subset with those
                if (refinement.size() > 1) {
                    partition.remove(i);
                    partition.addAll(refinement);
                    i--;
                    done = false;
                }
            }
        }

        updateDFA(partition, membership);
    }

    private Collection<Set<Node>> refineFinalStates(Set<Node> finalStates, Map<Node, Set<Node>> membership) {
        // If you want minimization with accept states possibly belonging to multiple tokens, uncomment the following
        // return List.of(finalStates);

        // TODO: This might be problematic if we have "A | B" and "B | A" which should go together but won't.
        Map<String, Set<Node>> matchRegexToSetMap = new HashMap<>();
        for (Node node : finalStates) {
            String name;
            if (node.regexMatch == null) {
                name = "\0";
            } else
                name = toName(node.regexMatch);
            if (!matchRegexToSetMap.containsKey(name))
                matchRegexToSetMap.put(name, new HashSet<>());
            matchRegexToSetMap.get(name).add(node);
            membership.put(node, matchRegexToSetMap.get(name));
        }

        return matchRegexToSetMap.values();
    }

    // Given a valid partition, sets this to be a new DFA
    // whose Nodes correspond to the subsets within the partition
    private void updateDFA(List<Set<Node>> partition, Map<Node, Set<Node>> membership) {
        // Maps subsets in partition to the new Nodes that they correspond to
        Map<Set<Node>, Node> newStates = new HashMap<>();

        // Initialize the correspondence to a bunch of empty Nodes
        for (Set<Node> subset : partition) {
            newStates.put(subset, new Node());
        }

        // This will correspond to all subsets made up of only accepting states
        List<Node> newAcceptStates = new ArrayList<>();

        // Set the neighbors field of each new Node
        for (Set<Node> subset : partition) {
            // Pick a representative
            Node representative = subset.iterator().next();

            // This corresponds to subset
            Node newNode = newStates.get(subset);

            // For each transition of the representative, create one for newNode
            for (Character c : representative.neighbors.keySet()) {
                Node neighbor = representative.neighbors.get(c);
                Set<Node> setNeighbor = membership.get(neighbor);
                newNode.neighbors.put(c, newStates.get(setNeighbor));
            }

            newNode.neighborSet = true;

            // If the representative is in acceptStates, then add newNode to newAcceptStates
            if (acceptStates.contains(representative)) {
                newAcceptStates.add(newNode);
                if (representative.regexMatch != null) {
                    newNode.regexMatch = new HashSet<>();
                    for (Node node : subset) {
                        newNode.regexMatch.addAll(node.regexMatch);
                    }
                }
            }
        }

        // The new startState will be the Node corresponding to the subset containing the old startState
        startState = newStates.get(membership.get(startState));
        acceptStates = newAcceptStates;
    }

    // Does a depth-first traversal of the graph starting at current
    // and adds all non-acceptStates to notFinalStates
    private void initNotFinal(Node current, Set<Node> notFinalStates) {
        if (!acceptStates.contains(current))
            notFinalStates.add(current);

        for (Node neighbor : current.neighbors.values()) {
            if (!neighbor.marked) {
                neighbor.marked = true;
                initNotFinal(neighbor, notFinalStates);
            }
        }
    }

    // If there are two states in group which transition into different groups on the same character,
    // then refine will separate them into separate Sets and update membership accordingly
    // (This is done only for one character per call to refine)
    private Collection<Set<Node>> refine(Set<Node> group, Map<Node, Set<Node>> membership) {
        // Stores the expected transitions for group
        Map<Character, Set<Node>> validator = new HashMap<>();

        // Initialize validator using the first Node in group
        Node first = group.iterator().next();
        for (Character c : first.neighbors.keySet()) {
            validator.put(c, membership.get(first.neighbors.get(c)));
        }

        char disagree = '\0';

        // Look for a Character on which a Node in group disagrees with first
        for (Node node : group) {
            // Compare node's transitions to first's
            for (Character c : node.neighbors.keySet()) {
                if (!validator.containsKey(c) ||
                        membership.get(node.neighbors.get(c)) != validator.get(c)) {
                    disagree = c;
                    break;
                }
            }

            // And check that node is not missing transitions
            for (Character c : validator.keySet()) {
                if (!node.neighbors.keySet().contains(c)) {
                    disagree = c;
                    break;
                }
            }
        }

        // Map from sets reached to partition subsets of group
        Map<Set<Node>, Set<Node>> partitioner = new HashMap<>();

        // A (key) value for if a Node does not have a transition
        final Set<Node> empty = new HashSet<>();

        // If all Nodes agree, then group need not be partitioned
        if (disagree == '\0') {
            partitioner.put(new HashSet<>(), group);
        } else {
            // Assign each node to a subset of group using partitioner
            for (Node node : group) {
                // The set reached by node on disagree
                Set<Node> target;
                if (!node.neighbors.containsKey(disagree))
                    target = empty;
                else
                    target = membership.get(node.neighbors.get(disagree));

                // Add node into the corresponding Set in partitioner
                // and update membership
                if (!partitioner.containsKey(target))
                    partitioner.put(target, new HashSet<>());
                partitioner.get(target).add(node);
                membership.put(node, partitioner.get(target));
            }
        }

        return partitioner.values();
    }

    private void clearPowerSetStates(List<Node> nodes) {
        for (Node node : nodes) {
            node.set = null;
        }
    }

    // Returns the set of all NFA Nodes that can be reached from inSet by using empty transitions
    private Set<NFA.Node> closure(List<NFA.Node> inSet) {
        Set<NFA.Node> set = new HashSet<>(inSet);
        boolean flag = true;
        Set<NFA.Node> prevExtra = set;

        // While new Nodes are being reached, add Nodes to the set
        while (flag) {
            flag = false;
            List<NFA.Node> extra = new LinkedList<>();
            for (NFA.Node node : prevExtra) {
                if (node.neighbors.containsKey('\0')) {
                    extra.addAll(node.neighbors.get('\0'));
                    flag = true;
                }
            }
            set.addAll(extra);
            prevExtra = new HashSet<>(extra);
        }

        return set;
    }

    // Returns true if the intersection of finalStates and states is non-trivial
    private boolean containsFinalState(List<NFA.Node> finalStates, Set<NFA.Node> states) {
        for (NFA.Node node : states) {
            if (finalStates.contains(node))
                return true;
        }
        return false;
    }

    // Gets the node in nodes whose set is set, or creates such a Node and adds it to nodes
    private Node getOrCreateNode(List<Node> nodes, Set<NFA.Node> set) {
        for (Node node : nodes) {
            if (node.set.equals(set))
                return node;
        }
        Node node = new Node(set);
        nodes.add(node);
        return node;
    }

    /**
     * Returns true if {@code input} is matched by this DFA
     * @param input A string to check against the regular expression(s) this DFA was constructed with
     * @return True if {@code input} matches the regular expression represented by this DFA
     */
    public boolean match(String input) {
        Node current = startState;
        for (int i = 0; i < input.length(); i++) {
            if (!current.neighbors.containsKey(input.charAt(i))) {
                if (current.neighbors.containsKey(NFA.WILDCARD))
                    current = current.neighbors.get(NFA.WILDCARD);
                else
                    return false;
            } else
                current = current.neighbors.get(input.charAt(i));
        }

        return acceptStates.contains(current);
    }

    /**
     * Returns a GraphViz representation of this DFA
     * @return A GraphViz representation of this DFA with labeled edges for transitions
     * (where eps, short for epsilon, is the label for the empty word)
     */
    public String toString() {
        StringBuilder out = new StringBuilder("digraph G {\nahead [shape = plaintext, label = \"\"];\nahead-> a0;\n");
        int nextName = 0;
        Map<Node, Integer> name = new HashMap<>();
        Queue<Node> toProcess = new LinkedList<>();
        toProcess.add(startState);
        name.put(startState, nextName++);
        while(!toProcess.isEmpty()) {
            Node currentNode = toProcess.poll();
            for (Character c : currentNode.neighbors.keySet()) {
                Node neighbor = currentNode.neighbors.get(c);
                if (!name.keySet().contains(neighbor)) {
                    name.put(neighbor, nextName++);
                    toProcess.add(neighbor);
                }

                String label;
                switch (c) {
                    case NFA.WILDCARD: label = "WILDCARD"; break;
                    case ' ': label = "SPACE"; break;
                    case '\n': label = "NEWLINE"; break;
                    case '\t': label = "TAB"; break;
                    case '\f': label = "FORMFEED"; break;
                    case '\r': label = "CARRIAGERETURN"; break;
                    case '\\': label = "BACKSLASH"; break;
                    default: label = Character.toString(c);
                }

                out.append("a").append(name.get(currentNode))
                        .append(" -> a").append(name.get(neighbor)).append(" [label = \"").append(label).append("\"];\n");
            }
        }

        for (Node node : acceptStates) {
            // out += "a" + name.get(node) + " [shape = doublecircle];\n";
            out.append("a").append(name.get(node)).append(" [shape = doublecircle");
            if (node.regexMatch != null && !node.regexMatch.isEmpty())
                out.append(", label = \"").append(toName(node.regexMatch)).append("\"");
            out.append("];\n");
        }

        out.append("}\n");

        return out.toString();
    }

    public static void main(String[] args) {
        DFA dfa = new DFA("((1|0(00)*01)((11|10(00)*01))*|(0(00)*1|(1|0(00)*01)((11|10(00)*01))*(0|10(00)*1))((1(00)*1|(0|1(00)*01)((11|10(00)*01))*(0|10(00)*1)))*(0|1(00)*01)((11|10(00)*01))*)");
        System.out.println(dfa);

        dfa = new DFA("(a|b)+ab*");
        System.out.println(dfa);

        dfa = new DFA("a?b{3}*");
        System.out.println(dfa);

        dfa = new DFA("a|b");
        System.out.println(dfa);

        String[] names = {"A", "B"};
        String[] tokens = {"a", "b"};
        dfa = new DFA(names, tokens);
        System.out.println(dfa);
    }
}
