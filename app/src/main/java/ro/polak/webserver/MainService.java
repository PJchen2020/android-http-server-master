/**************************************************
 * Android Web Server
 * Based on JavaLittleWebServer (2008)
 * <p/>
 * Copyright (c) Piotr Polak 2008-2017
 **************************************************/

package ro.polak.webserver;

import android.content.Context;
import android.content.Intent;

import androidx.annotation.NonNull;

import java.util.logging.Level;
import java.util.logging.Logger;

import impl.AndroidServerConfigFactory;
import ro.polak.webserver.base.BaseMainService;
import ro.polak.webserver.base.impl.BaseAndroidServerConfigFactory;

/**
 * Main application service that holds http server.
 *
 * @author Piotr Polak piotr [at] polak [dot] ro
 * @since 201709
 */
public final class MainService extends BaseMainService {

    private static final Logger LOGGER = Logger.getLogger(MainService.class.getName());

    @NonNull
    @Override
    protected Class<MainActivity> getActivityClass() {
        return MainActivity.class;
    }

    @NonNull
    @Override
    protected BaseAndroidServerConfigFactory getServerConfigFactory(final Context context) {
        return new AndroidServerConfigFactory(context);
    }

    @Override
    public void onDestroy() {
        LOGGER.log(Level.INFO, "MainService is destroy");
        super.onDestroy();
    }
}
