package admin.logic;

import android.os.Build;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

public class HtmlReader {
    private final String filePath;

    public HtmlReader(String path)
    {
        filePath = path;
    }

    public String readHtmlFile()
    {
        String htmlFileContent = "FileNotFound";

        try {
            File htmFile = new File(filePath);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                htmlFileContent = new String(Files.readAllBytes(htmFile.toPath()));
            }
        }catch (IOException e)
        {
            htmlFileContent = e.toString();
        }

        return htmlFileContent;
    }
}
