package ch.obermuhlner.scriptengine.java;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.script.Bindings;
import javax.script.CompiledScript;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

import ch.obermuhlner.scriptengine.java.execution.ExecutionStrategy;

/**
 * The compiled Java script created by a {@link JavaScriptEngine}.
 */
public class JavaCompiledScript extends CompiledScript {
    private final JavaScriptEngine engine;
    private final Class<?> compiledClass;
    private final Object compiledInstance;
    private ExecutionStrategy executionStrategy;

    /**
     * Construct a {@link JavaCompiledScript}.
     *
     * @param engine the {@link JavaScriptEngine} that compiled this
     *            script
     * @param compiledClass the compiled {@link Class}
     * @param compiledInstance the instance of the compiled {@link Class} or
     *            {@code null} if no instance was created and only
     *            static methods will be called by the the
     *            {@link ExecutionStrategy}.
     * @param executionStrategy the {@link ExecutionStrategy}
     */
    JavaCompiledScript(JavaScriptEngine engine, Class<?> compiledClass, Object compiledInstance,
            ExecutionStrategy executionStrategy) {
        this.engine = engine;
        this.compiledClass = compiledClass;
        this.compiledInstance = compiledInstance;
        this.executionStrategy = executionStrategy;
    }

    /**
     * Returns the compiled {@link Class}.
     *
     * @return the compiled {@link Class}.
     */
    public Class<?> getCompiledClass() {
        return compiledClass;
    }

    /**
     * Returns the instance of the compiled {@link Class}.
     *
     * @return the instance of the compiled {@link Class} or {@code null} if no
     *         instance was created and only static methods will be called by the
     *         the {@link ExecutionStrategy}.
     */
    public Object getCompiledInstance() {
        return compiledInstance;
    }

    /**
     * Returns the compiled {@link Class}.
     *
     * @return the compiled {@link Class}.
     * @deprecated in release 1.1.0 this method was deprecated, use
     *             {@link #getCompiledClass()} instead.
     */
    @Deprecated
    public Class<?> getInstanceClass() {
        return getCompiledClass();
    }

    /**
     * Returns the instance of the compiled {@link Class}.
     *
     * @return the instance of the compiled {@link Class} or {@code null} if no
     *         instance was created and only static methods will be called by the
     *         the {@link ExecutionStrategy}.
     * @deprecated in release 1.1.0 this method was deprecated, use
     *             {@link #getCompiledInstance()} instead.
     */
    @Deprecated
    public Object getInstance() {
        return getCompiledInstance();
    }

    /**
     * Sets the {@link ExecutionStrategy} to be used when evaluating the compiled
     * class instance.
     *
     * @param executionStrategy the {@link ExecutionStrategy}
     */
    public void setExecutionStrategy(ExecutionStrategy executionStrategy) {
        this.executionStrategy = executionStrategy;
    }

    @Override
    public ScriptEngine getEngine() {
        return engine;
    }

    @Override
    public Object eval(ScriptContext context) throws ScriptException {
        Bindings globalBindings = context.getBindings(ScriptContext.GLOBAL_SCOPE);
        Bindings engineBindings = context.getBindings(ScriptContext.ENGINE_SCOPE);

        // pushVariables(globalBindings, engineBindings);

        Map<String, Object> mergedBindings = mergeBindings(globalBindings, engineBindings);
        Entry<Object, Map<String, Object>> me = executionStrategy.execute(compiledInstance, mergedBindings);

        Map<String, Object> values = me.getValue();

        pullVariables(values, globalBindings, engineBindings);

        return me.getKey();
    }

    private void pushVariables(Bindings globalBindings, Bindings engineBindings) throws ScriptException {
        Map<String, Object> mergedBindings = mergeBindings(globalBindings, engineBindings);

        if (true) {
            Method m;
            try {
                m = compiledClass.getMethod("setBindings", Map.class);

                m.invoke(compiledInstance, mergedBindings);
            } catch (Exception e) {
                throw new ScriptException(e);
            }

            return;
        }

        for (Map.Entry<String, Object> entry : mergedBindings.entrySet()) {
            String name = entry.getKey();
            Object value = entry.getValue();

            try {
                Field field = compiledClass.getField(name);
                field.set(compiledInstance, value);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                throw new ScriptException(e);
            }
        }
    }

    private void pullVariables(Map<String, Object> mergedBindings, Bindings globalBindings, Bindings engineBindings)
            throws ScriptException {
        if (true) {
            for (Map.Entry<String, Object> entry : mergedBindings.entrySet()) {
                String name = entry.getKey();
                Object value = entry.getValue();
                setBindingsValue(globalBindings, engineBindings, name, value);
            }
            return;

        }
        for (Field field : compiledClass.getFields()) {
            try {
                String name = field.getName();
                Object value = field.get(compiledInstance);
                setBindingsValue(globalBindings, engineBindings, name, value);
            } catch (IllegalAccessException e) {
                throw new ScriptException(e);
            }
        }
    }

    private void setBindingsValue(Bindings globalBindings, Bindings engineBindings, String name, Object value) {
        if (!engineBindings.containsKey(name) && globalBindings.containsKey(name)) {
            globalBindings.put(name, value);
        } else {
            engineBindings.put(name, value);
        }
    }

    private Map<String, Object> mergeBindings(Bindings... bindingsToMerge) {
        Map<String, Object> variables = new HashMap<>();

        for (Bindings bindings : bindingsToMerge) {
            if (bindings != null) {
                for (Map.Entry<String, Object> globalEntry : bindings.entrySet()) {
                    variables.put(globalEntry.getKey(), globalEntry.getValue());
                }
            }
        }

        return variables;
    }
}
