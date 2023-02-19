import org.apache.logging.log4j.message.StructuredDataId;

import java.io.*;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class TestServeralFunc {

    public static void main(String[] args) {
        List<String> command = new ArrayList<>(); // 执行的命令;
        command.add("pwsh");
        command.add("-Command");
        command.add("cd " + "D:\\GitRepository\\autokeras\n");
        command.add("git switch --detach d22b71d190f68b95b7171f1fddf6ff4692078a0d\n");
        command.add("cp -r ..\\autokeras D:\\tmp\\autokeras");
        implLongCommand(command); // 执行命令，将每个commitId对应的diff结果存到文件中;
    }


    public static void implLongCommand(List<String> command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
//            BufferedWriter writer = new BufferedWriter(new FileWriter(target.getParent() + File.separator + "bug_fixing_commit_" + index + ".txt"));
//            String line;
//            while ((line = reader.readLine()) != null) {
//                writer.write(line + "\n");
//            }

            process.waitFor();
            reader.close();
//            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
