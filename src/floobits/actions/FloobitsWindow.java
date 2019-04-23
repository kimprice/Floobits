package floobits.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.Log;

public class FloobitsWindow extends AnAction {

    @Override
    public void actionPerformed(AnActionEvent e) {
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        if (floobitsPlugin != null) {
            floobitsPlugin.context.toggleFloobitsWindow();
            // add logging here - causes NullPointerException if not connected to a session, so only log when connected
            Log.toTextFile(floobitsPlugin.context, floobitsPlugin.context.getFlooHandler().state, Log.LogType.UI_ACTION, "Toggled Floobits Window");
        }
    }
}
