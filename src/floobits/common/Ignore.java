package floobits.common;

import floobits.common.interfaces.IContext;
import floobits.common.interfaces.IFile;
import floobits.common.jgit.ignore.IgnoreNode;
import floobits.common.jgit.ignore.IgnoreRule;
import floobits.utilities.Flog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class Ignore implements Comparable<Ignore>{
    static final HashSet<String> IGNORE_FILES = new HashSet<String>(Arrays.asList(".gitignore", ".hgignore", ".flignore", ".flooignore"));
    static final ArrayList<String> DEFAULT_IGNORES = new ArrayList<String>(Arrays.asList("extern", "node_modules", "tmp", "vendor", ".idea/workspace.xml", ".idea/misc.xml"));
    public static final int MAX_FILE_SIZE = 1024 * 1024 * 5;
    public final IFile file;
    public final String stringPath;
    public final HashMap<String, Ignore> children = new HashMap<String, Ignore>();
    public final ArrayList<IFile> files = new ArrayList<IFile>();
    public int size = 0;
    private final IgnoreNode ignoreNode = new IgnoreNode();

    public class UploadData {
        public HashMap<String, Integer> bigStuff;
        public HashSet<String> paths;
        public UploadData(HashMap<String, Integer> bigStuff, HashSet<String> paths) {
            this.bigStuff = bigStuff;
            this.paths = paths;
        }
    }

    public static Ignore BuildIgnore(IFile virtualFile) {
        Ignore ig = new Ignore(virtualFile);
        // TODO: add more hard-coded ignores
        ig.ignoreNode.addRule(new IgnoreRule(".idea/workspace.xml"));
        ig.ignoreNode.addRule(new IgnoreRule(".idea/misc.xml"));
        ig.ignoreNode.addRule(new IgnoreRule(".git"));
        ig.ignoreNode.addRule(new IgnoreRule(".svn"));
        ig.ignoreNode.addRule(new IgnoreRule(".hg"));
        ig.recurse(ig, virtualFile.getPath());
        return ig;
    }

    public static void writeDefaultIgnores(IContext context) {
        Flog.log("Creating default ignores.");
        String path = FilenameUtils.concat(context.colabDir, ".flooignore");

        try {
            File f = new File(path);
            if (f.exists()) {
                return;
            }
            FileUtils.writeLines(f, DEFAULT_IGNORES);
        } catch (IOException e) {
            Flog.error(e);
        }
    }

    public static boolean isIgnoreFile(IFile virtualFile) {
        return virtualFile != null && IGNORE_FILES.contains(virtualFile.getName()) && virtualFile.isValid();
    }

    public Boolean isIgnored(IFile f, String relPath) {
        if (isFlooIgnored(f, f.getPath())) {
            Flog.log("Ignoring %s just because.", f.getPath());
            return true;
        }
        relPath = FilenameUtils.separatorsToUnix(relPath);
        return !relPath.equals(stringPath) && isGitIgnored(relPath, f.isDirectory());
    }

    private Ignore(IFile virtualFile) {
        file = virtualFile;
        stringPath = FilenameUtils.separatorsToUnix(virtualFile.getPath());
        Flog.debug("Initializing ignores for %s", file);
        for (IFile vf : file.getChildren()) {
            addRules(vf);
        }
    }

    protected void addRules(IFile virtualFile) {
        if (!isIgnoreFile(virtualFile)) {
            return;
        }

        InputStream inputStream = virtualFile.getInputStream();
        if (inputStream != null) {
            try {
                ignoreNode.parse(inputStream);
            } catch (IOException e) {
                Flog.error(e);
            }
        }
    }

    @SuppressWarnings("UnsafeVfsRecursion")
    public void recurse(Ignore root, String rootPath) {
        @SuppressWarnings("UnsafeVfsRecursion") IFile[] fileChildren = file.getChildren();
        for (IFile file : fileChildren) {
            String absPath = file.getPath();
            if (isFlooIgnored(file, absPath))  {
                continue;
            }
            String relPath = FilenameUtils.separatorsToUnix(Utils.toProjectRelPath(absPath, rootPath));

            Boolean isDir = file.isDirectory();
            if (root.isGitIgnored(relPath, isDir)) {
                continue;
            }

            if (isDir) {
                Ignore child = new Ignore(file);
                children.put(file.getName(), child);
                child.recurse(root, rootPath);
                continue;
            }

            files.add(file);
            size += file.getLength();
        }
    }

    /**
    * @param path
    *            the rel path to test. The path must be relative to this ignore
    *            node's own repository path, and in repository path format
    *            (uses '/' and not '\').
    **/
    private boolean isGitIgnored(String path, boolean isDir) {
        IgnoreNode.MatchResult ignored = ignoreNode.isIgnored(path, isDir);
        switch (ignored) {
            case IGNORED:
                Flog.log("Ignoring %s because it is ignored by git.", path);
                return true;
            case NOT_IGNORED:
                return false;
            case CHECK_PARENT:
                break;
        }
        String[] split = path.split("/", 2);
        if (split.length != 2) {
            return false;
        }
        String nextName = split[0];
        path = split[1];
        Ignore ignore = children.get(nextName);
        return ignore != null && ignore.isGitIgnored(path, isDir);
    }

    public boolean isFlooIgnored(IFile virtualFile, String absPath) {
        if (!file.isValid()) {
            return true;
        }
        if (virtualFile.isSpecial() || virtualFile.isSymLink()) {
            Flog.log("Ignoring %s because it is special or a symlink.", absPath);
            return true;
        }
        if (!virtualFile.isDirectory() && virtualFile.getLength() > MAX_FILE_SIZE) {
            Flog.log("Ignoring %s because it is too big (%s)", absPath, virtualFile.getLength());
            return true;
        }
        return false;
    }

    List<Ignore> buildIgnores(Ignore startIgnore, HashSet<Ignore> ignoredIgnores) {
        List<Ignore> ignores = new ArrayList<Ignore>();
        LinkedList<Ignore> tempIgnores = new LinkedList<Ignore>();
        tempIgnores.add(startIgnore);
        Ignore ignore;
        while (tempIgnores.size() > 0) {
            ignore = tempIgnores.removeLast();
            if (ignoredIgnores != null && ignoredIgnores.contains(ignore)) {
                continue;
            }
            ignores.add(ignore);
            for(Ignore ig: ignore.children.values()) {
                tempIgnores.add(ig);
            }
        }
        Collections.sort(ignores);
        return ignores;
    }

    public UploadData getUploadData(Integer maxSize, Utils.FileProcessor<String> fileProcessor) {
        HashMap<String, Integer> bigStuff = new HashMap<String, Integer>();
        HashSet<String> paths = new HashSet<String>();
        HashSet<Ignore> ignoredIgnores = new HashSet<Ignore>();
        List<Ignore> allIgnores = buildIgnores(this, null);
        int totalSize = 0;
        for (Ignore ignore : allIgnores) {
            totalSize += ignore.size;
        }
        while (totalSize > maxSize) {
            Ignore ig = allIgnores.remove(0);
            ignoredIgnores.add(ig);
            totalSize -= ig.size;
            bigStuff.put(ig.file.getPath(), ig.size);
        }
        if (ignoredIgnores.size() > 0) {
            allIgnores = buildIgnores(this, ignoredIgnores);
        }
        for (Ignore ig : allIgnores) {
            for (IFile virtualFile : ig.files)
                paths.add(fileProcessor.call(virtualFile));
        }
        return new UploadData(bigStuff, paths);
    }

    @Override
    public int compareTo(@NotNull Ignore ignore) {
        return ignore.size - size;
    }
}
