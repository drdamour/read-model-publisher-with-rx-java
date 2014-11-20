package com.bodybuilding.commerce;

import com.bodybuilding.commerce.assets.Asset;
import com.bodybuilding.commerce.assets.TypeA;
import com.bodybuilding.commerce.assets.TypeB;
import com.bodybuilding.commerce.assets.TypeC;
import com.bodybuilding.commerce.observables.AssetObservable;
import com.bodybuilding.commerce.observers.TypeAWriterObserver;
import com.bodybuilding.commerce.observers.TypeBWriterObserver;
import com.bodybuilding.commerce.observers.TypeCWriterObserver;
import org.junit.Test;
import rx.Observable;
import rx.subjects.PublishSubject;

import java.io.IOException;

import static org.hamcrest.Matchers.not;
import static org.junit.Assert.*;

public class StraightThroughDupes {



    @Test
    public void testStraightThroughDupes() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Doesn't have any inter dependencies between types
        //Checks that there are no duplicates and will fail because there are

        String filename = "AB_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeB> typeBChanged = AssetObservable.filterType(TypeB.class, allChangedAssets, "TypeB");
        Observable<TypeA> typeAChanged = AssetObservable.filterType(TypeA.class, allChangedAssets, "TypeA");


        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged.subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged.subscribe(bObserver);

        Application.simulateChanges(source, filename);


        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();


        //CHECKS FOR DUPES
        assertThat("We expect this to fail", aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));

    }



    @Test
    public void testStraightThroughDupesDistinctOnSourceObservable() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Doesn't have any inter dependencies between types
        //Switched up to have some generic helpers for As & Bs
        //Checks that there are no duplicates
        //Uses distinct() on the root source observable
        //relies on equals() implementation of Asset

        String filename = "AB_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename)
            .distinct() //<-- here's the magic
            ;

        Observable<TypeB> typeBChanged = AssetObservable.filterType(TypeB.class, allChangedAssets, "TypeB");
        Observable<TypeA> typeAChanged = AssetObservable.filterType(TypeA.class, allChangedAssets, "TypeA");


        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged.subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged.subscribe(bObserver);

        Application.simulateChanges(source, filename);


        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();


        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));


    }


    @Test
    public void testStraightThroughDupesThreeTypesDistinctOnSourceObservable() throws IOException {
        //DEALS with only A's, B's, & C's
        //Ignores everything else
        //Doesn't care about missed things
        //Doesn't have any inter dependencies between types
        //Checks that there are no duplicates
        //Uses distinct() on the root source observable
        //relies on equals() implementation of Asset

        String filename = "ABC_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename)
            .distinct() //<-- HERES THE MAGIC
            ;

        Observable<TypeB> typeBChanged = AssetObservable.filterType(TypeB.class, allChangedAssets, "TypeB");
        Observable<TypeA> typeAChanged = AssetObservable.filterType(TypeA.class, allChangedAssets, "TypeA");
        Observable<TypeC> typeCChanged = AssetObservable.filterType(TypeC.class, allChangedAssets, "TypeC");

        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged.subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged.subscribe(bObserver);

        TypeCWriterObserver cObserver = new TypeCWriterObserver();
        typeCChanged.subscribe(cObserver);

        Application.simulateChanges(source, filename);


        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();
        Object[] cContents = cObserver.getContents();


        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));
    }



}
