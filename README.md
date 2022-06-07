# clojure-testing

A library that allows you to run `clojure.test` from a [gradle project](https://gradle.org/) via [junit-jupiter](https://search.maven.org/artifact/org.junit.jupiter/junit-jupiter-api/5.7.1/jar) besides java tests.

## Where ?
[![Clojars Project](https://img.shields.io/clojars/v/org.clojars.dnikku/clojure-testing.svg)](https://clojars.org/org.clojars.dnikku/clojure-testing)



## Quick example

```clojure
(ns com.clojure-project.some-tests
  (:require
    [clojure.test :refer [deftest is]]))

;; some tests may failed, to see the output

(deftest unhandled-exception
  (throw (Exception. "unhandled error")))

(deftest failed-assertion
  (is (= {:some-key "one"} {:some-key "two"})))

(deftest ok-assertion
  (is (= {:some-key "one"} {:some-key "one"})))
```

Intellij Output:
```
FAIL in (failed-assertion) (some_tests.clj:12)
expected: <(= {:some-key "one"} {:some-key "two"})> but was: <(not (= {:some-key "one"} {:some-key "two"}))>
Expected :(= {:some-key "one"} {:some-key "two"})
Actual   :(not (= {:some-key "one"} {:some-key "two"}))
<Click to see difference>

com.github.dnikku.clojure_testing.ClojureTests$Fx$_AssertionFailedError: FAIL in (failed-assertion) (some_tests.clj:12)
expected: <(= {:some-key "one"} {:some-key "two"})> but was: <(not (= {:some-key "one"} {:some-key "two"}))>


ERROR in (unhandled-exception) (some_tests.clj:7)
com.github.dnikku.clojure_testing.ClojureTests$Fx$_Error: ERROR in (unhandled-exception) (some_tests.clj:7)
	at com.clojure_project.some_tests$fn__295.invokeStatic(some_tests.clj:7)
	at com.clojure_project.some_tests$fn__295.invoke(some_tests.clj:7)
	at clojure.core$apply.invokeStatic(core.clj:667)
	at clojure.core$with_bindings_STAR_.invokeStatic(core.clj:1990)
	at clojure.core$with_bindings_STAR_.doInvoke(core.clj:1990)
	at clojure.core$apply.invokeStatic(core.clj:667)
	at clojure.core$apply.invoke(core.clj:662)
	at com.some_project.RunClojureTests.onlyTestsWithCustomStacktraceFilter(RunClojureTests.java:33)
	at com.sun.proxy.$Proxy2.stop(Unknown Source)
```

```java
package com.some_project;

import com.github.dnikku.clojure_testing.ClojureTests;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.TestFactory;

import java.util.Iterator;
import java.util.List;

public class RunClojureTests {
    
    private final ClojureTests runner = new ClojureTests();

    @TestFactory
    public Iterator<DynamicNode> allClojureTests() {
        return runner.execute();
    }
}
```


## [Try it out](./doc/sample-project.md)


## How ?
Using new [Junit5 DynamicTest](https://www.baeldung.com/junit5-dynamic-tests).

TODO: add more details.
