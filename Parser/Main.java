import java.util.ArrayList;

public final class Main {

	public static void main(String[] args) {
		String str = "x = 1; y = 2; z = ---(x+y)*(x+-y);";
		Parser ps = new Parser(str);
		Program pg = ps.parse();
		System.out.println(pg.toString());
	}

}
