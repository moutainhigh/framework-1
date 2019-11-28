/*
 * The MIT License (MIT)
 * Copyright © 2019 <sky>
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the “Software”), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.sky.framework.rpc.cluster;

import com.sky.framework.rpc.common.exception.RpcException;
import com.sky.framework.rpc.invoker.consumer.Dispatcher;
import com.sky.framework.rpc.invoker.future.DefaultInvokeFuture;
import com.sky.framework.rpc.register.meta.RegisterMeta;
import com.sky.framework.rpc.remoting.Request;
import lombok.extern.slf4j.Slf4j;

/**
 * @author
 */
@Slf4j
public class FailoverClusterInvoker implements ClusterInvoker {

    private Dispatcher dispatcher;

    public FailoverClusterInvoker(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    @Override
    public <T> T invoke(Request request, RegisterMeta.ServiceMeta serviceMeta, Class<?> returnType) {
        Object result = null;
        int retry = 3;
        while (retry > 0) {
            retry--;
            DefaultInvokeFuture future = dispatcher.dispatch(request, serviceMeta, returnType);
            try {
                result = future.getResult();
                if (future.isCompletedExceptionally()) {
                    throw future.getCause();
                }
                break;
            } catch (Throwable throwable) {
                if (retry == 0) {
                    log.error("failoverClusterInvoker invoke exception:{}", throwable);
                    RpcException rpcException = throwable instanceof RpcException ? (RpcException) throwable :
                            new RpcException(throwable);
                    throw rpcException;
                }
            }
        }
        return (T) result;
    }
}
