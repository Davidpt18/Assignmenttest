import java.util.*;

public class RegexEngine {

    public boolean isValid(String re) {
        if (re.isEmpty()) {
            return false;
        }
        if(re.charAt(0)=='*' || re.charAt(0)==' ')
            {
                return false;
            }
        int openParenCount = 0;
        for (char c : re.toCharArray()) {
            if (!Character.isLetter(c) && !Character.isDigit(c) && c != '(' && c != ')' && c != '*' && c != '+' && c != '|' && c != ' ') {
                return false;
            }
            if (c == '(') openParenCount++;
            if (c == ')') openParenCount--;
            if (openParenCount < 0) return false; // Closing parenthesis before matching open
        }
        return openParenCount == 0; // Ensure all parentheses are balanced
    }

    public static class Transition {
        int from;
        int to;
        Character alpha;

        public Transition(int from, int to, Character alpha) {
            this.from = from;
            this.to = to;
            this.alpha = alpha;
        }
    }

    public static class ENFA {
        ArrayList<Integer> states;
        ArrayList<Transition> transitions;
        int accept;
        int start;

        public ENFA() {
            this.states = new ArrayList<>();
            this.transitions = new ArrayList<>();
            this.accept = 0;
            this.start = 0;
        }

        public ENFA(char ch) {
            this.states = new ArrayList<>();
            this.transitions = new ArrayList<>();
            this.createState(2);
            this.accept = 1;
            this.start = 0;
            this.transitions.add(new Transition(0, 1, ch));
        }

        public ENFA(int n) {
            this.states = new ArrayList<>();
            this.transitions = new ArrayList<>();
            this.accept = 0;
            this.start = 0;
            this.createState(n);
        }

        public void createState(int n) {
            for (int i = 0; i < n; i++) {
                this.states.add(i);
            }
        }

        public void display() {
            System.out.println("Transition Table:");
            for (Transition t : transitions) {
                System.out.println("(" + t.from + ", " + t.alpha + ", " + t.to + ")");
            }
        }
    }

    public static ENFA kleenePlus(ENFA enfa1) {
        ENFA result = new ENFA(enfa1.states.size() + 1);
        result.transitions.add(new Transition(0, 1, 'E'));
        for (Transition t : enfa1.transitions) {
            result.transitions.add(new Transition(t.from + 1, t.to + 1, t.alpha));
        }
        result.transitions.add(new Transition(enfa1.states.size(), 1, 'E'));
        result.transitions.add(new Transition(enfa1.states.size(), enfa1.states.size() + 1, 'E'));
        result.accept = enfa1.states.size() + 1;
        return result;
    }

    public static ENFA kleene(ENFA enfa1) {
        ENFA result = new ENFA(enfa1.states.size() + 2);
        result.transitions.add(new Transition(0, 1, 'E'));
        for (Transition t : enfa1.transitions) {
            result.transitions.add(new Transition(t.from + 1, t.to + 1, t.alpha));
        }
        result.transitions.add(new Transition(enfa1.states.size(), enfa1.states.size() + 1, 'E'));
        result.transitions.add(new Transition(enfa1.states.size(), 1, 'E'));
        result.transitions.add(new Transition(0, enfa1.states.size() + 1, 'E'));
        result.accept = enfa1.states.size() + 1;
        return result;
    }

    public static ENFA concat(ENFA enfa1, ENFA enfa2) {
        enfa2.states.remove(0);
        for (Transition t : enfa2.transitions) {
            enfa1.transitions.add(new Transition(t.from + enfa1.states.size() - 1, t.to + enfa1.states.size() - 1, t.alpha));
        }
        for (Integer i : enfa2.states) {
            enfa1.states.add(i + enfa1.states.size() - 1);
        }
        enfa1.accept = enfa1.states.size() - 1;
        return enfa1;
    }

    public static ENFA union(ENFA enfa1, ENFA enfa2) {
        ENFA result = new ENFA(enfa1.states.size() + enfa2.states.size() + 2);
        result.transitions.add(new Transition(0, 1, 'E'));
        for (Transition t : enfa1.transitions) {
            result.transitions.add(new Transition(t.from + 1, t.to + 1, t.alpha));
        }
        result.transitions.add(new Transition(enfa1.states.size(), enfa1.states.size() + enfa2.states.size() + 1, 'E'));
        result.transitions.add(new Transition(0, enfa1.states.size() + 1, 'E'));
        for (Transition t : enfa2.transitions) {
            result.transitions.add(new Transition(t.from + enfa1.states.size() + 1, t.to + enfa1.states.size() + 1, t.alpha));
        }
        result.transitions.add(new Transition(enfa2.states.size() + enfa1.states.size(), enfa1.states.size() + enfa2.states.size() + 1, 'E'));
        result.accept = enfa1.states.size() + enfa2.states.size() + 1;
        return result;
    }

    public static ENFA isInputValid(String re) {
        int pcount = 0;
        char ch;
        boolean cflag = false;
        ENFA enfa1, enfa2;
        Stack<ENFA> operand = new Stack<>();
        Stack<Character> operator = new Stack<>();
        Stack<ENFA> stack_concat = new Stack<>();

        for (int i = 0; i < re.length(); i++) {
            ch = re.charAt(i);
            if (Character.isLetter(ch) || Character.isDigit(ch)) {
                operand.push(new ENFA(ch));
                if (cflag) {
                    operator.push('.');
                } else {
                    cflag = true;
                }
            } else {
                if (ch == '(') {
                    operator.push(ch);
                    pcount = pcount + 1;
                } else if (ch == '*') {
                    operand.push(kleene(operand.pop()));
                    cflag = true;
                } else if (ch == '+') {
                    operand.push(kleenePlus(operand.pop()));
                    cflag = true;
                } else if (ch == ')') {
                    if (pcount == 0) {
                        cflag = false;
                        System.out.println("Unbalanced Parenthesis");
                        System.exit(1);
                    } else {
                        pcount = pcount - 1;
                    }
                    while (!operator.isEmpty() && operator.peek() != '(') {
                        char op = operator.pop();
                        if (op == '.') {
                            enfa1 = operand.pop();
                            enfa2 = operand.pop();
                            operand.push(concat(enfa2, enfa1));
                        } else if (op == '|') {
                            enfa1 = operand.pop();
                            if (!operator.isEmpty() && operator.peek() == '.') {
                                stack_concat.push(operand.pop());
                                while (!operator.isEmpty() && operator.peek() == '.') {
                                    stack_concat.push(operand.pop());
                                    operator.pop();
                                }
                                enfa2 = concat(stack_concat.pop(), stack_concat.pop());
                                while (!stack_concat.isEmpty()) {
                                    enfa2 = concat(enfa2, stack_concat.pop());
                                }
                            } else {
                                enfa2 = operand.pop();
                            }
                            operand.push(union(enfa2, enfa1));
                        }
                    }
                    operator.pop(); // Pop '(' from operator stack
                } else if (ch == '|') {
                    operator.push(ch);
                    cflag = false;
                }
            }
        }

        while (!operator.isEmpty()) {
            char op = operator.pop();
            if (op == '.') {
                enfa1 = operand.pop();
                enfa2 = operand.pop();
                operand.push(concat(enfa2, enfa1));
            } else if (op == '|') {
                enfa1 = operand.pop();
                if (!operator.isEmpty() && operator.peek() == '.') {
                    stack_concat.push(operand.pop());
                    while (!operator.isEmpty() && operator.peek() == '.') {
                        stack_concat.push(operand.pop());
                        operator.pop();
                    }
                    enfa2 = concat(stack_concat.pop(), stack_concat.pop());
                    while (!stack_concat.isEmpty()) {
                        enfa2 = concat(enfa2, stack_concat.pop());
                    }
                } else {
                    enfa2 = operand.pop();
                }
                operand.push(union(enfa2, enfa1));
            }
        }

        return operand.pop();
    }

    public static boolean isAccepted(ENFA enfa, String input) {
        Set<Integer> currentStates = epsilonClosure(enfa, enfa.start);

        for (char c : input.toCharArray()) {
            Set<Integer> nextStates = new HashSet<>();
            for (int state : currentStates) {
                for (Transition t : enfa.transitions) {
                    if (t.from == state && t.alpha == c) {
                        nextStates.add(t.to);
                    }
                }
            }
            currentStates = new HashSet<>(nextStates);
            // Compute epsilon closure after every character
            Set<Integer> epsilonClosureStates = new HashSet<>();
            for (int state : currentStates) {
                epsilonClosureStates.addAll(epsilonClosure(enfa, state));
            }
            currentStates = epsilonClosureStates;
        }

        // After processing the entire input, check if any of the current states are accept states
        return currentStates.contains(enfa.accept);
    }

    private static Set<Integer> epsilonClosure(ENFA enfa, int state) {
        Set<Integer> closure = new HashSet<>();
        Stack<Integer> stack = new Stack<>();
        stack.push(state);
        while (!stack.isEmpty()) {
            int s = stack.pop();
            if (!closure.contains(s)) {
                closure.add(s);
                for (Transition t : enfa.transitions) {
                    if (t.from == s && t.alpha == 'E') {
                        stack.push(t.to);
                    }
                }
            }
        }
        return closure;
    }

    public static void main(String[] args) {
        String re, input;
        boolean valid;
        boolean verboseMode = args.length > 0 && "-v".equals(args[0]);
        RegexEngine as = new RegexEngine();
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the regular Expression:");
        re = sc.nextLine();
        valid = as.isValid(re);
        if (!valid) {
            System.out.println("Invalid Expression");
            System.exit(1);
        } else {
            ENFA output = as.isInputValid(re);
            if (verboseMode) {
                output.display();
            }
            System.out.println("ready");
            while (true) {

                input = sc.nextLine();
                if(input.length()<=2)
                {
                    if(input.charAt(0)==' ' && input.charAt(1)=='*')
                    {
                        System.out.println("false");
                        continue;

                    }
                }
                if (isAccepted(output, input)) {
                    System.out.println("true");
                } else {
                    System.out.println("false");
                }
            }
        }
    }
}
