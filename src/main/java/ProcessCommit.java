import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class ProcessCommit {

    public static void main(String[] args) throws IOException {
        String projectsDir = "/home/yao/tmp"; // 所有项目处理结果的目录，其中的每个文件夹都是一个项目;
        String gitRepoPath = "/home/yao/gitRepo"; // 每个项目的仓库所在地;
        // 1. 提取commit 和bug-fixing commit,及对应的id
        // 每个项目的目录所在的目录; 自定义
        calCommitNum(projectsDir);

        // 2. 提取所有bug-fixing commit的diff结果到文件中;
        extractAllDiffOfBugFixingCommitToFile(projectsDir, gitRepoPath);

//         3. 从diff文件提取修改行;
        CalAllChangedLinesToFile(projectsDir);

//         4. saveAllVersionFiles
        saveAllBugFixingCommitDir(projectsDir, gitRepoPath);
    }

    // 功能： 1. 计算 'git log commit1..commit2'产生的commit信息文件中所有的commit数量
    // 2. 计算其中bug-fixing commit的数量
    // 3. 提取出bug-fixing 的所有commit id;
    public static void calCommitNum(String projectsDir) throws IOException {
        // 用于bug-fixing commit的关键字匹配
        String[] keyWords = new String[]{"bug", "fix", "wrong", "error", "fail", "problem", "patch"};

        File[] Dir = new File(projectsDir).listFiles(); // 项目目录所在目录;
        assert Dir != null;
        for (File project : Dir) { // 每次取一个项目目录
            String name = project.getName(); // 项目名

            // 找到项目文件夹中的目标log文件;
            File target = null;
            for (File file : Objects.requireNonNull(project.listFiles())) {
                if (file.getName().contains("_log.txt")) {
                    target = file;
                    break;
                }
            }

            // 把所有的bug-fixing ID都写入以commitIds结尾的文件中;
            BufferedWriter bW = new BufferedWriter(new FileWriter(project.getAbsolutePath() + File.separator + name + "_commitIds.txt"));
            int k = 0;

            // 开始读取文件，看文件中有多少commit和bug-fixing commit
            int totalCommit = 0, bugCommit = 0;
            assert target != null;
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
            // 打印结果出来;
            System.out.println("commit总数: " + totalCommit + ", bugfix数量: " + bugCommit + "    " + name);
        }
    }

    // 检查commit的格式是否是 commit + 空格 + 40位hash值
    public static boolean checkIfIsCommit(String str) {
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
        assert projects != null;
        for (File project : projects) { // 针对每一个项目，把所有bug-fixing commit的diff结果存到不同的文件;
            String name = project.getName();
            // 找到项目文件夹中的以_commitIds结尾的文件;
            File target = null;
            for (File file : Objects.requireNonNull(project.listFiles())) {
                if (file.getName().contains("_commitIds")) {
                    target = file;
                    break;
                }
            }

            // 读取每个commitId，然后对这个commit使用 "git diff commitId commitId^"
            // 求得这个修改行的信息;
            assert target != null;
            BufferedReader bR = new BufferedReader(new FileReader(target));
            String line;
            while ((line = bR.readLine()) != null) {
                // 拿到commit;
                String commitId = line.split(" ")[0];
                int index = Integer.parseInt(line.split(" ")[1]); // 第几个bug-fixing

                // 开始执行命令行命令，将每个bug-fixing commit的 diff内容求出，并得到一个bug_fixing_commit_index.txt文件;：
                List<String> command = new ArrayList<>(); // 执行的命令;

                command.add("zsh");
                command.add("-c");
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
            BufferedWriter writer = new BufferedWriter(new FileWriter(target.getParent() + File.separator + "bug_fixing_commit_" + index + ".txt"));
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


    // 功能3. 从diff文件中提取修改行到目标文件中;
    public static void CalAllChangedLinesToFile(String projectDir) throws IOException {
        File[] projects = new File(projectDir).listFiles();

        // 开始读取每个项目的bug_fixing_commit文件，对每一个diff hunk， 求出它的文件所在位置，求出它的修改的行号;
        assert projects != null;
        for (File project : projects) { // 针对每一个项目，提取每个bug-fixing的修改行，存到对应的文件中;
            for (File file : Objects.requireNonNull(project.listFiles())) {
                String name = file.getName();
                if (!name.startsWith("bug_fixing_commit")) continue; // 只考虑每一个buf-fixing文件;
                int index = Integer.parseInt(name.substring(name.lastIndexOf("_") + 1, name.lastIndexOf(".txt"))); // 当前commit的序号

                BufferedReader reader = new BufferedReader(new FileReader(file)); // 读取bug-fixing文件;
                // 目标结果文件，保存新增行的结果;
                BufferedWriter addWriter = new BufferedWriter(new FileWriter(project.getAbsolutePath() + File.separator + "ChangedLines_commit_Add_" + index + ".txt"));
                // 目标结果文件，保存删除行的结果;
                BufferedWriter minusWriter = new BufferedWriter(new FileWriter(project.getAbsolutePath() + File.separator + "ChangedLines_commit_Minus_" + index + ".txt"));

                String line = reader.readLine();
                while (line != null) { // 读取文件的每一行;
                    // 处理每一个diff，一个diff就是一个文件内的变化，因此保存文件的路径，以及这个文件中发生修改的这些行号。
                    if (line.startsWith("diff --git")) {
                        String[] str = line.split(" ");
                        String minusFilePath = project.getName() + "/" + str[2].substring(str[2].indexOf("/") + 1); // 被比较文件的路径;
                        String addFilePath = project.getName() + "/" + str[3].substring(str[3].indexOf("/") + 1); // 比较文件的路径;
                        minusWriter.write("***** " + minusFilePath + " *****\n"); //每一diff都是一个文件内的变化;
                        addWriter.write("***** " + addFilePath + " *****\n"); // 每一个diff都是一个文件内的变化;
                        // 保存发生修改的行号，是离散的行号;一个diff都是一个文件内的修改行号;
                        StringBuilder msb = new StringBuilder(); // 被比较的 修改行
                        StringBuilder asb = new StringBuilder(); // 比较的 修改行

                        line = reader.readLine(); // 读到diff的第二行
                        // 只考虑同时修改，仅删除，仅新加的情况，像什么文件更名，文件的权限更改的请求，都不考虑。
                        if (line.startsWith("index") || line.startsWith("deleted") || line.startsWith("new")) {
                            // 提取所有的hunk块
                            while (true) {
                                while (line != null && !line.startsWith("diff --git") && !line.startsWith("@@"))
                                    line = reader.readLine();
                                if (line == null || line.startsWith("diff --git")) break; // 提前结束
                                // 找到了一对hunk块;
                                // 提取被比较文件的起始行，和比较文件的起始行;
                                int[] startsLine = analyzeHunk(line); // startLines[0]，是被比较文件的起始行，[1]是比较文件的起始行;
                                int mK = startsLine[0] - 1, aK = startsLine[1] - 1;
                                line = reader.readLine(); // 开始读取hunk的下每一个内容
                                while (line != null && !line.startsWith("diff --git") && !line.startsWith("@@")) {
                                    if (line.startsWith(" ")) {
                                        mK++;
                                        aK++;
                                    } else if (line.startsWith("-")) {
                                        mK++;
                                        msb.append(mK).append(" ");
                                    } else if (line.startsWith("+")) {
                                        aK++;
                                        asb.append(aK).append(" ");
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

                        }
                        minusWriter.newLine();
                        minusWriter.newLine();
                        addWriter.newLine();
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

    // 提取每一个的hunk "@@...@@"的起始行;
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


    // 功能4： 到每个项目的git仓库中，切换到每一个bug-fixing commit版本以及commit^版本， 并将当前目录的所有文件都复制一份到
    // 目标目录下即可; 生成的目录名称格式为 name-Add-index， 名字-比较版本(记为Add)-第几个bug-fixing commit的序号
    // 比较版本（bug-fixing commit）记为Add， 被比较版本(bug-fixing commit^)记为Minus
    public static void saveAllBugFixingCommitDir(String projectsDir, String gitRepoPath) throws IOException {
        File projectDir = new File(projectsDir); // 项目所在的目录;
        for (File project : Objects.requireNonNull(projectDir.listFiles())) { // 遍历每一个项目的目录;
            String name = project.getName(); // 项目的名称;

            // 找到保存所有commitId的文件;
            File commitIdsFile = null;
            for (File file : Objects.requireNonNull(project.listFiles())) {
                if (!file.getName().endsWith("commitIds.txt")) continue;
                commitIdsFile = file;
                break;
            }

            // 开始读取每一个commitId
            assert commitIdsFile != null;
            BufferedReader reader = new BufferedReader(new FileReader(commitIdsFile));
            String line;
            while ((line = reader.readLine()) != null) { // 读取每一个;
                String[] arr = line.split(" ");
                String commit = arr[0]; // commitId的值
                int index = Integer.parseInt(arr[1]); // 第几个id;

                // 拿到Id, 开始去git仓库里面切换版本，并保存版本文件;
                // 第一次执行命令，拿到commitId的版本文件；
                String[] command1 = {"/bin/bash", "-c", "cd " + gitRepoPath + "/" + name + ";" + "git switch --detach " + commit + ";" + "cp -r ../" + name + " " + projectsDir + "/" + name + ";" + "mv " + projectsDir + "/" + name + "/" + name + " " + projectsDir + "/" + name + "/" + name + "-Add-" + index};
                Utilities.implCommand(command1);
                // 第二次执行命令，拿到commitId^的版本文件;
                String[] command2 = {"/bin/bash", "-c", "cd " + gitRepoPath + "/" + name + ";" + "git switch --detach " + commit + "^;" + "cp -r ../" + name + " " + projectsDir + "/" + name + ";" + "mv " + projectsDir + "/" + name + "/" + name + " " + projectsDir + "/" + name + "/" + name + "-Minus-" + index};
                Utilities.implCommand(command2);
            }
            reader.close();
        }
    }

    // 功能5： 对每个commit版本，都执行一次Nicad克隆，得到Nicad克隆的结果。
    // 这个功能在linux中使用脚本功能能够实现。
}
