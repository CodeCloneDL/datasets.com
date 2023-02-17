package features;

import org.apache.commons.math3.distribution.RealDistribution;

import java.io.*;

public class ProcessCommit {
    public static void main(String[] args) throws IOException {
        // 1. 提取commit 和bug-fixing commit
        String path = "D:\\SyncFiles\\Master2\\TOSEM修改\\bug倾向\\";
        ProcessCommit.calCommitNum(path);

        //
    }



    // 功能： 1. 计算 'git log commit1..commit2'产生的commit信息文件中所有的commit数量
    // 2. 计算其中bug-fixing commit的数量
    public static void calCommitNum(String path) throws IOException {
        String[] keyWords = new String[]{"bug", "fix", "wrong", "error", "fail", "problem", "patch"};
        File[] files = new File(path).listFiles(); // 所在目录的文件们;
        for (File file : files) {
            String name = file.getName(); // 包含后缀的名字
            if (!name.contains("_log.txt")) continue;
            name = name.substring(0, name.indexOf("_log")); // 去掉后缀"_log.txt";

            // 开始读取文件，看文件中有多少commit和bugfixing commit
            int totalCommit = 0, bugCommit = 0;
            BufferedReader bR = new BufferedReader(new FileReader(file)); // 读取这个文件;
            String temp = bR.readLine();
            while (temp != null) {
                while (temp != null && !temp.contains("commit")) temp = bR.readLine();
                if (temp == null) break; // 到达末尾;
                // 如果commit的下一行不是Author，说明这个commit不是我们要的，跳过;
                temp = bR.readLine();
                if (!temp.contains("Author")) continue;
                temp = bR.readLine();
                if (!temp.contains("Date")) continue;
                ++totalCommit; // 找到了一个commit;
                temp = bR.readLine();
                boolean bugFlag = false; // 只需要找一个关键词匹配到了就行。
                while (temp != null && !temp.contains("commit")) {
                    if (!bugFlag) {
                        temp = temp.toLowerCase();
                        for (String keyWord : keyWords) {
                            if (temp.contains(keyWord)) {
                                ++bugCommit;
                                bugFlag = true;
                                break;
                            }
                        }
                    }
                    temp = bR.readLine();
                }
            }
            bR.close();;
            // 打印结果出来;
            System.out.println("commit总数: " + totalCommit +", bugfix数量: " + bugCommit + "    " + name);
        }
    }
}
