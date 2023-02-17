package features;

import org.apache.commons.math3.distribution.RealDistribution;

import java.io.*;
import java.util.regex.Pattern;

public class ProcessCommit {
    public static void main(String[] args) throws IOException {
        // 1. 提取commit 和bug-fixing commit
        String path = "D:\\SyncFiles\\Master2\\TOSEM修改\\bug倾向\\";
        ProcessCommit.calCommitNum(path);

        //
    }

    // 功能： 1. 计算 'git log commit1..commit2'产生的commit信息文件中所有的commit数量
    // 2. 计算其中bug-fixing commit的数量
    // 3. 提取出bug-fixing 的所有commit id;
    public static void calCommitNum(String path) throws IOException {
        String[] keyWords = new String[]{"bug", "fix", "wrong", "error", "fail", "problem", "patch"};
        File[] files = new File(path).listFiles(); // 所在目录的文件们;
        for (File file : files) {
            String name = file.getName(); // 包含后缀的名字
            if (!name.contains("_log.txt")) continue;
            name = name.substring(0, name.indexOf("_log")); // 去掉后缀"_log.txt";
            // 把所有的bug-fixing ID都写入该文件中;
            BufferedWriter bW = new BufferedWriter(new FileWriter(new File(file.getParent() + File.separator + name + "_commitIds.txt")));
            int k = 0;
            // 开始读取文件，看文件中有多少commit和bug-fixing commit
            int totalCommit = 0, bugCommit = 0;
            BufferedReader bR = new BufferedReader(new FileReader(file)); // 读取这个文件;
            String temp = bR.readLine();
            while (temp != null) {
                // 找到一个commit的位置
                while (temp != null && !checkIfIsCommit(temp)) temp = bR.readLine();
                if (temp == null) break; // 到达末尾;
                String commitId = temp;
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
}
