/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.messaging.tcp.reactor;

import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.springframework.util.concurrent.ListenableFutureAdapter;
import reactor.fn.Functions;
import reactor.io.net.ChannelStream;
import reactor.rx.Promise;
import reactor.rx.Promises;
import reactor.rx.Streams;
import reactor.rx.broadcast.Broadcaster;

import org.springframework.messaging.Message;
import org.springframework.messaging.tcp.TcpConnection;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * An implementation of {@link org.springframework.messaging.tcp.TcpConnection
 * TcpConnection} based on the TCP client support of the Reactor project.
 *
 * @param <P> the payload type of messages read or written to the TCP stream.
 *
 * @author Rossen Stoyanchev
 * @since 4.2
 */
public class Reactor2TcpConnection<P> implements TcpConnection<P> {

	private final ChannelStream<Message<P>, Message<P>> channelStream;
	private final Promise<Void> closePromise;

	public Reactor2TcpConnection(ChannelStream<Message<P>, Message<P>> channelStream, Promise<Void> closePromise) {
		this.channelStream = channelStream;
		this.closePromise = closePromise;
	}


	@Override
	public ListenableFuture<Void> send(Message<P> message) {
		Promise<Void> afterWrite = Promises.prepare();
		this.channelStream.writeWith(Streams.just(message)).subscribe(afterWrite);
		return new PassThroughPromiseToListenableFutureAdapter<Void>(afterWrite);
	}

	@Override
	public void onReadInactivity(Runnable runnable, long inactivityDuration) {
		this.channelStream.on().readIdle(inactivityDuration, Functions.<Void>consumer(runnable));
	}

	@Override
	public void onWriteInactivity(Runnable runnable, long inactivityDuration) {
		this.channelStream.on().writeIdle(inactivityDuration, Functions.<Void>consumer(runnable));
	}

	@Override
	public void close() {
		this.closePromise.onComplete();
	}
}