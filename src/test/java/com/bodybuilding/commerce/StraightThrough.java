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

public class StraightThrough {

    @Test
    public void testStraightThrough() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Doesn't have any inter dependencies between types
        //All written out...no helpers

        final String filename = "AB_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = source
            .map(new Func1<String, Asset>() {
                @Override
                public Asset call(String s) {
                    String[] split = s.split(":");
                    return new Asset(split[1], split[0], filename);
                }
            })
            //.doOnEach(new LogAction())
            ;

        Observable<TypeA> typeAChanged = allChangedAssets
            .filter(new Func1<Asset, Boolean>() {
                @Override
                public Boolean call(Asset asset) {
                    return asset.getType().equals("TypeA");
                }
            })
            .map(new Func1<Asset, TypeA>() {
                @Override
                public TypeA call(Asset asset) {
                    return new TypeA(asset.getId(), asset.getSource());
                }
            })
            ;

        Observable<TypeB> typeBChanged = allChangedAssets
            .filter(new Func1<Asset, Boolean>() {
                @Override
                public Boolean call(Asset asset) {
                    return asset.getType().equals("TypeB");
                }
            })
            .map(new Func1<Asset, TypeB>() {
                @Override
                public TypeB call(Asset asset) {
                    return new TypeB(asset.getId(), asset.getSource());
                }
            })
            ;


        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged.subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged.subscribe(bObserver);

        Application.simulateChanges(source, filename);

    }


    @Test
    public void testStraightThroughWithHelpers() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Doesn't have any inter dependencies between types
        //Switched up to some specific helpers

        String filename = "AB_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

        Observable<TypeB> typeBChanged = AssetObservable.typeBs(allChangedAssets);
        Observable<TypeA> typeAChanged = AssetObservable.typeAs(allChangedAssets);


        TypeAWriterObserver aObserver = new TypeAWriterObserver();
        typeAChanged.subscribe(aObserver);

        TypeBWriterObserver bObserver = new TypeBWriterObserver();
        typeBChanged.subscribe(bObserver);

        Application.simulateChanges(source, filename);

    }



    @Test
    public void testStraightThroughWithGenericHelpers() throws IOException {
        //DEALS with only A's and B's
        //Ignores everything else
        //Doesn't care about missed things
        //Doesn't have any inter dependencies between types
        //Switched up to have some generic helpers for As & Bs

        String filename = "AB_NO_DUPES.txt";

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

    }


    @Test
    public void testStraightThroughThreeTypes() throws IOException {
        //DEALS with only A's, B's, & C's
        //Ignores everything else
        //Doesn't care about missed things
        //Doesn't have any inter dependencies between types

        String filename = "ABC_NO_DUPES.txt";

        //Build up dependency tree
        PublishSubject<String> source = PublishSubject.create();

        Observable<Asset> allChangedAssets = AssetObservable.from(source, filename);

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

    }



}
