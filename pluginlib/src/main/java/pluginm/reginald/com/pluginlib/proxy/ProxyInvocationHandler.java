package pluginm.reginald.com.pluginlib.proxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by lxy on 16-10-27.
 */
public class ProxyInvocationHandler implements InvocationHandler {

    private static final Map<Class<?>, HashMap<Class<?>, HashMap<Method, Method>>> sProxyMethodMap = new HashMap<>();

    private final Object mTarget;
    private final HashMap<Method, Method> mMethodMap;

    public ProxyInvocationHandler(Object target, Class<?> proxyInterface) {
        HashMap<Method, Method> methodMap;
        mTarget = target;
        Class<?> targetClass = target.getClass();

        synchronized (sProxyMethodMap) {
            HashMap<Class<?>, HashMap<Method, Method>> proxyMap = sProxyMethodMap.get(targetClass);
            if (proxyMap != null) {
                methodMap = proxyMap.get(proxyInterface);
                if (methodMap == null) {
                    methodMap = makeMethodMap(targetClass, proxyInterface);
                    proxyMap.put(proxyInterface, methodMap);
                }
            } else {
                proxyMap = new HashMap<>();
                methodMap = makeMethodMap(targetClass, proxyInterface);
                proxyMap.put(proxyInterface, methodMap);
                sProxyMethodMap.put(targetClass, proxyMap);
            }
        }

        mMethodMap = methodMap;
    }

    private static HashMap<Method, Method> makeMethodMap(Class<?> targetClass, Class<?> proxyClass) {
        HashMap<Method, Method> methodMap = new HashMap<>();
        Method[] proxyMethods = proxyClass.getMethods();
        for (int i = 0; i < proxyMethods.length; i++) {
            Method proxyMethod = proxyMethods[i];
            Method targetMethodFound = null;
            try {
                targetMethodFound = findMethod(targetClass, proxyMethod.getName(), proxyMethod.getParameterTypes());
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (targetMethodFound == null || !proxyMethod.getReturnType().equals(targetMethodFound.getReturnType())) {
                throw new IllegalArgumentException("method " + proxyMethod + " in " + proxyClass + " is not found in " + targetClass);
            }
            methodMap.put(proxyMethod, targetMethodFound);
        }

        return methodMap;
    }


    @Override
    public Object invoke(Object proxy, Method proxyMethod, Object[] args) throws Throwable {
        Method targetMethod = mMethodMap.get(proxyMethod);
        if (targetMethod != null) {
            return targetMethod.invoke(mTarget, args);
        } else {
            return null;
        }
    }

    public static Method findMethod(Class<?> cls, String name, Class<?>... parameterTypes)
            throws NoSuchMethodException {
        try {
            Method m = cls.getMethod(name, parameterTypes);
            if (m != null) {
                m.setAccessible(true);
                return m;
            }
        } catch (Exception e) {
            // ignore this error & pass down
        }

        Class<?> clsType = cls;
        while (clsType != null) {
            try {
                Method m = clsType.getDeclaredMethod(name, parameterTypes);
                m.setAccessible(true);
                return m;
            } catch (NoSuchMethodException e) {
            }
            clsType = clsType.getSuperclass();
        }
        throw new NoSuchMethodException();
    }
}
