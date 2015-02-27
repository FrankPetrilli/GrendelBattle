public class Debug {
	public static void print(boolean debug, String input) {
		if (debug) {
			System.out.println("[debug] " + input);
		}
	}
}
