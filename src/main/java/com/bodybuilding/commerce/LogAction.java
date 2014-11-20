package com.bodybuilding.commerce;

import rx.functions.Action1;

public class LogAction<T> implements Action1<T> {

    private final String prefix;

    public LogAction(String prefix){
        this.prefix = prefix;
    }

    @Override
    public void call(T t) {
        System.out.println(prefix + t);
    }
}
