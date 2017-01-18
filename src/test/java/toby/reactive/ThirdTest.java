package toby.reactive;

import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
public class ThirdTest {
	@Test
	public void scheduler_example() {
		Publisher<Integer> pub = sub -> {
			sub.onSubscribe(new Subscription() {
				@Override
				public void request(long n) {
					log.debug("request: {}", n);
					sub.onNext(1);
					sub.onNext(2);
					sub.onNext(3);
					sub.onNext(4);
					sub.onNext(5);
					sub.onComplete();
				}

				@Override
				public void cancel() {
					log.debug("cancel");
				}
			});
		};
		// end of pub

		ExecutorService executorService = Executors.newSingleThreadExecutor();
		// slow publisher, fast subscriber
		Publisher subcribeOn_publisher = sub -> {
//			// 하나 이상의 쓰레드가 요청되면 queue 넣고 대기 시킴
			executorService.execute(() -> pub.subscribe(sub));
		};

		// fast publisher, slow subscriber
		// 느린 subscriber(consumer)를 별도의 쓰레드에서 동작시킴
		Publisher<Integer> publishOn_publisher = sub -> {
			pub.subscribe(new Subscriber<Integer>() {
				@Override
				public void onSubscribe(Subscription s) {
					sub.onSubscribe(s);
				}

				@Override
				public void onNext(Integer integer) {
					executorService.execute(() -> sub.onNext(integer));
				}

				@Override
				public void onError(Throwable t) {
					executorService.execute(() -> sub.onError(t));
				}

				@Override
				public void onComplete() {
					executorService.execute(() -> sub.onComplete());
				}
			});
		};

		// start of sub
		publishOn_publisher.subscribe(new Subscriber<Integer>() {
			@Override
			public void onSubscribe(Subscription s) {
				log.debug("onSubscribe");
				s.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(Integer integer) {
				log.debug("onNext: {}", integer);
			}

			@Override
			public void onError(Throwable t) {
				log.debug("onError: {}", t);
			}

			@Override
			public void onComplete() {
				log.debug("onComplete");
			}
		});

		log.debug("exit");
	}
}
