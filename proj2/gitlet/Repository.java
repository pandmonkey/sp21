package gitlet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import static gitlet.Utils.*;

/** Represents a gitlet repository.
 *  does at a high level.
 *
 */
public class Repository {
    /**
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");
    /** The .commit directory*/
    public static final File COMMIT_DIR = join(GITLET_DIR, ".commit");
    /** The .blob directory*/
    public static final File BLOB_DIR = join(GITLET_DIR, ".blob");
    /** The status save directory*/
    public static final File STATUS_DOC = join(GITLET_DIR, "status.txt");
    /** The add storage directory*/
    public static final File ADD_STORAGE = join(GITLET_DIR, ".add");

    public static HashMap<String, String> branch2Commit; //branchName --> 最新commit的sha1

    public static HashMap<String, String> addBlobs; // fileName --> sha1

    public static List<String> removalStage;

    public static String nowBranch;

    public static Commit nowHead;

    public static boolean getted = false;


    public static void init() throws IOException {
        if (GITLET_DIR.exists()) {
            System.out.println("A Gitlet version-control system already exists in the current directory.");
            System.exit(0);
        }
        Commit init = new Commit(); // 初始化的commit
        nowHead = init;
        initFolders();
        branch2Commit = new HashMap<>();
        nowBranch = "master";
        branch2Commit.put("master", nowHead.getSHA1());
        addBlobs = new HashMap<>();
        removalStage = new ArrayList<>();
        setStatus();
    }  //git init

    public static void add(String filename) throws IOException {

        //恢复程序数据
        getStatus();
        addHelper(filename);

        setStatus();
    }

    public static void addHelper(String filename) throws IOException {
        File toaddFile = new File(CWD, filename);
        if (!toaddFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        }
        Blob tmp = new Blob(toaddFile); //构造好了tmp blob
        File addedFile = new File(ADD_STORAGE, tmp.getSHA1());
        if (nowHead.fileExists(filename) && nowHead.fileContent(filename).equals(tmp.getSHA1())) {
            //和commit中的内容相同 直接删除得了
            if (addBlobs.containsKey(filename)) {
                deleteSingleStorage(filename);
            }
        }
        else {
            if (addBlobs.containsKey(filename)) {
                String addFileName = addBlobs.get(filename); //原始的sha1文件
                File t1 = new File(ADD_STORAGE,addFileName);
                t1.delete();
            }
            addBlobs.put(filename, tmp.getSHA1());
            if (addedFile.exists()) {
                addedFile.delete();
            }
            addedFile.createNewFile();
            Utils.writeObject(addedFile, tmp);
        }

        if (removalStage.contains(filename)) {
            removalStage.remove(filename);
        } //如果删掉的又add 取消对它的删除

    }

    public static void commit(String message) throws IOException {
        getStatus();

        if (addBlobs.isEmpty() && removalStage.isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }
        Commit toSaveCommit = readCommit(branch2Commit.get(nowBranch));
        nowHead = new Commit(message, toSaveCommit); //当前

        clearStorage();
        removalDelete();
        nowHead.setSHA1();
        branch2Commit.put(nowBranch, nowHead.getSHA1());

        setStatus();
    }

    public static void rm(String filename) throws IOException {
        getStatus();
        if (!(addBlobs.containsKey(filename) || nowHead.fileExists(filename))) {
            System.out.println("No reason to remove the file.");
        }

        if (addBlobs.containsKey(filename)) {
            deleteSingleStorage(filename);
            addBlobs.remove(filename);
        }

        if (nowHead.fileExists(filename)) {
            removalStage.add(filename);
            File nowFile = join(CWD, filename);
            if (nowFile.exists()) {
                restrictedDelete(nowFile); //尝试删除
            }
        }
        setStatus();
    }

    public static void log() throws IOException {
        getStatus();
        Commit iterator = nowHead;
        while(true) {
            formatLog(iterator);
            if(iterator.getParent() == null) {
                break;
            }
            iterator = readCommit(iterator.getParent());
        }
    }

    public static void globalLog() throws IOException {
        getStatus();
        List<String> lst = Utils.plainFilenamesIn(COMMIT_DIR);
        for (String s : lst) {
            Commit it = readCommit(s);
            formatLog(it);
        }
    }

    public static void find(String message) throws IOException {
        getStatus();
        List<String> lst = Utils.plainFilenamesIn(COMMIT_DIR);
        List<String> found = new ArrayList<>();
        for (String s : lst) {
            Commit it = readCommit(s);
            if (it.getMessage().equals(message)) {
                found.add(s);
                System.out.println(s);
            }
        }
        if (found.isEmpty()) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    public static void status() throws  IOException {

        getStatus();
        printBranches();
        stagedFiles();
        removed();
        notStaged();
        untracked();
    }

    /*
    报错: 1. 文件不在要找的 commit中 File does not exist in that commit.
    2. 找不到id对应的 commit: No commit with that id exists.
    3.  一。找不到对branch: No such branch exists.
        二。 branch就是当前的: No need to checkout the current branch.
        三。 一个untracked file 要被覆写: There is an untracked file in the way; delete it, or add and commit it first.
            注意在任何修改前做这一点 考虑对比两个 commit的内容
     */

    /**
     * 实现思路:
     * 从当前的commit中找到(不然报错)对应的文件名
     * 存在/创建一个对应的文件即可
     * @param filename
     * @throws IOException
     */
    public static void checkoutFile(String filename) throws IOException {
        getStatus();
        checkOutCommitHelper(nowHead, filename);
    }

    public static void checkout(String cid, String filename) throws IOException {
        getStatus();
        Commit nowCommit = findDesignatedCmt(cid);
        checkOutCommitHelper(nowCommit, filename);
    }

    public static void checkoutBranch(String branchName) throws IOException {
        getStatus();

        if (!branch2Commit.containsKey(branchName)) {
            System.out.println("No such branch exists.");
            System.exit(0);
        }

        if (branchName.equals(nowBranch)) {
            System.out.println("No need to checkout the current branch.");
            System.exit(0);
        }

        //尝试获取转向的 branch 的对应内容 并对比 如果可以转 那就转
        String bSHA1 = branch2Commit.get(branchName);
        Commit bch = readCommit(bSHA1);
        resetOrCheckout(bch);

        nowBranch = branchName;
        setStatus();
    }

    //创建一个新的 branch名 引用 结束即可
    public static void branch(String branchName) throws IOException {
        getStatus();
        if (branch2Commit.containsKey(branchName)) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        }
        branch2Commit.put(branchName, branch2Commit.get(nowBranch));
        setStatus();
    }

    public static void rmBranch(String branchName) throws IOException {
        getStatus();
        if (!branch2Commit.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        if (branchName.equals(nowBranch)) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        }
        branch2Commit.remove(branchName);
        setStatus();
    }

    public static void reset(String commitID) throws IOException {
        getStatus();
        Commit toReset = findDesignatedCmt(commitID);
        resetOrCheckout(toReset);
        branch2Commit.put(nowBranch, toReset.getSHA1());
        setStatus();
    }

    public static void merge(String branchName) throws IOException {
        getStatus();
        //err1: uncommitted changes:
        if (!(addBlobs.isEmpty() && removalStage.isEmpty())) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        //err2: 不存在的分支
        if (!branch2Commit.containsKey(branchName)) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
        //err3: 想要merge自己:
        if (nowBranch.equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }

        Commit givenBranch = readCommit(branch2Commit.get(branchName));
        Commit current = readCommit(branch2Commit.get(nowBranch));
        Commit split = getLatestCommonCommit(givenBranch, current);
        //得到三个结点 进行比较和合并

        //split 的两个特殊情况:
        if (split.equals(givenBranch)) {
            // givenBranch 和split相同
            System.out.println("Given branch is an ancestor of the current branch.");
            System.exit(0);
        } else if (split.equals(current)) {
            // currentBranch 和split 相同
            System.out.println("Current branch fast-forwarded.");
            checkoutBranch(nowBranch); //转到当前分支
            System.exit(0);
        }
        mergeThreeCommit(split, current, givenBranch, branchName);
        //*******
        setStatus();
    }
    /*
     branch 和 Head 相关内容修改后 必须要存下来
     */
    private static void setStatus() throws IOException {
        //删写branch 和 head
        if (STATUS_DOC.exists()) {
            STATUS_DOC.delete();
        }//如果存在 直接清空并写入即可
            STATUS_DOC.createNewFile();
            ArrayList<Object> tmp = new ArrayList<Object> ();
            tmp.add(branch2Commit);
            tmp.add(nowBranch);
            tmp.add(addBlobs);
            tmp.add(removalStage);
            tmp.add(nowHead);
            Utils.writeObject(STATUS_DOC,tmp);
            writeCommit(nowHead);
    }

    private static void clearStorage() throws IOException {
        for (String s : addBlobs.keySet()) {
            String nowSHA1 = addBlobs.get(s);
            nowHead.putFile(s, nowSHA1);
            File addFile = join(ADD_STORAGE, nowSHA1);

            Blob nowBlob = readObject(addFile, Blob.class);
            writeBlob(nowBlob); //更换存储位置
            deleteSingleStorage(s);
        }
        addBlobs = new HashMap<>();
    }

    private static void removalDelete() throws IOException {
        for (String s : removalStage) {
            nowHead.deleteFile(s);
        }
        removalStage = new ArrayList<>();
    }

    private static void deleteSingleStorage(String filename) throws IOException {

        String nowSHA1 = addBlobs.get(filename);
        File addFile = join(ADD_STORAGE, nowSHA1);
        addFile.delete();
    }

    @SuppressWarnings("unchecked")
    private static void getStatus() {
        if (getted) {
            return;
        }
        getted = true;
        if (!GITLET_DIR.exists()) {
            System.out.println("Not in an initialized Gitlet directory.");
            System.exit(0);
        }
        ArrayList<Object> tmp = Utils.readObject(STATUS_DOC, ArrayList.class);
        branch2Commit = (HashMap<String, String>) tmp.get(0);
        nowBranch = (String) tmp.get(1);
        addBlobs = (HashMap<String, String>) tmp.get(2);
        removalStage = (ArrayList<String>) tmp.get(3);
        nowHead = (Commit) tmp.get(4);
    }

    private static void writeCommit(Commit cmt) throws IOException {
        File file = join(COMMIT_DIR, cmt.getSHA1());
        file.createNewFile();
        Utils.writeObject(file, cmt);
    }

    private static Commit readCommit(String sha1) {
        File file = join(COMMIT_DIR, sha1);
        Commit ret = readObject(file, Commit.class);
        return ret;
    }

    private static void writeBlob(Blob in) throws  IOException {
        File file = join(BLOB_DIR, in.getSHA1());
        file.createNewFile();
        Utils.writeObject(file, in);
    }

    private static Blob readBlob(String sha1) {
        File file = join(BLOB_DIR, sha1);
        Blob ret = readObject(file, Blob.class);
        return ret;
    }

    private static void formatLog(Commit input) {
        System.out.println("===");
        System.out.println("commit " + input.getSHA1());
        if (input.haveSecondParent()) {
            String p1 = input.getParent();
            String p2 = input.secondParent();
            System.out.println("Merge: " + p1.substring(0, 7) + " " + p2.substring(0, 7));
        }
        Formatter formatter = new Formatter(Locale.ENGLISH);
        formatter.format("Date: %1$ta %1$tb %1$td %1$tT %1$tY %1$tz", input.getTimestamp());
        System.out.println(formatter);
        System.out.println(input.getMessage());
        System.out.println();
    }

    private static void printBranches() {
        System.out.println("=== Branches ===");
        System.out.println("*" + nowBranch);
        String[] branches = new String[branch2Commit.size() - 1];
        int i = 0;
        for (String s : branch2Commit.keySet()) {
            if (!s.equals(nowBranch)) {
                branches[i] = s;
                i++;
            }
        }
        lexiStringListOutput(branches);
        System.out.println();
    }

    private static void stagedFiles() {
        System.out.println("=== Staged Files ===");

        String[] files = addBlobs.keySet().toArray(new String[0]);
        lexiStringListOutput(files);
        System.out.println();
    }

    private static void removed() {
        System.out.println("=== Removed Files ===");

        String[] removal = removalStage.toArray(new String[0]);
        lexiStringListOutput(removal);
        System.out.println();
    }

    private static void notStaged() {
        System.out.println("=== Modifications Not Staged For Commit ===");
        //如果不存在 赋 deleted
        //如果存在 造一个blob 若 sha1 不同赋 modified nowHead
        List<String> strings = new ArrayList<>();
        HashMap<String, String> mp = nowHead.getFileNames();
        for (String s : mp.keySet()) {
            File file = join(CWD, s);
            if (!file.exists()) {
                if (removalStage.contains(s)) {
                    continue;
                }
                strings.add(s + " (deleted)");
            } else {
                Blob tmp = new Blob(file);
                if (!mp.get(s).equals(tmp.getSHA1())) {
                    if (addBlobs.containsKey(s)&&addBlobs.get(s).equals(tmp.getSHA1())) {
                        continue;
                    }
                    strings.add(s + " (modified)");
                }
            }
        }
        lexiStringListOutput(strings.toArray(new String[0]));
        System.out.println();
    }

    private static void untracked() {
        System.out.println("=== Untracked Files ===");
        HashMap<String, String> mp = nowHead.getFileNames();
        List<String> strings = new ArrayList<>();
        List<String> lst = Utils.plainFilenamesIn(CWD);
        for (String str : lst) {
            if (((!mp.containsKey(str) || removalStage.contains(str)) && (!addBlobs.containsKey(str)))) {
                strings.add(str);
            }
        }
        lexiStringListOutput(strings.toArray(new String[0]));
        System.out.println();
    }

    private static void lexiStringListOutput(String[] in) {
        Arrays.sort(in);
        for (String s : in) {
            System.out.println(s);
        }
    }

    private static void initFolders() {
        GITLET_DIR.mkdir();
        COMMIT_DIR.mkdir();
        BLOB_DIR.mkdir();
        ADD_STORAGE.mkdir();
    }

    private static void refreshFile(String archiveSHA1, String fileName) throws IOException {
        Blob archive = readBlob(archiveSHA1);
        File file = join(CWD, fileName);
        if (file.exists()) {
            file.delete();
        }
        file.createNewFile();
        Utils.writeContents(file, archive.getContent());
    }

    private static Commit findDesignatedCmt(String shortSHA1) {
        List<String> lst = Utils.plainFilenamesIn(COMMIT_DIR);
        for (String fullID : lst) {
            if (fullID.startsWith(shortSHA1)) {
                return readCommit(fullID);
            }
        }
            System.out.println("No commit with that id exists.");
            System.exit(0);
            return null;
    }

    private static void checkOutCommitHelper(Commit cmt, String filename) throws IOException  {
        if (!cmt.fileExists(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
        String fileSHA1 = cmt.fileContent(filename);
        refreshFile(fileSHA1, filename);
    }

    private static boolean allowCheckout(Commit bch) {
        for (String s : bch.getFileNames().keySet()) {
            File file = join(CWD, s);
            if (file.exists() && !nowHead.fileExists(s)) {
                return false;
            }
        }
        return true;
    }

    private static void clearNowWorkDirectory() {
        for (String s : nowHead.getFileNames().keySet()) {
            Utils.restrictedDelete(s);
        }
    }

    private static void rebuildNowHead(Commit cmt) throws IOException {
        //将head设为新的branch
        nowHead = cmt;
        HashMap<String, String> tmp = nowHead.getFileNames();
        for (String s : tmp.keySet()) {
            String sha1 = tmp.get(s);
            refreshFile(sha1, s);
        }
    }

    private static void resetOrCheckout(Commit targetCommit) throws IOException {
        if (!allowCheckout(targetCommit)) {
            System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
            System.exit(0);
        }
        clearNowWorkDirectory();
        rebuildNowHead(targetCommit);
        clearStorage();
        removalDelete();
    }

    private static Commit getLatestCommonCommit(Commit cmt1, Commit cmt2) {
        //记录一个的路径 在另一个走的过程中不断地查找
        HashSet<String> route = new HashSet<>();
        while(true) {
            route.add(cmt1.getSHA1());
            if (cmt1.getParent() == null) {
                break;
            }
            cmt1 = readCommit(cmt1.getParent());
        }
        while(true) {
            if (route.contains(cmt2.getSHA1())) {
                return cmt2;
            }
            cmt2 = readCommit(cmt2.getParent());
        }
    }

    private static void mergeThreeCommit(Commit split, Commit current, Commit given, String branchName) throws IOException {
        Set<String> allFiles = new HashSet<>();
        allFiles.addAll(split.getFileNames().keySet());
        allFiles.addAll(current.getFileNames().keySet());
        allFiles.addAll(given.getFileNames().keySet());
        HashMap<String, String> splitFiles = split.getFileNames();
        HashMap<String, String> currentFiles = current.getFileNames();
        HashMap<String, String> givenFiles = given.getFileNames();
        //先挑选需要修改或删除的情况 check 一遍

        for (String s : allFiles) {
            if (checkThreeMorD(split, current, given, s)) {
                if (checkIfUntracked(current, s)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }

        Commit mergeCommit = new Commit(current, given, "Merged " + branchName + " into " + nowBranch + ".");
        for (String s : allFiles) {
            boolean splitExist = splitFiles.containsKey(s);
            boolean givenExist = givenFiles.containsKey(s);
            boolean currentExist = currentFiles.containsKey(s);
            String sps = splitFiles.get(s);
            String gs = givenFiles.get(s);
            String cs = currentFiles.get(s);
            //处理最特殊的情况
            nowHead = mergeCommit;
            if (splitExist && givenExist && currentExist) {
                if (sps.equals(cs) && (!sps.equals(gs))) {
                    //1. split 和current同 和 given 不同 checkout到given 并add
                    checkout(given.getSHA1(), s);
                    addHelper(s);
                    continue;
                }
                //2.
                if (sps.equals(gs) && (!sps.equals(cs))) {
                    addHelper(s);
                    continue;
                }
                //
            } else if (!splitExist) {
                if ((!givenExist) && currentExist) {
                    //4. split 没有 只有 current 有: 保持原状
                    addHelper(s);
                    continue;
                }
                if ((!currentExist) && givenExist) {
                    //5. split 没有 只有 given 有 checkout & staged
                    Commit nowCommit = findDesignatedCmt(given.getSHA1());
                    checkOutCommitHelper(nowCommit, s);
                    addHelper(s);
                    continue;
                }
            }
            //3. 只考虑 sp 和 cs相同
            if (splitExist && currentExist) {
                if (sps.equals(cs)) {
                    if (givenExist) {
                        //3.
                        addHelper(s);
                    }
                    //6.
                    continue; //直接continue
                }
            }
            //7.
            if (splitExist && givenExist && (!currentExist)) {
                if (sps.equals(gs)) {
                    continue;
                }
            }
            //8. conflict
            // 都变了
            if ((currentExist && givenExist && (!cs.equals(gs)))) {
                Blob currentBlob = readBlob(cs);
                Blob givenBlob = readBlob(gs);
                Utils.restrictedDelete(s);
                File sf = join(CWD, s);
                sf.createNewFile();
                List<Object> lst = new ArrayList<>();
                lst.add(new String("<<<<<<< HEAD\n"));
                lst.add(currentBlob.getContent());                lst.add(new String("=======\n"));
                lst.add(givenBlob.getContent());
                lst.add(new String(">>>>>>>\n"));
                Utils.writeContents(sf, lst.toArray());
                addHelper(s);
                continue;
            }
            //一个删了 另一个 变了
            if ((!currentExist) || (!givenExist)) {
                Blob valid = currentExist ? readBlob(cs) : readBlob(gs);
                Utils.restrictedDelete(s);
                File sf = join(CWD, s);
                sf.createNewFile();
                Utils.writeContents(sf, valid.getContent());;
                addHelper(s);
            }
        }
        //按需合并

        for (String s : allFiles) {
            if(!addBlobs.containsKey(s)) {
                File f = join(CWD, s);
                if (f.exists()) {
                    f.delete();
                }
            }
        }
        clearStorage();
        removalDelete();
        nowHead.setSHA1();
        branch2Commit.put(nowBranch, nowHead.getSHA1());
        setStatus();

    }

    //已知该文件是修改的文件 如果文件在 目录中 且没有被 跟踪 那么就

    private static boolean checkIfUntracked(Commit cmt, String fileName) {
        File file = Utils.join(CWD, fileName);
        return file.exists() && (!cmt.fileExists(fileName));
    }

    private static boolean checkThreeMorD(Commit split, Commit current, Commit given, String fileName) {
        HashMap<String, String> splitFiles = split.getFileNames();
        HashMap<String, String> currentFiles = current.getFileNames();
        HashMap<String, String> givenFiles = given.getFileNames();
        boolean splitExist = splitFiles.containsKey(fileName);
        boolean givenExist = givenFiles.containsKey(fileName);
        boolean currentExist = currentFiles.containsKey(fileName);
        String sps = splitFiles.get(fileName);
        String gs = givenFiles.get(fileName);
        String cs = currentFiles.get(fileName);
        if (splitExist && givenExist && currentExist) {
            return (sps.equals(cs) && (!sps.equals(gs)));
        } else if ((!givenExist) && splitExist && currentExist) {
            return sps.equals(cs);
        }
        return false;
    }
}
