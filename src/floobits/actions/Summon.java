package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import floobits.FloobitsPlugin;
import floobits.common.EditorEventHandler;
import floobits.impl.ContextImpl;
import floobits.impl.FactoryImpl;


public class Summon extends IsJoinedAction {

    @Override
    public void actionPerformed(AnActionEvent e, final EditorEventHandler editorEventHandler, FloobitsPlugin floobitsPlugin) {
        final Editor editor = PlatformDataKeys.EDITOR.getData(e.getDataContext());
        summon(editor, editorEventHandler, null, floobitsPlugin.context);
    }

    public static void summon(Editor editor, EditorEventHandler editorEventHandler, String username, ContextImpl context) {
        if (editor == null) {
            return;
        }
        Document document = editor.getDocument();
        FactoryImpl iFactory = (FactoryImpl) context.iFactory;
        String path = iFactory.getPathForDoc(document);
        if (path == null) {
            return;
        }
        int offset = editor.getCaretModel().getOffset();
        editorEventHandler.summon(path, offset, username);
    }
}
