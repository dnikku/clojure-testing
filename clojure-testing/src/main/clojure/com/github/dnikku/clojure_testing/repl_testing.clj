(ns com.github.dnikku.clojure-testing.repl-testing
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.string :as str]
            [clojure.stacktrace]
            [clojure.test :as t]
            [com.github.dnikku.clojure-testing.print
             :refer
             [*print-ex-exclude-re* *print-ex-stack-limit* print-ex nano->str]]
            [com.github.dnikku.clojure-testing.colorize :as c])
  (:import [clojure.lang Compiler$CompilerException]))

(def ^:dynamic *test-var-extra* nil)

(defmulti report :type)

(def -run-tests
  (fn []
    (println (c/colorize [:cyan-bg] ":warn Rerun command!\n"))
    :done))

(defn run-tests
  "Run c.t.n.r.refresh for loading all changed files namespaces and all deps,
  then run all tests matching pattern."
  ([pattern]
   (run-tests pattern
              [#"^java\.lang"
               #"^jdk\.internal"
               #"^clojure\."
               #"^nrepl\."
               #"^dev[\$\.]"
               #"^com\.github\.dnikku\.clojure_testing\.repl_testing"
               ;;
               ]))

  ([pattern exclude-re]
   (alter-var-root #'-run-tests
                   (constantly
                    ;; capture params
                    (let [pattern    pattern
                          exclude-re exclude-re]
                      (fn []
                        (println ":running-tests"
                                 (c/colorize [:italic :cyan] `(t/run-all-tests ~(str " #\"" pattern "\""))))
                        (with-bindings
                          {#'t/report               report
                           #'*print-ex-stack-limit* nil
                           #'*print-ex-exclude-re*  exclude-re
                           #'*test-var-extra*       (atom {})}
                          (let [start-at (System/nanoTime)]
                            (assoc (t/run-all-tests pattern)
                                   :duration (nano->str (- (System/nanoTime) start-at)))))))))

   (let [error (refresh :after 'com.github.dnikku.clojure-testing.repl-testing/-run-tests)]
     (cond
       (= :summary (:type error))
       (let [m error]
         (newline)
         (println
          (c/colorize [:bold :inverse]
                      "Ran " (:test m) " tests containing " (+ (:pass m) (:fail m) (:error m)) " assertions"
                      " (in " (:duration m) ")."
                      "\n\t" (:fail m) " failures, " (:error m) " errors.\n"))
         :done)

       (= :done error)
       :done

       :else
       (with-out-str
         (if (instance? Compiler$CompilerException error)
           (let [^Compiler$CompilerException ex error
                 data                           (.getData ex)]
             (newline)
             (println
              (str (-> ex .getClass .getName) ": " (.getMessage (clojure.stacktrace/root-cause ex))))
             ;; //FIXME: [nd] - dont know how to tell intellij to interpret as link the error
             (println
              (str " at dev.repl_testing$compiler_error"
                   " (" (:clojure.error/source data) ":" (:clojure.error/line data) ")")))
           (prn error)))))))


;;; repl - reporting

(defn- get-filename
  [path]
  (second (re-find #"[\\/]?([^\\/]*$)" path)))

(defn- get-running-test
  [m]
  (let [t-meta (meta (first t/*testing-vars*))
        f-meta (:form-meta m)]
    (str (:ns t-meta) "/" (:name t-meta) " (" (get-filename (:file t-meta))
         ":" (or (:line f-meta) (:line t-meta)) ")")))


(defmethod report :default
  [m]
  (t/with-test-out (prn m)))

(defmethod report :pass
  [m]
  (t/with-test-out
   (t/inc-report-counter :pass)))

(defmethod report :fail
  [m]
  (t/with-test-out
   (t/inc-report-counter :fail)
   (println (c/colorize [:red] "\nFAIL in") (get-running-test m))
   (when (seq t/*testing-contexts*)
     (println (c/colorize [:white] (str "\t> \"" (t/testing-contexts-str) "\""))))
   (when (:form m)
     (print "    form: ")
     (prn (:form m) :meta (:form-meta m)))
   (when (:message m) (println (c/colorize [:red] (str "     msg: " (:message m)))))
   (print "expected: ") (prn (:expected m))
   (print "  actual: ")
   (let [actual (:actual m)]
     (if (instance? Throwable actual)
       (print-ex actual)
       (prn actual)))
   (when (:diffs m) (println "   diffs:" (:diffs m)))
   (newline)))

(defmethod report :error
  [m]
  (t/with-test-out
   (t/inc-report-counter :error)
   (println (c/colorize [:bold :red] "\nERROR in") (get-running-test m))
   (when (seq t/*testing-contexts*)
     (println (c/colorize [:white] (str "\t> \"" (t/testing-contexts-str) "\""))))
   (when (:message m) (println (c/colorize [:red] (str "     msg: " (:message m)))))
   (let [actual (:actual m)]
     (if (instance? Throwable actual)
       (print-ex actual)
       ; (clojure.stacktrace/print-cause-trace actual)
       (prn actual)))
   (newline)))


(defmethod report :summary [m])

(defmethod report :begin-test-ns
  [m]
  (t/with-test-out
   (println "Testing"
            (c/colorize [:bold :magenta] (str (ns-name (:ns m)))))))

(defmethod report :end-test-ns [m])

(defmethod report :begin-test-var
  [m]
  #_(t/with-test-out
     (println
      (c/colorize [:italic :white] "Testing " (get-running-test m))))
  (swap! *test-var-extra* assoc :start-at (System/nanoTime)))

(defmethod report :end-test-var
  [m]
  (let [dur (- (System/nanoTime) (:start-at @*test-var-extra*))]
    (t/with-test-out
     (println
      (c/colorize [:italic :white] "Tested " (get-running-test m) "  took " (nano->str dur))))))
