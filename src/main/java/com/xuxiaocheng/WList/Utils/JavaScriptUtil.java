package com.xuxiaocheng.WList.Utils;

import com.xuxiaocheng.HeadLibs.DataStructures.ParametersMap;
import com.xuxiaocheng.HeadLibs.DataStructures.Triad;
import com.xuxiaocheng.HeadLibs.Functions.FunctionE;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.script.ScriptEngineManager;
import java.io.Serial;
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
    public static <V> @Nullable V execute(final @NotNull String script) throws ScriptException {
        JavaScriptUtil.jsEngineCore.initializeIfNot(() -> {
            final @NotNull ScriptEngineManager manager = new ScriptEngineManager();
            return s -> {
                try {
                    return manager.getEngineByName("JavaScript").eval(s);
                } catch (final javax.script.ScriptException e) {
                    if (e.getCause() != null)
                        throw new ScriptException(e.getCause());
                    throw new ScriptException(e.getMessage(), e.getFileName(), e.getLineNumber(), e.getColumnNumber());
                }
            };
        });
        return (V) JavaScriptUtil.jsEngineCore.getInstance().apply(script);
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
            final Map<String, Object> ajax = JavaScriptUtil.execute("var ajaxObj;var $={ajax:function(o){if(ajaxObj===undefined)ajaxObj=o;else throw 'Multiple ajax requests.';}};" + script + ";ajaxObj;");
            if (ajax == null) return null;
            final Object type = ajax.get("type");
            final Object path = ajax.get("url");
            final Map<String, Object> data = (Map<String, Object>) ajax.get("data");
            if (type == null || path == null || data == null) return null;
            return Triad.ImmutableTriad.makeImmutableTriad(type.toString(), path.toString(), data.entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
        }
    }

    /**
     * A fixed class for android.
     * @see javax.script.ScriptException
     */
    @SuppressWarnings("ClassHasNoToStringMethod")
    public static class ScriptException extends Exception {
        @Serial
        private static final long serialVersionUID = -2289427385620744646L;
        protected final String fileName;
        protected final int lineNumber;
        protected final int columnNumber;
        public ScriptException(final @NotNull String s) {
            super(s);
            this.fileName = null;
            this.lineNumber = -1;
            this.columnNumber = -1;
        }
        public ScriptException(final @NotNull Throwable e) {
            super(e);
            this.fileName = null;
            this.lineNumber = -1;
            this.columnNumber = -1;
        }
        public ScriptException(final @NotNull String message, final @NotNull String fileName, final int lineNumber) {
            super(message);
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.columnNumber = -1;
        }
        public ScriptException(final @NotNull String message, final @NotNull String fileName, final int lineNumber, final int columnNumber) {
            super(message);
            this.fileName = fileName;
            this.lineNumber = lineNumber;
            this.columnNumber = columnNumber;
        }
        public String getMessage() {
            String ret = super.getMessage();
            if (this.fileName != null) {
                ret += (" in " + this.fileName);
                if (this.lineNumber != -1)
                    ret += " at line number " + this.lineNumber;
                if (this.columnNumber != -1)
                    ret += " at column number " + this.columnNumber;
            }
            return ret;
        }
        public int getLineNumber() {
            return this.lineNumber;
        }
        public int getColumnNumber() {
            return this.columnNumber;
        }
        public String getFileName() {
            return this.fileName;
        }
    }
}
