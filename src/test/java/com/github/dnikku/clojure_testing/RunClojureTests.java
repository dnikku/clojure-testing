package com.github.dnikku.clojure_testing;


import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import java.util.Iterator;
import java.util.List;

/**
 * Triggers all Clojure Tests via {@link TestFactory}
 */
public class RunClojureTests {

    private final ClojureTests runner = new ClojureTests();

    @TestFactory
    public Iterator<DynamicNode> all() {
        // run all tests, with default stack-trace filering
        return runner.execute();
    }

}