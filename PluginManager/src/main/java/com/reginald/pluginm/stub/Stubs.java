package com.reginald.pluginm.stub;

import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * 所有的Stub组件，需要与Manifest中的Stub组建同步!
 * Created by lxy on 17-8-18.
 */

public class Stubs {

    public static class Activity {
        public static class Fake extends android.app.Activity {
            @Override
            protected void onCreate(@Nullable Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                finish();
            }
        }

        public static class P0 {
            public static class Standard0 extends android.app.Activity {
            }

            public static class SingleTop0 extends android.app.Activity {
            }

            public static class SingleTop1 extends android.app.Activity {
            }

            public static class SingleTask0 extends android.app.Activity {
            }

            public static class SingleTask1 extends android.app.Activity {
            }

            public static class SingleInstance0 extends android.app.Activity {
            }

            public static class SingleInstance1 extends android.app.Activity {
            }
        }

        public static class P1 {
            public static class Standard0 extends android.app.Activity {
            }

            public static class SingleTop0 extends android.app.Activity {
            }

            public static class SingleTop1 extends android.app.Activity {
            }

            public static class SingleTask0 extends android.app.Activity {
            }

            public static class SingleTask1 extends android.app.Activity {
            }

            public static class SingleInstance0 extends android.app.Activity {
            }

            public static class SingleInstance1 extends android.app.Activity {
            }
        }
    }

    public static class Service {
        public static class P0 {
            public static class Service0 extends PluginStubMainService {
            }
        }

        public static class P1 {
            public static class Service0 extends PluginStubMainService {
            }
        }
    }

    public static class Provider {
        public static class P0 {
            public static class Provider0 extends PluginStubMainProvider {
            }
        }

        public static class P1 {
            public static class Provider0 extends PluginStubMainProvider {
            }
        }
    }

}
