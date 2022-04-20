(ns com.github.dnikku.clojure-testing.interop
  (:import [java.util Iterator]))


(defn seq->java-iter
  "Converts a clojure sequence into a java iterator."
  [xs]
  (let [ss (atom (seq xs))]
    (reify
     Iterator
     (hasNext [_] (some? (first @ss)))
     (next [_]
           (let [curr (first @ss)]
             (reset! ss (rest @ss))
             curr)))))

(def coll->java-alist
  "Converts a clojure sequence into java ArrayList."
  (fn [coll]
    (let [alist (java.util.ArrayList.)]
      (doseq [it coll] (.add alist it))
      alist)))