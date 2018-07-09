package com.reginald.pluginm.hook;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.reginald.pluginm.core.PluginManager;
import com.reginald.pluginm.reflect.FieldUtils;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.reflect.Utils;
import com.reginald.pluginm.utils.Logger;

import android.content.ComponentName;
import android.content.Context;
import android.os.IBinder;
import android.os.IInterface;

/**
 * Created by lxy on 18-7-2.
 */

public class SystemServiceHook extends ServiceHook {
    private static final String TAG = "SystemServiceHook";

    private static final Set<String> sServiceUnhookList = new HashSet<>();
    private static final Map<String, MethodHandler> sBinderMethodHandlers = new HashMap<>();

    // 缓存的系统服务binder
    private Map<String, IBinder> mProxyMap = new HashMap<>();

    public static class MethodBlocker extends MethodHandler {
        private String mName;
        public MethodBlocker(String className, String methodName) {
            mName = keyForBlocker(className, methodName);
        }

        @Override
        public String getName() {
            return mName;
        }

        public Object blockReturn() {
            return null;
        }

        @Override
        public Object onStartInvoke(Object receiver, Method method, Object[] args) {
            return blockReturn();
        }

        public static String keyForBlocker(String className, String methodName) {
            return className + "#" + methodName;
        }

        public static String keyForBlocker(Method method) {
            return method.getDeclaringClass().getName() + "#" + method.getName();
        }
    }

    static final ServiceHook.MethodHandler sBinderInterfaceHandler = new ServiceHook.MethodHandler() {
        PluginManager mPluginManager = PluginManager.getInstance();


        /**
         * receiver 为 实现系统服务接口的本地代理类
         * 例如 com.android.internal.app.IAppOpsService$Stub$Proxy
         *
         * method 为 系统服务的aidl接口方法
         * 例如 com.android.internal.app.IAppOpsService.checkOp(String op, int uid, String packageName)
         *
         * args 为 具体参数列表
         */
        @Override
        public Object onStartInvoke(Object receiver, Method method, Object[] args) {
            Logger.d(TAG, "BinderInterfaceHandler.invoke %s(%s) ",
                    method, args != null ? Arrays.asList(args) : "null");

            // 接口屏蔽处理
            MethodHandler handler = sBinderMethodHandlers.get(MethodBlocker.keyForBlocker(method));
            if (handler != null) {
                Object result = handler.onStartInvoke(receiver, method, args);
                if (result != NONE_RETURN) {
                    Logger.w(TAG, "BinderInterfaceHandler BLOCK method [ %s ]", method);
                    return result;
                }
            }

            // 修改所有带有plugin包名的参数
            if (args != null && args.length > 0) {
                for (int index = 0; index < args.length; index++) {
                    Object param = args[index];
                    if (param == null) {
                        continue;
                    }
                    if (param instanceof String) {
                        String str = ((String) param);
                        if (mPluginManager.isPlugin(str)) {
                            args[index] = mPluginManager.getHostContext().getPackageName();
                            Logger.d(TAG, "BinderInterfaceHandler REPLACE pkgName %s to host in "
                                    + "method %s (%dth param)", param, method, index + 1);
                        }
                    } else if (param instanceof ComponentName) {
                        ComponentName componentName = (ComponentName) param;
                        if (mPluginManager.isPlugin(componentName.getPackageName())) {
                            args[index] = new ComponentName(mPluginManager.getHostContext().getPackageName(),
                                    componentName.getClassName());
                            Logger.d(TAG, "BinderInterfaceHandler REPLACE ComponentName %s to host in "
                                    + "method %s (%dth param)", param, method, index + 1);
                        }
                    }
                }
            }

            return super.onStartInvoke(receiver, method, args);
        }

        @Override
        public Object onEndInvoke(Object receiver, Method method, Object[] args, Object invokeResult) {
            if (invokeResult instanceof IInterface) {
                IInterface iInterface = (IInterface) invokeResult;
                Logger.d(TAG, "BinderInterfaceHandler REPLACE IInterface %s in "
                        + "method %s (return value)", invokeResult, method);
                iInterface = StubProxyHook.fetch(iInterface);
                invokeResult = iInterface != null ? iInterface : invokeResult;
            } else if (invokeResult instanceof IBinder) {
                Logger.d(TAG, "BinderInterfaceHandler REPLACE IBinder %s in "
                        + "method %s (return value)", invokeResult, method);
            }
            return super.onEndInvoke(receiver, method, args, invokeResult);
        }
    };

    static {
        // 不需要进行binder hook的名单
        sServiceUnhookList.add(Context.ACTIVITY_SERVICE);
        sServiceUnhookList.add("package");

        // 需要进行屏蔽的具体方法。
        addMethodBlocker(new MethodBlocker("android.app.job.IJobScheduler", "schedule") {
            @Override
            public Object blockReturn() {
                return 1;
            }
        });

        addMethodBlocker(new MethodBlocker("android.accounts.IAccountManager","addAccountExplicitly") {
            @Override
            public Object blockReturn() {
                return false;
            }
        });

    }

    private static void addMethodBlocker(MethodBlocker methodBlocker) {
        sBinderMethodHandlers.put(methodBlocker.getName(), methodBlocker);
    }

    public static boolean init() {
        SystemServiceHook hook = new SystemServiceHook();
        return hook.install();
    }

    static class BinderHook extends ServiceHook {
        private IBinder mObj;
        private IBinder mProxy;
        private IInterface mHookedService;

        public static IBinder fetch(IBinder binder) {
            BinderHook hook = new BinderHook();
            return hook.install(binder);
        }

        public IBinder install(IBinder iBinder) {
            mObj = iBinder;
            boolean isSuc = install();
            return isSuc ? mProxy : null;
        }

        @Override
        public boolean install() {
            try {
                List<Class<?>> interfaces = Utils.getAllInterfaces(mObj.getClass());
                Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces
                        .toArray(new Class[interfaces.size()]) : new Class[0];
                mProxy = (IBinder) Proxy.newProxyInstance(mObj.getClass().getClassLoader(), ifs, this);

                mBase = mObj;

                setAllMethodHandler(sQueryLocalInterfaceHandler);
                Logger.d(TAG, "BinderHook.install success for binder %s !", mObj);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            Logger.e(TAG, "BinderHook.install error for binder %s !", mObj);
            return false;
        }

        private final ServiceHook.MethodHandler sQueryLocalInterfaceHandler = new ServiceHook.MethodHandler() {
            @Override
            public Object onEndInvoke(Object receiver, Method method, Object[] args, Object invokeResult) {
                Logger.d(TAG, "QueryLocalInterfaceHandler.onEndInvoke %s(%s) ",
                        method, args != null ? Arrays.asList(args) : "null");

                if (method.getName().equals("queryLocalInterface")) {
                    String descriptor = (String) args[0];
                    try {
                        if (mHookedService == null) {
                            synchronized(BinderHook.this) {
                                if (mHookedService == null) {
                                    Class<?> interfaceClazz = null;
                                    String className = String.format("%s$Stub$Proxy", descriptor);
                                    try {
                                        interfaceClazz = Class.forName(className);
                                    } catch (ClassNotFoundException e) {
                                        Logger.e(TAG, "queryLocalInterface: interfaceClazz not found for %s",
                                                className);
                                        e.printStackTrace();
                                    }

                                    if (interfaceClazz != null) {
                                        IInterface service =
                                                (IInterface) MethodUtils.invokeConstructor(interfaceClazz, receiver);
                                        IInterface newProxy = StubProxyHook.fetch(service);
                                        if (newProxy != null) {
                                            Logger.d(TAG, "queryLocalInterface: create proxy for service %s -> %s",
                                                    descriptor, newProxy);
                                            mHookedService = newProxy;
                                        } else {
                                            Logger.e(TAG, "queryLocalInterface: create proxy for service %s error!",
                                                    descriptor);
                                        }
                                    }
                                }
                            }
                        }

                        if (mHookedService != null) {
                            invokeResult = mHookedService;
                            Logger.d(TAG, "queryLocalInterface: found cached proxy for service %s -> %s",
                                    descriptor, mHookedService);
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                return invokeResult;
            }
        };
    }

    static class StubProxyHook extends ServiceHook {
        private IInterface mObj;
        private IInterface mProxy;

        public static IInterface fetch(IInterface binder) {
            StubProxyHook hook = new StubProxyHook();
            return hook.install(binder);
        }

        public IInterface install(IInterface iBinder) {
            mObj = iBinder;
            boolean isSuc = install();
            return isSuc ? mProxy : null;
        }

        @Override
        public boolean install() {
            try {
                List<Class<?>> interfaces = Utils.getAllInterfaces(mObj.getClass());
                Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces
                        .toArray(new Class[interfaces.size()]) : new Class[0];
                mProxy = (IInterface) Proxy.newProxyInstance(mObj.getClass().getClassLoader(), ifs, this);

                mBase = mObj;

                setAllMethodHandler(sBinderInterfaceHandler);
                Logger.d(TAG, "StubProxyHook.install success for binder %s !", mObj);
                return true;
            } catch (Exception e) {
                e.printStackTrace();
            }
            Logger.e(TAG, "StubProxyHook.install error for binder %s !", mObj);
            return false;
        }
    }

    public boolean install() {
        try {
            Logger.d(TAG, "install");
            Class<?> serviceManagerClazz = Class.forName("android.os.ServiceManager");
            Object iServiceManager = FieldUtils.readStaticField(serviceManagerClazz, "sServiceManager");
            if (iServiceManager == null) {
                MethodUtils.invokeStaticMethod(serviceManagerClazz, "getIServiceManager");
                iServiceManager = FieldUtils.readStaticField(serviceManagerClazz, "sServiceManager");
            }

            List<Class<?>> interfaces = Utils.getAllInterfaces(iServiceManager.getClass());
            Class[] ifs = interfaces != null && interfaces.size() > 0 ? interfaces
                    .toArray(new Class[interfaces.size()]) : new Class[0];
            Object proxy = Proxy.newProxyInstance(iServiceManager.getClass().getClassLoader(), ifs, this);
            FieldUtils.writeStaticField(serviceManagerClazz, "sServiceManager", proxy);

            Object serviceCache = FieldUtils.readStaticField(serviceManagerClazz, "sCache");
            if (!proxyServiceCache(serviceCache)) {
                Logger.d(TAG, "install: services cache proxy failed!");
                return false;
            }

            mBase = iServiceManager;

            addMethodHandler(new getService());
            addMethodHandler(new checkService());

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return false;
    }

    private boolean proxyServiceCache(Object cacheObj) {
        if (cacheObj != null) {
            if (cacheObj instanceof Map) {
                Map<String, IBinder> map = (Map<String, IBinder>) cacheObj;
                for (Map.Entry<String, IBinder> entry : map.entrySet()) {
                    String name = entry.getKey();
                    IBinder service = entry.getValue();
                    IBinder proxy = fetchProxyBinder(name, service);
                    if (proxy != null) {
                        entry.setValue(proxy);
                        Logger.d(TAG, "proxyServiceCache: service %s hook success!", name);
                    } else {
                        Logger.e(TAG, "proxyServiceCache: service %s hook error!", name);
                    }
                }
                return true;
            }
        }
        return false;
    }

    private IBinder fetchProxyBinder(String name, IBinder originService) {
        if (sServiceUnhookList.contains(name)) {
            Logger.d(TAG, "fetchProxyBinder: service %s should not installed due to config",
                    name, originService);
            return originService;
        }

        synchronized(mProxyMap) {
            IBinder proxy = mProxyMap.get(name);

            if (proxy != null) {
                Logger.d(TAG, "fetchProxyBinder: found cached proxy for %s: %s",
                        name, proxy);
            } else {
                proxy = BinderHook.fetch(originService);
                if (proxy != null) {
                    mProxyMap.put(name, proxy);
                    Logger.d(TAG, "fetchProxyBinder: create new proxy for %s: %s",
                            name, proxy);
                } else {
                    Logger.e(TAG, "fetchProxyBinder: create new proxy for %s error!",
                            name, proxy);
                }
            }

            return proxy;
        }
    }

    private class getService extends ServiceHook.MethodHandler {
        @Override
        public String getName() {
            return "getService";
        }

        public Object onEndInvoke(Object receiver, Method method, Object[] args, Object invokeResult) {
            String name = null;
            if (args != null && args.length > 0 && args[0] instanceof String) {
                name = (String) args[0];
                Logger.d(TAG, "getService() for %s", name);

                if (invokeResult != null && invokeResult instanceof IBinder) {
                    IBinder service = (IBinder) invokeResult;
                    invokeResult = fetchProxyBinder(name, service);
                }
            }

            return invokeResult;
        }
    }

    private class checkService extends ServiceHook.MethodHandler {
        @Override
        public String getName() {
            return "checkService";
        }

        public Object onEndInvoke(Object receiver, Method method, Object[] args, Object invokeResult) {
            String name = null;
            if (args != null && args.length > 0 && args[0] instanceof String) {
                name = (String) args[0];
                Logger.d(TAG, "checkService() for %s", name);
                if (invokeResult != null && invokeResult instanceof IBinder) {
                    IBinder service = (IBinder) invokeResult;
                    invokeResult = fetchProxyBinder(name, service);
                }
            }

            return invokeResult;
        }
    }

}
