package floobits.dialogs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.ui.DialogWrapper;
import floobits.common.RunLater;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.util.List;

public class SelectOwner extends DialogWrapper {
    private JPanel jPanel;
    private RunLater<String> runLater;
    private JComboBox orgList;

    public static void build(final List<String> title, final RunLater<String> runLater) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                SelectOwner SelectOwner = new SelectOwner(title, runLater);
                SelectOwner.createCenterPanel();
                SelectOwner.show();
            }
        });
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return jPanel;
    }

    protected SelectOwner(List<String> title, RunLater<String> runLater) {
        super(true);
        jPanel = new JPanel();
        this.runLater = runLater;
        init();
        this.setTitle("Select Workspace Owner");

        Object[] organizations = title.toArray();
        orgList = new ComboBox(organizations, 200);
        if (organizations.length > 0) {
            orgList.setSelectedIndex(0);
        }
        JLabel jLabel = new JLabel();
        jLabel.setText("Select the owner for the workspace:");
        jPanel.add(jLabel);
        jPanel.add(orgList);
    }

    @Override
    public void doCancelAction() {
        super.doCancelAction();
    }

    @Override
    protected void doOKAction() {
        super.doOKAction();
        if(orgList == null) {
            return;
        }
        runLater.run((String) orgList.getSelectedItem());
    }
}
