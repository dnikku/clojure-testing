package com.some_project;


import com.github.dnikku.clojure_testing.ClojureTests;
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

    @TestFactory
    public Iterator<DynamicNode> onlyExpectations() {
        return runner.execute(
                List.of("^com\\.clojure-project\\.some-expectations")
        );
    }

    @TestFactory
    public Iterator<DynamicNode> onlyTestsWithCustomStacktraceFilter() {
        return runner.execute(
                List.of("^com\\.clojure-project\\.some-tests"),

                List.of("^com\\.github\\.dnikku\\.clojure_testing\\.",
                        "^org\\.junit",
                        "^org\\.gradle",
                        "^java\\.",
                        "^jdk\\.internal",
                        "^clojure\\.test",
                        "^clojure\\.lang"
                )
        );
    }
}