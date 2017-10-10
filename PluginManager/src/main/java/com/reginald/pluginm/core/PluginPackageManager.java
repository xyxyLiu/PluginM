package com.reginald.pluginm.core;

import android.annotation.TargetApi;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.ApplicationInfo;
import android.content.pm.FeatureInfo;
import android.content.pm.InstrumentationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageInstaller;
import android.content.pm.PackageManager;
import android.content.pm.PermissionGroupInfo;
import android.content.pm.PermissionInfo;
import android.content.pm.ProviderInfo;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.content.res.XmlResourceParser;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.UserHandle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.annotation.XmlRes;

import com.reginald.pluginm.PluginInfo;
import com.reginald.pluginm.reflect.MethodUtils;
import com.reginald.pluginm.utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by lxy on 2017/8/13.
 */

public class PluginPackageManager extends PackageManager {
    private static final String TAG = "PluginPackageManager";
    private final static int sDefaultFlags = PackageManager.GET_SHARED_LIBRARY_FILES;
    private PluginManager mPluginManager;
    private PackageManager mBase;

    public PluginPackageManager(Context hostContext, PackageManager base) {
        mPluginManager = PluginManager.getInstance(hostContext);
        mBase = base;
    }

    @Override
    public PackageInfo getPackageInfo(String packageName, int flags) throws NameNotFoundException {
        try {
            PackageInfo packageInfo = mPluginManager.getPackageInfo(packageName, flags);
            if (packageInfo != null) {
                return packageInfo;
            }
        } catch (Exception e) {
            Logger.e(TAG, "getPackageInfo() packageName = " + packageName, e);
        }

        return mBase.getPackageInfo(packageName, flags);
    }

    @Override
    public String[] currentToCanonicalPackageNames(String[] names) {
        return mBase.currentToCanonicalPackageNames(names);
    }

    @Override
    public String[] canonicalToCurrentPackageNames(String[] names) {
        return mBase.canonicalToCurrentPackageNames(names);
    }

    @Override
    public Intent getLaunchIntentForPackage(String packageName) {
        // First see if the package has an INFO activity; the existence of
        // such an activity is implied to be the desired front-door for the
        // overall package (such as if it has multiple launcher entries).
        Intent intentToResolve = new Intent(Intent.ACTION_MAIN);
        intentToResolve.addCategory(Intent.CATEGORY_INFO);
        intentToResolve.setPackage(packageName);
        List<ResolveInfo> ris = queryIntentActivities(intentToResolve, 0);

        // Otherwise, try to find a main launcher activity.
        if (ris == null || ris.size() <= 0) {
            // reuse the intent instance
            intentToResolve.removeCategory(Intent.CATEGORY_INFO);
            intentToResolve.addCategory(Intent.CATEGORY_LAUNCHER);
            intentToResolve.setPackage(packageName);
            ris = queryIntentActivities(intentToResolve, 0);
        }
        if (ris == null || ris.size() <= 0) {
            return null;
        }
        Intent intent = new Intent(intentToResolve);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.setClassName(ris.get(0).activityInfo.packageName,
                ris.get(0).activityInfo.name);
        return intent;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Intent getLeanbackLaunchIntentForPackage(String packageName) {
        return mBase.getLeanbackLaunchIntentForPackage(packageName);
    }

    @Override
    public int[] getPackageGids(String packageName) throws NameNotFoundException {
        PluginInfo pluginInfo = mPluginManager.getInstalledPluginInfo(packageName);
        if (pluginInfo != null) {
            packageName = mPluginManager.getHostContext().getPackageName();
        }
        return mBase.getPackageGids(packageName);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public int[] getPackageGids(String packageName, int flags) throws NameNotFoundException {
        PluginInfo pluginInfo = mPluginManager.getInstalledPluginInfo(packageName);
        if (pluginInfo != null) {
            packageName = mPluginManager.getHostContext().getPackageName();
        }
        return mBase.getPackageGids(packageName, flags);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public int getPackageUid(String packageName, int flags) throws NameNotFoundException {
        PluginInfo pluginInfo = mPluginManager.getInstalledPluginInfo(packageName);
        if (pluginInfo != null) {
            packageName = mPluginManager.getHostContext().getPackageName();
        }
        return mBase.getPackageUid(packageName, flags);
    }

    @Override
    public PermissionInfo getPermissionInfo(String name, int flags) throws NameNotFoundException {
        return mBase.getPermissionInfo(name, flags);
    }

    @Override
    public List<PermissionInfo> queryPermissionsByGroup(String group, int flags) throws NameNotFoundException {
        return mBase.queryPermissionsByGroup(group, flags);
    }

    @Override
    public PermissionGroupInfo getPermissionGroupInfo(String name, int flags) throws NameNotFoundException {
        return mBase.getPermissionGroupInfo(name, flags);
    }

    @Override
    public List<PermissionGroupInfo> getAllPermissionGroups(int flags) {
        return mBase.getAllPermissionGroups(flags);
    }

    @Override
    public ApplicationInfo getApplicationInfo(String packageName, int flags) throws NameNotFoundException {
        try {
            PackageInfo packageInfo = mPluginManager.getPackageInfo(packageName, flags);
            Logger.d(TAG, "getApplicationInfo() packageName = " + packageName + " , packageInfo = " + packageInfo);
            if (packageInfo != null) {
                Logger.d(TAG, "getApplicationInfo() packageName = " + packageName + " , applicationInfo = " + packageInfo.applicationInfo);
                return packageInfo.applicationInfo;
            }
        } catch (Exception e) {
            Logger.e(TAG, "getApplicationInfo() packageName = " + packageName, e);
        }
        return mBase.getApplicationInfo(packageName, flags);
    }

    @Override
    public ActivityInfo getActivityInfo(ComponentName component, int flags) throws NameNotFoundException {
        ActivityInfo activityInfo = mPluginManager.getActivityInfo(component, flags);

        if (activityInfo != null) {
            return activityInfo;
        }

        return mBase.getActivityInfo(component, flags);
    }

    @Override
    public ActivityInfo getReceiverInfo(ComponentName component, int flags) throws NameNotFoundException {
        ActivityInfo receiverInfo = mPluginManager.getReceiverInfo(component, flags);

        if (receiverInfo != null) {
            return receiverInfo;
        }

        return mBase.getReceiverInfo(component, flags);
    }

    @Override
    public ServiceInfo getServiceInfo(ComponentName component, int flags) throws NameNotFoundException {
        ServiceInfo serviceInfo = mPluginManager.getServiceInfo(component, flags);

        if (serviceInfo != null) {
            return serviceInfo;
        }

        return mBase.getServiceInfo(component, flags);
    }

    @Override
    public ProviderInfo getProviderInfo(ComponentName component, int flags) throws NameNotFoundException {
        ProviderInfo providerInfo = mPluginManager.getProviderInfo(component, flags);

        if (providerInfo != null) {
            return providerInfo;
        }

        return mBase.getProviderInfo(component, flags);
    }

    @Override
    public List<PackageInfo> getInstalledPackages(int flags) {
        List<PackageInfo> packageInfos = mBase.getInstalledPackages(flags);
        List<PluginInfo> pluginInfos = mPluginManager.getAllInstalledPlugins();

        if (pluginInfos != null) {
            if (packageInfos == null) {
                packageInfos = new ArrayList<>();
            }

            for (PluginInfo plugin : pluginInfos) {
                try {
                    PackageInfo packageInfo = mPluginManager.getPackageInfo(plugin.packageName, flags);
                    if (packageInfo != null) {
                        packageInfos.add(packageInfo);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "getInstalledPackages() try add packageName = " + plugin.packageName, e);
                }
            }
        }

        return packageInfos;
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    public List<PackageInfo> getPackagesHoldingPermissions(String[] permissions, int flags) {
        return mBase.getPackagesHoldingPermissions(permissions, flags);
    }

    @Override
    public int checkPermission(String permName, String pkgName) {
        return mBase.checkPermission(permName, pkgName);
    }

    @TargetApi(Build.VERSION_CODES.M)
    @Override
    public boolean isPermissionRevokedByPolicy(@NonNull String permName, @NonNull String pkgName) {
        return mBase.isPermissionRevokedByPolicy(permName, pkgName);
    }

    @Override
    public boolean addPermission(PermissionInfo info) {
        return mBase.addPermission(info);
    }

    @Override
    public boolean addPermissionAsync(PermissionInfo info) {
        return mBase.addPermissionAsync(info);
    }

    @Override
    public void removePermission(String name) {
        mBase.removePermission(name);
    }

    @Override
    public int checkSignatures(String pkg1, String pkg2) {
        return mBase.checkSignatures(pkg1, pkg2);
    }

    @Override
    public int checkSignatures(int uid1, int uid2) {
        return mBase.checkSignatures(uid1, uid2);
    }

    @Nullable
    @Override
    public String[] getPackagesForUid(int uid) {
        return mBase.getPackagesForUid(uid);
    }

    @Nullable
    @Override
    public String getNameForUid(int uid) {
        return mBase.getNameForUid(uid);
    }

    @Override
    public List<ApplicationInfo> getInstalledApplications(int flags) {
        List<ApplicationInfo> applicationInfos = mBase.getInstalledApplications(flags);

        List<PluginInfo> pluginInfos = mPluginManager.getAllInstalledPlugins();

        if (pluginInfos != null) {
            if (applicationInfos != null) {
                applicationInfos = new ArrayList<>();
            }

            for (PluginInfo plugin : pluginInfos) {
                try {
                    PackageInfo packageInfo = mPluginManager.getPackageInfo(plugin.packageName, flags);
                    if (packageInfo != null && packageInfo.applicationInfo != null) {
                        applicationInfos.add(packageInfo.applicationInfo);
                    }
                } catch (Exception e) {
                    Logger.e(TAG, "getInstalledApplications() try add packageName = " + plugin.packageName, e);
                }
            }
        }

        return applicationInfos;
    }

    @Override
    public String[] getSystemSharedLibraryNames() {
        return mBase.getSystemSharedLibraryNames();
    }

    @Override
    public FeatureInfo[] getSystemAvailableFeatures() {
        return mBase.getSystemAvailableFeatures();
    }

    @Override
    public boolean hasSystemFeature(String name) {
        return mBase.hasSystemFeature(name);
    }

    @TargetApi(Build.VERSION_CODES.N)
    @Override
    public boolean hasSystemFeature(String name, int version) {
        return mBase.hasSystemFeature(name, version);
    }

    @Override
    public ResolveInfo resolveActivity(Intent intent, int flags) {
        ActivityInfo activityInfo = mPluginManager.resolveActivityInfo(intent, flags);
        if (activityInfo != null) {
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.activityInfo = activityInfo;
            return resolveInfo;
        }
        return mBase.resolveActivity(intent, flags);
    }

    @Override
    public List<ResolveInfo> queryIntentActivities(Intent intent, int flags) {
        List<ResolveInfo> resolveInfos = mPluginManager.queryIntentActivities(intent, flags);
        if (resolveInfos != null && !resolveInfos.isEmpty()) {
            return resolveInfos;
        }
        return mBase.queryIntentActivities(intent, flags);
    }

    @Override
    public List<ResolveInfo> queryIntentActivityOptions(ComponentName caller, Intent[] specifics, Intent intent, int flags) {
        //TODO 后期考虑适配
        return mBase.queryIntentActivityOptions(caller, specifics, intent, flags);
    }

    @Override
    public List<ResolveInfo> queryBroadcastReceivers(Intent intent, int flags) {
        List<ResolveInfo> resolveInfos = mPluginManager.queryBroadcastReceivers(intent, flags);
        if (resolveInfos != null && !resolveInfos.isEmpty()) {
            return resolveInfos;
        }
        return mBase.queryBroadcastReceivers(intent, flags);
    }

    @Override
    public ResolveInfo resolveService(Intent intent, int flags) {
        ServiceInfo serviceInfo = mPluginManager.resolveServiceInfo(intent, flags);
        if (serviceInfo != null) {
            ResolveInfo resolveInfo = new ResolveInfo();
            resolveInfo.serviceInfo = serviceInfo;
            return resolveInfo;
        }
        return mBase.resolveService(intent, flags);
    }

    @Override
    public List<ResolveInfo> queryIntentServices(Intent intent, int flags) {
        List<ResolveInfo> resolveInfos = mPluginManager.queryIntentServices(intent, flags);
        if (resolveInfos != null && !resolveInfos.isEmpty()) {
            return resolveInfos;
        }
        return mBase.queryIntentServices(intent, flags);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public List<ResolveInfo> queryIntentContentProviders(Intent intent, int flags) {
        List<ResolveInfo> resolveInfos = mPluginManager.queryIntentContentProviders(intent, flags);
        if (resolveInfos != null && !resolveInfos.isEmpty()) {
            return resolveInfos;
        }
        return mBase.queryIntentContentProviders(intent, flags);
    }

    @Override
    public ProviderInfo resolveContentProvider(String name, int flags) {
        ProviderInfo providerInfo = mPluginManager.resolveProviderInfo(name);
        if (providerInfo != null) {
            return providerInfo;
        }
        return mBase.resolveContentProvider(name, flags);
    }

    @Override
    public List<ProviderInfo> queryContentProviders(String processName, int uid, int flags) {
        //TODO 后期考虑适配
        return mBase.queryContentProviders(processName, uid, flags);
    }

    @Override
    public InstrumentationInfo getInstrumentationInfo(ComponentName className, int flags) throws NameNotFoundException {
        return mBase.getInstrumentationInfo(className, flags);
    }

    @Override
    public List<InstrumentationInfo> queryInstrumentation(String targetPackage, int flags) {
        return mBase.queryInstrumentation(targetPackage, flags);
    }

    @Override
    public Drawable getDrawable(String packageName, @DrawableRes int resid, ApplicationInfo appInfo) {
        Resources pluginResource = getPluginResouces(packageName);
        if (pluginResource != null) {
            return pluginResource.getDrawable(resid);
        }
        return mBase.getDrawable(packageName, resid, appInfo);
    }

    @Override
    public Drawable getActivityIcon(ComponentName activityName) throws NameNotFoundException {
        return getActivityInfo(activityName, sDefaultFlags).loadIcon(this);
    }

    @Override
    public Drawable getActivityIcon(Intent intent) throws NameNotFoundException {
        if (intent.getComponent() != null) {
            return getActivityIcon(intent.getComponent());
        }

        ResolveInfo info = resolveActivity(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (info != null) {
            return info.activityInfo.loadIcon(this);
        }

        throw new NameNotFoundException(intent.toUri(0));
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public Drawable getActivityBanner(ComponentName activityName) throws NameNotFoundException {
        return getActivityInfo(activityName, sDefaultFlags).loadBanner(this);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public Drawable getActivityBanner(Intent intent) throws NameNotFoundException {
        if (intent.getComponent() != null) {
            return getActivityBanner(intent.getComponent());
        }

        ResolveInfo info = resolveActivity(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (info != null) {
            return info.activityInfo.loadBanner(this);
        }

        throw new NameNotFoundException(intent.toUri(0));
    }

    @Override
    public Drawable getDefaultActivityIcon() {
        return mBase.getDefaultActivityIcon();
    }

    @Override
    public Drawable getApplicationIcon(ApplicationInfo info) {
        return mBase.getApplicationIcon(info);
    }

    @Override
    public Drawable getApplicationIcon(String packageName) throws NameNotFoundException {
        return getApplicationIcon(getApplicationInfo(packageName, sDefaultFlags));
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public Drawable getApplicationBanner(ApplicationInfo info) {
        return info.loadBanner(this);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT_WATCH)
    @Override
    public Drawable getApplicationBanner(String packageName) throws NameNotFoundException {
        return getApplicationBanner(getApplicationInfo(packageName, sDefaultFlags));
    }

    @Override
    public Drawable getActivityLogo(ComponentName activityName) throws NameNotFoundException {
        return getActivityInfo(activityName, sDefaultFlags).loadLogo(this);
    }

    @Override
    public Drawable getActivityLogo(Intent intent) throws NameNotFoundException {
        if (intent.getComponent() != null) {
            return getActivityLogo(intent.getComponent());
        }

        ResolveInfo info = resolveActivity(
                intent, PackageManager.MATCH_DEFAULT_ONLY);
        if (info != null) {
            return info.activityInfo.loadLogo(this);
        }

        throw new NameNotFoundException(intent.toUri(0));
    }

    @Override
    public Drawable getApplicationLogo(ApplicationInfo info) {
        return info.loadLogo(this);
    }

    @Override
    public Drawable getApplicationLogo(String packageName) throws NameNotFoundException {
        return getApplicationLogo(getApplicationInfo(packageName, sDefaultFlags));
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Drawable getUserBadgedIcon(Drawable icon, UserHandle user) {
        return mBase.getUserBadgedIcon(icon, user);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    public Drawable getUserBadgeForDensity(UserHandle user, int density) {
        try {
            return (Drawable) MethodUtils.invokeMethod(mBase, "getUserBadgeForDensity", user, density);
        } catch (Exception e) {
            Logger.e(TAG, "getUserBadgeForDensity()", e);
        }
        return null;
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public Drawable getUserBadgedDrawableForDensity(Drawable drawable, UserHandle user, Rect badgeLocation, int badgeDensity) {
        return mBase.getUserBadgedDrawableForDensity(drawable, user, badgeLocation, badgeDensity);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public CharSequence getUserBadgedLabel(CharSequence label, UserHandle user) {
        return mBase.getUserBadgedLabel(label, user);
    }

    @Override
    public CharSequence getText(String packageName, @StringRes int resid, ApplicationInfo appInfo) {
        Resources pluginResource = getPluginResouces(packageName);
        if (pluginResource != null) {
            return pluginResource.getText(resid);
        }
        return mBase.getText(packageName, resid, appInfo);
    }

    @Override
    public XmlResourceParser getXml(String packageName, @XmlRes int resid, ApplicationInfo appInfo) {
        Resources pluginResource = getPluginResouces(packageName);
        if (pluginResource != null) {
            return pluginResource.getXml(resid);
        }
        return mBase.getXml(packageName, resid, appInfo);
    }

    @Override
    public CharSequence getApplicationLabel(ApplicationInfo info) {
        return info.loadLabel(this);
    }

    @Override
    public Resources getResourcesForActivity(ComponentName activityName) throws NameNotFoundException {
        Resources pluginResource = getPluginResouces(activityName.getPackageName());
        if (pluginResource != null) {
            return pluginResource;
        }
        return mBase.getResourcesForActivity(activityName);
    }

    @Override
    public Resources getResourcesForApplication(ApplicationInfo app) throws NameNotFoundException {
        Resources pluginResource = getPluginResouces(app.packageName);
        if (pluginResource != null) {
            return pluginResource;
        }
        return mBase.getResourcesForApplication(app);
    }

    @Override
    public Resources getResourcesForApplication(String appPackageName) throws NameNotFoundException {
        Resources pluginResource = getPluginResouces(appPackageName);
        if (pluginResource != null) {
            return pluginResource;
        }
        return mBase.getResourcesForApplication(appPackageName);
    }

    private Resources getPluginResouces(String pluginPkg) {
        // 目前只可以加载本进程已经加载的插件资源
        PluginInfo pluginInfo = mPluginManager.getLoadedPluginInfo(pluginPkg);
        Logger.d(TAG, "getResourcesForApplication() pluginPkg = " + pluginPkg + " , pluginInfo = " + pluginInfo);
        if (pluginInfo != null) {
            return pluginInfo.resources;
        }
        return null;
    }


    @TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
    @Override
    public void verifyPendingInstall(int id, int verificationCode) {
        mBase.verifyPendingInstall(id, verificationCode);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
    @Override
    public void extendVerificationTimeout(int id, int verificationCodeAtTimeout, long millisecondsToDelay) {
        mBase.extendVerificationTimeout(id, verificationCodeAtTimeout, millisecondsToDelay);
    }

    @Override
    public void setInstallerPackageName(String targetPackage, String installerPackageName) {
        mBase.setInstallerPackageName(targetPackage, installerPackageName);
    }

    @Override
    public String getInstallerPackageName(String packageName) {
        PluginInfo pluginInfo = mPluginManager.getInstalledPluginInfo(packageName);
        if (pluginInfo != null) {
            return mPluginManager.getHostContext().getPackageName();
        }
        return mBase.getInstallerPackageName(packageName);
    }

    @Override
    public void addPackageToPreferred(String packageName) {
        mBase.addPackageToPreferred(packageName);
    }

    @Override
    public void removePackageFromPreferred(String packageName) {
        mBase.removePackageFromPreferred(packageName);
    }

    @Override
    public List<PackageInfo> getPreferredPackages(int flags) {
        return mBase.getPreferredPackages(flags);
    }

    @Override
    public void addPreferredActivity(IntentFilter filter, int match, ComponentName[] set, ComponentName activity) {
        mBase.addPreferredActivity(filter, match, set, activity);
    }

    @Override
    public void clearPackagePreferredActivities(String packageName) {
        mBase.clearPackagePreferredActivities(packageName);
    }

    @Override
    public int getPreferredActivities(@NonNull List<IntentFilter> outFilters, @NonNull List<ComponentName> outActivities, String packageName) {
        return mBase.getPreferredActivities(outFilters, outActivities, packageName);
    }

    @Override
    public void setComponentEnabledSetting(ComponentName componentName, int newState, int flags) {
        if (mPluginManager.getInstalledPluginInfo(componentName.getPackageName()) != null) {
            // if plugin
            return;
        }
        mBase.setComponentEnabledSetting(componentName, newState, flags);
    }

    @Override
    public int getComponentEnabledSetting(ComponentName componentName) {
        if (mPluginManager.getInstalledPluginInfo(componentName.getPackageName()) != null) {
            // if plugin
            return PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        }
        return mBase.getComponentEnabledSetting(componentName);
    }

    @Override
    public void setApplicationEnabledSetting(String packageName, int newState, int flags) {
        if (mPluginManager.getInstalledPluginInfo(packageName) != null) {
            // if plugin
            return;
        }
        mBase.setApplicationEnabledSetting(packageName, newState, flags);
    }

    @Override
    public int getApplicationEnabledSetting(String packageName) {
        if (mPluginManager.getInstalledPluginInfo(packageName) != null) {
            // if plugin
            return PackageManager.COMPONENT_ENABLED_STATE_DEFAULT;
        }
        return mBase.getApplicationEnabledSetting(packageName);
    }

    @Override
    public boolean isSafeMode() {
        return mBase.isSafeMode();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @NonNull
    @Override
    public PackageInstaller getPackageInstaller() {
        return mBase.getPackageInstaller();
    }
}
