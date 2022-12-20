import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayDeque;

class Parser {
	private String target;
	private Program program;
	Parser(String target) {
		this.target = target;
	}
	Program parse() throws InvalidSyntaxException {
		if (program != null) 
			return program;
		Tokenizer tz = new Tokenizer(target);
		program = new Program(tz.getTokens());
		return program;
	}
}

class Program {
	private HashMap<String, Integer> variables;
	Program(Token tokens[]) throws InvalidSyntaxException {
		variables = new HashMap<String, Integer>();
		parse(tokens);
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		for (String name : variables.keySet()) {
			sb.append(name);
			sb.append(" = ");
			sb.append(variables.get(name));
			sb.append(System.getProperty("line.separator"));
		}
		return sb.toString();
	}
	private void parse(Token tokens[]) throws InvalidSyntaxException {
		int start = 0;
		for (int i = 0; i < tokens.length + 1; i++) {
			try {
				Assignment.parse(Arrays.copyOfRange(tokens, start, i), variables);
				start = i--;
			} catch (InvalidSyntaxException e) {
				continue;
			}
		}
		if (start < tokens.length) 
			throw new InvalidSyntaxException();
	}
}

class Assignment {
	private Assignment() {}
	static void parse(Token tokens[], HashMap<String, Integer> variables) throws InvalidSyntaxException {
		if (tokens.length < 3) 
			throw new InvalidSyntaxException();
		if (!(tokens[0].tag == Tag.IDENTIFIER)) 
			throw new InvalidSyntaxException();
		String varName = tokens[0].getValue();
		if (!(tokens[1].tag == Tag.OPERATOR && tokens[1].getValue().equals("="))) 
			throw new InvalidSyntaxException("Variable \"" + varName + "\" is not initialized.");
		if (!(tokens[tokens.length-1].tag == Tag.OPERATOR && tokens[tokens.length-1].getValue().equals(";"))) 
			throw new InvalidSyntaxException();
		Exp exp = new Exp(Arrays.copyOfRange(tokens, 2, tokens.length-1), variables);
		int varValue = exp.getValue();
		variables.put(varName, varValue);
	}
}

interface Valuable {
	int getValue();
}

class Exp implements Valuable {
	private int value;
	Exp(Token tokens[], HashMap<String, Integer> variables) throws InvalidSyntaxException {
		check(tokens);
		parse(tokens, variables);
	}
	@Override
	public int getValue() {
		return value;
	}
	static void check(Token tokens[]) throws InvalidSyntaxException {
		for (int i = 0; i < tokens.length; i++) {
			if ((tokens[i].tag == Tag.OPERATOR) && 
				(tokens[i].getValue().equals("+") || 
				tokens[i].getValue().equals("-"))) {
				try {
					check(Arrays.copyOfRange(tokens, 0, i));
					Term.check(Arrays.copyOfRange(tokens, i+1, tokens.length));
					return;
				} catch (InvalidSyntaxException e) {
					continue;
				}
			}
		}
		Term.check(tokens);
	}
	private void parse(Token tokens[], HashMap<String, Integer> variables) 
			throws InvalidSyntaxException, EvaluationException {
		// Build reverse Polish expression.
		ArrayList<Token> reversePolish = new ArrayList<Token>();
		ArrayDeque<Token> stack = new ArrayDeque<Token>();
		String precedence[] = {"+-", "*", "uv", "()"};
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].tag == Tag.OPERATOR) {
				char operator = tokens[i].getValue().charAt(0);
				if ((operator == '+' || operator == '-') && 
						(i == 0 || (tokens[i-1].tag == Tag.OPERATOR && 
						tokens[i-1].getValue().charAt(0) != ')'))) {
					operator = 'u';  // unary + -
					tokens[i] = new Operator(tokens[i].getValue(), 1);
				}
				switch (operator) {
				case '(':
					stack.addLast(tokens[i]);
					break;
				case ')':
					while (stack.getLast().getValue().charAt(0) != '(') {
						reversePolish.add(stack.removeLast());
					}
					stack.removeLast();
					break;
				default:
					int operatorPrec = -1;
					for (int j = 0; j < precedence.length; j++) {
						if (precedence[j].indexOf(operator) >= 0) {
							operatorPrec = j;
							break;
						}
					}
					while (!stack.isEmpty()) {
						char op = stack.getLast().getValue().charAt(0);
						if (op == '(') 
							break;
						int opPrec = -1;
						for (int j = 0; j < precedence.length; j++) {
							if (precedence[j].indexOf(op) >= 0) {
								opPrec = j;
								break;
							}
						}
						if (operatorPrec > opPrec) 
							break;
						reversePolish.add(stack.removeLast());
					}
					stack.addLast(tokens[i]);
					break;
				}
			} else {
				reversePolish.add(tokens[i]);
			}
		}
		while (!stack.isEmpty()) 
			reversePolish.add(stack.removeLast());
		
		// Start evaluation of reverse Polish expression.
		while (true) {
			Token opToken = null;
			boolean found = false;
			int size = reversePolish.size();
			int i = 0;
			for (; i < size; i++) {
				opToken = reversePolish.get(i);
				if (opToken.tag == Tag.OPERATOR) {
					found = true;
					break;
				}
			}
			if (!found) 
				break;
			int value;
			if (((Operator)opToken).getOpNum() == 1) {  // unary
				value = getTokenValue(reversePolish.get(i-1), variables);
				if (opToken.getValue().charAt(0) == '-') 
					value = -value;
				Token vToken = new ValueToken(value);
				for (int k = 0; k < 2; k++) 
					reversePolish.remove(i-1);
				reversePolish.add(i-1, vToken);
			} else {  // binary
				switch (opToken.getValue().charAt(0)) {
				case '+':
					value = getTokenValue(reversePolish.get(i-2), variables) + 
							getTokenValue(reversePolish.get(i-1), variables);
					break;
				case '-':
					value = getTokenValue(reversePolish.get(i-2), variables) - 
							getTokenValue(reversePolish.get(i-1), variables);
					break;
				case '*':
					value = getTokenValue(reversePolish.get(i-2), variables) * 
							getTokenValue(reversePolish.get(i-1), variables);
					break;
				default:
					throw new EvaluationException("Unrecognized operator in expression.");
				}
				Token vToken = new ValueToken(value);
				for (int k = 0; k < 3; k++) 
					reversePolish.remove(i-2);
				reversePolish.add(i-2, vToken);
			}
		}
		if (!(reversePolish.size() == 1 && 
				(reversePolish.get(0).tag == Tag.LITERAL || 
				reversePolish.get(0).tag == Tag.VALUE))) 
			throw new EvaluationException();
		this.value = Integer.parseInt(reversePolish.get(0).getValue());
	}
	private int getTokenValue(Token token, HashMap<String, Integer> variables) 
			throws EvaluationException {
		switch (token.tag) {
		case VALUE:
			return Integer.parseInt(token.getValue());
		case LITERAL:
			return Integer.parseInt(token.getValue());
		case IDENTIFIER:
			String varName = token.getValue();
			if (!variables.containsKey(varName)) 
				throw new EvaluationException("Variable \"" + varName + "\" is undefined.");
			return variables.get(varName);
		default:
			throw new EvaluationException("Operand is not literal or variable.");
		}
	}
}

class Term {
	private Term() {}
	static void check(Token tokens[]) throws InvalidSyntaxException {
		for (int i = 0; i < tokens.length; i++) {
			if (tokens[i].tag == Tag.OPERATOR && tokens[i].getValue().equals("*")) {
				try {
					Term.check(Arrays.copyOfRange(tokens, 0, i));
					Fact.check(Arrays.copyOfRange(tokens, i+1, tokens.length));
					return;
				} catch (InvalidSyntaxException e) {
					continue;
				}
			}
		}
		Fact.check(tokens);
	}
}

class Fact {
	private Fact() {}
	static void check(Token tokens[]) throws InvalidSyntaxException {
		if (tokens.length < 1) 
			throw new InvalidSyntaxException();
		if (tokens[0].tag == Tag.OPERATOR) {
			if (tokens[0].getValue().equals("(") && 
					tokens[tokens.length-1].getValue().equals(")")) {
				Exp.check(Arrays.copyOfRange(tokens, 1, tokens.length-1));
			} else if (tokens[0].getValue().equals("+") || 
					tokens[0].getValue().equals("-")) {
				Fact.check(Arrays.copyOfRange(tokens, 1, tokens.length));
			} else 
				throw new InvalidSyntaxException();
		} else if (!(tokens.length == 1 && 
				(tokens[0].tag == Tag.LITERAL || 
				tokens[0].tag == Tag.IDENTIFIER))) 
			throw new InvalidSyntaxException();
	}
}

class InvalidSyntaxException extends RuntimeException {
	private static final long serialVersionUID = 5207509071877186587L;
	public InvalidSyntaxException() {
		super("Invalid Syntax");
	}
	public InvalidSyntaxException(String message) {
		super(message);
	}
}

class EvaluationException extends RuntimeException {
	private static final long serialVersionUID = -6510306614842571235L;
	public EvaluationException() {
		super("Evaluation error");
	}
	public EvaluationException(String message) {
		super(message);
	}
}
