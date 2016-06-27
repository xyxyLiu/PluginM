package com.android.common;

import android.content.ComponentName;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.IPackageDataObserver;
import android.content.pm.IPackageDeleteObserver;
import android.content.pm.IPackageDeleteObserver2;
import android.content.pm.IPackageInstallObserver;
import android.content.pm.IPackageInstallObserver2;
import android.content.pm.IPackageMoveObserver;
import android.content.pm.IPackageStatsObserver;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ServiceInfo;
import android.content.pm.Signature;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class PackageCompat {
    private static final boolean DEBUG = true;
    private static final String TAG = "PackageCompat";
    private static final String CLASSNAME_PACKAGEPARSER = "android.content.pm.PackageParser";
    private static final String CLASSNAME_PACKAGEPARSER_ACTIVITY = "android.content.pm.PackageParser$Activity";
    private static final String CLASSNAME_PACKAGEPARSER_SERVICE = "android.content.pm.PackageParser$Service";
    private static final String CLASSNAME_PACKAGEPARSER_COMPONENT = "android.content.pm.PackageParser$Component";
    private static final String CLASSNAME_PACKAGEPARSER_PACKAGE = "android.content.pm.PackageParser$Package";
    private static final String CLASSNAME_PACKAGEPARSER_ACTIVITYINTENTINFO = "android.content.pm.PackageParser$ActivityIntentInfo";

    private static final String CLASSNAME_IPACKAGEMANAGER = "android.content.pm.IPackageManager";
    private static final String CLASSNAME_IPACKAGEMANAGER_STUB = "android.content.pm.IPackageManager$Stub";

    private static final String CLASSNAME_VERIFICATIONPARAMS = "android.content.pm.VerificationParams";
    private static final String CLASSNAME_MANIFESTDIGEST = "android.content.pm.ManifestDigest";

//    private static final String CLASSNAME_APPLICATIONPACKAGEMANAGER = "android.app.ApplicationPackageManager";

    public static final int INSTALL_LOCATION_AUTO = 0;
    public static final int INSTALL_LOCATION_INTERNAL_ONLY = 1;
    public static final int INSTALL_LOCATION_PREFER_EXTERNAL = 2;
    public static final int INSTALL_LOCATION_UNSPECIFIED = -1;
    public static final int APP_INSTALL_EXTERNAL = 2;

    public static final int MOVE_INTERNAL = 0x00000001;
    public static final int MOVE_EXTERNAL_MEDIA = 0x00000002;
    public static final int MOVE_SUCCEEDED = 1;

    public static final int FLAG_FORWARD_LOCK = 1<<29;

    private static final int VERSION_1 = 1;
    private static final int VERSION_2 = 2;
    private static final int VERSION_3 = 3;

    private static Class<?> sVerificationParamsClass;
    private static Class<?> sManifestDigestClass;
    private static Class<?> sPackageParserClass;
    private static Class<?> sPackageParserActivityClass;
    private static Class<?> sPackageParserServiceClass;
    private static Class<?> sPackageParserComponentClass;
    private static Class<?> sPackageParserPackageClass;
    private static Class<?> sPackageParserActivityIntentInfoClass;
//    private static Class<?> sApplicationPackageManagerClass;
    private static Constructor<?> sPackageParserConstructor;
    private static Constructor<?> sPackageParserConstructor50;
    private static Constructor<?> sVerificationParamsConstructor;
    private static Method sParsePackageMethod;
    private static Method sParsePackageMethod50;
    private static Method sPackageParser_collectCertificatesMethod;
    private static Method sCountActionsMethod;
    private static Method sGetActionMethod;
    private static Method sGetPackageSizeInfoMethod;
    private static Method sFreeStorageAndNotifyMethod;
    private static Method sSetPackageNameMethod;
    private static Field sPackageParser_SignaturesField;
    private static Field sInstallLocationField;
    private static Field sReceiversField;
    private static Field sApplicationInfoField;
    private static Field sMVersionCodeField;
    private static Field sPackageNameField;
    private static Field sActivityIntentsField;
    private static Field sActivityInfoField;
//    private static Field sMpmField;

    /**
     * PackageParser.requestedPermissions
     */
    private static Field sPackageParser_RequestedPermissionsField;

    private static Class<?> sIPackageManagerClass;
    private static Method sAsInterfaceMethod;

    /**
     * IPackageManager.getInstallLocation
     */
    private static Method sGetInstallLocationMethod;

    /**
     * IPackageManager.movePackage
     */
    private static Method sMovePackageMethod;

    /**
     * IPackageManager.setComponentEnabledSetting
     */
    private static Method sSetComponentEnabledSettingMethod;
    private static int sSetComponentEnabledSettingMethodVersion = VERSION_1;

    /**
     * IPackageManager.setApplicationEnabledSetting
     */
    private static Method sSetApplicationEnabledSettingMethod;
    private static int sSetApplicationEnabledSettingMethodVersion = VERSION_1;

    /**
     * IPackageManager.installPackage
     */
    private static Method sInstallPackageMethod;
    private static Method sInstallPackageMethod50; // for android 5.0

    /**
     * IPackageManager.deletePackage or IPackageManager.deletePackageAsUser
     */
    private static Method sDeletePackageMethod;
    private static Method sDeletePackageMethod50; // for android 5.0
    private static int sDeletePackageMethodVersion = VERSION_1;

    private static Class<?> sClazzPackageUserState;
    private static Method sGeneratePackageInfo;
    private static Field sActivitiesField;
    private static Field sServicesField;
    private static Field sComponentIntentsField;
    private static Field sServiceInfoField;

    static {
        try {
            sInstallLocationField = PackageInfo.class.getField("installLocation");
        } catch (SecurityException e) {
            if (DEBUG) e.printStackTrace();
            sInstallLocationField = null;
        } catch (NoSuchFieldException e) {
            if (DEBUG) e.printStackTrace();
            sInstallLocationField = null;
        }

        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            sPackageParserClass = Class.forName(CLASSNAME_PACKAGEPARSER, false,
                    classLoader);
            sPackageParserActivityClass = Class.forName(CLASSNAME_PACKAGEPARSER_ACTIVITY, false,
                    classLoader);
            sPackageParserServiceClass = Class.forName(CLASSNAME_PACKAGEPARSER_SERVICE, false,
                    classLoader);
            sPackageParserComponentClass = Class.forName(CLASSNAME_PACKAGEPARSER_COMPONENT, false,
                    classLoader);
            sPackageParserActivityIntentInfoClass = Class.forName(CLASSNAME_PACKAGEPARSER_ACTIVITYINTENTINFO, false,
                    classLoader);
            sPackageParserPackageClass = Class.forName(CLASSNAME_PACKAGEPARSER_PACKAGE, false,
                    classLoader);
//            sApplicationPackageManagerClass = Class.forName(CLASSNAME_APPLICATIONPACKAGEMANAGER, false,
//                    classLoader);
            try {
                Class<?>[] arrayOfClass = new Class[] { String.class };
                sPackageParserConstructor = sPackageParserClass.getConstructor(arrayOfClass);
            } catch(NoSuchMethodException e) {
                sPackageParserConstructor = null;
                // maybe android 5.0 PackageParser
                sPackageParserConstructor50 = sPackageParserClass.getConstructor();
            }

            try {
                Class<?>[] arrayOfClass = new Class[] { File.class, String.class, DisplayMetrics.class,
                        int.class};
                sParsePackageMethod = sPackageParserClass.getDeclaredMethod("parsePackage",
                        arrayOfClass);
            } catch(NoSuchMethodException e) {
                sParsePackageMethod = null;
                // maybe android 5.0 parsePackage(File packageFile, int flags)
                Class<?>[] arrayOfClass = new Class[] { File.class, int.class};
                sParsePackageMethod50 = sPackageParserClass.getDeclaredMethod("parsePackage",
                        arrayOfClass);
            }

            sPackageParser_collectCertificatesMethod = sPackageParserClass.getDeclaredMethod(
                    "collectCertificates", sPackageParserPackageClass, int.class);
            sCountActionsMethod = sPackageParserActivityIntentInfoClass.getMethod("countActions");
            sGetActionMethod = sPackageParserActivityIntentInfoClass.getMethod("getAction",
                    int.class);
            sGetPackageSizeInfoMethod = PackageManager.class.getMethod("getPackageSizeInfo",
                    String.class, IPackageStatsObserver.class);
            sFreeStorageAndNotifyMethod = PackageManager.class.getMethod("freeStorageAndNotify",
                    long.class, IPackageDataObserver.class);
            try {
                sSetPackageNameMethod = sPackageParserPackageClass.getDeclaredMethod(
                        "setPackageName",
                        String.class);
            } catch (NoSuchMethodException nsme) {
                if (DEBUG) {
                    nsme.printStackTrace();
                }
                sSetPackageNameMethod = null;
            }
            sPackageParser_SignaturesField = sPackageParserPackageClass.getField("mSignatures");
            sActivitiesField = sPackageParserPackageClass.getField("activities");
            sServicesField = sPackageParserPackageClass.getField("services");
            sReceiversField = sPackageParserPackageClass.getField("receivers");
            sPackageNameField = sPackageParserPackageClass.getField("packageName");
            sApplicationInfoField = sPackageParserPackageClass.getField("applicationInfo");
            sMVersionCodeField = sPackageParserPackageClass.getField("mVersionCode");
            sActivityIntentsField = sPackageParserActivityClass.getField("intents");
            sActivityInfoField = sPackageParserActivityClass.getField("info");
            sComponentIntentsField = sPackageParserComponentClass.getField("intents");
            sServiceInfoField = sPackageParserServiceClass.getField("info");
//            sMpmField = sApplicationPackageManagerClass.getDeclaredField("mPM");
//            sMpmField.setAccessible(true);
            if (DEBUG) Log.d(TAG, "==== good, it works");
        } catch (ClassNotFoundException e) {
            if (DEBUG) e.printStackTrace();
            sPackageParserClass = null;
            sPackageParserActivityClass = null;
            sPackageParserServiceClass = null;
            sPackageParserComponentClass = null;
            sPackageParserActivityIntentInfoClass = null;
        } catch (NoSuchMethodException e) {
            if (DEBUG) e.printStackTrace();
            sPackageParserConstructor50 = null;
            sParsePackageMethod50 = null;
            sGetPackageSizeInfoMethod = null;
            sFreeStorageAndNotifyMethod = null;
        } catch (NoSuchFieldException e) {
            if (DEBUG) e.printStackTrace();
            sReceiversField = null;
            sActivityIntentsField = null;
            sActivityInfoField = null;
            sApplicationInfoField = null;
            sMVersionCodeField = null;
            sPackageNameField = null;
            sActivitiesField = null;
            sServicesField = null;
            sComponentIntentsField = null;
            sServiceInfoField = null;
//            sMpmField = null;
        }

        try {
            if (sPackageParserClass != null) {
                sPackageParser_RequestedPermissionsField = sPackageParserPackageClass.getField("requestedPermissions");
            }
        } catch (Exception e) {
            if (DEBUG) Log.w(TAG, "unexpected exception", e);
        }
        try {
            sIPackageManagerClass = Class.forName(CLASSNAME_IPACKAGEMANAGER);
            Class<?> clazz = Class.forName(CLASSNAME_IPACKAGEMANAGER_STUB);
            sAsInterfaceMethod = clazz.getMethod("asInterface", IBinder.class);
        } catch (Exception e) {
            if (DEBUG) Log.w(TAG, "unexpected exception", e);
            sIPackageManagerClass = null;
            sAsInterfaceMethod = null;
        }

        // IPackageManager.getInstallLocation
        try {
            sGetInstallLocationMethod = sIPackageManagerClass.getMethod("getInstallLocation");
        } catch (Exception e) {
            if (DEBUG) Log.w(TAG, "not supported by system", e);
        }

        // IPackageManager.movePackage
        try {
            sMovePackageMethod = sIPackageManagerClass.getMethod("movePackage",
                    String.class, IPackageMoveObserver.class, int.class);
        } catch (Exception e) {
            if (DEBUG) Log.w(TAG, "not supported by system", e);
        }

        // IPackageManager.setComponentEnabledSetting
        try {
            sSetComponentEnabledSettingMethod = sIPackageManagerClass.getMethod(
                    "setComponentEnabledSetting", ComponentName.class, int.class, int.class);
            sSetComponentEnabledSettingMethodVersion = VERSION_1;
        } catch (Exception e) {
            try {
                // Android 4.0
                sSetComponentEnabledSettingMethod = sIPackageManagerClass.getMethod(
                        "setComponentEnabledSetting", ComponentName.class, int.class, int.class, int.class);
                sSetComponentEnabledSettingMethodVersion = VERSION_2;
            } catch (Exception e1) {
                if (DEBUG) Log.w(TAG, "not supported by system", e1);
            }
        }

        // IPackageManager.setApplicationEnabledSetting
        try {
            sSetApplicationEnabledSettingMethod = sIPackageManagerClass.getMethod(
                    "setApplicationEnabledSetting", String.class, int.class, int.class);
            sSetApplicationEnabledSettingMethodVersion = VERSION_1;
        } catch (Exception e) {
            try {
                // Android 4.0
                sSetApplicationEnabledSettingMethod = sIPackageManagerClass.getMethod(
                        "setApplicationEnabledSetting", String.class, int.class, int.class, int.class);
                sSetApplicationEnabledSettingMethodVersion = VERSION_2;
            } catch (Exception e1) {
                try {
                    // android 4.3
                    sSetApplicationEnabledSettingMethod = sIPackageManagerClass.getMethod(
                            "setApplicationEnabledSetting", String.class, int.class,
                            int.class, int.class, String.class);
                    sSetApplicationEnabledSettingMethodVersion = VERSION_3;
                } catch (Exception e2) {
                    if (DEBUG) Log.w(TAG, "not supported by system", e2);
                }
            }
        }

        // IPackageManager.installPackage
        try {
            sInstallPackageMethod = sIPackageManagerClass.getMethod("installPackage",
                    Uri.class, IPackageInstallObserver.class, int.class, String.class);
        } catch (Exception e) {
            if (DEBUG) Log.w(TAG, "not supported by system", e);
        }

// android 5.0
//        void installPackage(in String originPath,
//                in IPackageInstallObserver2 observer,
//                int flags,
//                in String installerPackageName,
//                in VerificationParams verificationParams,
//                in String packageAbiOverride);
        try {
            sVerificationParamsClass = Class.forName(CLASSNAME_VERIFICATIONPARAMS, false,
                    Thread.currentThread().getContextClassLoader());
            sManifestDigestClass = Class.forName(CLASSNAME_MANIFESTDIGEST, false,
                    Thread.currentThread().getContextClassLoader());
//            VerificationParams(Uri verificationURI, Uri originatingURI, Uri referrer,
//                    int originatingUid, ManifestDigest manifestDigest)
            sVerificationParamsConstructor = sVerificationParamsClass.getConstructor(Uri.class, Uri.class, Uri.class,
                    int.class, sManifestDigestClass);
            sInstallPackageMethod50 = sIPackageManagerClass.getMethod("installPackage",
                    String.class, IPackageInstallObserver2.class, int.class, String.class, sVerificationParamsClass, String.class);
        } catch (Exception e) {
            if (DEBUG) Log.w(TAG, "not supported by system", e);
        }

        // IPackageManager.deletePackage or IPackageManager.deletePackageAsUser
        try {
            sDeletePackageMethod = sIPackageManagerClass.getMethod("deletePackage",
                    String.class, IPackageDeleteObserver.class, int.class);
            sDeletePackageMethodVersion = VERSION_1;
        } catch (Exception e) {
            try {
                // Android 4.3
                sDeletePackageMethod = sIPackageManagerClass.getMethod("deletePackageAsUser",
                        String.class, IPackageDeleteObserver.class, int.class, int.class);
                sDeletePackageMethodVersion = VERSION_2;
            } catch (Exception e1) {
                if (DEBUG) Log.w(TAG, "not supported by system", e1);
            }
        }

        try {
            // Android 5.0
            // void deletePackage(in String packageName, IPackageDeleteObserver2 observer, int userId, int flags);
            sDeletePackageMethod50 = sIPackageManagerClass.getMethod("deletePackage",
                    String.class, IPackageDeleteObserver2.class, int.class, int.class);
        } catch (Exception e) {
            if (DEBUG) Log.w(TAG, "not supported by system", e);
        }
    }

    public static Object asInterface(IBinder binder) {
        if (sAsInterfaceMethod != null) {
            try {
                Method localMethod = sAsInterfaceMethod;
                Object[] arrayOfObject = new Object[] {binder};
                Object ret = localMethod.invoke(null, arrayOfObject);
                return ret;
            } catch (IllegalAccessException e) {
                if (DEBUG) e.printStackTrace();
            } catch (InvocationTargetException e) {
                if (DEBUG) e.printStackTrace();
            }
        }
        return null;
    }

    public static int getInstallLocation() {
        if (sGetInstallLocationMethod != null) {
            try {
                Object pm = asInterface(
                        ServiceManagerCompat.getService("package"));
                Method localMethod = sGetInstallLocationMethod;
                Object[] arrayOfObject = new Object[0];
                Object ret = localMethod.invoke(pm, arrayOfObject);
                return (Integer) ret;
            } catch (Exception e) {
                if (DEBUG) Log.w(TAG, "Failed to get default install location: " + e);
            }
        }
        return INSTALL_LOCATION_AUTO;
    }

    
    public static void setComponentEnabledSetting(Object pm, ComponentName cn, int newState,
                                                  int flags) {
        if (sSetComponentEnabledSettingMethod != null) {
            try {
                if (sSetComponentEnabledSettingMethodVersion == VERSION_1) {
                    sSetComponentEnabledSettingMethod.invoke(pm, cn, newState, flags);
                } else if (sSetComponentEnabledSettingMethodVersion == VERSION_2) {
                    // Android 4.0
                    sSetComponentEnabledSettingMethod.invoke(pm, cn, newState, flags, UserIdCompat.myUserId());
                } else {
                    Log.e(TAG, "bad logic, please check");
                }
            } catch (Exception e) {
                if (DEBUG) Log.w(TAG, "failed to invoke setComponentEnabledSetting", e);
            }
        } else {
            if (DEBUG) Log.w(TAG, "setComponentEnabledSetting not supported by system");
        }
    }

    
    public static void setApplicationEnabledSetting(Context cxt, Object pm, String pkgName, int newState,
                                                    int flags) {
        if (sSetApplicationEnabledSettingMethod != null) {
            try {
                if (sSetApplicationEnabledSettingMethodVersion == VERSION_1) {
                    sSetApplicationEnabledSettingMethod.invoke(pm, pkgName, newState, flags);
                } else if (sSetApplicationEnabledSettingMethodVersion == VERSION_2) {
                    // Android 4.0
                    sSetApplicationEnabledSettingMethod.invoke(pm, pkgName, newState, flags,
                            UserIdCompat.myUserId());
                } else if (sSetApplicationEnabledSettingMethodVersion == VERSION_3) {
                    // Android 4.3
                    sSetApplicationEnabledSettingMethod.invoke(pm, pkgName, newState, flags,
                            UserIdCompat.myUserId(), cxt.getPackageName());
                } else {
                    Log.e(TAG, "bad logic, please check");
                }
            } catch (Exception e) {
                if (DEBUG) Log.w(TAG, "failed to invoke setApplicationEnabledSetting", e);
            }
        } else {
            if (DEBUG) Log.w(TAG, "setApplicationEnabledSetting not supported by system");
        }
    }

    
    public static boolean installPackage(Object pm, Uri packageUri, IPackageInstallObserver observer,
                                         int flags, String name) {
        if (sInstallPackageMethod != null) {
            try {
                Method localMethod = sInstallPackageMethod;
                Object[] arrayOfObject = new Object[] { packageUri, observer, flags, name };
                localMethod.invoke(pm, arrayOfObject);
                return true;
            } catch (Exception e) {
                if (DEBUG) Log.w(TAG, "failed to invoke installPackage", e);
            }
        } else {
            if (DEBUG) Log.w(TAG, "installPackage not supported by system");
        }
        return false;
    }

    // add for android 5.0
    
    public static boolean installPackage50(Object pm, Uri packageUri, IPackageInstallObserver2 observer,
                                           int flags, String name) {
        if (sInstallPackageMethod50 != null) {
            try {
                Object vp = sVerificationParamsConstructor.newInstance(null, null, null, -1, null);
                Method localMethod = sInstallPackageMethod50;
                Object[] arrayOfObject = new Object[] { packageUri.getPath(), observer, flags, name, vp, null};
                localMethod.invoke(pm, arrayOfObject);
                return true;
            } catch (Exception e) {
                if (DEBUG) Log.w(TAG, "failed to invoke installPackage50", e);
            }
        } else {
            if (DEBUG) Log.w(TAG, "installPackage50 not supported by system");
        }
        return false;
    }

    
    public static boolean movePackage(Object pm, String packageName, IPackageMoveObserver observer,
                                      int flags) {
        if (sMovePackageMethod != null) {
            try {
                Method localMethod = sMovePackageMethod;
                Object[] arrayOfObject = new Object[] { packageName, observer, flags };
                localMethod.invoke(pm, arrayOfObject);
                return true;
            } catch (Exception e) {
                if (DEBUG) Log.w(TAG, "failed to invoke movePackage", e);
            }
        } else {
            if (DEBUG) Log.w(TAG, "movePackage not supported by system");
        }
        return false;
    }

    
    public static boolean deletePackage(Object pm, String pkgName, IPackageDeleteObserver observer,
                                        int flags) {
        if (sDeletePackageMethod != null) {
            try {
                if (sDeletePackageMethodVersion == VERSION_1) {
                    sDeletePackageMethod.invoke(pm, pkgName, observer, flags);
                } else if (sDeletePackageMethodVersion == VERSION_2) {
                    // Android 4.3
                    sDeletePackageMethod.invoke(pm, pkgName, observer, UserIdCompat.myUserId(), flags);
                } else {
                    Log.e(TAG, "bad logic, please check");
                }
                return true;
            } catch (Exception e) {
                if (DEBUG) Log.w(TAG, "failed to invoke deletePackage", e);
            }
        } else {
            if (DEBUG) Log.w(TAG, "deletePackage not supported by system");
        }
        return false;
    }

    // add for android 5.0
    
    public static boolean deletePackage50(Object pm, String pkgName, IPackageDeleteObserver2 observer,
                                          int flags) {
        if (sDeletePackageMethod50 != null) {
            try {
                sDeletePackageMethod50.invoke(pm, pkgName, observer, UserIdCompat.myUserId(), flags);
                return true;
            } catch (Exception e) {
                if (DEBUG) Log.w(TAG, "failed to invoke deletePackage50", e);
            }
        } else {
            if (DEBUG) Log.w(TAG, "deletePackage50 not supported by system");
        }
        return false;
    }

    public static boolean deletePackageCache(Object pm, String pkgName, IPackageDataObserver observer) {
        try {
            Method deleteCache = pm.getClass().getMethod("deleteApplicationCacheFiles",
                    String.class, IPackageDataObserver.class);
            deleteCache.invoke(pm, pkgName, observer);
            return true;
        } catch (IllegalArgumentException e) {
            if (DEBUG) e.printStackTrace();
        } catch (IllegalAccessException e) {
            if (DEBUG) e.printStackTrace();
        } catch (InvocationTargetException e) {
            if (DEBUG) e.printStackTrace();
        } catch (NoSuchMethodException e) {
            if (DEBUG) e.printStackTrace();
        }
        return false;
    }

    public static int packageInfo_installLocation(PackageInfo obj) {
        if (sInstallLocationField != null) {
            try {
                Field localField = sInstallLocationField;
                Object ret = localField.get(obj);
                return (Integer) ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "packageInfo_installLocation failure");
        return INSTALL_LOCATION_UNSPECIFIED;
    }

    public static Object createPackageParser(String path) {
        if (sPackageParserConstructor != null) {
            try {
                Constructor<?> constructor = sPackageParserConstructor;
                return constructor.newInstance(path);
            } catch (IllegalArgumentException e) {
                if (DEBUG) e.printStackTrace();
            } catch (InstantiationException e) {
                if (DEBUG) e.printStackTrace();
            } catch (IllegalAccessException e) {
                if (DEBUG) e.printStackTrace();
            } catch (InvocationTargetException e) {
                if (DEBUG) e.printStackTrace();
            }
        } else if (sPackageParserConstructor50 != null) {
            try {
                return sPackageParserConstructor50.newInstance();
            } catch (Exception e) {
                if (DEBUG) e.printStackTrace();
            }
        }
        if (DEBUG) Log.e(TAG, "fail createPackageParser");
        return null;
    }

    public static Object packageParser_parsePackage(Object obj, File sourceFile,
                                                    String destCodePath, DisplayMetrics metrics, int flags) {
        if (sParsePackageMethod != null) {
            try {
                Method localMethod = sParsePackageMethod;
                Object[] arrayOfObject = new Object[] {sourceFile, destCodePath, metrics, flags};
                Object ret = localMethod.invoke(obj, arrayOfObject);
                return ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            } catch (InvocationTargetException localInvocationTargetException) {
                // ignore this, will to the final
            }
        } else if (sParsePackageMethod50 != null) {
            try {
                Object[] arrayOfObject = new Object[] {sourceFile, flags};
                Object ret = sParsePackageMethod50.invoke(obj, arrayOfObject);
                return ret;
            } catch (Exception e) {
                // ignore this, will to the final
            }
        }
        // if anything wrong, will be here
        if (DEBUG) Log.e(TAG, "packageParser_parsePackage failure");
        return null;
    }

    public static Object packageParser_collectCertificates(Object obj, Object pkg, int flags) {
        if (sPackageParser_collectCertificatesMethod != null) {
            try {
                Method localMethod = sPackageParser_collectCertificatesMethod;
                Object[] arrayOfObject = new Object[] {pkg, flags};
                Object ret = localMethod.invoke(obj, arrayOfObject);
                return ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            } catch (InvocationTargetException localInvocationTargetException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "packageParser_collectCertificates failure");
        return null;
    }

    /**
     * Cannot called in UI thread (ANR)
     */
    public static Signature[] getPackageArchiveSignature(Object packageParser, Object pkg) {
        if (pkg == null) {
            return null;
        }
        packageParser_collectCertificates(packageParser, pkg, 0);
        return package_signatures(pkg);
    }

    /**
     * Cannot called in UI thread (ANR)
     */
    public static Signature[] getPackageArchiveSignature(String archiveFilePath) {
        Object packageParser = createPackageParser(archiveFilePath);
        if (packageParser == null) {
            return null;
        }
        DisplayMetrics metrics = new DisplayMetrics();
        metrics.setToDefaults();
        final File sourceFile = new File(archiveFilePath);
        Object pkg = packageParser_parsePackage(packageParser, sourceFile,
                archiveFilePath, metrics, 0);
        return getPackageArchiveSignature(packageParser, pkg);
    }

    public static boolean packageManager_getPackageSizeInfo(PackageManager obj, String packageName,
                                                            IPackageStatsObserver observer) {
        if (sGetPackageSizeInfoMethod != null) {
            try {
                Method localMethod = sGetPackageSizeInfoMethod;
                Object[] arrayOfObject = new Object[] {packageName, observer};
                localMethod.invoke(obj, arrayOfObject);
                return true;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            } catch (InvocationTargetException localInvocationTargetException) {
                // ignore this, will to the final
            }
        }
        // if anything wrong, will be here
        if (DEBUG) Log.e(TAG, "packageManager_getPackageSizeInfo failure");
        return false;
    }

    public static boolean packageManager_freeStorageAndNotify(PackageManager obj, long size,
                                                              IPackageDataObserver observer) {
        if (sFreeStorageAndNotifyMethod != null) {
            try {
                Method localMethod = sFreeStorageAndNotifyMethod;
                Object[] arrayOfObject = new Object[] {size, observer};
                localMethod.invoke(obj, arrayOfObject);
                return true;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            } catch (InvocationTargetException localInvocationTargetException) {
                // ignore this, will to the final
            }
        }
        // if anything wrong, will be here
        if (DEBUG) Log.e(TAG, "packageManager_freeStorageAndNotify failure");
        return false;
    }

    public static int activityIntentInfo_countActions(Object obj) {
        if (sCountActionsMethod != null) {
            try {
                Method localMethod = sCountActionsMethod;
                Object[] arrayOfObject = new Object[0];
                Object ret = localMethod.invoke(obj, arrayOfObject);
                return (Integer) ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            } catch (InvocationTargetException localInvocationTargetException) {
                // ignore this, will to the final
            }
        }
        // if anything wrong, will be here
        if (DEBUG) Log.e(TAG, "activityIntentInfo_countActions failure");
        return 0;
    }

    public static String activityIntentInfo_getAction(Object obj, int i) {
        if (sGetActionMethod != null) {
            try {
                Method localMethod = sGetActionMethod;
                Object[] arrayOfObject = new Object[] { i };
                Object ret = localMethod.invoke(obj, arrayOfObject);
                return (String) ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            } catch (InvocationTargetException localInvocationTargetException) {
                // ignore this, will to the final
            }
        }
        // if anything wrong, will be here
        if (DEBUG) Log.e(TAG, "activityIntentInfo_getAction failure");
        return null;
    }

    public static void package_setPackageName(Object obj, String pkgName) {
        if (sSetPackageNameMethod != null) {
            try {
                Method localMethod = sSetPackageNameMethod;
                Object[] arrayOfObjetc = new Object[] { pkgName };
                localMethod.invoke(obj, arrayOfObjetc);
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            } catch (InvocationTargetException localInvocationTargetException) {
                // ignore this, will to the final
            }
        }
    }

    public static ArrayList<?> package_receivers(Object obj) {
        if (sReceiversField != null) {
            try {
                Field localField = sReceiversField;
                Object ret = localField.get(obj);
                return (ArrayList<?>) ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "package_receivers failure");
        return null;
    }

    public static ArrayList<?> package_activitys(Object obj) {
        if (sActivitiesField != null) {
            try {
                return (ArrayList<?>) sActivitiesField.get(obj);
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "package_activitys failure");
        return null;
    }

    public static ArrayList<?> package_services(Object obj) {
        if (sServicesField != null) {
            try {
                return (ArrayList<?>) sServicesField.get(obj);
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "package_services failure");
        return null;
    }

    public static Signature[] package_signatures(Object obj) {
        if (sPackageParser_SignaturesField != null) {
            try {
                Field localField = sPackageParser_SignaturesField;
                Object ret = localField.get(obj);
                return (Signature[]) ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "package_signatures failure");
        return null;
    }

    public static ArrayList<String> package_permissions(Object obj) {
        if (sPackageParser_RequestedPermissionsField != null) {
            try {
                ArrayList<String> permissions = (ArrayList<String>) sPackageParser_RequestedPermissionsField.get(obj);
                return permissions;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "package_permissions failure");
        return null;
    }

    public static ApplicationInfo package_applicationInfo(Object obj) {
        if (sApplicationInfoField != null) {
            try {
                Field localField = sApplicationInfoField;
                Object ret = localField.get(obj);
                return (ApplicationInfo) ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "package_applicationInfo failure");
        return null;
    }

    public static String package_packageName(Object obj) {
        if (sPackageNameField != null) {
            try {
                Field localField = sPackageNameField;
                Object ret = localField.get(obj);
                return (String) ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "package_packageName failure");
        return null;
    }

    public static int package_mVersionCode(Object obj) {
        if (sMVersionCodeField != null) {
            try {
                Field localField = sMVersionCodeField;
                Object ret = localField.get(obj);
                return (Integer) ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG)
            Log.e(TAG, "package_mVersionCode failure");
        return -1;
    }

    public static ArrayList<?> activity_intents(Object obj) {
        if (sActivityIntentsField != null) {
            try {
                Field localField = sActivityIntentsField;
                Object ret = localField.get(obj);
                return (ArrayList<?>) ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "activity_intents failure");
        return null;
    }

    public static ArrayList<?> component_intents(Object obj) {
        if (sComponentIntentsField != null) {
            try {
                return (ArrayList<?>) sComponentIntentsField.get(obj);
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.e(TAG, "component_intents failure");
        return null;
    }

    public static ActivityInfo activity_info(Object obj) {
        if (sActivityInfoField != null) {
            try {
                Field localField = sActivityInfoField;
                Object ret = localField.get(obj);
                return (ActivityInfo) ret;
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.d(TAG, "activity_info failure");
        return null;
    }

    public static ServiceInfo service_info(Object obj) {
        if (sActivityInfoField != null) {
            try {
                return (ServiceInfo) sServiceInfoField.get(obj);
            } catch (IllegalAccessException localIllegalAccessException) {
                // ignore this, will to the final
            }
        }
        if (DEBUG) Log.d(TAG, "service_info failure");
        return null;
    }

    public static PackageInfo generatePackageInfo(Object pkg, int flag) {
        if (pkg == null) {
            return null;
        }

        PackageInfo pi = null;
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();

        try {
            // v2.2
            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.FROYO) {
                if (sGeneratePackageInfo == null) {
                    sGeneratePackageInfo = sPackageParserClass.getMethod("generatePackageInfo",
                            sPackageParserPackageClass, int[].class, int.class);
                }
                pi = (PackageInfo) sGeneratePackageInfo.invoke(null, pkg, null, flag);

                // v2.3 ~ v4.0.3
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1) {
                if (sGeneratePackageInfo == null) {
                    sGeneratePackageInfo = sPackageParserClass.getMethod("generatePackageInfo",
                            sPackageParserPackageClass,
                            int[].class, int.class, long.class, long.class);
                }
                pi = (PackageInfo) sGeneratePackageInfo.invoke(null, pkg, null, flag, 0, 0);

                // v4.1
            } else if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN) {
                if (sGeneratePackageInfo == null) {
                    sGeneratePackageInfo = sPackageParserClass.getMethod("generatePackageInfo",
                            sPackageParserPackageClass,
                            int[].class, int.class, long.class, long.class,
                            HashSet.class, boolean.class, int.class);
                }
                pi = (PackageInfo) sGeneratePackageInfo.invoke(null, pkg, null, flag, 0, 0, null, false, 0);

            } else {
                if (sGeneratePackageInfo == null) {
                    if (sClazzPackageUserState == null) {
                        sClazzPackageUserState = classLoader.loadClass("android.content.pm.PackageUserState");
                    }
                    // v4.2 ~ v5.0
                    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP) {
                        sGeneratePackageInfo = sPackageParserClass.getMethod("generatePackageInfo",
                                sPackageParserPackageClass,
                                int[].class, int.class, long.class, long.class,
                                HashSet.class, sClazzPackageUserState);
                        // v5.1
                    } else if(Build.VERSION.SDK_INT <= Build.VERSION_CODES.LOLLIPOP_MR1) {
                        final Class<?> clsArraySet = classLoader.loadClass("android.util.ArraySet");
                        sGeneratePackageInfo = sPackageParserClass.getMethod("generatePackageInfo",
                                sPackageParserPackageClass,
                                int[].class, int.class, long.class, long.class,
                                clsArraySet, sClazzPackageUserState);

                        // v6.0
                    } else {
                        sGeneratePackageInfo = sPackageParserClass.getMethod("generatePackageInfo",
                                sPackageParserPackageClass,
                                int[].class, int.class, long.class, long.class,
                                Set.class, sClazzPackageUserState);
                    }
                }
                Object state = sClazzPackageUserState.newInstance();
                pi = (PackageInfo) sGeneratePackageInfo.invoke(null, pkg, null, flag, 0, 0, null, state);
            }
        } catch (Exception e) {
            if (DEBUG) Log.w(TAG, "reflect generatePackageInfo fail", e);
        }

        return pi;
    }

    /**
    public static boolean isPackageManagerServiceAlive(PackageManager pm) {
        boolean isAlive = true;
        if (sMpmField != null) {
            try {
                Object obj = sMpmField.get(pm);
                if (obj instanceof IInterface) {
                    IInterface i = (IInterface) obj;
                    IBinder b = i.asBinder();
                    return b.isBinderAlive();
                }
            } catch (Exception e) {
            }
        }
        if (DEBUG) {
            throw new RuntimeException("ApplicationPackageManager mPm failed!");
        }
        return isAlive;
    }*/
}
