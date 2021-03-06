package ch.obermuhlner.scriptengine.java.execution;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;

import javax.script.ScriptException;

/**
 * The default {@link ExecutionStrategy} implementation.
 *
 * <ul>
 * <li>class implements `Supplier`: the `get()` method is called</li>
 * <li>class implements `Runnable`: the `run()` method is called</li>
 * <li>class has exactly one public method without arguments: call it</li>
 * </ul>
 */
public class DefaultExecutionStrategy implements ExecutionStrategy {

    private final Class<?> clazz;
    private final Method method;

    /**
     * Constructs a {@link DefaultExecutionStrategy} for the specified {@link Class}.
     *
     * @param clazz the {@link Class}
     */
    public DefaultExecutionStrategy(Class<?> clazz) {
        method = findCallableMethod(clazz);
        this.clazz = clazz;
    }

    @Override
    public Entry<Object, Map<String, Object>> execute(Object instance, Map<String, Object> bindings)
            throws ScriptException

    {
        if (instance instanceof Supplier) {
            Supplier<?> supplier = (Supplier<?>) instance;

            Entry<Object, Map<String, Object>> e = new AbstractMap.SimpleEntry((String) null, supplier.get());

            return e;
        }

        if (instance instanceof Runnable) {
            Runnable runnable = (Runnable) instance;
            runnable.run();
            return null;
        }
        /*
         * // TODO
         * 
         * if (method != null) {
         * try {
         * return method.invoke(instance);
         * } catch (IllegalAccessException | InvocationTargetException e) {
         * throw new ScriptException(e);
         * }
         * }
         */
        if (instance == null) {
            throw new ScriptException("No static method found to execute of type " + clazz.getName());
        }
        throw new ScriptException("No method found to execute instance of type " + clazz.getName());
    }

    private static Method findCallableMethod(Class<?> clazz) {
        List<Method> callableMethods = new ArrayList<>();
        for (Method method : clazz.getDeclaredMethods()) {
            int modifiers = method.getModifiers();
            if (method.getParameterCount() == 0 && Modifier.isPublic(modifiers)) {
                callableMethods.add(method);
            }
        }

        if (callableMethods.size() == 1) {
            return callableMethods.get(0);
        }

        return null;
    }
}
