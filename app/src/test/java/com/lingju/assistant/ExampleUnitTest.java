package com.lingju.assistant;

import android.app.Application;
import android.content.Context;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.*;
import static org.mockito.Mockito.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Pair;

import java.util.Random;
import java.util.concurrent.Callable;

import io.reactivex.BackpressureStrategy;
import io.reactivex.Flowable;
import io.reactivex.FlowableEmitter;
import io.reactivex.FlowableOnSubscribe;
import io.reactivex.Notification;
import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.ObservableSource;
import io.reactivex.Scheduler;
import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.BiFunction;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Function;
import io.reactivex.functions.Predicate;
import io.reactivex.schedulers.Schedulers;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
@RunWith(MockitoJUnitRunner.class)
public class ExampleUnitTest {

    @Mock
    private Application mMockContext;

    public static class Vo{
        int retry=3;
        Vo(int i){this.retry=i;}
    }

    public static class RetryThrowable extends Exception{
        public RetryThrowable() {
        }

        public RetryThrowable(String detailMessage) {
            super(detailMessage);
        }

        public RetryThrowable(String detailMessage, Throwable cause) {
            super(detailMessage, cause);
        }

        public RetryThrowable(Throwable cause) {
            super(cause);
        }

    }

    private Observable<Vo> start4(final Vo v){
        return Observable.create(new ObservableOnSubscribe<Vo>() {
                    @Override
                    public void subscribe(ObservableEmitter<Vo> e) throws Exception {
                        System.out.println("111111="+(e==null));
                        e.onNext(v);
                    }
                })
                .map(new Function<Vo, Vo>() {
                    @Override
                    public Vo apply(Vo vo) throws Exception {
                        vo.retry++;
                        return vo;
                    }
                })
                .flatMap(new Function<Vo, ObservableSource<Vo>>() {
                    @Override
                    public ObservableSource<Vo> apply(final Vo vo) throws Exception {
                        return Observable.create(new ObservableOnSubscribe<Vo>() {
                            @Override
                            public void subscribe(final ObservableEmitter<Vo> e) throws Exception {
                                System.out.println("e==="+(e==null));
                                new Thread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            Thread.sleep(3000);
                                        } catch (InterruptedException e1) {
                                            e1.printStackTrace();
                                        }
                                        System.out.println("e2222"+(e==null));
                                        e.onNext(vo);
                                        try {
                                            Thread.sleep(3000);
                                        } catch (InterruptedException e1) {
                                            e1.printStackTrace();
                                        }
                                        System.out.println("e3333"+(e==null));
                                        e.onNext(vo);
                                        try {
                                            Thread.sleep(3000);
                                        } catch (InterruptedException e1) {
                                            e1.printStackTrace();
                                        }
                                        System.out.println("e4444"+(e==null));
                                        vo.retry=110;
                                        e.onNext(vo);
                                        try {
                                            Thread.sleep(3000);
                                        } catch (InterruptedException e1) {
                                            e1.printStackTrace();
                                        }
                                        System.out.println("completed111111111111111111111");
                                        e.onComplete();
                                    }
                                }).start();
                            }
                        }).filter(new Predicate<Vo>() {
                            @Override
                            public boolean test(Vo vo) throws Exception {
                                System.out.println("test>>>"+vo.retry);
                                return vo.retry==110;
                            }
                        });
                    }
                });
    }


    @Test
    public void test6(){
        int i=10;
        while(--i>0) {
            Flowable.create(new FlowableOnSubscribe<Integer>() {
                @Override
                public void subscribe(FlowableEmitter<Integer> e) throws Exception {
                    System.out.println("subscribe.................");
                    e.onNext(111);
                }
            }, BackpressureStrategy.BUFFER)
                    .subscribe(new Consumer<Integer>() {
                        @Override
                        public void accept(Integer integer) throws Exception {
                            System.out.println("onNext>>>>>" + integer);
                        }
                    });

            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void test5()  {
        start4(new Vo(10)).doOnNext(new Consumer<Vo>() {
            @Override
            public void accept(Vo vo) throws Exception {
                System.out.println("vo+"+vo.retry);
            }
        })
        .zipWith(Observable.just(new Vo(20)).doOnNext(new Consumer<Vo>() {
            @Override
            public void accept(Vo vo) throws Exception {
                System.out.println("22222222222>>"+vo.retry);
            }
        }), new BiFunction<Vo, Vo, Pair<Vo,Vo>>() {
            @Override
            public Pair<Vo,Vo> apply(Vo vo, Vo vo2) throws Exception {
                return new Pair<Vo, Vo>(vo,vo2);
            }
        }).doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                System.out.println("completed.................");
            }
        }).subscribe();
        try {
            Thread.sleep(20000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Observable<Vo> start3(final Vo v){
        return Observable.create(new ObservableOnSubscribe<Vo>() {
                    @Override
                    public void subscribe(final ObservableEmitter<Vo> e) throws Exception {
                        System.out.println("start>>>>>>"+Thread.currentThread());
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                System.out.println("subscribe spleep 3s ");
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                                System.out.println("subscribe end");
                                e.onNext(v);
                                System.out.println("subscribe spleep 3s bbbbb");
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                                System.out.println("subscribe end bbbbb");
                                e.onNext(v);
                                System.out.println("subscribe spleep 2s ccccccc");
                                try {
                                    Thread.sleep(2000);
                                } catch (InterruptedException e1) {
                                    e1.printStackTrace();
                                }
                                System.out.println("subscribe end cccccccccc");
                                e.onNext(v);
                                e.onComplete();
                            }
                        }).start();
/*                        System.out.println("to next.....");
                        e.onNext(v);
                        e.onComplete();*/
                    }
                })
                .doOnNext(new Consumer<Vo>() {
                    @Override
                    public void accept(Vo vo) throws Exception {
                        System.out.println("B>>doOnNext111 sleep 5s>>>"+vo.retry+"--"+Thread.currentThread());
                        Thread.sleep(5000);
                        System.out.println("E>>doOnNext111>>>end>>");
                    }
                })
                .map(new Function<Vo, Vo>() {
                    @Override
                    public Vo apply(Vo vo) throws Exception {
                        System.out.println("map>>"+vo.retry+"--"+Thread.currentThread());
                        return vo;
                    }
                })
                .doOnNext(new Consumer<Vo>() {
                    @Override
                    public void accept(Vo vo) throws Exception {
                        System.out.println("doOnNext222222>>"+vo.retry+"--"+Thread.currentThread());
                        Thread.sleep(5000);
                    }
                });


    }

    @Test
    public void test4(){
        start3(new Vo(10))
                .subscribeOn(Schedulers.single())
                .zipWith(Observable.create(new ObservableOnSubscribe<Vo>() {
                            @Override
                            public void subscribe(ObservableEmitter<Vo> e) throws Exception {
                                System.out.println("2>>to next....."+Thread.currentThread());
                                e.onNext(new Vo(11));
                                e.onComplete();
                            }
                        })
                        .subscribeOn(Schedulers.newThread())
                        .doOnNext(new Consumer<Vo>() {
                            @Override
                            public void accept(Vo vo) throws Exception {
                                System.out.println("2>>sleep 6s "+Thread.currentThread());
                                Thread.sleep(6000);
                                System.out.println("2>>sleep 6s end");
                            }
                        })
                        ,
                        new BiFunction<Vo, Vo, Vo>() {
                            @Override
                            public Vo apply(Vo vo, Vo vo2) throws Exception {
                                System.out.println("combine apply......"+Thread.currentThread());
                                return new Vo(vo.retry+vo2.retry);
                            }
                        }
                )
                .observeOn(Schedulers.io())
                .doOnNext(new Consumer<Vo>() {
                    @Override
                    public void accept(Vo vo) throws Exception {
                        System.out.println("last>>"+vo.retry+"-"+Thread.currentThread());
                    }
                })
                .doOnComplete(new Action() {
                    @Override
                    public void run() throws Exception {
                        System.out.println("last>>complated>>"+Thread.currentThread());
                    }
                })
                //.observeOn(Schedulers.single())
                .subscribe();
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    public void test3(){
        start3(new Vo(10)).doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                throwable.printStackTrace();
            }
        }).doOnComplete(new Action() {
            @Override
            public void run() throws Exception {
                System.out.println("doOnCompleted......."+Thread.currentThread());
            }
        }).doOnTerminate(new Action() {
            @Override
            public void run() throws Exception {
                System.out.println("doOnTerminate......."+Thread.currentThread());
            }
        }).subscribe();
        try {
            Thread.sleep(30000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }


    private Single<String> start2(final String msg){
        return Single.just(msg).doOnDispose(new Action() {
            @Override
            public void run() throws Exception {
                System.out.println("onDispose>>"+Thread.currentThread());
            }
        }).doOnSubscribe(new Consumer<Disposable>() {
            @Override
            public void accept(Disposable disposable) throws Exception {
                System.out.println("doOnSubscribe>>"+Thread.currentThread());

            }
        })/*.subscribeOn(Schedulers.computation())*/.doOnEvent(new BiConsumer<String, Throwable>() {
            @Override
            public void accept(String s, Throwable throwable) throws Exception {
                System.out.println("doOnEvent>>"+s+"--"+Thread.currentThread());

            }
        }).doOnError(new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) throws Exception {
                System.out.println("doOnError>>"+Thread.currentThread());
            }
        }).doOnSuccess(new Consumer<String>() {
            @Override
            public void accept(String s) throws Exception {
                System.out.println("doOnSuccess>>"+s+"--"+Thread.currentThread());

            }
        }).subscribeOn(Schedulers.io());
    }

    public void test2(){
        start2("111").subscribe();
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    private Observable<Notification<String>> start(final String msg){

        return Observable.create(new ObservableOnSubscribe<Notification<String>>() {
            @Override
            public void subscribe(final ObservableEmitter<Notification<String>> e) throws Exception {
                start_(msg, new CallBack() {
                    @Override
                    public void onCompleted(int code) {
                        e.onNext(Notification.createOnNext(code+""));
                        e.onComplete();
                    }
                });
            }
        }).subscribeOn(Schedulers.computation());
    }

    private void start_(String msg,final CallBack callback){
        System.out.println("start_............"+Thread.currentThread());
        new Thread(new Runnable() {
            @Override
            public void run() {
                System.out.println("start_............begin");
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println("start_............end");
                callback.onCompleted(0);
            }
        }).start();
    }

    public void addition_isCorrect() throws Exception {
        System.out.println("main............"+Thread.currentThread());
       start("test")
               .observeOn(Schedulers.io())
               .doOnNext(new Consumer<Notification<String>>() {
                   @Override
                   public void accept(Notification<String> s) throws Exception {
                       System.out.println("doOnNext..................."+s.isOnNext()+"--"+s.isOnComplete());
                   }
               })
               .doOnComplete(new Action() {
                   @Override
                   public void run() throws Exception {
                       System.out.println("completed111.............................."+Thread.currentThread());
                   }
               })
               .map(new Function<Notification<String>, Notification<Float>>() {

                   @Override
                   public Notification<Float> apply(Notification<String> s) throws Exception {
                       System.out.println("ssss==="+s.isOnNext()+"--"+s.isOnComplete());
                       return Notification.createOnNext(new Float(3.000));
                   }
               })
               .doOnNext(new Consumer<Notification<Float>>() {
                   @Override
                   public void accept(Notification<Float> aFloat) throws Exception {
                       System.out.println("doOnNext:"+aFloat.toString()+aFloat.isOnNext()+"--"+aFloat.isOnComplete());
                   }
               })
               .doOnComplete(new Action() {
                               @Override
                               public void run() throws Exception {
                                    System.out.println("completed222.............................."+Thread.currentThread());
                               }
                           })
               .subscribe();

        System.out.println("start.......end");
        Thread.sleep(10000);
        System.out.println("end.............");
    }

    public static  interface  CallBack{
        public void onCompleted(int code);
    }

    public static class MyCallBack implements CallBack,Action{

        @Override
        public void run() throws Exception {
            System.out.println("MyCallBack.run....................");
        }

        @Override
        public void onCompleted(int code) {
            System.out.println("MyCallBack.onCompleted....................");
        }
    }
}