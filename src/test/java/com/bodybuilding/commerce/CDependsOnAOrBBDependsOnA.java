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

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class CDependsOnAOrBBDependsOnA {


    @Test
    public void testCDepsAOrBBDepsANoDupesInFile() throws IOException {
        //DEALS with only A's, B's, and C's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Every A record causes C records of same id to need updating
        //Every B record causes C records of same id to need updating
        //Checks that there are no duplicates and will fail because there are (b:55 is in file, a:55 triggers b:55 so there's dupes there

        String filename = "ABC_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeA> typeAChanged = AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA");


        Observable<TypeB> bsTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeB>() {
            @Override
            public TypeB call(TypeA typeA) {
                return new TypeB(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeB> typeBChanged = AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            .mergeWith(bsTriggeredByA)
            ;


        Observable<TypeC> csTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeC>() {
            @Override
            public TypeC call(TypeA typeA) {
                return new TypeC(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeC> csTriggeredByB = typeBChanged.map(new Func1<TypeB, TypeC>() {
            @Override
            public TypeC call(TypeB typeB) {
                return new TypeC(typeB.getId(), typeB.makeSourceString());
            }
        });


        Observable<TypeC> typeCChanged = AssetObservable
            .filterType(TypeC.class, allChangedAssets, "TypeC")
            .mergeWith(csTriggeredByA)
            .mergeWith(csTriggeredByB);


        //SUBSCRIBE


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


        //Make sure EVERY A has corresponding B & C
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();

            //Corresponding B for A is same Id value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
            //Corresponding C for A is same Id value
            assertThat(cContents, hasItemInArray(hasProperty("id", equalTo(id))));

        }

        //Make sure EVERY B has corresponding C
        for(Object b : bContents){
            Integer id = ((TypeB)b).getId();

            //Corresponding C for B is same Id value
            assertThat(cContents, hasItemInArray(hasProperty("id", equalTo(id))));

        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat("We expect this to fail", bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

    }



    @Test
    public void testCDepsAOrBBDepsANoDupesInFileDistinctBeforeSubscriptions() throws IOException {
        //DEALS with only A's, B's, and C's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Every A record causes C records of same id to need updating
        //Every B record causes C records of same id to need updating
        //Checks that there are no duplicates
        // succeeds because distinct between subscriptions

        String filename = "ABC_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeA> typeAChanged = AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA");


        Observable<TypeB> bsTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeB>() {
            @Override
            public TypeB call(TypeA typeA) {
                return new TypeB(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeB> typeBChanged = AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            .mergeWith(bsTriggeredByA)
            ;


        Observable<TypeC> csTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeC>() {
            @Override
            public TypeC call(TypeA typeA) {
                return new TypeC(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeC> csTriggeredByB = typeBChanged.map(new Func1<TypeB, TypeC>() {
            @Override
            public TypeC call(TypeB typeB) {
                return new TypeC(typeB.getId(), typeB.makeSourceString());
            }
        });


        Observable<TypeC> typeCChanged = AssetObservable
            .filterType(TypeC.class, allChangedAssets, "TypeC")
            .mergeWith(csTriggeredByA)
            .mergeWith(csTriggeredByB);


        //SUBSCRIBE


        //Magic going on here..we distinct (de-dupe) everything before the subscription
        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged
            .distinct()
            .subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged
            .distinct()
            .subscribe(bObserver);

        TypeCWriterObserver cObserver = new TypeCWriterObserver();
        typeCChanged
            .distinct()
            .subscribe(cObserver);

        Application.simulateChanges(source, filename);


        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();
        Object[] cContents = cObserver.getContents();


        //Make sure EVERY A has corresponding B & C
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();

            //Corresponding B for A is same Id value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
            //Corresponding C for A is same Id value
            assertThat(cContents, hasItemInArray(hasProperty("id", equalTo(id))));

        }

        //Make sure EVERY B has corresponding C
        for(Object b : bContents){
            Integer id = ((TypeB)b).getId();

            //Corresponding C for B is same Id value
            assertThat(cContents, hasItemInArray(hasProperty("id", equalTo(id))));

        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

    }


    @Test
    public void testCDepsAOrBBDepsADupesInFileDistinctBeforeSubscriptions() throws IOException {
        //DEALS with only A's, B's, and C's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Every A record causes C records of same id to need updating
        //Every B record causes C records of same id to need updating
        //Checks that there are no duplicates
        // succeeds because distinct between subscriptions

        String filename = "ABC_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeA> typeAChanged = AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA");


        Observable<TypeB> bsTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeB>() {
            @Override
            public TypeB call(TypeA typeA) {
                return new TypeB(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeB> typeBChanged = AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            .mergeWith(bsTriggeredByA)
            ;


        Observable<TypeC> csTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeC>() {
            @Override
            public TypeC call(TypeA typeA) {
                return new TypeC(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeC> csTriggeredByB = typeBChanged.map(new Func1<TypeB, TypeC>() {
            @Override
            public TypeC call(TypeB typeB) {
                return new TypeC(typeB.getId(), typeB.makeSourceString());
            }
        });


        Observable<TypeC> typeCChanged = AssetObservable
            .filterType(TypeC.class, allChangedAssets, "TypeC")
            .mergeWith(csTriggeredByA)
            .mergeWith(csTriggeredByB);


        //SUBSCRIBE

        //Magic going on here..we distinct (de-dupe) everything before the subscription
        typeAChanged.count().subscribe(new LogAction<Integer>("A changes observed: "));
        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged
            .distinct()
            .subscribe(aObserver);

        typeBChanged.count().subscribe(new LogAction<Integer>("B changes observed: "));
        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged
            .distinct()
            .subscribe(bObserver);

        typeCChanged.count().subscribe(new LogAction<Integer>("C changes observed: "));
        TypeCWriterObserver cObserver = new TypeCWriterObserver();
        typeCChanged
            .distinct()
            .subscribe(cObserver);

        Application.simulateChanges(source, filename);


        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();
        Object[] cContents = cObserver.getContents();

        //Make sure EVERY A has corresponding B & C
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();

            //Corresponding B for A is same Id value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
            //Corresponding C for A is same Id value
            assertThat(cContents, hasItemInArray(hasProperty("id", equalTo(id))));

        }

        //Make sure EVERY B has corresponding C
        for(Object b : bContents){
            Integer id = ((TypeB)b).getId();

            //Corresponding C for B is same Id value
            assertThat(cContents, hasItemInArray(hasProperty("id", equalTo(id))));

        }




        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

    }


    @Test
    public void testCDepsAOrBBDepsADupesInFileDistinctAfterChangedAndBeforeSubscriptions() throws IOException {
        //DEALS with only A's, B's, and C's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Every A record causes C records of same id to need updating
        //Every B record causes C records of same id to need updating
        //Checks that there are no duplicates
        // succeeds because distinct between subscriptions and before
        // Note that the observed changes for each is the same as the actual output
        // Important to put distinct()'s up front as possible to lessen duplicate "expensive" work

        String filename = "ABC_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeA> typeAChanged = AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA")
            .distinct();


        Observable<TypeB> bsTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeB>() {
            @Override
            public TypeB call(TypeA typeA) {
                return new TypeB(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeB> typeBChanged = AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            //Note: could do a distinct here if there were dupes in source
            .mergeWith(bsTriggeredByA)
            .distinct()
            ;


        Observable<TypeC> csTriggeredByA = typeAChanged.map(new Func1<TypeA, TypeC>() {
            @Override
            public TypeC call(TypeA typeA) {
                return new TypeC(typeA.getId(), typeA.makeSourceString());
            }
        });

        Observable<TypeC> csTriggeredByB = typeBChanged.map(new Func1<TypeB, TypeC>() {
            @Override
            public TypeC call(TypeB typeB) {
                return new TypeC(typeB.getId(), typeB.makeSourceString());
            }
        });


        Observable<TypeC> typeCChanged = AssetObservable
            .filterType(TypeC.class, allChangedAssets, "TypeC")
            //NOTE: switch these lines to see sources change in output..right now A's are favoured over B's
            .mergeWith(csTriggeredByA)
            .mergeWith(csTriggeredByB)
            ;


        //SUBSCRIBE


        //Magic going on here..we distinct (de-dupe) everything before the subscription
        typeAChanged.count().subscribe(new LogAction<Integer>("A changes observed: "));
        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged
            //.distinct() //worthless..this is already applied above
            .subscribe(aObserver);

        typeBChanged.count().subscribe(new LogAction<Integer>("B changes observed: "));
        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged
            //.distinct()  //worthless..this is already applied above
            .subscribe(bObserver);

        typeCChanged.count().subscribe(new LogAction<Integer>("C changes observed: "));
        TypeCWriterObserver cObserver = new TypeCWriterObserver();
        typeCChanged
            .distinct()
            .subscribe(cObserver);

        Application.simulateChanges(source, filename);


        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();
        Object[] cContents = cObserver.getContents();

        //Make sure EVERY A has corresponding B & C
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();

            //Corresponding B for A is same Id value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
            //Corresponding C for A is same Id value
            assertThat(cContents, hasItemInArray(hasProperty("id", equalTo(id))));

        }

        //Make sure EVERY B has corresponding C
        for(Object b : bContents){
            Integer id = ((TypeB)b).getId();

            //Corresponding C for B is same Id value
            assertThat(cContents, hasItemInArray(hasProperty("id", equalTo(id))));

        }




        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

    }

}
