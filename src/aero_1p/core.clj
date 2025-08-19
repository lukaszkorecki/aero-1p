(ns aero-1p.core
  (:refer-clojure :exclude [run!])
  (:require [aero.core :as aero]
            [clojure.java.process :as proc]
            [clojure.string :as str]))

(defonce ^:private authorized?
  (atom false))

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

;; XXX: this is not ideal - we need to ensure that the 1Password CLI is authorized
;;      first, to speed up the loading of the config. Currently we don't handle reading across multiple accounts - something to fix in the future.
(defn- op-secret* [value]
  (let [{:keys [account path]} (cond
                                 (string? value) {:account ::default
                                                  :path value}
                                 (map? value) value)]
    (if (compare-and-set! authorized? false true)
      ;; run synchronously if not already authorized since 1p will prompt us
      (get-secret {:account account :path path})
      ;; return a deferred that will execute the command so that Aero optimizes loading
      (aero.core/->Deferred (future (get-secret {:account account :path path}))))))

(defmethod aero/reader 'op/secret
  [_opts _tag value]
  (op-secret* value))
