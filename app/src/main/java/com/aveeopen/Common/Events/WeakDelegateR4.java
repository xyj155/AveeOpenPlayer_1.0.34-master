/*
 * Copyright 2019 Avee Player. All rights reserved.
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

package com.aveeopen.Common.Events;

import java.lang.ref.WeakReference;
import java.util.List;

public class WeakDelegateR4<T1, T2, T3, T4, TResult> {

    WeakReference<Handler<T1, T2, T3, T4, TResult>> listenerWeak = new WeakReference<>(null);

    public void clear() {
        listenerWeak = new WeakReference<>(null);
    }

    public WeakDelegateR4<T1, T2, T3, T4, TResult> subscribeWeak(Handler<T1, T2, T3, T4, TResult> listener, List<Object> listenerRefHolder) {
        listenerRefHolder.add(listener);
        listenerWeak = new WeakReference<>(listener);
        return this;
    }

    public Handler subscribeHoldWeak(Handler<T1, T2, T3, T4, TResult> listener) {
        listenerWeak = new WeakReference<>(listener);
        return listener;
    }

    public TResult invoke(T1 arg1, T2 arg2, T3 arg3, T4 arg4, TResult defaultValue) {
        Handler<T1, T2, T3, T4, TResult> lstnr = listenerWeak.get();
        if (lstnr != null)
            return lstnr.invoke(arg1, arg2, arg3, arg4);

        return defaultValue;
    }

    public interface Handler<T1, T2, T3, T4, TResult> {
        TResult invoke(T1 t1, T2 t2, T3 t3, T4 t4);
    }

}

