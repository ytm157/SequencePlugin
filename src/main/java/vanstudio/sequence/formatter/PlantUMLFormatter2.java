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
            // class interface 以及自定义的class color，分别使用不同的颜色
            buffer.append("  participant ").append(obj.getName()).append(determineBackgroundPaintForObject(obj)).append("\n");
            buffer.append(String.format("  url of %s is [[%s]]", obj.getName(), obj.getAbsPath())).append("\n");
        }
        buffer.append("end box\n\n");

        MethodDescription methodDescription = callStack.getMethod();
        ClassDescription classDescription = methodDescription.getClassDescription();
        String classA = classDescription.getClassShortName();
        String method = getMethodName(methodDescription);
        if (Constants.CONSTRUCTOR_METHOD_NAME.equals(methodDescription.getMethodName())) {
            buffer.append("create ").append(classA).append('\n');
        }
        buffer.append("Actor").append(" -> ").append(classA).append(methodColors.get(0))
                .append(" : ")
                // 形如 [[D:/xx/LoginController.java#loginSimple loginSimple(String)]]
                .append(String.format("[[%s#%s %s]]", classDescription.getAbsPath(), methodDescription.getMethodName(), method))
                .append('\n');
        generate(buffer, callStack);
        buffer.append(classA).append(" --> Actor : return\n");
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
        MethodDescription methodDescriptionA = parent.getMethod();
        ClassDescription classDescriptionA = methodDescriptionA.getClassDescription();
        String classA = classDescriptionA.getClassShortName();
        // 是从哪个方法调用的
        String methodA = methodDescriptionA.getMethodName();
        if (!selfCallCount.containsKey(classA)) {
            selfCallCount.put(classA, 0);
        }
        for (CallStack callStack : parent.getCalls()) {
            MethodDescription methodDescriptionB = callStack.getMethod();
            ClassDescription classDescriptionB = methodDescriptionB.getClassDescription();
            String classB = classDescriptionB.getClassShortName();
            String method = getMethodName(methodDescriptionB);
            if (Constants.CONSTRUCTOR_METHOD_NAME.equals(methodDescriptionB.getMethodName())) {
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
                    // 形如 [[D:/xx/LoginController.java#loginSimple loginSimple(String)]]
                    .append(String.format("[[%s#%s %s]]", classDescriptionB.getAbsPath(), methodDescriptionB.getMethodName(), method))
                    .append('\n');
            generate(buffer, callStack);
            buffer.append(classB).append(" --> ").append(classA)
                    .append(" : ")
                    // 形如 void to [[D:/xx/LoginController.java#loginSimple return loginSimple]]
                    .append(String.format("%s to [[%s#%s %s]]", methodDescriptionB.getReturnTypeShort(), classDescriptionA.getAbsPath(), methodDescriptionA.getMethodName(), methodA))
                    .append('\n');
        }

    }

    private String getMethodName(MethodDescription method) {
        if (method == null) return "";

        if (SequenceSettingsState.getInstance().SHOW_SIMPLIFY_CALL_NAME) {
            return method.getMethodName();
        } else {
            return method.getFullName2();
        }

    }

    /**
     * @see vanstudio.sequence.diagram.DisplayObject#determineBackgroundPaintForObject
     * @param objectInfo
     * @return
     */
    private String determineBackgroundPaintForObject(ObjectInfo objectInfo) {
        return objectInfo.hasAttribute(Info.EXTERNAL_ATTRIBUTE)
                ? toHexColorString(sequenceSettingsState.EXTERNAL_CLASS_COLOR)
                : objectInfo.hasAttribute(Info.INTERFACE_ATTRIBUTE)
                ? toHexColorString(sequenceSettingsState.INTERFACE_COLOR)
                : toHexColorString(sequenceSettingsState.CLASS_COLOR);
    }
}
