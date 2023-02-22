import javax.lang.model.element.Name;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.function.Predicate;


/**
 * @author CrazyYao
 * @create 2022-03-17 14:46
 */
//程序入口类
public class Main {
    public static void main(String[] args) throws Exception {

        long starttime = System.currentTimeMillis();  //时间戳
//
        File input = new File("Input"); // 所有项目的输入文件所在处;
        File output = new File("Output"); // 所有项目的结果文件所在处;
        if (!output.exists()) output.mkdir();

        File[] inputfiles = input.listFiles(); // 所有项目文件;
        assert inputfiles != null;
        Arrays.sort(inputfiles, new AlphanumFileComparator<>()); // 按照项目进行块分类。

        int index = 0; // 表示来到第几个项目块了；
        int projectNum = 0;
        while (index < inputfiles.length) { // 遍历Input文件夹中的所有项目，一个项目是一个整块;

            // 获取项目的名称, 这个不是base版本的名称;
            String fullName = inputfiles[index].getName(); // 包含project-Add-index_functions-blind-clones的全部名称;
            String name = fullName.substring(0, fullName.indexOf("_"));// 项目文件的全名; project-Add-index 这种形式;
            StringBuilder pureNameSB = new StringBuilder(); // 获取项目的纯名称;
            for (int i = 0; i < name.length(); ++i) {
                char ch = name.charAt(i);
                if (ch != '-') {
                    pureNameSB.append(ch);
                } else {
                    String str = name.substring(i + 1);
                    if (str.startsWith("Add") || str.startsWith("Minus")) break;
                    pureNameSB.append("ch");
                    ++i;
                    while (name.charAt(i) != '-') {
                        pureNameSB.append(name.charAt(i++));
                    }
                    break;
                }
            }
            String pureName = pureNameSB.toString();  // 项目的纯名称;
            // 获取当前项目块的最后一个位置; 就base版本项目的位置；
            int nextIndex = index + 1;
            while (nextIndex < inputfiles.length && inputfiles[nextIndex].getName().contains(pureName)) ++nextIndex;
            --nextIndex;
            // 即目前的版本块的范围为[index, nextIndex]



            // 选取最新版本为基版本，然后分别跟剩下的版本进行共变比较选择。
            String subjectwholename1 = inputfiles[nextIndex].getName();
            String subjectname1 = subjectwholename1.substring(0, subjectwholename1.indexOf('_'));
            String inputf1, inputf1c, inputf2, inputf2c, outputfile1, outputfile2, AllResults, outputfile1withcode, outputfile2withcode;
            inputf1 = inputfiles[nextIndex].getAbsolutePath() + File.separator + subjectwholename1 + "-0.30.xml";
            inputf1c = inputfiles[nextIndex].getAbsolutePath() + File.separator + subjectwholename1 + "-0.30-classes-withsource.xml";

            File file1 = new File(output.getAbsolutePath() + File.separator + subjectname1);
            if (!file1.exists()) {
                file1.mkdir();
            }
            int i = nextIndex - 1;
            while (i >= index) {//该循环，输出1-2，1-3,1-4,1-5....版本的共变信息。

                String subjectwholename2 = inputfiles[i].getName();

                String subjectname2 = subjectwholename2.substring(0, subjectwholename2.indexOf('_'));

                inputf2 = inputfiles[i].getAbsolutePath() + File.separator + subjectwholename2 + "-0.30.xml";
                inputf2c = inputfiles[i].getAbsolutePath() + File.separator + subjectwholename2 + "-0.30-classes-withsource.xml";


                outputfile1 = output.getAbsolutePath() + File.separator + subjectname1 + File.separator + subjectname1 + "___" + subjectname2 + "-A.txt";
                outputfile2 = output.getAbsolutePath() + File.separator + subjectname1 + File.separator + subjectname1 + "___" + subjectname2 + "-B" + ".txt";

                AllResults = output.getAbsolutePath() + File.separator + subjectname1 + File.separator + "Allresults___" + subjectname1.substring(0, subjectname1.indexOf('-')) + ".txt";

                outputfile1withcode = output.getAbsolutePath() + File.separator + subjectname1 + File.separator + subjectname1 + "___" + subjectname2 + "-A-withcode.txt";
                outputfile2withcode = output.getAbsolutePath() + File.separator + subjectname1 + File.separator + subjectname1 + "___" + subjectname2 + "-B-withcode" + ".txt";


//            克隆对信息和源码文件。
                FindCoChangeClone.run(inputf1, inputf1c, inputf2, inputf2c, outputfile1, outputfile2, subjectname2, AllResults, outputfile1withcode, outputfile2withcode);
                --i;
            }

            //*************************************//
            //以下去除各版本之间的非重复共变克隆对,并提取出来；
            File[] files2 = output.listFiles(); // 很多项目都在一个结果文件中
            assert files2 != null;
            
            // 选择我们目标的那个结果项目;
            File target = null;
            for (File file : files2) {
                if (file.getName().contains(subjectname1)) {
                    target = file;
                    break;
                }
            }

            assert target != null;
            File[] files2_1 = target.listFiles();
            assert files2_1 != null;
            Arrays.sort(files2_1, new AlphanumFileComparator<>());
            i = files2_1.length - 1;

            BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(output.getAbsolutePath() + File.separator + target.getName() + "__noduplicated.txt"));
            HashSet<Integer> set = new HashSet<>(); // 使用两字符串的哈希值的异或，来保证一对字符串不会重复出现。
            while (i >= 0) {//遍历结果目录，提取基版本的非重复克隆对
                if (files2_1[i].getName().endsWith("A.txt")) {
                    BufferedReader bufferedReader = new BufferedReader(new FileReader(files2_1[i]));
                    bufferedWriter.write("以下是" + files2_1[i].getName() + "文件的共变克隆对\n");
                    String tmp1 = bufferedReader.readLine();
                    String tmp2, tmp3;
                    int num = 0;
                    while (tmp1 != null) {
                        if (tmp1.contains("<clonepair")) {
                            tmp2 = tmp1;
                            tmp3 = bufferedReader.readLine();
                            tmp1 = bufferedReader.readLine();
                            String pcid1 = Utilities.getPcid(tmp3);
                            String pcid2 = Utilities.getPcid(tmp1);

                            int hashValue = pcid1.hashCode() ^ pcid2.hashCode();

                            if (!set.add(hashValue)) { // 如果添加失败，说明组合重复
                                tmp1 = bufferedReader.readLine();
                                continue;
                            }
                            // 是一对新的pcid
                            num++;
                            bufferedWriter.write(tmp2 + "\n");
                            bufferedWriter.write(tmp3 + "\n");
                            bufferedWriter.write(tmp1 + "\n");
                            tmp1 = bufferedReader.readLine();
                            bufferedWriter.write(tmp1 + "\n\n\n");
                        }
                        tmp1 = bufferedReader.readLine();
                    }
                    if (num == 0) {
                        bufferedWriter.write("\n\n\n\n\n");
                    }
                    bufferedReader.close();
                    bufferedWriter.write("\n\n\n\n\n");
                }
                --i;
            }
            bufferedWriter.close();

            //开始写入总结果。
            BufferedWriter bufferedWriter1 = new BufferedWriter(new FileWriter(output.getAbsolutePath() + File.separator + target.getName() + "__FinalResults.txt"));
            BufferedReader bufferedReader = new BufferedReader(new FileReader(files2_1[0]));
            String tmp = bufferedReader.readLine();

            while (tmp != null) {
                bufferedWriter1.write(tmp + "\n");
                tmp = bufferedReader.readLine();
            }

            bufferedWriter1.write("\n\n");
            bufferedReader.close();


            bufferedReader = new BufferedReader(new FileReader(files2_1[0]));
            tmp = bufferedReader.readLine();

            StringBuilder str = new StringBuilder();
            int sum = 0;
            int x = tmp.indexOf("检测了") + 3;
            while (tmp.charAt(x) != '个') {
                str.append(tmp.charAt(x++));
            }
            bufferedWriter1.write("本次基版本项目" + target.getName() + "共有" + str + "个克隆对。\n\n");

            while (tmp != null) {
                if (tmp.contains("发生了")) {
                    str.delete(0, str.length());
                    x = tmp.indexOf("发生了") + 3;
                    while (tmp.charAt(x) != '次') {
                        str.append(tmp.charAt(x++));
                    }
                    sum += Integer.parseInt(str.toString());
                }
                tmp = bufferedReader.readLine();
            }

            bufferedWriter1.write("几次共变检测过程中，共检测到了" + sum + "对共变克隆对\n\n" + "去掉重复的共变克隆对，还有" +
                    Utilities.GetTotalCloneNum(new File(output.getAbsolutePath() + File.separator + target.getName() + "__noduplicated.txt")) + "对共变的克隆对！！\n\n");
            bufferedWriter1.close();
            bufferedReader.close();




            //    删除中间文件，同时移动两个结果文件放进同一个文件夹。
            for (File file : files2_1) {
                if (file.getName().contains("Allresult")) {
                    file.delete();
                    break;
                }
            }
            File[] files3 = output.listFiles();
            File FinalResult = null, noduplicated = null, other = null;
            for (File file : files3) {
                if (file.getName().contains("FinalResult") && file.getName().contains(subjectname1)) {
                    FinalResult = file;
                } else if (file.getName().contains("noduplicated") && file.getName().contains(subjectname1)) {
                    noduplicated = file;
                } else if (file.getName().contains(subjectname1) && !file.getName().contains("FinalResult") && !file.getName().contains("noduplicated")){
                    other = file;
                }
            }
            String[] command1 = {"bash", "-c", "mv " + FinalResult.getAbsolutePath() + " " + other.getAbsolutePath()};
            Utilities.implCommand(command1);
            String[] command2 = {"bash", "-c", "mv " + noduplicated.getAbsolutePath() + " " + other.getAbsolutePath()};
            Utilities.implCommand(command2);

            long endtime = System.currentTimeMillis();
            Utilities.printTime(endtime - starttime);
            System.out.println("\n\n------------------\n\n已经处理完第" + ++projectNum + "个项目的共变情况");
            index = nextIndex + 1;
        }
        }


}

