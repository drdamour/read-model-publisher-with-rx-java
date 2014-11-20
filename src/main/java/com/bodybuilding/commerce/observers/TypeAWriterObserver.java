package com.bodybuilding.commerce.observers;

import com.bodybuilding.commerce.assets.TypeA;
import rx.Observer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeAWriterObserver implements Observer<TypeA> {

    private final List<TypeA> writes = new ArrayList<TypeA>();


    @Override
    public void onCompleted() {
        System.out.println("A's (" + writes.size() + "): " + Arrays.toString(writes.toArray()));
    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(TypeA typeA) {
        writes.add(typeA);
    }

    public Object[] getContents(){
        return writes.toArray();
    }
}
