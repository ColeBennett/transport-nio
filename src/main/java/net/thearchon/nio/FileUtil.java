package net.thearchon.nio;

import java.io.*;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public final class FileUtil {

    private static final DecimalFormat format = new DecimalFormat("#.##");

    public static byte[] fileToBytes(File file) {
        try {
            byte[] result = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(result);
            fis.close();
            return result;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void bytesToFile(byte[] bytes, File file) {
        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(bytes);
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static String getDirectorySizeDisplay(File dir) {
        long bytes = 0;
        for (File file : getDirectoryFiles(dir)) {
            bytes += file.length();
        }
        return getSizeDisplay(bytes);
    }

    public static String getSizeDisplay(File file) {
        return getSizeDisplay(file.length());
    }

    public static String getSizeDisplay(byte[] bytes) {
        return getSizeDisplay(bytes.length);
    }

    public static String getSizeDisplay(long size) {
        String result = "";
        if ((size / 1024 / 1024 / 1024 / 1024) >= 1) {
            result = format.format((size / 1024 / 1024 / 1024 / 1024)) + " TB";
        } else if ((size / 1024 / 1024 / 1024) >= 1) {
            result = format.format((size / 1024 / 1024 / 1024)) + " GB";
        } else if ((size / 1024 / 1024) >= 1) {
            result = format.format((size / 1024 / 1024)) + " MB";
        } else if ((size / 1024) >= 1) {
            result = format.format((size / 1024)) + " KB";
        } else {
            result = format.format(size) + " Bytes";
        }
        return result;
    }

    public static void writeLines(File file, List<String> lines) {
        FileWriter fw = null;
        try {
            fw = new FileWriter(file, true);
            for (String line : lines) {
                fw.write(line + "\n");
            }
            fw.flush();
            fw.close();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static List<String> readLines(File file) {
        List<String> result = new ArrayList<>();
        BufferedReader br = null;
        try {
            String line;
            br = new BufferedReader(new FileReader(file));
            while ((line = br.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return result;
    }

    public static void clearFile(File file) {
        try {
            FileOutputStream writer = new FileOutputStream(file);
            writer.write("".getBytes());
            writer.flush();
            writer.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static long getLineCount(File file) {
        int count = 0;
        try {
            LineNumberReader reader = new LineNumberReader(new FileReader(file));
            while (reader.readLine() != null) {
                count++;
            }
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return count;
    }

    public static long getDirectoryLineCount(File dir) {
        long result = 0;
        for (File file : getDirectoryFiles(dir)) {
            result += getLineCount(file);
        }
        return result;
    }

    public static long getDirectoryLineCount(File dir, String extension) {
        long result = 0;
        for (File file : getDirectoryFiles(dir, extension)) {
            result += getLineCount(file);
        }
        return result;
    }

    public static List<File> getDirectoryFiles(File dir) {
        return new FileList(dir).getFiles();
    }

    public static List<File> getDirectoryFiles(File dir, String filter) {
        return new FileList(dir).getFiles(filter);
    }

    private final static class FileList {

        private final List<File> files = new ArrayList<>();

        public FileList(File file) {
            addFiles(file);
        }

        public List<File> getFiles() {
            return files;
        }

        public List<File> getFiles(String filter) {
            List<File> filtered = new ArrayList<>();
            for (File file : files) {
                if (file.getName().endsWith(filter)) {
                    filtered.add(file);
                }
            }
            return filtered;
        }

        private void addFiles(File file) {
            File[] fileList;
            if (file.isFile()) {
                files.add(file);
            } else {
                fileList = file.listFiles();
                for (File aFileList : fileList) {
                    addFiles(aFileList);
                }
            }
        }
    }

    private FileUtil() {

    }
}