package util;

import java.util.Scanner;

public final class InputUtil {
    private static final Scanner SCANNER = new Scanner(System.in);
    private InputUtil() { }

    public static String readLine(String prompt) {
        System.out.print(prompt);
        return SCANNER.nextLine().trim();
    }

    public static int readInt(String prompt) {
        while (true) {
            try { return Integer.parseInt(readLine(prompt)); }
            catch (NumberFormatException exception) { System.out.println("Enter a whole number."); }
        }
    }

    public static double readDouble(String prompt) {
        while (true) {
            try { return Double.parseDouble(readLine(prompt)); }
            catch (NumberFormatException exception) { System.out.println("Enter a valid number."); }
        }
    }

    public static void close() { SCANNER.close(); }
}
