package com.bodybuilding.commerce;

import com.bodybuilding.commerce.assets.Asset;
import com.bodybuilding.commerce.assets.TypeA;
import com.bodybuilding.commerce.assets.TypeB;
import com.bodybuilding.commerce.observables.AssetObservable;
import com.bodybuilding.commerce.observers.TypeAWriterObserver;
import com.bodybuilding.commerce.observers.TypeBWriterObserver;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.subjects.PublishSubject;

import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class BDependsOnAADependsOnB {


    @Test
    public void testCircularNoDupesInFileProblem() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Every B record causes A records of same id to need updating
        //Checks that there are no duplicates

        String filename = "AB_NO_DUPES.txt";

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
            .filterType(TypeB.class, allChangedAssets, "TypeB") //Those from source
            .mergeWith(bsTriggeredByA); //Those from A changing

        Observable<TypeA> asTriggeredByB = typeBChanged.map(new Func1<TypeB, TypeA>() {
            @Override
            public TypeA call(TypeB typeB) {
                return new TypeA(typeB.getId(), typeB.makeSourceString());
            }
        });


        //Always a chicken or egg...how do you make a Observable<B> depend on an Observable<A> that depends on Observable<B>?


    }


    @Test
    public void testCircularNoDupesInFileProblemSolutionPart1() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Every B record causes A records of same id to need updating
        //Checks that there are no duplicates



        String filename = "AB_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        //BEGIN CONCEPTUAL CHANGES -- We need a observable that can subscribe to various things
        //This is known as a Subject
        //NOTE: having one subscriber can't really work because it causes duplicates (as well as 2 on completes...which is weird

        PublishSubject<TypeA> typeAChanges = PublishSubject.create();
        PublishSubject<TypeB> typeBChanges = PublishSubject.create();


        //Subscribe our subjects to source changes
        AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA")
            .subscribe(typeAChanges);

        AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            .subscribe(typeBChanges);


        //Now observer changes from these and subscribe to them as needed
        typeAChanges.map(new Func1<TypeA, TypeB>() {
            @Override
            public TypeB call(TypeA typeA) {
                return new TypeB(typeA.getId(), typeA.makeSourceString());
            }
        }).subscribe(typeBChanges);


        typeBChanges.map(new Func1<TypeB, TypeA>() {
            @Override
            public TypeA call(TypeB typeB) {
                return new TypeA(typeB.getId(), typeB.makeSourceString());
            }
        }).subscribe(typeAChanges);


        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanges.subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanges.subscribe(bObserver);

        //We will stack overflow at this point!
        Application.simulateChanges(source, filename);




    }






    @Test
    public void testCircularNoDupesInFileProblemSolutionPart2() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Every B record causes A records of same id to need updating
        //Checks that there are no duplicates



        String filename = "AB_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        //BEGIN CONCEPTUAL CHANGES -- We need a observable that can subscribe to various things
        //This is known as a Subject
        //NOTE: having one subscriber can't really work because it causes duplicates (as well as 2 on completes...which is weird

        //Conceptual Change: need a unique set of changes to hook to to avoid circular detection
        //Call them In's and Outs or Requests & Verifications...terminology still not finalized
        PublishSubject<TypeA> typeAChangeRequests = PublishSubject.create();
        PublishSubject<TypeB> typeBChangeRequests = PublishSubject.create();


        //Subscribe our request subjects to source changes
        AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA")
            .subscribe(typeAChangeRequests);

        AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            .subscribe(typeBChangeRequests);


        //Now build an observable of "verified" changes
        //Why verified? well..right now it's only verified if it's unique
        //but we could have more filtering logic to weed out changes that shouldn't propagate throughout the system
        Observable<TypeA> typeAChangesVerified = typeAChangeRequests.distinct();
        Observable<TypeB> typeBChangesVerified = typeBChangeRequests.distinct();

        //Now observer changes from these and subscribe to them as needed
        typeAChangesVerified.map(new Func1<TypeA, TypeB>() {
            @Override
            public TypeB call(TypeA typeA) {
                return new TypeB(typeA.getId(), typeA.makeSourceString());
            }
        }).subscribe(typeBChangeRequests);


        typeBChangesVerified.map(new Func1<TypeB, TypeA>() {
            @Override
            public TypeA call(TypeB typeB) {
                return new TypeA(typeB.getId(), typeB.makeSourceString());
            }
        }).subscribe(typeAChangeRequests);


        //REACT to ONLY verified changes
        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChangesVerified.subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChangesVerified.subscribe(bObserver);

        typeAChangeRequests.count().subscribe(new LogAction<Integer>("A changes requested: "));
        typeBChangeRequests.count().subscribe(new LogAction<Integer>("B changes requested: "));

        Application.simulateChanges(source, filename);

        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();

        //Make sure EVERY A has corresponding B
        for (Object a : aContents) {
            Integer aId = ((TypeA) a).getId();

            //Corresponding B for A is same Id value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(aId))));

        }

        //Make sure EVERY B has corresponding A
        for (Object b : bContents) {
            Integer bId = ((TypeB) b).getId();

            //Corresponding A for B is same Id value
            assertThat(aContents, hasItemInArray(hasProperty("id", equalTo(bId))));

        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));


    }


    @Test
    public void testCircularDupesInFile() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Every A record causes B records of same id to need updating
        //Every B record causes A records of same id to need updating
        //Checks that there are no duplicates



        String filename = "AB_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        //BEGIN CONCEPTUAL CHANGES -- We need a observable that can subscribe to various things
        //This is known as a Subject
        //NOTE: having one subscriber can't really work because it causes duplicates (as well as 2 on completes...which is weird

        //Conceptual Change: need a unique set of changes to hook to to avoid circular detection
        //Call them In's and Outs or Requests & Verifications...terminology still not finalized
        PublishSubject<TypeA> typeAChangeRequests = PublishSubject.create();
        PublishSubject<TypeB> typeBChangeRequests = PublishSubject.create();


        //Subscribe our request subjects to source changes
        AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA")
            .distinct() //valuable, remove and see workload changes
            .subscribe(typeAChangeRequests);

        AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            .distinct() //valuable, remove and see workload changes
            .subscribe(typeBChangeRequests);


        //Now build an observable of "verified" changes
        //Why verified? well..right now it's only verified if it's unique
        //but we could have more filtering logic to weed out changes that shouldn't propagate throughout the system
        Observable<TypeA> typeAChangesVerified = typeAChangeRequests.distinct();
        Observable<TypeB> typeBChangesVerified = typeBChangeRequests.distinct();

        //Now observer changes from these and subscribe to them as needed
        typeAChangesVerified.map(new Func1<TypeA, TypeB>() {
            @Override
            public TypeB call(TypeA typeA) {
                return new TypeB(typeA.getId(), typeA.makeSourceString());
            }
        }).subscribe(typeBChangeRequests);


        typeBChangesVerified.map(new Func1<TypeB, TypeA>() {
            @Override
            public TypeA call(TypeB typeB) {
                return new TypeA(typeB.getId(), typeB.makeSourceString());
            }
        }).subscribe(typeAChangeRequests);


        //REACT to ONLY verified changes
        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChangesVerified.subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChangesVerified.subscribe(bObserver);

        typeAChangeRequests.count().subscribe(new LogAction<Integer>("A changes requested: "));
        typeBChangeRequests.count().subscribe(new LogAction<Integer>("B changes requested: "));

        Application.simulateChanges(source, filename);

        Object[] aContents = aObserver.getContents();
        Object[] bContents = bObserver.getContents();

        //Make sure EVERY A has corresponding B
        for (Object a : aContents) {
            Integer aId = ((TypeA) a).getId();

            //Corresponding B for A is same Id value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(aId))));

        }

        //Make sure EVERY B has corresponding A
        for (Object b : bContents) {
            Integer bId = ((TypeB) b).getId();

            //Corresponding A for B is same Id value
            assertThat(aContents, hasItemInArray(hasProperty("id", equalTo(bId))));

        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));


    }

}
