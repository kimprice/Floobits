package floobits;
import floobits.common.API;
import floobits.common.FlooUserDetail;
import floobits.common.FloobitsState;
import floobits.common.interfaces.IContext;

import java.io.BufferedWriter;
import java.io.IOException;
import java.time.Instant;
import java.io.FileWriter;
import java.util.Calendar;
import java.util.Date;

public class Log {

    public enum LogType {
        CHAT_MESSAGE, UI_ACTION, STATUS_MESSAGE, ERROR_MESSAGE
    }

    public static void toTextFile(IContext context, FloobitsState state, LogType type, String message) {
        Long timestamp = Instant.now().toEpochMilli();
        Date date = new Date();
        FlooUserDetail flooUserDetail = API.getUserDetail(context, state);
        String sep = " *|* ";
        try {
            // need to define this file writer elsewhere and then add the contents by calling a method
            //System.out.println("Working Directory = " + System.getProperty("user.dir"));
            // this file is being written to /Applications/IntelliJ IDEA CE.app/Contents/bin
            BufferedWriter writer = new BufferedWriter(new FileWriter("actionLogs.txt", true));
            writer.write("\n" + timestamp + sep + date + sep + type.name() + sep + flooUserDetail.username + sep + message);
            // System.out.println("\n" + timestamp + sep + date + sep + type.name() + sep + flooUserDetail.username + sep + message);
            writer.close(); //need to move this
        } catch (IOException ex) {
            // System.out.println("caught an IOException");
        }
    }

    public static void toTextFile(Date date, String username, LogType type, String message) {
        Long timestamp = Instant.now().toEpochMilli();
        String sep = " *|* ";
        try {
            // this file is being written to /Applications/IntelliJ IDEA CE.app/Contents/bin
            BufferedWriter writer = new BufferedWriter(new FileWriter("actionLogs.txt", true));
            writer.write("\n" + timestamp + sep + date + sep + type.name() + sep + username + sep + message);
            // System.out.println("\n" + timestamp + sep + date + sep + type.name() + sep + username + sep + message);
            writer.close(); //need to move this
        } catch (IOException ex) {
            // System.out.println("caught an IOException");
        }
    }
    // call this function if unable to get username, context, or state
    public static void toTextFile(LogType type, String message) {
        Long timestamp = Instant.now().toEpochMilli();
        Date date = new Date();
        String sep = " *|* ";
        try {
            // this file is being written to /Applications/IntelliJ IDEA CE.app/Contents/bin
            BufferedWriter writer = new BufferedWriter(new FileWriter("actionLogs.txt", true));
            writer.write("\n" + timestamp + sep + date + sep + type.name() + sep + "Anonymous" + sep + message);
            // System.out.println("\n" + timestamp + sep + date + sep + type.name() + sep + "Anonymous" + sep + message);
            writer.close(); //need to move this
        } catch (IOException ex) {
            // System.out.println("caught an IOException");
        }
    }
}
