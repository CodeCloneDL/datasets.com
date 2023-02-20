import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class PowerShellTest {

    public static void main(String[] args) throws Exception {
        ProcessBuilder builder = new ProcessBuilder("powershell.exe", "-NoLogo", "-NoProfile");
        builder.redirectErrorStream(true);
        Process process = builder.start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

        // 第一条命令
        process.getOutputStream().write("Get-ChildItem\n".getBytes());
        process.getOutputStream().flush();
        String line;
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        // 第二条命令
        process.getOutputStream().write("Get-Process\n".getBytes());
        process.getOutputStream().flush();
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        // 第三条命令
        process.getOutputStream().write("Get-Service\n".getBytes());
        process.getOutputStream().flush();
        while ((line = reader.readLine()) != null) {
            System.out.println(line);
        }

        process.getOutputStream().write("exit\n".getBytes());
        process.getOutputStream().flush();
        process.waitFor();
    }

}
