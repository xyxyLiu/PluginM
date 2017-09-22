package com.reginald.pluginm.pluginapi.proxy;

import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.WeakHashMap;

/**
 * Created by lxy on 16-10-27.
 */
public class ProxyManager {
    private static final WeakHashMap<Object, HashMap<Class<?>, Object>> sProxyMap = new WeakHashMap<>();

    public static <T> T getProxy(Object target, Class<T> proxyInterface) {
        if (proxyInterface.isInstance(target)) {
            return proxyInterface.cast(target);
        } else {
            Object proxy;
            HashMap targetProxyMap;
            synchronized (sProxyMap) {
                targetProxyMap = sProxyMap.get(target);
                if (targetProxyMap == null) {
                    targetProxyMap = new HashMap();
                    proxy = makeProxy(target, proxyInterface);
                    if (proxy == null) {
                        return null;
                    } else {
                        targetProxyMap.put(proxyInterface, proxy);
                        sProxyMap.put(target, targetProxyMap);
                    }
                } else {
                    proxy = targetProxyMap.get(proxyInterface);
                    if (proxy == null) {
                        proxy = makeProxy(target, proxyInterface);
                        if (proxy == null) {
                            return null;
                        } else {
                            targetProxyMap.put(proxyInterface, proxy);
                        }
                    }
                }
            }
            return proxyInterface.cast(proxy);
        }
    }

    private static Object makeProxy(Object target, Class<?> proxyClass) {
        ProxyInvocationHandler proxyInvocationHandler = new ProxyInvocationHandler(target, proxyClass);
        try {
            Object proxy = Proxy.newProxyInstance(proxyClass.getClassLoader(), new Class[]{proxyClass}, proxyInvocationHandler);
            return proxy;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
