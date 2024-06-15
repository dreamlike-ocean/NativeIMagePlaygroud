package io.github.dreamlike;

import java.util.HashMap;
import java.util.Map;

public class Context {

    private final Context frozenContext;

    private final Map<String, String> current;


    public Context() {
        this.current = new HashMap<>();
        this.frozenContext = null;
    }


    private Context(Context parentContext) {
        Map<String, String> parentFrozenMap = Map.copyOf(parentContext.current);
        this.frozenContext = new Context(parentContext.frozenContext, parentFrozenMap);
        this.current = new HashMap<>();
    }

    private Context(Context frozenContext, Map<String, String> current) {
        this.current = current;
        this.frozenContext = frozenContext;
    }


    public Context fork() {
        return new Context(this);
    }

    public String get(String key) {
        String string = current.get(key);
        //fast path
        if (string != null) {
            return string;
        }

        Context parent = frozenContext;
        while (parent != null) {
            string = parent.current.get(key);
            if (string != null) {
                return string;
            }
            parent = parent.frozenContext;
        }
        return null;
    }

    public void set(String key, String value) {
        current.put(key, value);
    }

}
