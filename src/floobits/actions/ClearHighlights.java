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
            Log.toTextFile(floobitsPlugin.context, floobitsPlugin.context.getFlooHandler().state, Log.LogType.UI_ACTION, "Cleared highlights" );
/*            String timestamp = Instant.now().toString();
            try {
                // need to define this file writer elsewhere and then add the contents by calling a method
                //System.out.println("Working Directory = " + System.getProperty("user.dir"));
                // this file is being written to /Applications/IntelliJ IDEA CE.app/Contents/bin
                BufferedWriter writer = new BufferedWriter(new FileWriter("actionLogs.txt", true));
                writer.write("\n" + timestamp + "\tUser1\t cleared highlights");
                System.out.println("\n" + timestamp + "\tUser1\t cleared highlights");

                writer.close(); //need to move this
            } catch (IOException ex) {
                System.out.println("caught an IOException");

            }*/
        }
    }
}
