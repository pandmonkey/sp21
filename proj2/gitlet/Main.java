package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }
        String firstArg = args[0];
        try {
        switch (firstArg) {
            case "init":
                Repository.init();
                break;
            case "add":
                if (args.length != 2) {
                    System.out.println("Please enter a filename");
                    System.exit(0);
                }
                Repository.add(args[1]);
                break;
            case "commit":
                if (args.length != 2) {
                    System.out.println("Please enter a commit message");
                    System.exit(0);
                }
                Repository.commit(args[1]);
                break;
            case "rm":
                if (args.length != 2) {
                    System.out.println("Please enter a filename");
                    System.exit(0);
                }
                Repository.rm(args[1]);
                break;
            case "log":
                Repository.log();
                break;
            case "global-log":
                Repository.globalLog();
                break;
            case "find":
                if (args.length != 2) {
                    System.out.println("Please enter a commit message");
                    System.exit(0);
                }
                Repository.find(args[1]);
                break;
            case "status":
                Repository.status();
            case "checkout":
                switch (args.length) {
                    case 3:
                        Repository.checkoutFile(args[2]);
                        break;
                    case 4:
                        Repository.checkout(args[1], args[3]);
                        break;
                    case 2:
                        Repository.checkoutBranch(args[1]);
                        break;
                }
                break;
            case "branch":
                if (args.length != 2) {
                    System.out.println("Please enter a branch Name");
                    System.exit(0);
                }
                Repository.branch(args[1]);
                break;
            case "rm-branch":
                if (args.length != 2) {
                    System.out.println("Please enter a branch Name");
                    System.exit(0);
                }
                Repository.rmBranch(args[1]);
                break;
            case "reset":
                if (args.length != 2) {
                    System.out.println("Please enter a commit id");
                    System.exit(0);
                }
                Repository.reset(args[1]);
                break;
            case "merge":
                if (args.length != 2) {
                    System.out.println("Please enter a branch Name");
                    System.exit(0);
                }
                Repository.merge(args[1]);
                break;
        }}
        catch (IOException e) {
            System.exit(0);
        }

    }
}
