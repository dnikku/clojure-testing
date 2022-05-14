(ns
  ^{:doc "Imports all goodies from 'clojure.test"}
  com.github.dnikku.clojure-testing.testing
  (:require
    [clojure.test :as t]
    [com.github.dnikku.clojure-testing.print :refer [diff-str]])
  (:import
    [java.util.regex Pattern]))

(def ^:dynamic *assert-meta* nil)
(def ^:dynamic *assert-out* nil)

(defmacro deftest
  "Forwards declaration to `clojure.test/deftest`"
  [name & body]
  `(t/deftest ~name ~@body))

(with-out-str)

(defmacro is
  ([form] `(is ~form nil))
  ([form msg]
   `(let [s# (new java.io.StringWriter)]
     (with-bindings
       {#'*assert-meta* ~(meta form),
        #'*assert-out*  s#
        #'*err*         s#
        #'*out*         s#}
       (try ~(t/assert-expr msg form)
         (catch Throwable t#
           (t/do-report
            {:type     :error,
             :message  ~msg,
             :expected '~form,
             :actual   t#,
             :output   (str *assert-out*)})))))))

(defmacro are
  "Forwards declaration to `clojure.test/are`"
  [argv expr & args]
  `(t/are ~argv ~expr ~@args))

(defmacro testing
  "Forwards declaration to `clojure.test/testing`"
  [s & body]
  `(t/testing ~s ~@body))

(defmacro =?
  "Define a fake `=?` that always fail to compile but make intellij/Clojure-Kit happy."
  ([a & extra] `(Please-use-=?-only-inside-of-is)))

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

(defn assert-not-equals
  [e a msg]
  (let [r (not= e a)]
    (if r
      {:type     :pass, :message msg,
       :expected e, :actual a}
      {:type     :fail,
       :message  msg,
       :expected e,
       :actual   a})))

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

                 (t/function? e#)
                 (assert-predicate e# a# ~msg)

                 :else
                 (assert-equals e# a# ~msg))]
      (t/report
       (assoc r# :form '~form :form-meta *assert-meta* :output (str *assert-out*)))
      (:pass r#))

    `(throw (Exception. (str "=? expects 2 arguments. actual :form " '~form)))))


(defmethod t/assert-expr '=
  ;; Remap = to custom implementation, better report
  [msg form]
  (t/assert-expr msg (conj (rest form) '=?)))


(defmethod t/assert-expr 'some?
  ;; Remap = to custom implementation, better report
  [msg form]
  (if (= 2 (count form))
    `(let [a#  ~(nth form 1)
           r#  (assert-predicate some? a# ~msg)]
      (t/report
       (assoc r# :form '~form :form-meta *assert-meta* :output (str *assert-out*)))
      (:pass r#))

    `(throw (Exception. (str "=? expects 1 arguments. actual :form " '~form)))))


(defmethod t/assert-expr 'nil?
  ;; Remap = to custom implementation, better report
  [msg form]
  (if (= 2 (count form))
    `(let [a#  ~(nth form 1)
           r#  (assert-predicate nil? a# ~msg)]
      (t/report
       (assoc r# :form '~form :form-meta *assert-meta* :output (str *assert-out*)))
      (:pass r#))

    `(throw (Exception. (str "=? expects 1 arguments. actual :form " '~form)))))

(defmethod t/assert-expr 'not=
  ;; Defines a new special assert operator.
  [msg form]
  (if (= 3 (count form))
    `(let [e#  ~(nth form 1)
           a#  ~(nth form 2)
           r#  (assert-not-equals e# a# ~msg)]
      (t/report
       (assoc r# :form '~form :form-meta *assert-meta* :output (str *assert-out*)))
      (:pass r#))

    `(throw (Exception. "=? expects 2 arguments."))))



