package edu.uoc.allago.uocsubmissionsystem;

import com.intellij.ide.plugins.PluginManagerCore;
import com.intellij.openapi.extensions.PluginId;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.Nullable;
import javax.swing.*;

public class AboutDialog extends DialogWrapper {
    private JPanel contentPane;
    private JLabel pluginVersion;

    public AboutDialog() {
        // current window as parent
        super(true);
        setTitle("About this plugin");
        init();
    }
    @Override
    protected @Nullable JComponent createCenterPanel() {
        // version
        String version =
                PluginManagerCore.getPlugin(PluginId.getId("edu.uoc.allago.UOCSubmissionSystem")).getVersion();
        pluginVersion.setText("Version " + version);
        return contentPane;
    }
    protected Action[] createActions() {
        return new Action[]{getOKAction()
        };
    }
}
