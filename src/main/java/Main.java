import javax.lang.model.element.Name;
import java.io.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
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
        File input = new File("InputTmp2"); // 所有项目的输入文件所在处;
        File output = new File("Output"); // 所有项目的结果文件所在处;
        if (!output.exists()) output.mkdir();

        File[] inputfiles = input.listFiles(); // 所有项目文件;
        assert inputfiles != null;
        Arrays.sort(inputfiles, new AlphanumFileComparator<>()); // 按照项目进行块分类。

        int index = 0; // 表示某个的项目块的起始位置；
        int projectNum = 0; // 表示到第几个项目了
        while (index < inputfiles.length) { // 遍历Input文件夹中的所有项目，一个项目是一个整块;

            // 获取项目的名称, 这个不是base版本的名称;
            String fullName = inputfiles[index].getName(); // 包含project-Add-index_functions-blind-clones的全部名称;

            String pureName = fullName.substring(0, fullName.indexOf("-"));  // 项目的前一部分名称，用来判断是否是同一个项目块;
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
            // 即找到一个项目的所有A文件，然后提取其中的克隆对，然后进行去重。
            File[] outputProject = output.listFiles(); // 很多项目都在一个结果文件中
            assert outputProject != null;
            
            // 选择我们目标的那个结果项目，名字就是Base版本的名字;
            File target = null;
            for (File file : outputProject) {
                if (file.getName().contains(subjectname1)) {
                    target = file;
                    break;
                }
            }

            assert target != null;
            File[] resultLists = target.listFiles(); // 获得Output/该项目/*
            assert resultLists != null;
            Arrays.sort(resultLists, new AlphanumFileComparator<>()); // 排个序;
            i = resultLists.length - 1;

            // 把不重复的Base版本的克隆对写入noduplicated文件中;
            BufferedWriter noDupWriter = new BufferedWriter(new FileWriter(output.getAbsolutePath() + File.separator + target.getName() + "__noduplicated.txt"));
            // 去重，保存base版本不重复的克隆对到文件noduplicated.txt中;
            HashSet<String> set = new HashSet<>();
            while (i >= 0) {// 倒序遍历结果目录，提取基版本的非重复克隆对
                if (resultLists[i].getName().endsWith("A.txt")) { // 找到一个版本的A文件;
                    BufferedReader aFileReader = new BufferedReader(new FileReader(resultLists[i])); // 读取这个A文件
                    noDupWriter.write("以下是" + resultLists[i].getName() + "文件的共变克隆对\n"); // 写下这个共变克隆的项目信息
                    String tmp1 = aFileReader.readLine();
                    String tmp2, tmp3;
                    int num = 0; // 记录当前文件非重复克隆对的数量;
                    while (tmp1 != null) { // 读取这个文件所有的非重复共变克隆对;
                        if (tmp1.contains("<clonepair")) { // 找到一个克隆对;
                            tmp2 = tmp1;
                            tmp3 = aFileReader.readLine();
                            tmp1 = aFileReader.readLine();
                            String pcid1 = Utilities.getPcid(tmp3);
                            String pcid2 = Utilities.getPcid(tmp1);
                            // 对克隆对进行全局去重，一个base版本的克隆对，不能重复;
                            boolean t1 = set.add(pcid1 + pcid2);
                            boolean t2 = set.add(pcid2 + pcid1);
                            if (!t1 || !t2) {
                                tmp1 = aFileReader.readLine();
                                continue;
                            }
//                             以下是更标准的写法，但是先不这样做。
//                            int hashValue = pcid1.hashCode() ^ pcid2.hashCode();
//
//                            if (!set.add(hashValue)) { // 如果添加失败，说明组合重复
//                                tmp1 = bufferedReader.readLine();
//                                continue;
//                            }
                            // 是一对新的pcid
                            num++;
                            noDupWriter.write(tmp2 + "\n");
                            noDupWriter.write(tmp3 + "\n");
                            noDupWriter.write(tmp1 + "\n");
                            tmp1 = aFileReader.readLine();
                            noDupWriter.write(tmp1 + "\n\n\n");
                        }
                        tmp1 = aFileReader.readLine();
                    }
                    // 如果这个A文件没有共变克隆对，那么没关系，来几个回车隔绝一下：
                    if (num == 0) {
                        noDupWriter.write("\n\n\n\n\n");
                    }
                    aFileReader.close();
                    noDupWriter.write("\n\n\n\n\n"); // 每个文件之间的位置空一点点;
                }
                --i;
            }
            noDupWriter.close();

            //开始写入总结果。
            // 把结果写入到 FinalResult文件中
            BufferedWriter FinalResultFiles = new BufferedWriter(new FileWriter(output.getAbsolutePath() + File.separator + target.getName() + "__FinalResults.txt"));

            // 读取Allresult中的所有数据;
            // 首先找到这个文件
            File AllresultFile = null;
            for (File file : Objects.requireNonNull(target.listFiles())) {
                if (file.getName().contains("Allresults")) {
                    AllresultFile = file;
                    break;
                }
            }
            // 开始读取Allresults文件
            assert AllresultFile != null;
            BufferedReader AllresultReader = new BufferedReader(new FileReader(AllresultFile));
            String tmp = AllresultReader.readLine();
            while (tmp != null) {
                FinalResultFiles.write(tmp + "\n");
                tmp = AllresultReader.readLine();
            }
            FinalResultFiles.write("\n\n");
            AllresultReader.close(); // 读完该文件即关闭

            // 再次读取这个文件
            AllresultReader = new BufferedReader(new FileReader(AllresultFile));
            tmp = AllresultReader.readLine();

            // 读取base版本项目共有多少的代码克隆
            StringBuilder str = new StringBuilder();
            int x = tmp.indexOf("检测了") + 3;
            while (tmp.charAt(x) != '个') {
                str.append(tmp.charAt(x++));
            }
            // 把base版本的代码克隆数量打印出来；
            FinalResultFiles.write("本次基版本项目" + target.getName() + "共有" + str + "个克隆对。\n\n");

            // 开始记录base版本和所有比较版本发生出现共变的次数;
            int sum = 0;
            int n = 0; // 记录当前被比较版本有多少个！
            while (tmp != null) {
                if (tmp.contains("发生了")) {
                    str.delete(0, str.length());
                    x = tmp.indexOf("发生了") + 3;
                    while (tmp.charAt(x) != '次') {
                        str.append(tmp.charAt(x++));
                    }
                    sum += Integer.parseInt(str.toString());
                    ++n;
                }
                tmp = AllresultReader.readLine();
            }
            // 写入最终结果, Utilities.GetTotalCloneNum是根据文件中"<clonepair"的数量，来求克隆对是数量的;
            FinalResultFiles.write(n + " 次共变检测过程中，共检测到了" + sum + "对共变克隆对\n\n" + "去掉重复的共变克隆对，还有" +
                    Utilities.GetTotalCloneNum(new File(output.getAbsolutePath() + File.separator + target.getName() + "__noduplicated.txt")) + "对共变的克隆对！！\n\n");
            FinalResultFiles.close();
            AllresultReader.close();




            //    删除中间文件，同时移动两个结果文件放进同一个文件夹。
            for (File file : resultLists) {
                if (file.getName().contains("Allresult")) {
                    file.delete();
                    break;
                }
            }
            File[] files3 = output.listFiles();
            File FinalResult = null, noduplicated = null, other = null;
            assert files3 != null;
            for (File file : files3) {
                if (file.getName().contains("FinalResult") && file.getName().contains(subjectname1)) {
                    FinalResult = file;
                } else if (file.getName().contains("noduplicated") && file.getName().contains(subjectname1)) {
                    noduplicated = file;
                } else if (file.getName().contains(subjectname1) && !file.getName().contains("FinalResult") && !file.getName().contains("noduplicated")){
                    other = file;
                }
            }
            assert FinalResult != null;
            assert other != null;
            String[] command1 = {"bash", "-c", "mv " + FinalResult.getAbsolutePath() + " " + other.getAbsolutePath()};
            Utilities.implCommand(command1);
            assert noduplicated != null;
            String[] command2 = new String[]{"bash", "-c", "mv " + noduplicated.getAbsolutePath() + " " + other.getAbsolutePath()};
            Utilities.implCommand(command2);

            long endtime = System.currentTimeMillis();
            Utilities.printTime(endtime - starttime);
            System.out.println("\n\n------------------\n\n已经处理完第" + ++projectNum + "个项目的共变情况");
            index = nextIndex + 1;
        }
        }


}

