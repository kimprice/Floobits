package floobits.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import floobits.FloobitsPlugin;
import floobits.Log;

public class ClearHighlights extends RequiresAccountAction {

    @Override
    protected void actionPerformedHasAccount(AnActionEvent e) {
        FloobitsPlugin floobitsPlugin = FloobitsPlugin.getInstance(e.getProject());
        if (floobitsPlugin != null) {
            floobitsPlugin.context.iFactory.clearHighlights();
            // add logging here
            // if this works the way I think it should then we can get rid of one parameter
            try {
                Log.toTextFile(floobitsPlugin.context, floobitsPlugin.context.getFlooHandler().state, Log.LogType.UI_ACTION, "Cleared highlights" );
            } catch (NullPointerException ex) {
                Log.toTextFile(Log.LogType.UI_ACTION, "Cleared highlights" );
            }

        }
    }
}
