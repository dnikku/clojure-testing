(ns clojure-testing.print-tests
  (:require
    [clojure.test :refer [deftest is]]
    [com.github.dnikku.clojure-testing.print :refer :all]))

(defn print-ex->str [tr]
  (with-out-str (print-ex tr)))


(deftest print-ex->str-java-syle
  (binding [*print-ex-stack-limit* 10]
    (let [ex-stack (print-ex->str (Exception. "some exception"))]
      (is (re-find #"^java.lang.Exception: some exception" ex-stack)))))

(deftest print-ex->str-java-syle-with-root-cause
  (binding [*print-ex-stack-limit* 4]
    (let [ex-stack (print-ex->str (Exception. "some exception" (Exception. "root cause")))]
      ;; (println ex-stack)
      (is (re-find #"^java.lang.Exception: some exception" ex-stack))
      (is (re-find #"Caused by: java.lang.Exception: root cause" ex-stack)))))