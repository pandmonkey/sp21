package gitlet;

// TODO: any imports you need here

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */
    public Commit() {
        this.message = "initial commit";
        this.parent  = null;
        this.parent2 = null;
        this.timestamp = new Date(0);
        this.fileNames = new HashMap<>();
        setSHA1();
    } //只用于初始化的构造函数

    public Commit(String message, Commit parent) {
        this.message = message;
        this.parent = parent.getSHA1();
        this.timestamp = new Date();
        this.parent2 = null;
        this.fileNames = new HashMap<>(parent.fileNames);
    } //正常的初始化commit

    public Commit(Commit parent1, Commit parent2, String msg) {
        this.message = msg;
        this.parent = parent1.getSHA1();
        this.parent2 = parent2.getSHA1();
        this.timestamp = new Date();
        this.fileNames = new HashMap<>();
    }
    /** The message of this Commit. */
    private final String message;
    private final Date timestamp;
    private final String parent; // 默认的parent
    private final String parent2;
    private final HashMap<String, String> fileNames; //注意 这里面存的是 Blob的 sha1
    private String sha1Code;

    public void setSHA1() {

        List<Object> inputList = new ArrayList<>();
        inputList.add(message);
        inputList.add(timestamp.toString());
        inputList.add((parent != null) ? parent: "null");
        inputList.add((parent2 != null) ? parent2: "null");
        inputList.add((fileNames != null) ? fileNames.toString(): "null");
        sha1Code = Utils.sha1(inputList);
    }

    public String getSHA1() {
        return sha1Code;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public String getParent() {
        return this.parent;
    }

    public boolean fileExists(String filename) {
        return this.fileNames.containsKey(filename);
    }

    public String fileContent(String filename) {
        return this.fileNames.get(filename);
    }

    public void putFile(String fileName, String sha1) {
        this.fileNames.put(fileName, sha1);
    }

    public void deleteFile(String fileName) {
        this.fileNames.remove(fileName);
    }

    public HashMap<String, String> getFileNames() {
        return new HashMap<>(this.fileNames);
    }

    public boolean haveSecondParent() {
        return this.parent2 != null;
    }

    public String secondParent() {
        return this.parent2;
    }



}
