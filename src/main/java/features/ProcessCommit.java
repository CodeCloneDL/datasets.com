package features;

import java.io.*;
import java.security.AlgorithmConstraints;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class ProcessCommit {
    public static void main(String[] args) throws IOException {
        // 1. 提取commit 和bug-fixing commit,及对应的id
        String projectsDir = "D:\\tmp"; // 每个项目的目录所在的目录; 自定义
//        ProcessCommit.calCommitNum(projectsDir);

        // 2. 提取所有bug-fixing commit的diff结果到文件中;
        String gitRepoPath = "D:\\GitRepository";
//        extractAllDiffOfBugFixingCommitToFile(projectsDir, gitRepoPath);

        // 3. 从diff文件提取修改行;
        CalAllChangedLinesToFile(projectsDir, gitRepoPath);
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
            bR.close();
            ;
            // 打印结果出来;
            System.out.println("commit总数: " + totalCommit + ", bugfix数量: " + bugCommit + "    " + name);
        }
    }
    // 检查commit的格式是否是 commit + 空格 + 40位hash值
    public static boolean checkIfIsCommit(String str) throws IOException {
        String regex = "commit\\s[0-9a-f]{40}";
        Pattern pattern = Pattern.compile(regex);
        return pattern.matcher(str).find() && str.startsWith("commit");
    }
    // 检查关键字是否匹配;
    public static boolean checkIfKeyWordsMatch(String str, String[] keyWords) {
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




    // 功能： 3. 从diff文件中提取修改行到目标文件中;
    public static void CalAllChangedLinesToFile(String projectDir, String gitRepoPath) throws IOException {
        File[] projects = new File(projectDir).listFiles();

        // 开始读取每个项目的bug_fixing_commit文件，对每一个diff hunk， 求出它的文件所在位置，求出它的修改的行号;
        for (File project : projects) { // 针对每一个项目，提取每个bug-fixing的修改行，存到对应的文件中;
            for (File file : project.listFiles()) {
                String name = file.getName();
                if (!name.startsWith("bug_fixing_commit")) continue; // 只考虑每一个buf-fixing文件;
                int index = Integer.parseInt(name.substring(name.lastIndexOf("_") + 1, name.lastIndexOf(".txt"))); // 当前commit的序号

                BufferedReader reader = new BufferedReader(new FileReader(file)); // 读取bug-fixing文件;
                // 目标结果文件，保存新增行的结果;
                BufferedWriter addWriter = new BufferedWriter(new FileWriter(new File(project.getAbsolutePath() + File.separator + "ChangedLines_commit_Add_" + index + ".txt")));
                // 目标结果文件，保存删除行的结果;
                BufferedWriter minusWriter = new BufferedWriter(new FileWriter(new File(project.getAbsolutePath() + File.separator + "ChangedLines_commit_Minus_" + index + ".txt")));

                String line = reader.readLine();
                while(line != null) { // 读取文件的每一行;
                    // 处理每一个diff，一个diff就是一个文件内的变化，因此保存文件的路径，以及这个文件中发生修改的这些行号。
                    if (line.startsWith("diff --git")) {
                        String[] str = line.split(" ");
                        String minusFilePath = project.getName() + "/" + str[2].substring(str[2].indexOf("/") + 1, str[2].length()); // 被比较文件的路径;
                        String addFilePath = project.getName() + "/" + str[3].substring(str[3].indexOf("/") + 1, str[3].length()); // 比较文件的路径;
                        minusWriter.write("___" + minusFilePath + "___\n"); //每一diff都是一个文件内的变化;
                        addWriter.write("___" + addFilePath + "___\n"); // 每一个diff都是一个文件内的变化;
                        // 保存发生修改的行号，是离散的行号
                        StringBuilder msb = new StringBuilder(); // 被比较的 修改行
                        StringBuilder asb = new StringBuilder(); // 比较的 修改行

                        line = reader.readLine();
                        if (line.startsWith("index")) { // 情况一：同时删除和增加同一文件中的行的diff的模式，当前行一定是index开头的;
                            // 提取所有的hunk块
                            while (line != null) {
                                while (line != null && !line.startsWith("diff --git") && !line.startsWith("@@")) line = reader.readLine();
                                if (line == null || line.startsWith("diff --git")) break;
                                // 找到了一对hunk块;
                                // 提取被比较文件的起始行，和比较文件的起始行;
                                int[] startsLine = analyzeHunk(line); // startLines[0]，是被比较文件的起始行，[1]是比较文件的起始行;
                                int mK = startsLine[0] - 1, aK = startsLine[1] - 1;
                                line = reader.readLine();
                                while (line != null && !line.startsWith("diff --git") && !line.startsWith("@@")) {
                                    if (line.startsWith(" ")) {
                                        mK++;
                                        aK++;
                                    } else if (line.startsWith("-")) {
                                        mK++;
                                        msb.append(mK + " ");
                                    } else if (line.startsWith("+")) {
                                        aK++;
                                        asb.append(aK + " ");
                                    }
                                    line = reader.readLine(); // 一直遍历当前hunk块;
                                }
                                if (line == null || line.startsWith("diff --git")) break;
                            }
                            // 删除最后一个空格
                            if (msb.length() != 0) msb.deleteCharAt(msb.length() - 1);
                            minusWriter.write(msb.toString());
                            minusWriter.newLine();
                            minusWriter.newLine();
                            minusWriter.newLine();
                            // 删除最后一个空格
                            if (asb.length() != 0) asb.deleteCharAt(asb.length() - 1);
                            addWriter.write(asb.toString());
                            addWriter.newLine();
                            addWriter.newLine();
                            addWriter.newLine();

                        } else if (line.startsWith("deleted")){ // 情况二：删除了一个文件

                        } else if (line.startsWith("new")) { // 情况三：增加了一个文件;

                        }
                        minusWriter.newLine();
                        addWriter.newLine();
                    } else {
                        line = reader.readLine();
                    }
                }
                // 一个bug-fixing commit的文件读取结束，关闭所有流;
                addWriter.close();
                minusWriter.close();
                reader.close();
            }
        }
    }
    // 提取每一个的hunk "@@...@@@"的起始行;
    public static int[] analyzeHunk(String hunk) {
        // 先求被比较的起始行号;
        int i = hunk.indexOf("-");
        int j;
        for (j = i + 1; j < hunk.length(); ++j) {
            if (!Character.isDigit(hunk.charAt(j))) break;
        }
        int minusK = Integer.parseInt(hunk.substring(i + 1, j)); // 求得被比较的起始行号;

        i = hunk.indexOf("+");
        for (j = i + 1; j < hunk.length(); ++j) {
            if (!Character.isDigit(hunk.charAt(j))) break;
        }
        int addK = Integer.parseInt(hunk.substring(i + 1, j));
        return new int[]{minusK, addK};
    }

}
