package dev.nick.watermarking;

import android.app.Application;
import android.util.Log;

import dev.nick.android.imageloader.ImageLoader;
import dev.nick.android.injection.Injector;
import dev.nick.logger.LoggerManager;

public class WMApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        Injector.init(this);
        ImageLoader.createShared(this);
        LoggerManager.setDebugLevel(Log.VERBOSE);
    }
}
