package com.bodybuilding.commerce;

import com.bodybuilding.commerce.assets.*;
import com.bodybuilding.commerce.observables.AssetObservable;
import com.bodybuilding.commerce.observers.TypeAWriterObserver;
import com.bodybuilding.commerce.observers.TypeBWriterObserver;
import com.bodybuilding.commerce.observers.TypeCWriterObserver;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;
import rx.Observable;
import rx.functions.Func1;
import rx.functions.Func2;
import rx.subjects.PublishSubject;

import java.io.IOException;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

public class CDependsOnAandB {


    @Test
    public void testCDepsAandBNoDupesInFileUsingJoin() throws IOException {
        //DEALS with only A's, B's, and C's
        //Ignores everything else
        //Doesn't care about missed things
        //A C record needs updating if both an A & B with the same ID need updating


        String filename = "ABC_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeA> typeAChanged = AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA")
            ;


        Observable<TypeB> typeBChanged = AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            ;



        //this is pretty interesting...it's kinda doing a cross product
        //then filtering out all bad matches
        //this is probably not very efficient
        //Is there a better way?
        //what about using group join?
        Observable<TypeC> csTriggeredByAAndB = typeAChanged
            .join(typeBChanged,
                new Func1<TypeA, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(TypeA typeA) {
                        return Observable.never();
                    }
                },
                new Func1<TypeB, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(TypeB typeB) {
                        return Observable.never();
                    }
                },
                new Func2<TypeA, TypeB, BaseType[]>() {
                    @Override
                    public BaseType[] call(TypeA typeA, TypeB typeB) {
                        return new BaseType[] { typeA, typeB};
                    }
                }
            )
            .filter(new Func1<BaseType[], Boolean>() {
                @Override
                public Boolean call(BaseType[] baseTypes) {
                    return baseTypes[0].getId().equals(baseTypes[1].getId());
                }
            })
            .map(new Func1<BaseType[], TypeC>() {
                @Override
                public TypeC call(BaseType[] baseTypes) {
                    return new TypeC(baseTypes[0].getId(), "BOTH[" + baseTypes[0].getSource() +  "," + baseTypes[1].getSource() + "]");
                }
            });


        Observable<TypeC> typeCChanged = AssetObservable
            .filterType(TypeC.class, allChangedAssets, "TypeC")
            .mergeWith(csTriggeredByAAndB)
            ;


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




        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

    }



    @Test
    public void testCDepsAandBNoDupesInFileUsingGroupJoin() throws IOException {
        //DEALS with only A's, B's, and C's
        //Ignores everything else
        //Doesn't care about missed things
        //A C record needs updating if both an A & B with the same ID need updating


        String filename = "ABC_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeA> typeAChanged = AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA")
            ;


        Observable<TypeB> typeBChanged = AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            ;



        Observable<TypeC> csTriggeredByAAndB = Observable.merge(
            typeAChanged
            .groupJoin(typeBChanged,
                new Func1<TypeA, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(TypeA typeA) {
                        return Observable.never();
                    }
                },
                new Func1<TypeB, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(TypeB typeB) {
                        return Observable.never();
                    }
                },
                new Func2<TypeA, Observable<TypeB>, Observable<Pair<TypeA, TypeB>>>() {
                    @Override
                    public Observable<Pair<TypeA, TypeB>> call(final TypeA typeA, Observable<TypeB> typeBObservable) {
                        return typeBObservable.filter(new Func1<TypeB, Boolean>() {
                            @Override
                            public Boolean call(TypeB typeB) {
                                return typeB.getId().equals(typeA.getId());
                            }
                        })
                        .map(new Func1<TypeB, Pair<TypeA, TypeB>>() {
                            @Override
                            public Pair<TypeA, TypeB> call(TypeB typeB) {
                                return  Pair.of(typeA, typeB);
                            }
                        });
                    }
                }
            )
        )
        .map(new Func1<Pair<TypeA, TypeB>, TypeC>() {
            @Override
            public TypeC call(Pair<TypeA, TypeB> p) {
                return new TypeC(p.getLeft().getId(), "BOTH[" + p.getLeft().getSource() +  "," + p.getRight().getSource() + "]");
            }
        });


        Observable<TypeC> typeCChanged = AssetObservable
            .filterType(TypeC.class, allChangedAssets, "TypeC")
            .mergeWith(csTriggeredByAAndB)
            ;


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




        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

    }


    @Test
    public void testCDepsAandBDupesInFileUsingJoin() throws IOException {
        //DEALS with only A's, B's, and C's
        //Ignores everything else
        //Doesn't care about missed things
        //A C record needs updating if both an A & B with the same ID need updating


        String filename = "ABC_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeA> typeAChanged = AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA")
            .distinct()  //<--change
            ;


        Observable<TypeB> typeBChanged = AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            .distinct() //<--change
            ;



        //this is pretty interesting...it's kinda doing a cross product
        //then filtering out all bad matches
        //this is probably not very efficient
        //Is there a better way?
        //what about using group join?
        Observable<TypeC> csTriggeredByAAndB = typeAChanged
            .join(typeBChanged,
                new Func1<TypeA, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(TypeA typeA) {
                        return Observable.never();
                    }
                },
                new Func1<TypeB, Observable<Object>>() {
                    @Override
                    public Observable<Object> call(TypeB typeB) {
                        return Observable.never();
                    }
                },
                new Func2<TypeA, TypeB, BaseType[]>() {
                    @Override
                    public BaseType[] call(TypeA typeA, TypeB typeB) {
                        return new BaseType[] { typeA, typeB};
                    }
                }
            )
            .filter(new Func1<BaseType[], Boolean>() {
                @Override
                public Boolean call(BaseType[] baseTypes) {
                    return baseTypes[0].getId().equals(baseTypes[1].getId());
                }
            })
            .map(new Func1<BaseType[], TypeC>() {
                @Override
                public TypeC call(BaseType[] baseTypes) {
                    return new TypeC(baseTypes[0].getId(), "BOTH[" + baseTypes[0].getSource() +  "," + baseTypes[1].getSource() + "]");
                }
            });


        Observable<TypeC> typeCChanged = AssetObservable
            .filterType(TypeC.class, allChangedAssets, "TypeC")
            .distinct() //<--change. Note it's here, because AndB are guaranteed to be distinct already
            .mergeWith(csTriggeredByAAndB)
            ;


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




        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

    }



    @Test
    public void testCDepsAandBDupesInFileUsingGroupJoin() throws IOException {
        //DEALS with only A's, B's, and C's
        //Ignores everything else
        //Doesn't care about missed things
        //A C record needs updating if both an A & B with the same ID need updating


        String filename = "ABC_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeA> typeAChanged = AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA")
            .distinct()  //<--change
            ;


        Observable<TypeB> typeBChanged = AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            .distinct()  //<--change
            ;



        Observable<TypeC> csTriggeredByAAndB = Observable.merge(
            typeAChanged
                .groupJoin(typeBChanged,
                    new Func1<TypeA, Observable<Object>>() {
                        @Override
                        public Observable<Object> call(TypeA typeA) {
                            return Observable.never();
                        }
                    },
                    new Func1<TypeB, Observable<Object>>() {
                        @Override
                        public Observable<Object> call(TypeB typeB) {
                            return Observable.never();
                        }
                    },
                    new Func2<TypeA, Observable<TypeB>, Observable<Pair<TypeA, TypeB>>>() {
                        @Override
                        public Observable<Pair<TypeA, TypeB>> call(final TypeA typeA, Observable<TypeB> typeBObservable) {
                            return typeBObservable.filter(new Func1<TypeB, Boolean>() {
                                @Override
                                public Boolean call(TypeB typeB) {
                                    return typeB.getId().equals(typeA.getId());
                                }
                            })
                                .map(new Func1<TypeB, Pair<TypeA, TypeB>>() {
                                    @Override
                                    public Pair<TypeA, TypeB> call(TypeB typeB) {
                                        return  Pair.of(typeA, typeB);
                                    }
                                });
                        }
                    }
                )
        )
            .map(new Func1<Pair<TypeA, TypeB>, TypeC>() {
                @Override
                public TypeC call(Pair<TypeA, TypeB> p) {
                    return new TypeC(p.getLeft().getId(), "BOTH[" + p.getLeft().getSource() +  "," + p.getRight().getSource() + "]");
                }
            });


        Observable<TypeC> typeCChanged = AssetObservable
            .filterType(TypeC.class, allChangedAssets, "TypeC")
            .distinct()  //<--change. Note it's here, because AndB are guaranteed to be distinct already
            .mergeWith(csTriggeredByAAndB)
            ;


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




        //CHECKS FOR DUPES
        assertThat(aContents, not(Matchers.hasDuplicateInArray()));
        assertThat(bContents, not(Matchers.hasDuplicateInArray()));
        assertThat(cContents, not(Matchers.hasDuplicateInArray()));

    }


}
