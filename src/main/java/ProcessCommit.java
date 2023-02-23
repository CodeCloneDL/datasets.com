import javax.print.attribute.standard.MediaSize;
import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.regex.Pattern;

public class ProcessCommit {

    public static void main(String[] args) throws IOException {
        String projectsDir = "/home/haosun/yao/tmp"; // 所有项目处理结果的目录，其中的每个文件夹都是一个项目;
        String gitRepo = "/home/haosun/yao/gitRepo"; // 每个项目的仓库所在地;
        String Output = "/home/haosun/yao/gitRepo/datasets.com/Output"; // 共变结果文件所在的目录;
        String targetFile = "/home/haosun/yao/target.txt"; // 格式化的文件，里面按空格分割，每一行是项目名 git链接 最新版本号 最远版本号
        String NiCadSystemsDir = "/home/haosun/yao/software/NiCad-6.2/systems"; // Nicad 对项目执行克隆检测的目录;
        // 1. 提取commit 和bug-fixing commit,及对应的id
        // 每个项目的目录所在的目录; 自定义
//        calCommitNum(projectsDir);

        // 2. 提取所有bug-fixing commit的diff结果到文件中;
//        extractAllDiffOfBugFixingCommitToFile(projectsDir, gitRepo);
//
////         3. 从diff文件提取修改行;
//        CalAllChangedLinesToFile(projectsDir);

//         4. saveAllVersionFile

//        saveAllBugFixingCommitDir(projectsDir, gitRepo, NiCadSystemsDir);

         // 6. 提取buggy的共变克隆。
//        extracAllBuggyCochangedClones(projectsDir, gitRepo, Output);

        // 7. 实现一点功能， 实现从格式化target.txt文件中自动提取commit区间的信息：

//        extractLogForProjects(projectsDir, gitRepo, targetFile);

        // 8. 提取每个项目的最新的版本作为base版本，放入到Nicad/systems/下;
            generateLatestProjects(targetFile, gitRepo, NiCadSystemsDir);
    }

    // 功能： 1. 计算 'git log commit1..commit2'产生的commit信息文件中所有的commit数量
    // 2. 计算其中bug-fixing commit的数量
    // 3. 提取出bug-fixing 的所有commit id;
    public static void calCommitNum(String projectsDir) throws IOException {
        // 用于bug-fixing commit的关键字匹配
        String[] keyWords = new String[]{"bug", "fix", "wrong", "error", "fail", "problem", "patch"};

        File[] Dir = new File(projectsDir).listFiles(); // 项目目录所在目录;
        assert Dir != null;
        Arrays.sort(Dir, Comparator.comparing(File::getName));
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

                command.add("bash");
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
    // Nicad/systems目录下即可; 生成的目录名称格式为 name-Add-index， 名字-比较版本(记为Add)-第几个bug-fixing commit的序号
    // 比较版本（bug-fixing commit）记为Add， 被比较版本(bug-fixing commit^)记为Minus
    public static void saveAllBugFixingCommitDir(String projectsDir, String gitRepo, String NiCadSystemsDir) throws IOException {
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
                String[] command1 = {"bash", "-c", "cd " + gitRepo + "/" + name + ";" + "git switch --detach " + commit + ";" + "cp -r ../" + name + " " + NiCadSystemsDir + ";" + "mv " + NiCadSystemsDir + "/" + name + " " + NiCadSystemsDir + "/" + name + "-Add-" + index};
                Utilities.implCommand(command1);
                // 第二次执行命令，拿到commitId^的版本文件;
                String[] command2 = {"bash", "-c", "cd " + gitRepo + "/" + name + ";" + "git switch --detach " + commit + "^;" + "cp -r ../" + name + " " + NiCadSystemsDir + ";" + "mv " + NiCadSystemsDir + "/" + name + " " + NiCadSystemsDir + "/" + name + "-Minus-" + index};
                Utilities.implCommand(command2);
            }
            reader.close();
        }
    }

    // 功能5： 对每个commit版本，都执行一次Nicad克隆，得到Nicad克隆的结果。
    // 这个功能在linux中使用脚本功能能够实现。


    // 功能6： 实现共变克隆的共变检测的问题， 并记录每个bug-fixing中，有多少commit是与共变克隆相关的；
    public static void extractAllBuggyCochangedClones(String projectsDir, String gitRepo, String Output) throws IOException {
        File[] projectsList = new File(Output).listFiles(); // 共变结果文件的列表;
        assert projectsList != null;
        Arrays.sort(projectsList, Comparator.comparing(File::getName)); // 排序;

        // 取每一个项目进行讨论
        for (File outPutProject : projectsList) {
            // 取得项目的名称
            String fullNameBase = outPutProject.getName(); // （包含-ZZZ），同时该文件名就是该项目的base版本名
            String name = fullNameBase.substring(0, fullNameBase.indexOf("-ZZZ")); // 纯项目名字;（不包含-ZZZ, -Add, -Minus）;

            Set<Integer> commitNumSet = new HashSet<>(); // 对bug-fixing进行去重;
            Set<Integer> commitRTCCloneNumSet = new HashSet<>(); // 对buggy共变克隆数量去重;
            for (File aFile : Objects.requireNonNull(outPutProject.listFiles())) { // 考虑项目的每个A文件,一对AB项目，就是一个具体版本项目的克隆的映射;
                // 找到一个A文件，即保存发生共变的base版本中的克隆的文件;
                // 一个A文件就是一个被比较版本文件， 其实也是一个bug-fixing commit/commit^ 版本;
                if (aFile.getName().endsWith("-A.txt")) {

                    // base文件就是fullName文件， 现在取得被比较的文件;
                    String fileName = aFile.getName();
                    // 取得被比较文件的全名;
                    String fullNameCompare = fileName.substring(fileName.indexOf("___") + 3, fileName.lastIndexOf("-A.txt"));

                    // 现在去取对应的B文件，即保存发生共变的被比较版本中的克隆
                    File bFile = null;
                    for (File file : Objects.requireNonNull(outPutProject.listFiles())) {
                        if (file.getName().contains(fullNameCompare + "-B.txt")) {
                            bFile = file;
                            break;
                        }
                    }

                    // 现在去取对应的changedFile文件; 根据纯项目名确定对应的位置; changedFile位于tmp目录下;
                    File tmpProjectDir = new File(projectsDir + File.separator + name);
                    // 找到对应的changedFile文件;
                    // 所以先拿标记，bug-fixing commit 为 Add， bug-fixing commit^ 为 Minus
                    // 还有index, 表示第一个bug-fixing
                    int index = Integer.parseInt(fullNameCompare.substring(fullNameCompare.lastIndexOf("-") + 1));
                    // 获得标记;
                    String sign;
                    if (fullNameCompare.contains("-Add-")) {
                        sign = "Add";
                    } else {
                        sign = "Minus";
                    }

                    // 考虑所有的bug-fixing commit, 不考虑重复的index;
                    commitNumSet.add(index);

                    // 获得修改行中的那个文件，ChangedFile, 保存当前被比较版本项目中所有的修改行;
                    File changedFile = null;
                    for (File file : Objects.requireNonNull(tmpProjectDir.listFiles())) { // 遍历tmp/project目录下的每个文件;
                        if (file.getName().endsWith("_" + sign + "_" + index + ".txt")) {
                            changedFile = file;
                            break;
                        }
                    }

                    // 保存具有bug倾向的base版本的共变克隆, 一个项目版本一个文件。
                    BufferedWriter changedWriter = new BufferedWriter(new FileWriter(outPutProject.getAbsolutePath() + File.separator + fullNameBase + "___" + fullNameCompare + "-BuggyCClone.txt"));
                    // 读取修改行的每个修改的文件路径，并记录发生的修改行;
                    assert changedFile != null;
                    BufferedReader changedFileReader = new BufferedReader(new FileReader(changedFile));
                    String line;
                    // 只要一次bug-fixing中任意一修改行和CClone相关，我们就认为这次bug-fixing为与CClone相关的bug-fixing;
                    while ((line = changedFileReader.readLine()) != null) {
                        if (line.startsWith("***** ")) { // 读取每一个文件路径;
                            TreeSet<Integer> changedLinesSet = new TreeSet<>(); // 该路径下发生的修改行记录到有序集合中;
                            // 首先提取发生的路径是什么; 该求得的路径不包含纯文件名，默认是在对应文件的根目录下;
                            String path = line.substring(line.indexOf("/") + 1, line.lastIndexOf(" "));
                            changedWriter.write("以下路径有---buggy共变克隆--- " + name + File.separator + path + "\n");
                            // 提取发生的修改行;
                            line = changedFileReader.readLine();
                            for (String s : line.split(" ")) {
                                if (!s.equals("")) {
                                    changedLinesSet.add(Integer.parseInt(s));
                                }
                            }
                            // 去bFile中，找是否有对应的路径，并发生了修改; 要求找出每一个路径发生修改的克隆对;
                            assert bFile != null;
                            BufferedReader bFileReader = new BufferedReader(new FileReader(bFile));
                            String bLine;
                            while ((bLine = bFileReader.readLine()) != null) {
                                if (bLine.startsWith("<clonepair")) {
                                    // 读取每一对克隆对，看是否有路径匹配;
                                    String clonepair = bLine; // 连续保存三条记录;
                                    String firstFile = bFileReader.readLine();
                                    String nextFile = bFileReader.readLine();

                                    // 开始判断是否两个克隆片段中是否任意一克隆片段涉及到修改，如果是，则这个克隆片段有bug倾向
                                    // 然后就能把对应的A文件的对应的克隆对，存入文件中；
                                    boolean isChanged = checkIfIsChanged(changedLinesSet, path, firstFile) || checkIfIsChanged(changedLinesSet, path, nextFile);
                                    // 如果这一对克隆对发生了修改，那就去对应的A文件，找到这对克隆对应的克隆。
                                    if (isChanged) { // 发生了修改，找到了发生修改的共变克隆;
                                        commitRTCCloneNumSet.add(index);
                                        BufferedReader aFileReader = new BufferedReader(new FileReader(aFile));
                                        String aLine;
                                        while ((aLine = aFileReader.readLine()) != null) {
                                            if (aLine.contains(clonepair)) { // 找到了a(即base)文件中的对应的克隆;
                                                // 先把base的克隆放在前头，compare的放在后头;
                                                changedWriter.write(aFileReader.readLine() + "\n");
                                                changedWriter.write(aFileReader.readLine() + "\n");
                                                changedWriter.write(firstFile + "\n");
                                                changedWriter.write(nextFile + "\n");
                                                changedWriter.newLine();
                                                changedWriter.newLine();
                                                changedWriter.newLine();
                                                break;
                                            }
                                        }
                                        aFileReader.close();
                                    }
                                }
                            }
                            changedWriter.newLine();
                            bFileReader.close();
                        }
                    }
                    changedWriter.close();
                    changedFileReader.close();
                }
            }


            // 下面开始对base版本的buggy共变克隆进行去重； 这里采用的方法和对共变克隆去重的方式是相同，可能不太准确！！！
            int num = 0; // 计算不重复的buggy共变的数量;
            Set<String> set = new HashSet<>(); // 对当前项目进行去重;
            for (File file : Objects.requireNonNull(outPutProject.listFiles())) { // 遍历这些文件;
                if (file.getName().contains("-BuggyCClone.txt")) { // 目标文件;
                    BufferedReader reader = new BufferedReader(new FileReader(file)); // 读取这个文件;
                    // 因为一个项目的不同克隆对的pcid是不同的，因此只需要根据pcid去重即可：
                    String line = reader.readLine();
                    while (line != null) {
                        if ((line.contains("---buggy"))) { // 目标路径;
                            // 读当前路径下的所有buggy对;
                            line = reader.readLine();
                            while (line != null && !line.contains("---buggy")) {
                                if (line.startsWith("<source file=")) {
                                    String pcid1 = Utilities.getPcid(line);
                                    String pcid2 = Utilities.getPcid((line = reader.readLine()));
                                    line = reader.readLine();
                                    line = reader.readLine();
                                    line = reader.readLine();

                                    boolean t1 = set.add(pcid1 + pcid2);
                                    boolean t2 = set.add(pcid2 + pcid1);
                                    if (t1 && t2) {
                                        ++num;
                                    }
                                    // 标准的写法
//                                    int hashValue = pcid1.hashCode() ^ pcid2.hashCode();
//                                    if (set.add(hashValue)) {
//                                        num++;
//                                    }
                                }
                                line = reader.readLine();
                            }
                        } else {
                            line = reader.readLine();
                        }
                    }
                    reader.close();
                }
            }

            // 把最终结果写入大FinalResult文件中, 首先拿到这个文件。
            File FinalResult = null;
            for (File file : Objects.requireNonNull(outPutProject.listFiles())) {
                if (file.getName().contains("FinalResult")) {
                    FinalResult = file;
                    break;
                }
            }

            // 把结果写入
            assert FinalResult != null;
            BufferedWriter writer = new BufferedWriter(new FileWriter(FinalResult, true));
            writer.newLine();
            writer.newLine();

            writer.write("--------去除掉重复的buggy共变克隆，还有" + num +"对--------------\n\n");
            writer.write("--------当前项目总共处理了" + commitNumSet.size() + "个bugfixing, 其中有" + commitRTCCloneNumSet.size()+ "个是跟CClone相关的");


            writer.close();
        }
    }

    private static boolean checkIfIsChanged(TreeSet<Integer> set, String path, String file) {
        boolean flag = false;
        if (file.contains(path)) {
            int startLine = Utilities.getStartLine(file);
            int endLine = Utilities.getEndLine(file);
            // 判断是否有修改行在区间内：
            Integer ceiling = set.ceiling(startLine);
            if (ceiling != null && ceiling <= endLine) {
                flag = true;
            }
        }
        return flag;
    }

    // 7. 实现一个小功能， 自动提取 一个commit区间中的所有commit信息;
    // 给定一个格式化的文件 "target.txt" ，里面的每一行都是 项目名 git克隆链接 最新版本号 最远版本号;
    public static void extractLogForProjects(String projectsDir, String gitRepo, String targetFile) throws IOException {
        File target = new File(targetFile);
        BufferedReader targetReader = new BufferedReader(new FileReader(target));
        String line;
        while ((line = targetReader.readLine()) != null) { // 读取每一行;
            String[] info = line.split(" ");
            String name = info[0], gitLink = info[1], latestVersion = info[2], olderVersion = info[3];
            File dir = new File(target.getParent() + File.separator + name); // 存储每个项目的目录;
            if (!dir.exists()) dir.mkdir(); // 创建这个文件夹;

            // 在gitRepo目录下克隆该项目, 如果已存在该项目，不会执行克隆;
            String[] clone = {"bash", "-c", "cd " + gitRepo + ";git clone " + gitLink};
            Utilities.implCommand(clone);

            // 来到gitRepo目录下， 执行git log命令;
            String[] gitLog = {"bash", "-c", "cd " + gitRepo + File.separator + name + "; git log " + olderVersion + ".." + latestVersion + " > " + dir.getAbsolutePath() + File.separator + name + "_log.txt"};
            Utilities.implCommand(gitLog);


        }
        targetReader.close();
    }

    // 8. 提取每个项目的最新的版本作为base版本，放入到Nicad/systems/下;
    public static void generateLatestProjects(String targetFile, String gitRepo, String NiCadSystemsDir) throws IOException {
        BufferedReader targetReader = new BufferedReader(new FileReader(targetFile)); // 读取每一行文件;
        String line;
        while ((line = targetReader.readLine()) != null) {
            String[] str = line.split(" ");
            String name = str[0], gitLink = str[1], latestVersion = str[2], olderVersion = str[3];

            // 把最新版本的项目复制一份到NiCadSystemsDir下;
            String[] command1 = {"bash", "-c", "cd " + gitRepo + File.separator + name + "; git checkout " + latestVersion + "; cp -r ../" + name + " " + NiCadSystemsDir};
            Utilities.implCommand(command1);
            // 改名一下：
            String[] command2 = {"bash", "-c", "cd " + NiCadSystemsDir + "; mv ./" + name + " ./" + name + "-ZZZ-999"};
            Utilities.implCommand(command2);
        }
        targetReader.close();
    }
}
