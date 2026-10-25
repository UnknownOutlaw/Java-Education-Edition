package edu.chemistry.util;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.function.Function;

/**
 * Small helpers for the few vanilla hooks that are private (Fabric API would normally widen these).
 * Proxies are used to implement vanilla's non-public interfaces.
 */
public final class Reflect {
    public static Class<?> cls(Class<?> anchor, String binaryName) {
        try {
            return Class.forName(binaryName, false, anchor.getClassLoader());
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Missing class " + binaryName, e);
        }
    }

    /** Implements a (possibly non-public) single-method interface; Object methods are handled sensibly. */
    public static Object implement(Class<?> iface, String methodName, Function<Object[], Object> body) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                return switch (name) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "edu-proxy(" + iface.getSimpleName() + ")";
                    default -> null;
                };
            }
            if (name.equals(methodName)) {
                return body.apply(args == null ? new Object[0] : args);
            }
            throw new UnsupportedOperationException(iface.getName() + "." + name);
        };
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, handler);
    }

    /** Like {@link #implement} but with several method names mapped to bodies. */
    public static Object implement(Class<?> iface, java.util.Map<String, Function<Object[], Object>> bodies) {
        InvocationHandler handler = (proxy, method, args) -> {
            String name = method.getName();
            if (method.getDeclaringClass() == Object.class) {
                return switch (name) {
                    case "hashCode" -> System.identityHashCode(proxy);
                    case "equals" -> proxy == args[0];
                    case "toString" -> "edu-proxy(" + iface.getSimpleName() + ")";
                    default -> null;
                };
            }
            Function<Object[], Object> body = bodies.get(name);
            if (body != null) {
                return body.apply(args == null ? new Object[0] : args);
            }
            throw new UnsupportedOperationException(iface.getName() + "." + name);
        };
        return Proxy.newProxyInstance(iface.getClassLoader(), new Class<?>[] {iface}, handler);
    }

    public static Method method(Class<?> owner, String name, Class<?>... params) {
        try {
            Method m = owner.getDeclaredMethod(name, params);
            m.setAccessible(true);
            return m;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Missing method " + owner.getName() + "." + name, e);
        }
    }

    public static Object invoke(Method m, Object target, Object... args) {
        try {
            return m.invoke(target, args);
        } catch (ReflectiveOperationException e) {
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            if (cause instanceof RuntimeException re) throw re;
            throw new IllegalStateException(cause);
        }
    }

    private Reflect() {
    }
}
