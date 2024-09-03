1.  Project can build by classpath 'com.android.tools.build:gradle:4.2.0' and 7.2

2.  Improvement to support send html file to client browser

3.  Improvement Web system performance.                      2024/08/21

4.  Finished BT main test functions.                         2024/08/22
    Update to send service object handle to servlet -- done
    Implement test by service -- done
    implement test by servlet directly -- done(current choose solution)

5.  Fix build error when import DeviceWiFiManager.java       2024/08/23
    update content in file: android-http-server-master\build.gradle
    change "options.deprecation = true" to "options.deprecation = false"

6.  Server点击run on background的时候，server会自动停止（2分15秒后） 2024/08/26
    2024-08-26 10:13:17.921 11736-13573 ActivityManagerWrapper  com.miui.home                        E  getRecentTasks: taskId=21   userId=0   baseIntent=Intent { act=android.intent.action.MAIN cat=[android.intent.category.LAUNCHER] flg=0x10000000 cmp=ro.polak.webserver/.MainActivity }
    2024-08-26 10:15:32.246 15862-15862 ro.polak.http.WebServer ro.polak.webserver                   I  Server has been stopped.(call controller.stop from BaseMainService::onDestroy)

    solution studying:
    1. android:persistent="true"  --> no work
    2. return START_STICKY in function onStartCommand   --> no work
    3. Use startForegroundService solution --> Workable (在后台及锁屏的状态下都可以工作，但是不能进行BT扫描及连接动作 -- 权限问题，需要完全允许位置权限)
        https://blog.csdn.net/weixin_46362658/article/details/127414362
        https://blog.csdn.net/qq_39373532/article/details/129128430
        Android 8.0 有一项复杂功能；系统不允许后台应用创建后台服务。 因此，Android 8.0 引入了一种全新的方法，
        即 Context.startForegroundService()，以在前台启动新服务。
        A. BaseMainActivity.startBackgroundService --> startForegroundService
        B. BaseMainService.onStartCommand --> run startForeground

7.  点击通知栏返回的主UI，UI显示异常，但是功能正常                2024/08/27
    https://blog.csdn.net/u012149399/article/details/49228069

8.  任务栏看不到activity                                    2024/08/27
    https://blog.csdn.net/weixin_36221923/article/details/141508772
    solution：    android:excludeFromRecents="false"

9.  按返回键后程序返回UI异常                                  2024/08/28
    solution: handle KeyEvent.KEYCODE_BACK event.

10. App退出时有异常信息：                                    2024/08/28
    2024-08-28 19:31:48.352  1273-3563  NotificationService     system_server
    E  No Channel found for pkg=ro.polak.webserver, channelId=null, id=0, tag=null,
    opPkg=ro.polak.webserver, callingUid=10236, userId=0, incomingUserId=0, notificationUid=10236,
    notification=Notification(channel=null shortcut=null contentView=null vibrate=null sound=null
    defaults=0x0 flags=0x2 color=0x00000000 actions=1 vis=PRIVATE)
    solution: change Notification cancel code in MainService destroy procedure

11. 所有的servlet都用同一个thread处理？并行运行效果差 （https://www.jianshu.com/p/95b186fbf192）       2024/08/28
    BTTestServlet thread ID = 4298
    WIFITestServlet thread ID = 4298

    Solution: code change as below:
    change "threadPoolExecutor = new ThreadPoolExecutor(1," to "threadPoolExecutor = new ThreadPoolExecutor(serverConfig.getMaxServerThreads() / 2,"
    in ServiceContainer.java

12. Implement base Wifi test functions.                     2024/08/29

13. Update Bluetooth API function                           2024/09/03