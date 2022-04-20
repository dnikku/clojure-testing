package com.github.dnikku.clojure_testing;

import clojure.java.api.Clojure;
import clojure.lang.IFn;
import clojure.lang.Symbol;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.function.Executable;
import org.opentest4j.AssertionFailedError;

import java.util.Iterator;
import java.util.List;


public class ClojureTests {

    private final List<String> sourceSets;

    public ClojureTests() {
        this(List.of("src/test/clojure"));
    }

    /**
     * Creates a new Instances with all class-paths where the clojure tests will be searched.
     *
     * @param sourceSets
     */
    public ClojureTests(List<String> sourceSets) {
        if (sourceSets == null) throw new RuntimeException("sourceSets should be not null");
        this.sourceSets = sourceSets;
    }

    /*
     * Finds all clojure tests defined via (deftest) in the default test source set.
     * @return an Iterator to all found clojure tests.
     */
    public Iterator<DynamicNode> execute() {
        return execute(null);
    }

    /**
     * Finds all clojure tests defined via (deftest) in the default test source set.
     *
     * @param includeOnlyNamespaces only include namespaces that match one of regular expression
     * @return
     */
    public Iterator<DynamicNode> execute(List<String> includeOnlyNamespaces) {
        return execute(
                includeOnlyNamespaces,
                List.of("^com\\.github\\.dnikku\\.clojure_testing",
                        "^org\\.junit",
                        "^org\\.gradle",
                        "^java\\.",
                        "^jdk\\.internal",
                        "^clojure\\.test",
                        "^clojure\\.lang"
                ));
    }

    /**
     * Finds all clojure tests defined via (deftest) in the default test source set.
     *
     * @param includeOnlyNamespaces only include namespaces that match one of regular expression
     * @param excludeStackClasses   exclude all classes that match one of regular expression
     * @return
     */
    public Iterator<DynamicNode> execute(List<String> includeOnlyNamespaces,
                                         List<String> excludeStackClasses) {
        return invoke("com.github.dnikku.clojure-testing.junit5-runner", "execute",
                sourceSets, includeOnlyNamespaces, excludeStackClasses, null);
    }

    /**
     * Finds all clojure tests defined via (deftest) in the default test source set.
     *
     * @param includeOnlyNamespaces only include namespaces that match one of regular expression
     * @param excludeStackClasses   exclude all classes that match one of regular expression
     * @param stackSize             limit the stack size reported for thrown exception.
     * @return
     */
    public Iterator<DynamicNode> execute(List<String> includeOnlyNamespaces,
                                         List<String> excludeStackClasses,
                                         int stackSize) {
        return invoke("com.github.dnikku.clojure-testing.junit5-runner", "execute",
                sourceSets, includeOnlyNamespaces, excludeStackClasses, stackSize);
    }


    @SuppressWarnings("unchecked")
    private static <T> T invoke(String namespace, String name, Object... args) {
        IFn require = Clojure.var("clojure.core", "require");
        IFn apply = Clojure.var("clojure.core", "apply");

        require.invoke(Symbol.intern(namespace));

        return (T) apply.invoke(Clojure.var(namespace, name), args);
    }

    /*
    public Stream<DynamicTest> discover() {
        // return Stream.empty();
        var list = List.of("nicu", "nader");

        return list.stream()
                .map(p -> newTest(p + getNow(), () -> {
                    expect(p, p);
                }));
    }*/

    /**
     * Some factories methods, used from Clojure side, avoiding clojure java method type annotations.
     */
    public static class Fx {

        public static DynamicContainer newSuite(String name, List<DynamicNode> dynamicNodes) {
            return DynamicContainer.dynamicContainer(name, dynamicNodes.stream());
        }

        public static DynamicTest newTest(String name, Executable executable) {
            return DynamicTest.dynamicTest(name, executable);
        }

        static class _AssertionFailedError extends AssertionFailedError {
            public _AssertionFailedError(String message, Object expected, Object actual) {
                super(message, expected, actual);
                this.setStackTrace(new StackTraceElement[0]);
            }
        }

        public static Throwable newFailure(String message, String expected, String actual) {
            return new _AssertionFailedError(message, expected, actual);
        }

        public static Throwable newError(String message, List<StackTraceElement> stackTraceElements) {
            return new _Error(message,
                    stackTraceElements != null
                            ? stackTraceElements.toArray(new StackTraceElement[stackTraceElements.size()])
                            : new StackTraceElement[0]);
        }

        static class _Error extends Error {
            public _Error(String message, StackTraceElement[] stackTraceElements) {
                super(message);
                this.setStackTrace(stackTraceElements);
            }
        }
    }
}
