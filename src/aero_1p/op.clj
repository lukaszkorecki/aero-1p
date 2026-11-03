(ns aero-1p.op
  "1Password CLI client."
  (:refer-clojure :exclude [read run!])
  (:require [clojure.java.io :as io]
            [clojure.java.process :as proc]
            [clojure.string :as str])
  (:import [java.util.concurrent Semaphore]))

(defprotocol OpCli
  (read [this path]
    "Returns the value of a secret reference, e.g. `op://vault/item/field`.")
  (inject [this template]
    "Returns `template` string with all secret references replaced by their values."))

;; op prompts for authorization (Touch ID) on first use of each account,
;; serializing all invocations across all clients avoids prompting more than once
(defonce ^:private shared-lock
  (Semaphore. 1))

(defn ^:private run!
  "Runs `cmd` while holding `lock`, optionally writing `input` to its stdin.
   Returns stdout, throws when `op` exits with non-zero code."
  [^Semaphore lock cmd input]
  (Semaphore/.acquire lock)
  (try
    (let [p (apply proc/start cmd)]
      (when input
        (with-open [w (io/writer (proc/stdin p))]
          (io/copy input w)))
      (let [out (slurp (proc/stdout p))
            err (slurp (proc/stderr p))
            exit @(proc/exit-ref p)]
        (when-not (zero? exit)
          (throw (ex-info "op command failed" {:cmd cmd :exit exit :err (str/trim err)})))
        out))
    (finally
      (Semaphore/.release lock))))

(defn ^:private cmd [account subcommand & args]
  (concat ["op" subcommand]
          (when account ["--account" account])
          args))

(defrecord Cli [account lock]
  OpCli
  (read [_ path]
    (str/trim (run! lock (cmd account "read" path) nil)))
  (inject [_ template]
    (run! lock (cmd account "inject") template)))

(defn cli
  "Creates `op` CLI client for given account, `nil` uses the default account.
   All clients share one lock, so `op` is never invoked concurrently."
  [account]
  (->Cli account shared-lock))
