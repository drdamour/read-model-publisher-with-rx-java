package com.bodybuilding.commerce;

import com.bodybuilding.commerce.assets.TypeA;
import com.bodybuilding.commerce.assets.TypeB;
import com.bodybuilding.commerce.observers.TypeAWriterObserver;
import com.bodybuilding.commerce.observers.TypeBWriterObserver;
import com.bodybuilding.commerce.observers.TypeCWriterObserver;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;

import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class BDependsOnAWhenAIsOdd {

    //Most other tests focus on dependencies that are always true..but this isn't really the case
    //Sometimes a change for an A creates a change for 2 Bs, or a change for an A creates a change for B
    //only for certain conditions of A.
    //These tests deal with techniques for working with the situations when B is triggered by odd A's changing.


    static class BDependsOnAWhenAIsOddReadModelSystem extends ReadModelSystem {
        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        TypeCWriterObserver cObserver = new TypeCWriterObserver();


        public BDependsOnAWhenAIsOddReadModelSystem(String s){
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

        public void wireUpBDependsOnAWhenAIsOddUsingFilters(){
            this.typeAChangesVerified
                //Look only for odds
                .filter(new Func1<TypeA, Boolean>() {
                    @Override
                    public Boolean call(TypeA typeA) {
                        return (typeA.getId() % 2 == 1);
                    }
                })
                //Now generate B's from A's
                .map(new Func1<TypeA, TypeB>() {
                    @Override
                    public TypeB call(TypeA typeA) {
                        return new TypeB(typeA.getId(), typeA.getSource());
                    }
                })
                //Send these over as B change requests
                .subscribe(this.typeBChangeRequests);
        }


        public void wireUpBDependsOnAWhenAIsOddUsingMerge(){

            Observable.merge(
                this.typeAChangesVerified
                //Look only for odds
                .map(new Func1<TypeA, Observable<TypeB>>() {
                    @Override
                    public Observable<TypeB> call(TypeA typeA) {
                        if(typeA.getId() % 2 == 1) {
                            return Observable.just(new TypeB(typeA.getId(), typeA.getSource()));
                        } else {
                            return Observable.never();
                        }
                    }
                })
            )
            //Send these over as B change requests
            .subscribe(this.typeBChangeRequests);
        }



        public void wireUpBDependsOnAWhenAIsOddUsingFlatMap(){

            this.typeAChangesVerified
                //Look only for odds
                .flatMap(new Func1<TypeA, Observable<TypeB>>() {
                    @Override
                    public Observable<TypeB> call(TypeA typeA) {
                        if (typeA.getId() % 2 == 1) {
                            return Observable.just(new TypeB(typeA.getId(), typeA.getSource()));
                        } else {
                            return Observable.never();
                        }
                    }
                })

            //Send these over as B change requests
            .subscribe(this.typeBChangeRequests);
        }
    }

    @Test
    public void testBDependsOnAWhenAIsOddUsingFilters() throws IOException {
        //Build up system
        String filename = "ABC_NO_DUPES.txt";

        BDependsOnAWhenAIsOddReadModelSystem system = new BDependsOnAWhenAIsOddReadModelSystem(filename);

        //wire it for conditions
        system.wireUpBDependsOnAWhenAIsOddUsingFilters();

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
    public void testBDependsOnAWhenAIsOddUsingMerge() throws IOException {
        //Build up system
        String filename = "ABC_NO_DUPES.txt";

        BDependsOnAWhenAIsOddReadModelSystem system = new BDependsOnAWhenAIsOddReadModelSystem(filename);

        //wire it for conditions
        system.wireUpBDependsOnAWhenAIsOddUsingMerge();

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
    public void testBDependsOnAWhenAIsOddUsingFlatMap() throws IOException {
        //Build up system
        String filename = "ABC_NO_DUPES.txt";

        BDependsOnAWhenAIsOddReadModelSystem system = new BDependsOnAWhenAIsOddReadModelSystem(filename);

        //wire it for conditions
        system.wireUpBDependsOnAWhenAIsOddUsingFlatMap();

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
