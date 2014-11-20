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

public class BDependsOnATwoBsPerA {

    //Most other tests focus on dependencies that are always true..but this isn't really the case
    //Sometimes a change for an A creates a change for 2 Bs, or a change for an A creates a change for B
    //only for certain conditions of A.
    //These tests deal with techniques for working with the situations when B is triggered by odd A's changing.
    //A B is generate -10 and +10 for every A


    static class BDependsOnATwoBsPerAReadModelSystem extends ReadModelSystem {
        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        TypeCWriterObserver cObserver = new TypeCWriterObserver();


        public BDependsOnATwoBsPerAReadModelSystem(String s){
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

        public void wireUpBDependsOnATwoBsPerAUsingFilters(){
            this.typeAChangesVerified
                //Now generate B's from A's -10
                .map(new Func1<TypeA, TypeB>() {
                    @Override
                    public TypeB call(TypeA typeA) {
                        return new TypeB(typeA.getId()-10, typeA.getSource());
                    }
                })
                //Send these over as B change requests
                .subscribe(this.typeBChangeRequests);

            this.typeAChangesVerified
                //Now generate B's from A's +10
                .map(new Func1<TypeA, TypeB>() {
                    @Override
                    public TypeB call(TypeA typeA) {
                        return new TypeB(typeA.getId()+10, typeA.getSource());
                    }
                })
                //Send these over as B change requests
                .subscribe(this.typeBChangeRequests);
        }


        public void wireUpBDependsOnATwoBsPerAUsingMerge(){

            Observable.merge(
                this.typeAChangesVerified
                //Look only for odds
                .map(new Func1<TypeA, Observable<TypeB>>() {
                    @Override
                    public Observable<TypeB> call(TypeA typeA) {

                        return Observable.just(
                            new TypeB(typeA.getId()+10, typeA.getSource()),
                            new TypeB(typeA.getId()-10, typeA.getSource())
                        );

                    }
                })
            )
            //Send these over as B change requests
            .subscribe(this.typeBChangeRequests);
        }



        public void wireUpBDependsOnATwoBsPerAUsingFlatMap(){

            this.typeAChangesVerified
                //Look only for odds
                .flatMap(new Func1<TypeA, Observable<TypeB>>() {
                    @Override
                    public Observable<TypeB> call(TypeA typeA) {
                        return Observable.just(
                            new TypeB(typeA.getId()+10, typeA.getSource()),
                            new TypeB(typeA.getId()-10, typeA.getSource())
                        );
                    }
                })

            //Send these over as B change requests
            .subscribe(this.typeBChangeRequests);
        }
    }

    @Test
    public void testBDependsOnATwoBsPerAUsingFilters() throws IOException {
        //Build up system
        String filename = "ABC_NO_DUPES.txt";

        BDependsOnATwoBsPerAReadModelSystem system = new BDependsOnATwoBsPerAReadModelSystem(filename);

        //wire it for conditions
        system.wireUpBDependsOnATwoBsPerAUsingFilters();

        system.simulateChangeProcessing();

        // verify things went right
        Object[] aContents = system.getAObserver().getContents();
        Object[] bContents = system.getBObserver().getContents();
        Object[] cContents = system.getCObserver().getContents();


        //Make sure EVERY A has the 2 corresponding B's
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();

            //Corresponding B for odd A's is A's Id+10 and value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id+10))));
            //Corresponding B for odd A's is A's Id-10 and value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id-10))));

        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

    }


    @Test
    public void testBDependsOnATwoBsPerAUsingMerge() throws IOException {
        //Build up system
        String filename = "ABC_NO_DUPES.txt";

        BDependsOnATwoBsPerAReadModelSystem system = new BDependsOnATwoBsPerAReadModelSystem(filename);

        //wire it for conditions
        system.wireUpBDependsOnATwoBsPerAUsingMerge();

        system.simulateChangeProcessing();

        // verify things went right
        Object[] aContents = system.getAObserver().getContents();
        Object[] bContents = system.getBObserver().getContents();
        Object[] cContents = system.getCObserver().getContents();


        //Make sure EVERY A has the 2 corresponding B's
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();

            //Corresponding B for odd A's is A's Id+10 and value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id+10))));
            //Corresponding B for odd A's is A's Id-10 and value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id-10))));

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

        BDependsOnATwoBsPerAReadModelSystem system = new BDependsOnATwoBsPerAReadModelSystem(filename);

        //wire it for conditions
        system.wireUpBDependsOnATwoBsPerAUsingFlatMap();

        system.simulateChangeProcessing();

        // verify things went right
        Object[] aContents = system.getAObserver().getContents();
        Object[] bContents = system.getBObserver().getContents();
        Object[] cContents = system.getCObserver().getContents();


        //Make sure EVERY A has the 2 corresponding B's
        for(Object a : aContents){
            Integer id = ((TypeA)a).getId();

            //Corresponding B for odd A's is A's Id+10 and value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id+10))));
            //Corresponding B for odd A's is A's Id-10 and value
            assertThat(bContents, hasItemInArray(hasProperty("id", equalTo(id-10))));

        }

        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

    }


}
