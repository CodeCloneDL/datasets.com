import java.io.*;
import java.util.Arrays;
import java.util.HashSet;


/**
 * @author CrazyYao
 * @create 2022-03-17 14:46
 */
//程序入口类
public class Main {
    public static void main(String[] args) throws Exception {

        long starttime = System.currentTimeMillis();  //时间戳
//
        File input = new File("Input");
        File output = new File("Output");
        if (!output.exists()) output.mkdir();

        File[] inputfiles = input.listFiles();
        assert inputfiles != null;
        Arrays.sort(inputfiles, new AlphanumFileComparator<>());

        int len = inputfiles.length;

//        选取最新版本为基版本，然后分别跟剩下的版本进行共变比较选择。
        String subjectwholename1 = inputfiles[len - 1].getName();
        String subjectname1 = subjectwholename1.substring(0, subjectwholename1.indexOf('_'));
        String inputf1, inputf1c, inputf2, inputf2c, outputfile1, outputfile2, AllResults, outputfile1withcode, outputfile2withcode;
        inputf1 = inputfiles[len - 1].getAbsolutePath() + File.separator + subjectwholename1 + "-0.30.xml";
        inputf1c = inputfiles[len - 1].getAbsolutePath() + File.separator + subjectwholename1 + "-0.30-classes-withsource.xml";

        File file1 = new File(output.getAbsolutePath() + File.separator + subjectname1);
        if (!file1.exists()) {
            file1.mkdir();
        }
        int i = len - 2;
        while (i >= 0) {//该循环，输出1-2，1-3,1-4,1-5....版本的共变信息。

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
        File[] files2 = output.listFiles();
        assert files2 != null;
        File[] files2_1 = files2[0].listFiles();
        assert files2_1 != null;
        Arrays.sort(files2_1, new AlphanumFileComparator<>());
        i = files2_1.length - 1;

        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(output.getAbsolutePath() + File.separator + files2[0].getName() + "__noduplicated.txt"));
        HashSet<String> set = new HashSet<>();
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

                        boolean t1 = set.add(pcid1 + pcid2);
                        boolean t2 = set.add(pcid2 + pcid1);
                        if (!t1 && !t2) {
                            tmp1 = bufferedReader.readLine();
                            continue;
                        }
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
        BufferedWriter bufferedWriter1 = new BufferedWriter(new FileWriter(output.getAbsolutePath() + File.separator + files2[0].getName() + "__FinalResults.txt"));
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
        bufferedWriter1.write("本次基版本项目" + files2[0].getName() + "共有" + str + "个克隆对。\n\n");

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

        bufferedWriter1.write("五次共变检测过程中，共检测到了" + sum + "对共变克隆对\n\n" + "去掉重复的共变克隆对，还有" +
                Utilities.GetTotalCloneNum(new File(output.getAbsolutePath() + File.separator + files2[0].getName() + "__noduplicated.txt")) + "对共变的克隆对！！\n\n");
        bufferedWriter1.close();
        bufferedReader.close();


//        删除中间文件，同时移动两个结果文件放进同一个文件夹。
        files2_1[0].delete();
        File[] files3 = output.listFiles();
        assert files3 != null;
        Utilities.copyFile(files3[1].getAbsolutePath(), files3[0].getAbsolutePath() + File.separator + files3[1].getName());
        Utilities.copyFile(files3[2].getAbsolutePath(), files3[0].getAbsolutePath() + File.separator + files3[2].getName());
        files3[1].delete();
        files3[2].delete();


        long endtime = System.currentTimeMillis();
        Utilities.printTime(endtime - starttime);

    }

}

