import java.io.BufferedReader;
import java.io.InputStreamReader;

public class BashCommands {
    public static void main(String[] args) {
        try {
            // Create a ProcessBuilder for "bash"
            ProcessBuilder builder = new ProcessBuilder();
            // Add the commands to execute in the bash shell
            builder.command("/bin/bash", "-c", "echo 'Hello World'; pwd; ls -l; pwd;pwd;pwd");
            // Start the process
            Process process = builder.start();
            // Read the output of the process
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
            // Wait for the process to complete and check the exit value
            int exitCode = process.waitFor();
            if (exitCode == 0) {
                System.out.println("Commands executed successfully.");
            } else {
                System.err.println("Error executing commands. Exit code: " + exitCode);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
