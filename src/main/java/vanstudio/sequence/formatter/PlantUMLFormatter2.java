package vanstudio.sequence.formatter;

import vanstudio.sequence.config.SequenceSettingsState;
import vanstudio.sequence.openapi.Constants;
import vanstudio.sequence.openapi.model.CallStack;
import vanstudio.sequence.openapi.model.MethodDescription;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Generate <a href="https://plantuml.com/sequence-diagram">PlantUml sequence diagram</a> format.
 */
public class PlantUMLFormatter2 implements IFormatter {

    private SequenceSettingsState sequenceSettingsState;
    private java.util.List<String> methodColors = new ArrayList<>();

    public PlantUMLFormatter2() {
        this.sequenceSettingsState = SequenceSettingsState.getInstance();
        methodColors.add(toHexColorString(sequenceSettingsState.METHOD_BAR_COLOR));
        methodColors.add("#00FFFF");
        methodColors.add("#8FBC8F");
        methodColors.add("#FFFAF0");
        methodColors.add("#DAA520");
    }

    @Override
    public String format(CallStack callStack) {
        StringBuffer buffer = new StringBuffer();
        buffer.append("@startuml").append('\n')
                .append("skinparam defaultFontName 微软雅黑").append("\n")
                .append("autoactivate on").append("\n")
                .append("autonumber").append("\n");

        buffer.append("participant Actor").append("\n").append("\n");
        String classA = callStack.getMethod().getClassDescription().getClassShortName();
        String method = getMethodName(callStack.getMethod());
        if (Constants.CONSTRUCTOR_METHOD_NAME.equals(callStack.getMethod().getMethodName())) {
            buffer.append("create ").append(classA).append('\n');
        }
        buffer.append("Actor").append(" -> ").append(classA).append(methodColors.get(0))
                .append(" : ").append(method).append('\n');
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
        String classA = parent.getMethod().getClassDescription().getClassShortName();
        if (!selfCallCount.containsKey(classA)) {
            selfCallCount.put(classA, 0);
        }

        for (CallStack callStack : parent.getCalls()) {
            String classB = callStack.getMethod().getClassDescription().getClassShortName();
            String method = getMethodName(callStack.getMethod());
            if (Constants.CONSTRUCTOR_METHOD_NAME.equals(callStack.getMethod().getMethodName())) {
                buffer.append("create ").append(classB).append('\n');
            }
            String methodColor = methodColors.get(0);
            if (classA.equals(classB)) {
                Integer count = selfCallCount.get(classA);
                count++;
                selfCallCount.put(classA, count);
                methodColor = methodColors.get(count % methodColors.size());
            }
            buffer.append(classA).append(" -> ").append(classB).append(methodColor)
                    .append(" : ").append(method).append('\n');
//            buffer.append("activate ").append(classB).append('\n');
            generate(buffer, callStack);
            buffer.append(classB).append(" --> ").append(classA)
                    .append(" : ").append("return ").append(classA)
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
