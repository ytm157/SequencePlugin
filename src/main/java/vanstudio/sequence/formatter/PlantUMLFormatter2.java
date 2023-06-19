package vanstudio.sequence.formatter;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import vanstudio.sequence.config.SequenceSettingsState;
import vanstudio.sequence.diagram.*;
import vanstudio.sequence.openapi.Constants;
import vanstudio.sequence.openapi.model.CallStack;
import vanstudio.sequence.openapi.model.ClassDescription;
import vanstudio.sequence.openapi.model.MethodDescription;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Generate <a href="https://plantuml.com/sequence-diagram">PlantUml sequence diagram</a> format.
 */
public class PlantUMLFormatter2 implements IFormatter {
    private static final Logger LOGGER = Logger.getInstance(PlantUMLFormatter2.class);

    private SequenceSettingsState sequenceSettingsState;
    private java.util.List<String> methodColors = new ArrayList<>();
    private java.util.List<ObjectInfo> objectInfos = new ArrayList<>();
    private Project project;
    private PsiElement psiElement;

    public PlantUMLFormatter2(Project project, PsiElement psiMethod, Model model) {
        this.sequenceSettingsState = SequenceSettingsState.getInstance();
        this.project = project;
        this.psiElement = psiMethod;
        methodColors.add(toHexColorString(sequenceSettingsState.METHOD_BAR_COLOR));
        methodColors.add("#00FFFF");
        methodColors.add("#8FBC8F");
        methodColors.add("#FFFAF0");
        methodColors.add("#DAA520");

        Parser p = new Parser();
        try {
            p.parse(model.getText());
        } catch (IOException ioe) {
            LOGGER.error("IOException", ioe);
            return;
        }
        objectInfos = p.getObjects();
    }

    @Override
    public String format(CallStack callStack) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("@startuml").append('\n')
                .append("skinparam defaultFontName 微软雅黑").append("\n")
                .append("autoactivate on").append("\n")
                .append("autonumber").append("\n");

        // 将class放在一起
        buffer.append("box Class\n");
        for (ObjectInfo obj : objectInfos) {
            buffer.append("  participant ").append(obj.getName()).append("\n");
            buffer.append(String.format("  url of %s is [[%s]]",obj.getName(), obj.getAbsPath())).append("\n");
        }
        buffer.append("end box\n\n");


        ClassDescription classDescription = callStack.getMethod().getClassDescription();
        String classA = classDescription.getClassShortName();
        String method = getMethodName(callStack.getMethod());
        if (Constants.CONSTRUCTOR_METHOD_NAME.equals(callStack.getMethod().getMethodName())) {
            buffer.append("create ").append(classA).append('\n');
        }
        buffer.append("Actor").append(" -> ").append(classA).append(methodColors.get(0))
                .append(" : ")
                // 形如 [[D:/xx/LoginController.java#loginSimple loginSimple]]
                .append(String.format("[[%s#%s %s]]", classDescription.getAbsPath(), method, method))
                .append('\n');
//        buffer.append("activate ").append(classA).append('\n');
        generate(buffer, callStack);
        buffer.append("return").append('\n');
        buffer.append("@enduml");
        return buffer.toString();
    }

    /**
     * @param color
     * @return #FFE0A7
     */
    private static String toHexColorString(Color color) {
        StringBuilder hex = new StringBuilder(Integer.toHexString(color.getRGB() & 0xFFFFFF));
        while (hex.length() < 6) {
            hex.insert(0, "0");
        }
        return "#" + hex.toString().toUpperCase();
    }

    // 自我调用计数
    Map<String, Integer> selfCallCount = new HashMap<>();

    private void generate(StringBuffer buffer, CallStack parent) {
        ClassDescription classDescriptionA = parent.getMethod().getClassDescription();
        String classA = classDescriptionA.getClassShortName();
        // 是从哪个方法调用的
        String methodA = parent.getMethod().getMethodName();
        if (!selfCallCount.containsKey(classA)) {
            selfCallCount.put(classA, 0);
        }
        for (CallStack callStack : parent.getCalls()) {
            ClassDescription classDescriptionB = callStack.getMethod().getClassDescription();
            String classB = classDescriptionB.getClassShortName();
            String method = getMethodName(callStack.getMethod());
            if (Constants.CONSTRUCTOR_METHOD_NAME.equals(callStack.getMethod().getMethodName())) {
                buffer.append("create ").append(classB).append('\n');
            }
            String methodColor = methodColors.get(0);
            if (classA.equals(classB)) {
                Integer count = selfCallCount.get(classA);
                count++;
                selfCallCount.put(classA, count);
                // 将count映射到[x,y]
                int x = 1;
                int y = methodColors.size() - 1;
                int mappedValue = (count - x) % y + 1;
                methodColor = methodColors.get(mappedValue);
            }
            buffer.append(classA).append(" -> ").append(classB).append(methodColor)
                    .append(" : ")
//                    .append(method)
                    // 形如 [[D:/xx/LoginController.java#loginSimple loginSimple]]
                    .append(String.format("[[%s#%s %s]]", classDescriptionB.getAbsPath(), method, method))
                    .append('\n');
//            buffer.append("activate ").append(classB).append('\n');
            generate(buffer, callStack);
            buffer.append(classB).append(" --> ").append(classA)
                    .append(" : ")
                    // 形如 [[D:/xx/LoginController.java#loginSimple return loginSimple]]
                    .append(String.format("return [[%s#%s %s]]", classDescriptionA.getAbsPath(), methodA, methodA))
                    .append('\n');
        }

    }

    private String getMethodName(MethodDescription method) {
        if (method == null) return "";

        if (SequenceSettingsState.getInstance().SHOW_SIMPLIFY_CALL_NAME) {
            return method.getMethodName();
        } else {
            return method.getFullName();
        }

    }
}
