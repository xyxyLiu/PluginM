package com.reginald.pluginm.stub;

import android.os.Bundle;
import android.support.annotation.Nullable;

/**
 * 所有的非Activity的Stub组件，需要与Manifest中的Stub组建同步!
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

        public static class P2 {
            public static class Service0 extends PluginStubMainService {
            }
        }

        public static class P3 {
            public static class Service0 extends PluginStubMainService {
            }
        }

        public static class P4 {
            public static class Service0 extends PluginStubMainService {
            }
        }

        public static class P5 {
            public static class Service0 extends PluginStubMainService {
            }
        }

        public static class P6 {
            public static class Service0 extends PluginStubMainService {
            }
        }

        public static class P7 {
            public static class Service0 extends PluginStubMainService {
            }
        }

        public static class P8 {
            public static class Service0 extends PluginStubMainService {
            }
        }

        public static class P9 {
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

        public static class P2 {
            public static class Provider0 extends PluginStubMainProvider {
            }
        }

        public static class P3 {
            public static class Provider0 extends PluginStubMainProvider {
            }
        }

        public static class P4 {
            public static class Provider0 extends PluginStubMainProvider {
            }
        }

        public static class P5 {
            public static class Provider0 extends PluginStubMainProvider {
            }
        }

        public static class P6 {
            public static class Provider0 extends PluginStubMainProvider {
            }
        }

        public static class P7 {
            public static class Provider0 extends PluginStubMainProvider {
            }
        }

        public static class P8 {
            public static class Provider0 extends PluginStubMainProvider {
            }
        }

        public static class P9 {
            public static class Provider0 extends PluginStubMainProvider {
            }
        }
    }

}
