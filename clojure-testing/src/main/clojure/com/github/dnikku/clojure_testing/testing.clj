(ns
  ^{:doc "Imports all goodies from 'clojure.test"}
  com.github.dnikku.clojure-testing.testing
  (:require
    [clojure.test :as t]
    [com.github.dnikku.clojure-testing.print :refer [diff-str]])
  (:import
    [java.util.regex Pattern]))


(defmacro deftest
  "Forwards declaration to `clojure.test/deftest`"
  [name & body]
  `(t/deftest ~name ~@body))

(defmacro is
  ([form] `(t/is ~form))
  ([form msg] `(t/is ~msg ~form)))

(defmacro are
  "Forwards declaration to `clojure.test/are`"
  [argv expr & args]
  `(t/are ~argv ~expr ~@args))

(defmacro testing
  "Forwards declaration to `clojure.test/testing`"
  [s & body]
  `(t/testing ~s ~@body))

(defmacro =?
  "Preatty prints evaluated expected and actual + diffs.

  Example: (=? [2 3] [3])
  Outputs:
    expected: [2 3]
      actual: [2]
       diffs: [2 +3]"
  ([a] `(t/is ~a))
  ([e a] `(t/is (~'=? ~e ~a)))
  ([e a msg] `(t/is (~'=? ~e ~a) ~msg)))

(defn assert-equals
  [e a msg]
  (let [r (= e a)]
    (if r
      (t/do-report
       {:type     :pass, :message msg,
        :expected e, :actual a})
      (t/do-report
       {:type     :fail,
        :message  msg,
        :expected e,
        :actual   a,
        :diffs    (diff-str e a)}))
    r))

(defn assert-regex
  [re s msg]
  (let [r (re-find (re-pattern re) s)]
    (if r
      (t/do-report
       {:type     :pass, :message msg,
        :expected re, :actual s})
      (t/do-report
       {:type     :fail,
        :message  msg,
        :expected re,
        :actual   nil}))
    r))

(defmethod t/assert-expr '=?
  ;; Defines a new special assert operator.
  [msg form]
  (if (= 3 (count form))
    `(let [e#  ~(nth form 1)
           a#  ~(nth form 2)]
      (cond
        (instance? java.util.regex.Pattern e#)
        (assert-regex e# a# ~msg)

        :else
        (assert-equals e# a# ~msg)))
    `(throw (Exception. "=? expects 2 arguments."))))
