## Sample Project
Full gradle project is here: [build.gradle](../sample-project/build.gradle)



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

    @TestFactory
    public Iterator<DynamicNode> onlyTestsWithCustomStacktraceFilter() {
        return runner.execute(
                List.of("^com\\.clojure-project\\.some-tests"),

                List.of("^com\\.github\\.dnikku\\.clojure_testing",
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
```

## Or direct from command line, if you use gradle test plugin:
```groovy
plugins {
    id "java"
    id "dev.clojurephant.clojure" version "0.6.0"
    id 'com.adarshr.test-logger' version '3.2.0'
}
```

```shell
$ ./gradlew test

> Task :sample-project:test

com.some_project.RunClojureTests

  onlyTestsWithCustomStacktraceFilter()

    Test #'com.clojure-project.some-tests/ok-assertion PASSED
    Test #'com.clojure-project.some-tests/failed-assertion FAILED

    com.github.dnikku.clojure_testing.ClojureTests$Fx$_AssertionFailedError: FAIL in (failed-assertion) (some_tests.clj:12)
    expected: <(= {:some-key "one"} {:some-key "two"})> but was: <(not (= {:some-key "one"} {:some-key "two"}))>


    Test #'com.clojure-project.some-tests/unhandled-exception FAILED

    com.github.dnikku.clojure_testing.ClojureTests$Fx$_Error: ERROR in (unhandled-exception) (some_tests.clj:7)
        at com.some_project.RunClojureTests.onlyTestsWithCustomStacktraceFilter(RunClojureTests.java:33)


  all()

    Test #'com.clojure-project.some-expectations/ok-assertion PASSED
    Test #'com.clojure-project.some-expectations/failed-assertion FAILED

    com.github.dnikku.clojure_testing.ClojureTests$Fx$_AssertionFailedError: FAIL in (failed-assertion) (some_expectations.clj:10)
    expected: <{:some-key "one"}> but was: <[{:some-key "two"}]>


    Test #'com.clojure-project.some-tests/ok-assertion PASSED
    Test #'com.clojure-project.some-tests/failed-assertion FAILED

    com.github.dnikku.clojure_testing.ClojureTests$Fx$_AssertionFailedError: FAIL in (failed-assertion) (some_tests.clj:12)
    expected: <{:some-key "one"}> but was: <({:some-key "two"})>


    Test #'com.clojure-project.some-tests/unhandled-exception FAILED

    com.github.dnikku.clojure_testing.ClojureTests$Fx$_Error: ERROR in (unhandled-exception) (some_tests.clj:7)



  onlyExpectations()

    Test #'com.clojure-project.some-expectations/ok-assertion PASSED
    Test #'com.clojure-project.some-expectations/failed-assertion FAILED

    com.github.dnikku.clojure_testing.ClojureTests$Fx$_AssertionFailedError: FAIL in (failed-assertion) (some_expectations.clj:10)
    expected: <{:some-key "one"}> but was: <[{:some-key "two"}]>



com.some_project.OtherJavaTests

  Test someTest() PASSED

FAILURE: Executed 11 tests in 2.4s (6 failed)


11 tests completed, 6 failed

> Task :sample-project:test FAILED
```
