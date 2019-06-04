import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.ListIterator;

import static cTools.KernelWrapper.*;

public class head {
    public static void main(String[] args) {
        int lines = 10;
        int bytes = -1;
        ArrayList<String> arguments = new ArrayList<>(Arrays.asList(args));
        ListIterator<String> iterator = arguments.listIterator();
        while (iterator.hasNext()) {
            String arg = iterator.next();
            switch (arg) {
                case "-c":
                    if (iterator.hasNext()) {
                        bytes = Integer.parseInt(iterator.next());
                    } else {
                        System.err.println("option -c: missing parameter");
                    }
                    break;
                case "-n":
                    if (iterator.hasNext()) {
                        lines = Integer.parseInt(iterator.next());
                    } else {
                        System.err.println("option -n: missing parameter");
                    }
                    break;
                case "--help":
                    execv("/bin/man", new String[]{"/bin/man", "head"});
                    break;
                default:
                    try {
                        System.out.println(bytes == -1 ? readLines(arg, lines) : readBytes(arg, bytes));
                    } catch (IOException e) {
                        System.out.println("File"+arg +"does not exist");
                    }
            }
        }
    }

    public static String readLines(String file, int lines) throws IOException {
        String out = "";
        int fd = file.equals("-") ? STDIN_FILENO : open(file, O_RDONLY);
        if (fd == -1) throw new IOException();

        byte[] buf = new byte[256];
        while (lines > 0) {
            int bytes = read(fd, buf, 256);
            if (bytes <= 0) return out;
            for (int i = 0; i < bytes; i++) {
                char tmp = (char) buf[i];
                if (tmp == '\n') lines--;
                if ((lines > 0)) {
                    out += tmp;
                } else {
                    return out;
                }
            }
        }
        return out;
    }

    public static String readBytes(String file, int bytes) throws IOException {
        String out = "";
        byte[] buf = new byte[bytes];
        int fd = file.equals("-") ? STDIN_FILENO : open(file, O_RDONLY);
        if (fd == -1) throw new IOException();
        int c = read(fd, buf, bytes);
        if (c <= 0) return out;
        for (int i = 0; i < c; i++) {
            out += (char) buf[i];
        }
        return out;
    }
}
