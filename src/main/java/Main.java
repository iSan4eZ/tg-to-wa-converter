import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

public class Main {

  private static final DateTimeFormatter chatDtf = DateTimeFormatter.ofPattern("dd.MM.yyyy, HH:mm:ss");
  private static final DateTimeFormatter fileDtf = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
  private static final String FIRST_MESSAGE = "\u200EMessages and calls are end-to-end encrypted. No one outside of this chat, not even WhatsApp, can read or listen to them.";
  private static final Map<Long, String> messageMap = new HashMap<>();
  //<name, path>
  private static final Map<String, String> filePathMap = new HashMap<>();

  public static void main(String[] args) {
    final String from;
    final String to;
    Scanner in = new Scanner(System.in);

    if (args.length > 1) {
      from = args[0];
      to = args[1];
    } else {
      System.out.println("Paste the path to the folder containing the exported JSON data:");
      from = in.nextLine();
      System.out.println("Paste the path to the output folder (we'll create it if it doesn't exist):");
      to = in.nextLine();
    }
    in.close();
    JSONParser jsonParser = new JSONParser();

    createDirectory(to);
    final String chatPath = to + "/._chat.txt";
    final File chatFile = new File(chatPath);
    if (chatFile.exists()) {
      chatFile.delete();
    }
    try (FileReader reader = new FileReader(from + "/result.json");
        FileWriter writer = new FileWriter(chatPath)) {
      final JSONObject parsedObject = (JSONObject) jsonParser.parse(reader);
      final String chatName = String.valueOf(parsedObject.get("name"));
      final String chatType = String.valueOf(parsedObject.get("type"));
      final JSONArray messages = (JSONArray) parsedObject.get("messages");
      final Iterator messageIterator = messages.iterator();
      int messageNumber = 0;
      while (messageIterator.hasNext()) {
        final JSONObject message = (JSONObject) messageIterator.next();
        System.out.println(message.toJSONString());
        String path = null;
        String fileType = null;
        if (message.containsKey("photo")) {
          path = message.get("photo").toString();
          fileType = "PHOTO";
        } else if (message.containsKey("file")) {
          path = message.get("file").toString();
          final String mediaType = String.valueOf(message.get("media_type"));
          switch (mediaType) {
            case "video_file":
            case "video_message":
              fileType = "VIDEO";
              break;
            case "voice_message":
              fileType = "AUDIO";
              break;
            case "sticker":
              fileType = "STICKER";
              break;
            case "animation":
              fileType = "GIF";
              break;
            default:
              fileType = "FILE";
              break;
          }
        }
        if (message.containsKey("from")) {
          final String sender = message.get("from").toString();
          final LocalDateTime date = LocalDateTime.parse(message.get("date").toString());

          final String formattedDate = chatDtf.format(date);

          if (messageNumber == 0 && !chatType.equals("personal_chat")) {
            writer.write(String.format("[%s] %s: %s\n", formattedDate, chatName, FIRST_MESSAGE));
          }
          if (Objects.nonNull(path)) {
            final String filename = path.substring(path.indexOf("/") + 1, path.lastIndexOf("."));
            final String extension = path.substring(path.lastIndexOf("."));
            String finalFilename = filename + extension;
            int fileNumber = 1;
            while (filePathMap.containsKey(finalFilename)) {
              finalFilename = String.format("%s(%s)%s", filename, fileNumber++, extension);
            }
            writer.write(String.format("[%s] %s: <attached: %s>\n", formattedDate, sender, finalFilename));
            filePathMap.put(finalFilename, from + "/" + path);
          }
          final StringBuilder finalText = new StringBuilder();

          final Object text = message.get("text");
          if (text instanceof JSONArray) {
            final JSONArray textArray = (JSONArray) text;
            final Iterator textIterator = textArray.iterator();
            while (textIterator.hasNext()) {
              final Object textPart = textIterator.next();
              if (textPart instanceof JSONObject) {
                final JSONObject textPartObject = (JSONObject) textPart;
                finalText.append(textPartObject.get("text"));
                if (textPartObject.containsKey("href")) {
                  finalText.append(String.format("(%s)", textPartObject.get("href")));
                }
              } else {
                finalText.append(textPart.toString());
              }
            }
          } else {
            finalText.append(text.toString());
          }

          final StringBuilder prefix = new StringBuilder();
          if (message.containsKey("forwarded_from")) {
            prefix.append("[Forwarded from: ")
                .append(message.get("forwarded_from"))
                .append("]\n\n");
          }
          if (message.containsKey("reply_to_message_id")) {
            prefix.append("[Reply to: ")
                .append(messageMap.get((Long) message.get("reply_to_message_id")))
                .append("]\n\n");
          }

          messageMap.put((Long) message.get("id"),
              String.format("[%s] %s: %s%s", formattedDate, sender,
                  Objects.nonNull(fileType) ? String.format("[%s] ", fileType) : "", finalText));
          if (finalText.length() > 0) {
            writer.write(String.format("[%s] %s: %s%s\n", formattedDate, sender, prefix, finalText));
          }
          messageNumber++;
        }
      }
      filePathMap.put("_chat.txt", chatPath);
      writer.flush();
      zipFiles(to, chatName);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }
    chatFile.delete();
  }

  private static void createDirectory(String path) {
    final File file = new File(path);
    file.mkdirs();
  }

  public static void zipFiles(String toPath, String chatName) throws IOException {
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
  }

}
