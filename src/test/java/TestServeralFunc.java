import java.io.BufferedReader;
import java.io.InputStreamReader;

public class TestServeralFunc {
    public static void main(String[] args) {
        String[] command = {"pwsh", "-Command", "cd ~; pwd; ls"};
        ExeLongCommand(command);
    }
    // 传入一个长命令，使用pwsh执行，并
    public static void ExeLongCommand(String[] command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
