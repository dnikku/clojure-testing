(ns com.github.dnikku.clojure-testing.junit5-runner
  (:require [clojure.test :as t]
            [clojure.string :as str]
            [clojure.stacktrace]
            [clojure.pprint]
            [com.github.dnikku.clojure-testing.interop
             :refer
             [seq->java-iter coll->java-alist]])

  (:import [org.junit.jupiter.api.function Executable]
           [java.io File]
           [clojure.lang ExceptionInfo]
           [com.github.dnikku.clojure_testing ClojureTests$Fx]))

(def ^:dynamic *running-ns* nil)

(def ^:dynamic *collector* nil)

(defmulti report :type)

(defn- trace-arg
  [prefix x]
  (if (and (seq x) (not (string? x)))
    (map #(do (prn {prefix %}) %) (seq x))
    (prn {prefix x}))
  x)

(defmacro executable
  "Wraps the provided forms in an implementation of
              'org.junit.jupiter.api.function.Executable "
  [& forms]
  `(reify
    Executable
    (execute [_] (do ~@forms))))


(defn get-stacktrace
  "Extracts StackTraces from the provided exc and then
   filter out excluded classes and limit the stack size."
  [exc exclude-strace stack-limit]
  (letfn
    [(filter-out [st]
                 (if (not-empty st)
                   (let [match-strace? (if (some? exclude-strace)
                                         (let [patterns (map re-pattern exclude-strace)]
                                           (fn [^StackTraceElement e]
                                             (some #(re-find % (.getClassName e)) patterns)))
                                         (fn [x] false))]
                     (take stack-limit
                           (concat [(first st)] ;; first is allways included
                                   (remove match-strace? (rest st)))))
                   []))]
    (cond
      (instance? ExceptionInfo exc)
      ;; HACK: dont know how to extract :trace from ExceptionInfo, so we print it and re-read it
      (let [exc (read-string (subs (pr-str exc) (count "#error")))]
        (filter-out
         (map
          (fn [[cls method file line]]
            (StackTraceElement. (str cls) (str method) (str file) line))
          (:trace exc))))

      (instance? Throwable exc)
      (filter-out (.getStackTrace (clojure.stacktrace/root-cause exc)))

      :else nil)))


(defn discover-namespaces
  "Returns all the clojure namespaces from the provided class-path source-sets.
  Filter all namespaces not match any 'include-namespaces regexs"
  [sourceset include-ns]
  (let [clj-file?  (fn [file-name]
                     (re-seq #".clj$" (:absolutePath file-name)))
        path->ns   (fn [class-path file-path]
                     (->
                      (str/replace file-path (re-pattern (str "^" class-path "/(.*)\\.clj$")) "$1")
                      (.replace "_" "-")
                      (.replace "/" ".")))

        filter-ns  (if (or (nil? include-ns) (empty? include-ns))
                     (fn [s] true)
                     (let [patterns (map #(if (string? %) (re-pattern %) %) (seq include-ns))]
                       (fn [s] (some #(re-find % s) patterns))))

        ns-from    (fn [class-path]
                     (->> class-path File.
                          file-seq
                          (map bean)
                          (remove :hidden) (remove :directory)
                          (filter clj-file?)
                          (map
                           #(do
                             {:namespace    (path->ns class-path (:path %))
                              :absolutePath (:absolutePath %)}))
                          #_(trace-arg :ns-from)
                          identity))


        files      (->> (seq sourceset)
                        (map ns-from)
                        #_(trace-arg :ns-from)
                        (apply concat)
                        (remove #(re-seq #"^com\.github\.dnikku\.clojure_junit" (:namespace %))) ; don't include me
                        (filter #(filter-ns (:namespace %))))]

    ;; make sure all namespaces are loaded
    (doseq [{:keys [absolutePath]} files] (load-file absolutePath))

    (->> files
         (map :namespace)
         (apply sorted-set)
         identity)))

(defn execute
  [sourceset include-ns exclude-straces stack-limit]
  #_(clojure.pprint/pprint
     `(execute
       {:sourceset       ~sourceset,
        :include-ns      ~include-ns
        :exclude-straces ~exclude-straces
        :stack-limit     ~stack-limit}))

  (letfn
    [(build [m]
            (ClojureTests$Fx/newTest
             (str (:name m))
             (executable
              (case (:type m)
                :fail  (let [expected  (pr-str (:expected m))
                             actual    (pr-str (:actual m))
                             diffs     (:diffs m)]
                         (throw
                           (ClojureTests$Fx/newFailure
                            (str "FAIL in " (:where m)
                                 (if diffs (str "\ndiffs: " diffs) "")
                                 "\nexpected: <" expected "> but was: <" actual ">")
                            expected actual)))
                :error (throw
                         (ClojureTests$Fx/newError
                          (str "ERROR in " (:where m))
                          (coll->java-alist
                           (get-stacktrace (:exception m)
                                           exclude-straces
                                           (or stack-limit 1000)))))

                :pass  ()))))

     (run [namespaces]
          (when-some [namespace (first namespaces)]
            (with-bindings
              {#'t/report     report
               #'*running-ns* namespace
               #'*collector*  (atom [])}
              (t/test-ns (symbol namespace))
              (let [result @*collector*] ;; we need to rebind collector, cause lazy-cat will deref later ...
                (lazy-cat (map build result)
                          (run (rest namespaces)))))))]

    (seq->java-iter (run (discover-namespaces sourceset include-ns)))))


;;; junit - reporting

(defmethod report :default
  [m]
  (t/with-test-out (prn m)))

(defmethod report :pass
  [m]
  (t/with-test-out
   (t/inc-report-counter :pass)
   (swap! *collector* conj
          {:name (first t/*testing-vars*)
           :type :pass})))

(defmethod report :fail
  [m]
  (t/with-test-out
   (t/inc-report-counter :fail)
   (swap! *collector* conj
          {:name     (first t/*testing-vars*)
           :type     :fail
           :expected (:expected m)
           :actual   (:actual m)
           :diffs    (:diffs m)
           :where    (t/testing-vars-str m)})))

(defmethod report :error
  [m]
  (t/with-test-out
   (t/inc-report-counter :error)

   (swap! *collector* conj
          {:name      (first t/*testing-vars*)
           :type      :error
           :exception (:actual m)

           :where     (t/testing-vars-str m)})))


(defmethod report :summary [m])

(defmethod report :begin-test-ns [m])

(defmethod report :end-test-ns [m])

(defmethod report :begin-test-var [m])

(defmethod report :end-test-var [m])