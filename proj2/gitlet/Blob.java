package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Blob implements Serializable {
    private byte[] content;
    private String sha1;
    private String fileName;
    public Blob(File fileName) {
        this.fileName = fileName.getName();
        this.content = Utils.readContents(fileName);
        setSHA1();
    }

    public String getSHA1() {
        return this.sha1;
    }
    private void setSHA1() {
        List<Object> inputList = new ArrayList<>();
        inputList.add(fileName);
        inputList.add(content);
        this.sha1 = Utils.sha1(inputList);
    }
    public String getFileName() {
        return this.fileName;
    }

    public byte[] getContent() {
        return Arrays.copyOf(this.content, this.content.length);
    }
}
