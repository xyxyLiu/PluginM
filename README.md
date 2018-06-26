# PluginM
Android插件化框架，支持APK免安装运行

此插件框架主要目的用于加载**非第三方插件**，即插件会认为其在插件框架中运行，且大部分情况下对宿主或其它插件存在依赖。

如果你需要类似应用双开加载任意第三方Apk的能力，请参考[VirtualApp](https://github.com/asLody/VirtualApp)或[DroidPlugin](https://github.com/DroidPluginTeam/DroidPlugin)，

## 特性
* 支持 API 14+
* 低入侵，对插件开发透明，无需任何修改即可直接加载插件apk
* 支持Android四大组件，Activity, Service, ContentProvider, Broadcast等，支持so加载。
* 支持多进程，提供4种进程模式可供选择。
* 调试方便，编译生成的插件apk即可以独立运行，也可以在插件框架中运行。
* 框架原理较为简洁，Hook单一，易于集成。
* 插件间，插件与宿主间除了可以通过标准的系统api通信，PluginApi中还提供一套**IInvoker框架**，提供支持跨进程的函数调用，Binder服务获取。

## 限制
* 不支持Activity的TaskAffinity属性, 不支持overridePendingTransition中携带插件自定义资源(可以通过PluginApi接口动态的获取宿主的资源)
* 不支持通知中携带插件自定义资源。
* 不支持系统或其它App直接调起插件组件，需要宿主预埋相应的组件做桥接*处理（即调用关系为： 系统或其它App->宿主->插件）。
* 暂不支持插件中宿主资源与插件资源合并，即插件的资源是隔离的，无法在插件中直接使用宿主的资源(可以通过PluginApi接口动态的获取宿主的资源)
* 插件中需要的所有权限需要预埋到宿主中。

## 项目结构
* PluginApi: 插件通信Api模块。此模块专门为非独立插件提供插件间，插件与宿主间的通信。如果插件不需要与宿主或其它插件进行通信，可以不用依赖此模块。
* PluginManager: 插件核心框架。

* testhost: 测试宿主Demo，可以免安装启动/sdcard/PluginM/目录中的apk
* testplugin: 测试插件Demo, 主要包含Android四大组件，so加载的测试。
* testplugin2: 另一个测试插件Demo, 主要包含插件间，插件与宿主间调用测试。
* pluginsharelib: 插件共享代码库，可以作为公共功能模块在插件中直接使用。


## 运行Demo
执行这个脚本直接为为你编译好testplugin,testplugin2的apk，并启动testhost, 进行插件测试：
```
chmod +x install-test
./install-test 
```
如果你还想测试其它apk作为插件，可以将apk放入/sdcard/PluginM/目录中即可。

## 项目接入

### 宿主工程：

1. 将PluginM作为library引入到您的主项目中
2. 预埋插件中需要使用到的权限
3. 在您的Application中添加如下代码：
``` java

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        
        // 插件配置
        PluginConfigs pluginConfigs = new PluginConfigs();
        
        // 选择插件进程模式，总共为4种，默认为PROCESS_TYPE_INDEPENDENT
        // 1. PROCESS_TYPE_INDEPENDENT：独立进程模式, 即为每个插件独享一个进程。
        // 2. PROCESS_TYPE_SINGLE：单一进程模式, 即所有插件都分配在一个固定的进程。
        // 3. PROCESS_TYPE_DUAL：双进程模式, 即所有插件都分配在两个固定的进程（一个前台进程，一个后台进程）。
        // 4. PROCESS_TYPE_COMPLETE：完整进程模式, 即所有插件都完全拥有自身全部的进程，进程名与插件声明的进程名称一致。
        进程名与插件包名相同的在一个特定进程，否则在另一个特定进程。
        pluginConfigs.setProcessType(pluginConfigs.PROCESS_TYPE_INDEPENDENT)
        
        // 是否对插件无法加载的类使用宿主classload进行加载，默认为true
        pluginConfigs.setUseHostLoader(true);
        
        PluginM.onAttachBaseContext(this, pluginConfigs);
    }

```


### 插件工程：
* 如果插件不依赖于宿主或其它插件，则**不用做任何处理**，直接编译出APK即可被加载
* 如果插件依赖与宿主或其它插件，则需要依赖于PluginApi，请以provided形式依赖PluginApi编译生成的Jar.



## 主要接口

### 宿主中:
``` java
    // 安装插件
    PluginM.install(pluginApkPath);
    
    // 卸载插件(插件已运行时无法卸载，需要重启进程后卸载)
    PluginM.uninstall(pluginPackageName);
    
    // 获取已安装插件列表
    PluginM.getAllInstalledPlugins();
    
    // 宿主中启动插件Activity
    PluginM.startActivity(context, intent);
    
    // 宿主中启动插件Service
    PluginM.startService(context, intent);
    
    // 宿主中访问插件ContentProvider
    PluginM.getContentResolver(context).query(....);
    
    // 使用IInvoker框架进行函数式调用 (调用宿主或其它插件）
    IInvokeResult result = PluginM.invoke(packageName, serviceName, methodName, params, callback);
    
    // 使用IInvoker框架进行Binder服务获取 (调用宿主或其它插件）
    IBinder binder = PluginM.fetchService(packageName, serviceName);
    
```

### 插件中:
**插件中启动四大组件不需要任何特殊处理**
``` java
    // 插件中启动四大组件不需要任何特殊处理。
    Intent intent = new Intent(this, PluginActivity.class);
    startActivity(intent);
    
    // 获取宿主context
    PluginHelper.getHostContext(context);
    
    // 获取插件package信息
    PluginHelper.getPluginPackageInfo(context, packageName, flags)
     
    // 使用IInvoker框架进行函数式调用（调用宿主或其它插件）
    IInvokeResult result = PluginHelper.invoke(packageName, serviceName, methodName, params, callback);
            
    // 使用IInvoker框架进行Binder服务获取 (调用宿主或其它插件）
    IBinder binder = PluginHelper.fetchService(packageName, serviceName);
```

## IInvoker框架

除了四大组件的标准系统api提供的通信机制，PluginApi还为插件间，插件与宿主间的通讯提供了另一种标准接口框架。详细api请参考[IInvoker](./PluginApi/src/main/java/com/reginald/pluginm/pluginapi/IInvoker.java)

**宿主或插件**都可以在Manifest中注册一个或多个IInvoker，用于提供对外部（插件或宿主）统一的函数接口调用和Binder服务获取。
``` xml
<application .... >
    
    <!-- 此插件中在插件的com.example.testplugin进程中注册了一个服务名称为main的IInvoker, 
    其具体实现在com.example.testplugin.MyPluginInvoker -->
    <meta-data android:name="pluginm_invoker"
                android:value="[
                {'service':'main', 'class':'com.example.testplugin.MyPluginInvoker', 'process':'com.example.testplugin'}
                ]" />

    ......
    ......

</application>

```
service： 每个IInvoker都必须配置一个服务名称，同包名下不可以相同。

class: 每个IInvoker都必须配置一个实现IInvoker接口的实现类。

process: 每个IInvoker都必须存在于一个进程，如果未填写，则默认为与包名相同的进程。

## License
[Apache v2.0](./LICENSE)

## 参考
* [DroidPlugin](https://github.com/DroidPluginTeam/DroidPlugin)
* [RePlugin](https://github.com/Qihoo360/RePlugin)
* [VirtualApk](https://github.com/didi/VirtualAPK)

## 反馈
tonyreginald@gmail.com
