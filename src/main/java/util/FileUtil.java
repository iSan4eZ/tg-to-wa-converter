package util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class FileUtil {

  public static String cleanupPath(String path) {
    String result = path.trim();
    if (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }

  public static void createDirectory(String path) {
    final File file = new File(path);
    file.mkdirs();
  }

  public static String zipFiles(Map<String, String> filePathMap, String toPath, String chatName) throws IOException {
    int fileNumber = 1;
    String fullPath = toPath + "/" + String.format("WhatsApp Chat - %s.zip", chatName);
    while (new File(fullPath).exists()) {
      fullPath = String.format("%s/WhatsApp Chat - %s(%s).zip", toPath, chatName, fileNumber++);
    }
    FileOutputStream fos = new FileOutputStream(fullPath);
    ZipOutputStream zipOut = new ZipOutputStream(fos);
    for (String fileName : filePathMap.keySet()) {
      final String path = filePathMap.get(fileName);
      File fileToZip = new File(path);
      FileInputStream fis = new FileInputStream(fileToZip);
      ZipEntry zipEntry = new ZipEntry(fileName);
      zipOut.putNextEntry(zipEntry);

      byte[] bytes = new byte[1024];
      int length;
      while ((length = fis.read(bytes)) >= 0) {
        zipOut.write(bytes, 0, length);
      }
      fis.close();
    }
    zipOut.close();
    fos.close();
    return fullPath;
  }

}
