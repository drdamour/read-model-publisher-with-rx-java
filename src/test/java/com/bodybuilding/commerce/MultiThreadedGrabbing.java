package com.bodybuilding.commerce;

import com.bodybuilding.commerce.assets.TypeA;
import com.bodybuilding.commerce.assets.TypeB;
import com.bodybuilding.commerce.assets.TypeC;
import com.bodybuilding.commerce.observers.TypeAWriterObserver;
import com.bodybuilding.commerce.observers.TypeBWriterObserver;
import com.bodybuilding.commerce.observers.TypeCWriterObserver;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.GroupedObservable;
import rx.schedulers.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class MultiThreadedGrabbing {

    // In some cases it may make sense to do work on multiple threads
    // RxJava has this baked in

    static class MultiThreadedGrabbingReadModelSystem extends ReadModelSystem {
        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        TypeCWriterObserver cObserver = new TypeCWriterObserver();


        public MultiThreadedGrabbingReadModelSystem(String s) {
            super(s);

            this.getTypeAChanges().subscribe(aObserver);
            this.getTypeBChanges().subscribe(bObserver);
            this.getTypeCChanges().subscribe(cObserver);
        }

        public TypeAWriterObserver getAObserver() {
            return aObserver;
        }

        public TypeBWriterObserver getBObserver() {
            return bObserver;
        }

        public TypeCWriterObserver getCObserver() {
            return cObserver;
        }



        public void wireUpBDependsOnASingleThread(){
            //B's on A
            typeAChangesVerified.map(new Func1<TypeA, TypeB>() {
                @Override
                public TypeB call(TypeA typeA) {
                    return new TypeB(typeA.getId(), typeA.makeSourceString());
                }
            }).subscribe(typeBChangeRequests);

            //C's on B
            typeBChangesVerified.map(new Func1<TypeB, TypeC>() {
                @Override
                public TypeC call(TypeB typeB) {
                    return new TypeC(typeB.getId(), typeB.makeSourceString());
                }
            }).subscribe(typeCChangeRequests);
        }


        public void wireUpBDependsOnAMultiThread(){
            typeAChangesVerified
                //Magic! use computation if the Func1 to map is computation, use io of it's io bound
                .observeOn(Schedulers.computation())
                .map(new Func1<TypeA, TypeB>() {
                    @Override
                    public TypeB call(TypeA typeA) {
                        return new TypeB(typeA.getId(), typeA.makeSourceString());
                    }
                })
                .subscribe(typeBChangeRequests);


            //C's on B
            typeBChangesVerified
                //Magic! use computation if the Func1 to map is computation, use io of it's io bound
                .observeOn(Schedulers.computation())
                .map(new Func1<TypeB, TypeC>() {
                @Override
                public TypeC call(TypeB typeB) {
                    return new TypeC(typeB.getId(), typeB.makeSourceString());
                }
            }).subscribe(typeCChangeRequests);
        }


    }

    @Test
    public void testMultiThreadedGrabbingNoDupesInFileSingleThread() throws IOException {



        String filename = "ABC_NO_DUPES.txt";

        MultiThreadedGrabbingReadModelSystem system = new MultiThreadedGrabbingReadModelSystem(filename);

        system.wireUpBDependsOnASingleThread();


        system.simulateChangeProcessing();

        // verify things went right
        Object[] aContents = system.getAObserver().getContents();
        Object[] bContents = system.getBObserver().getContents();
        Object[] cContents = system.getCObserver().getContents();


        //Make sure EVERY A has corresponding B
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
        }


        //Make sure EVERY B has corresponding C
        for(Object b : bContents){
            Integer id = ((TypeB)b).getId();
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
        }



        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

        //Check threads..should all be 1..which is the same as saying there are no items in the array that are NOT created on 1
        assertThat(aContents, not(hasItemInArray((hasProperty("threadCreatedOnId", not(equalTo(1l)))))));
        assertThat(bContents, not(hasItemInArray((hasProperty("threadCreatedOnId", not(equalTo(1l)))))));
        assertThat(cContents, not(hasItemInArray((hasProperty("threadCreatedOnId", not(equalTo(1l)))))));
    }


    @Test
    public void testMultiThreadedGrabbingNoDupesInFileMultiThread() throws IOException {



        String filename = "ABC_NO_DUPES.txt";

        MultiThreadedGrabbingReadModelSystem system = new MultiThreadedGrabbingReadModelSystem(filename);

        system.wireUpBDependsOnAMultiThread();

        System.out.println("Thread before processing: " + Thread.currentThread().getId());
        system.simulateChangeProcessing();
        System.out.println("Thread after processing: " + Thread.currentThread().getId());

        // verify things went right
        Object[] aContents = system.getAObserver().getContents();
        Object[] bContents = system.getBObserver().getContents();
        Object[] cContents = system.getCObserver().getContents();


        //Make sure EVERY A has corresponding B
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
        }

        //Make sure EVERY B has corresponding C
        for(Object b : bContents){
            Integer id = ((TypeB)b).getId();
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

        //Check threads..should all be 1..whic is the same as saying there are no items in the array that are NOT created on 1
        assertThat(aContents, not(hasItemInArray((hasProperty("threadCreatedOnId", not(equalTo(1l)))))));
        assertThat(bContents, not(hasItemInArray((hasProperty("threadCreatedOnId", not(equalTo(1l)))))));
        assertThat(cContents, not(hasItemInArray((hasProperty("threadCreatedOnId", not(equalTo(1l)))))));

    }


}

