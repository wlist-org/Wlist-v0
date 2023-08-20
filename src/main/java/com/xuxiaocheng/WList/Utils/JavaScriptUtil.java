package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class JavaScriptUtil {
    private JavaScriptUtil() {
        super();
    }

    private static final @NotNull ScriptEngineManager manager = new ScriptEngineManager();

    public static @NotNull ScriptEngine getScriptEngine() {
        return JavaScriptUtil.manager.getEngineByName("JavaScript");
    }

    private static final @NotNull String scriptStartTag = "<script type=\"text/javascript\">";
    private static final @NotNull String scriptEndTag = "</script>";
    public static @NotNull List<@NotNull String> findScripts(final @NotNull String html) {
        final List<String> scripts = new ArrayList<>();
        int index = 0;
        while (true) {
            index = html.indexOf(JavaScriptUtil.scriptStartTag, index);
            if (index == -1) break;
            final int endIndex = html.indexOf(JavaScriptUtil.scriptEndTag, index);
            if (endIndex == -1) break;
            scripts.add(html.substring(index + JavaScriptUtil.scriptStartTag.length(), endIndex));
            index = endIndex;
        }
        return scripts;
    }

    @SuppressWarnings("unchecked")
    public static <V> @Nullable V execute(final @NotNull String script, final @Nullable String functionName, final @NotNull Object... args) throws ScriptException {
        final ScriptEngine engine = JavaScriptUtil.manager.getEngineByName("JavaScript");
        if (functionName == null)
            return (V) engine.eval(script);
        engine.put("args", args);
        return (V) engine.eval(String.format("%s\n%s(args)", script, functionName));
    }

    public static final class Ajax {
        private Ajax() {
            super();
        }

        public static @Nullable String getOnlyAjaxScripts(final @NotNull Iterable<@NotNull String> scripts) {
            String res = null;
            for (final String script: scripts)
                if (script.contains("$.ajax")) {
                    //noinspection VariableNotUsedInsideIf
                    if (res != null)
                        throw new IllegalStateException("Multiple ajax requests." + ParametersMap.create().add("scripts", scripts));
                    res = script;
                }
            return res;
        }

        @SuppressWarnings("unchecked")
        public static @Nullable Triad<@NotNull String, @NotNull String, @NotNull Map<String, String>> extraOnlyAjaxData(final @NotNull String script) throws ScriptException {
            final ScriptEngine engine = JavaScriptUtil.getScriptEngine();
            engine.eval("var ajaxObj;var $={ajax:function(o){if(ajaxObj===undefined)ajaxObj=o;else throw 'Multiple ajax requests.';}};");
            engine.eval(script);
            final Map<String, Object> ajax = (Map<String, Object>) engine.get("ajaxObj");
            if (ajax == null) return null;
            final Object type = ajax.get("type");
            final Object path = ajax.get("url");
            final Map<String, Object> data = (Map<String, Object>) ajax.get("data");
            if (type == null || path == null || data == null) return null;
            return Triad.ImmutableTriad.makeImmutableTriad(type.toString(), path.toString(), data.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
        }
    }
}
