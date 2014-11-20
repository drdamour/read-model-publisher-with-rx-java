package com.bodybuilding.commerce;

import com.bodybuilding.commerce.assets.TypeA;
import rx.Observer;

import java.io.*;


public class Application {



    public static void main(String[] args) throws IOException {


    }


    public static void simulateChanges(Observer<String> allChangedAssets, String resourceFileName ) throws IOException {

        ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        InputStream is = classloader.getResourceAsStream(resourceFileName);
        BufferedReader br = new BufferedReader(new InputStreamReader(is));

        String line = null;
        while((line = br.readLine()) != null){
            //Ignore "comment" lines
            if(line.startsWith("#")) continue;
            allChangedAssets.onNext(line);

        }

        allChangedAssets.onCompleted();

    }


}
