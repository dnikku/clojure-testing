(ns com.github.dnikku.clojure-testing.repl-testing
  (:require [clojure.tools.namespace.repl :refer [refresh]]
            [clojure.string :as str]
            [clojure.stacktrace]
            [clojure.test :as t]
            [com.github.dnikku.clojure-testing.print
             :refer
             [*print-ex-exclude-re* *print-ex-stack-limit* print-ex]]
            [com.github.dnikku.clojure-testing.colorize :as c])
  (:import [clojure.lang Compiler$CompilerException]))

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
                           #'*print-ex-exclude-re*  exclude-re}
                          (t/run-all-tests pattern))))))

   (let [error (refresh :after 'com.github.dnikku.clojure-testing.repl-testing/-run-tests)]
     (cond
       (= :summary (:type error))
       (let [m error]
         (newline)
         (println
          (c/colorize [:bold :inverse]
                      "Ran " (:test m) " tests containing " (+ (:pass m) (:fail m) (:error m)) " assertions."
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
  []
  (let [m (meta (first t/*testing-vars*))]
    (str (:ns m) "/" (:name m) " (" (get-filename (:file m)) ":" (:line m) ")")))


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
   (println (c/colorize [:red] "\nFAIL in") (get-running-test))
   (when (seq t/*testing-contexts*) (println (t/testing-contexts-str)))
   (when-let [message (:message m)] (println message))
   (when (:form m) (println "    form:" (:form m) :meta (:form-meta m)))
   (println "expected:" (:expected m))
   (println "  actual:" (:actual m))
   (when (:diffs m) (println "   diffs:" (:diffs m)))
   (newline)))

(defmethod report :error
  [m]
  (t/with-test-out
   (t/inc-report-counter :error)
   (println (c/colorize [:bold :red] "\nERROR in") (get-running-test))
   (when (seq t/*testing-contexts*) (println (t/testing-contexts-str)))
   (when-let [message (:message m)] (println (c/colorize [:red] message)))
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
  (t/with-test-out
   (println
    (c/colorize [:italic :white] "Testing " (get-running-test)))))

(defmethod report :end-test-var [m])


