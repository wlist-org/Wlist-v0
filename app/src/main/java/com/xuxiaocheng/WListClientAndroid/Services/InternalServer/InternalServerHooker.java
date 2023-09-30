package com.xuxiaocheng.WListClientAndroid.Services.InternalServer;

import com.xuxiaocheng.Rust.NativeUtil;
import com.xuxiaocheng.WList.Server.Util.JavaScriptUtil;
import io.netty.util.internal.PlatformDependent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.RhinoException;
import org.mozilla.javascript.Scriptable;

import java.util.Map;

final class InternalServerHooker {
    private InternalServerHooker() {
        super();
    }

    static void hookBefore() {
        NativeUtil.ExtraPathGetterCore.reinitialize(l -> {
            final String arch = PlatformDependent.normalizedArch();
            throw new IllegalStateException("Unknown architecture: " + ("unknown".equals(arch) ? System.getProperty("os.arch") : arch));
        }); // Normally is unreachable.
        JavaScriptUtil.JavaScriptEngineCore.reinitialize(InternalServerHooker.RhinoScriptEngineBuilder::new);
    }

    static void hookFinish() {
    }

    private static class RhinoScriptEngineBuilder extends JavaScriptUtil.IEngineBuilder {
        @Override
        public @NotNull RhinoScriptEngineBuilder allowJavaMethod(final @NotNull Class<?> clazz) throws JavaScriptUtil.ScriptException {
            return (RhinoScriptEngineBuilder) super.allowJavaMethod(clazz); // TODO
        }

        @Override
        public JavaScriptUtil.@NotNull IEngine build() {
            return new RhinoScriptEngine(Context.enter());
        }
    }

    private static class RhinoScriptEngine extends JavaScriptUtil.IEngine {
        protected final @NotNull Context context;
        protected final @NotNull Scriptable scope;

        protected RhinoScriptEngine(final @NotNull Context context) {
            super();
            this.context = context;
            context.setOptimizationLevel(-1);
            this.scope = this.context.initSafeStandardObjects();
        }

        @SuppressWarnings("unchecked")
        @Override
        public @Nullable Map<String, Object> eval(final @NotNull String script) throws JavaScriptUtil.ScriptException {
            try {
                return (Map<String, Object>) this.context.evaluateString(this.scope, script, "<eval>", 1, null);
            } catch (final RhinoException exception) {
                throw new JavaScriptUtil.ScriptException(exception);
            }
        }

        @Override
        public void execute(final @NotNull String script) throws JavaScriptUtil.ScriptException {
            try {
                this.context.evaluateString(this.scope, script, "<eval>", 1, null);
            } catch (final RhinoException exception) {
                throw new JavaScriptUtil.ScriptException(exception);
            }
        }

        @Override
        public void close() {
            this.context.close();
        }

        @Override
        public @NotNull String toString() {
            return "RhinoScriptEngine{" +
                    "context=" + this.context +
                    ", scope=" + this.scope +
                    ", super=" + super.toString() +
                    '}';
        }
    }
}
