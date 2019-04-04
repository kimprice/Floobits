package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.common.API;
import floobits.common.EditorEventHandler;
import floobits.common.interfaces.IContext;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.utilities.Flog;

public abstract class IsJoinedAction extends RequiresAccountAction {

    public abstract void actionPerformed(AnActionEvent e, EditorEventHandler editorEventHandler, FloobitsPlugin floobitsPlugin);

    @Override
    public void actionPerformedHasAccount(AnActionEvent e) {
        FloobitsPlugin floobitsPlugin;
        FlooHandler flooHandler;
        IContext context = null;
        try {
            floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
            if (floobitsPlugin == null) {
                Flog.log("no floobits plugin, aborting.");
                return;
            }
            context = floobitsPlugin.context;
            flooHandler = context.getFlooHandler();
            if (flooHandler == null) {
                context.errorMessage("You must join a workspace to perform this action.");
                return;
            }
            actionPerformed(e, flooHandler.editorEventHandler, floobitsPlugin);
        } catch (Throwable throwable) {
            API.uploadCrash(context, throwable);
        }
    }

    @Override
    public void update(AnActionEvent e) {
        super.update(e);
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        if (floobitsPlugin == null) {
            return;
        }
        e.getPresentation().setEnabled(isEnabled(floobitsPlugin));
    }

    protected boolean isEnabled(FloobitsPlugin floobitsPlugin) {
        return floobitsPlugin.context.isJoined();
    }
}
