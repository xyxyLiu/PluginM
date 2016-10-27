package pluginm.reginald.com.pluginlib;

import android.content.Context;
import android.content.Intent;

/**
 * Created by lxy on 16-10-27.
 */
public interface IPluginManager {
    Context createPluginContext(String packageName, Context baseContext);
    Intent getPluginActivityIntent(Intent pluginIntent);
}
