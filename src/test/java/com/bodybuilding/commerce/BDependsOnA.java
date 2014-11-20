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
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import java.io.IOException;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.hamcrest.Matchers.*;

public class BDependsOnA {



    @Test
    public void testBDependsOnANoDupesInFile() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Checks that there are no duplicates and will fail because there are (b:55 is in file, a:55 triggers b:55 so there's dupes there

        String filename = "AB_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename)
            .distinct()
            ;

        Observable<TypeA> typeAChanged = AssetObservable.filterType(TypeA.class, allChangedAssets, "TypeA");

        Observable<TypeB> bsTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeB>() {
            @Override
            public TypeB call(TypeA typeA) {
                return new TypeB(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeB> typeBChanged = AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            .mergeWith(bsTriggeredByA);


        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged.subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged.subscribe(bObserver);

        Application.simulateChanges(source, filename);

        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();

        //Make sure EVERY A has corresponding B
        for(Object a : aContents){
            Integer aId = ((TypeA)a).getId();

            //Corresponding B for A is same Id value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(aId))));

        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat("We expect this to fail", bContents, not(Matchers.hasDuplicateInArray()));



    }



    @Test
    public void testBDependsOnANoDupesInFileDistinctPerType() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Checks that there are no duplicates, passes because we distinct before the subscription

        String filename = "AB_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeA> typeAChanged = AssetObservable.filterType(TypeA.class, allChangedAssets, "TypeA");

        Observable<TypeB> bsTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeB>() {
            @Override
            public TypeB call(TypeA typeA) {
                return new TypeB(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeB> typeBChanged = AssetObservable.filterType(TypeB.class, allChangedAssets, "TypeB")
            .mergeWith(bsTriggeredByA)
        ;


        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged

            .subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged
            .distinct() //<--the magic sauce going on here
            .subscribe(bObserver);

        Application.simulateChanges(source, filename);

        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();

        //Make sure EVERY A has corresponding B
        for(Object a : aContents){
            Integer aId = ((TypeA)a).getId();

            //Corresponding B for A is same Id value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(aId))));

        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));

    }

    @Test
    public void testBDependsOnADupesInFileDistinctPerType() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Now there are dupes in the file
        //Checks that there are no duplicates, passes because we distinct before the subscription

        String filename = "AB_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename)
            //We distinct here to eliminate dupes from the file
            //if the source is known to be dupe free (like perhaps bcc) this wouldn't be very useful
            .distinct()
        ;


        Observable<TypeA> typeAChanged = AssetObservable.filterType(TypeA.class, allChangedAssets, "TypeA");

        Observable<TypeB> bsTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeB>() {
            @Override
            public TypeB call(TypeA typeA) {
                return new TypeB(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeB> typeBChanged = AssetObservable.filterType(TypeB.class, allChangedAssets, "TypeB")
            .mergeWith(bsTriggeredByA)
            ;


        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged
            .distinct() //<--this is here..but because we distinct the source file..and A only comes from the source..this does nothing
            .subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged
            .distinct() //<--the magic sauce going on here
            .subscribe(bObserver);

        Application.simulateChanges(source, filename);

        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();

        //Make sure EVERY A has corresponding B
        for(Object a : aContents){
            Integer aId = ((TypeA)a).getId();

            //Corresponding B for A is same Id value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(aId))));

        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));


    }



}
