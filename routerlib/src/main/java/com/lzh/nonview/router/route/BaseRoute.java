/*
 * Copyright (C) 2017 Haoge
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.lzh.nonview.router.route;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;

import com.lzh.nonview.router.RouteManager;
import com.lzh.nonview.router.Router;
import com.lzh.nonview.router.Utils;
import com.lzh.nonview.router.exception.NotFoundException;
import com.lzh.nonview.router.extras.RouteBundleExtras;
import com.lzh.nonview.router.interceptors.RouteInterceptor;
import com.lzh.nonview.router.interceptors.RouteInterceptorAction;
import com.lzh.nonview.router.module.RouteRule;
import com.lzh.nonview.router.parser.URIParser;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseRoute<T extends IBaseRoute, E extends RouteBundleExtras> implements IRoute, IBaseRoute<T>, RouteInterceptorAction<T> {
    URIParser parser;
    Bundle bundle;
    E extras;
    private RouteCallback.InternalCallback callback;
    Uri uri;
    RouteRule routeRule = null;

    public final IRoute create(Uri uri, RouteCallback.InternalCallback callback) {
        try {
            this.uri = uri;
            this.callback = callback;
            this.extras = createExtras();
            this.extras.setCallback(callback.getCallback());
            this.parser = new URIParser(uri);
            this.routeRule = obtainRouteMap();
            this.bundle = Utils.parseRouteMapToBundle(parser, routeRule);
            this.bundle.putParcelable(Router.RAW_URI, uri);
            return this;
        } catch (Throwable e) {
            callback.onOpenFailed(uri,e);
            return IRoute.EMPTY;
        }
    }

    // =========Unify method of IBaseRoute
    @Override
    public final void open(Context context) {
        try {
            Utils.checkInterceptor(uri, extras,context,getInterceptors());
            realOpen(context);
            callback.onOpenSuccess(uri, routeRule);
        } catch (Throwable e) {
            if (e instanceof NotFoundException) {
                callback.notFound(uri, (NotFoundException) e);
            } else {
                callback.onOpenFailed(this.uri,e);
            }
        }
    }

    @Override
    public T addExtras(Bundle extras) {
        this.extras.addExtras(extras);
        //noinspection unchecked
        return (T) this;
    }

    // =============RouteInterceptor operation===============
    public T addInterceptor(RouteInterceptor interceptor) {
        if (extras != null) {
            extras.addInterceptor(interceptor);
        }
        //noinspection unchecked
        return (T) this;
    }

    @Override
    public T removeInterceptor(RouteInterceptor interceptor) {
        if (extras != null) {
            extras.removeInterceptor(interceptor);
        }
        //noinspection unchecked
        return (T) this;
    }

    @Override
    public T removeAllInterceptors() {
        if (extras != null) {
            extras.removeAllInterceptors();
        }
        //noinspection unchecked
        return (T) this;
    }

    @Override
    public List<RouteInterceptor> getInterceptors() {
        List<RouteInterceptor> interceptors = new ArrayList<>();
        // add global interceptor
        if (RouteManager.get().getGlobalInterceptor() != null) {
            interceptors.add(RouteManager.get().getGlobalInterceptor());
        }

        // add extra interceptors
        if (extras != null) {
            interceptors.addAll(extras.getInterceptors());
        }

        return interceptors;
    }

    // ========getter/setter============
    public void replaceExtras(E extras) {
        if (extras != null) {
            this.extras = extras;
            this.callback.setCallback(extras.getCallback());
        }
    }

    // ============abstract methods============
    protected abstract E createExtras();

    protected abstract RouteRule obtainRouteMap();

    protected abstract void realOpen(Context context) throws Throwable;

}
