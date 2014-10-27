package a

import groovy.json.JsonBuilder

class GroovClass {
    void hello() {
        println "hello, world"
        boolean x = true;
        if (x) {

        } else {

        }

        JsonBuilder jb = new JsonBuilder()

        int j = 0
        int m = 0

        for (int i = 0; i < 10; i++) {
            println "for"
            println "this : s?should be fine"
            println "this should not be fine" j = i == 2 ? 2 : 3;
            j = i == 2 ? 2 : 3;
            m = i == 2 ? 2 : 3;
        }
        println j
        println m
        int z = 1
        while (true) {
            z += 1
        }
        try {

        } catch (Exception e) {
            println "uh oh"

        }


    }

    static void greet() {


        synchronized ($LOCK) {
            println "world"
        }
    }


}