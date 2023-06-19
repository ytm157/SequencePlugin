package vanstudio.sequence.diagram;


import com.intellij.openapi.diagnostic.Logger;

import java.util.ArrayList;
import java.util.List;

public class ObjectInfo extends Info {
    private static final Logger LOGGER = Logger.getInstance(ObjectInfo.class);
    public static final String ACTOR_NAME = "Actor";

    private final String _name;
    private final String _fullName;
    private final int _seq;
    private final List<MethodInfo> _methods = new ArrayList<>();
    private String absPath;

    public ObjectInfo(String name, List<String> attributes, int seq, String absPath) {
        super(attributes);
        _name = name.substring(name.lastIndexOf('.') + 1);
        _fullName = name;
        _seq = seq;
        this.absPath = absPath;
    }

    public void addMethod(MethodInfo mi) {
        if (LOGGER.isDebugEnabled())
            LOGGER.debug("addMethod(" + mi + ")");
        int possible = -1;
        for (int i = 0; i < _methods.size(); ++i) {
            MethodInfo otherMethod = _methods.get(i);
            if (otherMethod.getStartSeq() > mi.getStartSeq()) {
                possible = i;
                break;
            }
        }
        if (possible == -1)
            _methods.add(mi);
        else
            _methods.add(possible, mi);
    }

    public List<MethodInfo> getMethods() {
        return _methods;
    }

    public String getName() {
        return _name;
    }

    public String getFullName() {
        return _fullName;
    }

    public String getHtmlPackageName() {
        int index = _fullName.lastIndexOf('.');
        return index != -1 ? _fullName.substring(0, index) : "&lt;default&gt;";
    }

    public int getSeq() {
        return _seq;
    }

    public String toString() {
        return "Object " + _name + " seq " + _seq;
    }

    public int hashCode() {
        return _name.hashCode();
    }

    public boolean equals(Object o) {
        return _name.equals(((ObjectInfo) o)._name);
    }

    public boolean isActor() {
        return _name.equals(ACTOR_NAME);
    }

    public String getAbsPath() {
        return absPath;
    }

    public void setAbsPath(String absPath) {
        this.absPath = absPath;
    }
}
