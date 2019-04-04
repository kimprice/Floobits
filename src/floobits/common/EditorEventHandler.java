package floobits.common;

import floobits.common.interfaces.IContext;
import floobits.common.interfaces.IDoc;
import floobits.common.interfaces.IFactory;
import floobits.common.interfaces.IFile;
import floobits.common.protocol.buf.Buf;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.common.protocol.json.receive.FlooHighlight;
import floobits.utilities.Flog;

import java.util.ArrayList;
import java.util.HashSet;

public class EditorEventHandler {
    private final IContext context;
    public final FloobitsState state;
    private final OutboundRequestHandler outbound;
    private final InboundRequestHandler inbound;

    public EditorEventHandler(IContext context, FloobitsState state, OutboundRequestHandler outbound, InboundRequestHandler inbound) {
        this.context = context;
        this.state = state;
        this.outbound = outbound;
        this.inbound = inbound;
    }

    public void createFile(final IFile virtualFile) {
        if (context.isIgnored(virtualFile)) {
            return;
        }
        context.setTimeout(100, new Runnable() {
            @Override
            public void run() {
                context.readThread(new Runnable() {
                    @Override
                    public void run() {
                        FlooHandler flooHandler = context.getFlooHandler();
                        if (flooHandler == null) {
                            return;
                        }
                        flooHandler.editorEventHandler.upload(virtualFile);
                    }
                });
            }
        });
    }

    public void go() {
        context.listenToEditor(this);
    }

    public void rename(String path, String newPath) {
        if (!state.can("patch")) {
            return;
        }
        Flog.log("Renamed buf: %s - %s", path, newPath);
        Buf buf = state.getBufByPath(path);
        if (buf == null) {
            Flog.info("buf does not exist.");
            return;
        }
        String newRelativePath = context.toProjectRelPath(newPath);
        if (newRelativePath == null) {
            Flog.warn(String.format("%s was moved to %s, deleting from workspace.", buf.path, newPath));
            outbound.deleteBuf(buf, true);
            return;
        }
        if (buf.path.equals(newRelativePath)) {
            Flog.info("rename handling workspace rename, aborting.");
            return;
        }
        outbound.renameBuf(buf, newRelativePath);
    }

    public void change(IFile file) {
        String filePath = file.getPath();
        if (!state.can("patch")) {
            return;
        }
        if (!context.isShared(filePath)) {
            return;
        }
        state.pauseFollowing(true);
        final Buf buf = state.getBufByPath(filePath);
        if (buf == null) {
            return;
        }
        synchronized (buf) {
            if (Buf.isBad(buf)) {
                Flog.info("buf isn't populated yet %s", file.getPath());
                return;
            }
            buf.send_patch(file);
        }
    }

    public void changeSelection(String path, ArrayList<ArrayList<Integer>> textRanges, boolean following) {
        Buf buf = state.getBufByPath(path);
        outbound.highlight(buf, textRanges, false, following);
    }

    public void save(String path) {
        Buf buf = state.getBufByPath(path);
        outbound.saveBuf(buf);
    }

    public void softDelete(HashSet<String> files) {
        if (!state.can("patch")) {
            return;
        }

        for (String path : files) {
            Buf buf = state.getBufByPath(path);
            if (buf == null) {
                context.warnMessage(String.format("The file, %s, is not in the workspace.", path));
                continue;
            }
            outbound.deleteBuf(buf, false);
        }
    }

    void delete(String path) {
        Buf buf = state.getBufByPath(path);
        if (buf == null) {
            return;
        }
        outbound.deleteBuf(buf, true);
    }

    public void deleteDirectory(ArrayList<String> filePaths) {
        if (!state.can("patch")) {
            return;
        }

        for (String filePath : filePaths) {
            delete(filePath);
        }
    }

    public void msg(String chatContents) {
        outbound.message(chatContents);
    }

    public void kick(int userId) {
        outbound.kick(userId);
    }

    public void changePerms(int userId, String[] perms) {
        outbound.setPerms("set", userId, perms);
    }

    public void upload(IFile virtualFile) {
        if (state.readOnly) {
            return;
        }
        if (!virtualFile.isValid()) {
            return;
        }
        String path = virtualFile.getPath();
        Buf b = state.getBufByPath(path);
        if (b != null) {
            Flog.info("Already in workspace: %s", path);
            return;
        }
        outbound.createBuf(virtualFile);
    }

    public boolean follow() {
        boolean mode = !state.getFollowing();
        state.setFollowing(mode);
        context.statusMessage(String.format("%s follow mode", mode ? "Enabling" : "Disabling"));
        if (mode) {
            goToLastHighlight();
        }
        return mode;
    }

    public void goToLastHighlight() {
        goToHighlight(state.lastHighlight);
    }

    public void goToLastHighlight(String username) {
        FlooHighlight hl = state.lastUserHighlights.get(username);
        if (hl == null) {
            context.statusMessage(String.format("%s has no previous highlight to go to.", username));
            return;
        }
        goToHighlight(hl);
        context.statusMessage(String.format("Jumped to %s's last highlight.", username));
    }

    public void goToHighlight(FlooHighlight highlight) {
        if (highlight == null) {
            return;
        }
        FlooHighlight newHighlight = new FlooHighlight(highlight.id, highlight.ranges, true,
                highlight.following, highlight.user_id);
        inbound._on_highlight(newHighlight);
    }

    public void summon(String path, Integer offset, String username) {
        outbound.summon(path, offset, username);
    }

    public void sendEditRequest() {
        outbound.requestEdit();
    }

    public void beforeChange(IDoc doc) {
        final IFile virtualFile = doc.getVirtualFile();
        final String path = virtualFile.getPath();
        final Buf bufByPath = state.getBufByPath(path);
        if (bufByPath == null) {
            return;
        }
        String msg;
        if (state.readOnly) {
            msg = "This document is readonly because you don't have edit permission in the workspace.";
        } else if (!bufByPath.isPopulated()) {
            msg = "This document is temporarily readonly while we fetch a fresh copy.";
        } else {
            return;
        }
        context.statusMessage(msg);
        doc.setReadOnly(true);
        IFactory.readOnlyBufferIds.add(bufByPath.path);
        final String text = doc.getText();
        // This setTimeout is important, don't remove. Might have something to do with re-entrant threads.
        context.setTimeout(0, new Runnable() {
            @Override
            public void run() {
                context.writeThread(new Runnable() {
                    @Override
                    public void run() {
                        if (!state.readOnly && bufByPath.isPopulated()) {
                            return;
                        }
                        synchronized (context) {
                            try {
                                context.setListener(false);
                                IDoc d = context.iFactory.getDocument(virtualFile);
                                if (d == null) {
                                    return;
                                }
                                d.setReadOnly(false);
                                d.setText(text);
                                d.setReadOnly(true);
                            } catch (Throwable e) {
                                Flog.error(e);
                            } finally {
                                context.setListener(true);
                            }
                        }
                    }
                });
            }
        });
    }
}
