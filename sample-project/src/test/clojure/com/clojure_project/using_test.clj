(ns com.clojure-project.using-test
  (:require
    [clojure.test :refer [deftest is]]))

;; some tests may failed, to see the output

(deftest unhandled-exception
  (throw (Exception. "unhandled error")))


(deftest failed-assertion
  (is (= {:some-key "one"} {:some-key "two"})))

(deftest ok-assertion
  (is (= {:some-key "one"} {:some-key "one"})))