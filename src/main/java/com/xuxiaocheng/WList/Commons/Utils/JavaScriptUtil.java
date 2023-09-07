package com.xuxiaocheng.WList.Commons.Utils;

import com.xuxiaocheng.HeadLibs.Functions.FunctionE;
import com.xuxiaocheng.HeadLibs.Helpers.HRandomHelper;
import com.xuxiaocheng.HeadLibs.Initializers.HInitializer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.openjdk.nashorn.api.scripting.NashornScriptEngineFactory;

import javax.script.ScriptEngine;
import java.io.Serial;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

public final class JavaScriptUtil {
    private JavaScriptUtil() {
        super();
    }

    public abstract static class IEngine implements AutoCloseable {
        public abstract @Nullable Map<@NotNull String, @Nullable Object> eval(final @NotNull String script) throws ScriptException;

        public void execute(final @NotNull String script) throws ScriptException {
            this.eval(script);
        }

        @Override
        public void close() throws ScriptException {
        }
    }

    public abstract static class IEngineBuilder {
        public abstract @NotNull IEngine build() throws ScriptException;

        public @NotNull IEngineBuilder allowJavaMethod(final @NotNull Class<?> clazz) throws ScriptException {
            throw new UnsupportedOperationException("Java script engine does not support Java method invocation.");
        }
    }

    public static class DefaultScriptEngine extends IEngine {
        private final @NotNull ScriptEngine engine;

        public DefaultScriptEngine(final @NotNull ScriptEngine engine) {
            super();
            this.engine = engine;
        }

        @SuppressWarnings("unchecked")
        @Override
        public @Nullable Map<@NotNull String, @Nullable Object> eval(final @NotNull String script) throws ScriptException {
            try {
                return (Map<String, Object>) this.engine.eval(script);
            } catch (final javax.script.ScriptException exception) {
                if (exception.getFileName() != null)
                    throw new ScriptException(exception.getMessage());
                throw new ScriptException(exception.getCause());
            } catch (final ClassCastException exception) {
                throw new ScriptException(exception);
            }
        }

        @Override
        public void execute(@NotNull final String script) throws ScriptException {
            try {
                this.engine.eval(script);
            } catch (final javax.script.ScriptException exception) {
                if (exception.getFileName() != null)
                    throw new ScriptException(exception.getMessage());
                throw new ScriptException(exception.getCause());
            }
        }

        @Override
        public @NotNull String toString() {
            return "DefaultScriptEngine{" +
                    "engine=" + this.engine +
                    '}';
        }
    }

    public static class NashornScriptEngineBuilder extends IEngineBuilder {
        private final @NotNull Set<@NotNull String> allowedClass = new HashSet<>();

        @Override
        public @NotNull IEngineBuilder allowJavaMethod(final @NotNull Class<?> clazz) {
            this.allowedClass.add(clazz.getName());
            return this;
        }

        @Override
        public @NotNull IEngine build() throws ScriptException {
            final NashornScriptEngineFactory factory = new NashornScriptEngineFactory();
            final ScriptEngine engine = factory.getScriptEngine(this.allowedClass::contains);
            return new DefaultScriptEngine(engine);
        }

        @Override
        public boolean equals(final @Nullable Object o) {
            if (this == o) return true;
            if (!(o instanceof NashornScriptEngineBuilder that)) return false;
            return this.allowedClass.equals(that.allowedClass);
        }

        @Override
        public int hashCode() {
            return Objects.hash(this.allowedClass);
        }

        @Override
        public @NotNull String toString() {
            return "NashornScriptEngineBuilder{" +
                    "allowedClass=" + this.allowedClass +
                    '}';
        }
    }

    /**
     * A fixed class for android.
     * @see javax.script.ScriptException
     */
    public static class ScriptException extends Exception {
        @Serial
        private static final long serialVersionUID = -2289427385620744646L;
        public ScriptException(final @NotNull String s) {
            super(s);
        }
        public ScriptException(final @NotNull Throwable e) {
            super(e);
        }
    }

    @FunctionalInterface
    public interface EngineCore extends FunctionE<@NotNull String, @Nullable Map<@NotNull String, @Nullable Object>> {
        @NotNull JavaScriptUtil.IEngineBuilder newEngineBuilder();

        default @Nullable Map<@NotNull String, @Nullable Object> apply(final @NotNull String script) throws ScriptException {
            try (final IEngine engine = this.newEngineBuilder().build()) {
                return engine.eval(script);
            }
        }
    }

    public static final @NotNull HInitializer<EngineCore> JavaScriptEngineCore = new HInitializer<>("JavaScriptEngineCore", NashornScriptEngineBuilder::new);

    public static @Nullable Map<@NotNull String, @Nullable Object> execute(final @NotNull String script) throws ScriptException {
        return JavaScriptUtil.JavaScriptEngineCore.getInstance().apply(script);
    }

    // TODO: more ajax support.
    public static @Nullable Map<@NotNull String, @Nullable Object> extraOnlyAjaxData(final @NotNull Iterable<@NotNull String> scripts) throws ScriptException {
        final String ajaxObjName = "ajaxObj_" + HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 128, null);
        final String ajaxFlagName = "ajaxFlag_" + HRandomHelper.nextString(HRandomHelper.DefaultSecureRandom, 128, null);
        try (final IEngine engine = JavaScriptUtil.JavaScriptEngineCore.getInstance().newEngineBuilder().build()) {
            engine.execute("var obj,flag=true,$={ajax:function(o){if(obj===undefined&&flag){obj=o;flag=false;}else{obj=undefined;throw 'Multiple ajax requests.';}}};"
                    .replace("obj", ajaxObjName).replace("flag", ajaxFlagName));
            final AtomicReference<ScriptException> exception = new AtomicReference<>(null);
            for (final String script: scripts)
                try {
                    engine.execute(script);
                } catch (final ScriptException e) {
                    exception.compareAndSet(null, e);
                }
            final Map<String, Object> eval = engine.eval(ajaxObjName);
            if (eval == null && exception.get() != null)
                throw exception.get();
            return eval;
        }
    }
}
