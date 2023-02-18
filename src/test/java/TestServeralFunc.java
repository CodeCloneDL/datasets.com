import org.apache.logging.log4j.message.StructuredDataId;

import java.io.*;
import java.util.LinkedList;

public class TestServeralFunc {
    static ProcessBuilder pB = new ProcessBuilder("pwsh", "-NoExit", "-NoLogo");
    static Process process;
    static OutputStream stdin;
    static InputStream stdout;
    public static void main(String[] args) throws IOException {
        pB.directory(new File("D:\\tmp\\"));
        pB.redirectErrorStream(true);

        LinkedList<String> command = new LinkedList<>();
        ExeLongCommand("cd D:\\tmp");
        ExeLongCommand("pwd");
        ExeLongCommand("cd D:\\Clash");
        ExeLongCommand("pwd");
    }
    // 传入一个长命令，使用pwsh执行，并
    public static void ExeLongCommand(String command) {
        try {
            process = pB.start();
            stdin = process.getOutputStream();
            stdout = process.getInputStream();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stdin));
            writer.write(command);
            writer.flush();;
            writer.close();
            BufferedReader reader = new BufferedReader(new InputStreamReader(stdout));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();
            reader.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
