import java.util.ArrayList;

class Tokenizer {
	private String text;
	private Token allTokens[];
	private Token tokens[];

	Tokenizer(String text) {
		this.text = text;
	}

	Token[] getTokens() throws InvalidTokenException {
		if (null != tokens) 
			return tokens;
		getAllTokens();
		ArrayList<Token> tokensList = new ArrayList<Token>();
		for (Token t : allTokens) {
			if (t.tag != Tag.SEPARATOR) 
				tokensList.add(t);
		}
		tokens = tokensList.toArray(new Token[0]);
		return tokens;
	}

	Token[] getAllTokens() throws InvalidTokenException {
		if (null != allTokens)
			return allTokens;
		tokenize();
		return allTokens;
	}

	private void tokenize() throws InvalidTokenException {
		ArrayList<Token> allTokensList = new ArrayList<Token>();
		int len = text.length();
		int start = 0;
		while (start < len) {
			int end = len;
			while (end > start) {
				String value = text.substring(start, end);
				Token token;
				if (Separator.check(value)) {
					token = new Separator(value);
				} else if (Literal.check(value)) {
					token = new Literal(value);
				} else if (Identifier.check(value)) {
					token = new Identifier(value);
				} else if (Operator.check(value)) {
					token = new Operator(value, 0);
				} else {
					end -= 1;
					continue;
				}
				allTokensList.add(token);
				break;
			}
			if (!(end > start)) {
				throw new InvalidTokenException();
			}
			start = end;
		}
		allTokens = allTokensList.toArray(new Token[0]);
	}
}

enum Tag {
	SEPARATOR, 
	LITERAL, 
	IDENTIFIER, 
	OPERATOR, 
	VALUE, 
}

abstract class Token {
	Tag tag;
	abstract String getValue();
	@Override
	public String toString() {
		return tag.name() + ": \"" + getValue() + "\"";
	}
}

class Separator extends Token {
	private static char sepChars[] = {
			' ', '\t', '\n', '\u000b', '\f', '\r', 
	};
	private String value;
	Separator(String value) throws InvalidTokenException {
		tag = Tag.SEPARATOR;
		if (!check(value)) 
			throw new InvalidTokenException();
		this.value = value;
	}
	static boolean check(String target) {
		int lenT = target.length();
		for (int i = 0; i < lenT; i++) {
			char c = target.charAt(i);
			boolean found = false;
			for (int j = 0; j < sepChars.length; j++) {
				if (c == sepChars[j]) {
					found = true;
					break;
				}
			}
			if (!found) 
				return false;
		}
		return true;
	}
	@Override
	String getValue() {
		return value;
	}
}

class Literal extends Token {
	private static char nonZeroDigits[] = {
			'1', '2', '3', '4', '5', '6', '7', '8', '9',
	};
	private static char digits[] = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	};
	private String value;
	Literal(String value) throws InvalidTokenException {
		tag = Tag.LITERAL;
		if (!check(value)) 
			throw new InvalidTokenException();
		this.value = value;
	}
	static boolean check(String target) {
		if (target.equals("0")) 
			return true;
		char initChar = target.charAt(0);
		boolean found = false;
		for (int i = 0; i < nonZeroDigits.length; i++) {
			if (initChar == nonZeroDigits[i]) {
				found = true;
				break;
			}
		}
		if (!found) 
			return false;
		for (int i = 1; i < target.length(); i++) {
			char c = target.charAt(i);
			found = false;
			for (int j = 0; j < digits.length; j++) {
				if (c == digits[j]) {
					found = true;
					break;
				}
			}
			if (!found) 
				return false;
		}
		return true;
	}
	@Override
	String getValue() {
		return value;
	}
}

class Identifier extends Token {
	private static char letters[] = {
			'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 
			'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', 
			'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 
			'N', 'O', 'P', 'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X', 'Y', 'Z', '_', 
	};
	private static char digits[] = {
			'0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
	};
	private String value;
	Identifier(String value) throws InvalidTokenException {
		tag = Tag.IDENTIFIER;
		if (!check(value)) 
			throw new InvalidTokenException();
		this.value = value;
	}
	static boolean check(String target) {
		char initChar = target.charAt(0);
		boolean found = false;
		for (int i = 0; i < letters.length; i++) {
			if (initChar == letters[i]) {
				found = true;
				break;
			}
		}
		if (!found) 
			return false;
		for (int i = 1; i < target.length(); i++) {
			char c = target.charAt(i);
			found = false;
			for (int j = 0; j < letters.length; j++) {
				if (c == letters[j]) {
					found = true;
					break;
				}
			}
			if (found) 
				continue;
			for (int j = 0; j < digits.length; j++) {
				if (c == digits[j]) {
					found = true;
					break;
				}
			}
			if (!found) 
				return false;
		}
		return true;
	}
	@Override
	String getValue() {
		return value;
	}
}

class Operator extends Token {
	private static char operators[] = {
			'+', '-', '*', '(', ')', '=', ';'
	};
	private String value;
	private int opNum;
	Operator(String value, int opNum) throws InvalidTokenException {
		tag = Tag.OPERATOR;
		if (!check(value)) 
			throw new InvalidTokenException();
		this.value = value;
		this.opNum = opNum;
	}
	static boolean check(String target) {
		if (target.length() != 1) 
			return false;
		char c = target.charAt(0);
		for (int i = 0; i < operators.length; i++) {
			if (c == operators[i]) 
				return true;
		}
		return false;
	}
	@Override
	String getValue() {
		return value;
	}
	int getOpNum() {
		return opNum;
	}
}

class ValueToken extends Token {
	private int value;
	ValueToken(int value) {
		tag = Tag.VALUE;
		this.value = value;
	}
	@Override
	String getValue() {
		return Integer.toString(value);
	}
}

class InvalidTokenException extends RuntimeException {
	private static final long serialVersionUID = 8239852103589479869L;
	public InvalidTokenException() {
		super("Invalid Token");
	}
}
