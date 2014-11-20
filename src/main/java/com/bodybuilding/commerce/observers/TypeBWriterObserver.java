package com.bodybuilding.commerce.observers;

import com.bodybuilding.commerce.assets.TypeA;
import com.bodybuilding.commerce.assets.TypeB;
import rx.Observer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class TypeBWriterObserver implements Observer<TypeB> {

    private final List<TypeB> writes = new ArrayList<TypeB>();


    @Override
    public void onCompleted() {
        System.out.println("B's (" + writes.size() + "): " + Arrays.toString(writes.toArray()));
    }

    @Override
    public void onError(Throwable e) {

    }

    @Override
    public void onNext(TypeB typeB) {
        writes.add(typeB);
    }

    public Object[] getContents(){
        return writes.toArray();
    }
}
