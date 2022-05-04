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
  ([form] `(is ~form nil))
  ([form msg]
   #_(println ":meta" (meta form) ":form" &form)
   `(try ~(t/assert-expr msg form)
     (catch Throwable t#
       (t/do-report
        {:type     :error, :message ~msg,
         :expected '~form, :actual t#})))))

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
  ([a] `(is ~a))
  ([e a] `(is (~'=? ~e ~a)))
  ([e a msg] `(is (~'=? ~e ~a) ~msg)))

(defn assert-equals
  [e a msg]
  (let [r (= e a)]
    (if r
      {:type     :pass, :message msg,
       :expected e, :actual a}
      {:type     :fail,
       :message  msg,
       :expected e,
       :actual   a,
       :diffs    (diff-str e a)})))

(defn assert-regex
  [re s msg]
  (let [r (re-find (re-pattern re) s)]
    (if r
      {:type     :pass, :message msg,
       :expected re, :actual s}
      {:type     :fail, :message msg,
       :expected re, :actual nil})))

(defn assert-predicate
  [pred a msg]
  (let [r (pred a)]
    (if r
      {:type     :pass, :message msg,
       :expected true, :actual r}
      {:type     :fail, :message msg,
       :expected true, :actual r})))

(defmethod t/assert-expr '=?
  ;; Defines a new special assert operator.
  [msg form]
  (if (= 3 (count form))
    `(let [e#  ~(nth form 1)
           a#  ~(nth form 2)
           r#  (cond
                 (instance? java.util.regex.Pattern e#)
                 (assert-regex e# a# ~msg)

                 (fn? e#)
                 (assert-predicate e# a# ~msg)

                 :else
                 (assert-equals e# a# ~msg))]
      (t/report (assoc r# :form '~form :form-meta {:line nil}))
      (:pass r#))

    `(throw (Exception. "=? expects 2 arguments."))))


