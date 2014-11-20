package com.bodybuilding.commerce.observers;

import com.bodybuilding.commerce.assets.TypeC;
import rx.Observer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeCWriterObserver implements Observer<TypeC> {

    private final List<TypeC> writes = new ArrayList<TypeC>();


    @Override
    public void onCompleted() {
        System.out.println("C's (" + writes.size() + "): " + Arrays.toString(writes.toArray()));
    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(TypeC typeC) {
        writes.add(typeC);
    }

    public Object[] getContents(){
        return writes.toArray();
    }
}
