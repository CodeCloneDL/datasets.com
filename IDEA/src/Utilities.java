import java.io.*;
import java.util.Comparator;
import java.util.HashSet;

/**
 * @author CrazyYao
 * @create 2022-03-21 10:08
 */
public class Utilities {

    public static String getFilePath(String str) {
        return str.substring(str.indexOf("systems"), str.indexOf(".py") + 3);
    }

    public static String getPcid(String str) {
        StringBuilder pcid = new StringBuilder();
        int t1 = str.indexOf("pcid") + 6;
        while (str.charAt(t1) != '\"') {
            pcid.append(str.charAt(t1));
            ++t1;
        }
        return pcid.toString();
    }

    public static int getStartLine(String str) {
        StringBuilder strtmp = new StringBuilder();
        int t1 = str.indexOf("startline=") + 11;
        while (str.charAt(t1) != '\"') {
            strtmp.append(str.charAt(t1));
            ++t1;
        }
        return Integer.parseInt(strtmp.toString());
    }

    public static int getEndLine(String str) {
        StringBuilder strtmp = new StringBuilder();
        int t1 = str.indexOf("endline=") + 9;
        while (str.charAt(t1) != '\"') {
            strtmp.append(str.charAt(t1));
            ++t1;
        }
        return Integer.parseInt(strtmp.toString());
    }

    public static boolean judge1_4CCClone(String[] string1, String[] string2, String[] string3, String[] string4) {
        System.out.println("函数匹配成功！开始检测是否发生共变！");
        int changeflag1 = 0;
        for (int i = 0; i < string1.length; ++i) {
            if (string1[i] == null && string4[i] == null)
                break;
            if (string1[i] == null && string4[i] != null || string1[i] != null && string4[i] == null) {
                ++changeflag1;
                break;
            }
            if (!string1[i].trim().equals(string4[i].trim())) {
                ++changeflag1;
                break;
            }
        }

        int changeflag2 = 0;
        for (int i = 0; i < string2.length; ++i) {
            if (string2[i] == null || string3[i] == null)
                break;
            if (string2[i] == null && string3[i] != null || string2[i] != null && string3[i] == null) {
                changeflag2++;
                break;
            }
            if (!string2[i].trim().equals(string3[i].trim())) {
                ++changeflag2;
                break;
            }

        }
        return changeflag1 != 0 && changeflag2 != 0;
    }

    public static boolean judge1_3CCClone(String[] string1, String[] string2, String[] string3, String[] string4) {
        System.out.println("函数匹配成功！开始检测是否发生共变！");
        int changeflag1 = 0;
        for (int i = 0; i < string1.length; ++i) {
            if (string1[i] == null && string3[i] == null)
                break;
            if (string1[i] == null && string3[i] != null || string1[i] != null && string3[i] == null) {
                ++changeflag1;
                break;
            }
            if (!string1[i].trim().equals(string3[i].trim())) {
                ++changeflag1;
                break;
            }
        }

        int changeflag2 = 0;
        for (int i = 0; i < string2.length; ++i) {
            if (string2[i] == null || string4[i] == null)
                break;
            if (string2[i] == null && string4[i] != null || string2[i] != null && string4[i] == null) {
                changeflag2++;
                break;
            }
            if (!string2[i].trim().equals(string4[i].trim())) {
                ++changeflag2;
                break;
            }
        }

        return changeflag1 != 0 && changeflag2 != 0;
    }

    public static void printTime(long l) {
        long minute, second;
        minute = l / 1000 / 60;
        second = l / 1000 % 60;
        System.out.println("运行时间为：" + minute + "分" + second + "秒");
    }

    public static void deleteFileOrDir(File file) throws Exception {
        if (file.isFile()) {//判断是否为文件，是，则删除
            if (!file.delete())
                throw new Exception("删除" + file.getAbsolutePath() + "文件失败！");
        } else {
            File[] files = file.listFiles();
            for (File filetmp : files) {
                deleteFileOrDir(filetmp);//递归，对每个都进行判断
            }
            if (!file.delete())
                throw new Exception("删除" + file.getAbsolutePath() + "文件夹失败！");
        }
    }

    public static void copyFile(String rename1, String rename2) throws Exception {

        BufferedReader bufferedReader = new BufferedReader(new FileReader(rename1));
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(rename2));

        String str;
        while ((str = bufferedReader.readLine()) != null) {
            bufferedWriter.write(str + "\n");
        }

        //关闭文件
        bufferedReader.close();
        bufferedWriter.close();

    }

    public static void copyDir(String sourceDir, String targetDir) throws Exception {
        // 获取源文件夹当下的文件。
        File[] file = (new File(sourceDir)).listFiles();

        assert file != null;
        for (File value : file) {
            if (value.isFile()) {
                copyFile(value.getAbsolutePath(), targetDir + File.separator + value.getName());
            }

            if (value.isDirectory()) {
                // 准备复制的源文件夹
                String dir1 = sourceDir + File.separator + value.getName();
                // 准备复制的目标文件夹
                String dir2 = targetDir + File.separator + value.getName();
                File file1 = new File(dir2);
                if (!file1.exists())
                    file1.mkdir();
                copyDir(dir1, dir2);
            }
        }
    }

    public static int GetTotalSLOC(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        HashSet<String> set = new HashSet<>();

        String tmp;
        int sum = 0;
        int startline, endline;

        while ((tmp = bufferedReader.readLine()) != null) {
            if (!tmp.contains("startline") || !set.add(Utilities.getPcid(tmp)))
                continue;
            startline = tmp.indexOf("startline=") + 11;
            endline = tmp.indexOf("endline=") + 9;
            StringBuilder str1 = new StringBuilder();
            StringBuilder str2 = new StringBuilder();

            while ((tmp.charAt(startline)) != '"') {
                str1.append(tmp.charAt(startline));
                ++startline;
            }
            while (tmp.charAt(endline) != '"') {
                str2.append(tmp.charAt(endline));
                ++endline;
            }
            sum += Integer.parseInt(str2.toString()) - Integer.parseInt(str1.toString()) + 1;
        }
        bufferedReader.close();
        return sum;
    }

    public static int GetNiCadTotalCloneNum(File file) throws IOException {
        FileReader fileReader = new FileReader(file);
        BufferedReader bufferedReader = new BufferedReader(fileReader);
        String tmp;
        int a, sum = 0;
        while ((tmp = bufferedReader.readLine()) != null) {
            a = tmp.indexOf("<clone nlines");
            if (a != -1)
                ++sum;
        }
        bufferedReader.close();
        return sum;
    }

    public static int GetTotalCloneNum(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        String tmp;
        int a, sum = 0;
        while ((tmp = bufferedReader.readLine()) != null) {
            if (tmp.contains("<clonepair")) {
                sum++;
            }
        }
        bufferedReader.close();
        return sum;
    }
}

class AlphanumFileComparator<T> implements Comparator<T> {

    public String getUnit(String s, int strlength, int marker) {
        StringBuilder Unit = new StringBuilder();
        char c = s.charAt(marker);
        Unit.append(c);
        marker++;
        if (isDigit(c)) {
            while (marker < strlength) {
                c = s.charAt(marker);
                if (!isDigit(c))
                    break;
                Unit.append(c);
                marker++;
            }
        } else {
            while (marker < strlength) {
                c = s.charAt(marker);
                if (isDigit(c))
                    break;
                Unit.append(c);
                marker++;
            }
        }
        return Unit.toString();
    }

    @Override
    public int compare(Object o1, Object o2) {
        if (!(o1 instanceof File f1) || !(o2 instanceof File f2)) {
            return 0;
        }
        String s1 = f1.getName();
        String s2 = f2.getName();

        int thisMarker = 0;
        int thatMarker = 0;
        int s1Length = s1.length();
        int s2Length = s2.length();

        while (thisMarker < s1Length && thatMarker < s2Length) {
            String thisUnit = getUnit(s1, s1Length, thisMarker);
            thisMarker += thisUnit.length();

            String thatUnit = getUnit(s2, s2Length, thatMarker);
            thatMarker += thatUnit.length();


            int result;
            if (isDigit(thisUnit.charAt(0)) && isDigit(thatUnit.charAt(0))) {
                int thisUnitLength = thisUnit.length();
                result = thisUnitLength - thatUnit.length();
                if (result == 0) {
                    for (int i = 0; i < thisUnitLength; i++) {
                        result = thisUnit.charAt(i) - thatUnit.charAt(i);
                        if (result != 0) {
                            return result;
                        }
                    }
                }
            } else {
                result = thisUnit.compareTo(thatUnit);
            }

            if (result != 0)
                return result;
        }

        return s1Length - s2Length;
    }

    public boolean isDigit(char ch) {
        return ch >= 48 && ch <= 57;
    }
}