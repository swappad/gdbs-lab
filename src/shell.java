import javax.print.DocFlavor;
import java.io.*;
import java.sql.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.ListIterator;

import static cTools.KernelWrapper.*;

class shell {
    public static void main(String[] args) throws IOException {

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        while (true) {
            System.out.print("shell $");
            String rawIn = bufferedReader.readLine();
            rawIn = rawIn.replace(">", " > ").replace("<", " < ");

            String[] input = rawIn.split("\\s+");

            //Wildcards
            ArrayList<String> arguments = new ArrayList<String>();
            for (String arg : input) {
                arguments.addAll(expand(arg));
            }

            switch (arguments.get(0)) {
                case "exit":
                    exit(0);
                default:
                    if (!(new File(arguments.get(0))).canExecute()) {
                        System.err.println(arguments.get(0) + " is not an executable file!");
                    } else {

                        int pid = fork();
                        if (pid == 0) {
                            execPipe(arguments, -1);
                            //exit(0);
                        }
                        int[] stat = new int[1];
                        waitpid(pid, stat, 0);
                    }
            }
        }
    }

    private static ArrayList<String> expand(String arg) {
        ArrayList arguments = new ArrayList();

        /*if(!arg.contains("*") && !arg.contains("?")) {
            arguments.add(arg);
            return arguments;
        }*/

        String argreg = arg.replace("*", ".*").replace("?", ".");
        String[] files = new File(".").list((file, s) -> s.matches(argreg));

        if (!(files.length == 0)) {
            arguments.addAll(Arrays.asList(files));
        } else {
            arguments.add(arg);
        }

        return arguments;
    }

    private static void moveIO(ArrayList<String> set) {
        ListIterator<String> iterator = set.listIterator();
        while (iterator.hasNext()) {
            int io;
            int options;
            switch (iterator.next()) {
                case ">":
                    io = STDOUT_FILENO;
                    options = O_WRONLY | O_CREAT;
                    break;
                case "<":
                    io = STDIN_FILENO;
                    options = O_RDONLY;
                    break;
                default:
                    continue;
            }
            iterator.remove();
            String file = iterator.next();
            close(io);
            int fd = open(file, options);
            if (fd == -1) {
                System.err.println("Egal was");
            }
            iterator.remove();
        }


    }

    private static void execPipe(ArrayList<String> args, int infd) {
        // extract first programm with parameters
        ListIterator<String> iterator = args.listIterator();
        ArrayList<String> subargs = new ArrayList<String>();
        while (iterator.hasNext()) {
            String current = iterator.next();
            if (current.equals("|")) {
                iterator.remove();
                break;
            } else {
                subargs.add(current);
                iterator.remove();
            }
        }

        moveIO(subargs);

        if (!iterator.hasNext() || !(new File(args.get(0))).canExecute()) {
            if (infd != -1 && dup2(infd, STDIN_FILENO) == -1) {
                System.err.println("pipe does not work1");
            }

            checkandexec(subargs);

            exit(0);
        } else {
            // fork
            int[] fildes = new int[2];
            if (pipe(fildes) == -1) {
                System.err.println("pipe is broken");
            }

            int pid = fork();

            switch (pid) {
                case -1:
                    System.err.println("fork failed");
                    exit(0);
                case 0:
                    close(fildes[0]);
                    if (infd != -1 && dup2(infd, STDIN_FILENO) == -1) {
                        System.err.println("pipe does not work1");
                    }
                    if (dup2(fildes[1], STDOUT_FILENO) == -1) {
                        System.err.println("pipe does not work1");
                    }

                    checkandexec(subargs);

                    exit(0);
                default:
                    close(fildes[1]);
                    if (infd != -1) {
                        close(infd);
                    }
                    execPipe(args, fildes[0]);
            }

        }

    }

    private static void checkandexec(ArrayList<String> args) {
        if (execv(args.get(0), args.toArray(new String[args.size()])) == -1) {
            System.err.println("execution failed!");
        }
    }


}

