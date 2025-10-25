(ns aero-1p.core
  (:refer-clojure :exclude [run!])
  (:require [aero.core :as aero]
            [clojure.java.process :as proc]
            [clojure.string :as str])
  (:import [java.util.concurrent Semaphore]))

(defonce ^:private authorized?
  (atom false))

(defonce ^:private op-semaphore
  (Semaphore. 1))

(defn ^:private conf->op-cmd
  "Constructs the command to read a secret from 1Password CLI."
  [{:keys [account path]}]
  (->> ["op" "read"
        (when-not (contains? #{nil ::default} account)
          ["--account" account])
        path]
       flatten
       (remove nil?)))

(defn ^:private run!
  "Runs the command and returns the output as a string.
   The command is expected to be a sequence of strings."
  [cmd]
  (-> (apply proc/start cmd)
      proc/stdout
      slurp
      str/trim))

(defn ^:private get-secret
  "Fetches a secret from 1Password using the provided account and path.
   Returns a string containing the secret."
  [{:keys [account path]}]
  (run! (conf->op-cmd {:account account :path path})))

(defn- op-secret* [value]
  (let [{:keys [account path]} (cond
                                 (string? value) {:account ::default
                                                  :path value}
                                 (map? value) value)]
    (aero.core/->Deferred (future
                            (Semaphore/.acquire op-semaphore)
                            (try
                              (get-secret {:account account :path path})
                              (finally
                                (Semaphore/.release op-semaphore)))))))

(defmethod aero/reader 'op/secret
  [_opts _tag value]
  (op-secret* value))
