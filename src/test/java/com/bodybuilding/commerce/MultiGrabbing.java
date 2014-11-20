package com.bodybuilding.commerce;

import com.bodybuilding.commerce.assets.TypeA;
import com.bodybuilding.commerce.assets.TypeB;
import com.bodybuilding.commerce.observers.TypeAWriterObserver;
import com.bodybuilding.commerce.observers.TypeBWriterObserver;
import com.bodybuilding.commerce.observers.TypeCWriterObserver;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.observables.GroupedObservable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

public class MultiGrabbing {

    // In some cases it may make sense to grab multiple changes and react to them in a group
    // perhaps it makes less roundtrips to the persistence layer/db?

    static class MultiGrabbingReadModelSystem extends ReadModelSystem {
        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        TypeCWriterObserver cObserver = new TypeCWriterObserver();


        public MultiGrabbingReadModelSystem(String s) {
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

        public static List<TypeB> doSomeLogicInBulk(List<TypeA> typeAs){
            //This doesn't really do anything in bulk...but it would
            List<TypeB> result = new ArrayList<TypeB>();
            for(TypeA a : typeAs){
                if(a.getId() % 2 ==1 ){
                    result.add(new TypeB(a.getId(), a.getSource()));
                }

            }
            return result;

        }


        public static List<TypeB> doSomeLogicInBulk2(List<TypeA> typeAs){
            //This doesn't really do anything in bulk...but it would
            //NOTE that this doesn't need to do the odd check
            List<TypeB> result = new ArrayList<TypeB>();
            for(TypeA a : typeAs){
                result.add(new TypeB(a.getId(), a.getSource()));
            }
            return result;

        }

        public void wireUpMultiGrabbingWithFlatMap(int numberAtATime){
            this.typeAChangesVerified
                .buffer(numberAtATime)
                .flatMap(new Func1<List<TypeA>, Observable<TypeB>>() {
                    @Override
                    public Observable<TypeB> call(List<TypeA> typeAs) {
                        //GO DO SOME checks with the List of typeAs
                        List<TypeB> resultsOfBulkOperation = doSomeLogicInBulk(typeAs);

                        return Observable.from(resultsOfBulkOperation);
                    }
                })
                .subscribe(this.typeBChangeRequests);
        }


        public void wireUpMultiGrabbingWithGroupBy(final int numberAtATime){
            //In some cases it makes sense to do the bulk operations on a set
            //of entries that satisfy some condition
            //this example is a little trite because we don't have a bulk operation
            this.typeAChangesVerified
                .groupBy(new Func1<TypeA, Integer>() {
                    @Override
                    public Integer call(TypeA typeA) {
                        return typeA.getId() % 2;
                    }
                })
                .flatMap(new Func1<GroupedObservable<Integer, TypeA>, Observable<TypeB>>() {
                    @Override
                    public Observable<TypeB> call(GroupedObservable<Integer, TypeA> integerTypeAGroupedObservable) {
                        if(integerTypeAGroupedObservable.getKey() == 1){
                            return integerTypeAGroupedObservable
                                .buffer(numberAtATime)
                                .flatMap(new Func1<List<TypeA>, Observable<TypeB>>() {
                                @Override
                                public Observable<TypeB> call(List<TypeA> typeA) {
                                    return Observable.from(doSomeLogicInBulk2(typeA));
                                }
                            });
                        } else {
                            return Observable.never();
                        }
                    }
                })
                .subscribe(this.typeBChangeRequests);
        }


    }

    @Test
    public void testMultiGrabbingNoDupesInFile() throws IOException {



        String filename = "ABC_NO_DUPES.txt";

        MultiGrabbingReadModelSystem system = new MultiGrabbingReadModelSystem(filename);

        system.wireUpMultiGrabbingWithFlatMap(5);


        system.simulateChangeProcessing();

        // verify things went right
        Object[] aContents = system.getAObserver().getContents();
        Object[] bContents = system.getBObserver().getContents();
        Object[] cContents = system.getCObserver().getContents();


        //Make sure EVERY A that is Odd has corresponding B
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();
            if(id % 2 == 1) {
                //Corresponding B for odd A's is same Id value
                assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
            } else {
                //There should be no B's for even A's (note for this condition to be satisfied the input source file must never have an even B corresponding to an A)
                assertThat(bContents, not(hasItemInArray(hasProperty("id", equalTo(id)))));
            }
        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));
    }


    @Test
    public void testMultiGrabbingDupesInFile() throws IOException {



        String filename = "ABC_DUPES.txt";

        MultiGrabbingReadModelSystem system = new MultiGrabbingReadModelSystem(filename);

        system.wireUpMultiGrabbingWithFlatMap(5);


        system.simulateChangeProcessing();

        // verify things went right
        Object[] aContents = system.getAObserver().getContents();
        Object[] bContents = system.getBObserver().getContents();
        Object[] cContents = system.getCObserver().getContents();


        //Make sure EVERY A that is Odd has corresponding B
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();
            if(id % 2 == 1) {
                //Corresponding B for odd A's is same Id value
                assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
            } else {
                //There should be no B's for even A's (note for this condition to be satisfied the input source file must never have an even B corresponding to an A)
                assertThat(bContents, not(hasItemInArray(hasProperty("id", equalTo(id)))));
            }
        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));
    }



    @Test
    public void testMultiGrabbingNoDupesInFileUsingGroup() throws IOException {



        String filename = "ABC_NO_DUPES.txt";

        MultiGrabbingReadModelSystem system = new MultiGrabbingReadModelSystem(filename);

        system.wireUpMultiGrabbingWithGroupBy(5);


        system.simulateChangeProcessing();

        // verify things went right
        Object[] aContents = system.getAObserver().getContents();
        Object[] bContents = system.getBObserver().getContents();
        Object[] cContents = system.getCObserver().getContents();


        //Make sure EVERY A that is Odd has corresponding B
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();
            if(id % 2 == 1) {
                //Corresponding B for odd A's is same Id value
                assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
            } else {
                //There should be no B's for even A's (note for this condition to be satisfied the input source file must never have an even B corresponding to an A)
                assertThat(bContents, not(hasItemInArray(hasProperty("id", equalTo(id)))));
            }
        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));
    }


    @Test
    public void testMultiGrabbingDupesInFileUsingGroup() throws IOException {



        String filename = "ABC_DUPES.txt";

        MultiGrabbingReadModelSystem system = new MultiGrabbingReadModelSystem(filename);

        system.wireUpMultiGrabbingWithGroupBy(5);


        system.simulateChangeProcessing();

        // verify things went right
        Object[] aContents = system.getAObserver().getContents();
        Object[] bContents = system.getBObserver().getContents();
        Object[] cContents = system.getCObserver().getContents();


        //Make sure EVERY A that is Odd has corresponding B
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();
            if(id % 2 == 1) {
                //Corresponding B for odd A's is same Id value
                assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id))));
            } else {
                //There should be no B's for even A's (note for this condition to be satisfied the input source file must never have an even B corresponding to an A)
                assertThat(bContents, not(hasItemInArray(hasProperty("id", equalTo(id)))));
            }
        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));
    }

}

