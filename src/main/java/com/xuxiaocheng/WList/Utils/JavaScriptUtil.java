package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.FunctionE;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @FunctionalInterface
    public interface EngineCore extends FunctionE<@NotNull String, @Nullable Object> {
        @Override
        @Nullable Object apply(final @NotNull String script) throws ScriptException;
    }

    public static final @NotNull HInitializer<EngineCore> jsEngineCore = new HInitializer<>("JavaScriptEngineCore");

    static {
        JavaScriptUtil.jsEngineCore.initializeIfNot(() -> {
            final @NotNull ScriptEngineManager manager = new ScriptEngineManager();
            return s -> manager.getEngineByName("JavaScript").eval(s);
        });
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
    public static <V> @Nullable V execute(final @NotNull String script, final @Nullable String functionName) throws ScriptException {
        final EngineCore engine = JavaScriptUtil.jsEngineCore.getInstance();
        if (functionName == null)
            return (V) engine.apply(script);
        return (V) engine.apply(String.format("%s\n%s()", script, functionName));
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
            final EngineCore engine = JavaScriptUtil.jsEngineCore.getInstance();
            final Map<String, Object> ajax = (Map<String, Object>) engine.apply("var ajaxObj;var $={ajax:function(o){if(ajaxObj===undefined)ajaxObj=o;else throw 'Multiple ajax requests.';}};" + script + ";ajaxObj;");
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
