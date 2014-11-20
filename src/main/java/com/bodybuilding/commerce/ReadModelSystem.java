package com.bodybuilding.commerce;

import com.bodybuilding.commerce.assets.Asset;
import com.bodybuilding.commerce.assets.TypeA;
import com.bodybuilding.commerce.assets.TypeB;
import com.bodybuilding.commerce.assets.TypeC;
import com.bodybuilding.commerce.observables.AssetObservable;
import rx.Observable;
import rx.Observer;
import rx.subjects.PublishSubject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class ReadModelSystem {
    final PublishSubject<String> source = PublishSubject.create();

    final PublishSubject<TypeA> typeAChangeRequests = PublishSubject.create();
    final PublishSubject<TypeB> typeBChangeRequests = PublishSubject.create();
    final PublishSubject<TypeC> typeCChangeRequests = PublishSubject.create();


    final Observable<TypeA> typeAChangesVerified = typeAChangeRequests.distinct();
    final Observable<TypeB> typeBChangesVerified = typeBChangeRequests.distinct();
    final Observable<TypeC> typeCChangesVerified = typeCChangeRequests.distinct();

    final String resourceFileName;

    public ReadModelSystem(String resourceFileName){
        this.resourceFileName = resourceFileName;

        Observable<Asset> allChangedAssets = AssetObservable.from(source, resourceFileName);

        AssetObservable
            .filterType(TypeA.class, allChangedAssets, "TypeA")
            .distinct() //valuable as long as the source can have dupes
            .subscribe(typeAChangeRequests);

        AssetObservable
            .filterType(TypeB.class, allChangedAssets, "TypeB")
            .distinct() //valuable as long as the source can have dupes
            .subscribe(typeBChangeRequests);

        AssetObservable
            .filterType(TypeC.class, allChangedAssets, "TypeC")
            .distinct() //valuable as long as the source can have dupes
            .subscribe(typeCChangeRequests);
    }

    public Observer<String> getChangedAssetsObserver(){
        return source;
    }

    public Observable<TypeA> getTypeAChanges(){
        return typeAChangesVerified;
    }


    public Observable<TypeB> getTypeBChanges(){
        return typeBChangesVerified;
    }


    public Observable<TypeC> getTypeCChanges(){
        return typeCChangesVerified;
    }



    public void simulateChangeProcessing() throws IOException {

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(resourceFileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while((line = br.readLine()) != null){
            if(line.startsWith("#")) continue;
            source.onNext(line);

        }

        source.onCompleted();

    }
}
