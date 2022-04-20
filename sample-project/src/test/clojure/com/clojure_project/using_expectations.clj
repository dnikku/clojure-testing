(ns com.clojure-project.using-expectations
  (:require
    [clojure.test :refer [deftest]]
    [expectations.clojure.test :refer [expect]]))


;; some tests may failed, to see the output

(deftest failed-assertion
  (expect {:some-key "one"} {:some-key "two"}))

(deftest ok-assertion
  (expect {:some-key "one"} {:some-key "one"}))
