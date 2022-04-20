(ns com.clojure-project.using-clojure-testing
  (:require
    [com.github.dnikku.clojure-testing.testing :refer :all]))

;; some tests may failed, to see the output

(deftest unhandled-exception
  (throw (Exception. "unhandled error")))


(deftest failed-assertion
  (=? {:some-key "one"} {:some-key "two"}))

(deftest ok-assertion
  (=? {:some-key "one"} {:some-key "one"}))

#_(deftest multiple-ok-assertion
  (are [x y] (=? x y)
       {:some-key "one"} {:some-key "one"}
       1 1
       [2 3] [2 3]))