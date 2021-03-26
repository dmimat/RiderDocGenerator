import com.intellij.ide.plugins.IdeaPluginDescriptor;
import com.intellij.ide.plugins.PluginManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.vfs.VirtualFile;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;

/**
 * Created by Dmitry.Matveev on 22-Jun-17.
 */
//public class DumpBundledPlugins extends AnAction {
//    @Override
//    public void actionPerformed(AnActionEvent anActionEvent) {
//
//        FileChooserDescriptor descriptor =
//                new FileChooserDescriptor(false, true,
//                        false, false, false, false);
//        descriptor.setDescription("Select where to save the list of bundled plugins");
//        String path = FileChooser.chooseFile(descriptor, null, null).getPath();
//
//        IdeaPluginDescriptor[] plugins = PluginManager.getPlugins();
//
//        try {
//            PrintWriter pw = new PrintWriter(new FileOutputStream(path + "\\Rider.Bundled.Plugins.txt"));
//            for (IdeaPluginDescriptor plugin : plugins){
//                if(plugin.isBundled()){
//                    pw.println(plugin.getName());
//                }
//            }
//            pw.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
//
//    }
//}
