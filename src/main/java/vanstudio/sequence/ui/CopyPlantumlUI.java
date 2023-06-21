package vanstudio.sequence.ui;

import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.ui.components.JBScrollPane;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;

public class CopyPlantumlUI extends DialogWrapper {
    private JTextArea textArea;

    public CopyPlantumlUI() {
        super(true);
        setOKButtonText("Copy");
        setTitle("Generated Plantuml");
        setResizable(true);
        init();
    }

    @Override
    protected JComponent createCenterPanel() {
        // 创建一个滚动面板，并设置其大小和布局
        JScrollPane scrollPane = new JBScrollPane();
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int width= (int) (screenSize.width*0.5);
        int height=(int)(screenSize.height*0.7);
        scrollPane.setPreferredSize(new Dimension(width,height));
        scrollPane.setLayout(new ScrollPaneLayout());
        textArea = new JTextArea();
        // 将文本区域添加到滚动面板中
        scrollPane.setViewportView(textArea);
        return scrollPane;
    }

    public void setText(String text){
        textArea.setText(text);
    }

    public String getText(){
        return textArea.getText();
    }

    @Override
    protected void doOKAction() {
        // 点击OK按钮时的操作，这里将文本内容复制到剪贴板
        String text = textArea.getText();
        if (!text.isEmpty()) {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(
                    new StringSelection(text), null);
            Messages.showInfoMessage("Content text copied successful!", "Plantuml");
        }
        super.doOKAction();
    }
}
