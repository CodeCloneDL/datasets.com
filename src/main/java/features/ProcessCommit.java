package features;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProcessCommit {
    public static void main(String[] args) throws IOException {
        // 1. 提取commit 和bug-fixing commit,及对应的id
        String projectsDir = "D:\\tmp"; // 每个项目的目录所在的目录; 自定义
        ProcessCommit.calCommitNum(projectsDir);

        // 2. 提取所有bug-fixing commit的diff结果到文件中;
        String gitRepoPath = "D:\\GitRepository";
        extractAllDiffOfBugFixingCommitToFile(projectsDir, gitRepoPath);

        // 3. 从diff文件提取修改行;
    }

    // 功能： 1. 计算 'git log commit1..commit2'产生的commit信息文件中所有的commit数量
    // 2. 计算其中bug-fixing commit的数量
    // 3. 提取出bug-fixing 的所有commit id;
    public static void calCommitNum(String projectsDir) throws IOException {
        // 用于bug-fixing commit的关键字匹配
        String[] keyWords = new String[]{"bug", "fix", "wrong", "error", "fail", "problem", "patch"};

        File[] Dir = new File(projectsDir).listFiles(); // 项目目录所在目录;
        for (File project : Dir) { // 每次取一个项目目录
            String name = project.getName(); // 项目名

            // 找到项目文件夹中的目标log文件;
            File target = null;
            for (File file : project.listFiles()) {
                if (file.getName().contains("_log.txt")) {
                    target = file;
                    break;
                }
            }

            // 把所有的bug-fixing ID都写入以commitIds结尾的文件中;
            BufferedWriter bW = new BufferedWriter(new FileWriter(new File(project.getAbsolutePath() + File.separator + name + "_commitIds.txt")));
            int k = 0;

            // 开始读取文件，看文件中有多少commit和bug-fixing commit
            int totalCommit = 0, bugCommit = 0;
            BufferedReader bR = new BufferedReader(new FileReader(target)); // 读取这个文件;
            String temp = bR.readLine();
            while (temp != null) {
                // 找到一个commit的位置
                while (temp != null && !checkIfIsCommit(temp)) temp = bR.readLine();
                if (temp == null) break; // 到达末尾;

                String commitId = temp; // 保存commit的信息
                // 再次确认是否是需要的commit
                String Author = bR.readLine();
                if (!Author.startsWith("Author")) continue;
                String Date = bR.readLine();
                if (!Date.startsWith("Date")) continue;

                // 找到了需要的commit hunk
                ++totalCommit; // 找到了一个commit;
                temp = bR.readLine(); // 开始读取内容
                boolean bugFlag = false; // 只需要找一个关键词匹配到了就行。
                while (temp != null && !checkIfIsCommit(temp)) {
                    if (!bugFlag) {
                        temp = temp.toLowerCase();
                        if (checkIfKeyWordsMatch(temp, keyWords)) {
                            bW.write(commitId.split(" ")[1] + " " + k++ + "\n");
                            bugFlag = true;
                            bugCommit++;
                        }
                    }
                    temp = bR.readLine();
                }
            }
            bW.close();
            bR.close();;
            // 打印结果出来;
            System.out.println("commit总数: " + totalCommit +", bugfix数量: " + bugCommit + "    " + name);
        }
    }
    // 检查commit的格式是否是 commit + 空格 + 40位hash值
    public static boolean checkIfIsCommit(String str) throws IOException {
        String regex = "commit\\s[0-9a-f]{40}";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(str).find() && str.startsWith("commit");
    }
    // 检查关键字是否匹配;
    public  static boolean checkIfKeyWordsMatch(String str, String[] keyWords) {
        for (String keyWord : keyWords) {
            if (Pattern.compile(keyWord).matcher(str).find()) return true;
        }
        return false;
    }


    // 功能2： 利用每个项目的所有bug-fixing Id，提取所有的bug_fixing commit的diff结果存到文件中;
    public static void extractAllDiffOfBugFixingCommitToFile(String projectsDir, String gitRepoPath) throws IOException {
        File[] projects = new File(projectsDir).listFiles();
        for (File project : projects) { // 针对每一个项目，把所有bug-fixing commit的diff结果存到不同的文件;
            String name = project.getName();
            // 找到项目文件夹中的以_commitIds结尾的文件;
            File target = null;
            for (File file : project.listFiles()) {
                if (file.getName().contains("_commitIds")) {
                    target = file;
                    break;
                }
            }

            // 读取每个commitId，然后对这个commit使用 "git diff commitId commitId^"
            // 求得这个修改行的信息;
            BufferedReader bR = new BufferedReader(new FileReader(target));
            String line;
            while ((line = bR.readLine()) != null) {
                // 拿到commit;
                String commitId = line.split(" ")[0];
                int index = Integer.parseInt(line.split(" ")[1]); // 第几个bug-fixing

                // 开始执行命令行命令，将每个bug-fixing commmit的 diff内容求出，并得到一个bug_fixing_commit_index.txt文件;：
                List<String> command = new ArrayList<>(); // 执行的命令;
                command.add("pwsh");
                command.add("-Command");
                command.add("cd " + gitRepoPath + File.separator + name + "; git diff " + commitId + " " + commitId + "^");
                implLongCommand(command, index, target); // 执行命令，将每个commitId对应的diff结果存到文件中;
            }
            bR.close();
        }
    }
    // 执行一个长命令，把命令结果输出到target文件所在目录的新文件中，文件的名字格式为 "bug_fixing_commit_" + index;
    public static void implLongCommand(List<String> command, int index, File target) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            Process process = processBuilder.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            BufferedWriter writer = new BufferedWriter(new FileWriter(new File(target.getParent() + File.separator + "bug_fixing_commit_" + index + ".txt")));
            String line;
            while ((line = reader.readLine()) != null) {
                writer.write(line + "\n");
            }

            process.waitFor();
            reader.close();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }




}
