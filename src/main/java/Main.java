import java.util.Scanner;
import util.ConverterUtil;
import util.FileUtil;

public class Main {

  public static void main(String[] args) {
    final String from;
    final String to;
    Scanner in = new Scanner(System.in);

    if (args.length > 1) {
      from = FileUtil.cleanupPath(args[0]);
      to = FileUtil.cleanupPath(args[1]);
    } else {
      System.out.println("Paste the path to the folder containing the exported JSON data:");
      from = FileUtil.cleanupPath(in.nextLine());
      System.out.println("Paste the path to the output folder (we'll create it if it doesn't exist):");
      to = FileUtil.cleanupPath(in.nextLine());
    }
    in.close();
    ConverterUtil.convert(from, to);
  }

}
