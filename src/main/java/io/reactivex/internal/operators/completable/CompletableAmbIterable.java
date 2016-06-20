package io.reactivex.internal.operators.completable;

import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;

import io.reactivex.*;
import io.reactivex.disposables.*;
import io.reactivex.plugins.RxJavaPlugins;

public final class CompletableAmbIterable extends Completable {

    final Iterable<? extends CompletableConsumable> sources;
    
    public CompletableAmbIterable(Iterable<? extends CompletableConsumable> sources) {
        this.sources = sources;
    }
    
    @Override
    protected void subscribeActual(final CompletableSubscriber s) {
        final CompositeDisposable set = new CompositeDisposable();
        s.onSubscribe(set);

        final AtomicBoolean once = new AtomicBoolean();
        
        CompletableSubscriber inner = new CompletableSubscriber() {
            @Override
            public void onComplete() {
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onComplete();
                }
            }

            @Override
            public void onError(Throwable e) {
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
            }

            @Override
            public void onSubscribe(Disposable d) {
                set.add(d);
            }
            
        };
        
        Iterator<? extends CompletableConsumable> it;
        
        try {
            it = sources.iterator();
        } catch (Throwable e) {
            s.onError(e);
            return;
        }
        
        if (it == null) {
            s.onError(new NullPointerException("The iterator returned is null"));
            return;
        }
        
        boolean empty = true;
        
        for (;;) {
            if (once.get() || set.isDisposed()) {
                return;
            }
            
            boolean b;
            
            try {
                b = it.hasNext();
            } catch (Throwable e) {
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
                return;
            }
            
            if (!b) {
                if (empty) {
                    s.onComplete();
                }
                break;
            }
            
            empty = false;
            
            if (once.get() || set.isDisposed()) {
                return;
            }

            CompletableConsumable c;
            
            try {
                c = it.next();
            } catch (Throwable e) {
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onError(e);
                } else {
                    RxJavaPlugins.onError(e);
                }
                return;
            }
            
            if (c == null) {
                NullPointerException npe = new NullPointerException("One of the sources is null");
                if (once.compareAndSet(false, true)) {
                    set.dispose();
                    s.onError(npe);
                } else {
                    RxJavaPlugins.onError(npe);
                }
                return;
            }
            
            if (once.get() || set.isDisposed()) {
                return;
            }
            
            // no need to have separate subscribers because inner is stateless
            c.subscribe(inner);
        }
    }

}
