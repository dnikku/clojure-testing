(ns com.github.dnikku.clojure-testing.print
  "Some pretty print tools."
  (:require
    [clojure.pprint :as pp]
    [clojure.string :as str]
    [clojure.stacktrace :as stacktrace])
  (:import
    [clojure.lang ExceptionInfo]))


(def ^:dynamic *print-ex-stack-limit* nil)

(def ^:dynamic *print-ex-exclude-re* [#"^org\.junit"
                                      #"^org\.gradle"
                                      #"^java\.lang"
                                      #"^jdk\.internal"
                                      #"^clojure\."
                                      #"^nrepl\."
                                      #"^dev\."])

(defn print-ex1
  "Print only current Exception to stdout."
  [^Throwable tr]
  (let [st         (if (instance? ExceptionInfo tr)
                     (let [exc (read-string (subs (pr-str tr) (count "#error")))]
                       (map
                        (fn [[cls method file line]]
                          (StackTraceElement. (str cls) (str method) (str file) line))
                        (:trace exc)))
                     (.getStackTrace tr))
        limit      (or *print-ex-stack-limit* 1000)
        exclude-re (or *print-ex-exclude-re* [])]
    (stacktrace/print-throwable tr)
    (newline)
    (print " at ")
    (if-let [e (first st)]
      (do
        (stacktrace/print-trace-element e)
        (doseq [e (->> (rest st)
                       (remove
                        (fn [^StackTraceElement e]
                          (some #(re-find % (str (.getClassName e) "." (.getMethodName e))) exclude-re)))
                       (take limit))]
          (newline)
          (print "    ")
          (stacktrace/print-trace-element e)))
      (print "[empty stack trace]"))
    (newline)))

(defn print-ex
  "Prints an Exception + all Causes to stdout."
  [^Throwable tr]
  (do
    (print-ex1 tr)
    (when-let [cause (.getCause tr)]
      (print "Caused by: ")
      (recur cause))))


(defmacro timeit
  "Evaluates expr and prints the time it took.  Returns the value of expr.
  Inspired from [[https://stackoverflow.com/questions/40529482/print-execution-time-of-function-in-seconds]]"
  [s expr]
  `(let [start# (System/nanoTime)
         ret#   ~expr]
    (println
     (str ~s " Elapsed time: " (/ (double (- (System/nanoTime) start#)) 1000000.0) " msecs"))
    ret#))


(def diffs-output?
  "If lambdaisland.deep-diff2 is in classpath loaded and set it true,
  otherwise `nil`."
  (try
    (require
     'lambdaisland.deep-diff2)
    true
    (catch Exception e
      (binding [*out* *err*]
        (println "'diffs-str will be disabled! Cause:" (.getMessage e))))))

(defn diff-str
  "Preatty print differences between provided a and b structure recursively
  using 'lambdaisland.deep-diff2 library"
  [a b]
  (if diffs-output?
    (let [diff-pp (resolve 'lambdaisland.deep-diff2/pretty-print)
          diff    (resolve 'lambdaisland.deep-diff2/diff)]
      (->>
       (with-out-str
         (diff-pp (diff a b)))
       (str/trim-newline)))))
