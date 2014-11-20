package com.bodybuilding.commerce.observables;

import com.bodybuilding.commerce.assets.Asset;
import com.bodybuilding.commerce.assets.TypeA;
import com.bodybuilding.commerce.assets.TypeB;
import rx.Observable;
import rx.functions.Func1;

import java.lang.reflect.InvocationTargetException;


public class AssetObservable {

    /**
     * Takes an observable of strings and turns them into assets
     * @param source
     * @return
     */
    public static Observable<Asset> from(Observable<String> source, final String sourceName){
        return source
            .map(new Func1<String, Asset>() {
                @Override
                public Asset call(String s) {
                    String[] split = s.split(":");
                    return new Asset(split[1], split[0], sourceName);
                }
            })

            ;
    }

    /**
     * Filters an observable of assets to just those that are TypeB
     * @param source
     * @return
     */
    public static Observable<TypeB> typeBs(Observable<Asset> source){
        return source
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
    }

    /**
     * Filters an observable of assets to just those that are TypeA
     * @param source
     * @return
     */
    public static Observable<TypeA> typeAs(Observable<Asset> source){
        return source
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
    }

    /**
     * filters assets generically
     * @param classToFilter
     * @param source
     * @param typeString
     * @param <T>
     * @return
     */
    public static <T> Observable<T> filterType(final Class<T> classToFilter, Observable<Asset> source, final String typeString){

        return source
            .filter(new Func1<Asset, Boolean>() {
                @Override
                public Boolean call(Asset asset) {
                    return asset.getType().equals(typeString);
                }
            })
            .map(new Func1<Asset, T>() {
                @Override
                public T call(Asset asset){
                    try {
                        return classToFilter.getConstructor(Integer.class, String.class).newInstance(asset.getId(), asset.getSource());
                    } catch (InstantiationException e) {
                        e.printStackTrace();
                    } catch (IllegalAccessException e) {
                        e.printStackTrace();
                    } catch (InvocationTargetException e) {
                        e.printStackTrace();
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    return null;
                }


            })
            ;
    }
}
